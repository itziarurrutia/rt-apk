#!/usr/bin/env bash
#
# Despliega una nueva versión de rt-apk en el contenedor rt-mobile (puerto 8443 de la máquina, 8085 dentro del contenedor).
#
# Se ejecuta desde la carpeta de despliegue, junto a:
#   Dockerfile, application-security.yml, baobab2.unavarra.es.jks, cacerts
#   y uno o dos JAR:
#     - dos: el que está corriendo (el que nombra el Dockerfile) y el nuevo
#     - uno: solo el nuevo (primer despliegue, o el anterior ya se retiró)
#
# Con dos JAR: copia de seguridad del actual (y del Dockerfile) en backups/<fecha>/, el Dockerfile pasa
# a usar el nuevo, se borra el JAR actual, se reconstruye la imagen y se vuelve a crear el contenedor.
# La imagen anterior queda etiquetada como rt-mobile:anterior para poder volver atrás.
#
#   ./deploy.sh
#
set -Eeuo pipefail

CONTAINER=rt-mobile
IMAGE=rt-mobile
HOST_PORT=8443
APP_PORT=8085
START_TIMEOUT=120   # segundos esperando a que arranque Spring Boot

cd "$(dirname "$(readlink -f "$0")")"

log()  { printf '\n==> %s\n' "$*"; }
fail() { printf '\nERROR: %s\n' "$*" >&2; exit 1; }

BACKUP_DIR=""
on_error() {
    printf '\nERROR: el despliegue se ha detenido (línea %s).\n' "$1" >&2
    if [[ -n "$BACKUP_DIR" ]]; then
        printf 'Para dejarlo como estaba:\n  cp %s/* .\n' "$BACKUP_DIR" >&2
    fi
    printf 'Si el contenedor ya se había borrado, la imagen anterior es %s:anterior:\n' "$IMAGE" >&2
    printf '  podman run -d --name %s -p %s:%s --restart=always %s:anterior\n' \
        "$CONTAINER" "$HOST_PORT" "$APP_PORT" "$IMAGE" >&2
}
trap 'on_error $LINENO' ERR

# ---------- Comprobaciones previas ----------
command -v podman >/dev/null || fail "podman no está instalado"
for f in Dockerfile application-security.yml baobab2.unavarra.es.jks cacerts; do
    [[ -f "$f" ]] || fail "falta $f en $(pwd)"
done

shopt -s nullglob
jars=(*.jar)
shopt -u nullglob

# JAR que usa ahora el Dockerfile (línea COPY ... .jar)
current=$(grep -E '^[[:space:]]*COPY[[:space:]]+[^[:space:]]+\.jar' Dockerfile | head -n1 | awk '{print $2}' || true)

case ${#jars[@]} in
    0)
        fail "no hay ningún .jar en $(pwd)"
        ;;
    1)
        new=${jars[0]}
        if [[ "$new" == "$current" ]]; then
            log "Solo está $new, que ya es el del Dockerfile: se reconstruye con él"
        fi
        old=""
        ;;
    2)
        if [[ -n "$current" && ( "${jars[0]}" == "$current" || "${jars[1]}" == "$current" ) ]]; then
            old=$current
            [[ "${jars[0]}" == "$current" ]] && new=${jars[1]} || new=${jars[0]}
        else
            # El Dockerfile no nombra ninguno de los dos: el más reciente es el nuevo
            new=$(ls -1t -- "${jars[@]}" | head -n1)
            old=$(ls -1t -- "${jars[@]}" | tail -n1)
            log "El Dockerfile no nombra ninguno de los JAR; se toma el más reciente como nuevo"
        fi
        ;;
    *)
        fail "hay ${#jars[@]} JAR en $(pwd); debe haber uno (el nuevo) o dos (el actual y el nuevo)"
        ;;
esac

log "JAR actual: ${old:-ninguno}"
log "JAR nuevo:  $new"

# ---------- Copia de seguridad del JAR que está corriendo ----------
if [[ -n "$old" ]]; then
    BACKUP_DIR="backups/$(date +%Y%m%d-%H%M%S)"
    log "Copia de seguridad en $BACKUP_DIR"
    mkdir -p "$BACKUP_DIR"
    cp -p -- "$old" Dockerfile "$BACKUP_DIR/"
fi

# ---------- Dockerfile con el JAR nuevo ----------
log "Dockerfile: ${current:-?} -> $new"
# Sustituye el nombre del JAR en las líneas COPY y ENTRYPOINT (no en los comentarios)
new_escaped=$(printf '%s' "$new" | sed 's/[&/\]/\\&/g')
sed -i -E "/^[[:space:]]*#/!s/[A-Za-z0-9._+-]+\.jar/${new_escaped}/g" Dockerfile
grep -nE '\.jar' Dockerfile | grep -vE '^[0-9]+:[[:space:]]*#' || true

# ---------- Borrar el JAR que estaba corriendo ----------
if [[ -n "$old" ]]; then
    log "Borrando $old"
    rm -f -- "$old"
fi

# ---------- Reconstruir la imagen ----------
if podman image exists "$IMAGE:latest"; then
    podman tag "$IMAGE:latest" "$IMAGE:anterior"
fi
log "Construyendo la imagen $IMAGE:latest"
podman build -t "$IMAGE:latest" .

# ---------- Volver a crear el contenedor ----------
if podman container exists "$CONTAINER"; then
    log "Borrando el contenedor $CONTAINER"
    podman rm -f "$CONTAINER"
fi
log "Creando el contenedor $CONTAINER (puerto $HOST_PORT)"
podman run -d --name "$CONTAINER" -p "$HOST_PORT:$APP_PORT" --restart=always "$IMAGE:latest"

# ---------- Esperar al arranque ----------
log "Esperando a que arranque (máx. ${START_TIMEOUT}s)"
for ((i = 0; i < START_TIMEOUT; i += 2)); do
    if podman logs "$CONTAINER" 2>&1 | grep -q 'Started RtApkApplication'; then
        podman logs "$CONTAINER" 2>&1 | grep -E 'Started RtApkApplication|Tomcat started' | tail -n2
        log "Desplegado $new en $CONTAINER (puerto $HOST_PORT)"
        exit 0
    fi
    if [[ "$(podman inspect -f '{{.State.Running}}' "$CONTAINER" 2>/dev/null)" != "true" ]]; then
        podman logs --tail 40 "$CONTAINER" >&2 || true
        fail "el contenedor $CONTAINER se ha parado al arrancar"
    fi
    sleep 2
done

podman logs --tail 40 "$CONTAINER" >&2 || true
fail "$CONTAINER no ha terminado de arrancar en ${START_TIMEOUT}s (revisa: podman logs -f $CONTAINER)"
