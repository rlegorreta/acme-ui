import { html, render } from 'lit';
import { customElement } from 'lit/decorators.js';
import { View } from 'Frontend/views/view';
import { uiStore } from 'Frontend/stores/app-store';
import '@vaadin/notification';
import '@vaadin/text-field';
import '@vaadin/button';
import '@vaadin/grid';
import '@vaadin/grid/vaadin-grid-column';
import type { GridItemModel } from '@vaadin/grid';
import './sector-form';
import { sectorViewStore } from './sector-view-store';
import Sector from "Frontend/generated/com/acme/acmeui/data/dto/Sector";
import dateFnsFormat from "date-fns/format";
import dateFnsParse from "date-fns/parse";

@customElement('sector-view')
export class SectorView extends View {
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
            if (sectorViewStore.selectedSector) {
                this.classList.add('editing');
            } else {
                this.classList.remove('editing');
            }
        });
    }

    render() {
        return html`
      <div class="toolbar spacing-e-s">
        <vaadin-text-field
          placeholder="Filtrar por sector"
          .value=${sectorViewStore.filterText}
          @input=${this.updateFilter}
          clear-button-visible
        ></vaadin-text-field>
        <vaadin-button
          @click=${sectorViewStore.editNew}
          ?disabled=${uiStore.offline}
          >Añadir un nuevo sector</vaadin-button
        >
      </div>
      <div class="content flex spacing-e-m h-full">
        <vaadin-grid
          class="grid h-full"
          .items=${sectorViewStore.filteredSectors}
          .selectedItems=${[sectorViewStore.selectedSector]}
          @active-item-changed=${this.handleGridSelection}
        >
            <vaadin-grid-column path="nombre" header="Nombre del sector" auto-width></vaadin-grid-column>
            <vaadin-grid-column path="usuarioModificacion" header="Modificó" auto-width></vaadin-grid-column>
            <vaadin-grid-column
                        header="Fecha modificación"
                        .renderer="${this.dayRenderer}"
                        flex-grow="0"
                        auto-width
             ></vaadin-grid-column>
        </vaadin-grid>
        <sector-form
          class="flex flex-col spacing-b-s"
          ?hidden=${!sectorViewStore.selectedSector}
        ></sector-form>
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
            return "formato inválido";
        }
    };

    private dayRenderer = (root: HTMLElement, _: HTMLElement, model: GridItemModel<Sector>) => {
        const sector = model.item;
        const dateFormatted = this.formatDateIso8601(sector.fechaModificacion);

        render(
            html`
                <span>${dateFormatted}</span>
            `,
            root
        );
    };

    updateFilter(e: { target: HTMLInputElement }) {
        sectorViewStore.updateFilter(e.target.value);
    }

    // vaadin-grid fires a null-event when initialized,
    // we are not interested in it.
    first = true;
    handleGridSelection(e: CustomEvent) {
        if (this.first) {
            this.first = false;
            return;
        }
        sectorViewStore.setSelectedSector(e.detail.value);
    }

}
