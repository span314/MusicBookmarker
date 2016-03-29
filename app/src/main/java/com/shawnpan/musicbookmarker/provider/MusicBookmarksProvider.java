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
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;

/**
 * Content provider for music bookmarks.
 */
public class MusicBookmarksProvider extends ContentProvider {
    private static final String TAG = "MusicBookmarkerProvider";

    private static final String AUTHORITY = "com.shawnpan.musicbookmarker.provider.MusicBookmarksProvider";
    private static final String TABLE_MUSIC = MusicBookmarksDatabaseHelper.TABLE_MUSIC;

    private static final Uri SEARCH_URI = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
    private static final Uri MUSIC_TABLE_URI = Uri.parse("content://" + AUTHORITY + "/" + TABLE_MUSIC);
    private static final String GET_INFO = "get_info";
    public static final Uri SUGGESTIONS_URI = Uri.parse("content://" + AUTHORITY + "/" + SearchManager.SUGGEST_URI_PATH_QUERY);
    public static final Uri GET_INFO_URI = Uri.parse("content://" + AUTHORITY + "/" + GET_INFO);

    private static final int URI_MATCH_SUGGEST = 1;
    private static final int URI_MATCH_GET = 2;
    private static final UriMatcher URI_MATCHER = new UriMatcher(UriMatcher.NO_MATCH);
    static {
        URI_MATCHER.addURI(AUTHORITY, SearchManager.SUGGEST_URI_PATH_QUERY, URI_MATCH_SUGGEST);
        URI_MATCHER.addURI(AUTHORITY, GET_INFO, URI_MATCH_GET);
    }

    private SQLiteOpenHelper openHelper;

    @Override
    public boolean onCreate() {
        openHelper = new MusicBookmarksDatabaseHelper(getContext());
        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        switch (URI_MATCHER.match(uri)) {
            case URI_MATCH_SUGGEST:
                //Query for search suggestions - everything except selectionArgs[0] is ignored
                return getSuggestions(selectionArgs[0]);
            case URI_MATCH_GET:
                //Query by id - everything except selectionArgs[0] is ignored
                return getById(selectionArgs[0]);
        }
        throw new IllegalArgumentException("Invalid query URI: " + uri);
    }

    /*
     * Query for search suggestions
     */
    private static final int SUGGESTION_RESULT_LIMIT = 50;

    private static final String RECENT_FILTER =
            MusicColumns.DISPLAY_NAME_KEY + " LIKE ? OR " +
            MusicColumns.TITLE_KEY + " LIKE ? OR " +
            MusicColumns.ALBUM_KEY + " LIKE ? OR " +
            MusicColumns.ARTIST_KEY + " LIKE ?";
    private static final String RECENT_ORDER_BY = MusicColumns.LAST_USED + " DESC";
    private static final String RECENT_LIMIT = Integer.toString(SUGGESTION_RESULT_LIMIT);

    private static final String SEARCH_FILTER =
            MusicColumns.IS_MUSIC + " = 1 AND (" +
            MusicColumns.TITLE_KEY + " LIKE ? OR " +
            MusicColumns.ALBUM_KEY + " LIKE ? OR " +
            MusicColumns.ARTIST_KEY + " LIKE ?)";
    private static final String SEARCH_ORDER_BY_LIMIT = MusicColumns.TITLE + " ASC LIMIT " + SUGGESTION_RESULT_LIMIT;

    /**
     * Find search suggestions. First looks in the local music table, then in the mediastore.
     * @param keyword search term
     * @return cursor of suggestions. See {@link MusicSuggestionsCursor} for schema of cursor output.
     */
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

        MusicSuggestionsCursor suggestionsCursor = new MusicSuggestionsCursor();

        //First query local music table
        Cursor recentCursor = db.query(TABLE_MUSIC, MusicColumns.PROJECTION, recentFilter, recentArgs, null, null, RECENT_ORDER_BY, RECENT_LIMIT);
        while (recentCursor.moveToNext()) {
            suggestionsCursor.addUnique(MusicItem.fromMusicTableCursor(recentCursor));
        }
        recentCursor.close();

