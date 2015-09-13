package com.chronicle.app;

import com.google.android.gms.maps.model.LatLng;

import java.util.Comparator;

/**
 * Created by Дмитрий on 19.08.2015.
 */
public class EventModel implements Comparable<EventModel>, Comparator<EventModel> {
    int year;
    String text;
    Coordinate coord;

    public EventModel(String evnt){
        text = evnt;
    }


    public EventModel(String evnt, int yr){
        year = yr;
        text = evnt;
    }
    public EventModel(String evnt, LatLng latLng){
        text = evnt;
        coord = new Coordinate(latLng.latitude, latLng.longitude);
    }

    public void SetCoord(LatLng latLng){
        coord = new Coordinate(latLng.latitude, latLng.longitude);
    }

    @Override
    public int compareTo(EventModel another) {
        if (another.coord == null) {
            if (this.coord != null)
                return 1;
            if (this.year != another.year)
                return this.year - another.year;
            return this.text.compareTo(another.text);
        }

        if (this.coord == null)
            return -1;
        if (this.coord != another.coord)
            return this.coord.compareTo(another.coord);
        if (this.year != another.year)
            return this.year - another.year;
        return this.text.compareTo(another.text);

    }

    @Override
    public int compare(EventModel lhs, EventModel rhs) {//для сравнения данніх из БД и Parse
        if(lhs.year == rhs.year & lhs.text == rhs.text)
            return 0;
        return 1;
    }
}
