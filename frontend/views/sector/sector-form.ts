import { html } from 'lit';
import { customElement, query, state } from 'lit/decorators.js';
import { View } from 'Frontend/views/view';
import { Binder, field } from '@hilla/form';
import SectorModel from 'Frontend/generated/com/acme/acmeui/data/dto/SectorModel';
import { sectorViewStore } from './sector-view-store';
import { uiStore } from 'Frontend/stores/app-store';
import '@vaadin/button';
import '@vaadin/combo-box';
import '@vaadin/text-field';
import '@vaadin/date-picker';

@customElement('sector-form')
export class SectorForm extends View {
    protected binder = new Binder(this, SectorModel);

    constructor() {
        super();
        this.autorun(() =>
            this.binder.read(
                sectorViewStore.selectedSector || SectorModel.createEmptyValue()
            )
        );
    }

    render() {
        const { model } = this.binder;
        this.binder.for(model.nombre).addValidator({
            message: 'El sector debe tener minimo 4 caracteres',
            validate: (nombre: string) => {
                if (nombre.length > 3) {
                    return true;
                }
                return { property: model.nombre };
            },
        });

        return html`

              <vaadin-text-field
                      label="Sector"
                      ?disabled=${uiStore.offline}
                      ...=${field(model.nombre)}
              ></vaadin-text-field>
              <div class="buttons spacing-e-s">
                <vaadin-button
                  theme="primary"
                  @click=${this.save}
                  ?disabled=${this.binder.invalid || uiStore.offline}
                >
                  ${this.binder.value.idNeo4j != null ? 'Guardar' : 'Nuevo sector'}
                </vaadin-button>
                <vaadin-button
                  theme="error"
                  @click=${sectorViewStore.delete}
                  ?disabled=${!this.binder.value.idNeo4j || uiStore.offline}
                >
                  Borrar
                </vaadin-button>
                <vaadin-button theme="tertiary" @click=${sectorViewStore.cancelEdit}>
                  Cancelar
                </vaadin-button>
              </div>
            `;
    }

    async save() {
        const validate = await sectorViewStore.validate(this.binder.value);

        if (!validate) {
            await this.binder.submitTo(sectorViewStore.save);
            this.binder.clear();
        }
    }
}
