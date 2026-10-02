const Database = require('better-sqlite3');
const db = new Database('urls.db');

db.exec(`
  CREATE TABLE IF NOT EXISTS urls (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    short_code TEXT UNIQUE NOT NULL,
    long_url TEXT NOT NULL,
    created_at TEXT NOT NULL,
    expires_at TEXT,
    clicks INTEGER DEFAULT 0
  )
`);

module.exports = db;