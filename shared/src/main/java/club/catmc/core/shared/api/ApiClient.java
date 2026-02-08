package club.catmc.core.shared.api;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;

/**
 * HTTP client for communicating with the Core REST API.
 * Uses Java 11+ HttpClient with async operations.
 */
public class ApiClient {
    private static final Logger log = LoggerFactory.getLogger(ApiClient.class);

    private final HttpClient httpClient;
    private final Gson gson;
    private final String baseUrl;
    private final String apiKey;

    /**
     * Creates a new ApiClient instance.
     *
     * @param baseUrl The base URL of the API (e.g., "http://localhost:3000/api")
     * @param apiKey  The API key for authentication
     */
    public ApiClient(String baseUrl, String apiKey) {
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        this.apiKey = apiKey;
        this.gson = new GsonBuilder()
                .setDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
                .create();
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(30))
                .build();
        log.info("ApiClient initialized with base URL: {}", this.baseUrl);
    }

    /**
     * Performs a GET request.
     *
     * @param path The endpoint path (e.g., "/players/uuid")
     * @return CompletableFuture containing the response body
     */
    public CompletableFuture<String> get(String path) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + path))
                .header("X-API-Key", apiKey)
                .header("Accept", "application/json")
                .timeout(Duration.ofSeconds(30))
                .GET()
                .build();

        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    if (response.statusCode() >= 400) {
                        throw new ApiClientException(response.statusCode(), response.body());
                    }
                    return response.body();
                })
                .exceptionally(e -> {
                    log.error("GET request failed: {}{}", baseUrl, path, e);
                    throw new ApiClientException("GET request failed: " + e.getMessage(), e);
                });
    }

    /**
     * Performs a GET request and deserializes the response to a specific type.
     *
     * @param path        The endpoint path
     * @param responseType The class to deserialize the response to
     * @param <T>         The type of the response
     * @return CompletableFuture containing the deserialized response
     */
    public <T> CompletableFuture<T> get(String path, Class<T> responseType) {
        return get(path).thenApply(body -> gson.fromJson(body, responseType));
    }

    /**
     * Performs a GET request and deserializes the response to a specific type.
     *
     * @param path        The endpoint path
     * @param responseType The type token to deserialize the response to
     * @param <T>         The type of the response
     * @return CompletableFuture containing the deserialized response
     */
    public <T> CompletableFuture<T> get(String path, java.lang.reflect.Type responseType) {
        return get(path).thenApply(body -> gson.fromJson(body, responseType));
    }

    /**
     * Performs a POST request with a JSON body.
     *
     * @param path  The endpoint path
     * @param body  The request body object (will be serialized to JSON)
     * @return CompletableFuture containing the response body
     */
    public CompletableFuture<String> post(String path, Object body) {
        String jsonBody = gson.toJson(body);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + path))
                .header("X-API-Key", apiKey)
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .timeout(Duration.ofSeconds(30))
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();

        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    if (response.statusCode() >= 400) {
                        throw new ApiClientException(response.statusCode(), response.body());
                    }
                    return response.body();
                })
                .exceptionally(e -> {
                    log.error("POST request failed: {}{}", baseUrl, path, e);
                    throw new ApiClientException("POST request failed: " + e.getMessage(), e);
                });
    }

    /**
     * Performs a POST request with a JSON body and deserializes the response.
     *
     * @param path         The endpoint path
     * @param body         The request body object (will be serialized to JSON)
     * @param responseType The class to deserialize the response to
     * @param <T>         The type of the response
     * @return CompletableFuture containing the deserialized response
     */
    public <T> CompletableFuture<T> post(String path, Object body, Class<T> responseType) {
        return post(path, body).thenApply(responseBody -> gson.fromJson(responseBody, responseType));
    }

    /**
     * Performs a PUT request with a JSON body.
     *
     * @param path  The endpoint path
     * @param body  The request body object (will be serialized to JSON)
     * @return CompletableFuture containing the response body
     */
    public CompletableFuture<String> put(String path, Object body) {
        String jsonBody = gson.toJson(body);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + path))
                .header("X-API-Key", apiKey)
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .timeout(Duration.ofSeconds(30))
                .PUT(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();

        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    if (response.statusCode() >= 400) {
                        throw new ApiClientException(response.statusCode(), response.body());
                    }
                    return response.body();
                })
                .exceptionally(e -> {
                    log.error("PUT request failed: {}{}", baseUrl, path, e);
                    throw new ApiClientException("PUT request failed: " + e.getMessage(), e);
                });
    }

    /**
     * Performs a PUT request with a JSON body and deserializes the response.
     *
     * @param path         The endpoint path
     * @param body         The request body object (will be serialized to JSON)
     * @param responseType The class to deserialize the response to
     * @param <T>         The type of the response
     * @return CompletableFuture containing the deserialized response
     */
    public <T> CompletableFuture<T> put(String path, Object body, Class<T> responseType) {
        return put(path, body).thenApply(responseBody -> gson.fromJson(responseBody, responseType));
    }

    /**
     * Performs a DELETE request.
     *
     * @param path The endpoint path
     * @return CompletableFuture containing the response body
     */
    public CompletableFuture<String> delete(String path) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + path))
                .header("X-API-Key", apiKey)
                .header("Accept", "application/json")
                .timeout(Duration.ofSeconds(30))
                .DELETE()
                .build();

        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    if (response.statusCode() >= 400) {
                        throw new ApiClientException(response.statusCode(), response.body());
                    }
                    return response.body();
                })
                .exceptionally(e -> {
                    log.error("DELETE request failed: {}{}", baseUrl, path, e);
                    throw new ApiClientException("DELETE request failed: " + e.getMessage(), e);
                });
    }

    /**
     * Gets the Gson instance for custom serialization/deserialization.
     *
     * @return The Gson instance
     */
    public Gson getGson() {
        return gson;
    }

    /**
     * Checks if the API client is connected (always returns true for HTTP).
     *
     * @return true
     */
    public boolean isConnected() {
        return true;
    }

    /**
     * Exception thrown when API requests fail.
     */
    public static class ApiClientException extends RuntimeException {
        private final int statusCode;
        private final String responseBody;

        public ApiClientException(int statusCode, String responseBody) {
            super("API request failed with status " + statusCode + ": " + responseBody);
            this.statusCode = statusCode;
            this.responseBody = responseBody;
        }

        public ApiClientException(String message, Throwable cause) {
            super(message, cause);
            this.statusCode = -1;
            this.responseBody = null;
        }

        public int getStatusCode() {
            return statusCode;
        }

        public String getResponseBody() {
            return responseBody;
        }
    }
}
