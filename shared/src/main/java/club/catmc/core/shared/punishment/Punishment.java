package club.catmc.core.shared.punishment;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Represents a punishment issued to a player
 */
public class Punishment {
    private int id;
    private UUID playerUuid;
    private UUID punishedByUuid;
    private String punishedByName;
    private PunishmentType type;
    private String reason;
    private Long durationSeconds;
    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;
    private boolean isActive;
    private boolean executed;

    public Punishment() {
    }

    public Punishment(int id, UUID playerUuid, UUID punishedByUuid, String punishedByName,
                       PunishmentType type, String reason, Long durationSeconds,
                       LocalDateTime createdAt, LocalDateTime expiresAt, boolean isActive, boolean executed) {
        this.id = id;
        this.playerUuid = playerUuid;
        this.punishedByUuid = punishedByUuid;
        this.punishedByName = punishedByName;
        this.type = type;
        this.reason = reason;
        this.durationSeconds = durationSeconds;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
        this.isActive = isActive;
        this.executed = executed;
    }

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

    public UUID getPunishedByUuid() {
        return punishedByUuid;
    }

    public void setPunishedByUuid(UUID punishedByUuid) {
        this.punishedByUuid = punishedByUuid;
    }

    public String getPunishedByName() {
        return punishedByName;
    }

    public void setPunishedByName(String punishedByName) {
        this.punishedByName = punishedByName;
    }

    public PunishmentType getType() {
        return type;
    }

    public void setType(PunishmentType type) {
        this.type = type;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public Long getDurationSeconds() {
        return durationSeconds;
    }

    public void setDurationSeconds(Long durationSeconds) {
        this.durationSeconds = durationSeconds;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        this.isActive = active;
    }

    public boolean isExecuted() {
        return executed;
    }

    public void setExecuted(boolean executed) {
        this.executed = executed;
    }

    /**
     * Check if this punishment is a kick (one-time punishment)
     */
    public boolean isKick() {
        return type == PunishmentType.KICK;
    }

    /**
     * Check if this punishment is temporary (has duration)
     */
    public boolean isTemporary() {
        return durationSeconds != null && durationSeconds > 0;
    }

    /**
     * Check if punishment is expired (current time past expires_at)
     */
    public boolean isExpired() {
        return expiresAt != null && LocalDateTime.now().isAfter(expiresAt);
    }
}
