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
 *  CacheEndpoint.java
 *
 *  Developed 2023 by LegoSoftSoluciones, S.C. www.legosoft.com.mx
 */
package com.acme.acmeui.data.endpoint;

import com.acme.acmeui.service.cache.CacheService;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import dev.hilla.Endpoint;
import dev.hilla.Nonnull;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * This endPoint is read the cache data.
 * For now it reads the system date and the exchange rate MX-DLR
 *
 * In the future more data wil be added as acme-ui will be added
 *
 * @project acme-ui
 * @author rlh
 * @date November 2023
 */
@Endpoint
@AnonymousAllowed
public class CacheEndpoint {
    private final CacheService cacheService;

    public CacheEndpoint(CacheService cacheService) {
        this.cacheService = cacheService;
    }

    @Nonnull
    public BigDecimal getRate(@Nonnull String name) {
        try {
            BigDecimal rate = null;

            return cacheService.getRate(name);

        } catch(Exception e) {
            e.printStackTrace();
            return BigDecimal.ZERO;
        }
    }

    @Nonnull
    public LocalDate getDay(@Nonnull Integer days) {
        return cacheService.getDay(days);
    }
}
