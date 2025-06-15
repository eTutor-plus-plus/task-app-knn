package at.jku.dke.task_app.knn.data.entities;

import at.jku.dke.etutor.task_app.dto.TaskStatus;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class KnnTaskTest {

    @Test
    void testConstructor1() {
        String expected = "42";

        var task  = new KnnTask(expected);
        String actual = task.getSolution();

        assertEquals(expected, actual);
    }

    @Test
    void testConstructor2() {
        String       expected  = "42";
        BigDecimal   maxPoints = BigDecimal.TEN;
        TaskStatus   status    = TaskStatus.APPROVED;


        var task = new KnnTask(maxPoints, status, expected);

        assertEquals(expected,           task.getSolution());
        assertEquals(maxPoints,          task.getMaxPoints());
        assertEquals(status,             task.getStatus());;
    }

    @Test
    void testConstructor3() {
        String       expected  = "42";
        BigDecimal   maxPoints = BigDecimal.TEN;
        TaskStatus   status    = TaskStatus.APPROVED;
        long         id        = 1L;

        var task = new KnnTask(id, maxPoints, status, expected);

        assertEquals(id,                 task.getId());
        assertEquals(expected,           task.getSolution());
        assertEquals(maxPoints,          task.getMaxPoints());
        assertEquals(status,             task.getStatus());
    }

    @Test
    void testGetSetSolution() {
        var task     = new KnnTask();
        String expected = "42";

        task.setSolution(expected);

        assertEquals(expected, task.getSolution());
    }
}
