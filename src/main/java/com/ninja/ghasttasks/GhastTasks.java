package com.ninja.ghasttasks;

import com.ninja.ghasttasks.commands.TaskCommand;
import com.ninja.ghasttasks.database.DatabaseManager;
import com.ninja.ghasttasks.managers.TaskManager;
import com.ninja.ghasttasks.managers.TimeManager;
import org.bukkit.plugin.java.JavaPlugin;

public class GhastTasks extends JavaPlugin {

    private DatabaseManager databaseManager;
    private TaskManager taskManager;
    private TimeManager timeManager;

    @Override
    public void onEnable() {
        // Check if PlaceholderAPI is available
        if (!getServer().getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            getLogger().severe("PlaceholderAPI is required for this plugin to work!");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        getLogger().info("PlaceholderAPI detected, continuing initialization...");

        // Save default config if it doesn't exist
        saveDefaultConfig();

        // Initialize managers
        try {
            getLogger().info("Initializing database manager...");
            databaseManager = new DatabaseManager(this);

            getLogger().info("Initializing task manager...");
            taskManager = new TaskManager(this);

            getLogger().info("Initializing time manager...");
            timeManager = new TimeManager(this);

            // Register commands
            getCommand("ghasttasks").setExecutor(new TaskCommand(this));

            getLogger().info("GhastTasks has been enabled successfully!");
            getLogger().info("Total tasks loaded: " + taskManager.getAllTasks().size());

            // Print debug info if enabled
            if (getConfig().getBoolean("debug", false)) {
                getLogger().info("Debug mode is enabled");
                getLogger().info("Database file location: " + databaseManager);
            }

        } catch (Exception e) {
            getLogger().severe("Failed to initialize GhastTasks: " + e.getMessage());
            e.printStackTrace();
            getServer().getPluginManager().disablePlugin(this);
        }
    }

    @Override
    public void onDisable() {
        if (timeManager != null) {
            timeManager.shutdown();
        }
        if (databaseManager != null) {
            databaseManager.close();
        }
        getLogger().info("GhastTasks has been disabled.");
    }

    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }

    public TaskManager getTaskManager() {
        return taskManager;
    }

    public TimeManager getTimeManager() {
        return timeManager;
    }

    public void reloadPlugin() {
        try {
            reloadConfig();
            taskManager.reloadTasks();
            getLogger().info("GhastTasks configuration reloaded successfully!");
        } catch (Exception e) {
            getLogger().severe("Failed to reload GhastTasks: " + e.getMessage());
            e.printStackTrace();
        }
    }
}