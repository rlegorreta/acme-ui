/* Copyright (c) 2023, LegoSoft Soluciones, S.C.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are not permitted.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 *
 *  SysStore.ts
 *
 *  Developed 2023 by LegoSoftSoluciones, S.C. www.legosoft.com.mx
 */
import { makeAutoObservable, observable, runInAction } from 'mobx';

import * as endpoint from 'Frontend/generated/AcmeEndpoint';
import * as cacheEndpoint from 'Frontend/generated/CacheEndpoint';
import * as messageEndpoint from 'Frontend/generated/MessageEndpoint';
import SysDataModel from 'Frontend/generated/com/acme/acmeui/data/endpoint/AcmeEndpoint/SysDataModel';
import { uiStore } from './app-store';
import Sector from "Frontend/generated/com/acme/acmeui/data/dto/Sector";
import { cacheable } from './cacheable';
import Compania from "Frontend/generated/com/acme/acmeui/data/dto/Compania";
import Municipio from "Frontend/generated/com/acme/acmeui/data/dto/Municipio";
import Estado from "Frontend/generated/com/acme/acmeui/data/dto/Estado";
import Codigo from "Frontend/generated/com/acme/acmeui/data/dto/Codigo";
import Persona from "Frontend/generated/com/acme/acmeui/data/dto/Persona";

export class SysStore {
    sectors: Sector[] = [];
    states:Estado[] = [];
    sysDate = "";
    exchangeRate = 0;

    constructor() {
        makeAutoObservable(
            this,
            {
                initFromServer: false,
                sectors: observable.shallow,
                states: observable.shallow,
            },
            { autoBind: true }
        );
    }

    async initFromServer() {
        // To use data cache. This is for small grid sizes
        const data = await cacheable(
            () => endpoint.getSysData(),
            'sys',
            SysDataModel.createEmptyValue()
        );

        // If we don´t want to use cache
        // const data = await endpoint.getSysData();
        const sysDate = await cacheEndpoint.getDay(0);
        const exchangeRate = await cacheEndpoint.getRate("MXN-DLR");

        runInAction(() => {
            this.sectors = data.sectors;
            this.states = data.states;
            this.sysDate = sysDate;
            this.exchangeRate = exchangeRate;
        });
    }

    /**
     * Notification functions
     */
    async notifications() {
        const notifications = await messageEndpoint.notifications();

        console.log(">>>>> IN nOTIFICATIONS");
        console.log(notifications);
        console.log(">>>>> AFTER nOTIFICATIONS");
        return notifications;
    }

    /**
     * Sector functions
     */
    async validateSector(sector: Sector) {
        const validate = await endpoint.uniqueSectorValidator(sector);

        if (validate) {
            uiStore.showError(validate);
        }
        return validate;
    }

    async saveSector(sector: Sector) {
        try {
            const saved = await endpoint.saveSector(sector);

            if (saved) {
                this.saveSectorLocal(saved);
                uiStore.showSuccess('Se almacenó el sector de empresa.');
            } else {
                uiStore.showError('Existió un error. No se pudo almacenar el sector de empresa');
            }
        } catch (e) {
            console.log(e);
            uiStore.showError('Existió un error. No se pudo almacenar el sector de empresa.' );
        }
    }

    async deleteSector(sector: Sector) {
        if (!sector.idNeo4j) return;     // could be a new register. Then do nothing

        try {
            const error = await endpoint.deleteSector(sector.idNeo4j);

            if (error) {
                uiStore.showError(error);
            } else {
                this.deleteSectorLocal(sector);
                uiStore.showSuccess('Sector industrial borrado.');
            }
        } catch (e) {
            console.log(e);
            uiStore.showError('Existió un error. No se pudo borrar el sector empresarial.');
        }
    }

