package uk.ac.wlv.chatapp;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.view.ActionMode;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;

public class ChatActivity extends AppCompatActivity implements MessageAdapter.SelectionListener {

    private Toolbar toolbar;
    private RecyclerView recyclerViewMessages;
    private EditText editTextMessage;
    private Button buttonSend;
    private ImageButton buttonAttach, buttonCamera;
    private MessageAdapter messageAdapter;
    private List<Message> messageList;
    private DatabaseHelper databaseHelper;
    private String currentUsername;
    private String recipientName;
    private ActionMode actionMode;

    private static final int PERMISSION_REQUEST_CODE = 100;
    private static final int GALLERY_REQUEST_CODE = 101;
    private static final int CAMERA_REQUEST_CODE = 102;
    private static final int IMAGE_PREVIEW_REQUEST_CODE = 103;
    private String currentPhotoPath;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        try {
            toolbar = findViewById(R.id.toolbar);
            setSupportActionBar(toolbar);
            Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);

            recipientName = getIntent().getStringExtra("RECIPIENT_NAME");

            if (recipientName != null) {
                getSupportActionBar().setTitle(recipientName);
            } else {
                Log.e("ChatActivity", "Recipient name is null.");
                Toast.makeText(this, "Error: Recipient not found.", Toast.LENGTH_SHORT).show();
                finish(); // Close the activity if recipientName is not available
                return;
            }

            SharedPreferences prefs = getSharedPreferences("ChatApp", MODE_PRIVATE);
            currentUsername = prefs.getString("USERNAME", "Unknown");

            recyclerViewMessages = findViewById(R.id.recyclerViewMessages);
            editTextMessage = findViewById(R.id.editTextMessage);
            buttonSend = findViewById(R.id.buttonSend);
            buttonAttach = findViewById(R.id.buttonAttach);
            buttonCamera = findViewById(R.id.buttonCamera);

            databaseHelper = new DatabaseHelper(this);
            messageList = new ArrayList<>();

            messageAdapter = new MessageAdapter(this, messageList, currentUsername, this);
            recyclerViewMessages.setLayoutManager(new LinearLayoutManager(this));
            recyclerViewMessages.setAdapter(messageAdapter);

            loadMessages();

            buttonSend.setOnClickListener(v -> sendMessage());

            buttonAttach.setOnClickListener(v -> {
                String permission = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU ?
                        Manifest.permission.READ_MEDIA_IMAGES :
                        Manifest.permission.READ_EXTERNAL_STORAGE;
                if (checkPermissions(permission)) {
                    openGallery();
                } else {
                    requestPermissions(new String[]{permission});
                }
            });

