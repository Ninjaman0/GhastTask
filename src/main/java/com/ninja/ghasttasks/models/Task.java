package com.ninja.ghasttasks.models;

import java.time.LocalTime;
import java.util.List;

public class Task {
    private final int id;
    private LocalTime time;
    private List<String> commands;
    private String taskMessage;

    public Task(int id, LocalTime time, List<String> commands, String taskMessage) {
        this.id = id;
        this.time = time;
        this.commands = commands;
        this.taskMessage = taskMessage;}
    public int getId() {return id;}
    public LocalTime getTime() {return time;}
    public void setTime(LocalTime time) {this.time = time;}
    public List<String> getCommands() {return commands;}
    public void setCommands(List<String> commands) {this.commands = commands;}
    public String getTaskMessage()
    {return taskMessage;}
    public void setTaskMessage(String taskMessage) {this.taskMessage = taskMessage;}
    public String getFormattedTime() {return String.format("%02d:%02d", time.getHour(), time.getMinute());}
    @Override
    public String toString()
    {return "Task{id=" + id + ", time=" + getFormattedTime() + ", commands=" + commands.size() + ", taskMessage='" + taskMessage + "'}";}
}