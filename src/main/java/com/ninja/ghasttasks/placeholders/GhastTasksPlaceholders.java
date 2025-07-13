package com.ninja.ghasttasks.placeholders;

import com.ninja.ghasttasks.GhastTasks;
import com.ninja.ghasttasks.models.Task;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.Map;

public class GhastTasksPlaceholders extends PlaceholderExpansion {
    private final GhastTasks plugin;
    public GhastTasksPlaceholders(GhastTasks plugin) {this.plugin = plugin;}
    @Override
    public @NotNull String getIdentifier() {return "ghasttasks";} // papi identifier!

    @Override
    public @NotNull String getAuthor() {return plugin.getDescription().getAuthors().toString();}
    @Override
    public @NotNull String getVersion() {return plugin.getDescription().getVersion();}
    @Override
    public boolean persist() {return true;}

    @Override
    public @Nullable String onPlaceholderRequest(Player player, @NotNull String params) {
        NextTaskInfo nextTask = getNextTask();
        if (nextTask == null) {return "N/A";}
        long secondsUntilNext = getSecondsUntilNextTask(nextTask);

        switch (params.toLowerCase()) {
            case "next_task_id": return String.valueOf(nextTask.task.getId());
            case "next_task_time": return nextTask.task.getFormattedTime();
            case "countdown_seconds": return String.valueOf(secondsUntilNext);
            case "countdown_minutes": long minutes = secondsUntilNext / 60;return String.valueOf(minutes);
            case "countdown_hours": long hours = secondsUntilNext / 3600;return String.valueOf(hours);
            case "countdown_formatted": return formatCountdown(secondsUntilNext);
            case "countdown_simple": return formatSimpleCountdown(secondsUntilNext);
            case "countdown_detailed": return formatDetailedCountdown(secondsUntilNext, nextTask.task);
            case "tasks_total": return String.valueOf(plugin.getTaskManager().getAllTasks().size());
            case "next_task_commands": return String.valueOf(nextTask.task.getCommands().size());
            case "time_until_minutes_only": long minutesOnly = (secondsUntilNext / 60) % 60;return String.valueOf(minutesOnly);
            case "time_until_hours_only": long hoursOnly = (secondsUntilNext / 3600) % 24;return String.valueOf(hoursOnly);
            case "time_until_seconds_only": long secondsOnly = secondsUntilNext % 60;return String.valueOf(secondsOnly);
            default:
                if (params.equals("next_taskmsg")) {return getNextTaskMessage(nextTask);}
                // Handle individual task message placeholders like "task_1_msg"
                if (params.startsWith("task_") && params.endsWith("_msg")) {
                    try {String taskIdStr = params.substring(5, params.length() - 4);
                        int taskId = Integer.parseInt(taskIdStr);return getTaskMessage(taskId);} catch (NumberFormatException e) {return "Invalid task ID";}}
                if (params.startsWith("task_") && params.endsWith("_countdown")) {try {String taskIdStr = params.substring(5, params.length() - 10);int taskId = Integer.parseInt(taskIdStr);return getTaskCountdownWithMessage(taskId);} catch (NumberFormatException e) {return "Invalid task ID";}}
                return null;}}

    private NextTaskInfo getNextTask() {
        Map<Integer, Task> allTasks = plugin.getTaskManager().getAllTasks();

        if (allTasks.isEmpty()) {return null;}
        LocalTime currentTime = plugin.getTimeManager().getCurrentServerTime();
        NextTaskInfo nextTask = null;
        long shortestWait = Long.MAX_VALUE;
        for (Task task : allTasks.values()) {
            long secondsUntil = calculateSecondsUntil(currentTime, task.getTime());
            if (secondsUntil < shortestWait) {shortestWait = secondsUntil;nextTask = new NextTaskInfo(task, secondsUntil);}}
        return nextTask;}
    private long calculateSecondsUntil(LocalTime currentTime, LocalTime targetTime) {
        long secondsUntil = ChronoUnit.SECONDS.between(currentTime, targetTime);
        if (secondsUntil <= 0) {secondsUntil += 24 * 60 * 60;}return secondsUntil;}

    private long getSecondsUntilNextTask(NextTaskInfo nextTask) {
        if (nextTask == null) {return 0;}LocalTime currentTime = plugin.getTimeManager().getCurrentServerTime();return calculateSecondsUntil(currentTime, nextTask.task.getTime());}

    private String formatCountdown(long totalSeconds) {
        if (totalSeconds <= 0)
        {return "00:00:00";}long hours = totalSeconds / 3600;long minutes = (totalSeconds % 3600) / 60;long seconds = totalSeconds % 60;return String.format("%02d:%02d:%02d", hours, minutes, seconds);}
    private String formatSimpleCountdown(long totalSeconds) {
        if (totalSeconds <= 0)
        {return "Now";}long hours = totalSeconds / 3600;long minutes = (totalSeconds % 3600) / 60;long seconds = totalSeconds % 60;
        if (hours > 0)
        {return String.format("%dh %dm", hours, minutes);}
        else if (minutes > 0) {return String.format("%dm %ds", minutes, seconds);}
        else {return String.format("%ds", seconds);}}


    private String formatDetailedCountdown(long totalSeconds, Task task) {
        String timeLeft = formatSimpleCountdown(totalSeconds);return String.format("Task %d in %s", task.getId(), timeLeft);}

    private String getNextTaskMessage(NextTaskInfo nextTask) {
        if (nextTask == null || nextTask.task.getTaskMessage() == null || nextTask.task.getTaskMessage().trim().isEmpty()) {
            return "";}
        long secondsUntil = getSecondsUntilNextTask(nextTask);String countdown = formatCountdown(secondsUntil);return nextTask.task.getTaskMessage() + " " + countdown;}

    private String getTaskMessage(int taskId)
    {Task task = plugin.getTaskManager().getTask(taskId);if (task == null) {return "Task not found";}
        String taskMessage = task.getTaskMessage();
        if (taskMessage == null || taskMessage.trim().isEmpty())
        {return "No message set";}
        return taskMessage;}

    private String getTaskCountdownWithMessage(int taskId)
    {Task task = plugin.getTaskManager().getTask(taskId);
        if (task == null) {return "Task not found";}
        LocalTime currentTime = plugin.getTimeManager().getCurrentServerTime();
        long secondsUntil = calculateSecondsUntil(currentTime, task.getTime());
        String countdown = formatCountdown(secondsUntil);
        String taskMessage = task.getTaskMessage();
        if (taskMessage == null || taskMessage.trim().isEmpty()) {return countdown;}
        return taskMessage + " " + countdown;}

    private static class NextTaskInfo {
        final Task task;
        final long secondsUntil;
        NextTaskInfo(Task task, long secondsUntil) {this.task = task;this.secondsUntil = secondsUntil;}}
}