package club.catmc.core.bukkit.manager;

import club.catmc.core.bukkit.BukkitPlugin;
import club.catmc.core.shared.player.Player;
import club.catmc.core.shared.rank.Rank;
import club.catmc.core.shared.rank.RankDao;
import club.catmc.core.shared.player.PlayerDao;
import club.catmc.core.shared.grant.GrantDao;
import org.bukkit.permissions.PermissionAttachment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages player profiles, grants, and ranks for the Bukkit plugin
 */
public class PlayerManager {

    private static final Logger log = LoggerFactory.getLogger(PlayerManager.class);

    private final BukkitPlugin plugin;
    private final PlayerDao playerDao;
    private final GrantDao grantDao;
    private final RankDao rankDao;

    // Cache of online players by UUID
    private final Map<UUID, Player> onlinePlayers;

    // Cache of Bukkit permission attachments
    private final Map<UUID, PermissionAttachment> permissionAttachments;

    // Cache of all ranks
    private final Map<String, Rank> rankCache;

    /**
     * Creates a new PlayerManager
     *
     * @param plugin The Bukkit plugin instance
     * @param playerDao The PlayerDao instance
     * @param grantDao The GrantDao instance
     * @param rankDao The RankDao instance
     */
    public PlayerManager(BukkitPlugin plugin, PlayerDao playerDao, GrantDao grantDao, RankDao rankDao) {
        this.plugin = plugin;
        this.playerDao = playerDao;
        this.grantDao = grantDao;
        this.rankDao = rankDao;
        this.onlinePlayers = new ConcurrentHashMap<>();
        this.permissionAttachments = new ConcurrentHashMap<>();
        this.rankCache = new ConcurrentHashMap<>();
    }

    /**
     * Gets the Bukkit plugin instance
     *
     * @return The Bukkit plugin
     */
    public BukkitPlugin getPlugin() {
        return plugin;
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

            // Notify BungeeCord of the grant change
            notifyProxyOfGrantChange(uuid);
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
     * Setup permissions for a Bukkit player based on their Core profile
     *
     * @param bukkitPlayer The Bukkit player
     */
    public void setupPermissions(org.bukkit.entity.Player bukkitPlayer) {
        UUID uuid = bukkitPlayer.getUniqueId();
        Player corePlayer = onlinePlayers.get(uuid);

        if (corePlayer == null) {
            log.warn("[PlayerManager] Cannot setup permissions for " + bukkitPlayer.getName() + ": profile not loaded");
            return;
        }

        // Remove existing attachment if present
        PermissionAttachment oldAttachment = permissionAttachments.remove(uuid);
        if (oldAttachment != null) {
            bukkitPlayer.removeAttachment(oldAttachment);
        }

        // Create new permission attachment
        PermissionAttachment attachment = bukkitPlayer.addAttachment(plugin);
        permissionAttachments.put(uuid, attachment);

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

        // Apply permissions to attachment
        for (String permission : permissions) {
            boolean value = true;
            // Handle negative permissions (starting with -)
            if (permission.startsWith("-")) {
                permission = permission.substring(1);
                value = false;
            }
            attachment.setPermission(permission, value);
        }

        log.info("[PlayerManager] Setup " + permissions.size() + " permissions for " + bukkitPlayer.getName() +
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

        org.bukkit.entity.Player bukkitPlayer = plugin.getServer().getPlayer(uuid);
        if (bukkitPlayer == null || !bukkitPlayer.isOnline()) {
            return;
        }

        setupPermissions(bukkitPlayer);
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
     * Notifies BungeeCord of a grant change for a player
     *
     * @param uuid The player's UUID
     */
    private void notifyProxyOfGrantChange(UUID uuid) {
        org.bukkit.entity.Player bukkitPlayer = plugin.getServer().getPlayer(uuid);
        if (bukkitPlayer == null || !bukkitPlayer.isOnline()) {
            return;
        }

        try {
            java.io.ByteArrayOutputStream byteStream = new java.io.ByteArrayOutputStream();
            java.io.DataOutputStream out = new java.io.DataOutputStream(byteStream);

            // Write message type and UUID
            out.writeUTF("GRANT_CHANGE");
            out.writeUTF(uuid.toString());

            // Send to BungeeCord
            bukkitPlayer.sendPluginMessage(plugin, "core:channel", byteStream.toByteArray());

            log.info("[PlayerManager] Notified proxy of grant change for " + bukkitPlayer.getName());
        } catch (java.io.IOException e) {
            log.error("[PlayerManager] Failed to send grant change notification: " + e.getMessage());
        }
    }
}
