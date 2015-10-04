package com.chronicle.app;

import android.content.Context;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;


/**
 * Created by ִלטענטי on 01.10.2015.
 */
public class DBHelper extends SQLiteOpenHelper {

    private static String DB_PATH = "/data/data/com.chronicle.app/databases/";

    private static String DB_NAME = "chronDB";

    //private SQLiteDatabase myDataBase;

    private final Context myContext;

    public DBHelper(Context context){
        super(context, DB_NAME, null, 1);
        this.myContext = context;
        Log.i("DBHelper", "DBHelper -> DBHelper success");
    }

    void createDataBase() throws IOException {

        boolean dbExist = checkDataBase();

        if(!dbExist){
            this.getReadableDatabase();
            copyDataBase();
            Log.i("createDataBase", "createDataBase success");
        }
    }

    private boolean checkDataBase(){

        SQLiteDatabase checkDB = null;
        try{
            String myPath = DB_PATH + DB_NAME;
            checkDB = SQLiteDatabase.openDatabase(myPath, null, SQLiteDatabase.OPEN_READONLY|SQLiteDatabase.NO_LOCALIZED_COLLATORS);
            Log.i("checkDataBase", "checkDataBase success");
        }catch(SQLiteException e){
            Log.i("checkDataBase", "checkDataBase SQLiteException");
        }

        if(checkDB != null){
            checkDB.close();
        }

        return checkDB != null ? true : false;
    }

    /**
     * Copies your database from your local assets-folder to the just created empty database in the
     * system folder, from where it can be accessed and handled.
     * This is done by transfering bytestream.
     * */
    private void copyDataBase() throws IOException{

        //Open your local db as the input stream
        InputStream myInput = myContext.getAssets().open(DB_NAME);

        // Path to the just created empty db
        String outFileName = DB_PATH + DB_NAME;

        //Open the empty db as the output stream
        OutputStream myOutput = new FileOutputStream(outFileName);

        //transfer bytes from the inputfile to the outputfile
        byte[] buffer = new byte[1024];
        int length;
        while ((length = myInput.read(buffer))>0){
            myOutput.write(buffer, 0, length);
        }

        //Close the streams
        myOutput.flush();
        myOutput.close();
        myInput.close();

    }
//
//    public void openDataBase() throws SQLException {
//        //Open the database
//        String myPath = DB_PATH + DB_NAME;
//        //myDataBase = SQLiteDatabase.openDatabase(myPath, null, SQLiteDatabase.OPEN_READONLY);
//        myDataBase = this.getWritableDatabase();
//    }

//    @Override
//    public synchronized void close() {
//        if(myDataBase != null)
//            myDataBase.close();
//
//        super.close();
//    }


    @Override
    public void onCreate(SQLiteDatabase db) {
        try {
            createDataBase();
            Log.i("createDataBase()", "DBHelper -> createDataBase() success");
        } catch (IOException e) {
            db.execSQL("create table Pages " +
                    "(year integer not null unique, " + "revisionID integer, " + "lastUpdate text)");

            db.execSQL("create table Events " +
                    "(year integer, " + "event text, " +
                    "latitude double, " + "longitude double)");
            Log.i("onCreate", "DBHelper -> onCreate success");
        }

    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.i("onUpgrade", "DBHelper -> onUpgrade success");
    }







//    private static String DB_PATH = "/data/data/com.chronicle.app/databases/";
//
//    private static String DB_NAME = "chronDB";
//
//    private SQLiteDatabase myDataBase;
//
//    private final Context myContext;
//
//    public DBHelper(Context context){
//        super(context, DB_NAME, null, 1);
//        this.myContext = context;
//        Log.i("DBHelper", "DBHelper -> DBHelper success");
//    }
//
//    @Override
//    public void onCreate(SQLiteDatabase db) {
//        db.execSQL("create table Pages " +
//                "(year integer not null unique, " + "revisionID integer, " + "lastUpdate text)");
//
//        db.execSQL("create table Events " +
//                "(year integer, " + "event text, " +
//                "latitude double, " + "longitude double)");
//        Log.i("onCreate", "DBHelper -> onCreate success");
//    }
//
//    @Override
//    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
//        Log.i("onUpgrade", "DBHelper -> onUpgrade success");
//    }
}