package com.shawnpan.musicbookmarker;

import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.os.SystemClock;

/**
 * Wrapper around audio player that supports a leader segment and delegates to another audio player.
 * Negative positions represent a time in the leader segment.
 */
public class AudioPlayerWithLeader implements AudioPlayer {
    private AudioPlayer player;

    private enum CountDownState {
        ACTIVE, //timer is counting down
        PAUSED, //timer is paused
        INACTIVE //timer is not active
    }
    private CountDownState countDownState = CountDownState.INACTIVE;
    private long leaderPosition; //position in leader in milliseconds (negative), valid when state is paused in leader
    private long startTimeMilliseconds; //time to post the start, valid when state is active
    private Handler countdownHandler = new Handler();
    private Runnable countdownRunnable = new Runnable() {
        @Override
        public void run() {
            countDownState = CountDownState.INACTIVE;
            player.start();
        }
    };

    /**
     * Constructor
     * @param audioPlayer AudioPlayer to wrap
     */
    public AudioPlayerWithLeader(AudioPlayer audioPlayer) {
        player = audioPlayer;
    }

    /**
     * Stop the leader countdown
     */
    private void stopCountdown() {
        if (countDownState == CountDownState.ACTIVE) {
            countdownHandler.removeCallbacks(countdownRunnable);
            leaderPosition = SystemClock.uptimeMillis() - startTimeMilliseconds;
            countDownState = CountDownState.PAUSED;
        }
    }

    /**
     * Start the leader countdown
     */
    private void startCountdown() {
        startTimeMilliseconds = SystemClock.uptimeMillis() - leaderPosition;
        countdownHandler.postAtTime(countdownRunnable, startTimeMilliseconds);
        countDownState = CountDownState.ACTIVE;
    }

    @Override
    public void start() {
        if (countDownState == CountDownState.INACTIVE) {
            player.start();
        } else {
            startCountdown();
        }
    }

    @Override
    public void stop() {
        stopCountdown();
        player.stop();
    }

    @Override
    public void pause() {
        stopCountdown();
        player.pause();
    }

    @Override
    public void reset() {
        stopCountdown();
        player.reset();
    }

    @Override
    public void release() {
        stopCountdown();
        player.release();
    }

    /**
     * {@inheritDoc}
     * @param msec position in milliseconds, or position in leader if negative
     */
    @Override
    public void seekTo(int msec) throws IllegalStateException {
        boolean active = isPlaying();
        stopCountdown();
        //TODO
        if (player.isPlaying()) { //Needed for edge case - pause is invalid before first time start is called
            player.pause();
        }
        if (msec < 0) {
            player.seekTo(0);
            leaderPosition = msec;
            countDownState = CountDownState.PAUSED;
        } else {
            player.seekTo(msec);
            countDownState = CountDownState.INACTIVE;
        }
        if (active) {
            start();
        }
    }

    /**
     * {@inheritDoc}
     * May return a negative number representing a position in the leader countdown.
     */
    @Override
    public int getCurrentPosition() {
        if (countDownState == CountDownState.ACTIVE) {
            return (int) (SystemClock.uptimeMillis() - startTimeMilliseconds);
        } else if (countDownState == CountDownState.PAUSED) {
            return (int) leaderPosition;
        } else {
            return player.getCurrentPosition();
        }
    }

    @Override
    public int getDuration() {
        return player.getDuration();
    }

    @Override
    public boolean isPlaying() {
        return player.isPlaying() || countDownState == CountDownState.ACTIVE;
    }

    @Override
    public boolean isLooping() {
        return player.isLooping();
    }

    @Override
    public void setLooping(boolean looping) {
        player.setLooping(looping);
    }

    @Override
    public void playUri(Context context, Uri uri) {
        player.playUri(context, uri);
    }
}
