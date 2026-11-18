package ru.mirea.cargograph;

import java.util.List;

/** Parsed subset of a Cargo.toml manifest required for dependency analysis. */
public record CargoManifest(
        String name,
        String version,
        List<DependencySpec> dependencies) {
}
