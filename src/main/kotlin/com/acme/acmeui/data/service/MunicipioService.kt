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
 *  MunicipioService.kt
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
 * Municipio service (colony) is a service that permits to add new municipios if they not exist in the database.
 * The municipio must have the relationship to on código already defined. Código must exist it is added before
 * usign the addZipcode method in CodigoServicio.
 *
 * note: when a municipio is inserted it is never deleted again.
 *
 * @project acme-ui
 * @author rlh
 * @date February 2023
 */
@Service
class MunicipioService (@Qualifier("authorization_code") val webClient: WebClient,
                        private val eventService: EventService,
                        private val serviceConfig: ServiceConfig): HasLogger {

    fun uri(): UriComponentsBuilder = UriComponentsBuilder.fromUriString(serviceConfig.getBupProvider())

    fun getColony(id: Long? = null, nombre: String? = null): List<Municipio>? {
        val variables = if (id == null) mutableMapOf("nombre" to nombre)
                        else if (nombre == null) mutableMapOf("id" to id)
                        else mutableMapOf("id" to id, "nombre" to nombre)
        val graphQLRequestBody = GraphqlRequestBody(GraphqlSchemaReaderUtil.getSchemaFromFileName("getColony"),
                                                    variables)
        val res = webClient.post()
                            .uri(uri().path("/bup/graphql").build().toUri())
                            .accept(MediaType.APPLICATION_JSON)
                            .body(Mono.just(graphQLRequestBody), GraphqlRequestBody::class.java)
                            .attributes(clientRegistrationId(serviceConfig.securityClientId + "-oidc"))
                            .retrieve()
                            .bodyToMono(GraphqlResponseMunicipios::class.java)
                            .block()

        if (res == null || res.errors != null) {
            logger.error("Error al leer loc municipio:" + res?.errors)
            return null
        }

        return res.data!!.municipios
    }

    fun addColony(colony: Municipio): Municipio? {
        val graphQLRequestBody = GraphqlRequestBody(
            GraphqlSchemaReaderUtil.getSchemaFromFileName("addColony"),
                                                            mutableMapOf("nombre" to colony.nombre))
        val res = webClient.post()
                            .uri(uri().path("/bup/graphql").build().toUri())
                            .accept(MediaType.APPLICATION_JSON)
                            .body(Mono.just(graphQLRequestBody), GraphqlRequestBody::class.java)
                            .attributes(clientRegistrationId(serviceConfig.securityClientId + "-oidc"))
                            .retrieve()
                            .toEntity(GraphqlResponseCreateMunicipio::class.java)
                            .block()

        if ((res == null) || (res.body?.errors != null)) {
            logger.error("Error al añadir el municipio:" + (res?.body?.errors ?: ""))
            eventService.sendEvent(eventType = EventType.ERROR_EVENT,
                                    headers = res!!.headers, userName = SecurityContextHolder.getContext().authentication!!.name,
                                    eventName = "ERROR:ALTA_MUNICIPIO", value = EventGraphqlError(res.body?.errors, mutableMapOf("colony" to colony))
            )
            return null
        }
        eventService.sendEvent(headers = res.headers, userName = SecurityContextHolder.getContext().authentication!!.name,
                               eventName = "ALTA_MUNICIPIO", value = res.body!!.data!!.createMunicipio)

        // now its relationships to zipcodes (at least on (i.e, the first one) must exist
        // note: we think they already where created before
        try {
            addColonyZipcode(idColony = res.body!!.data!!.createMunicipio._id!!,
                             idZipcode = colony.codigos!!.first().idNeo4j!!)
        } catch (e: Exception) {
            logger.error("Error al añadir la relación de código postal a estado:" + e.message)
            return null
        }

        return res.body!!.data!!.createMunicipio
    }

    fun addColonyZipcode(idColony: String, idZipcode: String): Municipio {
        val graphQLRequestBody = GraphqlRequestBody(
            GraphqlSchemaReaderUtil.getSchemaFromFileName("addColonyZipcode"),
                                                            mutableMapOf("id" to idColony,
                                                                        "codigo" to idZipcode))
        val res = webClient.post()
                            .uri(uri().path("/bup/graphql").build().toUri())
                            .accept(MediaType.APPLICATION_JSON)
                            .body(Mono.just(graphQLRequestBody), GraphqlRequestBody::class.java)
                            .attributes(clientRegistrationId(serviceConfig.securityClientId + "-oidc"))
                            .retrieve()
                            .toEntity(GraphqlResponseAddMunicipioCodigo::class.java)
                            .block()

        if ((res == null) || (res.body?.errors != null)) {
            logger.error("Error al añadir la relación municipio com código:" + (res?.body?.errors ?: ""))
            val graphQLError = EventGraphqlError(res?.body?.errors, mutableMapOf("idColony" to idColony))

            graphQLError.addExtraData("idZipcode", idZipcode)
            eventService.sendEvent(eventType = EventType.ERROR_EVENT,
                                    headers = res!!.headers, userName = SecurityContextHolder.getContext().authentication!!.name,
                                    eventName = "ERROR:ALTA_MUNICIPIO_CODIGO_POSTAL", value = graphQLError)
            throw Exception("Error al añadir la relación municipio con código")
        }
        eventService.sendEvent(headers = res.headers, userName = SecurityContextHolder.getContext().authentication!!.name,
                               eventName = "ALTA_MUNICIPIO_CODIGO_POSTAL", value = res.body!!.data!!.addMunicipioCodigo)

        return res.body!!.data!!.addMunicipioCodigo
    }
}
