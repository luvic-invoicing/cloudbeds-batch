# cloudbeds-batch-cr
Proceso de batch para enviar factura electronica desde el punto de venta de hoteles cloudbeds en Costa Rica

Swagger local: http://localhost:8051/swagger-ui.html

El mapeo fiscal consulta Accounting por separado para el `externalRelationKind`
de las transacciones y el `originId` de sus impuestos (`taxes` o `taxLines`).
Los impuestos se consultan por su `id` desde `mapFiscalDocumentTaxes`, reutilizando
la fecha minima de las transacciones. El `originId` se asigna al `taxID`;
si falta el `id` o el `originId`, el procesamiento falla explicitamente.

## Convivencia con la version anterior

| Version | Contenedor | Imagen | Puerto |
| --- | --- | --- | --- |
| Actual: invoice changes | `cloudbeds-batch-cr-invoice` | `cloudbeds-batch-cr:invoice-v1` | 8050 |
| Nueva: fiscal-document changes | `cloudbeds-batch-cr-fiscal` | `cloudbeds-batch-cr:fiscal-v1` | 8051 |

El Compose se administra directamente en `/root/docker-compose.yml` del servidor,
fuera de este repositorio. El servicio fiscal usa la red `host`, igual que el
despliegue actual. Conserva
`ports` con el mismo formato, aunque Docker lo ignora en modo `host` y no restringe
la escucha a localhost. `PORT=8051` cambia el puerto real de Spring.
La nueva instancia escucha por defecto en todas las interfaces en 8051 para recibir los webhooks
que la aplicacion registra en `http://144.202.35.15:8051`. Verificar que el firewall
del servidor y del proveedor permita ese trafico antes de registrar propiedades.

Las dos versiones comparten intencionalmente `cb-notification-queue` y su exchange.
RabbitMQ entrega cada mensaje a uno de los consumidores, no a los dos. Esto requiere
que cualquiera de las versiones pueda procesar cualquier notificacion. Esta cola
de respuestas no separa los eventos HTTP de invoice y fiscal document.

Usa las conexiones de `application.properties`, sin archivos de entorno adicionales.
Se conserva `spring.jpa.hibernate.ddl-auto=update` de la aplicacion: antes de
arrancar contra una base compartida, respaldarla y revisar que cualquier cambio de
esquema sea compatible con invoice. Los logs tienen un directorio independiente.

### Renombrar el repositorio

En GitHub, cambiar el nombre en Settings > General > Repository name a
`cloudbeds-batch-cr`. Despues actualizar el remoto de cada clon:

```sh
git remote set-url origin https://github.com/luvic-invoicing/cloudbeds-batch-cr.git
git remote -v
```

No es necesario renombrar la carpeta local. El cambio de nombre de GitHub no cambia
imagenes, contenedores ni el proceso que ya esta corriendo.

### Construir y desplegar la nueva version

La compilacion del JAR y de la imagen se realiza en el servidor Linux.
Se requiere JDK 8, Maven, Docker y el plugin `docker compose`.
Usar un checkout separado del despliegue anterior en
`/root/deployment/cloudbeds-batch-cr-fiscal`, con los cambios de
`feature/fiscal-document-migration` publicados y descargados antes de compilar.

Antes de compilar, revisar las conexiones de
[application.properties](src/main/resources/application.properties).
Confirmar la URL del emailer: el Compose actual lo ubica en 8029, mientras que
la configuracion historica de la aplicacion usa 8019. El JAR incluye la
configuracion del repositorio: no publicar la imagen en un registro publico.

Comprobar que Maven usa Java 8 y compilar desde ese checkout:

```sh
cd /root/deployment/cloudbeds-batch-cr-fiscal
java -version
mvn -version
mvn clean verify
```

Continuar solo si termina en `BUILD SUCCESS`.
El artefacto es `target/cloudbeds-batch-cr.jar`, independientemente de la version
Maven. El Dockerfile declara 8051 y ambos perfiles de la aplicacion usan 8051 por
defecto. El Compose tambien configura explicitamente `PORT=8051`.

