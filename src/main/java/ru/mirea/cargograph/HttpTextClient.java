package ru.mirea.cargograph;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/** JDK-only HTTP client for retrieving Cargo.toml and sparse-index files. */
public final class HttpTextClient {
    private static final int HTTP_SUCCESS_MIN = 200;
    private static final int HTTP_REDIRECT_MIN = 300;
    private final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(15))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    public String get(URI uri) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(uri)
                .timeout(Duration.ofSeconds(30))
                .header("User-Agent", "CargoDependencyVisualizer/1.0")
                .header("Accept", "text/plain, application/json, */*")
                .GET().build();
        HttpResponse<String> response = client.send(request,
                HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < HTTP_SUCCESS_MIN
                || response.statusCode() >= HTTP_REDIRECT_MIN) {
            throw new IOException("HTTP " + response.statusCode() + " для " + uri);
        }
        return response.body();
    }
}
