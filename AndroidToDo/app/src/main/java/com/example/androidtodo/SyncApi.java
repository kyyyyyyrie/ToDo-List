package com.example.androidtodo;

import java.util.List;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface SyncApi {
    @POST("api/todos")
    Call<SyncResponse> syncTodos(@Body List<TodoItem> todos);
}
