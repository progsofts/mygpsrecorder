package com.progsoft.mygpsrecorder;


import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Build;
import android.util.Log;
import android.widget.Toast;

import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.overlay.Polyline;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;

class GPSINFO {
    private long mID;
    private double mLatitude;
    private double mLongitude;
    private String mDate;
    private long gpsTime;

    public GPSINFO() {
        super();
    }

    public void setId(Long id) {
        mID = id;
    }

    public void setPosition(double latitude, double longitude) {
        mLatitude = latitude;
        mLongitude = longitude;
    }

    public double getmLatitude() {
        return mLatitude;
    }

    public double getmLongitude() {
        return mLongitude;
    }

    public void setDate(String date) {
        mDate = date;
    }

    public String getDate() {
        return mDate;
    }

    public void setGpsTime(long t) {
        gpsTime = t;
    }

    public long getGpsTime() {
        return gpsTime;
    }

    public String toString() {
        return "ID: " + mID + " " + mDate + ":(" + mLatitude + "," + mLongitude + ")";
    }
}


public class DBServer {
    private static final String TAG = "GPS DBServer";
    private int DB_VERSION = 3;
    private String lat = "latitude________";
    private String lon = "longitude________";
    private String time = "time";
    private String type = "type";
    private String workmode = "workmode";
    private String state = "state";
    private static HashMap<String, GPSINFO> gpsinfos = null;

    public String getGPSINFO(Double l1, Double l2) {
        Log.e(TAG, "GPSinfo: " + gpsinfos);
        Log.e(TAG, "S:" + String.format("%.6f", l1) + String.format("%.6f", l2));
        GPSINFO gp = gpsinfos.get("Lat:" + String.format(Locale.US, "%.6f",l1) + " Lon:" + String.format(Locale.US, "%.6f", l2));
        if (gp == null) {
            return "";
        } else {
            return gp.getDate();
        }
    }

