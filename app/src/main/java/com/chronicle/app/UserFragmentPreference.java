package com.chronicle.app;

import android.os.Bundle;
import android.preference.PreferenceFragment;

public class UserFragmentPreference extends PreferenceFragment {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.userpreferences);
    }
}
