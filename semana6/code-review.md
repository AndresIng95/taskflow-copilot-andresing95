src/test/java/com/taskflow/slice/ReasignarTareaControllerTest.java:24

    El static import anyLong no se usa en este test; conviene quitarlo para mantener los imports mínimos.

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;



src/main/java/com/taskflow/controller/TaskController.java
Comment on lines +157 to +160
    /**
     * PATCH /tasks/{id}/assignee — cambia SOLO el assigneeId de una tarea. 404 si no existe, 422 si
     * la tarea está DONE (TaskStateException), 400 por validación si falta/invalid assigneeId.
     */
Copilot commented 7 minutes ago
Copilot
CopilotAI
7m ago

    Low

En el Javadoc del endpoint nuevo aparece la palabra en inglés "invalid"; el resto de comentarios del proyecto está en español y aquí se lee como un typo.


src/main/java/com/taskflow/dto/TaskAssigneeUpdateRequest.java
Comment on lines +10 to +13
public record TaskAssigneeUpdateRequest(
        @NotNull(message = "assigneeId es obligatorio")
        @Positive(message = "assigneeId debe ser positivo")
        Long assigneeId
Copilot commented 8 minutes ago
Copilot
CopilotAI
8m ago

    Low

Los mensajes de Bean Validation de este DTO no siguen el estilo que se usa en el resto de DTOs (frase en español con mayúscula inicial y punto final, p. ej. "El estado es obligatorio."). Mantenerlo consistente ayuda a que los errores 400 sean uniformes para el cliente.


src/test/java/com/taskflow/slice/ReasignarTareaControllerTest.java
Comment on lines +3 to +6
import com.taskflow.controller.TaskController;
import com.taskflow.exception.TaskNotFoundException;
import com.taskflow.exception.TaskStateException;
import com.taskflow.model.Priority;
Copilot commented 11 minutes ago
Copilot
CopilotAI
11m ago

    Low

Hay un import sin uso (TaskNotFoundException) en este test slice; eliminarlo evita ruido y warnings en IDEs.



src/test/java/com/taskflow/unit/ReasignarTareaServiceTest.java
Comment on lines +3 to +6
import com.taskflow.exception.TaskNotFoundException;
import com.taskflow.exception.TaskStateException;
import com.taskflow.exception.TaskValidationException;
import com.taskflow.model.Priority;
Copilot commented 12 minutes ago
Copilot
CopilotAI
12m ago

    Low

Hay un import sin uso (TaskNotFoundException) en este test; conviene eliminarlo para evitar ruido y mantener el archivo limpio.