package com.chronicle.app;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.support.annotation.IntegerRes;
import android.support.v4.app.FragmentActivity;
import android.support.v4.util.ArrayMap;
import android.util.JsonReader;
import android.util.Log;
import android.util.Pair;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;
import com.google.android.gms.maps.*;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.maps.android.clustering.ClusterManager;
import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.lucene.morphology.russian.RussianLuceneMorphology;
import org.json.JSONObject;
import org.json.JSONTokener;
import wikipedia.Wiki;
import org.apache.lucene.morphology.*;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Array;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

//implements OnMapReadyCallback
public class MainActivity extends FragmentActivity implements OnMapReadyCallback {


    MapFragment mapFragment = null;
    GoogleMap map = null;
    Marker marker = null;

    //final String TAG = "myLogs";
    SharedPreferences settings;
    private static final int SHOW_PREFERENCES = 1;
    Button mainButton;

    int startDate;
    int finishDate;
    String eventItem;
    List<String> wordBaseForms;
    char noun = 'С';
    private int firstCenturyAC;
    int centuryStart;
    int centuryFinish;
    int yearStart;
    int yearFinish;

    Wiki wikipedia;

    DBHelper dbHelper;
    SQLiteDatabase db;

    SimpleDateFormat dateFormat;

    //GetPageAsync getPageAsync;
    //GetPageRevIDAsync getPageRevIDAsync;
    //GetPageTemplatesAsync getPageTemplatesAsync;
    LocationManager locationManager;
    ClusterManager<Coordinate> mClusterManager;

    String LOG_TAG = "INF";

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_layout);
        firstCenturyAC = (int) getResources().getInteger(R.integer.firstCenturyAC);
        //this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        settings = getSharedPreferences(getString(R.string.preference_file_key), 0);

        mapFragment = (MapFragment) getFragmentManager().findFragmentById(R.id.map);

        map = mapFragment.getMap();
        if(map == null){
            finish();
            return;
        }

        dbHelper = new DBHelper(this);

        InitClusterer();
        //onMapReady(map);

        wikipedia = new Wiki();

        locationManager = (LocationManager)getSystemService(LOCATION_SERVICE);


        //String jsn = StringEscapeUtils.unescapeJava("\\u041a\\u0438\\u0435\\u0432\\");


//        LuceneMorphology luceneMorph = null;
//        try {
//            luceneMorph = new RussianLuceneMorphology();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//        String _text = "Бостон";
//
//        _text = _text.toLowerCase();
//        //_textUTF8 = _textUTF8.toLowerCase();
//        try {
//            wordBaseForms = luceneMorph.getMorphInfo(_text);
//        }catch (WrongCharaterException e){
//            e.printStackTrace();
//        }
//        if(wordBaseForms != null) {
//            int pos = (wordBaseForms.get(0)).indexOf("|") + 3;
//            String partOfSpeach = wordBaseForms.get(0).substring(pos, pos + 1);
//            char pOS = partOfSpeach.charAt(0);
//
//            if(pOS == noun){
//                String lex = wordBaseForms.get(0).substring(0, pos - 3);
//
//                getPageTemplatesAsync = new GetPageTemplatesAsync();
//                String utf8lex = "";
//                try {
//                    utf8lex = new String(lex.getBytes("UTF-8"), "UTF-8");
//                } catch (UnsupportedEncodingException e) {
//                    e.printStackTrace();
//                }
//                getPageTemplatesAsync.execute(utf8lex);
//
//            }
//
//        }


        int centuryStart = settings.getInt("centStartIndx", 0);
        int centuryFinish = settings.getInt("centFinishIndx", 0);
        int yearStart = settings.getInt("yearStartIndx", 0);
        int yearFinish = settings.getInt("yearFinishIndx", 0);

        int startYEAR;
        int finishYEAR;

        if(centuryStart < firstCenturyAC){
            startYEAR = (centuryStart - firstCenturyAC) * 100 - yearStart;
        }
        else{
            startYEAR = (centuryStart - firstCenturyAC + 1) * 100 + yearStart;
        }
        if(centuryFinish < firstCenturyAC){
            finishYEAR = (centuryFinish - firstCenturyAC) * 100 - yearFinish;
        }
        else{
            finishYEAR = (centuryFinish - firstCenturyAC + 1) * 100 + yearFinish;
        }

        mainButton = (Button) this.findViewById(R.id.settingsButton);
        mainButton.setText(startYEAR + " - " + finishYEAR);

        SQLiteDatabase db = dbHelper.getWritableDatabase();

        Cursor cursor = db.query("Pages", new String[]{"year", "lastUpdate"}, "year >= ? and year <= ?",
                new String[]{Integer.toString(startYEAR, finishYEAR)}, null, null, null);

        ArrayList<Integer> listForFirstInit = new ArrayList<Integer>();
        ArrayList<Integer> listForUpdate = new ArrayList<Integer>();
        ArrayList<Integer> listForNotUpdate = new ArrayList<Integer>();

        if(cursor == null || cursor.getCount() == 0){//ни одной записи
            for(int _year = startYEAR; _year <= finishYEAR; _year++) {
                listForFirstInit.add(_year);
            }
        }

        else {

            do {

                int yearFromDB = cursor.getInt(cursor.getColumnIndex("year"));
                String str = cursor.getString(cursor.getColumnIndex("lastUpdate"));

                Log.d(LOG_TAG, str);
                try {
                    Calendar thatDay = Calendar.getInstance();
                    Date date = dateFormat.parse(str);
                    thatDay.setTime(date);

                    Calendar today = Calendar.getInstance();

                    long diff = today.getTimeInMillis() - thatDay.getTimeInMillis(); //result in millis

                    if (diff / (60 * 60 * 1000) > 24) {
                        listForUpdate.add(yearFromDB);
                    } else {
                        listForNotUpdate.add(yearFromDB);
                    }

                } catch (ParseException e) {
                    e.printStackTrace();
                }

            } while (cursor.moveToNext());

            for (int _year = startYEAR; _year <= finishYEAR; _year++) {
                if(!(listForUpdate.contains(_year) || listForNotUpdate.contains(_year))){
                    listForFirstInit.add((_year));
                }
            }
        }

        cursor.close();
        dbHelper.close();

