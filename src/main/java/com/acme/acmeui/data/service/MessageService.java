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
 *  MessageService.java
 *
 *  Developed 2023 by LegoSoftSoluciones, S.C. www.legosoft.com.mx
 */
package com.acme.acmeui.data.service;

import com.acme.acmeui.config.ServiceConfig;
import com.ailegorreta.commons.utils.HasLogger;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.Security;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.jose4j.lang.JoseException;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import nl.martijndwars.webpush.Notification;
import nl.martijndwars.webpush.PushService;
/**
 * This service is to send message for web-browser notifications.
 * @see //github.com/marcushellberg/fusion-push-notifications for more technical information.
 *
 * @project asme-ui
 * @author rlh
 * @Date: July 2021
 */
@Service
public class MessageService implements HasLogger {
    private final String        publicKey;      // We use the service worker Java Library
    private final String        privateKey;     // The key values are stored in the properties file
    private final ObjectMapper  mapper;
    private PushService         pushService;    // Service Worker library
    private List<Subscription>  subscriptions = new ArrayList<>(); // Unique URL clients subscribers

    public MessageService(ServiceConfig serviceConfig, ObjectMapper mapper) {
        publicKey = serviceConfig.getPublicKey();
        privateKey = serviceConfig.getPrivateKey();
        this.mapper = mapper;
    }

    @PostConstruct
    private void init() throws GeneralSecurityException {
        Security.addProvider(new BouncyCastleProvider());

        pushService = new PushService(publicKey, privateKey);
    }

    public String getPublicKey() {
        return publicKey;
    }

    public String subscribe(String subscriptionStr, String uname) {
        try {
            Subscription subscription = mapper.readValue(subscriptionStr, Subscription.class);
            // ^ Subscription is a subclass of the service worker library, because we need to ignore some JSon fields
            subscription.setUsername(uname);
            getLogger().info("A new browser client was subscribed to notifications: " + subscription.endpoint);
            this.subscriptions.add(subscription);

            return subscription.endpoint;
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }

        return null;
    }

    public void unsubscribe(String endpoint) {
        getLogger().info("The client " + endpoint + " was unsubscribed to web notifications");
        subscriptions = subscriptions.stream().filter(s -> !endpoint.equals(s.endpoint)).collect(Collectors.toList());
    }

    /**
     *   This is actually the method that sends the notification
     */
    public Boolean sendNotification(nl.martijndwars.webpush.Subscription subscription, String messageJson) {
        try {
            pushService.send(new Notification(subscription, messageJson));

            return true;
        } catch (GeneralSecurityException | IOException | JoseException | ExecutionException | InterruptedException e) {
            e.printStackTrace();

            return false;
        }
    }

    public void sendNotification(String title, String message, String uname) {
        System.out.println("Sending manual notification to all subscribers title:" + title +
                " message:" + message);
        var json = """
                {
                "title": "%s",
                "body": "%s"
                }
                """;

        subscriptions.forEach(subscription -> {
            if ((uname.equals("*")) || (Objects.requireNonNull(subscription.getUsername()).equals(uname)))
                sendNotification(subscription, String.format(json, title, message));
        });
    }

    @NotNull
    @Override
    public Logger getLogger() { return HasLogger.DefaultImpls.getLogger(this); }

}
