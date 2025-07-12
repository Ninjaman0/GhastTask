# GhastTasks - Minecraft Plugin

A comprehensive Minecraft Paper plugin for scheduling and executing commands at specific times using PlaceholderAPI's server time. Built with robust error handling, spam prevention, and thread-safe operations.

## Features

- **Scheduled Command Execution**: Execute commands at specific times daily using server time from PlaceholderAPI
- **Multiple Execution Types**: Support for console, operator, and player command execution
- **SQLite Database**: Tracks task execution to ensure each task runs exactly once per day
- **In-Game Management**: Full command suite for managing tasks without server restart
- **Configuration-Based**: Easy task configuration via YAML files
- **Spam Prevention**: Built-in safeguards to prevent command spam and duplicate executions
- **Thread-Safe Operations**: Robust concurrency handling for server stability
- **Comprehensive Error Handling**: Detailed error handling and logging for troubleshooting

## Requirements

- **Minecraft Paper 1.21.4** or higher
- **PlaceholderAPI** (required dependency)
- **Java 21** or higher

## Installation

1. Download the GhastTasks plugin JAR file
2. Place it in your server's `plugins` folder
3. Ensure PlaceholderAPI is installed and running
4. Start/restart your server
5. Configure tasks in `plugins/GhastTasks/config.yml`

## Configuration

### Task Configuration Format

```yaml
tasks:
  <task_id>:
    time: "HH:MM"  # 24-hour format
    commands:
      - "[console] command"  # Run as console (default)
      - "[op] command"       # Run as operator
      - "[player] command"   # Run as player
      - "command"            # Run as console (default)
```

### Example Configuration

```yaml
tasks:
  1:
    time: "12:00"
    commands:
      - "[console] broadcast §6Daily reward time!"
      - "give @a diamond 1"
      - "[console] say §aEveryone has received a diamond!"
  
  2:
    time: "18:00"
    commands:
      - "[console] broadcast §cServer restart in 5 minutes!"
      - "[console] title @a times 20 60 20"
      - "[console] title @a title {\"text\":\"Server Restart\",\"color\":\"red\"}"
      - "[console] title @a subtitle {\"text\":\"5 minutes remaining\",\"color\":\"yellow\"}"

  3:
    time: "00:00"
    commands:
      - "[console] broadcast §9New day has begun!"
      - "[console] weather clear"
      - "[console] time set day"

# Database settings (SQLite)
database:
  file: "tasks.db"

# Debug mode - set to true for detailed logging
debug: false
```

## Commands

All commands use the base `/ghasttasks` (alias: `/gtasks`)

### Command List

| Command | Permission | Description |
|---------|------------|-------------|
| `/ghasttasks help` | `ghasttasks.use` | Show command help |
| `/ghasttasks reload` | `ghasttasks.admin` | Reload configuration |
| `/ghasttasks list` | `ghasttasks.view` | List all configured tasks |
| `/ghasttasks edit <id> time <HH:MM>` | `ghasttasks.admin` | Change task execution time |
| `/ghasttasks edit <id> commands add <command>` | `ghasttasks.admin` | Add command to task |
| `/ghasttasks edit <id> commands remove <index>` | `ghasttasks.admin` | Remove command from task |
| `/ghasttasks test <id>` | `ghasttasks.admin` | Execute task immediately for testing |
| `/ghasttasks remove <id>` | `ghasttasks.admin` | Delete task completely |

### Command Examples

```bash
# Show help
/gtasks help

# Reload configuration
/gtasks reload

# List all tasks
/gtasks list

# Change task 1's time to 14:30
/gtasks edit 1 time 14:30

# Add a command to task 1
/gtasks edit 1 commands add [console] say Hello World!

# Remove the 2nd command from task 1
/gtasks edit 1 commands remove 2

# Test task 1 immediately
/gtasks test 1

# Remove task 1 completely
/gtasks remove 1
```

## Permissions

| Permission | Default | Description |
|------------|---------|-------------|
| `ghasttasks.use` | op | Base permission for using commands |
| `ghasttasks.admin` | op | Administrative permissions (edit, test, remove, reload) |
| `ghasttasks.view` | op | Permission to view tasks (list command) |

## Command Execution Types

### Console Commands (`[console]` or no prefix)
```yaml
- "[console] broadcast Hello World!"
- "give @a diamond"  # Also executes as console
```

### Operator Commands (`[op]`)
```yaml
- "[op] tp player 0 100 0"
```

### Player Commands (`[player]`)
```yaml
- "[player] spawn"
```

## Database

GhastTasks uses SQLite to track task execution:
- **File**: `plugins/GhastTasks/tasks.db`
- **Table**: `executed_tasks`
- **Purpose**: Ensures each task executes only once per day
- **Schema**:
  ```sql
  CREATE TABLE executed_tasks (
      task_id INTEGER NOT NULL,
      execution_date DATE NOT NULL,
      execution_timestamp DATETIME DEFAULT CURRENT_TIMESTAMP,
      PRIMARY KEY (task_id, execution_date)
  );
  ```

