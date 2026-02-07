package club.catmc.core.shared.grant;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Represents a grant of a rank to a player
 */
public class Grant {

    private int id;
    private UUID playerUuid;
    private String rankId;
    private UUID granterUuid;
    private String granterName;
    private LocalDateTime grantedAt;
    private LocalDateTime expiresAt;
    private String reason;
    private boolean active;

    /**
     * Creates a new permanent grant
     *
     * @param playerUuid   The player receiving the grant
     * @param rankId       The rank being granted
     * @param granterUuid  The UUID of the player/console granting the rank
     * @param granterName  The name of the granter
     * @param reason       The reason for the grant
     */
    public Grant(UUID playerUuid, String rankId, UUID granterUuid, String granterName, String reason) {
        this.playerUuid = playerUuid;
        this.rankId = rankId;
        this.granterUuid = granterUuid;
        this.granterName = granterName;
        this.reason = reason;
        this.grantedAt = LocalDateTime.now();
        this.expiresAt = null; // Permanent grant
        this.active = true;
    }

    /**
     * Creates a new grant with optional expiration
     *
     * @param playerUuid   The player receiving the grant
     * @param rankId       The rank being granted
     * @param granterUuid  The UUID of the player/console granting the rank
     * @param granterName  The name of the granter
     * @param reason       The reason for the grant
     * @param expiresAt    When the grant expires (null for permanent)
     */
    public Grant(UUID playerUuid, String rankId, UUID granterUuid, String granterName,
                String reason, LocalDateTime expiresAt) {
        this.playerUuid = playerUuid;
        this.rankId = rankId;
        this.granterUuid = granterUuid;
        this.granterName = granterName;
        this.reason = reason;
        this.grantedAt = LocalDateTime.now();
        this.expiresAt = expiresAt;
        this.active = true;
    }

    /**
     * Creates a new grant with all fields
     */
    public Grant(int id, UUID playerUuid, String rankId, UUID granterUuid, String granterName,
                LocalDateTime grantedAt, LocalDateTime expiresAt, String reason, boolean active) {
        this.id = id;
        this.playerUuid = playerUuid;
        this.rankId = rankId;
        this.granterUuid = granterUuid;
        this.granterName = granterName;
        this.grantedAt = grantedAt;
        this.expiresAt = expiresAt;
        this.reason = reason;
        this.active = active;
    }

    // Getters and Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public UUID getPlayerUuid() {
        return playerUuid;
    }

    public void setPlayerUuid(UUID playerUuid) {
        this.playerUuid = playerUuid;
    }

    public String getRankId() {
        return rankId;
    }

    public void setRankId(String rankId) {
        this.rankId = rankId;
    }

    public UUID getGranterUuid() {
        return granterUuid;
    }

    public void setGranterUuid(UUID granterUuid) {
        this.granterUuid = granterUuid;
    }

    public String getGranterName() {
        return granterName;
    }

    public void setGranterName(String granterName) {
        this.granterName = granterName;
    }

    public LocalDateTime getGrantedAt() {
        return grantedAt;
    }

    public void setGrantedAt(LocalDateTime grantedAt) {
        this.grantedAt = grantedAt;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    /**
     * Checks if this grant is permanent (no expiration)
     *
     * @return true if the grant is permanent
     */
    public boolean isPermanent() {
        return expiresAt == null;
    }

    /**
     * Checks if this grant has expired
     *
     * @return true if the grant has expired
     */
    public boolean isExpired() {
        if (isPermanent()) {
            return false;
        }
        return LocalDateTime.now().isAfter(expiresAt);
    }

    /**
     * Checks if this grant is currently valid (active and not expired)
     *
     * @return true if the grant is valid
     */
    public boolean isValid() {
        return active && !isExpired();
    }

    /**
     * Gets the remaining duration until expiration
     *
     * @return Remaining duration in milliseconds, or -1 if permanent
     */
    public long getRemainingDuration() {
        if (isPermanent()) {
            return -1;
        }
        LocalDateTime now = LocalDateTime.now();
        if (now.isAfter(expiresAt)) {
            return 0;
        }
        return java.time.Duration.between(now, expiresAt).toMillis();
    }
}
