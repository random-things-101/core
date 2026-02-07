package club.catmc.core.bungee.commands;

import club.catmc.core.bungee.BungeePlugin;
import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Default;
import co.aikar.commands.annotation.Description;
import co.aikar.commands.annotation.Syntax;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.UUID;

/**
 * Reply command for quick replies to the last person who messaged you
 */
@CommandAlias("r|reply")
@CommandPermission("core.command.reply")
@Description("Reply to the last person who messaged you")
public class ReplyCommand extends BaseCommand {

    private final BungeePlugin plugin;

    public ReplyCommand(BungeePlugin plugin) {
        this.plugin = plugin;
    }

    @Default
    @Syntax("<message>")
    @Description("Reply to the last person who messaged you")
    public void onReply(ProxiedPlayer sender, String[] message) {
        // Join message arguments
        String fullMessage = String.join(" ", message);

        // Check if message is empty
        if (fullMessage.trim().isEmpty()) {
            sender.sendMessage(new TextComponent(ChatColor.RED + "You must provide a message!"));
            return;
        }

        // Get the target player (last person who messaged sender, or last person sender messaged)
        UUID targetUuid = plugin.getMessageManager().getReplyTarget(sender);
        if (targetUuid == null) {
            sender.sendMessage(new TextComponent(ChatColor.RED + "No one has messaged you recently, and you haven't messaged anyone!"));
            return;
        }

        ProxiedPlayer target = plugin.getProxy().getPlayer(targetUuid);
        if (target == null) {
            sender.sendMessage(new TextComponent(ChatColor.RED + "The player you were messaging is no longer online!"));
            return;
        }

        // Check if sender is trying to message themselves
        if (sender.equals(target)) {
            sender.sendMessage(new TextComponent(ChatColor.RED + "You cannot message yourself!"));
            return;
        }

        // Record the message relationship for replies
        plugin.getMessageManager().recordMessage(sender, target);

        // Get sender display name (use Adventure display name if available, otherwise regular name)
        String senderName = sender.getDisplayName();
        String targetName = target.getDisplayName();

        // Format messages
        String senderFormat = ChatColor.GRAY + "[" + ChatColor.GOLD + "me" + ChatColor.GRAY + " -> " +
                ChatColor.GOLD + targetName + ChatColor.GRAY + "] " + ChatColor.WHITE + fullMessage;
        String targetFormat = ChatColor.GRAY + "[" + ChatColor.GOLD + senderName + ChatColor.GRAY + " -> " +
                ChatColor.GOLD + "me" + ChatColor.GRAY + "] " + ChatColor.WHITE + fullMessage;

        // Send confirmation to sender
        sender.sendMessage(new TextComponent(senderFormat));

        // Send message to target via plugin messaging
        sendMessageToPlayer(target, sender.getUniqueId(), senderName, fullMessage, targetFormat);
    }

    /**
     * Sends a message to a player on their server via plugin messaging
     *
     * @param target The target player
     * @param senderUuid The UUID of the sender
     * @param senderName The name of the sender
     * @param message The message content
     * @param formattedMessage The formatted message to display
     */
    private void sendMessageToPlayer(ProxiedPlayer target, UUID senderUuid, String senderName,
                                     String message, String formattedMessage) {
        if (target.getServer() == null) {
            return;
        }

        try (ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
             DataOutputStream out = new DataOutputStream(byteStream)) {

            // Message type: PRIVATE_MESSAGE
            out.writeUTF("PRIVATE_MESSAGE");
            // Sender info
            out.writeUTF(senderUuid.toString());
            out.writeUTF(senderName);
            // Message content
            out.writeUTF(message);
            // Formatted message (with colors)
            out.writeUTF(formattedMessage);

            // Send to the target's server
            target.getServer().sendData("core:channel", byteStream.toByteArray());

        } catch (IOException e) {
            plugin.getLogger().severe("Failed to send private message: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
