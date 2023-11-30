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
 *  Direccion.kt
 *
 *  Developed 2023 by LegoSoftSoluciones, S.C. www.legosoft.com.mx
 */
package com.acme.acmeui.data.dto

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

/**
 * BUP Addresses for Companies and Personas
 *
 * @project acme-ui
 * @author rlh
 * @date November 2023
 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class Direccion(var _id: String?,
                     var calle: String,
                     var ciudad: String? = null,
                     var tipo: DireccionType,
                     var municipio: Municipio? = null,
                     var codigo: Codigo? = null) {
                    var idNeo4j = _id

    override fun hashCode(): Int = _id.hashCode()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as Direccion
        return _id == other._id
    }
}

enum class DireccionType {
    OFICIAL, CASA, TEMPORAL, FACTURAR
}

data class GraphqlResponseDirecciones(val data: Data? = null,
                                      val errors: Collection<Map<String, Any>>? = null) {
    data class Data(val direccions: List<Direccion>)
                                // ^ this is correct name since is the GraphQL generated schema
}

data class GraphqlResponseCreateDireccion(val data: Data? = null,
                                          val errors: Collection<Map<String, Any>>? = null) {
    data class Data(val createDireccion: Direccion)
}

data class GraphqlResponseDeleteDireccion(val data: Data? = null,
                                          val errors: Collection<Map<String, Any>>? = null) {
    data class Data(val deleteDireccion: Direccion)
}

/** Direccion -> Codigo 1: 1 */
data class GraphqlResponseAddDireccionCodigo(val data: Data? = null,
                                             val errors: Collection<Map<String, Any>>? = null) {
    data class Data(val addDireccionCodigo: Direccion)
}
data class GraphqlResponseDeleteDireccionCodigo(val data: Data? = null,
                                                val errors: Collection<Map<String, Any>>? = null) {
    data class Data(val deleteDireccionCodigo: Direccion)
}

/** Direccion -> Municipio  1: 1 */
data class GraphqlResponseAddDireccionMunicipio(val data: Data? = null,
                                                val errors: Collection<Map<String, Any>>? = null) {
    data class Data(val addDireccionMunicipio: Direccion)
}
data class GraphqlResponseDeleteDireccionMunicipio(val data: Data? = null,
                                                   val errors: Collection<Map<String, Any>>? = null) {
    data class Data(val deleteDireccionMunicipio: Direccion)
}

/** Direccion -> Compania 1:m */
data class GraphqlResponseAddDireccionCompania(val data: Data? = null,
                                               val errors: Collection<Map<String, Any>>? = null) {
    data class Data(val addDireccionCompania: Direccion)
}
data class GraphqlResponseDeleteDireccionCompania(val data: Data? = null,
                                                  val errors: Collection<Map<String, Any>>? = null) {
    data class Data(val deleteDireccionCompania: Direccion)
}

/** Direccion -> Persona 1:m */
data class GraphqlResponseAddDireccionPersona(val data: Data? = null,
                                              val errors: Collection<Map<String, Any>>? = null) {
    data class Data(val addDireccionPersona: Direccion)
}
data class GraphqlResponseDeleteDireccionPersona(val data: Data? = null,
                                                 val errors: Collection<Map<String, Any>>? = null) {
    data class Data(val deleteDireccionPersona: Direccion)
}
