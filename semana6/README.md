# Proyecto final · Semana 6 · GitHub Copilot

**Alumno:** `Andres Carlos Barrera Basilio` · **Usuario de GitHub:** `AndresIng95`

## 1. Qué construí


| | Feature | Especificación |
|---|---|---|
| [x] | `PATCH /tasks/{id}/assignee` — cambiar el responsable | [`specs/assignee.md`](../specs/assignee.md) |

## 2. El pull request

- **URL del PR (mergeado):** `https://github.com/AndresIng95/taskflow-copilot-andresing95/pull/5`
- **Commit del merge en `main`:** `2aef98b (HEAD -> main, origin/main, origin/HEAD) Merge pull request #5 from AndresIng95/feature/assignee`
- **Comentarios de Copilot code review:** `5`

## 3. Cómo lo hice


| Paso | Qué hice | Evidencia |
|---|---|---|
| Rama y spec | `git switch -c feature/assignee` y copié la spec a `specs/` | `git log --oneline main..feature/assignee` (antes del merge) |
| Implementación | `copilot -p "/crear-endpoint-taskflow …"` con `gpt-5-mini` | `semana6/sesion-implementacion.md` (tiene la línea `Skill "crear-endpoint-taskflow" loaded successfully`) |
| Revisión | agente `revisor` sobre `semana6/proyecto-final.diff` | `semana6/revision.md` (termina con `Veredicto: APROBADO`) |
| Tests | `mvn test` en verde | `Tests run: 82, Failures: 0, Errors: 0, Skipped: 0` |
| Comprobación REST | `verificar.ps1` con `casos-assignee.ps1` | sección 5 de este documento |
| Code review | Copilot en el PR | la pestaña *Files changed* del PR |

## 4. Qué hizo el agente y qué corregí yo


| # | Qué hizo mal el agente (archivo) | Quién lo detectó | Cómo quedó corregido |
|---|---|---|---|
| 1 | `El static import anyLong no se usa en este test — ReasignarTareaControllerTest.java` | `Copilot review` | `prompt de corrección y commit` |
| 2 | `En el Javadoc del endpoint nuevo aparece la palabra en inglés "invalid"; el resto de comentarios del proyecto está en español y aquí se lee como un typo. — TaskController.java` | `Copilot review` | `prompt de corrección y commit` |
| 3 | `Los mensajes de Bean Validation de este DTO no siguen el estilo que se usa en el resto de DTOs — TaskAssigneeUpdateRequest.java` | `Copilot review` | `prompt de corrección y commit` |
| 4 | `Hay un import sin uso (TaskNotFoundException)  — ReasignarTareaControllerTest.java>` | `Copilot review` | `prompt de corrección y commit` |
| 5 | `Hay un import sin uso (TaskNotFoundException) — ReasignarTareaServiceTest.java` | `Copilot review` | `prompt de corrección y commit` |

**Lo que el agente hizo bien a la primera** (una o dos líneas): `La logica del codigo salio bien desde un inicio, solamente eran errores de documentacion o de sobra.`

## 5. Comprobaciones REST

```text
Repositorio: /home/user/taskflow-copilot-andresing95
URL de la app: http://127.0.0.1:8080
Empaquetando con Maven (mvn -q package -DskipTests), tarda unos segundos...
App arrancando (PID 42157). Esperando a que /info responda...
App lista en 17 s.
[OK]    GET /tasks/overdue devuelve solo la tarea 7
[OK]    GET /tasks/unassigned devuelve las tareas 4 y 6
[OK]    GET /projects/1/summary
[OK]    GET /projects/2/summary
[OK]    GET /projects/3/summary
[OK]    GET /projects/99/summary responde 404
[OK]    GET /projects/1/summary sin token responde 401
[OK]    PATCH /tasks/4/assignee asigna a luis
[OK]    GET /tasks/4 conserva el responsable nuevo
[OK]    PATCH /tasks/4/status a DONE ahora responde 200
[OK]    PATCH /tasks/2/assignee (DONE) responde 422
[OK]    PATCH /tasks/99/assignee responde 404
[OK]    PATCH /tasks/6/assignee con {} responde 400 y nombra assigneeId
[OK]    PATCH /tasks/6/assignee con 0 responde 400
[OK]    PATCH /tasks/6/assignee sin token responde 401
App detenida (PID 42157).
[OK]    App apagada: el puerto 8080 ya no responde
RESULTADO: 16/16 OK
```

## 6. Créditos de la semana


| Qué | AI credits |
|---|---|
| Usados en septiembre según github.com (incluye semanas anteriores si usaste Copilot antes) | `160.95 credits` |
| Implementación con la skill (`AI Credits` del PF-2) | ` 5.99` |
| Revisión del `revisor` (`AI Credits` del PF-3) | `1.42` |
| Correcciones del PF-4 y del PF-6, si hubo (`AI Credits`) | `2.62` |
