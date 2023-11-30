import { html } from 'lit';
import { customElement } from 'lit/decorators.js';
import { View } from '../view';
import { uiStore } from 'Frontend/stores/app-store';

@customElement("logged-out")
export class SessionExpiredView extends View {
    connectedCallback() {
        super.connectedCallback();
        this.classList.add('flex', 'h-full', 'w-full');
        uiStore.logout();
    }

    render () {
        return html`
            <html xmlns:th="http://www.thymeleaf.org" lang="en">
            <head>
                <title>Logged out</title>
            </head>
            <body>
                <h2>El usuario ha terminado la sesión.</h2>
                <p>
                    Teclee <a th:href="@{/}">aqui</a> para regresar a la aplicación. Se le van a preguntar nuevamente el usuario y clave de acceso.
                </p>
            </body>
            </html>
        '`
    }

}
