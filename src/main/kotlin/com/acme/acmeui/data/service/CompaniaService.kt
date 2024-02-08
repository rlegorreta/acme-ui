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
 *  CompaniaService.kt
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
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.util.UriComponentsBuilder
import reactor.core.publisher.Mono
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.support.PageableExecutionUtils
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.oauth2.client.web.reactive.function.client.ServerOAuth2AuthorizedClientExchangeFilterFunction.clientRegistrationId
import java.time.format.DateTimeFormatter

/**
 * Companies to communicate to the BUP microservice server repo to get all companies
 * and do all its possible mutations
 *
 * @project acme-ui
 * @author rlh
 * @date February 2024
 */
@Service
class CompaniaService(@Qualifier("authorization_code") val webClient: WebClient,
                      private val areaService: AreaService,
                      private val telefonoService: TelefonoService,
                      private val direccionService: DireccionService,
                      private val rfcService: RfcService,
                      private val eventService: EventService,
                      private val serviceConfig: ServiceConfig): HasLogger {
    fun uri(): UriComponentsBuilder = UriComponentsBuilder.fromUriString(serviceConfig.getBupProvider())

    fun allCompanies(nombre: String?, page: Int, size: Int): Page<Compania>? {
        val variables = if (nombre.isNullOrBlank())
                            mutableMapOf("skip" to (page * size), "limit" to size)
                        else
                            mutableMapOf("nombre" to nombre,
                                         "skip" to (page * size), "limit" to size)
        val graphQLRequestBody = GraphqlRequestBody(GraphqlSchemaReaderUtil.getSchemaFromFileName("allCompaniesPageable"),
                                                    variables)
        val res = webClient.post()
                            .uri(uri().path("/bup/graphql").build().toUri())
                            .accept(MediaType.APPLICATION_JSON)
                            .body(Mono.just(graphQLRequestBody), GraphqlRequestBody::class.java)
                            .attributes(clientRegistrationId(serviceConfig.securityClientId + "-oidc"))
                            .retrieve()
                            .bodyToMono(GraphqlResponseCompanias::class.java)
                            .block()

        if ((res == null) || (res.errors != null)) {
            logger.error("Error al leer una página de compañías:" + res?.errors)
            return null
        }

        return PageableExecutionUtils.getPage(res.data!!.companias, PageRequest.of(page, size)) { 0 }
    }

    fun count(nombre: String?) : Long {
        val graphQLRequestBody = if (nombre.isNullOrBlank())
                                    GraphqlRequestBody(GraphqlSchemaReaderUtil.getSchemaFromFileName("allCompaniesCount"))
                                 else
                                    GraphqlRequestBody(GraphqlSchemaReaderUtil.getSchemaFromFileName("allCompaniesCount"),
                                                         mutableMapOf("nombre" to nombre))

        val res = webClient.post()
                            .uri(uri().path("/bup/graphql").build().toUri())
                            .accept(MediaType.APPLICATION_JSON)
                            .body(Mono.just(graphQLRequestBody), GraphqlRequestBody::class.java)
                            .attributes(clientRegistrationId(serviceConfig.securityClientId + "-oidc"))
                            .retrieve()
                            .bodyToMono(GraphqlResponseCompaniasCount::class.java)
                            .block()

        if ((res == null) || (res.errors != null)) {
            logger.error("Error al leer una el número de registros en compañías:" + res?.errors)
            return 0L
        }

        return res.data!!.companiasCount.toLong()
    }

    fun getCompany(id: String? = null, nombre: String? = null): List<Compania>? {
        val variables = if (id == null) mutableMapOf("nombre" to nombre)
                        else if (nombre == null) mutableMapOf("id" to id)
                        else mutableMapOf("id" to id, "nombre" to nombre)
        val graphQLRequestBody = GraphqlRequestBody(GraphqlSchemaReaderUtil.getSchemaFromFileName("getCompany"),
            variables)
        val res = webClient.post()
                        .uri(uri().path("/bup/graphql").build().toUri())
                        .accept(MediaType.APPLICATION_JSON)
                        .body(Mono.just(graphQLRequestBody), GraphqlRequestBody::class.java)
                        .attributes(clientRegistrationId(serviceConfig.securityClientId + "-oidc"))
                        .retrieve()
                        .bodyToMono(GraphqlResponseCompanias::class.java)
                        .block()

        if (res == null || res.errors != null) {
            logger.error("Error al leer la compañía:" + res?.errors)
            return null
        }

        return res.data!!.companias
    }

    fun uniqueValidator(compania: Compania): String? {
        val companies = getCompany(nombre = compania.nombre)

        return if (companies == null) "Error al leer de la base de datos"
               else if (companies.isEmpty()) null
               else if (companies.first().idNeo4j == compania.idNeo4j) null
               else "Ya existe una compañía con la misma razón social"
    }

    fun addCompany(company: Compania): Compania? {
        val graphQLRequestBody = GraphqlRequestBody(GraphqlSchemaReaderUtil.getSchemaFromFileName("addCompany"),
                                    mutableMapOf("nombre" to company.nombre,
                                                 "usuarioModificacion" to company.usuarioModificacion!!,
                                                 "fechaModificacion" to company.fechaModificacion!!.format(DateTimeFormatter.ISO_DATE_TIME),
                                                 "padre" to company.padre,
                                                 "activo" to company.activo,
                                                 "idPersona" to company.idPersona))
        val res = webClient.post()
                            .uri(uri().path("/bup/graphql").build().toUri())
                            .accept(MediaType.APPLICATION_JSON)
                            .body(Mono.just(graphQLRequestBody), GraphqlRequestBody::class.java)
                            .attributes(clientRegistrationId(serviceConfig.securityClientId + "-oidc"))
                            .retrieve()
                            .toEntity(GraphqlResponseCreateCompania::class.java)
                            .block()

        if ((res == null) || (res.body?.errors != null)) {
            logger.error("Error al añadir a la compañía:" + (res?.body?.errors ?: ""))
            eventService.sendEvent(eventType = EventType.ERROR_EVENT,
                                   headers = res?.headers, userName = company.usuarioModificacion!!,
                                   eventName = "ERROR:ALTA_COMPANIA", value = EventGraphqlError(res?.body?.errors, mutableMapOf("company" to company))
            )
            return null
        }
        // Inform Kafka event
        eventService.sendEvent(headers = res.headers, userName = company.usuarioModificacion!!,
                               eventName = "ALTA_COMPANIA", value = res.body?.data!!.createCompania)

        // Now check relationships to be added
        // Sector 1:1
        if (company.sector != null)
            try {
                addCompanySector(res.body!!.data!!.createCompania._id!!, company.sector!!._id!!)
            } catch (e : Exception) {
                logger.error("No se pudo dar de alta el sector para la compañía ${company.nombre}: ${e.message}")
                return null
            }
        // Rfc 1:1
        if (company.rfc.rfc != null)
            try {
                val newRfc = rfcService.addRfcIfNotExists(company.rfc)

                if (newRfc != null)
                    addCompanyRfc(res.body!!.data!!.createCompania._id!!, newRfc.idNeo4j!!)
            } catch (e : Exception) {
                logger.error("No se pudo dar de alta el rfc para la compañía ${company.nombre}: ${e.message}")
                return null
            }
        // Areas 1:m
        if (company.areas != null && company.areas!!.isNotEmpty())
            try {
                company.areas!!.forEach { area ->
                    run {
                        val newArea = areaService.addAreaIfNotExists(area)

                        if (newArea != null)
                            areaService.addAreaCompany(idArea = newArea.idNeo4j!!,
                                                       idCompany = res.body!!.data!!.createCompania.idNeo4j!!)
                    }
                }

            } catch (e: Exception) {
                logger.error("No se pudo dar de alta las áreas de la compañía ${company.nombre}: ${e.message}")
                return null
            }
        // Check telephones
        if (company.telefonos != null && company.telefonos!!.isNotEmpty())
            try {
                company.telefonos!!.forEach { telephone ->
                    run {
                        val newTelephone = telefonoService.addTelephoneIfNotExists(telephone)

                        if (newTelephone != null)
                            telefonoService.addTelephoneCompany(idTelephone =  newTelephone.idNeo4j!!,
                                                                idCompany = res.body!!.data!!.createCompania.idNeo4j!!,)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                logger.error("No se pudo dar de alta los teléfonos de la compañía ${company.nombre}: ${e.message}")
                return null
            }
        // Addresses 1:m
        if (company.direcciones != null && company.direcciones!!.isNotEmpty())
            try {
                company.direcciones!!.forEach { address ->
                    run {
                        val newAddress = direccionService.addAddress(address)

                        if (newAddress != null)
                            direccionService.addAddressCompany(idAddress = newAddress.idNeo4j!!,
                                                               idCompany = res.body!!.data!!.createCompania.idNeo4j!!)
                    }
                }
            } catch (e: Exception) {
                logger.error("No se pudo dar de alta las direcciones de la compañía ${company.nombre}: ${e.message}")
                return null
            }

        return res.body!!.data!!.createCompania
    }

    fun updateCompany(company: Compania): Compania? {
        // now check relationship if you need to be deleted or added
        val companies = getCompany(company._id)

        if (companies == null || companies.size != 1) {
            logger.error("No se pudo leer el registro anterior de la compañía ${company._id}. No se hizo ninguna actualización")
            return null
        }
        val oldCompany = companies.first()

        // update company
        val graphQLRequestBody = GraphqlRequestBody(GraphqlSchemaReaderUtil.getSchemaFromFileName("updateCompany"),
                                                mutableMapOf("id" to company.idNeo4j,
                                                             "nombre" to company.nombre,
                                                             "usuarioModificacion" to company.usuarioModificacion!!,
                                                             "fechaModificacion" to company.fechaModificacion!!.format(DateTimeFormatter.ISO_DATE_TIME),
                                                             "padre" to company.padre,
                                                             "activo" to company.activo,
                                                             "idPersona" to company.idPersona))
        val res = webClient.post()
                            .uri(uri().path("/bup/graphql").build().toUri())
                            .accept(MediaType.APPLICATION_JSON)
                            .body(Mono.just(graphQLRequestBody), GraphqlRequestBody::class.java)
                            .attributes(clientRegistrationId(serviceConfig.securityClientId + "-oidc"))
                            .retrieve()
                            .toEntity(GraphqlResponseUpdateCompania::class.java)
                            .block()

        if ((res == null) || (res.body?.errors != null)) {
            logger.error("Error al actualizar a la compañía:" + (res?.body?.errors ?: ""))
            eventService.sendEvent(eventType = EventType.ERROR_EVENT,
                                   headers = res!!.headers, userName = company.usuarioModificacion!!,
                                   eventName = "ERROR:ACTUALIZA_COMPANIA", value = EventGraphqlError(res.body?.errors, mutableMapOf("company" to company)))
            return null
        }
        // Inform Kafka event
        eventService.sendEvent(headers = res.headers, userName = company.usuarioModificacion!!,
                               eventName = "ACTUALIZA_COMPANIA", value = res.body!!.data!!.updateCompania)

        // Check CompaniaSector relationship
        if (!((oldCompany.sector != null) && (company.sector != null) &&
            (oldCompany.sector!!.idNeo4j == company.sector!!.idNeo4j)))
            try {
                if (oldCompany.sector != null)
                    deleteCompanySector(oldCompany.idNeo4j!!, oldCompany.sector!!._id!!)
                if (company.sector != null)
                    addCompanySector(company.idNeo4j!!, company.sector!!._id!!)
            } catch (e: Exception) {
                logger.error("No se pudo actualizar la relación con el sector. ")
                return null
            }
        // Check CompaniaRfc relationship
        if (!((oldCompany.rfc.rfc != null) && (company.rfc.rfc != null) &&
              (oldCompany.rfc.idNeo4j == company.rfc.idNeo4j)))
            try {
                if (oldCompany.rfc.rfc != null) {
                    deleteCompanyRfc(oldCompany.idNeo4j!!, oldCompany.rfc.idNeo4j!!)
                    rfcService.deleteRfcIfNotNeeded(oldCompany.rfc.idNeo4j!!)
                }
                if (company.rfc.rfc != null) {
                    val newRfc = rfcService.addRfcIfNotExists(company.rfc)

                    if (newRfc != null)
                        addCompanyRfc(company.idNeo4j!!, newRfc.idNeo4j!!)
                }
            } catch (e: Exception) {
                logger.error("No se pudo actualizar la relación con el rfc. ")
                return null
            }
        // Check CompaniaArea relationship
        if (oldCompany.areas != null && oldCompany.areas!!.isNotEmpty())
            try {
                oldCompany.areas!!.forEach { area ->
                        if ((company.areas == null) || (!company.areas!!.any {it == area})) {
                            areaService.deleteAreaCompany(idArea = area.idNeo4j!!, idCompany = oldCompany.idNeo4j!! )
                            areaService.deleteAreaIfNotNeeded(id = area.idNeo4j!!, idCompany = oldCompany.idNeo4j!!)
                        }
                    }
            } catch (e: Exception) {
                logger.error("No se pudo dar de baja las áreas de la compañía ${company.nombre}: ${e.message}")
                return null
            }
        if (company.areas != null && company.areas!!.isNotEmpty())
            try {
                company.areas!!.forEach { area ->
                    if ((oldCompany.areas == null) || (!oldCompany.areas!!.any {it == area})) {
                        val newArea = areaService.addAreaIfNotExists(area)

                        if (newArea != null)
                            areaService.addAreaCompany(idArea =  newArea.idNeo4j!!,
                                                       idCompany = res.body!!.data!!.updateCompania.idNeo4j!!,)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                logger.error("No se pudo dar de alta las áreas de la compañía ${company.nombre}: ${e.message}")
                return null
            }
        // Check CompaniaTelefono relationship
        if (oldCompany.telefonos != null && oldCompany.telefonos!!.isNotEmpty())
            try {
                oldCompany.telefonos!!.forEach { telephone ->
                    if ((company.telefonos == null) || (!company.telefonos!!.any {it == telephone})) {
                        telefonoService.deleteTelephoneCompany(idTelephone = telephone.idNeo4j!!, idCompany = oldCompany.idNeo4j!!)
                        telefonoService.deleteTelephoneIfNotNeeded(id = telephone.idNeo4j!!, idCompany = oldCompany.idNeo4j!!)
                    }
                }
            } catch (e: Exception) {
                logger.error("No se pudo dar de baja los teléfonos de la compañía ${company.nombre}: ${e.message}")
                return null
            }
        if (company.telefonos != null && company.telefonos!!.isNotEmpty())
            try {
                company.telefonos!!.forEach { telephone ->
                    if ((oldCompany.telefonos == null) || (!oldCompany.telefonos!!.any {it == telephone})) {
                        val newTelephone = telefonoService.addTelephoneIfNotExists(telephone)

                        if (newTelephone != null)
                            telefonoService.addTelephoneCompany(idTelephone =  newTelephone.idNeo4j!!,
                                                                idCompany = res.body!!.data!!.updateCompania.idNeo4j!!,)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                logger.error("No se pudo dar de alta los teléfonos de la compañía ${company.nombre}: ${e.message}")
                return null
            }
        // Check CompaniaDireccion relationship
        if (oldCompany.direcciones != null && oldCompany.direcciones!!.isNotEmpty())
            try {
                oldCompany.direcciones!!.forEach { address ->
                    if ((company.direcciones == null) || (!company.direcciones!!.any {it == address})) {
                        direccionService.deleteAddressCompany(idAddress = address.idNeo4j!!, idCompany = oldCompany.idNeo4j!! )
                        direccionService.deleteAddress(address)
                    }
                }
            } catch (e: Exception) {
                logger.error("No se pudo dar de baja las direcciones de la compañía ${company.nombre}: ${e.message}")
                return null
            }
        if (company.direcciones != null && company.direcciones!!.isNotEmpty())
            try {
                company.direcciones!!.forEach { address ->
                    if ((oldCompany.direcciones == null) || (!oldCompany.direcciones!!.any {it == address})) {
                        val newAddress = direccionService.addAddress(address)

                        if (newAddress != null)
                            direccionService.addAddressCompany(idAddress =  newAddress.idNeo4j!!,
                                                              idCompany = res.body!!.data!!.updateCompania.idNeo4j!!,)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                logger.error("No se pudo dar de alta las direcciones de la compañía ${company.nombre}: ${e.message}")
                return null
            }

        return res.body!!.data!!.updateCompania
    }

    /**
     * Company sector relationship maintenance
     */
    private fun addCompanySector(idCompany: String, idSector: String): Compania {
        val graphQLRequestBody = GraphqlRequestBody(GraphqlSchemaReaderUtil.getSchemaFromFileName("addCompanySector"),
            mutableMapOf("id" to idCompany,
                         "sector" to idSector))
        val res = webClient.post()
                            .uri(uri().path("/bup/graphql").build().toUri())
                            .accept(MediaType.APPLICATION_JSON)
                            .body(Mono.just(graphQLRequestBody), GraphqlRequestBody::class.java)
                            .attributes(clientRegistrationId(serviceConfig.securityClientId + "-oidc"))
                            .retrieve()
                            .toEntity(GraphqlResponseAddCompaniaSector::class.java)
                            .block()

        if ((res == null) || (res.body!!.errors != null)) {
            logger.error("Error al añadir la relación compañía sector:" + (res?.body?.errors ?: ""))
            val graphQLError = EventGraphqlError(res?.body?.errors, mutableMapOf("idCompany" to idCompany))

            graphQLError.addExtraData("idSector", idSector)
            eventService.sendEvent(eventType = EventType.ERROR_EVENT,
                                   headers = res!!.headers, userName = SecurityContextHolder.getContext().authentication!!.name,
                                   eventName = "ERROR:ASIGNACION_SECTOR_COMPAÑIA", value = graphQLError)
            throw Exception("Error al añadir la relación compañía sector")
        }
        eventService.sendEvent(headers = res.headers, userName = SecurityContextHolder.getContext().authentication!!.name,
                               eventName = "ASIGNACION_SECTOR_COMPAÑIA", value = res.body!!.data!!.addCompaniaSector)

        return res.body!!.data!!.addCompaniaSector
    }

    private fun deleteCompanySector(idCompany: String, idSector: String): Compania {
        val graphQLRequestBody = GraphqlRequestBody(GraphqlSchemaReaderUtil.getSchemaFromFileName("deleteCompanySector"),
            mutableMapOf("id" to idCompany,
                         "sector" to idSector))
        val res = webClient.post()
                            .uri(uri().path("/bup/graphql").build().toUri())
                            .accept(MediaType.APPLICATION_JSON)
                            .body(Mono.just(graphQLRequestBody), GraphqlRequestBody::class.java)
                            .attributes(clientRegistrationId(serviceConfig.securityClientId + "-oidc"))
                            .retrieve()
                            .toEntity(GraphqlResponseDeleteCompaniaSector::class.java)
                            .block()

        if ((res == null) || (res.body!!.errors != null)) {
            logger.error("Error borrar la relación sector compañía:" + (res?.body?.errors ?: ""))
            val graphQLError = EventGraphqlError(res?.body?.errors, mutableMapOf("idCompny" to idCompany))

            graphQLError.addExtraData("idSector", idSector)
            eventService.sendEvent(eventType = EventType.ERROR_EVENT,
                                    headers = res!!.headers, userName = SecurityContextHolder.getContext().authentication!!.name,
                                    eventName = "ERROR:DES_ASIGNACION_SECTOR_COMPAÑIA", value = graphQLError)
            throw Exception("Error al borrar la relación compañía sector")
        }
        eventService.sendEvent(headers = res.headers, userName = SecurityContextHolder.getContext().authentication!!.name,
                               eventName = "DES_ASIGNACION_SECTOR_COMPAÑIA", value = res.body!!.data!!.deleteCompaniaSector)

        return res.body!!.data!!.deleteCompaniaSector
    }

    /**
     * Company rfc relationship maintenance
     */
    private fun addCompanyRfc(idCompany: String, idRfc: String): Compania {
        val graphQLRequestBody = GraphqlRequestBody(GraphqlSchemaReaderUtil.getSchemaFromFileName("addCompanyRfc"),
                                                mutableMapOf("id" to idCompany,
                                                             "rfc" to idRfc))
        val res = webClient.post()
                            .uri(uri().path("/bup/graphql").build().toUri())
                            .accept(MediaType.APPLICATION_JSON)
                            .body(Mono.just(graphQLRequestBody), GraphqlRequestBody::class.java)
                            .attributes(clientRegistrationId(serviceConfig.securityClientId + "-oidc"))
                            .retrieve()
                            .toEntity(GraphqlResponseAddCompaniaRfc::class.java)
                            .block()

        if ((res == null) || (res.body!!.errors != null)) {
            logger.error("Error al añadir la relación compañía rfc:" + (res?.body?.errors ?: ""))
            val graphQLError = EventGraphqlError(res?.body?.errors, mutableMapOf("idCompany" to idCompany))

            graphQLError.addExtraData("idRfc", idRfc)
            eventService.sendEvent(eventType = EventType.ERROR_EVENT,
                                    headers = res!!.headers, userName = SecurityContextHolder.getContext().authentication!!.name,
                                    eventName = "ERROR:ASIGNACION_RFC_COMPAÑIA", value = graphQLError)
            throw Exception("Error al añadir la relación compañía rfc")
        }
        eventService.sendEvent(headers = res.headers, userName = SecurityContextHolder.getContext().authentication!!.name,
            eventName = "ASIGNACION_RFC_COMPAÑIA", value = res.body!!.data!!.addCompaniaRfc)

        return res.body!!.data!!.addCompaniaRfc
    }

    private fun deleteCompanyRfc(idCompany: String, idRfc: String): Compania {
        val graphQLRequestBody = GraphqlRequestBody(GraphqlSchemaReaderUtil.getSchemaFromFileName("deleteCompanyRfc"),
                                                            mutableMapOf("id" to idCompany,
                                                                         "rfc" to idRfc))
        val res = webClient.post()
                            .uri(uri().path("/bup/graphql").build().toUri())
                            .accept(MediaType.APPLICATION_JSON)
                            .body(Mono.just(graphQLRequestBody), GraphqlRequestBody::class.java)
                            .attributes(clientRegistrationId(serviceConfig.securityClientId + "-oidc"))
                            .retrieve()
                            .toEntity(GraphqlResponseDeleteCompaniaRfc::class.java)
                            .block()

        if ((res == null) || (res.body!!.errors != null)) {
            logger.error("Error borrar la relación rfc compañía:" + (res?.body?.errors ?: ""))
            val graphQLError = EventGraphqlError(res?.body?.errors, mutableMapOf("idCompany" to idCompany))

            graphQLError.addExtraData("idRfc", idRfc)
            eventService.sendEvent(eventType = EventType.ERROR_EVENT,
                headers = res!!.headers, userName = SecurityContextHolder.getContext().authentication!!.name,
                eventName = "ERROR:DES_ASIGNACION_RFC_COMPAÑIA", value = graphQLError)
            throw Exception("Error al borrar la relación compañía rfc")
        }
        eventService.sendEvent(headers = res.headers, userName = SecurityContextHolder.getContext().authentication!!.name,
                               eventName = "DES_ASIGNACION_RFC_COMPAÑIA", value = res.body!!.data!!.deleteCompaniaRfc)

        return res.body!!.data!!.deleteCompaniaRfc
    }
    
    /**
     * Company vs company subsidiary relationships
     */
    fun addCompanySubsidiary(idCompany: String, idSubsidiary: String): Compania {
        val graphQLRequestBody = GraphqlRequestBody(GraphqlSchemaReaderUtil.getSchemaFromFileName("addCompanySubsidiary"),
                                    mutableMapOf("id" to idCompany,
                                                 "subsidiaria" to idSubsidiary))
        val res = webClient.post()
                            .uri(uri().path("/bup/graphql").build().toUri())
                            .accept(MediaType.APPLICATION_JSON)
                            .body(Mono.just(graphQLRequestBody), GraphqlRequestBody::class.java)
                            .attributes(clientRegistrationId(serviceConfig.securityClientId + "-oidc"))
                            .retrieve()
                            .toEntity(GraphqlResponseAddCompaniaSubsidiaria::class.java)
                            .block()

        if (res == null || res.body?.errors != null) {
            logger.error("Error al añadir la subsidiaria a la compañía :" + (res?.body?.errors ?: ""))
            val graphQLError = EventGraphqlError(res?.body?.errors, mutableMapOf("idCompany" to idCompany))

            graphQLError.addExtraData("idSubsidiary", idSubsidiary)
            eventService.sendEvent(eventType = EventType.ERROR_EVENT,
                                   headers = res!!.headers, userName = SecurityContextHolder.getContext().authentication!!.name,
                                   eventName = "ERROR:ASIGNACION_SUBSIDIARIA_COMPAÑIA", value = graphQLError)
            throw Exception("Error al añadir la subsidiaria a la compañía")
        }
        eventService.sendEvent(headers = res.headers, userName = SecurityContextHolder.getContext().authentication!!.name,
                               eventName = "ASIGNACION_SUBSIDIARIA_COMPAÑIA", value = res.body!!.data!!.addCompaniaSubsidiaria)

        return res.body!!.data!!.addCompaniaSubsidiaria
    }

    fun deleteCompanySubsidiaria(idCompany: String, idSubsidiary: String): Compania {
        val graphQLRequestBody = GraphqlRequestBody(GraphqlSchemaReaderUtil.getSchemaFromFileName("deleteCompanySubsidiary"),
                                            mutableMapOf("id" to idCompany,
                                                         "subsidiaria" to idSubsidiary))
        val res = webClient.post()
                            .uri(uri().path("/bup/graphql").build().toUri())
                            .accept(MediaType.APPLICATION_JSON)
                            .body(Mono.just(graphQLRequestBody), GraphqlRequestBody::class.java)
                            .attributes(clientRegistrationId(serviceConfig.securityClientId + "-oidc"))
                            .retrieve()
                            .toEntity(GraphqlResponseDeleteCompaniaSubsidiaria::class.java)
                            .block()

        if ((res == null) || (res.body!!.errors != null)) {
            logger.error("Error borrar la subsidiaria de la compañía:" + (res!!.body?.errors ?: ""))
            eventService.sendEvent(eventType = EventType.ERROR_EVENT,
                                   headers = res.headers, userName = SecurityContextHolder.getContext().authentication!!.name,
                                   eventName = "ERROR:DES_ASIGNACION_SUBSIDIARIA_COMPAÑIA", value = idCompany)
            throw Exception("Error al borrar la subsidiaria de la compañía")
        }
        eventService.sendEvent(headers = res.headers, userName = SecurityContextHolder.getContext().authentication!!.name,
                               eventName = "ERROR:ASIGNACION_SUBSIDIARIA_COMPAÑIA", value = res.body!!.data!!.deleteCompaniaSubsidiaria)

        return res.body!!.data!!.deleteCompaniaSubsidiaria
    }

}
