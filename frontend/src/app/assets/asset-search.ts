import { Component, effect, inject, signal } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { ActivatedRoute, Router } from '@angular/router';
import { map } from 'rxjs';
import { AssetApi, errorMessage } from '../core/api';
import { ASSET_QUERY_PATTERN } from '../core/format';
import { AssetMatch } from '../core/models';
import { AssetCard } from './asset-card';

/**
 * Buscador de equipos con un único campo. El texto se guarda en la URL (?q=) para poder
 * compartir la búsqueda y volver atrás sin perder los resultados.
 */
@Component({
  selector: 'app-asset-search',
  imports: [
    ReactiveFormsModule,
    MatButtonModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatProgressBarModule,
    AssetCard,
  ],
  templateUrl: './asset-search.html',
  styleUrl: './asset-search.scss',
})
export class AssetSearch {
  private readonly api = inject(AssetApi);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);

  protected readonly query = new FormControl('', {
    nonNullable: true,
    validators: [Validators.required, Validators.pattern(ASSET_QUERY_PATTERN)],
  });
  /** El FormGroup hace que (ngSubmit) evite el envío nativo del formulario (recarga de página). */
  protected readonly form = new FormGroup({ query: this.query });

  private readonly urlQuery = toSignal(this.route.queryParamMap.pipe(map((p) => p.get('q')?.trim() ?? '')), {
    initialValue: '',
  });

  protected readonly loading = signal(false);
  protected readonly error = signal<string | null>(null);
  /** null = todavía no se ha buscado nada. */
  protected readonly results = signal<AssetMatch[] | null>(null);
  protected readonly searched = signal('');

  constructor() {
    effect((onCleanup) => {
      const q = this.urlQuery();
      this.query.setValue(q);
      if (!q) {
        this.results.set(null);
        this.error.set(null);
        return;
      }
      this.loading.set(true);
      this.error.set(null);
      const sub = this.api.search(q).subscribe({
        next: (matches) => {
          this.results.set(matches);
          this.searched.set(q);
          this.loading.set(false);
        },
        error: (e) => {
          this.results.set(null);
          this.error.set(errorMessage(e));
          this.loading.set(false);
        },
      });
      onCleanup(() => sub.unsubscribe());
    });
  }

  protected submit(): void {
    this.query.setValue(this.query.value.trim());
    if (this.query.invalid) {
      this.query.markAsTouched();
      return;
    }
    this.router.navigate([], { queryParams: { q: this.query.value } });
  }

  protected clear(): void {
    this.query.reset();
    this.router.navigate([], { queryParams: {} });
  }
}