        //Then query media store
        Cursor searchCursor = getContext().getContentResolver().query(SEARCH_URI, MusicColumns.MEDIASTORE_PROJECTION, searchFilter, searchArgs, SEARCH_ORDER_BY_LIMIT);
        while (suggestionsCursor.getCount() < SUGGESTION_RESULT_LIMIT && searchCursor.moveToNext()) {
            suggestionsCursor.addUnique(MusicItem.fromMediaStoreCursor(searchCursor));
        }
        searchCursor.close();

        return suggestionsCursor;
    }

    /*
     * Query for music item by id
     */
    private static final String ID_FILTER = MusicColumns._ID + " = ?";
    private static final String TITLE_ALBUM_ARTIST_FILTER =
            MusicColumns.TITLE + " = ? AND " + MusicColumns.ALBUM + " = ? AND " + MusicColumns.ARTIST + " = ?";
    private static final String ID_ASC_ORDER = MusicColumns._ID + " ASC";

    /**
     * Gets information on a music track by id.
     * <p>
     * Warning: this query method has side effects - it attempts to synchronize the local music table
     * with the mediastore if necessary, and updates the last used time.
     * <p>
     * If the id only exists in the local music table, this method attempts to find a matching track
     * by title, album, and artist (in case the file was moved). If the id only exists in the mediastore,
     * a new entry in the local table will be created.
     *
     * Usually, the cursor will contain one entry corresponding to the input id. A empty cursor may
     * be returned if no match could be found. In rare cases, a cursor with multiple entries may be
     * returned if the fallback lookup by title/album/artist does not produce a unique result.
     * See {@link MusicColumns} for schema of cursor.
     *
     * @param id id of music item (corresponding to either the local table OR mediastore)
     * @return cursor with information on the music track.
     */
    private Cursor getById(String id) {
        MatrixCursor result = new MatrixCursor(MusicColumns.PROJECTION);

        String[] idArgs = new String[] {id};

        //Select from music table by id
        MusicItem musicTableItem = null;
        SQLiteDatabase db = openHelper.getReadableDatabase();
        Cursor musicTableCursor = db.query(TABLE_MUSIC, MusicColumns.PROJECTION, ID_FILTER, idArgs, null, null, null, null);
        if (musicTableCursor.moveToFirst()) {
            musicTableItem = MusicItem.fromMusicTableCursor(musicTableCursor);
        }
        musicTableCursor.close();

        //Try to select from mediastore first by id, then by title/album/artist
        Cursor searchCursor = getContext().getContentResolver().query(SEARCH_URI, MusicColumns.MEDIASTORE_PROJECTION, ID_FILTER, idArgs, null);
        if (searchCursor.getCount() == 0) {
            searchCursor.close();
            if (musicTableItem == null) {
                Log.w(TAG, "No results found for ID " + id + "in either MediaStore or local table, returning empty cursor");
                return result;
            }
            Log.w(TAG, "File not found by id, trying to re-sync db entry by title/album/artist");
            String[] titleAlbumArtistArgs = new String[] {musicTableItem.getTitle(), musicTableItem.getArtist(), musicTableItem.getAlbum()};
            searchCursor = getContext().getContentResolver().query(SEARCH_URI, MusicColumns.MEDIASTORE_PROJECTION, TITLE_ALBUM_ARTIST_FILTER, titleAlbumArtistArgs, ID_ASC_ORDER);
        }
        if (searchCursor.getCount() == 0) {
            Log.w(TAG, "File possibly deleted? Returning empty cursor");
            return result;
        }
        MusicItem mediaStoreItem = null;
        while (searchCursor.moveToNext()) {
            mediaStoreItem = MusicItem.fromMediaStoreCursor(searchCursor);
            String displayName = mediaStoreItem.getDisplayName();
            if (musicTableItem != null && !TextUtils.equals(displayName, musicTableItem.getDisplayName())) {
                displayName = musicTableItem.getDisplayName();
            }
            result.newRow()
                    .add(mediaStoreItem.getId())
                    .add(mediaStoreItem.getTitle())
                    .add(mediaStoreItem.getAlbum())
                    .add(mediaStoreItem.getArtist())
                    .add(displayName);
        }
        searchCursor.close();

        if (result.getCount() == 1) {
            //start update to music table
            asyncSaveRecentQuery(getContext(), Long.parseLong(id), mediaStoreItem);
        } else if (result.getCount() == 0) {
            //TODO label entry as broken if bookmarks still exist
            db = openHelper.getWritableDatabase();
            db.delete(TABLE_MUSIC, ID_FILTER, idArgs);
        } else {
            //TODO select correct match, for now assume file has moved and take the highest id (most recent update)
            asyncSaveRecentQuery(getContext(), Long.parseLong(id), mediaStoreItem);
        }

        return result;
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
        Uri newUri = Uri.withAppendedPath(MUSIC_TABLE_URI, String.valueOf(rowID));
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

    public static final String METHOD_CLEAR_HISTORY = "clearHistory";
    @Override
    public Bundle call(String method, String arg, Bundle extras) {
        switch (method) {
            case METHOD_CLEAR_HISTORY:
                truncateHistory(getContext().getContentResolver(), 0);
                return null;
        }
        throw new IllegalArgumentException("Unknown call method " + method);
    }


    /*
     * Utility methods for updating suggestions
     */
    private static final int MAX_HISTORY_COUNT = 100;

    /**
     * Add a query to the recent queries list.  Returns immediately, performing the save
     * in the background.
     */
    private static void asyncSaveRecentQuery(final Context context, final long id, final MusicItem musicItem) {
        new Thread("asyncSaveRecentQuery") {
            @Override
            public void run() {
                ContentResolver cr = context.getContentResolver();
                long now = System.currentTimeMillis();

                try {
                    ContentValues values = new ContentValues();
                    values.put(MusicColumns._ID, id);
                    values.put(MusicColumns.LAST_USED, now);
                    values.put(MusicColumns.DISPLAY_NAME, musicItem.getDisplayName());
                    values.put(MusicColumns.DISPLAY_NAME_KEY, MediaStore.Audio.keyFor(musicItem.getDisplayName()));
                    values.put(MusicColumns.TITLE, musicItem.getTitle());
                    values.put(MusicColumns.TITLE_KEY, MediaStore.Audio.keyFor(musicItem.getTitle()));
                    values.put(MusicColumns.ALBUM, musicItem.getAlbum());
                    values.put(MusicColumns.ALBUM_KEY, MediaStore.Audio.keyFor(musicItem.getAlbum()));
                    values.put(MusicColumns.ARTIST, musicItem.getArtist());
                    values.put(MusicColumns.ARTIST_KEY, MediaStore.Audio.keyFor(musicItem.getArtist()));
                    cr.insert(MUSIC_TABLE_URI, values);
                } catch (RuntimeException e) {
                    Log.e(TAG, "asyncSaveRecentQuery", e);
                }

                // Shorten the list (if it has become too long)
                truncateHistory(cr, MAX_HISTORY_COUNT);
            }
        }.start();
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
                selection = MusicColumns._ID + " IN " +
                        "(SELECT " + MusicColumns._ID + " FROM " + TABLE_MUSIC +
                        " ORDER BY " + MusicColumns.LAST_USED + " DESC" +
                        " LIMIT -1 OFFSET " + String.valueOf(maxEntries) + ")";
            }
            cr.delete(MUSIC_TABLE_URI, selection, null);
        } catch (RuntimeException e) {
            Log.e(TAG, "truncateHistory", e);
        }
    }


}
