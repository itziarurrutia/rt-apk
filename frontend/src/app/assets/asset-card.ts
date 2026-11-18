import { DatePipe } from '@angular/common';
import { Component, computed, effect, inject, input, signal, untracked } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatChipsModule } from '@angular/material/chips';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSlideToggleChange, MatSlideToggleModule } from '@angular/material/slide-toggle';
import { RouterLink } from '@angular/router';
import { Subscription } from 'rxjs';
import { AssetApi, errorMessage } from '../core/api';
import { formatMac, isOnOrAfterToday, MATCHED_BY_LABEL } from '../core/format';
import { Asset, AssetSearchField, PassIz } from '../core/models';

@Component({
  selector: 'app-asset-card',
  imports: [
    DatePipe,
    MatButtonModule,
    MatCardModule,
    MatChipsModule,
    MatIconModule,
    MatProgressSpinnerModule,
    MatSlideToggleModule,
    RouterLink,
  ],
  templateUrl: './asset-card.html',
  styleUrl: './asset-card.scss',
})
export class AssetCard {
  private readonly api = inject(AssetApi);

  readonly asset = input.required<Asset>();
  /** Campo por el que ha coincidido la búsqueda; no se muestra en el detalle. */
  readonly matchedBy = input<AssetSearchField>();
  /** Si es true, el título enlaza al detalle del asset. */
  readonly linkToDetail = input(false);

  protected readonly matchedByLabel = computed(() => {
    const field = this.matchedBy();
    return field ? MATCHED_BY_LABEL[field] : null;
  });
  protected readonly title = computed(() =>
    [this.asset().marca, this.asset().modelo].filter(Boolean).join(' ') || 'Equipo sin marca/modelo',
  );
  protected readonly mac = computed(() => formatMac(this.asset().macEth0));
  protected readonly inWarranty = computed(() => isOnOrAfterToday(this.asset().garantia));

  // Pass_iz: el switch lo pide a RT y lo muestra; mientras se mantiene pulsado el ojo se ve el fragmento
  protected readonly passOn = signal(false);
  protected readonly pass = signal<PassIz | null>(null);
  protected readonly passLoading = signal(false);
  protected readonly passError = signal<string | null>(null);
  protected readonly showFragment = signal(false);
  protected readonly passText = computed(() => {
    const pass = this.pass();
    if (!pass) {
      return '';
    }
    return this.showFragment() ? pass.fragmento : pass.passIz;
  });
  private passRequest?: Subscription;

  constructor() {
    // Si la tarjeta pasa a mostrar otro equipo, no se debe ver el dato del anterior
    effect(() => {
      this.asset();
      untracked(() => this.clearPass());
    });
  }

  protected togglePass(event: MatSlideToggleChange): void {
    if (!event.checked) {
      this.clearPass();
      return;
    }
    const inventario = this.asset().numeroInventario;
    if (!inventario) {
      return;
    }
    this.passOn.set(true);
    this.passLoading.set(true);
    this.passError.set(null);
    this.passRequest = this.api.passIz(inventario).subscribe({
      next: (p) => {
        this.pass.set(p);
        this.passLoading.set(false);
      },
      error: (e) => {
        this.passError.set(errorMessage(e));
        this.passLoading.set(false);
      },
    });
  }

  /** Ojo pulsado (ratón, dedo o teclado): se ve el fragmento hasta que se suelta. */
  protected pressEye(event: Event): void {
    event.preventDefault();
    this.showFragment.set(true);
  }

  protected releaseEye(): void {
    this.showFragment.set(false);
  }

  private clearPass(): void {
    this.passRequest?.unsubscribe();
    this.passOn.set(false);
    this.pass.set(null);
    this.passLoading.set(false);
    this.passError.set(null);
    this.showFragment.set(false);
  }
}
