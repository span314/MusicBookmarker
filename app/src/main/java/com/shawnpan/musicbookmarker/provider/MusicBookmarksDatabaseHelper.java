package com.shawnpan.musicbookmarker.provider;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

/**
 * Database builder
 */
public class MusicBookmarksDatabaseHelper extends SQLiteOpenHelper {
    /**
     * Table name for music tracks
     */
    public static final String TABLE_MUSIC = "music";

    private static final String TAG = "MusicBookmarkerProvider";
    private static final String DB_NAME = "MusicBookmarker.db";
    private static final int DB_VERSION = 3;
    private static final String CREATE_COMMAND =
            "CREATE TABLE " + TABLE_MUSIC +
                    " (" +
                    MusicColumns._ID + " LONG PRIMARY KEY UNIQUE, " +
                    MusicColumns.LAST_USED + " LONG, " +
                    MusicColumns.DISPLAY_NAME + " TEXT, " +
                    MusicColumns.DISPLAY_NAME_KEY + " TEXT, " +
                    MusicColumns.TITLE + " TEXT, " +
                    MusicColumns.TITLE_KEY + " TEXT, " +
                    MusicColumns.ALBUM + " TEXT, " +
                    MusicColumns.ALBUM_KEY + " TEXT, " +
                    MusicColumns.ARTIST + " TEXT, " +
                    MusicColumns.ARTIST_KEY + " TEXT" +
                    ");" +
                    "CREATE INDEX last_used_index ON " + TABLE_MUSIC + "(" + MusicColumns.LAST_USED + ")" +
                    "CREATE INDEX display_name_key_index ON " + TABLE_MUSIC + "(" + MusicColumns.DISPLAY_NAME_KEY + ")" +
                    "CREATE INDEX title_key_index ON " + TABLE_MUSIC + "(" + MusicColumns.TITLE_KEY + ")" +
                    "CREATE INDEX album_key_index ON " + TABLE_MUSIC + "(" + MusicColumns.ALBUM_KEY + ")" +
                    "CREATE INDEX artist_key_index ON " + TABLE_MUSIC + "(" + MusicColumns.ARTIST_KEY + ")";
    private static final String DROP_COMMAND = "DROP TABLE IF EXISTS " + TABLE_MUSIC;

    /**
     * Constructor
     * @param context context of application
     */
    public MusicBookmarksDatabaseHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_COMMAND);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.w(TAG, "Upgrading database from version " + oldVersion + " to "
                + newVersion + ", which will destroy all old data");
        db.execSQL(DROP_COMMAND);
        onCreate(db);
    }
}
