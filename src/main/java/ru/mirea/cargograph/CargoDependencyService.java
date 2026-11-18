package ru.mirea.cargograph;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Builds the transitive Cargo dependency graph with iterative BFS. */
public final class CargoDependencyService {
    private static final int MAX_NODES = 600;
    private final CargoTomlFetcher fetcher = new CargoTomlFetcher();
    private final CargoTomlParser manifestParser = new CargoTomlParser();
    private final CargoIndexClient index = new CargoIndexClient();

    public BuildResult build(String packageName, String repository)
            throws IOException, InterruptedException {
        String cargoToml = fetcher.fetch(repository);
        CargoManifest rootManifest = manifestParser.parse(cargoToml, packageName);
        DependencyGraph graph = new DependencyGraph();
        String rootId = rootManifest.name() + "@" + rootManifest.version();
        graph.addNode(rootId, rootId);
        ArrayDeque<WorkItem> queue = new ArrayDeque<>();
        Set<String> expanded = new HashSet<>();
        queue.add(new WorkItem(rootManifest.name(), rootManifest.version(),
                rootId, rootManifest.dependencies()));
        expanded.add(rootId);
        List<String> warnings = new java.util.ArrayList<>();
        while (!queue.isEmpty()) {
            WorkItem current = queue.removeFirst();
            expandDependencies(current, graph, queue, expanded, warnings);
            checkGraphLimit(graph);
        }
        return new BuildResult(graph, rootId, rootManifest.dependencies(), warnings);
    }

    private void expandDependencies(WorkItem current, DependencyGraph graph,
                                    ArrayDeque<WorkItem> queue, Set<String> expanded,
                                    List<String> warnings)
            throws InterruptedException {
        for (DependencySpec dependency : current.dependencies()) {
            if (dependency.optional() || dependency.kind().equals("dev")) continue;
            PackageVersion selected = resolve(dependency, warnings);
            if (selected == null) {
                addUnresolved(current.id(), dependency.name(), graph, warnings);
                continue;
            }
            String childId = selected.id();
            graph.addNode(childId, childId);
            graph.addDependency(current.id(), childId);
            if (expanded.add(childId)) {
                queue.addLast(new WorkItem(selected.name(), selected.version(),
                        childId, selected.dependencies()));
            }
        }
    }

    private PackageVersion resolve(DependencySpec dependency, List<String> warnings)
            throws InterruptedException {
        if (dependency.versionRequirement().startsWith("@git:")
                || dependency.versionRequirement().startsWith("@path:")) {
            warnings.add("Зависимость " + dependency.name()
                    + " подключена из git/path, а не из crates.io.");
            return null;
        }
        try {
            return index.resolve(dependency.name(), dependency.versionRequirement());
        } catch (IOException ex) {
            warnings.add("Не удалось получить индекс " + dependency.name()
                    + ": " + ex.getMessage());
            return null;
        }
    }

    private static void addUnresolved(String parent, String name,
                                      DependencyGraph graph, List<String> warnings) {
        String unresolvedId = name + "@unresolved";
        graph.addNode(unresolvedId, unresolvedId);
        graph.addDependency(parent, unresolvedId);
        warnings.add("Не удалось разрешить версию зависимости " + name
                + "; узел добавлен без транзитивных связей.");
    }

    private static void checkGraphLimit(DependencyGraph graph) throws IOException {
        if (graph.size() > MAX_NODES) throw new IOException(
                "Граф превысил безопасный предел " + MAX_NODES + " узлов.");
    }

    public record BuildResult(DependencyGraph graph, String rootId,
                              List<DependencySpec> directDependencies,
                              List<String> warnings) {
    }
}
