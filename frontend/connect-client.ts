import {
  MiddlewareContext,
  MiddlewareNext,
  ConnectClient,
  InvalidSessionMiddleware,
} from '@hilla/frontend';
import { uiStore } from './stores/app-store';
import {SessionExpiredView} from "Frontend/views/session/session-expired";

const client = new ConnectClient({
  prefix: "connect",
  middlewares: [
    async (context: MiddlewareContext, next: MiddlewareNext) => {
      const response = await next(context);
      // Log out if the session has expired
      if (response.status === 401) {
        uiStore.logout();
      }
      return response;
    },
    new InvalidSessionMiddleware(async () => {
      uiStore.setSessionExpired();
      console.log(">>>> Session expired (*)");
      const { SessionExpiredView } = await import('./views/session/session-expired');

      return SessionExpiredView.show();
    }),
  ],
});

export default client;
