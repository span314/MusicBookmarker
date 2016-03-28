package com.shawnpan.musicbookmarker.provider;

import android.provider.MediaStore;

import java.util.Arrays;

/**
 * Additional columns for local music table.
 * Implements AudioColumns for convenience, although not all columns are mirrored
 * in the local music table.
 */
public class MusicColumns implements MediaStore.Audio.AudioColumns {
    public static final String LAST_USED = "last_used";
    public static final String DISPLAY_NAME = "display_name";
    public static final String DISPLAY_NAME_KEY = "display_name_key";

    public static final String[] PROJECTION = new String[] {
            //Columns inherited from MediaStore
            _ID,
            TITLE,
            ALBUM,
            ARTIST,
            //Column specific to this table
            DISPLAY_NAME
    };
    public static final int COLUMN_INDEX_ID = 0;
    public static final int COLUMN_INDEX_TITLE = 1;
    public static final int COLUMN_INDEX_ALBUM = 2;
    public static final int COLUMN_INDEX_ARTIST = 3;
    public static final int COLUMN_INDEX_DISPLAY_NAME = 4;

    public static final String[] MEDIASTORE_PROJECTION = Arrays.copyOfRange(PROJECTION, COLUMN_INDEX_ID, COLUMN_INDEX_DISPLAY_NAME);
}
