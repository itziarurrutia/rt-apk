import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { Injectable, inject, signal } from '@angular/core';
import Keycloak from 'keycloak-js';
import { catchError, from, switchMap, throwError } from 'rxjs';

/** Respuesta de GET /api/config (pública). */
export interface PublicConfig {
  securityEnabled: boolean;
  issuer: string | null;
  clientId: string | null;
}

/** "https://kc.example.org/realms/rt-mobile" -> { url: "https://kc.example.org", realm: "rt-mobile" } */
export function splitIssuer(issuer: string): { url: string; realm: string } {
  const match = /^(.*)\/realms\/([^/]+)\/?$/.exec(issuer.trim());
  if (!match) {
    throw new Error(`URL de Keycloak no válida: ${issuer}`);
  }
  return { url: match[1], realm: decodeURIComponent(match[2]) };
}

/**
 * Login con Keycloak (OIDC, Authorization Code + PKCE S256) usando el adaptador oficial keycloak-js.
 * Keycloak solo controla el acceso a la aplicación: el backend consulta RT con su propia cuenta.
 *
 * Al arrancar se lee /api/config: si el backend tiene la seguridad desactivada (modo demo) no se pide login.
 */
@Injectable({ providedIn: 'root' })
export class AuthService {
  private keycloak: Keycloak | null = null;

  /** true si la aplicación exige login (y el usuario ya lo ha hecho). */
  readonly enabled = signal(false);
  readonly username = signal<string | null>(null);
  readonly displayName = signal<string | null>(null);

  async init(): Promise<void> {
    let config: PublicConfig;
    try {
      // fetch y no HttpClient: esta llamada no debe pasar por el interceptor del token
      const response = await fetch('/api/config');
      if (!response.ok) {
        throw new Error(`HTTP ${response.status}`);
      }
      config = (await response.json()) as PublicConfig;
    } catch (e) {
      // Sin backend no hay nada que proteger todavía; las pantallas mostrarán el error de conexión
      console.error('No se pudo leer /api/config', e);
      return;
    }
    if (!config.securityEnabled || !config.issuer || !config.clientId) {
      return;
    }

    const { url, realm } = splitIssuer(config.issuer);
    this.keycloak = new Keycloak({ url, realm, clientId: config.clientId });
    // login-required: si no hay sesión, redirige a la página de login de Keycloak y vuelve aquí
    await this.keycloak.init({ onLoad: 'login-required', pkceMethod: 'S256', checkLoginIframe: false });

    const claims = this.keycloak.tokenParsed as { preferred_username?: string; name?: string } | undefined;
    this.username.set(claims?.preferred_username ?? null);
    this.displayName.set(claims?.name ?? claims?.preferred_username ?? null);
    this.enabled.set(true);
  }

  /** Access token vigente; lo renueva si caduca en menos de 30 s. */
  async token(): Promise<string | null> {
    if (!this.keycloak) {
      return null;
    }
    try {
      await this.keycloak.updateToken(30);
    } catch {
      // La sesión de Keycloak ha caducado: volver a pedir login
      await this.login();
    }
    return this.keycloak.token ?? null;
  }

  login(): Promise<void> {
    return this.keycloak ? this.keycloak.login() : Promise.resolve();
  }

  logout(): Promise<void> {
    return this.keycloak ? this.keycloak.logout({ redirectUri: window.location.origin }) : Promise.resolve();
  }
}

/** Añade "Authorization: Bearer <token>" a las llamadas a /api y pide login de nuevo si el backend responde 401. */
export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const auth = inject(AuthService);
  if (!auth.enabled() || !req.url.startsWith('/api/') || req.url === '/api/config') {
    return next(req);
  }
  return from(auth.token()).pipe(
    switchMap((token) =>
      next(token ? req.clone({ setHeaders: { Authorization: `Bearer ${token}` } }) : req),
    ),
    catchError((error: unknown) => {
      if (error instanceof HttpErrorResponse && error.status === 401) {
        void auth.login();
      }
      return throwError(() => error);
    }),
  );
};
