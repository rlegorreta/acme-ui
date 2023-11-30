import { Commands, Context, Route, Router } from '@vaadin/router';
import { uiStore } from './stores/app-store';
import { autorun } from 'mobx';
import './views/mainlayout/main-layout';
import './views/help/help-view';
import './views/notification/notification-view';
import './views/session/session-expired';
import './views/session/logged-out';
import { Flow } from './generated/jar-resources';
import './views/sector/sector-view';

const authGuard = async (context: Context, commands: Commands) => {
  if (!uiStore.authentication) {
    return undefined;
  }
  // Save requested path
  sessionStorage.setItem('login-redirect-path', context.pathname.substr(1, context.pathname.length - 1));

  return undefined;
};

// Get the server-side routes (if any) from imports generated in the server
const {serverSideRoutes} = new Flow({
  imports: () => import('../frontend/generated/flow/generated-flow-imports')
});

export type ViewRoute = Route & {
  title?: string;
  icon?: string;
  children?: ViewRoute[];
  rolesAllowed?: string[];
};

// Check if the user has permission for this route. If not it is not displayed.
export function isAuthorizedViewRoute(route: ViewRoute) {
  if (route.rolesAllowed) {
    return route.rolesAllowed.find((role) => uiStore.isUserInRole(role));
  }

  return true;
}

