package club.catmc.core.shared.ws;

import com.google.gson.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;
import java.util.function.Consumer;

/**
 * Manages WebSocket connection and message routing
 */
public class WebSocketManager {

    private static final Logger log = LoggerFactory.getLogger(WebSocketManager.class);

    private final CoreWebSocketClient client;
    private final String serverType;  // 'paper' or 'bungee'
    private final String serverName;

    // Message handlers
    private Consumer<UUID> onGrantChange;
    private Consumer<String> onRankChange;
    private Consumer<UUID> onPlayerUpdate;
    private Consumer<PrivateMessage> onPrivateMessage;
    private Consumer<PunishmentExecute> onPunishmentExecute;

    public WebSocketManager(String wsUrl, String serverType, String serverName, String apiKey) {
        this.serverType = serverType;
        this.serverName = serverName;
        this.client = new CoreWebSocketClient(wsUrl, serverType, serverName, apiKey);

        setupMessageHandler();
        setupConnectionCallbacks();
    }

    /**
     * Setup message handler to route messages to appropriate handlers
     */
    private void setupMessageHandler() {
        client.setMessageHandler(json -> {
            String type = json.has("type") ? json.get("type").getAsString() : "UNKNOWN";

            switch (type) {
                case "GRANT_CHANGE":
                    handleGrantChange(json);
                    break;

                case "RANK_CHANGE":
                    handleRankChange(json);
                    break;

                case "PLAYER_UPDATE":
                    handlePlayerUpdate(json);
                    break;

                case "PRIVATE_MESSAGE":
                    handlePrivateMessage(json);
                    break;

                case "PUNISH_EXECUTE":
                    handlePunishmentExecute(json);
                    break;

                case "CONNECTED":
                    log.info("[WebSocket] Server confirmed connection: {}", json.get("message").getAsString());
                    break;

                case "ERROR":
                    log.error("[WebSocket] Server error: {}", json.get("message").getAsString());
                    break;

                default:
                    log.warn("[WebSocket] Unknown message type: {}", type);
            }
        });
    }

    /**
     * Setup connection callbacks
     */
    private void setupConnectionCallbacks() {
        client.setOnConnect(() -> {
            log.info("[WebSocket] Connected and ready to receive messages");
        });

        client.setOnDisconnect(() -> {
            log.warn("[WebSocket] Disconnected from Core API");
        });
    }

    /**
     * Handle GRANT_CHANGE message
     */
    private void handleGrantChange(JsonObject json) {
        try {
            String playerUuidStr = json.get("playerUuid").getAsString();
            UUID playerUuid = UUID.fromString(playerUuidStr);

            log.info("[WebSocket] Grant change notification for player: {}", playerUuidStr);

            if (onGrantChange != null) {
                onGrantChange.accept(playerUuid);
            }
        } catch (Exception e) {
            log.error("[WebSocket] Failed to handle GRANT_CHANGE: {}", e.getMessage());
        }
    }

    /**
     * Handle RANK_CHANGE message
     */
    private void handleRankChange(JsonObject json) {
        try {
            String rankId = json.get("rankId").getAsString();

            log.info("[WebSocket] Rank change notification for rank: {}", rankId);

            if (onRankChange != null) {
                onRankChange.accept(rankId);
            }
        } catch (Exception e) {
            log.error("[WebSocket] Failed to handle RANK_CHANGE: {}", e.getMessage());
        }
    }

    /**
     * Handle PLAYER_UPDATE message
     */
    private void handlePlayerUpdate(JsonObject json) {
        try {
            String playerUuidStr = json.get("playerUuid").getAsString();
            UUID playerUuid = UUID.fromString(playerUuidStr);

            log.info("[WebSocket] Player update notification for: {}", playerUuidStr);

            if (onPlayerUpdate != null) {
                onPlayerUpdate.accept(playerUuid);
            }
        } catch (Exception e) {
            log.error("[WebSocket] Failed to handle PLAYER_UPDATE: {}", e.getMessage());
        }
    }

    /**
     * Handle PRIVATE_MESSAGE message
     */
    private void handlePrivateMessage(JsonObject json) {
        try {
            String targetPlayer = json.get("targetPlayer").getAsString();
            String senderName = json.get("senderName").getAsString();
            String message = json.get("message").getAsString();

            log.info("[WebSocket] Private message from {} to {}", senderName, targetPlayer);

            if (onPrivateMessage != null) {
                onPrivateMessage.accept(new PrivateMessage(targetPlayer, senderName, message));
            }
        } catch (Exception e) {
            log.error("[WebSocket] Failed to handle PRIVATE_MESSAGE: {}", e.getMessage());
        }
    }

