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
 *  AreaService.kt
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
 * Areas to communicate to the BUP microservice server repo to maintain the areas, inserting and deleting
 * automatically depending on Company relationship with Areas.
 *
 * @project acme-ui
 * @author rlh
 * @date November 2023
 */
@Service
class AreaService(@Qualifier("authorization_code") val webClient: WebClient,
                  private val eventService: EventService,
                  private val serviceConfig: ServiceConfig
): HasLogger {

    fun uri(): UriComponentsBuilder = UriComponentsBuilder.fromUriString(serviceConfig.getBupProvider())

    fun getArea(id: String? = null, nombre: String? = null): List<Area>? {
        val variables = if (id == null) mutableMapOf("nombre" to nombre)
                        else if (nombre == null) mutableMapOf("id" to id)
                        else mutableMapOf("id" to id, "nombre" to nombre)
        val graphQLRequestBody = GraphqlRequestBody(GraphqlSchemaReaderUtil.getSchemaFromFileName("getArea"),
                                                    variables)
        val res = webClient.post()
                            .uri(uri().path("/bup/graphql").build().toUri())
                            .accept(MediaType.APPLICATION_JSON)
                            .body(Mono.just(graphQLRequestBody), GraphqlRequestBody::class.java)
                            .attributes(clientRegistrationId(serviceConfig.securityClientId + "-oidc"))
                            .retrieve()
                            .bodyToMono(GraphqlResponseAreas::class.java)
                            .block()

        if (res == null || res.errors != null) {
            logger.error("Error al leer las areas:" + res?.errors)
            return null
        }

        return res.data!!.areas
    }

    fun addAreaIfNotExists(area: Area): Area? {
        // check that not exists
        val areas = getArea(nombre = area.nombre) ?: return null

        if (areas.isNotEmpty()) return areas.first()

        val graphQLRequestBody = GraphqlRequestBody(GraphqlSchemaReaderUtil.getSchemaFromFileName("addArea"),
                                                    mutableMapOf("nombre" to area.nombre))
        val res = webClient.post()
                        .uri(uri().path("/bup/graphql").build().toUri())
                        .accept(MediaType.APPLICATION_JSON)
                        .body(Mono.just(graphQLRequestBody), GraphqlRequestBody::class.java)
                        .attributes(clientRegistrationId(serviceConfig.securityClientId + "-oidc"))
                        .retrieve()
                        .toEntity(GraphqlResponseCreateArea::class.java)
                        .block()

        if (res == null || res.body!!.errors != null) {
            logger.error("Error al añadir una área:" + (res?.body?.errors ?: ""))
            eventService.sendEvent(eventType = EventType.ERROR_EVENT,
                                   headers = res!!.headers, userName = SecurityContextHolder.getContext().authentication!!.name,
                                   eventName = "ERROR:ALTA_AREA_COMPAÑIA", value = EventGraphqlError(res.body?.errors, mutableMapOf("area" to area))
            )
            return null
        }
        eventService.sendEvent(headers = res.headers, userName = SecurityContextHolder.getContext().authentication!!.name,
                               eventName = "ALTA_AREA_COMPAÑIA", value = res.body!!.data!!.createArea)

        return res.body!!.data!!.createArea
    }

    private fun hasCompanies(id: String, idCompany: String): Boolean {
        val areas = getArea(id = id)

        return if (areas == null) true
               else if (areas.size == 1)
                        if (areas.first().companias == null) return false
                        else if (areas.first().companias!!.isEmpty()) return false
                        else if (areas.first().companias!!.size > 1) return true
                        else (!areas.first().companias!!.first().idNeo4j.equals(idCompany))
                else true // this is not possible
    }

    fun deleteAreaIfNotNeeded(id: String, idCompany: String): String? {
        if (hasCompanies(id, idCompany))
            return "El área tiene compañías asignadas. No se borró"

        val graphQLRequestBody = GraphqlRequestBody(GraphqlSchemaReaderUtil.getSchemaFromFileName("deleteArea"),
                                                    mutableMapOf("id" to id))
        val res = webClient.post()
                            .uri(uri().path("/bup/graphql").build().toUri())
                            .accept(MediaType.APPLICATION_JSON)
                            .body(Mono.just(graphQLRequestBody), GraphqlRequestBody::class.java)
                            .attributes(clientRegistrationId(serviceConfig.securityClientId + "-oidc"))
                            .retrieve()
                            .toEntity(GraphqlResponseDeleteArea::class.java)
                            .block()

        if (res == null || res.body!!.errors != null) {
            logger.error("Error al borrar el área:" + (res?.body?.errors ?: ""))
            val graphQLError = EventGraphqlError(res?.body?.errors, mutableMapOf("id" to id))

            graphQLError.addExtraData("idCompany", idCompany)
            eventService.sendEvent(eventType = EventType.ERROR_EVENT,
                                   headers = res!!.headers, userName = SecurityContextHolder.getContext().authentication!!.name,
                                   eventName = "ERROR:BORRADO_AUTOMATICO_AREA_COMPAÑIA", value = graphQLError)
            return "Error interno al tratar de borrar el área"
        }
        eventService.sendEvent(headers = res.headers, userName = SecurityContextHolder.getContext().authentication!!.name,
                               eventName = "BORRADO_AUTOMATICO_AREA_COMPAÑIA", value = idCompany)

        return null   // ok no error
    }

    fun addAreaCompany(idArea: String, idCompany: String): Area {
        val graphQLRequestBody = GraphqlRequestBody(GraphqlSchemaReaderUtil.getSchemaFromFileName("addAreaCompany"),
                                        mutableMapOf("id" to idArea,
                                                     "compania" to idCompany))
        val res = webClient.post()
                            .uri(uri().path("/bup/graphql").build().toUri())
                            .accept(MediaType.APPLICATION_JSON)
                            .body(Mono.just(graphQLRequestBody), GraphqlRequestBody::class.java)
                            .attributes(clientRegistrationId(serviceConfig.securityClientId + "-oidc"))
                            .retrieve()
                            .toEntity(GraphqlResponseAddAreaCompania::class.java)
                            .block()

        if ((res == null) || (res.body!!.errors != null)) {
            logger.error("Error al añadir la relación área compañía:" + (res?.body?.errors ?: ""))
            val graphQLError = EventGraphqlError(res?.body?.errors, mutableMapOf("idArea" to idArea))

            graphQLError.addExtraData("idCompany", idCompany)
            eventService.sendEvent(eventType = EventType.ERROR_EVENT,
                                  headers = res!!.headers, userName = SecurityContextHolder.getContext().authentication!!.name,
                                   eventName = "ERROR:ASIGNACION_AREA_COMPAÑIA", value = graphQLError)

            throw Exception("Error al añadir la relación área compañía")
        }
        eventService.sendEvent(headers = res.headers, userName = SecurityContextHolder.getContext().authentication!!.name,
                              eventName = "ASIGNACION_AREA_COMPAÑIA", value = res.body!!.data!!.addAreaCompania)

        return res.body!!.data!!.addAreaCompania
    }

    fun deleteAreaCompany(idArea: String, idCompany: String): Area {
        val graphQLRequestBody = GraphqlRequestBody(GraphqlSchemaReaderUtil.getSchemaFromFileName("deleteAreaCompany"),
                                                mutableMapOf("id" to idArea,
                                                             "compania" to idCompany))
        val res = webClient.post()
                            .uri(uri().path("/bup/graphql").build().toUri())
                            .accept(MediaType.APPLICATION_JSON)
                            .body(Mono.just(graphQLRequestBody), GraphqlRequestBody::class.java)
                            .attributes(clientRegistrationId(serviceConfig.securityClientId + "-oidc"))
                            .retrieve()
                            .toEntity(GraphqlResponseDeleteAreaCompania::class.java)
                            .block()

        if ((res == null) || (res.body!!.errors != null)) {
            logger.error("Error borrar la relación área compañía:" + (res?.body?.errors ?: ""))
            val graphQLError = EventGraphqlError(res?.body?.errors, mutableMapOf("idArea" to idArea))

            graphQLError.addExtraData("idCompany", idCompany)
            eventService.sendEvent(eventType = EventType.ERROR_EVENT,
                                   headers = res!!.headers, userName = SecurityContextHolder.getContext().authentication!!.name,
                                   eventName = "ERROR:DES_ASIGNACION_AREA_COMPAÑIA", value = graphQLError)
            throw Exception("Error al borrar la relación área compañía")
        }
        eventService.sendEvent(headers = res.headers, userName = SecurityContextHolder.getContext().authentication!!.name,
                               eventName = "DES_ASIGNACION_AREA_COMPAÑIA", value = res.body!!.data!!.deleteAreaCompania)

        return res.body!!.data!!.deleteAreaCompania
    }

}
