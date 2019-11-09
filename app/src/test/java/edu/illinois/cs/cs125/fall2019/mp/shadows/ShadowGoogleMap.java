package edu.illinois.cs.cs125.fall2019.mp.shadows;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.Projection;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polygon;
import com.google.android.gms.maps.model.PolygonOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;
import org.robolectric.shadow.api.Shadow;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import edu.illinois.cs.cs125.fall2019.mp.LatLngUtils;
import edu.illinois.cs.cs125.robolectricsecurity.Trusted;

@Trusted
@Implements(GoogleMap.class)
public class ShadowGoogleMap {

    private List<Marker> markers = new ArrayList<>();
    private List<Circle> circles = new ArrayList<>();
    private List<Polyline> polylines = new ArrayList<>();
    private List<Polygon> polygons = new ArrayList<>();

    private Consumer<Object> addListener = ignored -> { };
    private GoogleMap.OnMapLongClickListener longPressListener = null;
    private GoogleMap.OnMarkerClickListener markerClickListener = null;

    private LatLngBounds visibleBounds = new LatLngBounds(
            new LatLng(40.098078, -88.238586),
            new LatLng(40.116426, -88.219487));

    @Implementation
    protected Marker addMarker(MarkerOptions options) {
        Marker marker = MockedWrapperInstantiator.create(Marker.class);
        ShadowMarker shadow = Shadow.extract(marker);
        shadow.setup(options, this);
        markers.add(marker);
        addListener.accept(marker);
        return marker;
    }

    @Implementation
    protected Circle addCircle(CircleOptions options) {
        Circle circle = MockedWrapperInstantiator.create(Circle.class);
        ShadowCircle shadow = Shadow.extract(circle);
        shadow.setup(options, this);
        circles.add(circle);
        addListener.accept(circle);
        return circle;
    }

    @Implementation
    protected Polyline addPolyline(PolylineOptions options) {
        Polyline polyline = MockedWrapperInstantiator.create(Polyline.class);
        ShadowPolyline shadow = Shadow.extract(polyline);
        shadow.setup(options, this);
        polylines.add(polyline);
        addListener.accept(polyline);
        return polyline;
    }

    @Implementation
    protected Polygon addPolygon(PolygonOptions options) {
        Polygon polygon = MockedWrapperInstantiator.create(Polygon.class);
        ShadowPolygon shadow = Shadow.extract(polygon);
        shadow.setup(options, this);
        polygons.add(polygon);
        addListener.accept(polygon);
        return polygon;
    }

    @Implementation
    protected void clear() {
        markers.clear();
        circles.clear();
        polylines.clear();
        polygons.clear();
    }

    @Implementation
    protected Projection getProjection() {
        Projection projection = MockedWrapperInstantiator.create(Projection.class);
        ShadowProjection shadow = Shadow.extract(projection);
        shadow.setup(this);
        return projection;
    }

    @Implementation
    protected void moveCamera(CameraUpdate update) {
        // Do nothing
    }

    @Implementation
    protected void setOnMapLongClickListener(GoogleMap.OnMapLongClickListener listener) {
        longPressListener = listener;
    }

    @Implementation
    protected void setOnMarkerClickListener(GoogleMap.OnMarkerClickListener listener) {
        markerClickListener = listener;
    }

    void removeMarker(Marker marker) {
        markers.remove(marker);
    }

    void removeCircle(Circle circle) {
        circles.remove(circle);
    }

    void removePolyline(Polyline polyline) {
        polylines.remove(polyline);
    }

    void removePolygon(Polygon polygon) {
        polygons.remove(polygon);
    }

    public List<Marker> getMarkers() {
        return Collections.unmodifiableList(markers);
    }

    public List<Circle> getCircles() {
        return Collections.unmodifiableList(circles);
    }

    public List<Polyline> getPolylines() {
        return Collections.unmodifiableList(polylines);
    }

    public List<Polygon> getPolygons() {
        return Collections.unmodifiableList(polygons);
    }

    public Marker getMarkerAt(LatLng position) {
        return markers.stream().filter(m -> LatLngUtils.same(m.getPosition(), position)).findAny().orElse(null);
    }

    public List<Marker> getMarkersWithColor(float hue) {
        return markers.stream().filter(m -> Math.abs(Shadow.<ShadowMarker>extract(m).getHue() - hue) < 1e-3)
                .collect(Collectors.toList());
    }

    public Circle getCircleAt(LatLng position) {
        return circles.stream().filter(c -> LatLngUtils.same(c.getCenter(), position)).findAny().orElse(null);
    }

    private void getPolylinesConnectingOrdered(LatLng start, LatLng end, List<Polyline> accumulate) {
        for (Polyline line : polylines) {
            List<LatLng> points = line.getPoints();
            for (int p = 0; p < points.size() - 1; p++) {
                if (LatLngUtils.same(start, points.get(p)) && LatLngUtils.same(end, points.get(p + 1))) {
                    accumulate.removeIf(l -> l == line);
                    accumulate.add(line);
                }
            }
        }
    }

    public List<Polyline> getPolylinesConnecting(LatLng one, LatLng another) {
        List<Polyline> result = new ArrayList<>();
        getPolylinesConnectingOrdered(one, another, result);
        getPolylinesConnectingOrdered(another, one, result);
        return result;
    }

    private void getPolygonsFillingOrdered(List<LatLng> sequence, List<Polygon> accumulate) {
        checkPolygon: for (Polygon poly : polygons) {
            List<LatLng> vertices = poly.getPoints();
            if (vertices.isEmpty()) {
                continue;
            }
            if (LatLngUtils.same(vertices.get(0), vertices.get(vertices.size() - 1))) {
                vertices.remove(vertices.size() - 1);
            }
            if (vertices.size() != sequence.size()) {
                continue;
            }
            for (int i = 0; i < sequence.size(); i++) {
                if (LatLngUtils.same(sequence.get(i), vertices.get(0))) {
                    for (int j = 1; j < sequence.size(); j++) {
                        int idxS = (i + j) % sequence.size();
                        int idxV = j % sequence.size();
                        if (!LatLngUtils.same(sequence.get(idxS), vertices.get(idxV))) {
                            continue checkPolygon;
                        }
                    }
                    accumulate.remove(poly);
                    accumulate.add(poly);
                    continue checkPolygon;
                }
            }
        }
    }

    public List<Polygon> getPolygonsFilling(LatLngBounds area) {
        List<LatLng> points = new ArrayList<>();
        points.add(area.northeast);
        points.add(new LatLng(area.northeast.latitude, area.southwest.longitude)); // northwest
        points.add(area.southwest);
        points.add(new LatLng(area.southwest.latitude, area.northeast.longitude)); // southeast
        List<Polygon> result = new ArrayList<>();
        getPolygonsFillingOrdered(points, result);
        Collections.reverse(points);
        getPolygonsFillingOrdered(points, result);
        return result;
    }

    public Polygon getPolygonFilling(LatLngBounds area) {
        List<Polygon> choices = getPolygonsFilling(area);
        if (choices.isEmpty()) return null;
        return choices.get(0);
    }

    public void setComponentAdditionListener(Consumer<Object> listener) {
        addListener = listener;
    }

    public void longPress(LatLng location) {
        if (longPressListener == null) return;
        longPressListener.onMapLongClick(location);
    }

    public boolean clickMarker(Marker marker) {
        if (markerClickListener == null) return false;
        return markerClickListener.onMarkerClick(marker);
    }

    public void setVisibleRegion(LatLngBounds newBounds) {
        visibleBounds = newBounds;
    }

    LatLngBounds getVisibleBounds() {
        return visibleBounds;
    }

}
