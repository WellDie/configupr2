package ru.mirea.cargograph;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

/** Downloads the Cargo.toml file from a repository URL. */
public final class CargoTomlFetcher {
    private static final int MIN_REPOSITORY_PARTS = 2;
    private static final int MIN_BLOB_URL_PARTS = 5;
    private static final int BLOB_FILE_PATH_START = 4;
    private final HttpTextClient http = new HttpTextClient();

    public String fetch(String repository) throws IOException, InterruptedException {
        List<URI> candidates = candidates(repository);
        IOException lastError = null;
        for (URI candidate : candidates) {
            try { return http.get(candidate); }
            catch (IOException ex) { lastError = ex; }
        }
        throw new IOException("Не удалось получить Cargo.toml. Проверьте URL: "
                + repository + ". Последняя ошибка: "
                + (lastError == null ? "неизвестная ошибка" : lastError.getMessage()));
    }

    private List<URI> candidates(String repository) {
        URI uri = URI.create(repository);
        String path = uri.getPath() == null ? "" : uri.getPath();
        if (path.endsWith("Cargo.toml")) return List.of(uri);
        if ("github.com".equalsIgnoreCase(uri.getHost())) {
            return githubCandidates(uri);
        }
        String base = repository.endsWith("/")
                ? repository.substring(0, repository.length() - 1) : repository;
        return List.of(URI.create(base + "/Cargo.toml"));
    }

    private List<URI> githubCandidates(URI uri) {
        String[] parts = uri.getPath().replaceAll("^/|/$", "").split("/");
        if (parts.length < MIN_REPOSITORY_PARTS) throw new IllegalArgumentException(
                "URL GitHub должен содержать owner/repository.");
        String owner = parts[0];
        String repo = parts[1].replaceFirst("\\.git$", "");
        if (parts.length >= MIN_BLOB_URL_PARTS && parts[2].equals("blob")) {
            String filePath = String.join("/", java.util.Arrays.copyOfRange(parts, BLOB_FILE_PATH_START, parts.length));
            return List.of(URI.create("https://raw.githubusercontent.com/" + owner
                    + "/" + repo + "/" + parts[3] + "/" + filePath));
        }
        List<URI> result = new ArrayList<>();
        for (String branch : List.of("HEAD", "main", "master")) {
            result.add(URI.create("https://raw.githubusercontent.com/" + owner
                    + "/" + repo + "/" + branch + "/Cargo.toml"));
        }
        return result;
    }
}
