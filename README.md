# Urban Gardens System

Sistema backend para una red de huertos urbanos comunitarios. Administra parcelas de cultivo y registra el estado ambiental que envían los sensores IoT.

Hay dos microservicios independientes. Cada uno tiene su propia base de datos PostgreSQL y se comunican por HTTP.

## Tecnologías

- Java 21
- Spring Boot 3.4
- Spring Web, Spring Data JPA y Spring Validation
- PostgreSQL 16
- Maven
- Docker y Docker Compose
- JUnit 5, Mockito y MockMvc

No hay autenticación ni frontend. La base de datos es PostgreSQL.

## Arquitectura

```text
Cliente HTTP
    |
    |  POST/GET /api/v1/parcels          Puerto 8081
    v
parcel-service  ----------------->  parcels_db
    ^
    |  GET /api/v1/parcels/{id}
    |
monitoring-service  ------------->  monitoring_db
    ^
    |  POST/GET /api/v1/readings         Puerto 8082
Cliente HTTP
```

`parcel-service` registra y consulta parcelas. `monitoring-service` guarda lecturas de sensores. Antes de registrar o consultar lecturas, el segundo servicio pregunta al primero si la parcela existe. Usa `RestClient`. No hay relaciones JPA entre las dos bases.

Si `parcel-service` responde 404, la lectura se rechaza. Si la llamada falla por un error HTTP o por una caída de red, la API responde 503.

## Estructura de carpetas

```text
urban-gardens-system/
├── parcel-service/
│   ├── pom.xml
│   ├── Dockerfile
│   └── src/main/java/com/urbangardens/parcel/
│       ├── controller/
│       ├── service/
│       ├── repository/
│       ├── entity/
│       ├── dto/
│       ├── exception/
│       ├── validation/
│       └── util/
├── monitoring-service/
│   ├── pom.xml
│   ├── Dockerfile
│   └── src/main/java/com/urbangardens/monitoring/
│       ├── controller/
│       ├── service/
│       ├── repository/
│       ├── entity/
│       ├── dto/
│       ├── client/
│       ├── config/
│       ├── exception/
│       ├── validation/
│       └── util/
├── docker-compose.yml
├── .env.example
└── pom.xml
```

El `pom.xml` de la raíz solo agrupa los dos módulos para compilarlos juntos. Cada microservicio sigue siendo un proyecto Maven independiente.

## Requisitos previos

- JDK 21
- Maven 3.9 o superior
- Docker y Docker Compose, si se va a levantar el sistema completo
- PostgreSQL 16, solo si se ejecuta fuera de Docker

## PostgreSQL

Hay dos bases aisladas:

| Servicio | Base de datos | Usuario de desarrollo | Puerto en el host |
|---|---|---|---|
| parcel-service | `parcels_db` | `parcels_user` | 5433 |
| monitoring-service | `monitoring_db` | `monitoring_user` | 5434 |

Dentro de Docker cada PostgreSQL escucha en el puerto 5432. Los puertos 5433 y 5434 son los que se publican en la máquina local para no chocar con un PostgreSQL que ya use el 5432.

Las contraseñas de desarrollo están en `.env.example`:

- `parcels_pass`
- `monitoring_pass`

Son valores de ejemplo. En Docker Compose se inyectan con variables de entorno. Para cambiarlos, copie `.env.example` a `.env` y edite ese archivo. `.env` no debe versionarse.

Hibernate usa `ddl-auto=update` para crear y ajustar las tablas al arrancar. Sirve para desarrollo. En producción conviene sustituirlo por migraciones versionadas y no dejar que la aplicación modifique el esquema sola.

Las consultas las genera Spring Data JPA con parámetros enlazados. El texto del usuario no se concatena en SQL.

## Ejecutar con Docker Compose

Desde `urban-gardens-system`:

```bash
docker compose up --build
```

El comando levanta:

1. PostgreSQL de parcelas
2. PostgreSQL de monitoreo
3. `parcel-service` en `http://localhost:8081`
4. `monitoring-service` en `http://localhost:8082`

Los servicios esperan a que su base esté saludable. `monitoring-service` además espera a que `parcel-service` responda en `/actuator/health`.

Para detenerlos:

```bash
docker compose down
```

