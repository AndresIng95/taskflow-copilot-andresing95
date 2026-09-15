# ARQUITECTURA de TaskFlow

Guía rápida para un desarrollador nuevo.

## 1. Visión general por capas y paquetes
El proyecto usa la clásica arquitectura en capas dentro del paquete raíz `com.taskflow`:

- Capa de controlador (HTTP): `src/main/java/com/taskflow/controller`
  - Clases: `ProjectController`, `TaskController`, `AuthController`, `InfoController`.
  - Archivos: `src/main/java/com/taskflow/controller/ProjectController.java`, `src/main/java/com/taskflow/controller/TaskController.java`.

- Capa de servicio (casos de uso): `src/main/java/com/taskflow/service`
  - Clases: `ProjectService`, `TaskService`, `AuthService`, `ReportService`, `JpaUserDetailsService`.
  - Archivo ejemplo: `src/main/java/com/taskflow/service/TaskService.java`.

- Capa de repositorio (persistencia): `src/main/java/com/taskflow/repository`
  - Interfaces: `ProjectRepository`, `TaskRepository`, `UserRepository`.
  - Archivo ejemplo: `src/main/java/com/taskflow/repository/TaskRepository.java`.

- Modelo/entidades y lógica de dominio: `src/main/java/com/taskflow/model`
  - Clases: `Task`, `Project`, `User`, enums como `TaskStatus`, `Priority`.
  - Archivo ejemplo: `src/main/java/com/taskflow/model/Task.java`.

- DTOs y mappers (contrato API): `src/main/java/com/taskflow/dto` y `src/main/java/com/taskflow/mapper`
  - DTOs: `TaskRequest`, `TaskResponse`, `ProjectRequest`, `ProjectResponse`.
  - Mappers: `TaskMapper`, `ProjectMapper`.
  - Archivos: `src/main/java/com/taskflow/dto/TaskRequest.java`, `src/main/java/com/taskflow/mapper/TaskMapper.java`.

- Seguridad y configuración: `src/main/java/com/taskflow/security` y `src/main/java/com/taskflow/config`
  - Clases: `JwtService`, `JwtAuthenticationFilter`, `ProjectSecurity`, `SecurityConfig`, `DataSeeder`.
  - Archivos: `src/main/java/com/taskflow/security/JwtService.java`, `src/main/java/com/taskflow/security/JwtAuthenticationFilter.java`, `src/main/java/com/taskflow/config/SecurityConfig.java`.

- Manejo de errores y utilidades: `src/main/java/com/taskflow/advice` y `src/main/java/com/taskflow/exception`
  - `GlobalExceptionHandler`, excepciones de dominio como `TaskNotFoundException`.

## 2. Recorrido de POST /projects/{projectId}/tasks (end-to-end)
Ejemplo de endpoint: `POST /projects/{projectId}/tasks`.

1. Request HTTP llega a la capa web y es recibida por `TaskController` (`src/main/java/com/taskflow/controller/TaskController.java`).
2. El controlador recibe un `@RequestBody @Valid TaskRequest` (`src/main/java/com/taskflow/dto/TaskRequest.java`).
3. Valida entrada (Bean Validation). Si falla, `GlobalExceptionHandler` convierte la excepción en 400.
4. `TaskController.createTask` comprueba que el proyecto exista llamando a `ProjectService.buscarPorId(projectId)`; si no existe lanza `ProjectNotFoundException` y el advice lo traduce a 404.
5. El `TaskController` invoca `TaskService.crear(request, projectId)`. Dentro de `TaskService.crear`, el DTO se convierte a entidad nueva mediante `TaskMapper.aEntidadNueva(request, projectId)` (que delega en la factory de dominio `Task.crear`) y la entidad se persiste con `TaskRepository.save`.
6. Tras persistir, `TaskController` construye la cabecera `Location` hacia `/tasks/{id}` y responde `201 Created` con el cuerpo obtenido de `TaskMapper.aResponse(creada)` (entidad -> `TaskResponse`).

Notas importantes: los controladores no contienen reglas de negocio; su responsabilidad es validación básica, transformación DTO y coordinación de servicios.

