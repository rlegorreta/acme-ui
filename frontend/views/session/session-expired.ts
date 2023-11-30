import { html } from 'lit';
import { customElement } from 'lit/decorators.js';
import { View } from '../view';
import { Router } from '@vaadin/router';

@customElement("session-expired")
export class SessionExpiredView extends View {
    render () {
        return html`
            <h2>La sesión de trabajo ha expirado.</h2>
            <p>
                Teclee <a th:href="@{/}">aqui</a> para regresar a la aplicación. Si mi sesión (SSO) aun sigue siendo válida,
                se regresara a la aplicación sin tener que registrarse nuevamente. De lo contrario, el panel de acceso al 
                sistema volverá a aparecer y solicitará al usuario ingresar al sistema nuevamente.
            </p>
        '`
    }

    static async show(): Promise<any> {
        Router.go('/session-expired');
    }
}
