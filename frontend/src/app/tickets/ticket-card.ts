import { DatePipe } from '@angular/common';
import { Component, input } from '@angular/core';
import { MatCardModule } from '@angular/material/card';
import { RouterLink } from '@angular/router';
import { statusLabel } from '../core/format';
import { TicketReciente } from '../core/models';

/** Destino del botón "volver" del detalle: la lista desde la que se abrió el ticket. */
export interface BackLink {
  path: string;
  label: string;
}

/** Tarjeta de un ticket con solicitante, departamento y último mensaje (vistas Tickets y Tickets recientes). */
@Component({
  selector: 'app-ticket-card',
  imports: [DatePipe, MatCardModule, RouterLink],
  templateUrl: './ticket-card.html',
  styleUrl: './ticket-card.scss',
})
export class TicketCard {
  readonly ticket = input.required<TicketReciente>();
  /** Lista a la que vuelve el detalle del ticket. */
  readonly back = input.required<BackLink>();

  protected readonly statusLabel = statusLabel;
}
