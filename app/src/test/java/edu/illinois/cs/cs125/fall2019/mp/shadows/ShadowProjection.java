package edu.illinois.cs.cs125.fall2019.mp.shadows;

import com.google.android.gms.maps.Projection;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.VisibleRegion;

import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;

@Implements(Projection.class)
public class ShadowProjection {

    private ShadowGoogleMap map;

    void setup(ShadowGoogleMap setMap) {
        map = setMap;
    }

    @Implementation
    protected VisibleRegion getVisibleRegion() {
        LatLngBounds bounds = map.getVisibleBounds();
        return new VisibleRegion(
                bounds.southwest,
                new LatLng(bounds.southwest.latitude, bounds.northeast.longitude),
                new LatLng(bounds.northeast.latitude, bounds.southwest.longitude),
                bounds.northeast,
                bounds);
    }

}
