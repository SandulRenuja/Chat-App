package uk.ac.wlv.chatapp;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.AdapterView;
import android.widget.GridView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import java.util.ArrayList;
import java.util.Objects;

public class GalleryActivity extends AppCompatActivity {

    private GridView galleryGridView;
    private ArrayList<String> imagePaths;
    private GalleryImageAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gallery);

        // --- Toolbar Setup ---
        Toolbar toolbar = findViewById(R.id.toolbar_gallery);
        setSupportActionBar(toolbar);
        Objects.requireNonNull(getSupportActionBar()).setTitle("Select photo");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        // --- End Toolbar Setup ---

        galleryGridView = findViewById(R.id.galleryGridView);
        imagePaths = new ArrayList<>();

        loadImages();

        adapter = new GalleryImageAdapter(this, imagePaths);
        galleryGridView.setAdapter(adapter);

        galleryGridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String selectedImagePath = imagePaths.get(position);
                Intent resultIntent = new Intent();
                resultIntent.putExtra("selectedImage", selectedImagePath);
                setResult(RESULT_OK, resultIntent);
                finish();
            }
        });
    }

    /**
     * Handles the click on the toolbar's back button.
     */
    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed(); // Go back to the previous screen
        return true;
    }

    private void loadImages() {
        // Load images from the device's external storage
        Uri uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
        String[] projection = {MediaStore.MediaColumns.DATA};
        Cursor cursor = getContentResolver().query(uri, projection, null, null, MediaStore.Images.Media.DATE_TAKEN + " DESC");

        if (cursor != null) {
            while (cursor.moveToNext()) {
                int columnIndex = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATA);
                imagePaths.add(cursor.getString(columnIndex));
            }
            cursor.close();
        }
    }
}