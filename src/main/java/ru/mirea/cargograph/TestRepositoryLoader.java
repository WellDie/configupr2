package ru.mirea.cargograph;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.Set;

/** Loads a small dependency graph from a plain text file for offline testing. */
public final class TestRepositoryLoader {
    private static final int EXPECTED_LINE_PARTS = 2;
    public DependencyGraph load(Path file) throws IOException {
        DependencyGraph graph = new DependencyGraph();
        Set<String> defined = new LinkedHashSet<>();
        for (String raw : Files.readAllLines(file)) {
            String line = withoutComment(raw).trim();
            if (line.isEmpty()) continue;
            String[] pair = line.split(":", 2);
            if (pair.length != EXPECTED_LINE_PARTS) throw formatError(line);
            String packageName = pair[0].trim();
            validateName(packageName);
            if (!defined.add(packageName)) throw new IOException(
                    "Пакет повторно определен: " + packageName);
            graph.addNode(packageName, packageName);
            addDependencies(graph, packageName, pair[1]);
        }
        if (graph.size() == 0) throw new IOException("Файл тестового репозитория пуст.");
        for (String node : graph.nodes()) {
            if (!defined.contains(node)) throw new IOException(
                    "Пакет " + node + " используется как зависимость, но не описан в файле.");
        }
        return graph;
    }

    private static void addDependencies(DependencyGraph graph,
                                        String source, String rawDependencies)
            throws IOException {
        for (String item : rawDependencies.split(",")) {
            String target = item.trim();
            if (target.isEmpty()) continue;
            validateName(target);
            graph.addDependency(source, target);
        }
    }

    private static String withoutComment(String line) {
        int position = line.indexOf('#');
        return position < 0 ? line : line.substring(0, position);
    }

    private static void validateName(String name) throws IOException {
        if (!name.matches("[A-Z][A-Z0-9_]*")) {
            throw new IOException("Имя тестового пакета должно состоять из заглавных "
                    + "латинских букв и цифр: " + name);
        }
    }

    private static IOException formatError(String line) {
        return new IOException("Неверная строка тестового графа: " + line
                + ". Ожидается формат A: B, C");
    }
}
