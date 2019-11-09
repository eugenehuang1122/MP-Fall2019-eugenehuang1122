package edu.illinois.cs.cs125.fall2019.mp;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.location.Location;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.firebase.auth.FirebaseAuth;
import com.google.gson.JsonObject;
import com.neovisionaries.ws.client.WebSocket;
import com.neovisionaries.ws.client.WebSocketCloseCode;

/**
 * Represents the game activity, where the user plays the game and sees its state.
 */
public final class GameActivity extends AppCompatActivity {

    /** The tag for Log calls - this makes it easier to tell what component messages come from. */
    private static final String TAG = "GameActivity";

    /** The radial location accuracy required to send a location update. */
    private static final float REQUIRED_LOCATION_ACCURACY = 28f;

    /** The handler for location updates sent by the location listener service. */
    private BroadcastReceiver locationUpdateReceiver;

    /** The current state of the game. */
    private int gameState = GameStateID.PAUSED;

    /** An object representing the game. */
    private Game game;

    /** A reference to the map control. */
    private GoogleMap map;

    /** Whether the user's location has been found and used to center the map. */
    private boolean centeredMap;

    /** The ID of the game being played. */
    private String gameId;

    /** The websocket used to communicate gameplay events. */
    private WebSocket webSocket;

    /** Whether permission has been granted to access the phone's exact location. */
    private boolean hasLocationPermission;

    /**
     * Called by the Android system when the activity is created. Performs initial setup.
     * @param savedInstanceState saved state from the last terminated instance (unused)
     */
    @Override
    @SuppressWarnings("ConstantConditions")
    protected void onCreate(final Bundle savedInstanceState) {
        Log.i(TAG, "Creating");
        // The "super" call is required for all activities
        super.onCreate(savedInstanceState);
        // Create the UI from the activity_game.xml layout file (in src/main/res/layout)
        setContentView(R.layout.activity_game);

        findViewById(R.id.pauseUnpauseGame).setOnClickListener(unused -> toggleGameRunning());
        findViewById(R.id.endGame).setOnClickListener(unused -> endGame());

        // 4.1: You need to fill this in to connect to the game's websocket
        // Load the game ID from the intent
        // Start the process of connecting to the server
        Intent id = getIntent();
        gameId = id.getStringExtra("game");
        startActivity(id);
        connectWebSocket();

        // Find the Google Maps UI component ("fragment")
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.gameMap);
        // Interestingly, the UI component itself doesn't have methods to manipulate the map
        // We need to get a GoogleMap instance from it and use that
        mapFragment.getMapAsync(theMap -> {
            // Save the map so it can be manipulated later
            map = theMap;
            // Configure it
            setUpMap();
        });

