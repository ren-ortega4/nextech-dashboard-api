# NexTech Dashboard API

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
