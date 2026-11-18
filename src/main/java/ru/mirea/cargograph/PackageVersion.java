package ru.mirea.cargograph;

import java.util.List;

/** A package version and the registry dependencies declared for that version. */
public record PackageVersion(
        String name,
        String version,
        List<DependencySpec> dependencies) {
    public String id() {
        return name + "@" + version;
    }
}
