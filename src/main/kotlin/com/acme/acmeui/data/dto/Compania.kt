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
 *  Compania.kt
 *
 *  Developed 2024 by LegoSoftSoluciones, S.C. www.legosoft.com.mx
 */
package com.acme.acmeui.data.dto

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import java.time.LocalDateTime

/**
 * BUP Companies (Personas moral)
 *
 * @project acme-ui
 * @author rlh
 * @date February 2024
 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class Compania(var _id: String?,
                    var nombre: String,
                    var usuarioModificacion: String? = null,
                    var fechaModificacion: LocalDateTime? = null,
                    val idPersona: Int = 0,
                    val activo: Boolean = true,
                    val padre: Boolean = true,
                    var sector: Sector?,
                    var rfc: Rfc = Rfc(),
                    var areas:Collection<Area>? = null,
                    var telefonos:Collection<Telefono>? = null,
                    var direcciones:Collection<Direccion>? = null,
                    var subsidiarias:Collection<Compania>? = null,
                    var proveedores:Collection<Proveedor>? = null) {
    override fun hashCode(): Int = _id.hashCode()

    var idNeo4j = _id       // this is because the mapping done from the endpoint with '_id' it ignores de underscore
                            // and make it wrong
}

data class GraphqlResponseCompanias(val data: Data? = null,
                                    val errors: Collection<Map<String, Any>>? = null) {
    data class Data(val companias: List<Compania>)
}

data class GraphqlResponseCompaniasCount(val data: Data? = null,
                                         val errors: Collection<Map<String, Any>>? = null) {
    data class Data(val companiasCount: String)
}

data class GraphqlResponseCreateCompania(val data: Data? = null,
                                         val errors: Collection<Map<String, Any>>? = null) {
    data class Data(val createCompania: Compania)
}

data class GraphqlResponseUpdateCompania(val data: Data? = null,
                                         val errors: Collection<Map<String, Any>>? = null) {
    data class Data(val updateCompania: Compania)
}

data class GraphqlResponseDeleteCompania(val data: Data? = null,
                                         val errors: Collection<Map<String, Any>>? = null) {
    data class Data(val deleteCompania: Compania)
}

/** Compania -> Sector */
data class GraphqlResponseAddCompaniaSector(val data: Data? = null,
                                            val errors: Collection<Map<String, Any>>? = null) {
    data class Data(val addCompaniaSector: Compania)
}
data class GraphqlResponseDeleteCompaniaSector(val data: Data? = null,
                                               val errors: Collection<Map<String, Any>>? = null) {
    data class Data(val deleteCompaniaSector: Compania)
}

/** Compania -> Rfc */
data class GraphqlResponseAddCompaniaRfc(val data: Data? = null,
                                         val errors: Collection<Map<String, Any>>? = null) {
    data class Data(val addCompaniaRfc: Compania)
}
data class GraphqlResponseDeleteCompaniaRfc(val data: Data? = null,
                                            val errors: Collection<Map<String, Any>>? = null) {
    data class Data(val deleteCompaniaRfc: Compania)
}

/** Compania -> Compania (subsidiaria) */
data class GraphqlResponseAddCompaniaSubsidiaria(val data: Data? = null,
                                                 val errors: Collection<Map<String, Any>>? = null) {
    data class Data(val addCompaniaSubsidiaria: Compania)
}
data class GraphqlResponseDeleteCompaniaSubsidiaria(val data: Data? = null,
                                                    val errors: Collection<Map<String, Any>>? = null) {
    data class Data(val deleteCompaniaSubsidiaria: Compania)
}

