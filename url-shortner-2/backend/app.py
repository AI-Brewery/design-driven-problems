from flask import Flask, request, jsonify, redirect, session
from flask_cors import CORS

from werkzeug.security import generate_password_hash, check_password_hash

from database import get_db, init_db
from url_shortener import generate_short_code

import os
from datetime import datetime, timezone


# =========================================================
# FLASK APP
# =========================================================

app = Flask(__name__)

app.secret_key = os.environ.get(
    "SECRET_KEY",
    "dev-secret-key-change-this-later"
)

app.config["SESSION_COOKIE_HTTPONLY"] = True
app.config["SESSION_COOKIE_SAMESITE"] = "Lax"
app.config["SESSION_COOKIE_SECURE"] = False

CORS(
    app,
    supports_credentials=True,
    origins=[
        "http://127.0.0.1:3000",
        "http://localhost:3000",
        "http://127.0.0.1:3001",
        "http://localhost:3001"
    ]
)


# Initialize database
init_db()


# =========================================================
# HOME / TEST ROUTE
# =========================================================

@app.route("/")
def home():

    return jsonify({
        "message": "URL Shortener API is running!",
        "status": "success"
    })


# =========================================================
# REGISTER
# =========================================================

@app.route("/api/register", methods=["POST"])
def register():

    data = request.get_json()

    name = data.get("name")
    email = data.get("email")
    password = data.get("password")


    # Check required fields
    if not name or not email or not password:

        return jsonify({
            "success": False,
            "message": "All fields are required"
        }), 400


    conn = get_db()


    # Check if email already exists
    existing_user = conn.execute(
        "SELECT id FROM users WHERE email = ?",
        (email,)
    ).fetchone()


    if existing_user:

        conn.close()

        return jsonify({
            "success": False,
            "message": "Email already registered"
        }), 409


    # Hash password
    hashed_password = generate_password_hash(password)


    # Create user
    cursor = conn.execute(
        """
        INSERT INTO users (name, email, password)
        VALUES (?, ?, ?)
        """,
        (name, email, hashed_password)
    )


    conn.commit()

    user_id = cursor.lastrowid

    conn.close()


    return jsonify({
        "success": True,
        "message": "Registration successful",
        "user_id": user_id
    }), 201


# =========================================================
# LOGIN
# =========================================================

@app.route("/api/login", methods=["POST"])
def login():

    data = request.get_json()

    email = data.get("email")
    password = data.get("password")


    if not email or not password:

        return jsonify({
            "success": False,
            "message": "Email and password are required"
        }), 400


    conn = get_db()


    user = conn.execute(
        """
        SELECT id, name, email, password, role
        FROM users
        WHERE email = ?
        """,
        (email,)
    ).fetchone()


    conn.close()


    if not user:

        return jsonify({
            "success": False,
            "message": "Invalid email or password"
        }), 401


    # Verify password
    if not check_password_hash(
        user["password"],
        password
    ):

        return jsonify({
            "success": False,
            "message": "Invalid email or password"
        }), 401


    # Create login session
    session["user_id"] = user["id"]
    session["user_name"] = user["name"]
    session["user_email"] = user["email"]
    session["role"] = user["role"]


    return jsonify({
        "success": True,
        "message": "Login successful",
        "user": {
            "id": user["id"],
            "name": user["name"],
            "email": user["email"],
            "role": user["role"]
        }
    })


# =========================================================
# LOGOUT
# =========================================================

@app.route("/api/logout", methods=["POST"])
def logout():

    session.clear()


    return jsonify({
        "success": True,
        "message": "Logged out successfully"
    })


# =========================================================
# CURRENT USER
# =========================================================

@app.route("/api/me", methods=["GET"])
def current_user():

    if "user_id" not in session:

        return jsonify({
            "logged_in": False
        })


    return jsonify({
        "logged_in": True,
        "user": {
            "id": session["user_id"],
            "name": session["user_name"],
            "email": session["user_email"],
            "role": session["role"]
        }
    })


# =========================================================
# SHORTEN URL
# =========================================================

@app.route("/api/shorten", methods=["POST"])
def shorten_url():

    data = request.get_json()

    original_url = data.get("original_url")
    url_type = data.get("url_type", "Public")
    expiry_date = data.get("expiry_date")


    if not original_url:

        return jsonify({
            "success": False,
            "message": "URL is required"
        }), 400


    # Private URLs require login
    if url_type == "Private" and "user_id" not in session:

        return jsonify({
            "success": False,
            "message": "Login required for private URLs"
        }), 401


    # Generate unique short code
    conn = get_db()


    while True:

        short_code = generate_short_code()


        existing = conn.execute(
            "SELECT id FROM urls WHERE short_code = ?",
            (short_code,)
        ).fetchone()


        if not existing:
            break


    # Logged-in user
    user_id = session.get("user_id")


    # Save URL
    cursor = conn.execute(
        """
        INSERT INTO urls
        (
            short_code,
            original_url,
            user_id,
            url_type,
            expiry_date
        )
        VALUES (?, ?, ?, ?, ?)
        """,
        (
            short_code,
            original_url,
            user_id,
            url_type,
            expiry_date
        )
    )


    conn.commit()

    url_id = cursor.lastrowid

    conn.close()


    return jsonify({
        "success": True,
        "message": "URL shortened successfully",
        "url": {
            "id": url_id,
            "short_code": short_code,
            "short_url": f"http://localhost:5000/s/{short_code}",
            "original_url": original_url,
            "type": url_type,
            "expiry_date": expiry_date
        }
    }), 201


