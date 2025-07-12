package com.ninja.ghasttasks.managers;

import com.ninja.ghasttasks.GhastTasks;
import com.ninja.ghasttasks.models.CommandExecution;
import com.ninja.ghasttasks.models.Task;
import org.bukkit.Bukkit;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Manages task loading, execution, and configuration
 */
public class TaskManager {
    private final GhastTasks plugin;
    private final Map<Integer, Task> tasks = new ConcurrentHashMap<>();
    private final Map<Integer, AtomicBoolean> executingTasks = new ConcurrentHashMap<>();
    private final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");
    
    public TaskManager(GhastTasks plugin) {
        this.plugin = plugin;
        loadTasks();
    }
    
    public void loadTasks() {
        tasks.clear();
        executingTasks.clear();
        ConfigurationSection tasksSection = plugin.getConfig().getConfigurationSection("tasks");
        
        if (tasksSection == null) {
            plugin.getLogger().warning("No tasks section found in config.yml");
            return;
        }
        
        int loadedCount = 0;
        for (String taskIdStr : tasksSection.getKeys(false)) {
            try {
                int taskId = Integer.parseInt(taskIdStr);
                ConfigurationSection taskSection = tasksSection.getConfigurationSection(taskIdStr);
                
                if (taskSection == null) {
                    plugin.getLogger().warning("Invalid task configuration for ID: " + taskId);
                    continue;
                }
                
                String timeStr = taskSection.getString("time");
                List<String> commands = taskSection.getStringList("commands");
                
                if (timeStr == null || timeStr.trim().isEmpty()) {
                    plugin.getLogger().warning("Task " + taskId + " is missing time configuration");
                    continue;
                }
                
                if (commands.isEmpty()) {
                    plugin.getLogger().warning("Task " + taskId + " has no commands configured");
                    continue;
                }
                
                // Validate time format
                try {
                    LocalTime time = LocalTime.parse(timeStr.trim(), timeFormatter);
                    Task task = new Task(taskId, time, new ArrayList<>(commands));
                    tasks.put(taskId, task);
                    executingTasks.put(taskId, new AtomicBoolean(false));
                    loadedCount++;
                    
                    if (plugin.getConfig().getBoolean("debug", false)) {
                        plugin.getLogger().info("Loaded task " + taskId + " scheduled for " + timeStr + " with " + commands.size() + " commands");
                    }
                } catch (DateTimeParseException e) {
                    plugin.getLogger().warning("Invalid time format for task " + taskIdStr + ": " + timeStr + " (expected HH:MM)");
                }
                
            } catch (NumberFormatException e) {
                plugin.getLogger().warning("Invalid task ID (must be a number): " + taskIdStr);
            } catch (Exception e) {
                plugin.getLogger().severe("Error loading task " + taskIdStr + ": " + e.getMessage());
            }
        }
        
        plugin.getLogger().info("Loaded " + loadedCount + " tasks successfully");
    }
    
    public void reloadTasks() {
        plugin.getLogger().info("Reloading tasks...");
        loadTasks();
    }
    
    public void executeTask(int taskId) {
        Task task = tasks.get(taskId);
        if (task == null) {
            plugin.getLogger().warning("Attempted to execute non-existent task: " + taskId);
            return;
        }
        
        // Prevent concurrent execution of the same task
        AtomicBoolean isExecuting = executingTasks.get(taskId);
        if (isExecuting == null || !isExecuting.compareAndSet(false, true)) {
            if (plugin.getConfig().getBoolean("debug", false)) {
                plugin.getLogger().info("Task " + taskId + " is already executing, skipping");
            }
            return;
        }
        
        plugin.getLogger().info("Executing task " + taskId + " with " + task.getCommands().size() + " commands");
        
        // Execute commands synchronously on main thread
        Bukkit.getScheduler().runTask(plugin, () -> {
            try {
                executeTaskCommands(task);
                
                // Mark task as executed (async) - only after successful execution
                plugin.getDatabaseManager().markTaskExecuted(taskId)
                    .exceptionally(throwable -> {
                        plugin.getLogger().severe("Failed to mark task " + taskId + " as executed: " + throwable.getMessage());
                        return null;
                    });
                    
                plugin.getLogger().info("Task " + taskId + " executed successfully");
                
            } catch (Exception e) {
                plugin.getLogger().severe("Error executing task " + taskId + ": " + e.getMessage());
                if (plugin.getConfig().getBoolean("debug", false)) {
                    e.printStackTrace();
                }
            } finally {
                // Always reset the execution flag
                isExecuting.set(false);
            }
        });
    }
    
