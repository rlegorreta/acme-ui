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
 *  Application.java
 *
 *  Developed 2023 by LegoSoftSoluciones, S.C. www.legosoft.com.mx
 */
package com.acme.acmeui;

import com.acme.acmeui.config.ServiceConfig;
import com.acme.acmeui.data.dto.Notification;
import com.acme.acmeui.data.service.MessageService;
import com.acmeui.views.bpm.ResultApprove;
import com.acme.acmeui.views.bpm.RevisionDocumentAdmin;
import com.acme.acmeui.views.bpm.RevisionDocumentLegal;
import com.ailegorreta.client.bpm.views.CustomViews;
import com.vaadin.flow.component.dependency.NpmPackage;
import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.server.PWA;
import com.vaadin.flow.theme.Theme;
import io.camunda.zeebe.client.ZeebeClient;
import io.camunda.zeebe.client.ZeebeClientBuilder;
import io.camunda.zeebe.spring.client.EnableZeebeClient;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.graphql.client.RSocketGraphQlClient;

/**
 * The entry point of the Spring Boot application for all ACME portal.
 *
 * @project ACME-UI
 * @author rlh
 * @date November 2023
 *
 */
@SpringBootApplication(scanBasePackages = {"com.acme.acmeui",
                                            "com.ailegorreta.client.security",
                                            "com.ailegorreta.client.bpm"})
@EnableDiscoveryClient
@Theme(value = "acme-ui")
@PWA(name = "ACME UI", shortName = "ACME",
        iconPath = "images/logos/icon.png",
        offlinePath="offline-page.html",
        offlineResources = { "/images/offline-login-banner.jpg"})
@NpmPackage(value = "line-awesome", version = "1.3.0")
@EnableZeebeClient
public class Application extends SpringBootServletInitializer implements AppShellConfigurator {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

    @Bean
    public PropertySourcesPlaceholderConfigurer propertyConfigurer() {
        PropertySourcesPlaceholderConfigurer propertyConfigurer = new PropertySourcesPlaceholderConfigurer();

        propertyConfigurer.setPlaceholderPrefix("@{");
        propertyConfigurer.setPlaceholderSuffix("}");
        propertyConfigurer.setIgnoreUnresolvablePlaceholders(true);

        return propertyConfigurer;
    }

    @Bean
    public  PropertySourcesPlaceholderConfigurer defaultPropertyConfigurer() {
        return new PropertySourcesPlaceholderConfigurer();
    }

    @Bean
    public CustomViews defaultCustomViews() {
        return viewName -> {
            if (viewName.equals("ResultApprove"))
                return new ResultApprove();         // This could be a spring @Component
            else if (viewName.equals("RevisionDocumentAdmin"))
                return new RevisionDocumentAdmin();
            else if (viewName.equals("RevisionDocumentLegal"))
                return new RevisionDocumentLegal();
            return null;
        };
    }

    @Bean
    public ZeebeClientBuilder defaultZeebeClientBuilder(ServiceConfig serviceConfig) {
        return ZeebeClient.newClientBuilder();
    }

    /**
     * This RSocket is to handle notifications from the AuditServer repo microservice on-line.
     *
     * note: This socket does not the Resource protection in Spring Security. Use it with caution
     *       (i.e., just for notifications)
     * note: This RSocket does not use the Gateway the microservice is called directly
     *
     */
    @Bean
    public RSocketGraphQlClient rSocketGraphQlClient(RSocketGraphQlClient.Builder<?> builder,
                                                     ServiceConfig serviceConfig) {
        return builder.tcp(serviceConfig.getSubscriptionHost(), serviceConfig.getSubscriptionPort())
                      .route("graphql")
                      .build();
    }

    /**
     * In this method we create a GraphQL subscription with the Auth microservice in order to receive on-line
     * notifications.
     *
     * In order to push the notification the user must be subscribed and the destination must me de user or
     * username = null for all users.
     *
     */
    @Bean
    ApplicationRunner applicationRunner(RSocketGraphQlClient rsocket,
                                        MessageService messageService) {
        return args -> {
            var rsocketRequestDocument = """
                    subscription {
                      notification { 
                          username
                          title
                          message
                      }
                    }
                    """;

            rsocket.document(rsocketRequestDocument)
                    .retrieveSubscription("notification")
                    .toEntity(Notification.class)
                    .subscribe(notification -> messageService.sendNotification(notification.title(),
                                                                               notification.message(),
                                                                               notification.username()));

        };
    }

}


