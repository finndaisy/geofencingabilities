package com.daisy.geofencingabilities;


import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.util.Log;

import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingEvent;
import com.daisy.geofencingabilities.R;

public class GeofenceTransitionsIntentService extends IntentService{

    protected static final String TAG = "GeofenceTransitionsIntentService";

    /**
     * This constructor is required, and calls the super IntentService(String)
     * constructor with the name for a worker thread.
     */
    public GeofenceTransitionsIntentService() {
        // Use the TAG to name the worker thread.
        super(TAG);
    }

    /**
     * Handles incoming intents.
     * @param intent sent by Location Services. This Intent is provided to Location
     *               Services (inside a PendingIntent) when addGeofences() is called.
     */
    @Override
    protected void onHandleIntent(Intent intent) {
        Log.d(TAG, "New geo event was fired");
        GeofencingEvent geofencingEvent = GeofencingEvent.fromIntent(intent);
        if (geofencingEvent.hasError()) {
            return;
        }

        int geofenceTransition = geofencingEvent.getGeofenceTransition();

        if (geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER ||
                geofenceTransition == Geofence.GEOFENCE_TRANSITION_EXIT) {

            String transitionString = getTransitionString(geofenceTransition);

            Location trigerredLocation = geofencingEvent.getTriggeringLocation();

            sendNotification(transitionString, trigerredLocation.toString());
        } else {
            // Log the error.
        }
    }

    private void sendNotification(String title, String message) {
        Log.d(TAG, "Send notification: title = " + title + "; message = " + message);
        Notification.Builder mBuilder =
                new Notification.Builder(this)
                        .setPriority(Notification.PRIORITY_MAX)
                        .setLargeIcon(BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher))
                        .setSmallIcon(R.mipmap.ic_launcher)
                        .setContentTitle(title)
                        .setContentText(message);
        Intent resultIntent = new Intent(this, MapsActivity.class);

        PendingIntent resultPendingIntent =
                PendingIntent.getActivity(this, 0, resultIntent, 0);
        mBuilder.setContentIntent(resultPendingIntent);
        NotificationManager mNotificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.notify(0, mBuilder.build());
    }

    /**
     * Maps geofence transition types to their human-readable equivalents.
     *
     * @param transitionType    A transition type constant defined in Geofence
     * @return                  A String indicating the type of transition
     */
    private String getTransitionString(int transitionType) {
        switch (transitionType) {
            case Geofence.GEOFENCE_TRANSITION_ENTER:
                return "zone-enter";
            case Geofence.GEOFENCE_TRANSITION_EXIT:
                return "zone-leave";
            default:
                return "Unknown transition";
        }
    }

}
