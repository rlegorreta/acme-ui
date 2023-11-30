import { html, render } from 'lit';
import { customElement, state } from 'lit/decorators.js';
import { View } from 'Frontend/views/view';
import { Binder, field } from '@hilla/form';
import CompaniaModel from 'Frontend/generated/com/acme/acmeui/data/dto/CompaniaModel';
import { companyViewStore } from './company-view-store';
import {sysStore, uiStore} from 'Frontend/stores/app-store';
import '@vaadin/horizontal-layout';
import '@vaadin/vertical-layout';
import '@vaadin/button';
import '@vaadin/combo-box';
import '@vaadin/text-field';
import '@vaadin/item';
import '@vaadin/list-box';
import '@vaadin/accordion';
import '@vaadin/dialog';
import { guard } from 'lit/directives/guard.js';
import Area from "Frontend/generated/com/acme/acmeui/data/dto/Area";
import Telefono from "Frontend/generated/com/acme/acmeui/data/dto/Telefono";
import Direccion from "Frontend/generated/com/acme/acmeui/data/dto/Direccion";
import {GridItemModel} from "@vaadin/grid";
import {TextField} from "@vaadin/text-field";
import {ComboBox, ComboBoxRenderer} from "@vaadin/combo-box";
import Estado from "Frontend/generated/com/acme/acmeui/data/dto/Estado";
import TelefonoType from "Frontend/generated/com/acme/acmeui/data/dto/TelefonoType";
import DireccionType from "Frontend/generated/com/acme/acmeui/data/dto/DireccionType";

@customElement('company-form')
export class CompanyForm extends View {
    protected binder = new Binder(this, CompaniaModel);

    constructor() {
        super();
        this.autorun(() =>
            this.binder.read(
                companyViewStore.selectedCompany || CompaniaModel.createEmptyValue()
            )
        );
    }

    @state()
    private telephoneTypes: string[] = Object.values(TelefonoType)
                                             .filter(value => typeof value === 'string') as string[];
    @state()
    private addressTypes: string[] = Object.values(DireccionType)
                                           .filter(value => typeof value === 'string') as string[];
    @state()
    private allStates: Estado[] = [];
    @state()
    private filteredStates: Estado[] = [];
    @state()
    private dialogOpened = false;

    async firstUpdated() {
        this.allStates = this.filteredStates = sysStore.states.map((state) => {
            return {
                ...state,
                displayName: `${state.nombre}`,
            }
        });
    }

