package com.ninja.ghasttasks.models;

import java.time.LocalTime;
import java.util.List;

/**
 * Represents a scheduled task with commands to execute
 */
public class Task {
    private final int id;
    private LocalTime time;
    private List<String> commands;
    
    public Task(int id, LocalTime time, List<String> commands) {
        this.id = id;
        this.time = time;
        this.commands = commands;
    }
    
    public int getId() {
        return id;
    }
    
    public LocalTime getTime() {
        return time;
    }
    
    public void setTime(LocalTime time) {
        this.time = time;
    }
    
    public List<String> getCommands() {
        return commands;
    }
    
    public void setCommands(List<String> commands) {
        this.commands = commands;
    }
    
    public String getFormattedTime() {
        return String.format("%02d:%02d", time.getHour(), time.getMinute());
    }
    
    @Override
    public String toString() {
        return "Task{id=" + id + ", time=" + getFormattedTime() + ", commands=" + commands.size() + "}";
    }
}