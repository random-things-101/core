package club.catmc.core.shared.grant;

import club.catmc.core.shared.api.ApiClient;
import club.catmc.core.shared.dto.GrantDto;
import com.google.gson.reflect.TypeToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Type;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Data Access Object for Grant operations.
 * Refactored to use REST API instead of direct database access.
 */
public class GrantDao {

    private static final Logger log = LoggerFactory.getLogger(GrantDao.class);
    private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
    private final ApiClient apiClient;

    public GrantDao(ApiClient apiClient) {
        this.apiClient = apiClient;
    }

    /**
     * No-op - tables are managed by the API server.
     *
     * @return CompletableFuture that completes immediately
     */
    public CompletableFuture<Void> createTable() {
        return CompletableFuture.completedFuture(null);
    }

    /**
     * Finds a grant by ID
     *
     * @param id The grant ID
     * @return CompletableFuture containing Optional<Grant>
     */
    public CompletableFuture<Optional<Grant>> findById(int id) {
        return apiClient.get("/grants/" + id, GrantDto.class)
                .thenApply(dto -> Optional.ofNullable(dto != null ? mapDtoToGrant(dto) : null))
                .exceptionally(e -> {
                    if (e.getCause() instanceof ApiClient.ApiClientException) {
                        ApiClient.ApiClientException ex = (ApiClient.ApiClientException) e.getCause();
                        if (ex.getStatusCode() == 404) {
                            return Optional.empty();
                        }
                    }
                    throw new RuntimeException("Failed to find grant", e);
                });
    }

    /**
     * Finds all grants for a player
     *
     * @param playerUuid The player's UUID
     * @return CompletableFuture containing List<Grant>
     */
    public CompletableFuture<List<Grant>> findByPlayer(UUID playerUuid) {
        Type listType = new TypeToken<List<GrantDto>>() {}.getType();
        return apiClient.get("/grants/player/" + playerUuid.toString(), listType)
                .thenApply(dtos -> {
                    List<Grant> grants = new ArrayList<>();
                    @SuppressWarnings("unchecked")
                    List<GrantDto> dtoList = (List<GrantDto>) dtos;
                    for (GrantDto dto : dtoList) {
                        grants.add(mapDtoToGrant(dto));
                    }
                    return grants;
                });
    }

    /**
     * Finds all active grants for a player
     *
     * @param playerUuid The player's UUID
     * @return CompletableFuture containing List<Grant>
     */
    public CompletableFuture<List<Grant>> findActiveByPlayer(UUID playerUuid) {
        Type listType = new TypeToken<List<GrantDto>>() {}.getType();
        return apiClient.get("/grants/player/" + playerUuid.toString() + "/active", listType)
                .thenApply(dtos -> {
                    List<Grant> grants = new ArrayList<>();
                    @SuppressWarnings("unchecked")
                    List<GrantDto> dtoList = (List<GrantDto>) dtos;
                    for (GrantDto dto : dtoList) {
                        grants.add(mapDtoToGrant(dto));
                    }
                    return grants;
                });
    }

    /**
     * Finds all active grants for a player (including expired ones that are still marked active)
     * This is useful for cleaning up expired grants
     *
     * @param playerUuid The player's UUID
     * @return CompletableFuture containing List<Grant>
     */
    public CompletableFuture<List<Grant>> findActiveByPlayerIncludingExpired(UUID playerUuid) {
        Type listType = new TypeToken<List<GrantDto>>() {}.getType();
        return apiClient.get("/grants/player/" + playerUuid.toString() + "/active-expired", listType)
                .thenApply(dtos -> {
                    List<Grant> grants = new ArrayList<>();
                    @SuppressWarnings("unchecked")
                    List<GrantDto> dtoList = (List<GrantDto>) dtos;
                    for (GrantDto dto : dtoList) {
                        grants.add(mapDtoToGrant(dto));
                    }
                    return grants;
                });
    }

