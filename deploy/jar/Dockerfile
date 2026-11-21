# Imagen de rt-apk a partir del JAR ya construido (web + API en el 8085 del contenedor; 8443 en baobab2). Ver deploy/jar/README.md.
#   cd backend && ./mvnw -Pfrontend package   -> target/rt-apk-backend-0.4.0.jar
#   podman build -t rt-mobile:latest .
# openjdk:21 no existe en Docker Hub (la imagen openjdk está obsoleta): Eclipse Temurin es su sustituta
FROM docker.io/library/eclipse-temurin:21-jre

WORKDIR /app


COPY rt-apk-backend-0.4.0.jar /app/rt-apk-backend-0.4.0.jar
COPY application-security.yml /app/application-security.yml

COPY baobab2.unavarra.es.jks /app/baobab2.unavarra.es.jks

COPY cacerts /etc/ssl/certs/java/cacerts

# Sin config.js: el frontend va dentro del JAR (static/) y lee su configuración de /api/config

# Exponer el puerto de la aplicación
EXPOSE 8085

ENTRYPOINT ["java","-Djavax.net.ssl.trustStore=/etc/ssl/certs/java/cacerts", "-Djavax.net.ssl.trustStorePassword=changeit","-jar","/app/rt-apk-backend-0.4.0.jar"]
