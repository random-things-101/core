package club.catmc.core.bungee.listener;

import club.catmc.core.bungee.BungeePlugin;
import club.catmc.core.bungee.manager.PlayerManager;
import net.md_5.bungee.api.event.PluginMessageEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.UUID;

/**
 * Handles plugin messages from Bukkit servers
 */
public class PluginMessageListener implements Listener {

    private static final Logger log = LoggerFactory.getLogger(PluginMessageListener.class);

    private final PlayerManager playerManager;

    public PluginMessageListener(BungeePlugin plugin, PlayerManager playerManager) {
        this.playerManager = playerManager;
    }

    @EventHandler
    public void onPluginMessage(PluginMessageEvent event) {
        // Check if the message is on our channel
        if (!event.getTag().equals("core:channel")) {
            return;
        }

        // Ignore messages sent from the proxy (to avoid loops)
        if (event.getSender() instanceof net.md_5.bungee.api.config.ServerInfo) {
            return;
        }

        try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(event.getData()))) {
            String messageType = in.readUTF();

            if ("GRANT_CHANGE".equals(messageType)) {
                String uuidString = in.readUTF();
                UUID uuid = UUID.fromString(uuidString);

                log.info("[PluginMessage] Received grant change notification from server for " + uuid);

                // Reload the player's grants and recalculate permissions
                playerManager.reloadPlayerGrants(uuid);
            }
        } catch (IOException e) {
            log.error("[PluginMessage] Failed to read plugin message: " + e.getMessage());
        }
    }
}
