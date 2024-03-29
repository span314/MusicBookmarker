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
import android.provider.BaseColumns;
import android.provider.MediaStore;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.SearchView;
import android.widget.TextView;
import android.widget.Toast;

import com.shawnpan.musicbookmarker.provider.MusicItem;
import com.shawnpan.musicbookmarker.provider.MusicBookmarksProvider;

public class MusicPlayerActivity extends ActionBarActivity {
    private static final String TAG = "MusicPlayerActivity";

    private TextView titleText;
    private TextView infoText;
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
        titleText = (TextView) findViewById(R.id.title_text);
        infoText = (TextView) findViewById(R.id.info_text);
        musicSeekBar = (MusicSeekBar) findViewById(R.id.seek_bar);
        resetButton = (ImageButton) findViewById(R.id.reset_button);
        previousButton = (ImageButton) findViewById(R.id.previous_button);
        playPauseButton = (ImageButton) findViewById(R.id.play_pause_button);
        nextButton = (ImageButton) findViewById(R.id.next_button);
        playModeButton = (ImageButton) findViewById(R.id.play_mode_button);

        bindListeners();
    }


    @Override
    protected void onNewIntent(Intent intent) {
        Log.v(TAG, "onNewIntent");
        if (intent == null) {
            return;
        }
        if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
            playMusicBySearchString(intent.getStringExtra(SearchManager.QUERY));
        } else if (Intent.ACTION_VIEW.equals(intent.getAction())) {
            playMusicById(intent.getStringExtra(SearchManager.EXTRA_DATA_KEY));
        } else {
            Log.e(TAG, "Unknown intent type");
        }
    }

    private void playMusicBySearchString(String query) {
        String[] musicSearchArgs = new String[] {query};
        Cursor musicSearchCursor = getContentResolver().query(MusicBookmarksProvider.SUGGESTIONS_URI, null, null, musicSearchArgs, null);
        if (musicSearchCursor.moveToFirst()) {
            Log.v(TAG, "Taking first match for query: " + query);
            String idString = Long.toString(musicSearchCursor.getLong(musicSearchCursor.getColumnIndex(BaseColumns._ID)));
            playMusicById(idString);
        } else {
            Toast.makeText(this, R.string.search_no_matches, Toast.LENGTH_SHORT).show();
        }
        musicSearchCursor.close();
    }

    private void playMusicById(String idString) {
        String[] musicIdLookupArgs = new String[] {idString};
        Cursor musicInfoCursor = getContentResolver().query(MusicBookmarksProvider.GET_INFO_URI, null, null, musicIdLookupArgs, null);
        if (musicInfoCursor.moveToFirst()) {
            if (musicInfoCursor.getCount() > 1) {
                Log.w(TAG, "Warning multiple matches, taking first match");
                Toast.makeText(this, R.string.music_modified, Toast.LENGTH_LONG).show();
            }
            MusicItem musicItem = MusicItem.fromMusicTableCursor(musicInfoCursor);
            titleText.setText(musicItem.getDisplayName());
            infoText.setText(musicItem.getDescription());
            Uri musicUri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, musicItem.getId());
            Log.v(TAG, "Playing music uri: " + musicUri);
            Intent playIntent = new Intent(MusicService.ACTION_PLAY, musicUri, getApplicationContext(), MusicService.class);
            startService(playIntent);
        } else {
            Toast.makeText(this, R.string.music_no_longer_found, Toast.LENGTH_SHORT).show();
        }
        musicInfoCursor.close();
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
    }

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
        //searchView.setIconified(false);

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
