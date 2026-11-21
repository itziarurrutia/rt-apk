import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import {
  Asset,
  AssetMatch,
  AssetRequestType,
  CurrentUser,
  MisTickets,
  QueueOption,
  PassIz,
  Ticket,
  TicketHistoryEntry,
  TicketsRecientes,
  UltimoDespliegue,
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

  /** Último despliegue del equipo; null si no tiene ninguno registrado (204). */
  ultimoDespliegue(numeroInventario: string) {
    return this.http.get<UltimoDespliegue | null>(
      `/api/assets/${encodeURIComponent(numeroInventario)}/ultimo-despliegue`,
    );
  }

  /** Crea un ticket de avería o de formateo del equipo en la cola de averías. */
  request(
    numeroInventario: string,
    tipo: AssetRequestType,
    texto: string,
    datosSalvados: boolean,
    sistemaOperativo?: string,
  ) {
    return this.http.post<{ ticketId: number }>(
      `/api/assets/${encodeURIComponent(numeroInventario)}/solicitudes`,
      { tipo, texto, datosSalvados, sistemaOperativo },
    );
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
   * Tickets activos de los que el usuario es propietario: sin texto, los más recientes; con texto, los que
   * lo contienen en el asunto (o también en el solicitante si lleva "@").
   */
  mine(q?: string) {
    return this.http.get<MisTickets>('/api/tickets/mios', { params: q ? { q } : {} });
  }

  /** Colas en las que se puede crear un ticket. */
  queues() {
    return this.http.get<QueueOption[]>('/api/tickets/colas');
  }

  /** Crea un ticket; el solicitante es el usuario que ha iniciado sesión. */
  create(ticket: { cola: string; asunto: string; texto: string; inventario?: string }) {
    return this.http.post<{ ticketId: number }>('/api/tickets', ticket);
  }

  get(id: number) {
    return this.http.get<Ticket>(`/api/tickets/${id}`);
  }

  history(id: number) {
    return this.http.get<TicketHistoryEntry[]>(`/api/tickets/${id}/history`);
  }

  /**
   * Comentario interno; con resolver, además se resuelve el ticket y, si es de apoyo.dpto y se envía otro
   * inventario, se cambia su campo Inventario.
   */
  comment(id: number, texto: string, resolver: boolean, inventario?: string) {
    return this.http.post<void>(`/api/tickets/${id}/comentarios`, { texto, resolver, inventario });
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
