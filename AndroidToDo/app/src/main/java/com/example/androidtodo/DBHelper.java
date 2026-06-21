package com.example.androidtodo;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class DBHelper extends SQLiteOpenHelper {
    private static final String DB_NAME = "todo.db";
    private static final int DB_VERSION = 3;

    public static final String TABLE_TODO = "todo";
    public static final String COL_ID = "id";
    public static final String COL_CONTENT = "content";
    public static final String COL_CREATE_TIME = "create_time";
    public static final String COL_PRIORITY = "priority";
    public static final String COL_COMPLETED = "completed";
    public static final String COL_SYNCED = "synced";
    public static final String COL_DUE_DATE = "due_date";

    private static final String CREATE_TABLE =
            "CREATE TABLE IF NOT EXISTS " + TABLE_TODO + " (" +
                    COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    COL_CONTENT + " TEXT NOT NULL, " +
                    COL_CREATE_TIME + " TEXT NOT NULL, " +
                    COL_PRIORITY + " INTEGER DEFAULT 1, " +
                    COL_COMPLETED + " INTEGER DEFAULT 0, " +
                    COL_SYNCED + " INTEGER DEFAULT 0, " +
                    COL_DUE_DATE + " TEXT)";

    public DBHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            db.execSQL("ALTER TABLE " + TABLE_TODO + " ADD COLUMN " + COL_PRIORITY + " INTEGER DEFAULT 1");
            db.execSQL("ALTER TABLE " + TABLE_TODO + " ADD COLUMN " + COL_COMPLETED + " INTEGER DEFAULT 0");
            db.execSQL("ALTER TABLE " + TABLE_TODO + " ADD COLUMN " + COL_SYNCED + " INTEGER DEFAULT 0");
        }
        if (oldVersion < 3) {
            db.execSQL("ALTER TABLE " + TABLE_TODO + " ADD COLUMN " + COL_DUE_DATE + " TEXT");
        }
    }

    public long insertTodo(String content, int priority, String dueDate) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_CONTENT, content);
        values.put(COL_CREATE_TIME, nowText());
        values.put(COL_PRIORITY, priority);
        values.put(COL_COMPLETED, 0);
        values.put(COL_SYNCED, 0);
        values.put(COL_DUE_DATE, dueDate);
        long id = db.insert(TABLE_TODO, null, values);
        db.close();
        return id;
    }

    public boolean updateTodo(long id, String content, int priority, String dueDate) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_CONTENT, content);
        values.put(COL_PRIORITY, priority);
        values.put(COL_DUE_DATE, dueDate);
        int rows = db.update(TABLE_TODO, values, COL_ID + "=?", new String[]{String.valueOf(id)});
        db.close();
        return rows > 0;
    }

    public boolean toggleCompleted(long id) {
        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.query(TABLE_TODO, new String[]{COL_COMPLETED}, COL_ID + "=?", new String[]{String.valueOf(id)}, null, null, null);
        boolean currentCompleted = false;
        if (cursor.moveToFirst()) {
            currentCompleted = cursor.getInt(cursor.getColumnIndexOrThrow(COL_COMPLETED)) == 1;
        }
        cursor.close();
        
        ContentValues values = new ContentValues();
        values.put(COL_COMPLETED, currentCompleted ? 0 : 1);
        int rows = db.update(TABLE_TODO, values, COL_ID + "=?", new String[]{String.valueOf(id)});
        db.close();
        return rows > 0;
    }

    public boolean deleteTodo(long id) {
        SQLiteDatabase db = this.getWritableDatabase();
        int rows = db.delete(TABLE_TODO, COL_ID + "=?", new String[]{String.valueOf(id)});
        db.close();
        return rows > 0;
    }

    public List<TodoItem> searchTodos(String query, int priorityFilter) {
        List<TodoItem> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        StringBuilder sql = new StringBuilder("SELECT " + COL_ID + ", " + COL_CONTENT + ", " + COL_CREATE_TIME + ", " + COL_PRIORITY + ", " + COL_COMPLETED + ", " + COL_DUE_DATE + ", " + COL_SYNCED + " FROM " + TABLE_TODO + " WHERE 1=1");
        List<String> args = new ArrayList<>();

        if (query != null && !query.trim().isEmpty()) {
            sql.append(" AND ").append(COL_CONTENT).append(" LIKE ?");
            args.add("%" + query.trim() + "%");
        }

        if (priorityFilter >= 0) {
            sql.append(" AND ").append(COL_PRIORITY).append("=?");
            args.add(String.valueOf(priorityFilter));
        }

        sql.append(" ORDER BY ").append(COL_ID).append(" DESC");

        Cursor cursor = db.rawQuery(sql.toString(), args.toArray(new String[0]));
        while (cursor.moveToNext()) {
            list.add(cursorToItem(cursor));
        }
        cursor.close();
        db.close();
        return list;
    }

    public List<TodoItem> getAllTodos() {
        return searchTodos(null, -1);
    }

    public List<TodoItem> getUnsyncedTodos() {
        List<TodoItem> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_TODO,
                new String[]{COL_ID, COL_CONTENT, COL_CREATE_TIME, COL_PRIORITY, COL_COMPLETED, COL_DUE_DATE, COL_SYNCED},
                COL_SYNCED + "=?", new String[]{"0"},
                null, null, COL_ID + " DESC");
        while (cursor.moveToNext()) {
            list.add(cursorToItem(cursor));
        }
        cursor.close();
        db.close();
        return list;
    }

    public boolean markSynced(long id) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_SYNCED, 1);
        int rows = db.update(TABLE_TODO, values, COL_ID + "=?", new String[]{String.valueOf(id)});
        db.close();
        return rows > 0;
    }

    private TodoItem cursorToItem(Cursor cursor) {
        long id = cursor.getLong(cursor.getColumnIndexOrThrow(COL_ID));
        String content = cursor.getString(cursor.getColumnIndexOrThrow(COL_CONTENT));
        String createTime = cursor.getString(cursor.getColumnIndexOrThrow(COL_CREATE_TIME));
        int priority = cursor.getInt(cursor.getColumnIndexOrThrow(COL_PRIORITY));
        int completed = cursor.getInt(cursor.getColumnIndexOrThrow(COL_COMPLETED));
        int synced = cursor.getInt(cursor.getColumnIndexOrThrow(COL_SYNCED));
        String dueDate = null;
        int dueDateIndex = cursor.getColumnIndex(COL_DUE_DATE);
        if (dueDateIndex >= 0) {
            dueDate = cursor.getString(dueDateIndex);
        }
        return new TodoItem(id, content, createTime, priority, completed == 1, dueDate, synced == 1);
    }

    public TodoItem getTodoById(long id) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_TODO, null, COL_ID + "=?", new String[]{String.valueOf(id)}, null, null, null);
        TodoItem item = null;
        if (cursor.moveToFirst()) {
            item = cursorToItem(cursor);
        }
        cursor.close();
        db.close();
        return item;
    }

    private String nowText() {
        return new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                .format(new Date());
    }
}
