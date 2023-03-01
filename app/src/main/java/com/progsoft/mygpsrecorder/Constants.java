package com.progsoft.mygpsrecorder;

import android.os.Environment;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;

public class Constants {
    public static long year = 0;
    public static String year_s = "2019-11-11 07:00:00";
    public static String year_e = "2019-11-11 22:00:00";
    public static int flag = 4; //auto gps & network
    public static boolean isStop = true;
    public static int countSatellite = -1;
    public static int totalSatellite = 0;

    public static boolean all = true;

    public static long mID = 0;
    public static double dLat = 0;
    public static double dLong = 0;
    public static String nowTime = "";
    public static String type = "";
    public static String workmode = "";
    public static long state = 0;

    public static long index = 0;
    public static boolean isStartedGPS = false;
    public static long begin = 0;
    public static long end = 0;

    public static HashMap<String, GPSINFO> gpsInfoInMem = new HashMap<>();

    public static boolean FileWrite(String context) {
        try {
            File file = new File(Environment.getExternalStorageDirectory(), "progsoft/log/Kalman " + year_s + ".txt");
            BufferedWriter bw = new BufferedWriter(new FileWriter(file, true));
            Date now = new Date();
            SimpleDateFormat f = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);
            bw.write(f.format(now) + " , " + context);
            bw.newLine();
            bw.flush();
            bw.close();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

}
