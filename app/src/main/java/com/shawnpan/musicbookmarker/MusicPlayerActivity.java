package com.shawnpan.musicbookmarker;

import android.app.SearchManager;
import android.content.ComponentName;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.provider.MediaStore;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.CursorAdapter;
import android.widget.FilterQueryProvider;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.SearchView;
import android.widget.SimpleCursorAdapter;
import com.shawnpan.musicbookmarker.provider.MusicSuggestionsProvider;

public class MusicPlayerActivity extends ActionBarActivity {
    private static final String TAG = "MusicPlayerActivity";

    private ListView musicList;
    private MusicSeekBar musicSeekBar;
    private ImageButton resetButton;
    private ImageButton previousButton;
    private ImageButton playPauseButton;
    private ImageButton nextButton;
    private ImageButton playModeButton;

    private MusicService musicService;
    private boolean musicServiceBound = false;
    private ServiceConnection musicServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            musicService = ((MusicService.MusicServiceBinder) service).getService();
            musicServiceBound = true;
            updateButtonIcons();
            updateSeekbar();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            progressUpdateHandler.removeCallbacks(progressUpdateRunnable);
            musicServiceBound = false;
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_music_player);

        //load views
        musicList = (ListView) findViewById(R.id.music_list);
        musicSeekBar = (MusicSeekBar) findViewById(R.id.seek_bar);
        resetButton = (ImageButton) findViewById(R.id.reset_button);
        previousButton = (ImageButton) findViewById(R.id.previous_button);
        playPauseButton = (ImageButton) findViewById(R.id.play_pause_button);
        nextButton = (ImageButton) findViewById(R.id.next_button);
        playModeButton = (ImageButton) findViewById(R.id.play_mode_button);

        //setup list
        String[] fromColumns = new String[] {MediaStore.Audio.Media.TITLE};
        int[] toViews = new int[] {android.R.id.text1};
        SimpleCursorAdapter selectMusicAdaptor = new SimpleCursorAdapter(MusicPlayerActivity.this, android.R.layout.simple_list_item_1, null, fromColumns, toViews, 0);
        selectMusicAdaptor.setFilterQueryProvider(new FilterQueryProvider() {
            @Override
            public Cursor runQuery(CharSequence constraint) {
                return searchMusic(constraint);
            }
        });
        selectMusicAdaptor.setCursorToStringConverter(new SimpleCursorAdapter.CursorToStringConverter() {
            @Override
            public CharSequence convertToString(Cursor cursor) {
                String title = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.TITLE));
                return title;
            }
        });
        musicList.setAdapter(selectMusicAdaptor);

        bindListeners();
    }


    @Override
    protected void onNewIntent(Intent intent) {
        Log.v(TAG, "onNewIntent");
        if (intent == null) {
            return;
        }
        if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
            String query = intent.getStringExtra(SearchManager.QUERY);
            Log.v(TAG, "query: " + query);
            changeCursor(searchMusic(query));
        } else if (Intent.ACTION_VIEW.equals(intent.getAction())) {
            String musicIdString = intent.getStringExtra(SearchManager.EXTRA_DATA_KEY);
            long musicId = Long.parseLong(musicIdString);
            //Uri musicInfoUri = ContentUris.withAppendedId(MusicSuggestionsProvider.SUGGESTIONS_URI, musicId);
            //Log.v(TAG, "view info uri: " + musicInfoUri);
            Cursor musicInfoCursor = getContentResolver().query(MusicSuggestionsProvider.GET_INFO_URI, null, null, new String[]{musicIdString}, null);
            musicInfoCursor.close();
            Uri musicUri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, musicId);
            Log.v(TAG, "view music uri: " + musicUri);
            Intent playIntent = new Intent(MusicService.ACTION_PLAY, musicUri, getApplicationContext(), MusicService.class);
            startService(playIntent);
            //MusicSuggestionsProvider.saveRecentQuery(this, musicId);
        }
    }

    private Cursor searchMusic(CharSequence constraint) {
        Uri uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        String[] select = new String[]{MediaStore.Audio.Media._ID, MediaStore.Audio.Media.TITLE, MediaStore.Audio.Media.TRACK, MediaStore.Audio.Media.ALBUM};
        String where = MediaStore.Audio.Media.IS_MUSIC + " = 1 and (" + MediaStore.Audio.Media.TITLE + " like ? or " + MediaStore.Audio.Media.ALBUM + " like ?)";
        String likePattern = constraint + "%";
        String[] args = new String[]{likePattern, likePattern};
        String orderBy = "title ASC LIMIT 20";
        return getContentResolver().query(uri, select, where, args, orderBy);
    }

    @Override
    protected void onDestroy() {
        changeCursor(null); //Close cursor on adapter
        super.onDestroy();
    }

    private void changeCursor(Cursor cursor) {
        CursorAdapter adapter = (CursorAdapter) musicList.getAdapter();
        adapter.changeCursor(cursor);
    }

    private void bindListeners() {
        resetButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (musicServiceBound) {
                    musicService.seekTo(0);
                }
            }
        });

        previousButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.v(TAG, "Previous button click");
            }
        });

        playPauseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (musicServiceBound) {
                    startService(new Intent(MusicService.ACTION_PLAY_PAUSE, null, getApplicationContext(), MusicService.class));
                }
            }
        });

        nextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.v(TAG, "Next button click");
            }
        });

        playModeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (musicServiceBound) {
                    musicService.setRepeat(!musicService.isRepeat());
                    updateButtonIcons();
                }
            }
        });

        musicSeekBar.setOnTimeChangeListener(new MusicSeekBar.OnTimeChangeListener() {
            @Override
            public void onTimeChanged(int timeMillis) {
                if (musicServiceBound) {
                    musicService.seekTo(timeMillis);
                }
            }
        });

        musicList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Cursor cursor = (Cursor) parent.getItemAtPosition(position);
                long musicId = cursor.getLong(cursor.getColumnIndex(MediaStore.Audio.Media._ID));
                //String musicTitle = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.TITLE));
                //String musicAlbum = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM));

                Uri musicUri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, musicId);

                Intent intent = new Intent(MusicService.ACTION_PLAY, musicUri, getApplicationContext(), MusicService.class);
                startService(intent);
                //MusicSuggestionsProvider.saveRecentQuery(MusicPlayerActivity.this, musicId); //TODO remove?