            buttonCamera.setOnClickListener(v -> {
                if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
                    if (checkPermissions(Manifest.permission.CAMERA) && checkPermissions(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                        openCamera();
                    } else {
                        requestPermissions(new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE});
                    }
                } else {
                    if (checkPermissions(Manifest.permission.CAMERA)) {
                        openCamera();
                    } else {
                        requestPermissions(new String[]{Manifest.permission.CAMERA});
                    }
                }
            });
        } catch (Exception e) {
            Log.e("ChatActivity", "Error in onCreate", e);
            Toast.makeText(this, "An error occurred while opening the chat.", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private boolean checkPermissions(String permission) {
        return ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestPermissions(String[] permissions) {
        ActivityCompat.requestPermissions(this, permissions, PERMISSION_REQUEST_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            boolean allGranted = true;
            if (grantResults.length > 0) {
                for (int grantResult : grantResults) {
                    if (grantResult != PackageManager.PERMISSION_GRANTED) {
                        allGranted = false;
                        break;
                    }
                }
            } else {
                allGranted = false;
            }

            if (allGranted) {
                String firstPermission = permissions[0];
                if (firstPermission.equals(Manifest.permission.READ_EXTERNAL_STORAGE) ||
                        (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && firstPermission.equals(Manifest.permission.READ_MEDIA_IMAGES))) {
                    openGallery();
                } else if (firstPermission.equals(Manifest.permission.CAMERA)) {
                    openCamera();
                }
            } else {
                Toast.makeText(this, "Permission Denied", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void openGallery() {
        Intent intent = new Intent(this, GalleryActivity.class);
        startActivityForResult(intent, GALLERY_REQUEST_CODE);
    }

    private void openCamera() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                Toast.makeText(this, "Error creating image file", Toast.LENGTH_SHORT).show();
            }
            if (photoFile != null) {
                Uri photoURI = FileProvider.getUriForFile(this,
                        getApplicationContext().getPackageName() + ".provider",
                        photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                startActivityForResult(takePictureIntent, CAMERA_REQUEST_CODE);
            }
        }
    }

    private File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", java.util.Locale.getDefault()).format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(imageFileName, ".jpg", storageDir);
        currentPhotoPath = image.getAbsolutePath();
        return image;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK) {
            String imagePathString = null;

            if (requestCode == GALLERY_REQUEST_CODE && data != null) {
                imagePathString = data.getStringExtra("selectedImage");
            } else if (requestCode == CAMERA_REQUEST_CODE) {
                imagePathString = currentPhotoPath;
            }

            if (imagePathString != null) {
                Intent previewIntent = new Intent(this, ImagePreviewActivity.class);
                String uriString;
                if (imagePathString.startsWith("drawable://")) {
                    // If it's a drawable resource, pass the custom path directly.
                    uriString = imagePathString;
                } else {
                    // If it's a file path, convert it to a file URI string.
                    Uri imageUri = Uri.fromFile(new File(imagePathString));
                    uriString = imageUri.toString();
                }
                previewIntent.putExtra("imageUri", uriString);
                startActivityForResult(previewIntent, IMAGE_PREVIEW_REQUEST_CODE);
            }
        }

        if (requestCode == IMAGE_PREVIEW_REQUEST_CODE && resultCode == RESULT_OK && data != null) {
            String imageUri = data.getStringExtra("imageUri");
            String caption = data.getStringExtra("caption");
            sendImageMessage(imageUri, caption);
        }
    }

    private void loadMessages() {
        List<Message> allMessages = databaseHelper.getMessages(currentUsername, recipientName);
        messageAdapter.updateList(allMessages);
        if (!allMessages.isEmpty()) {
            recyclerViewMessages.scrollToPosition(allMessages.size() - 1);
        }
    }

    private void sendMessage() {
        String content = editTextMessage.getText().toString().trim();
        if (!content.isEmpty()) {
            long timestamp = System.currentTimeMillis();
            Message message = new Message(content, currentUsername, recipientName, timestamp, Message.MessageType.TEXT);
            databaseHelper.addMessage(message);
            loadMessages();
        }
        editTextMessage.setText("");
    }

    private void sendImageMessage(String imageUri, String caption) {
        long timestamp = System.currentTimeMillis();
        Message message = new Message(imageUri, currentUsername, recipientName, timestamp, Message.MessageType.IMAGE, caption);
        databaseHelper.addMessage(message);
        loadMessages();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.chat_menu, menu);
        MenuItem searchItem = menu.findItem(R.id.action_search);
        SearchView searchView = (SearchView) searchItem.getActionView();

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) { return false; }
            @Override
            public boolean onQueryTextChange(String newText) {
                messageAdapter.filter(newText);
                return false;
            }
        });
        return true;
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    @Override
    public void onSelectionModeChanged(boolean isSelectionMode, int selectedCount) {
        if (isSelectionMode) {
            if (actionMode == null) {
                actionMode = startSupportActionMode(actionModeCallback);
            }
            if (actionMode != null) {
                actionMode.setTitle(selectedCount + " selected");
                actionMode.invalidate();
            }
        } else {
            if (actionMode != null) {
                actionMode.finish();
            }
        }
    }

    private final ActionMode.Callback actionModeCallback = new ActionMode.Callback() {
        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            mode.getMenuInflater().inflate(R.menu.chat_context_menu, menu);
            return true;
        }

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            boolean singleItemSelected = messageAdapter.getSelectedCount() == 1;

            MenuItem editItem = menu.findItem(R.id.action_edit);
            if (editItem != null) {
                editItem.setVisible(singleItemSelected && messageAdapter.isEditAllowed());
            }

            MenuItem shareItem = menu.findItem(R.id.action_share);
            if (shareItem != null) {
                shareItem.setVisible(singleItemSelected);
            }
            return true;
        }

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            int itemId = item.getItemId();
            if (itemId == R.id.action_delete) {
                messageAdapter.deleteSelectedMessages();
                mode.finish();
                return true;
            } else if (itemId == R.id.action_edit) {
                messageAdapter.editSelectedMessage();
                mode.finish();
                return true;
            } else if (itemId == R.id.action_share) {
                messageAdapter.shareSelectedMessage();
                mode.finish();
                return true;
            }
            return false;
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {
            actionMode = null;
            messageAdapter.clearSelection();
        }
    };
}
