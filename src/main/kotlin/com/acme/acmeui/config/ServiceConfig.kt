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
 *  ServiceConfig.kt
 *
 *  Developed 2023 by LegoSoftSoluciones, S.C. www.legosoft.com.mx
 */
package com.acme.acmeui.config

import com.ailegorreta.client.bpm.config.ServiceConfigBpm
import com.ailegorreta.client.security.config.SecurityServiceConfig
import com.ailegorreta.commons.cmis.config.ServiceConfigAlfresco
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.stereotype.Component
import org.springframework.web.servlet.config.annotation.CorsRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

/**
 * Configuration class to read all application properties.
 *
 * @project acme-ui
 * @author rlh
 * @date November 2023
 **/
@Component
@Configuration
open class ServiceConfig: SecurityServiceConfig, ServiceConfigBpm, ServiceConfigAlfresco {

    @Value("\${spring.application.name}")
    val applicationName: String = "Nombre de la aplicación no definido"
    override fun getAppName() = applicationName

    @Value("\${spring.application.version}")
    val appVersion: String = "Versión de la aplicación no definida"

    @Value("\${vapid.public.key}")
    val publicKey: String = "Public key not defined in application properties"

    @Value("\${vapid.private.key}")
    val privateKey: String = "Private key not defines in application properties"

    @Value("\${security.clientId}")
    private val securityClientId: String = "ClientID not defined"
    override fun getSecurityClientId() = securityClientId

    @Value("\${spring.security.oauth2.client.provider.spring.issuer-uri}")
    private val issuerUri: String = "Issuer uri not defined"
    override fun getIssuerUri() = issuerUri

    @Value("\${server.port}")
    private val serverPort: Int = 0
    override fun getServerPort() = serverPort

    @Value("\${microservice.iam.clientId}")
    private val securityIamClientId: String = "Issuer uri not defined"
    override fun getSecurityIAMClientId() =  securityIamClientId

    @Value("\${security.iam.provider-uri}")
    private val securityIamProvider: String = "Issuer uri not defined"
    override fun getSecurityIAMProvider() = securityIamProvider

    override fun useLoadBalanced(): Boolean = false

    @Value("\${microservice.cache.provider-uri}")
    private val cacheProviderUri: String = "Issuer uri not defined"
    fun getCacheProvider() =  cacheProviderUri

    @Value("\${microservice.bup.provider-uri}")
    private val bupProviderUri: String = "Issuer uri not defined"
    fun getBupProvider() =  bupProviderUri

    @Value("\${microservice.order.provider-uri}")
    private val orderProviderUri: String = "Issuer uri not defined"
    fun getOrderProvider() =  orderProviderUri

    @Value("\${microservice.expediente.provider-uri}")
    private val expediemteProviderUri: String = "Issuer uri not defined"
    fun getExpedienteProvider() =  expediemteProviderUri

    @Value("\${microservice.audit.provider-uri}")
    private val auditProviderUri: String = "Issuer uri not defined"
    fun getAuditProvider() =  auditProviderUri

    @Value("\${microservice.audit.subscription.host}")
    private val subscriptionHost: String = "Issuer uri not defined"
    fun getSubscriptionHost() =  subscriptionHost

    @Value("\${microservice.audit.subscription.port}")
    private val subscriptionPort = -1
    fun getSubscriptionPort() = subscriptionPort

    @Value("\${camunda.tasklist.url}")
    private val camundaTaskList: String = "Client rest not defined"

    override fun getCamundaTaskList() = camundaTaskList

    @Value("\${camunda.tasklist.username}")
    private val camundaTaskListUsername: String = "Camunda user name does not exists"
    override fun getCamundaTaskListUsername() = camundaTaskListUsername

    @Value("\${camunda.tasklist.password}")
    private val camundaTaskListPassword: String = "Camunda password"
    override fun getCamundaTaskListPassword() = camundaTaskListPassword

    @Value("\${alfresco.url}")
    private val alfrescoServer: String = "Client rest not defined"
    override fun getAlfrescoServer() = alfrescoServer

    @Value("\${alfresco.username}")
    private val alfrescoUsername: String = "Alfresco username dose not exists"
    override fun getAlfrescoUsername() = alfrescoUsername

    @Value("\${alfresco.password}")
    private val alfrescoPassword: String = "Alfresco password does not exists"
    override fun getAlfrescoPassword() = alfrescoPassword

    @Value("\${alfresco.company}")
    private val alfrescoCompany: String = "Alfresco root directory"
    override fun getAlfrescoCompany() = alfrescoCompany

    override fun getNotificaFacultad() = "NOTIFICA_BUP"
}

/**
 * TODO investigate more if this Bean is needed. See Spring Boot Security CORS
 */
@Configuration
class CORSConfiguration {
    @Bean
    fun corsConfigurer(): WebMvcConfigurer {
        return object : WebMvcConfigurer {
            override fun addCorsMappings(registry: CorsRegistry) {
                registry.addMapping("/**")
            }
        }
    }
}