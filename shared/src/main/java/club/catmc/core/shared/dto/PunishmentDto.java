package club.catmc.core.shared.dto;

/**
 * Data Transfer Object for Punishment entities.
 * Maps to/from JSON for API requests/responses.
 */
public class PunishmentDto {
    private Integer id;
    private String playerUuid;
    private String punishedByUuid;
    private String punishedByName;
    private String type;
    private String reason;
    private Long durationSeconds;
    private String createdAt;
    private String expiresAt;
    private Boolean isActive;
    private Boolean executed;

    public PunishmentDto() {
    }

    public PunishmentDto(Integer id, String playerUuid, String punishedByUuid, String punishedByName,
                        String type, String reason, Long durationSeconds, String createdAt,
                        String expiresAt, Boolean isActive, Boolean executed) {
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

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getPlayerUuid() {
        return playerUuid;
    }

    public void setPlayerUuid(String playerUuid) {
        this.playerUuid = playerUuid;
    }

    public String getPunishedByUuid() {
        return punishedByUuid;
    }

    public void setPunishedByUuid(String punishedByUuid) {
        this.punishedByUuid = punishedByUuid;
    }

    public String getPunishedByName() {
        return punishedByName;
    }

    public void setPunishedByName(String punishedByName) {
        this.punishedByName = punishedByName;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
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

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public String getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(String expiresAt) {
        this.expiresAt = expiresAt;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }

    public Boolean getExecuted() {
        return executed;
    }

    public void setExecuted(Boolean executed) {
        this.executed = executed;
    }

    /**
     * Simple response wrapper for API success responses.
     */
    public static class SuccessResponse {
        private boolean success;

        public SuccessResponse() {
        }

        public SuccessResponse(boolean success) {
            this.success = success;
        }

        public boolean isSuccess() {
            return success;
        }

        public void setSuccess(boolean success) {
            this.success = success;
        }
    }

    /**
     * Response wrapper for punishment execution
     */
    public static class ExecuteResponse {
        private boolean success;
        private String message;
        private Boolean kicked;

        public ExecuteResponse() {
        }

        public ExecuteResponse(boolean success, String message, Boolean kicked) {
            this.success = success;
            this.message = message;
            this.kicked = kicked;
        }

        public boolean isSuccess() {
            return success;
        }

        public void setSuccess(boolean success) {
            this.success = success;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public Boolean getKicked() {
            return kicked;
        }

        public void setKicked(Boolean kicked) {
            this.kicked = kicked;
        }
    }
}
