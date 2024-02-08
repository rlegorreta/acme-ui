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
 *  RfcService.kt
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
 * Rfc to communicate to the BUP microservice server repo
 *
 * @project acme-ui
 * @author rlh
 * @date February 20204
 */
@Service
class RfcService(@Qualifier("authorization_code") val webClient: WebClient,
                 private val eventService: EventService,
                 private val serviceConfig: ServiceConfig): HasLogger {

    fun uri(): UriComponentsBuilder = UriComponentsBuilder.fromUriString(serviceConfig.getBupProvider())

    private fun getRfc(id: String? = null, rfc: String? = null): List<Rfc>? {
        val variables = if (id == null) mutableMapOf("rfc" to rfc)
                        else if (rfc == null) mutableMapOf("id" to id)
                        else mutableMapOf("id" to id, "rfc" to rfc)
        val graphQLRequestBody = GraphqlRequestBody(GraphqlSchemaReaderUtil.getSchemaFromFileName("getRfc"),
                                    variables)
        val res = webClient.post()
                            .uri(uri().path("/bup/graphql").build().toUri())
                            .accept(MediaType.APPLICATION_JSON)
                            .body(Mono.just(graphQLRequestBody), GraphqlRequestBody::class.java)
                            .attributes(clientRegistrationId(serviceConfig.securityClientId + "-oidc"))
                            .retrieve()
                            .bodyToMono(GraphqlResponseRfcs::class.java)
                            .block()

        if (res == null || res.errors != null) {
            logger.error("Error al leer Rfc:" + res?.errors)
            return null
        }

        return res.data!!.rfcs
    }

    fun uniqueValidator(rfc: Rfc): String? {
        val rfcs = getRfc(rfc = rfc.rfc)

        return if (rfcs == null) "Error al leer de la base de datos"
        else if (rfcs.isEmpty()) null
        else if (rfcs.first().idNeo4j == rfc.idNeo4j) null
        else "El rfc YA existe en la base de datos"
    }

    private fun hasCompaniasOrPersonas(id: String): Boolean {
        val rfcs = getRfc(id = id)

        return if (rfcs == null) true
               else if (rfcs.size == 1) {
                   if (rfcs.first().companias!!.isEmpty())
                       rfcs.first().personas!!.isNotEmpty()
                    else
                       true
                }
                else false
    }

    fun addRfcIfNotExists(rfc: Rfc): Rfc? {
        // check if not exist the Rfc
        val rfcs = getRfc(rfc = rfc.rfc) ?: return null

        if (rfcs.isNotEmpty()) return rfcs.first()

        val graphQLRequestBody = GraphqlRequestBody(GraphqlSchemaReaderUtil.getSchemaFromFileName("addRfc"),
                                    mutableMapOf("rfc" to rfc.rfc))
        val res = webClient.post()
                            .uri(uri().path("/bup/graphql").build().toUri())
                            .accept(MediaType.APPLICATION_JSON)
                            .body(Mono.just(graphQLRequestBody), GraphqlRequestBody::class.java)
                            .attributes(clientRegistrationId(serviceConfig.securityClientId + "-oidc"))
                            .retrieve()
                            .toEntity(GraphqlResponseCreateRfc::class.java)
                            .block()

        if ((res == null) || (res.body?.errors != null)) {
            logger.error("Error al añadir un rfc:" + (res?.body?.errors ?: ""))
            eventService.sendEvent(eventType = EventType.ERROR_EVENT,
                headers = res!!.headers, userName = SecurityContextHolder.getContext().authentication!!.name,
                eventName = "ERROR:ALTA_RFC", value = EventGraphqlError(res.body?.errors, mutableMapOf("rfc" to rfc))
            )
            return null
        }
        eventService.sendEvent(headers = res.headers, userName = SecurityContextHolder.getContext().authentication!!.name,
            eventName = "ALTA_RFC", value = res.body!!.data!!.createRfc)

        return res.body!!.data!!.createRfc
    }


    fun deleteRfcIfNotNeeded(id: String): String? {
        if (hasCompaniasOrPersonas(id))
            return "El rfc que se quiere borrar tiene compañías o personar asignadas. No se borró"

        val graphQLRequestBody = GraphqlRequestBody(GraphqlSchemaReaderUtil.getSchemaFromFileName("deleteRfc"),
                                                    mutableMapOf("id" to id))
        val res = webClient.post()
                            .uri(uri().path("/bup/graphql").build().toUri())
                            .accept(MediaType.APPLICATION_JSON)
                            .body(Mono.just(graphQLRequestBody), GraphqlRequestBody::class.java)
                            .attributes(clientRegistrationId(serviceConfig.securityClientId + "-oidc"))
                            .retrieve()
                            .bodyToMono(GraphqlResponseDeleteRfc::class.java)
                            .block()

        if (res == null || res.errors != null) {
            logger.error("Error al borrar el rfc:" + res?.errors)
            return "Error interno al tratar de borrar el rfc"
        }

        return null   // ok no error
    }
}