    render() {
        const { model } = this.binder;
        this.binder.for(model.nombre).addValidator({
            message: 'La razón social de la compañía debe de tener al menos 4 caracteres',
            validate: (nombre: string) => {
                if (nombre.length > 3) {
                    return true;
                }
                return { property: model.nombre };
            },
        });

        return html`

              <vaadin-text-field
                      label="Razón social"
                      ?disabled=${uiStore.offline}
                      ...=${field(model.nombre)}
              ></vaadin-text-field>
              <vaadin-combo-box
                      label="Sector industrial"
                      auto-open-disabled
                      item-label-path="nombre"
                      item-value-path="idNeo4j"
                      .items=${sysStore.sectors}
                      ...=${field(model.sector)}
              ></vaadin-combo-box>
              <vaadin-accordion>
                  <vaadin-accordion-panel>
                      <div slot="summary"><span class="text-secondary text-s">Areas de la empresa</span></div>
                      <vaadin-vertical-layout>
                          <vaadin-grid style="max-height: 200px"
                                   .items=${companyViewStore.areas}
                          >
                              <vaadin-grid-column path="nombre" header="Area" auto-width></vaadin-grid-column>
                              <vaadin-grid-column
                                      .renderer="${this.trashAreaRenderer}"
                                      auto-width
                              ></vaadin-grid-column>
                          </vaadin-grid>
                          <vaadin-text-field
                                  label="Añadir área"
                                  minlength="4"
                                  ?disabled=${uiStore.offline}
                                  @change="${this.addArea}"
                          ></vaadin-text-field>
                      </vaadin-vertical-layout>
                  </vaadin-accordion-panel>
                  <vaadin-accordion-panel>
                      <div slot="summary"><span class="text-secondary text-s">Teléfonos de la empresa</span></div>
                      <vaadin-vertical-layout>
                          <vaadin-grid style="max-height: 200px"
                                       .items=${companyViewStore.telephones}
                          >
                              <vaadin-grid-column path="numero" header="Número" auto-width></vaadin-grid-column>
                              <vaadin-grid-column path="tipo" header="Tipo" auto-width></vaadin-grid-column>
                              <vaadin-grid-column path="ciudad" header="Ciudad" auto-width></vaadin-grid-column>
                              <vaadin-grid-column
                                      .renderer="${this.trashTelephoneRenderer}"
                                      auto-width
                              ></vaadin-grid-column>
                          </vaadin-grid>
                          <vaadin-horizontal-layout class="gap-x-s">
                              <vaadin-text-field
                                      label="Número"
                                      pattern="^[+]?[(]?[0-9]{3}[)]?[-s.]?[0-9]{3}[-s.]?[0-9]{4,6}$"
                                      ?disabled=${uiStore.offline}
                                      helper-text="Formato: +(123)456-7890"
                                      .value ="${companyViewStore.telephoneNumber}"
                                      @change="${this.setTelephoneNumber}"
                              ></vaadin-text-field>
                              <vaadin-text-field
                                      label="Ciudad"
                                      minlength="4"
                                      ?disabled=${uiStore.offline}
                                      .value ="${companyViewStore.telephoneCity}"
                                      @change="${this.setTelephoneCity}"
                              ></vaadin-text-field>
                              <vaadin-combo-box
                                      label="Tipo"
                                      auto-open-disabled
                                      .value="${companyViewStore.telephoneType}"
                                      @change="${this.setTelephoneType}"
                                      .items="${this.telephoneTypes}"
                              ></vaadin-combo-box>
                              <vaadin-button 
                                      theme="icon primary small" 
                                      title="Añadir el teléfono" 
                                      @click="${this.addTelephone}"
                                      ?disabled=${uiStore.offline || !((companyViewStore.telephoneNumber) && (companyViewStore.telephoneNumber.length > 0) &&
                                                                      (companyViewStore.telephoneCity) && (companyViewStore.telephoneCity.length > 0) &&
                                                                      (companyViewStore.telephoneType) && (companyViewStore.telephoneType.length > 0))}
                              >
                                  <vaadin-icon icon="vaadin:plus" slot="prefix"></vaadin-icon>
                              </vaadin-button>
                          </vaadin-horizontal-layout>
                      </vaadin-vertical-layout>
                  </vaadin-accordion-panel>
                  <vaadin-accordion-panel>
                      <div slot="summary"><span class="text-secondary text-s">Direcciones</span></div>
                      <vaadin-grid style="max-height: 300px"
                                   .items=${companyViewStore.addresses}
                      >
                          <vaadin-grid-column path="calle" header="Calle" auto-width></vaadin-grid-column>
                          <vaadin-grid-column path="tipo" header="Tipo" auto-width></vaadin-grid-column>
                          <vaadin-grid-column
                                  .renderer="${this.trashAddressRenderer}"
                                  auto-width
                          ></vaadin-grid-column>
                      </vaadin-grid>
                      <vaadin-button
                          theme="icon primary small"
                          title="Añadir la dirección"
                          @click="${this.addAddress}"
                          ?disabled=${uiStore.offline || !((companyViewStore.address?.calle) && (companyViewStore.address?.calle.length > 0) &&
                                  (companyViewStore.address?.ciudad) && (companyViewStore.address?.ciudad.length > 0) &&
                                  (companyViewStore.address?.tipo) && (companyViewStore.address?.tipo.length > 0) &&
                                  (companyViewStore.address?.codigo) &&
                                  (companyViewStore.address?.municipio))}
                        >
                        <vaadin-icon icon="vaadin:plus" slot="prefix"></vaadin-icon>
                      </vaadin-button>
                      <vaadin-text-field
                              class="block"
                              label="Calle"
                              ?disabled=${uiStore.offline}
                              .value ="${companyViewStore.address?.calle}"
                              @change="${this.setAddressStreet}"
                      ></vaadin-text-field>
                      <vaadin-vertical-layout class="block">
                          <vaadin-horizontal-layout class="gap-x-s">
                              <vaadin-text-field
                                      label="Ciudad"
                                      minlength="4"
                                      ?disabled=${uiStore.offline}
                                      .value ="${companyViewStore.address?.ciudad}"
                                      @change="${this.setAddressCity}"
                              ></vaadin-text-field>
                              <vaadin-combo-box
                                      label="Tipo"
                                      auto-open-disabled
                                      .value="${companyViewStore.address?.tipo}"
                                      @change="${this.setAddressType}"
                                      .items="${this.addressTypes}"
                              ></vaadin-combo-box>

                          </vaadin-horizontal-layout>
                          <vaadin-horizontal-layout class="gap-x-s">
                              <vaadin-text-field
                                      label="Municipio"
                                      minlength="4"
                                      ?disabled=${uiStore.offline}
                                      .value ="${companyViewStore.address?.municipio?.nombre}"
                                      @change="${this.changeColony}"
                              ></vaadin-text-field>
                              <vaadin-combo-box
                                      label="Código postal"
                                      allow-custom-value
                                      ?disabled=${uiStore.offline || (!companyViewStore.address?.municipio
                                                                     && !companyViewStore.enableZipcodes)}
                                      .value ="${companyViewStore.selectedZipcode}"
                                      @change="${this.changeZipcode}"
                                      .items="${companyViewStore.address?.municipio?.codigos}"
                                      item-label-path="cp"
                                      item-value-path="cp"
                                      pattern="^\\d{5}$"
                                      help-text="Códigos postal existentes. Para nuevos se debe definir el estado."
                              ></vaadin-combo-box>
                          </vaadin-horizontal-layout>
                          <vaadin-horizontal-layout class="gap-x-s">
                              <vaadin-combo-box
                                      label="Estado"
                                      item-label-path="displayName"
                                      auto-open-disabled
                                      .filteredItems=${this.filteredStates}
                                      .renderer="${this.stateRender}"
                                      ?disabled=${uiStore.offline  || !companyViewStore.enableStates}
                                      .selectedItem ="${companyViewStore.selectedState?.nombre}"
                                      @filter-changed="${this.filterChangedState}"
                                      @change="${this.changedState}"
                                      help-text="Código postal nuevos. Se debe definir el estado."
                              ></vaadin-combo-box>
                              <vaadin-text-field label="País" ?disabled=${true}
                                                 .value="${companyViewStore.address?.codigo?.estado?.pais}" 
                              >
                              </vaadin-text-field>
                          </vaadin-horizontal-layout>
                      </vaadin-vertical-layout>
                  </vaadin-accordion-panel> 
              </vaadin-accordion>
              <hr />
              <span class="text-secondary text-m">Subsidiarias</spam>
              <vaadin-list-box style="max-height: 200px">
                  <hr />
                  ${companyViewStore.selectedCompany?.subsidiarias?.map( 
                    (subsidiaria) => 
                        html`<vaadin-item>${subsidiaria?.nombre} </vaadin-item> `
                    )}
                  <hr />
              </vaadin-list-box> 
              <span class="text-secondary text-m">Proveedores</spam>
              <vaadin-list-box style="max-height: 200px">
                  <hr />
                  ${companyViewStore.selectedCompany?.proveedores?.map(
                          (proveedor) =>
                        html`<vaadin-item>${proveedor?.to?.nombre} 
                                <span class="text-2xs font-extralight">(${proveedor?.tipo})</span>
                             </vaadin-item> `
                  )}
                  <hr />
              </vaadin-list-box>
              <div class="buttons spacing-e-s">
                <vaadin-button
                  theme="primary"
                  @click=${this.save}
                  ?disabled=${this.binder.invalid || uiStore.offline}
                >
                  ${this.binder.value.idNeo4j != null ? 'Guardar' : 'Nueva compañía'}
                </vaadin-button>
                <vaadin-button
                  theme="error"
                  @click="${() => (this.dialogOpened = true)}"
                  ?disabled=${!this.binder.value.idNeo4j || uiStore.offline}
                >
                  Borrar
                </vaadin-button>
                <vaadin-button theme="tertiary" @click=${companyViewStore.cancelEdit}>
                  Cancelar
                </vaadin-button>
              </div>
                  
              <vaadin-dialog
                    header-title="${`¿Se desea borrar de la compañía "${this.binder.value.nombre}"?`}"
                    .footerRenderer="${guard([], () => (root: HTMLElement) => {
                            render( html`
                                <vaadin-button
                                    theme="primary error"
                                    @click=${this.delete}
                                    style="margin-right: auto;"
                                >
                                    Borrar
                                </vaadin-button>
                                <vaadin-button theme="tertiary" @click="${() => (this.dialogOpened = false)}"
                                >
                                    Cancelar
                                </vaadin-button>
                            `,
                                root
                            );
                    })}"
                    .opened="${this.dialogOpened}"
                    @opened-changed="${(e: CustomEvent) => (this.dialogOpened = e.detail.value)}"
                    .renderer="${guard([], () => (root: HTMLElement) => {
                            render(html`Normalmente solo se debe de poner el status en 'inactivo' y no borrarla`, root);
                    })}"
                ></vaadin-dialog>
            `;
    }

