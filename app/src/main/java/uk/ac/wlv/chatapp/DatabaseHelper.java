package uk.ac.wlv.chatapp;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import java.util.ArrayList;
import java.util.List;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "UserManager.db";
    // NOTE: Version is 7 to support captions. If you don't have captions yet, you can use 6.
    private static final int DATABASE_VERSION = 7;

    private static final String TABLE_USER = "user";
    private static final String TABLE_MESSAGE = "message";

    private static final String COLUMN_USER_ID = "user_id";
    private static final String COLUMN_USER_NAME = "user_name";
    private static final String COLUMN_USER_EMAIL = "user_email";
    private static final String COLUMN_USER_PASSWORD = "user_password";

    private static final String COLUMN_MESSAGE_ID = "message_id";
    private static final String COLUMN_MESSAGE_CONTENT = "message_content";
    private static final String COLUMN_SENDER = "sender";
    private static final String COLUMN_RECEIVER = "receiver";
    private static final String COLUMN_TIMESTAMP = "timestamp";
    private static final String COLUMN_MESSAGE_TYPE = "message_type";
    private static final String COLUMN_MESSAGE_CAPTION = "message_caption";

    private final String CREATE_USER_TABLE = "CREATE TABLE " + TABLE_USER + "("
            + COLUMN_USER_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
            + COLUMN_USER_NAME + " TEXT,"
            + COLUMN_USER_EMAIL + " TEXT,"
            + COLUMN_USER_PASSWORD + " TEXT" + ")";

    private final String CREATE_MESSAGE_TABLE = "CREATE TABLE " + TABLE_MESSAGE + "("
            + COLUMN_MESSAGE_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
            + COLUMN_MESSAGE_CONTENT + " TEXT,"
            + COLUMN_SENDER + " TEXT,"
            + COLUMN_RECEIVER + " TEXT,"
            + COLUMN_TIMESTAMP + " INTEGER,"
            + COLUMN_MESSAGE_TYPE + " TEXT,"
            + COLUMN_MESSAGE_CAPTION + " TEXT" + ")";

    private final String DROP_USER_TABLE = "DROP TABLE IF EXISTS " + TABLE_USER;
    private final String DROP_MESSAGE_TABLE = "DROP TABLE IF EXISTS " + TABLE_MESSAGE;

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_USER_TABLE);
        db.execSQL(CREATE_MESSAGE_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL(DROP_USER_TABLE);
        db.execSQL(DROP_MESSAGE_TABLE);
        onCreate(db);
    }

    public void addUser(User user) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_USER_NAME, user.getName());
        values.put(COLUMN_USER_EMAIL, user.getEmail());
        values.put(COLUMN_USER_PASSWORD, user.getPassword());
        db.insert(TABLE_USER, null, values);
        db.close();
    }

    public boolean checkUser(String username) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_USER, new String[]{COLUMN_USER_ID},
                COLUMN_USER_NAME + " = ?",
                new String[]{username}, null, null, null);
        int cursorCount = cursor.getCount();
        cursor.close();
        db.close();
        return cursorCount > 0;
    }

    public boolean checkUser(String username, String password) {
        SQLiteDatabase db = this.getReadableDatabase();
        String selection = COLUMN_USER_NAME + " = ? AND " + COLUMN_USER_PASSWORD + " = ?";
        String[] selectionArgs = {username, password};
        Cursor cursor = db.query(TABLE_USER, new String[]{COLUMN_USER_ID},
                selection, selectionArgs, null, null, null);
        int cursorCount = cursor.getCount();
        cursor.close();
        db.close();
        return cursorCount > 0;
    }

    public List<User> getAllUsers() {
        List<User> userList = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_USER, new String[]{COLUMN_USER_NAME},
                null, null, null, null, null);

        if (cursor.moveToFirst()) {
            do {
                User user = new User();
                user.setName(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_USER_NAME)));
                userList.add(user);
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return userList;
    }

    public void addMessage(Message message) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_MESSAGE_CONTENT, message.getContent());
        values.put(COLUMN_SENDER, message.getSender());
        values.put(COLUMN_RECEIVER, message.getReceiver());
        values.put(COLUMN_TIMESTAMP, message.getTimestamp());
        values.put(COLUMN_MESSAGE_TYPE, message.getType().name());
        values.put(COLUMN_MESSAGE_CAPTION, message.getCaption());
        db.insert(TABLE_MESSAGE, null, values);
        db.close();
    }

    public List<Message> getMessages(String user1, String user2) {
        List<Message> messageList = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        String[] columns = {COLUMN_MESSAGE_CONTENT, COLUMN_SENDER, COLUMN_RECEIVER, COLUMN_TIMESTAMP, COLUMN_MESSAGE_TYPE, COLUMN_MESSAGE_CAPTION};
        Cursor cursor = db.query(TABLE_MESSAGE, columns,
                "(" + COLUMN_SENDER + " = ? AND " + COLUMN_RECEIVER + " = ?) OR ("
                        + COLUMN_SENDER + " = ? AND " + COLUMN_RECEIVER + " = ?)",
                new String[]{user1, user2, user2, user1}, null, null, COLUMN_TIMESTAMP + " ASC");

        if (cursor.moveToFirst()) {
            do {
                String typeString = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_MESSAGE_TYPE));
                Message.MessageType type = Message.MessageType.valueOf(typeString != null ? typeString : "TEXT");
                String caption = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_MESSAGE_CAPTION));

                Message message = new Message(
                        cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_MESSAGE_CONTENT)),
                        cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_SENDER)),
                        cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_RECEIVER)),
                        cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_TIMESTAMP)),
                        type,
                        caption
                );
                messageList.add(message);
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return messageList;
    }

    public Message getLastMessage(String user1, String user2) {
        Message message = null;
        SQLiteDatabase db = this.getReadableDatabase();
        String[] columns = {COLUMN_MESSAGE_CONTENT, COLUMN_SENDER, COLUMN_RECEIVER, COLUMN_TIMESTAMP, COLUMN_MESSAGE_TYPE, COLUMN_MESSAGE_CAPTION};
        Cursor cursor = db.query(TABLE_MESSAGE, columns,
                "(" + COLUMN_SENDER + " = ? AND " + COLUMN_RECEIVER + " = ?) OR ("
                        + COLUMN_SENDER + " = ? AND " + COLUMN_RECEIVER + " = ?)",
                new String[]{user1, user2, user2, user1}, null, null, COLUMN_TIMESTAMP + " DESC", "1");

        if (cursor.moveToFirst()) {
            String typeString = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_MESSAGE_TYPE));
            Message.MessageType type = Message.MessageType.valueOf(typeString != null ? typeString : "TEXT");
            String caption = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_MESSAGE_CAPTION));

            message = new Message(
                    cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_MESSAGE_CONTENT)),
                    cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_SENDER)),
                    cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_RECEIVER)),
                    cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_TIMESTAMP)),
                    type,
                    caption
            );
        }
        cursor.close();
        db.close();
        return message;
    }

    public void updateMessage(long timestamp, String newContent) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_MESSAGE_CONTENT, newContent);
        db.update(TABLE_MESSAGE, values, COLUMN_TIMESTAMP + " = ?", new String[]{String.valueOf(timestamp)});
        db.close();
    }

    public void updateMessageCaption(long timestamp, String newCaption) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_MESSAGE_CAPTION, newCaption);
        db.update(TABLE_MESSAGE, values, COLUMN_TIMESTAMP + " = ?", new String[]{String.valueOf(timestamp)});
        db.close();
    }

    public void deleteMessages(List<Long> timestamps) {
        if (timestamps == null || timestamps.isEmpty()) {
            return;
        }
        SQLiteDatabase db = this.getWritableDatabase();
        String[] whereArgs = new String[timestamps.size()];
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < timestamps.size(); i++) {
            whereArgs[i] = String.valueOf(timestamps.get(i));
            builder.append("?");
            if (i < timestamps.size() - 1) {
                builder.append(",");
            }
        }
        db.delete(TABLE_MESSAGE, COLUMN_TIMESTAMP + " IN (" + builder.toString() + ")", whereArgs);
        db.close();
    }
}

