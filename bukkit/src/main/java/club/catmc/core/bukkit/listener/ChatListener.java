package club.catmc.core.bukkit.listener;

import club.catmc.core.bukkit.BukkitPlugin;
import club.catmc.core.bukkit.manager.PlayerManager;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.util.UUID;

/**
 * Listens for and handles chat events
 */
public class ChatListener implements Listener {

    private final PlayerManager playerManager;

    public ChatListener(BukkitPlugin plugin, PlayerManager playerManager) {
        this.playerManager = playerManager;
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
