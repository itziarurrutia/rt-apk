# rt-apk

Portal web responsive (y, más adelante, app Android) para consultar equipos (assets) y tickets de
Request Tracker (RT), con autenticación de Keycloak.

```
Navegador / APK ──OIDC + PKCE──▶ Keycloak (realm rt-apk)          [pendiente]
      │ Bearer JWT
      ▼
nginx :80 (pod Podman) ──/api──▶ backend Spring Boot :8085 ──usuario/contraseña──▶ RT (texto plano REST 1.0)
```

**Fase actual:** perfil `demo`, sin Keycloak y sin RT, con datos de ejemplo en memoria.

## Estructura

| Carpeta | Contenido |
|---|---|
| `backend/` | API REST Spring Boot 4 (Java 21) y su `Containerfile` |
| `frontend/` | Angular 21 + Angular Material, responsive, y su `Containerfile` (nginx) |
| `deploy/` | Pod Podman con Quadlet y pasos de Keycloak ([deploy/README.md](deploy/README.md)) |

## Desarrollo en local

Backend en modo demo (puerto 8085):

```bash
cd backend
./mvnw spring-boot:run -Dspring-boot.run.profiles=demo
```

Frontend (puerto 4200, redirige `/api` a `localhost:8085` con `proxy.conf.json`):

```bash
cd frontend
npm install
npm start
```

Abre http://localhost:4200. Datos demo para probar el buscador de equipos:

| Buscar | Resultado |
|---|---|
| `78763` | Coincide por inventario con un equipo y por nº de serie con otro |
| `carlos.lopez` | Los dos equipos de ese usuario de Windows |
| `PF4XK21B` | Nº de serie alfanumérico |
| `65012` | Equipo fuera de garantía y sin usuario |

> **Windows:** si los tests del backend fallan con `Unable to establish loopback connection`, el JDK no
> puede crear su socket interno en la carpeta temporal del usuario. Indícale otra carpeta:
> `./mvnw test "-DargLine=-Djdk.net.unixdomain.tmpdir=E:/tmp"` (o define
> `JAVA_TOOL_OPTIONS=-Djdk.net.unixdomain.tmpdir=E:/tmp`).

## Backend

### Diseño

- Puertos (interfaces) de acceso a RT; el resto de la aplicación solo los conoce a ellos:
  - `rt/AssetGateway`: equipos. `Rest1AssetGateway` usa el servicio de assets de RT
    (`rt.assets-path`): `/<inventario>`, `/<serie>/ns` y `/<usuario windows>/usuario`.
  - `rt/RtGateway`: tickets. `Rest1RtGateway` (REST 1.0) y `Rest2RtGateway` (vacío, para la migración).
  - `rt/demo`: implementaciones en memoria con los datos de `src/main/resources/demo/`.
  - `RT_API_VERSION=rest1|rest2|demo` elige la implementación.
- `rt/rest1/Rest1Client`: login con usuario/contraseña, cookie de sesión (repite el login si caduca).
- `rt/rest1/Rest1ResponseParser`: parser del texto plano de RT (estado, bloques `--`, multilínea, `CF.{...}`).
- `service/AssetService`: búsqueda con un único texto. Si es numérico se busca como nº de inventario y
  nº de serie; si no, como usuario de Windows y nº de serie. Se juntan los resultados indicando por qué
  campo ha coincidido cada uno.
- `service/TicketService`: reglas de acceso a tickets (un usuario normal solo ve los suyos y sin
  comentarios internos; el rol `rt-agent` lo ve todo).
- `security/`: resource server OAuth2 (Keycloak). Con `APP_SECURITY_ENABLED=false` (perfil demo) la API
  queda abierta y se usa un usuario fijo.

### API

| Método | Ruta | Descripción |
|---|---|---|
| GET | `/api/assets?q=78763` | Busca equipos por nº de inventario, nº de serie o usuario de Windows |
| GET | `/api/assets/{inventario}` | Detalle de un equipo |
| GET | `/api/assets/{inventario}/pass-iz` | `{"passIz", "fragmento"}` de `<RT>/REST/1.0/fecha_p/<inventario>` (`RT_FECHA_P_PATH`); `fragmento` = caracteres 10 a 18. Solo rol `rt-agent`, `Cache-Control: no-store`. En la web: switch junto a Fecha P y ojo que, mantenido pulsado, muestra el fragmento |
| GET | `/api/tickets/recientes` | Tickets activos de `APP_TICKETS_QUEUE` (`apoyo.dpto`) actualizados en `APP_TICKETS_UPDATED_SINCE` (`5 hours ago`), con solicitante, departamento y último mensaje. Solo rol `rt-agent` |
| GET | `/api/me` | Usuario actual |
| GET | `/api/tickets?status=open&q=texto` | Búsqueda de tickets |
| GET | `/api/tickets/{id}` | Detalle |
| GET | `/api/tickets/{id}/history` | Historial |
| POST | `/api/tickets/{id}/replies` `{"text": "..."}` | Responder |

Campos de un equipo: `numeroInventario`, `numeroSerie`, `marca`, `modelo`, `sufijoDns`, `ipEth0`,
`macEth0`, `fechaCompra`, `garantia` (fechas ISO `yyyy-MM-dd`), `fechaP` (texto tal cual, puede llevar `*`)
y `usuarioWindows`.

Los errores devuelven `application/problem+json` (400 búsqueda no válida, 404 no encontrado, 502 RT no
disponible).

### Pendiente de validar con el RT real

- Vista de tickets: por cada ticket de `search/ticket` se hacen 3 consultas (`ticket/<id>/show`,
  `ticket/<id>/attachments` y `attachment/<id>` del text/plain con mayor id), hasta 6 tickets en paralelo.
  Con muchos tickets puede tardar; si hace falta se puede cachear unos segundos.
- Las fechas ISO de `ticket/<id>` se interpretan en `RT_TIME_ZONE` (en vuestro ejemplo LastUpdated
  09:07:50 local frente a 08:07:50 UTC del adjunto).

- Si `/usuario` puede devolver varios equipos separados por `--` (está soportado). Sin coincidencias el
  servicio responde `# Error: No existe asset` y se trata como búsqueda vacía; cualquier otro
  `# Error: ...` se devuelve como 502 (RT no disponible).
- Si `<url_rt_rest>` del servicio de assets es `/REST/1.0/assets` (configurable con `RT_ASSETS_PATH`) y
  si usa la misma sesión que la API REST 1.0.
- Los fixtures de tickets de `backend/src/test/resources/rt/rest1/` son sintéticos (el de assets es el
  vuestro).
