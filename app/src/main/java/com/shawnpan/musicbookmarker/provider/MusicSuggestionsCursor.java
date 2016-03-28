package com.shawnpan.musicbookmarker.provider;

import android.app.SearchManager;
import android.database.MatrixCursor;
import android.provider.BaseColumns;

import java.util.HashSet;
import java.util.Set;

/**
 * Convenience subclass of MatrixCursor for building music search suggestions.
 */
public class MusicSuggestionsCursor extends MatrixCursor {
    private static final String[] SUGGESTION_COLUMNS = new String[]{
            BaseColumns._ID,
            SearchManager.SUGGEST_COLUMN_TEXT_1,
            SearchManager.SUGGEST_COLUMN_TEXT_2,
            SearchManager.SUGGEST_COLUMN_QUERY,
            SearchManager.SUGGEST_COLUMN_ICON_1,
            SearchManager.SUGGEST_COLUMN_INTENT_EXTRA_DATA
    };
    private Set<Long> addedIds = new HashSet<>();

    public MusicSuggestionsCursor() {
        super(SUGGESTION_COLUMNS);
    }

    public void addUnique(MusicItem item) {
        if (addedIds.add(item.getId())) {
            newRow()
                .add(item.getId())
                .add(item.getDisplayName())
                .add(item.getDescription())
                .add(item.getTitle())
                .add(item.getIcon())
                .add(Long.toString(item.getId()));
        }
    }
}