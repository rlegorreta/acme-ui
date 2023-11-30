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
 *  TelefonoService.kt
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
 * Telephones to communicate to the BUP microservice server repo to maintain the telephones, inserting and deleting
 * automatically depending on Company relationship with Telefonos.
 *
 * @project acme-ui
 * @author rlh
 * @date November 2023
 */
@Service
class TelefonoService(@Qualifier("authorization_code") val webClient: WebClient,
                      private val eventService: EventService,
                      private val serviceConfig: ServiceConfig): HasLogger {

    fun uri(): UriComponentsBuilder = UriComponentsBuilder.fromUriString(serviceConfig.getBupProvider())

    private fun getTelephone(id: String? = null, numero: String? = null): List<Telefono>? {
        val variables = if (id == null) mutableMapOf("numero" to numero)
                        else if (numero == null) mutableMapOf("id" to id)
                        else mutableMapOf("id" to id, "numero" to numero)
        val graphQLRequestBody = GraphqlRequestBody(GraphqlSchemaReaderUtil.getSchemaFromFileName("getTelephone"),
                                                    variables)
        val res = webClient.post()
                            .uri(uri().path("/bup/graphql").build().toUri())
                            .accept(MediaType.APPLICATION_JSON)
                            .body(Mono.just(graphQLRequestBody), GraphqlRequestBody::class.java)
                            .attributes(clientRegistrationId(serviceConfig.securityClientId + "-oidc"))
                            .retrieve()
                            .bodyToMono(GraphqlResponseTelefonos::class.java)
                            .block()

        if (res == null || res.errors != null) {
            logger.error("Error al leer el teléfono:" + res?.errors)
            return null
        }

        return res.data!!.telefonoes
    }

    fun addTelephoneIfNotExists(telephone: Telefono): Telefono? {
        // check that not exists
        val telefonos = getTelephone(numero = telephone.numero) ?: return null

        if (telefonos.isNotEmpty()) return telefonos.first()

        val graphQLRequestBody = GraphqlRequestBody(GraphqlSchemaReaderUtil.getSchemaFromFileName("addTelephone"),
                                                    mutableMapOf("numero" to telephone.numero.filter { it.isDigit() },
                                                                 "ciudad" to telephone.ciudad,
                                                                 "tipo" to telephone.tipo))
        val res = webClient.post()
                            .uri(uri().path("/bup/graphql").build().toUri())
                            .accept(MediaType.APPLICATION_JSON)
                            .body(Mono.just(graphQLRequestBody), GraphqlRequestBody::class.java)
                            .attributes(clientRegistrationId(serviceConfig.securityClientId + "-oidc"))
                            .retrieve()
                            .toEntity(GraphqlResponseCreateTelefono::class.java)
                            .block()

        if ((res == null) || (res.body?.errors != null)) {
            logger.error("Error al añadir un teléfono:" + (res?.body?.errors ?: ""))
            eventService.sendEvent(eventType = EventType.ERROR_EVENT,
                                    headers = res!!.headers, userName = SecurityContextHolder.getContext().authentication!!.name,
                                    eventName = "ERROR:ALTA_TELEFONO", value = EventGraphqlError(res.body?.errors, mutableMapOf("telephone" to telephone))
            )
            return null
        }

        eventService.sendEvent(headers = res.headers, userName = SecurityContextHolder.getContext().authentication!!.name,
                                eventName = "ERROR:ALTA_TELEFONO", value = res.body!!.data!!.createTelefono)

        return res.body!!.data!!.createTelefono
    }

    private fun hasCompanies(id: String, idCompany: String): Boolean {
        val telephones = getTelephone(id = id)

        return if (telephones == null) true
               else if (telephones.size == 1)
                   if (telephones.first().companias == null) return false
                   else if (telephones.first().companias!!.isEmpty()) return false
                   else if (telephones.first().companias!!.size > 1) return true
                   else (!telephones.first().companias!!.first().idNeo4j.equals(idCompany))
               else true // this is not possible
    }

    fun deleteTelephoneIfNotNeeded(id: String, idCompany: String): String? {
        if (hasCompanies(id, idCompany))
            return "El teléfono tiene compañías asignadas. No se borró"

        val graphQLRequestBody = GraphqlRequestBody(GraphqlSchemaReaderUtil.getSchemaFromFileName("deleteTelephone"),
                                                    mutableMapOf("id" to id))
        val res = webClient.post()
                            .uri(uri().path("/bup/graphql").build().toUri())
                            .accept(MediaType.APPLICATION_JSON)
                            .body(Mono.just(graphQLRequestBody), GraphqlRequestBody::class.java)
                            .attributes(clientRegistrationId(serviceConfig.securityClientId + "-oidc"))
                            .retrieve()
                            .toEntity(GraphqlResponseDeleteTelefono::class.java)
                            .block()

        if ((res == null) || (res.body?.errors != null)) {
            logger.error("Error al borrar el teléfono:" + (res?.body?.errors ?: ""))
            val graphQLError = EventGraphqlError(res?.body?.errors, mutableMapOf("id" to id))

            graphQLError.addExtraData("idCompany", idCompany)
            eventService.sendEvent(eventType = EventType.ERROR_EVENT,
                                    headers = res!!.headers, userName = SecurityContextHolder.getContext().authentication!!.name,
                                    eventName = "ERROR:BAJA_TELEFONO", value = graphQLError)
            return "Error interno al tratar de borrar el teléfono"
        }
        eventService.sendEvent(headers = res.headers, userName = SecurityContextHolder.getContext().authentication!!.name,
                                eventName = "BAJA_TELEFONO", value = id)

        return null   // ok no error
    }

    fun addTelephoneCompany(idTelephone: String, idCompany: String): Telefono {
        val graphQLRequestBody = GraphqlRequestBody(GraphqlSchemaReaderUtil.getSchemaFromFileName("addTelephoneCompany"),
                                                    mutableMapOf("id" to idTelephone,
                                                                 "compania" to idCompany))
        val res = webClient.post()
                            .uri(uri().path("/bup/graphql").build().toUri())
                            .accept(MediaType.APPLICATION_JSON)
                            .body(Mono.just(graphQLRequestBody), GraphqlRequestBody::class.java)
                            .attributes(clientRegistrationId(serviceConfig.securityClientId + "-oidc"))
                            .retrieve()
                            .toEntity(GraphqlResponseAddTelefonoCompania::class.java)
                            .block()

        if ((res == null) || (res.body?.errors != null)) {
            logger.error("Error al añadir la relación teléfono compañía:" + (res?.body?.errors ?: ""))
            val graphQLError = EventGraphqlError(res?.body?.errors, mutableMapOf("idTelephone" to idTelephone))

            graphQLError.addExtraData("idCompany", idCompany)
            eventService.sendEvent(eventType = EventType.ERROR_EVENT,
                                    headers = res!!.headers, userName = SecurityContextHolder.getContext().authentication!!.name,
                                    eventName = "ERROR:ALTA_TELEFONO_COMPAÑÏA", value = graphQLError)
            throw Exception("Error al añadir la relación teléfono compañía")
        }
        eventService.sendEvent(headers = res.headers, userName = SecurityContextHolder.getContext().authentication!!.name,
                                eventName = "ALTA_TELEFONO_COMPAÑÏA", value = res.body!!.data!!.addTelefonoCompania)

        return res.body!!.data!!.addTelefonoCompania
    }

    fun deleteTelephoneCompany(idTelephone: String, idCompany: String): Telefono {
        val graphQLRequestBody = GraphqlRequestBody(GraphqlSchemaReaderUtil.getSchemaFromFileName("deleteTelephoneCompany"),
                                                        mutableMapOf("id" to idTelephone,
                                                                     "compania" to idCompany))
        val res = webClient.post()
                            .uri(uri().path("/bup/graphql").build().toUri())
                            .accept(MediaType.APPLICATION_JSON)
                            .body(Mono.just(graphQLRequestBody), GraphqlRequestBody::class.java)
                            .attributes(clientRegistrationId(serviceConfig.securityClientId + "-oidc"))
                            .retrieve()
                            .toEntity(GraphqlResponseDeleteTelefonoCompania::class.java)
                            .block()

        if ((res == null) || (res.body?.errors != null)) {
            logger.error("Error borrar la relación teléfono compañía:" + (res?.body?.errors ?: ""))
            val graphQLError = EventGraphqlError(res?.body?.errors, mutableMapOf("idTelephone" to idTelephone))

            graphQLError.addExtraData("idCompany", idCompany)
            eventService.sendEvent(eventType = EventType.ERROR_EVENT,
                                    headers = res!!.headers, userName = SecurityContextHolder.getContext().authentication!!.name,
                                    eventName = "ERROR:BAJA_TELEFONO_COMPAÑÏA", value = graphQLError)
            throw Exception("Error al borrar la relación teléfono compañía")
        }
        eventService.sendEvent(headers = res.headers, userName = SecurityContextHolder.getContext().authentication!!.name,
                                eventName = "BAJA_TELEFONO_COMPAÑÏA", value = res.body!!.data!!.deleteTelefonoCompania)

        return res.body!!.data!!.deleteTelefonoCompania
    }

    fun addTelephonePerson(idTelephone: String, idPerson: String): Telefono {
        val graphQLRequestBody = GraphqlRequestBody(GraphqlSchemaReaderUtil.getSchemaFromFileName("addTelephonePerson"),
                                                    mutableMapOf("id" to idTelephone,
                                                                 "persona" to idPerson))
        val res = webClient.post()
                            .uri(uri().path("/bup/graphql").build().toUri())
                            .accept(MediaType.APPLICATION_JSON)
                            .body(Mono.just(graphQLRequestBody), GraphqlRequestBody::class.java)
                            .attributes(clientRegistrationId(serviceConfig.securityClientId + "-oidc"))
                            .retrieve()
                            .toEntity(GraphqlResponseAddTelefonoPersona::class.java)
                            .block()

        if ((res == null) || (res.body?.errors != null)) {
            logger.error("Error al añadir la relación teléfono persona:" + (res?.body?.errors ?: ""))
            val graphQLError = EventGraphqlError(res?.body?.errors, mutableMapOf("idTelephone" to idTelephone))

            graphQLError.addExtraData("idPerson", idPerson)
            eventService.sendEvent(eventType = EventType.ERROR_EVENT,
                                    headers = res!!.headers, userName = SecurityContextHolder.getContext().authentication!!.name,
                                    eventName = "ERROR:ALTA_TELEFONO_PERSONA", value = graphQLError)
            throw Exception("Error al añadir la relación teléfono persona")
        }
        eventService.sendEvent(headers = res.headers, userName = SecurityContextHolder.getContext().authentication!!.name,
                                eventName = "ALTA_TELEFONO_PERSONA", value = res.body!!.data!!.addTelefonoPersona)

        return res.body!!.data!!.addTelefonoPersona
    }

    fun deleteTelephonePerson(idTelephone: String, idPerson: String): Telefono {
        val graphQLRequestBody = GraphqlRequestBody(GraphqlSchemaReaderUtil.getSchemaFromFileName("deleteTelephonePerson"),
                                                    mutableMapOf("id" to idTelephone,
                                                                 "person" to idPerson))
        val res = webClient.post()
                            .uri(uri().path("/bup/graphql").build().toUri())
                            .accept(MediaType.APPLICATION_JSON)
                            .body(Mono.just(graphQLRequestBody), GraphqlRequestBody::class.java)
                            .attributes(clientRegistrationId(serviceConfig.securityClientId + "-oidc"))
                            .retrieve()
                            .toEntity(GraphqlResponseDeleteTelefonoPersona::class.java)
                            .block()

        if ((res == null) || (res.body?.errors != null)) {
            logger.error("Error borrar la relación teléfono compañía:" + (res?.body?.errors ?: ""))
            val graphQLError = EventGraphqlError(res?.body?.errors, mutableMapOf("idTelephone" to idTelephone))

            graphQLError.addExtraData("idPerson", idPerson)
            eventService.sendEvent(eventType = EventType.ERROR_EVENT,
                                    headers = res!!.headers, userName = SecurityContextHolder.getContext().authentication!!.name,
                                    eventName = "ERROR:BAJA_TELEFONO_PERSONA", value = graphQLError)
            throw Exception("Error al borrar la relación teléfono compañía")
        }
        eventService.sendEvent(headers = res.headers, userName = SecurityContextHolder.getContext().authentication!!.name,
                                eventName = "BAJA_TELEFONO_PERSONA", value = res.body!!.data!!.deleteTelefonoPersona)

        return res.body!!.data!!.deleteTelefonoPersona
    }
}
