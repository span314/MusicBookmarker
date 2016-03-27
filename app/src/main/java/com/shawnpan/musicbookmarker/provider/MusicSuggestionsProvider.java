package com.shawnpan.musicbookmarker.provider;

import android.app.SearchManager;
import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;
import android.provider.BaseColumns;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;

import com.shawnpan.musicbookmarker.R;

import java.util.HashSet;
import java.util.Set;

/**
 * Content provider for search suggestions.
 */
public class MusicSuggestionsProvider extends ContentProvider {
    private static final String TAG = "MusicBookmarkerProvider";

    private static final String AUTHORITY = "com.shawnpan.musicbookmarker.provider.MusicSuggestionsProvider";
    private static final String DB_NAME = "MusicBookmarker.db";
    private static final String TABLE_MUSIC = "music";
    private static final int DB_VERSION = 1;


    private static final Uri SEARCH_URI = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
    private static final Uri SUGGESTIONS_URI = Uri.parse("content://" + AUTHORITY + "/" + TABLE_MUSIC);
    private static final String GET_INFO = "get_info";
    public static final Uri GET_INFO_URI = Uri.parse("content://" + AUTHORITY + "/" + GET_INFO);


    private static final int URI_MATCH_SUGGEST = 1;
    private static final int URI_MATCH_GET = 2;
    private static final UriMatcher URI_MATCHER = new UriMatcher(UriMatcher.NO_MATCH);
    static {
        URI_MATCHER.addURI(AUTHORITY, SearchManager.SUGGEST_URI_PATH_QUERY, URI_MATCH_SUGGEST);
        URI_MATCHER.addURI(AUTHORITY, GET_INFO, URI_MATCH_GET);
    }

    /**
     * Additional columns for local music table.
     * Implements AudioColumns for convenience, although not all columns are mirrored
     * in the local music table.
     */
    public static class MusicColumns implements MediaStore.Audio.AudioColumns {
        public static final String LAST_USED = "last_used";
        public static final String DISPLAY_NAME = "display_name";
        public static final String DISPLAY_NAME_KEY = "display_name_key";
    }

    /**
     * Builds the database.
     */
    private static class DatabaseHelper extends SQLiteOpenHelper {
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


