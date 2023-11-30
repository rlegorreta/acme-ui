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
 *  Telefono.kt
 *
 *  Developed 2023 by LegoSoftSoluciones, S.C. www.legosoft.com.mx
 */
package com.acme.acmeui.data.dto

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

/**
 * BUP Telephones for Companies and Personas
 *
 * @project acme-ui
 * @author rlh
 * @date May 2022
 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class Telefono(var _id: String?,
                    var numero: String,
                    var ciudad: String,
                    var tipo: TelefonoType,
                    var companias: Collection<Compania>? = null) {
    var idNeo4j = _id

    override fun hashCode(): Int = _id.hashCode()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as Telefono
        return numero == other.numero
    }
}

enum class TelefonoType {
    OFICINA, CASA, CELULAR
}

data class GraphqlResponseTelefonos(val data: Data? = null,
                                    val errors: Collection<Map<String, Any>>? = null) {
    data class Data(val telefonoes: List<Telefono>)
                                // ^ this is correct name since is the GraphQL generated schema
}

data class GraphqlResponseCreateTelefono(val data: Data? = null,
                                         val errors: Collection<Map<String, Any>>? = null) {
    data class Data(val createTelefono: Telefono)
}

data class GraphqlResponseDeleteTelefono(val data: Data? = null,
                                         val errors: Collection<Map<String, Any>>? = null) {
    data class Data(val deleteTelefono: Telefono)
}


/** Telefono -> Compania 1:m auto-maintenance */
data class GraphqlResponseAddTelefonoCompania(val data: Data? = null,
                                              val errors: Collection<Map<String, Any>>? = null) {
    data class Data(val addTelefonoCompania: Telefono)
}
data class GraphqlResponseDeleteTelefonoCompania(val data: Data? = null,
                                                 val errors: Collection<Map<String, Any>>? = null) {
    data class Data(val deleteTelefonoCompania: Telefono)
}

/** Telefono -> Persona 1:m auto-maintenance */
data class GraphqlResponseAddTelefonoPersona(val data: Data? = null,
                                             val errors: Collection<Map<String, Any>>? = null) {
    data class Data(val addTelefonoPersona: Telefono)
}
data class GraphqlResponseDeleteTelefonoPersona(val data: Data? = null,
                                                val errors: Collection<Map<String, Any>>? = null) {
    data class Data(val deleteTelefonoPersona: Telefono)
}
