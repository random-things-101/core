package club.catmc.core.shared.ws;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * WebSocket client for real-time communication with the Core API
 */
public class CoreWebSocketClient extends WebSocketClient {

    private static final Logger log = LoggerFactory.getLogger(CoreWebSocketClient.class);
    private final Gson gson;
    private final String serverType;
    private final String serverName;
    private final String apiKey;

    private Consumer<JsonObject> messageHandler;
    private Runnable onConnect;
    private Runnable onDisconnect;
    private volatile boolean isConnected = false;

    /**
     * Create a new WebSocket client
     *
     * @param serverUri  The WebSocket server URI (e.g., ws://localhost:3000/ws)
     * @param serverType The type of server ('paper' for game servers, 'bungee' for proxy)
     * @param serverName The name of this server instance
     * @param apiKey     The API key for authentication
     */
    public CoreWebSocketClient(String serverUri, String serverType, String serverName, String apiKey) {
        super(URI.create(serverUri + "?api_key=" + apiKey + "&type=" + serverType + "&name=" + serverName));
        this.gson = new Gson();
        this.serverType = serverType;
        this.serverName = serverName;
        this.apiKey = apiKey;

        // Set connection timeout to 30 seconds
        this.setConnectionLostTimeout(30);
    }

    @Override
    public void onOpen(ServerHandshake handshakedata) {
        isConnected = true;
        log.info("[WebSocket] Connected to Core API as {}/{}", serverType, serverName);

        if (onConnect != null) {
            onConnect.run();
        }
    }

    @Override
    public void onMessage(String message) {
        log.debug("[WebSocket] Received message: {}", message);

        try {
            JsonObject json = gson.fromJson(message, JsonObject.class);
            String type = json.has("type") ? json.get("type").getAsString() : "UNKNOWN";

            log.info("[WebSocket] Received {} message", type);

            if (messageHandler != null) {
                messageHandler.accept(json);
            }
        } catch (Exception e) {
            log.error("[WebSocket] Failed to parse message: {}", e.getMessage());
        }
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        isConnected = false;
        log.warn("[WebSocket] Disconnected from Core API: {} (code: {})", reason, code);

        if (onDisconnect != null) {
            onDisconnect.run();
        }

        // Attempt to reconnect after 5 seconds
        scheduleReconnect();
    }

    @Override
    public void onError(Exception ex) {
        log.error("[WebSocket] Error occurred: {}", ex.getMessage());
    }

    /**
     * Set the message handler for incoming messages
     */
    public void setMessageHandler(Consumer<JsonObject> handler) {
        this.messageHandler = handler;
    }

    /**
     * Set callback for connection established
     */
    public void setOnConnect(Runnable callback) {
        this.onConnect = callback;
    }

    /**
     * Set callback for disconnection
     */
    public void setOnDisconnect(Runnable callback) {
        this.onDisconnect = callback;
    }

    /**
     * Check if currently connected
     */
    public boolean isConnected() {
        return isConnected && this.isOpen();
    }

    /**
     * Send a message to the server
     *
     * @param type The message type
     * @param data The message data as JsonObject
     */
    public void send(String type, JsonObject data) {
        if (!isConnected()) {
            log.warn("[WebSocket] Cannot send message: not connected");
            return;
        }

        JsonObject message = new JsonObject();
        message.addProperty("type", type);
        message.add("data", data);
        message.addProperty("serverType", serverType);
        message.addProperty("serverName", serverName);
        message.addProperty("timestamp", java.time.Instant.now().toString());

        String json = gson.toJson(message);
        send(json);
        log.debug("[WebSocket] Sent: {}", type);
    }

    /**
     * Schedule a reconnection attempt
     */
    private void scheduleReconnect() {
        new Thread(() -> {
            try {
                TimeUnit.SECONDS.sleep(5);
                log.info("[WebSocket] Attempting to reconnect...");
                this.reconnect();
            } catch (InterruptedException e) {
                log.error("[WebSocket] Reconnect interrupted: {}", e.getMessage());
            }
        }).start();
    }

    /**
     * Connect to the WebSocket server
     *
     * @return true if connection initiated successfully
     */
    public boolean connectSync() {
        try {
            this.connect();
            return true;
        } catch (Exception e) {
            log.error("[WebSocket] Failed to connect: {}", e.getMessage());
            return false;
        }
    }
}
