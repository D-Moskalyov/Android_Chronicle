package com.chronicle.app;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.graphics.Bitmap;
import android.graphics.pdf.PdfDocument;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Parcelable;
import android.support.annotation.IntegerRes;
import android.support.v4.app.FragmentActivity;
import android.support.v4.util.ArrayMap;
import android.text.Layout;
import android.util.JsonReader;
import android.util.Log;
import android.util.Pair;
import android.view.*;
import android.os.Bundle;
import android.widget.*;
import com.google.android.gms.maps.*;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.maps.android.MarkerManager;
import com.google.maps.android.clustering.Cluster;
import com.google.maps.android.clustering.ClusterManager;
import com.google.maps.android.clustering.view.DefaultClusterRenderer;
import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.apache.lucene.morphology.russian.RussianLetterDecoderEncoder;
import org.apache.lucene.morphology.russian.RussianLuceneMorphology;
import org.apache.lucene.util.SortedVIntList;
import org.json.JSONObject;
import org.json.JSONTokener;
import wikipedia.Wiki;
import org.apache.lucene.morphology.*;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Array;
import java.sql.Time;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;

//implements OnMapReadyCallback
public class MainActivity extends FragmentActivity implements OnMapReadyCallback {

    MapFragment mapFragment = null;
    GoogleMap map = null;
    //Marker marker = null;

    //final String TAG = "myLogs";
    SharedPreferences settings;
    private static final int SHOW_PREFERENCES = 1;
    Button mainButton;

    int startDate;
    int finishDate;
    //String eventItem;
    List<String> wordBaseForms;
    char noun = '�';
    private int firstCenturyAC;
    //int centuryStart;
    //int centuryFinish;
    int globalYearStart;
    int globalYearFinish;
    int innerYearStart;
    int innerYearfinish;

    Wiki wikipedia;

    DBHelper dbHelper;
    SQLiteDatabase db;

    SimpleDateFormat dateFormat;

    LocationManager locationManager;
    ClusterManager<EventsMarker> mClusterManager;

    List<PageModel> pagesForIsertDB;
    List<PageModel> pagesForUdateDB;
    List<PageModel> pagesForDeleteDB;
    List<EventModel> eventsForDeleteDB;

    ArrayList<EventWithYear> eventWithYearList;

    ArrayList<Integer> listForFirstInit;
    ArrayList<Integer> listForUpdate;
    ArrayList<Integer> listForNotUpdate;

    InitAsync initAsync;
    GetPageAsync getPageAsync;
    GetNewPageRevIDAsync getNewPageRevIDAsync;
    GetPageRevIDAsync getPageRevIDAsync;
    GetPageTemplatesAsync getPageTemplatesAsync;
    GetCoordsAsynk getCoordsAsynk;
    GetPageRedirectAsync getPageRedirectAsync;
    GetAddressPageForRedirectAsync getAddressPageForRedirectAsync;

    Context context;
    EventsMarker clickedClusterItem;
    boolean isCrached;

    ConnectivityManager cm;
    NetworkInfo activeNetwork;
    boolean isConnected;

    String LOG_TAG = "INF";
    Logger logger = Logger.getLogger("chron");

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        context = getApplicationContext();
        Object object = getLastCustomNonConfigurationInstance();
//        if(object != null) {
//            if (object.getClass() == InitAsync.class)
//                initAsync = (InitAsync) object;
//            else if (object.getClass() == GetPageAsync.class)
//                getPageAsync = (GetPageAsync) object;
//            else if (object.getClass() == GetNewPageRevIDAsync.class)
//                getNewPageRevIDAsync = (GetNewPageRevIDAsync) object;
//            else if (object.getClass() == GetPageRevIDAsync.class)
//                getPageRevIDAsync = (GetPageRevIDAsync) object;
//            else if (object.getClass() == GetPageTemplatesAsync.class)
//                getPageTemplatesAsync = (GetPageTemplatesAsync) object;
//            else if (object.getClass() == GetCoordsAsynk.class)
//                getCoordsAsynk = (GetCoordsAsynk) object;
//            else if (object.getClass() == GetPageRedirectAsync.class)
//                getPageRedirectAsync = (GetPageRedirectAsync) object;
//            else if (object.getClass() == GetAddressPageForRedirectAsync.class)
//                getAddressPageForRedirectAsync = (GetAddressPageForRedirectAsync) object;
//        }
        setContentView(R.layout.main_layout);
        firstCenturyAC = (int) getResources().getInteger(R.integer.firstCenturyAC);
        //this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        settings = getSharedPreferences(getString(R.string.preference_file_key), 0);
        mapFragment = (MapFragment) getFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        map = mapFragment.getMap();
        if(map == null){
            finish();
            return;
        }

        dbHelper = new DBHelper(this);
//        db = dbHelper.getWritableDatabase();
//        db.delete("Pages", null, null);
//        db.delete("Events", null, null);
//        dbHelper.close();

        //onMapReady(map);
        wikipedia = new Wiki();
        locationManager = (LocationManager)getSystemService(LOCATION_SERVICE);
//        wordBaseForms = luceneMorph.getMorphInfo("����������");
//        wordBaseForms = luceneMorph.getMorphInfo("����������");
//        wordBaseForms = luceneMorph.getMorphInfo("�����");
//        wordBaseForms = luceneMorph.getMorphInfo("���������");
//        wordBaseForms = luceneMorph.getMorphInfo("���������������");
        mainButton = (Button) this.findViewById(R.id.settingsButton);
        innerYearStart = 0;
        innerYearfinish = 0;

        cm = (ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);
        activeNetwork = cm.getActiveNetworkInfo();

        InitClusterer();
        //Log.d(LOG_TAG, "--- onCreate main ---");

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


