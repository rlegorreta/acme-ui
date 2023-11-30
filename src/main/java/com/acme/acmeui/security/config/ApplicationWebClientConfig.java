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
 *  ApplicationWebClientConfig.java
 *
 *  Developed 2023 by LegoSoftSoluciones, S.C. www.legosoft.com.mx
 */
package com.acme.acmeui.security.config;

import com.ailegorreta.client.rest.RestCallerConfiguration;
import com.ailegorreta.client.rest.config.WebClientConfig;
import com.ailegorreta.client.security.config.SecurityServiceConfig;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.cloud.loadbalancer.annotation.LoadBalancerClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.ReactiveOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizedClientRepository;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Creates two new instances of WebClient.
 *
 * One bean for client_credentials grant type and
 * a second bean for authentication_code.
 *
 * In this application we just used the authentication_code
 *
 * Other bean instantiated the WebClient.builder in order to balance the calls
 *
 * @project: ACME UI
 * @author rlh
 * @date November 2023
 */
@Configuration(proxyBeanMethods = false)
@LoadBalancerClient(name = "restCaller", configuration = RestCallerConfiguration.class)
public class ApplicationWebClientConfig extends WebClientConfig {

    /**
     * The @LoadBalanced is enabled just when this microservice runs inside the docker. This is because it utilizes
     * the eureka server repository and the URLs must be just for with the server name.
     *
     * Comment the @LoadBalanced configuration is if you want to run it in localNoDocker mode.
     *
     * Error 503 service unavailable if we use @LoadBalanced webClient with Spring actuator.
     *
     * See: https://stackoverflow.com/questions/68153309/spring-webclient-load-balance
     *     for more information.
     *
     * If we don´t want to use loadBalanced webClients we need to do the following:
     * - comment @LoadBalancedClient
     * - comment @LoadBalanced
     * - (optional) comment configuration for RestCallerConfiguration and RestCaller class
     */
    @LoadBalanced
    @Bean("webClientBuilderLoadBalanced")
    WebClient.Builder webClientBuilderLoadBalanced() {
        return WebClient.builder();
    }

    @Bean("webClientBuilder")
    WebClient.Builder webClientBuilder() {
        return WebClient.builder();
    }

    @Bean("client_credentials")
    WebClient webClientCC(@Qualifier("webClientBuilder")WebClient.Builder builder,
                          SecurityServiceConfig serviceConfig,
                          OAuth2AuthorizedClientManager clientManager) {
        return super.webClientClientCredentials(builder, serviceConfig, clientManager);
        // ^ for this case we do not do any balanced since we don't  use the credential_code
    }

    // @Bean("client_credentials_load_balanced")
    WebClient webClientCCLoadBalanced(@Qualifier("webClientBuilderLoadBalanced")WebClient.Builder builderLoadBalanced,
                                      @Qualifier("webClientBuilder") WebClient.Builder builder,
                                      SecurityServiceConfig serviceConfig,
                                      OAuth2AuthorizedClientManager clientManager) {
        if (serviceConfig.useLoadBalanced())
            return super.webClientClientCredentials(builderLoadBalanced, serviceConfig, clientManager);
        else
            return super.webClientClientCredentials(builder, serviceConfig, clientManager);
        // ^ for this case we do balanced for use the credential_code
    }

    @Bean("authorization_code")
    WebClient webClient(@Qualifier("webClientBuilder")WebClient.Builder builder,
                        SecurityServiceConfig serviceConfig,
                        OAuth2AuthorizedClientManager authorizedClientManager) {
        return super.webClientAuthenticationCode(builder, serviceConfig, authorizedClientManager);
    }

    @Bean("authorization_code_load_balanced")
    WebClient webClientACLoadBalanced(@Qualifier("webClientBuilderLoadBalanced") WebClient.Builder builderLoaBalanced,
                                      @Qualifier("webClientBuilder") WebClient.Builder builder,
                                      SecurityServiceConfig serviceConfig,
                                      OAuth2AuthorizedClientManager authorizedClientManager) {
        if (serviceConfig.useLoadBalanced())
            return super.webClientAuthenticationCode(builderLoaBalanced, serviceConfig, authorizedClientManager);
        else
            return super.webClientAuthenticationCode(builder, serviceConfig, authorizedClientManager);    }

    @Bean
    ReactiveOAuth2AuthorizedClientManager clientManager(SecurityServiceConfig serviceConfig,
                                                        ClientRegistrationRepository clientRegistrationRepository) {
        return super.getClientManager(serviceConfig, clientRegistrationRepository, "oidc");
    }

    @Bean
    OAuth2AuthorizedClientManager authorizedClientManager( ClientRegistrationRepository clientRegistrationRepository,
                                                           OAuth2AuthorizedClientRepository authorizedClientRepository) {
        return super.getAuthorizedClientManager(clientRegistrationRepository, authorizedClientRepository);
    }
}
