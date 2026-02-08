package club.catmc.core.bungee.manager;

import club.catmc.core.bungee.BungeePlugin;
import club.catmc.core.shared.player.Player;
import club.catmc.core.shared.rank.Rank;
import club.catmc.core.shared.rank.RankDao;
import club.catmc.core.shared.player.PlayerDao;
import club.catmc.core.shared.grant.GrantDao;
import club.catmc.core.shared.ws.WebSocketManager;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages player profiles, grants, and ranks for the BungeeCord plugin
 */
public class PlayerManager {

    private static final Logger log = LoggerFactory.getLogger(PlayerManager.class);

    private final BungeePlugin plugin;
    private final PlayerDao playerDao;
    private final GrantDao grantDao;
    private final RankDao rankDao;
    private final WebSocketManager wsManager;

    // Cache of online players by UUID
    private final Map<UUID, Player> onlinePlayers;

    // Cache of all ranks
    private final Map<String, Rank> rankCache;

    /**
     * Creates a new PlayerManager
     *
     * @param plugin The Bungee plugin instance
     * @param playerDao The PlayerDao instance
     * @param grantDao The GrantDao instance
     * @param rankDao The RankDao instance
     * @param wsManager The WebSocketManager instance
     */
    public PlayerManager(BungeePlugin plugin, PlayerDao playerDao, GrantDao grantDao, RankDao rankDao, WebSocketManager wsManager) {
        this.plugin = plugin;
        this.playerDao = playerDao;
        this.grantDao = grantDao;
        this.rankDao = rankDao;
        this.wsManager = wsManager;
        this.onlinePlayers = new ConcurrentHashMap<>();
        this.rankCache = new ConcurrentHashMap<>();
    }

    /**
     * Initializes the player manager by loading all ranks
     *
     * @return CompletableFuture that completes when initialized
     */
    public CompletableFuture<Void> initialize() {
        log.info("[PlayerManager] Initializing...");

        return rankDao.findAll().thenAccept(ranks -> {
            rankCache.clear();
            for (Rank rank : ranks) {
                rankCache.put(rank.getId(), rank);
            }
            log.info("[PlayerManager] Loaded " + rankCache.size() + " ranks into cache");
        }).exceptionally(e -> {
            log.error("[PlayerManager] Failed to initialize: " + e.getMessage());
            return null;
        });
    }

    /**
     * Loads a player's profile from the database
     * Creates a new player entry if they don't exist
     *
     * @param uuid The player's UUID
     * @param username The player's username
     * @return CompletableFuture containing the loaded Player
     */
    public CompletableFuture<Player> loadProfile(UUID uuid, String username) {
        log.info("[PlayerManager] Loading profile for: " + username);

        return playerDao.findByUuid(uuid).thenComposeAsync(playerOpt -> {
            Player player;

            if (playerOpt.isEmpty()) {
                // New player, create profile
                player = new Player(uuid, username);
                player.setFirstLoginIfNotSet();
                player.setOnline(true);
                player.updateLastLogin();

                return playerDao.save(player).thenApply(v -> {
                    log.info("[PlayerManager] Created new profile for: " + username);
                    return player;
                });
            } else {
                // Existing player
                player = playerOpt.get();
                player.setOnline(true);
                player.updateLastLogin();

                // Save updated status
                return playerDao.save(player).thenApply(v -> player);
            }
        }).thenCompose(player -> {
            // Load grants
            return grantDao.findActiveByPlayer(uuid).thenApply(grants -> {
                player.setGrants(grants);

                // Set active rank from first valid grant
                String activeRankId = player.getActiveRankId();
                if (activeRankId != null) {
                    Rank rank = rankCache.get(activeRankId);
                    if (rank != null) {
                        player.setRank(rank);
                    }
                }

                // Cache the player
                onlinePlayers.put(uuid, player);

                log.info("[PlayerManager] Loaded profile for " + username +
                        " with " + grants.size() + " grants, rank: " +
                        (player.getRank() != null ? player.getRank().getName() : "None"));

                return player;
            });
        }).exceptionally(e -> {
            log.error("[PlayerManager] Failed to load profile for " + username + ": " + e.getMessage());
            return null;
        });
    }

