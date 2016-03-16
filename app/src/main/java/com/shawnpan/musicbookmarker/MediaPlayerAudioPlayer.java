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
public class MediaPlayerAudioPlayer extends MediaPlayer implements AudioPlayer {
    private static final String TAG = "MediaPlayerAudioPlayer";

    @Override
    public void playUri(Context context, Uri uri) {
        setWakeMode(context, PowerManager.PARTIAL_WAKE_LOCK);
        setAudioStreamType(AudioManager.STREAM_MUSIC);
        setVolume(1.0F, 1.0F);
        try {
            setDataSource(context, uri);
        } catch (IOException ie) {
            Log.e(TAG, "Failed to load music from Uri");
            return;
        }
        prepareAsync();
    }
}
