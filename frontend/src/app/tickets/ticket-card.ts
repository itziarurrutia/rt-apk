import { DatePipe } from '@angular/common';
import { Component, input } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatExpansionModule } from '@angular/material/expansion';
import { MatIconModule } from '@angular/material/icon';
import { RouterLink } from '@angular/router';
import { statusLabel } from '../core/format';
import { TicketReciente } from '../core/models';

/** Destino del botón "volver" del detalle: la lista desde la que se abrió el ticket. */
export interface BackLink {
  path: string;
  label: string;
  /** Parámetros de la lista (p. ej. la búsqueda ?q=), para volver a ella tal como estaba. */
  queryParams?: Record<string, string>;
}

/**
 * Ficha plegable de un ticket (vistas Tickets y Tickets recientes): plegada muestra asunto, solicitante y
 * fecha; desplegada, estado, departamento, el último mensaje y el enlace al detalle.
 */
@Component({
  selector: 'app-ticket-card',
  imports: [DatePipe, MatButtonModule, MatExpansionModule, MatIconModule, RouterLink],
  templateUrl: './ticket-card.html',
  styleUrl: './ticket-card.scss',
})
export class TicketCard {
  readonly ticket = input.required<TicketReciente>();
  /** Lista a la que vuelve el detalle del ticket. */
  readonly back = input.required<BackLink>();

  protected readonly statusLabel = statusLabel;
}
