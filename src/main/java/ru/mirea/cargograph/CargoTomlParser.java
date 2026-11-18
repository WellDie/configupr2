package ru.mirea.cargograph;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Reads the package and dependency tables from a Cargo.toml file. */
public final class CargoTomlParser {
    private static final int MIN_KEY_INDEX = 1;
    private static final int MIN_QUOTED_VALUE_LENGTH = 2;
    public CargoManifest parse(String text, String expectedName) {
        String packageName = "";
        String packageVersion = "0.0.0";
        String section = "";
        Map<String, String> workspace = new LinkedHashMap<>();
        Map<String, Map<String, String>> nested = new LinkedHashMap<>();
        List<DependencySpec> dependencies = new ArrayList<>();
        for (String raw : text.split("\\R")) {
            String line = stripComment(raw).trim();
            if (line.isEmpty()) continue;
            if (isSection(line)) {
                section = line.substring(1, line.length() - 1).trim();
                continue;
            }
            String[] pair = splitAssignment(line);
            if (pair == null) continue;
            if (section.equals("package")) {
                packageName = readString(pair[0], pair[1], "name", packageName);
                packageVersion = readString(pair[0], pair[1], "version", packageVersion);
            } else if (section.equals("workspace.dependencies")) {
                workspace.put(pair[0], pair[1]);
            } else if (isDependencySection(section)) {
                DependencySpec spec = parseDependency(pair[0], pair[1], workspace);
                if (spec != null && !spec.optional()) dependencies.add(spec);
            } else if (isNestedDependencySection(section)) {
                String alias = section.substring(section.lastIndexOf('.') + 1);
                nested.computeIfAbsent(alias, ignored -> new LinkedHashMap<>())
                        .put(pair[0], pair[1]);
            }
        }
        for (Map.Entry<String, Map<String, String>> entry : nested.entrySet()) {
            DependencySpec spec = createDependency(entry.getKey(), entry.getValue(), workspace);
            if (!spec.optional()) dependencies.add(spec);
        }
        validatePackageName(packageName, expectedName);
        return new CargoManifest(packageName, packageVersion, dependencies);
    }

    private static boolean isSection(String line) {
        return line.startsWith("[") && line.endsWith("]");
    }

    private static boolean isDependencySection(String section) {
        if (section.equals("workspace.dependencies")) return false;
        return section.equals("dependencies") || section.equals("build-dependencies")
                || section.endsWith(".dependencies")
                || section.endsWith(".build-dependencies");
    }

    private static boolean isNestedDependencySection(String section) {
        return section.startsWith("dependencies.")
                || section.startsWith("build-dependencies.");
    }

    private static String[] splitAssignment(String line) {
        int index = line.indexOf('=');
        if (index < MIN_KEY_INDEX) return null;
        String key = line.substring(0, index).trim().replace("\"", "");
        return new String[]{key, line.substring(index + 1).trim()};
    }

    private static String readString(String key, String value,
                                     String target, String current) {
        if (!key.equals(target)) return current;
        return unquote(value);
    }

    private static DependencySpec parseDependency(String alias,
                                                   String value,
                                                   Map<String, String> workspace) {
        return createDependency(alias, parseFields(value), workspace);
    }

    private static DependencySpec createDependency(String alias,
                                                    Map<String, String> fields,
                                                    Map<String, String> workspace) {
        boolean optional = Boolean.parseBoolean(fields.getOrDefault("optional", "false"));
        String actualName = unquote(fields.getOrDefault("package", alias));
        String requirement = unquote(fields.getOrDefault("version", "*"));
        if (Boolean.parseBoolean(fields.getOrDefault("workspace", "false"))) {
            Map<String, String> shared = parseFields(workspace.getOrDefault(alias, ""));
            requirement = unquote(shared.getOrDefault("version", requirement));
            actualName = unquote(shared.getOrDefault("package", actualName));
        }
        if (fields.containsKey("git")) requirement = "@git:" + unquote(fields.get("git"));
        if (fields.containsKey("path")) requirement = "@path:" + unquote(fields.get("path"));
        return new DependencySpec(actualName, requirement, optional, "normal");
    }

    private static Map<String, String> parseFields(String value) {
        Map<String, String> fields = new LinkedHashMap<>();
        String body = value.trim();
        if (body.startsWith("{") && body.endsWith("}")) {
            body = body.substring(1, body.length() - 1);
        } else if (body.startsWith("\"") || body.startsWith("'")) {
            fields.put("version", body);
            return fields;
        }
        for (String item : splitTopLevel(body, ',')) {
            String[] pair = splitAssignment(item.trim());
            if (pair != null) fields.put(pair[0], pair[1]);
        }
        return fields;
    }

    private static List<String> splitTopLevel(String value, char delimiter) {
        List<String> parts = new ArrayList<>();
        boolean quoted = false;
        char quote = '\0';
        int start = 0;
        for (int i = 0; i < value.length(); i++) {
            char current = value.charAt(i);
            if ((current == '"' || current == '\'') && !isEscaped(value, i)) {
                if (!quoted) { quoted = true; quote = current; }
                else if (quote == current) quoted = false;
            }
            if (current == delimiter && !quoted) {
                parts.add(value.substring(start, i));
                start = i + 1;
            }
        }
        parts.add(value.substring(start));
        return parts;
    }

    private static boolean isEscaped(String value, int index) {
        return index > 0 && value.charAt(index - 1) == '\\';
    }

    private static String stripComment(String line) {
        boolean quoted = false;
        char quote = '\0';
        for (int i = 0; i < line.length(); i++) {
            char current = line.charAt(i);
            if ((current == '"' || current == '\'') && !isEscaped(line, i)) {
                if (!quoted) { quoted = true; quote = current; }
                else if (quote == current) quoted = false;
            }
            if (current == '#' && !quoted) return line.substring(0, i);
        }
        return line;
    }

    private static String unquote(String value) {
        String result = value.trim();
        if (result.length() >= MIN_QUOTED_VALUE_LENGTH && ((result.startsWith("\"")
                && result.endsWith("\"")) || (result.startsWith("'")
                && result.endsWith("'")))) {
            return result.substring(1, result.length() - 1);
        }
        return result;
    }

    private static void validatePackageName(String actual, String expected) {
        if (actual.isBlank()) throw new IllegalArgumentException(
                "В Cargo.toml не найден раздел [package] с параметром name.");
        if (!actual.equalsIgnoreCase(expected)) throw new IllegalArgumentException(
                "Имя пакета в Cargo.toml ('" + actual + "') не совпадает с --package ('"
                        + expected + "'). Укажите имя корневого Cargo-пакета.");
    }
}
