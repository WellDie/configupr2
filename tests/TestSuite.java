package ru.mirea.cargograph;

import java.nio.file.Path;
import java.util.List;

/** Lightweight tests runnable without JUnit or any external dependency. */
public final class TestSuite {
    private int passed;

    public static void main(String[] args) throws Exception {
        new TestSuite().runAll();
    }

    private void runAll() throws Exception {
        testCargoManifestParsing();
        testNestedCargoManifestParsing();
        testCommandLineValidation();
        testBreadthFirstReachability();
        testCyclesAndReverseDependencies();
        testSemverRequirements();
        testPlantUmlAndSvgExport();
        System.out.println("Успешно пройдено тестов: " + passed);
    }

    private void testCargoManifestParsing() {
        String text = "[package]\nname = \"demo\"\nversion = \"0.1.0\"\n"
                + "[dependencies]\nserde = \"^1.0\"\n"
                + "smallvec = { version = \"1.13\", optional = true }\n"
                + "[dev-dependencies]\npretty_assertions = \"1\"\n";
        CargoManifest manifest = new CargoTomlParser().parse(text, "demo");
        check(manifest.name().equals("demo"), "Cargo package name");
        check(manifest.dependencies().size() == 1, "optional/dev dependencies excluded");
        check(manifest.dependencies().get(0).name().equals("serde"), "dependency parsed");
    }


    private void testNestedCargoManifestParsing() {
        String text = "[package]\nname = \"nested\"\nversion = \"1.0.0\"\n"
                + "[dependencies.alpha]\nversion = \"^2.0\"\n"
                + "default-features = false\n"
                + "[build-dependencies]\nbindgen = \"0.69\"\n";
        CargoManifest manifest = new CargoTomlParser().parse(text, "nested");
        check(manifest.dependencies().size() == 2, "nested and build dependency tables");
        check(manifest.dependencies().stream().anyMatch(
                item -> item.name().equals("alpha") && item.versionRequirement().equals("^2.0")),
                "nested dependency version");
    }

    private void testCommandLineValidation() {
        Options options = Options.parse(new String[]{"--package", "A", "--test",
                "--repo-file", "test-repo/graph.txt", "--output", "test.svg"});
        check(options.testMode() && options.packageName().equals("A"),
                "test-mode command line options");
        expectError(() -> Options.parse(new String[]{"--package", "A", "--test"}),
                "missing test repository option");
        expectError(() -> Options.parse(new String[]{"--package", "A"}),
                "missing repository URL");
        expectError(() -> Options.parse(new String[]{"--package", "bad name",
                "--repository", "https://example.org"}), "invalid package name");
        expectError(() -> Options.parse(new String[]{"--package", "A",
                "--repository", "wrong-url"}), "invalid repository URL");
        expectError(() -> Options.parse(new String[]{"--package", "A", "--test",
                "--repo-file", "missing-graph.txt"}), "missing test graph file");
        expectError(() -> Options.parse(new String[]{"--package", "A", "--test",
                "--repo-file", "test-repo/graph.txt", "--output", "graph.png"}),
                "invalid graph output extension");
        expectError(() -> Options.parse(new String[]{"--package", "A", "--unknown"}),
                "unknown command-line option");
    }

    private void expectError(Runnable action, String description) {
        try {
            action.run();
            throw new AssertionError("Ожидалась ошибка: " + description);
        } catch (IllegalArgumentException expected) {
            check(true, description);
        }
    }

    private void testBreadthFirstReachability() throws Exception {
        DependencyGraph graph = new TestRepositoryLoader().load(
                Path.of("test-repo", "graph.txt"));
        DependencyGraph reachable = graph.reachableFrom("A");
        check(reachable.size() == 6, "BFS reachable node count");
        check(reachable.dependenciesOf("A").containsAll(List.of("B", "C")),
                "BFS preserves direct edges");
        check(!reachable.contains("G") && !reachable.contains("H"),
                "BFS excludes disconnected and upstream nodes");
    }

    private void testCyclesAndReverseDependencies() throws Exception {
        DependencyGraph graph = new TestRepositoryLoader().load(
                Path.of("test-repo", "graph.txt"));
        check(graph.hasCycle(), "cycle detection");
        List<String> reverse = graph.reverseDependencies("D");
        check(reverse.containsAll(List.of("A", "B", "C", "F", "G")),
                "transitive reverse dependencies");
        check(!reverse.contains("D"), "reverse dependencies exclude target");
    }

    private void testSemverRequirements() {
        check(Semver.matches("1.9.0", "^1.2"), "caret requirement match");
        check(!Semver.matches("2.0.0", "^1.2"), "caret upper bound");
        check(Semver.matches("1.4.5", ">=1.0, <2.0"), "range requirement");
        check(!Semver.matches("1.5.0", "~1.4.0"), "tilde upper bound");
    }

    private void testPlantUmlAndSvgExport() throws Exception {
        DependencyGraph graph = new TestRepositoryLoader().load(
                Path.of("test-repo", "graph.txt"));
        String plantUml = new PlantUmlExporter().render(graph, "A");
        String svg = new SvgExporter().render(graph, "A");
        check(plantUml.startsWith("@startuml") && plantUml.contains("@enduml"),
                "PlantUML output markers");
        check(plantUml.contains("node0 -->") || plantUml.contains(" --> "),
                "PlantUML dependency edges");
        check(svg.contains("<svg") && svg.contains("</svg>"), "SVG document");
        check(svg.contains("&amp;") || svg.contains("<text"), "SVG labels");
    }

    private void check(boolean condition, String description) {
        if (!condition) throw new AssertionError("Тест не пройден: " + description);
        passed++;
        System.out.println("[OK] " + description);
    }
}
