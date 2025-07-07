package at.jku.dke.task_app.knn.evaluation;

import at.jku.dke.etutor.task_app.dto.CriterionDto;
import at.jku.dke.etutor.task_app.dto.GradingDto;
import at.jku.dke.etutor.task_app.dto.SubmissionMode;
import at.jku.dke.etutor.task_app.dto.SubmitSubmissionDto;
import at.jku.dke.task_app.knn.data.entities.KnnTask;
import at.jku.dke.task_app.knn.data.repositories.KnnTaskEntityRepository;
import at.jku.dke.task_app.knn.data.repositories.KnnTaskRepository;
import at.jku.dke.task_app.knn.dto.KnnSubmissionDto;
import at.jku.dke.task_app.knn.services.KnnClassifier;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Evaluates submissions for k-nearest-neighbour (KNN) tasks.
 * <p>
 * – Grades the answer<br>
 * – Generates feedback depending on feedback level (0-3)<br>
 * – Validates the answer strictly: length, character set **and** that every
 *   label occurs in the training set of the task instance.
 * </p>
 */
@Service
public class EvaluationService {

    private static final Logger LOG = LoggerFactory.getLogger(EvaluationService.class);

    private final KnnTaskRepository         taskRepository;
    private final KnnTaskEntityRepository   entityRepository;
    private final MessageSource             messageSource;
    private final ObjectMapper              objectMapper = new ObjectMapper();

    public EvaluationService(KnnTaskRepository taskRepository,
                             KnnTaskEntityRepository entityRepository,
                             MessageSource messageSource) {
        this.taskRepository   = taskRepository;
        this.entityRepository = entityRepository;
        this.messageSource    = messageSource;
    }

    /**
     * Main entry-point for the grader.
     */
    @Transactional
    public GradingDto evaluate(SubmitSubmissionDto<KnnSubmissionDto> submission) {

       // Task + basic data

        var task = taskRepository.findById(submission.taskId())
            .orElseThrow(() -> new EntityNotFoundException(
                "Entity for task " + submission.taskId() + " does not exist"));

        Locale locale = Locale.of(submission.language());

        String solutionRaw   = Optional.ofNullable(task.getSolution()).orElse("").trim();
        String submissionRaw = Optional.ofNullable(submission.submission().input()).orElse("").trim();

        String[] solutionArr = Arrays.stream(solutionRaw.split(",", -1))
            .map(String::trim)
            .toArray(String[]::new);
        String[] providedArr = Arrays.stream(submissionRaw.split(",", -1))
            .map(String::trim)
            .toArray(String[]::new);

        // pad submission with empty strings so both arrays have equal length
        if (providedArr.length < solutionArr.length) {
            providedArr = Arrays.copyOf(providedArr, solutionArr.length);
            for (int i = 0; i < providedArr.length; i++)
                if (providedArr[i] == null) providedArr[i] = "";
        }

        // whitelist of allowed labels

        Set<String> allowedLabels = entityRepository.findByTaskId(task.getId())
            .map(ent -> {
                try {
                    Map<String, ?> mp = objectMapper.readValue(
                        ent.getTrainPointsJson(), new TypeReference<>() {});
                    return mp.keySet().stream()
                        .map(s -> s.trim().toUpperCase(Locale.ROOT))
                        .collect(Collectors.toSet());
                } catch (Exception ex) {
                    LOG.warn("Could not read trainPointsJson for task {}", task.getId(), ex);
                    return Set.<String>of();
                }
            })
            .orElse(Set.of());

        // syntax validation

        Set<String> invalidLabels = new LinkedHashSet<>();
        for (String token : providedArr) {
            if (token.isBlank()) continue; // blanks always allowed
            if (!allowedLabels.contains(token.toUpperCase(Locale.ROOT)))
                invalidLabels.add(token);
        }

        boolean lengthOk = solutionArr.length == providedArr.length;
        boolean charsOk  = submissionRaw.matches("\\s*[A-Za-z ,]*\\s*");
        boolean labelsOk = invalidLabels.isEmpty();

        boolean syntaxValid = lengthOk && charsOk && labelsOk;

        String syntaxFeedback =
            syntaxValid
                ? msg("criterium.syntax.valid", locale)
                : !lengthOk
                ? msg("submission.length.mismatch", locale,
                solutionArr.length, providedArr.length)
                : !charsOk
                ? msg("submission.invalid.chars", locale)
                : msg("submission.invalid.labels", locale,
                String.join(", ", invalidLabels));

        List<CriterionDto> criteria = new ArrayList<>();
        criteria.add(new CriterionDto(msg("criterium.syntax", locale),
            null, syntaxValid, syntaxFeedback));

       //  Exam / RUN mode -> no grading, no points -------- */

        if (submission.mode() == SubmissionMode.RUN) {
            return new GradingDto(task.getMaxPoints(), BigDecimal.ZERO, "", criteria);
        }

        // simple grading (level 0)

        if (submission.feedbackLevel() == 0) {
            BigDecimal pts = syntaxValid
                ? gradeSimple(solutionArr, providedArr, task.getMaxPoints())
                : BigDecimal.ZERO;

            return new GradingDto(task.getMaxPoints(), pts,
                criteria.get(0).feedback(), criteria);
        }

        // 4. detailed grading

        GradingContext ctx = new GradingContext(solutionArr, providedArr,
            task.getMaxPoints(), syntaxValid);

        if (syntaxValid) {
            switch (submission.feedbackLevel()) {
                case 1  -> addPerPointFeedback(ctx, locale, criteria);
                case 2,3-> addPerPointFeedbackWithKnn(ctx, locale, criteria, task);
            }
        }

        // assemble response

        String overall = (syntaxValid && ctx.incorrect == 0)
            ? msg("correct", locale)
            : msg("incorrect", locale);

        if (syntaxValid && submission.feedbackLevel() == 3)
            overall = embedSolutionImage(overall, locale, task);

        return new GradingDto(task.getMaxPoints(), ctx.totalPoints, overall, criteria);
    }


