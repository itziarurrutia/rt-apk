import { Component, computed, inject, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { errorMessage, TicketApi } from '../core/api';
import { updatedSinceLabel } from '../core/format';
import { TicketsRecientes } from '../core/models';
import { BackLink, TicketCard } from './ticket-card';

/** Tickets activos de la cola actualizados recientemente, con el último mensaje de cada uno. */
@Component({
  selector: 'app-ticket-list',
  imports: [MatButtonModule, MatIconModule, MatProgressBarModule, TicketCard],
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
    return d?.actualizadosDesde ? updatedSinceLabel(d.actualizadosDesde) : '';
  });
  protected readonly back: BackLink = { path: '/tickets-recientes', label: 'Tickets recientes' };

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
