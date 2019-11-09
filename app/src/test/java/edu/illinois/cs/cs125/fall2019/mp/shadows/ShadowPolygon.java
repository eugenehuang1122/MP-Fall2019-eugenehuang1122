package edu.illinois.cs.cs125.fall2019.mp.shadows;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Polygon;
import com.google.android.gms.maps.model.PolygonOptions;

import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;
import org.robolectric.annotation.RealObject;

import java.util.ArrayList;
import java.util.List;

import edu.illinois.cs.cs125.fall2019.mp.RandomHelper;

@Implements(Polygon.class)
public class ShadowPolygon {

    @RealObject private Polygon self;
    private PolygonOptions options;
    private ShadowGoogleMap map;
    private String id;
    private Object tag;

    void setup(PolygonOptions setOptions, ShadowGoogleMap setMap) {
        options = setOptions;
        map = setMap;
        id = RandomHelper.randomId();
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
    protected void remove() {
        map.removePolygon(self);
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
