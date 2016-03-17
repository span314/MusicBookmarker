package com.shawnpan.musicbookmarker;

import android.content.Context;
import android.graphics.Rect;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.widget.AutoCompleteTextView;

/**
 * Created by shawn on 3/17/16.
 */
public class AutoClearAutoCompleteTextView extends AutoCompleteTextView {
    private String currentText;

    public AutoClearAutoCompleteTextView(Context context) {
        super(context);
    }

    public AutoClearAutoCompleteTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public AutoClearAutoCompleteTextView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public boolean enoughToFilter() {
        return true;
    }

    @Override
    protected void onFocusChanged(boolean focused, int direction, Rect previouslyFocusedRect) {
        super.onFocusChanged(focused, direction, previouslyFocusedRect);
        String text = getText().toString();
        if (focused) {
            if (text.length() > 0) {
                currentText = text;
            }
            setText("");
            performFiltering("", 0);
        } else if (text.length() == 0) {
            setText(currentText);
        }
    }

    //Storing current text state
    @Override
    public Parcelable onSaveInstanceState() {
        Parcelable superState = super.onSaveInstanceState();

        SavedState ss = new SavedState(superState);
        ss.currentText = this.currentText;
        return ss;
    }

    @Override
    public void onRestoreInstanceState(Parcelable state) {
        if(!(state instanceof SavedState)) {
            super.onRestoreInstanceState(state);
            return;
        }

        SavedState ss = (SavedState) state;
        super.onRestoreInstanceState(ss.getSuperState());
        this.currentText = ss.currentText;
    }

    static class SavedState extends BaseSavedState {
        String currentText;

        SavedState(Parcelable superState) {
            super(superState);
        }

        private SavedState(Parcel in) {
            super(in);
            this.currentText = in.readString();
        }

        @Override
        public void writeToParcel(Parcel out, int flags) {
            super.writeToParcel(out, flags);
            out.writeString(this.currentText);
        }

        //required field that makes Parcelables from a Parcel
        public static final Parcelable.Creator<SavedState> CREATOR =
                new Parcelable.Creator<SavedState>() {
                    public SavedState createFromParcel(Parcel in) {
                        return new SavedState(in);
                    }
                    public SavedState[] newArray(int size) {
                        return new SavedState[size];
                    }
                };
    }
}
