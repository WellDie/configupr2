package ru.mirea.cargograph;

import java.util.List;

/** Queue item used by the iterative breadth-first graph builder. */
public record WorkItem(
        String name,
        String version,
        String id,
        List<DependencySpec> dependencies) {
}