        log(Level.INFO, "onCreate", "onCreate success");
    }

    @Override
    protected void onResume() {
        super.onResume();

        SetStartFinishYear(settings);
        if (innerYearStart == globalYearStart & innerYearfinish == globalYearFinish)//��������� �� ����������
            return;

        isCrached = false;
        innerYearStart = globalYearStart;
        innerYearfinish = globalYearFinish;

        mainButton = (Button) this.findViewById(R.id.settingsButton);
        String textBtn = "";
        if (globalYearStart < 0) textBtn += globalYearStart * -1 + "�� - ";
        else textBtn += globalYearStart + " - ";
        if (globalYearFinish < 0) textBtn += globalYearFinish * -1 + "��";
        else textBtn += globalYearFinish;
        mainButton.setText(textBtn);

        pagesForIsertDB = new ArrayList<PageModel>();
        pagesForUdateDB = new ArrayList<PageModel>();
        pagesForDeleteDB = new ArrayList<PageModel>();
        eventsForDeleteDB = new ArrayList<EventModel>();

        listForFirstInit = new ArrayList<Integer>();
        listForUpdate = new ArrayList<Integer>();
        listForNotUpdate = new ArrayList<Integer>();

        ArrayMap<Integer, String> allEvntsByYear = new ArrayMap<Integer, String>();
        ArrayList<EventModel> eventsToParseLex = new ArrayList<EventModel>();
        List<EventWithLex> eventWithLexList = new ArrayList<EventWithLex>();
        List<EventWithLex> eventWithLexes = new ArrayList<EventWithLex>();
        SortedSet<EventModel> events = new TreeSet<EventModel>();

        isConnected = activeNetwork != null && activeNetwork.isConnectedOrConnecting();

        if(isConnected) {
            GetPagesForUpdateDeleteCreate();

            if (listForFirstInit.size() != 0 || listForUpdate.size() != 0 & !isCrached)
                allEvntsByYear = GetPageForParse(listForFirstInit, listForUpdate);
            if (allEvntsByYear.size() != 0 & !isCrached)
                eventsToParseLex = ParseEvent(allEvntsByYear);
            if (eventsToParseLex.size() != 0 & !isCrached)
                eventWithLexList = ParseLexFromEvents(eventsToParseLex);
            if (eventWithLexList.size() != 0 & !isCrached)
                eventWithLexes = GetRedirectForLexemes(eventWithLexList);
            if (eventWithLexes.size() != 0 & !isCrached)
                events = GetCoordForEvent(eventWithLexes);
            if (!isCrached)
                WriteDB(events);
        }

        MakeMarkers();
        mClusterManager.cluster();

        if(!isConnected)
            Toast.makeText(this, "Something wrong", Toast.LENGTH_LONG);
        if(isCrached)
            Toast.makeText(this, "Data is not updated. Check the connection", Toast.LENGTH_LONG);

        //Log.i("onResume", "onResume");

        //initAsync = new InitAsync();
        //initAsync.execute();
//        try {
//            Integer f = initAsync.get();
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        } catch (ExecutionException e) {
//            e.printStackTrace();
//        }

        log(Level.INFO, "onResume", "onResume success");
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        log(Level.INFO, "onWindowFocusChanged", "onWindowFocusChanged success");
    }

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        log(Level.INFO, "onAttachedToWindow", "onAttachedToWindow success");
    }

    @Override
    protected void onPostResume() {
        super.onPostResume();
        //Log.i("onPostResume", "onPostResume");
        log(Level.INFO, "onPostResume", "onPostResume success");
    }

    @Override
    protected void onPause() {
        super.onPause();
        locationManager.removeUpdates(locationListener);
        log(Level.INFO, "onPause", "onPause success");
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
    protected void onStart() {
        super.onStart();
        log(Level.INFO, "onStart", "onStart success");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        log(Level.INFO, "onDestroy", "onDestroy success");
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        log(Level.INFO, "onRestart", "onRestart success");
    }

    @Override
    protected void onStop() {
        super.onStop();
        log(Level.INFO, "onStop", "onStop success");
    }

    @Override
    public void onMapReady(GoogleMap map) {

        log(Level.INFO, "onMapReady", "onMapReady success");
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.menu_items, menu);
        log(Level.INFO, "onCreateOptionsMenu", "onCreateOptionsMenu success");
        return true;

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()){
            case R.id.interval :
                Toast.makeText(getApplicationContext(), "Yes", Toast.LENGTH_SHORT).show();
        }
        log(Level.INFO, "onOptionsItemSelected", "onOptionsItemSelected success");
        return true;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode == SHOW_PREFERENCES) {
            super.onActivityResult(requestCode, resultCode, data);
        }
        log(Level.INFO, "onActivityResult", "onActivityResult success");
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
        log(Level.INFO, "onClickPreferences", "onClickPreferences success");
    }

    @Override
    public Object onRetainCustomNonConfigurationInstance() {

//        if(initAsync != null & initAsync.getStatus() == AsyncTask.Status.RUNNING)
//            return initAsync;
//        if(getPageAsync != null & getPageAsync.getStatus() == AsyncTask.Status.RUNNING)
//            return getPageAsync;
//        if(getNewPageRevIDAsync != null & getNewPageRevIDAsync.getStatus() == AsyncTask.Status.RUNNING)
//            return getNewPageRevIDAsync;
//        if(getPageRevIDAsync != null & getPageRevIDAsync.getStatus() == AsyncTask.Status.RUNNING)
//            return getPageRevIDAsync;
//        if(getPageTemplatesAsync != null & getPageTemplatesAsync.getStatus() == AsyncTask.Status.RUNNING)
//            return getPageTemplatesAsync;
//        if(getCoordsAsynk != null & getCoordsAsynk.getStatus() == AsyncTask.Status.RUNNING)
//            return getCoordsAsynk;
//        if(getPageRedirectAsync != null & getPageRedirectAsync.getStatus() == AsyncTask.Status.RUNNING)
//            return getPageRedirectAsync;
//        if(getAddressPageForRedirectAsync != null & getAddressPageForRedirectAsync.getStatus() == AsyncTask.Status.RUNNING)
//            return getAddressPageForRedirectAsync;

        log(Level.INFO, "onRetainCustomNonConfigurationInstance", "onRetainCustomNonConfigurationInstance success");
        return null;
//        List<Object> list = new ArrayList<Object>();
//
//        list.add(initAsync);
//        list.add(getPageAsync);
//        list.add(getNewPageRevIDAsync);
//        list.add(getPageRevIDAsync);
//        list.add(getPageTemplatesAsync);
//        list.add(getCoordsAsynk);
//        list.add(getPageRedirectAsync);
//        list.add(getAddressPageForRedirectAsync);
//
//        return list;
    }

    private void InitClusterer(){
        mClusterManager = new ClusterManager<EventsMarker>(this, map);
        mClusterManager.setRenderer(new EventRenderer());
        map.setOnCameraChangeListener(mClusterManager);
        map.setOnMarkerClickListener(mClusterManager);
        map.setOnInfoWindowClickListener(mClusterManager);
        map.setInfoWindowAdapter(mClusterManager.getMarkerManager());
        mClusterManager.getMarkerCollection().setOnInfoWindowAdapter(new MyInfoWindowAdapter());
        mClusterManager.setOnClusterItemClickListener(new ClusterManager.OnClusterItemClickListener<EventsMarker>() {
            @Override
            public boolean onClusterItemClick(EventsMarker item) {
                clickedClusterItem = item;
                return false;
            }
        });
        mClusterManager.setOnClusterItemInfoWindowClickListener(new ClusterManager.OnClusterItemInfoWindowClickListener<EventsMarker>() {
            @Override
            public void onClusterItemInfoWindowClick(EventsMarker item) {
                //ListEventsActivity listEventsActivity = new ListEventsActivity(item);
                Intent intent = new Intent(context, ListEventsActivity.class);
                intent.putExtra("eventWithYearList", eventWithYearList);
                startActivity(intent);

                //intent.putParcelableArrayListExtra("eventWithYearList", eventWithYearList);
                //intent.putExtra("eventWithYearArray", eventWithYearList.toArray());
                //Bundle mBundle = new Bundle();
                //mBundle.putParcelable("eventWithYear", eventWithYearList.get(0));
                //intent.putExtras(mBundle);
                //intent.putExtra("eventWithYear", eventWithYearList.get(0));
                //listEventsActivity.startActivity();
            }
        });

        mClusterManager.setOnClusterClickListener(new ClusterManager.OnClusterClickListener<EventsMarker>() {
            @Override
            public boolean onClusterClick(Cluster<EventsMarker> cluster) {
                return false;
            }
        });
        mClusterManager.setOnClusterInfoWindowClickListener(new ClusterManager.OnClusterInfoWindowClickListener<EventsMarker>() {
            @Override
            public void onClusterInfoWindowClick(Cluster<EventsMarker> cluster) {

            }
        });
        log(Level.INFO, "InitClusterer", "InitClusterer success");
        //addItems();
    }

    private void SetStartFinishYear(SharedPreferences settings){
        int centuryStart = settings.getInt("centStartIndx", 0) + 1;
        int centuryFinish = settings.getInt("centFinishIndx", 0) + 1;
        int yearStart = settings.getInt("yearStartIndx", 0);
        int yearFinish = settings.getInt("yearFinishIndx", 0);


        if(centuryStart < firstCenturyAC){
            //globalYearStart = (centuryStart - firstCenturyAC) * 100 - yearStart;
            globalYearStart = (centuryStart - firstCenturyAC + 1) * 100 - yearStart;
        }
        else{
            globalYearStart = (centuryStart - firstCenturyAC) * 100 + yearStart;
        }
        if(centuryFinish < firstCenturyAC){
            //globalYearFinish = (centuryFinish - firstCenturyAC) * 100 - yearFinish;
            globalYearFinish = (centuryFinish - firstCenturyAC + 1) * 100 - yearFinish;
        }
        else{
            globalYearFinish = (centuryFinish - firstCenturyAC) * 100 + yearFinish;
        }
        log(Level.INFO, "SetStartFinishYear", "SetStartFinishYear success");
    }

    private void GetPagesForUpdateDeleteCreate(){
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        String glYearStart = Integer.toString(globalYearStart);
        String glYearFinish = Integer.toString(globalYearFinish);
        Cursor cursor = db.query("Pages", new String[]{"year", "lastUpdate"}, "year >= ? and year <= ?",
                new String[]{glYearStart, glYearFinish}, null, null, null);

        if(cursor == null || cursor.getCount() == 0){
            for(int _year = globalYearStart; _year <= globalYearFinish; _year++) {
                listForFirstInit.add(_year);
            }
        }

        else {
            cursor.moveToFirst();
            do {

                int yearFromDB = cursor.getInt(cursor.getColumnIndex("year"));
                String str = cursor.getString(cursor.getColumnIndex("lastUpdate"));

                //Log.d(LOG_TAG, str);
                try {
                    Calendar thatDay = Calendar.getInstance(TimeZone.getTimeZone("Europe/Kiev"));
                    dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                    //dateFormat.setTimeZone(TimeZone.getTimeZone("Europe/Kiev"));
                    Date date = dateFormat.parse(str);
                    thatDay.setTime(date);

                    Calendar today = Calendar.getInstance();
                    Date todayD = new Date();
                    today.setTime(todayD);

                    long diff = today.getTimeInMillis() - thatDay.getTimeInMillis(); //result in millis

                    if (diff / (60 * 1000) > 1200) {
                        listForUpdate.add(yearFromDB);
                    } else {
                        listForNotUpdate.add(yearFromDB);
                    }

                } catch (ParseException e) {
                    isCrached = true;
                    e.printStackTrace();
                }

            } while (cursor.moveToNext());

            for (int _year = globalYearStart; _year <= globalYearFinish; _year++) {
                if(!(listForUpdate.contains(_year) || listForNotUpdate.contains(_year))){
                    listForFirstInit.add((_year));
                }
            }
        }

        cursor.close();
        dbHelper.close();
        log(Level.INFO, "GetPagesForUpdateDeleteCreate", "GetPagesForUpdateDeleteCreate success");
    }

    private ArrayMap<Integer, String> GetPageForParse(ArrayList<Integer> listForFirstInit, ArrayList<Integer>listForUpdate){
        try {
            ArrayMap<Integer, String> allEvntsByYear = new ArrayMap<Integer, String>();

            Integer[] masForFirstInit = GetNewPageForInsertIntoDB(listForFirstInit);


            getPageAsync = new GetPageAsync();
            getPageAsync.execute(masForFirstInit);

//        while (getPageAsync.getStatus() != AsyncTask.Status.FINISHED){
//
//        }

            try {
                allEvntsByYear.putAll((Map<? extends Integer, ? extends String>) getPageAsync.get((masForFirstInit.length + 10) / 2, TimeUnit.SECONDS));

            } catch (TimeoutException e) {
                isCrached = true;
                e.printStackTrace();
            } catch (InterruptedException e) {
                isCrached = true;
                e.printStackTrace();
            } catch (ExecutionException e) {
                isCrached = true;
                e.printStackTrace();
            }

            Integer[] masForUpdate = GetMasForGetPageRevID(listForUpdate);


            getPageRevIDAsync = new GetPageRevIDAsync();
            getPageRevIDAsync.execute(masForUpdate);

//        while (getPageRevIDAsync.getStatus() == AsyncTask.Status.RUNNING){}

            listForUpdate = new ArrayList<Integer>();
            try {
                listForUpdate.addAll(getPageRevIDAsync.get((masForUpdate.length + 10) / 2, TimeUnit.SECONDS));
            } catch (TimeoutException e) {
                isCrached = true;
                e.printStackTrace();
            } catch (InterruptedException e) {
                isCrached = true;
                e.printStackTrace();
            } catch (ExecutionException e) {
                isCrached = true;
                e.printStackTrace();
            }

            if (listForUpdate.size() == 0)
                return allEvntsByYear;

            Integer[] listForUpdateMas = listForUpdate.toArray(new Integer[listForUpdate.size()]);
            getPageAsync = new GetPageAsync();
            getPageAsync.execute(listForUpdateMas);

//        while (getPageUpdateAsync.getStatus() == AsyncTask.Status.RUNNING){}

            try {
                allEvntsByYear.putAll((Map<? extends Integer, ? extends String>) getPageAsync.get((listForUpdateMas.length + 10) / 2, TimeUnit.SECONDS));
            } catch (TimeoutException e) {
                isCrached = true;
                e.printStackTrace();
            } catch (InterruptedException e) {
                isCrached = true;
                e.printStackTrace();
            } catch (ExecutionException e) {
                isCrached = true;
                e.printStackTrace();
            }


            return allEvntsByYear;
        }finally {
            log(Level.INFO, "GetPageForParse", "GetPageForParse success");
        }
    }

    private Integer[] GetNewPageForInsertIntoDB(ArrayList<Integer> listForFirstInit){
        if(listForFirstInit != null & listForFirstInit.size() != 0) {
            Integer[] masForFirstInit = listForFirstInit.toArray(new Integer[listForFirstInit.size()]);

            getNewPageRevIDAsync = new GetNewPageRevIDAsync();
            getNewPageRevIDAsync.execute(masForFirstInit);

            ArrayMap<Integer, Long> pagesWithID = null;
            try {
                pagesWithID = getNewPageRevIDAsync.get((masForFirstInit.length + 10) / 2, TimeUnit.SECONDS);
            } catch (TimeoutException e) {
                isCrached = true;
                e.printStackTrace();
            } catch (InterruptedException e) {
                isCrached = true;
                e.printStackTrace();
            } catch (ExecutionException e) {
                isCrached = true;
                e.printStackTrace();
            }

            dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            //dateFormat.setTimeZone(TimeZone.getTimeZone("Europe/Kiev"));
            Date date = new Date();

            for(int i = 0; i < pagesWithID.size(); i++){
                pagesWithID.keyAt(i);
                pagesForIsertDB.add(new PageModel(pagesWithID.keyAt(i), pagesWithID.valueAt(i), dateFormat.format(date)));
            }
            log(Level.INFO, "GetNewPageForInsertIntoDB", "GetNewPageForInsertIntoDB success");
            return listForFirstInit.toArray(new Integer[listForFirstInit.size()]);
        }
        log(Level.INFO, "GetNewPageForInsertIntoDB", "GetNewPageForInsertIntoDB success");
        return new Integer[]{};
    }

    private Integer[] GetMasForGetPageRevID(ArrayList<Integer> listForUpdate) {
        if(listForUpdate != null & listForUpdate.size() != 0) {
            return listForUpdate.toArray(new Integer[listForUpdate.size()]);
        }
        log(Level.INFO, "GetMasForGetPageRevID", "GetMasForGetPageRevID success");
        return new Integer[]{};
    }

    private ArrayList<EventWithLex> ParseLexFromEvents(ArrayList<EventModel> events){

        ArrayList<EventWithLex> eventWithLexList = new ArrayList<EventWithLex>();

        LuceneMorphology luceneMorph = null;
        try {
            luceneMorph = new RussianLuceneMorphology();
        } catch (IOException e) {
            isCrached = true;
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

                if(word != null & word != "" & word != " "){
                    rawWords.add(word);
                }

                x = y + 1;
                y = text.indexOf(" ", x);
            }

            y = text.indexOf(".", x);

            if(y != -1){
                word = text.substring(x, y);
                word = PunctuationHook(word);

                if(word != null & word != "" & word != " "){
                    rawWords.add(word);
                }
            }

            for(String rawWord : rawWords){
                String lex = "";

                rawWord = rawWord.toLowerCase();
                try {//�� �����
                    try {
                        wordBaseForms = luceneMorph.getMorphInfo(rawWord);
                    } catch (Exception e){wordBaseForms = null;}

                    if(wordBaseForms != null) {
                        int pos = (wordBaseForms.get(0)).indexOf("|") + 3;
                        String partOfSpeach = wordBaseForms.get(0).substring(pos, pos + 1);
                        char pOS = partOfSpeach.charAt(0);

                        if(pOS == noun){
                            lex = wordBaseForms.get(0).substring(0, pos - 3);
                            if(lex.compareTo("���") != 0)
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
                    isCrached = true;
                    e.printStackTrace();
                }


            }

            eventWithLexList.add(new EventWithLex(evModel, lexemes));
        }
        log(Level.INFO, "ParseLexFromEvents", "ParseLexFromEvents success");
        return eventWithLexList;

    }

    private List<EventWithLex> GetRedirectForLexemes(List<EventWithLex> eventWithLexes){

        ArrayList<String> rawLex = new ArrayList<String>();

        for(EventWithLex eventWithLex : eventWithLexes){
            rawLex.addAll(eventWithLex.lexemes);
        }

        String[] rawLexMas = rawLex.toArray(new String[rawLex.size()]);

        getPageRedirectAsync = new GetPageRedirectAsync();
        getPageRedirectAsync.execute(rawLexMas);

        try {
            ArrayList<String> lexForRedirect = getPageRedirectAsync.get((rawLexMas.length + 10) / 2, TimeUnit.SECONDS);

            for(String lexForRedir : lexForRedirect){
                getAddressPageForRedirectAsync =
                        new GetAddressPageForRedirectAsync();
                getAddressPageForRedirectAsync.execute(lexForRedir);
                String lexToRedir = getAddressPageForRedirectAsync.get(5000, TimeUnit.MILLISECONDS);

                lexToRedir = lexToRedir.toLowerCase();
                lexForRedir = lexForRedir.toLowerCase();

                if(lexToRedir.compareTo(lexForRedir) != 0) {
                    for (int i = 0; i < eventWithLexes.size(); i++) {

                        int indx = eventWithLexes.get(i).lexemes.indexOf(lexForRedir);
                        while (indx >= 0) {
                            eventWithLexes.get(i).lexemes.set(indx, lexToRedir);
                            indx = eventWithLexes.get(i).lexemes.indexOf(lexForRedir);
                        }

                    }
                }
            }

        } catch (InterruptedException e) {
            isCrached = true;
            e.printStackTrace();
        } catch (ExecutionException e) {
            isCrached = true;
            e.printStackTrace();
        } catch (TimeoutException e) {
            isCrached = true;
            e.printStackTrace();
        } finally {
            log(Level.INFO, "GetRedirectForLexemes", "GetRedirectForLexemes success");
            return eventWithLexes;
        }

    }

    private SortedSet<EventModel> GetCoordForEvent(List<EventWithLex> eventWithLexes){

        SortedSet<EventModel> events = new TreeSet<EventModel>();
        ArrayList<String> allLexemes = new ArrayList<String>();

        for(EventWithLex eventWithLex : eventWithLexes){
            allLexemes.addAll(eventWithLex.lexemes);
        }

        HashSet<String> set = new HashSet<String>(allLexemes);

        String[] rawLexMas = set.toArray(new String[set.size()]);

        getPageTemplatesAsync = new GetPageTemplatesAsync();
        getPageTemplatesAsync.execute(rawLexMas);

        try {
            ArrayList<String> lexesWithCoord = getPageTemplatesAsync.get((rawLexMas.length + 10) / 2, TimeUnit.SECONDS);

            String[] lexesWithCoordMas = lexesWithCoord.toArray(new String[lexesWithCoord.size()]);

            getCoordsAsynk = new GetCoordsAsynk();
            getCoordsAsynk.execute(lexesWithCoordMas);

            ArrayMap<String, Coordinate> placesWithCoord = getCoordsAsynk.get((lexesWithCoordMas.length + 10) / 2, TimeUnit.SECONDS);



            for(int i = 0; i < eventWithLexes.size(); i++){
                ArrayList<String> lexesFromEvent = eventWithLexes.get(i).lexemes;

                for(String lexFromEvent : lexesFromEvent){

                    int ind = placesWithCoord.indexOfKey(lexFromEvent);
                    if(ind >= 0){
                        eventWithLexes.get(i).evntModel.coord = placesWithCoord.valueAt(ind);
                        //����� ������������� ������ ���������� ����������

                        break;
                    }
                }

            }

            for(EventWithLex eventWithLex : eventWithLexes){
                events.add(eventWithLex.evntModel);
                //mClusterManager.setOnClusterItemClickListener(eventWithLex.evntModel.coord);
            }

        } catch (InterruptedException e) {
            isCrached = true;
            e.printStackTrace();
        } catch (ExecutionException e) {
            isCrached = true;
            e.printStackTrace();
        } catch (TimeoutException e) {
            isCrached = true;
            e.printStackTrace();
        } finally {
            log(Level.INFO, "GetCoordForEvent", "GetCoordForEvent success");
            return events;
        }

    }



    private LocationListener locationListener = new LocationListener() {
        @Override
        public void onLocationChanged(Location location) {
            ShowLocation(location);
            log(Level.INFO, "onLocationChanged", "onLocationChanged success");
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
            if(provider.equals(locationManager.GPS_PROVIDER)){

            }
            else if(provider.equals(locationManager.NETWORK_PROVIDER)){

            }
            log(Level.INFO, "onStatusChanged", "onStatusChanged success");
        }

        @Override
        public void onProviderEnabled(String provider) {
            CheckEnable();
            ShowLocation(locationManager.getLastKnownLocation(provider));
            log(Level.INFO, "onProviderEnabled", "onProviderEnabled success");
        }

        @Override
        public void onProviderDisabled(String provider) {
            CheckEnable();
            log(Level.INFO, "onProviderDisabled", "onProviderDisabled success");
        }
    };

    private void ShowLocation(Location location){
        if(location == null)
            return;
        if(location.getProvider().equals(locationManager.GPS_PROVIDER)){

        }
        else if(location.getProvider().equals(locationManager.NETWORK_PROVIDER)){

        }
        log(Level.INFO, "ShowLocation", "ShowLocation success");
    }

    private String FormatLocation(Location location){
        log(Level.INFO, "FormatLocation", "FormatLocation success");
        if(location == null)
            return "";
        return String.format(Double.toString(location.getLatitude()), Double.toString(location.getLongitude())
                , new Date(location.getTime()).toString());
    }

    private void CheckEnable(){
        log(Level.INFO, "CheckEnable", "CheckEnable success");
    }



    private class InitAsync extends AsyncTask<Void, Void, Integer > {

        protected Integer doInBackground(Void... params) {


            log(Level.INFO, "doInBackground", "InitAsync -> doInBackground success");
            return 1;
        }

        @Override
        protected void onPostExecute(Integer integer) {
            super.onPostExecute(integer);

            log(Level.INFO, "doInBackground", "InitAsync -> onPostExecute success");
        }

    }

    private class GetPageAsync extends AsyncTask<Integer, String, ArrayMap<Integer, String>> {

        protected ArrayMap<Integer, String> doInBackground(Integer... params) {

            int count = params.length;
            ArrayMap<Integer, String> pages = new ArrayMap<Integer, String>();
            //long totalSize = 0;
            for (int i = 0; i < count; i++) {
                String year = String.valueOf(params[i]) + "_���";
                if (params[i] < 0) {
                    year += "_��_�._�.";
                    year = year.substring(1);
                }
                try {
                    String text = wikipedia.getPageText(year);
                    pages.put(params[i], text);
                } catch (IOException e) {
                    isCrached = true;
                    e.printStackTrace();
                    //return page;
                }
            }
            //String[] pagesMas = pages.toArray(new String[pages.size()]);
            //return pages;



            if(pages == null)
                return new ArrayMap<Integer, String>();

            ArrayMap<Integer, String> allEvntsByYear = new ArrayMap<Integer, String>();


            for(int i = 0; i < pages.size(); i++) {
                if (!pages.valueAt(i).contains("#���������������") &
                        !pages.valueAt(i).contains("#REDIRECT") &
                        !pages.valueAt(i).contains("#redirect"))
                    allEvntsByYear.put(pages.keyAt(i), pages.valueAt(i));
            }
            log(Level.INFO, "doInBackground", "GetPageAsync -> doInBackground success");
            return allEvntsByYear;
        }

//        protected void onProgressUpdate(Integer... progress) {
//            setProgressPercent(progress[0]);
//        }
//
//        protected void onPostExecute(ArrayMap<Integer, String> result) {
//            if(result == null)
//                return;
//
//            ArrayMap<Integer, String> allEvntsByYear = new ArrayMap<Integer, String>();
//
//
//            for(int i = 0; i < result.size(); i++) {
//                if (!result.valueAt(i).contains("#���������������") &
//                        !result.valueAt(i).contains("#REDIRECT") &
//                        !result.valueAt(i).contains("#redirect"))
//                    allEvntsByYear.put(result.keyAt(i), result.valueAt(i));
//            }
//
//            ParseEvent(allEvntsByYear);
//        }

    }

    private class GetNewPageRevIDAsync extends AsyncTask<Integer, String, ArrayMap<Integer, Long>> {

        protected ArrayMap<Integer, Long> doInBackground(Integer... params) {
            int count = listForFirstInit.size();
            int rowCount = count / 50;
            if(listForFirstInit.size() % 50 != 0)
                rowCount++;

            ArrayList<String> pages = new ArrayList<String>();
            ArrayMap<Integer, Long> pagesWithID = new ArrayMap<Integer, Long>();

            for(int i = 0; i < rowCount; i++){
                for(int j = 0; j < 50 & count > i * 50 + j; j++){
                    String year = String.valueOf(listForFirstInit.get(i * 50 + j)) + "_���";
                    if (listForFirstInit.get(i * 50 + j) < 0) {
                        year += "_��_�._�.";
                        year = year.substring(1);
                    }

                    pages.add(year);
                }

                try {
                    pagesWithID.putAll((Map<? extends Integer, ? extends Long>) wikipedia.getPagesRevId(pages));
                } catch (IOException e) {
                    isCrached = true;
                    e.printStackTrace();
                }
                pages.clear();
            }
            log(Level.INFO, "doInBackground", "GetNewPageRevIDAsync -> doInBackground success");
            return pagesWithID;
        }
    }

    private class GetPageRevIDAsync extends AsyncTask<Integer, String, ArrayList<Integer>> {

        protected ArrayList<Integer> doInBackground(Integer... params) {
            int count = params.length;
            long revId = 0;

            int rowCount = count / 50;
            if(params.length % 50 != 0)
                rowCount++;

            ArrayList<String> pages = new ArrayList<String>();
            ArrayMap<Integer, Long> pagesWithID = new ArrayMap<Integer, Long>();

            for(int i = 0; i < rowCount; i++){
                for(int j = 0; j < 50 & count > i * 50 + j; j++){
                    String year = String.valueOf(params[i * 50 + j]) + "_���";
                    if (params[i * 50 + j] < 0) {
                        year += "_��_�._�.";
                        year = year.substring(1);
                    }

                    pages.add(year);
                }
                try {
                    pagesWithID.putAll((Map<? extends Integer, ? extends Long>) wikipedia.getPagesRevId(pages));
                } catch (IOException e) {
                    isCrached = true;
                    e.printStackTrace();
                }
                pages.clear();
            }

            //return pagesWithID;




            if(pagesWithID == null || pagesWithID.size() == 0)
                return new ArrayList<Integer>();

            //long res = Long.parseLong(result[1]);
            SQLiteDatabase db = dbHelper.getWritableDatabase();

            Cursor cursor = db.query("Pages", new String[]{"year, revisionID"}, "year >= ? and year <= ?",
                    new String[]{String.valueOf(globalYearStart), String.valueOf(globalYearFinish)}, null, null, null);

            ArrayList<Integer> listForUpdateInner = new ArrayList<Integer>();

            dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            //dateFormat.setTimeZone(TimeZone.getTimeZone("Europe/Kiev"));
            Date date = new Date();

            if(cursor != null){
                if(cursor.moveToFirst()){
                    do {
                        int year = cursor.getInt(cursor.getColumnIndex("year"));
                        long revID = cursor.getInt(cursor.getColumnIndex("revisionID"));

                        if(pagesWithID.get(new Integer(year)) == null){
                            //pagesForDeleteDB.add(new PageModel(year, revID, dateFormat.format(date)));
                        }
                        else{
                            long revIDFromWiki = pagesWithID.get(new Integer(year));
                            if (revIDFromWiki != revID) {
                                listForUpdateInner.add(new Integer(year));
                            }
                            pagesForUdateDB.add(new PageModel(year, revIDFromWiki, dateFormat.format(date)));
                        }

                    } while (cursor.moveToNext());
                    //return listForUpdateInner;
                }
                cursor.close();
                //return listForUpdateInner;
            }
            dbHelper.close();
            log(Level.INFO, "doInBackground", "GetPageRevIDAsync -> doInBackground success");
            return listForUpdateInner;

        }

        //        protected void onProgressUpdate(Integer... progress) {
//            setProgressPercent(progress[0]);
//        }

//        protected void onPostExecute(ArrayMap<Integer, Long> pagesWithID) {
//            if(pagesWithID == null)
//                return;
//
//            //long res = Long.parseLong(result[1]);
//            SQLiteDatabase db = dbHelper.getWritableDatabase();
//
//            Cursor cursor = db.query("Page", new String[]{"year, revisionID"}, "year >= ? and year <= ?",
//                    new String[]{String.valueOf(startDate), String.valueOf(finishDate)}, null, null, null);
//
//            ArrayList<Integer> listForUpdate = new ArrayList<Integer>();
//
//            dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
//            dateFormat.setTimeZone(TimeZone.getTimeZone("Europe/Kiev"));
//            Date date = new Date();
//
//            if(cursor != null){
//                if(cursor.moveToFirst()){
//                    do {
//                        int year = cursor.getInt(cursor.getColumnIndex("year"));
//                        long revID = cursor.getInt(cursor.getColumnIndex("revisionID"));
//
//                        if(pagesWithID.get(new Integer(year)) == null){
//                            pagesForDeleteDB.add(new PageModel(year, revID, dateFormat.format(date)));
//                        }
//                        else{
//                            long revIDFromWiki = pagesWithID.get(new Integer(year));
//                            if (revIDFromWiki != revID) {
//                                listForUpdate.add(new Integer(year));
//                            }
//                            pagesForUdateDB.add(new PageModel(year, revIDFromWiki, dateFormat.format(date)));
//                        }
//
//                    } while (cursor.moveToNext());
//
//                    cursor.close();
//
//                    //ContentValues cv = new ContentValues();
//
//                    Integer[] listForUpdateMas = listForUpdate.toArray(new Integer[listForUpdate.size()]);
//                    getPageAsync = new GetPageAsync();
//                    getPageAsync.execute(listForUpdateMas);
//
////                    try {
////                        getPageAsync.get(50000, TimeUnit.MILLISECONDS);
////
////                        cv.put("lastUpdate", Calendar.getInstance().toString());
////                        db.update("Page", cv, "year >= ? and year <= ?",
////                                new String[]{String.valueOf(startDate), String.valueOf(finishDate)});
////                    } catch (InterruptedException e) {
////                        e.printStackTrace();
////                    } catch (ExecutionException e) {
////                        e.printStackTrace();
////                    } catch (TimeoutException e) {
////                        e.printStackTrace();
////                    }
////                    finally {
//                        dbHelper.close();
//                    //}
//
//                }
//
//            }
//        }
    }

    private class GetPageTemplatesAsync extends AsyncTask<String, String, ArrayList<String>> {

        protected ArrayList<String> doInBackground(String... strs) {

            ArrayList<String> lexesList = new ArrayList<String>(Arrays.asList(strs));
            //ArrayList<String> titleWithCoordTemplate = wikipedia.getTitlePageWithCoordTemplate(lexesList);


            ArrayList<String> titleWithCoordTemplate = new ArrayList<String>();
            ArrayList<String> tempList = new ArrayList<String>();

            int sizeParamsList = lexesList.size();
            int rowCount = sizeParamsList / 50;
            if(lexesList.size() % 50 != 0)
                rowCount++;

            for(int i = 0; i < rowCount; i++){
                for(int j = 0; j < 50 & sizeParamsList > i * 50 + j; j++){
                    tempList.add(lexesList.get(i * 50 + j));
                }

                try {
                    titleWithCoordTemplate.addAll(wikipedia.getTitlePageWithCoordTemplate(tempList));
                } catch (IOException e) {
                    isCrached = true;
                    e.printStackTrace();
                }
                tempList.clear();
            }


            for(int i = 0; i < titleWithCoordTemplate.size(); i++){
                titleWithCoordTemplate.set(i, StringEscapeUtils.unescapeJava(titleWithCoordTemplate.get(i)));
            }

//            if (wikipedia.isContainCoordTemplate(strs[0])){
//                return strs[0];
//            }
//            else
//                return "";
            log(Level.INFO, "doInBackground", "GetPageTemplatesAsync -> doInBackground success");
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

            ArrayList<String> paramsList = new ArrayList<String>(Arrays.asList(params));
            ArrayMap<String, Coordinate> placesWithCoord = new ArrayMap<String, Coordinate>();
            ArrayMap<String, Coordinate> coord =  new ArrayMap<String, Coordinate>();

            //ArrayList<String> titleWithCoordTemplate = new ArrayList<String>();
            ArrayList<String> tempList = new ArrayList<String>();

            int sizeParamsList = paramsList.size();
            int rowCount = sizeParamsList / 50;
            if(paramsList.size() % 50 != 0)
                rowCount++;

            for(int i = 0; i < rowCount; i++){
                for(int j = 0; j < 50 & sizeParamsList > i * 50 + j; j++){
                    tempList.add(paramsList.get(i * 50 + j));
                }

                ArrayMap<String, LatLng> latLng = null;
                try {
                    latLng = wikipedia.getCoordinateForPlaces(tempList);
                } catch (IOException e) {
                    isCrached = true;
                    e.printStackTrace();
                }

                for(int j = 0; j < latLng.size(); j++){
                    placesWithCoord.put(latLng.keyAt(j), new Coordinate(latLng.valueAt(j)));
                }

                //placesWithCoord.putAll((Map<? extends String, ? extends Coordinate>) coord);
                tempList.clear();
                coord.clear();
            }
            log(Level.INFO, "doInBackground", "GetCoordsAsynk -> doInBackground success");
            return  placesWithCoord;
        }
    }

    private class GetPageRedirectAsync extends AsyncTask<String, String, ArrayList<String>>{
        @Override
        protected ArrayList<String> doInBackground(String... params) {

            ArrayList<String> paramsList = new ArrayList<String>(Arrays.asList(params));
            HashSet<String> paramsSet = new HashSet<String>();
            ArrayList<String> titleWithRedirect = new ArrayList<String>();

            int sizeParamsList = paramsList.size();
            int rowCount = sizeParamsList / 50;
            if(paramsList.size() % 50 != 0)
                rowCount++;

            for(int i = 0; i < rowCount; i++){
                for(int j = 1; j <= 50 & sizeParamsList > i * 50 + j; j++){
                    paramsSet.add(paramsList.get(i * 50 + j));
                }

                try {
                    titleWithRedirect.addAll(wikipedia.getTitlePageWithRedirect(paramsSet));
                } catch (IOException e) {
                    isCrached = true;
                    e.printStackTrace();
                }
                paramsSet.clear();
            }
            //HashSet<String> paramsSet = new HashSet<String>(paramsList);


            for(int i = 0; i < titleWithRedirect.size(); i++){
                titleWithRedirect.set(i, StringEscapeUtils.unescapeJava(titleWithRedirect.get(i)));
            }
            log(Level.INFO, "doInBackground", "GetPageRedirectAsync -> doInBackground success");
            return titleWithRedirect;
        }
    }

    private class GetAddressPageForRedirectAsync extends AsyncTask<String, String, String>{
        @Override
        protected String doInBackground(String... params) {
            String str = null;
            try {
                str = wikipedia.getRedirectForPage(params[0]);
            } catch (IOException e) {
                isCrached = true;
                e.printStackTrace();
            }
            log(Level.INFO, "doInBackground", "GetAddressPageForRedirectAsync -> doInBackground success");
            return str;
        }
    }



    private void WriteDB(SortedSet<EventModel> events) {

        db = dbHelper.getWritableDatabase();
        ContentValues cv = new ContentValues();

        db.beginTransaction();
        for(EventModel eventModel: events){

            cv.put("year", eventModel.year);
            cv.put("event", eventModel.text);
            if(eventModel.coord != null) {
                cv.put("latitude", eventModel.coord.getmPosition().latitude);
                cv.put("longitude", eventModel.coord.getmPosition().longitude);
            }

            db.insert("Events", null, cv);

            cv.clear();
        }

        for(EventModel eventModel: eventsForDeleteDB){
//            db.delete("Events", "year = ? and text = ?",
//                    new String[]{String.valueOf(eventModel.year), String.valueOf(eventModel.text)});
            db.delete("Events", "year = " + String.valueOf(eventModel.year) + " and event = \"" + eventModel.text + "\"", null);
        }

//        for(PageModel pageModel : pagesForDeleteDB){
////            db.delete("Pages", "year = ? and revisionID = ?",
////                    new String[]{String.valueOf(pageModel.year), String.valueOf(pageModel.revID)});
//            db.delete("Pages", "year = " + String.valueOf(pageModel.year) +
//                    " and revisionID = " + String.valueOf(pageModel.revID), null);
//        }

        for(PageModel pageModel : pagesForUdateDB) {

            cv.put("revisionID", pageModel.revID);
            cv.put("lastUpdate", pageModel.lastUpdate);

            db.update("Pages", cv, "year = ?",
                    new String[]{String.valueOf(pageModel.year)});

            cv.clear();
        }

        for(PageModel pageModel : pagesForIsertDB){

            cv.put("year", pageModel.year);
            cv.put("revisionID", pageModel.revID);
            cv.put("lastUpdate", pageModel.lastUpdate);

            db.insert("Pages", null, cv);

            cv.clear();
        }

        db.setTransactionSuccessful();
        db.endTransaction();

        dbHelper.close();
        log(Level.INFO, "WriteDB", "WriteDB success");
    }

    private void MakeMarkers() {
        mClusterManager.clearItems();
        map.clear();

        TreeSet<EventModel> sortedEvent = new TreeSet<EventModel>();
        db = dbHelper.getWritableDatabase();

        Cursor cursor = db.query("Events", new String[]{"year, event, latitude, longitude"}, "year >= ? and year <= ?",
                new String[]{String.valueOf(globalYearStart), String.valueOf(globalYearFinish)}, null, null, null);

        if (cursor.moveToFirst()) {
            do {
                EventModel eventModel = new EventModel();
                eventModel.setYear(cursor.getInt(cursor.getColumnIndex("year")));
                eventModel.setText(cursor.getString(cursor.getColumnIndex("event")));
                if(!cursor.isNull(cursor.getColumnIndex("latitude")) & !cursor.isNull(cursor.getColumnIndex("longitude"))) {
                    Coordinate coordinate = new Coordinate(cursor.getDouble(cursor.getColumnIndex("latitude")),
                            cursor.getDouble(cursor.getColumnIndex("longitude")));
                    eventModel.setCoord(coordinate);
                }
                sortedEvent.add(eventModel);
            } while (cursor.moveToNext());
        }

        dbHelper.close();


        ArrayList<EventsMarker> eventsMarkers = new ArrayList<EventsMarker>();

        EventsMarker eventsMarker = new EventsMarker(new LatLng(181, 181));
        EventWithYear eventWithYear = null;

        for (EventModel eventModel : sortedEvent) {
            if (eventModel.coord != null) {
                if (eventModel.coord.getmPosition().latitude == eventsMarker.coordinate.latitude &
                        eventModel.coord.getmPosition().longitude == eventsMarker.coordinate.longitude) {
                    eventWithYear = new EventWithYear(eventModel.text, eventModel.year);
                    eventsMarkers.get(eventsMarkers.size() - 1).addEventWithYear(eventWithYear);
                } else {
                    eventsMarker = new EventsMarker(eventModel.coord.getmPosition());
                    eventWithYear = new EventWithYear(eventModel.text, eventModel.year);
                    eventsMarker.addEventWithYear(eventWithYear);
                    eventsMarkers.add(new EventsMarker(eventsMarker));
                }

            }
        }

        for (int i = 0; i < eventsMarkers.size(); i++) {
            switch (eventsMarkers.get(i).eventWithYears.size()) {
                case 1:
                    eventsMarkers.get(i).iconID = R.drawable.number_1;
                    break;
                case 2:
                    eventsMarkers.get(i).iconID = R.drawable.number_2;
                    break;
                case 3:
                    eventsMarkers.get(i).iconID = R.drawable.number_3;
                    break;
                case 4:
                    eventsMarkers.get(i).iconID = R.drawable.number_4;
                    break;
                case 5:
                    eventsMarkers.get(i).iconID = R.drawable.number_5;
                    break;
                case 6:
                    eventsMarkers.get(i).iconID = R.drawable.number_6;
                    break;
                case 7:
                    eventsMarkers.get(i).iconID = R.drawable.number_7;
                    break;
                case 8:
                    eventsMarkers.get(i).iconID = R.drawable.number_8;
                    break;
                case 9:
                    eventsMarkers.get(i).iconID = R.drawable.number_9;
                    break;
                case 10:
                    eventsMarkers.get(i).iconID = R.drawable.number_10;
                    break;

                default:
                    eventsMarkers.get(i).iconID = R.drawable.number_10_;
                    break;

//            String path = "mapicons-numbers/number_" + eventsMarkers.get(i).eventWithYears.size() + ".png";
//            eventsMarkers.get(i).pathToIcon = path;
            }
        }

        mClusterManager.addItems(eventsMarkers);
        log(Level.INFO, "MakeMarkers", "MakeMarkers success");
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
        log(Level.INFO, "ParseCoord", "ParseCoord success");
        return new Coordinate(latitude, longitude);
    }

    private ArrayList<EventModel> ParseEvent(ArrayMap<Integer, String> eventsByYear){

        List<EventModel> eventList = new ArrayList<EventModel>();

        String eventItem = null;
        int start;
        int finish;
        int startEv;
        int finishEv;
        boolean isLast = false;
        boolean isDate = false;

        for(int i = 0; i < eventsByYear.size(); i++) {
            isLast = false;
            String events = eventsByYear.valueAt(i);

            start = events.indexOf("== ������� ==");
            if(start < 0) {
                start = events.indexOf("== ��������� ������� ==");
                finish = events.indexOf(" ==\n", start + 24);
            }
            else
                finish = events.indexOf(" ==\n", start + 14);
            if (finish != -1)
                events = events.substring(start + 14, finish);
            else events = events.substring(start + 14);

            startEv = events.indexOf("*");
            finishEv = events.indexOf("*", startEv + 1);

            while (finishEv > 0) {

                if ((finishEv - startEv) != 1 & (finishEv - startEv) != 0) {
                    if ((finishEv - startEv) > 3) {
                        if (finishEv - startEv > 18) {//�� ���� � ����������� ��������
                            isDate = false;
                            eventItem = events.substring(startEv + 2, finishEv - 1);

                            eventItem = ParseEventHelper(eventItem);

                            log(Level.INFO, "ParseEvent", "new EventModel " + Integer.toString(eventsByYear.keyAt(i)) +
                                    ": " + eventItem);
                            eventList.add(new EventModel(eventItem, eventsByYear.keyAt(i)));

                        } else { //���� � ����������� ��������
                            String str = events.substring(startEv + 2, finishEv - 1).trim();
                            if(str.compareTo("") != 0) {
                                isDate = true;
                                eventItem = events.substring(startEv + 4, finishEv - 3) + " � ";
                            }
                        }
                    }
                } else {
                    if (!isLast & isDate) {
                        startEv++;
                        finishEv = events.indexOf("*", startEv + 1);

                        int indxDef = eventItem.indexOf(" � ");

                        if (indxDef != -1) {
                            if(finishEv > -1)
                                eventItem = eventItem.substring(0, indxDef + 3) +
                                        events.substring(startEv + 2, finishEv - 1);

                            else {
                                eventItem = eventItem.substring(0, indxDef + 3) +
                                        events.substring(startEv + 2);
                                finishEv = events.length() - 2;
                            }
                        }
                        else {
                            if(finishEv > -1)
                                eventItem = eventItem +
                                    events.substring(startEv + 2, finishEv - 1);
                            else {
                                eventItem = eventItem +
                                        events.substring(startEv + 2);
                                finishEv = events.length() - 2;
                            }
                        }

                        eventItem = ParseEventHelper(eventItem);

                        log(Level.INFO, "ParseEvent", "new EventModel " + Integer.toString(eventsByYear.keyAt(i)) +
                                ": " + eventItem);
                        eventList.add(new EventModel(eventItem, eventsByYear.keyAt(i)));
                    }
                }

                startEv = finishEv;
                finishEv = events.indexOf("*", startEv + 1);
                if (finishEv < 0 & !isLast) {
                    finishEv = events.indexOf("\n", startEv + 1) + 1;
                    isLast = true;
                }
            }
        }

        db = dbHelper.getWritableDatabase();

        Cursor cursor = db.query("Events", new String[]{"year", "event"}, "year >= ? and year <= ?",
                new String[]{String.valueOf(globalYearStart), String.valueOf(globalYearFinish)}, null, null, null);

        ArrayList<EventModel> eventsFromDB = new ArrayList<EventModel>();
        //ArrayList<EventModel> eventsToDelFromDB = new ArrayList<EventModel>();
        ArrayList<EventModel> eventsToParseLex = new ArrayList<EventModel>();

        if(cursor == null || cursor.getCount() == 0){
            eventsToParseLex.addAll(eventList);
        }

        else {
            cursor.moveToFirst();
            do {
                int yearFromDB = cursor.getInt(cursor.getColumnIndex("year"));
                String textFromDB = cursor.getString(cursor.getColumnIndex("event"));

                eventsFromDB.add(new EventModel(textFromDB, yearFromDB));
            } while (cursor.moveToNext());

            for(EventModel evntMod : eventList){
                if (!(eventsFromDB.contains(evntMod))){
                    eventsToParseLex.add(evntMod);
                }
            }
        }
        cursor.close();
        dbHelper.close();


        for(EventModel evntMod : eventsFromDB){
            if (!(eventList.contains(evntMod)) & eventsByYear.keySet().contains(evntMod.year)){
                //eventsToDelFromDB.add(evntMod);
                eventsForDeleteDB.add(new EventModel(evntMod.text, evntMod.year));
                //db.delete("Event", "year = ? and text = ?", new String[]{String.valueOf(evntMod.year), evntMod.text});
            }
        }
        log(Level.INFO, "ParseEvent", "ParseEvent success");
        return eventsToParseLex;

    }

    private String ParseEventHelper(String eventItem){
        int startRef;
        int finishRef;

        int indxSlash = eventItem.indexOf("==");
        if (indxSlash != -1) {
            eventItem = eventItem.substring(0, indxSlash - 2);
        }


        startRef = eventItem.indexOf("<ref");
        while (startRef != -1) {
            startRef = eventItem.indexOf("<ref>");
            if (startRef != -1) {
                finishRef = eventItem.indexOf("</ref>", startRef);
                if (finishRef < 0)
                    eventItem = eventItem.substring(0, startRef);
                else
                    eventItem = eventItem.substring(0, startRef) + eventItem.substring(finishRef + 6);

                startRef = eventItem.indexOf("<ref");
            } else {
                startRef = eventItem.indexOf("<ref");
                finishRef = eventItem.indexOf("/>", startRef);
                if (finishRef != -1) {
                    eventItem = eventItem.substring(0, startRef) + eventItem.substring(finishRef + 2);
                } else {
                    finishRef = eventItem.indexOf("</ref>", startRef);
                    if (finishRef > -1)
                        eventItem = eventItem.substring(0, startRef) + eventItem.substring(finishRef + 6);
                    else
                        eventItem = eventItem.substring(0, startRef);
                }
                startRef = eventItem.indexOf("<ref");
            }

        }

        while (eventItem.contains("[[")) {
            eventItem = TrimHooks(eventItem);
        }
        return eventItem;
    }


    private String PunctuationHook(String word){

        word = word.replace(" ", "");
        word = word.replace(",", "");
        word = word.replace(";", "");
        word = word.replace(":", "");
        word = word.replace(".", "");
        word = word.replace("...", "");
        word = word.replace(")", "");
        word = word.replace("(", "");
        word = word.replace("�", "");
        word = word.replace("\"", "");
        word = word.replace("'", "");
        log(Level.INFO, "PunctuationHook", "PunctuationHook success");
        return  word;
    }

    private String TrimHooks(String evnt) {

        //int startToNext = start;
        try {
            int startEv = evnt.indexOf("[[");
            if (startEv != -1) {
                int finishEv = evnt.indexOf("]]", startEv + 2);
                //startToNext =+ 4;

                if (finishEv != -1) {
                    String inHooks = evnt.substring(startEv + 2, finishEv);
                    int separ = inHooks.indexOf("|");
                    if (separ != -1) {
                        //startToNext += inHooks.length() - separ - 1;
                        inHooks = inHooks.substring(separ + 1, inHooks.length());
                    }

                    evnt = evnt.substring(0, startEv) + inHooks + evnt.substring(finishEv + 2, evnt.length());
                }
                else {
                    //TrimHooks(evnt);
                    evnt =  evnt.substring(0, startEv) + evnt.substring(startEv + 2, evnt.length() - 1);
                }
            }
//        else {
//            String hi = evnt;
//            return;
//        }
            return evnt;
        } finally {
            log(Level.INFO, "TrimHooks", "TrimHooks success");
        }

    }



    private class DBHelper extends SQLiteOpenHelper{

        public DBHelper(Context context){
            super(context, "chronDB", null, 1);
            log(Level.INFO, "DBHelper", "DBHelper -> DBHelper success");
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL("create table Pages " +
                    "(year integer not null unique, " + "revisionID integer, " + "lastUpdate text)");

            db.execSQL("create table Events " +
                    "(year integer, " + "event text, " +
                    "latitude double, " + "longitude double)");
            log(Level.INFO, "onCreate", "DBHelper -> onCreate success");
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            log(Level.INFO, "onUpgrade", "DBHelper -> onUpgrade success");
        }
    }

    private class EventRenderer extends DefaultClusterRenderer<EventsMarker> {
        private final ImageView mImageView;
        private final int mDimension;

        public EventRenderer() {
            super(getApplicationContext(), map, mClusterManager);

            mImageView = new ImageView(getApplicationContext());
            mDimension = (int) getResources().getDimension(R.dimen.custom_profile_image);
            mImageView.setLayoutParams(new ViewGroup.LayoutParams(mDimension, mDimension));
            log(Level.INFO, "EventRenderer", "EventRenderer -> EventRenderer success");
        }

        @Override
        protected void onBeforeClusterItemRendered(EventsMarker item, MarkerOptions markerOptions) {
            mImageView.setImageResource(item.iconID);
            markerOptions.icon(BitmapDescriptorFactory.fromResource(item.iconID))
                    .title(item.eventWithYears.get(0).text)
                    .snippet(Integer.toString(item.eventWithYears.get(0).year));
            log(Level.INFO, "onBeforeClusterItemRendered", "EventRenderer -> onBeforeClusterItemRendered success");
        }

//        @Override
//        protected void onBeforeClusterRendered(Cluster<EventsMarker> cluster, MarkerOptions markerOptions) {
//            super.onBeforeClusterRendered(cluster, markerOptions);
//        }

        @Override
        protected boolean shouldRenderAsCluster(Cluster cluster) {
            // Always render clusters.
            log(Level.INFO, "shouldRenderAsCluster", "EventRenderer -> shouldRenderAsCluster success");
            return cluster.getSize() > 1;
        }
    }

    private class EventWithLex{

        EventModel evntModel;
        ArrayList<String> lexemes;

        public EventWithLex(EventModel evModel, ArrayList<String> lxms){
            evntModel= evModel;
            lexemes = lxms;
            log(Level.INFO, "EventWithLex", "EventWithLex -> EventWithLex success");
        }
    }

    private class MyInfoWindowAdapter implements GoogleMap.InfoWindowAdapter {
        @Override
        public View getInfoContents(Marker marker) {
            View v = getLayoutInflater().inflate(R.layout.info_window_layout, null);
            LinearLayout layout = (LinearLayout) v.findViewById(R.id.main_layout);
            eventWithYearList = new ArrayList<EventWithYear>();
            if (clickedClusterItem != null) {
                eventWithYearList = clickedClusterItem.getEventWithYears();

                for (int i = 0; i < eventWithYearList.size() & i < 3; i++) {
                    TextView tViewEvent = new TextView(context);
                    TextView tViewDate = new TextView(context);
                    tViewEvent.setTextColor(getResources().getColor(R.color.black));
                    tViewDate.setTextColor(getResources().getColor(R.color.grey));
                    tViewDate.setGravity(Gravity.RIGHT);
                    tViewDate.setTextSize(11);
                    tViewEvent.setMaxHeight(50);
                    tViewEvent.setMaxWidth(300);
                    tViewDate.setMaxHeight(50);
                    tViewDate.setMaxWidth(300);
                    tViewEvent.setText(eventWithYearList.get(i).text);
                    tViewDate.setText(Integer.toString(eventWithYearList.get(i).year));

                    layout.addView(tViewEvent);
                    layout.addView(tViewDate);
                }

                if(eventWithYearList.size() > 3){
                    TextView tViewMore = new TextView(context);
                    tViewMore.setTextColor(getResources().getColor(R.color.black));
                    tViewMore.setGravity(Gravity.RIGHT);
                    tViewMore.setMaxHeight(50);
                    tViewMore.setMaxWidth(300);
                    tViewMore.setTextSize(20);
                    tViewMore.setText("...");

                    layout.addView(tViewMore);
                }
                return v;
            }
            return null;

        }

        @Override
        public View getInfoWindow(Marker marker) {
//            View v = getLayoutInflater().inflate(R.layout.info_window_layout, null);
//            LinearLayout layout = (LinearLayout) v.findViewById(R.id.main_layout);
//            List<EventWithYear> eventWithYearList = new ArrayList<EventWithYear>();
//            if (clickedClusterItem != null) {
//                eventWithYearList = clickedClusterItem.getEventWithYears();
//
//                for (EventWithYear eventWithYear : eventWithYearList) {
//                    TextView tViewEvent = new TextView(context);
//                    TextView tViewDate = new TextView(context);
//                    tViewEvent.setTextColor(getResources().getColor(R.color.black));
//                    tViewDate.setTextColor(getResources().getColor(R.color.grey));
//                    tViewDate.setGravity(View.TEXT_ALIGNMENT_VIEW_END);
//                    tViewDate.setTextSize(11);
//                    tViewEvent.setMaxHeight(50);
//                    tViewEvent.setMaxWidth(300);
//                    tViewDate.setMaxHeight(50);
//                    tViewDate.setMaxWidth(300);
//                    tViewDate.setMaxWidth(300);
//                    tViewEvent.setText(eventWithYear.text);
//                    tViewDate.setText(Integer.toString(eventWithYear.year));
//
//                    layout.addView(tViewEvent);
//                    layout.addView(tViewDate);
//                }
//
//                return v;
//            }
            return null;
        }
    }



    protected void log(Level level, String method, String text)
    {
        logger.logp(level, "Chron", method, text);
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