        // Set up a receiver for location-update messages from the service (LocationListenerService)
        locationUpdateReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(final Context context, final Intent intent) {
                Log.i(TAG, "Received location update from service");
                // Android Intents represent action plans or notifications
                // They can contain data - the ones from our service contain a Location
                Location location = intent.getParcelableExtra(LocationListenerService.UPDATE_DATA_ID);

                // If the location is usable, call updateLocation
                if (map != null && location != null && location.hasAccuracy()
                        && location.getAccuracy() < REQUIRED_LOCATION_ACCURACY) {
                    ensureMapCentered(location);
                    updateLocation(location);
                }
            }
        };
        // Register (activate) it
        LocalBroadcastManager.getInstance(this).registerReceiver(locationUpdateReceiver,
                new IntentFilter(LocationListenerService.UPDATE_ACTION)); // Only listen for messages from the service

        // Android only allows location access to apps that asked for it and had the request approved by the user
        // See if we need to make a request
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            // If permission isn't already granted, start a request
            ActivityCompat.requestPermissions(this, new String[] {Manifest.permission.ACCESS_FINE_LOCATION}, 0);
            Log.i(TAG, "Asked for location permission");
            // The result will be delivered to the onRequestPermissionsResult function
        } else {
            Log.i(TAG, "Already had location permission");
            // If we have the location permission, start the location listener service
            hasLocationPermission = true;
            startLocationWatching();
        }
    }

    /**
     * Called by the Android system when the activity is stopped and cannot be returned to.
     */
    @Override
    protected void onDestroy() {
        // The "super" call is required for all activities
        super.onDestroy();

        // Location is only needed while playing a game - stop the service to save power
        stopLocationWatching();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(locationUpdateReceiver);

        // Disconnect from the web socket
        if (webSocket != null) {
            webSocket.disconnect(WebSocketCloseCode.AWAY);
        }
        Log.i(TAG, "Destroyed");
    }

    /**
     * Called by the Android system when a permissions request receives a response from the user.
     * @param requestCode the ID of the request (always 0 in our case)
     * @param permissions the affected permissions' names
     * @param grantResults whether each permission was granted (corresponds to the permissions array)
     */
    @Override
    @SuppressLint("MissingPermission")
    public void onRequestPermissionsResult(final int requestCode, final @NonNull String[] permissions,
                                           final @NonNull int[] grantResults) {
        Log.i(TAG, "Permission request result received");
        // The "super" call is required so that the notification will be delivered to fragments
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        // Check whether the request was approved by the user
        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            Log.i(TAG, "Permission granted by the user");
            // Got the location permission for the first time
            hasLocationPermission = true;
            // Enable the My Location blue dot on the map
            if (map != null) {
                Log.i(TAG, "onRequestPermissionsResult enabled My Location");
                map.setMyLocationEnabled(true);
            }
            // Start the location listener service
            startLocationWatching();
        }
    }

    /**
     * Sets up the Google map.
     */
    @SuppressWarnings("MissingPermission")
    private void setUpMap() {
        Log.i(TAG, "Entered setUpMap");
        if (hasLocationPermission) {
            // Can only enable the blue My Location dot if the location permission is granted
            map.setMyLocationEnabled(true);
            Log.i(TAG, "setUpMap enabled My Location");
        }

        // Disable some extra UI that gets in the way
        map.getUiSettings().setIndoorLevelPickerEnabled(false);
        map.getUiSettings().setMapToolbarEnabled(false);

        // This function is no longer responsible for rendering game-specific elements
        // That's taken care of by the Game subclasses
    }

    /**
     * Centers the map on the user's location if the map hasn't been centered yet.
     * @param location the current location
     */
    private void ensureMapCentered(final Location location) {
        if (location != null && !centeredMap) {
            final float defaultMapZoom = 18f;
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(
                    new LatLng(location.getLatitude(), location.getLongitude()), defaultMapZoom));
            centeredMap = true;
            Log.i(TAG, "Centered map");
            if (game != null && game.getMyTeam() == TeamID.OBSERVER) {
                stopLocationWatching();
            }
        }
    }

    /**
     * Called when a high-confidence location update is available.
     * <p>
     * You need to fill in this function in section 4.3 so that the player's movements affect the
     * game and are sent to the server, if appropriate.
     * @param location the phone's current location, not null
     */
    private void updateLocation(final Location location) {
        // If the game object or websocket haven't been set yet, return (nothing can be done)
        // If the user is only an observer in the game, return (their movements don't matter)

        // Notify the server of the movement - start by creating a Gson JSON object representing the message
        JsonObject locUpdate = new JsonObject();
        // You need to fill the object out with the properties of a location update
        // Once the object is ready, convert it to a JSON string and send it over the websocket
        webSocket.sendText(locUpdate.toString());

        // Call the logic that updates gameplay based on the user's movements
    }

    /**
     * Starts watching for location changes if possible under the current permissions.
     */
    @SuppressWarnings("MissingPermission")
    private void startLocationWatching() {
        Log.i(TAG, "Starting location watching");
        // Make sure the location permission has been granted
        if (!hasLocationPermission) {
            Log.w(TAG, "startLocationWatching: Missing permission");
            return;
        }
        // Make sure the My Location blue dot on the map is enabled
        if (map != null) {
            map.setMyLocationEnabled(true);
            Log.i(TAG, "startLocationWatching enabled My Location");
        }
        // Start the location listener service, which will notify this activity of movements
        ContextCompat.startForegroundService(this, new Intent(this, LocationListenerService.class));
        // Keep the screen on even if not touched in a while
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    /**
     * Stops watching for location changes.
     */
    private void stopLocationWatching() {
        Log.i(TAG, "Stopping location watching");
        // Stop the location listener service
        stopService(new Intent(this, LocationListenerService.class));
        // Allow the screen to turn off after a moment of inactivity
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    /**
     * Called when a message is received from the server.
     * <p>
     * You should fill out this function to react to game data, gameplay events, and game state changes.
     * @param message the parsed JSON from the server
     */
    private void receivedData(final JsonObject message) {
        String type = message.get("type").getAsString();
        switch (type) {
            case "full":
                // The full update contains the entire current state of the game
                String myEmail = FirebaseAuth.getInstance().getCurrentUser().getEmail();
                if (message.get("owner").getAsString().equals(myEmail)) {
                    // Show the game owner controls if and only if the user owns the game
                    findViewById(R.id.gameOwnerControls).setVisibility(View.VISIBLE);
                }
                // 4.2: You need to fill this in to act on the game's current state
                // Call the updateGameState helper function with the game state

                // 4.3: You need to fill this in to load the game progress into the game variable
                // Initialize the game instance variable with an instance of the Game subclass appropriate for the mode
                // Then you can uncomment the if statement below

                // Observers don't need to have their location tracked
                /*if (game.getMyTeam() == TeamID.OBSERVER && centeredMap) {
                    stopLocationWatching();
                }*/
                break;
            case "gameState":
                // 4.7: If the game is over, show the winner in a dialog that finishes the activity when dismissed
                // 4.2: Otherwise use the updateGameState helper function to display the state change
                break;
            default:
                // 4.3: Process any other message as a gameplay update, using the game object
        }
    }

    /**
     * Attempts to connect to the server via websocket.
     */
    private void connectWebSocket() {
        // Reset UI
        findViewById(R.id.gameOwnerControls).setVisibility(View.GONE);
        TextView gameStateLabel = findViewById(R.id.gameState);
        gameStateLabel.setText("Connecting...");
        webSocket = null;

        // Start connecting to the websocket
        WebApi.connectWebSocket(WebApi.WEBSOCKET_BASE + "/games/" + gameId + "/play",
            // When an update is received from the server, use the receivedData function to act on it
            data -> runOnUiThread(() -> receivedData(data)),
            // When the websocket is first created, store it in an instance variable (analogous to getMapAsync)
            ws -> webSocket = ws,
            // When an existing connection is lost, try to reconnect
            () -> runOnUiThread(this::connectWebSocket),
            // When a new connection fails, display an error
            error -> runOnUiThread(() -> gameStateLabel.setText("Connection lost")));
    }

    /**
     * Updates UI according to the state of the ongoing game.
     * <p>
     * You should implement this helper function for section 4.2.
     * @param newState the game's current state (a GameStateID constant, PAUSED or RUNNING)
     */
    private void updateGameState(final int newState) {
        // Record the new game state in the appropriate instance variable
        // Change the text of the pauseUnpauseGame button and the gameState label
        // When the game is paused, the button should say Resume and the label should say Paused
        // When the game is running, the button should say Pause and the label should say Running
    }

    /**
     * Prompts the user (who is the game owner) whether to end the game.
     */
    private void endGame() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("Are you sure you want to end the game? This cannot be undone.");
        builder.setNegativeButton("Cancel", null);
        builder.setPositiveButton("End Game", (unused1, unused2) -> gameLifecycleControl("end"));
        builder.create().show();
    }

    /**
     * Called when the user (who is the game owner) presses the Pause/Resume button.
     */
    private void toggleGameRunning() {
        if (gameState == GameStateID.PAUSED) {
            gameLifecycleControl("resume");
        } else {
            gameLifecycleControl("pause");
        }
    }

    /**
     * Makes an API call to control the lifecycle of the game.
     * @param action the game sub-endpoint: "resume", "pause", or "end"
     */
    private void gameLifecycleControl(final String action) {
        WebApi.startRequest(this, WebApi.API_BASE + "/games/" + gameId + "/" + action, Request.Method.POST, null,
            unused -> { }, error -> Toast.makeText(this, "Could not connect to server.", Toast.LENGTH_LONG).show());
    }

    /**
     * Updates the scores label.
     * <p>
     * You should fill out this helper function in section 4.6 to show all teams' scores in the gameScores label.
     * This can be used in several places after you have gameplay working.
     */
    private void updateScores() {
        if (game == null) {
            return;
        }
        String[] teamNames = getResources().getStringArray(R.array.team_choices);
        TextView scoresLabel = findViewById(R.id.gameScores);

        // Get each team's score from the game and build a string showing all of them
        // Set the scores label's text to that string

        // Each team's name should be separated from its score by a colon and a space
        // Other than that, you may use any format you like
        // For example, "Red: 0, Yellow: 4, Green: 0, Blue: 3" is acceptable
    }

}
