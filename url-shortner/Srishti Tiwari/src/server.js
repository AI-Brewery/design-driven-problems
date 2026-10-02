const express = require('express');
const db = require('./db');
const { generateShortCode } = require('./shortcode');

const app = express();
app.use(express.json());

app.post('/shorten', (req, res) => {
  const { longUrl, customAlias, expiresAt } = req.body;

  if (!longUrl || !longUrl.startsWith('http')) {
    return res.status(400).json({ error: 'invalid longUrl' });
  }

  let shortCode;
  const createdAt = new Date().toISOString();

  if (customAlias) {
    const existing = db.prepare('SELECT * FROM urls WHERE short_code = ?').get(customAlias);
    if (existing) {
      return res.status(409).json({ error: 'alias already taken' });
    }

    db.prepare(
      'INSERT INTO urls (short_code, long_url, created_at, expires_at) VALUES (?, ?, ?, ?)'
    ).run(customAlias, longUrl, createdAt, expiresAt || null);

    shortCode = customAlias;
  } else {
    const insert = db.prepare(
      'INSERT INTO urls (short_code, long_url, created_at, expires_at) VALUES (?, ?, ?, ?)'
    ).run('temp', longUrl, createdAt, expiresAt || null);

    shortCode = generateShortCode(insert.lastInsertRowid);

    db.prepare('UPDATE urls SET short_code = ? WHERE id = ?').run(shortCode, insert.lastInsertRowid);
  }

  res.json({ shortUrl: `http://localhost:3000/${shortCode}` });
});

app.get('/:shortCode', (req, res) => {
  const { shortCode } = req.params;
  const row = db.prepare('SELECT * FROM urls WHERE short_code = ?').get(shortCode);

  if (!row) {
    return res.status(404).json({ error: 'not found' });
  }

  if (row.expires_at && new Date(row.expires_at) < new Date()) {
    return res.status(404).json({ error: 'link expired' });
  }

  db.prepare('UPDATE urls SET clicks = clicks + 1 WHERE short_code = ?').run(shortCode);

  res.redirect(302, row.long_url);
});

app.get('/stats/:shortCode', (req, res) => {
  const { shortCode } = req.params;
  const row = db.prepare('SELECT * FROM urls WHERE short_code = ?').get(shortCode);

  if (!row) {
    return res.status(404).json({ error: 'not found' });
  }

  res.json({
    longUrl: row.long_url,
    clicks: row.clicks,
    createdAt: row.created_at,
    expiresAt: row.expires_at
  });
});

app.delete('/:shortCode', (req, res) => {
  const { shortCode } = req.params;
  const result = db.prepare('DELETE FROM urls WHERE short_code = ?').run(shortCode);

  if (result.changes === 0) {
    return res.status(404).json({ error: 'not found' });
  }

  res.json({ message: 'deleted' });
});

app.listen(3000, () => {
  console.log('Server running on http://localhost:3000');
});