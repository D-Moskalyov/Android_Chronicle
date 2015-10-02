package com.chronicle.app;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.*;
import android.os.Bundle;
import android.widget.*;
import com.google.android.gms.maps.*;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.maps.android.clustering.Cluster;
import com.google.maps.android.clustering.ClusterManager;
import com.google.maps.android.clustering.view.DefaultClusterRenderer;

import java.util.*;


public class MainActivity extends FragmentActivity implements OnMapReadyCallback {

    MapFragment mapFragment = null;
    GoogleMap map = null;

    SharedPreferences settings;
    private static final int SHOW_PREFERENCES = 1;
    Button mainButton;


    int globalYearStart;
    int globalYearFinish;

    DBHelper dbHelper;
    SQLiteDatabase db;

    private int firstCenturyAC;

    int innerYearStart;
    int innerYearfinish;

    String textBtn;

    LocationManager locationManager;
    ClusterManager<EventsMarker> mClusterManager;

    ArrayList<EventWithYear> eventWithYearList;

    InitAsync initAsync;

    boolean isConnected;
    //boolean justRotate;

    Context context;
    EventsMarker clickedClusterItem;
    ConnectivityManager cm;
    NetworkInfo activeNetwork;

    String LOG_TAG = "INF";


    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        context = getApplicationContext();

        initAsync = (InitAsync) getLastCustomNonConfigurationInstance();
        if(initAsync == null) {
            initAsync = new InitAsync();
            //initAsync.execute();
        }
        if(initAsync.getStatus() == AsyncTask.Status.FINISHED)
            initAsync = new InitAsync();
        initAsync.link(this);

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

        locationManager = (LocationManager)getSystemService(LOCATION_SERVICE);

        mainButton = (Button) this.findViewById(R.id.settingsButton);

        if(savedInstanceState != null) {
            innerYearStart = savedInstanceState.getInt("innerYearStart", 0);
            innerYearfinish = savedInstanceState.getInt("innerYearfinish", 0);
        }

        cm = (ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);
        activeNetwork = cm.getActiveNetworkInfo();

        dbHelper = new DBHelper(context);

        InitClusterer();

