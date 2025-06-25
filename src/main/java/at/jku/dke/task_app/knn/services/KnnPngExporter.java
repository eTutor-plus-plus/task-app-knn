package at.jku.dke.task_app.knn.services;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Utility class for exporting a KNN scenario as a PNG image.
 * <p>
 * The plot shows a 2D grid with color-coded training points, numbered test points,
 * and, if requested, solution lines connecting test points to their K nearest neighbors.
 * Includes axis labels, tick marks, and a legend. Supports English and German.
 * </p>
 */
public class KnnPngExporter {
    // Color palette for colorblind users (Okabe–Ito, color palette)
    private static final Color[] COLORBLIND_PALETTE = {
        new Color(230, 159,   0),
        new Color( 86, 180, 233),
        new Color(  0, 158, 115),
        new Color(240, 228,  66),
        new Color(  0, 114, 178),
        new Color(213,  94,   0),
        new Color(204, 121, 167)
    };

    /**
     * Generates a PNG visualization of the KNN task.
     *
     * @param xMin         Minimum x coordinate on the grid.
     * @param xMax         Maximum x coordinate on the grid.
     * @param yMin         Minimum y coordinate on the grid.
     * @param yMax         Maximum y coordinate on the grid.
     * @param trainMap     Map of class label to list of training points.
     * @param testPoints   List of test points to be classified.
     * @param knnResults   Results from KNN classifier for each test point.
     * @param drawSolution If true, draws solution lines from test points to neighbors.
     * @param locale       Language ("de" or "en") for legend/labels.
     * @param xLabel       X axis label.
     * @param yLabel       Y axis label.
     * @return PNG image of the plot.
     */
    public static BufferedImage generateKnnImage(
        int xMin, int xMax, int yMin, int yMax,
        Map<String, List<int[]>> trainMap,
        List<int[]> testPoints,
        List<KnnClassifier.KNNResult> knnResults,
        boolean drawSolution,
        String locale,
        String xLabel,
        String yLabel) {

        // List of marker types for classes, excluding diamond (reserved for test points)
        final String[] SHAPES = new String[] {
            "circle", "square", "triangle_up", "plus", "star", "triangle_down", "triangle_right"
        };

        // Layout constants
        final int TARGET_PLOT   = 500;
        final int LEGEND_SPACE  = 210;
        final int MARGIN        = 60;
        final int MIN_LABEL_PX  = 22;

        // Calculate grid scaling
        int xSteps = Math.max(1, xMax - xMin);
        int ySteps = Math.max(1, yMax - yMin);

        double cell = Math.min((double) TARGET_PLOT / xSteps, (double) TARGET_PLOT / ySteps);
        int grid = Math.max(1, (int) Math.floor(cell));
        int pointSize = Math.max(8, Math.min((int) (grid * 0.7), 20));

        int plotW = xSteps * grid;
        int plotH = ySteps * grid;

        int axisLeft   = MARGIN;
        int axisTop    = MARGIN;
        int axisRight  = axisLeft + plotW;
        int axisBottom = axisTop  + plotH;

        int width  = plotW + 2 * MARGIN + LEGEND_SPACE;
        int height = plotH + 2 * MARGIN;

        // Axis label skipping to avoid overlap
        int xSkip = Math.max(1, (int) Math.ceil((double) MIN_LABEL_PX / grid));
        int ySkip = Math.max(1, (int) Math.ceil((double) MIN_LABEL_PX / grid));

        // Create the image and graphics context
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Fill background
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, width, height);

        // Draw grid lines
        g.setColor(new Color(220,220,220));
        for (int i = xMin; i <= xMax; i++) {
            int x = axisLeft + (i - xMin) * grid;
            g.drawLine(x, axisTop, x, axisBottom);
        }
        for (int i = yMin; i <= yMax; i++) {
            int y = axisTop + (yMax - i) * grid;
            g.drawLine(axisLeft, y, axisRight, y);
        }

        // Draw axes
        int yAxisX = (0 >= xMin && 0 <= xMax) ? axisLeft + (0 - xMin) * grid : axisLeft;
        int xAxisY = (0 >= yMin && 0 <= yMax) ? axisTop  + (yMax - 0) * grid : axisBottom;
        g.setColor(Color.BLACK);
        g.setStroke(new BasicStroke(2f));
        g.drawLine(axisLeft, xAxisY, axisRight, xAxisY);    // X axis
        g.drawLine(yAxisX,  axisTop, yAxisX,  axisBottom);  // Y axis

        // Draw axis tick labels
        g.setFont(new Font("Arial", Font.PLAIN, 18));
        for (int i = xMin; i <= xMax; i++) {
            if ((i - xMin) % xSkip != 0) continue;
            int x = axisLeft + (i - xMin) * grid;
            g.drawString(String.valueOf(i), x - 6, xAxisY + 24);
        }
        for (int i = yMin; i <= yMax; i++) {
            if ((i - yMin) % ySkip != 0) continue;
            int y = axisTop + (yMax - i) * grid;
            g.drawString(String.valueOf(i), yAxisX - 28, y + 6);
        }

