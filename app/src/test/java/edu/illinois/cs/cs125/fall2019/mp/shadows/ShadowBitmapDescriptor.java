package edu.illinois.cs.cs125.fall2019.mp.shadows;

import com.google.android.gms.maps.model.BitmapDescriptor;

import org.robolectric.annotation.Implements;

@Implements(BitmapDescriptor.class)
public class ShadowBitmapDescriptor {

    private float hue;

    public float getHue() {
        return hue;
    }

    public void setHue(float newHue) {
        hue = newHue;
    }

}
