package com.shawnpan.musicbookmarker.audioplayer;

import android.content.Context;
import android.media.AudioManager;
import android.media.MediaCodec;
import android.net.Uri;
import android.os.Handler;
import android.util.Log;

import com.google.android.exoplayer.ExoPlaybackException;
import com.google.android.exoplayer.ExoPlayer;
import com.google.android.exoplayer.MediaCodecAudioTrackRenderer;
import com.google.android.exoplayer.MediaCodecSelector;
import com.google.android.exoplayer.MediaCodecTrackRenderer;
import com.google.android.exoplayer.audio.AudioCapabilities;
import com.google.android.exoplayer.audio.AudioTrack;
import com.google.android.exoplayer.drm.DrmSessionManager;
import com.google.android.exoplayer.extractor.ExtractorSampleSource;
import com.google.android.exoplayer.upstream.Allocator;
import com.google.android.exoplayer.upstream.DataSource;
import com.google.android.exoplayer.upstream.DefaultAllocator;
import com.google.android.exoplayer.upstream.DefaultUriDataSource;

/**
 * ExoPlayer based implementation of AudioPlayer
 */
public class ExoPlayerAudioPlayer implements AudioPlayer {
    private static final String TAG = "ExoPlayerAudioPlayer";
    private static final int BUFFER_SEGMENT_SIZE = 64 * 1024;
    private static final int BUFFER_SEGMENT_COUNT = 256;
    private static final int TRACK_COUNT = 1;

    private ExoPlayer exoPlayer;
    private OnDoneListener onDoneListener;
    private volatile boolean looping = false;

    public ExoPlayerAudioPlayer() {
        exoPlayer = ExoPlayer.Factory.newInstance(TRACK_COUNT);
        exoPlayer.addListener(playerListener);
    }

    @Override
    public void start() {
        exoPlayer.setPlayWhenReady(true);
    }

    @Override
    public void pause() {
        exoPlayer.setPlayWhenReady(false);
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
    public void reset() {
        exoPlayer.stop();
        exoPlayer.seekTo(0);
    }

    @Override
    public void release() {
        if (exoPlayer != null) {
            exoPlayer.release();
            exoPlayer = null;
        }
    }

    @Override
    public void seekTo(int msec) {
        exoPlayer.seekTo(msec);
    }

    @Override
    public int getCurrentPosition() {
        return (int) exoPlayer.getCurrentPosition();
    }

    @Override
    public int getDuration() {
        return (int) exoPlayer.getDuration();
    }

    @Override
    public boolean isPlaying() {
        return exoPlayer.getPlaybackState() == ExoPlayer.STATE_READY && exoPlayer.getPlayWhenReady();
    }

    @Override
    public boolean isReady() {
        return exoPlayer.getPlaybackState() == ExoPlayer.STATE_READY || exoPlayer.getPlaybackState() == ExoPlayer.STATE_BUFFERING;
    }

    @Override
    public boolean isLooping() {
        return looping;
    }

    @Override
    public void setLooping(boolean looping) {
        this.looping = looping;
    }

    @Override
    public void playUri(Context context, Uri uri) {
        Allocator allocator = new DefaultAllocator(BUFFER_SEGMENT_SIZE);
        DataSource dataSource = new DefaultUriDataSource(context, TAG);
        ExtractorSampleSource sampleSource = new ExtractorSampleSource(uri, dataSource, allocator, BUFFER_SEGMENT_SIZE * BUFFER_SEGMENT_COUNT);
        Handler handler = null;
        DrmSessionManager drmSessionManager = null;
        MediaCodecAudioTrackRenderer audioTrackRenderer = new MediaCodecAudioTrackRenderer(
                sampleSource, MediaCodecSelector.DEFAULT, drmSessionManager, true, handler, eventListener, AudioCapabilities.getCapabilities(context), AudioManager.STREAM_MUSIC);
        exoPlayer.prepare(audioTrackRenderer);
        exoPlayer.setPlayWhenReady(true);
    }

    @Override
    public void setOnDoneListener(OnDoneListener onDoneListener) {
        this.onDoneListener = onDoneListener;
    }

    private void handleError(String message) {
        Log.v(TAG, message);
        if (onDoneListener != null) {
            onDoneListener.onDone(message);
        }
    }

    //Listeners
    private MediaCodecAudioTrackRenderer.EventListener eventListener = new MediaCodecAudioTrackRenderer.EventListener() {
        @Override
        public void onAudioTrackInitializationError(AudioTrack.InitializationException e) {
            handleError(e.getMessage());
        }

        @Override
        public void onAudioTrackWriteError(AudioTrack.WriteException e) {
            handleError(e.getMessage());
        }

        @Override
        public void onAudioTrackUnderrun(int bufferSize, long bufferSizeMs, long elapsedSinceLastFeedMs) {
            //TODO
        }

        @Override
        public void onDecoderInitializationError(MediaCodecTrackRenderer.DecoderInitializationException e) {
            handleError(e.getMessage());
        }

        @Override
        public void onCryptoError(MediaCodec.CryptoException e) {
            handleError(e.getMessage());
        }

        @Override
        public void onDecoderInitialized(String decoderName, long elapsedRealtimeMs, long initializationDurationMs) {
            //No-op
        }
    };

    private ExoPlayer.Listener playerListener = new ExoPlayer.Listener() {
        @Override
        public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {
            if (playbackState == ExoPlayer.STATE_ENDED) {
                Log.v(TAG, "State Ended");
                if (looping) {
                    seekTo(0);
                } else {
                    if (onDoneListener != null) {
                        onDoneListener.onDone(null);
                    }
                }
            }
        }

        @Override
        public void onPlayWhenReadyCommitted() {
            //No-op
        }

        @Override
        public void onPlayerError(ExoPlaybackException error) {
            handleError(error.getMessage());
        }
    };
}
