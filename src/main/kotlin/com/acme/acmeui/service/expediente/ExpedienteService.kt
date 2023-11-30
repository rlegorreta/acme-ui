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
 *  ExpedienteService.kt
 *
 *  Developed 2023 by LegoSoftSoluciones, S.C. www.legosoft.com.mx
 */
package com.acme.acmeui.service.expediente

import com.acme.acmeui.config.ServiceConfig
import com.ailegorreta.commons.utils.HasLogger
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.http.MediaType
import org.springframework.security.oauth2.client.web.reactive.function.client.ServletOAuth2AuthorizedClientExchangeFilterFunction.clientRegistrationId
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.util.UriComponentsBuilder
import reactor.core.publisher.Mono

/**
 * Service to communicate with the expediente microservice
 *
 * @project acme-ui
 * @author rlh
 * @date November 2023
 */
@Service
class ExpedienteService(@Qualifier("authorization_code") val webClient: WebClient,
                        private val serviceConfig: ServiceConfig) : HasLogger {


    fun uri(): UriComponentsBuilder = UriComponentsBuilder.fromUriString(serviceConfig.getExpedienteProvider())

    fun startProcess(processId: String, variables: Map<String, String>): ProcessInstanceEvent? {
        val requestBody = StartProcessRequestBody(processId, variables)

        return webClient.post()
                        .uri(uri().path("/expediente/startProcess").build().toUri())
                        .accept(MediaType.APPLICATION_JSON)
                        .body(Mono.just(requestBody), StartProcessRequestBody::class.java)
                        .attributes(clientRegistrationId(serviceConfig.securityClientId + "-oidc"))
                        .retrieve()
                        .bodyToMono(ProcessInstanceEvent::class.java)
                        .block()
    }
}

data class StartProcessRequestBody constructor(val processId: String,
                                               val variables: Map<String, String>? = null)

data class ProcessInstanceEvent constructor(val processDefinitionKey: Long,
                                            val bpmnProcessId: String,
                                            val version: Long,
                                            val processInstanceKey: Long) {
    override fun toString() = """
            Nombre : $bpmnProcessId [$processDefinitionKey]
            Versión: $version
                               """.trimIndent()
}
