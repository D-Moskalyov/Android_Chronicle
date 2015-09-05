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
import android.util.Log;
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
import org.apache.lucene.morphology.russian.RussianLuceneMorphology;
import wikipedia.Wiki;
import org.apache.lucene.morphology.*;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

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
    char noun = '�';
    private int firstCenturyAC = R.string.firstCenturyAC;
    int centuryStart;
    int centuryFinish;
    int yearStart;
    int yearFinish;

    Wiki wikipedia;

    DBHelper dbHelper;
    SQLiteDatabase db;

    SimpleDateFormat dateFormat;

    GetPageAsync getPageAsync;
    GetPageRevIDAsync getPageRevIDAsync;
    GetPageTemplatesAsync getPageTemplatesAsync;
    LocationManager locationManager;
    ClusterManager<Coordinate> mClusterManager;

    String LOG_TAG = "INF";

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_layout);
        //this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        settings = getSharedPreferences(getString(R.string.preference_file_key), 0);

        mapFragment = (MapFragment) getFragmentManager().findFragmentById(R.id.map);

        map = mapFragment.getMap();
        if(map == null){
            finish();
            return;
        }

        InitClusterer();
        onMapReady(map);

        wikipedia = new Wiki();

        mainButton = (Button) this.findViewById(R.id.settingsButton);
        mainButton.setText("main");

        locationManager = (LocationManager)getSystemService(LOCATION_SERVICE);


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

        SQLiteDatabase db = dbHelper.getWritableDatabase();

        Cursor cursor = db.query("Page", new String[]{"year", "lastUpdate"}, "year >= ? and year <= ?",
                new String[]{Integer.toString(startYEAR, finishYEAR)}, null, null, null);

        ArrayList<Integer> listForFirstInit = new ArrayList<>();
        ArrayList<Integer> listForUpdate = new ArrayList<>();
        ArrayList<Integer> listForNotUpdate = new ArrayList<>();

        if(cursor == null){//�� ����� ������
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

        Integer[] listForFirstInitMas = listForFirstInit.toArray(new Integer[listForFirstInit.size()]);
        getPageAsync.execute(listForFirstInitMas);
//
//        for(int yearForUpdate : listForNotUpdate) {
//            GetPageRevIDAsync getPageRevIDAsync = new GetPageRevIDAsync();
//            getPageRevIDAsync.execute(yearForUpdate);
//        }

        Integer[] listForNotUpdateMas = listForNotUpdate.toArray(new Integer[listForNotUpdate.size()]);
        getPageRevIDAsync.execute(listForNotUpdateMas);


        LuceneMorphology luceneMorph = null;
        try {
            luceneMorph = new RussianLuceneMorphology();
        } catch (IOException e) {
            e.printStackTrace();
        }
        String _text = "������";

        _text = _text.toLowerCase();
        //_textUTF8 = _textUTF8.toLowerCase();
        try {
            wordBaseForms = luceneMorph.getMorphInfo(_text);
        }catch (WrongCharaterException e){
            e.printStackTrace();
        }
        if(wordBaseForms != null) {
            int pos = (wordBaseForms.get(0)).indexOf("|") + 3;
            String partOfSpeach = wordBaseForms.get(0).substring(pos, pos + 1);
            char pOS = partOfSpeach.charAt(0);

            if(pOS == noun){
                String lex = wordBaseForms.get(0).substring(0, pos - 3);

                getPageTemplatesAsync = new GetPageTemplatesAsync();
                String utf8lex = "";
                try {
                    utf8lex = new String(lex.getBytes("UTF-8"), "UTF-8");
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
                getPageTemplatesAsync.execute(utf8lex);

            }

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
//        startDate = 1914;//�������� �� Preference
//        finishDate = 1915;//�������� �� Preference
//
//        Calendar thatDay = Calendar.getInstance();
//        if(true) {//��������� ���
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
//                    getPageRevIDAsync.execute(("1914_���"));
//
//
//                //}
//            }
//
//            else {
//                //������ � ���������� � ��
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
                //������� �� ���������
            }
            else if(provider.equals(locationManager.NETWORK_PROVIDER)){
                //������� �� ���������
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
            //������� �� ������
        }
        else if(location.getProvider().equals(locationManager.NETWORK_PROVIDER)){
            //������� �� ������
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



    private class GetPageAsync extends AsyncTask<Integer, String, String[]> {

        protected String[] doInBackground(Integer... params) {

            int count = params.length;
            ArrayList<String> pages = new ArrayList<>();
            //long totalSize = 0;
            for (int i = 0; i < count; i++) {
                String year = String.valueOf(params[i]) + "_���";
                if (params[i] < 0) {
                    year += "_��_�._�.";
                    year = year.substring(1);
                }
                try {
                    String text = wikipedia.getPageText(year);
                    pages.add(text);
                } catch (IOException e) {
                    e.printStackTrace();
                    //return page;
                }
            }
            String[] pagesMas = pages.toArray(new String[pages.size()]);
            return pagesMas;
        }

//        protected void onProgressUpdate(Integer... progress) {
//            setProgressPercent(progress[0]);
//        }
//
        protected void onPostExecute(String... result) {
            if(result == null)
                return;

            for(String resItem : result) {
                if (resItem.contains("#���������������"))
                    return;

                ParseEvent(resItem);
            }
        }
    }

    private class GetPageRevIDAsync extends AsyncTask<Integer, String, ArrayMap<Integer, Long>> {

        protected ArrayMap<Integer, Long> doInBackground(Integer... params) {
            int count = params.length;
            long revId = 0;

            ArrayList<String> pages = new ArrayList<>();
            //long totalSize = 0;
            for (int i = 0; i < count; i++) {
                String year = String.valueOf(params[i]) + "_���";
                if (params[i] < 0) {
                    year += "_��_�._�.";
                    year = year.substring(1);
                }

                pages.add(year);
                //revId = wikipedia.getPageRevId(year);
            }

            ArrayMap<Integer, Long> pagesWithID = wikipedia.getPagesRevId(pages);


            //String revIdStr = Long.toString(revId);
            //return new String[]{params[0], revIdStr};
            return pagesWithID;
        }

        //        protected void onProgressUpdate(Integer... progress) {
//            setProgressPercent(progress[0]);
//        }

        protected void onPostExecute(ArrayMap<Integer, Long> pagesWithID) {
            if(pagesWithID == null)
                return;

            long res = Long.parseLong(result[1]);
            SQLiteDatabase db = dbHelper.getWritableDatabase();

            Cursor cursor = db.query("Page", new String[]{"revisionID"}, "year = ?",
                    new String[]{result[0]}, null, null, null);

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
                            getPageAsync.execute(result[0]);
                        }
                        else{
                            //���� �� ��
                        }
                    } while (cursor.moveToNext());
                }

            }
        }
    }

    private class GetPageTemplatesAsync extends AsyncTask<String, String, String> {

        protected String doInBackground(String... strs) {
            if (wikipedia.isContainCoordTemplate(strs[0])){
                return strs[0];
            }
            else
                return "";
        }

        protected void onPostExecute(String result) {
            if(result != "") {
                //try {
                    GetPageForCoordAsync getPageForCoordAsync = new GetPageForCoordAsync();
                    getPageForCoordAsync.execute(result);
                    //Object[] coord = ParseCoord(wikipedia.getPageText(result));

//                } catch (IOException e) {
//                    e.printStackTrace();
//                }
            }
        }

        private class GetPageForCoordAsync extends AsyncTask<String, String, String> {

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
                Object[] coord = ParseCoord(result);

            }
        }
    }



    private Object[] ParseCoord(String fullText){

        Object[] coord = new Object[8];

        int lat_dir_indx; int lat_deg_indx; int lat_min_indx; int lat_sec_indx;
        int lon_dir_indx; int lon_deg_indx; int lon_min_indx; int lon_sec_indx;

        int lat_dir_indx_end; int lat_deg_indx_end; int lat_min_indx_end; int lat_sec_indx_end;
        int lon_dir_indx_end; int lon_deg_indx_end; int lon_min_indx_end; int lon_sec_indx_end;

        lat_dir_indx = fullText.indexOf("lat_dir");
        lat_deg_indx = fullText.indexOf("lat_deg");
        lat_min_indx = fullText.indexOf("lat_min");
        lat_sec_indx = fullText.indexOf("lat_sec");
        lon_dir_indx = fullText.indexOf("lon_dir");
        lon_deg_indx = fullText.indexOf("lon_deg");
        lon_min_indx = fullText.indexOf("lon_min");
        lon_sec_indx = fullText.indexOf("lon_sec");

        lat_dir_indx_end = fullText.indexOf("|", lat_dir_indx + 9);
        lat_deg_indx_end = fullText.indexOf("|", lat_deg_indx + 9);
        lat_min_indx_end = fullText.indexOf("|", lat_min_indx + 9);
        lat_sec_indx_end = fullText.indexOf("|", lat_sec_indx + 9);
        lon_dir_indx_end = fullText.indexOf("|", lon_dir_indx + 9);
        lon_deg_indx_end = fullText.indexOf("|", lon_deg_indx + 9);
        lon_min_indx_end = fullText.indexOf("|", lon_min_indx + 9);
        lon_sec_indx_end = fullText.indexOf("|", lon_sec_indx + 9);

        if(lat_dir_indx != -1)
            coord[0] = fullText.substring(lat_dir_indx, lat_dir_indx_end);
        else coord[0] = "N";
        coord[1] = fullText.substring(lat_deg_indx, lat_deg_indx_end);
        coord[2] = fullText.substring(lat_min_indx, lat_min_indx_end);
        coord[3] = fullText.substring(lat_sec_indx, lat_sec_indx_end);
        if(lon_dir_indx != -1)
            coord[4] = fullText.substring(lon_dir_indx, lon_dir_indx_end);
        else coord[4] = "E";
        coord[5] = fullText.substring(lon_deg_indx, lon_deg_indx_end);
        coord[6] = fullText.substring(lon_min_indx, lon_min_indx_end);
        coord[7] = fullText.substring(lon_sec_indx, lon_sec_indx_end);

        coord[0] = ((String)(coord[0])).trim();
        coord[1] = ((String)(coord[1])).trim();
        coord[2] = ((String)(coord[2])).trim();
        coord[3] = ((String)(coord[3])).trim();
        coord[4] = ((String)(coord[4])).trim();
        coord[5] = ((String)(coord[5])).trim();
        coord[6] = ((String)(coord[6])).trim();
        coord[7] = ((String)(coord[7])).trim();

        return coord;
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

        start = events.indexOf("== ������� ==");
        finish = events.indexOf(" ==\n", start + 14);
        if(finish != -1)
            events = events.substring(start + 14, finish);
        else events = events.substring(start + 14);

        startEv = events.indexOf("*");
        finishEv = events.indexOf("*", startEv + 1);

        while(finishEv != -1) {//�� ��������� �������

            if (finishEv - startEv != 1) {

                if (finishEv - startEv > 17) {//��� ���-�� ����� ����
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
                    eventItem = events.substring(startEv + 4, finishEv - 3) + " � ";
                }

            } else {//��������� �������

                startEv++;
                finishEv = events.indexOf("*", startEv + 1);

                int indxDef= eventItem.indexOf(" � ");

                if(indxDef != -1)
                    eventItem = eventItem.substring(0, indxDef + 3) + //����� ���� �� ������� eventItem
                            events.substring(startEv + 2, finishEv - 1);
                else
                    eventItem = eventItem + //����� ���� �� ������� eventItem
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
