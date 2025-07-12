package com.ninja.ghasttasks.managers;

import com.ninja.ghasttasks.GhastTasks;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitTask;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
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
        startTimeChecker();
    }
    
    private void startTimeChecker() {
        // Check every 10 seconds to reduce server load while maintaining accuracy
        timeCheckTask = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, this::checkTime, 0L, 200L);
        plugin.getLogger().info("Time checker started (checking every 10 seconds)");
    }
    
    private void checkTime() {
        try {
            LocalTime currentTime = getCurrentServerTime();
            if (currentTime == null) {
                if (plugin.getConfig().getBoolean("debug", false)) {
                    plugin.getLogger().warning("Could not get current server time, skipping check");
                }
                return;
            }
            
            String currentMinute = currentTime.format(DateTimeFormatter.ofPattern("HH:mm"));
            
            // Only process if we've moved to a new minute AND the time actually changed
            if (!currentMinute.equals(lastMinute) && !currentTime.equals(lastCheckedTime)) {
                executedThisMinute.clear();
                lastMinute = currentMinute;
                lastCheckedTime = currentTime;
                
                if (plugin.getConfig().getBoolean("debug", false)) {
                    plugin.getLogger().info("New minute detected: " + currentMinute + " - checking tasks");
                }
                
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
                
                if (plugin.getConfig().getBoolean("debug", false)) {
                    plugin.getLogger().info("Task " + taskId + " scheduled for execution at " + currentMinute);
                }
                
                // Check database to ensure not executed today
                plugin.getDatabaseManager().hasTaskExecutedToday(taskId)
                    .thenAccept(hasExecuted -> {
                        if (!hasExecuted) {
                            if (plugin.getConfig().getBoolean("debug", false)) {
                                plugin.getLogger().info("Executing task " + taskId + " - not executed today");
                            }
                            // Execute the task
                            plugin.getTaskManager().executeTask(taskId);
                        } else {
                            if (plugin.getConfig().getBoolean("debug", false)) {
                                plugin.getLogger().info("Task " + taskId + " already executed today - skipping");
                            }
                        }
                    })
                    .exceptionally(throwable -> {
                        plugin.getLogger().severe("Error checking task execution status for task " + taskId + ": " + throwable.getMessage());
                        return null;
                    });
            }
        }
    }
    
    private LocalTime parseServerTime(String timeStr) {
        if (timeStr == null || timeStr.trim().isEmpty()) {
            return null;
        }
        
        try {
            timeStr = timeStr.trim();
            
            // Try different time formats
            if (timeStr.length() == 5 && timeStr.contains(":")) {
                // Format: HH:mm
                return LocalTime.parse(timeStr, DateTimeFormatter.ofPattern("HH:mm"));
            } else if (timeStr.length() == 4 && timeStr.matches("\\d{4}")) {
                // Format: HHmm
                return LocalTime.parse(timeStr, DateTimeFormatter.ofPattern("HHmm"));
            } else if (timeStr.length() == 8 && timeStr.contains(":")) {
                // Format: HH:mm:ss
                return LocalTime.parse(timeStr, DateTimeFormatter.ofPattern("HH:mm:ss"));
            } else if (timeStr.length() == 6 && timeStr.matches("\\d{6}")) {
                // Format: HHmmss
                return LocalTime.parse(timeStr, DateTimeFormatter.ofPattern("HHmmss"));
            }
        } catch (DateTimeParseException e) {
            if (plugin.getConfig().getBoolean("debug", false)) {
                plugin.getLogger().warning("Could not parse server time format: " + timeStr + " - " + e.getMessage());
            }
        }
        
        return null;
    }
    
    public LocalTime getCurrentServerTime() {
        try {
            String serverTimeStr = PlaceholderAPI.setPlaceholders(null, "%servertime%");
            
            if (serverTimeStr == null || serverTimeStr.equals("%servertime%") || serverTimeStr.trim().isEmpty()) {
                if (plugin.getConfig().getBoolean("debug", false)) {
                    plugin.getLogger().info("PlaceholderAPI servertime not available, using system time");
                }
                // Fallback to system time
                return LocalTime.now();
            }
            
            LocalTime parsedTime = parseServerTime(serverTimeStr);
            if (parsedTime != null) {
                return parsedTime;
            }
            
            // If parsing failed, fallback to system time
            if (plugin.getConfig().getBoolean("debug", false)) {
                plugin.getLogger().warning("Failed to parse PlaceholderAPI time, using system time");
            }
            return LocalTime.now();
            
        } catch (Exception e) {
            plugin.getLogger().warning("Error getting server time from PlaceholderAPI: " + e.getMessage());
            return LocalTime.now();
        }
    }
    
    public void shutdown() {
        if (timeCheckTask != null && !timeCheckTask.isCancelled()) {
            timeCheckTask.cancel();
            plugin.getLogger().info("Time checker stopped");
        }
        executedThisMinute.clear();
    }
}