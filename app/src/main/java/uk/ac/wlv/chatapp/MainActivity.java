package uk.ac.wlv.chatapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ListView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private ListView listViewUsers;
    private DatabaseHelper databaseHelper;
    private List<User> userList;
    private UserAdapter adapter;
    private Toolbar toolbar;
    private String currentUsername;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        listViewUsers = findViewById(R.id.listViewUsers);
        databaseHelper = new DatabaseHelper(this);
        userList = new ArrayList<>();

        currentUsername = getIntent().getStringExtra("USERNAME");
        getSupportActionBar().setTitle("Chats");

        adapter = new UserAdapter(this, userList);
        listViewUsers.setAdapter(adapter);

        listViewUsers.setOnItemClickListener((parent, view, position, id) -> {
            User selectedUser = userList.get(position);
            Intent intent = new Intent(MainActivity.this, ChatActivity.class);
            intent.putExtra("RECIPIENT_NAME", selectedUser.getName());
            startActivity(intent);
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadUsersAndConversations();
        adapter.notifyDataSetChanged();
    }

    private void loadUsersAndConversations() {
        List<User> allUsers = databaseHelper.getAllUsers();
        userList.clear();

        for (User user : allUsers) {
            if (user != null && user.getName() != null && !user.getName().equals(currentUsername)) {
                // This line will no longer cause an error
                Message lastMessage = databaseHelper.getLastMessage(currentUsername, user.getName());
                if (lastMessage != null) {
                    user.setLastMessage(lastMessage.getContent());
                    user.setLastMessageTimestamp(lastMessage.getTimestamp());
                }
                userList.add(user);
            }
        }

        Collections.sort(userList, (u1, u2) -> Long.compare(u2.getLastMessageTimestamp(), u1.getLastMessageTimestamp()));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_logout) {
            getSharedPreferences("ChatApp", MODE_PRIVATE).edit().remove("USERNAME").apply();

            Intent intent = new Intent(MainActivity.this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
