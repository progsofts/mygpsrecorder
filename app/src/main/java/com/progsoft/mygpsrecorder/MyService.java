package com.progsoft.mygpsrecorder;

import static com.progsoft.mygpsrecorder.Constants.FileWrite;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.GpsSatellite;
import android.location.GpsStatus;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.Date;
import java.util.Iterator;

import static java.lang.System.currentTimeMillis;

import org.osmdroid.util.GeoPoint;

@RequiresApi(api = Build.VERSION_CODES.O)
public class MyService extends Service {
    private static Thread mThread = null;
    int mNotificationId = 1;
    int mGPSInfoId = 2;
    String channelId = "my_chn_01";

    Notification.Builder mBuilder;
    NotificationManager mNotificationManager;

    private static final String TAG = "GPS SERVER Huang";
    private LocationManager mLocationManager = null;
    private static final int LOCATION_INTERVAL = 10000;
    private static final float LOCATION_DISTANCE = 0;

    private static final int RUNNABLE_INTERVAL = 5000;
    private static final int DEFAULT_DELAY_TIME = 5;

    private Runnable runnable;
    private Handler handler;
    long mLTEStauts = 0;

    int num = 0;
    int func = 0;
    /*
    public boolean FileWrite(String context) {
        try {
            File file = new File(Environment.getExternalStorageDirectory(), "progsoft/gps.txt");
            BufferedWriter bw = new BufferedWriter(new FileWriter(file, true));
            Date now = new Date();
            bw.write(now + " , " + context);
            bw.newLine();
            bw.flush();
            bw.close();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

     */

    private class SatellitesListener implements GpsStatus.Listener {
        @Override
        public void onGpsStatusChanged(int event) {
            switch (event) {
                case GpsStatus.GPS_EVENT_SATELLITE_STATUS:
                    @SuppressLint("MissingPermission")
                    GpsStatus gpsStatus = mLocationManager.getGpsStatus(null);
                    int maxSatellites = gpsStatus.getMaxSatellites();
                    Iterator <GpsSatellite> iters = gpsStatus.getSatellites().iterator();
                    int count =0;
                    int total = 0;
                    while(iters.hasNext() && count <= maxSatellites) {
                        total++;
                        GpsSatellite s = iters.next();
                        if (s.getSnr() > 0) {
                            count++;
                        }
                    }
                    Date now = new Date();
                    mBuilder.setSmallIcon(R.drawable.ic_launcher_background)
                            .setContentText("Time: " + now)
                            .setContentTitle(Constants.flag + " Satellite:(" + count + "/" + total + "）")
                            .setOnlyAlertOnce(true);
                    Notification notification = mBuilder.build();
                    mNotificationManager.notify(mNotificationId, notification);
                    Log.d(TAG, "Satellite Found:(" + count + "," + total + ") Flag: " + Constants.flag);
                    if ((Constants.countSatellite != count) || (Constants.totalSatellite != total)) {
                        FileWrite("Satellite Found:(" + count + "," + total + ") Flag: " + Constants.flag);
                    }
                    Constants.totalSatellite = total;
                    Constants.countSatellite = count;
                    break;
                default:
                    break;
            }
        }
    }

    void startGPS(String type) {
        if (mLocationManager == null)
            return;
        if (Constants.flag == 5) {
            Log.e(TAG, "startGPS: " + type);
            FileWrite("startGPS: " + type);
            try {
                mLocationManager.requestLocationUpdates(
                        LocationManager.GPS_PROVIDER, LOCATION_INTERVAL, LOCATION_DISTANCE,
                        mLocationListeners[0]);
                mLocationManager.addGpsStatusListener(gpsStatusListener);
                Constants.flag = 4;
                gps_new = currentTimeMillis();
            } catch (java.lang.SecurityException ex) {
                Log.i(TAG, "fail to request location update, ignore", ex);
            } catch (IllegalArgumentException ex) {
                Log.d(TAG, "gps provider does not exist " + ex.getMessage());
            }
        }
    }

