package edu.illinois.cs.cs125.fall2019.mp;

import android.content.Context;
import android.graphics.Color;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.neovisionaries.ws.client.WebSocket;

import java.util.HashMap;
import java.util.Map;

/**
 * Represents a multiplayer game, providing or defining methods common to all game modes.
 * <p>
 * This is used in Checkpoint 4. You do not need to modify it.
 */
public abstract class Game {

    /** The current user's email. */
    private String email;

    /** The Google Maps view to render to. */
    private GoogleMap map;

    /** The websocket for sending data to the server. */
    private WebSocket websocket;

    /** The Android UI context. */
    private Context context;

    /** All participants' team IDs. */
    private Map<String, Integer> playerTeams = new HashMap<>();

    /** The map indicators for other players. */
    private Map<String, Circle> otherPlayerCircles = new HashMap<>();

    /**
     * Sets up this Game.
     * @param setEmail the user's email (from Firebase)
     * @param setMap the Google Maps view to render to
     * @param setWebSocket the websocket to send events to
     * @param initialState the "full" update from the server
     * @param setContext the Android UI context
     */
    public Game(final String setEmail, final GoogleMap setMap, final WebSocket setWebSocket,
                final JsonObject initialState, final Context setContext) {
        email = setEmail;
        map = setMap;
        websocket = setWebSocket;
        context = setContext;

        map.clear();
        for (JsonElement p : initialState.getAsJsonArray("players")) {
            JsonObject player = p.getAsJsonObject();
            String playerEmail = player.get("email").getAsString();
            int playerTeam = player.get("team").getAsInt();
            int playerState = player.get("state").getAsInt();
            playerTeams.put(playerEmail, playerTeam);
            if (!playerEmail.equals(email) && playerTeam != TeamID.OBSERVER && playerState == PlayerStateID.PLAYING
                    && player.has("lastLatitude")) {
                updateOtherPlayerPosition(player);
            }
        }
        if (!playerTeams.containsKey(email)) {
            throw new IllegalArgumentException("The user specified by setEmail is not in the game");
        }
    }

    /**
     * Gets the user's email address.
     * <p>
     * This method is here to expose the private email variable to subclasses.
     * @return the current user's email
     */
    protected final String getEmail() {
        return email;
    }

    /**
     * Gets the Google Maps view used by this Game.
     * <p>
     * This method is here to expose the private map variable to subclasses.
     * @return the Google Maps control to render to
     */
    protected final GoogleMap getMap() {
        return map;
    }

    /**
     * Gets the UI context.
     * <p>
     * This method is here to expose the private context variable to subclasses.
     * @return an Android UI context
     */
    protected final Context getContext() {
        return context;
    }

    /**
     * Sends a message to the server.
     * @param text serialized JSON to send
     */
    protected final void sendMessage(final String text) {
        websocket.sendText(text);
    }

    /**
     * Sends a message to the server.
     * <p>
     * Subclasses can use this to send updates via the websocket.
     * @param message JSON object to send
     */
    protected final void sendMessage(final JsonObject message) {
        sendMessage(message.toString());
    }

    /**
     * Processes a location change, makes appropriate changes to the game state,
     * and sends appropriate notifications to the server.
     * <p>
     * Subclasses should implement this in a way specific to the mode of game they represent.
     * @param location a location FusedLocationProviderClient is reasonably confident about
     */
    public abstract void locationUpdated(LatLng location);

    /**
     * Gets a team's score.
     * <p>
     * Subclasses should implement this in a way specific to their game mode's type of objective.
     * @param teamId the team ID
     * @return how many objectives the team has captured
     */
    public abstract int getTeamScore(int teamId);

    /**
     * Processes an update from the server.
     * <p>
     * This implementation handles playerLocation and playerExit events, updating the
     * player circles appropriately. Subclass implementations should handle events specific
     * to their game mode, delegating others to this implementation with a super call.
     * @param message JSON from the server
     * @param type the update type
     * @return whether the message was handled
     */
    public boolean handleMessage(final JsonObject message, final String type) {
        switch (type) {
            case "playerLocation":
                updateOtherPlayerPosition(message);
                return true;
            case "playerExit":
                Circle c = otherPlayerCircles.remove(message.get("email").getAsString());
                if (c != null) {
                    c.remove();
                }
                return true;
            default:
                return false;
        }
    }

    /**
     * Gets the user's team ID in this game.
     * @return team ID as defined in TeamID
     */
    @SuppressWarnings("ConstantConditions")
    public final int getMyTeam() {
        return playerTeams.get(email);
    }

    /**
     * Updates the map indicator of another player.
     * @param player parsed JSON from a player location update or a player section of a full update
     */
    @SuppressWarnings("ConstantConditions")
    private void updateOtherPlayerPosition(final JsonObject player) {
        String playerEmail = player.get("email").getAsString();
        LatLng location = new LatLng(player.get("lastLatitude").getAsDouble(),
                player.get("lastLongitude").getAsDouble());
        int[] teamColors = getContext().getResources().getIntArray(R.array.team_colors);
        final double circleRadius = 4.0;
        CircleOptions c = new CircleOptions().center(location)
                .radius(circleRadius)
                .fillColor(teamColors[playerTeams.get(playerEmail)])
                .zIndex(2.0f)
                .strokeColor(Color.BLACK)
                .strokeWidth(2);
        Circle old = otherPlayerCircles.put(playerEmail, map.addCircle(c));
        if (old != null) {
            old.remove();
        }
    }

}
