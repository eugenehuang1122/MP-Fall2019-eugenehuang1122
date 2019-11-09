package edu.illinois.cs.cs125.fall2019.mp;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

public final class SampleData {

    static final String USER_EMAIL = "nobody@illinois.edu";

    private SampleData() { }

    static JsonObject createGamesResponse() {
        JsonObject response = new JsonObject();
        JsonArray games = new JsonArray();
        games.add(JsonHelper.game(RandomHelper.randomId(), "someone.else@example.com", GameStateID.PAUSED, "target",
                JsonHelper.player("someone.else@example.com", TeamID.TEAM_RED, PlayerStateID.ACCEPTED),
                JsonHelper.player(USER_EMAIL, TeamID.OBSERVER, PlayerStateID.INVITED)));
        games.add(JsonHelper.game(RandomHelper.randomId(), USER_EMAIL, GameStateID.PAUSED, "target",
                JsonHelper.player(USER_EMAIL, TeamID.TEAM_RED, PlayerStateID.ACCEPTED)));
        games.add(JsonHelper.game(RandomHelper.randomId(), "yet.another@example.com", GameStateID.RUNNING, "area",
                JsonHelper.player("yet.another@example.com", TeamID.TEAM_RED, PlayerStateID.PLAYING),
                JsonHelper.player(USER_EMAIL, TeamID.OBSERVER, PlayerStateID.ACCEPTED)));
        games.add(JsonHelper.game(RandomHelper.randomId(), USER_EMAIL, GameStateID.ENDED, "area",
                JsonHelper.player(USER_EMAIL, TeamID.TEAM_RED, PlayerStateID.ACCEPTED),
                JsonHelper.player("invitee@example.com", TeamID.OBSERVER, PlayerStateID.INVITED)));
        games.add(JsonHelper.game(RandomHelper.randomId(), "another@example.com", GameStateID.RUNNING, "area",
                JsonHelper.player("another@example.com", TeamID.OBSERVER, PlayerStateID.ACCEPTED),
                JsonHelper.player("third@example.com", TeamID.TEAM_YELLOW, PlayerStateID.REMOVED),
                JsonHelper.player(USER_EMAIL, TeamID.TEAM_GREEN, PlayerStateID.INVITED)));
        response.add("games", games);
        return response;
    }

    static JsonObject createTargetPresetsResponse() {
        JsonObject presets = new JsonObject();
        JsonObject empty = new JsonObject();
        empty.addProperty("name", "Empty");
        empty.add("targets", JsonHelper.arrayOf());
        JsonObject walmarts = new JsonObject();
        walmarts.addProperty("name", "Walmart Stores");
        walmarts.add("targets", JsonHelper.arrayOf(
                JsonHelper.position(40.146879, -88.254737),
                JsonHelper.position(40.048544, -88.254927),
                JsonHelper.position(40.111625, -88.159373)
        ));
        presets.add("presets", JsonHelper.arrayOf(empty, walmarts));
        return presets;
    }

    static JsonObject createMinimalTestGame(int userTeam) {
        JsonObject fullUpdate = new JsonObject();
        fullUpdate.addProperty("type", "full");
        fullUpdate.addProperty("owner", "someone.else@example.com");
        fullUpdate.addProperty("state", GameStateID.PAUSED);
        fullUpdate.addProperty("mode", "target");
        fullUpdate.addProperty("proximityThreshold", 10);
        JsonArray targets = new JsonArray();
        targets.add(JsonHelper.target(RandomHelper.randomId(), RandomHelper.randomLat(), -88.227208, TeamID.OBSERVER));
        fullUpdate.add("targets", targets);
        JsonArray players = new JsonArray();
        players.add(JsonHelper.player("someone.else@example.com", TeamID.OBSERVER, PlayerStateID.ACCEPTED));
        players.add(JsonHelper.player(USER_EMAIL, userTeam, PlayerStateID.ACCEPTED));
        fullUpdate.add("players", players);
        return fullUpdate;
    }

    static JsonObject createStateTestGame() {
        return createMinimalTestGame(TeamID.OBSERVER);
    }

