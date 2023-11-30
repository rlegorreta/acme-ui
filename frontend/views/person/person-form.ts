import { html, render } from 'lit';
import {customElement, query, state} from 'lit/decorators.js';
import { View } from 'Frontend/views/view';
import { Binder, field } from '@hilla/form';
import PersonaModel from 'Frontend/generated/com/acme/acmeui/data/dto/PersonaModel';
import { personViewStore } from './person-view-store';
import {sysStore, uiStore} from 'Frontend/stores/app-store';
import '@vaadin/horizontal-layout';
import '@vaadin/vertical-layout';
import '@vaadin/button';
import '@vaadin/combo-box';
import '@vaadin/text-field';
import '@vaadin/date-picker';
import { DatePicker, DatePickerDate, DatePickerValueChangedEvent } from '@vaadin/date-picker';
import '@vaadin/item';
import '@vaadin/list-box';
import '@vaadin/accordion';
import '@vaadin/checkbox';
import '@vaadin/email-field';
import '@vaadin/dialog';
import { guard } from 'lit/directives/guard.js';
import Telefono from "Frontend/generated/com/acme/acmeui/data/dto/Telefono";
import Direccion from "Frontend/generated/com/acme/acmeui/data/dto/Direccion";
import {TextField} from "@vaadin/text-field";
import {ComboBox, ComboBoxRenderer} from "@vaadin/combo-box";
import Estado from "Frontend/generated/com/acme/acmeui/data/dto/Estado";
import GeneroType from "Frontend/generated/com/acme/acmeui/data/dto/GeneroType";
import dateFnsFormat from 'date-fns/format';
import dateFnsParse from 'date-fns/parse';
import {GridItemModel} from "@vaadin/grid";
import EmailAsignado from 'Frontend/generated/com/acme/acmeui/data/dto/EmailAsignado';
import EstadoCivilType from "Frontend/generated/com/acme/acmeui/data/dto/EstadoCivilType";
import TelefonoType from "Frontend/generated/com/acme/acmeui/data/dto/TelefonoType";
import DireccionType from "Frontend/generated/com/acme/acmeui/data/dto/DireccionType";
import {EmailField} from "@vaadin/email-field";
import {companyViewStore} from "Frontend/views/company/company-view-store";

@customElement('person-form')
export class PersonForm extends View {
    protected binder = new Binder(this, PersonaModel);

    constructor() {
        super();
        this.autorun(() =>
            this.binder.read(
                personViewStore.selectedPerson || PersonaModel.createEmptyValue()
            )
        );
    }

    @state()
    private generoTypes: string[] = Object.values(GeneroType)
                                          .filter(value => typeof value === 'string') as string[];
    @state()
    private estadoCivilTypes: string[] = Object.values(EstadoCivilType)
                                               .filter(value => typeof value === 'string') as string[];
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
    @query('vaadin-date-picker')
    private datePicker?: DatePicker;
    @state()
    private selectedDateValue: string = dateFnsFormat(new Date(), 'dd-MM-yyyy');
    @state()
    private dialogOpened = false;

    async firstUpdated() {
        this.allStates = this.filteredStates = sysStore.states.map((state) => {
            return {
                ...state,
                displayName: `${state.nombre}`,
            }
        });
        const formatDateIso8601 = (dateParts: DatePickerDate): string => {
            const { year, month, day } = dateParts;
            const date = new Date(year, month, day);

            return dateFnsFormat(date, 'dd-MM-yyyy');
        };

        const parseDateIso8601 = (inputValue: string): DatePickerDate => {
            const date = dateFnsParse(inputValue, 'dd-MM-yyyy', new Date());

            return { year: date.getFullYear(), month: date.getMonth(), day: date.getDate() };
        };

        if (this.datePicker) {
            this.datePicker.i18n = {
                ...this.datePicker.i18n,
                formatDate: formatDateIso8601,
                parseDate: parseDateIso8601,
            };
        }
    }

