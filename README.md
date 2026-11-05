# BioHub

Sistema Nacional de Registro y Vigilancia de Biodiversidad.
Proyecto de Ingeniería de Software 2 - Universidad El Bosque.

## Cómo levantar el proyecto

Necesitas: Git, Docker Desktop, Java 21 y Node 24.

1. Clona el repo y entra a la carpeta.
2. Copia el archivo de contraseñas y pon las tuyas:
   ```
   copy .env.example .env
   ```
3. Levanta todo:
   ```
   docker compose up --build -d
   ```
   La primera vez tarda varios minutos.
4. Levanta el frontend:
   ```
   cd frontend
   npm install
   npm run dev
   ```

| Qué | Dónde |
|---|---|
| Frontend | http://localhost:5174 |
| Nginx (entrada a las APIs) | http://localhost:8080 |
| PostgreSQL (Navicat, DBeaver) | localhost:5433, usuario `postgres`, clave del `.env` |

Comandos útiles:

| Comando | Para qué |
|---|---|
| `docker compose up -d` | Levantar todo |
| `docker compose up --build -d` | Levantar después de cambiar el backend |
| `docker compose down` | Apagar todo (los datos se quedan) |
| `docker compose logs -f api-interna` | Ver el log de un contenedor |

## Datos de prueba

Se cargan solos al levantar el proyecto. La contraseña de todos es `biohub123`.

| Correo | Rol |
|---|---|
| investigador1@biohub.test | Investigador (tiene equipos) |
| investigador2@biohub.test | Investigador (sin equipos) |
| auditor@biohub.test | Auditor científico |
| director@biohub.test | Director científico |
| altorango@biohub.test | Alto rango |
| admin@biohub.test | Administrador |

Consumidores externos: `dev-key-universidad` (120 por minuto) y `dev-key-ong` (60 por minuto).

## Cómo trabajamos

1. Mueve tu HU a **En curso** en Jira.
2. Crea tu rama desde `develop`: `feature/HU-01-registrar-hallazgo`.
3. Haz commits pequeños y sube tu rama.
4. Abre un PR hacia **`develop`**. Otro integrante lo revisa y lo aprueba.
5. Cuando `develop` está estable, se abre un PR de `develop` a `main`.

Una HU está terminada cuando:
- Cumple sus criterios de aceptación.
- Respeta las capas (ver abajo).
- Tiene pruebas unitarias del caso de uso (con dobles) y una prueba de integración del endpoint.
- Funciona con `docker compose up`.
- Otro integrante revisó el PR.

## Organización del backend

Un solo código Spring Boot que arranca de tres formas, según el perfil:

| Perfil | Contenedor | Puerto | Ruta en Nginx |
|---|---|---|---|
| `interna` | api-interna | 8081 | `/interno` |
| `publica` | api-pública (2 réplicas) | 8082 | `/publico`, `/externo` |
| `analitica` | analítica | 8083 | — |

Los paquetes siguen el C4: primero el contenedor y luego el componente.

| Paquete | Componente | HU |
|---|---|---|
| `interna/seguridad` | Seguridad y Acceso | HU-12, HU-14 |
| `interna/usuarios` | Usuarios y Roles | HU-15 |
| `interna/taxonomia` | Taxonomía (Builder y Strategy) | HU-02, HU-03 |
| `interna/hallazgos` | Hallazgos | HU-01, HU-06 |
| `interna/historial` | Historial y Auditoría | HU-07, HU-08 |
| `interna/ubicaciones` | Ubicaciones | HU-13 |
| `interna/equipos` | Equipos de Campo | HU-04, HU-05 |
| `interna/reportes` | Consulta de Reportes | HU-18 |
| `publica/consultapublica` | Consulta Pública | HU-09, HU-10, HU-11 |
| `publica/integracion` | Integración Externa | HU-16 |
| `analitica/planificador` | Planificador | HU-19 |
| `analitica/generador` | Generador de Reportes | HU-18 |

### Capas dentro de cada componente (Clean Architecture)

```
interna/hallazgos/
├── dominio/          Clases del negocio y la interfaz del repositorio
├── aplicacion/       Casos de uso (RegistrarHallazgo, ReclasificarHallazgo)
├── infraestructura/  Repositorio con JPA que implementa la interfaz
└── web/              Controladores y los datos que entran y salen
```

Regla: `web → aplicacion → dominio ← infraestructura`.

- `dominio` no importa nada de Spring. Solo se permite `@Entity` de JPA.
- Un controlador nunca llama a un repositorio directo. Siempre pasa por un caso de uso.
- Los casos de uso dependen de la interfaz del repositorio, no de JPA. Así se pueden probar con dobles.

### Base de datos

- Las tablas se crean con Flyway: `backend/src/main/resources/db/migration`.
- **Una migración que ya corrió no se edita.** Cualquier cambio va en un archivo nuevo (`V4__...sql`).
- Las tablas de versiones, cambios e intentos solo aceptan insertar. Si creas otra tabla así, agrega su `REVOKE UPDATE, DELETE`.
- Nunca cambies tablas a mano desde Navicat.

## Organización del frontend

```
frontend/src/
├── api/cliente.ts       Todas las llamadas a la API pasan por aquí
├── auth/                Sesión y RutaProtegida
├── components/ui/       Componentes de shadcn (Button, Card, Table...)
├── components/biohub/   Componentes propios (Logo, BarraLateral)
├── layouts/             Marco público y marco interno
└── App.tsx              Rutas
```

- Para una página nueva: créala en `src/paginas/` y agrega su ruta en `App.tsx`, dentro del marco que corresponde.
- Si es interna, agrega su ítem en el `MENU` de `BarraLateral.tsx`, para el rol que la usa.
- Usa los colores del tema: `bg-bosque`, `text-brote`, `bg-ambar`, `text-primary`. No pongas colores sueltos.
- Componente nuevo de shadcn: `npx shadcn@latest add <nombre>`.