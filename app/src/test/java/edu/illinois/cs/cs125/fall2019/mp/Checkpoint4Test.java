package edu.illinois.cs.cs125.fall2019.mp;

import android.app.Dialog;
import android.content.Context;
import android.util.Pair;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.IdRes;
import androidx.test.core.app.ApplicationProvider;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.Polygon;
import com.google.android.gms.maps.model.Polyline;
import com.google.firebase.auth.FirebaseAuth;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.rule.PowerMockRule;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.shadow.api.Shadow;
import org.robolectric.shadows.ShadowDialog;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

import edu.illinois.cs.cs125.fall2019.mp.shadows.MockedWrapperInstantiator;
import edu.illinois.cs.cs125.fall2019.mp.shadows.ShadowGoogleMap;
import edu.illinois.cs.cs125.fall2019.mp.shadows.ShadowLocalBroadcastManager;
import edu.illinois.cs.cs125.fall2019.mp.shadows.ShadowMarker;
import edu.illinois.cs.cs125.gradlegrader.annotations.Graded;
import edu.illinois.cs.cs125.robolectricsecurity.PowerMockSecurity;
import edu.illinois.cs.cs125.robolectricsecurity.Trusted;

@RunWith(RobolectricTestRunner.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@PowerMockIgnore({"org.mockito.*", "org.powermock.*", "org.robolectric.*", "android.*", "androidx.*", "com.google.android.*", "edu.illinois.cs.cs125.fall2019.mp.shadows.*"})
@PrepareForTest({WebApi.class, FirebaseAuth.class})
@Trusted
public class Checkpoint4Test {

    private static final Predicate<JsonObject> VALID = message -> {
        if (!message.has("type")) {
            throw new IllegalStateException("All updates must have a 'type' property");
        }
        return true;
    };

    private static final Predicate<JsonObject> EXCEPT_LOCATION_UPDATE = VALID
            .and(message -> !message.get("type").getAsString().equals("locationUpdate"));

    private Context appContext;

    @Rule
    public PowerMockRule mockStaticClasses = new PowerMockRule();

    @Before
    public void setup() {
        PowerMockSecurity.secureMockMethodCache();
        FirebaseMocker.mock();
        FirebaseMocker.setEmail(SampleData.USER_EMAIL);
        appContext = ApplicationProvider.getApplicationContext();
        try {
            Class<?> defaultTargetsClass = Class.forName("edu.illinois.cs.cs125.fall2019.mp.DefaultTargets");
            defaultTargetsClass.getMethod("disable").invoke(null);
        } catch (ClassNotFoundException | NoSuchMethodException e) {
            // Expected
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    @After
    public void teardown() {
        WebApiMocker.reset();
        ShadowLocalBroadcastManager.reset();
        FirebaseMocker.setBan(null);
    }

    @Test(timeout = 60000)
    @Graded(points = 5)
    public void testWebSocket() {
        // Start the activity
        WebSocketMocker webSocketControl = WebSocketMocker.expectConnection();
        String gameId = RandomHelper.randomId();
        GameActivityLauncher launcher = new GameActivityLauncher(gameId);
        Assert.assertTrue("The activity should connect to the websocket when started", webSocketControl.isConnected());
        Assert.assertEquals("Websocket URL is incorrect",
                WebApi.WEBSOCKET_BASE + "/games/" + gameId + "/play", webSocketControl.getConnectionUrl());

        // Stop the activity
        launcher.shutdown();
        Assert.assertFalse("The activity should disconnect from the websocket when stopped", webSocketControl.isConnected());
    }

    @Test(timeout = 60000)
    @Graded(points = 5)
    public void testGameStateDisplay() {
        // Set up activity
        WebSocketMocker webSocketControl = WebSocketMocker.expectConnection();
        GameActivity activity = new GameActivityLauncher().getActivity();

        // Send a paused full update
        webSocketControl.sendData(SampleData.createStateTestGame());
        TextView stateLabel = activity.findViewById(IdLookup.require("gameState"));
        Assert.assertEquals("The game state label should be visible", View.VISIBLE, stateLabel.getVisibility());
        Assert.assertEquals("Game state label was incorrect for a paused game", "Paused", stateLabel.getText().toString());

        // Resume the game
        webSocketControl.sendData(JsonHelper.updateGameState(GameStateID.RUNNING));
        Assert.assertEquals("Game state label was incorrect for a running game", "Running", stateLabel.getText().toString());

        // Pause the game again
        webSocketControl.sendData(JsonHelper.updateGameState(GameStateID.PAUSED));
        Assert.assertEquals("Game state label was incorrect for a paused game", "Paused", stateLabel.getText().toString());

        // Start the activity with a running game
        webSocketControl = WebSocketMocker.expectConnection();
        activity = new GameActivityLauncher().getActivity();
        stateLabel = activity.findViewById(IdLookup.require("gameState"));
        JsonObject fullUpdate = SampleData.createStateTestGame();
        fullUpdate.addProperty("state", GameStateID.RUNNING);
        webSocketControl.sendData(fullUpdate);
        Assert.assertEquals("Game state label was incorrect for a running game", "Running", stateLabel.getText().toString());

        // Pause the game
        webSocketControl.sendData(JsonHelper.updateGameState(GameStateID.PAUSED));
        Assert.assertEquals("Game state label was incorrect for a paused game", "Paused", stateLabel.getText().toString());
    }

    @Test(timeout = 60000)
    @Graded(points = 5)
    public void testLocationUpdates() {
        // Start the activity
        WebSocketMocker webSocketControl = WebSocketMocker.expectConnection();
        GameActivityLauncher launcher = new GameActivityLauncher();

        // Update the location before sending the game information
        for (int i = 0; i < 10; i++) {
            launcher.sendLocationUpdate(new LatLng(40.06929 + i * 0.0001, RandomHelper.randomLng()));
        }
        webSocketControl.assertNoMessagesMatch("Location updates should not be sent before game information is received", VALID);

        // Send a game where the user is an observer
        webSocketControl.sendData(SampleData.createMinimalTestGame(TeamID.OBSERVER));

        // Send location updates
        for (int i = 0; i < 10; i++) {
            launcher.sendLocationUpdate(new LatLng(RandomHelper.randomLat(), -88.23298 - i * 0.0001));
        }
        webSocketControl.assertNoMessagesMatch("Location updates should not be sent when the user is an observer", VALID);

        // Start the activity with a game in which the user is a player
        launcher = new GameActivityLauncher();
        webSocketControl.sendData(SampleData.createMinimalTestGame(TeamID.TEAM_RED));

        // Send location updates
        for (int i = 0; i < 10; i++) {
            LatLng position = new LatLng(RandomHelper.randomLat(), -88.23298 - i * 0.0001);
            launcher.sendLocationUpdate(position);
            webSocketControl.processOneMessage("Location updates should be sent via websocket when the user is a player", VALID,
                    message -> {
                        Assert.assertEquals("Incorrect location update message type", "locationUpdate", message.get("type").getAsString());
                        Assert.assertTrue("locationUpdate updates should have a 'latitude' property", message.has("latitude"));
                        Assert.assertEquals("Incorrect position in location update", position.latitude, message.get("latitude").getAsDouble(), 1e-7);
                        Assert.assertTrue("locationUpdate updates should have a 'longitude' property", message.has("longitude"));
                        Assert.assertEquals("Incorrect position in location update", position.longitude, message.get("longitude").getAsDouble(), 1e-7);
                    });
            if (i == 4) {
                // Location updates should be sent in both PAUSED and RUNNING states
                webSocketControl.sendData(JsonHelper.updateGameState(GameStateID.RUNNING));
            }
        }
    }

    @Test(timeout = 60000)
    @Graded(points = 25)
    public void testMultiplePlayersTargetMode() {
        // Create the game
        FirebaseMocker.setBan("TargetGame should use the Game getEmail instance method rather than asking FirebaseAuth");
        GoogleMap map = MockedWrapperInstantiator.create(GoogleMap.class);
        ShadowGoogleMap shadowMap = Shadow.extract(map);
        WebSocketMocker webSocketControl = new WebSocketMocker();
        JsonObject gameConfig = SampleData.createTargetModeTestGame();
        Game game = new TargetGame(SampleData.USER_EMAIL, map, webSocketControl.getWebSocket(), gameConfig.deepCopy(), appContext);

        // Make sure it only handles known events
        Assert.assertFalse("TargetGame should not handle area mode events",
                game.handleMessage(JsonHelper.updatePlayerCellCapture("noone@illinois.edu", TeamID.TEAM_RED, 1, 2), "playerCellCapture"));
        JsonObject nonsenseUpdate = new JsonObject();
        nonsenseUpdate.addProperty("type", "nonsense");
        Assert.assertFalse("TargetGame should not handle unknown events", game.handleMessage(nonsenseUpdate, "nonsense"));

        // Check initial target colors
        JsonArray targets = gameConfig.getAsJsonArray("targets");
        LatLng[] targetPos = new LatLng[targets.size()];
        for (int t = 0; t < targets.size(); t++) {
            JsonObject target = targets.get(t).getAsJsonObject();
            LatLng position = new LatLng(target.get("latitude").getAsDouble(), target.get("longitude").getAsDouble());
            targetPos[t] = position;
            Marker marker = shadowMap.getMarkerAt(position);
            Assert.assertNotNull("TargetGame did not create/position markers for targets", marker);
            ShadowMarker shadowMarker = Shadow.extract(marker);
            switch (target.get("team").getAsInt()) {
                case TeamID.TEAM_RED:
                    Assert.assertEquals("Incorrect marker hue for red-claimed target", BitmapDescriptorFactory.HUE_RED, shadowMarker.getHue(), 1e-3);
                    break;
                case TeamID.TEAM_YELLOW:
                    Assert.assertEquals("Incorrect marker hue for yellow-claimed target", BitmapDescriptorFactory.HUE_YELLOW, shadowMarker.getHue(), 1e-3);
                    break;
                default:
                    Assert.assertEquals("Incorrect marker hue for unclaimed target", BitmapDescriptorFactory.HUE_VIOLET, shadowMarker.getHue(), 1e-3);
            }
        }

        // Check initial lines
        List<Polyline> polylines = shadowMap.getPolylinesConnecting(targetPos[0], targetPos[1]);
        Assert.assertNotEquals("No polyline connects the first two points in the user's path", 0, polylines.size());
        int yellow = appContext.getColor(R.color.yellow);
        Assert.assertTrue("The user's path polyline does not have the team color", polylines.stream().anyMatch(p -> p.getColor() == yellow));
        polylines = shadowMap.getPolylinesConnecting(targetPos[1], targetPos[2]);
        Assert.assertNotEquals("No polyline connects the last two points in the user's path", 0, polylines.size());
        Assert.assertTrue("The user's path polyline does not have the team color", polylines.stream().anyMatch(p -> p.getColor() == yellow));
        for (int i = 3; i < targets.size(); i++) {
            polylines = shadowMap.getPolylinesConnecting(targetPos[2], targetPos[i]);
            Assert.assertEquals("A polyline connects two unrelated targets", 0, polylines.size());
        }
        polylines = shadowMap.getPolylinesConnecting(targetPos[3], targetPos[4]);
        Assert.assertNotEquals("No polyline connects the two points in noone@illinois.edu's path", 0, polylines.size());
        Assert.assertTrue("noone@illinois.edu's polyline does not have the team color", polylines.stream().anyMatch(p -> p.getColor() == yellow));

        // Try to claim a target on the opposite side of a teammate's path
        game.locationUpdated(targetPos[6]); // FarDown
        webSocketControl.assertNoMessagesMatch("It should not be possible to cross another player's path", EXCEPT_LOCATION_UPDATE);
        polylines = shadowMap.getPolylinesConnecting(targetPos[2], targetPos[6]);
        Assert.assertEquals("Failed target claims should not create polylines", 0, polylines.size());

        // Try to claim an available target
        game.locationUpdated(targetPos[7]); // FarUp
        webSocketControl.processOneMessage("Visiting a target should produce a targetVisit event", EXCEPT_LOCATION_UPDATE,
                message -> {
                    Assert.assertEquals("Incorrect target visit update type", "targetVisit", message.get("type").getAsString());
                    Assert.assertTrue("targetVisit updates should have a 'targetId' property", message.has("targetId"));
                    Assert.assertEquals("Incorrect target ID in update", "FarUp", message.get("targetId").getAsString());
                });
        long markerCountAtClaimedPos = shadowMap.getMarkers().stream().filter(m -> LatLngUtils.same(m.getPosition(), targetPos[7])).count();
        Assert.assertNotEquals("Claiming a target should not remove the marker", 0, markerCountAtClaimedPos);
        Assert.assertEquals("Claiming a target should not create duplicate markers", 1, markerCountAtClaimedPos);
        Marker marker = shadowMap.getMarkerAt(targetPos[7]);
        ShadowMarker shadowMarker = Shadow.extract(marker);
        Assert.assertEquals("Claiming a target should turn it the team color", BitmapDescriptorFactory.HUE_YELLOW, shadowMarker.getHue(), 1e-3);
        polylines = shadowMap.getPolylinesConnecting(targetPos[2], targetPos[7]);
        Assert.assertNotEquals("Claiming a target should add a polyline", 0, polylines.size());
        Assert.assertTrue("The new polyline does not have the team color", polylines.stream().anyMatch(p -> p.getColor() == yellow));

        // Extend a teammate's path
        Assert.assertTrue("TargetGame should handle playerTargetVisit",
                game.handleMessage(JsonHelper.updatePlayerTargetVisit("noone@illinois.edu", TeamID.TEAM_YELLOW, "FarDown"), "playerTargetVisit"));
        marker = shadowMap.getMarkerAt(targetPos[6]);
        shadowMarker = Shadow.extract(marker);
        Assert.assertEquals("A teammate claiming a target should turn it the team color", BitmapDescriptorFactory.HUE_YELLOW, shadowMarker.getHue(), 1e-3);
        polylines = shadowMap.getPolylinesConnecting(targetPos[4], targetPos[6]);
        Assert.assertNotEquals("A teammate claiming another target should create a polyline extending their path", 0, polylines.size());
        Assert.assertTrue("The polyline created when a teammate claims another target should be the team color",
                polylines.stream().anyMatch(p -> p.getColor() == yellow));

        // Extend another player's path
        game.handleMessage(JsonHelper.updatePlayerTargetVisit("opponent@example.com", TeamID.TEAM_RED, "Other1"), "playerTargetVisit");
        marker = shadowMap.getMarkerAt(targetPos[8]);
        shadowMarker = Shadow.extract(marker);
        Assert.assertEquals("Another player claiming a target should turn it that player's team color", BitmapDescriptorFactory.HUE_RED, shadowMarker.getHue(), 1e-3);
        polylines = shadowMap.getPolylinesConnecting(targetPos[5], targetPos[8]);
        Assert.assertNotEquals("Another player claiming another target should create a polyline", 0, polylines.size());
        int red = appContext.getColor(R.color.red);
        Assert.assertTrue("The polyline created when another player claims a target should have that player's team color",
                polylines.stream().anyMatch(p -> p.getColor() == red));

        // Start yet another player's path
        int polylinesCount = shadowMap.getPolylines().size();
        game.handleMessage(JsonHelper.updatePlayerTargetVisit("another@example.com", TeamID.TEAM_GREEN, "Other2"), "playerTargetVisit");
        marker = shadowMap.getMarkerAt(targetPos[9]);
        shadowMarker = Shadow.extract(marker);
        Assert.assertEquals("Another player claiming a target should turn it that player's team color", BitmapDescriptorFactory.HUE_GREEN, shadowMarker.getHue(), 1e-3);
        Assert.assertEquals("A player claiming their first target should not create a polyline", polylinesCount, shadowMap.getPolylines().size());

        // Try visiting a target captured by another team
        game.locationUpdated(targetPos[8]); // Other1
        webSocketControl.assertNoMessagesMatch("Visiting a target claimed by a different team should do nothing", EXCEPT_LOCATION_UPDATE);
        polylines = shadowMap.getPolylinesConnecting(targetPos[7], targetPos[8]);
        Assert.assertEquals("Trying to visit a target claimed by another team should not create a polyline", 0, polylines.size());

        // Test a newly started game
        targets = gameConfig.getAsJsonArray("targets");
        for (JsonElement t : targets) {
            JsonObject target = t.getAsJsonObject();
            target.addProperty("team", TeamID.OBSERVER);
        }
        for (JsonElement p : gameConfig.getAsJsonArray("players")) {
            JsonObject player = p.getAsJsonObject();
            player.add("path", new JsonArray());
        }
        map = MockedWrapperInstantiator.create(GoogleMap.class);
        shadowMap = Shadow.extract(map);
        webSocketControl = new WebSocketMocker();
        game = new TargetGame(SampleData.USER_EMAIL, map, webSocketControl.getWebSocket(), gameConfig.deepCopy(), appContext);
        Assert.assertEquals("TargetGame should create one marker per target", targets.size(), shadowMap.getMarkers().size());
        Assert.assertEquals("No targets should be claimed yet",
                targets.size(), shadowMap.getMarkersWithColor(BitmapDescriptorFactory.HUE_VIOLET).size());
        Assert.assertEquals("There should be no paths when no targets have been captured", 0, shadowMap.getPolylines().size());
        game.handleMessage(JsonHelper.updatePlayerTargetVisit("noone@illinois.edu", TeamID.TEAM_YELLOW, "LowerLeft"), "playerTargetVisit");
        marker = shadowMap.getMarkerAt(new LatLng(40.106388, -88.227814)); // LowerLeft
        Assert.assertEquals("A teammate capturing a target should turn it the team color",
                BitmapDescriptorFactory.HUE_YELLOW, Shadow.<ShadowMarker>extract(marker).getHue(), 1e-3);
        Assert.assertEquals("There should be no connecting lines yet", 0, shadowMap.getPolylines().size());
        LatLng upperLeft = new LatLng(40.108765, -88.227945); // UpperLeft
        game.locationUpdated(upperLeft);
        webSocketControl.processOneMessage("Visiting a target should capture it", EXCEPT_LOCATION_UPDATE, message -> {
            Assert.assertEquals("Incorrect message type", "targetVisit", message.get("type").getAsString());
            Assert.assertEquals("Incorrect target ID in update", "UpperLeft", message.get("targetId").getAsString());
        });
        shadowMarker = Shadow.extract(shadowMap.getMarkerAt(upperLeft));
        Assert.assertEquals("The user capturing a target should turn it the team color",
                BitmapDescriptorFactory.HUE_YELLOW, shadowMarker.getHue(), 1e-3);
        Assert.assertEquals("Captures by different players should be not connected by lines", 0, shadowMap.getPolylines().size());
        game.handleMessage(JsonHelper.updatePlayerTargetVisit("opponent@example.com", TeamID.TEAM_RED, "UpperMiddle"), "playerTargetVisit");
        marker = shadowMap.getMarkerAt(new LatLng(40.108930, -88.226455)); // UpperMiddle
        Assert.assertEquals("A player capturing a target should turn it that player's team color",
                BitmapDescriptorFactory.HUE_RED, Shadow.<ShadowMarker>extract(marker).getHue(), 1e-3);
        Assert.assertEquals("A player's first capture should not create a line", 0, shadowMap.getPolylines().size());
    }

    @Test(timeout = 60000)
    @Graded(points = 25)
    public void testMultiplePlayersAreaMode() {
        // Create the game
        FirebaseMocker.setBan("AreaGame should use the Game getEmail instance method rather than asking FirebaseAuth");
        GoogleMap map = MockedWrapperInstantiator.create(GoogleMap.class);
        ShadowGoogleMap shadowMap = Shadow.extract(map);
        WebSocketMocker webSocketControl = new WebSocketMocker();
        JsonObject gameConfig = SampleData.createAreaModeTestGame();
        Game game = new AreaGame(SampleData.USER_EMAIL, map, webSocketControl.getWebSocket(), gameConfig.deepCopy(), appContext);

        // Make sure it only handles known events
        Assert.assertFalse("AreaGame should not handle target mode events",
                game.handleMessage(JsonHelper.updatePlayerTargetVisit("noone@illinois.edu", TeamID.TEAM_RED, RandomHelper.randomId()), "playerTargetVisit"));
        JsonObject nonsenseUpdate = new JsonObject();
        nonsenseUpdate.addProperty("type", "nonsense");
        Assert.assertFalse("AreaGame should not handle unknown events", game.handleMessage(nonsenseUpdate, "nonsense"));

        // Check initial polygons
        int originalPolygonCount = shadowMap.getPolygons().size();
        Assert.assertTrue(originalPolygonCount < 10);
        LatLngBounds playerStartCell = new LatLngBounds(new LatLng(40.1135878, -88.22536167), new LatLng(40.1142706, -88.22446883));
        List<Polygon> polygons = shadowMap.getPolygonsFilling(playerStartCell); // (4, 1)
        Assert.assertNotEquals("No polygon shows the cell captured by the player", 0, polygons.size());
        int red = appContext.getColor(R.color.red);
        Assert.assertTrue("The polygon for the cell captured by the player should be the player's team color",
                polygons.stream().anyMatch(p -> solidColorOf(p) == red));
        LatLngBounds playerSecondCell = new LatLngBounds(new LatLng(40.1135878, -88.2262545), new LatLng(40.1142706, -88.22536167));
        Assert.assertNull("There should be no polygon in uncaptured cells", shadowMap.getPolygonFilling(playerSecondCell));
        List<Pair<LatLngBounds, Integer>> expectedCells = new ArrayList<>();
        expectedCells.add(new Pair<>(new LatLngBounds(new LatLng(40.1149534, -88.2262545), new LatLng(40.1156362, -88.22536167)), R.color.red)); // (3, 3)
        expectedCells.add(new Pair<>(new LatLngBounds(new LatLng(40.1149534, -88.22536167), new LatLng(40.1156362, -88.22446883)), R.color.red)); // (4, 3)
        expectedCells.add(new Pair<>(new LatLngBounds(new LatLng(40.1149534, -88.228933), new LatLng(40.1156362, -88.228040167)), R.color.yellow)); // (0, 3)
        expectedCells.add(new Pair<>(new LatLngBounds(new LatLng(40.1149534, -88.228040167), new LatLng(40.1156362, -88.2271473)), R.color.yellow)); // (1, 3)
        expectedCells.add(new Pair<>(new LatLngBounds(new LatLng(40.1142706, -88.228040167), new LatLng(40.1149534, -88.2271473)), R.color.yellow)); // (1, 2)
        expectedCells.add(new Pair<>(new LatLngBounds(new LatLng(40.112905, -88.228933), new LatLng(40.1135878, -88.228040167)), R.color.blue)); // (0, 0)
        for (Pair<LatLngBounds, Integer> pair : expectedCells) {
            Polygon polygon = shadowMap.getPolygonFilling(pair.first);
            Assert.assertNotNull("AreaGame did not create/position polygons on captured cells", polygon);
            int expectedColor = appContext.getColor(pair.second);
            Assert.assertEquals("Captured cell polygons should be the capturing team's color", expectedColor, solidColorOf(polygon));
        }

        // Try to capture a non-adjacent cell
        game.locationUpdated(new LatLng(40.1141, -88.2263)); // (2, 1)
        webSocketControl.assertNoMessagesMatch("Visiting a cell far away from the path should do nothing", EXCEPT_LOCATION_UPDATE);

        // Capture an adjacent cell
        game.locationUpdated(new LatLng(40.1136, -88.2258)); // (3, 1)
        webSocketControl.processOneMessage("Visiting a capturable cell should produce an event", EXCEPT_LOCATION_UPDATE,
                message -> {
                    Assert.assertEquals("Incorrect message type for cell capture", "cellCapture", message.get("type").getAsString());
                    Assert.assertTrue("cellCapture updates should have an 'x' property", message.has("x"));
                    Assert.assertEquals("Incorrect X coordinate in cell capture update", 3, message.get("x").getAsInt());
                    Assert.assertTrue("cellCapture updates should have a 'y' property", message.has("y"));
                    Assert.assertEquals("Incorrect Y coordinate in cell capture update", 1, message.get("y").getAsInt());
                });
        polygons = shadowMap.getPolygonsFilling(playerSecondCell);
        Assert.assertNotEquals("Capturing a cell should add a polygon", 0, polygons.size());
        Assert.assertEquals("Capturing a cell should add exactly one polygon", originalPolygonCount + 1, shadowMap.getPolygons().size());
        Assert.assertTrue("The added polygon should have the team color", polygons.stream().anyMatch(p -> solidColorOf(p) == red));
        Polygon polygon = shadowMap.getPolygonFilling(playerStartCell);
        Assert.assertNotNull("The previously captured cell should still have a polygon", polygon);
        Assert.assertEquals("The previously captured cell's polygon should still have the team color", red, solidColorOf(polygon));

        // Try to capture a cell adjacent to the starting point
        game.locationUpdated(new LatLng(40.1132, -88.2245)); // (4, 0)
        webSocketControl.assertNoMessagesMatch("Visiting a cell that doesn't share a side with the most recent capture should do nothing", EXCEPT_LOCATION_UPDATE);

        // Try moving outside the area entirely
        game.locationUpdated(new LatLng(40.1121, -88.2246)); // (4, <0)
        webSocketControl.assertNoMessagesMatch("Going outside the bounds of the game should do nothing", EXCEPT_LOCATION_UPDATE);

        // Start another player's path
        Assert.assertTrue("AreaGame should handle playerCellCapture",
                game.handleMessage(JsonHelper.updatePlayerCellCapture("late@example.com", TeamID.TEAM_GREEN, 3, 0), "playerCellCapture"));
        polygon = shadowMap.getPolygonFilling(new LatLngBounds(new LatLng(40.112905, -88.2262545), new LatLng(40.1135878, -88.22536167)));
        Assert.assertNotNull("Another player capturing a cell should create a polygon", polygon);
        Assert.assertEquals("Polygons for cells captured by other teams should have the capturing team's color",
                appContext.getColor(R.color.green), solidColorOf(polygon));

        // Try to capture a cell taken by another team
        game.locationUpdated(new LatLng(40.1131, -88.2255)); // (3, 0)
        webSocketControl.assertNoMessagesMatch("Visiting a cell captured by another team should do nothing", EXCEPT_LOCATION_UPDATE);

        // Try revisiting a previously captured cell
        game.locationUpdated(new LatLng(40.1140, -88.2245)); // (4, 1)
        webSocketControl.assertNoMessagesMatch("Revisiting a captured cell should do nothing", EXCEPT_LOCATION_UPDATE);

        // Try to capture a cell adjacent to a teammate's most recent capture
        game.locationUpdated(new LatLng(40.1158, -88.2249)); // (4, 4)
        webSocketControl.assertNoMessagesMatch("Visiting a cell adjacent to a teammate's (but not one's own) most recent capture should do nothing", EXCEPT_LOCATION_UPDATE);

        // Capture another cell
        game.locationUpdated(new LatLng(40.1142, -88.2264)); // (2, 1)
        webSocketControl.processOneMessage("Visiting a capturable cell should produce an event", EXCEPT_LOCATION_UPDATE,
                message -> {
                    Assert.assertEquals("Incorrect message type for cell capture", "cellCapture", message.get("type").getAsString());
                    Assert.assertEquals("Incorrect X coordinate in cell capture update", 2, message.get("x").getAsInt());
                    Assert.assertEquals("Incorrect Y coordinate in cell capture update", 1, message.get("y").getAsInt());
                });
        polygons = shadowMap.getPolygonsFilling(new LatLngBounds(new LatLng(40.1135878, -88.2271473), new LatLng(40.1142706, -88.2262545)));
        Assert.assertNotEquals("Capturing another cell should create another polygon", 0, polygons.size());
        Assert.assertTrue("The added polygon should have the team color", polygons.stream().anyMatch(p -> solidColorOf(p) == red));

        // Extend another player's path
        LatLngBounds blueCaptureBounds = new LatLngBounds(new LatLng(40.1135878, -88.228933), new LatLng(40.1142706, -88.228040167));
        polygon = shadowMap.getPolygonFilling(blueCaptureBounds);
        Assert.assertNull(polygon);
        game.handleMessage(JsonHelper.updatePlayerCellCapture("another@example.com", TeamID.TEAM_BLUE, 0, 1), "playerCellCapture");
        polygon = shadowMap.getPolygonFilling(blueCaptureBounds);
        Assert.assertNotNull("Other players capturing additional cells should create additional polygons", polygon);
        Assert.assertEquals("Polygons for cells captured by other teams should have the capturing team's color",
                appContext.getColor(R.color.blue), solidColorOf(polygon));
    }

    @Test(timeout = 60000)
    @Graded(points = 10)
    public void testScoring() {
        // Exercise TargetGame#getTeamScore directly
        FirebaseMocker.setBan("TargetGame should use the Game getEmail instance method rather than asking FirebaseAuth");
        WebSocketMocker webSocketControl = new WebSocketMocker();
        Game game = new TargetGame(SampleData.USER_EMAIL, MockedWrapperInstantiator.create(GoogleMap.class),
                webSocketControl.getWebSocket(), SampleData.createTargetModeTestGame(), appContext);
        final String initialTargetScoreMessage = "TargetGame's getTeamScore didn't reflect the game state provided by the server";
        Assert.assertEquals(initialTargetScoreMessage, 1, game.getTeamScore(TeamID.TEAM_RED));
        Assert.assertEquals(initialTargetScoreMessage, 5, game.getTeamScore(TeamID.TEAM_YELLOW));
        Assert.assertEquals(initialTargetScoreMessage, 0, game.getTeamScore(TeamID.TEAM_GREEN));
        Assert.assertEquals(initialTargetScoreMessage, 0, game.getTeamScore(TeamID.TEAM_BLUE));
        JsonObject greenCapture1 = JsonHelper.updatePlayerTargetVisit("another@example.com", TeamID.TEAM_GREEN, "Other1");
        final String updatedTargetScoreMessage = "TargetGame's getTeamScore was incorrect after another player captured a target";
        game.handleMessage(greenCapture1.deepCopy(), "playerTargetVisit");
        Assert.assertEquals(updatedTargetScoreMessage, 0, game.getTeamScore(TeamID.TEAM_BLUE));
        Assert.assertEquals(updatedTargetScoreMessage, 1, game.getTeamScore(TeamID.TEAM_RED));
        Assert.assertEquals(updatedTargetScoreMessage, 1, game.getTeamScore(TeamID.TEAM_GREEN));
        Assert.assertEquals(updatedTargetScoreMessage, 5, game.getTeamScore(TeamID.TEAM_YELLOW));
        JsonObject greenCapture2 = greenCapture1.deepCopy();
        greenCapture2.addProperty("targetId", "Other2");
        final String secondUpdatedTargetScoreMessage = "TargetGame's getTeamScore was incorrect after another player captured another target";
        game.handleMessage(greenCapture2.deepCopy(), "playerTargetVisit");
        Assert.assertEquals(secondUpdatedTargetScoreMessage, 2, game.getTeamScore(TeamID.TEAM_GREEN));
        Assert.assertEquals(secondUpdatedTargetScoreMessage, 1, game.getTeamScore(TeamID.TEAM_RED));
        Assert.assertEquals(secondUpdatedTargetScoreMessage, 0, game.getTeamScore(TeamID.TEAM_BLUE));
        Assert.assertEquals(secondUpdatedTargetScoreMessage, 5, game.getTeamScore(TeamID.TEAM_YELLOW));
        FirebaseMocker.setBan(null);

        // Create an activity with a target mode game
        webSocketControl = WebSocketMocker.expectConnection();
        GameActivityLauncher launcher = new GameActivityLauncher();
        GameActivity activity = launcher.getActivity();
        webSocketControl.sendData(SampleData.createTargetModeTestGame());
        @IdRes int rIdGameScores = IdLookup.require("gameScores");
        TextView scoresLabel = activity.findViewById(rIdGameScores);
        Assert.assertEquals("The scores label should be visible once game data is loaded", View.VISIBLE, scoresLabel.getVisibility());
        Assert.assertTrue("The scores label should show the scores of teams that captured targets", scoresLabel.getText().toString().contains("Yellow: 5"));
        Assert.assertTrue("The scores label should show the scores of teams that captured targets", scoresLabel.getText().toString().contains("Red: 1"));

        // Send some events
        webSocketControl.sendData(greenCapture2.deepCopy());
        Assert.assertTrue("The scores label should update after a target is captured", scoresLabel.getText().toString().contains("Green: 1"));
        launcher.sendLocationUpdate(new LatLng(40.106466, -88.226318)); // LowerMiddle
        Assert.assertTrue("Revisiting a target should not affect the score", scoresLabel.getText().toString().contains("Yellow: 5"));
        launcher.sendLocationUpdate(new LatLng(40.111915, -88.226418)); // FarUp
        Assert.assertTrue("The user's captures should affect the score", scoresLabel.getText().toString().contains("Yellow: 6"));

        // Exercise AreaGame#getTeamScore directly
        FirebaseMocker.setBan("AreaGame should use the Game getEmail instance method rather than asking FirebaseAuth");
        webSocketControl = new WebSocketMocker();
        game = new AreaGame(SampleData.USER_EMAIL, MockedWrapperInstantiator.create(GoogleMap.class),
                webSocketControl.getWebSocket(), SampleData.createAreaModeTestGame(), appContext);
        final String initialAreaScoreMessage = "AreaGame's getTeamScore didn't reflect the game state provided by the server";
        Assert.assertEquals(initialAreaScoreMessage, 3, game.getTeamScore(TeamID.TEAM_RED));
        Assert.assertEquals(initialAreaScoreMessage, 3, game.getTeamScore(TeamID.TEAM_YELLOW));
        Assert.assertEquals(initialAreaScoreMessage, 0, game.getTeamScore(TeamID.TEAM_GREEN));
        Assert.assertEquals(initialAreaScoreMessage, 1, game.getTeamScore(TeamID.TEAM_BLUE));
        JsonObject redCapture = JsonHelper.updatePlayerCellCapture("noone@illinois.edu", TeamID.TEAM_RED, 4, 2);
        final String updatedAreaScoreMessage = "AreaGame's getTeamScore was incorrect after another player captured a cell";
        game.handleMessage(redCapture.deepCopy(), "playerCellCapture");
        Assert.assertEquals(updatedAreaScoreMessage, 0, game.getTeamScore(TeamID.TEAM_GREEN));
        Assert.assertEquals(updatedAreaScoreMessage, 1, game.getTeamScore(TeamID.TEAM_BLUE));
        Assert.assertEquals(updatedAreaScoreMessage, 3, game.getTeamScore(TeamID.TEAM_YELLOW));
        Assert.assertEquals(updatedAreaScoreMessage, 4, game.getTeamScore(TeamID.TEAM_RED));
        final String secondUpdatedAreaScoreMessage = "AreaGame's getTeamScore was incorrect after a different team captured a cell";
        game.handleMessage(JsonHelper.updatePlayerCellCapture("opponent@illinois.edu", TeamID.TEAM_YELLOW, 1, 1), "playerCellCapture");
        Assert.assertEquals(secondUpdatedAreaScoreMessage, 4, game.getTeamScore(TeamID.TEAM_YELLOW));
        Assert.assertEquals(secondUpdatedAreaScoreMessage, 1, game.getTeamScore(TeamID.TEAM_BLUE));
        Assert.assertEquals(secondUpdatedAreaScoreMessage, 0, game.getTeamScore(TeamID.TEAM_GREEN));
        Assert.assertEquals(secondUpdatedAreaScoreMessage, 4, game.getTeamScore(TeamID.TEAM_RED));
        FirebaseMocker.setBan(null);

        // Create an activity with an area mode game
        webSocketControl = WebSocketMocker.expectConnection();
        launcher = new GameActivityLauncher();
        activity = launcher.getActivity();
        webSocketControl.sendData(SampleData.createAreaModeTestGame());
        scoresLabel = activity.findViewById(rIdGameScores);
        Assert.assertTrue("The scores label should show the scores of teams that captured cells", scoresLabel.getText().toString().contains("Red: 3"));
        Assert.assertTrue("The scores label should show the scores of teams that captured cells", scoresLabel.getText().toString().contains("Yellow: 3"));
        Assert.assertTrue("The scores label should show the scores of teams that captured cells", scoresLabel.getText().toString().contains("Blue: 1"));

        // Send some events
        webSocketControl.sendData(redCapture);
        Assert.assertTrue("The scores label should update after a cell is captured", scoresLabel.getText().toString().contains("Red: 4"));
        launcher.sendLocationUpdate(new LatLng(40.1135, -88.2246)); // (4, 0)
        Assert.assertTrue("The user's cell captures should affect the score label", scoresLabel.getText().toString().contains("Red: 5"));

        // Make sure the user's movements do nothing when the game is paused
        webSocketControl.processMessages(ignored -> { });
        webSocketControl.sendData(JsonHelper.updateGameState(GameStateID.PAUSED));
        launcher.sendLocationUpdate(new LatLng(40.1134, -88.2244)); // (5, 0)
        Assert.assertFalse("The user's movements should not affect the score when the game is paused", scoresLabel.getText().toString().contains("Red: 6"));
        Assert.assertTrue("The user's movements should not affect the score when the game is paused", scoresLabel.getText().toString().contains("Red: 5"));
        webSocketControl.assertNoMessagesMatch("The user's movements should do nothing when the game is paused", EXCEPT_LOCATION_UPDATE);
    }

    @Test(timeout = 60000)
    @Graded(points = 5)
    public void testGameOver() {
        // Set up the activity
        WebSocketMocker webSocketControl = WebSocketMocker.expectConnection();
        GameActivityLauncher launcher = new GameActivityLauncher();
        webSocketControl.sendData(SampleData.createTargetModeTestGame());
        ShadowDialog.reset();

        // Send the game-over update
        webSocketControl.sendData(JsonHelper.updateGameState(GameStateID.ENDED));
        String message = getGameOverPopupMessage();
        Assert.assertTrue("The game-over popup should state the winning team", message.contains("Yellow wins"));
        Assert.assertFalse(message.contains("Red wins"));
        Assert.assertFalse(message.contains("Green wins"));
        Assert.assertFalse(message.contains("Blue wins"));

        // Start and end several games with different winners
        String[] teamNames = appContext.getResources().getStringArray(R.array.team_choices);
        for (int i = 0; i < 10; i++) {
            launcher = new GameActivityLauncher();
            int winningTeam = RandomHelper.randomTeam();
            JsonObject fullUpdate = SampleData.createTargetModeTestGame();
            for (JsonElement p : fullUpdate.getAsJsonArray("players")) {
                p.getAsJsonObject().addProperty("team", winningTeam);
            }
            for (JsonElement t : fullUpdate.getAsJsonArray("targets")) {
                JsonObject target = t.getAsJsonObject();
                if (target.get("team").getAsInt() != TeamID.OBSERVER) {
                    target.addProperty("team", winningTeam);
                }
            }
            webSocketControl.sendData(fullUpdate);
            ShadowDialog.reset();
            webSocketControl.sendData(JsonHelper.updateGameState(GameStateID.ENDED));
            message = getGameOverPopupMessage();
            Assert.assertTrue("The game-over popup should name the winning team", message.contains(teamNames[winningTeam] + " wins"));
            for (int team = TeamID.MIN_TEAM; team <= TeamID.MAX_TEAM; team++) {
                if (team != winningTeam) {
                    Assert.assertFalse("The game-over popup should not name losing teams", message.contains(teamNames[team]));
                }
            }
        }

        // Dismiss the dialog
        ShadowDialog.getLatestDialog().dismiss();
        Assert.assertTrue("The activity should finish() after the game-over popup is dismissed", launcher.getActivity().isFinishing());
    }

    private String getGameOverPopupMessage() {
        Dialog dialog = ShadowDialog.getLatestDialog();
        Assert.assertNotNull("A popup should appear to announce the winner when the game ends", dialog);
        TextView messageView = dialog.findViewById(android.R.id.message);
        Assert.assertNotNull("The game-over popup should have a message", messageView);
        Assert.assertEquals("The game-over popup should show a message", View.VISIBLE, messageView.getVisibility());
        return messageView.getText().toString();
    }

    private int solidColorOf(Polygon polygon) {
        return polygon.getFillColor() | (0xFF << 24);
    }

}
