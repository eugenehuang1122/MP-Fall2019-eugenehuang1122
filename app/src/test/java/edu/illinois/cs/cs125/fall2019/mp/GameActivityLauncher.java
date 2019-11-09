package edu.illinois.cs.cs125.fall2019.mp;

import android.content.Intent;
import android.location.Location;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.test.core.app.ApplicationProvider;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.LatLng;

import org.robolectric.Robolectric;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.shadow.api.Shadow;

import edu.illinois.cs.cs125.fall2019.mp.shadows.ShadowSupportMapFragment;
import edu.illinois.cs.cs125.robolectricsecurity.Trusted;

@Trusted
final class GameActivityLauncher {

    private ActivityController<GameActivity> controller;

    GameActivityLauncher(Intent intent) {
        controller = Robolectric.buildActivity(GameActivity.class, intent).create().start().resume();
        ShadowSupportMapFragment mapFragment = Shadow.extract(controller.get().getSupportFragmentManager().findFragmentById(R.id.gameMap));
        mapFragment.notifyMapReady();
    }

    GameActivityLauncher(String gameId) {
        this(new Intent() {{ putExtra("game", gameId); }});
    }

    GameActivityLauncher() {
        this(RandomHelper.randomId());
    }

    GameActivity getActivity() {
        return controller.get();
    }

    void shutdown() {
        controller.pause().stop().destroy();
    }

    void sendLocationUpdate(LatLng position) {
        Location location = new Location("test");
        location.setLatitude(position.latitude);
        location.setLongitude(position.longitude);
        location.setAccuracy(15.0f);
        Intent intent = new Intent();
        intent.setAction(LocationListenerService.UPDATE_ACTION);
        intent.putExtra(LocationListenerService.UPDATE_DATA_ID, location);
        LocalBroadcastManager.getInstance(ApplicationProvider.getApplicationContext()).sendBroadcastSync(intent);
    }

    GoogleMap getMap() {
        return Shadow.<ShadowSupportMapFragment>extract(controller.get().getSupportFragmentManager().findFragmentById(R.id.gameMap)).getMap();
    }

}
