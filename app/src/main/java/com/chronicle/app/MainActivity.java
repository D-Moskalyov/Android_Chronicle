package com.chronicle.app;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.opengl.EGLSurface;
import android.os.Build;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.os.Bundle;
import android.view.Window;
import android.widget.Toast;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.maps.*;
import com.google.android.gms.maps.internal.StreetViewLifecycleDelegate;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
//import kankan.wheel.widget;


public class MainActivity extends FragmentActivity implements OnMapReadyCallback {

    //SupportMapFragment mapFragment;
    //GoogleMap map;
    //final String TAG = "myLogs";
    private static final int SHOW_PREFERENCES = 1;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.main_layout);

        //Fragment.setHasOptionsMenu(true);

//        mapFragment = (SupportMapFragment)getSupportFragmentManager().findFragmentById(R.id.map);
//        mapFragment.getMapAsync(this);

//        MapFragment mapFragment = (MapFragment) getFragmentManager()
//                .findFragmentById(R.id.map);
//        mapFragment.getMapAsync(this);

    }

    @Override
    public void onMapReady(GoogleMap map) {
        map.addMarker(new MarkerOptions()
                .position(new LatLng(0, 0))
                .title("Marker"));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.menu_items, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()){
            case R.id.interval :
                Toast.makeText(getApplicationContext(), "Yes", Toast.LENGTH_SHORT).show();
        }
        return true;
    }

    public void onSetTen(View view){
        SharedPreferences settings = getSharedPreferences(getString(R.string.preference_file_key), 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.putInt("value", 10);

        editor.commit();
    }

    public void onSetFive(View view){
        SharedPreferences settings = getSharedPreferences(getString(R.string.preference_file_key), 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.putInt("value", 5);

        editor.commit();
    }

    public void onShowValue(View view){
        SharedPreferences settings = getSharedPreferences(getString(R.string.preference_file_key), 0);
        int value = settings.getInt("value", 0);
        Context context = getApplicationContext();
        Toast.makeText(context, String.valueOf(value), Toast.LENGTH_SHORT).show();
    }

    public void onClickPreferences(View view){
//        Class c = Build.VERSION.SDK_INT <Build.VERSION_CODES.HONEYCOMB ?
//                OldPreferenceActivity.class : FragmentPreferenceActivity.class;
//        Intent i = new Intent(this, c);
//        startActivityForResult(i, SHOW_PREFERENCES);

        Intent i = new Intent(this, OldPreferenceActivity.class);
        startActivityForResult(i, SHOW_PREFERENCES);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode == SHOW_PREFERENCES) {
            super.onActivityResult(requestCode, resultCode, data);
        }


    }
}
