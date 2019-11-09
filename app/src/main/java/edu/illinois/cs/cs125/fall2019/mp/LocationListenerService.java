package edu.illinois.cs.cs125.fall2019.mp;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.location.Location;
import android.os.Build;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;

/**
 * A foreground service to allow listening for location updates during the game even when
 * the screen is turned off.
 * <p>
 * You do not need to modify this file.
 * <p>
 * When an activity requests location updates, it gets them while it's being used, but they
 * stop when the phone's screen is off (like when it's in the pocket of someone who's running
 * around). Services are more detachable from the user-visible part of the app. This is a
 * "foreground service", which has special privileges with regard to getting location updates.
 * Foreground services need to display a persistent notification so the user knows what app
 * owns the service.
 * <p>
 * When location updates are received from the Google Play FusedLocationProviderClient API,
 * this service delivers them to the game activity via an in-process broadcast.
 */
public final class LocationListenerService extends Service {

    /** Tag for logging. */
    private static final String TAG = "LocationListenerService";

    /** The Google API object used to listen for location updates. */
    private FusedLocationProviderClient locationClient;

    /** Callback to receive location updates. */
    private LocationCallback locationCallback;

    /** A broadcaster that only transmits inside the app. */
    private LocalBroadcastManager broadcaster;

    /** The channel name of the foreground service notification. */
    private static final String NOTIFICATION_CHANNEL = "CS125Location";

    /** The ID of the foreground service notification. */
    private static final int NOTIFICATION_ID = 125;

    /** The requested interval between location updates, in milliseconds. */
    private static final int LOCATION_UPDATE_INTERVAL = 5000;

    /** The location request for FusedLocationProviderClient. */
    private static final LocationRequest LOCATION_REQUEST = LocationRequest.create()
            .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
            .setInterval(LOCATION_UPDATE_INTERVAL);

    /** The action name for the location-update Intents. */
    static final String UPDATE_ACTION = "LocationUpdate";

    /** The name of the Intent extra data item that holds the new location. */
    static final String UPDATE_DATA_ID = "Location";

    /**
     * Called when the service is created. Performs some setup and gets access to some Android components.
     */
    @Override
    public void onCreate() {
        Log.i(TAG, "Creating");
        broadcaster = LocalBroadcastManager.getInstance(this);
        locationClient = new FusedLocationProviderClient(this);
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(final LocationResult locationResult) {
                Log.i(TAG, "onLocationResult");
                if (locationResult != null && locationResult.getLastLocation() != null) {
                    Log.i(TAG, "Usable location");
                    gotLocationUpdate(locationResult.getLastLocation());
                }
            }
        };

        // Recent Android versions require a channel for notifications
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    NOTIFICATION_CHANNEL,
                    getResources().getString(R.string.app_name),
                    NotificationManager.IMPORTANCE_LOW);
            channel.setSound(null, null);
            channel.setShowBadge(false);
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
            Log.i(TAG, "Created notification channel");
        }
    }

    /**
     * Called when the service is started. Starts location updates.
     * @param intent unused
     * @param flags unused
     * @param startId unused
     * @return the restart policy of the service: recreate ASAP
     */
    @Override
    public int onStartCommand(final Intent intent, final int flags, final int startId) {
        Log.i(TAG, "Starting");
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL)
                .setOngoing(true)
                .setPriority(NotificationManager.IMPORTANCE_DEFAULT)
                .setContentText(getResources().getString(R.string.notification_text))
                .setSmallIcon(R.drawable.ic_persistent_notification);
        startForeground(NOTIFICATION_ID, builder.build());
        try {
            locationClient.requestLocationUpdates(LOCATION_REQUEST, locationCallback, Looper.myLooper());
        } catch (SecurityException e) {
            // Shouldn't happen - the service is only started after permission is granted
            Log.e(TAG, "requestLocationUpdates failed", e);
        }
        return START_STICKY;
    }

    /**
     * Called when the service is fully shut down. Stops the location listener.
     */
    @Override
    public void onDestroy() {
        if (locationClient != null) {
            locationClient.removeLocationUpdates(locationCallback);
            locationClient = null;
        }
        Log.i(TAG, "Destroyed");
    }

    /**
     * Called when the location system provides a location update.
     * @param location a non-null location
     */
    private void gotLocationUpdate(final Location location) {
        Intent update = new Intent();
        update.setAction(UPDATE_ACTION);
        update.putExtra(UPDATE_DATA_ID, location);
        broadcaster.sendBroadcast(update);
    }

    /**
     * Called when a component tries to bind to the service for interprocess communication.
     * @param intent the binding Intent (unused)
     * @return always null, since this isn't a bound service
     */
    @Nullable
    @Override
    public IBinder onBind(final Intent intent) {
        // This isn't a bound service
        Log.w(TAG, "onBind should not be called");
        return null;
    }

}
