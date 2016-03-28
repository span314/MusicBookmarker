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

    private MusicItem() {}

    public static MusicItem fromMediaStoreCursor(Cursor cursor) {
        MusicItem item = loadCommonFields(cursor);
        item.displayName = item.title;
        item.icon = DRAWABLE_ALBUM;
        return item;
    }

    public static MusicItem fromMusicTableCursor(Cursor cursor) {
        MusicItem item = loadCommonFields(cursor);
        item.displayName = cursor.getString(MusicColumns.COLUMN_INDEX_DISPLAY_NAME);
        item.icon = DRAWABLE_ACCESS_TIME;
        return item;
    }

    private static MusicItem loadCommonFields(Cursor cursor) {
        MusicItem item = new MusicItem();
        item.id = cursor.getLong(MusicColumns.COLUMN_INDEX_ID);
        item.title = cursor.getString(MusicColumns.COLUMN_INDEX_TITLE);
        item.album = cursor.getString(MusicColumns.COLUMN_INDEX_ALBUM);
        item.artist = cursor.getString(MusicColumns.COLUMN_INDEX_ARTIST);
        return item;
    }

    public long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getAlbum() {
        return album;
    }

    public String getArtist() {
        return artist;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getIcon() {
        return icon;
    }

    public String getDescription() {
        if (TextUtils.equals(title, displayName)) {
            return concatNonEmpty(album, artist);
        } else {
            return concatNonEmpty(title, album, artist);
        }
    }

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
