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
 *  CodigoService.kt
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
import org.springframework.security.oauth2.client.web.reactive.function.client.ServerOAuth2AuthorizedClientExchangeFilterFunction.clientRegistrationId
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.util.UriComponentsBuilder
import reactor.core.publisher.Mono

/**
 * Codigo service (zipcode) is a service that permits to add new códigos if they not exist in the database.
 * The código must have the relationship to estados already defined. Estado must exist and cannot be added, they
 * are a static catalog and no screen maintenance exists, just in the script.
 *
 * note: when a código is inserted it is never deleted again.
 *
 * @project acme-ui
 * @author rlh
 * @date November 2023
 */
@Service
class CodigoService(@Qualifier("authorization_code") val webClient: WebClient,
                    private val eventService: EventService,
                    private val serviceConfig: ServiceConfig): HasLogger {

    fun uri(): UriComponentsBuilder = UriComponentsBuilder.fromUriString(serviceConfig.getBupProvider())

    fun getZipcode(id: Long? = null, cp: Int? = null): List<Codigo>? {
        val variables = if (id == null) mutableMapOf("cp" to cp)
                        else if (cp == null) mutableMapOf("id" to id)
                        else mutableMapOf("id" to id, "cp" to cp)
        val graphQLRequestBody = GraphqlRequestBody(GraphqlSchemaReaderUtil.getSchemaFromFileName("getZipcode"),
            variables)
        val res = webClient.post()
                            .uri(uri().path("/bup/graphql").build().toUri())
                            .accept(MediaType.APPLICATION_JSON)
                            .body(Mono.just(graphQLRequestBody), GraphqlRequestBody::class.java)
                            .attributes(clientRegistrationId(serviceConfig.securityClientId + "-oidc"))
                            .retrieve()
                            .bodyToMono(GraphqlResponseGetCodigos::class.java)
                            .block()

        if (res == null || res.errors != null) {
            logger.error("Error al leer los códigos:" + res?.errors)
            return null
        }

        return res.data!!.codigoes
    }

    fun addZipcode(zipcode: Codigo): Codigo? {
        val graphQLRequestBody = GraphqlRequestBody(
            GraphqlSchemaReaderUtil.getSchemaFromFileName("addZipcode"),
                                                        mutableMapOf("cp" to zipcode.cp))
        val res = webClient.post()
                            .uri(uri().path("/bup/graphql").build().toUri())
                            .accept(MediaType.APPLICATION_JSON)
                            .body(Mono.just(graphQLRequestBody), GraphqlRequestBody::class.java)
                            .attributes(clientRegistrationId(serviceConfig.securityClientId + "-oidc"))
                            .retrieve()
                            .toEntity(GraphqlResponseCreateCodigo::class.java)
                            .block()

        if ((res == null) || (res.body?.errors != null)) {
            logger.error("Error al añadir un código postal:" + (res?.body?.errors ?: ""))
            eventService.sendEvent(eventType = EventType.ERROR_EVENT,
                                   headers = res?.headers, userName = SecurityContextHolder.getContext().authentication!!.name,
                                   eventName = "ERROR:ALTA_CODIGO_POSTAL", value = EventGraphqlError(res?.body?.errors, mutableMapOf("zipcode" to zipcode))
            )
            return null
        }
        eventService.sendEvent(headers = res.headers, userName = SecurityContextHolder.getContext().authentication!!.name,
                               eventName = "ALTA_CODIGO_POSTAL", value = zipcode)
        // now its relationships to states
        // note: we think they already where created before
        try {
            addZipcodeState(idZipcode = res.body!!.data!!.createCodigo._id!!,
                            idState = zipcode.estado!!.idNeo4j!!)
        } catch (e: Exception) {
            logger.error("Error al añadir la relación de código postal a estado:" + e.message)
            return null
        }

        return res.body!!.data!!.createCodigo
    }

    private fun addZipcodeState(idZipcode: String, idState: String): Codigo {
        val graphQLRequestBody = GraphqlRequestBody(GraphqlSchemaReaderUtil.getSchemaFromFileName("addZipcodeState"),
                                                    mutableMapOf("id" to idZipcode,
                                                                  "estado" to idState))
        val res = webClient.post()
                            .uri(uri().path("/bup/graphql").build().toUri())
                            .accept(MediaType.APPLICATION_JSON)
                            .body(Mono.just(graphQLRequestBody), GraphqlRequestBody::class.java)
                            .attributes(clientRegistrationId(serviceConfig.securityClientId + "-oidc"))
                            .retrieve()
                            .toEntity(GraphqlResponseAddCodigoEstado::class.java)
                            .block()

        if ((res == null) || (res.body?.errors != null)) {
            logger.error("Error al añadir la relación código con estado:" + (res?.body?.errors ?: ""))
            val graphQLError = EventGraphqlError(res?.body?.errors, mutableMapOf("idZipcode" to idZipcode))

            graphQLError.addExtraData("idState", idState)
            eventService.sendEvent(eventType = EventType.ERROR_EVENT,
                                   headers = res?.headers, userName = SecurityContextHolder.getContext().authentication!!.name,
                                   eventName = "ERROR:ASIGNAR_CODIGO_POSTAL_ESTADO", value = graphQLError)
            throw Exception("Error al añadir la relación código con estado")
        }
        eventService.sendEvent(headers = res.headers, userName = SecurityContextHolder.getContext().authentication!!.name,
                               eventName = "ASIGNAR_CODIGO_POSTAL_ESTADO", value = res.body!!.data!!.addCodigoEstado)

        return res.body!!.data!!.addCodigoEstado
    }

}