## 3. Dónde viven las reglas de negocio

- Reglas de dominio (invariantes, transiciones de estado, validaciones complejas) = dentro de las entidades en `src/main/java/com/taskflow/model`.
  - Ejemplo: `Task` encapsula reglas como "crear en TODO", "no permitir marcar DONE sin responsable", y `estaVencida()`.

- Reglas de aplicación (orquestación entre repos y entidades, políticas de creación) = en `src/main/java/com/taskflow/service` (p.ej. `TaskService`, `ProjectService`).

- Validaciones de entrada simples y contratos API = DTOs con Bean Validation (`@Valid` en `TaskRequest`).

- Errores específicos lanzan excepciones de dominio (`TaskValidationException`, `TaskStateException`) y son convertidos por `GlobalExceptionHandler` (`src/main/java/com/taskflow/advice/GlobalExceptionHandler.java`).

## 4. Seguridad con JWT

- Autenticación y autorización se configuran en `src/main/java/com/taskflow/config/SecurityConfig.java`.
- `JwtService` (`src/main/java/com/taskflow/security/JwtService.java`) crea y valida tokens JWT (sin estado).
- `JwtAuthenticationFilter` (`src/main/java/com/taskflow/security/JwtAuthenticationFilter.java`) intercepta peticiones, extrae el token desde `Authorization: Bearer ...`, valida y carga el usuario en el contexto de seguridad.
- Rutas públicas: `/auth/**`, `/info`, Swagger y consola H2 están explícitamente permitidas; el resto requiere token.
- Reglas de autorización más finas usan `@PreAuthorize` y `ProjectSecurity` (`src/main/java/com/taskflow/security/ProjectSecurity.java`) para operaciones como borrar un proyecto (solo dueño o `ADMIN`).

Resumen: JWT = tokens sin estado; la verificación se hace en cada petición por el filtro y la carga de detalles de usuario la hace `JpaUserDetailsService` (`src/main/java/com/taskflow/service/JpaUserDetailsService.java`).

## 5. Organización de tests

Estructura de tests en `src/test/java/com/taskflow`:

- Unitarios (`unit`): pruebas rápidas sin Spring, usan JUnit 5 + Mockito.
  - Ejemplos: `src/test/java/com/taskflow/unit/TaskServiceTest.java`, `ReportServiceTest.java`.

- Slice tests (`slice`): pruebas con un slice de la aplicación (p.ej. `@WebMvcTest`, `@DataJpaTest`) para validar controladores o repos.
  - Ejemplos: `src/test/java/com/taskflow/slice/ProjectControllerTest.java`, `TaskControllerTest.java`, `TaskRepositoryTest.java`.

- Integración / E2E (`integration`): `@SpringBootTest` y uso de perfil `test`. Algunos tests de integración usan Testcontainers y están marcados `*IT.java` (no se ejecutan en la suite normal sin `-Ddocker.tests=true`).
  - Ejemplos: `src/test/java/com/taskflow/integration/FlujoCompletoE2ETest.java`, `TaskRepositoryPostgresIT.java`.

Convención práctica:
- Ejecutar `mvn -q test` para la suite normal (unit + slice + integration leve). No activar Testcontainers salvo que sepas lo que haces.

## 6. Puntos rápidos de navegación

- Punto de entrada app: `src/main/java/com/taskflow/TaskflowApiApplication.java`.
- Seed de datos para desarrollo: `src/main/java/com/taskflow/config/DataSeeder.java` (perfil `h2`).
- Manejo de errores: `src/main/java/com/taskflow/advice/GlobalExceptionHandler.java`.
- Seguridad: `src/main/java/com/taskflow/config/SecurityConfig.java`, `src/main/java/com/taskflow/security/JwtService.java`.

---

Si se necesita, se puede ampliar esta guía con diagramas de secuencia para requests clave o ejemplos de commits de migraciones. Para cualquier cambio en reglas de negocio, primero revisar `src/main/java/com/taskflow/model/Task.java` y los tests relacionados en `src/test/java/com/taskflow/unit/TaskValidationTest.java` y `src/test/java/com/taskflow/unit/TaskServiceTest.java`.
