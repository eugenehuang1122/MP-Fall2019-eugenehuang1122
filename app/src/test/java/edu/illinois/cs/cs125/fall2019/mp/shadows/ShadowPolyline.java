package edu.illinois.cs.cs125.fall2019.mp.shadows;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;
import org.robolectric.annotation.RealObject;

import java.util.ArrayList;
import java.util.List;

import edu.illinois.cs.cs125.fall2019.mp.RandomHelper;

@Implements(Polyline.class)
public class ShadowPolyline {

    @RealObject private Polyline self;
    private PolylineOptions options;
    private ShadowGoogleMap map;
    private String id;
    private Object tag;

    void setup(PolylineOptions setOptions, ShadowGoogleMap setMap) {
        options = setOptions;
        map = setMap;
        id = RandomHelper.randomId();
    }

    @Implementation
    protected int getColor() {
        return options.getColor();
    }

    @Implementation
    protected void setColor(int color) {
        options.color(color);
    }

    @Implementation
    protected String getId() {
        return id;
    }

    @Implementation
    protected List<LatLng> getPoints() {
        return new ArrayList<>(options.getPoints());
    }

    @Implementation
    protected void setPoints(List<LatLng> points) {
        options.getPoints().clear();
        options.addAll(points);
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
    protected float getWidth() {
        return options.getWidth();
    }

    @Implementation
    protected void setWidth(float width) {
        options.width(width);
    }

    @Implementation
    protected void remove() {
        map.removePolyline(self);
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
