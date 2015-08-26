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
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.web.client.RestTemplate;
import wikipedia.Wiki;
import org.apache.lucene.morphology.*;

import java.io.IOException;
import java.net.URLDecoder;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
//import kankan.wheel.widget;


public class MainActivity extends FragmentActivity implements OnMapReadyCallback {

    //SupportMapFragment mapFragment;
    //GoogleMap map;
    //final String TAG = "myLogs";
    SharedPreferences settings;
    private static final int SHOW_PREFERENCES = 1;
    Button mainButton;

    int startDate;
    int finishDate;
    String eventItem;

    Wiki wikipedia;

    DBHelper dbHelper;
    SQLiteDatabase db;

    SimpleDateFormat dateFormat;

    GetPageAsync getPageAsync;
    GetPageRevIDAsync getPageRevIDAsync;

    String LOG_TAG = "INF";

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        //settings = getSharedPreferences(getString(R.string.preference_file_key), 0);
        setContentView(R.layout.main_layout);
        mainButton = (Button) this.findViewById(R.id.settingsButton);
        mainButton.setText("main");

        Log.d(LOG_TAG, "--- onCreate main ---");

        dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        dateFormat.setTimeZone(TimeZone.getTimeZone("Europe/Kiev"));

        wikipedia = new Wiki();

        dbHelper = new DBHelper(this);
        db = dbHelper.getWritableDatabase();

        db.delete("Page", null, null);
        ContentValues cv = new ContentValues();

        cv.put("year", 1914);
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
        startDate = 1914;//значени€ из Preference
        finishDate = 1915;//значени€ из Preference

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

                //if(diff / (60 * 60 * 1000) > 24) {
                    getPageRevIDAsync = new GetPageRevIDAsync();
                    getPageRevIDAsync.execute(("1914_год"));


