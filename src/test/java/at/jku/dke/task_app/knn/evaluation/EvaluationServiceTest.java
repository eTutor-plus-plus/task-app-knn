package at.jku.dke.task_app.knn.evaluation;

import at.jku.dke.etutor.task_app.dto.*;
import at.jku.dke.task_app.knn.data.entities.KnnTask;
import at.jku.dke.task_app.knn.data.entities.KnnTaskEntity;
import at.jku.dke.task_app.knn.data.repositories.KnnTaskEntityRepository;
import at.jku.dke.task_app.knn.data.repositories.KnnTaskRepository;
import at.jku.dke.task_app.knn.dto.KnnSubmissionDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.MessageSource;

import java.math.BigDecimal;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link EvaluationService}.
 * DB is simulated with Mockito
 */
@ExtendWith(MockitoExtension.class)
class EvaluationServiceTest {

    @Mock  KnnTaskRepository       taskRepo;
    @Mock  KnnTaskEntityRepository entRepo;
    @Mock  MessageSource           msgSrc;
    @InjectMocks
    EvaluationService evaluationService;

    private static final long TASK_ID = 42L;
    private static final BigDecimal MAX = BigDecimal.TEN;

    private KnnTask       task;
    private KnnTaskEntity entity;
    private final ObjectMapper om = new ObjectMapper();

    /* helper to build a submission DTO  */
    private SubmitSubmissionDto<KnnSubmissionDto> dto(SubmissionMode mode,
                                                      int feedbackLvl,
                                                      String input) {
        return new SubmitSubmissionDto<>("user",
            "assgn",       // dummy assignment
            TASK_ID,
            "en",
            mode,
            feedbackLvl,
            new KnnSubmissionDto(input));
    }

