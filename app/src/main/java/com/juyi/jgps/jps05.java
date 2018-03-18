/*
 * Copyright (C) 2012 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.juyi.jgps;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.OnMyLocationButtonClickListener;
import com.google.android.gms.maps.GoogleMap.OnMyLocationClickListener;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.UUID;

/**
 * This demo shows how GMS Location can be used to check for changes to the users location.  The
 * "My Location" button uses GMS Location to set the blue dot representing the users location.
 * Permission for {@link Manifest.permission#ACCESS_FINE_LOCATION} is requested at run
 * time. If the permission has not been granted, the Activity is finished with an error message.
 */
public class jps05 extends AppCompatActivity
        implements
        OnMyLocationButtonClickListener,
        OnMyLocationClickListener,
        OnMapReadyCallback,
        ActivityCompat.OnRequestPermissionsResultCallback {

    /**
     * Request code for location permission request.
     *
     * @see #onRequestPermissionsResult(int, String[], int[])
     */
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;
    private static final int REQUEST_PERMISSIONS_REQUEST_CODE = 34;
    private boolean upload_gps_enable_flag = false;

    /**
     * Flag indicating whether a requested permission has been denied after returning in
     * {@link #onRequestPermissionsResult(int, String[], int[])}.
     */
    private boolean mPermissionDenied = false;

    private GoogleMap mMap;

    //((TextView) (findViewById(R.id.title))).setText(titleId);
    private TextView tv_top;
    private ImageButton btn_upload;
    private ImageButton btn_stop;
    //
    private FusedLocationProviderClient mFusedLocationClient;

    /**
     * Represents a geographical location.
     */
    protected Location mLastLocation;

    private String mLatitudeLabel;
    private String mLongitudeLabel;
    private String mAccuracyLabel;
    private String mAltitudeLabel;
    private String mBearingLabel;
    private String mSpeedLabel;

    private TextView mLatitudeText;
    private TextView mLongitudeText;
    private TextView mAccuracyText;
    private TextView mAltitudeText;
    private TextView mBearingText;
    private TextView mSpeedText;

    private String uniqueID;

    private String mLatitude_out;
    private String mLongitude_out;
    private String mAccuracy_out;
    private String mAltitude_out;
    private String mBearing_out;
    private String mSpeed_out;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON, WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        //setContentView(R.layout.my_location);
        setContentView(R.layout.jgps_layout_03);

        SupportMapFragment mapFragment =
                (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        tv_top = (TextView) findViewById(R.id.tv_info);
        btn_upload = (ImageButton) findViewById(R.id.btn_upload);
        btn_stop = (ImageButton) findViewById(R.id.btn_stop);

        btn_upload.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // Code here executes on main thread after user presses button
                upload_gps_enable_flag = true;
                tv_top.setText("Uploading GPS records ~");
                GPSupdate();
         }
        });

        btn_stop.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // Code here executes on main thread after user presses button
                upload_gps_enable_flag = false;
                tv_top.setText("Current GPS location:");
                handler_remove();
            }
        });

        mLatitudeLabel = getResources().getString(R.string.latitude_label);
        mLongitudeLabel = getResources().getString(R.string.longitude_label);
        mLatitudeText = (TextView) findViewById((R.id.latitude_text));
        mLongitudeText = (TextView) findViewById((R.id.longitude_text));

        mAccuracyLabel = getResources().getString(R.string.accuracy_label);
        mAltitudeLabel = getResources().getString(R.string.altitude_label);
        mBearingLabel = getResources().getString(R.string.bearing_label);
        mAccuracyText = (TextView) findViewById((R.id.accuracy_text));
        mAltitudeText = (TextView) findViewById((R.id.altitude_text));
        mBearingText = (TextView) findViewById((R.id.bearing_text));

        mSpeedLabel = getResources().getString(R.string.speed_label);
        mSpeedText = (TextView) findViewById((R.id.speed_text));

        uniqueID = UUID.randomUUID().toString();
        //tv_top.setText(uniqueID);

    }

    //
    private Runnable mutiThread = new Runnable(){
        public void run(){
            // 運行網路連線的程式
            HttpPostURL();
            //GPSupdate();
        }
    };

    @Override
    public void onStart() {
        super.onStart();

        if (!checkPermissions()) {
            requestPermissions();
        } else {
            getLastLocation();
        }
    }

    /**
     * Provides a simple way of getting a device's location and is well suited for
     * applications that do not require a fine-grained location and that do not need location
     * updates. Gets the best and most recent location currently available, which may be null
     * in rare cases when a location is not available.
     * <p>
     * Note: this method should be called after location permission has been granted.
     */
    @SuppressWarnings("MissingPermission")
    private void getLastLocation() {
        //Toast.makeText(getApplicationContext(),"getLastLocation()",Toast.LENGTH_SHORT).show();
        mFusedLocationClient.getLastLocation()
                .addOnCompleteListener(this, new OnCompleteListener<Location>() {
                    @Override
                    public void onComplete(@NonNull Task<Location> task) {
                        if (task.isSuccessful() && task.getResult() != null) {
                            mLastLocation = task.getResult();

                            mLatitudeText.setText(String.format(Locale.ENGLISH, "%s: %f",
                                    mLatitudeLabel,
                                    mLastLocation.getLatitude()));
                            mLongitudeText.setText(String.format(Locale.ENGLISH, "%s: %f",
                                    mLongitudeLabel,
                                    mLastLocation.getLongitude()));
                            //
                            mAccuracyText.setText(String.format(Locale.ENGLISH, "%s: %f",
                                    mAccuracyLabel,
                                    mLastLocation.getAccuracy()));
                            mAltitudeText.setText(String.format(Locale.ENGLISH, "%s: %f",
                                    mAltitudeLabel,
                                    mLastLocation.getAltitude()));
                            mSpeedText.setText(String.format(Locale.ENGLISH, "%s: %f",
                                    mSpeedLabel,
                                    mLastLocation.getSpeed()));
                            mBearingText.setText(String.format(Locale.ENGLISH, "%s: %f",
                                    mBearingLabel,
                                    mLastLocation.getBearing()));
                            //
                            mLatitude_out = String.format(Locale.ENGLISH, "%s", mLastLocation.getLatitude());
                            mLongitude_out = String.format(Locale.ENGLISH, "%s",mLastLocation.getLongitude());;
                            mAccuracy_out = String.format(Locale.ENGLISH, "%s",mLastLocation.getAccuracy());
                            mAltitude_out = String.format(Locale.ENGLISH, "%s",mLastLocation.getAltitude());
                            mBearing_out = String.format(Locale.ENGLISH, "%s",mLastLocation.getBearing());
                            mSpeed_out = String.format(Locale.ENGLISH, "%s",mLastLocation.getSpeed());
                        } else {
                            //Log.w(TAG, "getLastLocation:exception", task.getException());
                            //showSnackbar(getString(R.string.no_location_detected));
                        }
                    }
                });
    }

    /**
     * Return the current state of the permissions needed.
     */
    private boolean checkPermissions() {
        int permissionState = ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_COARSE_LOCATION);
        return permissionState == PackageManager.PERMISSION_GRANTED;
    }

    private void startLocationPermissionRequest() {
        ActivityCompat.requestPermissions(jps05.this,
                new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                REQUEST_PERMISSIONS_REQUEST_CODE);
    }

    private void requestPermissions() {
        boolean shouldProvideRationale =
                ActivityCompat.shouldShowRequestPermissionRationale(this,
                        Manifest.permission.ACCESS_COARSE_LOCATION);

        // Provide an additional rationale to the user. This would happen if the user denied the
        // request previously, but didn't check the "Don't ask again" checkbox.
        if (shouldProvideRationale) {
            /*
            Log.i(TAG, "Displaying permission rationale to provide additional context.");

            showSnackbar(R.string.permission_rationale, android.R.string.ok,
                    new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            // Request permission
                            startLocationPermissionRequest();
                        }
                    });
            */
        } else {
            //Log.i(TAG, "Requesting permission");
            // Request permission. It's possible this can be auto answered if device policy
            // sets the permission in a given state or the user denied the permission
            // previously and checked "Never ask again".
            startLocationPermissionRequest();
        }
    }

    @Override
    public void onMapReady(GoogleMap map) {
        mMap = map;

        mMap.setOnMyLocationButtonClickListener(this);
        mMap.setOnMyLocationClickListener(this);
        enableMyLocation();
    }

    /**
     * Enables the My Location layer if the fine location permission has been granted.
     */
    private void enableMyLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            // Permission to access the location is missing.
            PermissionUtils.requestPermission(this, LOCATION_PERMISSION_REQUEST_CODE,
                    Manifest.permission.ACCESS_FINE_LOCATION, true);
        } else if (mMap != null) {
            // Access to the location has been granted to the app.
            mMap.setMyLocationEnabled(true);
        }
    }

    @Override
    public boolean onMyLocationButtonClick() {
        Toast.makeText(this, "MyLocation button clicked", Toast.LENGTH_SHORT).show();
        // Return false so that we don't consume the event and the default behavior still occurs
        // (the camera animates to the user's current position).

        getLastLocation();

        //update location to web server by POST

        Thread thread = new Thread(mutiThread);
        thread.start();

        /*
        if (!upload_gps_enable_flag) {
            upload_gps_enable_flag = true;
            GPSupdate();
        } else {
            upload_gps_enable_flag = false;
            handler_remove();
        }
        */

        return false;
    }

    @Override
    public void onMyLocationClick(@NonNull Location location) {
        Toast.makeText(this, "Current location:\n" + location, Toast.LENGTH_LONG).show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
            @NonNull int[] grantResults) {
        if (requestCode != LOCATION_PERMISSION_REQUEST_CODE) {
            return;
        }

        if (PermissionUtils.isPermissionGranted(permissions, grantResults,
                Manifest.permission.ACCESS_FINE_LOCATION)) {
            // Enable the my location layer if the permission has been granted.
            enableMyLocation();
        } else {
            // Display the missing permission error dialog when the fragments resume.
            mPermissionDenied = true;
        }
    }

    @Override
    protected void onResumeFragments() {
        super.onResumeFragments();
        if (mPermissionDenied) {
            // Permission was not granted, display error dialog.
            showMissingPermissionError();
            mPermissionDenied = false;
        }
    }

    /**
     * Displays a dialog with error message explaining that the location permission is missing.
     */
    private void showMissingPermissionError() {
        PermissionUtils.PermissionDeniedDialog
                .newInstance(true).show(getSupportFragmentManager(), "dialog");
    }

    private void HttpPostURL()
    {
        //URL url = new URL("http://www.android.com/");
        //String urlParameters  = "Uid=" + uniqueID + "&Latitude=123&Longitude=456&Accuracy=16&Altitude=130&Speed=60&Bearing=90";
        String urlParameters  = "Uid=" + uniqueID +
                                "&Latitude=" + mLatitude_out +
                                "&Longitude=" + mLongitude_out +
                                "&Accuracy=" +  mAccuracy_out +
                                "&Altitude=" +  mAltitude_out +
                                "&Speed=" + mSpeed_out +
                                "&Bearing=" + mBearing_out;

        byte[] postData = urlParameters.getBytes( StandardCharsets.UTF_8 );
        int postDataLength = postData.length;
        URL url = null;
        try {
            url = new URL("http://[your GPS logger server]/gps_form.php");
            try {
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod( "POST" );
                conn.setRequestProperty( "Content-Length", Integer.toString( postDataLength ));
                conn.setRequestProperty( "Content-Type", "application/x-www-form-urlencoded");
                conn.setRequestProperty( "charset", "utf-8");
                conn.setUseCaches(false);
                conn.setAllowUserInteraction(false);
                conn.setInstanceFollowRedirects( false );
                conn.setDoOutput( true );

                DataOutputStream wr = new DataOutputStream(conn.getOutputStream());
                wr.writeBytes(urlParameters);
                wr.flush();
                wr.close();

                int responseCode = conn.getResponseCode();

                System.out.println("\nSending 'POST' request to URL : " + url);
                System.out.println("Post parameters : " + urlParameters);
                System.out.println("Response Code : " + responseCode);
            } catch (IOException e) {
                e.printStackTrace();
            }
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }

    }

    private Runnable upload_gps = new Runnable(){
        public void run(){
            // 運行網路連線的程式
            getLastLocation();
            HttpPostURL();
            //GPSupdate();
        }
    };

    private Handler handler= new Handler();
    private int period = 1000; // 1 second

    private Runnable r = new Runnable() {
        public void run() {
            //Toast.makeText(getApplicationContext(),"RUN!",Toast.LENGTH_SHORT).show();
            System.out.println("\nUpload GPS record per second with uid: " + uniqueID);
            //getLastLocation();
            //HttpPostURL();
            Thread thread2 = new Thread(upload_gps);
            thread2.start();
            handler.postDelayed(this, period);
        }
    };

    private void GPSupdate()
    {
        //final Handler handler= new Handler();
        //final int delay = 5000; // 5 second
        final int delay = 1000; // 1 second
        handler.postDelayed(r, delay);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        handler_remove();
    }

    public void handler_remove(){
        System.out.println("\n### Stop GPS record upload for uid: " + uniqueID);
        handler.removeCallbacks(upload_gps);
        handler.removeCallbacks(r);
    }
}
