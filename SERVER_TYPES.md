# Server Type Distinction - Bungee + Paper Only

## Overview

The API now supports only two server types:
- **BungeeCord** (type=`bungee`) → Normalized as `proxy`
- **Paper** (type=`paper`) → Normalized as `server`

## Connection URLs

### Paper Game Servers
```bash
ws://localhost:3000/ws?api_key=YOUR_KEY&type=paper&name=survival
ws://localhost:3000/ws?api_key=YOUR_KEY&type=paper&name=creative
ws://localhost:3000/ws?api_key=YOUR_KEY&type=paper&name=skyblock
```

### BungeeCord Proxy
```bash
ws://localhost:3000/ws?api_key=YOUR_KEY&type=bungee&name=main-proxy
ws://localhost:3000/ws?api_key=YOUR_KEY&type=bungee&name=lobby-proxy
```

## Architecture

```
┌─────────────┐
│  BungeeCord │  (type=bungee → proxy)
│   Proxy    │
└──────┬──────┘
       │
       ├────────────────┬────────────────┐
       │                │                │
       ▼                ▼                ▼
┌─────────────┐   ┌─────────────┐   ┌─────────────┐
│   Paper    │   │   Paper    │   │   Paper    │
│  Survival  │   │  Creative   │   │  Skyblock   │
│ (server)   │   │  (server)   │   │  (server)   │
└─────────────┘   └─────────────┘   └─────────────┘
(type=paper)      (type=paper)        (type=paper)
```

## Message Routing

### Grant Changes → Paper Servers Only
```
API Grant Modified
    ↓
Send to: type='server' (Paper only)
    ↓
┌─────────────┐   ┌─────────────┐   ┌─────────────┐
│  Survival  │   │  Creative   │   │  Skyblock   │
└─────────────┘   └─────────────┘   └─────────────┘
     ✅                  ✅                  ✅

❌ BungeeCord (proxy) - NOT notified (doesn't have players)
```

### Player Updates → Paper Servers Only
```
API Player Updated
    ↓
Send to: type='server' (Paper only)
    ↓
┌─────────────┐   ┌─────────────┐   ┌─────────────┐
│  Survival  │   │  Creative   │   │  Skyblock   │
└─────────────┘   └─────────────┘   └─────────────┘
     ✅                  ✅                  ✅

❌ BungeeCord (proxy) - NOT notified (doesn't have players)
```

### Rank Changes → All Servers
```
API Rank Modified
    ↓
Send to: All (servers + proxy)
    ↓
┌─────────────┐   ┌─────────────┐   ┌─────────────┐
│  Survival  │   │  Creative   │   │  Skyblock   │
└─────────────┘   └─────────────┘   └─────────────┘
     ✅                  ✅                  ✅

┌─────────────┐
│  BungeeCord │
│   Proxy    │
└─────────────┘
     ✅
```

### Private Messages → Paper Servers Only
```
BungeeCord sends PM via API
    ↓
API broadcasts to: type='server' (Paper only)
    ↓
Target player's Paper server receives message
```

## Type Validation

### ✅ Valid Types
```bash
type=bungee  → Normalized to 'proxy'
type=paper   → Normalized to 'server'
```

### ❌ Invalid Types
```bash
type=bukkit        → ERROR: Invalid type parameter
type=velocity      → ERROR: Invalid type parameter
type=spigot        → ERROR: Invalid type parameter
type=server        → ERROR: Invalid type parameter
type=proxy         → ERROR: Invalid type parameter
```

**Error Message**:
```json
{
  "type": "ERROR",
  "message": "Invalid type parameter. Use: bungee or paper"
}
```

## Configuration

### Paper Server (config.yml)
```yaml
api:
  base-url: "http://localhost:3000/api"
  api-key: "your-secret-api-key-here"
  ws-url: "ws://localhost:3000/ws"
  server-name: "survival"  # Your server name
```

### BungeeCord (config.yml)
```yaml
api:
  base-url: "http://localhost:3000/api"
  api-key: "your-secret-api-key-here"
  ws-url: "ws://localhost:3000/ws"
  server-name: "main-proxy"  # Your proxy name
```

## WebSocket Info Endpoint

### GET /ws
```json
{
  "websocket": {
    "url": "ws://localhost:3000/ws",
    "path": "/ws",
    "authentication": "api_key query parameter",
    "parameters": {
      "api_key": "Your API key (required)",
      "type": "Server type (required): bungee or paper",
      "name": "Server name (optional identifier)"
    },
    "types": {
      "proxy": "BungeeCord proxy (type=bungee)",
      "server": "Paper game server (type=paper)"
    },
    "examples": [
      "ws://localhost:3000/ws?api_key=YOUR_KEY&type=bungee&name=main-proxy",
      "ws://localhost:3000/ws?api_key=YOUR_KEY&type=paper&name=survival"
    ],
    "connectedServers": [
      {
        "type": "server",
        "name": "survival",
        "alive": true
      },
      {
        "type": "server",
        "name": "creative",
        "alive": true
      },
      {
        "type": "proxy",
        "name": "main-proxy",
        "alive": true
      }
    ],
    "counts": {
      "total": 3,
      "servers": 2,
      "proxies": 1
    }
  }
}
```

## Benefits

### 1. Simplified Code
- ✅ Only two server types to support
- ✅ Cleaner validation logic
- ✅ Easier to maintain

### 2. Clear Purpose
- ✅ Proxy (BungeeCord) - Routes players to servers
- ✅ Servers (Paper) - Hosts actual players

### 3. Smart Routing
- ✅ Grant/Player updates → Only Paper servers (they have the players)
- ✅ Rank updates → All (everyone needs rank cache)
- ✅ Private messages → Only Paper servers (to find target player)

### 4. Better Performance
- ✅ No wasted messages to proxy that doesn't have players
- ✅ More efficient network usage

## Testing

### 1. Test Paper Server Connection
```bash
websocat "ws://localhost:3000/ws?api_key=your-key&type=paper&name=survival"
```

Expected log:
```
✅ WebSocket connected: server/survival
```

### 2. Test BungeeCord Connection
```bash
websocat "ws://localhost:3000/ws?api_key=your-key&type=bungee&name=main-proxy"
```

Expected log:
```
✅ WebSocket connected: proxy/main-proxy
```

### 3. Test Invalid Type
```bash
websocat "ws://localhost:3000/ws?api_key=your-key&type=bukkit&name=test"
```

Expected result:
```json
{"type":"ERROR","message":"Invalid type parameter. Use: bungee or paper"}
Connection closed
```

## Files Modified

### API (Bun.js)
- `src/ws/server.ts` - Simplified type validation (bungee/paper only)
- `src/index.ts` - Updated documentation

### Java (Bukkit/Paper Module)
- Actually should rename to `paper` module, but keeping as `bukkit` for compatibility
- `BukkitPlugin.java` - Uses `type=paper` when connecting

### Java (Bungee Module)
- `BungeePlugin.java` - Uses `type=bungee` when connecting

## Summary

✅ **Simplified**: Only bungee and paper supported
✅ **Clearer**: Proxy vs server distinction is explicit
✅ **Smarter Routing**: Messages only go to servers that need them
✅ **Better Performance**: No wasted messages to proxy
✅ **Easier Maintenance**: Less code to maintain

The API is now streamlined for BungeeCord + Paper architecture!
