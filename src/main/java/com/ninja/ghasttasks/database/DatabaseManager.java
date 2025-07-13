package com.ninja.ghasttasks.database;

import com.ninja.ghasttasks.GhastTasks;

import java.io.File;
import java.sql.*;
import java.time.LocalDate;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Manages SQLite database operations for task execution tracking
 */
public class DatabaseManager {
    private final GhastTasks plugin;
    private Connection connection;
    private final String databasePath;
    private final ReentrantLock connectionLock = new ReentrantLock();

    public DatabaseManager(GhastTasks plugin) throws SQLException {
        this.plugin = plugin;

        // Ensure data folder exists first
        if (!plugin.getDataFolder().exists()) {
            boolean created = plugin.getDataFolder().mkdirs();
            plugin.getLogger().info("Data folder created: " + created);
        }

        this.databasePath = new File(plugin.getDataFolder(), "tasks.db").getAbsolutePath();
        plugin.getLogger().info("Database path: " + this.databasePath);

        initializeDatabase();
    }

    private void initializeDatabase() throws SQLException {
        plugin.getLogger().info("Initializing database...");
        connect();
        createTables();
        verifyTables();
        plugin.getLogger().info("Database initialization completed successfully");
    }

    private void connect() throws SQLException {
        connectionLock.lock();
        try {
            if (connection != null && !connection.isClosed()) {
                plugin.getLogger().info("Database connection already established");
                return;
            }

            String url = "jdbc:sqlite:" + databasePath;
            plugin.getLogger().info("Connecting to database with URL: " + url);

            connection = DriverManager.getConnection(url);

            // Enable foreign keys and other optimizations
            try (Statement stmt = connection.createStatement()) {
                stmt.execute("PRAGMA foreign_keys = ON");
                stmt.execute("PRAGMA journal_mode = WAL");
                stmt.execute("PRAGMA synchronous = NORMAL");
                stmt.execute("PRAGMA cache_size = 10000");
                stmt.execute("PRAGMA temp_store = MEMORY");
                plugin.getLogger().info("Database PRAGMA settings applied");
            }

            plugin.getLogger().info("Connected to SQLite database successfully: " + databasePath);
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to connect to database: " + e.getMessage());
            e.printStackTrace();
            throw e;
        } finally {
            connectionLock.unlock();
        }
    }

    private void createTables() throws SQLException {
        plugin.getLogger().info("Creating database tables...");

        String createTableSQL = """
            CREATE TABLE IF NOT EXISTS executed_tasks (
                task_id INTEGER NOT NULL,
                execution_date DATE NOT NULL,
                execution_timestamp DATETIME DEFAULT CURRENT_TIMESTAMP,
                PRIMARY KEY (task_id, execution_date)
            )
            """;

        String createIndexSQL = """
            CREATE INDEX IF NOT EXISTS idx_task_date ON executed_tasks(task_id, execution_date)
            """;

        connectionLock.lock();
        try (Statement stmt = connection.createStatement()) {
            plugin.getLogger().info("Executing CREATE TABLE statement...");
            stmt.execute(createTableSQL);
            plugin.getLogger().info("Table 'executed_tasks' created/verified");

            plugin.getLogger().info("Creating index...");
            stmt.execute(createIndexSQL);
            plugin.getLogger().info("Index 'idx_task_date' created/verified");

            plugin.getLogger().info("Database tables and indexes initialized successfully");
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to create tables: " + e.getMessage());
            e.printStackTrace();
            throw e;
        } finally {
            connectionLock.unlock();
        }
    }

