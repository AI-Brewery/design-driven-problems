from datetime import datetime

import database
from short_code_generator import ShortCodeGenerator


class URLService:
    """Handles the main business logic of the URL shortener."""

    def __init__(self):
        self.generator = ShortCodeGenerator()

        # Simple in-memory cache for frequently accessed URLs.
        # Key   = short code
        # Value = database row
        self.cache = {}

    def create_short_url(self, long_url, custom_alias=None, expires_at=None):
        """Create and store a new shortened URL."""

        # Use custom alias if provided.
        if custom_alias:
            short_code = custom_alias

            # Check whether the custom alias already exists.
            existing_url = database.get_url_by_code(short_code)

            if existing_url:
                raise ValueError("Custom alias already exists.")

        else:
            # Generate a random code and handle collisions.
            while True:
                short_code = self.generator.generate()

                if not database.get_url_by_code(short_code):
                    break

        created_at = datetime.now().isoformat()

        database.create_url(
            short_code=short_code,
            long_url=long_url,
            created_at=created_at,
            expires_at=expires_at
        )

        # Store the newly created URL in the cache.
        row = database.get_url_by_code(short_code)
        self.cache[short_code] = row

        return short_code

    def get_url(self, short_code):
        """Retrieve a URL and check whether it has expired."""

        # First check the cache.
        row = self.cache.get(short_code)

        # If not found in cache, check the database.
        if row is None:
            row = database.get_url_by_code(short_code)

            if row is None:
                return None

            # Add the result to the cache.
            self.cache[short_code] = row

        # Check expiry.
        if row["expires_at"]:
            expires_at = datetime.fromisoformat(row["expires_at"])

            if datetime.now() >= expires_at:
                return None

        return row

    def record_click(self, short_code):
        """Increase the click count after a successful redirect."""

        database.increment_clicks(short_code)

        # Remove the cached row because its click count is now outdated.
        self.cache.pop(short_code, None)

    def get_stats(self, short_code):
        """Return statistics for a shortened URL."""

        row = database.get_url_by_code(short_code)

        if row is None:
            return None

        # Check whether the URL has expired.
        if row["expires_at"]:
            expires_at = datetime.fromisoformat(row["expires_at"])

            if datetime.now() >= expires_at:
                return None

        return {
            "longUrl": row["long_url"],
            "clicks": row["clicks"],
            "createdAt": row["created_at"],
            "expiresAt": row["expires_at"]
        }

    def delete_url(self, short_code):
        """Delete a shortened URL."""

        deleted = database.delete_url(short_code)

        # Remove it from cache as well.
        self.cache.pop(short_code, None)

        return deleted