    private void executeTaskCommands(Task task) {
        ConsoleCommandSender console = Bukkit.getConsoleSender();
        int commandCount = 0;
        
        for (String command : task.getCommands()) {
            if (command == null || command.trim().isEmpty()) {
                plugin.getLogger().warning("Skipping empty command in task " + task.getId());
                continue;
            }
            
            try {
                CommandExecution executionType = CommandExecution.fromCommand(command);
                String cleanCommand = CommandExecution.stripPrefix(command).trim();
                
                if (cleanCommand.isEmpty()) {
                    plugin.getLogger().warning("Skipping empty command after prefix removal in task " + task.getId());
                    continue;
                }
                
                boolean executed = false;
                
                switch (executionType) {
                    case CONSOLE:
                        executed = Bukkit.dispatchCommand(console, cleanCommand);
                        break;
                        
                    case OP:
                        // Execute as console with OP privileges (same as console in most contexts)
                        executed = Bukkit.dispatchCommand(console, cleanCommand);
                        break;
                        
                    case PLAYER:
                        // Execute as the first online player, or console if no players online
                        Collection<? extends Player> players = Bukkit.getOnlinePlayers();
                        if (!players.isEmpty()) {
                            Player firstPlayer = players.iterator().next();
                            executed = firstPlayer.performCommand(cleanCommand);
                        } else {
                            plugin.getLogger().warning("No players online to execute player command: " + cleanCommand + " - executing as console instead");
                            executed = Bukkit.dispatchCommand(console, cleanCommand);
                        }
                        break;
                }
                
                commandCount++;
                
                if (plugin.getConfig().getBoolean("debug", false)) {
                    plugin.getLogger().info("Executed command " + commandCount + " (" + executionType + "): " + cleanCommand + " - Success: " + executed);
                }
                
                // Small delay between commands to prevent spam
                if (commandCount < task.getCommands().size()) {
                    try {
                        Thread.sleep(50); // 50ms delay between commands
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
                
            } catch (Exception e) {
                plugin.getLogger().severe("Error executing command '" + command + "' in task " + task.getId() + ": " + e.getMessage());
                if (plugin.getConfig().getBoolean("debug", false)) {
                    e.printStackTrace();
                }
            }
        }
        
        plugin.getLogger().info("Task " + task.getId() + " completed: " + commandCount + " commands executed");
    }
    
    public void executeTaskForTesting(int taskId) {
        Task task = tasks.get(taskId);
        if (task == null) {
            plugin.getLogger().warning("Cannot test non-existent task: " + taskId);
            return;
        }
        
        plugin.getLogger().info("Testing task " + taskId + " (bypassing schedule and database checks)");
        
        // Execute immediately without database check or execution flag
        Bukkit.getScheduler().runTask(plugin, () -> {
            try {
                executeTaskCommands(task);
                plugin.getLogger().info("Task " + taskId + " test completed");
            } catch (Exception e) {
                plugin.getLogger().severe("Error testing task " + taskId + ": " + e.getMessage());
            }
        });
    }
    
    public boolean shouldExecuteTask(int taskId, LocalTime currentTime) {
        Task task = tasks.get(taskId);
        if (task == null) {
            return false;
        }
        
        // Check if current time matches task time (within the same minute)
        boolean shouldExecute = task.getTime().getHour() == currentTime.getHour() && 
                               task.getTime().getMinute() == currentTime.getMinute();
        
        if (plugin.getConfig().getBoolean("debug", false) && shouldExecute) {
            plugin.getLogger().info("Task " + taskId + " should execute: current=" + currentTime.format(timeFormatter) + 
                                  ", scheduled=" + task.getFormattedTime());
        }
        
        return shouldExecute;
    }
    
    public Map<Integer, Task> getAllTasks() {
        return new HashMap<>(tasks);
    }
    
    public Task getTask(int taskId) {
        return tasks.get(taskId);
    }
    
    public boolean removeTask(int taskId) {
        if (tasks.remove(taskId) != null) {
            executingTasks.remove(taskId);
            
            // Remove from config
            plugin.getConfig().set("tasks." + taskId, null);
            plugin.saveConfig();
            
            // Remove from database
            plugin.getDatabaseManager().removeTaskRecords(taskId);
            
            plugin.getLogger().info("Removed task " + taskId);
            return true;
        }
        return false;
    }
    
    public boolean updateTaskTime(int taskId, String timeStr) {
        Task task = tasks.get(taskId);
        if (task == null) {
            return false;
        }
        
        try {
            LocalTime newTime = LocalTime.parse(timeStr.trim(), timeFormatter);
            task.setTime(newTime);
            
            // Update config
            plugin.getConfig().set("tasks." + taskId + ".time", timeStr.trim());
            plugin.saveConfig();
            
            plugin.getLogger().info("Updated task " + taskId + " time to " + timeStr);
            return true;
            
        } catch (DateTimeParseException e) {
            plugin.getLogger().warning("Invalid time format: " + timeStr + " (expected HH:MM)");
            return false;
        }
    }
    
    public boolean addCommandToTask(int taskId, String command) {
        Task task = tasks.get(taskId);
        if (task == null) {
            return false;
        }
        
        if (command == null || command.trim().isEmpty()) {
            plugin.getLogger().warning("Cannot add empty command to task " + taskId);
            return false;
        }
        
        List<String> commands = new ArrayList<>(task.getCommands());
        commands.add(command.trim());
        task.setCommands(commands);
        
        // Update config
        plugin.getConfig().set("tasks." + taskId + ".commands", commands);
        plugin.saveConfig();
        
        plugin.getLogger().info("Added command to task " + taskId + ": " + command.trim());
        return true;
    }
    
    public boolean removeCommandFromTask(int taskId, int commandIndex) {
        Task task = tasks.get(taskId);
        if (task == null || commandIndex < 1 || commandIndex > task.getCommands().size()) {
            return false;
        }
        
        List<String> commands = new ArrayList<>(task.getCommands());
        String removedCommand = commands.remove(commandIndex - 1); // Convert to 0-based index
        task.setCommands(commands);
        
        // Update config
        plugin.getConfig().set("tasks." + taskId + ".commands", commands);
        plugin.saveConfig();
        
        plugin.getLogger().info("Removed command from task " + taskId + ": " + removedCommand);
        return true;
    }
}