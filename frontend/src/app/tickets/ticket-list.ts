import { DatePipe } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { RouterLink } from '@angular/router';
import { errorMessage, TicketApi } from '../core/api';
import { statusLabel, updatedSinceLabel } from '../core/format';
import { TicketsRecientes } from '../core/models';

/** Tickets activos de la cola actualizados recientemente, con el último mensaje de cada uno. */
@Component({
  selector: 'app-ticket-list',
  imports: [
    DatePipe,
    MatButtonModule,
    MatCardModule,
    MatIconModule,
    MatProgressBarModule,
    RouterLink,
  ],
  templateUrl: './ticket-list.html',
  styleUrl: './ticket-list.scss',
})
export class TicketList {
  private readonly api = inject(TicketApi);

  protected readonly data = signal<TicketsRecientes | null>(null);
  protected readonly loading = signal(false);
  protected readonly error = signal<string | null>(null);
  protected readonly since = computed(() => {
    const d = this.data();
    return d ? updatedSinceLabel(d.actualizadosDesde) : '';
  });
  protected readonly statusLabel = statusLabel;

  constructor() {
    this.load();
  }

  protected load(): void {
    this.loading.set(true);
    this.error.set(null);
    this.api.recent().subscribe({
      next: (d) => {
        this.data.set(d);
        this.loading.set(false);
      },
      error: (e) => {
        this.error.set(errorMessage(e));
        this.loading.set(false);
      },
    });
  }
}
