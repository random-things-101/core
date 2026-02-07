package club.catmc.core.bukkit.listener;

import club.catmc.core.bukkit.BukkitPlugin;
import club.catmc.core.bukkit.manager.PlayerManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.PluginMessageListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.UUID;

/**
 * Handles plugin messages from BungeeCord
 */
public class CorePluginMessageListener implements PluginMessageListener {

    private static final Logger log = LoggerFactory.getLogger(CorePluginMessageListener.class);

    private final PlayerManager playerManager;

    public CorePluginMessageListener(BukkitPlugin plugin, PlayerManager playerManager) {
        this.playerManager = playerManager;
    }

    @Override
    public void onPluginMessageReceived(String channel, Player bukkitPlayer, byte[] data) {
        if (!channel.equals("core:channel")) {
            return;
        }

        try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(data))) {
            String messageType = in.readUTF();

            if ("GRANT_CHANGE".equals(messageType)) {
                String uuidString = in.readUTF();
                UUID uuid = UUID.fromString(uuidString);

                log.info("[PluginMessage] Received grant change notification for " + uuid);

                // Reload the player's grants and recalculate permissions
                playerManager.reloadPlayerGrants(uuid);
            } else if ("PRIVATE_MESSAGE".equals(messageType)) {
                String senderName = in.readUTF();
                String formattedMessage = in.readUTF();

                log.info("[PluginMessage] Received private message from " + senderName + " to " + bukkitPlayer.getName());

                // Display the message to the player using Adventure API
                Component messageComponent = LegacyComponentSerializer.legacySection().deserialize(formattedMessage);
                bukkitPlayer.sendMessage(messageComponent);

                // Play a message sound
                bukkitPlayer.playSound(bukkitPlayer.getLocation(), org.bukkit.Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
            }
        } catch (IOException e) {
            log.error("[PluginMessage] Failed to read plugin message: " + e.getMessage());
        }
    }
}
