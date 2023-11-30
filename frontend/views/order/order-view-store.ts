import Order from 'Frontend/generated/com/acme/acmeui/data/dto/Order';
import {makeAutoObservable, observable} from 'mobx';
import {GridDataProviderCallback} from "@vaadin/grid";

class OrderViewStore {
    content: Array<Order> | undefined;
    callBack: GridDataProviderCallback<Order> | null = null;
    ordersCount: number | undefined = undefined;

    constructor() {
        makeAutoObservable(
            this,
            { callBack: false,
                content: observable.shallow,
            },
            { autoBind: true }
        );
    }

    setCount(count: number | undefined) {
        this.ordersCount = count;
    }

    setContent(content: Array<Order> | undefined) {
        this.content = content;
    }

    setCallBack(callBack: GridDataProviderCallback<Order> ) {
        this.callBack = callBack;
    }

}

export const orderViewStore = new OrderViewStore();

