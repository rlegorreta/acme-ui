/* Copyright (c) 2024, LegoSoft Soluciones, S.C.
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
 *  Persona.kt
 *
 *  Developed 2024 by LegoSoftSoluciones, S.C. www.legosoft.com.mx
 */
package com.acme.acmeui.data.dto

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import jakarta.validation.constraints.Size
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * BUP Personas (Personas fisica)
 *
 * @project ACME-UI
 * @author rlh
 * @date February 2024
 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class Persona(var _id: String?,
                   var nombre: String,
                   var apellidoPaterno: String,
                   var apellidoMaterno: String,
                   var fechaNacimiento: LocalDate? = null,
                   var genero: GeneroType? = null,
                   var estadoCivil: EstadoCivilType? = null,
                   var usuarioModificacion: String? = null,
                   var fechaModificacion: LocalDateTime? = null,
                   var activo: Boolean,
                   var idPersona: Int? = null,
                   @Size(min=16, max=16, message = "El CURP debe de tener 16 caracteres")
                   var curp: String? = null,
                   var rfc: Rfc = Rfc(),
                   var trabaja: Collection<Trabaja>? = null,
                   var dirige: Collection<Dirige>? = null,
                   var relaciones:Collection<Relacion>? = null,
                   var telefonos:Collection<Telefono>? = null,
                   var emails:Collection<EmailAsignado>? = null,
                   var direcciones:Collection<Direccion>? = null) {
    override fun hashCode(): Int = _id.hashCode()

    var idNeo4j = _id       // this is because the mapping done from the endpoint with '_id' it ignores de underscore
                            // and make it wrong

    val fullName = "$nombre $apellidoPaterno $apellidoMaterno"
}

enum class GeneroType {
    MASCULINO, FEMENINO, OTRO
}

enum class EstadoCivilType {
    CASADO, SOLTERO, DIVORCIADO, VIUDO, CONCUBINATO
}

data class GraphqlResponsePersonas(val data: Data? = null,
                                   val errors: Collection<Map<String, Any>>? = null) {
    data class Data(val personae: List<Persona>)
}

data class GraphqlResponsePersonasCount(val data: Data? = null,
                                        val errors: Collection<Map<String, Any>>? = null) {
    data class Data(val personasCount: String)
}

data class GraphqlResponseCreatePersona(val data: Data? = null,
                                        val errors: Collection<Map<String, Any>>? = null) {
    data class Data(val createPersona: Persona)
}

data class GraphqlResponseUpdatePersona(val data: Data? = null,
                                        val errors: Collection<Map<String, Any>>? = null) {
    data class Data(val updatePersona: Persona)
}

data class GraphqlResponseDeletePersona(val data: Data? = null,
                                        val errors: Collection<Map<String, Any>>? = null) {
    data class Data(val deleteCompania: Compania)
}

/** Persona -> Trabaja */
data class GraphqlResponseAddPersonaTrabaja(val data: Data? = null,
                                            val errors: Collection<Map<String, Any>>? = null) {
    data class Data(val addPersonaTrabaja: Persona)
}
data class GraphqlResponseDeletePersonaTrabaja(val data: Data? = null,
                                               val errors: Collection<Map<String, Any>>? = null) {
    data class Data(val deletePersonaTrabaja: Persona)
}

/** Persona -> Area */
data class GraphqlResponseAddPersonaArea(val data: Data? = null,
                                         val errors: Collection<Map<String, Any>>? = null) {
    data class Data constructor(val addPersonaArea: Persona)
}
data class GraphqlResponseDeletePersonaArea(val data: Data? = null,
                                            val errors: Collection<Map<String, Any>>? = null) {
    data class Data(val deletePersonaArea: Persona)
}

/** Persona -> Rfc */
data class GraphqlResponseAddPersonaRfc(val data: Data? = null,
                                        val errors: Collection<Map<String, Any>>? = null) {
    data class Data(val addPersonaRfc: Persona)
}
data class GraphqlResponseDeletePersonaRfc(val data: Data? = null,
                                            val errors: Collection<Map<String, Any>>? = null) {
    data class Data(val deletePersonaRfc: Persona)
}


