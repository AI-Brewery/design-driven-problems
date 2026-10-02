import sqlite3
from pathlib import Path


# Store the database inside the src folder
BASE_DIR = Path(__file__).resolve().parent
DATABASE = BASE_DIR / "url_shortener.db"


def get_connection():
    """Create and return a connection to the SQLite database."""
    connection = sqlite3.connect(DATABASE)
    connection.row_factory = sqlite3.Row
    return connection


def init_db():
    """Create the URLs table if it does not already exist."""
    connection = get_connection()

    connection.execute("""
        CREATE TABLE IF NOT EXISTS urls (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            short_code VARCHAR(10) UNIQUE NOT NULL,
            long_url TEXT NOT NULL,
            clicks INTEGER NOT NULL DEFAULT 0,
            created_at DATETIME NOT NULL,
            expires_at DATETIME
        )
    """)

    connection.commit()
    connection.close()


def create_url(short_code, long_url, created_at, expires_at=None):
    """Store a new shortened URL in the database."""
    connection = get_connection()

    connection.execute(
        """
        INSERT INTO urls
        (short_code, long_url, clicks, created_at, expires_at)
        VALUES (?, ?, 0, ?, ?)
        """,
        (short_code, long_url, created_at, expires_at)
    )

    connection.commit()
    connection.close()


def get_url_by_code(short_code):
    """Find a URL using its short code."""
    connection = get_connection()

    row = connection.execute(
        "SELECT * FROM urls WHERE short_code = ?",
        (short_code,)
    ).fetchone()

    connection.close()
    return row


def increment_clicks(short_code):
    """Increase the click count for a shortened URL."""
    connection = get_connection()

    connection.execute(
        """
        UPDATE urls
        SET clicks = clicks + 1
        WHERE short_code = ?
        """,
        (short_code,)
    )

    connection.commit()
    connection.close()


def delete_url(short_code):
    """Delete a shortened URL."""
    connection = get_connection()

    cursor = connection.execute(
        "DELETE FROM urls WHERE short_code = ?",
        (short_code,)
    )

    connection.commit()
    connection.close()

    return cursor.rowcount > 0