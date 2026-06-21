from flask import Flask, jsonify, request
from flask_cors import CORS

import os
import sqlite3
from datetime import datetime

app = Flask(__name__)
CORS(app)

DB_PATH = os.path.join(os.path.dirname(__file__), "todo.db")


def get_db():
    conn = sqlite3.connect(DB_PATH)
    conn.row_factory = sqlite3.Row
    return conn


def init_db():
    conn = sqlite3.connect(DB_PATH)
    cursor = conn.cursor()
    cursor.execute("""
        CREATE TABLE IF NOT EXISTS todo (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            content TEXT NOT NULL,
            create_time TEXT NOT NULL,
            priority INTEGER DEFAULT 1,
            completed INTEGER DEFAULT 0,
            synced INTEGER DEFAULT 0
        )
    """)
    conn.commit()
    cursor.execute("PRAGMA table_info(todo)")
    columns = [row[1] for row in cursor.fetchall()]
    if "priority" not in columns:
        cursor.execute("ALTER TABLE todo ADD COLUMN priority INTEGER DEFAULT 1")
        conn.commit()
    if "completed" not in columns:
        cursor.execute("ALTER TABLE todo ADD COLUMN completed INTEGER DEFAULT 0")
        conn.commit()
    if "synced" not in columns:
        cursor.execute("ALTER TABLE todo ADD COLUMN synced INTEGER DEFAULT 0")
        conn.commit()
    conn.close()


@app.route("/todo/list", methods=["GET"])
def list_todos():
    query = request.args.get("q", "")
    priority = request.args.get("priority")

    conn = get_db()
    cursor = conn.cursor()
    sql = "SELECT id, content, create_time, priority, completed, synced FROM todo WHERE 1=1"
    params = []

    if query:
        sql += " AND content LIKE ?"
        params.append(f"%{query}%")

    if priority is not None:
        try:
            params.append(int(priority))
        except ValueError:
            params.append(1)
        sql += " AND priority = ?"

    sql += " ORDER BY id DESC"
    cursor.execute(sql, params)
    rows = cursor.fetchall()
    conn.close()

    data = []
    for row in rows:
        data.append({
            "id": row["id"],
            "content": row["content"],
            "create_time": row["create_time"],
            "priority": row["priority"],
            "completed": row["completed"] == 1,
            "synced": row["synced"] == 1,
        })

    return jsonify({"code": 0, "data": data})


@app.route("/todo/add", methods=["POST"])
def add_todo():
    data = request.get_json(silent=True) or {}
    content = str(data.get("content", "")).strip()

    if not content:
        return jsonify({"code": 400, "message": "content不能为空"}), 400

    priority = int(data.get("priority", 1))
    if priority not in [0, 1, 2]:
        priority = 1

    create_time = str(data.get("create_time", "")).strip()
    if not create_time:
        create_time = datetime.now().strftime("%Y-%m-%d %H:%M:%S")

    conn = get_db()
    cursor = conn.cursor()
    cursor.execute(
        "INSERT INTO todo (content, create_time, priority, completed, synced) VALUES (?, ?, ?, ?, ?)",
        (content, create_time, priority, 0, 1)
    )
    conn.commit()
    todo_id = cursor.lastrowid
    conn.close()

    return jsonify({"code": 0, "data": {"id": todo_id}})


@app.route("/todo/delete", methods=["POST"])
def delete_todo():
    data = request.get_json(silent=True) or {}
    todo_id = data.get("id")

    if todo_id is None:
        return jsonify({"code": 400, "message": "id不能为空"}), 400

    conn = get_db()
    cursor = conn.cursor()
    cursor.execute("DELETE FROM todo WHERE id = ?", (todo_id,))
    conn.commit()
    conn.close()

    return jsonify({"code": 0, "data": {"deleted": True}})


@app.route("/todo/update", methods=["POST"])
def update_todo():
    data = request.get_json(silent=True) or {}
    todo_id = data.get("id")
    content = str(data.get("content", "")).strip()

    if todo_id is None:
        return jsonify({"code": 400, "message": "id不能为空"}), 400
    if not content:
        return jsonify({"code": 400, "message": "content不能为空"}), 400

    priority = int(data.get("priority", 1))
    if priority not in [0, 1, 2]:
        priority = 1

    conn = get_db()
    cursor = conn.cursor()
    cursor.execute(
        "UPDATE todo SET content = ?, priority = ? WHERE id = ?",
        (content, priority, todo_id)
    )
    conn.commit()
    conn.close()

    return jsonify({"code": 0, "data": {"updated": True}})


@app.route("/todo/mark_completed", methods=["POST"])
def mark_completed():
    data = request.get_json(silent=True) or {}
    todo_id = data.get("id")

    if todo_id is None:
        return jsonify({"code": 400, "message": "id不能为空"}), 400

    conn = get_db()
    cursor = conn.cursor()
    cursor.execute("UPDATE todo SET completed = 1 WHERE id = ?", (todo_id,))
    conn.commit()
    conn.close()

    return jsonify({"code": 0, "data": {"completed": True}})


@app.route("/api/todos", methods=["POST"])
def sync_todos():
    data = request.get_json(silent=True) or {}
    todos = data if isinstance(data, list) else data.get("todos", [])

    conn = get_db()
    cursor = conn.cursor()
    synced_ids = []

    for todo in todos:
        content = str(todo.get("content", "")).strip()
        if not content:
            continue
        priority = int(todo.get("priority", 1))
        if priority not in [0, 1, 2]:
            priority = 1
        completed = 1 if todo.get("completed") else 0
        create_time = str(todo.get("create_time", "")).strip()
        if not create_time:
            create_time = datetime.now().strftime("%Y-%m-%d %H:%M:%S")
        cursor.execute(
            "INSERT INTO todo (content, create_time, priority, completed, synced) VALUES (?, ?, ?, ?, ?)",
            (content, create_time, priority, completed, 1)
        )
        synced_ids.append(cursor.lastrowid)

    conn.commit()
    conn.close()

    return jsonify({"code": 0, "data": {"synced": True, "ids": synced_ids}})


if __name__ == "__main__":
    init_db()
    app.run(host="0.0.0.0", port=5000, debug=True)
