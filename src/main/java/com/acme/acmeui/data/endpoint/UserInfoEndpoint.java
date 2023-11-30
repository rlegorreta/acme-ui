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
 *  UserInfoEndpoint.java
 *
 *  Developed 2023 by LegoSoftSoluciones, S.C. www.legosoft.com.mx
 */
package com.acme.acmeui.data.endpoint;

import com.acme.acmeui.security.authserver.ApplicationAuthServerUserLookupService;
import com.acme.acmeui.security.authserver.UserInfo;
import com.ailegorreta.client.security.authserver.AuthServerUser;
import com.ailegorreta.client.security.service.CurrentSession;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import dev.hilla.Endpoint;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.ReactiveOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

import jakarta.annotation.Nonnull;
import jakarta.annotation.security.PermitAll;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Use info endpoint in order to get from the Client the logged user information
 *
 * TODO: if we want to generate an endpoint developed in kotlin we need to fix from
 *       maven the order the class are compiled. For now keep them in Java.
 *
 * @project acme-ui
 * @author rlh
 * @Date: November 2023
 */
@Endpoint
@PermitAll
public class UserInfoEndpoint {

    private final CurrentSession                securityService;
    private final ApplicationAuthServerUserLookupService applicationAuthServerUserLookupService;
    private final ReactiveOAuth2AuthorizedClientManager clientManager;

    public UserInfoEndpoint(CurrentSession securityService,
                            ApplicationAuthServerUserLookupService   applicationAuthServerUserLookupService,
                            ReactiveOAuth2AuthorizedClientManager clientManager) {
        this.securityService = securityService;
        this.applicationAuthServerUserLookupService = applicationAuthServerUserLookupService;
        this.clientManager = clientManager;
    }

    @Nonnull
    @PermitAll
    public UserInfo getUserInfo() {
        var auth = SecurityContextHolder.getContext().getAuthentication();

        final List<String> authorities = auth.getAuthorities().stream()
                                            .map(GrantedAuthority::getAuthority)
                                            .collect(Collectors.toList());

        return new UserInfo(auth.getName(), authorities);
    }

    @AnonymousAllowed
    public String checkUser() {
        var auth = SecurityContextHolder.getContext().getAuthentication();

        return auth == null ? "anonymousUser" : auth.getName();
    }

    @PermitAll
    public String getCompany() {
        var userInfo = securityService.getAuthenticatedUser();

        if (userInfo.isPresent()) {
            var company = userInfo.get().getCompany();

            return (company.length() > 18) ? company.substring(0,17) + "..." : company;
        }

        return "no definida";
    }

    @PermitAll
    public String isAdministrator() {
        var userInfo = securityService.getAuthenticatedUser();

        if (userInfo.isPresent()) {
            var administrator = userInfo.get().isAdministrator();

            return administrator.toString();
        }

        return "false";
    }

    @PermitAll
    public String isEmployee() {
        var userInfo = securityService.getAuthenticatedUser();

        if (userInfo.isPresent()) {
            var employee = userInfo.get().isEmployee();

            return employee.toString();
        }

        return "true";
    }

    @PermitAll
    public OidcUser findByPrincipalName(String name) {
        var user = applicationAuthServerUserLookupService.findByPrincipalName(name, false)
                .blockOptional()
                .orElse(AuthServerUser.Companion.userNotExist());

        var user_authorization_code = applicationAuthServerUserLookupService.findByPrincipalName(name, true)
                .blockOptional()
                .orElse(AuthServerUser.Companion.userNotExist());

        return user;
    }

}
