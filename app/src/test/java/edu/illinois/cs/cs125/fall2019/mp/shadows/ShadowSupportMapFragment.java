package edu.illinois.cs.cs125.fall2019.mp.shadows;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;

import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;

@Implements(SupportMapFragment.class)
public class ShadowSupportMapFragment {

    private GoogleMap map;
    private boolean mapReady = false;
    private OnMapReadyCallback callback;

    @Implementation
    protected void getMapAsync(OnMapReadyCallback setCallback) {
        callback = setCallback;
        map = getMap();
        if (mapReady) {
            callback.onMapReady(map);
        }
    }
    public GoogleMap getMap() {
        if (map == null) {
            map = MockedWrapperInstantiator.create(GoogleMap.class);
        }
        return map;
    }

    public void notifyMapReady() {
        if (callback != null) {
            mapReady = true;
            callback.onMapReady(map);
        }
    }

}
