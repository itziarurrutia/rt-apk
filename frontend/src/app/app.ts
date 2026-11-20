import { Component, inject } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatMenuModule } from '@angular/material/menu';
import { MatToolbarModule } from '@angular/material/toolbar';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';

import { AuthService } from './core/auth';

interface NavItem {
  path: string;
  label: string;
  icon: string;
}

/** Estructura: barra superior con navegación en escritorio y barra inferior en móvil. */
@Component({
  selector: 'app-root',
  imports: [MatButtonModule, MatIconModule, MatMenuModule, MatToolbarModule, RouterLink, RouterLinkActive, RouterOutlet],
  templateUrl: './app.html',
  styleUrl: './app.scss',
})
export class App {
  protected readonly auth = inject(AuthService);
  protected readonly nav: NavItem[] = [
    { path: '/assets', label: 'Equipos', icon: 'computer' },
    { path: '/tickets', label: 'Tickets', icon: 'confirmation_number' },
  ];
}
