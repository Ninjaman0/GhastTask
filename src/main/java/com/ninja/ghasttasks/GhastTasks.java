package com.ninja.ghasttasks;

import com.ninja.ghasttasks.commands.TaskCommand;
import com.ninja.ghasttasks.database.DatabaseManager;
import com.ninja.ghasttasks.managers.TaskManager;
import com.ninja.ghasttasks.managers.TimeManager;
import com.ninja.ghasttasks.placeholders.GhastTasksPlaceholders;
import org.bukkit.plugin.java.JavaPlugin;

public class GhastTasks extends JavaPlugin {

    private DatabaseManager databaseManager;
    private TaskManager taskManager;
    private TimeManager timeManager;
    private GhastTasksPlaceholders placeholders;

    @Override
    public void onEnable() {
         // papi check
        if (!getServer().getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            getLogger().severe("PlaceholderAPI is required for this plugin to work!");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        getLogger().info("PlaceholderAPI detected, continuing initialization...");

        saveDefaultConfig();

        try {
            // todo: metrics soon . :)
            databaseManager = new DatabaseManager(this);
            taskManager = new TaskManager(this);
            timeManager = new TimeManager(this);


            getLogger().info("Registering PlaceholderAPI expansion...");
            placeholders = new GhastTasksPlaceholders(this);
            if (placeholders.register()) {
                getLogger().info("PlaceholderAPI expansion registered successfully!");
            } else {
                getLogger().warning("Failed to register PlaceholderAPI expansion - placeholders may not work");
            }
            getCommand("ghasttasks").setExecutor(new TaskCommand(this));
                       // Debug dump need to enabled in config...
            getLogger().info("GhastTasks has been enabled successfully!");
            getLogger().info("Total tasks loaded: " + taskManager.getAllTasks().size());

            if (getConfig().getBoolean("debug", false)) {
                getLogger().info("Debug mode is enabled");
                getLogger().info("Database file location: " + databaseManager);
                getLogger().info("Available placeholders:");
                getLogger().info("  %ghasttasks_next_task_id% - ID of the next scheduled task");
                getLogger().info("  %ghasttasks_next_task_time% - Time of the next scheduled task");
                getLogger().info("  %ghasttasks_countdown_seconds% - Seconds until next task");
                getLogger().info("  %ghasttasks_countdown_minutes% - Minutes until next task");
                getLogger().info("  %ghasttasks_countdown_hours% - Hours until next task");
                getLogger().info("  %ghasttasks_countdown_formatted% - Formatted countdown (HH:MM:SS)");
                getLogger().info("  %ghasttasks_countdown_simple% - Simple countdown (1h 30m)");
                getLogger().info("  %ghasttasks_countdown_detailed% - Detailed countdown with task info");
                getLogger().info("  %ghasttasks_time_until_minutes_only% - Only the minutes component");
                getLogger().info("  %ghasttasks_time_until_hours_only% - Only the hours component");
                getLogger().info("  %ghasttasks_time_until_seconds_only% - Only the seconds component");
                getLogger().info("  %ghasttasks_next_taskmsg% - Next task message with countdown");
                getLogger().info("  %ghasttasks_task_<id>_msg% - Specific task message");
                getLogger().info("  %ghasttasks_task_<id>_countdown% - Specific task countdown with message");
            }

        } catch (Exception e) {
            getLogger().severe("Failed to initialize GhastTasks: " + e.getMessage());
            e.printStackTrace();
            getServer().getPluginManager().disablePlugin(this);
        }
    }

    @Override
    public void onDisable() {
             // Cleanup
        if (placeholders != null) {
            placeholders.unregister();
            getLogger().info("PlaceholderAPI expansion unregistered");
        }

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

    public GhastTasksPlaceholders getPlaceholders() {
        return placeholders;
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