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
 *  ApplicationSecurityConfig.java
 *
 *  Developed 2023 by LegoSoftSoluciones, S.C. www.legosoft.com.mx
 */
package com.acme.acmeui.security.config;

import com.acme.acmeui.config.ServiceConfig;
import com.ailegorreta.client.security.config.SecurityConfig;
import com.ailegorreta.client.security.authserver.AuthServerAuthoritiesMapper;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;

/**
 * Security configuration for valid URLs in the Application. And also reads what are the valid
 * User and Roles.
 *
 * @project ACME UI
 * @author rlh
 * @Date: November 2023
 */
@Configuration(proxyBeanMethods = false)
@EnableWebSecurity        // It seems that Vaadin security does not support reactive WebFlux
// Do NOT put @EnableMethodSecurity
class ApplicationSecurityConfig extends SecurityConfig {

  ApplicationSecurityConfig(ClientRegistrationRepository clientRegistrationRepository,
                            AuthServerAuthoritiesMapper authoritiesMapper,
                            ServiceConfig serviceConfig) {
    super(clientRegistrationRepository, authoritiesMapper,serviceConfig.getSecurityClientId() + "-oidc", serviceConfig);
  }

  @Override
  public void configure(WebSecurity web) throws Exception {
    super.configure(web);
    // Add extra ignoring Matches
    // web.ignoring()
    //    .antMatchers("other");
  }

}
