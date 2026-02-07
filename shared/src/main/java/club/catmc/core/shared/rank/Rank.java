package club.catmc.core.shared.rank;

import java.util.List;

/**
 * Represents a rank with associated permissions and metadata
 */
public class Rank {

    private String id;
    private String name;
    private String displayName;
    private String prefix;
    private String suffix;
    private int priority;
    private boolean defaultRank;
    private List<String> permissions;

    /**
     * Creates a new Rank with required fields
     *
     * @param id        Unique rank identifier
     * @param name      Rank name
     * @param displayName Display name in chat
     * @param priority  Priority for inheritance (higher = more important)
     */
    public Rank(String id, String name, String displayName, int priority) {
        this.id = id;
        this.name = name;
        this.displayName = displayName;
        this.priority = priority;
        this.defaultRank = false;
    }

    /**
     * Creates a new Rank with all fields
     */
    public Rank(String id, String name, String displayName, String prefix, String suffix,
                int priority, boolean defaultRank, List<String> permissions) {
        this.id = id;
        this.name = name;
        this.displayName = displayName;
        this.prefix = prefix;
        this.suffix = suffix;
        this.priority = priority;
        this.defaultRank = defaultRank;
        this.permissions = permissions;
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getPrefix() {
        return prefix;
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    public String getSuffix() {
        return suffix;
    }

    public void setSuffix(String suffix) {
        this.suffix = suffix;
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public boolean isDefaultRank() {
        return defaultRank;
    }

    public void setDefaultRank(boolean defaultRank) {
        this.defaultRank = defaultRank;
    }

    public List<String> getPermissions() {
        return permissions;
    }

    public void setPermissions(List<String> permissions) {
        this.permissions = permissions;
    }

    /**
     * Checks if this rank has higher priority than another
     *
     * @param other The other rank to compare
     * @return true if this rank has higher priority
     */
    public boolean hasHigherPriorityThan(Rank other) {
        return this.priority > other.priority;
    }

    /**
     * Checks if this rank has a specific permission
     *
     * @param permission The permission node to check
     * @return true if the rank has the permission
     */
    public boolean hasPermission(String permission) {
        return permissions != null && permissions.contains(permission);
    }

    /**
     * Formats a chat display name with this rank's prefix
     * Format: "prefix username&7:" where &7 is the light gray color code
     *
     * @param username The player's username
     * @return Formatted chat name (e.g., "&7[Admin]&f Player&7:")
     */
    public String formatChatName(String username) {
        if (prefix != null && !prefix.isEmpty()) {
            return prefix + " " + username + "&7:";
        }
        return username + "&7:";
    }

    /**
     * Gets the chat display name without the colon suffix
     * Format: "prefix username"
     *
     * @param username The player's username
     * @return Formatted display name
     */
    public String formatDisplayName(String username) {
        if (prefix != null && !prefix.isEmpty()) {
            return prefix + " " + username;
        }
        return username;
    }
}
