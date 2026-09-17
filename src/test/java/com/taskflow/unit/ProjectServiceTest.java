package com.taskflow.unit;

import com.taskflow.dto.ProjectSummaryResponse;
import com.taskflow.exception.TaskValidationException;
import com.taskflow.model.Priority;
import com.taskflow.model.Project;
import com.taskflow.model.Task;
import com.taskflow.model.TaskStatus;
import com.taskflow.repository.ProjectRepository;
import com.taskflow.repository.TaskRepository;
import com.taskflow.repository.UserRepository;
import com.taskflow.service.ProjectService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProjectServiceTest {

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private ProjectService projectService;

    private Project proyectoConTareas;
    private Project proyectoSinTareas;

    @BeforeEach
    void setUp() {
        proyectoConTareas = new Project(2L, "App Móvil", "d", 2L, null);
        proyectoSinTareas = new Project(3L, "Vacío", "d", 1L, null);
    }

    @Test
    void resumen_proyectoConTareas_calculaTotales() throws Exception {
        // tareas: TODO x1, IN_PROGRESS x2, DONE x1; una vencida (IN_PROGRESS con dueDate pasada)
        Task t1 = new Task(5L, "T-1", "d", TaskStatus.TODO, Priority.MED, 2L, null, null);
        Task t2 = new Task(6L, "T-2", "d", TaskStatus.IN_PROGRESS, Priority.HIGH, 2L, 1L,
                LocalDate.now().minusDays(2)); // vencida
        Task t3 = new Task(7L, "T-3", "d", TaskStatus.IN_PROGRESS, Priority.LOW, 2L, null, null);
        Task t4 = new Task(8L, "T-4", "d", TaskStatus.DONE, Priority.MED, 2L, 1L, LocalDate.now().minusDays(5));

        when(taskRepository.findByProjectId(2L)).thenReturn(List.of(t1, t2, t3, t4));

        ProjectSummaryResponse res = projectService.resumen(proyectoConTareas);

        assertEquals(4, res.totalTasks());
        assertEquals(1, res.byStatus().get("TODO").intValue());
        assertEquals(2, res.byStatus().get("IN_PROGRESS").intValue());
        assertEquals(1, res.byStatus().get("DONE").intValue());
        assertEquals(1, res.overdue());
    }

    @Test
    void resumen_proyectoSinTareas_devuelveCeros() {
        when(taskRepository.findByProjectId(3L)).thenReturn(List.of());

        ProjectSummaryResponse res = projectService.resumen(proyectoSinTareas);

        assertEquals(0, res.totalTasks());
        assertEquals(0, res.byStatus().get("TODO").intValue());
        assertEquals(0, res.byStatus().get("IN_PROGRESS").intValue());
        assertEquals(0, res.byStatus().get("DONE").intValue());
        assertEquals(0, res.overdue());
    }
}