        public DatabaseHelper(Context context, int newVersion) {
            super(context, DB_NAME, null, newVersion);
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

    private SQLiteOpenHelper openHelper;

    @Override
    public boolean onCreate() {
        openHelper = new DatabaseHelper(getContext(), DB_VERSION);
        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        switch (URI_MATCHER.match(uri)) {
            case URI_MATCH_SUGGEST:
                return getSuggestions(selectionArgs[0]);
            case URI_MATCH_GET:
                return getById(selectionArgs[0]);
                //TODO allow arbitrary queries?
        }
        throw new IllegalArgumentException("Invalid query URI: " + uri);
    }

    private static final int RESULT_LIMIT = 50;
    private static final String[] RECENT_PROJECTION = new String [] {
            MusicColumns._ID,
            MusicColumns.TITLE,
            MusicColumns.ALBUM,
            MusicColumns.ARTIST,
            MusicColumns.DISPLAY_NAME
    };
    private static final int COLUMN_INDEX_ID = 0;
    private static final int COLUMN_INDEX_TITLE = 1;
    private static final int COLUMN_INDEX_ALBUM = 2;
    private static final int COLUMN_INDEX_ARTIST = 3;
    private static final int COLUMN_INDEX_DISPLAY_NAME = 4;

    private static final String RECENT_FILTER =
            MusicColumns.DISPLAY_NAME_KEY + " LIKE ? OR " +
            MusicColumns.TITLE_KEY + " LIKE ? OR " +
            MusicColumns.ALBUM_KEY + " LIKE ? OR " +
            MusicColumns.ARTIST_KEY + " LIKE ?";
    private static final String RECENT_ORDER_BY = MusicColumns.LAST_USED + " DESC";
    private static final String RECENT_LIMIT = Integer.toString(RESULT_LIMIT);

    private static final String SEARCH_FILTER =
            MusicColumns.IS_MUSIC + " = 1 AND (" +
            MusicColumns.TITLE_KEY + " LIKE ? OR " +
            MusicColumns.ALBUM_KEY + " LIKE ? OR " +
            MusicColumns.ARTIST_KEY + " LIKE ?)";
    private static final String[] SEARCH_PROJECTION = new String [] {
            MusicColumns._ID,
            MusicColumns.TITLE,
            MusicColumns.ALBUM,
            MusicColumns.ARTIST
    };
    private static final String SEARCH_ORDER_BY_LIMIT = MediaStore.Audio.Media.TITLE + " ASC LIMIT " + RESULT_LIMIT;


    private static final String[] SUGGESTION_COLUMNS = new String[]{
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
    private static final String SEPARATOR = " - ";
    
    private Cursor getSuggestions(String keyword) {
        SQLiteDatabase db = openHelper.getReadableDatabase();

        String constraint;
        String recentFilter = null;
        String searchFilter = null;
        String[] recentArgs = null;
        String[] searchArgs = null;
        if (!TextUtils.isEmpty(keyword)) {
            constraint  = MediaStore.Audio.keyFor(keyword) + "%";
            recentFilter = RECENT_FILTER;
            searchFilter = SEARCH_FILTER;
            recentArgs = new String[] {constraint, constraint, constraint, constraint};
            searchArgs = new String[] {constraint, constraint, constraint};
        }

        //De-duplicate results by id
        Set<Long> addedIds = new HashSet<>();
        MatrixCursor matrixCursor = new MatrixCursor(SUGGESTION_COLUMNS);

        //First query local music table
        Cursor recentCursor = db.query(TABLE_MUSIC, RECENT_PROJECTION, recentFilter, recentArgs, null, null, RECENT_ORDER_BY, RECENT_LIMIT);
        while (recentCursor.moveToNext()) {
            addMusicItem(recentCursor, matrixCursor, addedIds, true);
        }
        recentCursor.close();

        //Then query media store
        Cursor searchCursor = getContext().getContentResolver().query(SEARCH_URI, SEARCH_PROJECTION, searchFilter, searchArgs, SEARCH_ORDER_BY_LIMIT);
        while (matrixCursor.getCount() < RESULT_LIMIT && searchCursor.moveToNext()) {
            addMusicItem(searchCursor, matrixCursor, addedIds, false);
        }
        searchCursor.close();

        return matrixCursor;
    }

    private static void addMusicItem(Cursor inputCursor, MatrixCursor outputCursor, Set<Long> addedIds, boolean recent) {
        long id = inputCursor.getLong(COLUMN_INDEX_ID);
        if (addedIds.add(id)) { //only add unique items
            String title = inputCursor.getString(COLUMN_INDEX_TITLE);
            String album = inputCursor.getString(COLUMN_INDEX_ALBUM);
            String artist = inputCursor.getString(COLUMN_INDEX_ARTIST);
            String displayName = null;
            String icon;
            String line1;
            String line2;
            if (recent) {
                displayName = inputCursor.getString(COLUMN_INDEX_DISPLAY_NAME);
                icon = DRAWABLE_ACCESS_TIME;
            } else {
                icon = DRAWABLE_ALBUM;
            }
            if (TextUtils.isEmpty(displayName)) {
                line1 = title;
                line2 = concatNonEmpty(SEPARATOR, album, artist);
            } else {
                line1 = displayName;
                line2 = concatNonEmpty(SEPARATOR, title, album, artist);
            }
            outputCursor.newRow()
                    .add(id)
                    .add(line1)
                    .add(line2)
                    .add(title)
                    .add(icon)
                    .add(Long.toString(id));
        }
    }

    private static String concatNonEmpty(String separator, String... parts) {
        StringBuilder builder = new StringBuilder();
        for (String part : parts) {
            if (!TextUtils.isEmpty(part) && !MediaStore.UNKNOWN_STRING.equals(part)) {
                builder.append(part);
                builder.append(separator);
            }
        }
        return builder.substring(0, builder.length() - separator.length());
    }

    private static final String[] RESULT_COLUMNS = new String[] {
            MusicColumns._ID,
            MusicColumns.TITLE,
            MusicColumns.ALBUM,
            MusicColumns.ARTIST,
            MusicColumns.DISPLAY_NAME
    };

    private Cursor getById(String id) {
        String filter = "_id = ?";
        String[] args = new String[] {id};

        //select from mediastore by id
        Cursor searchCursor = getContext().getContentResolver().query(SEARCH_URI, SEARCH_PROJECTION, filter, args, null);
        searchCursor.moveToFirst();
        long searchId = searchCursor.getLong(0);
        String title = searchCursor.getString(1);
        String album = searchCursor.getString(2);
        String artist = searchCursor.getString(3);
        searchCursor.close();

        //select from music table by id
        SQLiteDatabase db = openHelper.getReadableDatabase();
        Cursor recentCursor = db.query(TABLE_MUSIC, RECENT_PROJECTION, filter, args, null, null, RECENT_ORDER_BY, RECENT_LIMIT);
        //TODO
        recentCursor.close();

        //if both -1, throw exception
        //if mediastore -1, select from mediastore by title, album, artist

        //start update to music table
        asyncSaveRecentQuery(getContext(), Long.parseLong(id), title, album, artist, null);

        //return media store cursor result

        MatrixCursor matrixCursor = new MatrixCursor(RESULT_COLUMNS);
        matrixCursor.newRow()
                .add(searchId)
                .add(title)
                .add(album)
                .add(artist)
                .add(null);
        return matrixCursor;
    }

    @Override
    public String getType(Uri uri) {
        if (URI_MATCHER.match(uri) == URI_MATCH_SUGGEST) {
            return SearchManager.SUGGEST_MIME_TYPE;
        }
        throw new IllegalArgumentException("Unknown Uri");
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        SQLiteDatabase db = openHelper.getWritableDatabase();

        //TODO check valid uri?

        long rowID = db.insertWithOnConflict(TABLE_MUSIC, MusicColumns.LAST_USED, values, SQLiteDatabase.CONFLICT_REPLACE);
        if (rowID < 0) {
            throw new IllegalArgumentException("Error inserting values to suggestions table");
        }
        Uri newUri = Uri.withAppendedPath(SUGGESTIONS_URI, String.valueOf(rowID));
        getContext().getContentResolver().notifyChange(newUri, null);
        return newUri;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        SQLiteDatabase db = openHelper.getWritableDatabase();

        int count = db.delete(TABLE_MUSIC, selection, selectionArgs);
        getContext().getContentResolver().notifyChange(uri, null);
        return count;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        throw new UnsupportedOperationException("Update not supported");
    }


    /*
     * Utility methods for updating suggestions
     */


    private static final int MAX_HISTORY_COUNT = 100;

    /**
     * Add a query to the recent queries list.  Returns immediately, performing the save
     * in the background.
     */
    private static void asyncSaveRecentQuery(final Context context, final long id, final String title, final String album, final String artist, final String displayName) {
        new Thread("asyncSaveRecentQuery") {
            @Override
            public void run() {
                ContentResolver cr = context.getContentResolver();
                long now = System.currentTimeMillis();

                try {
                    ContentValues values = new ContentValues();
                    values.put(MusicColumns._ID, id);
                    values.put(MusicColumns.LAST_USED, now);
                    values.put(MusicColumns.DISPLAY_NAME, displayName);
                    values.put(MusicColumns.DISPLAY_NAME_KEY, MediaStore.Audio.keyFor(displayName));
                    values.put(MusicColumns.TITLE, title);
                    values.put(MusicColumns.TITLE_KEY, MediaStore.Audio.keyFor(title));
                    values.put(MusicColumns.ALBUM, album);
                    values.put(MusicColumns.ALBUM_KEY, MediaStore.Audio.keyFor(album));
                    values.put(MusicColumns.ARTIST, artist);
                    values.put(MusicColumns.ARTIST_KEY, MediaStore.Audio.keyFor(artist));
                    cr.insert(SUGGESTIONS_URI, values);
                } catch (RuntimeException e) {
                    Log.e(TAG, "asyncSaveRecentQuery", e);
                }

                // Shorten the list (if it has become too long)
                truncateHistory(cr, MAX_HISTORY_COUNT);
            }
        }.start();
    }

    /**
     * Completely delete the history.  Use this call to implement a "clear history" UI.
     *
     * Any application that implements search suggestions based on previous actions (such as
     * recent queries, page/items viewed, etc.) should provide a way for the user to clear the
     * history.  This gives the user a measure of privacy, if they do not wish for their recent
     * searches to be replayed by other users of the device (via suggestions).
     */
    public static void clearHistory(Context context) {
        ContentResolver cr = context.getContentResolver();
        truncateHistory(cr, 0);
    }

    /**
     * Reduces the length of the history table, to prevent it from growing too large.
     *
     * @param cr Convenience copy of the content resolver.
     * @param maxEntries Max entries to leave in the table. 0 means remove all entries.
     */
    private static void truncateHistory(ContentResolver cr, int maxEntries) {
        if (maxEntries < 0) {
            throw new IllegalArgumentException();
        }

        try {
            // null means "delete all".  otherwise "delete but leave n newest"
            String selection = null;
            if (maxEntries > 0) {
                selection = BaseColumns._ID + " IN " +
                        "(SELECT " + BaseColumns._ID + " FROM " + TABLE_MUSIC +
                        " ORDER BY " + MusicColumns.LAST_USED + " DESC" +
                        " LIMIT -1 OFFSET " + String.valueOf(maxEntries) + ")";
            }
            cr.delete(SUGGESTIONS_URI, selection, null);
        } catch (RuntimeException e) {
            Log.e(TAG, "truncateHistory", e);
        }
    }


}
