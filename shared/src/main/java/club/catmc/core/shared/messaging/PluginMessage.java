package club.catmc.core.shared.messaging;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;

/**
 * Represents a plugin message sent between Bukkit and BungeeCord
 */
public class PluginMessage {

    private final String channel;
    private final byte[] data;

    /**
     * Creates a new PluginMessage
     *
     * @param channel The message channel
     * @param data    The message data
     */
    public PluginMessage(String channel, byte[] data) {
        this.channel = channel;
        this.data = data;
    }

    public String getChannel() {
        return channel;
    }

    public byte[] getData() {
        return data;
    }

    /**
     * Reads the message data as a UTF string
     *
     * @return The string content, or null if error
     */
    public String readString() {
        try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(data))) {
            return in.readUTF();
        } catch (IOException e) {
            System.err.println("[Messaging] Failed to read string: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
}