//                hideKeyboard();
            }
        });
    }

    //TODO?
//    private void hideKeyboard() {
//        selectMusic.clearFocus();
//        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
//        imm.hideSoftInputFromWindow(selectMusic.getWindowToken(), 0);
//    }

    @Override
    protected void onStart() {
        super.onStart();
        Intent intent = new Intent(this, MusicService.class);
        bindService(intent, musicServiceConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onStop() {
        if (musicServiceBound) {
            unbindService(musicServiceConnection);
        }
        super.onStop();
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateSeekbar();
    }

    @Override
    protected void onPause() {
        progressUpdateHandler.removeCallbacks(progressUpdateRunnable);
        super.onPause();
    }

    private void updateButtonIcons() {
        int playModeIcon = musicService.isRepeat() ?  R.drawable.ic_repeat_white_48dp : R.drawable.ic_looks_one_white_48dp;
        playModeButton.setImageResource(playModeIcon);
        int playPauseIcon = musicService.isPlaying() ? R.drawable.ic_pause_white_48dp : R.drawable.ic_play_arrow_white_48dp;
        playPauseButton.setImageResource(playPauseIcon);
    }

    private void updateSeekbar() {
        if (musicServiceBound) {
            progressUpdateHandler.removeCallbacks(progressUpdateRunnable);
            int currentTime = musicService.getCurrentTime();
            int duration = musicService.getDuration();
            musicSeekBar.updateTime(currentTime, duration);
            progressUpdateHandler.postDelayed(progressUpdateRunnable, 500);
        }
    }

    Handler progressUpdateHandler = new Handler();
    Runnable progressUpdateRunnable = new Runnable() {
        @Override
        public void run() {
            updateSeekbar();
            updateButtonIcons();
        }
    };



    //TODO settings activity
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_music_player, menu);

        // Associate searchable configuration with the SearchView
        SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        SearchView searchView = (SearchView) menu.findItem(R.id.search).getActionView();
        searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
