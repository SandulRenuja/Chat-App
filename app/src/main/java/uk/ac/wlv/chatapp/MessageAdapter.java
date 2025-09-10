package uk.ac.wlv.chatapp;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.RecyclerView;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public class MessageAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private List<Message> messageList;
    private final List<Message> messageListFull;
    private final String currentUsername;
    private final Context context;
    private final DatabaseHelper databaseHelper;
    private boolean isSelectionMode = false;
    private final List<Message> selectedMessages = new ArrayList<>();
    private final SelectionListener selectionListener;

    private static final int VIEW_TYPE_SENT_TEXT = 1;
    private static final int VIEW_TYPE_RECEIVED_TEXT = 2;
    private static final int VIEW_TYPE_SENT_IMAGE = 3;
    private static final int VIEW_TYPE_RECEIVED_IMAGE = 4;

    public MessageAdapter(Context context, List<Message> messageList, String currentUsername, SelectionListener listener) {
        this.context = context;
        this.messageList = messageList;
        this.messageListFull = new ArrayList<>(messageList);
        this.currentUsername = currentUsername;
        this.databaseHelper = new DatabaseHelper(context);
        this.selectionListener = listener;
    }

    @Override
    public int getItemViewType(int position) {
        Message message = messageList.get(position);
        boolean isSentByMe = message.getSender().equals(currentUsername);

        if (message.getType() == Message.MessageType.IMAGE) {
            return isSentByMe ? VIEW_TYPE_SENT_IMAGE : VIEW_TYPE_RECEIVED_IMAGE;
        } else {
            return isSentByMe ? VIEW_TYPE_SENT_TEXT : VIEW_TYPE_RECEIVED_TEXT;
        }
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view;
        switch (viewType) {
            case VIEW_TYPE_SENT_TEXT:
                view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_message_sent, parent, false);
                return new TextMessageViewHolder(view);
            case VIEW_TYPE_RECEIVED_TEXT:
                view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_message_received, parent, false);
                return new TextMessageViewHolder(view);
            case VIEW_TYPE_SENT_IMAGE:
                view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_message_sent_image, parent, false);
                return new ImageMessageViewHolder(view);
            case VIEW_TYPE_RECEIVED_IMAGE:
                view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_message_received_image, parent, false);
                return new ImageMessageViewHolder(view);
            default:
                throw new IllegalArgumentException("Invalid view type");
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Message message = messageList.get(position);
        String formattedTime = formatTimestamp(message.getTimestamp());

        if (holder instanceof TextMessageViewHolder) {
            TextMessageViewHolder textHolder = (TextMessageViewHolder) holder;
            textHolder.textViewMessage.setText(message.getContent());
            textHolder.textViewTimestamp.setText(formattedTime);
        } else if (holder instanceof ImageMessageViewHolder) {
            ImageMessageViewHolder imageHolder = (ImageMessageViewHolder) holder;
            imageHolder.textViewTimestamp.setText(formattedTime);

            if (message.getCaption() != null && !message.getCaption().isEmpty()) {
                imageHolder.textViewCaption.setText(message.getCaption());
                imageHolder.textViewCaption.setVisibility(View.VISIBLE);
            } else {
                imageHolder.textViewCaption.setVisibility(View.GONE);
            }

            String path = message.getContent();
            imageHolder.imageViewMessage.setImageURI(Uri.parse(path));
        }

        holder.itemView.setBackgroundColor(message.isSelected() ? Color.LTGRAY : Color.TRANSPARENT);

        holder.itemView.setOnLongClickListener(v -> {
            if (!isSelectionMode) {
                isSelectionMode = true;
                toggleSelection(message);
                selectionListener.onSelectionModeChanged(true, selectedMessages.size());
            }
            return true;
        });

        holder.itemView.setOnClickListener(v -> {
            if (isSelectionMode) {
                toggleSelection(message);
                selectionListener.onSelectionModeChanged(true, selectedMessages.size());
                if (selectedMessages.isEmpty()) {
                    isSelectionMode = false;
                    selectionListener.onSelectionModeChanged(false, 0);
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return messageList.size();
    }

    private String formatTimestamp(long timestamp) {
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
        return sdf.format(new Date(timestamp));
    }

    public static class TextMessageViewHolder extends RecyclerView.ViewHolder {
        TextView textViewMessage;
        TextView textViewTimestamp;
        public TextMessageViewHolder(@NonNull View itemView) {
            super(itemView);
            textViewMessage = itemView.findViewById(R.id.textViewMessage);
            textViewTimestamp = itemView.findViewById(R.id.textViewTimestamp);
        }
    }

    public static class ImageMessageViewHolder extends RecyclerView.ViewHolder {
        ImageView imageViewMessage;
        TextView textViewTimestamp;
        TextView textViewCaption;
        public ImageMessageViewHolder(@NonNull View itemView) {
            super(itemView);
            imageViewMessage = itemView.findViewById(R.id.imageViewMessage);
            textViewTimestamp = itemView.findViewById(R.id.textViewTimestamp);
            textViewCaption = itemView.findViewById(R.id.textViewCaption);
        }
    }


    /**
     * Prepares and launches the Android Share Intent for the selected message.
     */
    public void shareSelectedMessage() {
        if (selectedMessages.size() != 1) {
            return; // Safety check
        }
        Message messageToShare = selectedMessages.get(0);
        Intent shareIntent = new Intent(Intent.ACTION_SEND);

        if (messageToShare.getType() == Message.MessageType.TEXT) {
            shareIntent.setType("text/plain");
            shareIntent.putExtra(Intent.EXTRA_TEXT, messageToShare.getContent());
            context.startActivity(Intent.createChooser(shareIntent, "Share message via"));
        } else if (messageToShare.getType() == Message.MessageType.IMAGE) {
            shareIntent.setType("image/jpeg");
            String imageUriString = messageToShare.getContent();

            // The stored content is a URI string (e.g., "file:///...").
            // We need to parse it to a Uri object first.
            Uri imageUri = Uri.parse(imageUriString);

            // Create a File object from the URI's path.
            File imageFile = new File(Objects.requireNonNull(imageUri.getPath()));

            if (!imageFile.exists()) {
                Toast.makeText(context, "Image file not found", Toast.LENGTH_SHORT).show();
                return;
            }

            try {
                // Use FileProvider to get a secure content URI for the image file
                Uri shareableUri = FileProvider.getUriForFile(
                        context,
                        context.getApplicationContext().getPackageName() + ".provider",
                        imageFile
                );

                shareIntent.putExtra(Intent.EXTRA_STREAM, shareableUri);
                // If the image has a caption, share it as the text part of the message
                if (messageToShare.getCaption() != null && !messageToShare.getCaption().isEmpty()) {
                    shareIntent.putExtra(Intent.EXTRA_TEXT, messageToShare.getCaption());
                }
                // Grant permission to the receiving app to read the file
                shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                context.startActivity(Intent.createChooser(shareIntent, "Share image via"));
            } catch (IllegalArgumentException e) {
                Log.e("MessageAdapter", "FileProvider error for path: " + imageFile.getPath(), e);
                Toast.makeText(context, "Failed to share image. File path is invalid.", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void toggleSelection(Message message) {
        message.setSelected(!message.isSelected());
        if (message.isSelected()) {
            selectedMessages.add(message);
        } else {
            selectedMessages.remove(message);
        }
        notifyDataSetChanged();
    }

    public void deleteSelectedMessages() {
        List<Long> timestampsToDelete = new ArrayList<>();
        for (Message message : selectedMessages) {
            timestampsToDelete.add(message.getTimestamp());
        }
        databaseHelper.deleteMessages(timestampsToDelete);
        messageList.removeAll(selectedMessages);
        messageListFull.removeAll(selectedMessages);
        selectedMessages.clear();
        isSelectionMode = false;
        notifyDataSetChanged();
        selectionListener.onSelectionModeChanged(false, 0);
    }

    public void clearSelection() {
        for (Message message : messageList) {
            message.setSelected(false);
        }
        selectedMessages.clear();
        isSelectionMode = false;
        notifyDataSetChanged();
    }

    public void filter(String text) {
        messageList.clear();
        if (text.isEmpty()) {
            messageList.addAll(messageListFull);
        } else {
            text = text.toLowerCase();
            for (Message item : messageListFull) {
                if (item.getType() == Message.MessageType.TEXT && item.getContent().toLowerCase().contains(text)) {
                    messageList.add(item);
                }
            }
        }
        notifyDataSetChanged();
    }

    public void updateList(List<Message> newList) {
        messageList.clear();
        messageList.addAll(newList);
        messageListFull.clear();
        messageListFull.addAll(newList);
        notifyDataSetChanged();
    }

    public int getSelectedCount() {
        return selectedMessages.size();
    }

    public boolean isEditAllowed() {
        if (!selectedMessages.isEmpty()) {
            return selectedMessages.get(0).getType() == Message.MessageType.TEXT;
        }
        return false;
    }

    public void editSelectedMessage() {
        if (!isEditAllowed()) return;
        final Message message = selectedMessages.get(0);
        final int position = messageList.indexOf(message);
        showEditDialog(message, position);
    }

    private void showEditDialog(final Message message, final int position) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Edit Message");
        final View customLayout = LayoutInflater.from(context).inflate(R.layout.dialog_edit_message, null);
        builder.setView(customLayout);
        final EditText editTextNewMessage = customLayout.findViewById(R.id.editTextNewMessage);
        editTextNewMessage.setText(message.getContent());
        builder.setPositiveButton("Save", (dialog, which) -> {
            String newContent = editTextNewMessage.getText().toString().trim();
            if (!newContent.isEmpty()) {
                databaseHelper.updateMessage(message.getTimestamp(), newContent);
                message.setContent(newContent);
                notifyItemChanged(position);
            }
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    public interface SelectionListener {
        void onSelectionModeChanged(boolean isSelectionMode, int selectedCount);
    }
}
