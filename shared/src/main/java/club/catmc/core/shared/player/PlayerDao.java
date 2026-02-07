package club.catmc.core.shared.player;

import club.catmc.core.shared.db.DatabaseManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Data Access Object for Player operations
 */
public class PlayerDao {

    private static final Logger log = LoggerFactory.getLogger(PlayerDao.class);
    private final DatabaseManager databaseManager;

    public PlayerDao(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    /**
     * Creates the players table if it doesn't exist
     *
     * @return CompletableFuture that completes when table is created
     */
    public CompletableFuture<Void> createTable() {
        String sql = """
                CREATE TABLE IF NOT EXISTS players (
                    uuid VARCHAR(36) PRIMARY KEY,
                    username VARCHAR(16) NOT NULL,
                    playtime_ticks BIGINT DEFAULT 0,
                    first_login TIMESTAMP,
                    last_login TIMESTAMP,
                    is_online BOOLEAN DEFAULT FALSE,
                    additional_permissions TEXT
                )
                """;

        return databaseManager.executeUpdate(sql).thenRun(() ->
                log.info("[PlayerDao] Created players table")
        ).exceptionally(e -> {
            log.error("[PlayerDao] Failed to create table: " + e.getMessage());
            return null;
        });
    }

    /**
     * Finds a player by UUID
     *
     * @param uuid The player's UUID
     * @return CompletableFuture containing Optional<Player>
     */
    public CompletableFuture<Optional<Player>> findByUuid(UUID uuid) {
        CompletableFuture<Optional<Player>> future = new CompletableFuture<>();

        databaseManager.executeQuery(
                "SELECT * FROM players WHERE uuid = '" + uuid.toString() + "'",
                resultSet -> {
                    try {
                        if (resultSet.next()) {
                            future.complete(Optional.of(mapResultSetToPlayer(resultSet)));
                        } else {
                            future.complete(Optional.empty());
                        }
                    } catch (SQLException e) {
                        log.error("[PlayerDao] Error finding player: " + e.getMessage());
                        future.completeExceptionally(e);
                    }
                }
        );

        return future;
    }

    /**
     * Finds a player by username
     *
     * @param username The player's username
     * @return CompletableFuture containing Optional<Player>
     */
    public CompletableFuture<Optional<Player>> findByUsername(String username) {
        CompletableFuture<Optional<Player>> future = new CompletableFuture<>();

        databaseManager.executeQuery(
                "SELECT * FROM players WHERE username = '" + username + "' LIMIT 1",
                resultSet -> {
                    try {
                        if (resultSet.next()) {
                            future.complete(Optional.of(mapResultSetToPlayer(resultSet)));
                        } else {
                            future.complete(Optional.empty());
                        }
                    } catch (SQLException e) {
                        log.error("[PlayerDao] Error finding player by username: " + e.getMessage());
                        future.completeExceptionally(e);
                    }
                }
        );

        return future;
    }

    /**
     * Gets all online players
     *
     * @return CompletableFuture containing List<Player>
     */
    public CompletableFuture<List<Player>> findOnlinePlayers() {
        CompletableFuture<List<Player>> future = new CompletableFuture<>();
        List<Player> players = new ArrayList<>();

        databaseManager.executeQuery(
                "SELECT * FROM players WHERE is_online = TRUE",
                resultSet -> {
                    try {
                        while (resultSet.next()) {
                            players.add(mapResultSetToPlayer(resultSet));
                        }
                        future.complete(players);
                    } catch (SQLException e) {
                        log.error("[PlayerDao] Error finding online players: " + e.getMessage());
                        future.completeExceptionally(e);
                    }
                }
        );

        return future;
    }

    /**
     * Gets the top players by playtime
     *
     * @param limit Maximum number of players to return
     * @return CompletableFuture containing List<Player>
     */
    public CompletableFuture<List<Player>> findTopByPlaytime(int limit) {
        CompletableFuture<List<Player>> future = new CompletableFuture<>();
        List<Player> players = new ArrayList<>();

        databaseManager.executeQuery(
                "SELECT * FROM players ORDER BY playtime_ticks DESC LIMIT " + limit,
                resultSet -> {
                    try {
                        while (resultSet.next()) {
                            players.add(mapResultSetToPlayer(resultSet));
                        }
                        future.complete(players);
                    } catch (SQLException e) {
                        log.error("[PlayerDao] Error finding top players: " + e.getMessage());
                        future.completeExceptionally(e);
                    }
                }
        );

        return future;
    }

    /**
     * Saves or updates a player
     *
     * @param player The player to save
     * @return CompletableFuture that completes when saved
     */
    public CompletableFuture<Void> save(Player player) {
        String permissions = player.getAdditionalPermissions() != null
                ? String.join(",", player.getAdditionalPermissions())
                : "";

        String sql = String.format(
                "INSERT INTO players (uuid, username, playtime_ticks, first_login, last_login, is_online, additional_permissions) " +
                        "VALUES ('%s', '%s', %d, %s, %s, %b, '%s') " +
                        "ON DUPLICATE KEY UPDATE " +
                        "username = '%s', playtime_ticks = %d, " +
                        "first_login = %s, last_login = %s, is_online = %b, additional_permissions = '%s'",
                player.getUuid().toString(), player.getUsername(), player.getPlaytimeTicks(),
                timestampToSql(player.getFirstLogin()),
                timestampToSql(player.getLastLogin()),
                player.isOnline(), permissions,
                player.getUsername(), player.getPlaytimeTicks(),
                timestampToSql(player.getFirstLogin()),
                timestampToSql(player.getLastLogin()),
                player.isOnline(), permissions
        );

        return databaseManager.executeUpdate(sql).thenRun(() ->
                log.info("[PlayerDao] Saved player: " + player.getUsername())
        ).exceptionally(e -> {
            log.error("[PlayerDao] Failed to save player: " + e.getMessage());
            return null;
        });
    }

    /**
     * Deletes a player by UUID
     *
     * @param uuid The player's UUID
     * @return CompletableFuture that completes when deleted
     */
    public CompletableFuture<Void> deleteByUuid(UUID uuid) {
        return databaseManager.executeUpdate(
                "DELETE FROM players WHERE uuid = '" + uuid.toString() + "'"
        ).thenRun(() ->
                log.info("[PlayerDao] Deleted player: " + uuid)
        ).exceptionally(e -> {
            log.error("[PlayerDao] Failed to delete player: " + e.getMessage());
            return null;
        });
    }

    /**
     * Updates a player's online status
     *
     * @param uuid   The player's UUID
     * @param online The online status
     * @return CompletableFuture that completes when updated
     */
    public CompletableFuture<Void> updateOnlineStatus(UUID uuid, boolean online) {
        return databaseManager.executeUpdate(
                "UPDATE players SET is_online = " + online + ", last_login = " + getCurrentTimestamp() +
                        " WHERE uuid = '" + uuid.toString() + "'"
        ).thenRun(() ->
                log.info("[PlayerDao] Updated online status for: " + uuid)
        ).exceptionally(e -> {
            log.error("[PlayerDao] Failed to update online status: " + e.getMessage());
            return null;
        });
    }

    /**
     * Increments a player's playtime
     *
     * @param uuid   The player's UUID
     * @param ticks  Number of ticks to add
     * @return CompletableFuture that completes when updated
     */
    public CompletableFuture<Void> incrementPlaytime(UUID uuid, long ticks) {
        return databaseManager.executeUpdate(
                "UPDATE players SET playtime_ticks = playtime_ticks + " + ticks +
                        " WHERE uuid = '" + uuid.toString() + "'"
        ).exceptionally(e -> {
            log.error("[PlayerDao] Failed to increment playtime: " + e.getMessage());
            return null;
        });
    }

    /**
     * Maps a ResultSet to a Player object
     */
    private Player mapResultSetToPlayer(ResultSet resultSet) throws SQLException {
        String permissionsStr = resultSet.getString("additional_permissions");
        List<String> permissions = new ArrayList<>();

        if (permissionsStr != null && !permissionsStr.isEmpty()) {
            String[] parts = permissionsStr.split(",");
            for (String part : parts) {
                if (!part.isEmpty()) {
                    permissions.add(part);
                }
            }
        }

        Player player = new Player(
                UUID.fromString(resultSet.getString("uuid")),
                resultSet.getString("username")
        );
        player.setPlaytimeTicks(resultSet.getLong("playtime_ticks"));
        player.setFirstLogin(resultSet.getTimestamp("first_login") != null
                ? resultSet.getTimestamp("first_login").toLocalDateTime()
                : null);
        player.setLastLogin(resultSet.getTimestamp("last_login") != null
                ? resultSet.getTimestamp("last_login").toLocalDateTime()
                : null);
        player.setOnline(resultSet.getBoolean("is_online"));
        player.setAdditionalPermissions(permissions);

        return player;
    }

    /**
     * Converts a LocalDateTime to SQL timestamp string
     */
    private String timestampToSql(LocalDateTime dateTime) {
        if (dateTime == null) {
            return "NULL";
        }
        return "'" + dateTime.toString() + "'";
    }

    /**
     * Gets current timestamp as SQL string
     */
    private String getCurrentTimestamp() {
        return "NOW()";
    }
}
