package uk.ac.wlv.chatapp;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class UserAdapter extends ArrayAdapter<User> {

    public UserAdapter(@NonNull Context context, @NonNull List<User> users) {
        super(context, 0, users);
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        User user = getItem(position);

        if (convertView == null) {
            // This line causes the error if item_user.xml does not exist.
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.list_item_user, parent, false);
        }

        TextView tvName = convertView.findViewById(R.id.textViewUserName);
        TextView tvLastMessage = convertView.findViewById(R.id.textViewLastMessage);
        TextView tvTimestamp = convertView.findViewById(R.id.textViewTimestamp);

        tvName.setText(user.getName());

        if (user.getLastMessage() != null) {
            if (user.getLastMessage().startsWith("drawable://") || user.getLastMessage().contains("content://") || user.getLastMessage().contains("file://") || user.getLastMessage().contains("/storage/")) {
                tvLastMessage.setText("📷 Photo");
            } else {
                tvLastMessage.setText(user.getLastMessage());
            }

            tvTimestamp.setText(formatTimestamp(user.getLastMessageTimestamp()));
        } else {
            tvLastMessage.setText("No messages yet");
            tvTimestamp.setText("");
        }

        return convertView;
    }

    private String formatTimestamp(long timestamp) {
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
        return sdf.format(new Date(timestamp));
    }
}