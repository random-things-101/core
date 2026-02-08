package club.catmc.core.shared.player;

import club.catmc.core.shared.api.ApiClient;
import club.catmc.core.shared.dto.PlayerDto;
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
 * Data Access Object for Player operations.
 * Refactored to use REST API instead of direct database access.
 */
public class PlayerDao {

    private static final Logger log = LoggerFactory.getLogger(PlayerDao.class);
    private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
    private final ApiClient apiClient;

    public PlayerDao(ApiClient apiClient) {
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
     * Finds a player by UUID
     *
     * @param uuid The player's UUID
     * @return CompletableFuture containing Optional<Player>
     */
    public CompletableFuture<Optional<Player>> findByUuid(UUID uuid) {
        return apiClient.get("/players/" + uuid.toString(), PlayerDto.class)
                .thenApply(dto -> Optional.ofNullable(dto != null ? mapDtoToPlayer(dto) : null))
                .exceptionally(e -> {
                    if (e.getCause() instanceof ApiClient.ApiClientException) {
                        ApiClient.ApiClientException ex = (ApiClient.ApiClientException) e.getCause();
                        if (ex.getStatusCode() == 404) {
                            return Optional.empty();
                        }
                    }
                    throw new RuntimeException("Failed to find player", e);
                });
    }

    /**
     * Finds a player by username
     *
     * @param username The player's username
     * @return CompletableFuture containing Optional<Player>
     */
    public CompletableFuture<Optional<Player>> findByUsername(String username) {
        return apiClient.get("/players/username/" + username, PlayerDto.class)
                .thenApply(dto -> Optional.ofNullable(dto != null ? mapDtoToPlayer(dto) : null))
                .exceptionally(e -> {
                    if (e.getCause() instanceof ApiClient.ApiClientException) {
                        ApiClient.ApiClientException ex = (ApiClient.ApiClientException) e.getCause();
                        if (ex.getStatusCode() == 404) {
                            return Optional.empty();
                        }
                    }
                    throw new RuntimeException("Failed to find player by username", e);
                });
    }

    /**
     * Gets all online players
     *
     * @return CompletableFuture containing List<Player>
     */
    public CompletableFuture<List<Player>> findOnlinePlayers() {
        Type listType = new TypeToken<List<PlayerDto>>() {}.getType();
        return apiClient.get("/players/online", listType)
                .thenApply(dtos -> {
                    List<Player> players = new ArrayList<>();
                    @SuppressWarnings("unchecked")
                    List<PlayerDto> dtoList = (List<PlayerDto>) dtos;
                    for (PlayerDto dto : dtoList) {
                        players.add(mapDtoToPlayer(dto));
                    }
                    return players;
                });
    }

    /**
     * Gets the top players by playtime
     *
     * @param limit Maximum number of players to return
     * @return CompletableFuture containing List<Player>
     */
    public CompletableFuture<List<Player>> findTopByPlaytime(int limit) {
        Type listType = new TypeToken<List<PlayerDto>>() {}.getType();
        return apiClient.get("/players/top-playtime/" + limit, listType)
                .thenApply(dtos -> {
                    List<Player> players = new ArrayList<>();
                    @SuppressWarnings("unchecked")
                    List<PlayerDto> dtoList = (List<PlayerDto>) dtos;
                    for (PlayerDto dto : dtoList) {
                        players.add(mapDtoToPlayer(dto));
                    }
                    return players;
                });
    }

    /**
     * Saves or updates a player
     *
     * @param player The player to save
     * @return CompletableFuture that completes when saved
     */
    public CompletableFuture<Void> save(Player player) {
        PlayerDto dto = new PlayerDto(
                player.getUuid().toString(),
                player.getUsername(),
                player.getPlaytimeTicks(),
                player.getFirstLogin() != null ? player.getFirstLogin().format(ISO_FORMATTER) : null,
                player.getLastLogin() != null ? player.getLastLogin().format(ISO_FORMATTER) : null,
                player.isOnline(),
                player.getAdditionalPermissions()
        );
        return apiClient.post("/players", dto, PlayerDto.SuccessResponse.class)
                .thenRun(() -> log.info("[PlayerDao] Saved player: " + player.getUsername()));
    }

    /**
     * Deletes a player by UUID
     *
     * @param uuid The player's UUID
     * @return CompletableFuture that completes when deleted
     */
    public CompletableFuture<Void> deleteByUuid(UUID uuid) {
        return apiClient.delete("/players/" + uuid.toString())
                .thenRun(() -> log.info("[PlayerDao] Deleted player: " + uuid));
    }

    /**
     * Updates a player's online status
     *
     * @param uuid   The player's UUID
     * @param online The online status
     * @return CompletableFuture that completes when updated
     */
    public CompletableFuture<Void> updateOnlineStatus(UUID uuid, boolean online) {
        // Note: The API returns a success response, we ignore it
        return apiClient.put("/players/" + uuid.toString() + "/online",
                new OnlineStatusRequest(online), PlayerDto.SuccessResponse.class)
                .thenRun(() -> log.info("[PlayerDao] Updated online status for: " + uuid));
    }

    /**
     * Increments a player's playtime
     *
     * @param uuid  The player's UUID
     * @param ticks Number of ticks to add
     * @return CompletableFuture that completes when updated
     */
    public CompletableFuture<Void> incrementPlaytime(UUID uuid, long ticks) {
        return apiClient.post("/players/" + uuid.toString() + "/playtime",
                new PlaytimeIncrementRequest(ticks), PlayerDto.SuccessResponse.class)
                .thenRun(() -> {});
    }

    /**
     * Maps a PlayerDto to a Player domain object
     */
    private Player mapDtoToPlayer(PlayerDto dto) {
        Player player = new Player(
                UUID.fromString(dto.getUuid()),
                dto.getUsername()
        );
        player.setPlaytimeTicks(dto.getPlaytimeTicks() != null ? dto.getPlaytimeTicks() : 0L);
        if (dto.getFirstLogin() != null) {
            player.setFirstLogin(LocalDateTime.parse(dto.getFirstLogin(), ISO_FORMATTER));
        }
        if (dto.getLastLogin() != null) {
            player.setLastLogin(LocalDateTime.parse(dto.getLastLogin(), ISO_FORMATTER));
        }
        player.setOnline(dto.getIsOnline() != null ? dto.getIsOnline() : false);
        player.setAdditionalPermissions(dto.getAdditionalPermissions());
        return player;
    }

    /**
     * Request wrapper for online status updates
     */
    private static class OnlineStatusRequest {
        private final boolean isOnline;

        public OnlineStatusRequest(boolean isOnline) {
            this.isOnline = isOnline;
        }

        public boolean isOnline() {
            return isOnline;
        }
    }

    /**
     * Request wrapper for playtime increments
     */
    private static class PlaytimeIncrementRequest {
        private final long ticks;

        public PlaytimeIncrementRequest(long ticks) {
            this.ticks = ticks;
        }

        public long getTicks() {
            return ticks;
        }
    }
}