        Log.i("onCreate", "onCreate success");
    }

    @Override
    protected void onResume() {
        super.onResume();

        SetStartFinishYear(settings);

        textBtn = "";
        if (globalYearStart < 0) textBtn += globalYearStart * -1 + "бя - ";
        else textBtn += globalYearStart + " - ";
        if (globalYearFinish < 0) textBtn += globalYearFinish * -1 + "бя";
        else textBtn += globalYearFinish;

        if (globalYearFinish == innerYearfinish && globalYearStart == innerYearStart){
            if(initAsync.getStatus() == AsyncTask.Status.RUNNING)
                mainButton.setText("UPDATING");
            else
                mainButton.setText(textBtn);
            MakeMarkers();
            mClusterManager.cluster();

            return;
        }
        //justRotate = true;

        innerYearStart = globalYearStart;
        innerYearfinish = globalYearFinish;

        isConnected = activeNetwork != null && activeNetwork.isConnectedOrConnecting();

        if(isConnected) {
            MakeMarkers();
            mClusterManager.cluster();
            if (initAsync.getStatus() != AsyncTask.Status.RUNNING) {
                if (initAsync.getStatus() == AsyncTask.Status.FINISHED){
                    initAsync.unLink();
                    initAsync = new InitAsync();
                    initAsync.link(this);
                }
                mainButton.setText("UPDATING");
                initAsync.execute();
            }
            else {
                initAsync.cancel(false);
                mainButton.setText("STOPPING");
            }
        }
        else {

            mainButton = (Button) this.findViewById(R.id.settingsButton);
            mainButton.setText(textBtn);

            Toast.makeText(this, "Something wrong", Toast.LENGTH_LONG);
            MakeMarkers();
            mClusterManager.cluster();
        }

        Log.i("onResume", "onResume success");
    }


    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        Log.i("onWindowFocusChanged", "onWindowFocusChanged success");
    }

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        Log.i("onAttachedToWindow", "onAttachedToWindow success");
    }

    @Override
    protected void onPostResume() {
        super.onPostResume();
        //Log.i("onPostResume", "onPostResume");
        Log.i("onPostResume", "onPostResume success");
    }

    @Override
    protected void onPause() {
        super.onPause();
        locationManager.removeUpdates(locationListener);
        Log.i("onPause", "onPause success");
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.i("onStart", "onStart success");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.i("onDestroy", "onDestroy success");
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        Log.i("onRestart", "onRestart success");
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.i("onStop", "onStop success");
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        //super.onSaveInstanceState(outState);
        outState.putInt("innerYearStart", innerYearStart);
        outState.putInt("innerYearfinish", innerYearfinish);
    }

    @Override
    public void onMapReady(GoogleMap map) {

        Log.i("onMapReady", "onMapReady success");
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode == SHOW_PREFERENCES) {
            //justRotate = false;
            super.onActivityResult(requestCode, resultCode, data);
        }
        Log.i("onActivityResult", "onActivityResult success");
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
        Log.i("onClickPreferences", "onClickPreferences success");
    }

    @Override
    public Object onRetainCustomNonConfigurationInstance() {

        initAsync.unLink();
        Log.i("onRetainCustomNonC...", "onRetainCustomNonC... success");
        return initAsync;
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
        Log.i("SetStartFinishYear", "SetStartFinishYear success");
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
        Log.i("InitClusterer", "InitClusterer success");
    }

    protected void RestartAsync(){
        initAsync.unLink();
        initAsync = new InitAsync();
        initAsync.link(this);
        mainButton.setText("UPDATING");
        initAsync.execute();
    }




    private LocationListener locationListener = new LocationListener() {
        @Override
        public void onLocationChanged(Location location) {
            ShowLocation(location);
            Log.i("onLocationChanged", "onLocationChanged success");
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
            if(provider.equals(locationManager.GPS_PROVIDER)){

            }
            else if(provider.equals(locationManager.NETWORK_PROVIDER)){

            }
            Log.i("onStatusChanged", "onStatusChanged success");
        }

        @Override
        public void onProviderEnabled(String provider) {
            CheckEnable();
            ShowLocation(locationManager.getLastKnownLocation(provider));
            Log.i("onProviderEnabled", "onProviderEnabled success");
        }

        @Override
        public void onProviderDisabled(String provider) {
            CheckEnable();
            Log.i("onProviderDisabled", "onProviderDisabled success");
        }
    };

    private void ShowLocation(Location location){
        if(location == null)
            return;
        if(location.getProvider().equals(locationManager.GPS_PROVIDER)){

        }
        else if(location.getProvider().equals(locationManager.NETWORK_PROVIDER)){

        }
        Log.i("ShowLocation", "ShowLocation success");
    }

    private void CheckEnable(){
        Log.i("CheckEnable", "CheckEnable success");
    }




    protected void MakeMarkers() {
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

            }
        }

        mClusterManager.addItems(eventsMarkers);
        Log.i("MakeMarkers", "MakeMarkers success");
    }




    private class EventRenderer extends DefaultClusterRenderer<EventsMarker> {
        private final ImageView mImageView;
        private final int mDimension;

        public EventRenderer() {
            super(getApplicationContext(), map, mClusterManager);

            mImageView = new ImageView(getApplicationContext());
            mDimension = (int) getResources().getDimension(R.dimen.custom_profile_image);
            mImageView.setLayoutParams(new ViewGroup.LayoutParams(mDimension, mDimension));
            Log.i("EventRenderer", "EventRenderer -> EventRenderer success");
        }

        @Override
        protected void onBeforeClusterItemRendered(EventsMarker item, MarkerOptions markerOptions) {
            mImageView.setImageResource(item.iconID);
            markerOptions.icon(BitmapDescriptorFactory.fromResource(item.iconID))
                    .title(item.eventWithYears.get(0).text)
                    .snippet(Integer.toString(item.eventWithYears.get(0).year));
            Log.i("onBeforeClusterItemR...", "EventRenderer -> onBeforeClusterItemR...");
        }

        @Override
        protected boolean shouldRenderAsCluster(Cluster cluster) {
            // Always render clusters.
            Log.i("shouldRenderAsCluster", "EventRenderer -> shouldRenderAsCluster success");
            return cluster.getSize() > 1;
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
            return null;
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
