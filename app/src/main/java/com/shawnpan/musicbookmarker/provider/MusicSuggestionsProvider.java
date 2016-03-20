package com.shawnpan.musicbookmarker.provider;

import android.app.SearchManager;
import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;
import android.provider.BaseColumns;
import android.text.TextUtils;
import android.util.Log;
/**
 * Content provider for search suggestions.
 */
public class MusicSuggestionsProvider extends ContentProvider {
    private static final String TAG = "MusicBookmarkerProvider";

    private static final String AUTHORITY = "com.shawnpan.musicbookmarker.provider.MusicSuggestionsProvider";
    private static final String DB_NAME = "musicbookmarkerdb";
    public static final String TABLE_SUGGESTIONS = "suggestions";
    private static final int DB_VERSION = 2;

    public static final Uri URI_SUGGESTIONS = Uri.parse("content://" + AUTHORITY + "/" + TABLE_SUGGESTIONS);

    private static final String WHERE = Columns.TEXT1 + " LIKE ? OR " + Columns.TEXT2 + " LIKE ?";
    private static final String[] PROJECTION = new String [] {
            "0 AS " + SearchManager.SUGGEST_COLUMN_FORMAT,
            "0 AS " + SearchManager.SUGGEST_COLUMN_ICON_1, //TODO Fix "'android.resource://system/" + com.android.internal.R.drawable.ic_menu_recent_history
            Columns.TEXT1,
            Columns.TEXT2,
            Columns.QUERY,
            Columns._ID
    };
    private static final String ORDER_BY = "date DESC";
    private static final int URI_MATCH_SUGGEST = 1;

    private SQLiteOpenHelper openHelper;
    private UriMatcher uriMatcher;

    public MusicSuggestionsProvider() {
        uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        uriMatcher.addURI(AUTHORITY, SearchManager.SUGGEST_URI_PATH_QUERY, URI_MATCH_SUGGEST);
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
                    Columns._ID + " PRIMARY KEY, " +
                    Columns.TEXT1 + " TEXT UNIQUE ON CONFLICT REPLACE, " +
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

    @Override
    public boolean onCreate() {
        openHelper = new DatabaseHelper(getContext(), DB_VERSION);
        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        SQLiteDatabase db = openHelper.getReadableDatabase();

        // special case for actual suggestions (from search manager)
        if (uriMatcher.match(uri) == URI_MATCH_SUGGEST) {
            String suggestSelection;
            String[] myArgs;
            if (TextUtils.isEmpty(selectionArgs[0])) {
                suggestSelection = null;
                myArgs = null;
            } else {
                String like = "%" + selectionArgs[0] + "%";
                myArgs = new String [] { like, like };
                suggestSelection = WHERE;
            }
            // Suggestions are always performed with the default sort order
            Cursor c = db.query(TABLE_SUGGESTIONS, PROJECTION,
                    suggestSelection, myArgs, null, null, ORDER_BY, null);
            c.setNotificationUri(getContext().getContentResolver(), uri);
            return c;
        }

        // otherwise process arguments and perform a standard query
        int length = uri.getPathSegments().size();
        if (length != 1 && length != 2) {
            throw new IllegalArgumentException("Unknown Uri");
        }

        String base = uri.getPathSegments().get(0);
        if (!base.equals(TABLE_SUGGESTIONS)) {
            throw new IllegalArgumentException("Unknown Uri");
        }

        String[] useProjection = null;
        if (projection != null && projection.length > 0) {
            useProjection = new String[projection.length + 1];
            System.arraycopy(projection, 0, useProjection, 0, projection.length);
            useProjection[projection.length] = "_id AS _id";
        }

        StringBuilder whereClause = new StringBuilder(256);
        if (length == 2) {
            whereClause.append("(_id = ").append(uri.getPathSegments().get(1)).append(")");
        }

        // Tack on the user's selection, if present
        if (selection != null && selection.length() > 0) {
            if (whereClause.length() > 0) {
                whereClause.append(" AND ");
            }

            whereClause.append('(');
            whereClause.append(selection);
            whereClause.append(')');
        }

        // And perform the generic query as requested
        Cursor c = db.query(base, useProjection, whereClause.toString(),
                selectionArgs, null, null, sortOrder,
                null);
        c.setNotificationUri(getContext().getContentResolver(), uri);
        return c;
    }

    @Override
    public String getType(Uri uri) {
        if (uriMatcher.match(uri) == URI_MATCH_SUGGEST) {
            return SearchManager.SUGGEST_MIME_TYPE;
        }
        int length = uri.getPathSegments().size();
        if (length >= 1) {
            String base = uri.getPathSegments().get(0);
            if (base.equals(TABLE_SUGGESTIONS)) {
                if (length == 1) {
                    return "vnd.android.cursor.dir/suggestion";
                } else if (length == 2) {
                    return "vnd.android.cursor.item/suggestion";
                }
            }
        }
        throw new IllegalArgumentException("Unknown Uri");
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        SQLiteDatabase db = openHelper.getWritableDatabase();

        int length = uri.getPathSegments().size();
        if (length < 1) {
            throw new IllegalArgumentException("Unknown Uri");
        }
        // Note:  This table has on-conflict-replace semantics, so insert() may actually replace()
        long rowID = -1;
        String base = uri.getPathSegments().get(0);
        Uri newUri = null;
        if (base.equals(TABLE_SUGGESTIONS)) {
            if (length == 1) {
                rowID = db.insert(TABLE_SUGGESTIONS, Columns.QUERY, values);
                if (rowID > 0) {
                    newUri = Uri.withAppendedPath(URI_SUGGESTIONS, String.valueOf(rowID));
                }
            }
        }
        if (rowID < 0) {
            throw new IllegalArgumentException("Unknown Uri");
        }
        getContext().getContentResolver().notifyChange(newUri, null);
        return newUri;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        SQLiteDatabase db = openHelper.getWritableDatabase();

        final int length = uri.getPathSegments().size();
        if (length != 1) {
            throw new IllegalArgumentException("Unknown Uri");
        }

        final String base = uri.getPathSegments().get(0);
        int count;
        if (base.equals(TABLE_SUGGESTIONS)) {
            count = db.delete(TABLE_SUGGESTIONS, selection, selectionArgs);
        } else {
            throw new IllegalArgumentException("Unknown Uri");
        }
        getContext().getContentResolver().notifyChange(uri, null);
        return count;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        return 0;
    }


    /*
     * Utility methods for updating suggestions
     */


    private static final int MAX_HISTORY_COUNT = 250;

    /**
     * Add a query to the recent queries list.  Returns immediately, performing the save
     * in the background.
     *
     * @param queryString The string as typed by the user.  This string will be displayed as
     * the suggestion, and if the user clicks on the suggestion, this string will be sent to your
     * searchable activity (as a new search query).
     * @param line2 Second line to display below the primary line
     */
    public static void saveRecentQuery(final Context context, final String queryString, final String line2) {
        if (TextUtils.isEmpty(queryString)) {
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
                    values.put(MusicSuggestionsProvider.Columns.TEXT1, queryString);
                    values.put(MusicSuggestionsProvider.Columns.TEXT2, line2);
                    values.put(MusicSuggestionsProvider.Columns.QUERY, queryString);
                    values.put(MusicSuggestionsProvider.Columns.DATE, now);
                    cr.insert(MusicSuggestionsProvider.URI_SUGGESTIONS, values);
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
            cr.delete(URI_SUGGESTIONS, selection, null);
        } catch (RuntimeException e) {
            Log.e(TAG, "truncateHistory", e);
        }
    }


}