    async save() {
        const validate = await companyViewStore.validate(this.binder.value);

        if (!validate) {
            await this.binder.submitTo(companyViewStore.save);
            this.binder.clear();
        }
    };

    async delete() {
        this.dialogOpened = false;
        companyViewStore.delete();
    }

    /**
     * Area methods
     */
    private trashAreaRenderer = (root: HTMLElement, _: HTMLElement, model: GridItemModel<Area>) => {
        const nombre = model.item.nombre;

        render(
            html`
                <vaadin-button id=${nombre} theme="icon primary small" title="Eliminar el área" @click="${this.deleteArea}">
                    <vaadin-icon icon="vaadin:trash" slot="prefix"></vaadin-icon>
                </vaadin-button>
            `,
            root
        );
    };

    private deleteArea() {
        companyViewStore.deleteArea(this.id);
    }

    private addArea(event: CustomEvent) {
        // @ts-ignore
        const value = event.target.value;

        if (value.length === 0) return;
        if (companyViewStore.addArea(value)) {
            const textField = event.target as TextField;

            textField.value = "";
        }
    }

    /**
     * Telephone methods
     */
    private trashTelephoneRenderer = (root: HTMLElement, _: HTMLElement, model: GridItemModel<Telefono>) => {
        const numero = model.item.numero;

        render(
            html`
                <vaadin-button id=${numero} theme="icon primary small" title="Eliminar el teléfono" @click="${this.deleteTelephone}">
                    <vaadin-icon icon="vaadin:trash" slot="prefix"></vaadin-icon>
                </vaadin-button>
            `,
            root
        );
    };

