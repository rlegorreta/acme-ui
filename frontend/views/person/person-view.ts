import { html, render } from 'lit';
import {customElement, query, state} from 'lit/decorators.js';
import { View } from 'Frontend/views/view';
import { uiStore } from 'Frontend/stores/app-store';
import '@vaadin/notification';
import '@vaadin/text-field';
import '@vaadin/button';
import '@vaadin/checkbox';
import '@vaadin/grid';
import '@vaadin/grid/vaadin-grid-column';
import '@vaadin/grid/vaadin-grid-filter-column.js';
import './person-form';
import { personViewStore } from './person-view-store';
import Persona from "Frontend/generated/com/acme/acmeui/data/dto/Persona";
import {GridDataProviderCallback, GridDataProviderParams, GridItemModel, Grid} from "@vaadin/grid";
import * as endpoint from 'Frontend/generated/AcmeEndpoint';
import dateFnsFormat from "date-fns/format";
import dateFnsParse from "date-fns/parse";
import {parseISO} from "date-fns";

@customElement('person-view')
export class PersonView extends View {
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
            if (personViewStore.selectedPerson) {
                this.classList.add('editing');
            } else {
                this.classList.remove('editing');
            }
        });
    }

    @query('gridPersons')
    grid!: Grid;

    render() {
        return html`
      <div class="toolbar spacing-e-s">
          <vaadin-checkbox 
                  label="Solo activos"
                  @change="${this.changeActive}"
          ></vaadin-checkbox>
          <vaadin-button
          @click=${personViewStore.editNew}
          ?disabled=${uiStore.offline}
          >Añadir nueva persona física</vaadin-button
        >
      </div>
      <div class="content flex spacing-e-m h-full">
        <vaadin-grid
          id="gridPersonas"
          class="grid h-full"
          .dataProvider=${this.dataProvider}
          .selectedItems=${[personViewStore.selectedPerson]}
           @active-item-changed=${this.handleGridSelection}
           @change=${this.reloadGrid}
        >
            <vaadin-grid-column path="nombre" header="Nombre" auto-width></vaadin-grid-column>
            <vaadin-grid-filter-column path="apellidoPaterno" header="Apellido Paterno" auto-width></vaadin-grid-filter-column>
            <vaadin-grid-column path="apellidoMaterno" header="Apellido Materno" auto-width></vaadin-grid-column>
            <vaadin-grid-column
                    header="Activo"
                    .renderer="${this.activoRenderer}"
                    flex-grow="0"
                    auto-width
            ></vaadin-grid-column>
            <vaadin-grid-column 
                    header="Labora"
                    .renderer="${this.laboraRenderer}"
                    flex-grow="0"
                    auto-width>
            </vaadin-grid-column>
            <vaadin-grid-column
                    header="Emails"
                    .renderer="${this.emailsRenderer}"
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
        <person-form
          class="flex flex-col spacing-b-s"
          ?hidden=${!personViewStore.selectedPerson}
        ></person-form>
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
            try {
                // const date = dateFnsParse(dateStr, "yyyy-MM-dd'T'HH:mm:ss.SSSSSSSSS", new Date());
                const date = parseISO(dateStr);

                return dateFnsFormat(date, 'dd-MM-yyyy HH:mm');
            } catch (e) {
                return "formato inválido(*):" + dateStr;
            }
        } else {
            return "formato inválido:" + dateStr;
        }
    };

    private dayRenderer = (root: HTMLElement, _: HTMLElement, model: GridItemModel<Persona>) => {
        const company = model.item;
        const dateFormatted = this.formatDateIso8601(company.fechaModificacion);

        render(
            html`
                <span>${dateFormatted}</span>
            `,
            root
        );
    };

    private laboraRenderer = (root: HTMLElement, _: HTMLElement, model: GridItemModel<Persona>) => {
        const person = model.item;
        let empresas = "";
        let empresasSize = "";

        if ((person.trabaja != undefined) && (person.trabaja.length > 0)) {
            if (person.trabaja.length > 1)
                empresasSize = "[" + person.trabaja.length +"] ";
            // @ts-ignore
            empresas = person.trabaja[0]?.to?.nombre.substring(0, 10) + "...";
        }
        render(
            html`
                <span class="text-2xs font-extralight">${empresasSize}</span><span>${empresas}</span>
            `,
            root
        );
    };

    private emailsRenderer = (root: HTMLElement, _: HTMLElement, model: GridItemModel<Persona>) => {
        const person = model.item;
        let emails = "";
        let emailsSize = "";

        if ((person.emails != undefined) && (person.emails.length > 0)) {
            emailsSize = "[" + person.emails.length +"] ";
            // @ts-ignore
            emails = person.emails[0].email.substring(0, 10) + "...";
        }
        render(
            html`
                <span class="text-2xs font-extralight">${emailsSize}</span><span>${emails}</span>
            `,
            root
        );
    };
    private activoRenderer = (root: HTMLElement, _: HTMLElement, model: GridItemModel<Persona>) => {
        const person = model.item;

        render(
            html`
                <span theme="badge ${person.activo ? 'success' : 'error'}">Estatus</span>
            `,
            root
        );
    };

    private changeActive(event: CustomEvent) {
        personViewStore.setJustActivePersons((event.target as HTMLInputElement).checked);

    }

    private reloadGrid() {
        console.log(">>> On Reload", this.grid)
    }

    // vaadin-grid fires a null-event when initialized,
    // we are not interested in it.
    first = true;
    handleGridSelection(e: CustomEvent) {
        if (this.first) {
            this.first = false;
            return;
        }
        personViewStore.setSelectedPerson(e.detail.value);
    }

    async dataProvider(params: GridDataProviderParams<Persona>, callBack: GridDataProviderCallback<Persona>) {
        // @ts-ignore
        const page = await endpoint.persons(params.page, params.pageSize, params.filters, personViewStore.justActivePersons);

        // @ts-ignore
        personViewStore.setContent(page?.content);
        personViewStore.setCallBack(callBack);
        // @ts-ignore
        callBack(personViewStore.content, page.size);
    }

}
