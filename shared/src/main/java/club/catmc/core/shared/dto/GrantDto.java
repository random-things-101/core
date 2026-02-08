package club.catmc.core.shared.dto;

import java.util.List;

/**
 * Data Transfer Object for Grant entities.
 * Maps to/from JSON for API requests/responses.
 */
public class GrantDto {
    private Integer id;
    private String playerUuid;
    private String rankId;
    private String granterUuid;
    private String granterName;
    private String grantedAt;
    private String expiresAt;
    private String reason;
    private Boolean isActive;

    public GrantDto() {
    }

    public GrantDto(Integer id, String playerUuid, String rankId, String granterUuid, String granterName, String grantedAt, String expiresAt, String reason, Boolean isActive) {
        this.id = id;
        this.playerUuid = playerUuid;
        this.rankId = rankId;
        this.granterUuid = granterUuid;
        this.granterName = granterName;
        this.grantedAt = grantedAt;
        this.expiresAt = expiresAt;
        this.reason = reason;
        this.isActive = isActive;
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

    public String getRankId() {
        return rankId;
    }

    public void setRankId(String rankId) {
        this.rankId = rankId;
    }

    public String getGranterUuid() {
        return granterUuid;
    }

    public void setGranterUuid(String granterUuid) {
        this.granterUuid = granterUuid;
    }

    public String getGranterName() {
        return granterName;
    }

    public void setGranterName(String granterName) {
        this.granterName = granterName;
    }

    public String getGrantedAt() {
        return grantedAt;
    }

    public void setGrantedAt(String grantedAt) {
        this.grantedAt = grantedAt;
    }

    public String getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(String expiresAt) {
        this.expiresAt = expiresAt;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
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
}
