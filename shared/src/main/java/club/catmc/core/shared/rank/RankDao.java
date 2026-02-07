package club.catmc.core.shared.rank;

import club.catmc.core.shared.db.DatabaseManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Data Access Object for Rank operations
 */
public class RankDao {

    private static final Logger log = LoggerFactory.getLogger(RankDao.class);
    private final DatabaseManager databaseManager;

    public RankDao(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    /**
     * Creates the ranks table if it doesn't exist
     *
     * @return CompletableFuture that completes when table is created
     */
    public CompletableFuture<Void> createTable() {
        String sql = """
                CREATE TABLE IF NOT EXISTS ranks (
                    id VARCHAR(64) PRIMARY KEY,
                    name VARCHAR(100) NOT NULL,
                    display_name VARCHAR(100),
                    prefix VARCHAR(100),
                    suffix VARCHAR(100),
                    priority INT DEFAULT 0,
                    is_default BOOLEAN DEFAULT FALSE,
                    permissions TEXT
                )
                """;

        return databaseManager.executeUpdate(sql).thenRun(() ->
                log.info("[RankDao] Created ranks table")
        ).exceptionally(e -> {
            log.error("[RankDao] Failed to create table: " + e.getMessage());
            return null;
        });
    }

    /**
     * Finds a rank by ID
     *
     * @param id The rank ID
     * @return CompletableFuture containing Optional<Rank>
     */
    public CompletableFuture<Optional<Rank>> findById(String id) {
        CompletableFuture<Optional<Rank>> future = new CompletableFuture<>();

        databaseManager.executeQuery(
                "SELECT * FROM ranks WHERE id = '" + id + "'",
                resultSet -> {
                    try {
                        if (resultSet.next()) {
                            future.complete(Optional.of(mapResultSetToRank(resultSet)));
                        } else {
                            future.complete(Optional.empty());
                        }
                    } catch (SQLException e) {
                        log.error("[RankDao] Error finding rank: " + e.getMessage());
                        future.completeExceptionally(e);
                    }
                }
        );

        return future;
    }

    /**
     * Finds the default rank
     *
     * @return CompletableFuture containing Optional<Rank>
     */
    public CompletableFuture<Optional<Rank>> findDefaultRank() {
        CompletableFuture<Optional<Rank>> future = new CompletableFuture<>();

        databaseManager.executeQuery(
                "SELECT * FROM ranks WHERE is_default = TRUE LIMIT 1",
                resultSet -> {
                    try {
                        if (resultSet.next()) {
                            future.complete(Optional.of(mapResultSetToRank(resultSet)));
                        } else {
                            future.complete(Optional.empty());
                        }
                    } catch (SQLException e) {
                        log.error("[RankDao] Error finding default rank: " + e.getMessage());
                        future.completeExceptionally(e);
                    }
                }
        );

        return future;
    }

    /**
     * Gets all ranks ordered by priority
     *
     * @return CompletableFuture containing List<Rank>
     */
    public CompletableFuture<List<Rank>> findAll() {
        CompletableFuture<List<Rank>> future = new CompletableFuture<>();
        List<Rank> ranks = new ArrayList<>();

        databaseManager.executeQuery(
                "SELECT * FROM ranks ORDER BY priority DESC",
                resultSet -> {
                    try {
                        while (resultSet.next()) {
                            ranks.add(mapResultSetToRank(resultSet));
                        }
                        future.complete(ranks);
                    } catch (SQLException e) {
                        log.error("[RankDao] Error finding all ranks: " + e.getMessage());
                        future.completeExceptionally(e);
                    }
                }
        );

        return future;
    }

    /**
     * Saves or updates a rank
     *
     * @param rank The rank to save
     * @return CompletableFuture that completes when saved
     */
    public CompletableFuture<Void> save(Rank rank) {
        String permissions = rank.getPermissions() != null ? String.join(",", rank.getPermissions()) : "";

        String sql = String.format(
                "INSERT INTO ranks (id, name, display_name, prefix, suffix, priority, is_default, permissions) " +
                        "VALUES ('%s', '%s', '%s', '%s', '%s', %d, %b, '%s') " +
                        "ON DUPLICATE KEY UPDATE " +
                        "name = '%s', display_name = '%s', prefix = '%s', suffix = '%s', " +
                        "priority = %d, is_default = %b, permissions = '%s'",
                rank.getId(), rank.getName(), rank.getDisplayName(),
                rank.getPrefix(), rank.getSuffix(), rank.isDefaultRank(), permissions,
                rank.getName(), rank.getDisplayName(), rank.getPrefix(), rank.getSuffix(),
                rank.isDefaultRank(), permissions
        );

        return databaseManager.executeUpdate(sql).thenRun(() ->
                log.info("[RankDao] Saved rank: " + rank.getId())
        ).exceptionally(e -> {
            log.error("[RankDao] Failed to save rank: " + e.getMessage());
            return null;
        });
    }

    /**
     * Deletes a rank by ID
     *
     * @param id The rank ID
     * @return CompletableFuture that completes when deleted
     */
    public CompletableFuture<Void> deleteById(String id) {
        return databaseManager.executeUpdate(
                "DELETE FROM ranks WHERE id = '" + id + "'"
        ).thenRun(() ->
                log.info("[RankDao] Deleted rank: " + id)
        ).exceptionally(e -> {
            log.error("[RankDao] Failed to delete rank: " + e.getMessage());
            return null;
        });
    }

    /**
     * Maps a ResultSet to a Rank object
     */
    private Rank mapResultSetToRank(ResultSet resultSet) throws SQLException {
        String permissionsStr = resultSet.getString("permissions");
        List<String> permissions = new ArrayList<>();

        if (permissionsStr != null && !permissionsStr.isEmpty()) {
            String[] parts = permissionsStr.split(",");
            for (String part : parts) {
                if (!part.isEmpty()) {
                    permissions.add(part);
                }
            }
        }

        return new Rank(
                resultSet.getString("id"),
                resultSet.getString("name"),
                resultSet.getString("display_name"),
                resultSet.getString("prefix"),
                resultSet.getString("suffix"),
                resultSet.getInt("priority"),
                resultSet.getBoolean("is_default"),
                permissions
        );
    }
}
