package ru.mirea.cargograph;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Directed dependency graph; an edge A -> B means A requires B. */
public final class DependencyGraph {
    private static final int ZERO = 0;
    private final Map<String, String> labels = new LinkedHashMap<>();
    private final Map<String, Set<String>> edges = new LinkedHashMap<>();

    public void addNode(String id, String label) {
        labels.putIfAbsent(id, label);
        edges.computeIfAbsent(id, ignored -> new LinkedHashSet<>());
    }

    public void addDependency(String from, String to) {
        addNode(from, from);
        addNode(to, to);
        edges.get(from).add(to);
    }

    public boolean contains(String id) {
        return edges.containsKey(id);
    }

    public Set<String> nodes() {
        return Collections.unmodifiableSet(edges.keySet());
    }

    public Set<String> dependenciesOf(String id) {
        return Collections.unmodifiableSet(edges.getOrDefault(id, Set.of()));
    }

    public String labelOf(String id) {
        return labels.getOrDefault(id, id);
    }

    public int size() {
        return edges.size();
    }

    /** Returns the transitive subgraph reachable from the requested root. */
    public DependencyGraph reachableFrom(String root) {
        requireNode(root);
        DependencyGraph result = new DependencyGraph();
        ArrayDeque<String> queue = new ArrayDeque<>();
        Set<String> visited = new LinkedHashSet<>();
        queue.add(root);
        visited.add(root);
        while (!queue.isEmpty()) {
            String current = queue.removeFirst();
            result.addNode(current, labelOf(current));
            for (String next : dependenciesOf(current)) {
                result.addDependency(current, next);
                if (visited.add(next)) queue.addLast(next);
            }
        }
        return result;
    }

    /** Finds all direct and transitive packages that depend on the target. */
    public List<String> reverseDependencies(String target) {
        requireNode(target);
        Map<String, Set<String>> reverse = buildReverseEdges();
        ArrayDeque<String> queue = new ArrayDeque<>();
        Set<String> visited = new LinkedHashSet<>();
        queue.add(target);
        visited.add(target);
        while (!queue.isEmpty()) {
            String current = queue.removeFirst();
            for (String parent : reverse.getOrDefault(current, Set.of())) {
                if (visited.add(parent)) queue.addLast(parent);
            }
        }
        visited.remove(target);
        return new ArrayList<>(visited);
    }

    /** Calculates minimum BFS depth for SVG node placement. */
    public Map<String, Integer> depthsFrom(String root) {
        requireNode(root);
        Map<String, Integer> depths = new LinkedHashMap<>();
        ArrayDeque<String> queue = new ArrayDeque<>();
        depths.put(root, 0);
        queue.add(root);
        while (!queue.isEmpty()) {
            String current = queue.removeFirst();
            int nextDepth = depths.get(current) + 1;
            for (String next : dependenciesOf(current)) {
                if (!depths.containsKey(next)) {
                    depths.put(next, nextDepth);
                    queue.addLast(next);
                }
            }
        }
        return depths;
    }


    /** Detects directed cycles using Kahn's topological-sort algorithm. */
    public boolean hasCycle() {
        Map<String, Integer> indegrees = new LinkedHashMap<>();
        for (String node : nodes()) indegrees.put(node, 0);
        for (Set<String> children : edges.values()) {
            for (String child : children) indegrees.merge(child, 1, Integer::sum);
        }
        ArrayDeque<String> queue = new ArrayDeque<>();
        for (Map.Entry<String, Integer> entry : indegrees.entrySet()) {
            if (entry.getValue() == 0) queue.addLast(entry.getKey());
        }
        int removed = 0;
        while (!queue.isEmpty()) {
            String current = queue.removeFirst();
            removed++;
            for (String child : dependenciesOf(current)) {
                int remaining = indegrees.merge(child, -1, Integer::sum);
                if (remaining == ZERO) queue.addLast(child);
            }
        }
        return removed != size();
    }

    private Map<String, Set<String>> buildReverseEdges() {
        Map<String, Set<String>> reverse = new LinkedHashMap<>();
        for (String node : nodes()) reverse.put(node, new LinkedHashSet<>());
        for (Map.Entry<String, Set<String>> entry : edges.entrySet()) {
            for (String child : entry.getValue()) {
                reverse.computeIfAbsent(child, ignored -> new LinkedHashSet<>());
                reverse.get(child).add(entry.getKey());
            }
        }
        return reverse;
    }

    private void requireNode(String id) {
        if (!contains(id)) throw new IllegalArgumentException(
                "Пакет не найден в графе: " + id);
    }
}
