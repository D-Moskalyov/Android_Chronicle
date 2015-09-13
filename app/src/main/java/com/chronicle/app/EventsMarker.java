package com.chronicle.app;

import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.maps.android.clustering.ClusterItem;
import com.google.maps.android.clustering.ClusterManager;

import java.util.ArrayList;
import java.util.EventListener;
import java.util.List;

/**
 * Created by ִלטענטי on 13.09.2015.
 */
public class EventsMarker implements ClusterItem, ClusterManager.OnClusterItemClickListener<Coordinate>{

    LatLng coordinate;
    List<EventWithYear> eventWithYears;
    int iconID;

    @Override
    public boolean onClusterItemClick(Coordinate item) {
        return false;
    }

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

    public void addEventWithYear(EventWithYear evWithYear){
        eventWithYears.add(evWithYear);
    }
}
