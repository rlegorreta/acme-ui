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
 *  DireccionService.kt
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
 * Addresses to communicate to the BUP microservice server repo to maintain the addresses for Companies or
 * Persons , inserting and deleting manually depending on Company relationship with Addresses.
 *
 * @project acme-ui
 * @author rlh
 * @date November 2023
 */
@Service
class DireccionService (@Qualifier("authorization_code") val webClient: WebClient,
                        private val eventService: EventService,
                        private val serviceConfig: ServiceConfig): HasLogger {

    fun uri(): UriComponentsBuilder = UriComponentsBuilder.fromUriString(serviceConfig.getBupProvider())

    fun addAddress(direccion: Direccion): Direccion? {
        val graphQLRequestBody = GraphqlRequestBody(GraphqlSchemaReaderUtil.getSchemaFromFileName("addAddress"),
                                            mutableMapOf("calle" to direccion.calle,
                                                         "ciudad" to direccion.ciudad,
                                                         "tipo" to direccion.tipo))
        val res = webClient.post()
                        .uri(uri().path("/bup/graphql").build().toUri())
                        .accept(MediaType.APPLICATION_JSON)
                        .body(Mono.just(graphQLRequestBody), GraphqlRequestBody::class.java)
                        .attributes(clientRegistrationId(serviceConfig.securityClientId + "-oidc"))
                        .retrieve()
                        .toEntity(GraphqlResponseCreateDireccion::class.java)
                        .block()

        if ((res == null) || (res.body?.errors != null)) {
            logger.error("Error al añadir la dirección:" + (res?.body?.errors ?: ""))
            eventService.sendEvent(eventType = EventType.ERROR_EVENT,
                                   headers = res!!.headers, userName = SecurityContextHolder.getContext().authentication!!.name,
                                   eventName = "ERROR:AÑADIR_DIRECCION_COMPAÑIA", value = EventGraphqlError(res.body?.errors, mutableMapOf("direccion" to direccion))
            )
            return null
        }
        eventService.sendEvent(headers = res.headers, userName = SecurityContextHolder.getContext().authentication!!.name,
                               eventName = "AÑADIR_DIRECCION_COMPAÑIA", value = res.body!!.data!!.createDireccion)
        // now its relationships to municipios and código.
        // note: we think they already where created before
        try {
            addAddressZipcode(idAddress = res.body!!.data!!.createDireccion._id!!,
                              idZipcode = direccion.codigo!!.idNeo4j!!)
            addAddressColony(idAddress = res.body!!.data!!.createDireccion._id!!,
                             idColony = direccion.municipio!!.idNeo4j!!)
        } catch (e: Exception) {
            logger.error("Error al añadir las relaciones de código postal y municipio:" + e.message)
            return null
        }

        return res.body!!.data!!.createDireccion
    }

    private fun addAddressZipcode(idAddress: String, idZipcode: String): Direccion {
        val graphQLRequestBody = GraphqlRequestBody(GraphqlSchemaReaderUtil.getSchemaFromFileName("addAddressZipcode"),
                                            mutableMapOf("id" to idAddress,
                                                         "codigo" to idZipcode))
        val res = webClient.post()
                            .uri(uri().path("/bup/graphql").build().toUri())
                            .accept(MediaType.APPLICATION_JSON)
                            .body(Mono.just(graphQLRequestBody), GraphqlRequestBody::class.java)
                            .attributes(clientRegistrationId(serviceConfig.securityClientId + "-oidc"))
                            .retrieve()
                            .toEntity(GraphqlResponseAddDireccionCodigo::class.java)
                            .block()

        if ((res == null) || (res.body?.errors != null)) {
            logger.error("Error al añadir la relación dirección con código:" + (res?.body?.errors ?: ""))
            val graphQLError = EventGraphqlError(res?.body?.errors, mutableMapOf("idAddress" to idAddress))

            graphQLError.addExtraData("idZipcode", idZipcode)
            eventService.sendEvent(eventType = EventType.ERROR_EVENT,
                                   headers = res!!.headers, userName = SecurityContextHolder.getContext().authentication!!.name,
                                  eventName = "ERROR:AÑADIR_DIRECCION_CODIGO", value = graphQLError)
            throw Exception("Error al añadir la relación dirección con código")
        }
        eventService.sendEvent(headers = res.headers, userName = SecurityContextHolder.getContext().authentication!!.name,
                               eventName = "AÑADIR_DIRECCION_CODIGO", value =  res.body!!.data!!.addDireccionCodigo)

        return res.body!!.data!!.addDireccionCodigo
    }

    private fun addAddressColony(idAddress: String, idColony: String): Direccion {
        val graphQLRequestBody = GraphqlRequestBody(GraphqlSchemaReaderUtil.getSchemaFromFileName("addAddressColony"),
                                                    mutableMapOf("id" to idAddress,
                                                                 "municipio" to idColony))
        val res = webClient.post()
                            .uri(uri().path("/bup/graphql").build().toUri())
                            .accept(MediaType.APPLICATION_JSON)
                            .body(Mono.just(graphQLRequestBody), GraphqlRequestBody::class.java)
                            .attributes(clientRegistrationId(serviceConfig.securityClientId + "-oidc"))
                            .retrieve()
                            .toEntity(GraphqlResponseAddDireccionMunicipio::class.java)
                            .block()

        if ((res == null) || (res.body?.errors != null)) {
            logger.error("Error al añadir la relación dirección con municipio:" + (res?.body?.errors ?: ""))
            val graphQLError = EventGraphqlError(res?.body?.errors, mutableMapOf("idAddress" to idAddress))

            graphQLError.addExtraData("idColony", idColony)
            eventService.sendEvent(eventType = EventType.ERROR_EVENT,
                                   headers = res!!.headers, userName = SecurityContextHolder.getContext().authentication!!.name,
                                   eventName = "ERROR:AÑADIR_MUNICIPIO_DIRECCION", value = graphQLError)
            throw Exception("Error al añadir la relación dirección con municipio")
        }
        eventService.sendEvent(headers = res.headers, userName = SecurityContextHolder.getContext().authentication!!.name,
                               eventName = "AÑADIR_MUNICIPIO_DIRECCION", value = res.body!!.data!!.addDireccionMunicipio)

        return res.body!!.data!!.addDireccionMunicipio
    }

    fun deleteAddress(direccion: Direccion): Direccion? {
        // first delete its relationships for municipios and código.
        try {
            deleteAddressZipcode(idAddress = direccion._id!!, idZipcode = direccion.codigo!!.idNeo4j!!)
            deleteAddressColony(idAddress = direccion._id!!, idColony = direccion.municipio!!.idNeo4j!!)
        } catch (e: Exception) {
            logger.error("Error al borrar las relaciones de código postal y municipio:" + e.message)
            return null
        }
        val graphQLRequestBody = GraphqlRequestBody(GraphqlSchemaReaderUtil.getSchemaFromFileName("deleteAddress"),
                                                    mutableMapOf("id" to direccion._id!!))
        val res = webClient.post()
                            .uri(uri().path("/bup/graphql").build().toUri())
                            .accept(MediaType.APPLICATION_JSON)
                            .body(Mono.just(graphQLRequestBody), GraphqlRequestBody::class.java)
                            .attributes(clientRegistrationId(serviceConfig.securityClientId + "-oidc"))
                            .retrieve()
                            .toEntity(GraphqlResponseDeleteDireccion::class.java)
                            .block()

        if ((res == null) || (res.body?.errors != null)) {
            logger.error("Error al borrar la dirección:" + (res?.body?.errors ?: ""))
            eventService.sendEvent(eventType = EventType.ERROR_EVENT,
                                   headers = res!!.headers, userName = SecurityContextHolder.getContext().authentication!!.name,
                                   eventName = "ERROR:ELIMINAR_DIRECCION", value = EventGraphqlError(res.body?.errors, mutableMapOf("direccion" to direccion)))
            return null
        }
        eventService.sendEvent(headers = res.headers, userName = SecurityContextHolder.getContext().authentication!!.name,
                               eventName = " ELIMINAR_DIRECCION", value = res.body!!.data!!.deleteDireccion)

        return res.body!!.data!!.deleteDireccion
    }

    private fun deleteAddressZipcode(idAddress: String, idZipcode: String): Direccion {
        val graphQLRequestBody = GraphqlRequestBody(GraphqlSchemaReaderUtil.getSchemaFromFileName("deleteAddressZipcode"),
                                                    mutableMapOf("id" to idAddress,
                                                                 "codigo" to idZipcode))
        val res = webClient.post()
                            .uri(uri().path("/bup/graphql").build().toUri())
                            .accept(MediaType.APPLICATION_JSON)
                            .body(Mono.just(graphQLRequestBody), GraphqlRequestBody::class.java)
                            .attributes(clientRegistrationId(serviceConfig.securityClientId + "-oidc"))
                            .retrieve()
                            .toEntity(GraphqlResponseDeleteDireccionCodigo::class.java)
                            .block()

        if ((res == null) || (res.body?.errors != null)) {
            logger.error("Error al borrar la relación dirección con código:" + (res?.body?.errors ?: ""))
            val graphQLError = EventGraphqlError(res?.body?.errors, mutableMapOf("idAddress" to idAddress))

            graphQLError.addExtraData("idZipcode", idZipcode)
            eventService.sendEvent(eventType = EventType.ERROR_EVENT,
                                    headers = res!!.headers, userName = SecurityContextHolder.getContext().authentication!!.name,
                                    eventName = "ERROR:ELIMINAR_DIRECCION_CODIGO", value = graphQLError)
            throw Exception("Error al borrar la relación dirección con código")
        }
        eventService.sendEvent(headers = res.headers, userName = SecurityContextHolder.getContext().authentication!!.name,
                               eventName = "ELIMINAR_DIRECCION_CODIGO", value = res.body!!.data!!.deleteDireccionCodigo)

        return res.body!!.data!!.deleteDireccionCodigo
    }

    private fun deleteAddressColony(idAddress: String, idColony: String): Direccion {
        val graphQLRequestBody = GraphqlRequestBody(GraphqlSchemaReaderUtil.getSchemaFromFileName("deleteAddressColony"),
                                                    mutableMapOf("id" to idAddress,
                                                                "municipio" to idColony))
        val res = webClient.post()
                            .uri(uri().path("/bup/graphql").build().toUri())
                            .accept(MediaType.APPLICATION_JSON)
                            .body(Mono.just(graphQLRequestBody), GraphqlRequestBody::class.java)
                            .attributes(clientRegistrationId(serviceConfig.securityClientId + "-oidc"))
                            .retrieve()
                            .toEntity(GraphqlResponseDeleteDireccionMunicipio::class.java)
                            .block()

        if ((res == null) || (res.body?.errors != null)) {
            logger.error("Error al borrar la relación dirección con municipio:" + (res?.body?.errors ?: ""))
            val graphQLError = EventGraphqlError(res?.body?.errors, mutableMapOf("idAddress" to idAddress))

            graphQLError.addExtraData("idColony", idColony)
            eventService.sendEvent(eventType = EventType.ERROR_EVENT,
                                    headers = res!!.headers, userName = SecurityContextHolder.getContext().authentication!!.name,
                                    eventName = "ERROR:ELIMINAR_DIRECCION_MUNICIPIO", value = graphQLError)
            throw Exception("Error al borrar la relación dirección con municipio")
        }
        eventService.sendEvent(headers = res.headers, userName = SecurityContextHolder.getContext().authentication!!.name,
                               eventName = "ELIMINAR_DIRECCION_MUNICIPIO", value = res.body!!.data!!.deleteDireccionMunicipio)

        return res.body!!.data!!.deleteDireccionMunicipio
    }

    fun addAddressCompany(idAddress: String, idCompany: String): Direccion {
        val graphQLRequestBody = GraphqlRequestBody(GraphqlSchemaReaderUtil.getSchemaFromFileName("addAddressCompany"),
                                                    mutableMapOf("id" to idAddress,
                                                                 "compania" to idCompany))
        val res = webClient.post()
                        .uri(uri().path("/bup/graphql").build().toUri())
                        .accept(MediaType.APPLICATION_JSON)
                        .body(Mono.just(graphQLRequestBody), GraphqlRequestBody::class.java)
                        .attributes(clientRegistrationId(serviceConfig.securityClientId + "-oidc"))
                        .retrieve()
                        .toEntity(GraphqlResponseAddDireccionCompania::class.java)
                        .block()

        if ((res == null) || (res.body?.errors != null)) {
            logger.error("Error al añadir le dirección a la compañía:" + (res?.body?.errors ?: ""))
            val graphQLError = EventGraphqlError(res?.body?.errors, mutableMapOf("idAddress" to idAddress))

            graphQLError.addExtraData("idCompany", idCompany)
            eventService.sendEvent(eventType = EventType.ERROR_EVENT,
                                    headers = res!!.headers, userName = SecurityContextHolder.getContext().authentication!!.name,
                                    eventName = "ERROR:AÑADIR_DIRECCION_COMPANIA", value = graphQLError)
            throw Exception("Error al añadir la dirección a la compañía")
        }
        eventService.sendEvent(headers = res.headers, userName = SecurityContextHolder.getContext().authentication!!.name,
                               eventName = "AÑADIR_DIRECCION_COMPANIA", value = res.body!!.data!!.addDireccionCompania)

        return res.body!!.data!!.addDireccionCompania
    }

    fun deleteAddressCompany(idAddress: String, idCompany: String): Direccion {
        val graphQLRequestBody = GraphqlRequestBody(GraphqlSchemaReaderUtil.getSchemaFromFileName("deleteAddressCompany"),
                                                    mutableMapOf("id" to idAddress,
                                                                "compania" to idCompany))
        val res = webClient.post()
                            .uri(uri().path("/bup/graphql").build().toUri())
                            .accept(MediaType.APPLICATION_JSON)
                            .body(Mono.just(graphQLRequestBody), GraphqlRequestBody::class.java)
                            .attributes(clientRegistrationId(serviceConfig.securityClientId + "-oidc"))
                            .retrieve()
                            .toEntity(GraphqlResponseDeleteDireccionCompania::class.java)
                            .block()

        if ((res == null) || (res.body?.errors != null)) {
            logger.error("Error al borrar la dirección de la compañía:" + (res?.body?.errors ?: ""))
            val graphQLError = EventGraphqlError(res?.body?.errors, mutableMapOf("idAddress" to idAddress))

            graphQLError.addExtraData("idCompany", idCompany)
            eventService.sendEvent(eventType = EventType.ERROR_EVENT,
                                    headers = res!!.headers, userName = SecurityContextHolder.getContext().authentication!!.name,
                                    eventName = "ERROR:ELIMINAR_DIRECCION_COMPANIA", value = graphQLError)
            throw Exception("Error al borrar la dirección de la compañía")
        }
        eventService.sendEvent(headers = res.headers, userName = SecurityContextHolder.getContext().authentication!!.name,
                               eventName = "ELIMINAR_DIRECCION_COMPANIA", value = res.body!!.data!!.deleteDireccionCompania)

        return res.body!!.data!!.deleteDireccionCompania
    }

    fun addAddressPerson(idAddress: String, idPerson: String): Direccion {
        val graphQLRequestBody = GraphqlRequestBody(GraphqlSchemaReaderUtil.getSchemaFromFileName("addAddressPerson"),
                                                    mutableMapOf("id" to idAddress,
                                                                 "persona" to idPerson))
        val res = webClient.post()
                            .uri(uri().path("/bup/graphql").build().toUri())
                            .accept(MediaType.APPLICATION_JSON)
                            .body(Mono.just(graphQLRequestBody), GraphqlRequestBody::class.java)
                            .attributes(clientRegistrationId(serviceConfig.securityClientId + "-oidc"))
                            .retrieve()
                            .toEntity(GraphqlResponseAddDireccionPersona::class.java)
                            .block()

        if ((res == null) || (res.body?.errors != null)) {
            logger.error("Error al añadir le dirección a la persona:" +  (res?.body?.errors ?: ""))
            val graphQLError = EventGraphqlError(res?.body?.errors, mutableMapOf("idAddress" to idAddress))

            graphQLError.addExtraData("idPerson", idPerson)
            eventService.sendEvent(eventType = EventType.ERROR_EVENT,
                                    headers = res!!.headers, userName = SecurityContextHolder.getContext().authentication!!.name,
                                    eventName = "ERROR:AÑADIR_DIRECCION_PERSONA", value = graphQLError)
            throw Exception("Error al añadir la dirección a la persona")
        }
        eventService.sendEvent(headers = res.headers, userName = SecurityContextHolder.getContext().authentication!!.name,
                               eventName = "AÑADIR_DIRECCION_PERSONA", value = res.body!!.data!!.addDireccionPersona)

        return res.body!!.data!!.addDireccionPersona
    }

    fun deleteAddressPerson(idAddress: String, idPerson: String): Direccion {
        val graphQLRequestBody = GraphqlRequestBody(GraphqlSchemaReaderUtil.getSchemaFromFileName("deleteAddressPerson"),
                                                    mutableMapOf("id" to idAddress,
                                                                "persona" to idPerson))
        val res = webClient.post()
                            .uri(uri().path("/bup/graphql").build().toUri())
                            .accept(MediaType.APPLICATION_JSON)
                            .body(Mono.just(graphQLRequestBody), GraphqlRequestBody::class.java)
                            .attributes(clientRegistrationId(serviceConfig.securityClientId + "-oidc"))
                            .retrieve()
                            .toEntity(GraphqlResponseDeleteDireccionPersona::class.java)
                            .block()

        if ((res == null) || (res.body?.errors != null)) {
            logger.error("Error al borrar la dirección de la persona:" + (res?.body?.errors ?: ""))
            val graphQLError = EventGraphqlError(res?.body?.errors, mutableMapOf("idAddress" to idAddress))

            graphQLError.addExtraData("idPerson", idPerson)
            eventService.sendEvent(eventType = EventType.ERROR_EVENT,
                                    headers = res!!.headers, userName = SecurityContextHolder.getContext().authentication!!.name,
                                    eventName = "ERROR:ELIMINAR_DIRECCION_PERSONA", value = graphQLError)
            throw Exception("Error al borrar la dirección de la persona")
        }
        eventService.sendEvent(headers = res.headers, userName = SecurityContextHolder.getContext().authentication!!.name,
                                eventName = "ELIMINAR_DIRECCION_PERSONA", value = res.body!!.data!!.deleteDireccionPersona)

        return res.body!!.data!!.deleteDireccionPersona
    }
}
