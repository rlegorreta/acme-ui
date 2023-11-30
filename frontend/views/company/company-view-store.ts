import Compania from 'Frontend/generated/com/acme/acmeui/data/dto/Compania';
import CompaniaModel from 'Frontend/generated/com/acme/acmeui/data/dto/CompaniaModel';
import {sysStore, uiStore} from 'Frontend/stores/app-store';
import {makeAutoObservable, observable} from 'mobx';
import Area from "Frontend/generated/com/acme/acmeui/data/dto/Area";
import AreaModel from "Frontend/generated/com/acme/acmeui/data/dto/AreaModel";
import Telefono from "Frontend/generated/com/acme/acmeui/data/dto/Telefono";
import TelefonoModel from "Frontend/generated/com/acme/acmeui/data/dto/TelefonoModel";
import Direccion from "Frontend/generated/com/acme/acmeui/data/dto/Direccion";
import DireccionModel from "Frontend/generated/com/acme/acmeui/data/dto/DireccionModel";
import Municipio from "Frontend/generated/com/acme/acmeui/data/dto/Municipio";
import Codigo from "Frontend/generated/com/acme/acmeui/data/dto/Codigo";
import Estado from "Frontend/generated/com/acme/acmeui/data/dto/Estado";
import CodigoModel from "Frontend/generated/com/acme/acmeui/data/dto/CodigoModel";
import MunicipioModel from "Frontend/generated/com/acme/acmeui/data/dto/MunicipioModel";
import {GridDataProviderCallback} from "@vaadin/grid";

class CompanyViewStore {
    content: Array<Compania> | undefined;
    callBack: GridDataProviderCallback<Compania> | null = null;

    selectedCompany: Compania | null = null;
    areas?: Array<Area | undefined> = undefined;
    telephones?: Array<Telefono | undefined> = undefined;
    telephoneNumber: string | null = null;
    telephoneCity: string | null = null;
    telephoneType: string | null = null;
    addresses?: Array<Direccion | undefined> = undefined;
    address: Direccion | undefined = undefined;
    selectedZipcode: number | null = null;
    selectedState: Estado | null = null;
    enableStates = false;
    enableZipcodes = false;

    constructor() {
        makeAutoObservable(
            this,
            { callBack: false,
                content: observable.shallow,
                selectedCompany: observable.ref,
                areas: observable.ref,
                telephones: observable.ref,
                addresses: observable.ref,
                selectedState: observable.ref,
            },
            { autoBind: true }
        );
    }

    setSelectedCompany(company: Compania) {
        this.selectedCompany = company;
        if (company && this.selectedCompany.areas)
            this.areas = this.selectedCompany.areas;
        else
            this.areas = [];
        if (company && this.selectedCompany.telefonos)
            this.telephones = this.selectedCompany.telefonos;
        else
            this.telephones = [];
        if (company && this.selectedCompany.direcciones)
            this.addresses = this.selectedCompany.direcciones;
        else
            this.addresses = [];
        this.address = DireccionModel.createEmptyValue();
    }

    editNew() {
        this.selectedCompany = CompaniaModel.createEmptyValue();
        this.areas = [];
        this.telephones = [];
        this.telephoneNumber = this.telephoneCity = this.telephoneType = null;
        this.addresses = [];
        this.address = DireccionModel.createEmptyValue();
    }

    async cancelEdit() {
        this.selectedCompany = null;
        this.areas = this.telephones = undefined;
        this.telephoneNumber = this.telephoneCity = this.telephoneType = null;
        this.addresses = this.address = undefined;
    }

    async validate(company: Compania) {
        return await sysStore.validateCompany(company);
    }

    async save(company: Compania) {
        company.areas = this.areas;             // maybe a change in areas existed
        company.telefonos = this.telephones;    // and telephones
        company.direcciones = this.addresses;
        await sysStore.saveCompany(company);
        await this.cancelEdit();
        this.saveCompanyLocal(company);
    }

    async delete() {
        if (this.selectedCompany) {
            await sysStore.deleteCompany(this.selectedCompany);
            this.deleteCompanyLocal(this.selectedCompany);
            await this.cancelEdit();
        }
    }

    addArea(nombreArea: String) {
        // @ts-ignore
        const areaExists = (this.areas !== undefined) && this.areas.some((a) => a.nombre === nombreArea);

        if (areaExists) {
            uiStore.showError("El área ya existe en la compañía");
            return null;
        }
        const newArea = AreaModel.createEmptyValue();
        // @ts-ignore
        newArea.nombre = nombreArea;
        this.areas!!.push(newArea);
        this.areas = this.areas!!.map((a) => a);

        return newArea;
    }

