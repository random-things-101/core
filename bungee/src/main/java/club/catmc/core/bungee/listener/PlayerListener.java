package club.catmc.core.bungee.listener;

import club.catmc.core.bungee.BungeePlugin;
import club.catmc.core.bungee.manager.PlayerManager;
import net.md_5.bungee.api.event.PlayerDisconnectEvent;
import net.md_5.bungee.api.event.PostLoginEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;

import java.util.UUID;

/**
 * Handles player join/leave events for profile loading
 */
public class PlayerListener implements Listener {

    private final BungeePlugin plugin;
    private final PlayerManager playerManager;

    public PlayerListener(BungeePlugin plugin, PlayerManager playerManager) {
        this.plugin = plugin;
        this.playerManager = playerManager;
    }

    /**
     * Handles player login - loads profile
     *
     * @param event The post login event
     */
    @EventHandler
    public void onPostLogin(PostLoginEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        String username = event.getPlayer().getName();

        plugin.getLogger().info("Player joined: " + username);

        // Load profile asynchronously
        playerManager.loadProfile(uuid, username)
                .thenRun(() -> {
                    // Setup permissions after profile is loaded
                    playerManager.setupPermissions(event.getPlayer());
                })
                .exceptionally(e -> {
                    plugin.getLogger().warning("Failed to load profile for " + username + ": " + e.getMessage());
                    return null;
                });

        // Log to database
        if (plugin.getDatabaseManager().isConnected()) {
            plugin.getDatabaseManager().executeUpdate(
                    "INSERT INTO player_joins (player, timestamp) VALUES ('" +
                            username + "', NOW())"
            );
        }
    }

    /**
     * Handles player disconnect - saves profile
     *
     * @param event The disconnect event
     */
    @EventHandler
    public void onDisconnect(PlayerDisconnectEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();

        // Unload profile asynchronously
        playerManager.unloadProfile(uuid).exceptionally(e -> {
            plugin.getLogger().warning("Failed to unload profile: " + e.getMessage());
            return null;
        });
    }
}
