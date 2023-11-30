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
 *  OrderService.kt
 *
 *  Developed 2023 by LegoSoftSoluciones, S.C. www.legosoft.com.mx
 */
package com.acme.acmeui.data.service

import com.acme.acmeui.config.ServiceConfig
import com.acme.acmeui.data.dto.GraphqlRequestBody
import com.acme.acmeui.data.dto.GraphqlResponseOrders
import com.acme.acmeui.data.dto.GraphqlResponseOrdersCount
import com.acme.acmeui.data.dto.Order
import com.acme.acmeui.util.GraphqlSchemaReaderUtil
import com.ailegorreta.client.security.utils.HasLogger
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.util.UriComponentsBuilder
import reactor.core.publisher.Mono
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.support.PageableExecutionUtils
import org.springframework.security.oauth2.client.web.reactive.function.client.ServletOAuth2AuthorizedClientExchangeFilterFunction.clientRegistrationId

/**
 * Order to communicate to the Order microservice server repo to get all orders
 * and no mutation is done
 *
 * @project acme-ui
 * @author rlh
 * @date November 2023
 */
@Service
class OrderService(@Qualifier("authorization_code") val webClient: WebClient,
                   private val serviceConfig: ServiceConfig): HasLogger {
    fun uri(): UriComponentsBuilder = UriComponentsBuilder.fromUriString(serviceConfig.getOrderProvider())

    fun allOrders(page: Int, size: Int): Page<Order>? {
        val variables = mutableMapOf("skip" to (page * size), "limit" to size)
        val graphQLRequestBody = GraphqlRequestBody(GraphqlSchemaReaderUtil.getSchemaFromFileName("allOrdersPageable"),
                                                                variables)
        val res = webClient.post()
                            .uri(uri().path("/order/graphql").build().toUri())
                            .accept(MediaType.APPLICATION_JSON)
                            .body(Mono.just(graphQLRequestBody), GraphqlRequestBody::class.java)
                            .attributes(clientRegistrationId(serviceConfig.securityClientId + "-oidc"))
                            .retrieve()
                            .bodyToMono(GraphqlResponseOrders::class.java)
                            .block()

        if ((res == null) || (res.errors != null)) {
            logger.error("Error al leer una página de ordenes:" + res?.errors)
            return null
        }

        return PageableExecutionUtils.getPage(res.data!!.orders, PageRequest.of(page, size)) { 0 }
    }

    fun count() : Long {
        val graphQLRequestBody = GraphqlRequestBody(GraphqlSchemaReaderUtil.getSchemaFromFileName("allOrdersCount"))
        val res = webClient.post()
                            .uri(uri().path("/order/graphql").build().toUri())
                            .accept(MediaType.APPLICATION_JSON)
                            .body(Mono.just(graphQLRequestBody), GraphqlRequestBody::class.java)
                            .attributes(clientRegistrationId(serviceConfig.securityClientId + "-oidc"))
                            .retrieve()
                            .bodyToMono(GraphqlResponseOrdersCount::class.java)
                            .block()

        if ((res == null) || (res.errors != null)) {
            logger.error("Error al leer una el número de registros en ordenes:" + res?.errors)
            return 0L
        }

        return res.data!!.ordersCount.toLong()
    }
}
