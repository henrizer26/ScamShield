package com.group2.scamshield.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import com.group2.scamshield.models.ScamModel;
import com.group2.scamshield.utils.DateFormats;
import java.util.ArrayList;
import java.util.List;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "scamshield.db";
    private static final int DATABASE_VERSION = 1;

    // Table name
    private static final String TABLE_SCAMS = "scams";

    // Columns
    private static final String COLUMN_ID = "id";
    private static final String COLUMN_PHONE = "phone_number";
    private static final String COLUMN_MESSAGE = "message";
    private static final String COLUMN_SCAM_TYPE = "scam_type";
    private static final String COLUMN_DATE_TIME = "date_time";
    private static final String COLUMN_IS_REPORTED = "is_reported";

    // Create table SQL
    private static final String CREATE_TABLE_SCAMS =
            "CREATE TABLE " + TABLE_SCAMS + "("
                    + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + COLUMN_PHONE + " TEXT,"
                    + COLUMN_MESSAGE + " TEXT,"
                    + COLUMN_SCAM_TYPE + " TEXT,"
                    + COLUMN_DATE_TIME + " TEXT,"
                    + COLUMN_IS_REPORTED + " INTEGER DEFAULT 0"
                    + ")";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_TABLE_SCAMS);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_SCAMS);
        onCreate(db);
    }

    // ============ CRUD OPERATIONS ============

    // 1. INSERT - Add new scam
    public long addScam(String phoneNumber, String message, String scamType,
                        long timestampMillis) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_PHONE, phoneNumber);
        values.put(COLUMN_MESSAGE, message);
        values.put(COLUMN_SCAM_TYPE, scamType);
        values.put(COLUMN_DATE_TIME, DateFormats.stamp(timestampMillis));
        values.put(COLUMN_IS_REPORTED, 0);

        return db.insert(TABLE_SCAMS, null, values);
    }

    // 2. READ - Get all scams, newest first
    public List<ScamModel> getAllScams() {
        List<ScamModel> scamList = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.query(TABLE_SCAMS, null, null, null, null, null, COLUMN_ID + " DESC");

        if (cursor.moveToFirst()) {
            do {
                int id = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ID));
                String phone = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_PHONE));
                String message = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_MESSAGE));
                String scamType = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_SCAM_TYPE));
                String dateTime = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_DATE_TIME));
                boolean isReported = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_IS_REPORTED)) == 1;

                scamList.add(new ScamModel(id, phone, message, scamType, dateTime, isReported));
            } while (cursor.moveToNext());
        }
        cursor.close();
        return scamList;
    }

    // 3. UPDATE - Mark as reported
    public boolean markAsReported(int id) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_IS_REPORTED, 1);

        int rows = db.update(TABLE_SCAMS, values, COLUMN_ID + "=?", new String[]{String.valueOf(id)});
        return rows > 0;
    }

    // 4. DELETE - Delete scam by ID
    public boolean deleteScam(int id) {
        SQLiteDatabase db = this.getWritableDatabase();
        int rows = db.delete(TABLE_SCAMS, COLUMN_ID + "=?", new String[]{String.valueOf(id)});
        return rows > 0;
    }

    // 5. DELETE - Delete all scams
    public void deleteAllScams() {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_SCAMS, null, null);
    }

    // 6. COUNT - Get reported scams, for the dashboard "Reports Sent" tile
    public int getReportedCount() {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT COUNT(*) FROM " + TABLE_SCAMS + " WHERE " + COLUMN_IS_REPORTED + "=1", null);
        cursor.moveToFirst();
        int count = cursor.getInt(0);
        cursor.close();
        return count;
    }
}
