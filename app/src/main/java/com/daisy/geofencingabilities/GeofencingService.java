package com.daisy.geofencingabilities;

import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;
import android.util.Pair;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.model.LatLng;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class GeofencingService extends Service implements ConnectionCallbacks, OnConnectionFailedListener, ResultCallback<Status> {

    private final String TAG = "GeofencingService";

    protected GoogleApiClient mGoogleApiClient;
    protected HashMap<Pair<LatLng, Float>, Geofence> mGeofenceMap;
    private PendingIntent mGeofencePendingIntent;
    private int mId;

    @Override
    public void onCreate() {
        super.onCreate();
        mGeofenceMap = new HashMap<>();
        mGeofencePendingIntent = null;
        if (!mGeofenceMap.isEmpty()) {
            buildGoogleApiClient();
        }
    }

    protected synchronized void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
    }

    public int onStartCommand (Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand");
        super.onStartCommand(intent, flags, startId);
        if (mGoogleApiClient == null) {
            buildGoogleApiClient();
        }
        if(!mGoogleApiClient.isConnected()) {
            mGoogleApiClient.connect();
        }
        double lat = intent.getDoubleExtra(Consts.LATITUDE, 0);
        double lng = intent.getDoubleExtra(Consts.LONGITUDE, 0);
        if (Consts.ADD_GEOFENCE_ACTION.equals(intent.getAction())) {
            Log.d(TAG, "Try to add geofence zone");
            float rad = intent.getFloatExtra(Consts.RADIUS, 0);
            mGeofenceMap.put(new Pair<LatLng, Float>(new LatLng(lat, lng), rad), new Geofence.Builder()
                    .setRequestId(String.valueOf(++mId))
                    .setCircularRegion(lat, lng, rad)
                    .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER | Geofence.GEOFENCE_TRANSITION_EXIT)
                    .setExpirationDuration(Geofence.NEVER_EXPIRE)
                    .build());
            Log.e(TAG, String.valueOf(mGeofenceMap.size()));
            addGeofences();
        }
        if (Consts.REMOVE_GEOFENCE_ACTION.equals(intent.getAction())) {
            Log.d(TAG, "Try to remove geofence zones");
            // Delete selected geofence zones
            Iterator geoIterator = mGeofenceMap.entrySet().iterator();
            while (geoIterator.hasNext()) {
                Pair<LatLng, Float> circle = ((Map.Entry<Pair<LatLng, Float>, Geofence>)geoIterator.next()).getKey();
                float[] results = new float[1];
                Location.distanceBetween(circle.first.latitude, circle.first.longitude, lat, lng, results);
                if (circle.second >= results[0]) {
                    geoIterator.remove();
                }
            }
            if (!mGeofenceMap.isEmpty()) {
                addGeofences();
            } else {
                // There's no way we can set empty geofences list with addGeofences() method
                // thus we should remove any notifications and monitoring
                removeGeofences();
            }
            Log.e(TAG, String.valueOf(mGeofenceMap.size()));
        }

        return START_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onConnected(Bundle connectionHint) {
    }

    @Override
    public void onConnectionFailed(ConnectionResult result) {
    }

    @Override
    public void onConnectionSuspended(int cause) {
        mGoogleApiClient.connect();
    }


    private GeofencingRequest getGeofencingRequest() {
        GeofencingRequest.Builder builder = new GeofencingRequest.Builder();

        // The INITIAL_TRIGGER_ENTER flag indicates that geofencing service should trigger a
        // GEOFENCE_TRANSITION_ENTER notification when the geofence is added and if the device
        // is already inside that geofence.
        builder.setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER);

        // Add the geofences to be monitored by geofencing service.
        builder.addGeofences(new ArrayList<Geofence>(mGeofenceMap.values()));
        // Return a GeofencingRequest.
        return builder.build();
    }

    /**
     * Adds geofences, which sets alerts to be notified when the device enters or exits one of the
     * specified geofences. Handles the success or failure results returned by addGeofences().
     */
    public void addGeofences() {
        if (!mGoogleApiClient.isConnected()) {
            Log.e(TAG, "Google API client is not connected");
            return;
        }

        try {
            LocationServices.GeofencingApi.addGeofences(mGoogleApiClient, getGeofencingRequest(), getGeofencePendingIntent()).setResultCallback(this);
        } catch (SecurityException securityException) {
            Log.e(TAG, "Add geofenced error: ", securityException);
        }
    }

    /**
     * Removes geofences, which stops further notifications when the device enters or exits
     * previously registered geofences.
     */
    public void removeGeofences() {
        if (!mGoogleApiClient.isConnected()) {
            return;
        }
        try {
            // Remove geofences.
            LocationServices.GeofencingApi.removeGeofences(
                    mGoogleApiClient,
                    // This is the same pending intent that was used in addGeofences().
                    getGeofencePendingIntent()
            ).setResultCallback(this); // Result processed in onResult().
        } catch (SecurityException securityException) {
            // Catch exception generated if the app does not use ACCESS_FINE_LOCATION permission.
        }
    }

    /**
     * Gets a PendingIntent to send with the request to add or remove Geofences. Location Services
     * issues the Intent inside this PendingIntent whenever a geofence transition occurs for the
     * current list of geofences.
     *
     * @return A PendingIntent for the IntentService that handles geofence transitions.
     */
    private PendingIntent getGeofencePendingIntent() {
        // Reuse the PendingIntent if we already have it.
        if (mGeofencePendingIntent != null) {
            return mGeofencePendingIntent;
        }
        Intent intent = new Intent(this, GeofenceTransitionsIntentService.class);
        // We use FLAG_UPDATE_CURRENT so that we get the same pending intent back when calling
        // addGeofences() and removeGeofences().
        return PendingIntent.getService(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }


    /**
     * Runs when the result of calling addGeofences() and removeGeofences() becomes available.
     * Either method can complete successfully or with an error.
     *
     * Since this activity implements the {@link ResultCallback} interface, we are required to
     * define this method.
     *
     * @param status The Status returned through a PendingIntent when addGeofences() or
     *               removeGeofences() get called.
     */
    public void onResult(Status status) {
        if (status.isSuccess()) {
            Log.d(TAG, "Geofence result = success");
        } else {
            Log.e(TAG, "Geofence result error: code = " + status.getStatusCode() + "; " + status.getStatusMessage());
        }
    }

}
