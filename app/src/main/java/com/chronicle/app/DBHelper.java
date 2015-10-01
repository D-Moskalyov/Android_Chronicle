package com.chronicle.app;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;


/**
 * Created by ִלטענטי on 01.10.2015.
 */
public class DBHelper extends SQLiteOpenHelper {

    public DBHelper(Context context){
        super(context, "chronDB", null, 1);
        Log.i("DBHelper", "DBHelper -> DBHelper success");
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("create table Pages " +
                "(year integer not null unique, " + "revisionID integer, " + "lastUpdate text)");

        db.execSQL("create table Events " +
                "(year integer, " + "event text, " +
                "latitude double, " + "longitude double)");
        Log.i("onCreate", "DBHelper -> onCreate success");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.i("onUpgrade", "DBHelper -> onUpgrade success");
    }
}