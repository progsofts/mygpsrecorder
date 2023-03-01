package com.progsoft.mygpsrecorder;

import static com.progsoft.mygpsrecorder.Constants.FileWrite;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Notification;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Environment;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;

import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import org.osmdroid.api.IMapController;
import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.cachemanager.CacheManager;
import org.osmdroid.tileprovider.modules.SqlTileWriter;
import org.osmdroid.tileprovider.modules.SqliteArchiveTileWriter;
import org.osmdroid.tileprovider.tilesource.OnlineTileSourceBase;
import org.osmdroid.tileprovider.tilesource.TileSourcePolicy;
import org.osmdroid.tileprovider.tilesource.XYTileSource;
import org.osmdroid.util.BoundingBox;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Polyline;
import org.osmdroid.views.overlay.ScaleBarOverlay;
import org.osmdroid.views.overlay.compass.CompassOverlay;
import org.osmdroid.views.overlay.compass.InternalCompassOrientationProvider;
import org.osmdroid.views.overlay.milestones.MilestoneDisplayer;
import org.osmdroid.views.overlay.milestones.MilestoneManager;
import org.osmdroid.views.overlay.milestones.MilestoneMeterDistanceLister;
import org.osmdroid.views.overlay.milestones.MilestoneMeterDistanceSliceLister;
import org.osmdroid.views.overlay.milestones.MilestonePathDisplayer;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {
    String TAG = "GPS Main";
    public static double nowLong = 0;
    public static double nowLat = 0;
    Notification.Builder mBuilder;
    int mNotificationId = 1;
    String channelId = "my_chn_01";
    TextView tv;

    Button startButton;
    Button stopButton;
    Button refeshButton;
    Button postButton;
    Button OnOffButton;
    EditText et;
    //HttpUtils httptest;
    //HttpThread httpd;

    private static final String[] REQUIRED_SDK_PERMISSIONS = new String[] {
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.WRITE_EXTERNAL_STORAGE};
    private final static int REQUEST_CODE_ASK_PERMISSIONS = 1;

    MapView mMapView = null;

    /*
    public boolean FileWrite(String context) {
        try {
            File file = new File(Environment.getExternalStorageDirectory(), "progsoft/Kalman.txt");
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

    public void getMap() {

        final OnlineTileSourceBase TileServer = new XYTileSource(
                "OpenCycleMap", 0, 19, 256, ".png",
                new String[] {
//                       "https://a.tile.openstreetmap.de/","https://b.tile.openstreetmap.de/","https://c.tile.openstreetmap.de/",
                        "http://a.tile.openstreetmap.org/","http://b.tile.openstreetmap.org/","http://c.tile.openstreetmap.org/",
                },
                "OpenStreetMap contributors",
                new TileSourcePolicy(10, TileSourcePolicy.FLAG_NO_PREVENTIVE | TileSourcePolicy.FLAG_USER_AGENT_MEANINGFUL | TileSourcePolicy.FLAG_USER_AGENT_NORMALIZED)
        );

        mMapView.setTileSource(TileServer);

        mMapView.setBuiltInZoomControls(true);
        mMapView.setMultiTouchControls(true);


        IMapController mapController = mMapView.getController();
        mapController.setZoom(14);

        final DBServer db = new DBServer();
        GeoPoint startPoint = db.testQuerynow(this);
        mapController.setCenter(startPoint);

        ScaleBarOverlay mScaleBarOverlay = new ScaleBarOverlay(mMapView);
        mMapView.getOverlayManager().add(mScaleBarOverlay);

        mScaleBarOverlay.setAlignBottom(true);
        mScaleBarOverlay.setLineWidth(2 * (getResources().getDisplayMetrics()).density);
        mScaleBarOverlay.setMaxLength(2f);
        mScaleBarOverlay.setScaleBarOffset(400, 300);
        mScaleBarOverlay.drawLatitudeScale(true);
        mScaleBarOverlay.drawLongitudeScale(true);
        mScaleBarOverlay.setTextSize(36);

        CompassOverlay mCompassOverlay = new CompassOverlay(MainActivity.this, new InternalCompassOrientationProvider(MainActivity.this), mMapView);
        mMapView.getOverlayManager().add(mCompassOverlay);
        mCompassOverlay.enableCompass();

        mMapView.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        tv.setText("Zoom:" + mMapView.getZoomLevelDouble());
    }

    private Paint getFillPaint(final int pColor) {
        final Paint paint = new Paint();
        paint.setColor(pColor);
        paint.setStyle(Paint.Style.FILL_AND_STROKE);
        return paint;
    }

    private Paint getTextPaint(final  int pColor) {
        final Paint paint = new Paint();
        paint.setColor(pColor);
        paint.setTextSize(28);
        paint.setAntiAlias(true);
        return paint;
    }

    private Paint getStrokePaint(final int pColor, float pWidth) {
        final Paint paint = new Paint();
        paint.setStrokeWidth(pWidth);
        paint.setStyle(Paint.Style.STROKE);
        paint.setAntiAlias(true);
        paint.setColor(pColor);
        paint.setStrokeCap(Paint.Cap.ROUND);
        return paint;
    }

    private MilestoneManager getHalfKilometerManager() {
        final Path arrowPath = new Path();
        arrowPath.moveTo(-10, -10);
        arrowPath.lineTo(10,0);
        arrowPath.lineTo(-10, 10);
        arrowPath.close();
        final Paint backgroundPaint = getFillPaint(Color.RED);
        return new MilestoneManager(
                new MilestoneMeterDistanceLister(500),
                new MilestonePathDisplayer(0, true, arrowPath, backgroundPaint) {
                    @Override
                    protected void draw(Canvas pCanvas, Object pParameter) {
                        //Log.e(TAG, "*******************param:" + (double)pParameter);
                        final int halfKilometers = (int)Math.round((double)pParameter/500);
                        if (halfKilometers % 2 == 0) {
                            return;
                        }
                        super.draw(pCanvas, pParameter);
                    }
                }
        );
    }

    private MilestoneManager getKilometerManager() {
        final float backgroundRadius = 30;
        final Paint backgroundPaint1 = getFillPaint(Color.RED);
        final Paint backgroundPaint2 = getFillPaint(Color.BLUE);
        final Paint textPaint1 = getTextPaint(Color.WHITE);
        final Paint textPaint2 = getTextPaint(Color.WHITE);
        final Paint borderPaint = getStrokePaint(Color.WHITE, 3);
        return new MilestoneManager(
                new MilestoneMeterDistanceLister(1000),
                new MilestoneDisplayer(0, false) {
                    @Override
                    protected void draw(Canvas pCanvas, Object pParameter) {
                        final double meters = (double)pParameter;
                        final int kilometers = (int)Math.round(meters / 1000);
                        final boolean checked = (kilometers % 10 > 0);
                        final Paint textPaint = checked ? textPaint2 : textPaint1;
                        final Paint backgroundPaint = checked ? backgroundPaint2 : backgroundPaint1;
                        final String text = "" + kilometers + "K";
                        final Rect rect = new Rect();
                        textPaint1.getTextBounds(text, 0, text.length(), rect);
                        pCanvas.drawCircle(0, 0, backgroundRadius, backgroundPaint);
                        pCanvas.drawText(text, -rect.left - rect.width() /2, rect.height() / 2 - rect.bottom, textPaint);
                        pCanvas.drawCircle(0, 0, backgroundRadius + 1, borderPaint);
                    }
                }
        );
    }

    public void setNowPosition() {
        final Polyline line = new Polyline(mMapView);
        final DBServer db = new DBServer();
        if (Constants.all) {
            db.testQuery(this, line, 0);
        } else {
            db.testQuery(this, line, 1);
        }

        if (line.getPoints().size() > 0) {
            nowLat = line.getPoints().get(line.getPoints().size()-1).getLatitude();
            nowLong = line.getPoints().get(line.getPoints().size()-1).getLongitude();
        }
        line.getPaint().setStrokeCap(Paint.Cap.ROUND);
        line.setColor(Color.BLUE);
        line.setWidth(12);

        final Bitmap bitmap = BitmapFactory.decodeResource(getResources(), org.osmdroid.library.R.drawable.next);
        final MilestoneMeterDistanceSliceLister slicerForPath = new MilestoneMeterDistanceSliceLister();
        final List<MilestoneManager> managers = new ArrayList<>();
        final MilestoneMeterDistanceSliceLister slicerForIcon = new MilestoneMeterDistanceSliceLister();
        managers.add(getHalfKilometerManager());
        managers.add(getKilometerManager());
        line.setMilestoneManagers(managers);
        line.setTitle("Location");
        line.setSubDescription("Location: null");

        Polyline.OnClickListener listener = new Polyline.OnClickListener() {
            @Override
            public boolean onClick(Polyline polyline, MapView mapView, GeoPoint eventPos) {
                polyline.setSubDescription(db.getGPSINFO(eventPos.getLatitude(), eventPos.getLongitude()) + "("
                + String.format(Locale.US, "%.5f", eventPos.getLatitude()) + "," + String.format(Locale.US, "%.5f", eventPos.getLongitude()) + ")");
                polyline.onClickDefault(polyline, mapView, eventPos);
                return false;
            }
        };
        line.setOnClickListener(listener);

        Log.e(TAG, "Overlay1" + mMapView.getOverlayManager().size());
        mMapView.getOverlayManager().add(line);

        ScaleBarOverlay mScaleBarOverlay = new ScaleBarOverlay(mMapView);
        mMapView.getOverlayManager().add(mScaleBarOverlay);

        mScaleBarOverlay.setCentred(true);
        mScaleBarOverlay.setAlignBottom(true);
        mScaleBarOverlay.setLineWidth(2 * (getResources().getDisplayMetrics()).density);
        mScaleBarOverlay.setMaxLength(2f);
        mScaleBarOverlay.setScaleBarOffset(50, 50);
        mScaleBarOverlay.drawLongitudeScale(true);
        mScaleBarOverlay.drawLatitudeScale(true);
        mScaleBarOverlay.setTextSize(36);

        CompassOverlay mCompassOverlay = new CompassOverlay(MainActivity.this, new InternalCompassOrientationProvider(MainActivity.this), mMapView);
        mMapView.getOverlayManager().add(mCompassOverlay);
        mCompassOverlay.enableCompass();

        GeoPoint startPoint = new GeoPoint(nowLat, nowLong);
        IMapController mapController = mMapView.getController();
        mapController.setCenter(startPoint);

        Marker marker = new Marker(mMapView);
        marker.setAlpha((float)0.5);
        marker.setIcon(getResources().getDrawable(R.mipmap.ic_launcher));
        marker.setPosition(startPoint);
        marker.setAnchor(0.5f, 0.5f);
        marker.setTitle("woshi Title");
        marker.setSubDescription("wo shi subscritpion");
        //mMapView.getOverlayManager().add(marker);

        Log.e(TAG, "Overlay2" + mMapView.getOverlayManager().size());
        tv.setText("GPS mode: " + Constants.flag + " " + Constants.isStop + " Zoom: " + mMapView.getZoomLevelDouble());

    }

    @SuppressLint("NewApi")
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Context ctx = getApplicationContext();
        //Configuration.getInstance().load(ctx, PreferenceManager.getDefaultSharedPreferences(ctx));

        final File osmdroidBasePath = new File("/sdcard/progsoft", "");       osmdroidBasePath.mkdirs();
        Configuration.getInstance().setOsmdroidBasePath(osmdroidBasePath);

        final File osmdroidTileCache = new File("/sdcard/progsoft", "tiles_org"); osmdroidBasePath.mkdirs();
        Configuration.getInstance().setOsmdroidTileCache(osmdroidTileCache);

        Configuration.getInstance().setUserAgentValue("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/93.0.4577.63 Safari/537.36 Edg/93.0.961.38");
        //Configuration.getInstance().getAdditionalHttpRequestProperties().put("cookie", "_osm_totp_token=818498");
        Log.e(TAG, Configuration.getInstance().getUserAgentHttpHeader());
        Configuration.getInstance().setDebugMapTileDownloader(true);
        Configuration.getInstance().setDebugMode(true);
        Configuration.getInstance().setTileFileSystemCacheMaxBytes(4224000000l);
        Configuration.getInstance().setTileFileSystemCacheTrimBytes(4096000000l); //1G存储
        Log.e(TAG, "size************" + Configuration.getInstance().getTileFileSystemCacheTrimBytes());

        setContentView(R.layout.activity_main);
        tv = findViewById(R.id.tv);
        startButton = findViewById(R.id.button);
        stopButton = findViewById(R.id.button2);
        refeshButton = findViewById(R.id.button3);
        OnOffButton = findViewById(R.id.button4);
        postButton = findViewById(R.id.button5);

        String[] permissions = {Manifest.permission.WRITE_EXTERNAL_STORAGE};
        requestPermissions(permissions, 1);

        Log.e(TAG, "Huang Year:" + Constants.year);
        Constants.year = 2020;
        Log.e(TAG, "Huang Year:" + Constants.year);

        SimpleDateFormat f = new SimpleDateFormat("yyyy-MM-dd HH:mm:00", Locale.US);
        Calendar c = Calendar.getInstance();
        c.setTime(new Date());
        c.add(Calendar.HOUR, -4);
        Constants.year_s = f.format(c.getTime());
        c.add(Calendar.HOUR, 5);
        Constants.year_e = f.format(c.getTime());

        /*
        Constants.year_s = "2021-09-11 18:00:00";
        Constants.year_e = "2021-09-11 19:10:00";
        */
        //checkPermissions();

        DBServer db = new DBServer();
        db.testCreateDB(this);
        db.testUpdateDB(this, true);

        mMapView = (MapView) findViewById(R.id.map);
        getMap();

        startButton.setOnClickListener(new View.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.O)
            @Override
            public void onClick(View v) {
                tv.setText("Start GPS Flag:" + Constants.flag);
                if (Constants.isStop) {
                    Constants.gpsInfoInMem.clear();
                    Constants.index = 0;
                    Constants.isStartedGPS = false;
                }
                Constants.isStop = false;
                Intent service = new Intent(MainActivity.this, MyService.class);
                startForegroundService(service);
                FileWrite("Start Service by button.");
            }
        });

        stopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent service = new Intent(MainActivity.this, MyService.class);
                Constants.isStop = true;
                stopService(service);
                if (Constants.flag == 5)
                    Constants.flag= 4;
                FileWrite("Stop Service by buttom.");
                tv.setText("Stopped GPS");
            }
        });

        refeshButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mMapView.getOverlayManager().clear();
                setNowPosition();
                FileWrite("ReDraw by button");
            }
        });

        postButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Constants.all = !Constants.all;
                if (Constants.all) {
                    refeshButton.setText("ALL");
                } else {
                    refeshButton.setText("FILTER");
                }
                FileWrite("All/Filter by button");
            }
        });

        OnOffButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //Intent intent = new Intent(MainActivity.this, MyService.class);
                //startActivityForResult(intent, 1);
                tv.setText("Stoped:" + Constants.isStop + " GPSStart:" + Constants.isStartedGPS + " " + Constants.year_s + " " + Constants.year_e);
                FileWrite("Settting by button");
            }
        });

    }

    public void downloadTiles() {
        //最小等级
        int zoommin = 0;
        int zoommax = 15;
        //                           2K  2K  宋  国  华南  省 湾区  市  港 大社区 半区 街道 街道2 多小区 小区1 小区2
        //                           2K  2K  2K  2K  2K  2K 10K 10K 10K  3K  3K  3K  7H
        //            0   1   2   3   4   5   6   7   8   9  10  11  12  13  14  15  16  17  18  19
        int zoom[] = {6,  6,  7,  7,  8,  8,  9, 10, 11, 12, 14, 15, 16, 16, 17, 18, 18, 18, 19, 19};
        //nesw 范围
        String outputName = Environment.getExternalStorageDirectory().getAbsolutePath()
                + File.separator + "progsoft" + File.separator + "tiles_org" + File.separator + "cache.db";
        /*
        if (mMapView.getZoomLevelDouble() < 11) {
            Toast.makeText(MainActivity.this, "范围太大，不能下载: >= 11 " + mMapView.getZoomLevel() , Toast.LENGTH_LONG).show();
            return;
        } else {
            zoommax = mMapView.getZoomLevel() + 4;
            if (zoommax > 16) zoommax = 16;
        };
         */
        zoommin = mMapView.getZoomLevel();
        zoommax = zoom[zoommin];
        try {
            BoundingBox ba = mMapView.getBoundingBox();
            Log.e(TAG, ba.getLatNorth() + " " + ba.getLonEast() + " " + ba.getLatSouth() + " " + ba.getLonWest());
            Toast.makeText(MainActivity.this, ba.getLatNorth() + " " + ba.getLonEast() + " " + ba.getLatSouth() + " " + ba.getLonWest(), Toast.LENGTH_LONG).show();
//            SqliteArchiveTileWriter writer = new SqliteArchiveTileWriter(outputName);
            SqlTileWriter writer = new SqlTileWriter();
            //BoundingBox bb = new BoundingBox(22.55, 113.96, 22.47, 113.86);
            //须要下载的数量
            CacheManager mgr = new CacheManager(mMapView, writer);
            int tilecount = mgr.possibleTilesInArea(ba, zoommin, zoommax);

            //this triggers the download
            mgr.downloadAreaAsyncNoUI(MainActivity.this, ba, zoommin, zoommax, new CacheManager.CacheManagerCallback() {
                @Override
                public void onTaskComplete() {
                    Toast.makeText(MainActivity.this, "Download complete!", Toast.LENGTH_LONG).show();
                    tv.setText("Download done success (" + tilecount + "/" + tilecount + ")");
                    if (writer!=null)
                        writer.onDetach();
                }

                @Override
                public void onTaskFailed(int errors) {
                    Toast.makeText(MainActivity.this, "Download complete with " + errors + " errors", Toast.LENGTH_LONG).show();
                    tv.setText("Download done with error:" + errors + " (" + (tilecount - errors) + "/" + tilecount + ")");
                    if (writer!=null)
                        writer.onDetach();
                }

                @Override
                public void updateProgress(int progress, int currentZoomLevel, int zoomMin, int zoomMax) {
                    //NOOP since we are using the build in UI
                    tv.setText("Download doing:" + progress + "/" + tilecount + " " +
                            currentZoomLevel + " (" + zoomMin + "," + zoomMax + ")");
                    /*
                    if ((progress / 10) % 10 == 0) {

                        Toast.makeText(MainActivity.this, "Download progress:" + progress + "/" + tilecount + " " +
                                currentZoomLevel + " (" + zoomMin + "," + zoomMax + ")", Toast.LENGTH_LONG).show();
                    }

                     */
                }

                @Override
                public void downloadStarted() {
                    //NOOP since we are using the build in UI
                    Toast.makeText(MainActivity.this, "Download Started!", Toast.LENGTH_SHORT).show();
                }

                @Override
                public void setPossibleTilesInArea(int total) {
                    //NOOP since we are using the build in UI
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void click(View v) {
        int id = v.getId();
        boolean isShow = false;
        DBServer db = new DBServer();
        Log.e(TAG, "Overlay3" + mMapView.getOverlayManager().size() + "," + Constants.mID);

        if (Constants.mID > 0) {
            mMapView.getOverlayManager().remove(mMapView.getOverlayManager().size() - 1);
        }

        switch (id) {
            case R.id.Frist:
                db.testRoll(this, -99);
                isShow = true;
                break;
            case R.id.Prev10:
                db.testRoll(this, -10);
                isShow = true;
                break;
            case R.id.Prev:
                db.testRoll(this, -1);
                isShow = true;
                break;
            case R.id.Next:
                db.testRoll(this, 1);
                isShow = true;
                break;
            case R.id.Next10:
                db.testRoll(this, 10);
                isShow = true;
                break;
            case R.id.Last:
                db.testRoll(this, 99);
                isShow = true;
                break;
            case R.id.Change:
                isShow = false;
                downloadTiles();

                /*
                DateFormat format1 = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
                Date now = new Date();
                Date years_date = now;
                Date yeare_date = now;

                try {
                    years_date = format1.parse(Constants.year_s);
                    yeare_date = format1.parse(Constants.year_e);
                } catch (ParseException e) {
                    e.printStackTrace();
                }

                if (now.getTime() <= years_date.getTime()) {
                    SimpleDateFormat f = new SimpleDateFormat("yyyy-MM-dd HH:mm:00", Locale.US);
                    Calendar c = Calendar.getInstance();
                    c.setTime(years_date);
                    c.add(Calendar.HOUR, -24 * 30);
                    Constants.year_s = f.format(c.getTime());
                    c.setTime(yeare_date);
                    c.add(Calendar.HOUR, -24 * 30);
                    Constants.year_e = f.format(c.getTime());
                } else {
                    SimpleDateFormat f = new SimpleDateFormat("yyyy-MM-dd HH:mm:00", Locale.US);
                    Calendar c = Calendar.getInstance();
                    c.setTime(years_date);
                    c.add(Calendar.HOUR, 24);
                    Constants.year_s = f.format(c.getTime());
                    c.setTime(yeare_date);
                    c.add(Calendar.HOUR, 24);
                    Constants.year_e = f.format(c.getTime());
                }

                 */
                break;
            default:
                break;
        }

        if (isShow && Constants.mID > 0 ) {
            tv.setText(Constants.mID + ":(" + String.format(Locale.US, "%.5f", Constants.dLat) + "," + String.format(Locale.US, "%.5f", Constants.dLong) + ") " + Constants.nowTime + "\n" + Constants.type + "," + Constants.workmode + "," + Constants.state);
            GeoPoint startpoint = new GeoPoint(Constants.dLat, Constants.dLong);
            IMapController mapController = mMapView.getController();
            mapController.setCenter(startpoint);

            Marker marker = new Marker(mMapView);
            marker.setAlpha((float)0.5);
            marker.setPosition(startpoint);
            //marker.setAnchor(0.5f, 0.68f);
            marker.setTitle("woshi Title");
            marker.setSubDescription("wo shi subscritpion");
            mMapView.getOverlayManager().add(marker);
        }
    }
}