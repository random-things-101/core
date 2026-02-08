# Quick Start Deployment Guide

This guide will help you deploy the migrated Core plugin system with the REST API architecture.

## Prerequisites

- Java 21+ (for Minecraft servers)
- MySQL 8.0+ database
- Bun.js runtime (for API server)
- Text editor (nano, vim, or VS Code)

## Step 1: Set Up the Database

### 1.1 Create Database and User

```sql
-- Connect to MySQL
mysql -u root -p

-- Create database
CREATE DATABASE core_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- Create user (optional - you can use root for testing)
CREATE USER 'core_user'@'localhost' IDENTIFIED BY 'core_password';

-- Grant permissions
GRANT ALL PRIVILEGES ON core_db.* TO 'core_user'@'localhost';
FLUSH PRIVILEGES;

-- Exit
EXIT;
```

**Note**: Tables will be created automatically by the API server on first startup.

---

## Step 2: Deploy the API Server

### 2.1 Install Bun.js

```bash
# Install Bun
curl -fsSL https://bun.sh/install | bash

# Reload your shell
source ~/.bashrc  # or source ~/.zshrc
```

### 2.2 Configure the API

```bash
# Navigate to API directory
cd /home/vwinter/Projects/core-api

# Copy environment template
cp .env.example .env

# Edit configuration
nano .env
```

Edit `.env` with your settings:
```env
# Server Configuration
PORT=3000
HOST=0.0.0.0

# API Authentication - CHANGE THIS IN PRODUCTION!
API_KEY=your-secret-api-key-here

# MySQL Database Configuration
DB_HOST=localhost
DB_PORT=3306
DB_NAME=core_db
DB_USER=core_user
DB_PASSWORD=core_password

# Connection Pool Settings
DB_CONNECTION_LIMIT=10
DB_QUEUE_LIMIT=0
```

### 2.3 Install Dependencies and Start

```bash
# Install dependencies
bun install

# Start API server (development mode with auto-reload)
bun run dev

# OR start production server
bun run start
```

You should see:
```
🚀 Starting Core API...
✅ Database connection established successfully
📋 Ensuring database tables exist...
✅ Database tables ready
✨ Core API is running at http://0.0.0.0:3000
📚 API documentation: http://0.0.0.0:3000/
```

### 2.4 Test the API

Open a new terminal and test:

```bash
# Health check
curl http://localhost:3000/api/health

# Expected response:
# {"status":"ok","timestamp":"2024-01-01T00:00:00.000Z"}

# Test with API key
curl -H "X-API-Key: your-secret-api-key-here" \
  http://localhost:3000/api/ranks

# Expected response:
# []  (empty array - no ranks yet)
```

### 2.5 Run API as Service (Production)

#### Option A: Using PM2

```bash
# Install PM2
npm install -g pm2

# Start API with PM2
pm2 start bun --name "core-api" -- src/index.ts

# Save PM2 configuration
pm2 save

# Setup PM2 to start on boot
pm2 startup
# Follow the instructions displayed

# View logs
pm2 logs core-api

# Restart API
pm2 restart core-api

# Stop API
pm2 stop core-api
```

#### Option B: Using systemd

Create `/etc/systemd/system/core-api.service`:
```ini
[Unit]
Description=Core API Server
After=network.target mysql.service

[Service]
Type=simple
User=your-username
WorkingDirectory=/home/vwinter/Projects/core-api
ExecStart=/usr/local/bin/bun src/index.ts
Restart=always
RestartSec=10
Environment=NODE_ENV=production

[Install]
WantedBy=multi-user.target
```

Enable and start:
```bash
sudo systemctl daemon-reload
sudo systemctl enable core-api
sudo systemctl start core-api
sudo systemctl status core-api
```

---

## Step 3: Build and Deploy Plugins

### 3.1 Build Plugins

```bash
# Navigate to project root
cd /home/vwinter/Projects/core

# Build all modules
mvn clean package

# Verify JARs were created
ls -lh bukkit/target/bukkit-1.0.0.jar
ls -lh bungee/target/bungee-1.0.0.jar
```

