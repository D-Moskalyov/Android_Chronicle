package com.chronicle.app;

import android.os.Parcel;
import android.os.Parcelable;
import android.view.View;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.maps.android.clustering.ClusterItem;
import com.google.maps.android.clustering.ClusterManager;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.EventListener;
import java.util.List;

/**
 * Created by ִלטענטי on 13.09.2015.
 */
public class EventsMarker implements ClusterItem, Serializable {
    //private int mData;

    LatLng coordinate;

    public ArrayList<EventWithYear> getEventWithYears() {
        return eventWithYears;
    }

    ArrayList<EventWithYear> eventWithYears;
    int iconID;

//    @Override
//    public boolean onClusterItemClick(EventsMarker item) {
//        return false;
//    }
//
//    @Override
//    public void onClusterItemInfoWindowClick(EventsMarker item) {
//
//    }

    public LatLng getPosition() {
        return coordinate;
    }

    public EventsMarker(LatLng coord){
        eventWithYears = new ArrayList<EventWithYear>();
        coordinate = coord;
    }

    public EventsMarker(EventsMarker eventsMarker){
        eventWithYears = new ArrayList<EventWithYear>(eventsMarker.eventWithYears);
        coordinate = eventsMarker.coordinate;
    }

//    @Override
//    public View getInfoContents(Marker marker) {
//        View v = getLayoutInflater().inflate(R.layout.info_window_layout, null);
//    }
//
//    @Override
//    public View getInfoWindow(Marker marker) {
//        return null;
//    }


//    @Override
//    public int describeContents() {
//        return 0;
//    }
//
//    @Override
//    public void writeToParcel(Parcel dest, int flags) {
//        dest.writeArray(eventWithYears.toArray());
//    }
//
//    public static final Parcelable.Creator<EventsMarker> CREATOR = new Parcelable.Creator<EventsMarker>() {
//        public EventsMarker createFromParcel(Parcel in) {
//            return new EventsMarker(in);
//        }
//
//        public EventsMarker[] newArray(int size) {
//            return new EventsMarker[size];
//        }
//    };
//
//    private EventsMarker(Parcel in) {
//        eventWithYears = in.readArrayList(null);
//    }

    public void addEventWithYear(EventWithYear evWithYear){
        eventWithYears.add(evWithYear);
    }
}
