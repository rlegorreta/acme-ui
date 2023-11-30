/* Copyright (c) 2023, LegoSoft Soluciones, S.C.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are not permitted.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 *
 *  ApplicationAuthServerAuthoritiesMapper.java
 *
 *  Developed 2023 by LegoSoftSoluciones, S.C. www.legosoft.com.mx
 */
package com.acme.acmeui.data.service

import com.acme.acmeui.config.ServiceConfig
import com.acme.acmeui.data.dto.*
import com.acme.acmeui.service.event.EventService
import com.acme.acmeui.util.GraphqlSchemaReaderUtil
import com.ailegorreta.client.security.utils.HasLogger
import com.ailegorreta.commons.event.EventGraphqlError
import com.ailegorreta.commons.event.EventType
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.http.MediaType
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.oauth2.client.web.reactive.function.client.ServletOAuth2AuthorizedClientExchangeFilterFunction.clientRegistrationId
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.util.UriComponentsBuilder
import reactor.core.publisher.Mono

/**
 * Emails to communicate to the BUP microservice server repo to maintain the emails, inserting and deleting
 * automatically de uris, of the emails to keep related persons that have the same uri.
 *
 * The relationship depends  on Person relationship with Emails and the actual email is the the relationship data
 * EmailAsignado.
 *
 * @project acme-ui
 * @author rlh
 * @date November 2023
 */
@Service
class EmailService (@Qualifier("authorization_code") val webClient: WebClient,
                    private val eventService: EventService,
                    private val serviceConfig: ServiceConfig): HasLogger {

    fun uri(): UriComponentsBuilder = UriComponentsBuilder.fromUriString(serviceConfig.getBupProvider())

    /**
     * This is not actually the email, it is the mail server
     */
    fun getEmail(id: String? = null, uri: String? = null): List<Email>? {
        val variables = if (id == null) mutableMapOf("uri" to uri)
                        else if (uri == null) mutableMapOf("id" to id)
                        else mutableMapOf("id" to id, "uri" to uri)
        val graphQLRequestBody = GraphqlRequestBody(GraphqlSchemaReaderUtil.getSchemaFromFileName("getEmail"),
                                                    variables)
        val res = webClient.post()
                            .uri(uri().path("/bup/graphql").build().toUri())
                            .accept(MediaType.APPLICATION_JSON)
                            .body(Mono.just(graphQLRequestBody), GraphqlRequestBody::class.java)
                            .attributes(clientRegistrationId(serviceConfig.securityClientId + "-oidc"))
                            .retrieve()
                            .bodyToMono(GraphqlResponseEmails::class.java)
                            .block()

        if (res == null || res.errors != null) {
            logger.error("Error al leer los Emails servers:" + res?.errors)
            return null
        }

        return res.data!!.emails
    }

    fun addEmailIfNotExists(emailAsignado: EmailAsignado): Email? {
        val emailUri = emailAsignado.email.substringAfter('@')
        // check that not exists
        val emails = getEmail(uri = emailUri) ?: return null

        if (emails.isNotEmpty())
            return emails.first()

        val graphQLRequestBody = GraphqlRequestBody(GraphqlSchemaReaderUtil.getSchemaFromFileName("addEmail"),
                                    mutableMapOf("uri" to emailUri))
        val res = webClient.post()
                            .uri(uri().path("/bup/graphql").build().toUri())
                            .accept(MediaType.APPLICATION_JSON)
                            .body(Mono.just(graphQLRequestBody), GraphqlRequestBody::class.java)
                            .attributes(clientRegistrationId(serviceConfig.securityClientId + "-oidc"))
                            .retrieve()
                            .toEntity(GraphqlResponseCreateEmail::class.java)
                            .block()

        if ((res == null) || (res.body?.errors != null)) {
            logger.error("Error al añadir un email (server):" + (res?.body?.errors ?: ""))
            eventService.sendEvent(eventType = EventType.ERROR_EVENT,
                                    headers = res!!.headers, userName = SecurityContextHolder.getContext().authentication!!.name,
                                    eventName = "ERROR:ALTA_EMAIL_SERVER", value = EventGraphqlError(res.body?.errors, mutableMapOf("emailAsignado" to emailAsignado))
            )
            return null
        }
        eventService.sendEvent(headers = res.headers, userName = SecurityContextHolder.getContext().authentication!!.name,
                                eventName = "ALTA_EMAIL_SERVER", value = res.body!!.data!!.createEmail)

        return res.body!!.data!!.createEmail
    }

    private fun hasEmails(id: String): Boolean {
        val emails = getEmail(id = id)

        if (emails == null) return true
        else if (emails.size == 1)
             return if (emails.first().emails == null) false
                    else if (emails.first().emails!!.isEmpty()) false
                    else emails.first().emails!!.isNotEmpty()

        return true  // must never occur
    }

    fun deleteEmailIfNotNeeded(id: String): String? {
        if (hasEmails(id))
            return "El email (server) tiene personas asignadas. No se borró"

        val graphQLRequestBody = GraphqlRequestBody(GraphqlSchemaReaderUtil.getSchemaFromFileName("deleteEmail"),
                                                    mutableMapOf("id" to id))
        val res = webClient.post()
                            .uri(uri().path("/bup/graphql").build().toUri())
                            .accept(MediaType.APPLICATION_JSON)
                            .body(Mono.just(graphQLRequestBody), GraphqlRequestBody::class.java)
                            .attributes(clientRegistrationId(serviceConfig.securityClientId + "-oidc"))
                            .retrieve()
                            .toEntity(GraphqlResponseDeleteEmail::class.java)
                            .block()

        if ((res == null) || (res.body?.errors != null)) {
            logger.error("Error al borrar el email (server):" + (res?.body?.errors ?: ""))
            eventService.sendEvent(eventType = EventType.ERROR_EVENT,
                                   headers = res!!.headers, userName = SecurityContextHolder.getContext().authentication!!.name,
                                   eventName = "ERROR:DELETE_EMAIL_SERVER", value = EventGraphqlError(res.body?.errors, mutableMapOf("id" to id)))

            return "Error interno al tratar de borrar el email (server)"
        }
        eventService.sendEvent(headers = res.headers, userName = SecurityContextHolder.getContext().authentication!!.name,
                               eventName = "DELETE_EMAIL_SERVER", value = id)

        return null   // ok no error
    }
}
