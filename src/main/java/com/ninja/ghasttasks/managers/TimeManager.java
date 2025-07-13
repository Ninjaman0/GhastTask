package com.ninja.ghasttasks.managers;

import com.ninja.ghasttasks.GhastTasks;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitTask;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class TimeManager {
    private final GhastTasks plugin;
    private BukkitTask timeCheckTask;
    private final Set<String> executedThisMinute = ConcurrentHashMap.newKeySet();
    private String lastMinute = "";
    private LocalTime lastCheckedTime = null;

    public TimeManager(GhastTasks plugin) {
        this.plugin = plugin;
        plugin.getLogger().info("TimeManager initialized - using system time");
        startTimeChecker();
    }

    private void startTimeChecker() {
        // Check every 10 seconds to reduce server load while maintaining accuracy
        timeCheckTask = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, this::checkTime, 0L, 200L);
}

    private void checkTime() {
        try {
            LocalTime currentTime = LocalTime.now();
            String currentMinute = currentTime.format(DateTimeFormatter.ofPattern("HH:mm"));

            // Only process if we've moved to a new minute AND the time actually changed
            if (!currentMinute.equals(lastMinute) && !currentTime.equals(lastCheckedTime)) {
                executedThisMinute.clear();
                lastMinute = currentMinute;
                lastCheckedTime = currentTime;


                // Check all tasks for execution
                checkAndExecuteTasks(currentTime, currentMinute);
            }

        } catch (Exception e) {
            plugin.getLogger().severe("Error in time checker: " + e.getMessage());
            if (plugin.getConfig().getBoolean("debug", false)) {
                e.printStackTrace();
            }
        }
    }

    private void checkAndExecuteTasks(LocalTime currentTime, String currentMinute) {
        for (Integer taskId : plugin.getTaskManager().getAllTasks().keySet()) {
            String taskKey = taskId + ":" + currentMinute;

            // Skip if already executed this minute
            if (executedThisMinute.contains(taskKey)) {
                continue;
            }

            if (plugin.getTaskManager().shouldExecuteTask(taskId, currentTime)) {
                // Mark as executed this minute IMMEDIATELY to prevent race conditions
                executedThisMinute.add(taskKey);

                plugin.getLogger().info("Task " + taskId + " scheduled for execution at " + currentMinute);

                // Check database to ensure not executed today
                plugin.getDatabaseManager().hasTaskExecutedToday(taskId)
                        .thenAccept(hasExecuted -> {
                            if (!hasExecuted) {
                                plugin.getLogger().info("Executing task " + taskId + " - not executed today");
                                // Execute the task
                                plugin.getTaskManager().executeTask(taskId);
                            } else {
                                plugin.getLogger().info("Task " + taskId + " already executed today - skipping");
                            }
                        })
                        .exceptionally(throwable -> {
                            plugin.getLogger().severe("Error checking task execution status for task " + taskId + ": " + throwable.getMessage());
                            throwable.printStackTrace();
                            return null;
                        });
            }
        }
    }

    public LocalTime getCurrentServerTime() {
        return LocalTime.now();
    }

    public void shutdown() {
        if (timeCheckTask != null && !timeCheckTask.isCancelled()) {
            timeCheckTask.cancel();
            plugin.getLogger().info("Time checker stopped");
        }
        executedThisMinute.clear();
    }

    // Method to manually test time - useful for debugging
    public void testTime() {
        plugin.getLogger().info("=== Manual Time Test ===");

        LocalTime currentTime = getCurrentServerTime();
        plugin.getLogger().info("Current system time: " + currentTime.format(DateTimeFormatter.ofPattern("HH:mm:ss")));

        // Test database connection
        plugin.getLogger().info("Testing database connection...");
        plugin.getDatabaseManager().hasTaskExecutedToday(999)
                .thenAccept(result -> {
                    plugin.getLogger().info("Database test completed successfully. Test query result: " + result);
                })
                .exceptionally(throwable -> {
                    plugin.getLogger().severe("Database test failed: " + throwable.getMessage());
                    throwable.printStackTrace();
                    return null;
                });
    }
}