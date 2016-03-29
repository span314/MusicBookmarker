package com.shawnpan.musicbookmarker.provider;

import android.provider.BaseColumns;

/**
 * Columns in the bookmark table
 */
public class BookmarkColumns implements BaseColumns {
    /**
     * Name of the bookmark table
     */
    public static final String TABLE = "bookmark";

    /**
     * Join column with music table
     */
    public static final String MUSIC_ID = "music_id";

    /**
     * Position of bookmark in milliseconds
     */
    public static final String POSITION = "position";

    /**
     * Label of the bookmark
     */
    public static final String LABEL = "label";

    /**
     * Color of the bookmark
     */
    public static final String COLOR = "color";

    /**
     * Standard order of columns for queries
     */
    public static final String[] PROJECTION = new String[] {
            _ID,
            MUSIC_ID,
            POSITION,
            LABEL,
            COLOR
    };
    public static final int COLUMN_INDEX_ID = 0;
    public static final int COLUMN_INDEX_MUSIC_ID = 1;
    public static final int COLUMN_INDEX_POSITION = 2;
    public static final int COLUMN_INDEX_LABEL = 3;
    public static final int COLUMN_INDEX_COLOR = 4;
}
