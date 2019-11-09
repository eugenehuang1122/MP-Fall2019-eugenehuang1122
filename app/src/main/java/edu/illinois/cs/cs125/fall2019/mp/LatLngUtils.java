package edu.illinois.cs.cs125.fall2019.mp;

import com.google.android.gms.maps.model.LatLng;

/**
 * Holds helpful static methods for working with locations.
 * <p>
 * STOP! Do not modify this file. Changes will be overwritten during official grading.
 */
public final class LatLngUtils {

    /** Tolerance for LatLng-related double comparisons. */
    private static final double COMPARE_EPSILON = 0.0000001;

    /**
     * Computes the distance between two points.
     * @param oneLat the latitude of one point
     * @param oneLng the longitude of that point
     * @param anotherLat the latitude of another point
     * @param anotherLng the longitude of that other point
     * @return the distance between the two points, in meters
     */
    public static double distance(final double oneLat, final double oneLng,
                                  final double anotherLat, final double anotherLng) {
        return distance(new LatLng(oneLat, oneLng), new LatLng(anotherLat, anotherLng));
    }

    /**
     * Computes the distance between two points represented as LatLngs.
     * @param one one latitude-longitude coordinate pair
     * @param another the other latitude-longitude coordinate pair
     * @return the distance between the two points, in meters
     */
    public static double distance(final LatLng one, final LatLng another) {
        final double latDistanceScale = 110574;
        final double lngDistanceScale = 111320;
        final double degToRad = Math.PI / 180;
        double latRadians = degToRad * one.latitude;
        double latDistance = latDistanceScale * (one.latitude - another.latitude);
        double lngDistance = lngDistanceScale * (one.longitude - another.longitude) * Math.cos(latRadians);
        return Math.sqrt(latDistance * latDistance + lngDistance * lngDistance);
    }

    /**
     * Determines whether two location-related coordinates are similar enough to be considered the same.
     * @param one a coordinate of one point
     * @param another the corresponding coordinate of the other point
     * @return whether they're effectively the same coordinate
     */
    public static boolean same(final double one, final double another) {
        return Math.abs(one - another) < COMPARE_EPSILON;
    }

    /**
     * Determines whether two points are similar enough to be considered the same spot.
     * @param oneLat the latitude of one point
     * @param oneLng the longitude of that point
     * @param anotherLat the latitude of another point
     * @param anotherLng the longitude of that other point
     * @return whether they're the same place
     */
    public static boolean same(final double oneLat, final double oneLng,
                               final double anotherLat, final double anotherLng) {
        return same(oneLat, anotherLat) && same(oneLng, anotherLng);
    }

    /**
     * Determines whether two points are similar enough to be considered the same spot.
     * @param one one point
     * @param another the other point
     * @return whether they're the same place
     */
    public static boolean same(final LatLng one, final LatLng another) {
        return same(one.latitude, another.latitude) && same(one.longitude, another.longitude);
    }

}
