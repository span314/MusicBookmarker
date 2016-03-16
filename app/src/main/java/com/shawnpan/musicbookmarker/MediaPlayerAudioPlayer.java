package com.shawnpan.musicbookmarker;

import android.content.Context;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.PowerManager;
import android.util.Log;

import java.io.IOException;

/**
 * AudioPlayer implementation with default Android MediaPlayer.
 * This implementation has known hardware specific bugs.
 */
public class MediaPlayerAudioPlayer implements AudioPlayer {
    private static final String TAG = "MediaPlayerAudioPlayer";
    private volatile boolean ready = false;
    private MediaPlayer mediaPlayer;
    private OnDoneListener onDoneListener;

    /**
     * Constructor
     */
    public MediaPlayerAudioPlayer() {
        mediaPlayer = new MediaPlayer();
        mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        mediaPlayer.setVolume(1.0F, 1.0F);
        mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mp) {
                start();
                ready = true;
            }
        });
        mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                /*
                Whether onCompletion is called when the playback is looping seems to be hardware
                implementation dependent. We only want to fire the done event is the playback is
                actually stopped.
                 */
                if (!isLooping()) {
                    if (onDoneListener != null) {
                        onDoneListener.onDone(null);
                    }
                }
            }
        });
        mediaPlayer.setOnErrorListener(new MediaPlayer.OnErrorListener() {
            @Override
            public boolean onError(MediaPlayer mp, int what, int extra) {
                if (onDoneListener != null) {
                    String message = "MediaPlayer error with code " + extra;
                    onDoneListener.onDone(message);
                }
                return true;
            }
        });
    }

    @Override
    public void playUri(Context context, Uri uri) {
        mediaPlayer.setWakeMode(context, PowerManager.PARTIAL_WAKE_LOCK);
        try {
            mediaPlayer.setDataSource(context, uri);
        } catch (IOException ie) {
            Log.e(TAG, "Failed to load music from Uri");
            return;
        }
        mediaPlayer.prepareAsync();
    }

    @Override
    public void togglePlayPause() {
        if (isPlaying()) {
            pause();
        } else {
            start();
        }
    }

    @Override
    public boolean isReady() {
        return ready;
    }

    @Override
    public void reset() {
        ready = false;
        mediaPlayer.reset();
    }

    @Override
    public void release() {
        ready = false;
        mediaPlayer.release();
    }

    @Override
    public void stop() {
        ready = false;
        mediaPlayer.stop();
    }

    @Override
    public void start() {
        mediaPlayer.start();
    }

    @Override
    public void pause() {
        mediaPlayer.pause();
    }

    @Override
    public void seekTo(int msec) {
        mediaPlayer.seekTo(msec);
    }

    @Override
    public int getCurrentPosition() {
        return mediaPlayer.getCurrentPosition();
    }

    @Override
    public int getDuration() {
        return mediaPlayer.getDuration();
    }

    @Override
    public boolean isPlaying() {
        return mediaPlayer.isPlaying();
    }

    @Override
    public boolean isLooping() {
        return mediaPlayer.isLooping();
    }

    @Override
    public void setLooping(boolean looping) {
        mediaPlayer.setLooping(looping);
    }

    @Override
    public void setOnDoneListener(OnDoneListener onDoneListener) {
        this.onDoneListener = onDoneListener;
    }
}
