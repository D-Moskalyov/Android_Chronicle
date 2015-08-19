package com.chronicle.app;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.opengl.EGLSurface;
import android.os.AsyncTask;
import android.os.Build;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.os.Bundle;
import android.view.Window;
import android.widget.Button;
import android.widget.Toast;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.maps.*;
import com.google.android.gms.maps.internal.StreetViewLifecycleDelegate;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import wikipedia.Wiki;

import java.io.IOException;
import java.net.URLDecoder;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Objects;
//import kankan.wheel.widget;


public class MainActivity extends FragmentActivity implements OnMapReadyCallback {

    //SupportMapFragment mapFragment;
    //GoogleMap map;
    //final String TAG = "myLogs";
    SharedPreferences settings;
    private static final int SHOW_PREFERENCES = 1;
    Button mainButton;

    Wiki wikipedia;

    DBHelper dbHelper;
    SQLiteDatabase db;

    SimpleDateFormat dateFormat;

    GetPageAsync getPageAsync;

    String LOG_TAG = "INF:";

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        //settings = getSharedPreferences(getString(R.string.preference_file_key), 0);
        setContentView(R.layout.main_layout);
        mainButton = (Button) this.findViewById(R.id.settingsButton);
        mainButton.setText("main");

        dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        wikipedia = new Wiki();

        dbHelper = new DBHelper(this);
        db = dbHelper.getWritableDatabase();

        ContentValues cv = new ContentValues();

        cv.put("year", 1935);
        cv.put("revisionID", 1);
        Date date = new Date();
        cv.put("lastUpdate", dateFormat.format(date));

        long id = db.insert("Page", null, cv);
        dbHelper.close();

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

//    public void onSetTen(View view){
//        SharedPreferences.Editor editor = settings.edit();
//        editor.putInt("value", 10);
//
//        editor.commit();
//    }
//
//    public void onSetFive(View view){
//        SharedPreferences.Editor editor = settings.edit();
//        editor.putInt("value", 5);
//
//        editor.commit();
//    }
//
//    public void onShowValue(View view){
//        int value = settings.getInt("value", 0);
//        Context context = getApplicationContext();
//        Toast.makeText(context, String.valueOf(value), Toast.LENGTH_SHORT).show();
//    }

    public void onClickPreferences(View view){
//        Class c = Build.VERSION.SDK_INT <Build.VERSION_CODES.HONEYCOMB ?
//                OldPreferenceActivity.class : FragmentPreferenceActivity.class;
//        Intent i = new Intent(this, c);
//        startActivityForResult(i, SHOW_PREFERENCES);

//        Intent i = new Intent(this, OldPreferenceActivity.class);
//        startActivityForResult(i, SHOW_PREFERENCES);

        Intent i = new Intent(this, SettingActivity.class);
        startActivityForResult(i, SHOW_PREFERENCES);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode == SHOW_PREFERENCES) {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

//    public Objects onRetainNonConfigurationInstance(){
//        return getPageAsync;
//    }

    @Override
    protected void onResume() {
        super.onResume();
        //get getSharedPreferences year
        settings = getSharedPreferences(getString(R.string.preference_file_key), 0);
        String intervalString = settings.getString("intervalString", "Set Interval");
        mainButton.setText(intervalString);

//        try {
//            wikipedia.getPageText("1925_год");
//        } catch (IOException e) {
//            e.printStackTrace();
//        }

        //getPageAsync = (GetPageAsync) getLastNonConfigurationInstance();
        //if(getPageAsync == null)
        int startDate = 1935;//значения из Preference
        int finishDate = 1936;//значения из Preference

        Calendar thatDay = Calendar.getInstance();
        if(true) {//изменение дат
            SQLiteDatabase db = dbHelper.getWritableDatabase();

            Cursor cursor = db.query("Page", new String[]{"lastUpdate"}, "year = ?",
                    new String[]{Integer.toString(startDate)}, null, null, null);

            if(cursor != null){
                if(cursor.moveToFirst()){
                    do{
                        String str = "";
                        for(String cn : cursor.getColumnNames()){
                            str = cursor.getString(cursor.getColumnIndex(cn));
                        }
                        Log.d(LOG_TAG, str);
                        try {
                            Date date = dateFormat.parse(str);
                            thatDay.setTime(date);
                        } catch (ParseException e) {
                            e.printStackTrace();
                        }
                    } while (cursor.moveToNext());
                }

                Calendar today = Calendar.getInstance();
                long diff = today.getTimeInMillis() - thatDay.getTimeInMillis(); //result in millis

                if(diff / (60 * 60 * 1000) > 24) {
                    //проверяем версию ? парсим и обновляем : берём из БД

                    getPageAsync = new GetPageAsync();
                    getPageAsync.execute(("1935_год"));
                }
            }

            else {
                //парсим и записываем в БД
            }

            cursor.close();



            dbHelper.close();
        }
    }


    private class GetPageAsync extends AsyncTask<String, String, String> {

        protected String doInBackground(String... strs) {
            int count = strs.length;
            String page = null;
            //long totalSize = 0;
            for (int i = 0; i < count; i++) {
                try {
                    page = wikipedia.getPageText(strs[i]);
                } catch (IOException e) {
                    e.printStackTrace();
                    return page;
                }
            }
            return page;
        }

//        protected void onProgressUpdate(Integer... progress) {
//            setProgressPercent(progress[0]);
//        }
//
        protected void onPostExecute(String result) {
            if(result == null || result.contains("#перенаправление"))
                return;
            String str = result;
        }
    }




    class DBHelper extends SQLiteOpenHelper{

        public DBHelper(Context context){
            super(context, "chronDB", null, 1);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            Log.d(LOG_TAG, "--- onCreate ---");
            db.execSQL("create table Page " +
                    "(id integer primary key autoincrement, " +
                    "year integer not null unique, " + "revisionID integer, " + "lastUpdate text)");

            db.execSQL("create table Event " +
                    "(id integer primary key autoincrement, " +
                    "lat_dir text, " + "lon_dir text, " +
                    "lat_deg integer, " + "lon_deg integer, " +
                    "lat_min integer, " + "lon_min integer, " +
                    "lat_sec integer, " + "lon_sec integer)");
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

        }
    }
}
