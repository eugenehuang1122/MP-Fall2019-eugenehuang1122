package edu.illinois.cs.cs125.fall2019.mp;

import android.app.Dialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;

import androidx.annotation.IdRes;
import androidx.test.core.app.ApplicationProvider;

import com.android.volley.Request;
import com.android.volley.Response;
import com.firebase.ui.auth.AuthUI;
import com.google.firebase.auth.FirebaseAuth;
import com.google.gson.JsonArray;
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
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.Shadows;
import org.robolectric.shadows.ShadowActivity;
import org.robolectric.shadows.ShadowDialog;
import org.robolectric.shadows.ShadowPackageManager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.IntStream;

import edu.illinois.cs.cs125.gradlegrader.annotations.Graded;
import edu.illinois.cs.cs125.robolectricsecurity.PowerMockSecurity;
import edu.illinois.cs.cs125.robolectricsecurity.Trusted;

@RunWith(RobolectricTestRunner.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@PowerMockIgnore({"org.mockito.*", "org.powermock.*", "org.robolectric.*", "android.*", "androidx.*", "com.google.android.*", "edu.illinois.cs.cs125.fall2019.mp.shadows.*"})
@PrepareForTest({WebApi.class, FirebaseAuth.class, AuthUI.class})
@Trusted
public class Checkpoint2Test {

    @Rule
    public PowerMockRule mockStaticClasses = new PowerMockRule();

    @Before
    public void setup() {
        PowerMockSecurity.secureMockMethodCache();
        FirebaseMocker.mock();
        FirebaseMocker.setEmail(null);
        WebApiMocker.interceptHttp();
    }

    @After
    public void teardown() {
        WebApiMocker.reset();
    }

    @Test(timeout = 60000)
    @Graded(points = 5)
    public void testManifest() throws PackageManager.NameNotFoundException {
        // Make sure LaunchActivity is the startup activity
        Context appContext = ApplicationProvider.getApplicationContext();
        ShadowPackageManager packageManager = Shadows.shadowOf(appContext.getPackageManager());
        List<IntentFilter> filters = packageManager.getIntentFiltersForActivity(new ComponentName(appContext, LaunchActivity.class));
        Assert.assertEquals("LaunchActivity should have an <intent-filter> section in the manifest", 1, filters.size());
        IntentFilter filter = filters.get(0);
        Assert.assertNotEquals("LaunchActivity's <intent-filter> should specify the MAIN action", 0, filter.countActions());
        Assert.assertTrue("LaunchActivity should have the MAIN action in the manifest",
                IntStream.range(0, filter.countActions()).anyMatch(i -> filter.getAction(i).equals("android.intent.action.MAIN")));

        // Make sure MainActivity is no longer the startup activity
        filters = packageManager.getIntentFiltersForActivity(new ComponentName(appContext, MainActivity.class));
        Assert.assertEquals("MainActivity should no longer have an <intent-filter> section in the manifest", 0, filters.size());
    }

    @Test(timeout = 60000)
    @Graded(points = 20)
    public void testLaunchActivity() {
        // Start the app without having logged in
        FirebaseMocker.mockAuthUI();
        LaunchActivity activity = Robolectric.buildActivity(LaunchActivity.class).create().start().resume().get();
        Assert.assertFalse("LaunchActivity should not finish until a user has signed in", activity.isFinishing());
        ShadowActivity.IntentForResult ifr = Shadows.shadowOf(activity).getNextStartedActivityForResult();
        Assert.assertNotNull("LaunchActivity should start a Firebase Auth signin UI intent " +
                "(with startActivityForResult) if the user isn't logged in", ifr);
        Assert.assertTrue("The login intent should have been created by a SignInIntentBuilder",
                ifr.intent.hasExtra(FirebaseMocker.UI_INTENT_PROPERTY));
        Assert.assertEquals(FirebaseMocker.UI_INTENT_TOKEN, ifr.intent.getStringExtra(FirebaseMocker.UI_INTENT_PROPERTY));
        Shadows.shadowOf(activity).clearNextStartedActivities();

        // Cancel login
        Shadows.shadowOf(activity).callOnActivityResult(ifr.requestCode, LaunchActivity.RESULT_CANCELED, new Intent());
        Assert.assertFalse("LaunchActivity should not finish if the user cancels signin", activity.isFinishing());
        Assert.assertNull("LaunchActivity should not immediately retry signin if the user cancels",
                Shadows.shadowOf(activity).getNextStartedActivityForResult());
        Assert.assertNull("LaunchActivity should not launch another activity if the user cancels signin",
                Shadows.shadowOf(activity).getNextStartedActivity());

        // Press the button to retry login
        Button goLogin = activity.findViewById(IdLookup.require("goLogin"));
        Assert.assertEquals("The button to relaunch login should be visible after a canceled attempt",
                View.VISIBLE, goLogin.getVisibility());
        goLogin.performClick();
        ifr = Shadows.shadowOf(activity).getNextStartedActivityForResult();
        Assert.assertNotNull("Pressing the button should relaunch the Firebase Auth UI", ifr);
        Assert.assertEquals("The intent should have been created by a SignInIntentBuilder",
                FirebaseMocker.UI_INTENT_TOKEN, ifr.intent.getStringExtra(FirebaseMocker.UI_INTENT_PROPERTY));

        // Log in
        FirebaseMocker.setEmail(SampleData.USER_EMAIL);
        Shadows.shadowOf(activity).callOnActivityResult(ifr.requestCode, LaunchActivity.RESULT_OK, new Intent());
        Intent intent = Shadows.shadowOf(activity).getNextStartedActivity();
        Assert.assertNotNull("LaunchActivity should start the main activity once the user is logged in", intent);
        Assert.assertEquals("LaunchActivity should start MainActivity once the user is logged in",
                new ComponentName(activity, MainActivity.class), intent.getComponent());
        Assert.assertTrue("LaunchActivity should finish() once the user is logged in", activity.isFinishing());

        // Start the app when the user is already logged in
        activity = Robolectric.buildActivity(LaunchActivity.class).create().start().resume().get();
        intent = Shadows.shadowOf(activity).getNextStartedActivity();
        Assert.assertNotNull("LaunchActivity should immediately launch the main activity if the user is logged in", intent);
        Assert.assertEquals("LaunchActivity should immediately start MainActivity if the user is already logged in",
                new ComponentName(activity, MainActivity.class), intent.getComponent());
        Assert.assertTrue("LaunchActivity should immediately finish() if the user is already logged in", activity.isFinishing());
    }

    @Test(timeout = 60000)
    @Graded(points = 25)
    public void testGamesLists() {
        // Get IDs
        @IdRes int rIdInvitationsGroup = IdLookup.require("invitationsGroup");
        @IdRes int rIdInvitationsList = IdLookup.require("invitationsList");
        @IdRes int rIdOngoingGamesGroup = IdLookup.require("ongoingGamesGroup");
        @IdRes int rIdOngoingGamesList = IdLookup.require("ongoingGamesList");

        // Check UI
        FirebaseMocker.setEmail(SampleData.USER_EMAIL);
        MainActivity activity = Robolectric.buildActivity(MainActivity.class).create().start().resume().get();
        Assert.assertNull("MainActivity should not launch any another activity immediately",
                Shadows.shadowOf(activity).getNextStartedActivity());
        ViewGroup invitationsGroup = activity.findViewById(rIdInvitationsGroup);
        Assert.assertEquals("The invitations group should be gone until data is loaded",
                View.GONE, invitationsGroup.getVisibility());
        Assert.assertNotNull("The invitations group should contain an Invitations label",
                findViewWithText(invitationsGroup, "Invitations"));
        LinearLayout invitationsList = activity.findViewById(rIdInvitationsList);
        Assert.assertEquals("The invitations list should be inside the invitations group",
                invitationsGroup, invitationsList.getParent());
        Assert.assertEquals("The invitations list LinearLayout should be vertical",
                LinearLayout.VERTICAL, invitationsList.getOrientation());
        ViewGroup ongoingGamesGroup = activity.findViewById(rIdOngoingGamesGroup);
        Assert.assertEquals("The ongoing games group should be gone until data is loaded",
                View.GONE, ongoingGamesGroup.getVisibility());
        Assert.assertNotNull("The ongoing games group should contain an Ongoing Games label",
                findViewWithText(ongoingGamesGroup, "Ongoing Games"));
        LinearLayout ongoingGamesList = activity.findViewById(rIdOngoingGamesList);
        Assert.assertEquals("The ongoing games list should be inside the ongoing games group",
                ongoingGamesGroup, ongoingGamesList.getParent());
        Assert.assertEquals("The ongoing games list LinearLayout should be vertical",
                LinearLayout.VERTICAL, ongoingGamesList.getOrientation());

        // Populate the lists
        processGamesRequest(callback -> callback.onResponse(SampleData.createGamesResponse()));
        Assert.assertEquals("The invitations group should be visible when there are pending invitations",
                View.VISIBLE, invitationsGroup.getVisibility());
        Assert.assertEquals("The ongoing games group should be visible when there are ongoing games",
                View.VISIBLE, ongoingGamesGroup.getVisibility());

        // Check invitations list
        Assert.assertEquals("The invitations list should have an entry for each pending invitation",
                2, invitationsList.getChildCount());
        ViewGroup gameInfoChunk = (ViewGroup) invitationsList.getChildAt(0);
        Assert.assertNotNull("Invitation entries should show the game creator's email",
                findViewWithText(gameInfoChunk, "someone.else@example.com"));
        Assert.assertNotNull("Invitation entries should show the role the user would have in the game",
                findViewWithText(gameInfoChunk, "Observer"));
        String[] teamNames = activity.getResources().getStringArray(R.array.team_choices);
        for (int i = TeamID.MIN_TEAM; i <= TeamID.MAX_TEAM; i++) {
            Assert.assertNull("Invitation entries should not mention roles the user doesn't have in the game",
                    findViewWithText(gameInfoChunk, teamNames[i]));
        }
        Assert.assertNotNull("Invitation entries should show the game mode", findViewWithText(gameInfoChunk, "target mode"));
        Assert.assertNull(findViewWithText(gameInfoChunk, "area"));
        gameInfoChunk = (ViewGroup) invitationsList.getChildAt(1);
        Assert.assertNotNull("Invitation entries should show the team the user would be in the game",
                findViewWithText(gameInfoChunk, "Green"));
        for (int i = 0; i < TeamID.TEAM_GREEN; i++) {
            Assert.assertNull("Invitation entries should not mention roles the user doesn't have in the game",
                    findViewWithText(gameInfoChunk, teamNames[i]));
        }
        Assert.assertNotNull("Invitation entries should show the game creator's email",
                findViewWithText(gameInfoChunk, "another@example.com"));
        Assert.assertNotNull("Invitation entries should show the game mode",
                findViewWithText(gameInfoChunk, "area mode"));
        Assert.assertNull(findViewWithText(gameInfoChunk, "target"));

        // Check ongoing games list
        Assert.assertNotEquals("Ended games shouldn't be shown in the ongoing games list",
                3, ongoingGamesList.getChildCount());
        Assert.assertEquals("The ongoing games list should have one entry per ongoing game",
                2, ongoingGamesList.getChildCount());
        gameInfoChunk = (ViewGroup) ongoingGamesList.getChildAt(0);
        Assert.assertNotNull("Ongoing game entries should show the user's team", findViewWithText(gameInfoChunk, "Red"));
        Assert.assertNull("Ongoing game entries should not show roles the user doesn't have",
                findViewWithText(gameInfoChunk, "Observer"));
        Assert.assertNotNull("Ongoing game entries should show the game mode", findViewWithText(gameInfoChunk, "target mode"));
        Assert.assertNull(findViewWithText(gameInfoChunk, "area"));
        gameInfoChunk = (ViewGroup) ongoingGamesList.getChildAt(1);
        Assert.assertNotNull("Ongoing game entries should show the user's role in the game",
                findViewWithText(gameInfoChunk, "Observer"));
        for (int i = TeamID.MIN_TEAM; i <= TeamID.MAX_TEAM; i++) {
            Assert.assertNull("Ongoing game entries should not mention teams the user isn't on",
                    findViewWithText(gameInfoChunk, teamNames[i]));
        }
        Assert.assertNotNull("Ongoing game entries should show the game creator's email",
                findViewWithText(gameInfoChunk, "yet.another@example.com"));
        Assert.assertNull(findViewWithText(gameInfoChunk, SampleData.USER_EMAIL));
        Assert.assertNotNull("Ongoing game entries should show the game mode",
                findViewWithText(gameInfoChunk, "area mode"));
        Assert.assertNull(findViewWithText(gameInfoChunk, "target"));

        // Try a response with no pending invitations (with a different user logged in)
        FirebaseMocker.setEmail("another@example.com");
        activity = Robolectric.buildActivity(MainActivity.class).create().start().resume().get();
        processGamesRequest(callback -> {
            JsonObject response = SampleData.createGamesResponse();
            JsonArray games = response.getAsJsonArray("games");
            while (games.size() > 1) {
                games.remove(0);
            }
            callback.onResponse(response);
        });
        invitationsGroup = activity.findViewById(rIdInvitationsGroup);
        Assert.assertEquals("The invitations group should be gone when there are no pending invitations",
                View.GONE, invitationsGroup.getVisibility());
        ongoingGamesGroup = activity.findViewById(rIdOngoingGamesGroup);
        Assert.assertEquals("The ongoing games group should be visible once data has been received",
                View.VISIBLE, ongoingGamesGroup.getVisibility());
        ongoingGamesList = activity.findViewById(rIdOngoingGamesList);
        Assert.assertEquals("The ongoing games list should have one entry per ongoing game",
                1, ongoingGamesList.getChildCount());
        gameInfoChunk = (ViewGroup) ongoingGamesList.getChildAt(0);
        Assert.assertNotNull("Ongoing game entries should show the user's role in the game",
                findViewWithText(gameInfoChunk, "Observer"));

        // Randomized test
        FirebaseMocker.setEmail(SampleData.USER_EMAIL);
        for (int i = 0; i < 15; i++) {
            // Decide response
            JsonObject response = new JsonObject();
            JsonObject[] games = new JsonObject[2];
            String invitedOwner = RandomHelper.randomEmail();
            String invitedMode = RandomHelper.randomMode();
            int invitedRole = RandomHelper.randomRole();
            games[0] = JsonHelper.game(RandomHelper.randomId(), invitedOwner, GameStateID.PAUSED, invitedMode,
                    JsonHelper.player(invitedOwner, RandomHelper.randomRole(), PlayerStateID.PLAYING),
                    JsonHelper.player(SampleData.USER_EMAIL, invitedRole, PlayerStateID.INVITED));
            String ongoingOwner = RandomHelper.randomEmail(invitedOwner);
            String ongoingMode = RandomHelper.randomMode();
            int ongoingRole = RandomHelper.randomRole();
            games[1] = JsonHelper.game(RandomHelper.randomId(), ongoingOwner, GameStateID.PAUSED, ongoingMode,
                    JsonHelper.player(ongoingOwner, RandomHelper.randomRole(), PlayerStateID.PLAYING),
                    JsonHelper.player(SampleData.USER_EMAIL, ongoingRole, PlayerStateID.ACCEPTED));
            Collections.shuffle(Arrays.asList(games));
            response.add("games", JsonHelper.arrayOf(games));

            // Start the activity
            activity = Robolectric.buildActivity(MainActivity.class).create().start().resume().get();
            processGamesRequest(callback -> callback.onResponse(response));
            invitationsList = activity.findViewById(rIdInvitationsList);
            ongoingGamesList = activity.findViewById(rIdOngoingGamesList);

            // Check UI
            Assert.assertEquals("The invitations list should have one entry per invitation",
                    1, invitationsList.getChildCount());
            gameInfoChunk = (ViewGroup) invitationsList.getChildAt(0);
            Assert.assertNotNull("Invitation entries should show the game creator's email",
                    findViewWithText(gameInfoChunk, invitedOwner));
            Assert.assertNotNull("Invitation entries should show the role the user would have in the game",
                    findViewWithText(gameInfoChunk, teamNames[invitedRole]));
            Assert.assertNotNull("Invitation entries should show the game mode",
                    findViewWithText(gameInfoChunk, invitedMode));
            Assert.assertEquals("The ongoing games list should have one entry per ongoing game",
                    1, ongoingGamesList.getChildCount());
            gameInfoChunk = (ViewGroup) ongoingGamesList.getChildAt(0);
            Assert.assertNotNull("Ongoing game entries should show the game creator's email",
                    findViewWithText(gameInfoChunk, ongoingOwner));
            Assert.assertNotNull("Ongoing game entries should show the role the user has in the game",
                    findViewWithText(gameInfoChunk, teamNames[ongoingRole]));
            Assert.assertNotNull("Ongoing game entries should show the game mode",
                    findViewWithText(gameInfoChunk, ongoingMode));
        }
    }

    @Test(timeout = 60000)
    @Graded(points = 20)
    public void testResponseButtons() {
        // Start the activity
        FirebaseMocker.setEmail(SampleData.USER_EMAIL);
        MainActivity activity = Robolectric.buildActivity(MainActivity.class).create().start().resume().get();
        JsonObject response = SampleData.createGamesResponse();
        processGamesRequest(callback -> callback.onResponse(response.deepCopy()));

        // Check initial list sizes (see testGamesLists)
        LinearLayout invitationsList = activity.findViewById(IdLookup.require("invitationsList"));
        LinearLayout ongoingGamesList = activity.findViewById(IdLookup.require("ongoingGamesList"));
        Assert.assertEquals("The invitations list should have one entry per pending invitation",
                2, invitationsList.getChildCount());
        Assert.assertEquals("The ongoing games list should have one entry per ongoing game",
                2, ongoingGamesList.getChildCount());

        // Decline an invitation
        ViewGroup gameInfoChunk = (ViewGroup) invitationsList.getChildAt(0);
        Button acceptButton = findButtonWithText(gameInfoChunk, "Accept");
        Assert.assertNotNull("Invitation entries should each have an Accept button", acceptButton);
        Assert.assertEquals("The Accept button should be visible", View.VISIBLE, acceptButton.getVisibility());
        Button declineButton = findButtonWithText(gameInfoChunk, "Decline");
        Assert.assertNotNull("Invitation entries should each have a Decline button", declineButton);
        Assert.assertEquals("The Decline button should be visible", View.VISIBLE, declineButton.getVisibility());
        Button enterButton = findButtonWithText(gameInfoChunk, "Enter");
        Assert.assertNull("Invitation entries should not have ongoing game controls", enterButton);
        Button leaveButton = findButtonWithText(gameInfoChunk, "Leave");
        Assert.assertNull("Invitation entries should not have ongoing game controls", leaveButton);
        ShadowDialog.reset();
        declineButton.performClick();
        Dialog dialog = ShadowDialog.getLatestDialog();
        if (dialog != null) {
            // If there's a confirmation dialog, press the positive button to confirm
            dialog.findViewById(android.R.id.button1).performClick();
        }
        String firstInviteId = response.getAsJsonArray("games").get(0).getAsJsonObject().get("id").getAsString();
        WebApiMocker.processOne("Clicking a Decline button should use the web API to decline the invitation",
                (path, method, body, callback, errorListener) -> {
                    Assert.assertEquals("Incorrect endpoint to decline invitation",
                            "/games/" + firstInviteId + "/decline", path);
                    Assert.assertEquals("Invitation responses should use POST requests", Request.Method.POST, method);
                    Assert.assertNull("Invitation responses should not have a body", body);
                    callback.onResponse(null);
                });
        response.getAsJsonArray("games").remove(0);
        processGamesRequest("The games lists should be fetched again after an invitation response request completes",
                callback -> callback.onResponse(response.deepCopy()));
        Assert.assertEquals("The invitations list should update after a response request completes",
                1, invitationsList.getChildCount());

        // Accept an invitation
        gameInfoChunk = (ViewGroup) invitationsList.getChildAt(0);
        acceptButton = findButtonWithText(gameInfoChunk, "Accept");
        Assert.assertNotNull("Invitation entries should each have an Accept button", acceptButton);
        declineButton = findButtonWithText(gameInfoChunk, "Decline");
        Assert.assertNotNull("Invitation entries should each have a Decline button", declineButton);
        acceptButton.performClick();
        String secondInviteId = response.getAsJsonArray("games").get(3).getAsJsonObject().get("id").getAsString();
        WebApiMocker.processOne("Clicking an Accept button should use the web API to accept the invitation",
                (path, method, body, callback, errorListener) -> {
                    Assert.assertEquals("Incorrect endpoint to accept invitation",
                            "/games/" + secondInviteId + "/accept", path);
                    Assert.assertEquals("Invitation responses should use POST requests", Request.Method.POST, method);
                    Assert.assertNull("Invitation responses should not have a body", body);
                    callback.onResponse(null);
                });
        response.getAsJsonArray("games").get(3).getAsJsonObject().getAsJsonArray("players")
                .get(2).getAsJsonObject().addProperty("state", PlayerStateID.ACCEPTED);
        processGamesRequest("The games lists should be fetched again after an invitation response request completes",
                callback -> callback.onResponse(response.deepCopy()));
        Assert.assertEquals("The ongoing games list should update after an invitation accept request completes",
                3, ongoingGamesList.getChildCount());

        // Check ongoing games buttons
        for (int i = 0; i < 3; i++) {
            gameInfoChunk = (ViewGroup) ongoingGamesList.getChildAt(i);
            acceptButton = findButtonWithText(gameInfoChunk, "Accept");
            Assert.assertNull("Ongoing games entries should not have invitation-related buttons", acceptButton);
            declineButton = findButtonWithText(gameInfoChunk, "Decline");
            Assert.assertNull("Ongoing games entries should not have invitation-related buttons", declineButton);
            enterButton = findButtonWithText(gameInfoChunk, "Enter");
            Assert.assertNotNull("Each ongoing games entry should have an Enter button", enterButton);
            Assert.assertEquals("The Enter button should be visible", View.VISIBLE, enterButton.getVisibility());
            leaveButton = findButtonWithText(gameInfoChunk, "Leave");
            if (i == 0) {
                if (leaveButton != null) {
                    Assert.assertNotEquals("Entries for ongoing games owned by the user should not have Leave buttons",
                            View.VISIBLE, leaveButton.getVisibility());
                }
            } else {
                Assert.assertNotNull("Ongoing game entries (not owned by the user) should have a Leave button", leaveButton);
                Assert.assertEquals("The Leave button for ongoing game entries not owned by the user should be visible",
                        View.VISIBLE, leaveButton.getVisibility());
            }
        }

        // Leave a game
        ShadowDialog.reset();
        leaveButton.performClick();
        dialog = ShadowDialog.getLatestDialog();
        if (dialog != null) {
            // If there's a confirmation dialog, press the positive button to confirm
            dialog.findViewById(android.R.id.button1).performClick();
        }
        WebApiMocker.processOne("Clicking a Leave button should use the web API to leave the game",
                (path, method, body, callback, errorListener) -> {
                    Assert.assertEquals("Incorrect API endpoint to leave game",
                            "/games/" + secondInviteId + "/leave", path);
                    Assert.assertEquals("Leave-game commands should use POST requests", Request.Method.POST, method);
                    Assert.assertNull("Leave-game commands should not have a body", body);
                    callback.onResponse(null);
                });
        response.getAsJsonArray("games").remove(3);
        processGamesRequest("The games lists should be fetched again after a leave-game request completes",
                callback -> callback.onResponse(response.deepCopy()));
        Assert.assertEquals("The ongoing games list should update after a leave-game request completes",
                2, ongoingGamesList.getChildCount());
    }

    @Test(timeout = 60000)
    @Graded(points = 10)
    public void testGameIntent() {
        // Start the activity
        FirebaseMocker.setEmail(SampleData.USER_EMAIL);
        MainActivity activity = Robolectric.buildActivity(MainActivity.class).create().start().resume().get();
        JsonObject response = SampleData.createGamesResponse();
        processGamesRequest(callback -> callback.onResponse(response.deepCopy()));
        LinearLayout ongoingGamesList = activity.findViewById(IdLookup.require("ongoingGamesList"));

        // Enter games
        for (int i = 0; i < 2; i++) {
            ViewGroup gameInfoChunk = (ViewGroup) ongoingGamesList.getChildAt(i);
            System.out.print(i);
            Button enterButton = findButtonWithText(gameInfoChunk, "Enter");
            Assert.assertNotNull("Ongoing game entries should each have an Enter button", enterButton);
            enterButton.performClick();
            Intent intent = Shadows.shadowOf(activity).getNextStartedActivity();
            Assert.assertNotNull("Pressing an ongoing game's Enter button should start the game activity", intent);
            Assert.assertEquals("Pressing Enter should launch GameActivity",
                    new ComponentName(activity, GameActivity.class), intent.getComponent());
            Assert.assertTrue("The intent should specify the game ID in the 'game' extra",
                    intent.hasExtra("game"));
            String gameId = response.getAsJsonArray("games").get(i + 1).getAsJsonObject().get("id").getAsString();
            Assert.assertEquals("Incorrect game ID in intent 'game' extra",
                    gameId, intent.getStringExtra("game"));
        }
    }

    private void processGamesRequest(String failMessage, Consumer<Response.Listener<JsonObject>> handler) {
        WebApiMocker.processOne(failMessage, (path, method, body, callback, errorListener) -> {
            Assert.assertEquals("Incorrect endpoint for game information", "/games", path);
            Assert.assertEquals("Game information should be fetched in a GET request", Request.Method.GET, method);
            Assert.assertNull("GET requests should not have a payload", body);
            handler.accept(callback);
        });
    }

    private void processGamesRequest(Consumer<Response.Listener<JsonObject>> handler) {
        processGamesRequest("MainActivity should immediately start a web request to fetch game information", handler);
    }

    private View findViewWithText(ViewGroup group, String text) {
        ArrayList<View> list = new ArrayList<>();
        group.findViewsWithText(list, text, View.FIND_VIEWS_WITH_TEXT);
        if (list.size() == 0) {
            return null;
        } else {
            return list.get(0);
        }
    }

    private Button findButtonWithText(ViewGroup group, String text) {
        ArrayList<View> list = new ArrayList<>();
        group.findViewsWithText(list, text, View.FIND_VIEWS_WITH_TEXT);
        for (View v : list) {
            if (v instanceof Button) return (Button) v;
        }
        return null;
    }

}