    void stopGPS(String type) {
        if (Constants.flag == 4) {
            if (mLocationManager == null)
                return;
            FileWrite("Satellite Found:(" + Constants.countSatellite + "," + Constants.totalSatellite + ")");
            Log.e(TAG, "stopGPS: " + type);
            FileWrite("stopGPS: " + type);
            try {
                mLocationManager.removeUpdates(mLocationListeners[0]);
                mLocationManager.removeGpsStatusListener(gpsStatusListener);
                Constants.flag = 5;
                Constants.countSatellite = -1;
                Constants.totalSatellite = 0;
            } catch (java.lang.SecurityException ex) {
                Log.i(TAG, "fail to request location update, ignore", ex);
            } catch (IllegalArgumentException ex) {
                Log.d(TAG, "gps provider does not exist " + ex.getMessage());
            }
        }
    }

    void restartNetwork(String type) {
        if (mLocationManager == null)
            return;

        if ((Constants.flag == 4) || (Constants.flag == 5)) {
            FileWrite("Satellite Found:(" + Constants.countSatellite + "," + Constants.totalSatellite + ")");
            Log.e(TAG, "restartNetwork: " + type);
            FileWrite("restartNetwork: " + type);
            try {
                mLocationManager.removeUpdates(mLocationListeners[1]);
                mLocationManager = null;
                initializeLocationManager();
                mLocationManager.requestLocationUpdates(
                        LocationManager.NETWORK_PROVIDER, LOCATION_INTERVAL, LOCATION_DISTANCE,
                        mLocationListeners[1]);
                mLocationManager.removeGpsStatusListener(gpsStatusListener);
                mLocationManager.addGpsStatusListener(gpsStatusListener);
                Constants.countSatellite = -1;
                Constants.totalSatellite = 0;
            } catch (java.lang.SecurityException ex) {
                Log.i(TAG, "fail to request location update, ignore", ex);
            } catch (IllegalArgumentException ex) {
                Log.d(TAG, "gps provider does not exist " + ex.getMessage());
            }
        }
    }

    public void checkProvider(String provider, double lat, double lon, long t) {

        GPSINFO gpsinfo = new GPSINFO();
        gpsinfo.setId(Constants.index);
        gpsinfo.setPosition(lat, lon);
        gpsinfo.setDate(new Date().toString());
        gpsinfo.setGpsTime(t);
        Constants.gpsInfoInMem.put("" + Constants.index, gpsinfo);
        Constants.end = Constants.index;
        Constants.index++;

        if (Constants.flag <= 3) //非自动无需评估
            return;

        if (Constants.index < 10) //采集的数据少于10条，没必要评估
            return;

        gpsinfo = Constants.gpsInfoInMem.get("" + Constants.begin);
        GeoPoint center = new GeoPoint(gpsinfo.getmLatitude(), gpsinfo.getmLongitude());
        GeoPoint newCenter;
        double dist = 100000.0; //以100km开始计算
        double rad = 1000000;
        boolean findNew = false;

        do {
            newCenter = center;
            findNew = false;
            for (int step = 0; step < 8; step++) {
                double max = 0;
                GeoPoint newpoint = center.destinationPoint(dist, 45 * step);
                for (long k = Constants.begin; k <= Constants.end; k++) {
                    gpsinfo = Constants.gpsInfoInMem.get("" + k);
                    GeoPoint testPoint = new GeoPoint(gpsinfo.getmLatitude(), gpsinfo.getmLongitude());
                    double distx = newpoint.distanceToAsDouble(testPoint);
                    if (distx > max) max = distx;
                }
                if (max < rad) {
                    rad = max;
                    newCenter = newpoint;
                    findNew = true;
                }
            }
            if (!findNew) {
                dist = dist / 2;
            }
            center = newCenter;
        } while (dist > 0.1); //评估中心距离大于0.1m 就还需要继续计算

        FileWrite("(" + Constants.begin + " " + Constants.end + ") " + Constants.isStartedGPS + " rad:" + rad + " <20mi*20 >65mi*30");
        Log.e(TAG, "(" + Constants.begin + " " + Constants.end + ") " + Constants.isStartedGPS + " rad:" + rad + " <20mi*20 >65mi*30");

        if (Constants.isStartedGPS) { //已经打开GPS，需要定位中心半径小于20m
            if (rad < 20) {
                stopGPS(Constants.begin + " " + Constants.end + " rad:" + rad);
                Constants.isStartedGPS = false;
                Constants.begin = Constants.index; //从下一个开始算， 开启GPS是大于某个数，所以不用管个数
            }
            if (Constants.end - Constants.begin >= 19) { //老化掉旧的数据
                Constants.begin = Constants.end - 19;
            }
        } else { //未打开GPS，需要判断中心半径大于50m
            if (rad > 65) {
                startGPS(Constants.begin + " " + Constants.end + " rad:" + rad);
                Constants.isStartedGPS = true;
                Constants.begin = Constants.end - 9; //下面判断关闭GPS，所以至少要判断10个点的数据
            }
            if (Constants.end - Constants.begin >= 29) { //老化掉旧的数据
                Constants.begin = Constants.end - 29;
                if (Constants.begin < 0) Constants.begin = 0;
            }
        }
    }