Los volúmenes `parcels_data` y `monitoring_data` conservan los datos. Para borrarlos:

```bash
docker compose down -v
```

## Ejecutar en local, sin contenedores de la aplicación

Puede usar las bases que publica Docker y arrancar los JAR desde el IDE o con Maven.

1. Levante solo las bases:

```bash
docker compose up parcels-db monitoring-db
```

2. En una terminal:

```bash
cd parcel-service
mvn spring-boot:run
```

3. En otra:

```bash
cd monitoring-service
mvn spring-boot:run
```

Si ya tiene un PostgreSQL local en el puerto 5432, cree las dos bases y exporte las URLs antes de arrancar:

```sql
CREATE USER parcels_user WITH PASSWORD 'parcels_pass';
CREATE DATABASE parcels_db OWNER parcels_user;

CREATE USER monitoring_user WITH PASSWORD 'monitoring_pass';
CREATE DATABASE monitoring_db OWNER monitoring_user;
```

```bash
export SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/parcels_db
export SPRING_DATASOURCE_USERNAME=parcels_user
export SPRING_DATASOURCE_PASSWORD=parcels_pass
```

En `monitoring-service` apunte la URL a `monitoring_db` y, si `parcel-service` no está en el puerto 8081, defina `PARCEL_SERVICE_BASE_URL`.

## Unidades de las lecturas

| Campo | Unidad y rango |
|---|---|
| `soilMoisture` | Porcentaje de humedad del suelo, de 0 a 100 |
| `temperature` | Grados Celsius, de -40 a 60 |
| `ph` | Escala de pH, de 0 a 14 |
| `nitrogen`, `phosphorus`, `potassium` | mg/kg de suelo, no negativos |
| `airQuality` | Índice de calidad del aire (AQI), no negativo |

El rango de temperatura cubre condiciones al aire libre de un huerto. No se aceptan `null`, `NaN` ni infinitos. `createdAt` y `recordedAt` los genera el backend en UTC.

## Endpoints de parcelas

Base: `http://localhost:8081`

| Método | Ruta | Resultado |
|---|---|---|
| POST | `/api/v1/parcels` | 201 y la parcela creada |
| GET | `/api/v1/parcels` | 200 y la lista, vacía si no hay registros |
| GET | `/api/v1/parcels/{id}` | 200 o 404 |

El cliente no envía `id` ni `createdAt`.

### Registrar una parcela

```bash
curl -i -X POST http://localhost:8081/api/v1/parcels \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Huerto Comunitario Norte",
    "location": {
      "latitude": 28.6329,
      "longitude": -106.0691
    },
    "soilType": "CLAY",
    "ownerOrLead": "Responsable del huerto"
  }'
```

Respuesta `201 Created`:

```json
{
  "id": 1,
  "name": "Huerto Comunitario Norte",
  "location": {
    "latitude": 28.6329,
    "longitude": -106.0691
  },
  "soilType": "CLAY",
  "ownerOrLead": "Responsable del huerto",
  "createdAt": "2026-10-14T18:30:00"
}
```

`soilType` admite `CLAY` (arcilloso), `SILT` (limoso) y `SAND` (arenoso).

Los textos se recortan y los espacios repetidos se unifican. Se conservan acentos, números y signos habituales de un nombre, como el apóstrofo o el guion. Los caracteres de control se rechazan. Después de normalizar, el nombre y el responsable se vuelven a validar.

### Consultar parcelas

```bash
curl http://localhost:8081/api/v1/parcels
curl http://localhost:8081/api/v1/parcels/1
```

## Endpoints de monitoreo

Base: `http://localhost:8082`

| Método | Ruta | Resultado |
|---|---|---|
| POST | `/api/v1/readings` | 201 y la lectura guardada |
| GET | `/api/v1/readings/parcel/{parcelId}` | 200 y el historial, del más reciente al más antiguo |
| GET | `/api/v1/readings/parcel/{parcelId}/latest` | 200 con la última lectura, o 404 si la parcela aún no tiene lecturas |

### Registrar una lectura

La parcela debe existir en `parcel-service`.