//        for(int yearForFirstInit : listForFirstInit){
//            GetPageAsync getPageAsync = new GetPageAsync();
//            getPageAsync.execute(yearForFirstInit);
//        }
        if(listForFirstInit != null & listForFirstInit.size() != 0) {
            Integer[] masForFirstInit = listForFirstInit.toArray(new Integer[listForFirstInit.size()]);
            GetPageAsync getPageAsync = new GetPageAsync();
            getPageAsync.execute(masForFirstInit);
        }
//
//        for(int yearForUpdate : listForNotUpdate) {
//            GetPageRevIDAsync getPageRevIDAsync = new GetPageRevIDAsync();
//            getPageRevIDAsync.execute(yearForUpdate);
//        }

        if(listForUpdate != null & listForUpdate.size() != 0) {
            Integer[] listForNotUpdateMas = listForNotUpdate.toArray(new Integer[listForUpdate.size()]);
            GetPageRevIDAsync getPageRevIDAsync = new GetPageRevIDAsync();
            getPageRevIDAsync.execute(listForNotUpdateMas);
        }


        Log.d(LOG_TAG, "--- onCreate main ---");

//        dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
//        dateFormat.setTimeZone(TimeZone.getTimeZone("Europe/Kiev"));
//
//        dbHelper = new DBHelper(this);
//        db = dbHelper.getWritableDatabase();
//
//        db.delete("Page", null, null);
//        ContentValues cv = new ContentValues();
//
//        cv.put("year", 1914);
//        cv.put("revisionID", 1);
//
//        Date date = new Date();
//        cv.put("lastUpdate", dateFormat.format(date));
//
//        long id = db.insert("Page", null, cv);
//        dbHelper.close();

    }

    private void InitClusterer(){
        mClusterManager = new ClusterManager<Coordinate>(this, map);
        map.setOnCameraChangeListener(mClusterManager);
        map.setOnMarkerClickListener(mClusterManager);

        //addItems();
    }

