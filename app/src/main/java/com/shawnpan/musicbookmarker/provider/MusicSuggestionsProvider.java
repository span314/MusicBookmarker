package com.shawnpan.musicbookmarker.provider;

import android.app.SearchManager;
import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.MergeCursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;
import android.provider.BaseColumns;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;

import com.shawnpan.musicbookmarker.R;

/**
 * Content provider for search suggestions.
 */
public class MusicSuggestionsProvider extends ContentProvider {
    private static final String TAG = "MusicBookmarkerProvider";

    private static final String AUTHORITY = "com.shawnpan.musicbookmarker.provider.MusicSuggestionsProvider";
    private static final String DB_NAME = "musicbookmarkerdb";
    public static final String TABLE_SUGGESTIONS = "suggestions";
    private static final int DB_VERSION = 7;

    private static final String DRAWABLE_PREFIX = "'" + ContentResolver.SCHEME_ANDROID_RESOURCE + "://com.shawnpan.musicbookmarker/";
    private static final String DRAWABLE_SUFFIX = "'";
    private static final String DRAWABLE_ACCESS_TIME = DRAWABLE_PREFIX + R.drawable.ic_access_time_white_48dp + DRAWABLE_SUFFIX;
    private static final String DRAWABLE_ALBUM = DRAWABLE_PREFIX + R.drawable.ic_album_white_48dp + DRAWABLE_SUFFIX;
    private static final String CONTENT_URI = "'" + MediaStore.Audio.Media.EXTERNAL_CONTENT_URI + "'";

    private static final Uri SUGGESTIONS_URI = Uri.parse("content://" + AUTHORITY + "/" + TABLE_SUGGESTIONS);
    private static final String[] SUGGESTIONS_PROJECTION = new String [] {
            DRAWABLE_ACCESS_TIME + " AS " + SearchManager.SUGGEST_COLUMN_ICON_1,
            CONTENT_URI + " AS " + SearchManager.SUGGEST_COLUMN_INTENT_DATA,
            MediaStore.Audio.Media._ID + " AS " + SearchManager.SUGGEST_COLUMN_INTENT_DATA_ID,
            Columns.TEXT1,
            Columns.TEXT2,
            Columns.QUERY,
            Columns._ID
    };
    private static final String SUGGESTIONS_FILTER = Columns.TEXT1 + " LIKE ? OR " + Columns.TEXT2 + " LIKE ?";
    private static final String SUGGESTIONS_ORDER_BY = "date DESC";
    private static final String SUGGESTIONS_LIMIT = "20";

    private static final Uri SEARCH_URI = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
    private static final String SEARCH_FILTER = MediaStore.Audio.Media.IS_MUSIC + " = 1 and (" + MediaStore.Audio.Media.TITLE + " like ? or " + MediaStore.Audio.Media.ALBUM + " like ?)";
    private static final String[] SEARCH_PROJECTION = new String [] {
            DRAWABLE_ALBUM + " AS " + SearchManager.SUGGEST_COLUMN_ICON_1,
            CONTENT_URI + " AS " + SearchManager.SUGGEST_COLUMN_INTENT_DATA,
            MediaStore.Audio.Media._ID + " AS " + SearchManager.SUGGEST_COLUMN_INTENT_DATA_ID,
            MediaStore.Audio.Media.TITLE + " AS " + Columns.TEXT1,
            MediaStore.Audio.Media.ALBUM + " AS " + Columns.TEXT2,
            MediaStore.Audio.Media.TITLE + " AS " + Columns.QUERY,
            MediaStore.Audio.Media._ID + " AS " + Columns._ID
    };
    private static final String SEARCH_ORDER_BY = MediaStore.Audio.Media.TITLE + " ASC LIMIT 20";

    private static final int URI_MATCH_SUGGEST = 1;
    private static final UriMatcher URI_MATCHER = new UriMatcher(UriMatcher.NO_MATCH);
    static {
        URI_MATCHER.addURI(AUTHORITY, SearchManager.SUGGEST_URI_PATH_QUERY, URI_MATCH_SUGGEST);
    }

