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
 *  EventService.kt
 *
 *  Developed 2023 by LegoSoftSoluciones, S.C. www.legosoft.com.mx
 */
package com.acme.acmeui.service.event

import com.acme.acmeui.config.ServiceConfig
import com.acme.acmeui.data.dto.GraphqlRequestBody
import com.acme.acmeui.data.dto.Notification
import com.acme.acmeui.util.GraphqlSchemaReaderUtil
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.ailegorreta.client.security.utils.HasLogger
import com.ailegorreta.commons.event.*
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.cloud.stream.function.StreamBridge
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.oauth2.client.web.reactive.function.client.ServletOAuth2AuthorizedClientExchangeFilterFunction.clientRegistrationId
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.util.UriComponentsBuilder
import reactor.core.publisher.Mono

/**
 * EventService that sends events to the kafka machine.
 *
 * @project acme-ui
 * @author rlh
 * @date February 2023
 */
@Service
class EventService(@Qualifier("authorization_code") val webClient: WebClient,
                   private var streamBridge: StreamBridge,
                   private val serviceConfig: ServiceConfig,
                   private val mapper: ObjectMapper? = null): HasLogger {
    companion object {
        const val CORRELATION_ID = "lm-correlation-id"
        // ^ this constant is the header value defined in the gateway micro.service
        const val NOTIFICATION = "NOTIFICACION"
    }

    private val coreName = "bup" // By default, in this microservice all events go to
                                    // go to the 'iam' event channel Is other channels needed
                                    // this attribute will not be a constant value

    fun uri(): UriComponentsBuilder = UriComponentsBuilder.fromUriString(serviceConfig.getAuditProvider())

    /**
     * Send the event directly to a Kafka microservice using the EventConfig class
     */
    fun sendEvent(headers: HttpHeaders?,
                  userName: String,
                  eventName: String,
                  value: Any,
                  eventType: EventType = EventType.DB_STORE): EventDTO {
        val eventBody = mapper!!.readTree(mapper.writeValueAsString(value))
        val parentNode = mapper.createObjectNode()
        val correlationId = if ((headers == null) || (headers.get(CORRELATION_ID) == null))
                                "No gateway, so no correlation id found"
                            else
                                headers.get(CORRELATION_ID)?.firstOrNull { header -> header != null }

        // Add the permit where notification will be sent
        parentNode.put("notificaFacultad", serviceConfig.getNotificaFacultad())
        parentNode.set<JsonNode>("datos", eventBody!!)

        var event = EventDTO(correlationId = correlationId ?: "No gateway, so no correlation id found",
                            eventType = eventType,
                            username = userName,
                            eventName = eventName,
                            applicationName = serviceConfig.getAppName(),
                            coreName = coreName,
                            eventBody = parentNode)

        streamBridge.send("producer-out-0",event)

        return event
    }

    /**
     * Calls the Audit server repo microservice to get all persistent events from this day.
     *
     * This method is a support for the user that did not read the on-line event
     */
    fun notifications(): List<Notification> {
        val graphQLRequestBody = GraphqlRequestBody(GraphqlSchemaReaderUtil.getSchemaFromFileName("notifications"),
                                            mutableMapOf("username" to  SecurityContextHolder.getContext().authentication!!.name))

        val res = webClient.post()
                            .uri(uri().path("/audit/graphql").build().toUri())
                            .accept(MediaType.APPLICATION_JSON)
                            .body(Mono.just(graphQLRequestBody), GraphqlRequestBody::class.java)
                            .attributes(clientRegistrationId(serviceConfig.securityClientId + "-oidc"))
                            .retrieve()
                            .bodyToMono(GraphqlResponseNotifications::class.java)
                            .block()

        return res!!.data.notifications
    }

    data class GraphqlResponseNotifications(val data: Data) {
        data class Data(val notifications: List<Notification>)
    }

}


