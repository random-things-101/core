package club.catmc.core.shared.player;

import club.catmc.core.shared.grant.Grant;
import club.catmc.core.shared.rank.Rank;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Represents a player with associated grants and metadata
 */
public class Player {

    private UUID uuid;
    private String username;
    private Rank rank; // This will be the active rank from grants
    private List<Grant> grants; // List of all grants
    private long playtimeTicks;
    private LocalDateTime firstLogin;
    private LocalDateTime lastLogin;
    private boolean online;
    private List<String> additionalPermissions;

    /**
     * Creates a new Player with essential fields
     *
     * @param uuid     Player's unique identifier
     * @param username Player's username
     */
    public Player(UUID uuid, String username) {
        this.uuid = uuid;
        this.username = username;
        this.playtimeTicks = 0L;
        this.online = false;
        this.grants = new ArrayList<>();
        this.additionalPermissions = new ArrayList<>();
    }

    /**
     * Creates a new Player with all fields
     */
    public Player(UUID uuid, String username, Rank rank, List<Grant> grants, long playtimeTicks,
                  LocalDateTime firstLogin, LocalDateTime lastLogin, boolean online,
                  List<String> additionalPermissions) {
        this.uuid = uuid;
        this.username = username;
        this.rank = rank;
        this.grants = grants != null ? grants : new ArrayList<>();
        this.playtimeTicks = playtimeTicks;
        this.firstLogin = firstLogin;
        this.lastLogin = lastLogin;
        this.online = online;
        this.additionalPermissions = additionalPermissions != null ? additionalPermissions : new ArrayList<>();
    }

    // Getters and Setters
    public UUID getUuid() {
        return uuid;
    }

    public void setUuid(UUID uuid) {
        this.uuid = uuid;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public Rank getRank() {
        return rank;
    }

    public void setRank(Rank rank) {
        this.rank = rank;
    }

    public List<Grant> getGrants() {
        return grants;
    }

    public void setGrants(List<Grant> grants) {
        this.grants = grants != null ? grants : new ArrayList<>();
    }

    public long getPlaytimeTicks() {
        return playtimeTicks;
    }

    public void setPlaytimeTicks(long playtimeTicks) {
        this.playtimeTicks = playtimeTicks;
    }

    public LocalDateTime getFirstLogin() {
        return firstLogin;
    }

    public void setFirstLogin(LocalDateTime firstLogin) {
        this.firstLogin = firstLogin;
    }

    public LocalDateTime getLastLogin() {
        return lastLogin;
    }

    public void setLastLogin(LocalDateTime lastLogin) {
        this.lastLogin = lastLogin;
    }

    public boolean isOnline() {
        return online;
    }

    public void setOnline(boolean online) {
        this.online = online;
    }

    public List<String> getAdditionalPermissions() {
        return additionalPermissions;
    }

    public void setAdditionalPermissions(List<String> additionalPermissions) {
        this.additionalPermissions = additionalPermissions != null ? additionalPermissions : new ArrayList<>();
    }

    /**
     * Gets all valid (active and not expired) grants for this player
     *
     * @return List of valid grants
     */
    public List<Grant> getValidGrants() {
        if (grants == null) {
            return new ArrayList<>();
        }
        return grants.stream()
                .filter(Grant::isValid)
                .collect(Collectors.toList());
    }

    /**
     * Adds a grant to this player
     *
     * @param grant The grant to add
     */
    public void addGrant(Grant grant) {
        if (grants == null) {
            grants = new ArrayList<>();
        }
        grants.add(grant);
    }

    /**
     * Removes a grant from this player
     *
     * @param grant The grant to remove
     */
    public void removeGrant(Grant grant) {
        if (grants != null) {
            grants.remove(grant);
        }
    }

    /**
     * Removes a grant by ID
     *
     * @param grantId The grant ID to remove
     */
    public void removeGrantById(int grantId) {
        if (grants != null) {
            grants.removeIf(grant -> grant.getId() == grantId);
        }
    }

    /**
     * Gets the rank ID of the player's active grant
     *
     * @return The rank ID, or null if no active grants
     */
    public String getActiveRankId() {
        if (grants == null || grants.isEmpty()) {
            return null;
        }

        return grants.stream()
                .filter(Grant::isValid)
                .findFirst()
                .map(Grant::getRankId)
                .orElse(null);
    }

    /**
     * Checks if the player has a specific permission
     * Checks both rank permissions and additional permissions
     *
     * @param permission The permission node to check
     * @return true if the player has the permission
     */
    public boolean hasPermission(String permission) {
        // Check rank permissions first
        if (rank != null && rank.hasPermission(permission)) {
            return true;
        }

        // Check additional permissions
        return additionalPermissions != null && additionalPermissions.contains(permission);
    }

    /**
     * Gets the player's display name with rank prefix
     *
     * @return Formatted display name
     */
    public String getDisplayName() {
        if (rank != null) {
            return rank.formatDisplayName(username);
        }
        return username;
    }

    /**
     * Gets the player's chat name with rank prefix and colon suffix
     * Format: "prefix username&7:" where &7 is the light gray color code
     *
     * @return Formatted chat name
     */
    public String getChatName() {
        if (rank != null) {
            return rank.formatChatName(username);
        }
        return username + "&7:";
    }

    /**
     * Checks if the player is on their first login
     *
     * @return true if first login date is null
     */
    public boolean isFirstLogin() {
        return firstLogin == null;
    }

    /**
     * Updates the last login time to current time
     */
    public void updateLastLogin() {
        this.lastLogin = LocalDateTime.now();
    }

    /**
     * Sets the first login time if not already set
     */
    public void setFirstLoginIfNotSet() {
        if (firstLogin == null) {
            this.firstLogin = LocalDateTime.now();
        }
    }

    /**
     * Checks if the player has an active grant for a specific rank
     *
     * @param rankId The rank ID to check
     * @return true if the player has an active grant for the rank
     */
    public boolean hasActiveGrant(String rankId) {
        if (grants == null) {
            return false;
        }
        return grants.stream()
                .anyMatch(grant -> grant.isValid() && grant.getRankId().equals(rankId));
    }

    /**
     * Gets all grants for a specific rank
     *
     * @param rankId The rank ID
     * @return List of grants for the rank
     */
    public List<Grant> getGrantsForRank(String rankId) {
        if (grants == null) {
            return new ArrayList<>();
        }
        return grants.stream()
                .filter(grant -> grant.getRankId().equals(rankId))
                .collect(Collectors.toList());
    }
}
