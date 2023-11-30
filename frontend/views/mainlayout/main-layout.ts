/* Copyright (c) 2023, LMASSDesarrolladores, S.C.
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
 *  DataGenerator.java
 *
 *  Developed 2023 by LMASS Desarrolladores, S.C. www.lmass.com.mx
 */
import '@vaadin/app-layout';
import '@vaadin/app-layout/vaadin-drawer-toggle';
import '@vaadin/tabs';
import '@vaadin/vertical-layout';
import '@vaadin/vaadin-lumo-styles/typography';
import '@vaadin/icons';
import '@vaadin/icon';

import { css, html } from 'lit';
import { customElement, state } from 'lit/decorators.js';
import { Layout } from '../view';

import {isAuthorizedViewRoute, menuViews, ViewRoute} from '../../routes';
import { uiStore } from '../../stores/app-store';
import { appStore } from '../../stores/app-store';
import { sysStore } from 'Frontend/stores/app-store';
import { mainLayoutStore} from "./main-layout-store";
import * as server from "Frontend/generated/MessageEndpoint";
import '../../views/chat/chat-view';

/**
 * Main layout for all It's and flow views. Is was taken from Vaadin source examples
 *
 * @project SYS-UI
 * @author rlh
 * @date February 2023
 */
@customElement('main-layout')
export class MainLayout extends Layout {
    static styles = css`
    :host {
      display: block;
      height: 100%;
      width: 100%;
    }
  `;

    @state()
    authorizedViews:ViewRoute[] = [];

    initializedServer = false;

    connectedCallback() {
        super.connectedCallback();
        this.autorun(() => {
            // Because it is the first screen after login we download the cache data
            if (!this.initializedServer) sysStore.initFromServer().then(r => {
                mainLayoutStore.initSubViews();
                this.authorizedViews = menuViews.filter(isAuthorizedViewRoute);
            });
            this.initializedServer = true;
        });
    }

    @state()
    denied = Notification.permission === "denied";
    @state()
    subscribed = false;
    @state()
    isVisibleChat = false;

    render() {
        return html`
      <vaadin-app-layout primary-section="drawer">
        <header class="bg-base border-b border-contrast-10 box-border flex h-xl items-center w-full" slot="navbar">
            <vaadin-drawer-toggle aria-label="Menu toggle" class="text-secondary" theme="contrast"></vaadin-drawer-toggle>
            <h1 class="m-auto text-l">${appStore.currentViewTitle}</h1>
            <vaadin-icon class="m-0" ?hidden=${uiStore.offline} icon="vaadin:cog" title="Preferencias"></vaadin-icon>
            <a href="/notificaciones" class="ms-l" title="Notificaciones pasadas"><vaadin-icon class="h-full" icon="vaadin:bell"></vaadin-icon></a>
            <a href="" class="ms-l"><vaadin-icon class="h-full" icon="vaadin:question-circle"></vaadin-icon></a>
            <a href="/logout" class="ms-l" ?hidden=${uiStore.offline} title="Salida del sistema"><vaadin-icon icon="vaadin:sign-out"></vaadin-icon></a>
        </header>   
        <section class="flex flex-col items-stretch max-h-full min-h-full" slot="drawer" >
            <h2 class="flex items-center h-xl m-0 px-m text-m" title="${appStore.versionName}\n&copy; LegoSoft 2022">${appStore.applicationName}</h2>
            <nav aria-labelledby="views-title" class="border-b border-contrast-10 flex-grow overflow-auto">
                <h3 class="flex items-center h-m mx-m my-0 text-s text-tertiary" id="views-title">Vistas</h3>

                <vaadin-tabs orientation="vertical" style="max-width: 100%;" @selected-changed="${this.subViewChanged}">
                    ${this.authorizedViews.map((view) =>
            html`
                                ${this.getVerticalTab(view)}
                                `
        )}
                </vaadin-tabs>
            </nav>
            <div>
                <vaadin-button theme="icon" aria-label="Add item" title="Mostrar/ocultar el chat"
                                @click=${() => { this.isVisibleChat = !this.isVisibleChat }}>
                    <vaadin-icon class="h-full" icon="vaadin:chat"></vaadin-icon>
                </vaadin-button>
                <chat-view ?hidden=${!this.isVisibleChat}></chat-view>
            </div>
            <footer  class="flex items-center my-s px-m py-xs">
                ${this.denied
            ? html`
                <vaadin-icon class="me-l text-l" icon="vaadin:bell-slash-o" title="Notificaciones bloqueada por el browser"></vaadin-icon>
                        ` : ""}
                ${this.denied
            ? ""
            : this.subscribed
                ? html`
                        <vaadin-button theme="icon primary" title="Notificaciones activa" @click="${this.unsubscribe}">
                            <vaadin-icon icon="vaadin:bell" slot="prefix"></vaadin-icon>
                        </vaadin-button>
                        `
                : html`
                        <vaadin-button theme="icon secondary error" title="Notificación no activa" @click="${this.subscribe}">
                            <vaadin-icon icon="vaadin:bell-slash" slot="prefix"></vaadin-icon>
                        </vaadin-button>
                        `}
                <h6>${uiStore.company}</h6>
            </footer>
        </section>
        <div class="h-full">
            ${this.authorizedViews.map((subView) =>
            html`
                            ${this.getHorizontalTab(subView)}
                        `
        )}
            <slot><!-- application views go here --></slot>
        </div>
      </vaadin-app-layout>
    `;
    }

