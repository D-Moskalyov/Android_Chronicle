package com.chronicle.app;

import android.annotation.TargetApi;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;

import java.util.List;

public class FragmentPreferenceActivity extends PreferenceActivity {

//    @Override
//    protected void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        addPreferencesFromResource(R.xml.userpreferences);
//    }


    @Override
    public void onBuildHeaders(List<Header> target) {
        loadHeadersFromResource(R.xml.userpreferenceheaders, target);
    }

    public static class UserFragmentPreference extends PreferenceFragment {

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.userpreferences);
        }
    }
}