    /**
     * Unloads a player's profile and saves to database
     *
     * @param uuid The player's UUID
     * @return CompletableFuture that completes when unloaded
     */
    public CompletableFuture<Void> unloadProfile(UUID uuid) {
        Player player = onlinePlayers.remove(uuid);

        if (player == null) {
            log.warn("[PlayerManager] Attempted to unload non-existent player: " + uuid);
            return CompletableFuture.completedFuture(null);
        }

        log.info("[PlayerManager] Unloading profile for: " + player.getUsername());

        player.setOnline(false);
        player.updateLastLogin();

        return playerDao.save(player).thenRun(() -> {
            log.info("[PlayerManager] Saved profile for: " + player.getUsername());
        }).exceptionally(e -> {
            log.error("[PlayerManager] Failed to save profile for " + player.getUsername() + ": " + e.getMessage());
            return null;
        });
    }

    /**
     * Gets an online player by UUID
     *
     * @param uuid The player's UUID
     * @return The Player, or null if not found
     */
    public Player getPlayer(UUID uuid) {
        return onlinePlayers.get(uuid);
    }

    /**
     * Gets an online player by username
     *
     * @param username The player's username
     * @return The Player, or null if not found
     */
    public Player getPlayerByUsername(String username) {
        return onlinePlayers.values().stream()
                .filter(p -> p.getUsername().equalsIgnoreCase(username))
                .findFirst()
                .orElse(null);
    }

    /**
     * Gets an online player by ProxiedPlayer
     *
     * @param proxiedPlayer The ProxiedPlayer
     * @return The Player, or null if not found
     */
    public Player getPlayer(ProxiedPlayer proxiedPlayer) {
        return onlinePlayers.get(proxiedPlayer.getUniqueId());
    }

    /**
     * Gets all online players
     *
     * @return Collection of online players
     */
    public Collection<Player> getOnlinePlayers() {
        return onlinePlayers.values();
    }

    /**
     * Gets a rank by ID
     *
     * @param rankId The rank ID
     * @return The Rank, or null if not found
     */
    public Rank getRank(String rankId) {
        return rankCache.get(rankId);
    }

    /**
     * Gets all cached ranks
     *
     * @return Collection of all ranks
     */
    public Collection<Rank> getAllRanks() {
        return rankCache.values();
    }

    /**
     * Refreshes the rank cache from the database
     *
     * @return CompletableFuture that completes when refreshed
     */
    public CompletableFuture<Void> refreshRankCache() {
        log.info("[PlayerManager] Refreshing rank cache...");

        return rankDao.findAll().thenAccept(ranks -> {
            rankCache.clear();
            for (Rank rank : ranks) {
                rankCache.put(rank.getId(), rank);
            }
            log.info("[PlayerManager] Refreshed rank cache with " + rankCache.size() + " ranks");
        }).exceptionally(e -> {
            log.error("[PlayerManager] Failed to refresh rank cache: " + e.getMessage());
            return null;
        });
    }

    /**
     * Reloads a specific player's grants from the database
     *
     * @param uuid The player's UUID
     * @return CompletableFuture that completes when reloaded
     */
    public CompletableFuture<Void> reloadPlayerGrants(UUID uuid) {
        Player player = onlinePlayers.get(uuid);

        if (player == null) {
            return CompletableFuture.completedFuture(null);
        }

        return grantDao.findActiveByPlayer(uuid).thenAccept(grants -> {
            player.setGrants(grants);

            // Update active rank
            String activeRankId = player.getActiveRankId();
            if (activeRankId != null) {
                Rank rank = rankCache.get(activeRankId);
                player.setRank(rank);
            } else {
                player.setRank(null);
            }

            log.info("[PlayerManager] Reloaded grants for " + player.getUsername());

            // Recalculate permissions after grant reload
            recalculatePermissions(uuid);

            // Notify all Bukkit servers of the grant change
            notifyServersOfGrantChange(uuid);
        }).exceptionally(e -> {
            log.error("[PlayerManager] Failed to reload grants for " + player.getUsername() + ": " + e.getMessage());
            return null;
        });
    }

