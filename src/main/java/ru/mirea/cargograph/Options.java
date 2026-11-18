package ru.mirea.cargograph;

import java.nio.file.Files;
import java.nio.file.Path;
import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;

/** Command-line configuration for the application. */
public record Options(
        String packageName,
        String repository,
        boolean testMode,
        Path testRepository,
        Path outputFile,
        boolean reverseMode) {

    public static Options parse(String[] args) {
        Map<String, String> values = new LinkedHashMap<>();
        boolean test = false;
        boolean reverse = false;
        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            if (arg.equals("--test")) test = true;
            else if (arg.equals("--reverse")) reverse = true;
            else if (arg.equals("--help") || arg.equals("-h")) {
                printHelpAndExit();
            } else if (isValueOption(arg)) {
                if (++i >= args.length) throw new IllegalArgumentException(
                        "Для параметра " + arg + " не задано значение.");
                String key = normalizeKey(arg);
                if (values.putIfAbsent(key, args[i]) != null) {
                    throw new IllegalArgumentException("Параметр задан повторно: " + arg);
                }
            } else {
                throw new IllegalArgumentException("Неизвестный параметр: " + arg);
            }
        }
        return validate(values, test, reverse);
    }

    private static boolean isValueOption(String arg) {
        return arg.equals("--package") || arg.equals("-p")
                || arg.equals("--repository") || arg.equals("-r")
                || arg.equals("--repo-file") || arg.equals("--output")
                || arg.equals("-o");
    }

    private static String normalizeKey(String arg) {
        return switch (arg) {
            case "-p" -> "package";
            case "-r" -> "repository";
            case "-o" -> "output";
            case "--package" -> "package";
            case "--repository" -> "repository";
            case "--repo-file" -> "repo-file";
            default -> "output";
        };
    }

    private static Options validate(Map<String, String> values,
                                    boolean test, boolean reverse) {
        String name = required(values, "package");
        if (!name.matches("[A-Za-z0-9_-]+")) {
            throw new IllegalArgumentException("Некорректное имя пакета: " + name);
        }
        Path output = Path.of(values.getOrDefault("output", "dependency-graph.svg"));
        if (!output.getFileName().toString().toLowerCase().endsWith(".svg")) {
            throw new IllegalArgumentException("Файл результата должен иметь расширение .svg");
        }
        if (test) return validateTestMode(name, values, output, reverse);
        String repository = required(values, "repository");
        validateRepositoryUrl(repository);
        if (values.containsKey("repo-file")) throw new IllegalArgumentException(
                "--repo-file используется только вместе с --test.");
        return new Options(name, repository, false, null, output, reverse);
    }

    private static Options validateTestMode(String name,
                                            Map<String, String> values,
                                            Path output, boolean reverse) {
        if (values.containsKey("repository")) throw new IllegalArgumentException(
                "В тестовом режиме используйте --repo-file, а не --repository.");
        Path file = Path.of(required(values, "repo-file"));
        if (!Files.isRegularFile(file) || !Files.isReadable(file)) {
            throw new IllegalArgumentException("Файл тестового репозитория не найден "
                    + "или недоступен: " + file);
        }
        return new Options(name, "<test repository>", true, file, output, reverse);
    }

    private static String required(Map<String, String> values, String key) {
        String value = values.get(key);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Не задан обязательный параметр: --" + key);
        }
        return value;
    }

    private static void validateRepositoryUrl(String value) {
        try {
            URI uri = URI.create(value);
            if (!SetOfSchemes.isHttp(uri.getScheme()) || uri.getHost() == null) {
                throw new IllegalArgumentException("Ожидался HTTP(S) URL репозитория: " + value);
            }
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Некорректный URL репозитория: " + value);
        }
    }

    public void printConfiguration() {
        System.out.println("Параметры запуска:");
        System.out.println("package=" + packageName);
        System.out.println("repository=" + repository);
        System.out.println("testMode=" + testMode);
        System.out.println("repoFile=" + (testRepository == null ? "<не задан>" : testRepository));
        System.out.println("output=" + outputFile);
        System.out.println("reverseMode=" + reverseMode);
    }

    private static void printHelpAndExit() {
        System.out.println("Использование:");
        System.out.println("  run.sh --package NAME --repository URL [--output graph.svg] [--reverse]");
        System.out.println("  run.sh --package A --test --repo-file test-repo/graph.txt [--output graph.svg]");
        System.exit(0);
    }

    private static final class SetOfSchemes {
        private static boolean isHttp(String scheme) {
            return "http".equalsIgnoreCase(scheme)
                    || "https".equalsIgnoreCase(scheme);
        }
    }
}
