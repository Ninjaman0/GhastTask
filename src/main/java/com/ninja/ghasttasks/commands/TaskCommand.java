package com.ninja.ghasttasks.commands;

import com.ninja.ghasttasks.GhastTasks;
import com.ninja.ghasttasks.models.Task;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


public class TaskCommand implements CommandExecutor, TabCompleter {
    private final GhastTasks plugin;
    public TaskCommand(GhastTasks plugin) {
        this.plugin = plugin;
    }
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("ghasttasks.use")) {
            sender.sendMessage(Component.text("You don't have permission to use this command.").color(NamedTextColor.RED));
            return true;}
        if (args.length == 0) {sendHelp(sender);
            return true;}
        String subCommand = args[0].toLowerCase();
        switch (subCommand) {
            case "reload": return handleReload(sender);
            case "list": return handleList(sender);
            case "edit": return handleEdit(sender, args);
            case "test": return handleTest(sender, args);
            case "remove": return handleRemove(sender, args);
            case "testtime": return handleTestTime(sender);
            case "placeholders":return handlePlaceholders(sender);
            case "help": sendHelp(sender);
                return true;
            default:
                sender.sendMessage(Component.text("Unknown subcommand: " + subCommand).color(NamedTextColor.RED));
                sendHelp(sender);
                return true;}}
    private boolean handleReload(CommandSender sender) {
        if (!sender.hasPermission("ghasttasks.admin")) {
            sender.sendMessage(Component.text("You don't have permission to reload the configuration.").color(NamedTextColor.RED));
            return true;}
        try {
            sender.sendMessage(Component.text("Reloading GhastTasks configuration...").color(NamedTextColor.YELLOW));
            plugin.reloadPlugin();
            sender.sendMessage(Component.text("GhastTasks config reloaded successfully!").color(NamedTextColor.GREEN));
        } catch (Exception e) {
            sender.sendMessage(Component.text("Failed to reload config: " + e.getMessage()).color(NamedTextColor.RED));
            plugin.getLogger().severe("Error during reload: " + e.getMessage());}
        return true;}
    private boolean handleList(CommandSender sender) {
        if (!sender.hasPermission("ghasttasks.view")) {
            sender.sendMessage(Component.text("You don't have permission to view tasks.").color(NamedTextColor.RED));
            return true;}
        Map<Integer, Task> tasks = plugin.getTaskManager().getAllTasks();
        if (tasks.isEmpty()) {
            sender.sendMessage(Component.text("No tasks configured.").color(NamedTextColor.YELLOW));
            return true;}
        sender.sendMessage(Component.text("=== GhastTasks List (" + tasks.size() + " tasks) ===").color(NamedTextColor.GOLD));
        List<Task> sortedTasks = tasks.values().stream().sorted((t1, t2) -> Integer.compare(t1.getId(), t2.getId())).collect(Collectors.toList());
        for (Task task : sortedTasks) {
            sender.sendMessage(Component.text("Task ID: " + task.getId()).color(NamedTextColor.AQUA));
            sender.sendMessage(Component.text("Time: " + task.getFormattedTime()).color(NamedTextColor.WHITE));
            String taskMessage = task.getTaskMessage();
            if (taskMessage != null && !taskMessage.trim().isEmpty()) {
                sender.sendMessage(Component.text("Message: " + taskMessage).color(NamedTextColor.YELLOW));}
            sender.sendMessage(Component.text("Commands (" + task.getCommands().size() + "):").color(NamedTextColor.WHITE));
            for (int i = 0; i < task.getCommands().size(); i++) {
                String command = task.getCommands().get(i);
                if (command.length() > 80) {command = command.substring(0, 77) + "...";}
                sender.sendMessage(Component.text("  " + (i + 1) + ". " + command).color(NamedTextColor.GRAY));}
            sender.sendMessage(Component.text(""));}
        return true;}
    private boolean handleEdit(CommandSender sender, String[] args) {
        if (!sender.hasPermission("ghasttasks.admin")) {
            sender.sendMessage(Component.text("You don't have permission to edit tasks.").color(NamedTextColor.RED));
            return true;}
        if (args.length < 3) {
            sender.sendMessage(Component.text("Usage: /ghasttasks edit <task_id> <time|commands|message> <value>").color(NamedTextColor.RED));
            return true;}
        try {
            int taskId = Integer.parseInt(args[1]);
            String editType = args[2].toLowerCase();
            Task task = plugin.getTaskManager().getTask(taskId);
            if (task == null) {
                sender.sendMessage(Component.text("Task ID " + taskId + " not found.").color(NamedTextColor.RED));
                return true;}
            switch (editType) {
                case "time":
                    if (args.length < 4) {
                        sender.sendMessage(Component.text("Usage: /ghasttasks edit <task_id> time <HH:MM>").color(NamedTextColor.RED));
                        return true;}
                    String newTime = args[3];
                    if (plugin.getTaskManager().updateTaskTime(taskId, newTime)) {sender.sendMessage(Component.text("Task " + taskId + " time updated to " + newTime).color(NamedTextColor.GREEN));
                    } else {
                        sender.sendMessage(Component.text("Invalid time format. Use HH:MM format (24-hour).").color(NamedTextColor.RED));}
                    break;
                case "commands":
                    if (args.length < 5) {
                        sender.sendMessage(Component.text("Usage: /ghasttasks edit <task_id> commands <add|remove> <command|index>").color(NamedTextColor.RED));
                        return true;}
                    String commandAction = args[3].toLowerCase();
                    if ("add".equals(commandAction)) {
                        String newCommand = String.join(" ", Arrays.copyOfRange(args, 4, args.length));
                        if (newCommand.trim().isEmpty()) {
                            sender.sendMessage(Component.text("Cannot add empty command.").color(NamedTextColor.RED));
                            return true;}
                        if (plugin.getTaskManager().addCommandToTask(taskId, newCommand)) {
                            sender.sendMessage(Component.text("Command added to task " + taskId + ": " + newCommand).color(NamedTextColor.GREEN));
                        } else {
                            sender.sendMessage(Component.text("Failed to add command to task " + taskId).color(NamedTextColor.RED));}
                    } else if ("remove".equals(commandAction)) {
                        try {
                            int commandIndex = Integer.parseInt(args[4]);
                            if (commandIndex < 1) {
                                sender.sendMessage(Component.text("Command index must be 1 or greater.").color(NamedTextColor.RED));
                                return true;}
                            if (plugin.getTaskManager().removeCommandFromTask(taskId, commandIndex)) {
                                sender.sendMessage(Component.text("Command " + commandIndex + " removed from task " + taskId).color(NamedTextColor.GREEN));
                            } else {
                                sender.sendMessage(Component.text("Invalid command index or task not found. Use /ghasttasks list to see command numbers.").color(NamedTextColor.RED));}
                        } catch (NumberFormatException e) {
                            sender.sendMessage(Component.text("Invalid command index. Must be a number.").color(NamedTextColor.RED));}
                    } else {
                        sender.sendMessage(Component.text("Usage: /ghasttasks edit <task_id> commands <add|remove> <command|index>").color(NamedTextColor.RED));}
                    break;
                case "message":
                case "msg":
                    String newMessage = "";
                    if (args.length > 3) {newMessage = String.join(" ", Arrays.copyOfRange(args, 3, args.length));}
                    if (plugin.getTaskManager().updateTaskMessage(taskId, newMessage)) {
                        if (newMessage.trim().isEmpty()) {
                            sender.sendMessage(Component.text("Task " + taskId + " message cleared.").color(NamedTextColor.GREEN));
                        } else {
                            sender.sendMessage(Component.text("Task " + taskId + " message updated to: " + newMessage).color(NamedTextColor.GREEN));}
                    } else {
                        sender.sendMessage(Component.text("Failed to update task " + taskId + " message.").color(NamedTextColor.RED));
                    }break;
                default:
                    sender.sendMessage(Component.text("Invalid edit type. Use 'time', 'commands', or 'message'.").color(NamedTextColor.RED));
                    break;}
        } catch (NumberFormatException e) {
            sender.sendMessage(Component.text("Invalid task ID. Must be a number.").color(NamedTextColor.RED));}
        return true;}
    private boolean handleTest(CommandSender sender, String[] args) {
        if (!sender.hasPermission("ghasttasks.admin")) {
            sender.sendMessage(Component.text("You don't have permission to test tasks.").color(NamedTextColor.RED));
            return true;}
        if (args.length < 2) {
            sender.sendMessage(Component.text("Usage: /ghasttasks test <task_id>").color(NamedTextColor.RED));return true;}
        try {
            int taskId = Integer.parseInt(args[1]);
            Task task = plugin.getTaskManager().getTask(taskId);
            if (task == null) {
                sender.sendMessage(Component.text("Task ID " + taskId + " not found.").color(NamedTextColor.RED));
                return true;}
            sender.sendMessage(Component.text("Testing task " + taskId + " (" + task.getCommands().size() + " commands)...").color(NamedTextColor.YELLOW));
            plugin.getTaskManager().executeTaskForTesting(taskId);
            sender.sendMessage(Component.text("Task " + taskId + " executed for testing. Check console for execution details.").color(NamedTextColor.GREEN));
        } catch (NumberFormatException e) {
            sender.sendMessage(Component.text("Invalid task ID. Must be a number.").color(NamedTextColor.RED));}
        return true;}
    private boolean handleRemove(CommandSender sender, String[] args) {
        if (!sender.hasPermission("ghasttasks.admin")) {
            sender.sendMessage(Component.text("You don't have permission to remove tasks.").color(NamedTextColor.RED));
            return true;}
        if (args.length < 2) {
            sender.sendMessage(Component.text("Usage: /ghasttasks remove <task_id>").color(NamedTextColor.RED));
            return true;}
        try {
            int taskId = Integer.parseInt(args[1]);
            Task task = plugin.getTaskManager().getTask(taskId);
            if (task == null) {
                sender.sendMessage(Component.text("Task ID " + taskId + " not found.").color(NamedTextColor.RED));
                return true;}
            if (plugin.getTaskManager().removeTask(taskId)) {
                sender.sendMessage(Component.text("Task " + taskId + " removed successfully.").color(NamedTextColor.GREEN));
            } else {
                sender.sendMessage(Component.text("Failed to remove task " + taskId + ".").color(NamedTextColor.RED));}
        } catch (NumberFormatException e) {
            sender.sendMessage(Component.text("Invalid task ID. Must be a number.").color(NamedTextColor.RED));}
        return true;}
    private boolean handleTestTime(CommandSender sender) {
        if (!sender.hasPermission("ghasttasks.admin")) {
            sender.sendMessage(Component.text("You don't have permission to test time placeholders.").color(NamedTextColor.RED));
            return true;}
        sender.sendMessage(Component.text("Testing system time and database...").color(NamedTextColor.YELLOW));
        plugin.getTimeManager().testTime();
        sender.sendMessage(Component.text("Time and database test completed. Check console for details.").color(NamedTextColor.GREEN));
        return true;}
    private boolean handlePlaceholders(CommandSender sender) {
        if (!sender.hasPermission("ghasttasks.view")) {
            sender.sendMessage(Component.text("You don't have permission to view placeholder information.").color(NamedTextColor.RED));
            return true;}
        sender.sendMessage(Component.text("=== GhastTasks PlaceholderAPI Integration ===").color(NamedTextColor.GOLD));
        sender.sendMessage(Component.text("Basic Placeholders:").color(NamedTextColor.AQUA));
        sender.sendMessage(Component.text("  %ghasttasks_next_task_id% - ID of the next scheduled task").color(NamedTextColor.WHITE));
        sender.sendMessage(Component.text("  %ghasttasks_next_task_time% - Time of the next scheduled task").color(NamedTextColor.WHITE));
        sender.sendMessage(Component.text("  %ghasttasks_tasks_total% - Total number of configured tasks").color(NamedTextColor.WHITE));
        sender.sendMessage(Component.text("  %ghasttasks_next_task_commands% - Number of commands in next task").color(NamedTextColor.WHITE));
        sender.sendMessage(Component.text("Countdown Placeholders:").color(NamedTextColor.AQUA));
        sender.sendMessage(Component.text("  %ghasttasks_countdown_seconds% - Total seconds until next task").color(NamedTextColor.WHITE));
        sender.sendMessage(Component.text("  %ghasttasks_countdown_minutes% - Total minutes until next task").color(NamedTextColor.WHITE));
        sender.sendMessage(Component.text("  %ghasttasks_countdown_hours% - Total hours until next task").color(NamedTextColor.WHITE));
        sender.sendMessage(Component.text("Formatted Countdown:").color(NamedTextColor.AQUA));
        sender.sendMessage(Component.text("  %ghasttasks_countdown_formatted% - HH:MM:SS format").color(NamedTextColor.WHITE));
        sender.sendMessage(Component.text("  %ghasttasks_countdown_simple% - Human readable (1h 30m)").color(NamedTextColor.WHITE));
        sender.sendMessage(Component.text("  %ghasttasks_countdown_detailed% - With task info").color(NamedTextColor.WHITE));
        sender.sendMessage(Component.text("Individual Time Components:").color(NamedTextColor.AQUA));
        sender.sendMessage(Component.text("  %ghasttasks_time_until_seconds_only% - Only seconds component").color(NamedTextColor.WHITE));
        sender.sendMessage(Component.text("  %ghasttasks_time_until_minutes_only% - Only minutes component").color(NamedTextColor.WHITE));
        sender.sendMessage(Component.text("  %ghasttasks_time_until_hours_only% - Only hours component").color(NamedTextColor.WHITE));
        sender.sendMessage(Component.text("Custom Task Messages:").color(NamedTextColor.AQUA));
        sender.sendMessage(Component.text("  %ghasttasks_next_taskmsg% - Next task message with countdown").color(NamedTextColor.WHITE));
        sender.sendMessage(Component.text("  %ghasttasks_task_<id>_msg% - Specific task message only").color(NamedTextColor.WHITE));
        sender.sendMessage(Component.text("  %ghasttasks_task_<id>_countdown% - Specific task message + countdown").color(NamedTextColor.WHITE));
        sender.sendMessage(Component.text("Examples:").color(NamedTextColor.AQUA));
        sender.sendMessage(Component.text("  %ghasttasks_task_1_msg% - Shows task 1's message").color(NamedTextColor.GRAY));
        sender.sendMessage(Component.text("  %ghasttasks_task_3_countdown% - Shows 'Levels Event is in 02:10:04'").color(NamedTextColor.GRAY));
        return true;}
    private void sendHelp(CommandSender sender) {
        sender.sendMessage(Component.text("=== GhastTasks Commands ===").color(NamedTextColor.GOLD));
        sender.sendMessage(Component.text("/ghasttasks reload - Reload configuration").color(NamedTextColor.WHITE));
        sender.sendMessage(Component.text("/ghasttasks list - List all tasks").color(NamedTextColor.WHITE));
        sender.sendMessage(Component.text("/ghasttasks edit <id> time <HH:MM> - Edit task time").color(NamedTextColor.WHITE));
        sender.sendMessage(Component.text("/ghasttasks edit <id> commands add <command> - Add command").color(NamedTextColor.WHITE));
        sender.sendMessage(Component.text("/ghasttasks edit <id> commands remove <index> - Remove command").color(NamedTextColor.WHITE));
        sender.sendMessage(Component.text("/ghasttasks edit <id> message <text> - Set task message").color(NamedTextColor.WHITE));
        sender.sendMessage(Component.text("/ghasttasks test <id> - Test task execution").color(NamedTextColor.WHITE));
        sender.sendMessage(Component.text("/ghasttasks remove <id> - Remove task").color(NamedTextColor.WHITE));
        sender.sendMessage(Component.text("/ghasttasks testtime - Test system time and database").color(NamedTextColor.WHITE));
        sender.sendMessage(Component.text("/ghasttasks placeholders - Show available placeholders").color(NamedTextColor.WHITE));
        sender.sendMessage(Component.text("/ghasttasks help - Show this help").color(NamedTextColor.WHITE));}
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {List<String> completions = new ArrayList<>();
        if (!sender.hasPermission("ghasttasks.use")) {
            return completions;}
        if (args.length == 1) {
            List<String> subCommands = Arrays.asList("reload", "list", "edit", "test", "remove", "testtime", "placeholders", "help");
            String input = args[0].toLowerCase();
            for (String subCommand : subCommands) {
                if (subCommand.startsWith(input)) {completions.add(subCommand);}}
        } else if (args.length == 2 && !args[0].equalsIgnoreCase("reload") &&
                !args[0].equalsIgnoreCase("list") && !args[0].equalsIgnoreCase("help") &&
                !args[0].equalsIgnoreCase("testtime") && !args[0].equalsIgnoreCase("placeholders")) {
            String input = args[1];
            for (Integer taskId : plugin.getTaskManager().getAllTasks().keySet()) {
                String taskIdStr = taskId.toString();
                if (taskIdStr.startsWith(input)) {completions.add(taskIdStr);}}
        } else if (args.length == 3 && args[0].equalsIgnoreCase("edit")) {
            String input = args[2].toLowerCase();
            if ("time".startsWith(input)) {completions.add("time");}
            if ("commands".startsWith(input)) {completions.add("commands");}
            if ("message".startsWith(input)) {completions.add("message");}
            if ("msg".startsWith(input)) {completions.add("msg");}
        } else if (args.length == 4 && args[0].equalsIgnoreCase("edit") && args[2].equalsIgnoreCase("commands")) {
            String input = args[3].toLowerCase();
            if ("add".startsWith(input)) {completions.add("add");}
            if ("remove".startsWith(input)) {completions.add("remove");
            }}return completions;}}