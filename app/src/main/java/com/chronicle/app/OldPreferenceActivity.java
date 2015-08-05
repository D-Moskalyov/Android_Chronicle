package com.chronicle.app;

import android.os.Bundle;
import android.preference.PreferenceActivity;
import kankan.wheel.widget.WheelView;


public class OldPreferenceActivity extends PreferenceActivity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        WheelView d;
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.userpreferences);
    }
}
