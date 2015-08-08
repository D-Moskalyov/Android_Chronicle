package com.chronicle.app;

import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.PreferenceActivity;
import android.view.View;
import kankan.wheel.widget.*;


public class OldPreferenceActivity extends PreferenceActivity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        //WheelView d;
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.userpreferences);

    }
}
