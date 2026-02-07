package club.catmc.core.shared.db;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * Manages database connections and queries using HikariCP connection pooling
 */
public class DatabaseManager {

    private final String host;
    private final int port;
    private final String database;
    private final String username;
    private final String password;
    private HikariDataSource dataSource;

    /**
     * Creates a new DatabaseManager instance
     *
     * @param host     Database host
     * @param port     Database port
     * @param database Database name
     * @param username Database username
     * @param password Database password
     */
    public DatabaseManager(String host, int port, String database, String username, String password) {
        this.host = host;
        this.port = port;
        this.database = database;
        this.username = username;
        this.password = password;
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public String getDatabase() {
        return database;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    /**
     * Gets a connection from the pool
     * Note: The connection must be closed after use to return it to the pool
     *
     * @return A connection from the pool
     * @throws SQLException if unable to get a connection
     */
    public Connection getConnection() throws SQLException {
        if (dataSource == null) {
            throw new SQLException("Database not connected. Call connect() first.");
        }
        return dataSource.getConnection();
    }

    /**
     * Connects to the database and initializes the connection pool
     *
     * @return CompletableFuture that completes when connected
     */
    public CompletableFuture<Void> connect() {
        return CompletableFuture.runAsync(() -> {
            try {
                HikariConfig config = new HikariConfig();
                config.setJdbcUrl("jdbc:mysql://" + host + ":" + port + "/" + database + "?useSSL=false");
                config.setUsername(username);
                config.setPassword(password);

                // Connection pool settings
                config.setPoolName("Core-Pool");
                config.setMaximumPoolSize(10); // Maximum connections in the pool
                config.setMinimumIdle(2);      // Minimum idle connections ready to use
                config.setConnectionTimeout(30000); // 30 seconds to get a connection from pool
                config.setIdleTimeout(600000);      // 10 minutes - idle connections are closed
                config.setMaxLifetime(1800000);     // 30 minutes - max lifetime of a connection
                config.setLeakDetectionThreshold(60000); // Detect connection leaks after 60 seconds

                // MySQL-specific optimizations
                config.addDataSourceProperty("cachePrepStmts", "true");
                config.addDataSourceProperty("prepStmtCacheSize", "250");
                config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
                config.addDataSourceProperty("useServerPrepStmts", "true");
                config.addDataSourceProperty("useLocalSessionState", "true");
                config.addDataSourceProperty("rewriteBatchedStatements", "true");
                config.addDataSourceProperty("cacheResultSetMetadata", "true");
                config.addDataSourceProperty("cacheServerConfiguration", "true");
                config.addDataSourceProperty("elideSetAutoCommits", "true");
                config.addDataSourceProperty("maintainTimeStats", "false");

                dataSource = new HikariDataSource(config);

                // Test the connection
                try (Connection testConn = dataSource.getConnection()) {
                    if (testConn.isValid(5)) {
                        System.out.println("[Database] Connected successfully with connection pool!");
                    }
                }
            } catch (SQLException e) {
                System.err.println("[Database] Failed to connect: " + e.getMessage());
                e.printStackTrace();
                throw new RuntimeException(e);
            }
        });
    }

    /**
     * Disconnects from the database and closes the connection pool
     *
     * @return CompletableFuture that completes when disconnected
     */
    public CompletableFuture<Void> disconnect() {
        return CompletableFuture.runAsync(() -> {
            if (dataSource != null && !dataSource.isClosed()) {
                dataSource.close();
                System.out.println("[Database] Connection pool closed successfully!");
            }
        });
    }

    /**
     * Executes a query asynchronously
     * The connection is automatically returned to the pool after execution
     *
     * @param query    The SQL query
     * @param consumer Consumer to handle the result set
     */
    public void executeQuery(String query, Consumer<ResultSet> consumer) {
        CompletableFuture.runAsync(() -> {
            try (Connection connection = getConnection();
                 PreparedStatement statement = connection.prepareStatement(query)) {
                ResultSet resultSet = statement.executeQuery();
                consumer.accept(resultSet);
                resultSet.close();
            } catch (SQLException e) {
                System.err.println("[Database] Query error: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }

    /**
     * Executes an update asynchronously
     * The connection is automatically returned to the pool after execution
     *
     * @param query The SQL update query
     * @return CompletableFuture that completes when updated
     */
    public CompletableFuture<Void> executeUpdate(String query) {
        return CompletableFuture.runAsync(() -> {
            try (Connection connection = getConnection();
                 PreparedStatement statement = connection.prepareStatement(query)) {
                statement.executeUpdate();
            } catch (SQLException e) {
                System.err.println("[Database] Update error: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }

    /**
     * Checks if connected to the database
     *
     * @return true if connected, false otherwise
     */
    public boolean isConnected() {
        return dataSource != null && !dataSource.isClosed();
    }

    /**
     * Gets the number of active connections in the pool
     *
     * @return Number of active connections
     */
    public int getActiveConnections() {
        if (dataSource == null) {
            return 0;
        }
        return dataSource.getHikariPoolMXBean().getActiveConnections();
    }

    /**
     * Gets the number of idle connections in the pool
     *
     * @return Number of idle connections
     */
    public int getIdleConnections() {
        if (dataSource == null) {
            return 0;
        }
        return dataSource.getHikariPoolMXBean().getIdleConnections();
    }

    /**
     * Gets the total number of threads waiting for a connection
     *
     * @return Number of waiting threads
     */
    public int getWaitingThreads() {
        if (dataSource == null) {
            return 0;
        }
        return dataSource.getHikariPoolMXBean().getThreadsAwaitingConnection();
    }
}
