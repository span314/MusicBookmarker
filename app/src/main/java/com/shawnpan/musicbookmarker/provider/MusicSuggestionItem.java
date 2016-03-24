package com.shawnpan.musicbookmarker.provider;

import android.app.SearchManager;
import android.content.ContentResolver;
import android.provider.BaseColumns;
import android.provider.MediaStore;

import com.shawnpan.musicbookmarker.R;

/**
 * Created by shawn on 3/22/16.
 */
public class MusicSuggestionItem implements Comparable<MusicSuggestionItem> {
    public static final String[] SUGGESTION_COLUMNS = new String[]{
            BaseColumns._ID,
            SearchManager.SUGGEST_COLUMN_TEXT_1,
            SearchManager.SUGGEST_COLUMN_TEXT_2,
            SearchManager.SUGGEST_COLUMN_QUERY,
            SearchManager.SUGGEST_COLUMN_ICON_1,
            SearchManager.SUGGEST_COLUMN_INTENT_EXTRA_DATA
    };

    private static final String DRAWABLE_PREFIX = ContentResolver.SCHEME_ANDROID_RESOURCE + "://com.shawnpan.musicbookmarker/";
    private static final String DRAWABLE_ACCESS_TIME = DRAWABLE_PREFIX + R.drawable.ic_access_time_white_48dp;
    private static final String DRAWABLE_ALBUM = DRAWABLE_PREFIX + R.drawable.ic_album_white_48dp;

    private final long id;
    private final long lastUsedTimestamp;
    private final String title;
    private final String album;

    public MusicSuggestionItem(long id, long lastUsedTimestamp, String title, String album) {
        this.id = id;
        this.lastUsedTimestamp = lastUsedTimestamp;
        this.title = title;
        this.album = album;
    }

    public Object[] toSuggestionColumns() {
        String iconUri = lastUsedTimestamp > 0 ? DRAWABLE_ACCESS_TIME : DRAWABLE_ALBUM;
        return new Object[] {
                id,
                title,
                album,
                title,
                iconUri,
                Long.toString(id)
        };
    }

    @Override
    public int compareTo(MusicSuggestionItem another) {
        return 4 * Long.signum(this.lastUsedTimestamp - another.lastUsedTimestamp) +
               2 * Integer.signum(String.CASE_INSENSITIVE_ORDER.compare(this.title, another.title)) +
                   Integer.signum(String.CASE_INSENSITIVE_ORDER.compare(this.album, another.album));
    }

    @Override
    public boolean equals(Object object) {
        if (object instanceof MusicSuggestionItem) {
            MusicSuggestionItem another = (MusicSuggestionItem) object;
            return this.compareTo(another) == 0;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return (int) lastUsedTimestamp +
               7829 * (title == null ? 0 : title.hashCode()) +
               14851 * (album == null ? 0 : album.hashCode());
    }
}
