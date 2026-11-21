import { DatePipe } from '@angular/common';
import { Component, computed, input } from '@angular/core';
import { MatExpansionModule } from '@angular/material/expansion';
import { statusLabel } from '../core/format';
import { TicketReciente } from '../core/models';

/** Destino del botón "volver" del detalle: la lista desde la que se abrió el ticket. */
/** Cola cuyos tickets llevan el campo Departamento (NDepartamento). */
const DEPARTMENT_QUEUE = 'apoyo.dpto';

export interface BackLink {
  path: string;
  label: string;
  /** Parámetros de la lista (p. ej. la búsqueda ?q=), para volver a ella tal como estaba. */
  queryParams?: Record<string, string>;
}

/**
 * Ficha plegable de un ticket (vistas Tickets y Tickets recientes): plegada muestra asunto, solicitante y
 * fecha; desplegada, estado, departamento (solo en apoyo.dpto) y el último mensaje.
 */
@Component({
  selector: 'app-ticket-card',
  imports: [DatePipe, MatExpansionModule],
  templateUrl: './ticket-card.html',
  styleUrl: './ticket-card.scss',
})
export class TicketCard {
  readonly ticket = input.required<TicketReciente>();

  protected readonly statusLabel = statusLabel;
  /** El departamento solo tiene sentido en la cola de apoyo a departamentos. */
  protected readonly showDepartment = computed(
    () => this.ticket().cola?.toLowerCase() === DEPARTMENT_QUEUE,
  );
}
