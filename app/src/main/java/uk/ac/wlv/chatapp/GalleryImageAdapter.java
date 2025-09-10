package uk.ac.wlv.chatapp;

import android.content.Context;
import android.net.Uri;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import java.io.File;
import java.util.ArrayList;

public class GalleryImageAdapter extends BaseAdapter {

    private final Context context;
    private final ArrayList<String> imagePaths;

    public GalleryImageAdapter(Context context, ArrayList<String> imagePaths) {
        this.context = context;
        this.imagePaths = imagePaths;
    }

    @Override
    public int getCount() {
        return imagePaths.size();
    }

    @Override
    public Object getItem(int position) {
        return imagePaths.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ImageView imageView;
        if (convertView == null) {
            imageView = new ImageView(context);
            // Increased the size of the ImageView for larger photos
            imageView.setLayoutParams(new GridView.LayoutParams(350, 350));
            imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
            imageView.setPadding(8, 8, 8, 8);
        } else {
            imageView = (ImageView) convertView;
        }

        String path = imagePaths.get(position);

        if (path.startsWith("drawable://")) {
            int drawableId = Integer.parseInt(path.substring("drawable://".length()));
            imageView.setImageResource(drawableId);
        } else {
            imageView.setImageURI(Uri.fromFile(new File(path)));
        }

        return imageView;
    }
}
