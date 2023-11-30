import { html, render } from 'lit';
import {customElement, state} from 'lit/decorators.js';
import { View } from '../view';
import Notification from 'Frontend/generated/com/acme/acmeui/data/dto/Notification';
import * as endpoint from 'Frontend/generated/MessageEndpoint';
import '@vaadin/grid';
import '@vaadin/grid/vaadin-grid-column';
import '@vaadin/grid/vaadin-grid-filter-column.js';
import {GridDataProviderCallback, GridDataProviderParams, GridItemModel} from "@vaadin/grid";

@customElement("notification-view")
export class NotificationView extends View {
    connectedCallback() {
        super.connectedCallback();
        this.classList.add(
            'box-border',
            'flex',
            'flex-col',
            'p-m',
            'spacing-b-s',
            'w-full',
            'h-full'
        );

        this.autorun(() => {
        });
    }

    render() {
        return html`
            <h3><b>Notificaciones del día</b></h3>
            <vaadin-grid
                    class="grid h-full" theme="no-border"
                    .dataProvider=${this.dataProvider}
            >
                <vaadin-grid-column path="title" header="Tipo" auto-width></vaadin-grid-column>
                <vaadin-grid-column path="message" header="Notificación" auto-width></vaadin-grid-column>
                <vaadin-grid-column 
                        .renderer="${this.timeRender}"
                        flex-grow="0"
                        auto-width
                ></vaadin-grid-column>
            </vaadin-grid>
    `;
    }

    private timeRender = (root: HTMLElement, _: HTMLElement, model: GridItemModel<Notification>) => {
        const notification = model.item

        if (notification.time != null) {
            const prevTime = new Date(notification.time);
            const thisTime = new Date();
            const diff = Math.trunc((thisTime.getTime() - prevTime.getTime()) / 60000);

            render(
                html`
                <span class="text-2xs font-extralight">${this.prettyMinutes(diff)}</span>
            `,
                root
            );
        }
    }

    private prettyMinutes (minutes: number ) {
        if (minutes < 60) {
            if (minutes == 0) return "En este momento...";
            if (minutes == 1) return "Hace un minuto...";
            return "Acerca de " + minutes + " minutos";
        }
        if (minutes < 1440) {
            const hours = Math.trunc(minutes / 60);

            if (hours == 1) return "Una hora " + Math.trunc(minutes % 60) + " minutos";
            return hours + " horas " + Math.trunc(minutes % 60) + " minutos";
        }
        const days = Math.trunc(minutes / 1440);
        const hours = Math.trunc((minutes - days * 1440) / 60);
        const min = minutes - days * 1440  - hours * 60;

        if (days == 1) return "Un día " + hours + " horas " + min + " minutos";
        return days + " días " + hours + " horas " + min + " minutos";
    }

    async dataProvider(params: GridDataProviderParams<Notification>, callBack: GridDataProviderCallback<Notification>) {
        const notifications = await endpoint.notifications();

        // @ts-ignore
        callBack(notifications, notifications?.length);
    }

}