```bash
curl -i -X POST http://localhost:8082/api/v1/readings \
  -H "Content-Type: application/json" \
  -d '{
    "parcelId": 1,
    "soilMoisture": 35.5,
    "temperature": 22.4,
    "ph": 6.8,
    "nitrogen": 18.0,
    "phosphorus": 12.0,
    "potassium": 20.0,
    "airQuality": 42.0
  }'
```

Respuesta `201 Created`:

```json
{
  "id": 1,
  "parcelId": 1,
  "soilMoisture": 35.5,
  "temperature": 22.4,
  "ph": 6.8,
  "nitrogen": 18.0,
  "phosphorus": 12.0,
  "potassium": 20.0,
  "airQuality": 42.0,
  "recordedAt": "2026-10-14T18:31:00"
}
```

### Consultar lecturas

```bash
curl http://localhost:8082/api/v1/readings/parcel/1
curl http://localhost:8082/api/v1/readings/parcel/1/latest
```

En Postman use las mismas URLs, el método indicado y el cuerpo JSON con `Content-Type: application/json`.

## Errores

Todas las respuestas de error comparten esta estructura:

```json
{
  "timestamp": "2026-10-14T12:30:00",
  "status": 400,
  "error": "VALIDATION_ERROR",
  "message": "Los datos proporcionados no son válidos.",
  "path": "/api/v1/parcels",
  "details": [
    {
      "field": "name",
      "message": "El nombre debe contener entre 3 y 100 caracteres."
    }
  ]
}
```

| Código `error` | HTTP | Cuándo aparece |
|---|---|---|
| `VALIDATION_ERROR` | 400 | Un campo no cumple las validaciones |
| `INVALID_DATA` | 400 | El dato incumple una regla de negocio, por ejemplo un tipo de suelo desconocido |
| `MALFORMED_REQUEST` | 400 | El JSON está mal formado |
| `RESOURCE_NOT_FOUND` | 404 | No existe la parcela o la última lectura |
| `DATA_INTEGRITY_ERROR` | 409 | La base rechazó el guardado por integridad |
| `SERVICE_UNAVAILABLE` | 503 | No fue posible comunicarse con `parcel-service` |
| `INTERNAL_ERROR` | 500 | Error interno. La respuesta no incluye la traza |

Ejemplos:

```bash
curl -i -X POST http://localhost:8081/api/v1/parcels \
  -H "Content-Type: application/json" \
  -d '{
    "name": "",
    "location": { "latitude": 28.63, "longitude": -106.06 },
    "soilType": "CLAY",
    "ownerOrLead": "Responsable del huerto"
  }'
```

```bash
curl -i http://localhost:8081/api/v1/parcels/15
```

```json
{
  "timestamp": "2026-10-14T12:30:00",
  "status": 404,
  "error": "RESOURCE_NOT_FOUND",
  "message": "No se encontró la parcela con ID 15.",
  "path": "/api/v1/parcels/15",
  "details": []
}
```

```bash
curl -i http://localhost:8081/api/v1/parcels/-1
curl -i http://localhost:8081/api/v1/parcels/abc
```

Si `parcel-service` está apagado, un `POST /api/v1/readings` válido responde 503:

```json
{
  "timestamp": "2026-10-14T12:30:00",
  "status": 503,
  "error": "SERVICE_UNAVAILABLE",
  "message": "No fue posible comunicarse con el servicio de parcelas.",
  "path": "/api/v1/readings",
  "details": []
}
```

## Pruebas

Las pruebas usan JUnit 5, Mockito y MockMvc. No necesitan PostgreSQL: el repositorio y el cliente HTTP se sustituyen por dobles de prueba.

Desde la raíz:

```bash
mvn test
```

O por separado:

```bash
cd parcel-service && mvn test
cd monitoring-service && mvn test
```

Cubren el alta de parcelas, nombre vacío, coordenadas inválidas, tipo de suelo inválido, listado, consulta existente e inexistente, identificadores negativos o no numéricos, saneamiento de texto, formato de error, alta de lecturas, pH inválido, parcela inexistente y fallos de comunicación entre servicios.

## Salud de los servicios

```bash
curl http://localhost:8081/actuator/health
curl http://localhost:8082/actuator/health
```

Una respuesta `{"status":"UP"}` indica que el proceso y su base de datos están disponibles.
