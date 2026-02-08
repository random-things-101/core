package club.catmc.core.bungee;

import club.catmc.core.bungee.commands.CoreCommand;
import club.catmc.core.bungee.commands.MessageCommand;
import club.catmc.core.bungee.commands.ReplyCommand;
import club.catmc.core.bungee.config.ApiConfig;
import club.catmc.core.bungee.listener.PlayerListener;
import club.catmc.core.bungee.manager.MessageManager;
import club.catmc.core.bungee.manager.PlayerManager;
import club.catmc.core.shared.api.ApiClient;
import club.catmc.core.shared.grant.GrantDao;
import club.catmc.core.shared.punishment.PunishmentDao;
import club.catmc.core.shared.player.PlayerDao;
import club.catmc.core.shared.rank.RankDao;
import club.catmc.core.shared.ws.WebSocketManager;
import co.aikar.commands.BungeeCommandManager;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.config.ConfigurationProvider;
import net.md_5.bungee.config.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.concurrent.CompletableFuture;

/**
 * Main BungeeCord plugin class
 */
public class BungeePlugin extends Plugin {

    private ApiClient apiClient;
    private WebSocketManager wsManager;
    private BungeeCommandManager commandManager;
    private Configuration config;
    private PlayerManager playerManager;
    private MessageManager messageManager;
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

    public BungeeCommandManager getCommandManager() {
        return commandManager;
    }

    public Configuration getConfigConfiguration() {
        return config;
    }

    public void setConfigConfiguration(Configuration config) {
        this.config = config;
    }

    public PlayerManager getPlayerManager() {
        return playerManager;
    }

    public MessageManager getMessageManager() {
        return messageManager;
    }

    public PunishmentDao getPunishmentDao() {
        return punishmentDao;
    }

    @Override
    public void onLoad() {
        getLogger().info("Loading Core Bungee Plugin...");
    }

    @Override
    public void onEnable() {
        // Load configuration
        loadConfig();

        // Load API configuration
        ApiConfig apiConfig = loadApiConfig();

        // Initialize API client
        apiClient = new ApiClient(apiConfig.getBaseUrl(), apiConfig.getApiKey());
        getLogger().info("ApiClient initialized with base URL: " + apiConfig.getBaseUrl());

        // Initialize WebSocket client
        wsManager = new WebSocketManager(
            apiConfig.getWsUrl(),
            "bungee",  // Explicit server type
            apiConfig.getServerName(),
            apiConfig.getApiKey()
        );
        wsManager.connect();
        getLogger().info("WebSocketManager initialized as 'bungee' proxy");

        // Initialize DAOs with ApiClient
        playerDao = new PlayerDao(apiClient);
        grantDao = new GrantDao(apiClient);
        rankDao = new RankDao(apiClient);
        punishmentDao = new PunishmentDao(apiClient);

        // Initialize PlayerManager
        playerManager = new PlayerManager(this, playerDao, grantDao, rankDao, wsManager);
        playerManager.initialize().thenRun(() -> {
            getLogger().info("PlayerManager initialized!");

            // Initialize MessageManager
            messageManager = new MessageManager();

            // Setup ACF Command Manager
            setupCommands();

            // Register events
            getProxy().getPluginManager().registerListener(this, new PlayerListener(this, playerManager));

            getLogger().info("Core Bungee Plugin enabled!");
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

        getLogger().info("Core Bungee Plugin disabled!");
    }

    /**
     * Loads the configuration from config.yml
     */
    private void loadConfig() {
        try {
            File dataFolder = getDataFolder();
            if (!dataFolder.exists()) {
                dataFolder.mkdirs();
            }

            File configFile = new File(dataFolder, "config.yml");

            if (!configFile.exists()) {
                try (InputStream in = getResourceAsStream("config.yml")) {
                    if (in != null) {
                        Files.copy(in, configFile.toPath());
                    }
                } catch (IOException e) {
                    getLogger().warning("Failed to save default config: " + e.getMessage());
                }
            }

            config = ConfigurationProvider.getProvider(YamlConfiguration.class).load(configFile);
        } catch (IOException e) {
            getLogger().severe("Failed to load config: " + e.getMessage());
        }
    }

    /**
     * Loads API configuration from config.yml
     *
     * @return ApiConfig instance
     */
    private ApiConfig loadApiConfig() {
        return new ApiConfig(
                config.getString("api.base-url", "http://localhost:3000/api"),
                config.getString("api.api-key", "your-secret-api-key-here"),
                config.getString("api.ws-url", "ws://localhost:3000/ws"),
                config.getString("api.server-name", "bungee-proxy")
        );
    }

    /**
     * Sets up the ACF command manager
     */
    private void setupCommands() {
        commandManager = new BungeeCommandManager(this);

        // Register command completions
        commandManager.getCommandCompletions().registerAsyncCompletion("players", c -> {
            return getProxy().getPlayers().stream()
                    .map(player -> player.getName())
                    .toList();
        });

        // Register commands
        commandManager.registerCommand(new CoreCommand());
        commandManager.registerCommand(new MessageCommand(this));
        commandManager.registerCommand(new ReplyCommand(this));
    }

    /**
     * Saves the configuration to disk
     */
    public void saveConfig() {
        try {
            File configFile = new File(getDataFolder(), "config.yml");
            ConfigurationProvider.getProvider(YamlConfiguration.class).save(config, configFile);
        } catch (IOException e) {
            getLogger().severe("Failed to save config: " + e.getMessage());
        }
    }

    /**
     * Reloads the configuration from disk
     */
    public void reloadConfig() {
        loadConfig();
    }
}
