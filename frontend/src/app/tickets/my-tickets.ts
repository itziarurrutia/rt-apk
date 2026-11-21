import { Component, inject, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { errorMessage, TicketApi } from '../core/api';
import { TicketReciente } from '../core/models';
import { BackLink, TicketCard } from './ticket-card';

/** Los 5 tickets activos más recientes de los que el usuario es propietario en RT. */
@Component({
  selector: 'app-my-tickets',
  imports: [MatButtonModule, MatIconModule, MatProgressBarModule, TicketCard],
  templateUrl: './my-tickets.html',
  styleUrl: './ticket-list.scss',
})
export class MyTickets {
  private readonly api = inject(TicketApi);

  protected readonly tickets = signal<TicketReciente[] | null>(null);
  protected readonly loading = signal(false);
  protected readonly error = signal<string | null>(null);
  protected readonly back: BackLink = { path: '/tickets', label: 'Tickets' };

  constructor() {
    this.load();
  }

  protected load(): void {
    this.loading.set(true);
    this.error.set(null);
    this.api.mine().subscribe({
      next: (tickets) => {
        this.tickets.set(tickets);
        this.loading.set(false);
      },
      error: (e) => {
        this.error.set(errorMessage(e));
        this.loading.set(false);
      },
    });
  }
}
