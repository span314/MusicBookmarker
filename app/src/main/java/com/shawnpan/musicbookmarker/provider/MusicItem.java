package com.shawnpan.musicbookmarker.provider;

import android.content.ContentResolver;
import android.database.Cursor;
import android.provider.MediaStore;
import android.text.TextUtils;

import com.shawnpan.musicbookmarker.R;

/**
 * Represents a music track.
 * Created with factory methods that load from a cursor of either the MediaStore or Music tables.
 */
public class MusicItem {
    private static final String DRAWABLE_PREFIX = ContentResolver.SCHEME_ANDROID_RESOURCE + "://com.shawnpan.musicbookmarker/";
    private static final String DRAWABLE_ACCESS_TIME = DRAWABLE_PREFIX + R.drawable.ic_access_time_white_48dp;
    private static final String DRAWABLE_ALBUM = DRAWABLE_PREFIX + R.drawable.ic_album_white_48dp;
    private static final String SEPARATOR = " - ";

    private long id;
    private String title;
    private String album;
    private String artist;
    private String displayName;
    private String icon;

    /**
     * Private constructor - use one of the factory methods
     */
    private MusicItem() {}

    /**
     * Create an instance from current position in cursor matching the schema
     * in {@link MusicColumns#MEDIASTORE_PROJECTION}
     * @param cursor input cursor
     * @return new MusicItem
     */
    public static MusicItem fromMediaStoreCursor(Cursor cursor) {
        MusicItem item = loadCommonFields(cursor);
        item.displayName = item.title;
        item.icon = DRAWABLE_ALBUM;
        return item;
    }

    /**
     * Create an instance from current position in cursor matching the schema
     * in {@link MusicColumns#PROJECTION}
     * @param cursor input cursor
     * @return new MusicItem
     */
    public static MusicItem fromMusicTableCursor(Cursor cursor) {
        MusicItem item = loadCommonFields(cursor);
        item.displayName = cursor.getString(MusicColumns.COLUMN_INDEX_DISPLAY_NAME);
        item.icon = DRAWABLE_ACCESS_TIME;
        return item;
    }

    /**
     * Load fields common to both cursor schemas
     * @param cursor input cursor
     * @return new MusicItem
     */
    private static MusicItem loadCommonFields(Cursor cursor) {
        MusicItem item = new MusicItem();
        item.id = cursor.getLong(MusicColumns.COLUMN_INDEX_ID);
        item.title = cursor.getString(MusicColumns.COLUMN_INDEX_TITLE);
        item.album = cursor.getString(MusicColumns.COLUMN_INDEX_ALBUM);
        item.artist = cursor.getString(MusicColumns.COLUMN_INDEX_ARTIST);
        return item;
    }

    /**
     * @return id of track
     */
    public long getId() {
        return id;
    }

    /**
     * @return title of track
     */
    public String getTitle() {
        return title;
    }

    /**
     * @return album of track
     */
    public String getAlbum() {
        return album;
    }

    /**
     * @return artist of track
     */
    public String getArtist() {
        return artist;
    }

    /**
     * @return display name of track
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * @return uri for the icon to display for this track in searches
     */
    public String getIcon() {
        return icon;
    }

    /**
     * @return description string of track
     */
    public String getDescription() {
        if (TextUtils.equals(title, displayName)) {
            return concatNonEmpty(album, artist);
        } else {
            return concatNonEmpty(title, album, artist);
        }
    }

    /**
     * Helper method to concatenate non empty fields associated with a track
     * @param parts to concatenate
     * @return concatenated parts separated by {@link MusicItem#SEPARATOR}
     */
    private static String concatNonEmpty(String... parts) {
        StringBuilder builder = new StringBuilder();
        for (String part : parts) {
            if (!TextUtils.isEmpty(part) && !MediaStore.UNKNOWN_STRING.equals(part)) {
                builder.append(part);
                builder.append(SEPARATOR);
            }
        }
        return builder.substring(0, builder.length() - SEPARATOR.length());
    }
}