### 3.2 Configure Bukkit Plugin

Edit `bukkit/src/main/resources/config.yml` **before** building, or edit the deployed config:

```yaml
# API Configuration
api:
  base-url: "http://localhost:3000/api"
  api-key: "your-secret-api-key-here"
```

### 3.3 Configure BungeeCord Plugin

Edit `bungee/src/main/resources/config.yml` **before** building, or edit the deployed config:

```yaml
# API Configuration
api:
  base-url: "http://localhost:3000/api"
  api-key: "your-secret-api-key-here"
```

**Important**: The `api-key` must match the `API_KEY` in your API's `.env` file!

### 3.4 Deploy JARs

#### Bukkit Server
```bash
# Stop server
# Copy JAR to plugins folder
cp bukkit/target/bukkit-1.0.0.jar /path/to/bukkit/plugins/

# Remove old JAR if exists
rm /path/to/bukkit/plugins/core-*.jar  # if any old version

# Start server
```

#### BungeeCord Server
```bash
# Stop server
# Copy JAR to plugins folder
cp bungee/target/bungee-1.0.0.jar /path/to/bungee/plugins/

# Remove old JAR if exists
rm /path/to/bungee/plugins/core-*.jar  # if any old version

# Start server
```

---

## Step 4: Create Initial Data

### 4.1 Create Default Rank

```bash
curl -X POST \
  -H "X-API-Key: your-secret-api-key-here" \
  -H "Content-Type: application/json" \
  -d '{
    "id": "default",
    "name": "default",
    "displayName": "Default",
    "prefix": null,
    "suffix": null,
    "priority": 0,
    "isDefault": true,
    "permissions": null
  }' \
  http://localhost:3000/api/ranks
```

### 4.2 Create VIP Rank

```bash
curl -X POST \
  -H "X-API-Key: your-secret-api-key-here" \
  -H "Content-Type: application/json" \
  -d '{
    "id": "vip",
    "name": "vip",
    "displayName": "VIP",
    "prefix": "&a[VIP] ",
    "suffix": "",
    "priority": 10,
    "isDefault": false,
    "permissions": ["essentials.fly", "essentials.gamemode"]
  }' \
  http://localhost:3000/api/ranks
```

### 4.3 Create Admin Rank

```bash
curl -X POST \
  -H "X-API-Key: your-secret-api-key-here" \
  -H "Content-Type: application/json" \
  -d '{
    "id": "admin",
    "name": "admin",
    "displayName": "Administrator",
    "prefix": "&c[Admin] ",
    "suffix": "&f",
    "priority": 100,
    "isDefault": false,
    "permissions": ["*"]
  }' \
  http://localhost:3000/api/ranks
```

---

## Step 5: Test the Integration

### 5.1 Join the Server

1. Start your Minecraft client
2. Connect to the server
3. Your player profile should be automatically created

### 5.2 Verify Player Creation

```bash
# Replace YOUR_UUID with your actual UUID
curl -H "X-API-Key: your-secret-api-key-here" \
  http://localhost:3000/api/players/YOUR_UUID
```

### 5.3 Test Grant Command

In-game (as OP or with permission):
```
/grant PlayerName vip 30d "VIP Player"
```

### 5.4 Verify Grant

```bash
# Replace PLAYER_UUID with actual UUID
curl -H "X-API-Key: your-secret-api-key-here" \
  http://localhost:3000/api/grants/player/PLAYER_UUID/active
```

---

## Troubleshooting

### Plugin Won't Load

**Error**: `Can't connect to API server`

**Solutions**:
1. Check API server is running: `curl http://localhost:3000/api/health`
2. Verify `api.base-url` in plugin config matches API URL
3. Check firewall isn't blocking port 3000
4. Check API logs for errors

### API Key Errors

**Error**: `Unauthorized: Invalid API key`

**Solutions**:
1. Verify `api.api-key` in plugin config matches `API_KEY` in API `.env`
2. Check for typos (copy-paste carefully)
3. Restart plugin after changing config

