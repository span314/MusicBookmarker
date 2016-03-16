package com.shawnpan.musicbookmarker;

import android.content.Context;
import android.net.Uri;

/**
 * Simple audio player interface
 */
public interface AudioPlayer {

    interface OnDoneListener {
        void onDone(String error);
    }

    void start();

    void stop();

    void pause();

    void togglePlayPause();

    void reset();

    void release();

    void seekTo(int msec);

    int getCurrentPosition();

    int getDuration();

    boolean isPlaying();

    boolean isReady();

    boolean isLooping();

    void setLooping(boolean looping);

    void playUri(Context context, Uri uri);

    void setOnDoneListener(OnDoneListener onDoneListener);
}
