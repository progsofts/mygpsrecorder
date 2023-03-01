package com.progsoft.mygpsrecorder;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Environment;
import android.util.Log;

import androidx.annotation.Nullable;

public class DBHelper extends SQLiteOpenHelper {

    private String TAG = "GPS DBHelper";
    public DBHelper(@Nullable Context context, @Nullable String name, @Nullable SQLiteDatabase.CursorFactory factory, int version) {
        super(context, name, factory, version);
    }

    public DBHelper(Context context, int version) {
        super(context, Environment.getExternalStorageDirectory() + "/progsoft/gps_recorder.db", null ,version);
    }

    public DBHelper(Context context, String filename, int version) {
        super(context, filename, null,  version);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        Log.e(TAG, "Enter Create DBHelper...");
        String sql = "CREATE TABLE GPSINFO(_id integer primary key autoincrement, latitude________, longitude________ double, time varchar, type varchar, workmode verchar, state int)";
        db.execSQL(sql);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.e(TAG, "Enter Update DBHelper...");
        String sql;
        if (oldVersion == 2) {
            sql = "ALTER TABLE GPSINFO RENAME TO GPSINFO_";
            db.execSQL(sql);

            sql = "CREATE TABLE GPSINFO(_id integer primary key autoincrement, latitude________, longitude________ double, time varchar, type varchar, workmode verchar, state int)";
            db.execSQL(sql);

            sql = "INSERT INTO GPSINFO(latitude_______, longitude________, time, type, workmode, state) SELECT latitude, longitude, rectime, \"gps\", \"gps\", 0 FROM GPSINFO_";
            db.execSQL(sql);

            sql = "DROP TABLE GPSINFO_";
            db.execSQL(sql);
        }
        Log.e(TAG, "Update Version success.");
    }
}
