package com.chronicle.app;

import com.google.android.gms.maps.model.LatLng;

/**
 * Created by ִלטענטי on 13.09.2015.
 */
public class EventWithYear {
    int year;
    String text;

    public EventWithYear(String evnt){
        text = evnt;
    }


    public EventWithYear(String evnt, int yr){
        year = yr;
        text = evnt;
    }

}