    static JsonObject createTargetModeTestGame() {
        JsonObject llTarget = JsonHelper.target("LowerLeft", 40.106388, -88.227814, TeamID.TEAM_YELLOW);
        JsonObject ulTarget = JsonHelper.target("UpperLeft", 40.108765, -88.227945, TeamID.TEAM_YELLOW);
        JsonObject umTarget = JsonHelper.target("UpperMiddle", 40.108930, -88.226455, TeamID.TEAM_YELLOW);
        JsonObject lmTarget = JsonHelper.target("LowerMiddle", 40.106466, -88.226318, TeamID.TEAM_YELLOW);
        JsonObject lrTarget = JsonHelper.target("LowerRight", 40.106542, -88.224839, TeamID.TEAM_YELLOW);
        JsonObject urTarget = JsonHelper.target("UpperRight", 40.108952, -88.224957, TeamID.TEAM_RED);
        JsonObject fdTarget = JsonHelper.target("FarDown", 40.101994, -88.223845, TeamID.OBSERVER);
        JsonObject fuTarget = JsonHelper.target("FarUp", 40.111915, -88.226418, TeamID.OBSERVER);
        JsonObject o1Target = JsonHelper.target("Other1", 40.114507, -88.180760, TeamID.OBSERVER);
        JsonObject o2Target = JsonHelper.target("Other2", 40.114158, -88.173105, TeamID.OBSERVER);
        JsonObject o3Target = JsonHelper.target("Other3", 40.116287, -88.184009, TeamID.OBSERVER);
        JsonObject fullUpdate = new JsonObject();
        fullUpdate.addProperty("type", "full");
        fullUpdate.addProperty("owner", USER_EMAIL);
        fullUpdate.addProperty("state", GameStateID.RUNNING);
        fullUpdate.addProperty("mode", "target");
        fullUpdate.addProperty("proximityThreshold", 1);
        JsonArray players = new JsonArray();
        players.add(JsonHelper.player(USER_EMAIL, TeamID.TEAM_YELLOW, PlayerStateID.PLAYING, llTarget, ulTarget, umTarget));
        players.add(JsonHelper.player("noone@illinois.edu", TeamID.TEAM_YELLOW, PlayerStateID.PLAYING, lmTarget, lrTarget));
        players.add(JsonHelper.player("opponent@example.com", TeamID.TEAM_RED, PlayerStateID.PLAYING, urTarget));
        players.add(JsonHelper.player("another@example.com", TeamID.TEAM_GREEN, PlayerStateID.PLAYING));
        fullUpdate.add("players", players);
        fullUpdate.add("targets", JsonHelper.arrayOf(llTarget, ulTarget, umTarget, lmTarget, lrTarget, urTarget, fdTarget, fuTarget, o1Target, o2Target, o3Target));
        return fullUpdate;
    }

    static JsonObject createAreaModeTestGame() {
        JsonObject p1Start = JsonHelper.area(4, 1, USER_EMAIL, TeamID.TEAM_RED);
        JsonObject p2Start = JsonHelper.area(3, 3, "noone@illinois.edu", TeamID.TEAM_RED);
        JsonObject p2End = JsonHelper.area(4, 3, "noone@illinois.edu", TeamID.TEAM_RED);
        JsonObject p3Start = JsonHelper.area(0, 3, "opponent@example.com", TeamID.TEAM_YELLOW);
        JsonObject p3Mid = JsonHelper.area(1, 3, "opponent@example.com", TeamID.TEAM_YELLOW);
        JsonObject p3End = JsonHelper.area(1, 2, "opponent@example.com", TeamID.TEAM_YELLOW);
        JsonObject p4Start = JsonHelper.area(0, 0, "another@example.com", TeamID.TEAM_BLUE);
        JsonObject fullUpdate = new JsonObject();
        fullUpdate.addProperty("type", "full");
        fullUpdate.addProperty("owner", USER_EMAIL);
        fullUpdate.addProperty("state", GameStateID.RUNNING);
        fullUpdate.addProperty("mode", "area");
        fullUpdate.addProperty("areaNorth", 40.116319);
        fullUpdate.addProperty("areaEast", -88.223576);
        fullUpdate.addProperty("areaSouth", 40.112905);
        fullUpdate.addProperty("areaWest", -88.228933);
        fullUpdate.addProperty("cellSize", 80);
        JsonArray players = new JsonArray();
        players.add(JsonHelper.player(USER_EMAIL, TeamID.TEAM_RED, PlayerStateID.PLAYING, p1Start));
        players.add(JsonHelper.player("noone@illinois.edu", TeamID.TEAM_RED, PlayerStateID.PLAYING, p2Start, p2End));
        players.add(JsonHelper.player("opponent@example.com", TeamID.TEAM_YELLOW, PlayerStateID.PLAYING, p3Start, p3Mid, p3End));
        players.add(JsonHelper.player("another@example.com", TeamID.TEAM_BLUE, PlayerStateID.PLAYING, p4Start));
        players.add(JsonHelper.player("late@example.com", TeamID.TEAM_GREEN, PlayerStateID.ACCEPTED));
        fullUpdate.add("players", players);
        fullUpdate.add("cells", JsonHelper.arrayOf(p1Start, p2Start, p2End, p3Start, p3Mid, p3End, p4Start));
        return fullUpdate;
    }

}