# =========================================================
# GET PUBLIC URLS
# =========================================================

@app.route("/api/urls", methods=["GET"])
def get_public_urls():

    conn = get_db()


    urls = conn.execute(
        """
        SELECT
            id,
            short_code,
            original_url,
            created_at,
            expiry_date
        FROM urls
        WHERE url_type = 'Public'
        ORDER BY created_at DESC
        """
    ).fetchall()


    conn.close()


    result = []


    for url in urls:

        result.append({
            "id": url["id"],
            "short_code": url["short_code"],
            "short_url": f"http://localhost:5000/s/{url['short_code']}",
            "original_url": url["original_url"],
            "created_at": url["created_at"],
            "expiry_date": url["expiry_date"] or "Never"
        })


    return jsonify({
        "success": True,
        "urls": result
    })


# =========================================================
# GET MY URLS
# =========================================================

@app.route("/api/my-urls", methods=["GET"])
def my_urls():

    if "user_id" not in session:

        return jsonify({
            "success": False,
            "message": "Login required"
        }), 401


    conn = get_db()


    urls = conn.execute(
        """
        SELECT
            id,
            short_code,
            original_url,
            url_type,
            created_at,
            expiry_date
        FROM urls
        WHERE user_id = ?
        ORDER BY created_at DESC
        """,
        (session["user_id"],)
    ).fetchall()


    conn.close()


    result = []


    for url in urls:

        result.append({
            "id": url["id"],
            "short_code": url["short_code"],
            "short_url": f"http://localhost:5000/s/{url['short_code']}",
            "original_url": url["original_url"],
            "type": url["url_type"],
            "created_at": url["created_at"],
            "expiry_date": url["expiry_date"] or "Never"
        })


    return jsonify({
        "success": True,
        "urls": result
    })


# =========================================================
# DELETE URL
# =========================================================

@app.route("/api/urls/<int:url_id>", methods=["DELETE"])
def delete_url(url_id):

    if "user_id" not in session:

        return jsonify({
            "success": False,
            "message": "Login required"
        }), 401


    conn = get_db()


    url = conn.execute(
        """
        SELECT id
        FROM urls
        WHERE id = ? AND user_id = ?
        """,
        (
            url_id,
            session["user_id"]
        )
    ).fetchone()


    if not url:

        conn.close()

        return jsonify({
            "success": False,
            "message": "URL not found"
        }), 404


    conn.execute(
        "DELETE FROM urls WHERE id = ?",
        (url_id,)
    )


    conn.commit()

    conn.close()


    return jsonify({
        "success": True,
        "message": "URL deleted successfully"
    })


# =========================================================
# ADMIN — GET ALL URLS
# =========================================================

@app.route("/api/admin/urls", methods=["GET"])
def admin_urls():

    if "user_id" not in session:

        return jsonify({
            "success": False,
            "message": "Login required"
        }), 401


    if session.get("role") != "admin":

        return jsonify({
            "success": False,
            "message": "Admin access required"
        }), 403


    conn = get_db()


    urls = conn.execute(
        """
        SELECT
            urls.id,
            urls.short_code,
            urls.original_url,
            urls.url_type,
            urls.created_at,
            urls.expiry_date,
            users.name AS user_name,
            users.email AS user_email
        FROM urls
        LEFT JOIN users
        ON urls.user_id = users.id
        ORDER BY urls.created_at DESC
        """
    ).fetchall()


    conn.close()


    result = []


    for url in urls:

        result.append({
            "id": url["id"],
            "short_code": url["short_code"],
            "short_url": f"http://localhost:5000/s/{url['short_code']}",
            "original_url": url["original_url"],
            "user": url["user_name"] or "Guest",
            "email": url["user_email"] or "-",
            "type": url["url_type"],
            "created_at": url["created_at"],
            "expiry_date": url["expiry_date"] or "Never"
        })


    return jsonify({
        "success": True,
        "urls": result
    })


# =========================================================
# REDIRECT SHORT URL
# =========================================================

@app.route("/s/<short_code>")
def redirect_url(short_code):

    conn = get_db()


    url = conn.execute(
        """
        SELECT *
        FROM urls
        WHERE short_code = ?
        """,
        (short_code,)
    ).fetchone()


    conn.close()


    # Short URL does not exist
    if not url:

        return jsonify({
            "success": False,
            "message": "Short URL not found"
        }), 404


    # =====================================================
    # CHECK EXPIRY
    # =====================================================

    if url["expiry_date"]:

        try:

            expiry = datetime.fromisoformat(
                url["expiry_date"].replace(
                    "Z",
                    "+00:00"
                )
            )


            now = datetime.now(timezone.utc)


            if now > expiry:

                return jsonify({
                    "success": False,
                    "message": "This short URL has expired"
                }), 410


        except ValueError:

            pass


    # =====================================================
    # REDIRECT
    # =====================================================

    return redirect(url["original_url"])


# =========================================================
# RUN SERVER
# =========================================================

if __name__ == "__main__":

    app.run(
        host="0.0.0.0",
        port=5000,
        debug=True
    )