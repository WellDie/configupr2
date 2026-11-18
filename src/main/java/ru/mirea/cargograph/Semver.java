package ru.mirea.cargograph;

import java.util.ArrayList;
import java.util.List;

/** Minimal Cargo-compatible semantic-version comparison and requirement matching. */
public record Semver(int major, int minor, int patch, String prerelease)
        implements Comparable<Semver> {
    private static final int ZERO = 0;
    private static final int MIN_COMPONENTS = 1;
    private static final int MAX_COMPONENTS = 3;
    private static final int SINGLE_COMPONENT = 1;
    private static final int TWO_COMPONENTS = 2;
    private static final int FIRST_COMPONENT = 0;
    private static final int SECOND_COMPONENT = 1;
    private static final int THIRD_COMPONENT = 2;

    public static Semver parse(String value) {
        String clean = value.trim().split("\\+", 2)[0];
        String[] pre = clean.split("-", 2);
        String[] parts = pre[0].split("\\.");
        if (parts.length < MIN_COMPONENTS || parts.length > MAX_COMPONENTS) {
            throw new IllegalArgumentException("Неподдерживаемая версия: " + value);
        }
        return new Semver(number(parts, FIRST_COMPONENT),
                number(parts, SECOND_COMPONENT), number(parts, THIRD_COMPONENT),
                pre.length == TWO_COMPONENTS ? pre[1] : "");
    }

    private static int number(String[] parts, int index) {
        if (index >= parts.length) return ZERO;
        String digits = parts[index].replaceFirst("[^0-9].*$", "");
        return digits.isEmpty() ? 0 : Integer.parseInt(digits);
    }

    @Override
    public int compareTo(Semver other) {
        int[] left = {major, minor, patch};
        int[] right = {other.major, other.minor, other.patch};
        for (int i = 0; i < left.length; i++) {
            int result = Integer.compare(left[i], right[i]);
            if (result != ZERO) return result;
        }
        if (prerelease.isEmpty() != other.prerelease.isEmpty()) {
            return prerelease.isEmpty() ? 1 : -1;
        }
        return prerelease.compareTo(other.prerelease);
    }

    public static boolean matches(String version, String requirement) {
        if (requirement == null || requirement.isBlank() || requirement.equals("*")) {
            return true;
        }
        Semver candidate = parse(version);
        for (String alternative : requirement.split("\\s*\\|\\|\\s*")) {
            if (matchesConjunction(candidate, alternative)) return true;
        }
        return false;
    }

    private static boolean matchesConjunction(Semver candidate, String expression) {
        List<String> clauses = splitClauses(expression);
        for (String clause : clauses) {
            if (!matchesClause(candidate, clause.trim())) return false;
        }
        return true;
    }

    private static List<String> splitClauses(String expression) {
        List<String> result = new ArrayList<>();
        for (String part : expression.split(",")) {
            for (String token : part.trim().split("\\s+")) {
                if (!token.isBlank()) result.add(token);
            }
        }
        return result;
    }

    private static boolean matchesClause(Semver candidate, String clause) {
        if (clause.isEmpty() || clause.equals("*")) return true;
        String operator = operatorOf(clause);
        String versionText = clause.substring(operator.length()).trim();
        if (versionText.contains("*")) return matchesWildcard(candidate, versionText);
        Semver boundary = parse(versionText);
        int comparison = candidate.compareTo(boundary);
        return switch (operator) {
            case ">=" -> comparison >= ZERO;
            case ">" -> comparison > ZERO;
            case "<=" -> comparison <= ZERO;
            case "<" -> comparison < ZERO;
            case "=" -> comparison == ZERO;
            case "~" -> matchesTilde(candidate, boundary, versionText);
            case "^", "" -> matchesCaret(candidate, boundary, versionText);
            default -> false;
        };
    }

    private static String operatorOf(String clause) {
        for (String operator : List.of(">=", "<=", "^", "~", ">", "<", "=")) {
            if (clause.startsWith(operator)) return operator;
        }
        return "";
    }

    private static boolean matchesTilde(Semver candidate, Semver base, String text) {
        if (candidate.compareTo(base) < ZERO || candidate.major != base.major) return false;
        return text.split("\\.").length == SINGLE_COMPONENT || candidate.minor == base.minor;
    }

    private static boolean matchesCaret(Semver candidate, Semver base, String text) {
        if (candidate.compareTo(base) < ZERO) return false;
        int components = text.split("\\.").length;
        if (base.major > ZERO || components == SINGLE_COMPONENT) return candidate.major == base.major;
        if (base.minor > ZERO || components == TWO_COMPONENTS) {
            return candidate.major == ZERO && candidate.minor == base.minor;
        }
        return candidate.major == ZERO && candidate.minor == ZERO
                && candidate.patch == base.patch;
    }

    private static boolean matchesWildcard(Semver candidate, String wildcard) {
        String[] parts = wildcard.split("\\.");
        if (parts.length >= MIN_COMPONENTS && !parts[0].equals("*")
                && candidate.major != Integer.parseInt(parts[0])) return false;
        return parts.length < TWO_COMPONENTS || parts[1].equals("*")
                || candidate.minor == Integer.parseInt(parts[1]);
    }
}
