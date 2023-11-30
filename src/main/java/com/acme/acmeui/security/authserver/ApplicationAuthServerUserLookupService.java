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
 *  ApplicationAuthServerUserLookupService.java
 *
 *  Developed 2023 by LegoSoftSoluciones, S.C. www.legosoft.com.mx
 */
package com.acme.acmeui.security.authserver;

import com.ailegorreta.client.security.authserver.AuthServerUserLookupService;
import com.ailegorreta.client.security.config.SecurityServiceConfig;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * This service is to access a resource server with two possible grant types:
 * - client_credentials grant type
 * - authorization_code grant type
 *
 * For this application we mainly used the authorization_code that is declared in the
 * WebClient class
 *
 * @project acme-ui
 * @author rlh
 * @date November 2023
 */
@Service
// @Secured({Roles.AUTH_ROLE})
public class ApplicationAuthServerUserLookupService extends AuthServerUserLookupService {

    public ApplicationAuthServerUserLookupService(SecurityServiceConfig serviceConfig,
                                                  // @Qualifier("client_credentials") WebClient client_credentials,
                                                  @Qualifier("authorization_code") WebClient authorization_code) {
        super(serviceConfig, null, authorization_code);
    }

}
