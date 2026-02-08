package club.catmc.core.bukkit.listener;

import club.catmc.core.bukkit.BukkitPlugin;
import club.catmc.core.bukkit.manager.PlayerManager;
import club.catmc.core.shared.punishment.Punishment;
import club.catmc.core.shared.punishment.PunishmentDao;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

/**
 * Listens for and handles chat events
 */
public class ChatListener implements Listener {

    private final PlayerManager playerManager;
    private final PunishmentDao punishmentDao;

    public ChatListener(BukkitPlugin plugin, PlayerManager playerManager) {
        this.playerManager = playerManager;
        this.punishmentDao = plugin.getPunishmentDao();
    }

    /**
     * Handles chat messages and formats them with rank prefix
     *
     * @param event The chat event
     */
    @EventHandler
    public void onChat(AsyncChatEvent event) {
        Player bukkitPlayer = event.getPlayer();
        UUID uuid = bukkitPlayer.getUniqueId();

        // Check if player is muted
        try {
            Optional<Punishment> mutePunishment = punishmentDao.getActiveMute(uuid).get();
            if (mutePunishment.isPresent()) {
                Punishment mute = mutePunishment.get();
                event.setCancelled(true);

                // Send mute message to player
                Component muteMessage;
                if (mute.isTemporary()) {
                    LocalDateTime expiresAt = mute.getExpiresAt();
                    String expiry = expiresAt != null ? expiresAt.toString() : "unknown";
                    muteMessage = Component.text("You are muted until " + expiry)
                            .color(NamedTextColor.RED);
                } else {
                    muteMessage = Component.text("You are permanently muted")
                            .color(NamedTextColor.RED);
                }

                if (mute.getReason() != null && !mute.getReason().isEmpty()) {
                    muteMessage = muteMessage.append(Component.text("\nReason: " + mute.getReason())
                            .color(NamedTextColor.GRAY));
                }

                bukkitPlayer.sendMessage(muteMessage);
                return;
            }
        } catch (InterruptedException | ExecutionException e) {
            // If we can't check mute status, allow chat
        }

        // Get the player from cache
        club.catmc.core.shared.player.Player player = playerManager.getPlayer(uuid);
        if (player == null) {
            return;
        }

        // Format chat name with rank prefix
        String chatName = player.getChatName();

        // Parse the legacy color codes and create a component
        Component formattedName = MiniMessage.miniMessage().deserialize(legacyToMiniMessage(chatName));

        // Get the original message component
        Component originalMessage = event.message();

        // Combine formatted name with the message
        Component newMessage = Component.text()
                .append(formattedName)
                .append(Component.text(" "))
                .append(originalMessage)
                .color(NamedTextColor.WHITE)
                .build();

        // Set the formatted message
        event.renderer((source, sourceDisplayName, message, viewer) -> newMessage);
    }

    /**
     * Converts legacy Minecraft color codes to MiniMessage format
     * &0-9, &a-f, &r -> MiniMessage format
     *
     * @param legacy The legacy color code string
     * @return MiniMessage formatted string
     */
    private String legacyToMiniMessage(String legacy) {
        if (legacy == null) {
            return "";
        }

        // Replace legacy color codes with MiniMessage equivalents
        return legacy
                .replace("&0", "<black>")
                .replace("&1", "<dark_blue>")
                .replace("&2", "<dark_green>")
                .replace("&3", "<dark_aqua>")
                .replace("&4", "<dark_red>")
                .replace("&5", "<dark_purple>")
                .replace("&6", "<gold>")
                .replace("&7", "<gray>")
                .replace("&8", "<dark_gray>")
                .replace("&9", "<blue>")
                .replace("&a", "<green>")
                .replace("&b", "<aqua>")
                .replace("&c", "<red>")
                .replace("&d", "<light_purple>")
                .replace("&e", "<yellow>")
                .replace("&f", "<white>")
                .replace("&l", "<bold>")
                .replace("&o", "<italic>")
                .replace("&n", "<underlined>")
                .replace("&m", "<strikethrough>")
                .replace("&k", "<obfuscated>")
                .replace("&r", "<reset>");
    }
}
