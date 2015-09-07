package com.chronicle.app;

import com.google.android.gms.drive.query.SortableField;
import com.google.android.gms.maps.model.LatLng;
import com.google.maps.android.clustering.ClusterItem;

import java.util.Comparator;

/**
 * Created by ִלטענטי on 03.09.2015.
 */
public class Coordinate implements ClusterItem, Comparable<Coordinate> {
    private final LatLng mPosition;


    public Coordinate() {
        mPosition = null;
    }

    public Coordinate(double lat, double lng) {
        mPosition = new LatLng(lat, lng);
    }

    @Override
    public LatLng getPosition() {
        return mPosition;
    }

    @Override
    public int compareTo(Coordinate another) {
        if(mPosition.latitude > another.mPosition.latitude)
            return 1;
        if(mPosition.latitude < another.mPosition.latitude)
            return -1;
        return 0;
    }

}
