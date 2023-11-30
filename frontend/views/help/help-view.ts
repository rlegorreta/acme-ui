import { html } from 'lit';
import { customElement } from 'lit/decorators.js';
import { View } from '../view';
import { sysStore } from 'Frontend/stores/app-store';
import '@vaadin/text-field';
import '@vaadin/button';
import '@vaadin/notification';
import '@vaadin/icon';

@customElement("help-view")
export class HelpView extends View {

    render() {
        return html`
            <h1><b>Sistema ACME</b></h1>
            <p>
                Las funciones comprendidas del sistema ACME son las siguientes:
            </p>
            <p>
                <ul>
                <li>
                    Catálogo de sectores empresariales.
                </li>
                <li>
                    A/B/C de Personas Físicas.
                </li>
                <li>
                    A/B/C de Empresas (i.e., Personas Morales).
                </li>
                <li>
                    Relacionar Empresas y Personas Físicas.
                </li>
                <li>
                    <i>Otras funciones por añadir.</i>
                </li>
                </ul>
            </p>
            <br>
            <p>&emsp;La fecha del sistema es: <b>${sysStore.sysDate}</b></p>
            <p>&emsp;El tipo de cambio del día es: <b>${sysStore.exchangeRate}</b> por dólar</p>

    `;
    }

}
