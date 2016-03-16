package com.shawnpan.musicbookmarker;

import android.app.Service;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Binder;
import android.os.IBinder;
import android.os.PowerManager;
import android.util.Log;

import java.io.IOException;

/**
 * Service for playing music
 */
public class MusicService extends Service {
    public static final String TAG = "MusicService";

    //Intent actions - should match manifest
    public static final String ACTION_PLAY = "com.shawnpan.musicbookmarker.action.PLAY";
    public static final String ACTION_STOP = "com.shawnpan.musicbookmarker.action.STOP";
    public static final String ACTION_PLAY_PAUSE = "com.shawnpan.musicbookmarker.action.PLAY_PAUSE";

    //Media player
    private AudioPlayer mediaPlayer;

    //State of playback
    private enum MediaState {
        STOPPED,
        PLAYING,
        PAUSED
    }
    private MediaState mediaState = MediaState.STOPPED;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.v(TAG, "onStartCommand");
        if (intent != null && intent.getAction() != null) {
            switch (intent.getAction()) {
                case ACTION_PLAY:
                    playMusic(intent.getData());
                    break;
                case ACTION_STOP:
                    releaseMediaPlayer();
                    break;
                case ACTION_PLAY_PAUSE:
                    togglePlayPause();
                    break;
                default:
                    Log.e(TAG, "Unknown intent: " + intent.getAction());
            }
        }
        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        Log.v(TAG, "ondestroy");
        releaseMediaPlayer();
    }

    //Binder implementation
    public class MusicServiceBinder extends Binder {
        /**
         * @return this instance of MusicService
         */
        MusicService getService() {
            return MusicService.this;
        }
    }
    private final IBinder binder = new MusicServiceBinder();

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    private void playMusic(Uri uri) {
        Log.v(TAG, "Playing " + uri);
        initializeMediaPlayer();
        mediaPlayer.playUri(getApplicationContext(), uri);
    }

    private void togglePlayPause() {
        if (mediaState == MediaState.PLAYING) {
            pause();
        } else if (mediaState == MediaState.PAUSED) {
            play();
        }
    }

    private void pause() {
        mediaPlayer.pause();
        mediaState = MediaState.PAUSED;
    }

    private void play() {
        mediaPlayer.start();
        mediaState = MediaState.PLAYING;
    }

    private void initializeMediaPlayer() {
        if (mediaPlayer == null) {
            MediaPlayerAudioPlayer internalPlayer = new MediaPlayerAudioPlayer();
            internalPlayer.setOnPreparedListener(onPreparedListener);
            internalPlayer.setOnCompletionListener(onCompletionListener);
            internalPlayer.setOnErrorListener(onErrorListener);
            mediaPlayer = new AudioPlayerWithLeader(internalPlayer);
        } else {
            mediaPlayer.reset();
            mediaState = MediaState.STOPPED;
        }
    }

    private void releaseMediaPlayer() {
        if (mediaPlayer != null) {
            mediaPlayer.reset();
            mediaPlayer.release();
            mediaPlayer = null;
            mediaState = MediaState.STOPPED;
        }
    }

    //Listeners
    private MediaPlayer.OnPreparedListener onPreparedListener = new MediaPlayer.OnPreparedListener() {
        @Override
        public void onPrepared(MediaPlayer mp) {
            Log.v(TAG, "onprepared");
            //mediaPlayer.seekTo(80000);
            //mediaPlayer.seekTo(92000);
            play();

        }
    };

    private MediaPlayer.OnErrorListener onErrorListener = new MediaPlayer.OnErrorListener() {
        @Override
        public boolean onError(MediaPlayer mp, int what, int extra) {
            Log.v(TAG, "Error");
            releaseMediaPlayer();
            return true;
        }
    };

    private MediaPlayer.OnCompletionListener onCompletionListener = new MediaPlayer.OnCompletionListener() {
        @Override
        public void onCompletion(MediaPlayer mp) {
            Log.v(TAG, "Complete - Looping is " + mediaPlayer.isLooping());
            if (!mediaPlayer.isLooping()) {
                releaseMediaPlayer();
                stopSelf();
            }
        }
    };

    //Public API exposed to bound service
    public int getCurrentTime() {
        if (mediaState == MediaState.STOPPED) {
            return 0;
        }
        return mediaPlayer.getCurrentPosition();
    }

    public int getDuration() {
        if (mediaState == MediaState.STOPPED) {
            return 0;
        }
        return mediaPlayer.getDuration();
    }

    public void seekTo(int time) {
        if (mediaState == MediaState.STOPPED) {
            return;
        }
        mediaPlayer.seekTo(time);
    }

    public boolean isRepeat() {
        if (mediaState == MediaState.STOPPED) {
            return false;
        }
        return mediaPlayer.isLooping();

    }

    public void setRepeat(boolean repeat) {
        Log.v(TAG, "set repeat " + String.valueOf(repeat));
        if (mediaState == MediaState.STOPPED) {
            return;
        }
        mediaPlayer.setLooping(repeat);
    }

    public boolean isPlaying() {
        return mediaState == MediaState.PLAYING;
    }
}
