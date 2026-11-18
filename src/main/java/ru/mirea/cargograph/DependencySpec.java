package ru.mirea.cargograph;

/** A dependency declaration read from Cargo.toml or the Cargo registry index. */
public record DependencySpec(
        String name,
        String versionRequirement,
        boolean optional,
        String kind) {
    public DependencySpec {
        name = name == null ? "" : name.trim();
        versionRequirement = versionRequirement == null
                || versionRequirement.isBlank() ? "*" : versionRequirement.trim();
        kind = kind == null ? "normal" : kind;
    }
}
