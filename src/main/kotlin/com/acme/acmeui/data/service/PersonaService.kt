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
 *  PersonaService.kt
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
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.support.PageableExecutionUtils
import org.springframework.http.MediaType
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.oauth2.client.web.reactive.function.client.ServletOAuth2AuthorizedClientExchangeFilterFunction.clientRegistrationId
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.util.UriComponentsBuilder
import reactor.core.publisher.Mono
import java.time.format.DateTimeFormatter

/**
 * Persons to communicate to the BUP microservice server repo to get all persons
 * and do all its possible mutations
 *
 * @project acme-ui
 * @author rlh
 * @date November 2023
 */
@Service
class PersonaService (@Qualifier("authorization_code") val webClient: WebClient,
                      private val telefonoService: TelefonoService,
                      private val emailService: EmailService,
                      private val emailAsignadoService: EmailAsignadoService,
                      private val direccionService: DireccionService,
                      private val eventService: EventService,
                      private val serviceConfig: ServiceConfig): HasLogger {
    fun uri(): UriComponentsBuilder = UriComponentsBuilder.fromUriString(serviceConfig.getBupProvider())

    fun allPersons(apellidoPaterno: String?, activo: Boolean?, page: Int, size: Int): Page<Persona>? {
        val variables = if (apellidoPaterno.isNullOrBlank())
                            if (activo == null )
                                mutableMapOf("skip" to (page * size), "limit" to size)
                            else
                                mutableMapOf("activo" to activo, "skip" to (page * size), "limit" to size)
                        else if (activo == null )
                            mutableMapOf("apellidoPaterno" to apellidoPaterno,
                                         "skip" to (page * size), "limit" to size)
                        else
                            mutableMapOf("apellidoPaterno" to apellidoPaterno, "activo" to activo,
                                         "skip" to (page * size), "limit" to size)

        val graphQLRequestBody = GraphqlRequestBody(GraphqlSchemaReaderUtil.getSchemaFromFileName("allPersonsPageable"),
                                                    variables)
        val res = webClient.post()
                            .uri(uri().path("/bup/graphql").build().toUri())
                            .accept(MediaType.APPLICATION_JSON)
                            .body(Mono.just(graphQLRequestBody), GraphqlRequestBody::class.java)
                            .attributes(clientRegistrationId(serviceConfig.securityClientId + "-oidc"))
                            .retrieve()
                            .bodyToMono(GraphqlResponsePersonas::class.java)
                            .block()

        if (res == null || res.errors != null) {
            logger.error("Error al leer una página de personas:" + res?.errors)
            return null
        }

        return PageableExecutionUtils.getPage(res.data!!.personae, PageRequest.of(page, size)) { 0 }
    }


    fun count(apellidoPaterno: String?, activo: Boolean?) : Long {
        val graphQLRequestBody = if (apellidoPaterno.isNullOrBlank())
                                    if (activo == null )
                                        GraphqlRequestBody(GraphqlSchemaReaderUtil.getSchemaFromFileName("allPersonsCount"))
                                    else
                                        GraphqlRequestBody(GraphqlSchemaReaderUtil.getSchemaFromFileName("allPersonsCount"),
                                            mutableMapOf("activo" to activo))
                                else if (activo == null )
                                        GraphqlRequestBody(GraphqlSchemaReaderUtil.getSchemaFromFileName("allPersonsCount"),
                                            mutableMapOf("apellidoPaterno" to apellidoPaterno))
                                else
                                        GraphqlRequestBody(GraphqlSchemaReaderUtil.getSchemaFromFileName("allPersonsCount"),
                                            mutableMapOf("apellidoPaterno" to apellidoPaterno, "activo" to activo))

        val res = webClient.post()
                            .uri(uri().path("/bup/graphql").build().toUri())
                            .accept(MediaType.APPLICATION_JSON)
                            .body(Mono.just(graphQLRequestBody), GraphqlRequestBody::class.java)
                            .attributes(clientRegistrationId(serviceConfig.securityClientId + "-oidc"))
                            .retrieve()
                            .bodyToMono(GraphqlResponsePersonasCount::class.java)
                            .block()

        if ((res == null) || (res.errors != null)) {
            logger.error("Error al leer una el número de registros en personas:" + res?.errors)
            return 0L
        }

        return res.data!!.personasCount.toLong()
    }

    private fun getPerson(id: String? = null, nombre: String? = null,
                          apellidoPaterno: String? = null, apellidoMaterno: String? = null): List<Persona>? {
        val variables = if (id == null) mutableMapOf("nombre" to nombre,
                                                     "apellidoPaterno" to apellidoPaterno,
                                                     "apellidoMaterno" to apellidoMaterno)
                        else if (nombre == null) mutableMapOf("id" to id)
                        else mutableMapOf("id" to id,
                                          "nombre" to nombre,
                                          "apellidoPaterno" to apellidoPaterno,
                                          "apellidoMaterno" to apellidoMaterno)
        val graphQLRequestBody = GraphqlRequestBody(GraphqlSchemaReaderUtil.getSchemaFromFileName("getPerson"),
                                                    variables)
        val res = webClient.post()
                            .uri(uri().path("/bup/graphql").build().toUri())
                            .accept(MediaType.APPLICATION_JSON)
                            .body(Mono.just(graphQLRequestBody), GraphqlRequestBody::class.java)
                            .attributes(clientRegistrationId(serviceConfig.securityClientId + "-oidc"))
                            .retrieve()
                            .bodyToMono(GraphqlResponsePersonas::class.java)
                            .block()

        if (res == null || res.errors != null) {
            logger.error("Error al leer la persona:" + res?.errors)
            return null
        }

        return res.data!!.personae
    }

    fun uniqueValidator(persona: Persona): String? {
        val persons = getPerson(nombre = persona.nombre,
                                apellidoPaterno = persona.apellidoPaterno,
                                apellidoMaterno = persona.apellidoMaterno)

        return if (persons == null) "Error al leer de la base de datos"
        else if (persons.isEmpty()) null
        else if (persons.first().idNeo4j == persona.idNeo4j) null
        else "Ya existe una persona con el mismo nombre"
    }

    fun addPerson(person: Persona): Persona? {
        val graphQLRequestBody = GraphqlRequestBody(GraphqlSchemaReaderUtil.getSchemaFromFileName("addPerson"),
                                    mutableMapOf("nombre" to person.nombre,
                                                 "apellidoPaterno" to person.apellidoPaterno,
                                                 "apellidoMaterno" to person.apellidoMaterno,
                                                 "fechaNAcimiento" to person.fechaNacimiento,
                                                 "genero" to person.genero,
                                                 "estadoCivil" to person.estadoCivil,
                                                 "usuarioModificacion" to person.usuarioModificacion!!,
                                                 "fechaModificacion" to person.fechaModificacion!!.format(DateTimeFormatter.ISO_DATE_TIME),
                                                 "activo" to person.activo,
                                                 "idPersona" to person.idPersona))
        val res = webClient.post()
                            .uri(uri().path("/bup/graphql").build().toUri())
                            .accept(MediaType.APPLICATION_JSON)
                            .body(Mono.just(graphQLRequestBody), GraphqlRequestBody::class.java)
                            .attributes(clientRegistrationId(serviceConfig.securityClientId + "-oidc"))
                            .retrieve()
                            .toEntity(GraphqlResponseCreatePersona::class.java)
                            .block()

        if ((res == null) || (res.body?.errors != null)) {
            logger.error("Error al añadir la persona:" + (res?.body?.errors ?: ""))
            eventService.sendEvent(eventType = EventType.ERROR_EVENT,
                                    headers = res!!.headers, userName = SecurityContextHolder.getContext().authentication!!.name,
                                    eventName = "ERROR:ALTA_PERSONA", value = EventGraphqlError(res.body?.errors, mutableMapOf("person" to person))
            )
            return null
        }
        val newPerson = res.body!!.data!!.createPersona

        eventService.sendEvent(headers = res.headers, userName = SecurityContextHolder.getContext().authentication!!.name,
                               eventName = "ALTA_PERSONA", value = newPerson)
        // Now check relationships to be added
        // Check telephones 1:m
        if (person.telefonos != null && person.telefonos!!.isNotEmpty())
            try {
                person.telefonos!!.forEach { telephone ->
                    run {
                        val newTelephone = telefonoService.addTelephoneIfNotExists(telephone)

                        if (newTelephone != null)
                            telefonoService.addTelephonePerson(idTelephone =  newTelephone.idNeo4j!!,
                                                               idPerson = newPerson.idNeo4j!!,)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                logger.error("No se pudo dar de alta los teléfonos de la persona ${person.nombre} ${person.apellidoPaterno}: ${e.message}")
                return null
            }
        // Check email 1:m
        if (person.emails != null && person.emails!!.isNotEmpty())
            try {
                person.emails!!.forEach { emailAsignado ->
                    run {
                        val newEmail = emailService.addEmailIfNotExists(emailAsignado)

                        if (newEmail != null)
                            emailAsignadoService.addEmailAsignado(from = person, to = newEmail, emailAssigned = emailAsignado)    // add relationship
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                logger.error("No se pudo dar de alta los emails de la persona ${person.nombre} ${person.apellidoPaterno}: ${e.message}")
                return null
            }
        // Addresses 1:m
        if (person.direcciones != null && person.direcciones!!.isNotEmpty())
            try {
                person.direcciones!!.forEach { address ->
                    run {
                        val newAddress = direccionService.addAddress(address)

                        if (newAddress != null)
                            direccionService.addAddressPerson(idAddress = newAddress.idNeo4j!!,
                                                              idPerson = newPerson.idNeo4j!!)
                    }
                }
            } catch (e: Exception) {
                logger.error("No se pudo dar de alta las direcciones de la persona ${person.nombre} ${person.apellidoPaterno}: ${e.message}")
                return null
            }


        return newPerson
    }

    fun updatePerson(person: Persona): Persona? {
        // now check relationship if need to be deleted or added
        val personas = getPerson(person._id)

        if (personas == null || personas.size != 1) {
            logger.error("No se pudo leer el registro anterior de la persona ${person.nombre} ${person.apellidoPaterno}. No se hizo ninguna actualización")
            return null
        }
        val oldPerson = personas.first()

        // update person
        val graphQLRequestBody = GraphqlRequestBody(GraphqlSchemaReaderUtil.getSchemaFromFileName("updatePerson"),
                                                        mutableMapOf("id" to person._id,
                                                                     "nombre" to person.nombre,
                                                                    "apellidoPaterno" to person.apellidoPaterno,
                                                                    "apellidoMaterno" to person.apellidoMaterno,
                                                                    "fechaNacimiento" to person.fechaNacimiento,
                                                                    "genero" to person.genero,
                                                                    "estadoCivil" to person.estadoCivil,
                                                                    "usuarioModificacion" to person.usuarioModificacion!!,
                                                                    "fechaModificacion" to person.fechaModificacion!!.format(DateTimeFormatter.ISO_DATE_TIME),
                                                                    "activo" to person.activo,
                                                                    "idPersona" to person.idPersona))
        val res = webClient.post()
                        .uri(uri().path("/bup/graphql").build().toUri())
                        .accept(MediaType.APPLICATION_JSON)
                        .body(Mono.just(graphQLRequestBody), GraphqlRequestBody::class.java)
                        .attributes(clientRegistrationId(serviceConfig.securityClientId + "-oidc"))
                        .retrieve()
                        .toEntity(GraphqlResponseUpdatePersona::class.java)
                        .block()

        if ((res == null) || (res.body?.errors != null)) {
            logger.error("Error al actualizar a la persona:" + (res?.body?.errors ?: ""))
            eventService.sendEvent(eventType = EventType.ERROR_EVENT,
                                    headers = res!!.headers, userName = SecurityContextHolder.getContext().authentication!!.name,
                                    eventName = "ERROR:ACTUALIZA_PERSONA", value = EventGraphqlError(res.body?.errors, mutableMapOf("person" to person)))
            return null
        }

        // Check PersonTelefono relationship
        if (oldPerson.telefonos != null && oldPerson.telefonos!!.isNotEmpty())
            try {
                oldPerson.telefonos!!.forEach { telephone ->
                    if ((person.telefonos == null) || (!person.telefonos!!.any {it == telephone})) {
                        telefonoService.deleteTelephonePerson(idTelephone = telephone.idNeo4j!!, idPerson = oldPerson.idNeo4j!!)
                        telefonoService.deleteTelephoneIfNotNeeded(id = telephone.idNeo4j!!, idCompany = oldPerson.idNeo4j!!)
                    }
                }
            } catch (e: Exception) {
                logger.error("No se pudo dar de baja los teléfonos de la persona ${person.nombre} ${person.apellidoPaterno}: ${e.message}")
                return null
            }
        if (person.telefonos != null && person.telefonos!!.isNotEmpty())
            try {
                person.telefonos!!.forEach { telephone ->
                    if ((oldPerson.telefonos == null) || (!oldPerson.telefonos!!.any {it == telephone})) {
                        val newTelephone = telefonoService.addTelephoneIfNotExists(telephone)

                        if (newTelephone != null)
                            telefonoService.addTelephonePerson(idTelephone =  newTelephone.idNeo4j!!,
                                                               idPerson = res.body!!.data!!.updatePersona.idNeo4j!!,)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                logger.error("No se pudo dar de alta los teléfonos de la persona ${person.nombre} ${person.apellidoPaterno}: ${e.message}")
                return null
            }
        // Check PersonaDireccion relationship
        if (oldPerson.direcciones != null && oldPerson.direcciones!!.isNotEmpty())
            try {
                oldPerson.direcciones!!.forEach { address ->
                    if ((person.direcciones == null) || (!person.direcciones!!.any {it == address})) {
                        direccionService.deleteAddressPerson(idAddress = address.idNeo4j!!, idPerson = oldPerson.idNeo4j!!)
                        direccionService.deleteAddress(address)
                    }
                }
            } catch (e: Exception) {
                logger.error("No se pudo dar de baja las direcciones de la persona ${person.nombre} ${person.apellidoPaterno}: ${e.message}")
                return null
            }
        if (person.direcciones != null && person.direcciones!!.isNotEmpty())
            try {
                person.direcciones!!.forEach { address ->
                    if ((oldPerson.direcciones == null) || (!oldPerson.direcciones!!.any {it == address})) {
                        val newAddress = direccionService.addAddress(address)

                        if (newAddress != null)
                            direccionService.addAddressPerson(idAddress =  newAddress.idNeo4j!!,
                                                              idPerson = res.body!!.data!!.updatePersona.idNeo4j!!,)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                logger.error("No se pudo dar de alta las direcciones de la persona ${person.nombre} ${person.apellidoPaterno}: ${e.message}")
                return null
            }
        // Check PersonaEmails relationship
        if (oldPerson.emails != null && oldPerson.emails!!.isNotEmpty())
            try {
                val emailsSetToBeDeleted = mutableSetOf<String>()
                val emailsHashToBeDeleted = mutableMapOf<String, Email>()

                // Because the email relationship can delete more maisl from the email uri, we need solve it in three
                // tree passes:
                // First get all emails servers (uri) we need to delete
                oldPerson.emails!!.forEach { emailAsignado ->run {
                            if ((person.emails == null) || (!person.emails!!.any { it == emailAsignado }))
                                emailsSetToBeDeleted.add(emailAsignado.email.substringAfter('@'))

                    }
                }
                // 1st pass delete all mail servers (maybe we will delete more than needed
                emailsSetToBeDeleted.forEach {
                        val emails = emailService.getEmail(uri = it)

                        if (emails.isNullOrEmpty()) {
                            logger.error("No se pudo dar de baja los emails (no se encontró el email server) de la persona ${person.nombre} ${person.apellidoPaterno}")
                            return null
                        }
                        val email = emails.first()

                        emailsHashToBeDeleted[it] = email  // store for future passes
                        emailAsignadoService.deleteEmailAsignado(from = oldPerson, to = email)
                }
                // 2nd pass:  maybe we deleted more mails than necessary, so we insert email from the same email uri
                if (person.emails != null && person.emails!!.isNotEmpty()) {
                    person.emails!!.forEach {
                        if (emailsSetToBeDeleted.contains(it.email.substringAfter('@'))) {
                            val newEmail = emailsHashToBeDeleted[it.email.substringAfter('@')]

                            emailAsignadoService.addEmailAsignado(from = person, to = newEmail!!, emailAssigned = it)    // add relationship that was deleted
                        }
                    }
                }
                // 3rd pass: delete the mail server if no has no more
                // links in it
                emailsHashToBeDeleted.values.forEach {
                    emailService.deleteEmailIfNotNeeded(id = it.idNeo4j!!)
                }
            } catch (e: Exception) {
                logger.error("No se pudo dar de baja los emails de la persona ${person.nombre} ${person.apellidoPaterno}: ${e.message}")
                return null
            }
        if (person.emails != null && person.emails!!.isNotEmpty())
            try {
                person.emails!!.forEach { emailAsignado ->
                    if ((oldPerson.emails == null) || (!oldPerson.emails!!.any {it == emailAsignado})) {
                        val newEmail = emailService.addEmailIfNotExists(emailAsignado)

                        if (newEmail != null)
                            emailAsignadoService.addEmailAsignado(from = person, to = newEmail, emailAssigned = emailAsignado)    // add relationship
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                logger.error("No se pudo dar de alta los emails de la persona ${person.nombre} ${person.apellidoPaterno}: ${e.message}")
                return null
            }

        eventService.sendEvent(headers = res.headers, userName = SecurityContextHolder.getContext().authentication!!.name,
                               eventName = "ACTUALIZA_PERSONA", value = res.body!!.data!!.updatePersona)

        return res.body!!.data!!.updatePersona
    }

}
