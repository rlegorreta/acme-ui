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
 *  SectorService.kt
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
import java.time.format.DateTimeFormatter

/**
 * Sector to communicate to the BUP microservice server repo to get all sectors
 *
 * @project acme-ui
 * @author rlh
 * @date February 2023
 */
@Service
class SectorService(@Qualifier("authorization_code") val webClient: WebClient,
                    private val eventService: EventService,
                    private val serviceConfig: ServiceConfig): HasLogger {

    fun uri(): UriComponentsBuilder = UriComponentsBuilder.fromUriString(serviceConfig.getBupProvider())

    fun allSectors(): List<Sector> {
        val graphQLRequestBody = GraphqlRequestBody(GraphqlSchemaReaderUtil.getSchemaFromFileName("allSectores"))
        val res = webClient.post()
                            .uri(uri().path("/bup/graphql").build().toUri())
                            .accept(MediaType.APPLICATION_JSON)
                            .body(Mono.just(graphQLRequestBody), GraphqlRequestBody::class.java)
                            .attributes(clientRegistrationId(serviceConfig.securityClientId + "-oidc"))
                            .retrieve()
                            .bodyToMono(GraphqlResponseSectors::class.java)
                            .block()

        if (res == null || res.errors != null) {
            logger.error("Error al leer sectores:" + res?.errors)
            return emptyList()
        }

        return res.data!!.sectors
    }

    private fun getSector(id: String? = null, nombre: String? = null): List<Sector>? {
        val variables = if (id == null) mutableMapOf("nombre" to nombre)
                        else if (nombre == null) mutableMapOf("id" to id)
                        else mutableMapOf("id" to id, "nombre" to nombre)
        val graphQLRequestBody = GraphqlRequestBody(GraphqlSchemaReaderUtil.getSchemaFromFileName("getSector"),
                                                    variables)
        val res = webClient.post()
                            .uri(uri().path("/bup/graphql").build().toUri())
                            .accept(MediaType.APPLICATION_JSON)
                            .body(Mono.just(graphQLRequestBody), GraphqlRequestBody::class.java)
                            .attributes(clientRegistrationId(serviceConfig.securityClientId + "-oidc"))
                            .retrieve()
                            .bodyToMono(GraphqlResponseSectors::class.java)
                            .block()

        if (res == null || res.errors != null) {
            logger.error("Error al leer sectores:" + res?.errors)
            return null
        }

        return res.data!!.sectors
    }

    fun uniqueValidator(sector: Sector): String? {
        val sectors = getSector(nombre = sector.nombre)

        return if (sectors == null) "Error al leer de la base de datos"
               else if (sectors.isEmpty()) null
               else if (sectors.first().idNeo4j == sector.idNeo4j) null
               else "El sector YA existe en la base de datos"
    }

    private fun hasCompanias(id: String): Boolean {
        val sectors = getSector(id = id)

        return if (sectors == null) true
               else if (sectors.size == 1) sectors.first().companias!!.isNotEmpty()
               else false
    }

    fun addSector(sector: Sector): Sector? {
        val graphQLRequestBody = GraphqlRequestBody(GraphqlSchemaReaderUtil.getSchemaFromFileName("addSector"),
            mutableMapOf("nombre" to sector.nombre,
                         "usuarioModificacion" to sector.usuarioModificacion!!,
                         "fechaModificacion" to sector.fechaModificacion!!.format(DateTimeFormatter.ISO_DATE_TIME)))
        val res = webClient.post()
                            .uri(uri().path("/bup/graphql").build().toUri())
                            .accept(MediaType.APPLICATION_JSON)
                            .body(Mono.just(graphQLRequestBody), GraphqlRequestBody::class.java)
                            .attributes(clientRegistrationId(serviceConfig.securityClientId + "-oidc"))
                            .retrieve()
                            .toEntity(GraphqlResponseCreateSector::class.java)
                            .block()

        if ((res == null) || (res.body?.errors != null)) {
            logger.error("Error al añadir un sector:" + (res?.body?.errors ?: ""))
            eventService.sendEvent(eventType = EventType.ERROR_EVENT,
                                    headers = res!!.headers, userName = SecurityContextHolder.getContext().authentication!!.name,
                                    eventName = "ERROR:ALTA_SECTOR_INDUSTRIAL", value = EventGraphqlError(res.body?.errors, mutableMapOf("sector" to sector))
            )
            return null
        }
        eventService.sendEvent(headers = res.headers, userName = SecurityContextHolder.getContext().authentication!!.name,
                                eventName = "ALTA_SECTOR_INDUSTRIAL", value = res.body!!.data!!.createSector)

        return res.body!!.data!!.createSector
    }

    fun updateSector(sector: Sector): Sector? {
        val graphQLRequestBody = GraphqlRequestBody(GraphqlSchemaReaderUtil.getSchemaFromFileName("updateSector"),
                                    mutableMapOf("id" to sector.idNeo4j,
                                                 "nombre" to sector.nombre,
                                                 "usuarioModificacion" to sector.usuarioModificacion!!,
                                                 "fechaModificacion" to sector.fechaModificacion!!.format(DateTimeFormatter.ISO_DATE_TIME)))
        val res = webClient.post()
                            .uri(uri().path("/bup/graphql").build().toUri())
                            .accept(MediaType.APPLICATION_JSON)
                            .body(Mono.just(graphQLRequestBody), GraphqlRequestBody::class.java)
                            .attributes(clientRegistrationId(serviceConfig.securityClientId + "-oidc"))
                            .retrieve()
                            .toEntity(GraphqlResponseUpdateSector::class.java)
                            .block()

        if ((res == null) || (res.body?.errors != null)) {
            logger.error("Error al actualizar el sector:" + (res?.body?.errors ?: ""))
            eventService.sendEvent(eventType = EventType.ERROR_EVENT,
                                    headers = res!!.headers, userName = SecurityContextHolder.getContext().authentication!!.name,
                                    eventName = "ERROR:UPDATE_SECTOR_INDUSTRIAL", value = EventGraphqlError(res.body?.errors, mutableMapOf("sector" to sector)))
            return null
        }
        eventService.sendEvent(headers = res.headers, userName = SecurityContextHolder.getContext().authentication!!.name,
                                eventName = "UPDATESECTOR_INDUSTRIAL", value = res.body!!.data!!.updateSector)

        return res.body!!.data!!.updateSector
    }

    fun deleteSector(id: String): String? {
        if (hasCompanias(id))
            return "El sector que se quiere borrar tiene compañías asignadas. No se borró"

        val graphQLRequestBody = GraphqlRequestBody(GraphqlSchemaReaderUtil.getSchemaFromFileName("deleteSector"),
                                                mutableMapOf("id" to id))
        val res = webClient.post()
                            .uri(uri().path("/bup/graphql").build().toUri())
                            .accept(MediaType.APPLICATION_JSON)
                            .body(Mono.just(graphQLRequestBody), GraphqlRequestBody::class.java)
                            .attributes(clientRegistrationId(serviceConfig.securityClientId + "-oidc"))
                            .retrieve()
                            .bodyToMono(GraphqlResponseDeleteSector::class.java)
                            .block()

        if (res == null || res.errors != null) {
            logger.error("Error al borrar el sector:" + res?.errors)
            return "Error interno al tratar de borrar el sector"
        }

        return null   // ok no error
    }
}
