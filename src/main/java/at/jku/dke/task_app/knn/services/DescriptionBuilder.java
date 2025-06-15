package at.jku.dke.task_app.knn.services;

import at.jku.dke.task_app.knn.data.entities.KnnTaskEntity;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Locale;

/**
 * Builds the HTML task description for a knn task in the given language.
 */
@Component
public class DescriptionBuilder {

    private final MessageSource ms; // Provided by Spring

    public DescriptionBuilder(MessageSource ms) {
        this.ms = ms;
    }

    /**
     * Generates the full task description (with instructions, hints, and images).
     * @param locale    The target language.
     * @param e         The knn task entity.
     * @param maxPoints Maximum achievable points.
     * @return HTML-formatted description string.
     */
    public String build(Locale locale,
                        KnnTaskEntity e,
                        BigDecimal maxPoints) {

        // Main instruction text
        String desc = ms.getMessage(
            "task.knn.description",
            new Object[]{
                e.getNumTest(), e.getK(), e.getMetric(),
                e.getxMin(), e.getxMax(), e.getyMin(), e.getyMax()
            },
            locale);

        // Explanation for tiebreaker strategy
        String tbKey = switch ((e.getTiebreaker() == null ? "sum"
            : e.getTiebreaker().trim().toLowerCase())) {
            case "mean"    -> "task.knn.tiebreaker.mean.description";
            case "nearest" -> "task.knn.tiebreaker.nearest.description";
            default        -> "task.knn.tiebreaker.sum.description";
        };
        desc += "\n" + ms.getMessage(tbKey,
            new Object[]{e.getTiebreaker()},
            locale);

        // Add table of test points
        if (e.getTestPoints() != null && !e.getTestPoints().isEmpty())
            desc += buildTestPointTable(e.getTestPoints(), locale);

        // Add image if available
        String png = (locale == Locale.GERMAN)
            ? e.getAufgabenPngBase64De()
            : e.getAufgabenPngBase64En();

        if (png != null && !png.isBlank()) {
            desc += "<br><b>"
                + ms.getMessage("task.knn.visualisation",
                null, locale)
                + "</b><br><img src=\"data:image/png;base64," + png + "\"/>";
        }

        // Add submission hint (points per test case)
        int n = Math.max(1, e.getNumTest() == null ? 0 : e.getNumTest());

        BigDecimal total = maxPoints != null ? maxPoints : BigDecimal.ONE; // Fallback
        BigDecimal per   = total
            .divide(BigDecimal.valueOf(n), 10, RoundingMode.HALF_UP)
            .stripTrailingZeros();

        desc += ms.getMessage(
            "task.knn.submission.hint",
            new Object[]{
                per.toPlainString(),
                per.negate().toPlainString()
            },
            locale);

        return "<div>" + desc + "</div>";
    }

    /**
     * Builds an HTML table listing all test points.
     */
    private String buildTestPointTable(List<int[]> pts, Locale loc) {
        StringBuilder sb = new StringBuilder()
            .append("<br><b>")
            .append(ms.getMessage("task.knn.testpoints.table.title", null, loc))
            .append("</b><br><table border='1' cellpadding='4' style='border-collapse:collapse'>");

        for (int i = 0; i < pts.size(); i++) {
            int[] p = pts.get(i);
            sb.append("<tr><td>")
                .append(ms.getMessage("task.knn.testpoints.table.points",
                    new Object[]{i + 1}, loc))
                .append("</td><td>(")
                .append(p[0]).append("/").append(p[1])
                .append(")</td></tr>");
        }
        sb.append("</table>");
        return sb.toString();
    }
}
