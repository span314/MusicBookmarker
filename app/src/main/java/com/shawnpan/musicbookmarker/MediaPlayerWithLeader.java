package com.shawnpan.musicbookmarker;

import android.media.MediaPlayer;
import android.os.Handler;
import android.os.SystemClock;

/**
 * MediaPlayer subclass with support for a blank leader segment. It behaves like a normal
 * media player, but seeking to negative times to represent the leader segment.
 */
public class MediaPlayerWithLeader extends MediaPlayer {
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
            MediaPlayerWithLeader.super.start();
        }
    };

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
            return super.getCurrentPosition();
        }
    }

    @Override
    public void start() throws IllegalStateException {
        if (countDownState == CountDownState.INACTIVE) {
            super.start();
        } else {
            startCountdown();
        }
    }

    /**
     * {@inheritDoc}
     * @param msec position in milliseconds, or position in leader if negative
     */
    @Override
    public void seekTo(int msec) throws IllegalStateException {
        boolean active = isPlaying() || countDownState == CountDownState.ACTIVE;
        stopCountdown();
        if (isPlaying()) { //Needed for edge case - pause is invalid before first time start is called
            super.pause();
        }
        if (msec < 0) {
            super.seekTo(0);
            leaderPosition = msec;
            countDownState = CountDownState.PAUSED;
        } else {
            super.seekTo(msec);
            countDownState = CountDownState.INACTIVE;
        }
        if (active) {
            start();
        }
    }

    @Override
    public void stop() throws IllegalStateException {
        stopCountdown();
        super.stop();
    }

    @Override
    public void pause() throws IllegalStateException {
        stopCountdown();
        super.pause();
    }

    @Override
    public void reset() {
        stopCountdown();
        super.reset();
    }

    @Override
    public void release() {
        stopCountdown();
        super.release();
    }
}