### Database Connection Errors

**Error**: `Failed to connect to database`

**Solutions**:
1. Verify MySQL is running: `systemctl status mysql`
2. Check database credentials in API `.env`
3. Ensure database exists: `SHOW DATABASES;` in MySQL
4. Check user permissions: `GRANT ALL ON core_db.* TO 'core_user'@'localhost';`

### Player Not Found

**Error**: `Player not found` when testing

**Solutions**:
1. Join the server first (auto-creates player)
2. Check API logs for errors
3. Verify UUID format (36 characters with dashes)

---

## Production Checklist

Before deploying to production:

- [ ] Change `API_KEY` to a strong random value
- [ ] Use strong database password
- [ ] Enable HTTPS on API server (use reverse proxy like nginx)
- [ ] Set up firewall rules (only allow API port from Minecraft servers)
- [ ] Configure API to run as service (PM2 or systemd)
- [ ] Set up log rotation
- [ ] Test backup/restore procedures
- [ ] Monitor API health endpoint
- [ ] Set up alerts for API downtime
- [ ] Document your API key securely (password manager)
- [ ] Test with multiple concurrent players
- [ ] Verify cross-platform messaging (Bukkit ↔ BungeeCord)

---

## Monitoring

### Check API Health

```bash
watch -n 5 'curl -s http://localhost:3000/api/health | jq'
```

### View API Logs

```bash
# PM2
pm2 logs core-api

# systemd
journalctl -u core-api -f

# Direct (if running in foreground)
# Logs are in the terminal where you ran bun run dev
```

### Monitor Database Connections

```sql
-- In MySQL
SHOW PROCESSLIST;
-- Look for connections from core_api
```

### Check Plugin Stats

In-game:
```
/core debug
```

---

## Backup Strategy

### Database Backup

```bash
# Automated backup script
#!/bin/bash
DATE=$(date +%Y%m%d_%H%M%S)
mysqldump -u core_user -p'core_password' core_db > backup_$DATE.sql
gzip backup_$DATE.sql
# Keep last 7 days
find . -name "backup_*.sql.gz" -mtime +7 -delete
```

### Configuration Backup

```bash
# Backup plugin configs
cp -r /path/to/bukkit/plugins/Core /backup/core_bukkit_$(date +%Y%m%d)
cp -r /path/to/bungee/plugins/Core /backup/core_bungee_$(date +%Y%m%d)

# Backup API config
cp /home/vwinter/Projects/core-api/.env /backup/api_env_$(date +%Y%m%d)
```

---

## Scaling Considerations

### Multiple Servers

If running multiple Bukkit servers:
1. All servers connect to the same API
2. API handles concurrent requests
3. Consider increasing `DB_CONNECTION_LIMIT` in API `.env`

### API Performance

If API becomes bottleneck:
1. Add caching (Redis) for frequently accessed data
2. Implement connection pooling between API and multiple databases
3. Use load balancer (nginx) for multiple API instances

### Database Optimization

```sql
-- Add indexes if needed
CREATE INDEX idx_players_username ON players(username);
CREATE INDEX idx_grants_expires_at_active ON grants(expires_at, is_active);

-- Monitor slow queries
SET GLOBAL slow_query_log = 'ON';
SET GLOBAL long_query_time = 1;
```

---

## Next Steps

After successful deployment:

1. ✅ Test all plugin commands
2. ✅ Verify player join/quit flow
3. ✅ Test grant creation and expiration
4. ✅ Test rank management
5. ✅ Monitor API performance
6. ✅ Set up automated backups
7. ✅ Document any custom configurations
8. ✅ Train staff on new commands (if changed)

---

## Support

For issues or questions:
- Check logs first (API and plugin logs)
- Review this deployment guide
- Check API documentation: `core-api/README.md`
- Review migration documentation: `MIGRATION_COMPLETE.md`

---

**Deployment Status**: Ready to deploy! ✅

All components have been built, tested, and documented. Follow this guide step-by-step for a successful deployment.
