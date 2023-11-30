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
 *  MessageEndpoint.java
 *
 *  Developed 2023 by LegoSoftSoluciones, S.C. www.legosoft.com.mx
 */
package com.acme.acmeui.data.endpoint;

import com.acme.acmeui.data.dto.Notification;
import com.acme.acmeui.data.service.MessageService;
import com.acme.acmeui.service.event.EventService;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import dev.hilla.Endpoint;

import java.util.List;

/**
 * This endpoint is to receive the calls from the client for web-browser notifications.
 * @see //github.com/marcushellberg/fusion-push-notifications
 *
 * @project ACME-UI
 * @auther rlh
 * @date: November 2023
 */
@Endpoint
@AnonymousAllowed
public class MessageEndpoint {
    private final MessageService messageService;
    private final EventService eventService;

    public MessageEndpoint(MessageService messageService,
                           EventService eventService) {
        this.messageService = messageService;
        this.eventService = eventService;
    }

    public String getPublicKey() {
        return messageService.getPublicKey();
    }

    public void subscribe(String subscriptionStr, String username) { messageService.subscribe(subscriptionStr, username); }

    public void unsubscribe(String endpoint) {
        messageService.unsubscribe(endpoint);
    }

    public void sendNotification(String from, String message, String uname) { messageService.sendNotification(from, message, uname);}

    public List<Notification> notifications() { return eventService.notifications(); }

}
