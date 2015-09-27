package com.chronicle.app;

import android.os.Parcel;
import android.os.Parcelable;
import com.google.android.gms.maps.model.LatLng;

import java.io.Serializable;

/**
 * Created by ִלטענטי on 13.09.2015.
 */
public class EventWithYear implements Serializable {
    int year;
    String text;

    public EventWithYear(String evnt){
        text = evnt;
    }


    public EventWithYear(String evnt, int yr){
        year = yr;
        text = evnt;
    }

//    @Override
//    public int describeContents() {
//        return 0;
//    }
//
//    @Override
//    public void writeToParcel(Parcel dest, int flags) {
//        dest.writeString(text);
//        dest.writeInt(year);
//    }
//
//    public static final Parcelable.Creator<EventWithYear> CREATOR = new Parcelable.Creator<EventWithYear>() {
//        public EventWithYear createFromParcel(Parcel in) {
//            int yearX = in.readInt();
//            String textX = in.readString();
//            EventWithYear eventWithYear = new EventWithYear(textX, yearX);
//            return eventWithYear;
//        }
//
//        public EventWithYear[] newArray(int size) {
//            return new EventWithYear[size];
//        }
//    };

}
