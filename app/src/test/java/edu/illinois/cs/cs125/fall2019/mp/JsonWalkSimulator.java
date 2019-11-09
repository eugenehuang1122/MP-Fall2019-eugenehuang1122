package edu.illinois.cs.cs125.fall2019.mp;

import com.google.android.gms.maps.model.LatLng;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

class JsonWalkSimulator extends WalkSimulator {

    private JsonArray path;

    JsonWalkSimulator(JsonArray setPath) {
        path = setPath;
    }

    @Override
    protected LatLng current() {
        JsonObject point = path.get(getStep()).getAsJsonObject();
        return new LatLng(point.get("lat").getAsDouble(), point.get("lng").getAsDouble());
    }

    @Override
    boolean atEnd() {
        return getStep() == path.size();
    }

}
