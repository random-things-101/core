package club.catmc.core.shared.rank;

import club.catmc.core.shared.api.ApiClient;
import club.catmc.core.shared.dto.RankDto;
import com.google.gson.reflect.TypeToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Data Access Object for Rank operations.
 * Refactored to use REST API instead of direct database access.
 */
public class RankDao {

    private static final Logger log = LoggerFactory.getLogger(RankDao.class);
    private final ApiClient apiClient;

    public RankDao(ApiClient apiClient) {
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
     * Finds a rank by ID
     *
     * @param id The rank ID
     * @return CompletableFuture containing Optional<Rank>
     */
    public CompletableFuture<Optional<Rank>> findById(String id) {
        return apiClient.get("/ranks/" + id, RankDto.class)
                .thenApply(dto -> Optional.ofNullable(dto != null ? mapDtoToRank(dto) : null))
                .exceptionally(e -> {
                    if (e.getCause() instanceof ApiClient.ApiClientException) {
                        ApiClient.ApiClientException ex = (ApiClient.ApiClientException) e.getCause();
                        if (ex.getStatusCode() == 404) {
                            return Optional.empty();
                        }
                    }
                    throw new RuntimeException("Failed to find rank", e);
                });
    }

    /**
     * Finds the default rank
     *
     * @return CompletableFuture containing Optional<Rank>
     */
    public CompletableFuture<Optional<Rank>> findDefaultRank() {
        return apiClient.get("/ranks/default", RankDto.class)
                .thenApply(dto -> Optional.ofNullable(dto != null ? mapDtoToRank(dto) : null))
                .exceptionally(e -> {
                    if (e.getCause() instanceof ApiClient.ApiClientException) {
                        ApiClient.ApiClientException ex = (ApiClient.ApiClientException) e.getCause();
                        if (ex.getStatusCode() == 404) {
                            return Optional.empty();
                        }
                    }
                    throw new RuntimeException("Failed to find default rank", e);
                });
    }

    /**
     * Gets all ranks ordered by priority
     *
     * @return CompletableFuture containing List<Rank>
     */
    public CompletableFuture<List<Rank>> findAll() {
        Type listType = new TypeToken<List<RankDto>>() {}.getType();
        return apiClient.get("/ranks", listType)
                .thenApply(dtos -> {
                    List<Rank> ranks = new ArrayList<>();
                    @SuppressWarnings("unchecked")
                    List<RankDto> dtoList = (List<RankDto>) dtos;
                    for (RankDto dto : dtoList) {
                        ranks.add(mapDtoToRank(dto));
                    }
                    return ranks;
                });
    }

    /**
     * Saves or updates a rank
     *
     * @param rank The rank to save
     * @return CompletableFuture that completes when saved
     */
    public CompletableFuture<Void> save(Rank rank) {
        RankDto dto = new RankDto(
                rank.getId(),
                rank.getName(),
                rank.getDisplayName(),
                rank.getPrefix(),
                rank.getSuffix(),
                rank.getPriority(),
                rank.isDefaultRank(),
                rank.getPermissions()
        );
        return apiClient.post("/ranks", dto, RankDto.SuccessResponse.class)
                .thenRun(() -> log.info("[RankDao] Saved rank: " + rank.getId()));
    }

    /**
     * Deletes a rank by ID
     *
     * @param id The rank ID
     * @return CompletableFuture that completes when deleted
     */
    public CompletableFuture<Void> deleteById(String id) {
        return apiClient.delete("/ranks/" + id)
                .thenRun(() -> log.info("[RankDao] Deleted rank: " + id));
    }

    /**
     * Maps a RankDto to a Rank domain object
     */
    private Rank mapDtoToRank(RankDto dto) {
        Rank rank = new Rank(
                dto.getId(),
                dto.getName(),
                dto.getDisplayName(),
                dto.getPrefix(),
                dto.getSuffix(),
                dto.getPriority() != null ? dto.getPriority() : 0,
                dto.getIsDefault() != null ? dto.getIsDefault() : false,
                dto.getPermissions()
        );
        return rank;
    }
}
