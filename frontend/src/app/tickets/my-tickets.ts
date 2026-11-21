import { Component, computed, effect, inject, input, signal, untracked } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { Router } from '@angular/router';
import { Subscription } from 'rxjs';
import { errorMessage, TicketApi } from '../core/api';
import { MisTickets } from '../core/models';
import { NewTicketButton } from './new-ticket-button';
import { TicketCard } from './ticket-card';

/**
 * Tickets activos de los que el usuario es propietario en RT: sin búsqueda, los más recientes; con
 * búsqueda (?q=), los que contienen el texto en el asunto o, si lleva "@", también en el solicitante.
 */
@Component({
  selector: 'app-my-tickets',
  imports: [
    ReactiveFormsModule,
    MatButtonModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatProgressBarModule,
    TicketCard,
    NewTicketButton,
  ],
  templateUrl: './my-tickets.html',
  styleUrl: './my-tickets.scss',
})
export class MyTickets {
  private readonly api = inject(TicketApi);
  private readonly router = inject(Router);

  /** Texto buscado (parámetro ?q= de la ruta, withComponentInputBinding). */
  readonly q = input<string | undefined>();

  protected readonly query = new FormControl('', {
    nonNullable: true,
    validators: [Validators.maxLength(200)],
  });
  protected readonly form = new FormGroup({ query: this.query });
  protected readonly data = signal<MisTickets | null>(null);
  protected readonly loading = signal(false);
  protected readonly error = signal<string | null>(null);
  protected readonly searched = computed(() => this.q()?.trim() ?? '');
  private request?: Subscription;

  constructor() {
    effect(() => {
      const q = this.searched();
      untracked(() => {
        this.query.setValue(q);
        this.load(q);
      });
    });
  }

  protected submit(): void {
    if (this.query.invalid) {
      return;
    }
    const q = this.query.value.trim();
    if (q === this.searched()) {
      this.load(q);
    } else {
      this.router.navigate(['/tickets'], { queryParams: q ? { q } : {} });
    }
  }

  protected clear(): void {
    this.query.setValue('');
    this.router.navigate(['/tickets']);
  }

  protected reload(): void {
    this.load(this.searched());
  }

  private load(q: string): void {
    this.request?.unsubscribe();
    this.loading.set(true);
    this.error.set(null);
    this.request = this.api.mine(q || undefined).subscribe({
      next: (data) => {
        this.data.set(data);
        this.loading.set(false);
      },
      error: (e) => {
        this.data.set(null);
        this.error.set(errorMessage(e));
        this.loading.set(false);
      },
    });
  }
}
