# Despliegue con Podman

En el servidor, con un usuario sin privilegios (Podman rootless + Quadlet/systemd). El pod `rt-apk`
tiene dos contenedores que comparten red:

| Contenedor | Imagen | Puerto | Qué hace |
|---|---|---|---|
| `rt-apk-web` | `rt-apk-web` (desde `frontend/`) | 80 → host 8080 | Sirve el frontend Angular y redirige `/api` al backend |
| `rt-apk-backend` | `rt-apk-backend` (desde `backend/`) | 8085 → host 8085 | API Spring Boot |

## 1. Imágenes

```bash
podman build -t rt-apk-backend:latest backend/
podman build -t rt-apk-web:latest frontend/
```

## 2. Configuración

```bash
mkdir -p ~/.config/rt-apk ~/.config/containers/systemd
cp deploy/backend.env.example ~/.config/rt-apk/backend.env
```

Para empezar, `backend.env` trae `SPRING_PROFILES_ACTIVE=demo`: **sin autenticación y sin RT**, con datos
de ejemplo en memoria. No lo dejéis así en un servidor accesible desde fuera.

## 3. Arranque con Quadlet

```bash
cp deploy/quadlet/* ~/.config/containers/systemd/
systemctl --user daemon-reload
systemctl --user start rt-apk-pod
loginctl enable-linger "$USER"   # que arranque sin sesión abierta
```

Comprobación:

```bash
podman ps --pod
curl http://localhost:8085/api/assets?q=78763       # backend directo
curl http://localhost:8080/api/assets?q=78763       # a través de nginx
```

La web queda en `http://servidor:8080`. La terminación TLS (HTTPS) la hace el proxy o balanceador que
tengáis delante.

## Actualizar

```bash
podman build -t rt-apk-backend:latest backend/ && systemctl --user restart rt-apk-backend
podman build -t rt-apk-web:latest frontend/ && systemctl --user restart rt-apk-web
```

## Pasar a RT y Keycloak reales

1. En `backend.env` quita `SPRING_PROFILES_ACTIVE=demo` y rellena las variables comentadas.
2. Crea el secret con la contraseña de la cuenta de servicio de RT y descomenta las dos líneas
   `Secret=`/`Environment=` de `rt-apk-backend.container`:
   ```bash
   printf '%s' 'CONTRASEÑA_DE_RT' | podman secret create rt-apk-rt-password -
   ```
3. Keycloak: crea el realm `rt-apk` y dentro:
   - **Client `rt-web`** (frontend web y, más adelante, la APK): Client authentication **Off** (público),
     Standard flow **On**, Direct access grants **Off**, PKCE **S256**,
     Valid redirect URIs `https://tu-dominio/*` y `http://localhost:4200/*`, Web origins equivalentes.
   - En `rt-web-dedicated` añade un mapper **Audience** con custom audience `rt-api` en el access token.
   - **Realm role `rt-agent`** para los técnicos que deben ver todos los tickets.
   - El access token debe llevar `email` y coincidir con el email del usuario en RT.
4. En el frontend hay que añadir el login (pendiente: librería OIDC + interceptor del token).