    /*
     * note: In the front we do not use sector._id but idNeo4j because Hilla does not generate correctly
     *       the accessor methods (i.e., always is undefined because is not in sync from the server).
     */
    private saveSectorLocal(saved: Sector) {
        const sectorExists = this.sectors.some((c) => c.idNeo4j === saved.idNeo4j);

        if (sectorExists) {
            this.sectors = this.sectors.map((existing) => {
                if (existing.idNeo4j === saved.idNeo4j) {
                    return saved;
                } else {
                    return existing;
                }
            });
        } else {
            this.sectors.push(saved);
        }
    }

    private deleteSectorLocal(sector: Sector) {
        this.sectors = this.sectors.filter((c) => c.idNeo4j !== sector.idNeo4j);
    }

    /**
     * Companies methods
     */
    async validateCompany(company: Compania) {
        const validate = await endpoint.uniqueCompaniaValidator(company);

        if (validate) {
            uiStore.showError(validate);
        }
        return validate;
    }

    async saveCompany(company: Compania) {
        try {
            const saved = await endpoint.saveCompany(company);

            if (saved) {
                uiStore.showSuccess('Se almacenó la compañía.');
            } else {
                uiStore.showError('Existió un error. No se pudo almacenar la compañía');
            }
        } catch (e) {
            console.log(e);
            uiStore.showError('Existió un error. No se pudo almacenar la compañía.' );
        }
    }

    async deleteCompany(company: Compania) {
        if (!company.idNeo4j) return;     // could be a new register. Then do nothing

        try {
            const error ="TODO: por definir" //  await endpoint.deleteCompany(company.idNeo4j);

            if (error) {
                uiStore.showError(error);
            } else {
                uiStore.showSuccess('La compañía fue borrada.');
            }
        } catch (e) {
            console.log(e);
            uiStore.showError('Existió un error. No se pudo borrar a la compañía.');
        }
    }

    /**
     * Persons methods
     */
    async validatePerson(person: Persona) {
        const validate = await endpoint.uniquePersonValidator(person);

        if (validate) {
            uiStore.showError(validate);
        }
        return validate;
    }

    async savePerson(person: Persona) {
        try {
            const saved = await endpoint.savePerson(person);

            if (saved) {
                uiStore.showSuccess('Se almacenó la persona.');
            } else {
                uiStore.showError('Existió un error. No se pudo almacenar la persona');
            }
        } catch (e) {
            console.log(e);
            uiStore.showError('Existió un error. No se pudo almacenar la persona.' );
        }
    }

    async deletePerson(person: Persona) {
        if (!person.idNeo4j) return;     // could be a new register. Then do nothing

        try {
            const error ="TODO: por definir" //  await endpoint.deletePerson(person.idNeo4j);

            if (error) {
                uiStore.showError(error);
            } else {
                uiStore.showSuccess('La persona fue borrada.');
            }
        } catch (e) {
            console.log(e);
            uiStore.showError('Existió un error. No se pudo borrar a la persona.');
        }
    }

    /**
     * Address methods
     */
    async getColony(colony: string) {
        const validate = await endpoint.getMunicipio(colony);

        if (validate === undefined) {
            uiStore.showPrimary("El municipio no existe. Si se quiere dar de alta se deberá definir en que código postal se encuentra");
        }
        return validate;
    }

    async getZipcode(zipcode: number) {
        const validate = await endpoint.getZipcode(zipcode);

        if (validate === undefined) {
            uiStore.showPrimary("El código postal no existe. Si se quiere dar de alta se deberá definir a que estado pertenece");
        }
        return validate;
    }

    async addColonyZipcode(idColony: string, idZipcode: string) {
        const validate = await endpoint.addColonyZipcode(idColony, idZipcode);

        if (validate === undefined) {
            uiStore.showPrimary("No se pudo añadir el código postal al municipio");
        }

        return validate;
    }

    async addZipcode(zipcode: Codigo) {
        const validate = await endpoint.addZipcode(zipcode);

        if (validate === undefined) {
            uiStore.showPrimary("No se pudo añadir el código postal");
        }

        return validate;
    }

    async addColony(colony: Municipio) {
        const validate = await endpoint.addColony(colony);

        if (validate === undefined) {
            uiStore.showPrimary("No se pudo añadir el municipio");
        }

        return validate;
    }

}
