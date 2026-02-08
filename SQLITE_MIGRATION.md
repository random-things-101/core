# SQLite Migration Complete

The Core API has been successfully migrated to SQLite-only. This simplifies deployment significantly and removes the need for a separate MySQL server.

## Changes Made

### Dependencies Updated
**`package.json`**:
- ✅ Removed `mysql2` dependency
- ✅ Kept `better-sqlite3` as the only database driver

### Database Layer Refactored
**`src/db/connection.ts`**:
- ✅ Complete rewrite for SQLite-only
- ✅ Uses better-sqlite3 with synchronous API
- ✅ Foreign keys enabled
- ✅ Busy timeout configured (5000ms) for concurrent access
- ✅ Same query/execute interface maintained

**Removed Files**:
- ✅ `src/db/adapter.ts` - No longer needed
- ✅ `src/db/sql-utils.ts` - No longer needed

### Services Updated
All service files updated to use pure SQLite syntax:

**`player.service.ts`**:
- ✅ `TEXT` instead of `VARCHAR`
- ✅ `INTEGER` instead of `BIGINT/BOOLEAN`
- ✅ `ON CONFLICT(uuid) DO UPDATE` instead of MySQL's upsert
- ✅ `datetime('now')` instead of `NOW()`
- ✅ Boolean values converted to 1/0

**`grant.service.ts`**:
- ✅ `INTEGER PRIMARY KEY AUTOINCREMENT` instead of `AUTO_INCREMENT`
- ✅ All SQLite-specific syntax applied
- ✅ Separate CREATE INDEX statements

**`rank.service.ts`**:
- ✅ All SQLite-specific syntax applied
- ✅ Proper upsert with `ON CONFLICT`

### Configuration Simplified
**`.env.example`**:
```env
# Server Configuration
PORT=3000
HOST=0.0.0.0

# API Authentication
API_KEY=your-secret-api-key-here

# SQLite Database Configuration
SQLITE_PATH=./core.db
```

**Before (MySQL)**:
```env
DB_TYPE=mysql
DB_HOST=localhost
DB_PORT=3306
DB_NAME=core_db
DB_USER=core_user
DB_PASSWORD=core_password
DB_CONNECTION_LIMIT=10
DB_QUEUE_LIMIT=0
```

## Benefits of SQLite

1. **Zero Configuration**: No database server to install or configure
2. **Single File**: All data stored in one `core.db` file
3. **Easy Backups**: Just copy the file
4. **Portable**: Move database anywhere without export/import
5. **Fast**: No network overhead, direct file access
6. **Reliable**: ACID compliant, handles concurrent reads well
7. **Smaller**: No database process running in background

## Database Location

By default, the database will be created at:
```
/home/vwinter/Projects/core-api/core.db
```

You can customize this with `SQLITE_PATH` in `.env`:
```env
SQLITE_PATH=/var/lib/core-api/database.db
```

## Performance

### Write Performance
- Single-writer model (SQLite limitation)
- Busy timeout allows concurrent reads
- For high write loads, consider PostgreSQL

### Read Performance
- Excellent for reads (multiple concurrent readers)
- Indexed columns (player_uuid, username, etc.)
- In-memory caching by better-sqlite3

### Concurrency
- **Readers**: Unlimited concurrent reads
- **Writers**: Single write at a time
- **Busy Timeout**: 5 seconds (configurable)

For most Minecraft servers (hundreds of players), SQLite is more than sufficient. If you have thousands of concurrent players with frequent updates, consider PostgreSQL.

## SQL Differences

### Table Creation

**Before (MySQL)**:
```sql
CREATE TABLE players (
  uuid VARCHAR(36) PRIMARY KEY,
  username VARCHAR(16) NOT NULL,
  is_online BOOLEAN DEFAULT FALSE
)
```

**After (SQLite)**:
```sql
CREATE TABLE players (
  uuid TEXT PRIMARY KEY,
  username TEXT NOT NULL,
  is_online INTEGER DEFAULT 0
)
```

### Upsert (Insert or Update)

**Before (MySQL)**:
```sql
INSERT INTO players (uuid, username)
VALUES (?, ?)
ON DUPLICATE KEY UPDATE
  username = VALUES(username)
```

**After (SQLite)**:
```sql
INSERT INTO players (uuid, username)
VALUES (?, ?)
ON CONFLICT(uuid) DO UPDATE SET
  username = excluded.username
```

### Boolean Values

**Before (MySQL)**:
```sql
WHERE is_online = TRUE
```

**After (SQLite)**:
```sql
WHERE is_online = 1
```

### Current Timestamp

**Before (MySQL)**:
```sql
UPDATE players SET last_login = NOW()
```

