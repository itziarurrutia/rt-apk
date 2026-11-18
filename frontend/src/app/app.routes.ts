import { Routes } from '@angular/router';

export const routes: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'assets' },
  {
    path: 'assets',
    title: 'Equipos',
    loadComponent: () => import('./assets/asset-search').then((m) => m.AssetSearch),
  },
  {
    path: 'assets/:inventario',
    title: 'Equipo',
    loadComponent: () => import('./assets/asset-detail').then((m) => m.AssetDetail),
  },
  {
    path: 'tickets',
    title: 'Tickets',
    loadComponent: () => import('./tickets/ticket-list').then((m) => m.TicketList),
  },
  {
    path: 'tickets/:id',
    title: 'Ticket',
    loadComponent: () => import('./tickets/ticket-detail').then((m) => m.TicketDetail),
  },
  { path: '**', redirectTo: 'assets' },
];