    @BeforeEach
    void setUp() throws Exception {
        task = new KnnTask();
        task.setId(TASK_ID);
        task.setMaxPoints(MAX);
        task.setStatus(TaskStatus.APPROVED);
        task.setSolution("A");

        entity = new KnnTaskEntity();
        entity.setTask(task);
        entity.setK(1);
        entity.setMetric("euclidean");
        entity.setTiebreaker("sum");
        entity.setTrainPointsJson(om.writeValueAsString(
            Map.of("A", List.of(new int[]{1, 1}))));
        entity.setTrainLabelsJson(om.writeValueAsString(List.of("A")));
        entity.setTestPointsJson(om.writeValueAsString(List.of(new int[]{2, 2})));

        when(taskRepo.findById(TASK_ID)).thenReturn(Optional.of(task));
        when(entRepo.findByTaskId(TASK_ID)).thenReturn(Optional.of(entity));
        when(msgSrc.getMessage(anyString(), any(), any()))
            .thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void runModeZeroPoints() {
        // RUN mode must never reveal points
        var res = evaluationService.evaluate(dto(SubmissionMode.RUN, 1, "A"));
        assertEquals(BigDecimal.ZERO, res.points().stripTrailingZeros());
    }

    @Test
    void submitCorrectFullPoints() {
        // correct answer → full score
        var res = evaluationService.evaluate(dto(SubmissionMode.SUBMIT, 1, "A"));
        assertEquals(MAX.stripTrailingZeros(), res.points().stripTrailingZeros());
    }

    @Test
    void submitValidButWrongNegativePoints() throws Exception {
        entity.setTrainPointsJson(om.writeValueAsString(
            Map.of("A", List.of(new int[]{1,1}),
                "B", List.of(new int[]{8,8}))));
        entity.setTrainLabelsJson(om.writeValueAsString(List.of("A","B")));

        var res = evaluationService.evaluate(dto(SubmissionMode.SUBMIT, 1, "B"));

        assertTrue(res.points().compareTo(BigDecimal.ZERO) < 0,
            "Wrong but valid answer should yield negative points");
    }

    /** Solution expects 1 label, but 2 labels are supplied → length mismatch */
    @Test
    void syntaxLengthMismatchTriggersError() throws Exception {
        // make label B valid that only the length syntax check triggers
        entity.setTrainPointsJson(om.writeValueAsString(
            Map.of("A", List.of(new int[]{1, 1}),
                "B", List.of(new int[]{8, 8}))));
        entity.setTrainLabelsJson(om.writeValueAsString(List.of("A","B")));

        var res = evaluationService.evaluate(dto(SubmissionMode.SUBMIT, 1, "A,B"));

        // only return the Syntax-Criterion and Length-Mismatch
        assertEquals(1, res.criteria().size());
        assertTrue(res.criteria().get(0).feedback().contains("submission.length.mismatch"));
        assertEquals(BigDecimal.ZERO, res.points().stripTrailingZeros());
    }

    /** Entered class does not exist within train labels → invalid labels */
    @Test
    void syntaxUnknownLabelTriggersError() {
        var res = evaluationService.evaluate(dto(SubmissionMode.SUBMIT, 1, "Z"));

        // Syntax-Criterion must return "invalid.labels"
        assertEquals(1, res.criteria().size());
        assertTrue(res.criteria().get(0).feedback().contains("submission.invalid.labels"));
        assertEquals(BigDecimal.ZERO, res.points().stripTrailingZeros());
    }

    /** Not allowed chars (regex-violation) */
    @Test
    void syntaxInvalidCharactersTriggersError() {
        var res = evaluationService.evaluate(dto(SubmissionMode.SUBMIT, 1, "{}"));

        // Syntax-Criterion returns invalid chars
        assertEquals(1, res.criteria().size());
        assertTrue(res.criteria().get(0).feedback().contains("submission.invalid.chars"));
        assertEquals(BigDecimal.ZERO, res.points().stripTrailingZeros());
    }

    @Test
    void submitBlankIsSkipped() {
        // blank answer is treated as “skipped”
        var res = evaluationService.evaluate(dto(SubmissionMode.SUBMIT, 1, ""));
        assertTrue(res.criteria().get(1).feedback().contains("skipped"));
    }

    @Test
    void feedbackLevel0OnlyTotalPoints() {
        // feedback level 0 returns only syntax + total points
        var res = evaluationService.evaluate(dto(SubmissionMode.SUBMIT, 0, "A"));
        assertEquals(1, res.criteria().size()); // only syntax criterion
    }
    @Test
    void feedbackLevel1PerPointFeedback() {
        // submit with feedback-level 1 (per-point feedback)
        var res = evaluationService.evaluate(dto(SubmissionMode.SUBMIT, 1, "A"));

        // expect 2 criteria: Syntax + exactly one point feedback
        assertEquals(2, res.criteria().size(),
            "Level-1 should return syntax + one per-point criterion");

        //  2nd criterion should be the per-point feedback
        CriterionDto pointCrit = res.criteria().get(1);
        assertTrue(pointCrit.name().toLowerCase().contains("point"),
            "Point criterion missing or incorrectly named");
    }

    @Test
    void feedbackLevel2ContainsHtml() {
        // level 2 should add HTML table with KNN explanation
        var res = evaluationService.evaluate(dto(SubmissionMode.SUBMIT, 2, "A"));
        assertTrue(res.criteria().stream().anyMatch(c ->
            Optional.ofNullable(c.feedback()).orElse("").contains("<table")));
    }

    @Test
    void feedbackLevel3EmbedsImage() throws Exception {
        // when entity has a solution image → it must be embedded
        entity.setLoesungPngBase64En("iVBORw0KGgoAAAANSUhEUgAA");
        var res = evaluationService.evaluate(dto(SubmissionMode.SUBMIT, 3, "A"));
        assertTrue(res.generalFeedback().contains("<img"));
    }

    @Test
    void tieBreakerNearestReasonInFeedback() throws Exception {
        // special entity where tie breaker = “nearest”
        KnnTaskEntity tieEnt = new KnnTaskEntity();
        tieEnt.setTask(task);
        tieEnt.setK(2);
        tieEnt.setMetric("euclidean");
        tieEnt.setTiebreaker("nearest");
        tieEnt.setTrainPointsJson(om.writeValueAsString(
            Map.of("A", List.of(new int[]{1, 0}),
                "B", List.of(new int[]{3, 0}))));
        tieEnt.setTrainLabelsJson(om.writeValueAsString(List.of("A", "B")));
        tieEnt.setTestPointsJson(om.writeValueAsString(List.of(new int[]{0, 0})));
        when(entRepo.findByTaskId(TASK_ID)).thenReturn(Optional.of(tieEnt));

        var res = evaluationService.evaluate(dto(SubmissionMode.SUBMIT, 2, "A"));
        assertTrue(res.criteria().stream().anyMatch(c ->
            Optional.ofNullable(c.feedback()).orElse("").contains("nearestNeighbor")));
    }

    @Test
    void gradeSimpleMultiPointAggregation() throws Exception {
        // +3 –3 = 0  total points
        task.setSolution("A,B,A");
        entity.setTrainPointsJson(om.writeValueAsString(
            Map.of("A", List.of(new int[]{1, 1}),
                "B", List.of(new int[]{8, 8}))));

        var res = evaluationService.evaluate(dto(SubmissionMode.SUBMIT, 1, "A,Z,"));
        assertEquals(BigDecimal.ZERO, res.points().stripTrailingZeros());
    }
}
