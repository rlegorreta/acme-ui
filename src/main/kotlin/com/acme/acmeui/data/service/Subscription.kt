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
 *  Subscription.kt
 *
 *  Developed 2023 by LegoSoftSoluciones, S.C. www.legosoft.com.mx
 */
package com.acme.acmeui.data.service

import com.fasterxml.jackson.annotation.JsonIgnore
import java.util.*

/**
 * This class is used bye MessageService class in order to get a JSON os class nl.martijndwars.webpush.Subscription
 * from the Web Server.
 * The browser Subscription class come with an extra attribute 'expirationTime' that it is ignored.
 *
 * @project acme-ui
 * @author rlh
 * @date November 2023
 *
 */
internal class Subscription : nl.martijndwars.webpush.Subscription {
    @JsonIgnore
    var expirationTime: Date?
    @JsonIgnore
    var username: String?

    constructor() {
        expirationTime = null
        keys = null
        endpoint = null
        username = null
    }

    constructor(expirationTime: Date?, endpoint: String?, keys: Keys?, username: String?) {
        this.expirationTime = expirationTime
        this.keys = keys
        this.endpoint = endpoint
        this.username = username
    }

}