    private void verifyTables() {
        plugin.getLogger().info("Verifying database tables...");
        connectionLock.lock();
        try (Statement stmt = connection.createStatement()) {
            // Check if table exists and has correct structure
            ResultSet rs = stmt.executeQuery("SELECT name FROM sqlite_master WHERE type='table' AND name='executed_tasks'");
            if (rs.next()) {
                plugin.getLogger().info("Table 'executed_tasks' verified to exist");

                // Check table structure
                ResultSet columns = stmt.executeQuery("PRAGMA table_info(executed_tasks)");
                plugin.getLogger().info("Table structure:");
                while (columns.next()) {
                    String columnName = columns.getString("name");
                    String columnType = columns.getString("type");
                    boolean notNull = columns.getBoolean("notnull");
                    boolean pk = columns.getBoolean("pk");
                    plugin.getLogger().info("  Column: " + columnName + " (" + columnType +
                            ", NOT NULL: " + notNull + ", PK: " + pk + ")");
                }

                // Check row count
                ResultSet count = stmt.executeQuery("SELECT COUNT(*) as count FROM executed_tasks");
                if (count.next()) {
                    int rowCount = count.getInt("count");
                    plugin.getLogger().info("Table 'executed_tasks' contains " + rowCount + " records");
                }
            } else {
                plugin.getLogger().severe("Table 'executed_tasks' does not exist after creation attempt!");
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Error verifying tables: " + e.getMessage());
            e.printStackTrace();
        } finally {
            connectionLock.unlock();
        }
    }

    public CompletableFuture<Boolean> hasTaskExecutedToday(int taskId) {
        return CompletableFuture.supplyAsync(() -> {
            connectionLock.lock();
            try {
                ensureConnection();
                String sql = "SELECT 1 FROM executed_tasks WHERE task_id = ? AND execution_date = ? LIMIT 1";

                try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                    stmt.setInt(1, taskId);
                    String today = LocalDate.now().toString();
                    stmt.setString(2, today);

                    plugin.getLogger().info("Checking if task " + taskId + " executed today (" + today + ")");

                    try (ResultSet rs = stmt.executeQuery()) {
                        boolean executed = rs.next();

                        plugin.getLogger().info("Task " + taskId + " executed today: " + executed);

                        return executed;
                    }
                }
            } catch (SQLException e) {
                plugin.getLogger().severe("Error checking task execution status for task " + taskId + ": " + e.getMessage());
                e.printStackTrace();
                return false;
            } finally {
                connectionLock.unlock();
            }
        });
    }

    public CompletableFuture<Void> markTaskExecuted(int taskId) {
        return CompletableFuture.runAsync(() -> {
            connectionLock.lock();
            try {
                ensureConnection();
                String sql = "INSERT OR REPLACE INTO executed_tasks (task_id, execution_date) VALUES (?, ?)";

                try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                    stmt.setInt(1, taskId);
                    String today = LocalDate.now().toString();
                    stmt.setString(2, today);
                    int rowsAffected = stmt.executeUpdate();

                    plugin.getLogger().info("Marked task " + taskId + " as executed for today (" + today + ") - rows affected: " + rowsAffected);

                    // Verify the insert worked
                    try (PreparedStatement verifyStmt = connection.prepareStatement("SELECT COUNT(*) FROM executed_tasks WHERE task_id = ? AND execution_date = ?")) {
                        verifyStmt.setInt(1, taskId);
                        verifyStmt.setString(2, today);
                        try (ResultSet rs = verifyStmt.executeQuery()) {
                            if (rs.next()) {
                                int count = rs.getInt(1);
                                plugin.getLogger().info("Verification: Found " + count + " records for task " + taskId + " on " + today);
                            }
                        }
                    }
                }
            } catch (SQLException e) {
                plugin.getLogger().severe("Error marking task " + taskId + " as executed: " + e.getMessage());
                e.printStackTrace();
            } finally {
                connectionLock.unlock();
            }
        });
    }

    public CompletableFuture<Void> removeTaskRecords(int taskId) {
        return CompletableFuture.runAsync(() -> {
            connectionLock.lock();
            try {
                ensureConnection();
                String sql = "DELETE FROM executed_tasks WHERE task_id = ?";

                try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                    stmt.setInt(1, taskId);
                    int deleted = stmt.executeUpdate();

                    plugin.getLogger().info("Removed " + deleted + " execution records for task " + taskId);
                }
            } catch (SQLException e) {
                plugin.getLogger().severe("Error removing task records for task " + taskId + ": " + e.getMessage());
                e.printStackTrace();
            } finally {
                connectionLock.unlock();
            }
        });
    }

    private void ensureConnection() throws SQLException {
        if (connection == null || connection.isClosed()) {
            plugin.getLogger().info("Reconnecting to database...");
            connect();
        }
    }

    public void close() {
        connectionLock.lock();
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                plugin.getLogger().info("Database connection closed");
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Error closing database connection: " + e.getMessage());
            e.printStackTrace();
        } finally {
            connectionLock.unlock();
        }
    }
}