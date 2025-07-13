# GhastTasks

A powerful and flexible Minecraft plugin for scheduling and executing commands at specific times with PlaceholderAPI integration for countdown displays.

## 🌟 Features

### ⏰ **Scheduled Task Execution**
- Execute commands at specific times using 24-hour format (HH:MM)
- Support for multiple commands per task
- Different execution contexts: Console, OP, and Player commands
- Automatic daily execution tracking to prevent duplicate runs

### 🎯 **Command Execution Types**
- `[console]` - Execute as console (default)
- `[op]` - Execute with operator privileges
- `[player]` - Execute as the first online player

### 📊 **PlaceholderAPI Integration**
- Real-time countdown displays for next scheduled tasks
- Custom task messages with countdown integration
- Multiple countdown formats (seconds, minutes, hours, formatted)
- Individual task placeholders for specific tasks

### 🛠️ **Management Commands**
- In-game task management (add, remove, edit commands and times)
- Live configuration reloading
- Task testing without affecting schedules
- Comprehensive help system with tab completion

### 💾 **Database Integration**
- SQLite database for execution tracking
- Prevents duplicate task execution on the same day
- Automatic database optimization and maintenance
- Thread-safe async operations

### 🔧 **Advanced Configuration**
- YAML-based configuration with validation
- Debug mode for detailed logging
- Custom task messages for placeholder integration
- Hot-reloading without server restart

## 📋 Requirements

- **Minecraft Server**: 1.21+ (Bukkit/Paper/Spigot)
- **Java**: 17 or higher
- **Dependencies**: PlaceholderAPI (required)

## 🚀 Installation

