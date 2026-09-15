package com.taskflow.unit;

import com.taskflow.dto.TaskRequest;
import com.taskflow.exception.TaskNotFoundException;
import com.taskflow.exception.TaskStateException;
import com.taskflow.exception.TaskValidationException;
import com.taskflow.model.Priority;
import com.taskflow.model.Task;
import com.taskflow.model.TaskStatus;
import com.taskflow.repository.TaskRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * TaskServiceTest — UNIT PURO con Mockito (S3D1, MP-1/MP-2/MP-3/MP-5). El renacimiento del parche
 * @SpringBootTest heredado de S2D4: aquí NO arranca Spring — la clase corre en MILISEGUNDOS.
 *
 * Por qué Mockito HOY (narrativa AM-1): en S2D1 este mismo test usaba el InMemoryTaskRepository REAL
 * (un *fake*, rápido y determinista — era legítimo). S2D4 eliminó las clases InMemory*; desde entonces
 * TaskRepository es una interfaz cuya única implementación es un proxy de Spring Data respaldado por una
 * BD. Para volver a probar el service EN AISLAMIENTO ya no hay implementación de juguete que instanciar
 * -> por eso existe Mockito: el mock SUSTITUYE al fake cuando el colaborador real exige infraestructura.
 *
 * Reglas del día aplicadas:
 *   - @ExtendWith(MockitoExtension.class): sin ella, @Mock queda null (dolor #1).
 *   - Los DATOS (Task) se construyen DE VERDAD con el constructor de rehidratación; SOLO el COLABORADOR
 *     (el repository) se mockea (tabla mockear/no-mockear, T3).
 *   - MockitoExtension corre en strict-stubs: un when(...) que ningún camino usa FALLA la clase
 *     (UnnecessaryStubbingException, dolor #2) -> aquí cada stub tiene propósito.
 */
@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

    @Mock
    private TaskRepository repository;

    @InjectMocks
    private com.taskflow.service.TaskService service;

    private static final Long PROYECTO = 1L;

    @Nested
    @DisplayName("crear")
    class Crear {

        @Test
        void crear_valida_delegaEnSaveYDevuelveTareaConId() throws TaskValidationException {
            // El request pasa por TaskMapper.aEntidadNueva -> Task.crear (status=TODO, id=null). El
            // repositorio, mockeado, "devuelve" una tarea YA con id (lo que la BD real haría en save).
            TaskRequest request = new TaskRequest("Redactar informe", "desc", Priority.HIGH, 5L, null);
            Task tareaConId = new Task(99L, "Redactar informe", "desc",
                    TaskStatus.TODO, Priority.HIGH, PROYECTO, 5L, null);
            when(repository.save(any(Task.class))).thenReturn(tareaConId);

            Task creada = service.crear(request, PROYECTO);

            assertNotNull(creada.getId());
            assertEquals(99L, creada.getId());
        }

        @Test
        @DisplayName("thenThrow: el mock simula una BD caída — lo que el colaborador real no te da nunca")
        void crear_conBdCaida_propagaLaExcepcion() {
            // thenThrow: con la BD real NO puedes provocar una caída a demanda; el mock sí.
            TaskRequest request = new TaskRequest("Tarea válida", "desc", Priority.MED, 5L, null);
            when(repository.save(any(Task.class)))
                    .thenThrow(new RuntimeException("Conexión a la BD perdida"));

            assertThrows(RuntimeException.class, () -> service.crear(request, PROYECTO));
        }
    }

    @Nested
    @DisplayName("cambiarStatus")
    class CambiarStatus {

        @Test
        void cambiarStatus_conAssignee_guardaDone() {
            // El dato se construye REAL (constructor de rehidratación), con assignee -> SÍ puede pasar a
            // DONE. Solo el repository (colaborador) se mockea.
            Task tareaConAssignee = tarea(1L, "Con responsable", 5L);
            when(repository.findById(1L)).thenReturn(Optional.of(tareaConAssignee));
            when(repository.save(any(Task.class))).thenAnswer(inv -> inv.getArgument(0));

            service.cambiarStatus(1L, TaskStatus.DONE);

            // ArgumentCaptor (MP-3): capturar el Task que VIAJÓ a save. El retorno no basta cuando lo que
            // importa es el OBJETO MUTADO que se persistió.
            ArgumentCaptor<Task> captor = ArgumentCaptor.forClass(Task.class);
            verify(repository).save(captor.capture());
            assertEquals(TaskStatus.DONE, captor.getValue().getStatus());
        }

        @Test
        void cambiarStatus_idInexistente_lanzaTaskNotFoundException() {
            when(repository.findById(999L)).thenReturn(Optional.empty());

            assertThrows(TaskNotFoundException.class,
                    () -> service.cambiarStatus(999L, TaskStatus.DONE));
            // No se llegó a guardar nada.
            verify(repository, never()).save(any(Task.class));
        }

        @Test
        void cambiarStatus_sinAssignee_lanzaTaskStateExceptionYNoGuarda() {
            // Sin assignee, setStatus(DONE) lanza la checked; cambiarStatus la TRADUCE a TaskStateException.
            // La traducción de S2D3 MP-9, ahora probada EN AISLAMIENTO (sin HTTP, sin BD).
            Task sinAssignee = tarea(2L, "Sin responsable", null);
            when(repository.findById(2L)).thenReturn(Optional.of(sinAssignee));

            assertThrows(TaskStateException.class,
                    () -> service.cambiarStatus(2L, TaskStatus.DONE));
            // El never() es el assert más valioso del bloque: la regla PROHIBIÓ persistir.
            verify(repository, never()).save(any(Task.class));
        }
    }

    @Nested
    @DisplayName("eliminar")
    class Eliminar {

        @Test
        void eliminar_existente_llamaDeleteById() {
            Task existente = tarea(1L, "Borrable", 5L);
            when(repository.findById(1L)).thenReturn(Optional.of(existente));

            service.eliminar(1L);

            // verify sobre una operación MUDA (void): el estado no lo delata, la interacción sí.
            verify(repository).deleteById(1L);
        }

        @Test
        void eliminar_inexistente_lanzaTaskNotFoundExceptionYNoBorra() {
            when(repository.findById(999L)).thenReturn(Optional.empty());

            assertThrows(TaskNotFoundException.class, () -> service.eliminar(999L));
            // never() + anyLong(): NO se borró nada. (Regla "todos matchers o ninguno": aquí anyLong()).
            verify(repository, never()).deleteById(anyLong());
        }
    }

    @Nested
    @DisplayName("vencidas")
    class Vencidas {

        @Test
        void vencidas_devuelveSoloVencidasYOrdenadas() throws Exception {
            LocalDate hoy = LocalDate.now();
            // El repositorio devuelve en este orden:
            // 1) vencida hace 1 día (debe incluirse)
            Task vencida1dia = new Task(11L, "Vencida 1d", "desc", TaskStatus.IN_PROGRESS,
                    Priority.MED, PROYECTO, 1L, hoy.minusDays(1));
            // 2) con fecha en 3 días (futura, no incluir)
            Task futura3 = new Task(12L, "Futura 3d", "desc", TaskStatus.IN_PROGRESS,
                    Priority.MED, PROYECTO, 1L, hoy.plusDays(3));
            // 3) DONE pero vencida hace 10 días (no incluir porque está DONE)
            Task donePasada10 = new Task(13L, "Hecha vieja", "desc", TaskStatus.DONE,
                    Priority.HIGH, PROYECTO, 1L, hoy.minusDays(10));
            // 4) sin dueDate (no incluir)
            Task sinFecha = new Task(14L, "Sin fecha", "desc", TaskStatus.IN_PROGRESS,
                    Priority.HIGH, PROYECTO, 1L, null);
            // 5) vencida hace 5 días (debe incluirse)
            Task vencida5dias = new Task(15L, "Vencida 5d", "desc", TaskStatus.IN_PROGRESS,
                    Priority.MED, PROYECTO, 1L, hoy.minusDays(5));

            when(repository.findAll()).thenReturn(List.of(
                    vencida1dia, futura3, donePasada10, sinFecha, vencida5dias
            ));

            var result = service.vencidas();

            assertEquals(2, result.size());
            // Orden esperado por POR_FECHA ascendente: primero la de hace 5 días (id 15), luego la de 1 día (id 11)
            assertEquals(15L, result.get(0).getId());
            assertEquals(11L, result.get(1).getId());
        }
    }

    @Nested
    @DisplayName("SinResponsable")
    class SinResponsable {

        @Test
        void sinResponsable_filtraYOrdenaPorFecha() {
            LocalDate hoy = LocalDate.now();
            Task sin10;
            Task conResp;
            Task sinFecha;
            Task sin2;
            try {
                // El repositorio devuelve, en ESTE orden:
                // 1) sin responsable con dueDate = hoy + 10d
                sin10 = new Task(101L, "Sin10", "desc", TaskStatus.TODO, Priority.MED, PROYECTO, null, hoy.plusDays(10));
                // 2) con responsable (debe descartarse)
                conResp = new Task(102L, "ConResp", "desc", TaskStatus.TODO, Priority.MED, PROYECTO, 5L, hoy.plusDays(5));
                // 3) sin responsable sin fecha (null)
                sinFecha = new Task(103L, "SinFecha", "desc", TaskStatus.TODO, Priority.MED, PROYECTO, null, null);
                // 4) sin responsable con dueDate = hoy + 2d
                sin2 = new Task(104L, "Sin2", "desc", TaskStatus.TODO, Priority.MED, PROYECTO, null, hoy.plusDays(2));
            } catch (TaskValidationException e) {
                throw new IllegalStateException("dato de prueba inválido", e);
            }

            when(repository.findAll()).thenReturn(List.of(sin10, conResp, sinFecha, sin2));

            var result = service.sinResponsable();

            // Deben venir SOLO las tres sin responsable, ordenadas por fecha asc (2d, 10d, sin fecha al final)
            List<Long> ids = result.stream().map(Task::getId).toList();
            assertEquals(List.of(104L, 101L, 103L), ids);
        }

        @Test
        void sinResponsable_soloConResponsable_devuelveVacio() {
            Task c1;
            Task c2;
            try {
                c1 = new Task(201L, "Con1", "desc", TaskStatus.TODO, Priority.MED, PROYECTO, 1L, null);
                c2 = new Task(202L, "Con2", "desc", TaskStatus.TODO, Priority.MED, PROYECTO, 2L, null);
            } catch (TaskValidationException e) {
                throw new IllegalStateException("dato de prueba inválido", e);
            }
            when(repository.findAll()).thenReturn(List.of(c1, c2));

            var result = service.sinResponsable();

            assertEquals(0, result.size());
        }
    }

    /** Fabrica una Task de rehidratación REAL (dato, no mock). assigneeId null = sin responsable. */
    private Task tarea(Long id, String title, Long assigneeId) {
        try {
            return new Task(id, title, "desc", TaskStatus.TODO, Priority.MED, PROYECTO, assigneeId, null);
        } catch (TaskValidationException e) {
            throw new IllegalStateException("dato de prueba inválido", e);
        }
    }
}
