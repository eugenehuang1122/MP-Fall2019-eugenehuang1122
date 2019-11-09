package edu.illinois.cs.cs125.fall2019.mp.shadows;

import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;

import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;
import org.robolectric.annotation.RealObject;

import edu.illinois.cs.cs125.fall2019.mp.RandomHelper;

@Implements(Circle.class)
public class ShadowCircle {

    @RealObject private Circle self;
    private CircleOptions options;
    private ShadowGoogleMap map;
    private String id;
    private Object tag;

    void setup(CircleOptions setOptions, ShadowGoogleMap setMap) {
        options = setOptions;
        map = setMap;
        id = RandomHelper.randomId();
    }

    @Implementation
    protected LatLng getCenter() {
        return options.getCenter();
    }

    @Implementation
    protected void setCenter(LatLng center) {
        options.center(center);
    }

    @Implementation
    protected int getFillColor() {
        return options.getFillColor();
    }

    @Implementation
    protected void setFillColor(int color) {
        options.fillColor(color);
    }

    @Implementation
    protected String getId() {
        return id;
    }

    @Implementation
    protected double getRadius() {
        return options.getRadius();
    }

    @Implementation
    protected void setRadius(double radius) {
        options.radius(radius);
    }

    @Implementation
    protected Object getTag() {
        return tag;
    }

    @Implementation
    protected void setTag(Object setTag) {
        tag = setTag;
    }

    @Implementation
    protected void remove() {
        map.removeCircle(self);
    }

    @SuppressWarnings("EqualsWhichDoesntCheckParameterClass")
    @Implementation
    public boolean equals(Object other) {
        return self == other;
    }

    @Implementation
    public int hashCode() {
        return super.hashCode();
    }

}