1. **Download** the latest release from the releases page
2. **Place** the JAR file in your server's `plugins` folder
3. **Install** [PlaceholderAPI](https://www.spigotmc.org/resources/placeholderapi.6245/) if not already installed
4. **Restart** your server
5. **Configure** tasks in `plugins/GhastTasks/config.yml`
6. **Reload** the plugin with `/ghasttasks reload`

## ⚙️ Configuration

### Basic Task Configuration

```yaml
# GhastTasks Configuration
tasks:
  1:
    time: "12:00"                    # 24-hour format (HH:MM)
    task-msg: "Daily Reward is in"   # Custom message for placeholders
    commands:
      - "[console] broadcast §6Daily reward time!"
      - "give @a diamond 1"
      - "[console] say §aEveryone has received a diamond!"
  
  2:
    time: "18:00"
    task-msg: "Server Restart is in"
    commands:
      - "[console] broadcast §cServer restart in 5 minutes!"
      - "[console] title @a times 20 60 20"
      - "[console] title @a title {\"text\":\"Server Restart\",\"color\":\"red\"}"
  
  3:
    time: "00:00"
    task-msg: "New Day Event is in"
    commands:
      - "[console] broadcast §9New day has begun!"
      - "[console] weather clear"
      - "[console] time set day"

# Database settings
database:
  file: "tasks.db"

# Debug mode for detailed logging
debug: false
```

### Command Execution Types

| Prefix      | Description                    | Example                              |
|-------------|--------------------------------|--------------------------------------|
| `[console]` | Execute as console (default)   | `[console] give @a diamond 1`        |
| `[op]`      | Execute with OP privileges     | `[op] gamemode creative @a`          |
| `[player]`  | Execute as first online player | `[player] msg @a Hello from player!` |
| *(none)*    | Execute as console (default)   | `broadcast Hello World!`             |

## 🎮 Commands

### Main Command: `/ghasttasks` (Aliases: `/gtasks`)

| Command                    | Permission         | Description                   |
|----------------------------|--------------------|-------------------------------|
| `/ghasttasks help`         | `ghasttasks.use`   | Show command help             |
| `/ghasttasks list`         | `ghasttasks.view`  | List all configured tasks     |
| `/ghasttasks reload`       | `ghasttasks.admin` | Reload configuration          |
| `/ghasttasks test <id>`    | `ghasttasks.admin` | Test task execution           |
| `/ghasttasks remove <id>`  | `ghasttasks.admin` | Remove a task                 |
| `/ghasttasks testtime`     | `ghasttasks.admin` | Test system time and database |
| `/ghasttasks placeholders` | `ghasttasks.view`  | Show available placeholders   |

### Task Editing Commands

| Command                                         | Description                | Example                                                |
|-------------------------------------------------|----------------------------|--------------------------------------------------------|
| `/ghasttasks edit <id> time <HH:MM>`            | Change task execution time | `/ghasttasks edit 1 time 14:30`                        |
| `/ghasttasks edit <id> commands add <command>`  | Add command to task        | `/ghasttasks edit 1 commands add give @a gold_ingot 5` |
| `/ghasttasks edit <id> commands remove <index>` | Remove command by index    | `/ghasttasks edit 1 commands remove 2`                 |
| `/ghasttasks edit <id> message <text>`          | Set custom task message    | `/ghasttasks edit 1 message Daily Event is in`         |
| `/ghasttasks edit <id> message`                 | Clear task message         | `/ghasttasks edit 1 message`                           |

## 🔑 Permissions

| Permission         | Default | Description                                     |
|--------------------|---------|-------------------------------------------------|
| `ghasttasks.use`   | OP      | Base permission for using commands              |
| `ghasttasks.view`  | OP      | Permission to view tasks and placeholders       |
| `ghasttasks.admin` | OP      | Administrative permissions (edit, reload, test) |

## 📊 PlaceholderAPI Integration

### Basic Information Placeholders

| Placeholder                       | Description                      | Example Output |
|-----------------------------------|----------------------------------|----------------|
| `%ghasttasks_next_task_id%`       | ID of next scheduled task        | `1`            |
| `%ghasttasks_next_task_time%`     | Time of next scheduled task      | `12:00`        |
| `%ghasttasks_tasks_total%`        | Total number of configured tasks | `3`            |
| `%ghasttasks_next_task_commands%` | Number of commands in next task  | `5`            |

### Countdown Placeholders

| Placeholder                      | Description                   | Example Output |
|----------------------------------|-------------------------------|----------------|
| `%ghasttasks_countdown_seconds%` | Total seconds until next task | `7825`         |
| `%ghasttasks_countdown_minutes%` | Total minutes until next task | `130`          |
| `%ghasttasks_countdown_hours%`   | Total hours until next task   | `2`            |

### Formatted Countdown Placeholders

| Placeholder                        | Description           | Example Output     |
|------------------------------------|-----------------------|--------------------|
| `%ghasttasks_countdown_formatted%` | HH:MM:SS format       | `02:10:25`         |
| `%ghasttasks_countdown_simple%`    | Human readable format | `2h 10m`           |
| `%ghasttasks_countdown_detailed%`  | With task information | `Task 1 in 2h 10m` |

### Individual Time Components

| Placeholder                            | Description                   | Example Output |
|----------------------------------------|-------------------------------|----------------|
| `%ghasttasks_time_until_seconds_only%` | Only seconds component (0-59) | `25`           |
| `%ghasttasks_time_until_minutes_only%` | Only minutes component (0-59) | `10`           |
| `%ghasttasks_time_until_hours_only%`   | Only hours component (0-23)   | `2`            |

### Custom Task Message Placeholders

| Placeholder                        | Description                       | Example Output                |
|------------------------------------|-----------------------------------|-------------------------------|
| `%ghasttasks_next_taskmsg%`        | Next task message with countdown  | `Daily Reward is in 02:10:25` |
| `%ghasttasks_task_<id>_msg%`       | Specific task message only        | `Daily Reward is in`          |
| `%ghasttasks_task_<id>_countdown%` | Specific task message + countdown | `Daily Reward is in 02:10:25` |

### Usage Examples

```yaml
# In your scoreboard, chat, or other plugins:
- "Next Event: %ghasttasks_next_taskmsg%"
- "Time Remaining: %ghasttasks_countdown_formatted%"
- "Daily Reward: %ghasttasks_task_1_countdown%"
- "Events Today: %ghasttasks_tasks_total%"
```

## 🐛 Troubleshooting

### Common Issues

**Plugin doesn't load:**
- Ensure PlaceholderAPI is installed and enabled
- Check that you're using Java 17 or higher
- Verify the plugin JAR is in the correct plugins folder

**Tasks not executing:**
- Check server console for error messages
- Verify task time format is HH:MM (24-hour)
- Ensure debug mode is enabled for detailed logging
- Use `/ghasttasks testtime` to verify time synchronization

**Placeholders not working:**
- Confirm PlaceholderAPI is installed and running
- Check that the placeholder syntax is correct
- Verify the plugin registered successfully (check console on startup)

**Database errors:**
- Ensure the plugin has write permissions to the plugins folder
- Check available disk space
- Review console logs for specific SQLite errors

### Debug Mode

Enable debug mode in `config.yml` for detailed logging:

```yaml
debug: true
```

This will provide extensive logging information including:
- Task loading details
- Command execution results
- Database operations
- Time checking processes
- Placeholder resolution

## 📞 Support

- **Wiki**: Check the [Wiki](https://github.com/Ninjaman0/GhastTask/wiki) for detailed guides
- **Discord**: Join our [Discord Server](https://discord.gg/ghastlegion) for community support

**Made with ❤️ by Ninja0_0 aka NotNinja0_0**