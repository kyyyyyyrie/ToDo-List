package com.example.androidtodo;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.chip.Chip;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class MainActivity extends Activity {

    private static final String BASE_URL = "http://10.0.2.2:5000/";

    private EditText inputEdit;
    private Spinner prioritySpinner;
    private Button addBtn;
    private Button syncBtn;
    private TextInputEditText searchEdit;
    private RecyclerView todoRecyclerView;
    private View emptyContainer;
    private View loadingContainer;
    private com.google.android.material.floatingactionbutton.FloatingActionButton fabAdd;
    private Chip chipAll, chipHigh, chipMedium, chipLow;
    private TextView tvTotalCount, tvCompletedCount, tvProgress;
    private View dividerStats;
    private ProgressBar progressBar;

    private DBHelper dbHelper;
    private TodoAdapter adapter;
    private List<TodoItem> todoItems = new ArrayList<>();
    private List<TodoItem> filteredItems = new ArrayList<>();
    private Handler mainHandler = new Handler(Looper.getMainLooper());
    private ExecutorService executor = Executors.newSingleThreadExecutor();
    private int currentFilter = TodoItem.PRIORITY_LOW;
    private String searchQuery = "";
    private TodoItem editingItem = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        dbHelper = new DBHelper(this);
        inputEdit = findViewById(R.id.inputEdit);
        prioritySpinner = findViewById(R.id.prioritySpinner);
        addBtn = findViewById(R.id.addBtn);
        syncBtn = findViewById(R.id.syncBtn);
        searchEdit = findViewById(R.id.searchEdit);
        todoRecyclerView = findViewById(R.id.todoRecyclerView);
        emptyContainer = findViewById(R.id.tvEmpty);
        loadingContainer = findViewById(R.id.loadingIndicator);
        fabAdd = findViewById(R.id.fabAdd);
        chipAll = findViewById(R.id.chipAll);
        chipHigh = findViewById(R.id.chipHigh);
        chipMedium = findViewById(R.id.chipMedium);
        chipLow = findViewById(R.id.chipLow);
        tvTotalCount = findViewById(R.id.tvTotalCount);
        tvCompletedCount = findViewById(R.id.tvCompletedCount);
        tvProgress = findViewById(R.id.tvProgress);
        progressBar = findViewById(R.id.progressRing);
        dividerStats = findViewById(R.id.dividerStats);

        todoRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new TodoAdapter();
        todoRecyclerView.setAdapter(adapter);

        setupChips();

        addBtn.setOnClickListener(v -> addTodo());
        fabAdd.setOnClickListener(v -> addTodo());
        syncBtn.setOnClickListener(v -> syncToServer());

        searchEdit.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                searchQuery = s.toString();
                applyFilters();
            }

            @Override
            public void afterTextChanged(android.text.Editable s) {}
        });

        updateStats();
        loadTodos();
    }

    private void setupChips() {
        chipAll.setOnCheckedChangeListener((chip, checked) -> {
            if (checked) { currentFilter = -1; applyFilters(); }
        });
        chipHigh.setOnCheckedChangeListener((chip, checked) -> {
            if (checked) { currentFilter = TodoItem.PRIORITY_HIGH; applyFilters(); }
        });
        chipMedium.setOnCheckedChangeListener((chip, checked) -> {
            if (checked) { currentFilter = TodoItem.PRIORITY_MEDIUM; applyFilters(); }
        });
        chipLow.setOnCheckedChangeListener((chip, checked) -> {
            if (checked) { currentFilter = TodoItem.PRIORITY_LOW; applyFilters(); }
        });
        chipAll.setChecked(true);
    }

    private void updateStats() {
        int total = todoItems.size();
        int completed = 0;
        for (TodoItem item : todoItems) {
            if (item.isCompleted()) completed++;
        }
        int progress = total > 0 ? (completed * 100 / total) : 0;

        tvTotalCount.setText(String.valueOf(total));
        tvCompletedCount.setText(String.valueOf(completed));
        tvProgress.setText(progress + "%");
        if (progressBar != null) {
            progressBar.setProgress(progress);
            progressBar.setSecondaryProgress(progress > 0 ? progress + 10 : 0);
        }
    }

    private void applyFilters() {
        filteredItems.clear();
        for (TodoItem item : todoItems) {
            boolean matchesSearch = searchQuery.isEmpty() ||
                    item.getContent().toLowerCase().contains(searchQuery.toLowerCase());
            boolean matchesPriority = currentFilter == -1 || item.getPriority() == currentFilter;
            if (matchesSearch && matchesPriority) {
                filteredItems.add(item);
            }
        }
        adapter.notifyDataSetChanged();
        updateStats();
        boolean empty = filteredItems.isEmpty();
        if (emptyContainer != null) emptyContainer.setVisibility(empty ? View.VISIBLE : View.GONE);
        todoRecyclerView.setVisibility(empty ? View.GONE : View.VISIBLE);
    }

    private void loadTodos() {
        showSkeletonLoading(true);
        executor.execute(() -> {
            todoItems = dbHelper.getAllTodos();
            mainHandler.post(() -> {
                showSkeletonLoading(false);
                applyFilters();
            });
        });
    }

    private void showSkeletonLoading(boolean show) {
        if (loadingContainer != null) {
            loadingContainer.setVisibility(show ? View.VISIBLE : View.GONE);
        }
        if (todoRecyclerView != null) {
            todoRecyclerView.setVisibility(show ? View.GONE : (filteredItems.isEmpty() ? View.GONE : View.VISIBLE));
        }
        if (emptyContainer != null) {
            if (show) emptyContainer.setVisibility(View.GONE);
            else if (filteredItems.isEmpty()) emptyContainer.setVisibility(View.VISIBLE);
        }
    }

    private void addTodo() {
        String content = inputEdit.getText().toString().trim();
        if (content.isEmpty()) {
            Toast.makeText(this, R.string.input_empty, Toast.LENGTH_SHORT).show();
            return;
        }
        int priority = prioritySpinner.getSelectedItemPosition();
        long id = dbHelper.insertTodo(content, priority, null);
        if (id != -1) {
            inputEdit.setText("");
            loadTodos();
            Toast.makeText(this, R.string.add_success, Toast.LENGTH_SHORT).show();
        }
    }

    private void syncToServer() {
        executor.execute(() -> {
            List<TodoItem> unsynced = dbHelper.getUnsyncedTodos();
            mainHandler.post(() -> {
                if (unsynced.isEmpty()) {
                    Toast.makeText(this, R.string.nothing_to_sync, Toast.LENGTH_SHORT).show();
                    return;
                }
                syncBtn.setEnabled(false);
                syncBtn.setText(R.string.skeleton_loading);
                Retrofit retrofit = new Retrofit.Builder()
                        .baseUrl(BASE_URL)
                        .addConverterFactory(GsonConverterFactory.create())
                        .build();
                SyncApi api = retrofit.create(SyncApi.class);
                Call<SyncResponse> call = api.syncTodos(unsynced);
                call.enqueue(new Callback<SyncResponse>() {
                    @Override
                    public void onResponse(Call<SyncResponse> call, Response<SyncResponse> response) {
                        syncBtn.setEnabled(true);
                        syncBtn.setText(R.string.btn_sync);
                        if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                            for (TodoItem item : unsynced) {
                                dbHelper.markSynced(item.getId());
                            }
                            loadTodos();
                            Toast.makeText(MainActivity.this, R.string.sync_success, Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(MainActivity.this, R.string.sync_fail, Toast.LENGTH_SHORT).show();
                        }
                    }
                    @Override
                    public void onFailure(Call<SyncResponse> call, Throwable t) {
                        syncBtn.setEnabled(true);
                        syncBtn.setText(R.string.btn_sync);
                        Toast.makeText(MainActivity.this, R.string.sync_fail, Toast.LENGTH_SHORT).show();
                    }
                });
            });
        });
    }

    private void deleteTodo(int position) {
        TodoItem item = filteredItems.get(position);
        if (dbHelper.deleteTodo(item.getId())) {
            todoItems.remove(item);
            applyFilters();
            Snackbar snackbar = Snackbar.make(todoRecyclerView, R.string.delete_success, Snackbar.LENGTH_LONG);
            snackbar.setAction(R.string.btn_undo, v -> {
                dbHelper.insertTodo(item.getContent(), item.getPriority(), item.getDueDate());
                loadTodos();
            });
            snackbar.show();
        }
    }

    private void showEditDialog(final TodoItem item) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.dialog_title_edit);

        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_edit_todo, null);
        final EditText editText = dialogView.findViewById(R.id.editContent);
        final Spinner spinner = dialogView.findViewById(R.id.editPrioritySpinner);

        editText.setText(item.getContent());
        spinner.setSelection(item.getPriority());

        builder.setView(dialogView);

        builder.setPositiveButton(R.string.btn_save, (dialog, which) -> {
            String newContent = editText.getText().toString().trim();
            int newPriority = spinner.getSelectedItemPosition();
            if (!newContent.isEmpty()) {
                dbHelper.updateTodo(item.getId(), newContent, newPriority, item.getDueDate());
                loadTodos();
                Toast.makeText(this, R.string.toast_saved, Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton(R.string.btn_cancel, null);
        builder.show();
    }

    private void toggleCompleted(TodoItem item, int position) {
        boolean newState = !item.isCompleted();
        dbHelper.toggleCompleted(item.getId());
        item.setCompleted(newState);
        adapter.notifyItemChanged(position);
        updateStats();
        Toast.makeText(this, newState ? R.string.toast_done : R.string.toast_undone, Toast.LENGTH_SHORT).show();
    }

    private void setupSwipeToDelete() {
        new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(0,
                ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
            @Override
            public boolean onMove(@NonNull RecyclerView rv, @NonNull RecyclerView.ViewHolder vh, @NonNull RecyclerView.ViewHolder target) {
                return false;
            }
            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder vh, int dir) {
                int pos = vh.getBindingAdapterPosition();
                if (pos != RecyclerView.NO_POSITION) {
                    deleteTodo(pos);
                }
            }
        }).attachToRecyclerView(todoRecyclerView);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdown();
    }

    private int getPriorityBarBg(int priority) {
        if (priority == TodoItem.PRIORITY_HIGH) return R.drawable.priority_bar_high;
        if (priority == TodoItem.PRIORITY_MEDIUM) return R.drawable.priority_bar_medium;
        return R.drawable.priority_bar_low;
    }

    private class TodoAdapter extends RecyclerView.Adapter<TodoViewHolder> {

        @NonNull
        @Override
        public TodoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(MainActivity.this).inflate(R.layout.item_todo, parent, false);
            return new TodoViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull TodoViewHolder holder, int position) {
            final TodoItem item = filteredItems.get(position);
            holder.contentText.setText(item.getContent());

            String timeText = item.getCreateTime();
            if (item.getDueDate() != null && !item.getDueDate().isEmpty()) {
                holder.dueDateText.setText(item.getDueDate());
                holder.dueDateText.setVisibility(View.VISIBLE);
                holder.dueDateIcon.setVisibility(View.VISIBLE);
                timeText = timeText + " · " + item.getDueDate();
            } else {
                holder.dueDateText.setVisibility(View.GONE);
                holder.dueDateIcon.setVisibility(View.GONE);
            }

            holder.timeText.setText(timeText);
            holder.priorityBar.setBackgroundResource(getPriorityBarBg(item.getPriority()));

            if (item.isCompleted()) {
                holder.contentText.setAlpha(0.5f);
                holder.priorityBar.setAlpha(0.5f);
            } else {
                holder.contentText.setAlpha(1.0f);
                holder.priorityBar.setAlpha(1.0f);
            }

            holder.checkCompleted.setOnCheckedChangeListener(null);
            holder.checkCompleted.setChecked(item.isCompleted());
            holder.checkCompleted.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked != item.isCompleted()) {
                    buttonView.post(() -> {
                        int pos = holder.getBindingAdapterPosition();
                        if (pos != RecyclerView.NO_POSITION) {
                            MainActivity.this.toggleCompleted(item, pos);
                        }
                    });
                }
            });

            holder.editBtn.setOnClickListener(v -> showEditDialog(item));
            holder.deleteBtn.setOnClickListener(v -> deleteTodo(holder.getBindingAdapterPosition()));
        }

        @Override
        public int getItemCount() {
            return filteredItems.size();
        }
    }

    private static class TodoViewHolder extends RecyclerView.ViewHolder {
        TextView contentText;
        CheckBox checkCompleted;
        View priorityBar;
        ImageButton editBtn;
        ImageButton deleteBtn;
        TextView timeText;
        TextView dueDateText;
        ImageView dueDateIcon;

        TodoViewHolder(View view) {
            super(view);
            contentText = view.findViewById(R.id.contentText);
            checkCompleted = view.findViewById(R.id.checkCompleted);
            priorityBar = view.findViewById(R.id.priorityBar);
            editBtn = view.findViewById(R.id.editBtn);
            deleteBtn = view.findViewById(R.id.deleteBtn);
            timeText = view.findViewById(R.id.timeText);
            dueDateText = view.findViewById(R.id.dueDateText);
            dueDateIcon = view.findViewById(R.id.dueDateIcon);
        }
    }
}
