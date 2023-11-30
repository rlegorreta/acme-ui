import { html, render } from 'lit';
import {customElement, query} from 'lit/decorators.js';
import { View } from 'Frontend/views/view';
import { uiStore } from 'Frontend/stores/app-store';
import '@vaadin/notification';
import '@vaadin/text-field';
import '@vaadin/button';
import '@vaadin/grid';
import '@vaadin/grid/vaadin-grid-column';
import '@vaadin/grid/vaadin-grid-filter-column.js';
import './company-form';
import { companyViewStore } from './company-view-store';
import Compania from "Frontend/generated/com/acme/acmeui/data/dto/Compania";
import {GridDataProviderCallback, GridDataProviderParams, GridItemModel, GridDataProvider, Grid} from "@vaadin/grid";
import * as endpoint from 'Frontend/generated/AcmeEndpoint';
import dateFnsFormat from "date-fns/format";
import dateFnsParse from "date-fns/parse";

@customElement('company-view')
export class CompanyView extends View {
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
            if (companyViewStore.selectedCompany) {
                this.classList.add('editing');
            } else {
                this.classList.remove('editing');
            }
        });
    }

    @query('gridCompanies')
    grid!: Grid;

    render() {
        return html`
      <div class="toolbar spacing-e-s">
        <vaadin-button
          @click=${companyViewStore.editNew}
          ?disabled=${uiStore.offline}
          >Añadir nueva compañía</vaadin-button
        >
      </div>
      <div class="content flex spacing-e-m h-full">
        <vaadin-grid
          id="gridCompanies"
          class="grid h-full"
          .dataProvider=${this.dataProvider}
          .selectedItems=${[companyViewStore.selectedCompany]}
           @active-item-changed=${this.handleGridSelection}
        >
            <vaadin-grid-filter-column path="nombre" header="Razón social" auto-width></vaadin-grid-filter-column>
            <vaadin-grid-column path="sector.nombre" header="Sector" auto-with></vaadin-grid-column>
            <vaadin-grid-column
                    header="Areas"
                    .renderer="${this.areasRenderer}"
                    flex-grow="0"
                    auto-width
            ></vaadin-grid-column>
            <vaadin-grid-column
                    header="Subsidiarias"
                    .renderer="${this.subsidiariasRenderer}"
                    flex-grow="0"
                    auto-width
            ></vaadin-grid-column>
            <vaadin-grid-column
                    header="Proveedores"
                    .renderer="${this.proveedoresRenderer}"
                    flex-grow="0"
                    auto-width
            ></vaadin-grid-column>
            <vaadin-grid-column path="usuarioModificacion" header="Modificó" auto-width></vaadin-grid-column>
            <vaadin-grid-column
                        header="Fecha modificación"
                        .renderer="${this.dayRenderer}"
                        flex-grow="0"
                        auto-width
             ></vaadin-grid-column>
        </vaadin-grid>
        <company-form
          class="flex flex-col spacing-b-s"
          ?hidden=${!companyViewStore.selectedCompany}
        ></company-form>
      </div>
      <vaadin-notification
        theme=${uiStore.message.theme}
        position="bottom-start"
        .opened=${uiStore.message.open}
        .renderer=${(root: HTMLElement) =>
            (root.textContent = uiStore.message.text)}
      ></vaadin-notification>
    `;
    }

    /* Converts from default date format to dd-MM-yyyy format */
    private formatDateIso8601 = (dateStr: string | undefined): string => {
        if (typeof dateStr === 'string') {
            const date = dateFnsParse(dateStr, "yyyy-MM-dd'T'HH:mm:ss.SSSSSS", new Date());

            return dateFnsFormat(date, 'dd-MM-yyyy HH:mm');
        } else {
            return "formato inválido:" + dateStr;
        }
    };

    private dayRenderer = (root: HTMLElement, _: HTMLElement, model: GridItemModel<Compania>) => {
        const company = model.item;
        const dateFormatted = this.formatDateIso8601(company.fechaModificacion);

        render(
            html`
                <span>${dateFormatted}</span>
            `,
            root
        );
    };

    private areasRenderer = (root: HTMLElement, _: HTMLElement, model: GridItemModel<Compania>) => {
        const company = model.item;
        let areas = "";
        let areasSize = "";

        if ((company.areas != undefined) && (company.areas.length > 0)) {
            areasSize = "[" + company.areas.length +"] ";
            // @ts-ignore
            areas = company.areas[0].nombre.substring(0, 6) + "...";
        }
        render(
            html`
                <span class="text-2xs font-extralight">${areasSize}</span><span>${areas}</span>
            `,
            root
        );
    };

    private subsidiariasRenderer = (root: HTMLElement, _: HTMLElement, model: GridItemModel<Compania>) => {
        const company = model.item;
        let subsidiarias = "";
        let subsidiariasSize = "";

        if ((company.subsidiarias != undefined) && (company.subsidiarias.length > 0)) {
            subsidiariasSize = "[" + company.subsidiarias.length + "]";
            // @ts-ignore
            subsidiarias = company.subsidiarias[0].nombre.substring(0, 8) + "...";
        }
        render(
            html`
                <span class="text-2xs font-extralight">${subsidiariasSize}</span><span>${subsidiarias}</span>
            `,
            root
        );
    };

    private proveedoresRenderer = (root: HTMLElement, _: HTMLElement, model: GridItemModel<Compania>) => {
        const company = model.item;
        let proveedores = "";
        let proveedoresSize = "";

        if ((company.proveedores != undefined) && (company.proveedores.length > 0)){
            proveedoresSize = "[" + company.proveedores.length + "]";
            // @ts-ignore
            proveedores = company.proveedores[0].to.nombre.substring(0, 8) + "...";
        }
        render(
            html`
                <span class="text-2xs font-extralight">${proveedoresSize}</span><span>${proveedores}</span>
            `,
            root
        );
    };

    // vaadin-grid fires a null-event when initialized,
    // we are not interested in it.
    first = true;
    handleGridSelection(e: CustomEvent) {
        if (this.first) {
            this.first = false;
            return;
        }
        companyViewStore.setSelectedCompany(e.detail.value);
    }

    async dataProvider(params: GridDataProviderParams<Compania>, callBack: GridDataProviderCallback<Compania>) {
        // @ts-ignore
        const page = await endpoint.companies(params.page, params.pageSize, params.filters);

        // @ts-ignore
        companyViewStore.setContent(page?.content);
        companyViewStore.setCallBack(callBack);
        // @ts-ignore
        callBack(page.content, page.size);
    }

}
