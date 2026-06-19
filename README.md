# nextech-dashboard-api

Backend REST API del sistema de gestión de facturas de NexTech / RS Tech Limitada. Expone endpoints para autenticación, gestión de facturas, sincronización con Lioren y carga de documentos de retiro.

---

## Tecnologías

| Tecnología | Versión | Uso |
|---|---|---|
| Java | 17 | Lenguaje principal |
| Spring Boot | 3.3.5 | Framework web y configuración |
| Spring Security + JWT | — | Autenticación con tokens JWT (jjwt 0.12.6) |
| Spring Data JPA | — | ORM y acceso a base de datos |
| PostgreSQL | — | Base de datos relacional (Neon cloud) |
| Spring WebFlux | — | WebClient para llamadas a WooCommerce API |
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
| `DB_URL` | URL de conexión PostgreSQL |
| `DB_USERNAME` | Usuario de la base de datos |
| `DB_PASSWORD` | Contraseña de la base de datos |
| `JWT_SECRET` | Clave secreta para firmar tokens JWT |
| `WC_URL` | URL base de la tienda WooCommerce |
| `WC_KEY` | Consumer Key de la API de WooCommerce |
| `WC_SECRET` | Consumer Secret de la API de WooCommerce |

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
| `POST` | `/api/v1/sync/full` | Sincronización completa con WooCommerce |
| `POST` | `/api/v1/sync/incremental` | Sincronización incremental |

---

## Frontend relacionado

El frontend que consume esta API es **nextech-dashboard-ui** — React 18 + Vite + TanStack Query.  
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
