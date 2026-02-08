package club.catmc.core.bungee.config;

/**
 * API configuration settings
 */
public class ApiConfig {

    private String baseUrl;
    private String apiKey;
    private String wsUrl;
    private String serverName;

    public ApiConfig(String baseUrl, String apiKey, String wsUrl, String serverName) {
        this.baseUrl = baseUrl;
        this.apiKey = apiKey;
        this.wsUrl = wsUrl;
        this.serverName = serverName;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getWsUrl() {
        return wsUrl;
    }

    public void setWsUrl(String wsUrl) {
        this.wsUrl = wsUrl;
    }

    public String getServerName() {
        return serverName;
    }

    public void setServerName(String serverName) {
        this.serverName = serverName;
    }

    /**
     * Creates an ApiConfig with default values
     */
    public static ApiConfig createDefault() {
        return new ApiConfig(
            "http://localhost:3000/api",
            "your-secret-api-key-here",
            "ws://localhost:3000/ws",
            "bungee-proxy"
        );
    }
}
