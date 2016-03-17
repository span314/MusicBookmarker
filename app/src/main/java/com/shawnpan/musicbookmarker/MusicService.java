package com.shawnpan.musicbookmarker;

import android.app.Service;
import android.content.Intent;
import android.net.Uri;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

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
    private AudioPlayer audioPlayer;

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
        audioPlayer.playUri(getApplicationContext(), uri);
    }

    private void togglePlayPause() {
        audioPlayer.togglePlayPause();
    }

    private void initializeMediaPlayer() {
        if (audioPlayer == null) {
            //AudioPlayer internalPlayer = new MediaPlayerAudioPlayer();
            AudioPlayer internalPlayer = new ExoPlayerAudioPlayer();
            audioPlayer = new AudioPlayerWithLeader(internalPlayer);
            audioPlayer.setOnDoneListener(onDoneListener);
        } else {
            audioPlayer.reset();
        }
    }

    private void releaseMediaPlayer() {
        if (audioPlayer != null) {
            audioPlayer.reset();
            audioPlayer.release();
            audioPlayer = null;
        }
    }

    private boolean isStopped() {
        return audioPlayer == null || !audioPlayer.isReady();
    }

    //Listeners
    private AudioPlayer.OnDoneListener onDoneListener = new AudioPlayer.OnDoneListener() {
        @Override
        public void onDone(String error) {
            if (error == null) {
                Log.v(TAG, "Playback complete");
            } else {
                Log.e(TAG, error);
            }
            releaseMediaPlayer();
            stopSelf();
        }
    };

    //Public API exposed to bound service
    public int getCurrentTime() {
        if (isStopped()) {
            return 0;
        }
        return audioPlayer.getCurrentPosition();
    }

    public int getDuration() {
        if (isStopped()) {
            return 0;
        }
        return audioPlayer.getDuration();
    }

    public void seekTo(int time) {
        if (isStopped()) {
            return;
        }
        audioPlayer.seekTo(time);
    }

    public boolean isRepeat() {
        if (isStopped()) {
            return false;
        }
        return audioPlayer.isLooping();
    }

    public void setRepeat(boolean repeat) {
        if (isStopped()) {
            return;
        }
        audioPlayer.setLooping(repeat);
    }

    public boolean isPlaying() {
        return audioPlayer != null && audioPlayer.isPlaying();
    }
}