    public void getNewLocation(Location location, String s) {
        num++;
        double l1, l2;
        Date now = new Date();
        FileWrite("gpsrecorder " + Constants.index + " " + s + location.getProvider() + " " + num + ":" + location.getLatitude() + "," + location.getLatitude() + "," + location.getAltitude() + "," + location.getTime());
        String workmode = "";
        long state = 0;
        checkProvider(location.getProvider(), location.getLatitude(), location.getLongitude(), location.getTime());

        switch (Constants.flag) {
            case 1:
                workmode = "gps";
                break;
            case 2:
                workmode = "network";
                break;
            case 3:
                workmode = "all";
                if ("network".equals(location.getProvider())) {
                    if ((net_d - gps_d) < 20*1000L) {
                        state = 2;
                    }
                }
                break;
            case 4:
                workmode = "auto(a)";
                if ("network".equals(location.getProvider())) {
                    if ((net_d - gps_d) < 20*1000L) {
                        state = 2;
                    }
                }
                break;
            case 5:
                workmode = "auto(n)";
                if ("network".equals(location.getProvider())) {
                    state = 1;
                }
                break;
        }

        DBServer db = new DBServer();
        db.testInsert(getBaseContext(), location.getLatitude(), location.getLongitude(), location.getProvider(), workmode, state);

        mBuilder.setSmallIcon(R.drawable.ic_launcher_background)
                .setContentText("Type " + location.getProvider() + now)
                .setContentTitle(num+":" + location.getLatitude() +"," + location.getLongitude())
                .setOnlyAlertOnce(true);
        Notification notification = mBuilder.build();
        mNotificationManager.notify(mGPSInfoId, notification);
    }

    private class LocationListener implements android.location.LocationListener {
        Location mLastLocation;

        public LocationListener(String provider) {
            Log.e(TAG, "LocationListener " + provider);
            FileWrite("LocationListener " + provider);
            mLastLocation = new Location(provider);
        }

        @Override
        public void onLocationChanged(@NonNull Location location) {
            getNewLocation(location, "Changed ");
            Log.e(TAG, "onLocationChanged " + location);
            FileWrite("onLocationChanged " + location);
            mLastLocation.set(location);
        }

        @Override
        public void onProviderDisabled(@NonNull String provider) {
            Log.e(TAG, "onProviderDisabled " + provider);
            FileWrite("onProviderDisabled " + provider);
        }

        @Override
        public void onProviderEnabled(@NonNull String provider) {
            Log.e(TAG, "onProviderEnabled " + provider);
            FileWrite("onProviderEnabled " + provider);
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
            Log.e(TAG, "onStatusChanged " + provider);
            FileWrite("onStatusChanged " + provider);
        }
    }

    private LocationListener[] mLocationListeners = new LocationListener[] {
            new LocationListener(LocationManager.GPS_PROVIDER),
            new LocationListener(LocationManager.NETWORK_PROVIDER)
    };