Construir la nueva imagen sin sobrescribir `invoice-v1` ni `latest`:

```sh
mkdir -p /root/DockerLogs/cloudbeds-batch-cr-fiscal-logs
docker build -t cloudbeds-batch-cr:fiscal-v1 .
docker images cloudbeds-batch-cr
```

En `/root/docker-compose.yml`, mantener invoice sin cambios y verificar que el
servicio fiscal tenga este bloque bajo `services`:

```yaml
  cloudbeds-batch-cr-fiscal:
    image: cloudbeds-batch-cr:fiscal-v1
    container_name: cloudbeds-batch-cr-fiscal
    network_mode: host
    environment:
      PORT: "8051"
    ports:
      - "127.0.0.1:8051:8051"
    volumes:
      - "/root/DockerLogs/cloudbeds-batch-cr-fiscal-logs:/opt/service/cloudbeds-batch-cr-logs"
    ulimits:
      nofile:
        soft: 65536
        hard: 65536
    restart: always
    mem_limit: 212m
    mem_reservation: 212m
```

Validar el Compose:

```sh
docker compose -f /root/docker-compose.yml config --quiet
```

Solo si valida correctamente, arrancar unicamente fiscal:

```sh
docker compose -f /root/docker-compose.yml up -d --no-deps cloudbeds-batch-cr-fiscal
```

No ejecutar `down` ni usar `--remove-orphans`. No detener invoice.

### Verificar y habilitar los webhooks

```sh
docker ps --filter name=cloudbeds-batch-cr
curl --fail http://127.0.0.1:8051/api/wh/v2/test
curl --fail http://127.0.0.1:8050/api/wh/v1/test
docker logs --tail 100 cloudbeds-batch-cr-fiscal
tail -n 100 /root/DockerLogs/cloudbeds-batch-cr-fiscal-logs/CloudbedsBatchCrError.log
```

Ambos endpoints deben responder `true`. Revisar tambien los logs montados para
confirmar conexiones a PostgreSQL y RabbitMQ; el endpoint no verifica la
integracion completa.

Mantener la URL publica actual de invoice changes apuntando a 8050.
`createProperty` registra los eventos `fiscal_document/create` y
`fiscal_document/update` en `http://144.202.35.15:8051/api/wh/v2/fiscal_document`,
y `integration/appstate_changed` en
`http://144.202.35.15:8051/api/wh/v2/changeAppState`.
Esto ocurre al crear la propiedad; desplegar el contenedor no migra las
suscripciones existentes. Desde un equipo externo verificar tambien
`http://144.202.35.15:8051/api/wh/v2/test` antes de registrar propiedades.
No enviar eventos invoice al endpoint fiscal ni cambiar globalmente el destino
de 8050 a 8051.

Antes de habilitar los eventos nuevos, definir el corte por propiedad/reserva y
evitar que un mismo documento se envie por ambos flujos: compartir una base de
datos o una cola no garantiza deduplicacion entre versiones. Verificar con un
documento controlado la emision y su notificacion antes de ampliar el trafico.

### Actualizaciones y reversion

Para otra entrega usar una etiqueta nueva, por ejemplo
`cloudbeds-batch-cr:fiscal-v2`, al construir la imagen y en `image` del Compose. Mantener disponible
la etiqueta de la entrega fiscal anterior para poder volver a ella.

Para retirar solo la instancia fiscal:

```sh
# Primero deshabilitar o pausar la suscripcion nueva en Cloudbeds para evitar
# reintentos contra un destino apagado; no redirigir sus payloads al endpoint viejo.
docker compose -f /root/docker-compose.yml stop cloudbeds-batch-cr-fiscal
```

El contenedor anterior continua en 8050. No retirar esa version hasta completar la
migracion de propiedades, las notificaciones pendientes y el periodo de observacion.
Detener fiscal no revierte cambios de esquema realizados durante el arranque.

Los logs locales de `cloudbeds-batch-cr-logs` y los archivos `*.log` se ignoran
en Git; no forman parte de las entregas.