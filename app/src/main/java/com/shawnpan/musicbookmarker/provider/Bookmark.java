package com.shawnpan.musicbookmarker.provider;

import android.database.Cursor;

/**
 * Bookmark in music track
 */
public class Bookmark {
    private long id;
    private long musicId;
    private long position;
    private String label;
    private int color;

    /**
     * Create an instance from current position in cursor matching the schema
     * in {@link BookmarkColumns#PROJECTION}
     * @param cursor input cursor
     * @return new MusicItem
     */
    public static Bookmark fromCursor(Cursor cursor) {
        Bookmark bookmark = new Bookmark();
        bookmark.id = cursor.getLong(BookmarkColumns.COLUMN_INDEX_ID);
        bookmark.musicId = cursor.getLong(BookmarkColumns.COLUMN_INDEX_MUSIC_ID);
        bookmark.position = cursor.getLong(BookmarkColumns.COLUMN_INDEX_POSITION);
        bookmark.label = cursor.getString(BookmarkColumns.COLUMN_INDEX_LABEL);
        bookmark.color = cursor.getInt(BookmarkColumns.COLUMN_INDEX_COLOR);
        return bookmark;
    }

    public long getId() {
        return id;
    }

    public String getLabel() {
        return label;
    }

    public long getMusicId() {
        return musicId;
    }

    public long getPosition() {
        return position;
    }

    public int getColor() {
        return color;
    }
}