//    private void addItems() {
//
//        // Set some lat/lng coordinates to start with.
//        double lat = 51.5145160;
//        double lng = -0.1270060;
//
//        // Add ten cluster items in close proximity, for purposes of this example.
//        for (int i = 0; i < 10; i++) {
//            double offset = i / 60d;
//            lat = lat + offset;
//            lng = lng + offset;
//            Coordinate offsetItem = new Coordinate(lat, lng);
//            mClusterManager.addItem(offsetItem);
//        }
//    }

    @Override
    public void onMapReady(GoogleMap map) {
//        map.addMarker(new MarkerOptions()
//                .position(new LatLng(0, 0))
//                .title("Marker"));
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

    @Override
    protected void onResume() {
        super.onResume();
//        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,
//                1000 * 10, 100, locationListener);
//        locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER,
//                1000 * 10, 100, locationListener);
//        CheckEnable();
//
//        //get getSharedPreferences year
//        settings = getSharedPreferences(getString(R.string.preference_file_key), 0);
//        String intervalString = settings.getString("intervalString", "Set Interval");
//        mainButton.setText(intervalString);
//
//        startDate = 1914;//значения из Preference
//        finishDate = 1915;//значения из Preference
//
//        Calendar thatDay = Calendar.getInstance();
//        if(true) {//изменение дат
//            SQLiteDatabase db = dbHelper.getWritableDatabase();
//
//            Cursor cursor = db.query("Page", new String[]{"lastUpdate"}, "year = ?",
//                    new String[]{Integer.toString(startDate)}, null, null, null);
//
//            if(cursor != null){
//                if(cursor.moveToFirst()){
//                    do{
//                        String str = "";
//                        for(String cn : cursor.getColumnNames()){
//                            str = cursor.getString(cursor.getColumnIndex(cn));
//                        }
//                        Log.d(LOG_TAG, str);
//                        try {
//                            Date date = dateFormat.parse(str);
//                            thatDay.setTime(date);
//                        } catch (ParseException e) {
//                            e.printStackTrace();
//                        }
//                    } while (cursor.moveToNext());
//                }
//
//                Calendar today = Calendar.getInstance();
//                long diff = today.getTimeInMillis() - thatDay.getTimeInMillis(); //result in millis
//
//                //if(diff / (60 * 60 * 1000) > 24) {
//                    getPageRevIDAsync = new GetPageRevIDAsync();
//                    getPageRevIDAsync.execute(("1914_год"));
//
//
//                //}
//            }
//
//            else {
//                //парсим и записываем в БД
//            }
//
//            cursor.close();
//
//
//
//            dbHelper.close();
//        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        locationManager.removeUpdates(locationListener);
    }



    private LocationListener locationListener = new LocationListener() {
        @Override
        public void onLocationChanged(Location location) {
            ShowLocation(location);
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
            if(provider.equals(locationManager.GPS_PROVIDER)){
                //реакция на изменение
            }
            else if(provider.equals(locationManager.NETWORK_PROVIDER)){
                //реакция на изменение
            }
        }

        @Override
        public void onProviderEnabled(String provider) {
            CheckEnable();
            ShowLocation(locationManager.getLastKnownLocation(provider));
        }

        @Override
        public void onProviderDisabled(String provider) {
            CheckEnable();
        }
    };

    private void ShowLocation(Location location){
        if(location == null)
            return;
        if(location.getProvider().equals(locationManager.GPS_PROVIDER)){
            //реакция на данные
        }
        else if(location.getProvider().equals(locationManager.NETWORK_PROVIDER)){
            //реакция на данные
        }

    }

    private String FormatLocation(Location location){
        if(location == null)
            return "";
        return String.format(Double.toString(location.getLatitude()), Double.toString(location.getLongitude())
                , new Date(location.getTime()).toString());
    }

    private void CheckEnable(){

    }



    private class GetPageAsync extends AsyncTask<Integer, String, ArrayMap<Integer, String>> {

        protected ArrayMap<Integer, String> doInBackground(Integer... params) {

            int count = params.length;
            ArrayMap<Integer, String> pages = new ArrayMap<Integer, String>();
            //long totalSize = 0;
            for (int i = 0; i < count; i++) {
                String year = String.valueOf(params[i]) + "_год";
                if (params[i] < 0) {
                    year += "_до_н._э.";
                    year = year.substring(1);
                }
                try {
                    String text = wikipedia.getPageText(year);
                    pages.put(params[i], text);
                } catch (IOException e) {
                    e.printStackTrace();
                    //return page;
                }
            }
            //String[] pagesMas = pages.toArray(new String[pages.size()]);
            return pages;
        }

//        protected void onProgressUpdate(Integer... progress) {
//            setProgressPercent(progress[0]);
//        }
//
        protected void onPostExecute(ArrayMap<Integer, String> result) {
            if(result == null)
                return;

            ArrayMap<Integer, String> allEvntsByYear = new ArrayMap<Integer, String>();


            for(int i = 0; i < result.size(); i++) {
                if (!result.valueAt(i).contains("#перенаправление") &
                        !result.valueAt(i).contains("#REDIRECT"))
                    allEvntsByYear.put(result.keyAt(i), result.valueAt(i));
            }

            ParseEvent(allEvntsByYear);
        }
    }

    private class GetPageRevIDAsync extends AsyncTask<Integer, String, ArrayMap<Integer, Long>> {

        protected ArrayMap<Integer, Long> doInBackground(Integer... params) {
            int count = params.length;
            long revId = 0;

            ArrayList<String> pages = new ArrayList<String>();
            //long totalSize = 0;
            for (int i = 0; i < count; i++) {
                String year = String.valueOf(params[i]) + "_год";
                if (params[i] < 0) {
                    year += "_до_н._э.";
                    year = year.substring(1);
                }

                pages.add(year);
                //revId = wikipedia.getPageRevId(year);
            }

            ArrayMap<Integer, Long> pagesWithID = wikipedia.getPagesRevId(pages);

            return pagesWithID;
        }

        //        protected void onProgressUpdate(Integer... progress) {
//            setProgressPercent(progress[0]);
//        }

        protected void onPostExecute(ArrayMap<Integer, Long> pagesWithID) {
            if(pagesWithID == null)
                return;

            //long res = Long.parseLong(result[1]);
            SQLiteDatabase db = dbHelper.getWritableDatabase();

            Cursor cursor = db.query("Page", new String[]{"year, revisionID"}, "year >= ? and year <= ?",
                    new String[]{String.valueOf(startDate), String.valueOf(finishDate)}, null, null, null);

            ArrayList<Integer> listForUpdate = new ArrayList<Integer>();
            if(cursor != null){
                if(cursor.moveToFirst()){
                    do{
                        int year = cursor.getInt(cursor.getColumnIndex("year"));
                        long revID = cursor.getInt(cursor.getColumnIndex("revisionID"));

                        if (pagesWithID.get(new Integer(year)) != new Long(revID)){
                            listForUpdate.add(new Integer(year));

                        }
                        else{
                            //берём из БД
                        }

                    } while (cursor.moveToNext());

                    cursor.close();

                    ContentValues cv = new ContentValues();

                    Integer[] listForUpdateMas = listForUpdate.toArray(new Integer[listForUpdate.size()]);
                    GetPageAsync getPageAsync = new GetPageAsync();
                    getPageAsync.execute(listForUpdateMas);

                    try {
                        getPageAsync.get(50000, TimeUnit.MILLISECONDS);

                        cv.put("lastUpdate", Calendar.getInstance().toString());
                        db.update("Page", cv, "year >= ? and year <= ?",
                                new String[]{String.valueOf(startDate), String.valueOf(finishDate)});
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    } catch (ExecutionException e) {
                        e.printStackTrace();
                    } catch (TimeoutException e) {
                        e.printStackTrace();
                    }
                    finally {
                        dbHelper.close();
                    }

                }

            }
        }
    }

    private class GetPageTemplatesAsync extends AsyncTask<String, String, ArrayList<String>> {

        protected ArrayList<String> doInBackground(String... strs) {

            ArrayList<String> lexesList = new ArrayList<String>(Arrays.asList(strs));
            ArrayList<String> titleWithCoordTemplate = wikipedia.getTitlePageWithCoordTemplate(lexesList);

            for(int i = 0; i < titleWithCoordTemplate.size(); i++){
                titleWithCoordTemplate.set(i, StringEscapeUtils.unescapeJava(titleWithCoordTemplate.get(i)));
            }

//            if (wikipedia.isContainCoordTemplate(strs[0])){
//                return strs[0];
//            }
//            else
//                return "";

            return titleWithCoordTemplate;
        }

//        protected void onPostExecute(String result) {
//            if(result != "") {
//                //try {
//                    GetPageForCoordAsync getPageForCoordAsync = new GetPageForCoordAsync();
//                    getPageForCoordAsync.execute(result);
//                    //Object[] coord = ParseCoord(wikipedia.getPageText(result));
//
////                } catch (IOException e) {
////                    e.printStackTrace();
////                }
//            }
//        }
//
//        private class GetPageForCoordAsync extends AsyncTask<String, String, String> {
//
//            protected String doInBackground(String... strs) {
//                int count = strs.length;
//                String page = null;
//                //long totalSize = 0;
//                for (int i = 0; i < count; i++) {
//                    try {
//                        page = wikipedia.getPageText(strs[i]);
//                    } catch (IOException e) {
//                        e.printStackTrace();
//                        return page;
//                    }
//                }
//                return page;
//            }
//
//            //        protected void onProgressUpdate(Integer... progress) {
////            setProgressPercent(progress[0]);
////        }
////
//            protected void onPostExecute(String result) {
//                Object[] coord = ParseCoord(result);
//
//            }
//        }
    }

    private class GetCoordsAsynk extends AsyncTask<String, String, ArrayMap<String, Coordinate>>{
        @Override
        protected ArrayMap<String, Coordinate> doInBackground(String... params) {

            ArrayMap<String, Coordinate> placesWithCoord = new ArrayMap<String, Coordinate>();

            for(String place : params){
                try {
                    String fullText = wikipedia.getPageText(place);

                    Coordinate coordinate = ParseCoord(fullText);

                    placesWithCoord.put(place, coordinate);
                } catch (IOException e) {
                    e.printStackTrace();
                }

            }


            return  placesWithCoord;
        }
    }

    private class GetPageRedirectAsync extends AsyncTask<String, String, ArrayList<String>>{
        @Override
        protected ArrayList<String> doInBackground(String... params) {

            ArrayList<String> paramsList = new ArrayList<String>(Arrays.asList(params));
            ArrayList<String> titleWithRedirect = wikipedia.getTitlePageWithRedirect(paramsList);

            for(int i = 0; i < titleWithRedirect.size(); i++){
                titleWithRedirect.set(i, StringEscapeUtils.unescapeJava(titleWithRedirect.get(i)));
            }

            return titleWithRedirect;
        }
    }

    private class GetAddressPageForRedirectAsync extends AsyncTask<String, String, String>{
        @Override
        protected String doInBackground(String... params) {
            return wikipedia.getRedirectForPage(params[0]);
        }
    }


    private void ParseLexFromEvents(ArrayList<EventModel> events){

        ArrayList<EventWithLex> eventWithLexList = new ArrayList<EventWithLex>();

        LuceneMorphology luceneMorph = null;
        try {
            luceneMorph = new RussianLuceneMorphology();
        } catch (IOException e) {
            e.printStackTrace();
        }

        for(EventModel evModel : events){

            int x = 0;
            int y = 0;
            String word;

            ArrayList<String> lexemes = new ArrayList<String>();
            ArrayList<String> rawWords = new ArrayList<String>();
            String text = evModel.text;

            y = text.indexOf(" ", x);

            while (y != -1){
                word = text.substring(x, y);
                word = PunctuationHook(word);

                if(word != null & word != ""){
                    rawWords.add(word);
                }

                x = y + 1;
                y = text.indexOf(" ", x);
            }

            y = text.indexOf(".", x);

            if(y != -1){
                word = text.substring(x, y);
                word = PunctuationHook(word);

                if(word != null & word != ""){
                    rawWords.add(word);
                }
            }

            for(String rawWord : rawWords){
                String lex = "";


                rawWord = rawWord.toLowerCase();
                try {
                    wordBaseForms = luceneMorph.getMorphInfo(rawWord);

                    if(wordBaseForms != null) {
                        int pos = (wordBaseForms.get(0)).indexOf("|") + 3;
                        String partOfSpeach = wordBaseForms.get(0).substring(pos, pos + 1);
                        char pOS = partOfSpeach.charAt(0);

                        if(pOS == noun){
                            lex = wordBaseForms.get(0).substring(0, pos - 3);
                            lexemes.add(lex);
//                        getPageTemplatesAsync = new GetPageTemplatesAsync();
//                        String utf8lex = "";
//                        try {
//                            utf8lex = new String(lex.getBytes("UTF-8"), "UTF-8");
//                        } catch (UnsupportedEncodingException e) {
//                            e.printStackTrace();
//                        }
//                        getPageTemplatesAsync.execute(utf8lex);
                        }
                    }
                }catch (WrongCharaterException e){
                    e.printStackTrace();
                }


            }

            eventWithLexList.add(new EventWithLex(evModel, lexemes));
        }
        GetRedirectForLexemes(eventWithLexList);
    }

    private void GetRedirectForLexemes(List<EventWithLex> eventWithLexes){

        ArrayList<String> rawLex = new ArrayList<String>();

        for(EventWithLex eventWithLex : eventWithLexes){
            rawLex.addAll(eventWithLex.lexemes);
        }

        String[] rawLexMas = rawLex.toArray(new String[rawLex.size()]);

        GetPageRedirectAsync getPageRedirectAsync = new GetPageRedirectAsync();
        getPageRedirectAsync.execute(rawLexMas);

        try {
            ArrayList<String> lexForRedirect = getPageRedirectAsync.get(50000, TimeUnit.MICROSECONDS);

            for(String lexForRedir : lexForRedirect){
                GetAddressPageForRedirectAsync getAddressPageForRedirectAsync =
                        new GetAddressPageForRedirectAsync();
                getAddressPageForRedirectAsync.execute(lexForRedir);
                String lexToRedir = getAddressPageForRedirectAsync.get(50000, TimeUnit.MICROSECONDS);

                for(int i = 0; i < eventWithLexes.size(); i++){
                    int indx = eventWithLexes.get(i).lexemes.indexOf(lexForRedir);
                    while(indx >= 0){
                        eventWithLexes.get(i).lexemes.set(indx, lexToRedir);
                        indx = eventWithLexes.get(i).lexemes.indexOf(lexForRedir);
                    }
                }
            }

            GetCoordForEvent(eventWithLexes);

        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (TimeoutException e) {
            e.printStackTrace();
        }

    }

    private void GetCoordForEvent(List<EventWithLex> eventWithLexes){

        ArrayList<String> allLexemes = new ArrayList<String>();

        for(EventWithLex eventWithLex : eventWithLexes){
            allLexemes.addAll(eventWithLex.lexemes);
        }

        HashSet<String> set = new HashSet<String>(allLexemes);

        String[] rawLexMas = set.toArray(new String[set.size()]);

        GetPageTemplatesAsync getPageTemplatesAsync = new GetPageTemplatesAsync();
        getPageTemplatesAsync.execute(rawLexMas);

        try {
            ArrayList<String> lexesWithCoord = getPageTemplatesAsync.get(50000, TimeUnit.MICROSECONDS);

            String[] lexesWithCoordMas = lexesWithCoord.toArray(new String[lexesWithCoord.size()]);

            GetCoordsAsynk getCoordsAsynk = new GetCoordsAsynk();
            getCoordsAsynk.execute(lexesWithCoordMas);

            ArrayMap<String, Coordinate> placesWithCoord = getCoordsAsynk.get(500000, TimeUnit.MICROSECONDS);



            for(int i = 0; i <= eventWithLexes.size(); i++){
                ArrayList<String> lexesFromEvent = eventWithLexes.get(i).lexemes;

                for(String lexFromEvent : lexesFromEvent){

                    int ind = placesWithCoord.indexOfKey(lexFromEvent);
                    if(ind >= 0){
                        eventWithLexes.get(i).evntModel.coord = placesWithCoord.valueAt(ind);

                        break;
                    }
                }

            }

//            for(int i = 0; i < eventWithLexes.size(); i++){
//                int indx = eventWithLexes.get(i).lexemes.contains(lexForRedir);
//                while(indx >= 0){
//                    eventWithLexes.get(i).lexemes.set(indx, lexToRedir);
//                    indx = eventWithLexes.get(i).lexemes.indexOf(lexForRedir);
//                }
//            }

        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (TimeoutException e) {
            e.printStackTrace();
        }
    }

    private Coordinate ParseCoord(String fullText){

        double lonSign = 1;
        double latSign = 1;
        double longitude = 0;
        double latitude = 0;

        int x = 0;//begin
        int y = 0;//end
        int z = 0;//=




        x = fullText.indexOf("lat_dir");
        if(x < 0)
            latSign = -1;
        else{
            y = fullText.indexOf("|", x);
            z = fullText.indexOf("=", x);

            String dir = fullText.substring(z, y);
            dir = dir.trim();
            if(dir == "S")
                latSign = -1;
            else
                latSign = 1;
        }

        x = fullText.indexOf("lon_dir ");
        if(x < 0)
            lonSign = -1;
        else{
            y = fullText.indexOf("|", x);
            z = fullText.indexOf("=", x);

            String dir = fullText.substring(z, y);
            dir = dir.trim();
            if(dir == "W")
                lonSign = -1;
            else
                lonSign = 1;
        }




        x = fullText.indexOf("lat_deg");
        if(x >= 0){
            y = fullText.indexOf("|", x);
            z = fullText.indexOf("=", x);

            String deg = fullText.substring(z, y);
            deg = deg.trim();
            if(deg != "") {
                double degInt = Double.parseDouble(deg);
                latitude += degInt;
            }
        }
        x = fullText.indexOf("lat_min");
        if(x >= 0){
            y = fullText.indexOf("|", x);
            z = fullText.indexOf("=", x);

            String min = fullText.substring(z, y);
            min = min.trim();
            if(min != "") {
                double minInt = Double.parseDouble(min);
                latitude += minInt/60;
            }
        }
        x = fullText.indexOf("lat_sec");
        if(x >= 0){
            y = fullText.indexOf("|", x);
            z = fullText.indexOf("=", x);

            String sec = fullText.substring(z, y);
            sec = sec.trim();
            if(sec != "") {
                double secInt = Double.parseDouble(sec);
                latitude += secInt/3600;
            }
        }

        latitude *= latSign;




        x = fullText.indexOf("lon_deg");
        if(x >= 0){
            y = fullText.indexOf("|", x);
            z = fullText.indexOf("=", x);

            String deg = fullText.substring(z, y);
            deg = deg.trim();
            if(deg != "") {
                double degInt = Double.parseDouble(deg);
                longitude += degInt;
            }
        }
        x = fullText.indexOf("lon_min");
        if(x >= 0){
            y = fullText.indexOf("|", x);
            z = fullText.indexOf("=", x);

            String min = fullText.substring(z, y);
            min = min.trim();
            if(min != "") {
                double minInt = Double.parseDouble(min);
                longitude += minInt/60;
            }
        }
        x = fullText.indexOf("lon_sec");
        if(x >= 0){
            y = fullText.indexOf("|", x);
            z = fullText.indexOf("=", x);

            String sec = fullText.substring(z, y);
            sec = sec.trim();
            if(sec != "") {
                double secInt = Double.parseDouble(sec);
                longitude += secInt/3600;
            }
        }

        longitude *= lonSign;

//        int lat_dir_indx; int lat_deg_indx; int lat_min_indx; int lat_sec_indx;
//        int lon_dir_indx; int lon_deg_indx; int lon_min_indx; int lon_sec_indx;
//
//        int lat_dir_indx_end; int lat_deg_indx_end; int lat_min_indx_end; int lat_sec_indx_end;
//        int lon_dir_indx_end; int lon_deg_indx_end; int lon_min_indx_end; int lon_sec_indx_end;
//
//        lat_dir_indx = fullText.indexOf("lat_dir");
//        lat_deg_indx = fullText.indexOf("lat_deg");
//        lat_min_indx = fullText.indexOf("lat_min");
//        lat_sec_indx = fullText.indexOf("lat_sec");
//        lon_dir_indx = fullText.indexOf("lon_dir");
//        lon_deg_indx = fullText.indexOf("lon_deg");
//        lon_min_indx = fullText.indexOf("lon_min");
//        lon_sec_indx = fullText.indexOf("lon_sec");
//        if(lat_dir_indx >= 0)
//            lat_dir_indx_end = fullText.indexOf("|", lat_dir_indx + 9);
//        lat_deg_indx_end = fullText.indexOf("|", lat_deg_indx + 9);
//        lat_min_indx_end = fullText.indexOf("|", lat_min_indx + 9);
//        lat_sec_indx_end = fullText.indexOf("|", lat_sec_indx + 9);
//        if(lon_dir_indx >= 0)
//            lon_dir_indx_end = fullText.indexOf("|", lon_dir_indx + 9);
//        lon_deg_indx_end = fullText.indexOf("|", lon_deg_indx + 9);
//        lon_min_indx_end = fullText.indexOf("|", lon_min_indx + 9);
//        lon_sec_indx_end = fullText.indexOf("|", lon_sec_indx + 9);
//
////        if(lat_dir_indx >= 0)
////            coord[0] = fullText.substring(lat_dir_indx, lat_dir_indx_end);
////        else coord[0] = "N";
//        coord[1] = fullText.substring(lat_deg_indx, lat_deg_indx_end);
//        coord[2] = fullText.substring(lat_min_indx, lat_min_indx_end);
//        coord[3] = fullText.substring(lat_sec_indx, lat_sec_indx_end);
////        if(lon_dir_indx != -1)
////            coord[4] = fullText.substring(lon_dir_indx, lon_dir_indx_end);
////        else coord[4] = "E";
//        coord[5] = fullText.substring(lon_deg_indx, lon_deg_indx_end);
//        coord[6] = fullText.substring(lon_min_indx, lon_min_indx_end);
//        coord[7] = fullText.substring(lon_sec_indx, lon_sec_indx_end);
//
//        coord[0] = ((String)(coord[0])).trim();
//        coord[1] = ((String)(coord[1])).trim();
//        coord[2] = ((String)(coord[2])).trim();
//        coord[3] = ((String)(coord[3])).trim();
//        coord[4] = ((String)(coord[4])).trim();
//        coord[5] = ((String)(coord[5])).trim();
//        coord[6] = ((String)(coord[6])).trim();
//        coord[7] = ((String)(coord[7])).trim();

        return new Coordinate(latitude, longitude);
    }

    private void ParseEvent(ArrayMap<Integer, String> eventsByYear){

        //dbHelper = new DBHelper(this);

        //List<String> eventList = new ArrayList<String>();

        List<EventModel> eventList = new ArrayList<EventModel>();

        String eventItem = null;
        int start;
        int finish;
        int startEv;
        int finishEv;
        int startRef;
        int finishRef;

        for(int i = 0; i < eventsByYear.size(); i++) {

            String events = eventsByYear.valueAt(i);

            start = events.indexOf("== События ==");
            finish = events.indexOf(" ==\n", start + 14);
            if (finish != -1)
                events = events.substring(start + 14, finish);
            else events = events.substring(start + 14);

            startEv = events.indexOf("*");
            finishEv = events.indexOf("*", startEv + 1);

            while (finishEv != -1) {//не последнее событие

                if (finishEv - startEv != 1) {

                    if (finishEv - startEv > 17) {//ещё что-то кроме даты
                        eventItem = events.substring(startEv + 2, finishEv - 1);

                        int indxSlash = eventItem.indexOf("==");
                        if (indxSlash != -1) {
                            eventItem = eventItem.substring(0, indxSlash - 2);
                        }


                        startRef = eventItem.indexOf("<ref");
                        while (startRef != -1) {
                            startRef = eventItem.indexOf("<ref>");
                            if (startRef != -1) {
                                finishRef = eventItem.indexOf("</ref>", startRef);
                                eventItem = eventItem.substring(0, startRef) + eventItem.substring(finishRef + 6);

                                startRef = eventItem.indexOf("<ref");
                            } else {
                                startRef = eventItem.indexOf("<ref");
                                finishRef = eventItem.indexOf("/>", startRef);
                                if (finishRef != -1) {
                                    eventItem = eventItem.substring(0, startRef) + eventItem.substring(finishRef + 2);
                                } else {
                                    finishRef = eventItem.indexOf("</ref>", startRef);
                                    eventItem = eventItem.substring(0, startRef) + eventItem.substring(finishRef + 6);
                                }
                                startRef = eventItem.indexOf("<ref");
                            }

                        }

                        while (eventItem.contains("[[")) {
                            eventItem = TrimHooks(eventItem);
                        }

                        eventList.add(new EventModel(eventItem, eventsByYear.keyAt(i)));

                    } else {
                        eventItem = events.substring(startEv + 4, finishEv - 3) + " — ";
                    }

                } else {//несколько событий

                    startEv++;
                    finishEv = events.indexOf("*", startEv + 1);

                    int indxDef = eventItem.indexOf(" — ");

                    if (indxDef != -1)
                        eventItem = eventItem.substring(0, indxDef + 3) + //взять дату из старого eventItem
                                events.substring(startEv + 2, finishEv - 1);
                    else
                        eventItem = eventItem + //взять дату из старого eventItem
                                events.substring(startEv + 2, finishEv - 1);

                    int indxSlash = eventItem.indexOf("==");
                    if (indxSlash != -1) {
                        eventItem = eventItem.substring(0, indxSlash - 2);
                    }

                    startRef = eventItem.indexOf("<ref");
                    while (startRef != -1) {
                        startRef = eventItem.indexOf("<ref>");
                        if (startRef != -1) {
                            finishRef = eventItem.indexOf("</ref>", startRef);
                            eventItem = eventItem.substring(0, startRef) + eventItem.substring(finishRef + 6);

                            startRef = eventItem.indexOf("<ref");
                        } else {
                            startRef = eventItem.indexOf("<ref");
                            finishRef = eventItem.indexOf("/>", startRef);
                            if (finishRef != -1) {
                                eventItem = eventItem.substring(0, startRef) + eventItem.substring(finishRef + 2);
                            } else {
                                finishRef = eventItem.indexOf("</ref>", startRef);
                                eventItem = eventItem.substring(0, startRef) + eventItem.substring(finishRef + 6);
                            }
                            startRef = eventItem.indexOf("<ref");
                        }

                    }

                    while (eventItem.contains("[[")) {
                        eventItem = TrimHooks(eventItem);
                    }


                    eventList.add(new EventModel(eventItem, eventsByYear.keyAt(i)));
                }

                startEv = finishEv;
                finishEv = events.indexOf("*", startEv + 1);
            }
        }

        db = dbHelper.getWritableDatabase();

        Cursor cursor = db.query("Events", new String[]{"year", "event"}, "year >= ? and year <= ?",
                new String[]{Integer.toString(yearStart, yearFinish)}, null, null, null);

        ArrayList<EventModel> eventsFromDB = new ArrayList<EventModel>();
        //ArrayList<EventModel> eventsToDelFromDB = new ArrayList<EventModel>();
        ArrayList<EventModel> eventsToParseLex = new ArrayList<EventModel>();

        if(cursor == null || cursor.getCount() == 0){//ни одной записи
            eventsToParseLex.addAll(eventList);
        }

        else {
            do {
                int yearFromDB = cursor.getInt(cursor.getColumnIndex("year"));
                String textFromDB = cursor.getString(cursor.getColumnIndex("text"));

                eventsFromDB.add(new EventModel(textFromDB, yearFromDB));
            } while (cursor.moveToNext());

            for(EventModel evntMod : eventList){
                if (!(eventsFromDB.contains(evntMod))){//возможно не будет работать
                    eventsToParseLex.add(evntMod);
                }
            }
        }
        cursor.close();


        for(EventModel evntMod : eventsFromDB){
            if (!(eventList.contains(evntMod))){//возможно не будет работать
                //eventsToDelFromDB.add(evntMod);
                db.delete("Event", "year = ? and text = ?", new String[]{String.valueOf(evntMod.year), evntMod.text});
            }
        }


        dbHelper.close();

        ParseLexFromEvents(eventsToParseLex);

//        db = dbHelper.getWritableDatabase();
//
//        ContentValues cv = new ContentValues();
//
//        cv.put("text", eventItem);
//        cv.put("lat_dir", "NaN");
//        cv.put("lon_dir", "NaN");
//        cv.put("lat_deg", 0);
//        cv.put("lon_deg", 0);
//        cv.put("lat_min", 0);
//        cv.put("lon_min", 0);
//        cv.put("lat_sec", 0);
//        cv.put("lon_sec", 0);
//
//        long id = db.insert("Event", null, cv);
//
//        dbHelper.close();
    }

    private String PunctuationHook(String word){

        word = word.replace(" ", "");
        word = word.replace(",", "");
        word = word.replace(";", "");
        word = word.replace(":", "");
        word = word.replace(".", "");
        word = word.replace("...", "");

        return  word;
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
            db.execSQL("create table Pages " +
                    "(year integer not null unique, " + "revisionID integer, " + "lastUpdate text)");

            db.execSQL("create table Events " +
                    "(year integer, " + "event text, " +
                    "latitude double, " + "longitude double)");
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

        }
    }

    class EventWithLex{

        EventModel evntModel;
        ArrayList<String> lexemes;

        public EventWithLex(EventModel evModel, ArrayList<String> lxms){
            evntModel= evModel;
            lexemes = lxms;
        }
    }

//    private void exportDB(){
//        File sd = Environment.getExternalStorageDirectory();
//        File data = Environment.getDataDirectory();
//        FileChannel source=null;
//        FileChannel destination=null;
//        String currentDBPath = "/data/"+ "com.authorwjf.sqliteexport" +"/databases/"+SAMPLE_DB_NAME;
//        String backupDBPath = SAMPLE_DB_NAME;
//        File currentDB = new File(data, currentDBPath);
//        File backupDB = new File(sd, backupDBPath);
//        try {
//            source = new FileInputStream(currentDB).getChannel();
//            destination = new FileOutputStream(backupDB).getChannel();
//            destination.transferFrom(source, 0, source.size());
//            source.close();
//            destination.close();
//            Toast.makeText(this, "DB Exported!", Toast.LENGTH_LONG).show();
//        } catch(IOException e) {
//            e.printStackTrace();
//        }
//    }
}