    deleteArea(nombre: String) {
        // @ts-ignore
        this.areas = this.areas!!.filter((a) => a!!.nombre !== nombre);
    }

    addTelephone() {
        // @ts-ignore
        const telephoneExists = (this.telephones !== undefined) && this.telephones.some((a) => a.numero === this.telephoneNumber);

        if (telephoneExists) {
            uiStore.showError("El teléfono ya existe en la compañía");
            return null;
        }
        const newTelephone = TelefonoModel.createEmptyValue();
        newTelephone.numero = this.telephoneNumber as string;
        newTelephone.ciudad = this.telephoneCity as string;
        // @ts-ignore
        newTelephone.tipo = this.telephoneType;
        this.telephones!!.push(newTelephone);
        this.telephones = this.telephones!!.map((a) => a);

        return newTelephone;
    }

    deleteTelephone(numero: String) {
        // @ts-ignore
        this.telephones = this.telephones!!.filter((a) => a!!.numero !== numero);
    }

    setTelephoneNumber(telephoneNumber: string) {
        this.telephoneNumber = telephoneNumber;
    }
    setTelephoneCity(telephoneCity: string) {
        this.telephoneCity = telephoneCity;
    }
    setTelephoneType(telephoneType: string) {
        this.telephoneType = telephoneType;
    }

    addAddress() {
        // @ts-ignore
        const addressExists = (this.addresses !== undefined) &&
            this.addresses.some((a) => (a as Direccion).calle === this.address?.calle);

        if (addressExists) {
            uiStore.showError("La dirección ya existe en la compañía");
            return null;
        }
        // @ts-ignore
        this.addresses!!.push(this.address);
        this.addresses = this.addresses!!.map((a) => a);
        this.address = DireccionModel.createEmptyValue();

        return this.address;
    }

    deleteAddress(street: String) {
        // @ts-ignore
        this.addresses = this.addresses!!.filter((a) => a!!.calle !== street);
    }

    setAddressStreet(street: string) {
        (this.address as Direccion).calle = street;
    }
    setAddressCity(addressCity: string) {
        (this.address as Direccion).ciudad = addressCity;
    }
    setAddressType(addressType: string) {
        // @ts-ignore
        (this.address as Direccion).tipo = addressType;
    }

    async changeColony(colony: string) {
        let municipio = await sysStore.getColony(colony);

        if (municipio === undefined) {
            municipio = MunicipioModel.createEmptyValue();
            municipio.nombre = colony;
            municipio.codigos = [];
            this.setAddressCodigo(undefined);
            this.setSelectedState(undefined);
            this.setEnableZipcodes(true);
        } else { // @ts-ignore
            const cps = municipio.codigos as Array<Codigo>;
            this.setAddressCodigo(cps[0]);
            this.setSelectedZipcode(cps[0].cp as number);
            this.setSelectedState(cps[0].estado);
            this.setEnableZipcodes(false);
        }
        this.setAddressMunicipio(municipio);
    }

    async changeZipcode(zipcode: string | number ) {
        this.setEnableStates(false);
        let zc = 0;

        if (typeof zipcode === 'string') {
            if (zipcode.length != 5) {
                uiStore.showError("EL código postal debe ser de 5 dígitos");
                return;
            }
            zc = Number(zipcode);
            if (isNaN(zc)) {
                uiStore.showError("Zip code debe ser numérico");
                return;
            }
        } else {
            // this already exists in the combo Box
            zc = zipcode;
        }
        const address = this.address as Direccion;

        if (address.municipio === undefined) {
            uiStore.showError("No se ha definido un municipio");
            return;
        }
        let municipio: Municipio | undefined = address.municipio as Municipio;
        const zipCodeExists = municipio.codigos?.filter((c) => c?.cp === zc);

        if (zipCodeExists === undefined || zipCodeExists?.length === 0) {
            const newZipcode = await sysStore.getZipcode(zc);

            if (newZipcode === undefined) {
                uiStore.showPrimary("El código postal no existe. Si se quiere dar de alta deberá definir en que estado se encuentra");
                this.setSelectedZipcode(zc);
                this.setEnableStates(true);
            } else {
                // zipcode exists in the database the just added to municipio
                if (municipio.idNeo4j === undefined) {
                    // it is new municipio we need to add and the relationship at the same time
                    municipio.codigos?.push(newZipcode);
                    municipio = await sysStore.addColony(municipio);

                    if (municipio) {
                        this.setAddressMunicipio(municipio);
                        this.setAddressCodigo(newZipcode);
                        this.setSelectedState(newZipcode.estado);
                        uiStore.showContrast("Se dio de alta el municipio" + municipio.nombre +
                            " con el código postal:" + newZipcode.cp);
                    } else {
                        uiStore.showError("no se pudo dar de alta al municipio y ligarlo la código postal");
                        return;
                    }
                } else if (await sysStore.addColonyZipcode(municipio.idNeo4j as string, newZipcode.idNeo4j as string)) {
                    municipio.codigos?.push(newZipcode);
                    this.setAddressCodigo(newZipcode);
                    this.setSelectedState(newZipcode.estado);
                    uiStore.showContrast("Se habilitó que el municipio" + municipio.nombre +
                        " con el código postal:" + newZipcode.cp);
                } else {
                    uiStore.showError("Error al querer asignar el código al municipio");
                }
            }
        } else {
            // @ts-ignore
            address.codigo = zipCodeExists[0];
            this.setSelectedZipcode((address.codigo as Codigo).cp as number);
            this.setSelectedState(address.codigo?.estado);
            return;     // everything is ok:
        }
    }

