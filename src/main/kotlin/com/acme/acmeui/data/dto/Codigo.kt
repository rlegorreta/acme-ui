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
 *  Codigo.kt
 *
 *  Developed 2023 by LegoSoftSoluciones, S.C. www.legosoft.com.mx
 */
package com.acme.acmeui.data.dto

/**
 * BUP For zip codes for Companies and Persons
 *
 * @project acme-ui
 * @author rlh
 * @date November 2023
 */
data class Codigo(var _id: String?,
                  var cp: Int,
                  var estado: Estado? = null) {
    var idNeo4j = _id

    override fun hashCode(): Int = _id.hashCode()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as Codigo
        return cp == other.cp
    }
}

data class GraphqlResponseGetCodigos(val data: Data? = null,
                                     val errors: Collection<Map<String, Any>>? = null) {
    data class Data(val codigoes: List<Codigo>)
    // ^ this is correct name since is the GraphQL generated schema
}

data class GraphqlResponseCreateCodigo(val data: Data? = null,
                                       val errors: Collection<Map<String, Any>>? = null) {
    data class Data(val createCodigo: Codigo)
}

/** Codigo -> Estado: just add not deletion and just once 1:1 */
data class GraphqlResponseAddCodigoEstado(val data: Data? = null,
                                          val errors: Collection<Map<String, Any>>? = null) {
    data class Data(val addCodigoEstado: Codigo)
}