                //}
            }

            else {
                //парсим и записываем в Ѕƒ
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

            ParseEvent(result);

        }
    }

    private class GetPageRevIDAsync extends AsyncTask<String, String, Long> {

        protected Long doInBackground(String... strs) {
            int count = strs.length;
            long revId = 0;
            //long totalSize = 0;
            for (int i = 0; i < count; i++) {
                revId = wikipedia.getPageRevId("1914_год");
            }
            return revId;
        }

        //        protected void onProgressUpdate(Integer... progress) {
//            setProgressPercent(progress[0]);
//        }
//
        protected void onPostExecute(Long result) {
            if(result == null || result == -1)
                return;
            long res = result;
            SQLiteDatabase db = dbHelper.getWritableDatabase();

            Cursor cursor = db.query("Page", new String[]{"revisionID"}, "year = ?",
                    new String[]{Integer.toString(startDate)}, null, null, null);

            if(cursor != null){
                if(cursor.moveToFirst()){
                    do{
                        long id = -1;
                        for(String cn : cursor.getColumnNames()){
                            id = cursor.getLong(cursor.getColumnIndex(cn));
                        }
                        Log.d(LOG_TAG, Long.toString(id));
                        if(id != res){
                            getPageAsync = new GetPageAsync();
                            getPageAsync.execute(("1914_год"));
                        }
                        else{
                            //берЄм из Ѕƒ
                        }
                    } while (cursor.moveToNext());
                }

            }
        }
    }


    private void ParseEvent(String events){

        dbHelper = new DBHelper(this);

        List<String> eventList = new ArrayList<String>();
        String eventItem = null;
        int start;
        int finish;
        int startEv;
        int finishEv;
        int startRef;
        int finishRef;

        start = events.indexOf("== —обыти€ ==");
        finish = events.indexOf(" ==\n", start + 14);
        if(finish != -1)
            events = events.substring(start + 14, finish);
        else events = events.substring(start + 14);

        startEv = events.indexOf("*");
        finishEv = events.indexOf("*", startEv + 1);

        while(finishEv != -1) {//не последнее событие

            if (finishEv - startEv != 1) {

                if (finishEv - startEv > 17) {//ещЄ что-то кроме даты
                    eventItem = events.substring(startEv + 2, finishEv - 1);

                    int indxSlash = eventItem.indexOf("==");
                    if( indxSlash != -1){
                        eventItem = eventItem.substring(0, indxSlash - 2);
                    }


                    startRef = eventItem.indexOf("<ref");
                    while (startRef != -1) {
                        startRef = eventItem.indexOf("<ref>");
                        if (startRef != -1) {
                            finishRef = eventItem.indexOf("</ref>", startRef);
                            eventItem = eventItem.substring(0, startRef) + eventItem.substring(finishRef + 6);

                            startRef = eventItem.indexOf("<ref");
                        }
                        else{
                            startRef = eventItem.indexOf("<ref");
                            finishRef = eventItem.indexOf("/>", startRef);
                            if(finishRef != -1) {
                                eventItem = eventItem.substring(0, startRef) + eventItem.substring(finishRef + 2);
                            }
                            else{
                                finishRef = eventItem.indexOf("</ref>", startRef);
                                eventItem = eventItem.substring(0, startRef) + eventItem.substring(finishRef + 6);
                            }
                            startRef = eventItem.indexOf("<ref");
                        }

                    }

                    while (eventItem.contains("[[")) {
                        eventItem = TrimHooks(eventItem);
                    }


                    db = dbHelper.getWritableDatabase();

                    ContentValues cv = new ContentValues();

                    cv.put("text", eventItem);
                    cv.put("lat_dir", "NaN");
                    cv.put("lon_dir", "NaN");
                    cv.put("lat_deg", 0);
                    cv.put("lon_deg", 0);
                    cv.put("lat_min", 0);
                    cv.put("lon_min", 0);
                    cv.put("lat_sec", 0);
                    cv.put("lon_sec", 0);

                    long id = db.insert("Event", null, cv);
                }
                else {
                    eventItem = events.substring(startEv + 4, finishEv - 3) + " Ч ";
                }

            } else {//несколько событий

                startEv++;
                finishEv = events.indexOf("*", startEv + 1);

                int indxDef= eventItem.indexOf(" Ч ");

                if(indxDef != -1)
                    eventItem = eventItem.substring(0, indxDef + 3) + //вз€ть дату из старого eventItem
                            events.substring(startEv + 2, finishEv - 1);
                else
                    eventItem = eventItem + //вз€ть дату из старого eventItem
                            events.substring(startEv + 2, finishEv - 1);

                int indxSlash = eventItem.indexOf("==");
                if( indxSlash != -1){
                    eventItem = eventItem.substring(0, indxSlash - 2);
                }

                startRef = eventItem.indexOf("<ref");
                while (startRef != -1) {
                    startRef = eventItem.indexOf("<ref>");
                    if (startRef != -1) {
                        finishRef = eventItem.indexOf("</ref>", startRef);
                        eventItem = eventItem.substring(0, startRef) + eventItem.substring(finishRef + 6);

                        startRef = eventItem.indexOf("<ref");
                    }
                    else{
                        startRef = eventItem.indexOf("<ref");
                        finishRef = eventItem.indexOf("/>", startRef);
                        if(finishRef != -1) {
                            eventItem = eventItem.substring(0, startRef) + eventItem.substring(finishRef + 2);
                        }
                        else{
                            finishRef = eventItem.indexOf("</ref>", startRef);
                            eventItem = eventItem.substring(0, startRef) + eventItem.substring(finishRef + 6);
                        }
                        startRef = eventItem.indexOf("<ref");
                    }

                }

                while (eventItem.contains("[[")) {
                    eventItem = TrimHooks(eventItem);
                }


                db = dbHelper.getWritableDatabase();

                ContentValues cv = new ContentValues();

                cv.put("text", eventItem);
                cv.put("lat_dir", "NaN");
                cv.put("lon_dir", "NaN");
                cv.put("lat_deg", 0);
                cv.put("lon_deg", 0);
                cv.put("lat_min", 0);
                cv.put("lon_min", 0);
                cv.put("lat_sec", 0);
                cv.put("lon_sec", 0);






                org.apache.lucene.morphology.russian.RussianAnalyzer russianAnalyzer;
                LuceneMorphology luceneMorph;


                String url = "https://ajax.googleapis.com/ajax/" +
                        "services/search/web?v=1.0&q={query}";
                RestTemplate restTemplate = new RestTemplate();
                restTemplate.getMessageConverters().add(new StringHttpMessageConverter());
                String result = restTemplate.getForObject(url, String.class, "Android");







                long id = db.insert("Event", null, cv);
            }

            startEv = finishEv;
            finishEv = events.indexOf("*", startEv + 1);
        }
        dbHelper.close();
    }

    private String TrimHooks(String evnt){

        //int startToNext = start;

        int startEv = evnt.indexOf("[[");
        if(startEv != -1){
            int finishEv = evnt.indexOf("]]", startEv + 2);
            //startToNext =+ 4;

            String inHooks = evnt.substring(startEv + 2, finishEv);
            int separ = inHooks.indexOf("|");
            if(separ != -1){
                //startToNext += inHooks.length() - separ - 1;
                inHooks = inHooks.substring(separ + 1, inHooks.length());
            }

            evnt = evnt.substring(0, startEv) + inHooks + evnt.substring(finishEv + 2, evnt.length());

            //TrimHooks(evnt);
            return evnt;
        }
//        else {
//            String hi = evnt;
//            return;
//        }
        return "";
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
