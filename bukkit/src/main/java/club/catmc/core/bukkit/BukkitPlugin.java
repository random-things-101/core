package club.catmc.core.bukkit;

import club.catmc.core.bukkit.commands.CoreCommand;
import club.catmc.core.bukkit.config.DatabaseConfig;
import club.catmc.core.bukkit.listener.ChatListener;
import club.catmc.core.bukkit.listener.CorePluginMessageListener;
import club.catmc.core.bukkit.listener.PlayerListener;
import club.catmc.core.bukkit.manager.PlayerManager;
import club.catmc.core.shared.db.DatabaseManager;
import club.catmc.core.shared.grant.GrantDao;
import club.catmc.core.shared.player.PlayerDao;
import club.catmc.core.shared.rank.RankDao;
import co.aikar.commands.PaperCommandManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.messaging.Messenger;

import java.util.concurrent.CompletableFuture;

/**
 * Main Bukkit plugin class
 */
public class BukkitPlugin extends JavaPlugin {

    private DatabaseManager databaseManager;
    private PaperCommandManager commandManager;
    private PlayerManager playerManager;
    private PlayerDao playerDao;
    private GrantDao grantDao;
    private RankDao rankDao;

    public DatabaseManager getDatabaseManager() {
        return databaseManager;
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

    @Override
    public void onLoad() {
        getLogger().info("Loading Core Bukkit Plugin...");
    }

    @Override
    public void onEnable() {
        // Load configuration
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

            // Save default config
            saveDefaultConfig();

            // Setup ACF Command Manager
            setupCommands();

            // Register events
            getServer().getPluginManager().registerEvents(new ChatListener(this, playerManager), this);
            getServer().getPluginManager().registerEvents(new PlayerListener(this, playerManager), this);

            // Register plugin messaging channel
            Messenger messenger = getServer().getMessenger();
            if (!messenger.isOutgoingChannelRegistered(this, "core:channel")) {
                messenger.registerOutgoingPluginChannel(this, "core:channel");
            }
            if (!messenger.isIncomingChannelRegistered(this, "core:channel")) {
                messenger.registerIncomingPluginChannel(this, "core:channel",
                    new CorePluginMessageListener(this, playerManager));
            }

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

        // Unregister plugin messaging channels
        Messenger messenger = getServer().getMessenger();
        messenger.unregisterIncomingPluginChannel(this, "core:channel");
        messenger.unregisterOutgoingPluginChannel(this, "core:channel");

        // Save all online players
        if (playerManager != null) {
            playerManager.shutdown().join();
        }

        if (databaseManager != null) {
            databaseManager.disconnect().join();
        }

        getLogger().info("Core Bukkit Plugin disabled!");
    }

    /**
     * Loads database configuration from config.yml
     *
     * @return DatabaseConfig instance
     */
    private DatabaseConfig loadDatabaseConfig() {
        return new DatabaseConfig(
                getConfig().getString("database.host", "localhost"),
                getConfig().getInt("database.port", 3306),
                getConfig().getString("database.name", "minecraft"),
                getConfig().getString("database.user", "root"),
                getConfig().getString("database.password", "")
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
