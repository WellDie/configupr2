package ru.mirea.cargograph;

import java.util.LinkedHashMap;
import java.util.Map;

/** Converts a dependency subgraph into PlantUML description text. */
public final class PlantUmlExporter {
    public String render(DependencyGraph graph, String root) {
        DependencyGraph visible = graph.reachableFrom(root);
        Map<String, String> aliases = createAliases(visible);
        StringBuilder source = new StringBuilder("@startuml\n");
        source.append("title Cargo dependency graph: ")
                .append(escape(root)).append("\n");
        source.append("left to right direction\nskinparam shadowing false\n");
        source.append("skinparam packageStyle rectangle\n\n");
        appendNodes(source, visible, aliases);
        appendEdges(source, visible, aliases);
        source.append("@enduml\n");
        return source.toString();
    }

    private static Map<String, String> createAliases(DependencyGraph graph) {
        Map<String, String> aliases = new LinkedHashMap<>();
        int index = 0;
        for (String node : graph.nodes()) aliases.put(node, "node" + index++);
        return aliases;
    }

    private static void appendNodes(StringBuilder source, DependencyGraph graph,
                                    Map<String, String> aliases) {
        for (String node : graph.nodes()) {
            source.append("rectangle \"").append(escape(graph.labelOf(node)))
                    .append("\" as ").append(aliases.get(node)).append("\n");
        }
        source.append('\n');
    }

    private static void appendEdges(StringBuilder source, DependencyGraph graph,
                                    Map<String, String> aliases) {
        for (String node : graph.nodes()) {
            for (String dependency : graph.dependenciesOf(node)) {
                source.append(aliases.get(node)).append(" --> ")
                        .append(aliases.get(dependency)).append('\n');
            }
        }
    }

    private static String escape(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"")
                .replace("\n", " ");
    }
}
