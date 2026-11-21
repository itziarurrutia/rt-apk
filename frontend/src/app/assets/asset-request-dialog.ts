import { Component, inject, signal } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { AssetApi, errorMessage } from '../core/api';
import { AssetRequestType } from '../core/models';

export interface AssetRequestDialogData {
  tipo: AssetRequestType;
  numeroInventario: string;
}

const TITLES: Record<AssetRequestType, string> = {
  AVERIA: 'Reportar avería',
  FORMATEO: 'Solicitar formateo',
};

/** Formulario de avería o formateo de un equipo: crea un ticket en la cola de averías. Devuelve su id. */
@Component({
  selector: 'app-asset-request-dialog',
  imports: [
    ReactiveFormsModule,
    MatButtonModule,
    MatDialogModule,
    MatFormFieldModule,
    MatInputModule,
    MatProgressBarModule,
    MatSlideToggleModule,
  ],
  templateUrl: './asset-request-dialog.html',
  styleUrl: './asset-request-dialog.scss',
})
export class AssetRequestDialog {
  protected readonly data = inject<AssetRequestDialogData>(MAT_DIALOG_DATA);
  private readonly ref = inject<MatDialogRef<AssetRequestDialog, number>>(MatDialogRef);
  private readonly api = inject(AssetApi);

  protected readonly title = TITLES[this.data.tipo];
  protected readonly texto = new FormControl('', {
    nonNullable: true,
    validators: [Validators.required, Validators.maxLength(10_000)],
  });
  /** Inicialmente desactivado. */
  protected readonly datosSalvados = new FormControl(false, { nonNullable: true });
  protected readonly form = new FormGroup({ texto: this.texto, datosSalvados: this.datosSalvados });
  protected readonly sending = signal(false);
  protected readonly error = signal<string | null>(null);

  protected send(): void {
    if (!this.texto.value.trim()) {
      this.texto.setErrors({ required: true });
      this.texto.markAsTouched();
      return;
    }
    if (this.form.invalid || this.sending()) {
      return;
    }
    this.sending.set(true);
    this.error.set(null);
    this.ref.disableClose = true;
    this.api
      .request(this.data.numeroInventario, this.data.tipo, this.texto.value.trim(), this.datosSalvados.value)
      .subscribe({
        next: (created) => this.ref.close(created.ticketId),
        error: (e) => {
          this.sending.set(false);
          this.ref.disableClose = false;
          this.error.set(errorMessage(e));
        },
      });
  }
}
