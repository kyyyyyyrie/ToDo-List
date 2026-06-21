package com.example.androidtodo;

public class SyncResponse {
    private int code;
    private SyncData data;

    public int getCode() {
        return code;
    }

    public boolean isSuccess() {
        return code == 0 && data != null && data.isSynced();
    }

    private static class SyncData {
        private boolean synced;

        public boolean isSynced() {
            return synced;
        }
    }
}
