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
 *  ui.store.ts
 *
 *  Developed 2023 by LMASS Desarrolladores, S.C. www.lmass.com.mx
 */

import { makeAutoObservable, observable } from 'mobx';
import {  login as serverLogin,
    logout as serverLogout,
       } from '@hilla/frontend';
import { sysStore } from './app-store';
import { ConnectionState, ConnectionStateStore } from '@vaadin/common-frontend';
import UserInfo from "Frontend/generated/com/acme/acmeui/security/authserver/UserInfo";
import * as userInfoEndPoint from "Frontend/generated/UserInfoEndpoint";

// User in session information data
interface Authentication {
    user: UserInfo;
}

// The Message class is order to display notifications in the left-button of the screen
class Message {
    constructor(public text = '', public theme = 'contrast', public open = false) {}
}

export class UiStore {
    message = new Message();
    offline = false;
    authentication: Authentication | undefined = undefined;
    company: string | undefined = undefined;

    constructor() {
        makeAutoObservable(
            this,
            {
                connectionStateListener: false,
                connectionStateStore: false,
                setupOfflineListener: false,
                authentication: observable.shallow,
            },
            { autoBind: true }
        );
        this.setupOfflineListener();
    }

    connectionStateStore?: ConnectionStateStore;

    connectionStateListener = () => {
        this.setOffline(
            this.connectionStateStore?.state === ConnectionState.CONNECTION_LOST
        );
    };

    setupOfflineListener() {
        const $wnd = window as any;
        if ($wnd.Vaadin?.connectionState) {
            this.connectionStateStore = $wnd.Vaadin
                .connectionState as ConnectionStateStore;
            this.connectionStateStore.addStateChangeListener(
                this.connectionStateListener
            );
            this.connectionStateListener();
        }
    }

    /**
     * When we lose connection from the server we set the offline flag in order to disable soma functions from the
     * browser (see HTML templates).
     */
    private setOffline(offline: boolean) {
        // Refresh all the cache data from server when going online
        if (this.offline && !offline) {
            sysStore.initFromServer().then(r => this.offline = offline);
        }
    }

    private setCompany(company: string |undefined) {
        this.company = company;
    }

    async oauthLogin() {
        this.authentication = undefined;
        const username = await userInfoEndPoint.checkUser();
        if ('anonymousUser' !== username) {
            const user = await userInfoEndPoint.getUserInfo();  // read User information from server

            this.authentication = {user,};
            this.setCompany(await userInfoEndPoint.getCompany());
        } else {
            this.authentication = {user: { name: username, authorities: [], }};
            this.company = undefined;
        }
    }

    async logout() {
        this.authentication = undefined;
        window.location.href='logout';

        return await serverLogout();         // call Spring Security to close the server session
    }

    setSessionExpired() {
        this.authentication = undefined;
    }

    /**
     * This is to send message on the left-button of the screen
     */
    showSuccess(message: string) {
        this.showMessage(message, 'success');
    }

    showError(message: string) {
        this.showMessage(message, 'error');
    }

    showPrimary(message: string) {
        this.showMessage(message, 'primary');
    }

    showContrast(message: string) {
        this.showMessage(message, 'contrast');
    }

    clearMessage() {
        this.message = new Message();
    }

    private showMessage(text: string, theme: string) {
        this.message = new Message(text, theme, true);
        setTimeout(() => this.clearMessage(), 5000);    // display the message for 5 secs
    }

    /**
     * Check if the user has a role. Use from router.js to display menu options that the user has role
     * @param role: role to be checked
     */
    isUserInRole(role: string) {
        if (!this.authentication) {
            return false;
        }

        // @ts-ignore
        return this.authentication.user.authorities.includes(role);
    }
}
