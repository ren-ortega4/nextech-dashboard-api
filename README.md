# nextech-dashboard-api

Backend REST API del sistema de gestión de facturas de **NexTech / RS Tech Limitada**. Expone endpoints para autenticación, gestión de facturas, sincronización con Lioren (DTEs electrónicos) y carga de documentos de retiro.

---

## Tecnologías

| Tecnología | Versión | Uso |
|---|---|---|
| Java | 17 | Lenguaje principal |
| Spring Boot | 3.3.5 | Framework web y configuración |
| Spring Security + JWT | — | Autenticación con tokens JWT (jjwt 0.12.6) |
| Spring Data JPA | — | ORM y acceso a base de datos |
| PostgreSQL | — | Base de datos relacional (Neon cloud) |
| Spring WebFlux | — | WebClient para llamadas a Lioren API |
| Lombok | — | Reducción de boilerplate |
| Maven | — | Gestión de dependencias y build |

---

## Requisitos previos

- Java 17+
- Maven 3.8+
- PostgreSQL (o conexión a instancia Neon)

---

## Instalación y ejecución local

```bash
# 1. Clonar el repositorio
git clone https://github.com/ren-ortega4/nextech-dashboard-api.git
cd nextech-dashboard-api

# 2. Configurar variables de entorno (ver sección siguiente)

# 3. Compilar y ejecutar
mvn spring-boot:run
```

La API quedará disponible en: **http://localhost:8080**

---

## Variables de entorno

Configura las siguientes variables antes de ejecutar:

| Variable | Descripción |
|---|---|
| `DATABASE_URL` | URL de conexión PostgreSQL (Neon) |
| `DATABASE_USERNAME` | Usuario de la base de datos |
| `DATABASE_PASSWORD` | Contraseña de la base de datos |
| `JWT_SECRET` | Clave secreta para firmar tokens JWT (mín. 32 caracteres) |
| `LIOREN_BASE_URL` | URL base de la API de Lioren |
| `LIOREN_API_KEY` | API Key de Lioren |
| `LIOREN_TIPODOC` | Tipo de documento (33=Factura, 39=Boleta) |
| `UPLOAD_DIR` | Directorio local para documentos de retiro |
| `CORS_ORIGINS` | Orígenes permitidos para CORS |

---

## Endpoints principales

| Método | Ruta | Descripción |
|---|---|---|
| `POST` | `/api/v1/auth/login` | Autenticación |
| `POST` | `/api/v1/auth/register` | Registro de usuario |
| `GET` | `/api/v1/facturas/stats` | Stats del dashboard |
| `GET` | `/api/v1/facturas` | Listado paginado con filtros |
| `GET` | `/api/v1/facturas/:id` | Detalle de factura |
| `PATCH` | `/api/v1/facturas/:id` | Actualizar estado o entregado |
| `PATCH` | `/api/v1/facturas/bulk` | Actualización masiva de estado |
| `POST` | `/api/v1/facturas/:id/retiro` | Subir documento de retiro |
| `DELETE` | `/api/v1/facturas/:id/retiro/:fileId` | Eliminar documento |
| `GET` | `/api/v1/lioren/status` | Test de conexión con Lioren |
| `POST` | `/api/v1/lioren/sync/full` | Sincronización completa de DTEs desde Lioren |
| `POST` | `/api/v1/lioren/sync/incremental` | Sincronización incremental de DTEs |
| `GET` | `/api/v1/lioren/facturas/:id/dte` | Consultar DTE de una factura |
| `GET` | `/api/v1/lioren/dtes/:folio` | Consultar DTE por folio |
| `GET` | `/api/v1/lioren/dtes/:folio/pdf` | Consultar DTE con PDF en base64 |

---

## Frontend relacionado

El frontend que consume esta API es **nextech-dashboard-ui** — React 19 + Vite + TanStack Query.  
Repositorio: https://github.com/ren-ortega4/nextech-dashboard-ui

---

## Estructura del equipo

| Nombre | Rol |
|---|---|
| Alex Caica Zamora | Scrum Master / Development Team |
| Renato Ortega Ramos | Development Team |
| Ángel Prado Correa | Development Team |
| Manuel Reyes Bustos | Product Owner |

---

## Tablero Kanban

https://caica-ortega-prado.atlassian.net/jira/software/projects/SCRUM/boards/1/backlog
