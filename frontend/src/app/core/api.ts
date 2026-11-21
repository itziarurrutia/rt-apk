import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import {
  Asset,
  AssetMatch,
  CurrentUser,
  PassIz,
  Ticket,
  TicketHistoryEntry,
  TicketReciente,
  TicketsRecientes,
} from './models';

@Injectable({ providedIn: 'root' })
export class AssetApi {
  private readonly http = inject(HttpClient);

  /** Busca por nº de inventario, nº de serie o usuario de Windows con un solo texto. */
  search(q: string) {
    return this.http.get<AssetMatch[]>('/api/assets', { params: { q } });
  }

  get(numeroInventario: string) {
    return this.http.get<Asset>(`/api/assets/${encodeURIComponent(numeroInventario)}`);
  }

  /** Pass_iz y su fragmento; se pide solo al activar el switch y no se guarda. */
  passIz(numeroInventario: string) {
    return this.http.get<PassIz>(`/api/assets/${encodeURIComponent(numeroInventario)}/pass-iz`);
  }
}

@Injectable({ providedIn: 'root' })
export class TicketApi {
  private readonly http = inject(HttpClient);

  /** Tickets activos de la cola configurada actualizados recientemente, con su último mensaje. */
  recent() {
    return this.http.get<TicketsRecientes>('/api/tickets/recientes');
  }

  /**
   * Tickets activos de los que el usuario es propietario: sin texto, los 5 más recientes; con texto, los que
   * lo contienen en el asunto (o también en el solicitante si lleva "@").
   */
  mine(q?: string) {
    return this.http.get<TicketReciente[]>('/api/tickets/mios', { params: q ? { q } : {} });
  }

  get(id: number) {
    return this.http.get<Ticket>(`/api/tickets/${id}`);
  }

  history(id: number) {
    return this.http.get<TicketHistoryEntry[]>(`/api/tickets/${id}/history`);
  }

  reply(id: number, text: string) {
    return this.http.post<void>(`/api/tickets/${id}/replies`, { text });
  }

  me() {
    return this.http.get<CurrentUser>('/api/me');
  }
}

/** Mensaje legible de un error HTTP (el backend devuelve application/problem+json). */
export function errorMessage(error: unknown): string {
  if (error instanceof HttpErrorResponse) {
    if (error.status === 0) {
      return 'No se puede conectar con el servidor.';
    }
    if (error.status === 403) {
      return 'No tienes permiso para ver esta información.';
    }
    const detail = (error.error as { detail?: string } | null)?.detail;
    if (detail) {
      return detail;
    }
    return error.status >= 500
      ? `El servidor no está disponible (error ${error.status}). Inténtalo de nuevo en unos momentos.`
      : `Error ${error.status} del servidor.`;
  }
  return 'Error inesperado.';
}
