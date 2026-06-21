package com.example.androidtodo;

public class TodoItem {
    public static final int PRIORITY_HIGH = 2;
    public static final int PRIORITY_MEDIUM = 1;
    public static final int PRIORITY_LOW = 0;

    private long id;
    private String content;
    private String createTime;
    private int priority;
    private boolean completed;
    private String dueDate;
    private boolean synced;

    public TodoItem() {
    }

    public TodoItem(long id, String content, String createTime, int priority, boolean completed, String dueDate, boolean synced) {
        this.id = id;
        this.content = content;
        this.createTime = createTime;
        this.priority = priority;
        this.completed = completed;
        this.dueDate = dueDate;
        this.synced = synced;
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public String getCreateTime() { return createTime; }
    public void setCreateTime(String createTime) { this.createTime = createTime; }
    public int getPriority() { return priority; }
    public void setPriority(int priority) { this.priority = priority; }
    public boolean isCompleted() { return completed; }
    public void setCompleted(boolean completed) { this.completed = completed; }
    public String getDueDate() { return dueDate; }
    public void setDueDate(String dueDate) { this.dueDate = dueDate; }
    public boolean isSynced() { return synced; }
    public void setSynced(boolean synced) { this.synced = synced; }

    public static String priorityToString(int priority) {
        if (priority == PRIORITY_HIGH) return "高优";
        if (priority == PRIORITY_MEDIUM) return "中优";
        return "低优";
    }

    public static int getPriorityColor(int priority) {
        if (priority == PRIORITY_HIGH) return 0xFFF44336;
        if (priority == PRIORITY_MEDIUM) return 0xFFFF9800;
        return 0xFF4CAF50;
    }

    public static int getPriorityBgRes(int priority) {
        if (priority == PRIORITY_HIGH) return R.drawable.priority_bg_high;
        if (priority == PRIORITY_MEDIUM) return R.drawable.priority_bg_medium;
        return R.drawable.priority_bg_low;
    }
}