    /**
     * Column names
     */
    public static class Columns implements BaseColumns {
        public static final String TEXT1 = SearchManager.SUGGEST_COLUMN_TEXT_1;
        public static final String TEXT2 = SearchManager.SUGGEST_COLUMN_TEXT_2;
        public static final String QUERY = SearchManager.SUGGEST_COLUMN_QUERY;
        public static final String DATE = "date";
    }

    /**
     * Builds the database.
     */
    private static class DatabaseHelper extends SQLiteOpenHelper {

        public DatabaseHelper(Context context, int newVersion) {
            super(context, DB_NAME, null, newVersion);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            String command =
                    "CREATE TABLE " + TABLE_SUGGESTIONS +
                    " (" +
                    Columns._ID + " PRIMARY KEY UNIQUE ON CONFLICT REPLACE, " +
                    Columns.TEXT1 + " TEXT, " +
                    Columns.TEXT2 + " TEXT, " +
                    Columns.QUERY + " TEXT, " +
                    Columns.DATE + " LONG" +
                    ");";
            db.execSQL(command);
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            Log.w(TAG, "Upgrading database from version " + oldVersion + " to "
                    + newVersion + ", which will destroy all old data");
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_SUGGESTIONS);
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
        SQLiteDatabase db = openHelper.getReadableDatabase();

        if (URI_MATCHER.match(uri) != URI_MATCH_SUGGEST) {
            throw new IllegalArgumentException("Invalid query");
        }

        String query = selectionArgs[0];

        //Return recent results when query is empty
        if (TextUtils.isEmpty(query)) {
            Cursor allRecentCursor = db.query(TABLE_SUGGESTIONS, SUGGESTIONS_PROJECTION, null, null, null, null, SUGGESTIONS_ORDER_BY, SUGGESTIONS_LIMIT);
            return allRecentCursor;
        }

        String like = query + "%";
        String[] args = new String[]{like, like};
        Cursor recentCursor = db.query(TABLE_SUGGESTIONS, SUGGESTIONS_PROJECTION, SUGGESTIONS_FILTER, args, null, null, SUGGESTIONS_ORDER_BY, SUGGESTIONS_LIMIT);
        recentCursor.setNotificationUri(getContext().getContentResolver(), uri);
        Cursor searchCursor = getContext().getContentResolver().query(SEARCH_URI, SEARCH_PROJECTION, SEARCH_FILTER, args, SEARCH_ORDER_BY);

        MergeCursor mergeCursor = new MergeCursor(new Cursor[] {recentCursor, searchCursor});
        return mergeCursor;
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

        long rowID = db.insert(TABLE_SUGGESTIONS, Columns.QUERY, values);
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

        int count = db.delete(TABLE_SUGGESTIONS, selection, selectionArgs);
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


    private static final int MAX_HISTORY_COUNT = 250;

    /**
     * Add a query to the recent queries list.  Returns immediately, performing the save
     * in the background.
     *
     * @param title The string as typed by the user.  This string will be displayed as
     * the suggestion, and if the user clicks on the suggestion, this string will be sent to your
     * searchable activity (as a new search query).
     * @param album Second line to display below the primary line
     */
    public static void saveRecentQuery(final Context context, final String title, final String album, final long id) {
        if (TextUtils.isEmpty(title)) {
            return;
        }

        new Thread("saveRecentQuery") {
            @Override
            public void run() {
                ContentResolver cr = context.getContentResolver();
                long now = System.currentTimeMillis();

                // Use content resolver (not cursor) to insert/update this query
                try {
                    ContentValues values = new ContentValues();
                    values.put(Columns._ID, id);
                    values.put(Columns.TEXT1, title);
                    values.put(Columns.TEXT2, album);
                    values.put(Columns.QUERY, title);
                    values.put(Columns.DATE, now);
                    cr.insert(SUGGESTIONS_URI, values);
                } catch (RuntimeException e) {
                    Log.e(TAG, "saveRecentQuery", e);
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
                selection = Columns._ID + " IN " +
                        "(SELECT " + Columns._ID + " FROM " + TABLE_SUGGESTIONS +
                        " ORDER BY " + Columns.DATE + " DESC" +
                        " LIMIT -1 OFFSET " + String.valueOf(maxEntries) + ")";
            }
            cr.delete(SUGGESTIONS_URI, selection, null);
        } catch (RuntimeException e) {
            Log.e(TAG, "truncateHistory", e);
        }
    }


}
