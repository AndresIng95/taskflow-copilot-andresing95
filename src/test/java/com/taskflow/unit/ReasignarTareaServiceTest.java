package com.taskflow.unit;

import com.taskflow.exception.TaskStateException;
import com.taskflow.exception.TaskValidationException;
import com.taskflow.model.Priority;
import com.taskflow.model.Task;
import com.taskflow.model.TaskStatus;
import com.taskflow.repository.TaskRepository;
import com.taskflow.service.TaskService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReasignarTareaServiceTest {

    @Mock
    private TaskRepository repository;

    @InjectMocks
    private TaskService taskService;

    private Task tareaTodoSinAssignee;
    private Task tareaDone;

    @BeforeEach
    void setUp() throws TaskValidationException {
        tareaTodoSinAssignee = new Task(4L, "Escribir tests MockMvc", "desc", TaskStatus.TODO,
                Priority.MED, 1L, null, LocalDate.now().plusDays(7));
        tareaDone = new Task(2L, "Tarea ya terminada", "desc", TaskStatus.DONE,
                Priority.HIGH, 1L, 1L, LocalDate.now().plusDays(1));
    }

    @Test
    void reasignar_tareaTODO_sinResponsable_guardadoConNuevoAssignee() {
        when(repository.save(org.mockito.ArgumentMatchers.any())).thenAnswer(i -> i.getArgument(0));

        ArgumentCaptor<Task> captor = ArgumentCaptor.forClass(Task.class);

        Task result = taskService.reasignar(tareaTodoSinAssignee, 5L);

        verify(repository).save(captor.capture());
        Task saved = captor.getValue();
        assertEquals(5L, saved.getAssigneeId());
        assertEquals(5L, result.getAssigneeId());
    }

    @Test
    void reasignar_tareaDONE_lanzaTaskStateException_y_noGuarda() {
        assertThrows(TaskStateException.class, () -> taskService.reasignar(tareaDone, 3L));
        verify(repository, never()).save(org.mockito.ArgumentMatchers.any());
    }
}
