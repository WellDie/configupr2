package ru.mirea.cargograph;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/** Coordinates configuration, dependency loading, reporting and graph export. */
public final class Application {
    public void run(Options options) throws IOException, InterruptedException {
        options.printConfiguration();
        GraphContext context = loadGraph(options);
        printDirectDependencies(context);
        printWarnings(context.warnings());
        printReverseDependencies(options, context.graph(), context.rootId());
        printCycleStatus(context.graph());
        exportGraph(options, context.graph(), context.rootId());
    }

    private GraphContext loadGraph(Options options)
            throws IOException, InterruptedException {
        if (options.testMode()) {
            DependencyGraph graph = new TestRepositoryLoader().load(options.testRepository());
            return new GraphContext(graph, options.packageName(), List.of(), List.of());
        }
        CargoDependencyService.BuildResult result = new CargoDependencyService()
                .build(options.packageName(), options.repository());
        return new GraphContext(result.graph(), result.rootId(),
                result.directDependencies(), result.warnings());
    }

    private void printDirectDependencies(GraphContext context) {
        System.out.println("\nПрямые зависимости:");
        if (context.directDependencies().isEmpty()) {
            for (String dependency : context.graph().dependenciesOf(context.rootId())) {
                System.out.println("  - " + context.graph().labelOf(dependency));
            }
            return;
        }
        for (DependencySpec dependency : context.directDependencies()) {
            System.out.println("  - " + dependency.name()
                    + " (требование версии: " + dependency.versionRequirement() + ")");
        }
    }

    private void printWarnings(List<String> warnings) {
        for (String warning : warnings) System.err.println("Предупреждение: " + warning);
    }

    private void printReverseDependencies(Options options, DependencyGraph graph, String rootId) {
        if (!options.reverseMode()) return;
        List<String> reverse = graph.reverseDependencies(rootId);
        System.out.println("\nОбратные зависимости для " + rootId + ":");
        if (reverse.isEmpty()) System.out.println("  Нет зависимых пакетов.");
        for (String item : reverse) System.out.println("  - " + graph.labelOf(item));
    }

    private void printCycleStatus(DependencyGraph graph) {
        System.out.println("\nУзлов в графе: " + graph.size());
        System.out.println(graph.hasCycle()
                ? "Обнаружен цикл: повторное раскрытие узлов остановлено через visited."
                : "Циклические зависимости не обнаружены.");
    }

    private void exportGraph(Options options, DependencyGraph graph, String root)
            throws IOException {
        String plantUml = new PlantUmlExporter().render(graph, root);
        Path svgPath = options.outputFile();
        Path pumlPath = siblingWithExtension(svgPath, ".puml");
        writeFile(pumlPath, plantUml);
        writeFile(svgPath, new SvgExporter().render(graph, root));
        System.out.println("\nPlantUML-описание графа:");
        System.out.println(plantUml);
        System.out.println("SVG сохранен: " + svgPath.toAbsolutePath());
        System.out.println("PlantUML сохранен: " + pumlPath.toAbsolutePath());
    }

    private static Path siblingWithExtension(Path file, String extension) {
        String name = file.getFileName().toString();
        int dot = name.lastIndexOf('.');
        String base = dot < 0 ? name : name.substring(0, dot);
        return file.resolveSibling(base + extension);
    }

    private static void writeFile(Path path, String content) throws IOException {
        Path parent = path.toAbsolutePath().getParent();
        if (parent != null) Files.createDirectories(parent);
        Files.writeString(path, content, StandardCharsets.UTF_8);
    }

    private record GraphContext(DependencyGraph graph, String rootId,
                                List<DependencySpec> directDependencies,
                                List<String> warnings) {
    }
}
