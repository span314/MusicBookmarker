package com.shawnpan.musicbookmarker;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.SeekBar;

/**
 * Created by shawn on 3/6/16.
 */
public class MusicSeekBar extends ViewGroup {
    private static final int LEADER_STEPS = 2000;
    private static final int MUSIC_STEPS = 8000;
    private static final int TOTAL_STEPS = 10000;

    private SeekBar seekBar;
    //TODO Make configurable
    private int textHeight;
    private int leaderMS = 15000;
    private int musicMS = 0;

    private Paint paint;
    private int trackStart;
    private int trackWidth;
    private float markLeftOffset;
    private float markRightOffset;
    private float markTopOffset;
    private float markBottomOffset;


    private volatile boolean userDragging;

    public MusicSeekBar(Context context) {
        super(context);
        init(context);
    }

    public MusicSeekBar(Context context, AttributeSet attr) {
        super(context, attr);
        init(context);
    }

    private void init(Context context) {
        seekBar = new SeekBar(context);
        seekBar.setLayoutParams(new MarginLayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                //TODO test if this works for keyboard events
                if (fromUser && !userDragging && timeChangeListener != null) {
                    timeChangeListener.onTimeChanged(getTime());
                }
                //Invalidate the text, TODO will this call draw on the seekbar twice? partial invalidate?
                MusicSeekBar.this.invalidate();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                userDragging = true;
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                userDragging = false;
                if (timeChangeListener != null) {
                    timeChangeListener.onTimeChanged(getTime());
                }
            }
        });
        seekBar.setMax(TOTAL_STEPS);
        addView(seekBar);
        this.setWillNotDraw(false);
        this.setSaveEnabled(true);

        float fontSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 24, getResources().getDisplayMetrics());
        paint = new Paint();
        paint.setTextSize(fontSize);
        textHeight = (int) (fontSize);
    }


    /**
     * Callback when the user selects a new time on the seekbar.
     */
    public interface OnTimeChangeListener {

        /**
         * Called when user selects a new time
         * @param timeMillis new selected time
         */
        void onTimeChanged(int timeMillis);
    }

    private OnTimeChangeListener timeChangeListener;

    /**
     * @return time in milliseconds
     */
    public int getTime() {
        return stepsToMilliseconds(seekBar.getProgress());
    }

    /**
     * Update the time in milliseconds
     * @param time in ms
     */
    public void updateTime(int time, int duration) {
        if (!userDragging) {
            musicMS = duration;
            seekBar.setProgress(millisecondsToSteps(time));
        }
    }

    private int stepsToMilliseconds(int steps) {
        if (steps < LEADER_STEPS) { //In leader
            return (steps - LEADER_STEPS) * leaderMS / LEADER_STEPS;
        } else { //In music
            return (steps - LEADER_STEPS) * musicMS / MUSIC_STEPS;
        }
    }

    private int millisecondsToSteps(int time) {
        if (time < 0) { //In leader
            return LEADER_STEPS * (time + leaderMS) / leaderMS;
        } else if (musicMS == 0) { //Not initialized
            return LEADER_STEPS;
        } else { //In music
            return LEADER_STEPS + MUSIC_STEPS * time / musicMS;
        }
    }

    public void setOnTimeChangeListener(OnTimeChangeListener listener) {
        timeChangeListener = listener;
    }

    //TODO is synchronized necessary on these methods?
    @Override
    protected synchronized void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        //Measure SeekBar and add extra room for text
        measureChildWithMargins(seekBar, widthMeasureSpec, 0, heightMeasureSpec, textHeight);
        int widthAndState = resolveSizeAndState(seekBar.getMeasuredWidth(), widthMeasureSpec, seekBar.getMeasuredState());
        int heightAndState = resolveSizeAndState(seekBar.getMeasuredHeight() + textHeight, heightMeasureSpec, seekBar.getMeasuredState() << MEASURED_HEIGHT_STATE_SHIFT);
        setMeasuredDimension(widthAndState, heightAndState);
    }

    @Override
    protected synchronized void onLayout(boolean sizeChanged, int l, int t, int r, int b) {
        seekBar.layout(0, 0, seekBar.getMeasuredWidth(), seekBar.getMeasuredHeight());
        if (sizeChanged) {
            trackStart = seekBar.getPaddingLeft();
            trackWidth = seekBar.getMeasuredWidth() - seekBar.getPaddingLeft() - seekBar.getPaddingRight();
            float markHalfWidth = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 1.5F, getResources().getDisplayMetrics());
            markLeftOffset = trackStart - markHalfWidth;
            markRightOffset = trackStart + markHalfWidth;
            markTopOffset = 2 * seekBar.getMeasuredHeight() / 7;
            markBottomOffset = 5 * seekBar.getMeasuredHeight() / 7;
        }
    }

    @Override
    protected synchronized void onDraw(Canvas canvas) {
        Rect drawingRect = new Rect();
        getDrawingRect(drawingRect);

        //paint.setColor(Color.GREEN);
        //canvas.drawRect(drawingRect, paint);

        //Draw annotation marks
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.WHITE);
        int zeroMarkPosition = trackWidth * LEADER_STEPS / TOTAL_STEPS;
        canvas.drawRect(markLeftOffset + zeroMarkPosition, markTopOffset, markRightOffset + zeroMarkPosition, markBottomOffset, paint);

        paint.setStyle(Paint.Style.STROKE);
        paint.setTextAlign(Paint.Align.CENTER);
        canvas.drawText("0", trackStart + zeroMarkPosition, drawingRect.bottom, paint);


        paint.setTextAlign(Paint.Align.LEFT);
        canvas.drawText(Integer.toString(-leaderMS / 1000), drawingRect.left, drawingRect.bottom, paint);

        paint.setTextAlign(Paint.Align.RIGHT);
        String timeText = formatElapsedTime(getTime(), musicMS);
        canvas.drawText(timeText, drawingRect.right, drawingRect.bottom, paint);

        super.onDraw(canvas);
    }

    public static String formatElapsedTime(int elapsedMilliseconds, int totalMilliseconds) {
        if (elapsedMilliseconds < 0) {
            return "-" + formatElapsedTime(-elapsedMilliseconds, totalMilliseconds);
        }
        int elapsedSeconds = (elapsedMilliseconds / 1000) % 60;
        int elapsedMinutes = (elapsedMilliseconds / 60000) % 60;
        int elapsedHours = (elapsedMilliseconds / 3600000) % 60;
        int totalSeconds = (totalMilliseconds / 1000) % 60;
        int totalMinutes = (totalMilliseconds / 60000) % 60;
        int totalHours = (totalMilliseconds / 3600000) % 60;
        if (totalHours > 0) {
            return String.format("%d:%02d:%02d / %d:%02d:%02d",
                    elapsedHours, elapsedMinutes, elapsedSeconds, totalHours, totalMinutes, totalSeconds);
        } else {
            return String.format("%d:%02d / %d:%02d",
                    elapsedMinutes, elapsedSeconds, totalMinutes, totalSeconds);
        }
    }

    @Override
    public void onInitializeAccessibilityEvent(AccessibilityEvent event) {
        super.onInitializeAccessibilityEvent(event);
        event.setClassName(MusicSeekBar.class.getName());
    }

    @Override
    public void onInitializeAccessibilityNodeInfo(AccessibilityNodeInfo info) {
        super.onInitializeAccessibilityNodeInfo(info);
        info.setClassName(MusicSeekBar.class.getName());
    }

    @Override
    public Parcelable onSaveInstanceState() {
        Parcelable superState = super.onSaveInstanceState();
        SavedState ss = new SavedState(superState);
        ss.stateSeekBarPosition = seekBar.getProgress();
        return ss;
    }

    @Override
    public void onRestoreInstanceState(Parcelable state) {
        SavedState ss = (SavedState) state;
        super.onRestoreInstanceState(ss.getSuperState());
        seekBar.setProgress(ss.stateSeekBarPosition);
    }

    static class SavedState extends BaseSavedState {
        int stateSeekBarPosition;

        SavedState(Parcelable superState) {
            super(superState);
        }

        private SavedState(Parcel in) {
            super(in);
            stateSeekBarPosition = in.readInt();
        }

        @Override
        public void writeToParcel(Parcel out, int flags) {
            super.writeToParcel(out, flags);
            out.writeInt(stateSeekBarPosition);
        }

        public static final Creator<SavedState> CREATOR
                = new Creator<SavedState>() {
            public SavedState createFromParcel(Parcel in) {
                return new SavedState(in);
            }

            public SavedState[] newArray(int size) {
                return new SavedState[size];
            }
        };
    }
}
