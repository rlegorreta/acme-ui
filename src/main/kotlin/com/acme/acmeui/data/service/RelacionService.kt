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
 *  RelacionService.kt
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
 * Relationship from Person to Person with tipo and name as data.
 *
 * The relationship is "RELACION"
 *
 * @project acme-ui
 * @author rlh
 * @date November 2023
 */
@Service
class RelacionService (@Qualifier("authorization_code") val webClient: WebClient,
                       private val eventService: EventService,
                       private val serviceConfig: ServiceConfig): HasLogger {

    fun uri(): UriComponentsBuilder = UriComponentsBuilder.fromUriString(serviceConfig.getBupProvider())

    fun addRelacion(fromId: String, toId: String, tipo: String, name: String): Relacion? {
        val graphQLRequestBody = GraphqlRequestBody(GraphqlSchemaReaderUtil.getSchemaFromFileName("addRelationship"),
                                                    mutableMapOf("from" to fromId,
                                                                "to" to toId,
                                                                "tipo" to tipo,
                                                                "nombre" to name))
        val res = webClient.post()
                        .uri(uri().path("/bup/graphql").build().toUri())
                        .accept(MediaType.APPLICATION_JSON)
                        .body(Mono.just(graphQLRequestBody), GraphqlRequestBody::class.java)
                        .attributes(clientRegistrationId(serviceConfig.securityClientId + "-oidc"))
                        .retrieve()
                        .toEntity(GraphqlResponseCreateRelacion::class.java)
                        .block()

        if ((res == null) || (res.body?.errors != null)) {
            logger.error("Error al añadir un una relación entre personas:" + (res?.body?.errors ?: ""))
            val graphQLError = EventGraphqlError(res?.body?.errors, mutableMapOf("fromId" to fromId))

            graphQLError.addExtraData("toId", toId)
            graphQLError.addExtraData("tipo", tipo)
            graphQLError.addExtraData("name", name)
            eventService.sendEvent(eventType = EventType.ERROR_EVENT,
                                    headers = res!!.headers, userName = SecurityContextHolder.getContext().authentication!!.name,
                                    eventName = "ERROR:ALTA_RELACION_PERSONA", value = graphQLError)
            return null
        }
        eventService.sendEvent(headers = res.headers, userName = SecurityContextHolder.getContext().authentication!!.name,
                                eventName = "ALTA_RELACION_PERSONA", value = res.body!!.data!!.createRelacion)

        return res.body!!.data!!.createRelacion
    }

    /**
     * note important: ALL relationship between the two persons for "RELACION" are deleted no matter the value of
     * tipo and name are.
     *
     * If we need to keep possible existing previous relationships of "RELACION" then we need to implement the algorithm
     * same as EmailAsignado in CompaniasService class (i.e., delete all first and then insert the old ones).
     *
     * But this facility is not needed between "RELACION" since just one relation can exist for "RELACION"
     * between two persons.
     */
    fun deleteRelacion(fromId: String, toId: String): Persona? {
        val graphQLRequestBody = GraphqlRequestBody(GraphqlSchemaReaderUtil.getSchemaFromFileName("deleteRelationship"),
                                mutableMapOf("id" to fromId,
                                        "relacionDel" to toId))
        val res = webClient.post()
                            .uri(uri().path("/bup/graphql").build().toUri())
                            .accept(MediaType.APPLICATION_JSON)
                            .body(Mono.just(graphQLRequestBody), GraphqlRequestBody::class.java)
                            .attributes(clientRegistrationId(serviceConfig.securityClientId + "-oidc"))
                            .retrieve()
                            .toEntity(GraphqlResponseDeleteRelacion::class.java)
                            .block()

        if ((res == null) || (res.body?.errors != null)) {
            logger.error("Error al borrar la relación entre personas:" + (res?.body?.errors ?: ""))
            val graphQLError = EventGraphqlError(res?.body?.errors, mutableMapOf("fromId" to fromId))

            graphQLError.addExtraData("toId", toId)
            eventService.sendEvent(eventType = EventType.ERROR_EVENT,
                                    headers = res!!.headers, userName = SecurityContextHolder.getContext().authentication!!.name,
                                    eventName = "ERROR:BAJA_RELACIONES_PERSONA", value = graphQLError)
            return null
        }
        eventService.sendEvent(headers = res.headers, userName = SecurityContextHolder.getContext().authentication!!.name,
                                eventName = "BAJA_RELACIONES_PERSONA", value = res.body!!.data!!.deletePersonaRelacionDel)

        return res.body!!.data!!.deletePersonaRelacionDel
    }

}
