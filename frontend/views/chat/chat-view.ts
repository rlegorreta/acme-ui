import { html, render } from 'lit';
import {customElement, state} from 'lit/decorators.js';
import { View } from 'Frontend/views/view';
import '@vaadin/message-input';
import '@vaadin/message-list';
import { uiStore } from '../../stores/app-store';
import * as ChatEndpoint from "Frontend/generated/ChatEndpoint";
import Message from "Frontend/generated/com/acme/acmeui/data/endpoint/ChatEndpoint/Message";


@customElement('chat-view')
export class ChatView extends View {

    @state()
    messagges: Message[] = [];

    connectedCallback() {
        super.connectedCallback();
        this.classList.add(
            'flex',
            'flex-col',
            'h-full',
            'box-border'
        );
        ChatEndpoint.join().onNext((message) => {
            this.messagges = [...this.messagges.slice(-10), message]
        })
    }

    get formattedMessages() {
        return this.messagges.map((m) => ({
            ...m,
            // @ts-ignore
            time: new Date(m.time).toLocaleTimeString('en-US'),
        }))
    }

    render() {
        return html`
            <vaadin-message-list class="flex-grow" .items="${this.formattedMessages}">
            </vaadin-message-list>
            <div class="flex p-s gap-s items-baseline">
                <vaadin-message-input class="flex-grow" i="Mensaje" @submit="${this.submit}">
                </vaadin-message-input>
            </div>
        `;
    }

    submit(e : CustomEvent) {
        let message: Message = {
            text: e.detail.value,
            time: "undefined",
            userName: uiStore.authentication?.user.name!
        }
        ChatEndpoint.send(message).then()
    }
}