    // helpers

    /** Container holding counters needed across several helper methods. */
    private static class GradingContext {
        final String[]  solution;
        final String[]  provided;
        final BigDecimal ppa;            // points per answer

        int correct = 0, incorrect = 0, skipped = 0;
        BigDecimal totalPoints = BigDecimal.ZERO;

        GradingContext(String[] sol, String[] prov,
                       BigDecimal maxPts, boolean validSyntax) {
            this.solution  = sol;
            this.provided  = prov;
            this.ppa       = validSyntax && sol.length > 0
                ? maxPts.divide(BigDecimal.valueOf(sol.length), 10, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;
        }
    }

    /**
     * Level 0 grading – only returns total points.
     */
    private BigDecimal gradeSimple(String[] sol, String[] prov, BigDecimal maxPts) {
        int correct = 0, incorrect = 0;

        for (int i = 0; i < sol.length; i++) {
            String p = Optional.ofNullable(prov[i]).orElse("");
            if (p.isBlank())        continue;                 // skipped → 0 pts
            if (sol[i].equalsIgnoreCase(p)) correct++; else incorrect++;
        }

        BigDecimal ppa = maxPts.divide(BigDecimal.valueOf(sol.length), 10, RoundingMode.HALF_UP);
        return ppa.multiply(BigDecimal.valueOf(correct - incorrect))
            .setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Adds one {@code CriterionDto} per point (feedback level 1).
     */
    private void addPerPointFeedback(GradingContext ctx, Locale loc, List<CriterionDto> out) {

        for (int i = 0; i < ctx.solution.length; i++) {
            String prov = Optional.ofNullable(ctx.provided[i]).orElse("");
            boolean skip = prov.isBlank();
            boolean ok   = !skip && ctx.solution[i].equalsIgnoreCase(prov);

            if      (skip) ctx.skipped++;
            else if (ok)   ctx.correct++;
            else           ctx.incorrect++;

            BigDecimal pts = skip ? BigDecimal.ZERO
                : ok   ? ctx.ppa
                : ctx.ppa.negate();

            ctx.totalPoints = ctx.totalPoints.add(pts);

            String fb = skip ? safeMsg("skipped", loc, "skipped")
                : ok   ? msg("correct", loc)
                : msg("incorrect", loc);

            out.add(new CriterionDto(msg("knn.for.point", loc, i + 1),
                pts, ok || skip, fb));
        }

        ctx.totalPoints = ctx.totalPoints.setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Adds point-wise feedback **plus** KNN explanation (feedback level 2/3).
     */
    private void addPerPointFeedbackWithKnn(GradingContext ctx,
                                            Locale loc,
                                            List<CriterionDto> out,
                                            KnnTask task) {

        var entity = entityRepository.findByTaskId(task.getId()).orElse(null);
        if (entity == null) {
            addPerPointFeedback(ctx, loc, out);   // fallback: no explanation
            return;
        }

        // parse training & test data from the entity

        Map<String, List<int[]>> trainMap;
        List<int[]>              testPoints;
        try {
            trainMap   = objectMapper.readValue(entity.getTrainPointsJson(), new TypeReference<>() {});
            testPoints = objectMapper.readValue(entity.getTestPointsJson(),  new TypeReference<>() {});
        } catch (Exception ex) {
            LOG.warn("Cannot read train/test points for task {}", task.getId(), ex);
            addPerPointFeedback(ctx, loc, out);   // graceful fallback
            return;
        }

        // flattened arrays for the classifier
        List<int[]>  trainPts  = new ArrayList<>();
        List<String> trainLbls = new ArrayList<>();
        trainMap.forEach((lbl, pts) -> {
            trainPts.addAll(pts);
            pts.forEach(__ -> trainLbls.add(lbl));
        });

        // run classifier for every test-point

        int p = switch (entity.getMetric().toLowerCase()) {
            case "manhattan" -> 1;
            case "euclidean" -> 2;
            default          -> 3;
        };
        KnnClassifier knn = new KnnClassifier(entity.getK(), p);
        knn.fit(trainPts, trainLbls);

        for (int i = 0; i < ctx.solution.length && i < testPoints.size(); i++) {

            String prov = Optional.ofNullable(ctx.provided[i]).orElse("");
            boolean skip = prov.isBlank();
            boolean ok   = !skip && ctx.solution[i].equalsIgnoreCase(prov);

            if      (skip) ctx.skipped++;
            else if (ok)   ctx.correct++;
            else           ctx.incorrect++;

            BigDecimal pts = skip ? BigDecimal.ZERO
                : ok   ? ctx.ppa
                : ctx.ppa.negate();

            ctx.totalPoints = ctx.totalPoints.add(pts);

            KnnClassifier.KNNResult res = knn.explainPoint(
                testPoints.get(i), entity.getTiebreaker());

           // build tiny HTML explanation for feedback

            StringBuilder html = new StringBuilder();
            html.append("<div>");
            html.append("<b>").append(msg("knn.point", loc)).append("</b> ")
                .append(Arrays.toString(testPoints.get(i))).append("<br>");
            html.append("<b>").append(msg("knn.classified.as", loc)).append("</b> <span style='color:green'>")
                .append(res.prediction).append("</span><br>");
            html.append("<b>").append(msg("knn.k.neighbors", loc)).append("</b>");
            html.append("<table border='1' cellpadding='2' cellspacing='0' style='border-collapse:collapse;'>")
                .append("<tr><th>#</th><th>")
                .append(msg("knn.index", loc)).append("</th><th>")
                .append(msg("knn.class", loc)).append("</th><th>")
                .append(msg("knn.distance", loc)).append("</th></tr>");
            int row = 1;
            for (KnnClassifier.Neighbor n : res.neighbors) {
                html.append("<tr><td>").append(row++).append("</td><td>")
                    .append(n.index).append("</td><td>")
                    .append(n.label).append("</td><td>")
                    .append(String.format(Locale.US, "%.2f", n.dist))
                    .append("</td></tr>");
            }
            html.append("</table>");
            html.append("<b>").append(msg("knn.votes.per.class", loc)).append("</b> ")
                .append(res.votes.entrySet().stream()
                    .map(e -> e.getKey() + ": " + e.getValue())
                    .collect(Collectors.joining(", ")))
                .append("<br>");
            html.append("<b>").append(msg("knn.tie.break.reason", loc)).append("</b> ");
            html.append(res.tieBreakReasonKey != null && !res.tieBreakReasonKey.isBlank()
                ? msg("knn." + res.tieBreakReasonKey, loc)
                : msg("knn.majority", loc));
            html.append("</div>");

            out.add(new CriterionDto(
                msg("knn.for.point", loc, i + 1), pts, ok || skip, html.toString()));
        }

        ctx.totalPoints = ctx.totalPoints.setScale(2, RoundingMode.HALF_UP);
    }

    // embed solution screenshot (lvl 3)

    private String embedSolutionImage(String fb, Locale loc,
                                      KnnTask task) {

        return entityRepository.findByTaskId(task.getId()).map(ent -> {
            String img = loc.getLanguage().equals("de")
                ? ent.getLoesungPngBase64De()
                : ent.getLoesungPngBase64En();
            if (img == null || img.isBlank()) return fb;      // nothing to embed

            return fb + "<br><b>" + msg("submission.solution.visualisation", loc)
                + "</b><br><img src=\"data:image/png;base64," + img
                + "\" style=\"max-width:90%;border:2px solid #444;margin:8px 0;\"/>";
        }).orElse(fb);
    }

    // i18n helpers

    private String msg(String code, Locale loc, Object... args) {
        return messageSource.getMessage(code, args, loc);
    }
    private String safeMsg(String code, Locale loc, String fallback, Object... args) {
        try {
            return messageSource.getMessage(code, args, loc);
        } catch (Exception ex) {
            LOG.warn("Missing i18n key '{}'", code);
            return args == null || args.length == 0 ? fallback
                : java.text.MessageFormat.format(fallback, args);
        }
    }
}