    /**
     * Finds all grants for a specific rank
     *
     * @param rankId The rank ID
     * @return CompletableFuture containing List<Grant>
     */
    public CompletableFuture<List<Grant>> findByRank(String rankId) {
        Type listType = new TypeToken<List<GrantDto>>() {}.getType();
        return apiClient.get("/grants/rank/" + rankId, listType)
                .thenApply(dtos -> {
                    List<Grant> grants = new ArrayList<>();
                    @SuppressWarnings("unchecked")
                    List<GrantDto> dtoList = (List<GrantDto>) dtos;
                    for (GrantDto dto : dtoList) {
                        grants.add(mapDtoToGrant(dto));
                    }
                    return grants;
                });
    }

    /**
     * Saves or updates a grant
     *
     * @param grant The grant to save
     * @return CompletableFuture that completes when saved
     */
    public CompletableFuture<Void> save(Grant grant) {
        GrantDto dto = new GrantDto(
                grant.getId() > 0 ? grant.getId() : null,
                grant.getPlayerUuid().toString(),
                grant.getRankId(),
                grant.getGranterUuid().toString(),
                grant.getGranterName(),
                grant.getGrantedAt() != null ? grant.getGrantedAt().format(ISO_FORMATTER) : null,
                grant.getExpiresAt() != null ? grant.getExpiresAt().format(ISO_FORMATTER) : null,
                grant.getReason(),
                grant.isActive()
        );
        return apiClient.post("/grants", dto, GrantDto.SuccessResponse.class)
                .thenRun(() -> log.info("[GrantDao] Saved grant for player: " + grant.getPlayerUuid()));
    }

    /**
     * Updates a grant's active status
     *
     * @param grantId The grant ID
     * @param active  The new active status
     * @return CompletableFuture that completes when updated
     */
    public CompletableFuture<Void> updateActiveStatus(int grantId, boolean active) {
        return apiClient.put("/grants/" + grantId + "/active",
                new ActiveStatusRequest(active), GrantDto.SuccessResponse.class)
                .thenRun(() -> log.info("[GrantDao] Updated active status for grant: " + grantId));
    }

    /**
     * Deletes a grant by ID
     *
     * @param grantId The grant ID
     * @return CompletableFuture that completes when deleted
     */
    public CompletableFuture<Void> deleteById(int grantId) {
        return apiClient.delete("/grants/" + grantId)
                .thenRun(() -> log.info("[GrantDao] Deleted grant: " + grantId));
    }

    /**
     * Deletes all grants for a player
     *
     * @param playerUuid The player's UUID
     * @return CompletableFuture that completes when deleted
     */
    public CompletableFuture<Void> deleteByPlayer(UUID playerUuid) {
        return apiClient.delete("/grants/player/" + playerUuid.toString())
                .thenRun(() -> log.info("[GrantDao] Deleted all grants for player: " + playerUuid));
    }

    /**
     * Cleans up expired grants by marking them as inactive
     *
     * @return CompletableFuture containing the number of grants cleaned up
     */
    public CompletableFuture<Integer> cleanupExpiredGrants() {
        return apiClient.post("/grants/cleanup-expired", "", CleanupResponse.class)
                .thenApply(response -> response.getCleanupCount());
    }

    /**
     * Maps a GrantDto to a Grant domain object
     */
    private Grant mapDtoToGrant(GrantDto dto) {
        Grant grant = new Grant(
                dto.getId() != null ? dto.getId() : 0,
                UUID.fromString(dto.getPlayerUuid()),
                dto.getRankId(),
                UUID.fromString(dto.getGranterUuid()),
                dto.getGranterName(),
                dto.getGrantedAt() != null ? LocalDateTime.parse(dto.getGrantedAt(), ISO_FORMATTER) : null,
                dto.getExpiresAt() != null ? LocalDateTime.parse(dto.getExpiresAt(), ISO_FORMATTER) : null,
                dto.getReason(),
                dto.getIsActive() != null ? dto.getIsActive() : true
        );
        return grant;
    }

    /**
     * Request wrapper for active status updates
     */
    private static class ActiveStatusRequest {
        private final boolean isActive;

        public ActiveStatusRequest(boolean isActive) {
            this.isActive = isActive;
        }

        public boolean isActive() {
            return isActive;
        }
    }

    /**
     * Response wrapper for cleanup operations
     */
    private static class CleanupResponse {
        private int cleanupCount;

        public int getCleanupCount() {
            return cleanupCount;
        }

        public void setCleanupCount(int cleanupCount) {
            this.cleanupCount = cleanupCount;
        }
    }
}
