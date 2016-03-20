package com.shawnpan.musicbookmarker;

import android.content.SearchRecentSuggestionsProvider;

/**
 * Created by shawn on 3/20/16.
 */
public class SearchRecentMusicProvider extends SearchRecentSuggestionsProvider {
    public final static String AUTHORITY = "com.shawnpan.musicbookmarker.SearchRecentMusicProvider";
    public final static int MODE = DATABASE_MODE_QUERIES;

    public SearchRecentMusicProvider() {
        setupSuggestions(AUTHORITY, MODE);
    }

}