    /**
     * Handle PUNISH_EXECUTE message
     */
    private void handlePunishmentExecute(JsonObject json) {
        try {
            String playerUuidStr = json.get("playerUuid").getAsString();
            UUID playerUuid = UUID.fromString(playerUuidStr);
            String punishmentType = json.get("punishmentType").getAsString();
            String reason = json.has("reason") && !json.get("reason").isJsonNull() ? json.get("reason").getAsString() : null;

            log.info("[WebSocket] Punishment execution for player: {}, type: {}", playerUuidStr, punishmentType);

            if (onPunishmentExecute != null) {
                onPunishmentExecute.accept(new PunishmentExecute(playerUuid, punishmentType, reason));
            }
        } catch (Exception e) {
            log.error("[WebSocket] Failed to handle PUNISH_EXECUTE: {}", e.getMessage());
        }
    }

    // Setter methods for message handlers

    public void onGrantChange(Consumer<UUID> handler) {
        this.onGrantChange = handler;
    }

    public void onRankChange(Consumer<String> handler) {
        this.onRankChange = handler;
    }

    public void onPlayerUpdate(Consumer<UUID> handler) {
        this.onPlayerUpdate = handler;
    }

    public void onPrivateMessage(Consumer<PrivateMessage> handler) {
        this.onPrivateMessage = handler;
    }

    public void onPunishmentExecute(Consumer<PunishmentExecute> handler) {
        this.onPunishmentExecute = handler;
    }

    /**
     * Connect to the WebSocket server
     */
    public void connect() {
        log.info("[WebSocket] Connecting to Core API...");
        client.connectSync();
    }

    /**
     * Disconnect from the WebSocket server
     */
    public void disconnect() {
        log.info("[WebSocket] Disconnecting from Core API...");
        client.close();
    }

    /**
     * Check if connected
     */
    public boolean isConnected() {
        return client.isConnected();
    }

    /**
     * Broadcast a grant change to all servers
     */
    public void broadcastGrantChange(UUID playerUuid) {
        JsonObject data = new JsonObject();
        data.addProperty("playerUuid", playerUuid.toString());
        client.send("GRANT_CHANGE", data);
    }

    /**
     * Broadcast a rank change to all servers
     */
    public void broadcastRankChange(String rankId) {
        JsonObject data = new JsonObject();
        data.addProperty("rankId", rankId);
        client.send("RANK_CHANGE", data);
    }

    /**
     * Broadcast a player update to all servers
     */
    public void broadcastPlayerUpdate(UUID playerUuid) {
        JsonObject data = new JsonObject();
        data.addProperty("playerUuid", playerUuid.toString());
        client.send("PLAYER_UPDATE", data);
    }

    /**
     * Send a private message to a player
     */
    public void sendPrivateMessage(String targetPlayer, String senderName, String message) {
        JsonObject data = new JsonObject();
        data.addProperty("targetPlayer", targetPlayer);
        data.addProperty("senderName", senderName);
        data.addProperty("message", message);
        client.send("PRIVATE_MESSAGE", data);
    }

    /**
     * Data class for private messages
     */
    public static class PrivateMessage {
        private final String targetPlayer;
        private final String senderName;
        private final String message;

        public PrivateMessage(String targetPlayer, String senderName, String message) {
            this.targetPlayer = targetPlayer;
            this.senderName = senderName;
            this.message = message;
        }

        public String getTargetPlayer() {
            return targetPlayer;
        }

        public String getSenderName() {
            return senderName;
        }

        public String getMessage() {
            return message;
        }
    }

    /**
     * Data class for punishment execution
     */
    public static class PunishmentExecute {
        private final UUID playerUuid;
        private final String punishmentType;
        private final String reason;

        public PunishmentExecute(UUID playerUuid, String punishmentType, String reason) {
            this.playerUuid = playerUuid;
            this.punishmentType = punishmentType;
            this.reason = reason;
        }

        public UUID getPlayerUuid() {
            return playerUuid;
        }

        public String getPunishmentType() {
            return punishmentType;
        }

        public String getReason() {
            return reason;
        }
    }
}
