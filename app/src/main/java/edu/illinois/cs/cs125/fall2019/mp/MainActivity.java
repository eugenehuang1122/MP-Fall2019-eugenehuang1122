package edu.illinois.cs.cs125.fall2019.mp;

import android.content.Intent;

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.google.firebase.auth.FirebaseAuth;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

/**
 * Represents the main screen of the app, where the user will be able to view invitations and enter games.
 */
public final class MainActivity extends AppCompatActivity {

    /**
     * Called by the Android system when the activity is created.
     *
     * @param savedInstanceState saved state from the previously terminated instance of this activity (unused)
     */
    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        // This "super" call is required for all activities
        super.onCreate(savedInstanceState);
        // Create the UI from a layout resource
        setContentView(R.layout.activity_main);
        connect();
        // This activity doesn't do anything yet - it immediately launches the game activity
        // Work on it will start in Checkpoint 1

        // Intents are Android's way of specifying what to do/launch
        // Here we create an Intent for launching GameActivity and act on it with startActivity
        //startActivity(new Intent(this, GameActivity.class));
        // End this activity so that it's removed from the history
        // Otherwise pressing the back button in the game would come back to a blank screen here
        //finish();
    }

    // The functions below are stubs that will be filled out in Checkpoint 2

    /**
     * Starts an attempt to connect to the server to fetch/refresh games.
     */
    private void connect() {
        // Make any "loading" UI adjustments you like
        // Use WebApi.startRequest to fetch the games lists
        // In the response callback, call setUpUi with the received data
        LinearLayout invitationsGroup = findViewById(R.id.invitationsGroup);
        invitationsGroup.setVisibility(View.GONE);
        LinearLayout ongoingGamesGroup = findViewById(R.id.ongoingGamesGroup);
        ongoingGamesGroup.setVisibility(View.GONE);
        WebApi.startRequest(this, WebApi.API_BASE + "/games", response -> {
            // Code in this handler will run when the request completes successfully
            // Do something with the response?
            setUpUi(response); }, error -> {
            // Code in this handler will run if the request fails
            // Maybe notify the user of the error?
                Toast.makeText(this, "Oh no!", Toast.LENGTH_LONG).show();
            });
    }

    /**
     * Populates the games lists UI with data retrieved from the server.
     *
     * @param result parsed JSON from the server
     */
    private void setUpUi(final JsonObject result) {

        // Hide any optional "loading" UI you added
        // Clear the games lists
        // Add UI chunks to the lists based on the result data
        LinearLayout invitationsGroup = findViewById(R.id.invitationsGroup);
        LinearLayout ongoingGamesList = findViewById(R.id.ongoingGamesList);
        LinearLayout invitationList = findViewById(R.id.invitationsList);
        ongoingGamesList.removeAllViews();
        invitationList.removeAllViews();
        JsonArray array = result.get("games").getAsJsonArray();
        for (int i = 0; i < array.size(); i++) {
            JsonObject currentGame = array.get(i).getAsJsonObject();
            String id = currentGame.get("id").getAsString();
            String owner = currentGame.get("owner").getAsString();
            int state = currentGame.get("state").getAsInt();
            if (state != (GameStateID.ENDED)) {
                String mode = currentGame.get("mode").getAsString();
                JsonArray players = currentGame.get("players").getAsJsonArray();
                for (int j = 0; j < players.size(); j++) {
                    JsonObject currentPlayer = players.get(j).getAsJsonObject();
                    String email = currentPlayer.get("email").getAsString();
                    int team = currentPlayer.get("team").getAsInt();
                    state = currentPlayer.get("state").getAsInt();
                    if (email.equals(FirebaseAuth.getInstance().getCurrentUser().getEmail())) {
                        if ((state == PlayerStateID.ACCEPTED) || (state == PlayerStateID.PLAYING)) {
                            View messageChunk = getLayoutInflater().inflate(R.layout.chunk_ongoing_game,
                                    ongoingGamesList, false);
                            Button enter = messageChunk.findViewById(R.id.Enter);
                            enter.setOnClickListener(unused -> {
                                enterGame(id);
                            });
                            Button leave = messageChunk.findViewById(R.id.Leave);
                            leave.setOnClickListener((View v) -> WebApi.startRequest(this, WebApi.API_BASE
                                    + "/games/" + id + "/leave", Request.Method.POST, null, response -> {
                                    connect();
                                }, error -> {
                                    Toast.makeText(this, "Oh no!", Toast.LENGTH_LONG).show();
                                }));
                            if (email.equals(owner)) {
                                leave.setVisibility(View.GONE);
                            }
                            LinearLayout ongoingGamesGroup = findViewById(R.id.ongoingGamesGroup);
                            ongoingGamesGroup.setVisibility(View.VISIBLE);
                            TextView emaill = messageChunk.findViewById(R.id.email);
                            emaill.setText(owner);
                            TextView modee = messageChunk.findViewById(R.id.mode);
                            modee.setText(mode + " mode");
                            String[] teamNames = getResources().getStringArray(R.array.team_choices);
                            TextView teamm = messageChunk.findViewById(R.id.team);
                            teamm.setText(teamNames[team]);
                            ongoingGamesList.addView(messageChunk);
                        } else if (state == PlayerStateID.INVITED) {
                            View messageChunk = getLayoutInflater().inflate(
                                    R.layout.chunk_invitations, invitationList, false);
                            invitationsGroup.setVisibility(View.VISIBLE);
                            TextView emaill = messageChunk.findViewById(R.id.email);
                            emaill.setText(owner);
                            TextView modee = messageChunk.findViewById(R.id.mode);
                            modee.setText(mode + " mode");
                            String[] teamNames = getResources().getStringArray(R.array.team_choices);
                            TextView teamm = messageChunk.findViewById(R.id.team);
                            teamm.setText(teamNames[team]);
                            invitationList.addView(messageChunk);
                            Button accept = messageChunk.findViewById(R.id.Accept);
                            accept.setOnClickListener((View v) -> WebApi.startRequest(MainActivity.this,
                                    WebApi.API_BASE + "/games/" + id + "/accept",
                                    Request.Method.POST, null, response -> {
                                    connect();
                                }, error -> {
                                    Toast.makeText(this, "Oh no!", Toast.LENGTH_LONG).show();
                                }));
                            Button decline = messageChunk.findViewById(R.id.Decline);
                            decline.setOnClickListener((View v) -> WebApi.startRequest(this, WebApi.API_BASE
                                    + "/games/" + id + "/decline", Request.Method.POST, null, response -> {
                                    connect();
                                }, error -> {
                                    Toast.makeText(this, "Oh no!", Toast.LENGTH_LONG).show();
                                }));
                        }
                    }
                }
            }
        }
    }
    /**
     * Enters a game (shows the map).
     * @param gameId the ID of the game to enter
     */
    private void enterGame(final String gameId) {
        // Launch GameActivity with the game ID in an intent extra
        // Do not finish - the user should be able to come back here
        Intent id = new Intent(this, GameActivity.class);
        id.putExtra("game", gameId);
        startActivity(id);
    }
}


