package edu.illinois.cs.cs125.fall2019.mp;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

final class JsonHelper {

    private JsonHelper() { }

    static JsonObject game(String id, String owner, int state, String mode, JsonObject... players) {
        JsonObject json = new JsonObject();
        json.addProperty("id", id);
        json.addProperty("owner", owner);
        json.addProperty("state", state);
        json.addProperty("mode", mode);
        json.add("players", arrayOf(players));
        if (mode.equals("target")) {
            json.add("targets",
                    arrayOf(target(RandomHelper.randomId(), RandomHelper.randomLat(), RandomHelper.randomLng(), TeamID.OBSERVER)));
        } else if (mode.equals("area")) {
            json.add("cells", new JsonArray());
        }
        return json;
    }

    static JsonObject position(double lat, double lng) {
        JsonObject json = new JsonObject();
        json.addProperty("latitude", lat);
        json.addProperty("longitude", lng);
        return json;
    }

    static JsonObject player(String email, int team, int state, JsonObject... objectives) {
        JsonObject json = new JsonObject();
        json.addProperty("email", email);
        json.addProperty("team", team);
        json.addProperty("state", state);
        JsonArray path = new JsonArray();
        for (JsonObject o : objectives) {
            JsonObject objective = o.deepCopy();
            objective.remove("team");
            objective.remove("email");
            path.add(objective);
        }
        json.add("path", path);
        return json;
    }

    static JsonObject target(String id, double lat, double lng, int team) {
        JsonObject json = position(lat, lng);
        json.addProperty("id", id);
        json.addProperty("team", team);
        return json;
    }

    static JsonObject area(int x, int y, String email, int team) {
        JsonObject json = new JsonObject();
        json.addProperty("x", x);
        json.addProperty("y", y);
        json.addProperty("email", email);
        json.addProperty("team", team);
        return json;
    }

    static JsonObject updateGameState(int state) {
        JsonObject json = new JsonObject();
        json.addProperty("state", state);
        json.addProperty("type", "gameState");
        return json;
    }

    static JsonObject updatePlayerLocation(String email, int team, double lat, double lng) {
        JsonObject update = new JsonObject();
        update.addProperty("email", email);
        update.addProperty("team", team);
        update.addProperty("lastLatitude", lat);
        update.addProperty("lastLongitude", lng);
        update.addProperty("type", "playerLocation");
        return update;
    }

    static JsonObject updatePlayerExit(String email) {
        JsonObject update = new JsonObject();
        update.addProperty("email", email);
        update.addProperty("type", "playerExit");
        return update;
    }

    static JsonObject updatePlayerTargetVisit(String email, int team, String targetId) {
        JsonObject update = new JsonObject();
        update.addProperty("email", email);
        update.addProperty("team", team);
        update.addProperty("targetId", targetId);
        update.addProperty("type", "playerTargetVisit");
        return update;
    }

    static JsonObject updatePlayerCellCapture(String email, int team, int x, int y) {
        JsonObject update = new JsonObject();
        update.addProperty("email", email);
        update.addProperty("team", team);
        update.addProperty("x", x);
        update.addProperty("y", y);
        update.addProperty("type", "playerCellCapture");
        return update;
    }

    static JsonArray arrayOf(JsonElement... elements) {
        JsonArray array = new JsonArray();
        for (JsonElement e : elements) {
            array.add(e);
        }
        return array;
    }

}
