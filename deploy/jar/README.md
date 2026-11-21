# Contenedor a partir del JAR

Imagen con el JAR ya construido: la web y la API van en el mismo proceso, en el puerto 8085 del contenedor, publicado en el 8443 de baobab2.
Esta opción es distinta del despliegue con dos contenedores de [deploy/README.md](../README.md),
que usa nginx para la web.

## 1. Construir el JAR

```bash
cd backend
./mvnw -Pfrontend package
```

## 2. Preparar la carpeta de construcción

En el servidor, en una carpeta cualquiera, por ejemplo `~/rt-apk-build`:

| Fichero | De dónde sale |
|---|---|
| `Dockerfile` | `deploy/jar/Dockerfile` |
| `rt-apk-backend-<versión>.jar` | `backend/target/` |
| `application-security.yml` | Tu fichero con datos reales (ver `backend/application-security.yml.example`) |
| `baobab2.unavarra.es.jks` | Certificado del servidor para HTTPS |
| `cacerts` | Almacén de confianza de Java, con contraseña `changeit` (por ejemplo, el del JDK: `$JAVA_HOME/lib/security/cacerts`) |

`application-security.yml` lleva la contraseña de RT y la del `.jks`, y quedan dentro de la imagen.
No subas la imagen a un registro público.

Para servir HTTPS, descomenta el bloque `server.ssl` de `application-security.yml`.
Pon la contraseña y el alias del `.jks`; puedes ver el alias con este comando:

```bash
keytool -list -keystore baobab2.unavarra.es.jks
```

## 3. Desplegar con `deploy.sh`

Copia también `deploy/jar/deploy.sh` a la carpeta de despliegue. La primera vez, dale permiso de ejecución:

```bash
chmod +x deploy.sh
```

Para desplegar una versión nueva, deja el JAR nuevo junto al que está en uso y ejecuta:

```bash
./deploy.sh
```

- **Con dos JAR** (el actual, que es el que nombra el `Dockerfile`, y el nuevo), el script:
  1. copia el JAR actual y el `Dockerfile` a `backups/<fecha>/`;
  2. cambia el `Dockerfile` para que use el JAR nuevo;
  3. borra el JAR actual;
  4. reconstruye la imagen `rt-mobile:latest` (la anterior queda como `rt-mobile:anterior`);
  5. borra el contenedor `rt-mobile` y lo vuelve a crear con `-p 8443:8085 --restart=always`;
  6. espera a que Spring Boot arranque.
- **Con un solo JAR** (el primer despliegue), hace lo mismo, salvo la copia de seguridad y el borrado.

Los dos JAR tienen que tener nombres distintos. Por ejemplo, sube la versión en el `pom.xml` o renombra
el nuevo antes de copiarlo.

Si algo falla, el script indica cómo volver a la versión anterior desde `backups/` o desde la
imagen `rt-mobile:anterior`.

Para ver los logs:

```bash
podman logs -f rt-mobile
```

## 4. Keycloak

En el realm `rtmobile`, cliente `rt-web`:

- en *Valid redirect URIs*, añade `https://baobab2.unavarra.es:8443/*`;
- en *Web origins*, añade `https://baobab2.unavarra.es:8443`.

Tiene que ser HTTPS. Desde un nombre que no sea `localhost`, el navegador solo deja hacer el login
de Keycloak (PKCE) en una página segura.
