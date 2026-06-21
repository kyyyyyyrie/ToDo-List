# ToDo List

一个 Android 待办事项应用，配套 Python Flask 后端，支持任务管理和同步。

## 功能特性

### Android 应用 (AndroidToDo/)
- 添加、编辑、删除待办事项，支持优先级（高/中/低）
- 标记完成状态
- 搜索和按优先级筛选
- 滑动删除并支持撤销
- 同步待办事项到后端服务器
- 进度统计显示
- 本地 SQLite 数据库存储

### 后端 API (backend/)
- 基于 Flask 的 REST API
- 接口列表：
  - `GET /todo/list` - 获取所有待办事项（支持搜索和优先级筛选）
  - `POST /todo/add` - 添加新待办事项
  - `POST /todo/update` - 更新待办事项
  - `POST /todo/delete` - 删除待办事项
  - `POST /todo/mark_completed` - 标记为已完成
  - `POST /api/todos` - 从移动应用同步待办事项
- SQLite 数据库存储

## 环境设置

### 后端
```bash
cd backend
pip install -r requirements.txt
python app.py
```
服务器运行在 `http://0.0.0.0:5000`

### Android
1. 使用 Android Studio 打开 `AndroidToDo/`
2. 修改 `MainActivity.java` 中的 `BASE_URL` 为你的后端 IP（默认：`http://10.0.2.2:5000/`）
3. 在模拟器或设备上运行应用

## API 使用说明

### 获取待办事项列表
```
GET /todo/list?q=搜索关键词&priority=0
```
- `q`: 搜索关键词（可选）
- `priority`: 优先级筛选（0=高, 1=中, 2=低）

### 添加待办事项
```
POST /todo/add
{
  "content": "任务内容",
  "priority": 1,
  "create_time": "2024-01-01 10:00:00"
}
```

### 同步待办事项
```
POST /api/todos
[
  {"content": "任务", "priority": 1, "completed": false, "create_time": "2024-01-01 10:00:00"}
]
```