package club.catmc.core.bungee.manager;

import net.md_5.bungee.api.connection.ProxiedPlayer;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages message relationships for network-wide messaging
 */
public class MessageManager {

    // Tracks who a player last received a message from (for /r command)
    private final ConcurrentHashMap<UUID, UUID> lastReceivedFrom = new ConcurrentHashMap<>();

    // Tracks who a player last sent a message to (for /r command when switching perspectives)
    private final ConcurrentHashMap<UUID, UUID> lastSentTo = new ConcurrentHashMap<>();

    /**
     * Records that a message was sent from sender to receiver
     *
     * @param sender The player who sent the message
     * @param receiver The player who received the message
     */
    public void recordMessage(ProxiedPlayer sender, ProxiedPlayer receiver) {
        lastReceivedFrom.put(receiver.getUniqueId(), sender.getUniqueId());
        lastSentTo.put(sender.getUniqueId(), receiver.getUniqueId());
    }

    /**
     * Gets the UUID of the last player who sent a message to the given player
     *
     * @param player The player to check
     * @return The UUID of the last sender, or null if no messages received
     */
    public UUID getLastReceivedFrom(ProxiedPlayer player) {
        return lastReceivedFrom.get(player.getUniqueId());
    }

    /**
     * Gets the UUID of the last player the given player sent a message to
     *
     * @param player The player to check
     * @return The UUID of the last receiver, or null if no messages sent
     */
    public UUID getLastSentTo(ProxiedPlayer player) {
        return lastSentTo.get(player.getUniqueId());
    }

    /**
     * Gets the target UUID for replying (prioritizes received messages over sent)
     *
     * @param player The player who wants to reply
     * @return The UUID to reply to, or null if no conversation exists
     */
    public UUID getReplyTarget(ProxiedPlayer player) {
        // First check if someone messaged us
        UUID target = lastReceivedFrom.get(player.getUniqueId());
        if (target != null) {
            return target;
        }
        // Otherwise check if we messaged someone
        return lastSentTo.get(player.getUniqueId());
    }

    /**
     * Removes message relationships for a player when they disconnect
     *
     * @param player The player to remove
     */
    public void removePlayer(ProxiedPlayer player) {
        UUID uuid = player.getUniqueId();
        lastReceivedFrom.remove(uuid);
        lastSentTo.remove(uuid);
    }
}
