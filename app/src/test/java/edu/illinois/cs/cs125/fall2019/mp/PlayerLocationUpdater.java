package edu.illinois.cs.cs125.fall2019.mp;

import com.google.android.gms.maps.model.LatLng;
import com.google.gson.JsonObject;

class PlayerLocationUpdater {

    private WalkSimulator simulator;
    private String email;
    private int team;

    PlayerLocationUpdater(WalkSimulator setSimulator, String setEmail, int setTeam) {
        simulator = setSimulator;
        email = setEmail;
        team = setTeam;
    }

    JsonObject step() {
        LatLng pos = simulator.step();
        return JsonHelper.updatePlayerLocation(email, team, pos.latitude, pos.longitude);
    }

}
