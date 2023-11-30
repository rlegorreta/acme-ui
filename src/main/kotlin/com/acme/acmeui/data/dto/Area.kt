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
 *  Area.kt
 *
 *  Developed 2023 by LegoSoftSoluciones, S.C. www.legosoft.com.mx
 */
package com.acme.acmeui.data.dto

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

/**
 * BUP Company areas
 *
 * @project acme-ui
 * @author rlh
 * @date November 2023
 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class Area(var _id: String?,
                var nombre: String,
                var companias: Collection<Compania>? = null,) {
    var idNeo4j = _id

    override fun hashCode(): Int = _id.hashCode()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as Area
        return nombre == other.nombre
    }

}

data class GraphqlResponseAreas(val data: Data? = null,
                                val errors: Collection<Map<String, Any>>? = null) {
    data class Data(val areas: List<Area>)
}

data class GraphqlResponseCreateArea(val data: Data? = null,
                                     val errors: Collection<Map<String, Any>>? = null) {
    data class Data(val createArea: Area)
}

data class GraphqlResponseDeleteArea(val data: Data? = null,
                                     val errors: Collection<Map<String, Any>>? = null) {
    data class Data(val deleteArea: Area)
}


/** Area -> Compania */
data class GraphqlResponseAddAreaCompania(val data: Data? = null,
                                          val errors: Collection<Map<String, Any>>? = null) {
    data class Data(val addAreaCompania: Area)
}
data class GraphqlResponseDeleteAreaCompania(val data: Data? = null,
                                             val errors: Collection<Map<String, Any>>? = null) {
    data class Data(val deleteAreaCompania: Area)
}

/** Area -> Persona */
data class GraphqlResponseAddAreaPersona(val data: Data? = null,
                                         val errors: Collection<Map<String, Any>>? = null) {
    data class Data constructor(val addAreaPersona: Area)
}
data class GraphqlResponseDeleteAreaPersona(val data: Data? = null,
                                            val errors: Collection<Map<String, Any>>? = null) {
    data class Data(val deleteAreaPersona: Area)
}
