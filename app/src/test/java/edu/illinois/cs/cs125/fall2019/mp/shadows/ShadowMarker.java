package edu.illinois.cs.cs125.fall2019.mp.shadows;

import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;
import org.robolectric.annotation.RealObject;
import org.robolectric.shadow.api.Shadow;

import edu.illinois.cs.cs125.fall2019.mp.RandomHelper;

@Implements(Marker.class)
public class ShadowMarker {

    @RealObject private Marker self;
    private MarkerOptions options;
    private ShadowGoogleMap map;
    private String id;
    private Object tag;

    void setup(MarkerOptions setOptions, ShadowGoogleMap setMap) {
        options = setOptions;
        map = setMap;
        id = RandomHelper.randomId();
    }

    @Implementation
    protected void setIcon(BitmapDescriptor icon) {
        options.icon(icon);
    }

    @Implementation
    protected String getId() {
        return id;
    }

    @Implementation
    protected LatLng getPosition() {
        return options.getPosition();
    }

    @Implementation
    protected void setPosition(LatLng position) {
        options.position(position);
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
        map.removeMarker(self);
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

    public BitmapDescriptor getIcon() {
        return options.getIcon();
    }

    public float getHue() {
        if (options.getIcon() == null) {
            return BitmapDescriptorFactory.HUE_RED;
        } else {
            return Shadow.<ShadowBitmapDescriptor>extract(options.getIcon()).getHue();
        }
    }

}
