package ru.mirea.cargograph;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Reads crate versions directly from the public Cargo sparse index. */
public final class CargoIndexClient {
    private static final int ONE_CHARACTER_NAME = 1;
    private static final int TWO_CHARACTER_NAME = 2;
    private static final int THREE_CHARACTER_NAME = 3;
    private final HttpTextClient http = new HttpTextClient();
    private final JsonParser json = new JsonParser();
    private final Map<String, List<PackageVersion>> cache = new HashMap<>();

    public PackageVersion resolve(String name, String requirement)
            throws IOException, InterruptedException {
        List<PackageVersion> versions = loadVersions(name);
        return versions.stream()
                .filter(item -> Semver.matches(item.version(), requirement))
                .max(Comparator.comparing(item -> Semver.parse(item.version())))
                .orElse(null);
    }

    private List<PackageVersion> loadVersions(String name)
            throws IOException, InterruptedException {
        String key = name.toLowerCase();
        if (cache.containsKey(key)) return cache.get(key);
        String content = http.get(URI.create("https://index.crates.io/" + indexPath(key)));
        List<PackageVersion> versions = parseIndex(content);
        cache.put(key, versions);
        return versions;
    }

    private List<PackageVersion> parseIndex(String content) {
        List<PackageVersion> result = new ArrayList<>();
        for (String line : content.split("\\R")) {
            if (line.isBlank()) continue;
            Object parsed = json.parse(line);
            if (parsed instanceof Map<?, ?> record) {
                PackageVersion version = parseVersionRecord(record);
                if (version != null) result.add(version);
            }
        }
        return result;
    }

    private PackageVersion parseVersionRecord(Map<?, ?> record) {
        String name = string(record.get("name"));
        String version = string(record.get("vers"));
        if (name.isBlank() || version.isBlank()
                || Boolean.TRUE.equals(record.get("yanked"))) return null;
        return new PackageVersion(name, version, parseDependencies(record.get("deps")));
    }

    private List<DependencySpec> parseDependencies(Object raw) {
        List<DependencySpec> result = new ArrayList<>();
        if (!(raw instanceof List<?> list)) return result;
        for (Object item : list) {
            if (item instanceof Map<?, ?> dependency) {
                DependencySpec spec = parseDependencyRecord(dependency);
                if (spec != null && !spec.optional() && !spec.kind().equals("dev")) {
                    result.add(spec);
                }
            }
        }
        return result;
    }

    private DependencySpec parseDependencyRecord(Map<?, ?> record) {
        String name = string(record.get("name"));
        String requirement = string(record.get("req"));
        String kind = string(record.get("kind"));
        boolean optional = Boolean.TRUE.equals(record.get("optional"));
        if (name.isBlank()) return null;
        return new DependencySpec(name, requirement, optional,
                kind.isBlank() ? "normal" : kind);
    }

    private static String indexPath(String name) {
        return switch (name.length()) {
            case ONE_CHARACTER_NAME -> "1/" + name;
            case TWO_CHARACTER_NAME -> "2/" + name;
            case THREE_CHARACTER_NAME -> "3/" + name.charAt(0) + "/" + name;
            default -> name.substring(0, 2) + "/" + name.substring(2, 4) + "/" + name;
        };
    }

    private static String string(Object value) {
        return value == null ? "" : value.toString();
    }
}
