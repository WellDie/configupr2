package ru.mirea.cargograph;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Creates a simple SVG dependency diagram using only the Java standard library. */
public final class SvgExporter {
    private static final int NODE_WIDTH = 190;
    private static final int NODE_HEIGHT = 58;
    private static final int X_GAP = 110;
    private static final int Y_GAP = 42;
    private static final int MARGIN = 45;
    private static final int VERSION_LABEL_PARTS = 2;
    private static final int LONG_LABEL_LENGTH = 20;

    public String render(DependencyGraph graph, String root) {
        DependencyGraph visible = graph.reachableFrom(root);
        Map<String, Integer> depths = visible.depthsFrom(root);
        Map<String, Point> positions = positionNodes(visible, depths);
        int width = canvasWidth(depths);
        int height = canvasHeight(positions);
        StringBuilder svg = new StringBuilder();
        appendHeader(svg, width, height);
        appendEdges(svg, visible, positions);
        appendNodes(svg, visible, positions);
        svg.append("</svg>\n");
        return svg.toString();
    }

    private static Map<String, Point> positionNodes(DependencyGraph graph,
                                                     Map<String, Integer> depths) {
        Map<Integer, List<String>> layers = new LinkedHashMap<>();
        for (String node : graph.nodes()) {
            layers.computeIfAbsent(depths.get(node), ignored -> new ArrayList<>()).add(node);
        }
        Map<String, Point> positions = new LinkedHashMap<>();
        for (Map.Entry<Integer, List<String>> layer : layers.entrySet()) {
            layer.getValue().sort(Comparator.naturalOrder());
            placeLayer(layer.getKey(), layer.getValue(), positions);
        }
        return positions;
    }

    private static void placeLayer(int depth, List<String> nodes,
                                   Map<String, Point> positions) {
        for (int index = 0; index < nodes.size(); index++) {
            int x = MARGIN + depth * (NODE_WIDTH + X_GAP);
            int y = MARGIN + index * (NODE_HEIGHT + Y_GAP);
            positions.put(nodes.get(index), new Point(x, y));
        }
    }

    private static int canvasWidth(Map<String, Integer> depths) {
        int maximum = depths.values().stream().mapToInt(Integer::intValue).max().orElse(0);
        return MARGIN * 2 + (maximum + 1) * NODE_WIDTH + maximum * X_GAP;
    }

    private static int canvasHeight(Map<String, Point> positions) {
        int maximum = positions.values().stream().mapToInt(Point::y).max().orElse(MARGIN);
        return maximum + NODE_HEIGHT + MARGIN;
    }

    private static void appendHeader(StringBuilder svg, int width, int height) {
        svg.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
                .append("<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"")
                .append(width).append("\" height=\"").append(height)
                .append("\" viewBox=\"0 0 ").append(width).append(' ')
                .append(height).append("\">\n");
        svg.append("<defs><marker id=\"arrow\" markerWidth=\"10\" "
                + "markerHeight=\"10\" refX=\"8\" refY=\"3\" "
                + "orient=\"auto\"><path d=\"M0,0 L0,6 L9,3 z\" "
                + "fill=\"#64748b\"/></marker></defs>\n");
        svg.append("<rect width=\"100%\" height=\"100%\" fill=\"#f8fafc\"/>\n");
    }

    private static void appendEdges(StringBuilder svg, DependencyGraph graph,
                                    Map<String, Point> positions) {
        for (String source : graph.nodes()) {
            for (String target : graph.dependenciesOf(source)) {
                Point from = positions.get(source);
                Point to = positions.get(target);
                appendEdge(svg, from, to);
            }
        }
    }

    private static void appendEdge(StringBuilder svg, Point from, Point to) {
        int startX = from.x() + NODE_WIDTH;
        int startY = from.y() + NODE_HEIGHT / 2;
        int endX = to.x();
        int endY = to.y() + NODE_HEIGHT / 2;
        int control = Math.max(35, Math.abs(endX - startX) / 2);
        int firstControl = endX >= startX ? startX + control : startX - control;
        int secondControl = endX >= startX ? endX - control : endX + control;
        svg.append("<path d=\"M ").append(startX).append(' ').append(startY)
                .append(" C ").append(firstControl).append(' ').append(startY)
                .append(", ").append(secondControl).append(' ').append(endY)
                .append(", ").append(endX).append(' ').append(endY)
                .append("\" fill=\"none\" stroke=\"#64748b\" "
                        + "stroke-width=\"2\" marker-end=\"url(#arrow)\"/>\n");
    }

    private static void appendNodes(StringBuilder svg, DependencyGraph graph,
                                    Map<String, Point> positions) {
        for (String node : graph.nodes()) {
            Point point = positions.get(node);
            svg.append("<rect x=\"").append(point.x()).append("\" y=\"")
                    .append(point.y()).append("\" width=\"").append(NODE_WIDTH)
                    .append("\" height=\"").append(NODE_HEIGHT)
                    .append("\" rx=\"9\" fill=\"#e2e8f0\" "
                            + "stroke=\"#334155\" stroke-width=\"1.5\"/>\n");
            appendLabel(svg, graph.labelOf(node), point);
        }
    }

    private static void appendLabel(StringBuilder svg, String label, Point point) {
        String[] version = label.split("@", 2);
        int center = point.x() + NODE_WIDTH / 2;
        if (version.length == VERSION_LABEL_PARTS) {
            svg.append("<text x=\"").append(center).append("\" y=\"")
                    .append(point.y() + 25)
                    .append("\" text-anchor=\"middle\" font-family=\"Arial, sans-serif\" "
                            + "font-size=\"13\" fill=\"#0f172a\">")
                    .append(xmlEscape(version[0])).append("</text>\n");
            svg.append("<text x=\"").append(center).append("\" y=\"")
                    .append(point.y() + 44)
                    .append("\" text-anchor=\"middle\" font-family=\"Arial, sans-serif\" "
                            + "font-size=\"10\" fill=\"#475569\">")
                    .append(xmlEscape("@" + version[1])).append("</text>\n");
            return;
        }
        int fontSize = label.length() > LONG_LABEL_LENGTH ? 11 : 14;
        svg.append("<text x=\"").append(center).append("\" y=\"")
                .append(point.y() + 35)
                .append("\" text-anchor=\"middle\" font-family=\"Arial, sans-serif\" "
                        + "font-size=\"").append(fontSize)
                .append("\" fill=\"#0f172a\">")
                .append(xmlEscape(label)).append("</text>\n");
    }

    private static String xmlEscape(String value) {
        return value.replace("&", "&amp;").replace("<", "&lt;")
                .replace(">", "&gt;").replace("\"", "&quot;")
                .replace("'", "&apos;");
    }

    private record Point(int x, int y) {
    }
}
