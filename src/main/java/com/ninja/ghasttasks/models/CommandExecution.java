package com.ninja.ghasttasks.models;

public enum CommandExecution {
    CONSOLE("[console]"),
    OP("[op]"),
    PLAYER("[player]");
    private final String prefix;
    CommandExecution(String prefix) {
        this.prefix = prefix;
    }
    public String getPrefix() {
        return prefix;
    }
    public static CommandExecution fromCommand(String command) {if (command == null || command.trim().isEmpty()) {return CONSOLE;}
        String lowerCommand = command.toLowerCase().trim();
        if (lowerCommand.startsWith("[console]")) {return CONSOLE;
        } else if (lowerCommand.startsWith("[op]")) {return OP;
        } else if (lowerCommand.startsWith("[player]")) {return PLAYER;}
        return CONSOLE; }
    public static String stripPrefix(String command) {
        if (command == null || command.trim().isEmpty()) {return "";}
        CommandExecution type = fromCommand(command);
        String trimmedCommand = command.trim();
        if (type != CONSOLE || trimmedCommand.toLowerCase().startsWith("[console]"))
        {String pattern = "(?i)^\\[" + type.name() + "\\]\\s*";return trimmedCommand.replaceFirst(pattern, "");}
        return trimmedCommand;}
}