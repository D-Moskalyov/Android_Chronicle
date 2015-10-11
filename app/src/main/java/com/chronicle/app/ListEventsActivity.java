package com.chronicle.app;

import android.app.ActionBar;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;

/**
 * Created by ִלטענטי on 27.09.2015.
 */
public class ListEventsActivity extends Activity {
//
//    EventsMarker eventsMarker;
//
//    public ListEventsActivity(EventsMarker eventsMarker){
//        this.eventsMarker = eventsMarker;
//    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.list_layout);

        LinearLayout linearLayout = (LinearLayout) findViewById(R.id.linLayout);
        linearLayout.setBackgroundColor(Color.parseColor("#a8a8a8"));

        LayoutInflater layoutInflater = getLayoutInflater();

        Intent i = getIntent();
        ArrayList<EventWithYear> myParcelableObjects = (ArrayList<EventWithYear>) i.getSerializableExtra("eventWithYearList");

        for(EventWithYear eventWithYear : myParcelableObjects){
            View view = layoutInflater.inflate(R.layout.list_item_layout, linearLayout, false);
            TextView textViewEvent = (TextView) view.findViewById(R.id.event);
            TextView textViewYear = (TextView) view.findViewById(R.id.date);
            textViewEvent.setTextColor(Color.parseColor("#000000"));
            textViewYear.setTextColor(Color.parseColor("#000000"));
            //textViewYear.setGravity(Gravity.RIGHT);
            textViewYear.setTextSize(18);
            textViewEvent.setTextSize(22);
            textViewYear.setBackgroundColor(Color.parseColor("#d3d3d3"));
            textViewEvent.setText(eventWithYear.text);
            textViewYear.setText(String.valueOf(eventWithYear.year));
            view.getLayoutParams().width = LinearLayout.LayoutParams.MATCH_PARENT;
            linearLayout.addView(view);
        }

        //MainActivity mainActivity = (MainActivity) getParent();
        //ArrayList<EventWithYear> eventWithYearList = (ArrayList)mainActivity.eventWithYearList;
        //ArrayList<EventWithYear> myParcelableObjects = i.getParcelableArrayListExtra("eventWithYearList");
        //EventWithYear[] myParcelableObjectsArray = (EventWithYear[]) i.getParcelableArrayExtra("eventWithYearArray");
        //EventWithYear myParcelableObject = (EventWithYear) i.getParcelableExtra("eventWithYear");
        //Log.d("Parcelable: ", myParcelableObjects.get(0).text);
    }
}
