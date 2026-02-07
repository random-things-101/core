package club.catmc.core.shared.grant;

import club.catmc.core.shared.db.DatabaseManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Data Access Object for Grant operations
 */
public class GrantDao {

    private static final Logger log = LoggerFactory.getLogger(GrantDao.class);
    private final DatabaseManager databaseManager;

    public GrantDao(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    /**
     * Creates the grants table if it doesn't exist
     *
     * @return CompletableFuture that completes when table is created
     */
    public CompletableFuture<Void> createTable() {
        String sql = """
                CREATE TABLE IF NOT EXISTS grants (
                    id INT AUTO_INCREMENT PRIMARY KEY,
                    player_uuid VARCHAR(36) NOT NULL,
                    rank_id VARCHAR(64) NOT NULL,
                    granter_uuid VARCHAR(36) NOT NULL,
                    granter_name VARCHAR(16) NOT NULL,
                    granted_at TIMESTAMP NOT NULL,
                    expires_at TIMESTAMP NULL,
                    reason TEXT,
                    is_active BOOLEAN DEFAULT TRUE,
                    INDEX idx_player_uuid (player_uuid),
                    INDEX idx_rank_id (rank_id),
                    INDEX idx_is_active (is_active),
                    INDEX idx_expires_at (expires_at)
                )
                """;

        return databaseManager.executeUpdate(sql).thenRun(() ->
                log.info("[GrantDao] Created grants table")
        ).exceptionally(e -> {
            log.error("[GrantDao] Failed to create table: " + e.getMessage());
            return null;
        });
    }

    /**
     * Finds a grant by ID
     *
     * @param id The grant ID
     * @return CompletableFuture containing Optional<Grant>
     */
    public CompletableFuture<Optional<Grant>> findById(int id) {
        CompletableFuture<Optional<Grant>> future = new CompletableFuture<>();

        databaseManager.executeQuery(
                "SELECT * FROM grants WHERE id = " + id,
                resultSet -> {
                    try {
                        if (resultSet.next()) {
                            future.complete(Optional.of(mapResultSetToGrant(resultSet)));
                        } else {
                            future.complete(Optional.empty());
                        }
                    } catch (SQLException e) {
                        log.error("[GrantDao] Error finding grant: " + e.getMessage());
                        future.completeExceptionally(e);
                    }
                }
        );

        return future;
    }

    /**
     * Finds all grants for a player
     *
     * @param playerUuid The player's UUID
     * @return CompletableFuture containing List<Grant>
     */
    public CompletableFuture<List<Grant>> findByPlayer(UUID playerUuid) {
        CompletableFuture<List<Grant>> future = new CompletableFuture<>();
        List<Grant> grants = new ArrayList<>();

        databaseManager.executeQuery(
                "SELECT * FROM grants WHERE player_uuid = '" + playerUuid.toString() + "' ORDER BY granted_at DESC",
                resultSet -> {
                    try {
                        while (resultSet.next()) {
                            grants.add(mapResultSetToGrant(resultSet));
                        }
                        future.complete(grants);
                    } catch (SQLException e) {
                        log.error("[GrantDao] Error finding grants for player: " + e.getMessage());
                        future.completeExceptionally(e);
                    }
                }
        );

        return future;
    }

    /**
     * Finds all active grants for a player
     *
     * @param playerUuid The player's UUID
     * @return CompletableFuture containing List<Grant>
     */
    public CompletableFuture<List<Grant>> findActiveByPlayer(UUID playerUuid) {
        CompletableFuture<List<Grant>> future = new CompletableFuture<>();
        List<Grant> grants = new ArrayList<>();

        databaseManager.executeQuery(
                "SELECT * FROM grants WHERE player_uuid = '" + playerUuid.toString() + "' AND is_active = TRUE ORDER BY granted_at DESC",
                resultSet -> {
                    try {
                        while (resultSet.next()) {
                            grants.add(mapResultSetToGrant(resultSet));
                        }
                        future.complete(grants);
                    } catch (SQLException e) {
                        log.error("[GrantDao] Error finding active grants for player: " + e.getMessage());
                        future.completeExceptionally(e);
                    }
                }
        );

        return future;
    }

    /**
     * Finds all active grants for a player (including expired ones that are still marked active)
     * This is useful for cleaning up expired grants
     *
     * @param playerUuid The player's UUID
     * @return CompletableFuture containing List<Grant>
     */
    public CompletableFuture<List<Grant>> findActiveByPlayerIncludingExpired(UUID playerUuid) {
        CompletableFuture<List<Grant>> future = new CompletableFuture<>();
        List<Grant> grants = new ArrayList<>();

        databaseManager.executeQuery(
                "SELECT * FROM grants WHERE player_uuid = '" + playerUuid.toString() + "' AND is_active = TRUE ORDER BY granted_at DESC",
                resultSet -> {
                    try {
                        while (resultSet.next()) {
                            grants.add(mapResultSetToGrant(resultSet));
                        }
                        future.complete(grants);
                    } catch (SQLException e) {
                        log.error("[GrantDao] Error finding active grants for player: " + e.getMessage());
                        future.completeExceptionally(e);
                    }
                }
        );

        return future;
    }

    /**
     * Finds all grants for a specific rank
     *
     * @param rankId The rank ID
     * @return CompletableFuture containing List<Grant>
     */
    public CompletableFuture<List<Grant>> findByRank(String rankId) {
        CompletableFuture<List<Grant>> future = new CompletableFuture<>();
        List<Grant> grants = new ArrayList<>();

        databaseManager.executeQuery(
                "SELECT * FROM grants WHERE rank_id = '" + rankId + "' ORDER BY granted_at DESC",
                resultSet -> {
                    try {
                        while (resultSet.next()) {
                            grants.add(mapResultSetToGrant(resultSet));
                        }
                        future.complete(grants);
                    } catch (SQLException e) {
                        log.error("[GrantDao] Error finding grants for rank: " + e.getMessage());
                        future.completeExceptionally(e);
                    }
                }
        );

        return future;
    }

    /**
     * Saves or updates a grant
     *
     * @param grant The grant to save
     * @return CompletableFuture that completes when saved
     */
    public CompletableFuture<Void> save(Grant grant) {
        String expiresAtSql = grant.getExpiresAt() != null
                ? "'" + grant.getExpiresAt().toString() + "'"
                : "NULL";

        String reasonSql = grant.getReason() != null
                ? "'" + grant.getReason().replace("'", "''") + "'"
                : "NULL";

        String sql = String.format(
                "INSERT INTO grants (player_uuid, rank_id, granter_uuid, granter_name, granted_at, expires_at, reason, is_active) " +
                        "VALUES ('%s', '%s', '%s', '%s', '%s', %s, %s, %b)",
                grant.getPlayerUuid().toString(),
                grant.getRankId(),
                grant.getGranterUuid().toString(),
                grant.getGranterName(),
                grant.getGrantedAt().toString(),
                expiresAtSql,
                reasonSql,
                grant.isActive()
        );

        return databaseManager.executeUpdate(sql).thenRun(() ->
                log.info("[GrantDao] Saved grant for player: " + grant.getPlayerUuid())
        ).exceptionally(e -> {
            log.error("[GrantDao] Failed to save grant: " + e.getMessage());
            return null;
        });
    }

    /**
     * Updates a grant's active status
     *
     * @param grantId The grant ID
     * @param active  The new active status
     * @return CompletableFuture that completes when updated
     */
    public CompletableFuture<Void> updateActiveStatus(int grantId, boolean active) {
        return databaseManager.executeUpdate(
                "UPDATE grants SET is_active = " + active + " WHERE id = " + grantId
        ).thenRun(() ->
                log.info("[GrantDao] Updated active status for grant: " + grantId)
        ).exceptionally(e -> {
            log.error("[GrantDao] Failed to update active status: " + e.getMessage());
            return null;
        });
    }

    /**
     * Deletes a grant by ID
     *
     * @param grantId The grant ID
     * @return CompletableFuture that completes when deleted
     */
    public CompletableFuture<Void> deleteById(int grantId) {
        return databaseManager.executeUpdate(
                "DELETE FROM grants WHERE id = " + grantId
        ).thenRun(() ->
                log.info("[GrantDao] Deleted grant: " + grantId)
        ).exceptionally(e -> {
            log.error("[GrantDao] Failed to delete grant: " + e.getMessage());
            return null;
        });
    }

    /**
     * Deletes all grants for a player
     *
     * @param playerUuid The player's UUID
     * @return CompletableFuture that completes when deleted
     */
    public CompletableFuture<Void> deleteByPlayer(UUID playerUuid) {
        return databaseManager.executeUpdate(
                "DELETE FROM grants WHERE player_uuid = '" + playerUuid.toString() + "'"
        ).thenRun(() ->
                log.info("[GrantDao] Deleted all grants for player: " + playerUuid)
        ).exceptionally(e -> {
            log.error("[GrantDao] Failed to delete grants for player: " + e.getMessage());
            return null;
        });
    }

    /**
     * Cleans up expired grants by marking them as inactive
     *
     * @return CompletableFuture containing the number of grants cleaned up
     */
    public CompletableFuture<Integer> cleanupExpiredGrants() {
        CompletableFuture<Integer> future = new CompletableFuture<>();

        databaseManager.executeUpdate(
                "UPDATE grants SET is_active = FALSE WHERE expires_at IS NOT NULL AND expires_at < NOW()"
        ).thenRun(() -> {
            log.info("[GrantDao] Cleaned up expired grants");
            future.complete(0); // We don't get the actual count without another query
        }).exceptionally(e -> {
            log.error("[GrantDao] Failed to cleanup expired grants: " + e.getMessage());
            future.completeExceptionally(e);
            return null;
        });

        return future;
    }

    /**
     * Maps a ResultSet to a Grant object
     */
    private Grant mapResultSetToGrant(ResultSet resultSet) throws SQLException {
        return new Grant(
                resultSet.getInt("id"),
                UUID.fromString(resultSet.getString("player_uuid")),
                resultSet.getString("rank_id"),
                UUID.fromString(resultSet.getString("granter_uuid")),
                resultSet.getString("granter_name"),
                resultSet.getTimestamp("granted_at") != null
                        ? resultSet.getTimestamp("granted_at").toLocalDateTime()
                        : null,
                resultSet.getTimestamp("expires_at") != null
                        ? resultSet.getTimestamp("expires_at").toLocalDateTime()
                        : null,
                resultSet.getString("reason"),
                resultSet.getBoolean("is_active")
        );
    }
}
