package uk.ac.wlv.chatapp;

public class Message {
    public enum MessageType {
        TEXT, IMAGE
    }

    private String content;
    private String sender;
    private String receiver;
    private long timestamp;
    private MessageType type;
    private String caption; // New field for the caption
    private boolean isSelected = false;

    // Constructor for text messages (no caption)
    public Message(String content, String sender, String receiver, long timestamp, MessageType type) {
        this.content = content;
        this.sender = sender;
        this.receiver = receiver;
        this.timestamp = timestamp;
        this.type = type;
        this.caption = null; // Ensure caption is null for text messages
    }

    // Overloaded constructor for image messages (with potential caption)
    public Message(String content, String sender, String receiver, long timestamp, MessageType type, String caption) {
        this.content = content;
        this.sender = sender;
        this.receiver = receiver;
        this.timestamp = timestamp;
        this.type = type;
        this.caption = caption;
    }

    // Getters and Setters
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public String getSender() { return sender; }
    public String getReceiver() { return receiver; }
    public long getTimestamp() { return timestamp; }
    public MessageType getType() { return type; }
    public String getCaption() { return caption; }
    public void setCaption(String caption) { this.caption = caption; }
    public boolean isSelected() { return isSelected; }
    public void setSelected(boolean selected) { isSelected = selected; }
}
