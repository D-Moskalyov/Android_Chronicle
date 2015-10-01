package com.chronicle.app;

import android.util.Log;

import java.util.ArrayList;
import java.util.logging.Level;

/**
 * Created by ִלטענטי on 01.10.2015.
 */
public class EventWithLex {
    EventModel evntModel;
    ArrayList<String> lexemes;

    public EventWithLex(EventModel evModel, ArrayList<String> lxms){
        evntModel= evModel;
        lexemes = lxms;
        Log.i("EventWithLex", "EventWithLex -> EventWithLex success");
    }
}
