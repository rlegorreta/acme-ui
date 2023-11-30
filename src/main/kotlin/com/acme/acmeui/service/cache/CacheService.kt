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
 *  CacheService.kt
 *
 *  Developed 2023 by LegoSoftSoluciones, S.C. www.legosoft.com.mx
 */
package com.acme.acmeui.service.cache

import com.acme.acmeui.config.ServiceConfig
import com.acme.acmeui.data.dto.DocumentType
import com.ailegorreta.client.security.utils.HasLogger
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.security.oauth2.client.web.reactive.function.client.ServletOAuth2AuthorizedClientExchangeFilterFunction.clientRegistrationId
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.util.UriComponentsBuilder
import reactor.core.publisher.Mono
import java.math.BigDecimal
import java.time.Duration
import java.time.LocalDate

/**
 * CacheService to communicate to the cache server microservice
 *
 * @project acme-ui
 * @author rlh
 * @date February 2023
 */
@Service
class CacheService(@Qualifier("authorization_code") val webClient: WebClient,
                   private val serviceConfig: ServiceConfig) : HasLogger {

    fun uri(): UriComponentsBuilder = UriComponentsBuilder.fromUriString(serviceConfig.getCacheProvider())

    fun getRate(nombre: String): BigDecimal? {
        val res = webClient.get()
                            .uri(uri().path("/cache/sysvar")
                                .queryParam("nombre", nombre)
                                .build().toUri())
                            .attributes(clientRegistrationId(serviceConfig.securityClientId + "-oidc"))
                            .retrieve()
                            .bodyToMono(BigDecimal::class.java)
                            .block()

        return res
    }

    fun getDay(days: Int): LocalDate? {
        val res = webClient.get()
                            .uri(uri().path("/cache/day")
                                .queryParam("days", days)
                                .build().toUri())
                            .attributes(clientRegistrationId(serviceConfig.securityClientId + "-oidc"))
                            .retrieve()
                            .bodyToMono(LocalDate::class.java)
                            .block()

        return res
    }

    private fun switchIfEmpty() = Mono.just(arrayOfNulls<DocumentType>(0))

    fun allDocumentTypes(): List<DocumentType> {
        val res = webClient.get()
                        .uri(uri().path("/cache/doctypes")
                            .build().toUri())
                        .attributes(clientRegistrationId(serviceConfig.securityClientId + "-oidc"))
                        .retrieve()
                        .bodyToMono(Array<DocumentType>::class.java)
                        .timeout(Duration.ofMillis(10_000))
                        .switchIfEmpty(switchIfEmpty() as Mono<out Nothing>)
                        .map{ elements -> listOf(elements) }
                        as Mono<List<DocumentType>>

        val result = res.block()!!.first() as Array<DocumentType>

        return result.toList()
    }
}