        // Draw axis labels
        g.setFont(new Font("Arial", Font.BOLD, 20));
        if (xLabel != null && !xLabel.isBlank()) {
            g.drawString(xLabel, axisRight + 25, xAxisY + 12);
        }
        if (yLabel != null && !yLabel.isBlank()) {
            g.drawString(yLabel, yAxisX - 15, axisTop - 18);
        }

        // Draw training points using different marker shapes for each class
        List<String> lbls = new ArrayList<>(trainMap.keySet());
        List<int[]> flatTrain = new ArrayList<>();
        for (int ci = 0; ci < lbls.size(); ci++) {
            String shapeType = SHAPES[ci % SHAPES.length];
            for (int[] p : trainMap.get(lbls.get(ci))) {
                int cx = axisLeft + (p[0] - xMin) * grid;
                int cy = axisTop  + (yMax - p[1]) * grid;
                // Use a dark gray for all training points to maximize shape contrast
                g.setColor(new Color(50, 50, 50));
                switch(shapeType) {
                    case "circle":      drawCircle(g, cx, cy, pointSize); break;
                    case "square":      drawSquare(g, cx, cy, pointSize); break;
                    case "triangle_up": drawTriangleUp(g, cx, cy, pointSize); break;
                    case "triangle_down": drawTriangleDown(g, cx, cy, pointSize); break;
                    case "triangle_right": drawTriangleRight(g, cx, cy, pointSize); break;
                    case "star":        drawStar(g, cx, cy, pointSize); break;
                    case "plus":        drawPlus(g, cx, cy, pointSize); break;
                }
                flatTrain.add(p);
            }
        }

        // Draw test points as green diamonds with numbers
        int diamond = pointSize + 14;
        g.setColor(Color.GREEN.darker());
        for (int i = 0; i < testPoints.size(); i++) {
            int[] p = testPoints.get(i);
            int cx = axisLeft + (p[0] - xMin) * grid;
            int cy = axisTop  + (yMax - p[1]) * grid;
            drawDiamondWithNumber(g, cx, cy, diamond, String.valueOf(i + 1));
        }

        // Draw solution lines if requested
        if (drawSolution && knnResults != null) {
            g.setStroke(new BasicStroke(2f));
            List<Color> solPal = generateColorPalette(testPoints.size());
            for (int t = 0; t < testPoints.size(); t++) {
                KnnClassifier.KNNResult res = knnResults.get(t);
                int[] tp = testPoints.get(t);
                int cx = axisLeft + (tp[0] - xMin) * grid;
                int cy = axisTop  + (yMax - tp[1]) * grid;
                g.setColor(solPal.get(t));
                for (KnnClassifier.Neighbor nb : res.neighbors) {
                    if (nb.index < 0 || nb.index >= flatTrain.size()) continue;
                    int[] np = flatTrain.get(nb.index);
                    int nx = axisLeft + (np[0] - xMin) * grid;
                    int ny = axisTop  + (yMax - np[1]) * grid;
                    Point edge = edgeOfDiamond(cx, cy, diamond, nx, ny);
                    g.drawLine(edge.x, edge.y, nx, ny);
                }
            }
        }

        // Draw legend for class markers and test points
        int legX = width - LEGEND_SPACE + 10;
        int legY = 30;
        int line = 36; // more vertical spacing for larger symbols
        int legendPointSize = pointSize + 10;
        g.setFont(new Font("Arial", Font.PLAIN, 18));
        for (int i = 0; i < lbls.size(); i++) {
            String shapeType = SHAPES[i % SHAPES.length];
            g.setColor(new Color(50, 50, 50));
            drawLegendSymbol(g, legX, legY + i * line, legendPointSize, shapeType);
            g.setColor(Color.BLACK);
            String txt = (locale.equals("en") ? "Class " : "Klasse ") + lbls.get(i);
            g.drawString(txt, legX + 38, legY + i * line + 8); // Mehr Abstand für größere Symbole
        }
        g.setColor(Color.GREEN.darker());
        drawLegendSymbol(g, legX, legY + lbls.size() * line, legendPointSize + 4, "diamond");
        g.setColor(Color.BLACK);
        g.drawString(locale.equals("en") ? "Test Point" : "Neue Punkte",
            legX + 38, legY + lbls.size() * line + 8);

