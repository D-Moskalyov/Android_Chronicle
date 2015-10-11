package com.chronicle.app;

import com.google.android.gms.drive.query.SortableField;
import com.google.android.gms.maps.model.LatLng;
import com.google.maps.android.clustering.ClusterItem;
import com.google.maps.android.clustering.ClusterManager;

import java.util.Comparator;

public class Coordinate implements ClusterItem, Comparable<Coordinate>, ClusterManager.OnClusterItemClickListener<Coordinate> {
    public LatLng getmPosition() {
        return mPosition;
    }

    private final LatLng mPosition;

    @Override
    public boolean onClusterItemClick(Coordinate item) {
        return false;
    }

    public Coordinate() {
        mPosition = null;
    }

    public Coordinate(double lat, double lng) {
        mPosition = new LatLng(lat, lng);
    }

    public Coordinate(LatLng latLng) {
        mPosition = latLng;
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