    render() {
        const { model } = this.binder;
        this.binder.for(model.apellidoPaterno).addValidator({
            message: 'El apellido paterno debe de tener al menos 3 caracteres',
            validate: (apellidoPaterno: string) => {
                if (apellidoPaterno.length > 2) {
                    return true;
                }
                return { property: model.apellidoPaterno };
            },
        });

        return html`
              <vaadin-horizontal-layout class="gap-x-s">
                  <vaadin-text-field
                          label='Nombre'
                          ?disabled=${uiStore.offline}
                          ...=${field(model.nombre)}
                  ></vaadin-text-field>
              </vaadin-horizontal-layout>
              <vaadin-horizontal-layout class="gap-x-s">
                  <vaadin-text-field
                          label='Apellido paterno'
                          ?disabled=${uiStore.offline}
                          ...=${field(model.apellidoPaterno)}
                  ></vaadin-text-field>
                  <vaadin-text-field
                          label='Apellido materno'
                          ?disabled=${uiStore.offline}
                          ...=${field(model.apellidoMaterno)}
                  ></vaadin-text-field>
              </vaadin-horizontal-layout>
              <vaadin-horizontal-layout class="gap-x-s">
                  <vaadin-combo-box
                          label='Género'
                          auto-open-disabled
                          ?disabled=${uiStore.offline}
                          .items=${this.generoTypes}
                          ...=${field(model.genero)}
                  ></vaadin-combo-box>
                  <vaadin-combo-box
                          label='Estado civil'
                          auto-open-disabled
                          ?disabled=${uiStore.offline}
                          .items=${this.estadoCivilTypes}
                          ...=${field(model.estadoCivil)}
                  ></vaadin-combo-box>
              </vaadin-horizontal-layout>
              <vaadin-horizontal-layout class="gap-x-s">
                  <vaadin-date-picker
                          label='Fecha de nacimiento'
                          value='${this.selectedDateValue}'
                          ?disabled=${uiStore.offline}
                          ...=${field(model.fechaNacimiento)}
                          @change="${(e: DatePickerValueChangedEvent) => (this.selectedDateValue = (e.target as HTMLInputElement).value)}"
                  ></vaadin-date-picker>
                  <vaadin-text-field
                          label='Id único de persona'
                          ?disabled=${uiStore.offline}
                          ...=${field(model.idPersona)}
                          pattern="^\\d{6}$"
                          help-text='Identificador para LDAP y otras aplicaciones (opcional).'
                  ></vaadin-text-field>
              </vaadin-horizontal-layout>
              <vaadin-checkbox
                      label='Estatus activo'
                      ?disabled=${uiStore.offline}
                      ...=${field(model.activo)}
              ></vaadin-checkbox>
              <vaadin-accordion>
                  <vaadin-accordion-panel>
                      <div slot="summary"><span class="text-secondary text-s">Emails</span></div>
                      <vaadin-vertical-layout>
                          <vaadin-grid style="max-height: 200px"
                                   .items=${personViewStore.emails}
                          >
                              <vaadin-grid-column path="email" header="Email" auto-width></vaadin-grid-column>
                              <vaadin-grid-column
                                      .renderer="${this.trashEmailRenderer}"
                                      auto-width
                              ></vaadin-grid-column>
                          </vaadin-grid>
                          <vaadin-email-field
                                  label="Añadir email"
                                  ?disabled=${uiStore.offline}
                                  error-message="Teclear un email válido"
                                  clear-button-visible
                                  @change="${this.addEmail}"
                          ></vaadin-email-field>
                      </vaadin-vertical-layout>
                  </vaadin-accordion-panel>
                  <vaadin-accordion-panel>
                      <div slot="summary"><span class="text-secondary text-s">Teléfonos de la empresa</span></div>
                      <vaadin-vertical-layout>
                          <vaadin-grid style="max-height: 200px"
                                       .items=${personViewStore.telephones}
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
                                      .value ="${personViewStore.telephoneNumber}"
                                      @change="${this.setTelephoneNumber}"
                              ></vaadin-text-field>
                              <vaadin-text-field
                                      label="Ciudad"
                                      minlength="4"
                                      ?disabled=${uiStore.offline}
                                      .value ="${personViewStore.telephoneCity}"
                                      @change="${this.setTelephoneCity}"
                              ></vaadin-text-field>
                              <vaadin-combo-box
                                      label="Tipo"
                                      auto-open-disabled
                                      .value="${personViewStore.telephoneType}"
                                      @change="${this.setTelephoneType}"
                                      .items="${this.telephoneTypes}"
                              ></vaadin-combo-box>
                              <vaadin-button 
                                      theme="icon primary small" 
                                      title="Añadir el teléfono" 
                                      @click="${this.addTelephone}"
                                      ?disabled=${uiStore.offline || !((personViewStore.telephoneNumber) && (personViewStore.telephoneNumber.length > 0) &&
                                                                       (personViewStore.telephoneCity) && (personViewStore.telephoneCity.length > 0) &&
                                                                        (personViewStore.telephoneType) && (personViewStore.telephoneType.length > 0))}
                              >
                                  <vaadin-icon icon="vaadin:plus" slot="prefix"></vaadin-icon>
                              </vaadin-button>
                          </vaadin-horizontal-layout>
                      </vaadin-vertical-layout>
                  </vaadin-accordion-panel>
                  <vaadin-accordion-panel>
                      <div slot="summary"><span class="text-secondary text-s">Direcciones</span></div>
                      <vaadin-grid style="max-height: 300px"
                                   .items=${personViewStore.addresses}
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
                          ?disabled=${uiStore.offline || !((personViewStore.address?.calle) && (personViewStore.address?.calle.length > 0) &&
                                                            (personViewStore.address?.ciudad) && (personViewStore.address?.ciudad.length > 0) &&
                                                            (personViewStore.address?.tipo) && (personViewStore.address?.tipo.length > 0) &&
                                                            (personViewStore.address?.codigo) && (personViewStore.address?.municipio))}
                        >
                        <vaadin-icon icon="vaadin:plus" slot="prefix"></vaadin-icon>
                      </vaadin-button>
                      <vaadin-text-field
                              class="block"
                              label="Calle"
                              ?disabled=${uiStore.offline}
                              .value ="${personViewStore.address?.calle}"
                              @change="${this.setAddressStreet}"
                      ></vaadin-text-field>
                      <vaadin-vertical-layout class="block">
                          <vaadin-horizontal-layout class="gap-x-s">
                              <vaadin-text-field
                                      label="Ciudad"
                                      minlength="4"
                                      ?disabled=${uiStore.offline}
                                      .value ="${personViewStore.address?.ciudad}"
                                      @change="${this.setAddressCity}"
                              ></vaadin-text-field>
                              <vaadin-combo-box
                                      label="Tipo"
                                      auto-open-disabled
                                      .value="${personViewStore.address?.tipo}"
                                      @change="${this.setAddressType}"
                                      .items="${this.addressTypes}"
                              ></vaadin-combo-box>

                          </vaadin-horizontal-layout>
                          <vaadin-horizontal-layout class="gap-x-s">
                              <vaadin-text-field
                                      label="Municipio"
                                      minlength="4"
                                      ?disabled=${uiStore.offline}
                                      .value ="${personViewStore.address?.municipio?.nombre}"
                                      @change="${this.changeColony}"
                              ></vaadin-text-field>
                              <vaadin-combo-box
                                      label="Código postal"
                                      allow-custom-value
                                      ?disabled=${uiStore.offline || (!personViewStore.address?.municipio
                                                                    && !personViewStore.enableZipcodes)}
                                      .value ="${personViewStore.selectedZipcode}"
                                      @change="${this.changeZipcode}"
                                      .items="${personViewStore.address?.municipio?.codigos}"
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
                                      ?disabled=${uiStore.offline  || !personViewStore.enableStates}
                                      .selectedItem ="${personViewStore.selectedState?.nombre}"
                                      @filter-changed="${this.filterChangedState}"
                                      @change="${this.changedState}"
                                      help-text="Código postal nuevos. Se debe definir el estado."
                              ></vaadin-combo-box>
                              <vaadin-text-field label="País" ?disabled=${true}
                                                 .value="${personViewStore.address?.codigo?.estado?.pais}" 
                              >
                              </vaadin-text-field>
                          </vaadin-horizontal-layout>
                      </vaadin-vertical-layout>
                  </vaadin-accordion-panel>
                  <vaadin-accordion-panel>
                      <div slot="summary"><span class="text-secondary text-s">Empresas que trabaja</span></div>
                      <vaadin-list-box style="max-height: 200px">
                          <hr />
                          ${personViewStore.selectedPerson?.trabaja?.map((work) =>
                                  html`<vaadin-item>
                                      <span>${work?.to?.nombre} =></span>
                                      <span class="text-2xs font-extralight">${work?.puesto}</span>
                                  </vaadin-item> `
                          )}
                          <hr />
                      </vaadin-list-box>
                  </vaadin-accordion-panel>
                  <vaadin-accordion-panel>
                      <div slot="summary"><span class="text-secondary text-s">Areas que dirige</span></div>
                      <vaadin-list-box style="max-height: 200px">
                          <hr />
                          ${personViewStore.selectedPerson?.dirige?.map((directs) =>
                                  html`<vaadin-item>
                                      <span>${directs?.to?.nombre}</span>
                                      <span class="text-2xs font-extralight">${directs?.nombreCompania}</span>
                                  </vaadin-item> `
                          )}
                          <hr />
                      </vaadin-list-box>
                  </vaadin-accordion-panel>
                  <vaadin-accordion-panel>
                      <div slot="summary"><span class="text-secondary text-s">Relaciones</span></div>
                      <vaadin-list-box style="max-height: 200px">
                          <hr />
                          ${personViewStore.selectedPerson?.relaciones?.map((relacion) =>
                                  html`<vaadin-item>
                            <span class="text-2xs font-extralight">${relacion?.tipo}</span>
                            <span>${relacion?.nombre} => ${relacion?.to?.nombre} ${relacion?.to?.apellidoPaterno} ${relacion?.to?.apellidoMaterno}</span>
                        </vaadin-item> `
                          )}
                          <hr />
                      </vaadin-list-box>
                  </vaadin-accordion-panel>
              </vaadin-accordion>
              <hr />
              <div class="buttons spacing-e-s">
                <vaadin-button
                  theme="primary"
                  @click=${this.save}
                  ?disabled=${this.binder.invalid || uiStore.offline}
                >
                  ${this.binder.value.idNeo4j != null ? 'Guardar' : 'Nueva persona'}
                </vaadin-button>
                <vaadin-button
                  theme="error"
                  @click="${() => (this.dialogOpened = true)}"
                  ?disabled=${!this.binder.value.idNeo4j || uiStore.offline}
                >
                  Borrar
                </vaadin-button>
                <vaadin-button theme="tertiary" @click=${personViewStore.cancelEdit}>
                  Cancelar
                </vaadin-button>
              </div>
              
              <vaadin-dialog
                    header-title="${`¿Se desea borrar a la persona "${this.binder.value.nombre} ${this.binder.value.apellidoPaterno} ${this.binder.value.apellidoMaterno}"?`}"
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
        const validate = await personViewStore.validate(this.binder.value);

        if (!validate) {
            await this.binder.submitTo(personViewStore.save);
            this.binder.clear();
        }
    };


    async delete() {
        this.dialogOpened = false;
        personViewStore.delete();
    }

    /**
     * Email methods
     */
    private trashEmailRenderer = (root: HTMLElement, _: HTMLElement, model: GridItemModel<EmailAsignado>) => {
        const email = model.item.email;

        render(
            html`
                <vaadin-button id=${email} theme="icon primary small" title="Eliminar el email" @click="${this.deleteEmail}">
                    <vaadin-icon icon="vaadin:trash" slot="prefix"></vaadin-icon>
                </vaadin-button>
            `,
            root
        );
    };

    private deleteEmail() {
        personViewStore.deleteEmail(this.id);
    }

    private addEmail(event: CustomEvent) {
        const target = event.target as EmailField;

        if (target.checkValidity()) {
            const value = target.value;

            if (value.length === 0) return;
            if (personViewStore.addEmail(value)) {
                target.value = "";
            }
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
            personViewStore.setTelephoneNumber(target.value);
    }

    private setTelephoneCity(event: CustomEvent) {
        const target = event.target as TextField;

        if (target.checkValidity())
            personViewStore.setTelephoneCity(target.value);
    }

    private setTelephoneType(event: CustomEvent) {
        personViewStore.setTelephoneType((event.target as ComboBox).value);
    }

    private deleteTelephone() {
        personViewStore.deleteTelephone(this.id);
    }

    private addTelephone() {
        personViewStore.addTelephone();
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
        personViewStore.deleteAddress(this.id);
    }

    private setAddressStreet(event: CustomEvent) {
        const target = event.target as TextField;

        personViewStore.setAddressStreet(target.value);
    }

    private setAddressCity(event: CustomEvent) {
        const target = event.target as TextField;

        personViewStore.setAddressCity(target.value);
    }

    private setAddressType(event: CustomEvent) {
        personViewStore.setAddressType((event.target as ComboBox).value);
    }

    private addAddress() {
        personViewStore.addAddress();
    }

    private changeColony(event: CustomEvent) {
        const target = event.target as TextField;

        if (target.checkValidity()) {
            personViewStore.changeColony(target.value);
        }
    }

    private changeZipcode(event: CustomEvent) {
        const target = event.target as ComboBox;

        if (target.checkValidity()) {
            personViewStore.changeZipcode(target.value);
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

        personViewStore.changeState(target.selectedItem);
    }

}
