# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Structure

This is a multi-module Maven project for Minecraft server plugins that supports both Bukkit/Paper and BungeeCord platforms. The project consists of three modules:

- **shared/** - Common code shared between Bukkit and BungeeCord (database, messaging, domain models, DAOs)
- **bukkit/** - Bukkit/Paper server plugin implementation
- **bungee/** - BungeeCord proxy plugin implementation

### Module Dependencies

Both `bukkit` and `bungee` modules depend on the `shared` module. When building, Maven will automatically compile `shared` first and shade it into the platform-specific jars.

## Build Commands

```bash
# Build all modules (from project root)
mvn clean package

# Build a specific module
mvn clean package -pl bukkit
mvn clean package -pl bungee
mvn clean package -pl shared

# Build and install to local Maven repository
mvn clean install
```

The default Maven goal is `clean package`, so simply running `mvn` will also build the project.

Build outputs are located in:
- `bukkit/target/bukkit-1.0.0.jar` - Bukkit plugin (includes shaded shared module)
- `bungee/target/bungee-1.0.0.jar` - BungeeCord plugin (includes shaded shared module)

## Architecture

### Core Domain Model

The project implements a player permissions and ranking system with three main entities:

- **Player** - Player profiles with playtime tracking, login timestamps, online status, and additional permissions
- **Grant** - Links players to ranks with expiration times (permanent or time-limited), granter information, and reasons
- **Rank** - Rank definitions with display names, prefixes, suffixes, priorities, and permission nodes

Players can have multiple grants; the active rank is determined by selecting the most recently granted valid grant (grants are ordered by `granted_at DESC` in the database). A grant is considered valid if it:
1. Is marked as active (`is_active = TRUE`)
2. Has not expired (either permanent, or `expires_at` is in the future)

### Shared Module ([shared/](shared))

Contains platform-agnostic functionality:

- **DatabaseManager** ([shared/src/main/java/club/catmc/core/shared/db/DatabaseManager.java](shared/src/main/java/club/catmc/core/shared/db/DatabaseManager.java)) - Async MySQL database connection and query management using HikariCP connection pooling and `CompletableFuture`
- **PluginMessage** ([shared/src/main/java/club/catmc/core/shared/messaging/PluginMessage.java](shared/src/main/java/club/catmc/core/shared/messaging/PluginMessage.java)) - Plugin messaging channel abstraction for Bukkit-Bungee communication
- **DAOs** - Data access objects for database operations: [PlayerDao](shared/src/main/java/club/catmc/core/shared/player/PlayerDao.java), [GrantDao](shared/src/main/java/club/catmc/core/shared/grant/GrantDao.java), [RankDao](shared/src/main/java/club/catmc/core/shared/rank/RankDao.java)
- **Domain Models** - Player, Grant, and Rank entity classes in their respective packages

### Platform Modules

Both `bukkit` and `bungee` modules have their own **PlayerManager** implementation that caches online players and ranks, loads player profiles on join, and handles synchronization.

**Bukkit Module** ([bukkit/](bukkit))
- Main class: [BukkitPlugin.java](bukkit/src/main/java/club/catmc/core/bukkit/BukkitPlugin.java)
- PlayerManager: [bukkit/manager/PlayerManager.java](bukkit/src/main/java/club/catmc/core/bukkit/manager/PlayerManager.java)
- Uses Aikar's Command Framework (ACF) for commands via `PaperCommandManager`
- Registers event listeners using Bukkit's event system ([ChatListener](bukkit/src/main/java/club/catmc/core/bukkit/listener/ChatListener.java), [PlayerListener](bukkit/src/main/java/club/catmc/core/bukkit/listener/PlayerListener.java))
- Uses Adventure API for text components (`net.kyori.adventure.text`)
- Configuration via standard Bukkit `config.yml`
- Interactive dialogs using Paper's Dialog API ([bukkit/dialogs/](bukkit/src/main/java/club/catmc/core/bukkit/dialogs)) for rank and grant management UI

**BungeeCord Module** ([bungee/](bungee))
- Main class: [BungeePlugin.java](bungee/src/main/java/club/catmc/core/bungee/BungeePlugin.java)
- PlayerManager: [bungee/manager/PlayerManager.java](bungee/src/main/java/club/catmc/core/bungee/manager/PlayerManager.java)
- Uses Aikar's Command Framework (ACF) for commands via `BungeeCommandManager`
- BungeeCord uses legacy chat format (`TextComponent` with `§` color codes)
- Registers plugin messaging channel (`core:channel`)
- Configuration via BungeeCord's `Configuration` API (requires manual loading/saving, see [loadConfig()](bungee/src/main/java/club/catmc/core/bungee/BungeePlugin.java#L138))

### Command Structure

Commands are implemented using ACF annotations:
- `@CommandAlias` - Defines command aliases
- `@Subcommand` - Defines subcommand paths
- `@CommandPermission` - Required permission node
- `@Description` - Command description

Example: [CoreCommand.java (Bukkit)](bukkit/src/main/java/club/catmc/core/bukkit/commands/CoreCommand.java)

### Configuration

Both modules use YAML configuration:
- Bukkit uses standard Bukkit config (via `getConfig()`)
- BungeeCord uses `net.md_5.bungee.config.Configuration`

Database configuration is loaded into a `DatabaseConfig` POJO in each module.

## Technology Stack

- **Java 21** - Source and target compatibility
- **Maven** - Build tool and dependency management
- **Paper API 1.21.11-R0.1-SNAPSHOT** - Bukkit/Paper server API (provided scope)
- **BungeeCord API 1.21-R0.4** - BungeeCord proxy API (provided scope)
- **ACF 0.5.1-SNAPSHOT** - Aikar's Command Framework for command handling
- **MySQL Connector 8.0.33** - MySQL JDBC driver
- **HikariCP 5.1.0** - JDBC connection pooling (shared module)
- **SLF4J 2.0.9** - Logging facade (shared module)
- **Paper Dialog API** - Interactive dialog system for Bukkit module (rank management, grants)

## Maven Repositories

The project uses several Maven repositories defined in the root POM:
- PaperMC repository (Paper API)
- Sonatype snapshots (BungeeCord API)
- Aikar repository (ACF)
- Maven Central (fallback repository)

## Development Notes

### Plugin Messaging

The BungeeCord plugin registers the `core:channel` for plugin messaging. For Bukkit-Bungee communication, implement plugin message listeners in the Bukkit module and use `PluginMessage` class from the shared module.

### Database Operations

All database operations are asynchronous and return `CompletableFuture`. The shared module uses HikariCP for connection pooling with these settings:
- Maximum pool size: 10 connections
- Minimum idle connections: 2
- Connection timeout: 30 seconds
- MySQL-specific optimizations enabled (prepared statement caching, batched statements)

When adding new database queries, follow the pattern in [DatabaseManager.java](shared/src/main/java/club/catmc/core/shared/db/DatabaseManager.java):
- Use `executeQuery()` for SELECT queries (provides ResultSet to consumer)
- Use `executeUpdate()` for INSERT/UPDATE/DELETE queries (returns CompletableFuture<Void>)

### DAO Pattern

All database access goes through Data Access Objects (DAOs) in the shared module:
- [PlayerDao](shared/src/main/java/club/catmc/core/shared/player/PlayerDao.java) - Player CRUD and queries
- [GrantDao](shared/src/main/java/club/catmc/core/shared/grant/GrantDao.java) - Grant management
- [RankDao](shared/src/main/java/club/catmc/core/shared/rank/RankDao.java) - Rank management

DAOs use SLF4J for logging (`LoggerFactory.getLogger()`). Methods that create tables are called during plugin initialization in `onEnable()`.

### Player Manager Lifecycle

The PlayerManager in each platform module:
1. Loads all ranks into cache during initialization
2. Loads player profiles (including grants) when players join
3. Determines active rank by selecting the first valid grant (most recently granted)
4. Saves player data and unloads on quit/shutdown
5. Can refresh rank cache or reload individual player grants at runtime

### Shading

Both platform modules use the Maven Shade plugin to package dependencies (including the shared module) into a single JAR. The `createDependencyReducedPom` is set to `false` to avoid generating a reduced POM.
