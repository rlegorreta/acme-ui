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
 *  Sector.kt
 *
 *  Developed 2024 by LegoSoftSoluciones, S.C. www.legosoft.com.mx
 */
package com.acme.acmeui.data.dto

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import jakarta.validation.constraints.Size

/**
 * BUP industry sectors
 *
 * @project acme-ui
 * @author rlh
 * @date February 2024
 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class Rfc(var _id: String? = null,
               @Size(min=13, max=13, message = "El RFC debe de tener 13 caracteres")
               var rfc: String? = null,
               var companias: Collection<Compania>? = null,
               var personas: Collection<Persona>? = null) {

    override fun hashCode(): Int = _id.hashCode()

    var idNeo4j = _id       // this is because the mapping done from the endpoint with '_id' it ignores de underscore
    // and make it wrong
}

data class GraphqlResponseRfcs(val data: Data? = null,
                               val errors: Collection<Map<String, Any>>? = null) {
    data class Data(val rfcs: List<Rfc>)
}

data class GraphqlResponseCreateRfc(val data: Data? = null,
                                    val errors: Collection<Map<String, Any>>? = null) {
    data class Data(val createRfc: Rfc)
}

data class GraphqlResponseUpdateRfc(val data: Data? = null,
                                    val errors: Collection<Map<String, Any>>? = null) {
    data class Data(val updateRfc: Rfc)
}

data class GraphqlResponseDeleteRfc(val data: Data? = null,
                                    val errors: Collection<Map<String, Any>>? = null) {
    data class Data(val deleteRfc: Rfc)
}