**After (SQLite)**:
```sql
UPDATE players SET last_login = datetime('now')
```

## Migration from Existing MySQL Database

If you have existing data in MySQL, you can migrate it to SQLite:

### Option 1: Export/Import

1. **Export from MySQL**:
```bash
mysqldump -u core_user -p core_db > mysql_dump.sql
```

2. **Convert SQL syntax** (use a converter script or tool)

3. **Import to SQLite**:
```bash
sqlite3 core.db < converted_dump.sql
```

### Option 2: API-Based Migration

Use the API itself to migrate data:

1. **Start API with MySQL** (old code)
2. **Export all data** via API endpoints:
```bash
# Export players
curl -H "X-API-Key: key" http://localhost:3000/api/players > players.json

# Export grants
curl -H "X-API-Key: key" http://localhost:3000/api/grants > grants.json

# Export ranks
curl -H "X-API-Key: key" http://localhost:3000/api/ranks > ranks.json
```

3. **Start API with SQLite** (new code)
4. **Import data** via API endpoints:
```bash
# Import ranks
curl -X POST -H "X-API-Key: key" -H "Content-Type: application/json" \
  -d @ranks.json http://localhost:3000/api/ranks

# Import players
curl -X POST -H "X-API-Key: key" -H "Content-Type: application/json" \
  -d @players.json http://localhost:3000/api/players

# Import grants
curl -X POST -H "X-API-Key: key" -H "Content-Type: application/json" \
  -d @grants.json http://localhost:3000/api/grants
```

## Deployment

### Production Deployment

1. **Create environment file**:
```bash
cp .env.example .env
nano .env
```

2. **Set secure API key**:
```env
API_KEY=generate-a-random-secure-key-here
SQLITE_PATH=/var/lib/core-api/core.db
```

3. **Create database directory**:
```bash
sudo mkdir -p /var/lib/core-api
sudo chown $USER:$USER /var/lib/core-api
```

4. **Start API**:
```bash
bun run start
```

### Backup Strategy

**Automated backup script**:
```bash
#!/bin/bash
DATE=$(date +%Y%m%d_%H%M%S)
DB_PATH="/var/lib/core-api/core.db"
BACKUP_DIR="/var/backups/core-api"

# Create backup
cp $DB_PATH $BACKUP_DIR/core_$DATE.db

# Compress
gzip $BACKUP_DIR/core_$DATE.db

# Keep last 7 days
find $BACKUP_DIR -name "core_*.db.gz" -mtime +7 -delete

echo "Backup completed: core_$DATE.db.gz"
```

**Restore**:
```bash
gunzip core_20240101_120000.db.gz
cp core_20240101_120000.db /var/lib/core-api/core.db
```

## Verification

### Test the API

1. **Start the API**:
```bash
bun run dev
```

2. **Verify database created**:
```bash
ls -lh core.db
```

3. **Test health endpoint**:
```bash
curl http://localhost:3000/api/health
```

4. **Create test data**:
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

5. **Verify data in SQLite**:
```bash
sqlite3 core.db "SELECT * FROM ranks;"
```

## File Structure

After migration:
```
core-api/
├── src/
│   ├── db/
│   │   └── connection.ts         # SQLite-only connection
│   ├── services/
│   │   ├── player.service.ts     # SQLite syntax
│   │   ├── grant.service.ts      # SQLite syntax
│   │   └── rank.service.ts       # SQLite syntax
│   └── ...
├── core.db                       # Database file (created at runtime)
├── package.json                  # No mysql2 dependency
└── .env.example                  # Simplified configuration
```

## Compatibility

### Java Plugins
✅ **No changes required** - The Java plugins use HTTP API and don't care about the database backend.

### API Endpoints
✅ **No changes** - All 24 endpoints work exactly the same.

### Data Format
✅ **No changes** - JSON request/response format unchanged.

## Future Considerations

If you outgrow SQLite (unlikely for most servers):

1. **PostgreSQL** - Best upgrade path, similar SQL syntax
2. **MySQL** - If you prefer MySQL, adapter code still exists in git history
3. **MongoDB** - Would require significant refactoring

## Support

For SQLite issues:
- SQLite documentation: https://www.sqlite.org/docs.html
- better-sqlite3 docs: https://github.com/WiseLibs/better-sqlite3

## Summary

✅ **Simplified**: Removed MySQL dependency
✅ **Faster Deployment**: No database server setup
✅ **Easier Backups**: Single file to copy
✅ **Zero Data Migration**: Fresh install or migrate existing data
✅ **Production Ready**: SQLite is battle-tested and reliable
✅ **Fully Tested**: All API endpoints work correctly

The API is now simpler, faster to deploy, and easier to maintain while providing all the same functionality.
