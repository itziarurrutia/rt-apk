import { DatePipe } from '@angular/common';
import { Component, computed, effect, inject, input, signal, untracked } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatChipsModule } from '@angular/material/chips';
import { MatDialog } from '@angular/material/dialog';
import { MatExpansionModule } from '@angular/material/expansion';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSlideToggleChange, MatSlideToggleModule } from '@angular/material/slide-toggle';
import { MatSnackBar } from '@angular/material/snack-bar';
import { RouterLink } from '@angular/router';
import { Subscription } from 'rxjs';
import { AssetApi, errorMessage } from '../core/api';
import { formatMac, isOnOrAfterToday } from '../core/format';
import { Asset, AssetRequestType, AssetSearchField, PassIz, UltimoDespliegue } from '../core/models';
import { AssetRequestDialog, AssetRequestDialogData } from './asset-request-dialog';

@Component({
  selector: 'app-asset-card',
  imports: [
    DatePipe,
    MatButtonModule,
    MatCardModule,
    MatChipsModule,
    MatExpansionModule,
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
  private readonly dialog = inject(MatDialog);
  private readonly snackBar = inject(MatSnackBar);

  readonly asset = input.required<Asset>();
  /** Campo por el que ha coincidido la búsqueda (ya no se muestra: la ficha enseña tipo y ámbito). */
  readonly matchedBy = input<AssetSearchField>();
  /** Si es true, el título enlaza al detalle del asset. */
  readonly linkToDetail = input(false);

  /** Último despliegue: undefined = sin cargar; null = el equipo no tiene ninguno registrado. */
  protected readonly ajuste = signal<UltimoDespliegue | null | undefined>(undefined);
  protected readonly ajusteLoading = signal(false);
  protected readonly ajusteError = signal<string | null>(null);

  /** Se carga al desplegar la sección (son varias consultas a RT), y solo la primera vez. */
  protected loadAjuste(): void {
    const numeroInventario = this.asset().numeroInventario;
    if (!numeroInventario || this.ajusteLoading() || this.ajuste() !== undefined) {
      return;
    }
    this.ajusteLoading.set(true);
    this.ajusteError.set(null);
    this.api.ultimoDespliegue(numeroInventario).subscribe({
      next: (ajuste) => {
        this.ajuste.set(ajuste ?? null);
        this.ajusteLoading.set(false);
      },
      error: (e) => {
        this.ajusteError.set(errorMessage(e));
        this.ajusteLoading.set(false);
      },
    });
  }

  /** Abre el formulario de avería o formateo; al crear el ticket avisa con su número. */
  protected openRequest(tipo: AssetRequestType): void {
    const numeroInventario = this.asset().numeroInventario;
    if (!numeroInventario) {
      return;
    }
    this.dialog
      .open<AssetRequestDialog, AssetRequestDialogData, number>(AssetRequestDialog, {
        data: { tipo, numeroInventario },
        autoFocus: 'first-tabbable',
      })
      .afterClosed()
      .subscribe((ticketId) => {
        if (ticketId) {
          this.snackBar.open(`Ticket #${ticketId} creado en la cola de averías`, 'Cerrar', {
            duration: 6000,
          });
        }
      });
  }

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
