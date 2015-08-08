package com.chronicle.app;

import android.content.Context;
import android.content.Intent;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.util.AttributeSet;
import android.view.View;

/**
 * Created by ִלטענטי on 06.08.2015.
 */
public class WheelViewPreference extends Preference {

    public WheelViewPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public WheelViewPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public WheelViewPreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    public WheelViewPreference(Context context) {
        super(context);
    }

    @Override
    protected void onBindView(View view) {
        super.onBindView(view);

//        CheckBoxPreference cBP;
//        View checkboxView = view.findViewById(com.android.internal.R.id.checkbox);
    }

    @Override
    public void setIntent(Intent intent) {
        intent = new Intent(getContext(), WheelViewActivity.class);
        super.setIntent(intent);

    }
}