    async changeState(state: Estado) {
        if (this.selectedZipcode) {
            const newZipcode = CodigoModel.createEmptyValue();

            newZipcode.cp = this.selectedZipcode;
            newZipcode.estado = state;

            const addedZipcode = await sysStore.addZipcode(newZipcode);
            if (addedZipcode) {
                const address = this.address as Direccion;
                let municipio: Municipio | undefined = address.municipio as Municipio;

                if (municipio.idNeo4j === undefined) {
                    // it is new municipio we need to add and the relationship at the same time
                    municipio.codigos?.push(addedZipcode);
                    municipio = await sysStore.addColony(municipio);

                    if (municipio) {
                        this.setAddressMunicipio(municipio);
                        this.setAddressCodigo(addedZipcode);
                        this.setSelectedState(addedZipcode.estado);
                        this.setEnableStates(false);
                        this.setEnableZipcodes(false);
                        uiStore.showContrast("Se dio de alta el municipio" + municipio.nombre +
                            " con el código postal:" + addedZipcode.cp);
                    } else {
                        uiStore.showError("no se pudo dar de alta al municipio y ligarlo la código postal");
                        return;
                    }
                } else if (await sysStore.addColonyZipcode(municipio.idNeo4j as string, addedZipcode.idNeo4j as string)) {
                    municipio.codigos?.push(newZipcode);
                    address.codigo = newZipcode;
                    this.setEnableStates(false);
                    uiStore.showContrast("Se creo un nuevo código postal:" + newZipcode.cp +
                        " y se le asignó al municipio:" + municipio.nombre);
                } else {
                    uiStore.showError("Se creó el código postal pero no se pudo asignar al municipio");
                }
            } else {
                uiStore.showError("Error al dar de alta el código postal");
            }
        }
    }

    setContent(content: Array<Compania> | undefined) {
        this.content = content;
    }

    setCallBack(callBack: GridDataProviderCallback<Compania> ) {
        this.callBack = callBack;
    }

    /*
     * note: In the front we do not use company._id but idNeo4j because Hilla does not generate correctly
     *       the accessor methods (i.e., always is undefined because is not in sync from the server).
     */
    private saveCompanyLocal(saved: Compania) {
        if (this.content != undefined) {
            const companyExists = this.content.some((c) => c.idNeo4j === saved.idNeo4j);

            if (companyExists) {
                this.content = this.content.map((existing) => {
                    if (existing.idNeo4j === saved.idNeo4j) {
                        return saved;
                    } else {
                        return existing;
                    }
                });
            } else {
                this.content.push(saved);
            }

            // @ts-ignore
            this.callBack(this.content, this.content.length);
        }
    }

    private deleteCompanyLocal(company: Compania) {
        // @ts-ignore
        this.content = this.content.filter((c) => c.idNeo4j !== company.idNeo4j);
    }

    private setSelectedZipcode(selectedZipcode: number) {
        this.selectedZipcode = selectedZipcode;
    }
    private setSelectedState(selectedState: Estado | undefined) {
        if (selectedState === undefined)
            this.selectedState = null;
        else
            this.selectedState = selectedState;
    }
    private setAddressMunicipio(municipio: Municipio | undefined) {
        (this.address as Direccion).municipio = municipio;
    }
    private setAddressCodigo(codigo: Codigo | undefined) {
        (this.address as Direccion).codigo = codigo;
    }
    private setEnableStates(enableStates: boolean) {
        this.enableStates = enableStates;
    }
    private setEnableZipcodes(enableZipcodes: boolean) {
        this.enableZipcodes = enableZipcodes;
    }

}

export const companyViewStore = new CompanyViewStore();
