package com.chronicle.app;

import android.content.Context;
import android.preference.Preference;
import android.util.AttributeSet;
import android.view.View;
import android.widget.NumberPicker;

//implements NumberPicker

public class NumberPickerPreference extends Preference {

    public static final int MAX_VALUE = 100;
    public static final int MIN_VALUE = 0;

    private NumberPicker picker;
    private int value;

    public NumberPickerPreference(Context context, AttributeSet attrs) {
        super(context, attrs);

        setLayoutResource(R.layout.number_peaker);

    }



}
