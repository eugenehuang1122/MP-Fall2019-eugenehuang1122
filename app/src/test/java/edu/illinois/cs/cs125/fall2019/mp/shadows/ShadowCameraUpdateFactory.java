package edu.illinois.cs.cs125.fall2019.mp.shadows;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;

import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;

@Implements(CameraUpdateFactory.class)
public class ShadowCameraUpdateFactory {

    @Implementation
    protected static CameraUpdate newLatLngZoom(LatLng center, float zoom) {
        return null;
    }

    @Implementation
    protected static CameraUpdate newLatLngBounds(LatLngBounds bounds, int width, int height, int padding) {
        return null;
    }

}
