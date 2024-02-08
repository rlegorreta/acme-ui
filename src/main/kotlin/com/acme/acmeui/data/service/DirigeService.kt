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
 *  DirigeService.kt
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
 * Relationship from Persona to Area with idCompania and nombreCompania as data.
 *
 * @project acme-ui
 * @author rlh
 * @date November 2023
 */
@Service
class DirigeService (@Qualifier("authorization_code") val webClient: WebClient,
                     private val eventService: EventService,
                     private val serviceConfig: ServiceConfig): HasLogger {

    fun uri(): UriComponentsBuilder = UriComponentsBuilder.fromUriString(serviceConfig.getBupProvider())

    fun addDirects(person: Persona, toId: String, idCompania: String, nombreCompania: String): Dirige? {
        if (existRelationship(person, toId, idCompania))  return null        // avoid duplicates
        val graphQLRequestBody = GraphqlRequestBody(GraphqlSchemaReaderUtil.getSchemaFromFileName("addDirect"),
                                                mutableMapOf("from" to person._id,
                                                             "to" to toId,
                                                             "idCompania" to idCompania,
                                                             "nombreCompania" to nombreCompania))
        val res = webClient.post()
                            .uri(uri().path("/bup/graphql").build().toUri())
                            .accept(MediaType.APPLICATION_JSON)
                            .body(Mono.just(graphQLRequestBody), GraphqlRequestBody::class.java)
                            .attributes(clientRegistrationId(serviceConfig.securityClientId + "-oidc"))
                            .retrieve()
                            .toEntity(GraphqlResponseCreateDirige::class.java)
                            .block()

        if ((res == null) || (res.body?.errors != null)) {
            logger.error("Error al añadir la relación de persona a area (dirige):" +  (res?.body?.errors ?: ""))
            val graphQLError = EventGraphqlError(res?.body?.errors, mutableMapOf("person" to person))

            graphQLError.addExtraData("toId", toId)
            graphQLError.addExtraData("idCompania", idCompania)
            graphQLError.addExtraData("nombreCompania", nombreCompania)
            eventService.sendEvent(eventType = EventType.ERROR_EVENT,
                                    headers = res!!.headers, userName = SecurityContextHolder.getContext().authentication!!.name,
                                    eventName = "ERROR:ALTA_AREA_DIRIGE", value = graphQLError)
            return null
        }
        eventService.sendEvent(headers = res.headers, userName = SecurityContextHolder.getContext().authentication!!.name,
                                eventName = "ALTA_AREA_DIRIGE", value = res.body!!.data!!.createDirige)

        return res.body!!.data!!.createDirige
    }

    private fun existRelationship(person: Persona, toID: String, idCompania: String): Boolean {
        if (person.dirige == null) return false

        return person.dirige!!.firstOrNull {  directs ->
                     directs.to!!._id.equals(toID) && directs.idCompania == idCompania
        } != null
    }

    fun deleteDirects(fromId: String, toId: String): Persona? {
        val graphQLRequestBody = GraphqlRequestBody(GraphqlSchemaReaderUtil.getSchemaFromFileName("deleteDirect"),
                                                    mutableMapOf("id" to fromId,
                                                                 "dirigeDel" to toId))
        val res = webClient.post()
                            .uri(uri().path("/bup/graphql").build().toUri())
                            .accept(MediaType.APPLICATION_JSON)
                            .body(Mono.just(graphQLRequestBody), GraphqlRequestBody::class.java)
                            .attributes(clientRegistrationId(serviceConfig.securityClientId + "-oidc"))
                            .retrieve()
                            .toEntity(GraphqlResponseDeleteDirige::class.java)
                            .block()

        if ((res == null) || (res.body?.errors != null)) {
            logger.error("Error al borrar la relación de persona a área (dirige):" +  (res?.body?.errors ?: ""))
            val graphQLError = EventGraphqlError(res?.body?.errors, mutableMapOf("fromId" to fromId))

            graphQLError.addExtraData("toId", toId)
            eventService.sendEvent(eventType = EventType.ERROR_EVENT,
                                    headers = res!!.headers, userName = SecurityContextHolder.getContext().authentication!!.name,
                                    eventName = "ERROR:ALTA_ELIMINAR_AREA_DIRIGE", value = graphQLError)
            return null
        }
        eventService.sendEvent(headers = res.headers, userName = SecurityContextHolder.getContext().authentication!!.name,
                                eventName = "ERROR:ALTA_ELIMINAR_AREA_DIRIGE", value = res.body!!.data!!.deletePersonaDirigeDel)

        return res.body!!.data!!.deletePersonaDirigeDel
    }

}
