package com.chronicle.app;

import android.database.sqlite.SQLiteDatabase;

/**
 * Created by ִלטענטי on 04.10.2015.
 */
public class RetainObj {
    InitAsync initAsync;
    DBHelper dbHelper;
    SQLiteDatabase db;

    public RetainObj(InitAsync i, DBHelper d, SQLiteDatabase s){
        initAsync = i;
        dbHelper = d;
        db = s;
    }
}
