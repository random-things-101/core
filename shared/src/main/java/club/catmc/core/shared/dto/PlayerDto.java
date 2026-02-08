package club.catmc.core.shared.dto;

import java.util.List;

/**
 * Data Transfer Object for Player entities.
 * Maps to/from JSON for API requests/responses.
 */
public class PlayerDto {
    private String uuid;
    private String username;
    private Long playtimeTicks;
    private String firstLogin;
    private String lastLogin;
    private Boolean isOnline;
    private List<String> additionalPermissions;

    public PlayerDto() {
    }

    public PlayerDto(String uuid, String username, Long playtimeTicks, String firstLogin, String lastLogin, Boolean isOnline, List<String> additionalPermissions) {
        this.uuid = uuid;
        this.username = username;
        this.playtimeTicks = playtimeTicks;
        this.firstLogin = firstLogin;
        this.lastLogin = lastLogin;
        this.isOnline = isOnline;
        this.additionalPermissions = additionalPermissions;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public Long getPlaytimeTicks() {
        return playtimeTicks;
    }

    public void setPlaytimeTicks(Long playtimeTicks) {
        this.playtimeTicks = playtimeTicks;
    }

    public String getFirstLogin() {
        return firstLogin;
    }

    public void setFirstLogin(String firstLogin) {
        this.firstLogin = firstLogin;
    }

    public String getLastLogin() {
        return lastLogin;
    }

    public void setLastLogin(String lastLogin) {
        this.lastLogin = lastLogin;
    }

    public Boolean getIsOnline() {
        return isOnline;
    }

    public void setIsOnline(Boolean isOnline) {
        this.isOnline = isOnline;
    }

    public List<String> getAdditionalPermissions() {
        return additionalPermissions;
    }

    public void setAdditionalPermissions(List<String> additionalPermissions) {
        this.additionalPermissions = additionalPermissions;
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
