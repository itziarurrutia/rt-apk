import { Component, inject, output } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { MatSnackBar } from '@angular/material/snack-bar';
import { NewTicketDialog, NewTicketResult } from './new-ticket-dialog';

/** Botón flotante "+" (abajo a la derecha) que abre el formulario de ticket nuevo. */
@Component({
  selector: 'app-new-ticket-button',
  imports: [MatButtonModule, MatIconModule],
  template: `
    <button mat-fab class="fab" type="button" aria-label="Nuevo ticket" title="Nuevo ticket" (click)="open()">
      <mat-icon>add</mat-icon>
    </button>
  `,
  styles: `
    .fab {
      position: fixed;
      right: 24px;
      bottom: 24px;
      z-index: 20;
      /* Rojo UPNA sólido, como el resto de acciones principales */
      --mat-fab-container-color: var(--mat-sys-primary);
      --mat-fab-foreground-color: var(--mat-sys-on-primary);
    }

    /* En móvil, encima de la barra de navegación inferior */
    @media (max-width: 599px) {
      .fab {
        right: 16px;
        bottom: calc(80px + env(safe-area-inset-bottom));
      }
    }
  `,
})
export class NewTicketButton {
  private readonly dialog = inject(MatDialog);
  private readonly snackBar = inject(MatSnackBar);

  /** Se ha creado un ticket (la lista puede recargarse). */
  readonly created = output<NewTicketResult>();

  protected open(): void {
    this.dialog
      .open<NewTicketDialog, void, NewTicketResult>(NewTicketDialog, { autoFocus: 'first-tabbable' })
      .afterClosed()
      .subscribe((result) => {
        if (result) {
          this.snackBar.open(`Ticket #${result.ticketId} creado`, 'Cerrar', { duration: 6000 });
          this.created.emit(result);
        }
      });
  }
}
