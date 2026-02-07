package club.catmc.core.bukkit.listener;

import club.catmc.core.bukkit.BukkitPlugin;
import club.catmc.core.bukkit.manager.PlayerManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.UUID;

/**
 * Handles player join/leave events for profile loading
 */
public class PlayerListener implements Listener {

    private final BukkitPlugin plugin;
    private final PlayerManager playerManager;

    public PlayerListener(BukkitPlugin plugin, PlayerManager playerManager) {
        this.plugin = plugin;
        this.playerManager = playerManager;
    }

    /**
     * Handles player pre-login - loads profile before player joins
     *
     * @param event The pre-login event
     */
    @EventHandler
    public void onPreLogin(AsyncPlayerPreLoginEvent event) {
        UUID uuid = event.getUniqueId();
        String username = event.getName();

        if (uuid == null || username == null) {
            return;
        }

        // Load profile asynchronously
        playerManager.loadProfile(uuid, username).exceptionally(e -> {
            plugin.getLogger().warning("Failed to load profile for " + username + ": " + e.getMessage());
            return null;
        });
    }

    /**
     * Handles player join - setup permissions
     *
     * @param event The join event
     */
    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        // Setup permissions after player has joined
        playerManager.setupPermissions(event.getPlayer());
    }

    /**
     * Handles player quit - saves profile
     *
     * @param event The quit event
     */
    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();

        // Unload profile asynchronously
        playerManager.unloadProfile(uuid).exceptionally(e -> {
            plugin.getLogger().warning("Failed to unload profile: " + e.getMessage());
            return null;
        });
    }
}