    private SatellitesListener gpsStatusListener = new SatellitesListener();

    private static double gps_l1 = 0;
    private static double gps_l2 = 0;
    private static long gps_l3 = 0;
    private static long gps_l33 = 2;
    private static double net_l1 = 0;
    private static double net_l2 = 0;
    private static long net_l3 = 0;
    private static long net_l33 = 2;
    private static long gps_d = 0;
    private static long gps_new = 0;
    private static long net_d = 0;



    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        FileWrite("onStartCommand");
        Log.e(TAG, "onStartCommand");
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        FileWrite("onCreate");
        Log.e(TAG, "onCreate");
    /*
    MyRunnable runtimer = new MyRunnable();
    if (mThread = null) {
        mThread = new Thread(runtimer, "Timer Thread ");
        mThread.start();
        FileWrite("New Thread");
        Log.e(TAG, "New Thread");
    } else if (!mThread.isAlive()) {
        mThread = new Thread(runtimer, "Timer Thread ");
        mThread.start();
        FileWrite("ReNew Thread");
        Log.e(TAG, "ReNew Thread");
    } else {
        FileWrite("Thread Alive");
        Log.e(TAG, "Thread Alive");
    }
    */
        mNotificationManager =(NotificationManager)getSystemService(getApplicationContext().NOTIFICATION_SERVICE);
        if(Build.VERSION.SDK_INT >=Build.VERSION_CODES.O) {
            CharSequence name = "Offset";
            String description = "Command";
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel mChannel = new NotificationChannel(channelId, name, importance);

            mChannel.setDescription(description);
            mChannel.enableLights(true);

            mNotificationManager.createNotificationChannel(mChannel);
            mBuilder = new Notification.Builder(this, channelId);
        }

        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        Log.e(TAG,"huang: "+notificationIntent);
        mBuilder.setSmallIcon(R.drawable.ic_launcher_background)
                .setContentTitle("Inital " + mNotificationId).setContentText("Description notification")
                .setOnlyAlertOnce(true)
                .setContentIntent(pendingIntent);
        Notification notification = mBuilder.build();
        startForeground(mNotificationId, notification);
        mNotificationManager.notify(mNotificationId, notification);

        initializeLocationManager();

        if ((Constants.flag == 2) || (Constants.flag >= 3)) {
            try {
                mLocationManager.requestLocationUpdates(
                        LocationManager.NETWORK_PROVIDER, LOCATION_INTERVAL, LOCATION_DISTANCE,
                        mLocationListeners[1]);
            } catch (java.lang.SecurityException ex) {
                Log.i(TAG, "dkdk");
            } catch (IllegalArgumentException ex) {
                Log.d(TAG, "DDD");
            }
        }

        if ((Constants.flag == 1) || (Constants.flag == 3) || (Constants.flag == 4)) {
            try {
                Constants.isStartedGPS = true;
                mLocationManager.requestLocationUpdates(
                        LocationManager.GPS_PROVIDER, LOCATION_INTERVAL, LOCATION_DISTANCE,
                        mLocationListeners[0]);
                mLocationManager.addGpsStatusListener(gpsStatusListener);
            } catch (java.lang.SecurityException ex) {
                Log.i(TAG, "dkdk");
            } catch (IllegalArgumentException ex) {
                Log.d(TAG, "DDD");
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mLocationManager != null) {
            for (int i = 0; i < mLocationListeners.length; i++) {
                try {
                    mLocationManager.removeUpdates(mLocationListeners[i]);
                } catch (Exception ex) {
                    Log.i(TAG, "fail to remove location listener, ignore", ex);
                }
            }
            mLocationManager.removeGpsStatusListener(gpsStatusListener);
            Constants.countSatellite = -1;
            Constants.totalSatellite = 0;
        }
    }

    private void initializeLocationManager() {
        FileWrite("initializeLocationManager");
        Log.e(TAG, "initializeLocationManager");
        if (mLocationManager == null) {
            mLocationManager = (LocationManager) getApplicationContext().getSystemService(Context.LOCATION_SERVICE);
        }
    }
}
