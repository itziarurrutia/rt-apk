import { Component, effect, inject, input, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { Location } from '@angular/common';
import { Router } from '@angular/router';
import { AssetApi, errorMessage } from '../core/api';
import { Asset } from '../core/models';
import { AssetCard } from './asset-card';

/** Detalle de un equipo por número de inventario (/assets/:inventario), enlazable directamente. */
@Component({
  selector: 'app-asset-detail',
  imports: [MatButtonModule, MatIconModule, MatProgressBarModule, AssetCard],
  template: `
    <button mat-button (click)="back()"><mat-icon>arrow_back</mat-icon> Volver</button>
    @if (loading()) {
      <mat-progress-bar mode="indeterminate" />
    }
    @if (error(); as message) {
      <p class="error" role="alert">{{ message }}</p>
    }
    @if (asset(); as a) {
      <app-asset-card [asset]="a" />
    }
  `,
  styles: `
    button { margin-bottom: 8px; }
    .error { color: var(--mat-sys-error); }
  `,
})
export class AssetDetail {
  private readonly api = inject(AssetApi);
  private readonly location = inject(Location);
  private readonly router = inject(Router);

  /** Parámetro de ruta (withComponentInputBinding). */
  readonly inventario = input.required<string>();

  protected readonly asset = signal<Asset | null>(null);
  protected readonly loading = signal(false);
  protected readonly error = signal<string | null>(null);

  constructor() {
    effect((onCleanup) => {
      this.loading.set(true);
      this.error.set(null);
      const sub = this.api.get(this.inventario()).subscribe({
        next: (a) => {
          this.asset.set(a);
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

  protected back(): void {
    // Si se ha entrado directamente por URL no hay a dónde volver dentro de la app
    if (window.history.length > 1) {
      this.location.back();
    } else {
      this.router.navigate(['/assets']);
    }
  }
}
