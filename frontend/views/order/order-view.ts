import { html, render } from 'lit';
import {customElement, query} from 'lit/decorators.js';
import { View } from 'Frontend/views/view';
import '@vaadin/vertical-layout';
import '@vaadin/grid';
import '@vaadin/grid/vaadin-grid-column';
import { orderViewStore } from './order-view-store';
import Order from "Frontend/generated/com/acme/acmeui/data/dto/Order";
import {GridDataProviderCallback, GridDataProviderParams, GridDataProvider, Grid} from "@vaadin/grid";
import * as endpoint from 'Frontend/generated/AcmeEndpoint';

@customElement('order-view')
export class OrderView extends View {
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
            this.classList.remove('editing');
        });
    }

    @query('gridOrders')
    grid!: Grid;

    render() {
        return html`
                <div class="toolbar spacing-e-s"><p>&emsp;Número de ordenes: <b>${orderViewStore.ordersCount}</b></p>
                </div>
                <vaadin-grid
                  id="gridOrders"
                  class="grid h-full"
                  .dataProvider=${this.dataProvider}
                  @active-item-changed=${this.handleGridSelection}
                >
                    <vaadin-grid-column path="idNeo4j" header="Folio orden" auto-width></vaadin-grid-column>
                    <vaadin-grid-column path="fechaOperacion" header="Fecha Operación" auto-width></vaadin-grid-column>
                    <vaadin-grid-column path="tiendaID" header="ID de la tienda" auto-with></vaadin-grid-column>
                    <vaadin-grid-column path="productoID" header= "ID del producto" auto-width></vaadin-grid-column>
                    <vaadin-grid-column path="cantidad" header="Cantidad" auto-width></vaadin-grid-column>
                    <vaadin-grid-column path="monto" header="Monto" auto-width></vaadin-grid-column>
                </vaadin-grid>
    `;
    }

    // vaadin-grid fires a null-event when initialized,
    // we are not interested in it.
    first = true;
    handleGridSelection(e: CustomEvent) {
        if (this.first) {
            this.first = false;
            return;
        }
    }

    async dataProvider(params: GridDataProviderParams<Order>, callBack: GridDataProviderCallback<Order>) {
        const page = await endpoint.orders(params.page, params.pageSize);

        if (params.page == 0)
            orderViewStore.setCount(await endpoint.ordersCount())

        // @ts-ignore
        orderViewStore.setContent(page?.content);
        orderViewStore.setCallBack(callBack);
        // @ts-ignore
        callBack(page.content, page.size);
    }

}
