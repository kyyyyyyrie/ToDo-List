# ToDo List

An Android ToDo List application with Python Flask backend for task management and synchronization.

## Features

### Android App (AndroidToDo/)
- Add, edit, delete todos with priority levels (high/medium/low)
- Mark todos as completed
- Search and filter by priority
- Swipe-to-delete with undo
- Sync todos to backend server
- Progress statistics display
- Local SQLite database storage

### Backend API (backend/)
- REST API built with Flask
- Endpoints:
  - `GET /todo/list` - Get all todos (supports search and priority filter)
  - `POST /todo/add` - Add new todo
  - `POST /todo/update` - Update existing todo
  - `POST /todo/delete` - Delete todo
  - `POST /todo/mark_completed` - Mark todo as completed
  - `POST /api/todos` - Sync todos from mobile app
- SQLite database storage

## Setup

### Backend
```bash
cd backend
pip install -r requirements.txt
python app.py
```
Server runs on `http://0.0.0.0:5000`

### Android
1. Open `AndroidToDo/` in Android Studio
2. Update `BASE_URL` in `MainActivity.java` to your backend IP (default: `http://10.0.2.2:5000/`)
3. Run the app on emulator or device

## API Usage

### List todos
```
GET /todo/list?q=search_text&priority=0
```
- `q`: Search query (optional)
- `priority`: Filter by priority (0=high, 1=medium, 2=low)

### Add todo
```
POST /todo/add
{
  "content": "Task content",
  "priority": 1,
  "create_time": "2024-01-01 10:00:00"
}
```

### Sync todos
```
POST /api/todos
[
  {"content": "Task", "priority": 1, "completed": false, "create_time": "2024-01-01 10:00:00"}
]
```