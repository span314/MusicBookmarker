package com.shawnpan.musicbookmarker.provider;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

/**
 * Database builder
 */
public class MusicBookmarksDatabaseHelper extends SQLiteOpenHelper {
    private static final String TAG = "MusicBookmarksDatabase";
    private static final String DB_NAME = "MusicBookmarker.db";
    private static final int DB_VERSION = 4;
    private static final String CREATE_MUSIC_TABLE_COMMAND =
            "CREATE TABLE " + MusicColumns.TABLE +
                    " (" +
                    MusicColumns._ID + " INTEGER PRIMARY KEY UNIQUE, " +
                    MusicColumns.LAST_USED + " INTEGER, " +
                    MusicColumns.DISPLAY_NAME + " TEXT, " +
                    MusicColumns.DISPLAY_NAME_KEY + " TEXT, " +
                    MusicColumns.TITLE + " TEXT, " +
                    MusicColumns.TITLE_KEY + " TEXT, " +
                    MusicColumns.ALBUM + " TEXT, " +
                    MusicColumns.ALBUM_KEY + " TEXT, " +
                    MusicColumns.ARTIST + " TEXT, " +
                    MusicColumns.ARTIST_KEY + " TEXT" +
                    ");" +
                    "CREATE INDEX last_used_index ON " + MusicColumns.TABLE + "(" + MusicColumns.LAST_USED + ")" +
                    "CREATE INDEX display_name_key_index ON " + MusicColumns.TABLE + "(" + MusicColumns.DISPLAY_NAME_KEY + ")" +
                    "CREATE INDEX title_key_index ON " + MusicColumns.TABLE + "(" + MusicColumns.TITLE_KEY + ")" +
                    "CREATE INDEX album_key_index ON " + MusicColumns.TABLE + "(" + MusicColumns.ALBUM_KEY + ")" +
                    "CREATE INDEX artist_key_index ON " + MusicColumns.TABLE + "(" + MusicColumns.ARTIST_KEY + ")";
    private static final String CREATE_BOOKMARK_TABLE_COMMAND =
            "CREATE TABLE " + BookmarkColumns.TABLE +
                    " (" +
                    BookmarkColumns._ID + " INTEGER PRIMARY KEY UNIQUE, " +
                    BookmarkColumns.MUSIC_ID + " INTEGER REFERENCES " + MusicColumns.TABLE + "(" + MusicColumns._ID + ") ON UPDATE CASCADE ON DELETE CASCADE, " +
                    BookmarkColumns.POSITION + " INTEGER, " +
                    BookmarkColumns.LABEL + " TEXT, " +
                    BookmarkColumns.COLOR + " INTEGER" +
                    ");" +
                    "CREATE INDEX music_id_index ON " + BookmarkColumns.TABLE + "(" + BookmarkColumns.MUSIC_ID + ")";
    private static final String DROP_MUSIC_TABLE_COMMAND = "DROP TABLE IF EXISTS " + MusicColumns.TABLE;
    private static final String DROP_BOOKMARK_TABLE_COMMAND = "DROP TABLE IF EXISTS " + BookmarkColumns.TABLE;

    /**
     * Constructor
     * @param context context of application
     */
    public MusicBookmarksDatabaseHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_MUSIC_TABLE_COMMAND);
        db.execSQL(CREATE_BOOKMARK_TABLE_COMMAND);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.w(TAG, "Upgrading database from version " + oldVersion + " to "
                + newVersion + ", which will destroy all old data");
        db.execSQL(DROP_BOOKMARK_TABLE_COMMAND);
        db.execSQL(DROP_MUSIC_TABLE_COMMAND);
        onCreate(db);
    }
}