    public void testCreateDB(Context v) {
        gpsinfos = new HashMap<>();
        DBHelper dbHelper = new DBHelper(v, DB_VERSION);
        DBHelper dbOld = new DBHelper(v, "gps_recorder.db", 2);

        try {
            SQLiteDatabase database = dbHelper.getReadableDatabase();
            SQLiteDatabase db_old = dbHelper.getReadableDatabase();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void testUpdateDB(Context v, boolean flag) {
        if (flag) {
            return;
        }
        DBHelper dbHelper = new DBHelper(v, DB_VERSION);
        DBHelper dbOld = new DBHelper(v, "gps_recorder.db", 2);

        try {
            SQLiteDatabase database = dbHelper.getReadableDatabase();
            SQLiteDatabase db_old = dbHelper.getReadableDatabase();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void testInsert(Context v, Double l1, Double l2, String t, String w, Long s) {
        DBHelper dbHelper = new DBHelper(v, DB_VERSION);
        SQLiteDatabase database = dbHelper.getReadableDatabase();

        Date now = new Date();
        SimpleDateFormat f = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);
        ContentValues cv = new ContentValues();
        cv.put(lat, l1);
        cv.put(lon, l2);
        cv.put(time, f.format(now));
        cv.put(type, t);
        cv.put(workmode, w);
        cv.put(state, s);

        long count = database.insert("GPSINFO", null, cv);
        database.close();
        Log.d(TAG, "Inserted " + count + " line");
    }

    public void testUpdate(Context v) {
        if (Constants.mID == 0) {
            return;
        }

        DBHelper dbHelper = new DBHelper(v, DB_VERSION);
        SQLiteDatabase database = dbHelper.getReadableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(lat, Constants.dLat);
        cv.put(lon, Constants.dLong);
        cv.put(time, Constants.nowTime);
        cv.put(type, Constants.type);
        cv.put(workmode, Constants.workmode);
        if (Constants.state == 0) {
            Constants.state = 10;
        } else {
            Constants.state = -Constants.state;
        }

        if (Constants.state <= -10) {
            Constants.state = 0;
        }

        cv.put(state, Constants.state);
        database.update("GPSINFO", cv, "_id=?", new String[]{String.valueOf(Constants.mID)});
        database.close();
    }

    public GeoPoint testQuerynow(Context v) {
        DBHelper dbHelper = new DBHelper(v, DB_VERSION);
        SQLiteDatabase database = dbHelper.getReadableDatabase();

        Cursor cursor;
        cursor = database.query("GPSINFO", null, null, null, null, null, "time desc");
        if (cursor.getCount() > 0) {
            cursor.moveToFirst();
            double l1 = cursor.getDouble(1);
            double l2 = cursor.getDouble(2);
            GeoPoint startPoint = new GeoPoint(l1, l2);
            cursor.close();
            return startPoint;
        } else {
            GeoPoint startPoint = new GeoPoint(22.600, 114.040);
            cursor.close();
            return startPoint;
        }
    }

    public static long pingpong = 0;
    public void testQuery(Context v, Polyline line, long k) {
        DBHelper dbHelper = new DBHelper(v, DB_VERSION);
        SQLiteDatabase database = dbHelper.getReadableDatabase();

        String y1 = Constants.year_s;
        String y2 = Constants.year_e;

        Cursor cursor;
        pingpong = k;
        gpsinfos.clear();
        if (++pingpong % 2 == 0) {
            cursor = database.query("GPSINFO", null, "time >= ? and time <= ? and state <= 0", new String[]{y1, y2}, null, null, "time");
        } else {
            cursor = database.query("GPSINFO", null, "time >= ? and time <= ?", new String[]{y1, y2}, null, null, "time");
        }

        int count = cursor.getCount();
        Log.e(TAG, "Query db number is " + count + " " + y1 + " " + y2);
        while (cursor.moveToNext()) {
            Long id = cursor.getLong(0);
            double l1 = cursor.getDouble(1);
            double l2 = cursor.getDouble(2);
            String date = cursor.getString(3);
            GeoPoint gp = new GeoPoint(l1, l2);
            line.addPoint(gp);
            GPSINFO gpsinfo = new GPSINFO();
            gpsinfo.setId(id);
            gpsinfo.setPosition(l1,l2);
            gpsinfo.setDate(date);
            gpsinfos.put("Lat:" + String.format(Locale.US, "%.6f",l1) + " Lon:" + String.format(Locale.US, "%.6f", l2), gpsinfo);
            Log.w(TAG, gpsinfo.toString());
        }
        cursor.close();
        database.close();

        Toast.makeText(v, "Query result count = " + count , Toast.LENGTH_SHORT).show();
        Log.e(TAG, "gpsinfo: " + gpsinfos);
    }

    public void testQueryFilter(Context v, Polyline line, long k) {
        double DEG_TO_METER = 111225.0;

        DBHelper dbHelper = new DBHelper(v, DB_VERSION);
        SQLiteDatabase database = dbHelper.getReadableDatabase();

        String y1 = Constants.year_s;
        String y2 = Constants.year_e;

        Cursor cursor;
        pingpong = k;
        gpsinfos.clear();
        if (++pingpong % 2 == 0) {
            cursor = database.query("GPSINFO", null, "time >= ? and time <= ? and state <= 0", new String[]{y1, y2}, null, null, "time");
        } else {
            cursor = database.query("GPSINFO", null, "time >= ? and time <= ?", new String[]{y1, y2}, null, null, "time");
        }

        DateFormat format1 = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
        long nowdate = 0, nextdate = 0;
        double same1 = 0, same2 = 0;
        double now1 = 0, now2 = 0;

        if (cursor.getCount() > 0) {
            cursor.moveToFirst();
            now1 = cursor.getDouble(1);
            now2 = cursor.getDouble(2);
            try {
                nowdate = format1.parse(cursor.getString(3)).getTime();
            } catch (ParseException e) {
                e.printStackTrace();
            }
        }

        int count = cursor.getCount();
        Log.e(TAG, "Query db number is " + count + " " + y1 + " " + y2);
        int filter = 0;
        cursor.moveToFirst();
        while (cursor.moveToNext()) {
            Long id = cursor.getLong(0);
            double l1 = cursor.getDouble(1);
            double l2 = cursor.getDouble(2);
            String date = cursor.getString(3);
            try {
                nextdate = format1.parse(cursor.getString(3)).getTime();
            } catch (ParseException e) {
                e.printStackTrace();
            }

            double diff = Math.sqrt(Math.pow(now1 - l1, 2) + Math.pow(now2 - l2, 2)) * DEG_TO_METER;
            double speed = 3600 * diff / (nextdate - nowdate);
            if (((same1 == l1) && (same2 == l2)) || (diff < 1.0)) {
                Log.i(TAG, "(" + String.format(Locale.US, "%.6f", now1) + "," + String.format(Locale.US, "%.6f", now2) +
                        "),(" + String.format(Locale.US, "%.6f", l1) + "," + String.format(Locale.US, "%.6f", l2) +
                        "),s:m(" + String.format(Locale.US, "%.1f", speed) + ":" + String.format(Locale.US, "%.1f", diff) +
                        "):" + (nextdate - nowdate) / 1000  + " " + date);

                Constants.FileWrite("(" + String.format(Locale.US, "%.6f", now1) + "," + String.format(Locale.US, "%.6f", now2) +
                        ")####(" + String.format(Locale.US, "%.6f", l1) + "," + String.format(Locale.US, "%.6f", l2) +
                        "),s:m(" + String.format(Locale.US, "%.1f", speed) + ":" + String.format(Locale.US, "%.1f", diff) +
                        "):" + (nextdate - nowdate) / 1000  + " " + date);
                continue;
            }
            same1 = l1;
            same2 = l2;
            if (speed >= 100) {
                Log.e(TAG, "(" + String.format(Locale.US, "%.6f", now1) + "," + String.format(Locale.US, "%.6f", now2) +
                        "),(" + String.format(Locale.US, "%.6f", l1) + "," + String.format(Locale.US, "%.6f", l2) +
                        "),s:m(" + String.format(Locale.US, "%.1f", speed) + ":" + String.format(Locale.US, "%.1f", diff) +
                        "):" + (nextdate - nowdate) / 1000  + " " + date);
                Constants.FileWrite("(" + String.format(Locale.US, "%.6f", now1) + "," + String.format(Locale.US, "%.6f", now2) +
                        ")OOOO(" + String.format(Locale.US, "%.6f", l1) + "," + String.format(Locale.US, "%.6f", l2) +
                        "),s:m(" + String.format(Locale.US, "%.1f", speed) + ":" + String.format(Locale.US, "%.1f", diff) +
                        "):" + (nextdate - nowdate) / 1000  + " " + date);
            } else {
                filter++;
                GeoPoint gp = new GeoPoint(l1, l2);
                line.addPoint(gp);
                GPSINFO gpsinfo = new GPSINFO();
                gpsinfo.setId(id);
                gpsinfo.setPosition(l1, l2);
                gpsinfo.setDate(date);
                gpsinfos.put("Lat:" + String.format(Locale.US, "%.6f", l1) + " Lon:" + String.format(Locale.US, "%.6f", l2), gpsinfo);
                Log.w(TAG, "(" + String.format(Locale.US, "%.6f", now1) + "," + String.format(Locale.US, "%.6f", now2) +
                        "),(" + String.format(Locale.US, "%.6f", l1) + "," + String.format(Locale.US, "%.6f", l2) +
                        "),s:m(" + String.format(Locale.US, "%.1f", speed) + ":" + String.format(Locale.US, "%.1f", diff) +
                        "):" + (nextdate - nowdate) / 1000 + " " + date);
                Constants.FileWrite("(" + String.format(Locale.US, "%.6f", now1) + "," + String.format(Locale.US, "%.6f", now2) +
                        ")****(" + String.format(Locale.US, "%.6f", l1) + "," + String.format(Locale.US, "%.6f", l2) +
                        "),s:m(" + String.format(Locale.US, "%.1f", speed) + ":" + String.format(Locale.US, "%.1f", diff) +
                        "):" + (nextdate - nowdate) / 1000  + " " + date);
                //Log.w(TAG, gpsinfo.toString());
                now1 = l1;
                now2 = l2;
                nowdate = nextdate;
            }
        }
        cursor.close();
        database.close();

        Toast.makeText(v, "Query result " + filter + "/" + count , Toast.LENGTH_SHORT).show();
        Log.e(TAG, "gpsinfo: " + gpsinfos);
    }

    public void testQueryNewRecord(Context v, String sDate, StringBuffer str, long max) {
        DBHelper dbHelper = new DBHelper(v, DB_VERSION);
        SQLiteDatabase database = dbHelper.getReadableDatabase();

        Cursor cursor;
        cursor = database.query("GPSINFO", null, "time > ?", new String[]{sDate}, null ,null, "time");
        int count = cursor.getCount();
        while (cursor.moveToNext()) {
            String m = Build.BOARD;
            Long id = cursor.getLong(0);
            double l1 = cursor.getDouble(1);
            double l2 = cursor.getDouble(2);
            String date = cursor.getString(3);
            String type =cursor.getString(4);
            String workmode = cursor.getString(5);
            long state = cursor.getLong(6);

            if (max == 0) {
                break;
            }
            max--;

            if (str.length() > 0) {
                str.append(",");
            } else {
                str.append("[");
            }
            str.append("[" + id + String.format(Locale.US, "%.8f", l1) + "," + String.format(Locale.US, "%.8f", l2) + ",\"" + date
            + "\",\"" + m + "\", \"" + type + "\", \"" + workmode + "\", " + state + ", 0]");

        }
        str.append("]");

        cursor.close();
        database.close();

        Toast.makeText(v, sDate + "\n Query Insert count=" + count, Toast.LENGTH_SHORT).show();
    }


    public void setRecord(Cursor cursor) {
        Constants.mID = cursor.getLong(0);
        Constants.dLat = cursor.getDouble(1);
        Constants.dLong = cursor.getDouble(2);
        Constants.nowTime = cursor.getString(3);
        Constants.type = cursor.getString(4);
        Constants.workmode = cursor.getString(5);
        Constants.state = cursor.getLong(6);
    }

    public void testRoll(Context v, int next) {
        DBHelper dbHelper = new DBHelper(v, DB_VERSION);
        SQLiteDatabase database = dbHelper.getReadableDatabase();

        String y1 = Constants.year_s;
        String y2 = Constants.year_e;

        Cursor cursor;
        if (pingpong % 2 == 0) {
            cursor = database.query("GPSINFO", null, "time >= ? and time <= ? and state <= 0", new String[]{y1, y2}, null, null, "time");
        } else {
            cursor = database.query("GPSINFO", null, "time >= ? and time <= ?", new String[]{y1, y2}, null, null, "time");
        }

        if (cursor.getCount() == 0) {
            Constants.mID = 0;
            return;
        }

        if ((next == -99) || (Constants.mID == 0)) {
            cursor.moveToFirst();
            setRecord(cursor);
            return;
        }

        if (next == 99) {
            cursor.moveToLast();
            setRecord(cursor);
            return;
        }

        if (next > 0) {
            int loop = 1000000;
            long lastID = Constants.mID;
            cursor.moveToFirst();
            do {
                Long id = cursor.getLong(0);
                setRecord(cursor);
                if (lastID == id) {
                    lastID = -1;
                    loop = next;
                }
                loop -= 1;
            } while((cursor.moveToNext() && loop >= 0));
        } else {
            int loop = 1000000;
            long lastID = Constants.mID;
            cursor.moveToLast();
            do {
                Long id = cursor.getLong(0);
                setRecord(cursor);
                if (lastID == id) {
                    lastID = -1;
                    loop =-next;
                }
                loop -= 1;

            } while (cursor.moveToPrevious() && loop >= 0);
        }
    }






}
