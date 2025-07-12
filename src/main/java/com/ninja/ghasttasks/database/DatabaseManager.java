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
        this.databasePath = new File(plugin.getDataFolder(), "tasks.db").getAbsolutePath();
        initializeDatabase();
    }
    
    private void initializeDatabase() throws SQLException {
        connect();
        createTables();
    }
    
    private void connect() throws SQLException {
        connectionLock.lock();
        try {
            if (connection != null && !connection.isClosed()) {
                return;
            }
            
            // Ensure data folder exists
            plugin.getDataFolder().mkdirs();
            
            String url = "jdbc:sqlite:" + databasePath;
            connection = DriverManager.getConnection(url);
            
            // Enable foreign keys and other optimizations
            try (Statement stmt = connection.createStatement()) {
                stmt.execute("PRAGMA foreign_keys = ON");
                stmt.execute("PRAGMA journal_mode = WAL");
                stmt.execute("PRAGMA synchronous = NORMAL");
                stmt.execute("PRAGMA cache_size = 10000");
                stmt.execute("PRAGMA temp_store = MEMORY");
            }
            
            plugin.getLogger().info("Connected to SQLite database: " + databasePath);
        } finally {
            connectionLock.unlock();
        }
    }
    
    private void createTables() throws SQLException {
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
            stmt.execute(createTableSQL);
            stmt.execute(createIndexSQL);
            plugin.getLogger().info("Database tables and indexes initialized successfully");
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
                    stmt.setString(2, LocalDate.now().toString());
                    
                    try (ResultSet rs = stmt.executeQuery()) {
                        boolean executed = rs.next();
                        
                        if (plugin.getConfig().getBoolean("debug", false)) {
                            plugin.getLogger().info("Task " + taskId + " executed today: " + executed);
                        }
                        
                        return executed;
                    }
                }
            } catch (SQLException e) {
                plugin.getLogger().severe("Error checking task execution status for task " + taskId + ": " + e.getMessage());
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
                    stmt.setString(2, LocalDate.now().toString());
                    int rowsAffected = stmt.executeUpdate();
                    
                    if (plugin.getConfig().getBoolean("debug", false)) {
                        plugin.getLogger().info("Marked task " + taskId + " as executed for today (rows affected: " + rowsAffected + ")");
                    }
                }
            } catch (SQLException e) {
                plugin.getLogger().severe("Error marking task " + taskId + " as executed: " + e.getMessage());
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
                    
                    if (plugin.getConfig().getBoolean("debug", false)) {
                        plugin.getLogger().info("Removed " + deleted + " execution records for task " + taskId);
                    }
                }
            } catch (SQLException e) {
                plugin.getLogger().severe("Error removing task records for task " + taskId + ": " + e.getMessage());
            } finally {
                connectionLock.unlock();
            }
        });
    }
    
    private void ensureConnection() throws SQLException {
        if (connection == null || connection.isClosed()) {
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
        } finally {
            connectionLock.unlock();
        }
    }
}