export const views: ViewRoute[] = [
  // place routes below (more info https://hilla.dev/docs/routing)
  {
    path: 'sectores',
    component: 'sector-view',
    title: 'Sectores empresariales',
    icon: 'grid-big-o',
    rolesAllowed: ['ROLE_SECTORESEMPRESARIALES'],
    action: async (context, commands: Router.Commands) => {
      if (!uiStore.authentication) {
        return undefined;
      }
      const route = context.route as ViewRoute;

      if (!isAuthorizedViewRoute(route)) {
        return commands.prevent();
      }
      await import('./views/sector/sector-view');

      return undefined;
    },
  },
  {
    path: 'companias',
    component: 'company-view',
    title: 'Compañías',
    icon: 'factory',
    rolesAllowed: ['ROLE_COMPANIAS','ROLE_ALL'],
    action: async (context, commands: Router.Commands) => {
      if (!uiStore.authentication) {
        return undefined;
      }
      const route = context.route as ViewRoute;

      if (!isAuthorizedViewRoute(route)) {
        return commands.prevent();
      }
      await import('./views/company/company-view');

      return undefined;
    },
  },
  {
    path: 'personas',
    component: 'person-view',
    title: 'Personas Físicas',
    icon: 'male',
    rolesAllowed: ['ROLE_PERSONAS','ROLE_ALL'],
    action: async (context, commands: Router.Commands) => {
      if (!uiStore.authentication) {
        return undefined;
      }
      const route = context.route as ViewRoute;

      if (!isAuthorizedViewRoute(route)) {
        return commands.prevent();
      }
      await import('./views/person/person-view');

      return undefined;
    },
  },
  {
    path: 'serverviews/companiacompania',
    title: 'Relaciones entre compañías',
    icon: 'factory',
    rolesAllowed: ['ROLE_RELACIONES','ROLE_ALL'],
    action: async (context, commands: Commands) => {
      if (!uiStore.authentication) {
        return undefined;
      }
      const route = context.route as ViewRoute;

      if (!isAuthorizedViewRoute(route)) {
        return commands.prevent();
      }

      return undefined;
    },
  },
  {
    path: 'serverviews/personapersona',
    title: 'Relaciones entre personas',
    icon: 'family',
    rolesAllowed: ['ROLE_RELACIONES','ROLE_ALL'],
    action: async (context, commands: Commands) => {
      if (!uiStore.authentication) {
        return undefined;
      }
      const route = context.route as ViewRoute;

      if (!isAuthorizedViewRoute(route)) {
        return commands.prevent();
      }

      return undefined;
    },
  },
  {
    path: 'serverviews/personacompania',
    title: 'Relaciones entre personas y compañías',
    icon: 'user-card',
    rolesAllowed: ['ROLE_RELACIONES','ROLE_ALL'],
    action: async (context, commands: Commands) => {
      if (!uiStore.authentication) {
        return undefined;
      }
      const route = context.route as ViewRoute;

      if (!isAuthorizedViewRoute(route)) {
        return commands.prevent();
      }

      return undefined;
    },
  },
  {
    path: 'bpm/recepcion',
    title: 'Recepción de documentos',
    icon: 'inbox',
    rolesAllowed: ['ROLE_RECEPCIONDOCS','ROLE_ALL'],
    action: async (context, commands: Commands) => {
      if (!uiStore.authentication) {
        return undefined;
      }
      const route = context.route as ViewRoute;

      if (!isAuthorizedViewRoute(route)) {
        return commands.prevent();
      }

      return undefined;
    },
  },
  {
    path: 'bpm/tasksadmin',
    title: 'Tareas para Administración',
    icon: 'user-check',
    rolesAllowed: ['ROLE_REVISIONDOCS','ROLE_ALL'],
    action: async (context, commands: Commands) => {
      if (!uiStore.authentication) {
        return undefined;
      }
      const route = context.route as ViewRoute;

      if (!isAuthorizedViewRoute(route)) {
        return commands.prevent();
      }

      return undefined;
    },
  },
  {
    path: 'bpm/taskslegal',
    title: 'Tareas para el Area Legal',
    icon: 'scale-unbalance',
    rolesAllowed: ['ROLE_REVISIONLEGALDOCS','ROLE_ALL'],
    action: async (context, commands: Commands) => {
      if (!uiStore.authentication) {
        return undefined;
      }
      const route = context.route as ViewRoute;

      if (!isAuthorizedViewRoute(route)) {
        return commands.prevent();
      }

      return undefined;
    },
  },
  {
    path: 'bpm/tasks',
    title: 'Todas las tareas',
    icon: 'tasks',
    rolesAllowed: ['ROLE_TAREASPENDIENTES','ROLE_ALL'],
    action: async (context, commands: Commands) => {
      if (!uiStore.authentication) {
        return undefined;
      }
      const route = context.route as ViewRoute;

      if (!isAuthorizedViewRoute(route)) {
        return commands.prevent();
      }

      return undefined;
    },
  },
  {
    path: 'expediente',
    title: 'Expedientes',
    icon: 'archive',
    rolesAllowed: ['ROLE_EXPEDIENTES','ROLE_ALL'],
    action: async (context, commands: Commands) => {
      if (!uiStore.authentication) {
        return undefined;
      }
      const route = context.route as ViewRoute;

      if (!isAuthorizedViewRoute(route)) {
        return commands.prevent();
      }

      return undefined;
    },
  },
  {
    path: 'ordenes',
    component: 'order-view',
    title: 'Consulta de ordenes',
    icon: 'date-input',
    rolesAllowed: ['ROLE_ORDENES','ROLE_ALL'],
    action: async (context, commands: Commands) => {
      if (!uiStore.authentication) {
        return undefined;
      }
      const route = context.route as ViewRoute;

      if (!isAuthorizedViewRoute(route)) {
        return commands.prevent();
      }

      await import('./views/order/order-view');

      return undefined;
    },
  },
  {
    path: 'notificaciones',
    component: 'notification-view',
    title: 'Notificaciones',
    icon: 'bell',
  },
  {
    path: '',
    component: 'help-view',
    title: 'Ayuda',
    icon: 'question-circle',
  },
];
export const routes: ViewRoute[] = [
  {
    path: 'logout',
    action: (_: Context, commands: Commands) => {

      return commands.redirect('/logged-out');
    },
  },
  {
    path: 'session-expired',
    component: 'session-expired',
    title: 'La sesión expiró',
  },
  {
    path: 'logged-out',
    component: 'logged-out',
    title: 'Se salió de la sesión',
  },
  {
    path: '',
    component: 'main-layout',
    children: [...views, ...serverSideRoutes],
    action: authGuard,
  },
];

// Menu order and the subView
export const menuViews: ViewRoute[] = [
    views[0],
    views[1],
    views[2],
    { path: views[3].path,
      component: '',
      title: 'Relaciones',
      icon: 'cluster',
      rolesAllowed: ['ROLE_RELACIONES','ROLE_ALL'],
      children:[views[3], views[4], views[5]]
    },
    views[6],
    views[7],
    views[8],
    views[9],
    views[10],
    views[11],
];

// Catch logins and logouts, redirect appropriately
autorun(() => {
  if (uiStore.authentication) {
    let route = sessionStorage.getItem('login-redirect-path') || '/';
    let view = views.find(view => view.path === route);

    if (view && isAuthorizedViewRoute(view)) {
      try {
        Router.go(route);
      } catch (e) {
        console.log("Try to cacth the error")
        Router.go('');
      }
    } else {
      Router.go('');
    }
  } else {
    if (location.pathname !== '/oauth2login') {
      sessionStorage.setItem('login-redirect-path', location.pathname.substr(1, location.pathname.length - 1));
      uiStore.oauthLogin();
    }
  }
});



