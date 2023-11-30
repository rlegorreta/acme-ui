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
 *  Municipio.kt
 *
 *  Developed 2023 by LegoSoftSoluciones, S.C. www.legosoft.com.mx
 */
package com.acme.acmeui.data.dto

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

/**
 * BUP Municipios for addresses for Companies and Personas
 *
 * @project acme-ui
 * @author rlh
 * @date November 2023
 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class Municipio(var _id: String?,
                     var nombre: String,
                     var codigos: Collection<Codigo>? = null) {
    var idNeo4j = _id

    override fun hashCode(): Int = _id.hashCode()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as Municipio
        return _id == other._id
    }
}

data class GraphqlResponseMunicipios(val data: Data? = null,
                                     val errors: Collection<Map<String, Any>>? = null) {
    data class Data(val municipios: List<Municipio>)
}

data class GraphqlResponseCreateMunicipio(val data: Data? = null,
                                          val errors: Collection<Map<String, Any>>? = null) {
    data class Data(val createMunicipio: Municipio)
}

/** Municipio -> Codigo could be several 1:m  */
data class GraphqlResponseAddMunicipioCodigo(val data: Data? = null,
                                             val errors: Collection<Map<String, Any>>? = null) {
    data class Data(val addMunicipioCodigo: Municipio)
}
