/* Copyright (c) 2022, LMASSDesarrolladores, S.C.
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
 *  app.store.ts
 *
 *  Developed 2022 by LMASS Desarrolladores, S.C. www.lmass.com.mx
 */
import { SysStore } from './sys-store';
import { UiStore } from './ui-store';
import { makeAutoObservable } from 'mobx';
import { RouterLocation } from '@vaadin/router';
import {routes, ViewRoute} from "../routes";

export class AppStore {
  applicationName = 'ACME';                     // Could be read from the server as an application property
  versionName = "Versión 2.0 Noviembre, 2023";  // Could be read from the server as an application property

  // The location, relative to the base path, e.g. "hello" when viewing "/hello"
  location = '';

  currentViewTitle = '';

  uiStore = new UiStore();
  sysStore = new SysStore();

  constructor() {
    makeAutoObservable(this,
        {
          applicationName: false,
          versionName: false,
          uiStore: false,
          sysStore: false,
        },
        {autoBind: true});
  }

  setLocation(location: RouterLocation) {
    const serverSideRoute = location.route?.path == '(.*)';

    if (location.route && !serverSideRoute) {
      this.location = location.route.path;
    } else if (location.pathname.startsWith(location.baseUrl)) {
      this.location = location.pathname.substr(location.baseUrl.length);
    } else {
      this.location = location.pathname;
    }
    if (serverSideRoute) {
      this.currentViewTitle = document.title; // Title set by server
    } else {
      // this is the case that the view is Vaadin flow view and therefore location does not have the title.
      // search the title in routes
      const mainLayout = routes.find(route => route.component === 'main-layout');

      this.currentViewTitle = (mainLayout?.children?.find(view => view.path === this.location) as ViewRoute)?.title || '';
    }
  }
}

// Warranty a singleton instance for the application stores
export const appStore = new AppStore();
export const uiStore = new UiStore();
export const sysStore = new SysStore();
