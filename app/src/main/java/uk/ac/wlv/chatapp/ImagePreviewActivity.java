package uk.ac.wlv.chatapp;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import java.util.Objects;

public class ImagePreviewActivity extends AppCompatActivity {

    private ImageView imageViewPreview;
    private EditText editTextCaption;
    private FloatingActionButton buttonSendWithCaption;
    private String imageUriString;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_preview);

        Toolbar toolbar = findViewById(R.id.toolbar_preview);
        setSupportActionBar(toolbar);
        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("");

        imageViewPreview = findViewById(R.id.imageViewPreview);
        editTextCaption = findViewById(R.id.editTextCaption);
        buttonSendWithCaption = findViewById(R.id.buttonSendWithCaption);

        imageUriString = getIntent().getStringExtra("imageUri");
        if (imageUriString != null) {
            if (imageUriString.startsWith("drawable://")) {
                try {
                    // Handle the custom drawable resource path
                    int drawableId = Integer.parseInt(imageUriString.substring("drawable://".length()));
                    imageViewPreview.setImageResource(drawableId);
                } catch (NumberFormatException e) {
                    Log.e("ImagePreviewActivity", "Failed to parse drawable resource ID.", e);
                    Toast.makeText(this, "Failed to load image.", Toast.LENGTH_SHORT).show();
                }
            } else {
                // Handle the standard file URI path
                imageViewPreview.setImageURI(Uri.parse(imageUriString));
            }
        }

        buttonSendWithCaption.setOnClickListener(v -> sendImageAndCaption());
    }

    private void sendImageAndCaption() {
        String caption = editTextCaption.getText().toString().trim();
        Intent resultIntent = new Intent();
        resultIntent.putExtra("imageUri", imageUriString);
        resultIntent.putExtra("caption", caption);
        setResult(RESULT_OK, resultIntent);
        finish();
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}