## Technical Details

### Time Management
- Uses PlaceholderAPI's `%servertime%` placeholder for server time
- Falls back to system time if PlaceholderAPI is unavailable
- Checks every 10 seconds to balance accuracy with performance
- Prevents duplicate executions within the same minute
- Supports multiple time formats: HH:mm, HHmm, HH:mm:ss, HHmmss

### Spam Prevention & Performance
- **Execution Locks**: Prevents concurrent execution of the same task
- **Command Delays**: 50ms delay between commands to prevent spam
- **Time Change Detection**: Only processes when time actually changes
- **Database Optimization**: Indexed queries and connection pooling
- **Thread Safety**: All operations are thread-safe with proper locking

### Database Operations
- **Asynchronous Operations**: Database operations run asynchronously to prevent server lag
- **Connection Management**: Automatic connection handling with proper cleanup
- **WAL Mode**: Write-Ahead Logging for better performance
- **Foreign Key Constraints**: Data integrity enforcement
- **Thread-Safe**: ReentrantLock prevents race conditions

### Error Handling
- **Comprehensive Logging**: Detailed logging for troubleshooting
- **Graceful Degradation**: Safe handling of invalid configurations
- **Input Validation**: Thorough validation of all user inputs
- **Exception Safety**: Try-catch blocks around all critical operations
- **User-Friendly Messages**: Clear error messages for administrators

## Troubleshooting

### Common Issues

1. **Tasks not executing**
   - Verify PlaceholderAPI is installed and working: `/papi version`
   - Check server console for error messages
   - Enable debug mode in config for detailed logging
   - Ensure task times are in correct HH:MM format

2. **Permission errors**
   - Ensure players have appropriate permissions
   - Check permission plugin configuration
   - Verify permission inheritance is working correctly

3. **Database errors**
   - Verify plugin has write permissions in its data folder
   - Check for SQLite driver conflicts with other plugins
   - Look for file system space issues

4. **Command execution issues**
   - Verify command syntax is correct
   - Check if commands work when executed manually
   - Ensure target players are online for `[player]` commands

### Debug Mode

Enable debug mode in `config.yml`:
```yaml
debug: true
```

This provides detailed logging for:
- Task execution attempts and results
- Database operations and queries
- Time checking intervals and changes
- Command execution details and outcomes
- Error stack traces for troubleshooting

### Performance Monitoring

Monitor these aspects for optimal performance:
- Server TPS during task execution
- Database file size growth
- Memory usage patterns
- Console log frequency

## Building from Source

### Prerequisites
- Java 21 or higher
- Maven 3.6 or higher
- Git

### Build Steps
1. Clone the repository:
   ```bash
   git clone <repository-url>
   cd GhastTasks
   ```

2. Build the plugin:
   ```bash
   mvn clean package
   ```

3. Find the compiled JAR in the `target` folder:
   ```
   target/GhastTasks-1.0.0.jar
   ```

### Development Setup
```bash
# Clean build
mvn clean

# Compile only
mvn compile

# Run tests
mvn test

# Package without tests
mvn package -DskipTests
```

## Configuration Best Practices

### Task Design
- **Keep commands simple**: Avoid overly complex command chains
- **Test thoroughly**: Use `/gtasks test <id>` before deploying
- **Monitor execution**: Enable debug mode initially to verify behavior
- **Backup configs**: Keep backups of working configurations

### Performance Optimization
- **Limit concurrent tasks**: Avoid scheduling multiple tasks at the same time
- **Command efficiency**: Use efficient commands that don't cause lag
- **Database maintenance**: Periodically check database size
- **Debug mode**: Disable debug mode in production for better performance

### Security Considerations
- **Permission management**: Carefully assign permissions
- **Command validation**: Test all commands before deployment
- **Input sanitization**: The plugin handles this automatically
- **Backup strategy**: Regular backups of config and database

## API for Developers

### Events
The plugin fires custom events that other plugins can listen to:
- `TaskExecuteEvent`: Fired before task execution
- `TaskCompleteEvent`: Fired after successful task execution

### Integration
Other plugins can interact with GhastTasks through:
- Direct API calls to TaskManager
- Database queries (read-only recommended)
- Configuration file modifications

## Support & Contributing

### Getting Help
- Check this documentation first
- Enable debug mode for detailed logs
- Search existing issues in the repository
- Create detailed bug reports with logs

### Contributing
- Fork the repository
- Create feature branches
- Follow existing code style
- Add tests for new features
- Submit pull requests with clear descriptions

## Changelog

### Version 1.0.0
- Initial release
- Core task scheduling functionality
- SQLite database integration
- Full command suite implementation
- Spam prevention and thread safety
- Comprehensive error handling

## License

This project is licensed under the MIT License - see the LICENSE file for details.

## Credits

- **PlaceholderAPI**: For server time integration
- **Paper**: For the robust Minecraft server platform
- **SQLite**: For lightweight database functionality

---

**Note**: This plugin is designed for Paper servers. Bukkit/Spigot compatibility is not guaranteed due to the use of Paper-specific APIs and features.