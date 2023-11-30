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
 *  OrdersDataProvider.kt
 *
 *  Developed 2023 by LegoSoftSoluciones, S.C. www.legosoft.com.mx
 */
package com.acme.acmeui.views.dataproviders

import com.acme.acmeui.data.dto.Order
import com.acme.acmeui.data.service.OrderService
import org.springframework.data.domain.Page
import org.springframework.stereotype.Component

/**
 * DataProvider for Orders in class. It just coordinates tto call count() just the first time
 *
 * @project: acme-ui
 * @author: rlh
 * @date: November 2023
 */
@Component
class OrdersDataProvider(private val service: OrderService) {

    private var totalElements = 0L

    fun allOrders(page: Int, size: Int): Page<Order>? {
        val ordersPage = service.allOrders(page, size)

        if (page == 0 && ordersPage != null)
            totalElements = service.count()

        return ordersPage
    }

    fun getTotalElements() = totalElements

    fun count() = service.count()
}
