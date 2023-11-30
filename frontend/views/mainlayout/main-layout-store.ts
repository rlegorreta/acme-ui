import { makeAutoObservable, observable } from "mobx";
import {isAuthorizedViewRoute, menuViews} from '../../routes';

export type SelectedView = number | null;

class MainLayoutStore {
    selectedSubViews: SelectedView[] = [];

    constructor() {
        makeAutoObservable(
            this,
            {},
            {autoBind: true }
        );
    };

    setSelectedSubView(index: any, value: SelectedView) {
        this.selectedSubViews[index] = value;
    }

    initSubViews() {
        this.selectedSubViews = new Array(menuViews.filter(isAuthorizedViewRoute).length).fill(null);
    }

}

export const mainLayoutStore = new MainLayoutStore();
