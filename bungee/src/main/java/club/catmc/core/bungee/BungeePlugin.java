package club.catmc.core.bungee;

import club.catmc.core.bungee.commands.CoreCommand;
import club.catmc.core.bungee.commands.MessageCommand;
import club.catmc.core.bungee.commands.ReplyCommand;
import club.catmc.core.bungee.config.DatabaseConfig;
import club.catmc.core.bungee.listener.PlayerListener;
import club.catmc.core.bungee.listener.PluginMessageListener;
import club.catmc.core.bungee.manager.MessageManager;
import club.catmc.core.bungee.manager.PlayerManager;
import club.catmc.core.shared.db.DatabaseManager;
import club.catmc.core.shared.grant.GrantDao;
import club.catmc.core.shared.player.PlayerDao;
import club.catmc.core.shared.rank.RankDao;
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

    private DatabaseManager databaseManager;
    private BungeeCommandManager commandManager;
    private Configuration config;
    private PlayerManager playerManager;
    private MessageManager messageManager;
    private PlayerDao playerDao;
    private GrantDao grantDao;
    private RankDao rankDao;

    public DatabaseManager getDatabaseManager() {
        return databaseManager;
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

    @Override
    public void onLoad() {
        getLogger().info("Loading Core Bungee Plugin...");
    }

    @Override
    public void onEnable() {
        // Load configuration
        loadConfig();

        // Load database configuration
        DatabaseConfig dbConfig = loadDatabaseConfig();

        // Initialize database manager
        databaseManager = new DatabaseManager(
                dbConfig.getHost(),
                dbConfig.getPort(),
                dbConfig.getDatabase(),
                dbConfig.getUsername(),
                dbConfig.getPassword()
        );

        // Connect to database
        databaseManager.connect().thenCompose(v -> {
            getLogger().info("Database connection established!");

            // Initialize DAOs
            playerDao = new PlayerDao(databaseManager);
            grantDao = new GrantDao(databaseManager);
            rankDao = new RankDao(databaseManager);

            // Create tables
            CompletableFuture<Void> createPlayers = playerDao.createTable();
            CompletableFuture<Void> createGrants = grantDao.createTable();
            CompletableFuture<Void> createRanks = rankDao.createTable();

            return CompletableFuture.allOf(createPlayers, createGrants, createRanks);
        }).thenCompose(v -> {
            // Initialize PlayerManager
            playerManager = new PlayerManager(this, playerDao, grantDao, rankDao);
            return playerManager.initialize();
        }).thenRun(() -> {
            getLogger().info("PlayerManager initialized!");

            // Initialize MessageManager
            messageManager = new MessageManager();

            // Setup ACF Command Manager
            setupCommands();

            // Register events
            getProxy().getPluginManager().registerListener(this, new PlayerListener(this, playerManager));
            getProxy().getPluginManager().registerListener(this, new PluginMessageListener(this, playerManager));

            // Register plugin messaging channel
            getProxy().registerChannel("core:channel");

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

        // Save all online players
        if (playerManager != null) {
            playerManager.shutdown().join();
        }

        if (databaseManager != null) {
            databaseManager.disconnect().join();
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
     * Loads database configuration from config.yml
     *
     * @return DatabaseConfig instance
     */
    private DatabaseConfig loadDatabaseConfig() {
        return new DatabaseConfig(
                config.getString("database.host", "localhost"),
                config.getInt("database.port", 3306),
                config.getString("database.name", "minecraft"),
                config.getString("database.user", "root"),
                config.getString("database.password", "")
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