        // Finalize image
        g.dispose();
        return img;
    }

    /**
     * Draw a filled circle at (x, y) with diameter r.
     */
    private static void drawCircle(Graphics2D g, int x, int y, int r) {
        g.fillOval(x - r / 2, y - r / 2, r, r);
    }

    /**
     * Draw a green diamond at (x, y) with a number at the center.
     */
    private static void drawDiamondWithNumber(Graphics2D g, int x, int y, int r, String number) {
        // Filled diamond
        g.setColor(new Color(60, 220, 60));
        Polygon diamond = new Polygon(
            new int[]{x, x + r / 2, x, x - r / 2},
            new int[]{y - r / 2, y, y + r / 2, y},
            4
        );
        g.fillPolygon(diamond);

        // Diamond outline
        g.setColor(new Color(10, 80, 10));
        g.setStroke(new BasicStroke(3f));
        g.drawPolygon(diamond);

        // Centered number
        if (!number.isEmpty()) {
            int fontSize = Math.max(r - 12, 14);
            Font f = new Font("Arial", Font.BOLD, fontSize);
            FontMetrics fm = g.getFontMetrics(f);
            int textWidth = fm.stringWidth(number);
            int textHeight = fm.getAscent();
            int tx = x - textWidth / 2;
            int ty = y + textHeight / 2 - 2;
            g.setColor(Color.BLACK);
            g.setFont(f);
            g.drawString(number, tx, ty);
        }
    }

    /**
     * Calculates the edge point of a diamond for connecting lines.
     */
    private static Point edgeOfDiamond(int cx, int cy, int r, int tx, int ty) {
        double halfR = r / 2.0;
        double dx = tx - cx;
        double dy = ty - cy;
        if (dx == 0 && dy == 0) return new Point(cx, cy);
        double t = halfR / (Math.abs(dx) + Math.abs(dy));
        int edgeX = (int) Math.round(cx + dx * t);
        int edgeY = (int) Math.round(cy + dy * t);
        return new Point(edgeX, edgeY);
    }

    /**
     * Draws a symbol in the legend (circle or diamond).
     */
    private static void drawLegendSymbol(Graphics2D g, int x, int y, int size, String type) {
        int adjSize = size;
        // Increase size for certain symbols for better visibility in legend
        if ("star".equals(type) || "pentagon".equals(type) ||
            "triangle_up".equals(type) || "triangle_down".equals(type)) {
            adjSize += 4; // make these shapes a bit bigger in legend
        }
        switch (type) {
            case "circle":      drawCircle(g, x, y, adjSize); break;
            case "square":      drawSquare(g, x, y, adjSize); break;
            case "triangle_up": drawTriangleUp(g, x, y, adjSize); break;
            case "triangle_down": drawTriangleDown(g, x, y, adjSize); break;
            case "triangle_right":    drawTriangleRight(g, x, y, adjSize); break;
            case "star":        drawStar(g, x, y, adjSize); break;
            case "plus":        drawPlus(g, x, y, adjSize); break;
            case "diamond":     drawDiamondWithNumber(g, x, y, adjSize, ""); break;
        }
    }


    /**
     * Generates a list of visually distinct colors.
     *
     * @param n Number of colors needed.
     * @return List of colors.
     */
    public static List<Color> generateColorPalette(int n) {
        List<Color> out = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            out.add(COLORBLIND_PALETTE[i % COLORBLIND_PALETTE.length]);
        }
        return out;
    }

    private static void drawSquare(Graphics2D g, int x, int y, int r) {
        g.fillRect(x - r / 2, y - r / 2, r, r);
    }

    private static void drawTriangleUp(Graphics2D g, int x, int y, int r) {
        Polygon p = new Polygon(
            new int[]{x, x - r / 2, x + r / 2},
            new int[]{y - r / 2, y + r / 2, y + r / 2},
            3
        );
        g.fillPolygon(p);
    }

    private static void drawTriangleDown(Graphics2D g, int x, int y, int r) {
        Polygon p = new Polygon(
            new int[]{x, x - r / 2, x + r / 2},
            new int[]{y + r / 2, y - r / 2, y - r / 2},
            3
        );
        g.fillPolygon(p);
    }
    private static void drawTriangleRight(Graphics2D g, int x, int y, int r) {
        Polygon p = new Polygon(
            new int[]{x - r / 2, x + r / 2, x - r / 2},
            new int[]{y - r / 2, y, y + r / 2},
            3
        );
        g.fillPolygon(p);
    }

    private static void drawStar(Graphics2D g, int x, int y, int r) {
        int spikes = 5;
        int[] xs = new int[spikes * 2];
        int[] ys = new int[spikes * 2];
        for (int i = 0; i < spikes * 2; i++) {
            double angle = Math.toRadians(-90 + i * 360.0 / (spikes * 2));
            double rad   = (i % 2 == 0) ? r * 0.55 : r * 0.23;
            xs[i] = x + (int) Math.round(Math.cos(angle) * rad);
            ys[i] = y + (int) Math.round(Math.sin(angle) * rad);
        }
        g.fillPolygon(xs, ys, spikes * 2);
    }

    private static void drawPlus(Graphics2D g, int x, int y, int r) {
        int arm   = r / 2;
        int thick = Math.max(3, r / 4);
        g.fillRect(x - thick / 2, y - arm, thick, 2 * arm);
        g.fillRect(x - arm,       y - thick / 2, 2 * arm, thick);
    }
}
