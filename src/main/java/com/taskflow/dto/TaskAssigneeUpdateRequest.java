package com.taskflow.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * TaskAssigneeUpdateRequest — cuerpo de PATCH /tasks/{id}/assignee.
 * Solo contiene el id del nuevo responsable. No se permite null ni valores no positivos.
 */
public record TaskAssigneeUpdateRequest(
        @NotNull(message = "assigneeId es obligatorio")
        @Positive(message = "assigneeId debe ser positivo")
        Long assigneeId
) {
}
