import Sector from 'Frontend/generated/com/acme/acmeui/data/dto/Sector';
import SectorModel from 'Frontend/generated/com/acme/acmeui/data/dto/SectorModel';
import { sysStore } from 'Frontend/stores/app-store';
import { makeAutoObservable, observable } from 'mobx';

class SectorViewStore {
    selectedSector: Sector | null = null;
    filterText = "";

    constructor() {
        makeAutoObservable(
            this,
            { selectedSector: observable.ref },
            { autoBind: true }
        );
    }

    updateFilter(filterText: string) {
        this.filterText = filterText;
    }

    setSelectedSector(sector: Sector) {
        this.selectedSector = sector;
    }

    editNew() {
        this.selectedSector = SectorModel.createEmptyValue();
    }

    cancelEdit() {
        this.selectedSector = null;
    }

    async validate(sector: Sector) {
        return await sysStore.validateSector(sector);
    }

    async save(sector: Sector) {
        await sysStore.saveSector(sector);
        this.cancelEdit();
    }

    async delete() {
        if (this.selectedSector) {
            await sysStore.deleteSector(this.selectedSector);
            this.cancelEdit();
        }
    }

    get filteredSectors() {
        const filter = new RegExp(this.filterText, "i");
        const sectors = sysStore.sectors;
        return sectors.filter((sector) =>
            filter.test(`${sector.nombre}`)
        );
    }
}

export const sectorViewStore = new SectorViewStore();
