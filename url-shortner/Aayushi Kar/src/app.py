from datetime import datetime

from flask import Flask, jsonify, redirect, request

import database
from url_service import URLService


app = Flask(__name__)

# Initialize the database when the application starts.
database.init_db()

# Create one URL service for the application.
url_service = URLService()


@app.route("/shorten", methods=["POST"])
def shorten_url():
    """Create a new shortened URL."""

    data = request.get_json(silent=True)

    if not data:
        return jsonify({"error": "Request body must be JSON."}), 400

    long_url = data.get("longUrl")
    custom_alias = data.get("customAlias")
    expires_at = data.get("expiresAt")

    # Validate long URL.
    if not long_url or not isinstance(long_url, str):
        return jsonify({"error": "longUrl is required."}), 400

    # Basic URL validation.
    if not (
        long_url.startswith("http://")
        or long_url.startswith("https://")
    ):
        return jsonify({"error": "Invalid URL. Use http:// or https://."}), 400

    # Validate expiry date if provided.
    if expires_at:
        try:
            expiry_datetime = datetime.fromisoformat(expires_at)

            if expiry_datetime <= datetime.now():
                return jsonify(
                    {"error": "expiresAt must be in the future."}
                ), 400

        except ValueError:
            return jsonify(
                {"error": "Invalid expiresAt format."}
            ), 400

    # Validate custom alias.
    if custom_alias:
        if not custom_alias.isalnum():
            return jsonify(
                {"error": "customAlias must contain only letters and numbers."}
            ), 400

    try:
        short_code = url_service.create_short_url(
            long_url=long_url,
            custom_alias=custom_alias,
            expires_at=expires_at
        )

    except ValueError as error:
        return jsonify({"error": str(error)}), 409

    return jsonify({
        "shortUrl": f"http://localhost:5000/{short_code}"
    }), 201


@app.route("/<short_code>", methods=["GET"])
def redirect_url(short_code):
    """Redirect a short URL to its original URL."""

    row = url_service.get_url(short_code)

    if row is None:
        return jsonify(
            {"error": "Short URL not found or expired."}
        ), 404

    # Record the click before redirecting.
    url_service.record_click(short_code)

    return redirect(row["long_url"], code=302)


@app.route("/stats/<short_code>", methods=["GET"])
def get_stats(short_code):
    """Return statistics for a shortened URL."""

    stats = url_service.get_stats(short_code)

    if stats is None:
        return jsonify(
            {"error": "Short URL not found or expired."}
        ), 404

    return jsonify(stats), 200


@app.route("/<short_code>", methods=["DELETE"])
def delete_url(short_code):
    """Delete a shortened URL."""

    deleted = url_service.delete_url(short_code)

    if not deleted:
        return jsonify(
            {"error": "Short URL not found."}
        ), 404

    return "", 204


if __name__ == "__main__":
    app.run(debug=True)