import { Component, computed, inject, signal } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatSelectModule } from '@angular/material/select';
import { errorMessage, TicketApi } from '../core/api';
import { QueueOption } from '../core/models';

/** Resultado del formulario: el ticket creado. */
export interface NewTicketResult {
  ticketId: number;
}

/**
 * Formulario de ticket nuevo (botón "+"): cola, asunto, descripción y nº de inventario opcional. El solicitante
 * es siempre el usuario que ha iniciado sesión y el ticket queda sin asignar.
 */
@Component({
  selector: 'app-new-ticket-dialog',
  imports: [
    ReactiveFormsModule,
    MatButtonModule,
    MatDialogModule,
    MatFormFieldModule,
    MatInputModule,
    MatProgressBarModule,
    MatSelectModule,
  ],
  templateUrl: './new-ticket-dialog.html',
  styleUrl: './new-ticket-dialog.scss',
})
export class NewTicketDialog {
  private readonly ref = inject<MatDialogRef<NewTicketDialog, NewTicketResult>>(MatDialogRef);
  private readonly api = inject(TicketApi);

  protected readonly queues = signal<QueueOption[]>([]);
  /** Cola elegida (signal) para mostrar de qué trata. */
  private readonly selectedQueue = signal('');
  protected readonly queueDescription = computed(
    () => this.queues().find((q) => q.name === this.selectedQueue())?.description ?? '',
  );
  protected readonly sending = signal(false);
  protected readonly error = signal<string | null>(null);

  protected readonly cola = new FormControl('', { nonNullable: true, validators: [Validators.required] });
  protected readonly asunto = new FormControl('', {
    nonNullable: true,
    validators: [Validators.required, Validators.maxLength(200)],
  });
  protected readonly texto = new FormControl('', {
    nonNullable: true,
    validators: [Validators.required, Validators.maxLength(10_000)],
  });
  protected readonly inventario = new FormControl('', {
    nonNullable: true,
    validators: [Validators.maxLength(50), Validators.pattern(/^[\p{L}\p{N}._-]*$/u)],
  });
  protected readonly form = new FormGroup({
    cola: this.cola,
    asunto: this.asunto,
    texto: this.texto,
    inventario: this.inventario,
  });

  constructor() {
    this.cola.valueChanges.subscribe((value) => this.selectedQueue.set(value));
    this.api.queues().subscribe({
      next: (queues) => this.queues.set(queues),
      error: (e) => this.error.set(errorMessage(e)),
    });
  }

  protected send(): void {
    for (const control of [this.asunto, this.texto]) {
      if (!control.value.trim()) {
        control.setErrors({ required: true });
      }
    }
    if (this.form.invalid || this.sending()) {
      this.form.markAllAsTouched();
      return;
    }
    this.sending.set(true);
    this.error.set(null);
    this.ref.disableClose = true;
    this.api
      .create({
        cola: this.cola.value,
        asunto: this.asunto.value.trim(),
        texto: this.texto.value.trim(),
        inventario: this.inventario.value.trim() || undefined,
      })
      .subscribe({
        next: (created) => this.ref.close({ ticketId: created.ticketId }),
        error: (e) => {
          this.sending.set(false);
          this.ref.disableClose = false;
          this.error.set(errorMessage(e));
        },
      });
  }
}
