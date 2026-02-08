package club.catmc.core.shared.punishment;

import club.catmc.core.shared.api.ApiClient;
import club.catmc.core.shared.dto.PunishmentDto;
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
 * Data Access Object for Punishment operations.
 * Uses REST API for all data access.
 */
public class PunishmentDao {

    private static final Logger log = LoggerFactory.getLogger(PunishmentDao.class);
    private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
    private final ApiClient apiClient;

    public PunishmentDao(ApiClient apiClient) {
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
     * Finds a punishment by ID
     *
     * @param id The punishment ID
     * @return CompletableFuture containing Optional<Punishment>
     */
    public CompletableFuture<Optional<Punishment>> findById(int id) {
        return apiClient.get("/punishments/" + id, PunishmentDto.class)
                .thenApply(dto -> Optional.ofNullable(dto != null ? mapDtoToPunishment(dto) : null))
                .exceptionally(e -> {
                    if (e.getCause() instanceof ApiClient.ApiClientException) {
                        ApiClient.ApiClientException ex = (ApiClient.ApiClientException) e.getCause();
                        if (ex.getStatusCode() == 404) {
                            return Optional.empty();
                        }
                    }
                    throw new RuntimeException("Failed to find punishment", e);
                });
    }

    /**
     * Finds all punishments for a player
     *
     * @param playerUuid The player's UUID
     * @return CompletableFuture containing List<Punishment>
     */
    public CompletableFuture<List<Punishment>> findByPlayerUuid(UUID playerUuid) {
        Type listType = new TypeToken<List<PunishmentDto>>() {}.getType();
        return apiClient.get("/punishments/player/" + playerUuid.toString(), listType)
                .thenApply(dtos -> {
                    List<Punishment> punishments = new ArrayList<>();
                    @SuppressWarnings("unchecked")
                    List<PunishmentDto> dtoList = (List<PunishmentDto>) dtos;
                    for (PunishmentDto dto : dtoList) {
                        punishments.add(mapDtoToPunishment(dto));
                    }
                    return punishments;
                });
    }

    /**
     * Finds active punishments for a player
     *
     * @param playerUuid The player's UUID
     * @return CompletableFuture containing List<Punishment>
     */
    public CompletableFuture<List<Punishment>> findActiveByPlayerUuid(UUID playerUuid) {
        Type listType = new TypeToken<List<PunishmentDto>>() {}.getType();
        return apiClient.get("/punishments/player/" + playerUuid.toString() + "/active", listType)
                .thenApply(dtos -> {
                    List<Punishment> punishments = new ArrayList<>();
                    @SuppressWarnings("unchecked")
                    List<PunishmentDto> dtoList = (List<PunishmentDto>) dtos;
                    for (PunishmentDto dto : dtoList) {
                        punishments.add(mapDtoToPunishment(dto));
                    }
                    return punishments;
                });
    }

    /**
     * Finds active punishments for a player, including expired ones
     *
     * @param playerUuid The player's UUID
     * @return CompletableFuture containing List<Punishment>
     */
    public CompletableFuture<List<Punishment>> findActiveByPlayerUuidIncludingExpired(UUID playerUuid) {
        Type listType = new TypeToken<List<PunishmentDto>>() {}.getType();
        return apiClient.get("/punishments/player/" + playerUuid.toString() + "/active-expired", listType)
                .thenApply(dtos -> {
                    List<Punishment> punishments = new ArrayList<>();
                    @SuppressWarnings("unchecked")
                    List<PunishmentDto> dtoList = (List<PunishmentDto>) dtos;
                    for (PunishmentDto dto : dtoList) {
                        punishments.add(mapDtoToPunishment(dto));
                    }
                    return punishments;
                });
    }

    /**
     * Saves a new punishment
     *
     * @param punishment The punishment to save
     * @return CompletableFuture containing the saved punishment with ID
     */
    public CompletableFuture<Punishment> save(Punishment punishment) {
        PunishmentDto dto = mapPunishmentToDto(punishment);
        return apiClient.post("/punishments", dto, PunishmentDto.class)
                .thenApply(responseDto -> {
                    log.info("[PunishmentDao] Saved punishment for player: {}", punishment.getPlayerUuid());
                    return mapDtoToPunishment(responseDto);
                });
    }

    /**
     * Deletes a punishment by ID
     *
     * @param id The punishment ID
     * @return CompletableFuture that completes when deleted
     */
    public CompletableFuture<Void> deleteById(int id) {
        return apiClient.delete("/punishments/" + id)
                .thenRun(() -> log.info("[PunishmentDao] Deleted punishment: {}", id));
    }

    /**
     * Deletes all punishments for a player
     *
     * @param playerUuid The player's UUID
     * @return CompletableFuture that completes when deleted
     */
    public CompletableFuture<Void> deleteByPlayerUuid(UUID playerUuid) {
        return apiClient.delete("/punishments/player/" + playerUuid.toString())
                .thenRun(() -> log.info("[PunishmentDao] Deleted all punishments for player: {}", playerUuid));
    }

    /**
     * Updates the active status of a punishment
     *
     * @param id        The punishment ID
     * @param isActive  The new active status
     * @return CompletableFuture that completes when updated
     */
    public CompletableFuture<Void> updateActiveStatus(int id, boolean isActive) {
        return apiClient.put("/punishments/" + id + "/active",
                new ActiveStatusRequest(isActive), PunishmentDto.SuccessResponse.class)
                .thenRun(() -> log.info("[PunishmentDao] Updated active status for punishment: {}", id));
    }

    /**
     * Marks expired punishments as inactive
     *
     * @return CompletableFuture containing the number of punishments cleaned up
     */
    public CompletableFuture<Integer> cleanupExpired() {
        return apiClient.post("/punishments/cleanup-expired", null, CleanupResponse.class)
                .thenApply(response -> {
                    log.info("[PunishmentDao] Cleaned up {} expired punishments", response.getCount());
                    return response.getCount();
                });
    }

    /**
     * Executes a punishment (kicks player if needed)
     *
     * @param id The punishment ID
     * @return CompletableFuture containing the execution response
     */
    public CompletableFuture<ExecuteResult> execute(int id) {
        return apiClient.post("/punishments/" + id + "/execute", null, PunishmentDto.ExecuteResponse.class)
                .thenApply(response -> new ExecuteResult(response.isSuccess(), response.getMessage(), response.getKicked()));
    }

    /**
     * Check if a player is currently muted (has active mute punishment)
     *
     * @param playerUuid The player's UUID
     * @return CompletableFuture containing true if muted
     */
    public CompletableFuture<Boolean> isPlayerMuted(UUID playerUuid) {
        return findActiveByPlayerUuid(playerUuid)
                .thenApply(punishments -> punishments.stream()
                        .anyMatch(p -> p.getType() == PunishmentType.MUTE || p.getType() == PunishmentType.TEMP_MUTE));
    }

    /**
     * Get active mute punishment for a player (for mute message display)
     *
     * @param playerUuid The player's UUID
     * @return CompletableFuture containing the mute punishment, or empty if not muted
     */
    public CompletableFuture<Optional<Punishment>> getActiveMute(UUID playerUuid) {
        return findActiveByPlayerUuid(playerUuid)
                .thenApply(punishments -> punishments.stream()
                        .filter(p -> p.getType() == PunishmentType.MUTE || p.getType() == PunishmentType.TEMP_MUTE)
                        .findFirst());
    }

    /**
     * Get warning count for a player
     *
     * @param playerUuid The player's UUID
     * @return CompletableFuture containing the number of warnings
     */
    public CompletableFuture<Long> getWarningCount(UUID playerUuid) {
        return findByPlayerUuid(playerUuid)
                .thenApply(punishments -> punishments.stream()
                        .filter(p -> p.getType() == PunishmentType.WARN)
                        .count());
    }

    /**
     * Check if a player is currently banned (has active ban punishment)
     *
     * @param playerUuid The player's UUID
     * @return CompletableFuture containing true if banned
     */
    public CompletableFuture<Boolean> isPlayerBanned(UUID playerUuid) {
        return findActiveByPlayerUuid(playerUuid)
                .thenApply(punishments -> punishments.stream()
                        .anyMatch(p -> p.getType() == PunishmentType.BAN || p.getType() == PunishmentType.TEMPBAN));
    }

    /**
     * Maps a PunishmentDto to a Punishment domain object
     */
    private Punishment mapDtoToPunishment(PunishmentDto dto) {
        Punishment punishment = new Punishment();
        punishment.setId(dto.getId());
        punishment.setPlayerUuid(UUID.fromString(dto.getPlayerUuid()));
        if (dto.getPunishedByUuid() != null) {
            punishment.setPunishedByUuid(UUID.fromString(dto.getPunishedByUuid()));
        }
        punishment.setPunishedByName(dto.getPunishedByName());
        punishment.setType(PunishmentType.valueOf(dto.getType()));
        punishment.setReason(dto.getReason());
        punishment.setDurationSeconds(dto.getDurationSeconds());
        if (dto.getCreatedAt() != null) {
            punishment.setCreatedAt(LocalDateTime.parse(dto.getCreatedAt(), ISO_FORMATTER));
        }
        if (dto.getExpiresAt() != null) {
            punishment.setExpiresAt(LocalDateTime.parse(dto.getExpiresAt(), ISO_FORMATTER));
        }
        punishment.setActive(dto.getIsActive() != null ? dto.getIsActive() : false);
        punishment.setExecuted(dto.getExecuted() != null ? dto.getExecuted() : false);
        return punishment;
    }

    /**
     * Maps a Punishment domain object to a PunishmentDto
     */
    private PunishmentDto mapPunishmentToDto(Punishment punishment) {
        return new PunishmentDto(
                punishment.getId(),
                punishment.getPlayerUuid().toString(),
                punishment.getPunishedByUuid() != null ? punishment.getPunishedByUuid().toString() : null,
                punishment.getPunishedByName(),
                punishment.getType().name(),
                punishment.getReason(),
                punishment.getDurationSeconds(),
                punishment.getCreatedAt() != null ? punishment.getCreatedAt().format(ISO_FORMATTER) : null,
                punishment.getExpiresAt() != null ? punishment.getExpiresAt().format(ISO_FORMATTER) : null,
                punishment.isActive(),
                punishment.isExecuted()
        );
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
        private int count;

        public int getCount() {
            return count;
        }

        public void setCount(int count) {
            this.count = count;
        }
    }

    /**
     * Result of punishment execution
     */
    public static class ExecuteResult {
        private final boolean success;
        private final String message;
        private final Boolean kicked;

        public ExecuteResult(boolean success, String message, Boolean kicked) {
            this.success = success;
            this.message = message;
            this.kicked = kicked;
        }

        public boolean isSuccess() {
            return success;
        }

        public String getMessage() {
            return message;
        }

        public Boolean getKicked() {
            return kicked;
        }
    }
}
