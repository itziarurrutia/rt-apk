import { DatePipe } from '@angular/common';
import { Component, computed, inject, input, output, signal } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatExpansionModule } from '@angular/material/expansion';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { MatSnackBar } from '@angular/material/snack-bar';
import { errorMessage, TicketApi } from '../core/api';
import { statusLabel } from '../core/format';
import { TicketReciente } from '../core/models';

/** Cola cuyos tickets llevan el campo Departamento (NDepartamento). */
const DEPARTMENT_QUEUE = 'apoyo.dpto';

/** Destino del botón "volver" del detalle: la lista desde la que se abrió el ticket. */
export interface BackLink {
  path: string;
  label: string;
  /** Parámetros de la lista (p. ej. la búsqueda ?q=), para volver a ella tal como estaba. */
  queryParams?: Record<string, string>;
}

/** Comentario guardado desde la ficha. */
export interface CommentAdded {
  ticketId: number;
  resolved: boolean;
}

/**
 * Ficha plegable de un ticket (Mis tickets y Tickets recientes): plegada muestra asunto, solicitante y fecha;
 * desplegada, estado, departamento (solo en apoyo.dpto) y el último mensaje. Con canComment, además permite
 * añadir un comentario interno y, opcionalmente, resolver el ticket (en apoyo.dpto, cambiando su Inventario).
 */
@Component({
  selector: 'app-ticket-card',
  imports: [
    DatePipe,
    ReactiveFormsModule,
    MatButtonModule,
    MatExpansionModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatProgressBarModule,
    MatSlideToggleModule,
  ],
  templateUrl: './ticket-card.html',
  styleUrl: './ticket-card.scss',
})
export class TicketCard {
  private readonly api = inject(TicketApi);
  private readonly snackBar = inject(MatSnackBar);

  readonly ticket = input.required<TicketReciente>();
  /** Muestra la acción "Comentar" (tickets de los que el usuario es propietario). */
  readonly canComment = input(false);
  /** Se ha guardado un comentario (y quizá resuelto el ticket): la lista debe recargarse. */
  readonly commented = output<CommentAdded>();

  protected readonly statusLabel = statusLabel;
  /** El departamento solo tiene sentido en la cola de apoyo a departamentos. */
  protected readonly showDepartment = computed(
    () => this.ticket().cola?.toLowerCase() === DEPARTMENT_QUEUE,
  );

  protected readonly commenting = signal(false);
  protected readonly sending = signal(false);
  protected readonly error = signal<string | null>(null);
  protected readonly text = new FormControl('', {
    nonNullable: true,
    validators: [Validators.required, Validators.maxLength(10_000)],
  });
  protected readonly resolve = new FormControl(false, { nonNullable: true });
  /** Inventario del ticket: solo en apoyo.dpto y al resolver. */
  protected readonly inventory = new FormControl('', {
    nonNullable: true,
    validators: [Validators.maxLength(100)],
  });
  protected readonly form = new FormGroup({
    text: this.text,
    resolve: this.resolve,
    inventory: this.inventory,
  });
  /** Valor del switch Resolver como signal (para mostrar u ocultar el campo Inventario). */
  protected readonly resolving = signal(false);
  protected readonly showInventory = computed(() => this.showDepartment() && this.resolving());

  constructor() {
    this.resolve.valueChanges.subscribe((value) => this.resolving.set(value));
  }

  protected startComment(): void {
    this.form.reset({ text: '', resolve: false, inventory: this.ticket().inventario ?? '' });
    this.error.set(null);
    this.commenting.set(true);
  }

  protected cancel(): void {
    this.commenting.set(false);
    this.error.set(null);
  }

  protected send(): void {
    if (!this.text.value.trim()) {
      this.text.setErrors({ required: true });
      this.text.markAsTouched();
      return;
    }
    if (this.form.invalid || this.sending()) {
      return;
    }
    const id = this.ticket().id;
    const resolved = this.resolve.value;
    // El inventario solo se envía al resolver un ticket de apoyo.dpto (el backend lo vuelve a comprobar)
    const inventario = this.showInventory() ? this.inventory.value.trim() : undefined;
    this.sending.set(true);
    this.error.set(null);
    this.api.comment(id, this.text.value.trim(), resolved, inventario).subscribe({
      next: () => {
        this.sending.set(false);
        this.commenting.set(false);
        this.snackBar.open(
          resolved ? `Comentario añadido y ticket #${id} resuelto` : 'Comentario añadido',
          undefined,
          { duration: 3000 },
        );
        this.commented.emit({ ticketId: id, resolved });
      },
      error: (e) => {
        this.sending.set(false);
        this.error.set(errorMessage(e));
      },
    });
  }
}
