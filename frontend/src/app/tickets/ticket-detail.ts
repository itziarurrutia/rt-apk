import { DatePipe } from '@angular/common';
import { Component, effect, inject, input, signal, viewChild } from '@angular/core';
import {
  FormControl,
  FormGroup,
  FormGroupDirective,
  ReactiveFormsModule,
  Validators,
} from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatSnackBar } from '@angular/material/snack-bar';
import { RouterLink } from '@angular/router';
import { forkJoin } from 'rxjs';
import { errorMessage, TicketApi } from '../core/api';
import { historyLabel, statusLabel } from '../core/format';
import { Ticket, TicketHistoryEntry } from '../core/models';
import { BackLink } from './ticket-card';

@Component({
  selector: 'app-ticket-detail',
  imports: [
    DatePipe,
    ReactiveFormsModule,
    MatButtonModule,
    MatCardModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatProgressBarModule,
    RouterLink,
  ],
  templateUrl: './ticket-detail.html',
  styleUrl: './ticket-detail.scss',
})
export class TicketDetail {
  private readonly api = inject(TicketApi);
  private readonly snackBar = inject(MatSnackBar);

  /** Parámetro de ruta (withComponentInputBinding). */
  readonly id = input.required<string>();

  protected readonly ticket = signal<Ticket | null>(null);
  protected readonly history = signal<TicketHistoryEntry[]>([]);
  protected readonly loading = signal(false);
  protected readonly sending = signal(false);
  protected readonly error = signal<string | null>(null);
  protected readonly reply = new FormControl('', {
    nonNullable: true,
    validators: [Validators.required, Validators.maxLength(10_000)],
  });
  protected readonly form = new FormGroup({ reply: this.reply });
  private readonly formDirective = viewChild(FormGroupDirective);
  protected readonly statusLabel = statusLabel;
  protected readonly historyLabel = historyLabel;
  /** Lista desde la que se abrió (estado de la navegación); por defecto, Tickets. */
  protected readonly back: BackLink =
    (history.state as { back?: BackLink } | null)?.back ?? { path: '/tickets', label: 'Mis tickets' };

  constructor() {
    effect((onCleanup) => {
      const id = Number(this.id());
      this.loading.set(true);
      this.error.set(null);
      const sub = forkJoin({ ticket: this.api.get(id), history: this.api.history(id) }).subscribe({
        next: ({ ticket, history }) => {
          this.ticket.set(ticket);
          this.history.set(history);
          this.loading.set(false);
        },
        error: (e) => {
          this.error.set(errorMessage(e));
          this.loading.set(false);
        },
      });
      onCleanup(() => sub.unsubscribe());
    });
  }

  protected send(): void {
    const text = this.reply.value.trim();
    if (!text) {
      this.reply.markAsTouched();
      return;
    }
    const id = Number(this.id());
    this.sending.set(true);
    this.api.reply(id, text).subscribe({
      next: () => {
        // resetForm() también borra el estado 'enviado', para que no se muestre el error de campo vacío
        this.formDirective()?.resetForm();
        this.sending.set(false);
        this.snackBar.open('Respuesta enviada', undefined, { duration: 3000 });
        this.api.history(id).subscribe((h) => this.history.set(h));
      },
      error: (e) => {
        this.sending.set(false);
        this.snackBar.open(errorMessage(e), 'Cerrar');
      },
    });
  }
}
