package edu.illinois.cs.cs125.fall2019.mp;

import android.content.Intent;
import android.graphics.Point;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents the game creation screen, where the user configures a new game.
 */
public final class NewGameActivity extends AppCompatActivity {

    // This activity doesn't do much at first - it'll be worked on in Checkpoints 1 and 3

    /** The Google Maps view used to set the area for area mode. Null until getMapAsync finishes. */
    private GoogleMap areaMap;
    /** targetsMap. */
    private GoogleMap targetsMap;
    /** list. */
    private List<Marker> list;
    /** playerList. */
    private List<Invitee> playerList;
    /**
     * Called by the Android system when the activity is created.
     * @param savedInstanceState state from the previously terminated instance (unused)
     */
    @Override
    @SuppressWarnings("ConstantConditions")
    protected void onCreate(final Bundle savedInstanceState) {
        playerList = new ArrayList<>();
        playerList.add(new Invitee(FirebaseAuth.getInstance().getCurrentUser().getEmail(), TeamID.OBSERVER));
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_game);
        updatePlayersUi();
        // app/src/main/res/layout/activity_new_game.xml
        setTitle(R.string.create_game); // Change the title in the top bar
        // Now that setContentView has been called, findViewById and findFragmentById work
        list = new ArrayList<>();
        // Find the Google Maps component for the area map
        SupportMapFragment areaMapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.areaSizeMap);
        // Start the process of getting a Google Maps object
        areaMapFragment.getMapAsync(newMap -> {
            // NONLINEAR CONTROL FLOW: Code in this block is called later, after onCreate ends
            // It's a "callback" - it will be called eventually when the map is ready

            // Set the map variable so it can be used by other functions
            areaMap = newMap;
            // Center it on campustown
            centerMap(areaMap);
        });
        SupportMapFragment targetsMapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.targetsMap);
        targetsMapFragment.getMapAsync(newMap -> {
            targetsMap = newMap;
            centerMap(targetsMap);
            targetsMap.setOnMapLongClickListener(location -> {
                // Code here runs whenever the user presses on the map.
                // location is the LatLng position where the user pressed.
                // 1. Create a Google Maps Marker at the provided coordinates.
                // 2. Add it to your targets list instance variable.
                MarkerOptions options = new MarkerOptions().position(location);
                list.add(targetsMap.addMarker(options));
            });
            targetsMap.setOnMarkerClickListener(clickedMarker -> {
                // Code here runs whenever the user taps a marker.
                // clickedMarker is the Marker object the user clicked.
                // 1. Remove the marker from the map with its remove function.
                // 2. Remove it from your targets list.
                clickedMarker.remove();
                list.remove(clickedMarker);
                return true; // This makes Google Maps not pan the map again
            });
        });

        /*
         * Setting an ID for a control in the UI designer produces a constant on R.id
         * that can be passed to findViewById to get a reference to that control.
         * Here we get a reference to the Create Game button.
         */
        Button createGame = findViewById(R.id.createGame);
        /*
         * Now that we have a reference to the control, we can use its setOnClickListener
         * method to set the handler to run when the user clicks the button. That function
         * takes an OnClickListener instance. OnClickListener, like many types in Android,
         * has exactly one function which must be filled out, so Java allows instances of it
         * to be written as "lambdas", which are like small functions that can be passed around.
         * The part before the arrow is the argument list (Java infers the types); the part
         * after is the statement to run. Here we don't care about the argument, but it must
         * be there for the signature to match.
         */
        createGame.setOnClickListener(unused -> createGameClicked());
        /*
         * It's also possible to make lambdas for functions that take zero or multiple parameters.
         * In those cases, the parameter list needs to be wrapped in parentheses, like () for a
         * zero-argument lambda or (someArg, anotherArg) for a two-argument lambda. Lambdas that
         * run multiple statements, like the one passed to getMapAsync above, look more like
         * normal functions in that they need their body wrapped in curly braces. Multi-statement
         * lambdas for functions with a non-void return type need return statements, again like
         * normal functions.
         */
        Button addInvitee = findViewById(R.id.addInvitee);
        addInvitee.setOnClickListener(unused -> addInvite());

    }

    /**
     * Sets up the area sizing map with initial settings: centering on campustown.
     * <p>
     * You don't need to alter or understand this function, but you will want to use it when
     * you add another map control in Checkpoint 3.
     * @param map the map to center
     */
    private void centerMap(final GoogleMap map) {
        // Bounds of campustown and some surroundings
        final double swLatitude = 40.098331;
        final double swLongitude = -88.246065;
        final double neLatitude = 40.116601;
        final double neLongitude = -88.213077;

        // Get the window dimensions (for the width)
        Point windowSize = new Point();
        getWindowManager().getDefaultDisplay().getSize(windowSize);

        // Convert 300dp (height of map control) to pixels
        final int mapHeightDp = 300;
        float heightPx = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, mapHeightDp,
                getResources().getDisplayMetrics());

        // Submit the camera update
        final int paddingPx = 10;
        map.moveCamera(CameraUpdateFactory.newLatLngBounds(new LatLngBounds(
                new LatLng(swLatitude, swLongitude),
                new LatLng(neLatitude, neLongitude)), windowSize.x, (int) heightPx, paddingPx));
    }

    /**
     * Code to run when the Create Game button is clicked.
     */
    private void createGameClicked() {
        JsonObject object = new JsonObject();
        Intent intent = new Intent(this, GameActivity.class);
        EditText proximityThresholdText = findViewById(R.id.proximityThreshold);
        EditText cellSizeText = findViewById(R.id.cellSize);
        RadioGroup modeGroup = findViewById(R.id.gameModeGroup);
        int n = modeGroup.getCheckedRadioButtonId();
        String a = cellSizeText.getText().toString();
        String b = proximityThresholdText.getText().toString();
        if (n == R.id.targetModeOption) {
            if (!b.isEmpty()) {
                object.addProperty("mode", "target");
                int bThresh = Integer.parseInt(b);
                object.addProperty("proximityThreshold", bThresh);
                JsonArray invitees = new JsonArray();
                for (Invitee player : playerList) {
                    JsonObject eachInvitee = new JsonObject();
                    eachInvitee.addProperty("email", player.getEmail());
                    eachInvitee.addProperty("team", player.getTeamId());
                    invitees.add(eachInvitee);
                }
                JsonArray targets = new JsonArray();
                for (Marker currentTargets : list) {
                    JsonObject eachTarget = new JsonObject();
                    eachTarget.addProperty("latitude", currentTargets.getPosition().latitude);
                    eachTarget.addProperty("longitude", currentTargets.getPosition().longitude);
                    targets.add(eachTarget);
                }
                object.add("targets", targets);
                object.add("invitees", invitees);
            }
        }
        if (n == R.id.areaModeOption) {
            if (!a.isEmpty()) {
                LatLngBounds bounds = areaMap.getProjection().getVisibleRegion().latLngBounds;
                object.addProperty("mode", "area");
                int celln = Integer.parseInt(a);
                object.addProperty("cellSize", celln);
                object.addProperty("areaNorth", bounds.northeast.latitude);
                object.addProperty("areaEast", bounds.northeast.longitude);
                object.addProperty("areaSouth", bounds.southwest.latitude);
                object.addProperty("areaWest", bounds.southwest.longitude);
                JsonArray invitees = new JsonArray();
                for (Invitee player : playerList) {
                    JsonObject eachInvitee = new JsonObject();
                    eachInvitee.addProperty("email", player.getEmail());
                    eachInvitee.addProperty("team", player.getTeamId());
                    invitees.add(eachInvitee);
                }
                object.add("invitees", invitees);
            }
        }
        System.out.println(object);
        // Set up an Intent that will launch GameActivity
        WebApi.startRequest(this, WebApi.API_BASE + "/games/create", Request.Method.POST, object, response -> {
            // Code in this handler will run when the request completes successfully
            // Do something with the response?
            intent.putExtra("game", response.get("game").getAsString());
            startActivity(intent);
            finish();
        }, error -> {
            // Code in this handler will run if the request fails
            // Maybe notify the user of the error?
                Toast.makeText(this, error.getMessage(), Toast.LENGTH_LONG).show();
            });
        // Complete this function so that it populates the Intent with the user's settings (using putExtra)
        // If the user has set all necessary settings, launch the GameActivity and finish this activity
    }
    /**
     * update.
     */
    private void updatePlayersUi() {
        LinearLayout playersLinearLayout = findViewById(R.id.playersList);
        playersLinearLayout.removeAllViews();
        int flag = 0;
        for (Invitee player : playerList) {
            View messageChunk = getLayoutInflater().inflate(R.layout.chunk_invitee, playersLinearLayout, false);
            TextView inviteeEmail = messageChunk.findViewById(R.id.inviteeEmail);
            inviteeEmail.setText(player.getEmail());
            playersLinearLayout.addView(messageChunk);
            Spinner inviteeTeam = messageChunk.findViewById(R.id.inviteeTeam);
            inviteeTeam.setSelection(player.getTeamId());
            inviteeTeam.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(final AdapterView<?> parent, final View view,
                                           final int position, final long id) {
                    // Called when the user selects a different item in the dropdown
                    // The position parameter is the selected index
                    // The other parameters can be ignored
                    player.setTeamId(position);
                }
                @Override
                public void onNothingSelected(final AdapterView<?> parent) {
                    // Called when the selection becomes empty
                    // Not relevant to the MP - can be left blank
                }
            });
            Button removeButton = messageChunk.findViewById(R.id.removeInvitee);
            flag++;
            if (flag == 0) {
                removeButton.setVisibility(View.GONE);
            }
            if (player.getEmail().equals(FirebaseAuth.getInstance().getCurrentUser().getEmail())) {
                removeButton.setVisibility(View.GONE);
            }
            removeButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(final View v) {
                    playerList.remove(player);
                    playersLinearLayout.removeView(messageChunk);
                }
            });
        }
    }
    /**
     * addInvite.
     */
    private void addInvite() {
        TextView newInviteeEmail = findViewById(R.id.newInviteeEmail);
        LinearLayout playersLinearLayout = findViewById(R.id.playersList);
        if (!(newInviteeEmail.getText().toString().equals(""))) {
            playerList.add(new Invitee(newInviteeEmail.getText().toString(), TeamID.OBSERVER));
        }
        newInviteeEmail.setText("");
        updatePlayersUi();
    }
}
