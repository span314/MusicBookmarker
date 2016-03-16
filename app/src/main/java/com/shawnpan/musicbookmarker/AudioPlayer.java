package com.shawnpan.musicbookmarker;

import android.content.Context;
import android.net.Uri;

/**
 * Simple audio player interface
 */
public interface AudioPlayer {

    enum State {
        PLAYING,
        PAUSED,
        STOPPED
    }

    void start();

    void stop();

    void pause();

    void reset();

    void release();

    void seekTo(int msec);

    int getCurrentPosition();

    int getDuration();

    boolean isPlaying();

    boolean isLooping();

    void setLooping(boolean looping);

    void playUri(Context context, Uri uri);
}
