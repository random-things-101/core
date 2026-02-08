package club.catmc.core.bukkit;

import club.catmc.core.bukkit.commands.CoreCommand;
import club.catmc.core.bukkit.config.ApiConfig;
import club.catmc.core.bukkit.listener.ChatListener;
import club.catmc.core.bukkit.listener.PlayerListener;
import club.catmc.core.bukkit.manager.PlayerManager;
import club.catmc.core.shared.api.ApiClient;
import club.catmc.core.shared.grant.GrantDao;
import club.catmc.core.shared.punishment.PunishmentDao;
import club.catmc.core.shared.player.PlayerDao;
import club.catmc.core.shared.rank.RankDao;
import club.catmc.core.shared.ws.WebSocketManager;
import co.aikar.commands.PaperCommandManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.concurrent.CompletableFuture;

/**
 * Main Bukkit plugin class
 */
public class BukkitPlugin extends JavaPlugin {

    private ApiClient apiClient;
    private WebSocketManager wsManager;
    private PaperCommandManager commandManager;
    private PlayerManager playerManager;
    private PlayerDao playerDao;
    private GrantDao grantDao;
    private RankDao rankDao;
    private PunishmentDao punishmentDao;

    public ApiClient getApiClient() {
        return apiClient;
    }

    public WebSocketManager getWsManager() {
        return wsManager;
    }

    public PaperCommandManager getCommandManager() {
        return commandManager;
    }

    public PlayerManager getPlayerManager() {
        return playerManager;
    }

    public GrantDao getGrantDao() {
        return grantDao;
    }

    public RankDao getRankDao() {
        return rankDao;
    }

    public PunishmentDao getPunishmentDao() {
        return punishmentDao;
    }

    @Override
    public void onLoad() {
        getLogger().info("Loading Core Bukkit Plugin...");
    }

    @Override
    public void onEnable() {
        // Load configuration
        ApiConfig apiConfig = loadApiConfig();

        // Initialize API client
        apiClient = new ApiClient(apiConfig.getBaseUrl(), apiConfig.getApiKey());
        getLogger().info("ApiClient initialized with base URL: " + apiConfig.getBaseUrl());

        // Initialize WebSocket client
        wsManager = new WebSocketManager(
            apiConfig.getWsUrl(),
            "paper",  // Paper server type
            apiConfig.getServerName(),
            apiConfig.getApiKey()
        );
        wsManager.connect();
        getLogger().info("WebSocketManager initialized as 'paper' server");

        // Initialize DAOs with ApiClient
        playerDao = new PlayerDao(apiClient);
        grantDao = new GrantDao(apiClient);
        rankDao = new RankDao(apiClient);
        punishmentDao = new PunishmentDao(apiClient);

        // Initialize PlayerManager
        playerManager = new PlayerManager(this, playerDao, grantDao, rankDao, wsManager);
        playerManager.initialize().thenRun(() -> {
            getLogger().info("PlayerManager initialized!");

            // Save default config
            saveDefaultConfig();

            // Setup ACF Command Manager
            setupCommands();

            // Register events
            getServer().getPluginManager().registerEvents(new ChatListener(this, playerManager), this);
            getServer().getPluginManager().registerEvents(new PlayerListener(this, playerManager), this);

            getLogger().info("Core Bukkit Plugin enabled!");
        }).exceptionally(e -> {
            getLogger().severe("Failed to initialize: " + e.getMessage());
            e.printStackTrace();
            return null;
        });
    }

    @Override
    public void onDisable() {
        if (commandManager != null) {
            commandManager.unregisterCommands();
        }

        // Disconnect WebSocket
        if (wsManager != null) {
            wsManager.disconnect();
        }

        // Save all online players
        if (playerManager != null) {
            playerManager.shutdown().join();
        }

        getLogger().info("Core Bukkit Plugin disabled!");
    }

    /**
     * Loads API configuration from config.yml
     *
     * @return ApiConfig instance
     */
    private ApiConfig loadApiConfig() {
        return new ApiConfig(
                getConfig().getString("api.base-url", "http://localhost:3000/api"),
                getConfig().getString("api.api-key", "your-secret-api-key-here"),
                getConfig().getString("api.ws-url", "ws://localhost:3000/ws"),
                getConfig().getString("api.server-name", "bukkit-server")
        );
    }

    /**
     * Sets up the ACF command manager
     */
    private void setupCommands() {
        commandManager = new PaperCommandManager(this);

        // Register command completions
        commandManager.getCommandCompletions().registerAsyncCompletion("players", c -> {
            return getServer().getOnlinePlayers().stream()
                    .map(player -> player.getName())
                    .toList();
        });

        // Register commands
        commandManager.registerCommand(new CoreCommand());
        commandManager.registerCommand(new club.catmc.core.bukkit.commands.RankCommand(this));
        commandManager.registerCommand(new club.catmc.core.bukkit.commands.GrantCommand(this));
    }
}
