package club.catmc.core.shared.dto;

import java.util.List;

/**
 * Data Transfer Object for Rank entities.
 * Maps to/from JSON for API requests/responses.
 */
public class RankDto {
    private String id;
    private String name;
    private String displayName;
    private String prefix;
    private String suffix;
    private Integer priority;
    private Boolean isDefault;
    private List<String> permissions;

    public RankDto() {
    }

    public RankDto(String id, String name, String displayName, String prefix, String suffix, Integer priority, Boolean isDefault, List<String> permissions) {
        this.id = id;
        this.name = name;
        this.displayName = displayName;
        this.prefix = prefix;
        this.suffix = suffix;
        this.priority = priority;
        this.isDefault = isDefault;
        this.permissions = permissions;
    }

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

    public Integer getPriority() {
        return priority;
    }

    public void setPriority(Integer priority) {
        this.priority = priority;
    }

    public Boolean getIsDefault() {
        return isDefault;
    }

    public void setIsDefault(Boolean isDefault) {
        this.isDefault = isDefault;
    }

    public List<String> getPermissions() {
        return permissions;
    }

    public void setPermissions(List<String> permissions) {
        this.permissions = permissions;
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