    private setTelephoneNumber(event: CustomEvent) {
        const target = event.target as TextField;

        if (target.checkValidity())
            companyViewStore.setTelephoneNumber(target.value);
    }

    private setTelephoneCity(event: CustomEvent) {
        const target = event.target as TextField;

        if (target.checkValidity())
            companyViewStore.setTelephoneCity(target.value);
    }

    private setTelephoneType(event: CustomEvent) {
        companyViewStore.setTelephoneType((event.target as ComboBox).value);
    }

    private deleteTelephone() {
        companyViewStore.deleteTelephone(this.id);
    }

    private addTelephone() {
        companyViewStore.addTelephone();
    }

    /**
     * Address methods
     */
    private trashAddressRenderer = (root: HTMLElement, _: HTMLElement, model: GridItemModel<Direccion>) => {
        const calle = model.item.calle;

        render(
            html`
                <vaadin-button id=${calle} theme="icon primary small" title="Eliminar la dirección" @click="${this.deleteAddress}">
                    <vaadin-icon icon="vaadin:trash" slot="prefix"></vaadin-icon>
                </vaadin-button>
            `,
            root
        );
    };

    private deleteAddress() {
        companyViewStore.deleteAddress(this.id);
    }

    private setAddressStreet(event: CustomEvent) {
        const target = event.target as TextField;

        companyViewStore.setAddressStreet(target.value);
    }

    private setAddressCity(event: CustomEvent) {
        const target = event.target as TextField;

        companyViewStore.setAddressCity(target.value);
    }

    private setAddressType(event: CustomEvent) {
        companyViewStore.setAddressType((event.target as ComboBox).value);
    }

    private addAddress() {
        companyViewStore.addAddress();
    }

    private changeColony(event: CustomEvent) {
        const target = event.target as TextField;

        if (target.checkValidity()) {
            companyViewStore.changeColony(target.value);
        }
    }

    private changeZipcode(event: CustomEvent) {
        const target = event.target as ComboBox;

        if (target.checkValidity()) {
            companyViewStore.changeZipcode(target.value);
        }
    }

    private stateRender: ComboBoxRenderer<Estado> = (root, _, { item: state }) => {
        render(
            html`
        <div style="display: flex;">
          <div>
              ${state.nombre} 
            <div class="text-2xs">
              ${state.pais}
            </div>
          </div>
        </div>
      `,
            root
        );
    };

    private filterChangedState(event: CustomEvent) {
        const filter = event.detail.value as string;

        this.filteredStates = this.allStates.filter(({ nombre, pais}) => {
            return `${nombre} ${pais}`.toLowerCase().includes(filter.toLowerCase());
        });
    }

    private changedState(event: CustomEvent) {
        const target = event.target as ComboBox;

        companyViewStore.changeState(target.selectedItem);
    }

}