    /**
     * Shuts down the player manager and saves all online players
     *
     * @return CompletableFuture that completes when shutdown
     */
    public CompletableFuture<Void> shutdown() {
        log.info("[PlayerManager] Shutting down...");

        List<CompletableFuture<Void>> futures = new ArrayList<>();

        for (UUID uuid : onlinePlayers.keySet()) {
            futures.add(unloadProfile(uuid));
        }

        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).thenRun(() -> {
            log.info("[PlayerManager] Shutdown complete");
        });
    }

    /**
     * Setup permissions for a BungeeCord player based on their Core profile
     *
     * @param player The BungeeCord player
     */
    public void setupPermissions(ProxiedPlayer player) {
        UUID uuid = player.getUniqueId();
        Player corePlayer = onlinePlayers.get(uuid);

        if (corePlayer == null) {
            log.warn("[PlayerManager] Cannot setup permissions for " + player.getName() + ": profile not loaded");
            return;
        }

        // Get all permissions for the player (rank + additional)
        Set<String> permissions = new HashSet<>();

        // Add rank permissions
        if (corePlayer.getRank() != null && corePlayer.getRank().getPermissions() != null) {
            permissions.addAll(corePlayer.getRank().getPermissions());
        }

        // Add additional permissions
        if (corePlayer.getAdditionalPermissions() != null) {
            permissions.addAll(corePlayer.getAdditionalPermissions());
        }

        // Clear existing permissions and set new ones
        // Note: BungeeCord doesn't have a way to clear all permissions, so we need to track them
        // For now, we'll just set the permissions directly
        for (String permission : permissions) {
            boolean value = true;
            // Handle negative permissions (starting with -)
            if (permission.startsWith("-")) {
                permission = permission.substring(1);
                value = false;
            }
            player.setPermission(permission, value);
        }

        log.info("[PlayerManager] Setup " + permissions.size() + " permissions for " + player.getName() +
                " (rank: " + (corePlayer.getRank() != null ? corePlayer.getRank().getName() : "None") + ")");
    }

    /**
     * Recalculate permissions for a player (use after rank/grant changes)
     *
     * @param uuid The player's UUID
     */
    public void recalculatePermissions(UUID uuid) {
        Player corePlayer = onlinePlayers.get(uuid);
        if (corePlayer == null) {
            return;
        }

        ProxiedPlayer bungeePlayer = plugin.getProxy().getPlayer(uuid);
        if (bungeePlayer == null || !bungeePlayer.isConnected()) {
            return;
        }

        setupPermissions(bungeePlayer);
    }

    /**
     * Checks if a player has a permission using the Core system
     *
     * @param uuid The player's UUID
     * @param permission The permission node
     * @return true if the player has the permission
     */
    public boolean hasPermission(UUID uuid, String permission) {
        Player corePlayer = onlinePlayers.get(uuid);
        if (corePlayer == null) {
            return false;
        }
        return corePlayer.hasPermission(permission);
    }

    /**
     * Notifies all servers of a grant change for a player via WebSocket
     *
     * @param uuid The player's UUID
     */
    private void notifyServersOfGrantChange(UUID uuid) {
        if (wsManager != null && wsManager.isConnected()) {
            wsManager.broadcastGrantChange(uuid);
            log.info("[PlayerManager] Broadcast grant change notification for " + uuid);
        }
    }
}