    private getVerticalTab(view: ViewRoute) {
        if (view.path === 'null') {
            return html`                
                <vaadin-tab>
                    <vaadin-icon class="me-l text-l" icon="vaadin:${view.icon}"></vaadin-icon>
                    <span class="font-medium text-s">${view.title}</span>
                </vaadin-tab>
            `;
        } else {
            return html`
                <vaadin-tab >
                    <a tabindex="-1"
                       ?highlight=${view.path == appStore.location}
                       href=${view.path}>
                        <vaadin-icon class="me-s text-s" icon="vaadin:${view.icon}"></vaadin-icon>
                        <span class="font-small text-s">${view.title}</span>
                    </a>
                </vaadin-tab>
            `;
        }
    }

    private getHorizontalTab(view: ViewRoute) {
        if (view.children) {
            return html`       
               <vaadin-tabs orientation="horizontal" style="max-width: 100%;" theme="small"
                            ?hidden=${!mainLayoutStore.selectedSubViews[this.authorizedViews.indexOf(view)]}>
                    ${view.children.filter(isAuthorizedViewRoute).map((view) =>  html`
                            <vaadin-tab>
                                <a tabindex="-1"
                                   ?highlight=${view.path == appStore.location}
                                   href=${view.path}>
                                    <vaadin-icon class="me-s text-s" icon="vaadin:${view.icon}"></vaadin-icon>
                                    <span class="font-small text-s">${view.title}</span>
                                </a>
                            </vaadin-tab>
                            `
            )}
                </vaadin-tabs>
            `;
        } else {
            return;
        }
    }

    previousSelection = -1;

    subViewChanged(e: CustomEvent) {
        if (this.previousSelection >= 0) {
            mainLayoutStore.setSelectedSubView(this.previousSelection, null);
        }
        mainLayoutStore.setSelectedSubView(e.detail.value, e.detail.value);
        this.previousSelection = e.detail.value;
    }

    // Notification functions
    async firstUpdated() {
        try {
            if ('serviceWorker' in navigator) {
                const registration = await navigator.serviceWorker.getRegistration();
                this.subscribed = !!(await registration?.pushManager.getSubscription());
            } else {
                console.log("El navigator carece de serviceWorker");
                console.log(navigator)
            }
        } catch (e) {
            console.log("Error al tratar de leer el registro de notificaciones " + e);
        }
    }

    async subscribe() {
        const notificationPermission = await Notification.requestPermission();

        if (notificationPermission === "granted") {
            const publicKey = await server.getPublicKey();

            const registration = await navigator.serviceWorker.getRegistration();
            const subscription = await registration?.pushManager.subscribe({
                userVisibleOnly: true,
                applicationServerKey: this.urlB64ToUint8Array((<string>publicKey)),
            });

            if (subscription) {
                this.subscribed = true;
                // Serialize keys uint8array -> base64
                await server.subscribe(JSON.stringify(JSON.parse(JSON.stringify(subscription))),
                                        uiStore.authentication?.user.name);
            }
        } else {
            this.denied = true;
        }
    }

    async unsubscribe() {
        const registration = await navigator.serviceWorker.getRegistration();
        const subscription = await registration?.pushManager.getSubscription();
        if (subscription) {
            await subscription.unsubscribe();
            await server.unsubscribe(subscription.endpoint);
            this.subscribed = false;
        }
    }

    private urlB64ToUint8Array(base64String: string) {
        const padding = "=".repeat((4 - (base64String.length % 4)) % 4);
        const base64 = (base64String + padding)
            .replace(/\-/g, "+")
            .replace(/_/g, "/");
        const rawData = window.atob(base64);
        const outputArray = new Uint8Array(rawData.length);
        for (let i = 0; i < rawData.length; ++i) {
            outputArray[i] = rawData.charCodeAt(i);
        }
        return outputArray;
    }
}

