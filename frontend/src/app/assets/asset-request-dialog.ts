import { Component, inject, signal } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { AssetApi, errorMessage } from '../core/api';
import { AssetRequestType } from '../core/models';

export interface AssetRequestDialogData {
  tipo: AssetRequestType;
  numeroInventario: string;
  procesador: string | null;
  sistemaOperativo: string | null;
  compatibleWindows11: boolean;
}

/** Sistemas operativos que se pueden pedir en un formateo. */
export const OPERATING_SYSTEMS = ['Windows 10', 'Windows 11'] as const;

function currentOs(data: AssetRequestDialogData): string {
  return (data.sistemaOperativo ?? '').toLowerCase().replace(/\s+/g, ' ').trim();
}

/** El campo Procesador del equipo está vacío: no se sabe si admite Windows 11. */
export function processorUnknown(data: AssetRequestDialogData): boolean {
  return !data.procesador?.trim();
}

/**
 * Puede llevar Windows 11 si el procesador es compatible, si no se conoce el procesador (se deja elegir) o si
 * ya lo tiene instalado. Solo se descarta con un procesador conocido y no compatible.
 */
export function canRunWindows11(data: AssetRequestDialogData): boolean {
  return data.compatibleWindows11 || processorUnknown(data) || currentOs(data).startsWith('windows 11');
}

/** Opciones del formateo: Windows 11 solo si el equipo puede llevarlo. */
export function availableOperatingSystems(data: AssetRequestDialogData): readonly string[] {
  return canRunWindows11(data) ? OPERATING_SYSTEMS : OPERATING_SYSTEMS.filter((os) => os !== 'Windows 11');
}

/**
 * Sistema operativo propuesto para el formateo: Windows 11 si el procesador es compatible; si no, el que ya
 * tiene el equipo si está entre las opciones ("Windows 10 Pro" cuenta como Windows 10). Si no, ninguno.
 */
export function suggestedOperatingSystem(data: AssetRequestDialogData): string {
  if (data.compatibleWindows11) {
    return 'Windows 11';
  }
  const current = currentOs(data);
  return availableOperatingSystems(data).find((os) => current.startsWith(os.toLowerCase())) ?? '';
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
    MatSelectModule,
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
  protected readonly isFormat = this.data.tipo === 'FORMATEO';
  protected readonly operatingSystems = availableOperatingSystems(this.data);
  protected readonly canRunWindows11 = canRunWindows11(this.data);
  protected readonly processorUnknown = processorUnknown(this.data);
  /** Solo en el formateo (obligatorio); propuesto según el procesador y el sistema actual. */
  protected readonly sistemaOperativo = new FormControl(suggestedOperatingSystem(this.data), {
    nonNullable: true,
    validators: this.isFormat ? [Validators.required] : [],
  });
  protected readonly form = new FormGroup({
    texto: this.texto,
    datosSalvados: this.datosSalvados,
    sistemaOperativo: this.sistemaOperativo,
  });
  protected readonly sending = signal(false);
  protected readonly error = signal<string | null>(null);

  protected send(): void {
    if (!this.texto.value.trim()) {
      this.texto.setErrors({ required: true });
      this.texto.markAsTouched();
      return;
    }
    if (this.form.invalid || this.sending()) {
      this.form.markAllAsTouched();
      return;
    }
    this.sending.set(true);
    this.error.set(null);
    this.ref.disableClose = true;
    this.api
      .request(
        this.data.numeroInventario,
        this.data.tipo,
        this.texto.value.trim(),
        this.datosSalvados.value,
        this.isFormat ? this.sistemaOperativo.value : undefined,
      )
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
