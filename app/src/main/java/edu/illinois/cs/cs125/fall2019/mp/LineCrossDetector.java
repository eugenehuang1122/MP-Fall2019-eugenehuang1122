package edu.illinois.cs.cs125.fall2019.mp;

import com.google.android.gms.maps.model.LatLng;

/**
 * Holds a method to determine whether two lines cross.
 */
public class LineCrossDetector {

    /**
     * Determines whether two lines cross.
     * <p>
     * <i>Crossing</i> is not always the same as <i>intersecting</i>. Lines that share a tip
     * intersect but do not cross for purposes of this function. However, a line that has an endpoint
     * on the <i>middle</i> of another line must be considered to cross that line (to prevent
     * circumventing the snake rule).
     * <p>
     * For simplicity, longitude and latitude are treated as X and Y, respectively, on a 2D coordinate plane.
     * This ignores the roundness of the earth, but it's undetectable at reasonable scales of the game.
     * <p>
     * All parameters are assumed to be valid: both lines have positive length.
     * @param firstStart an endpoint of one line
     * @param firstEnd the other endpoint of that line
     * @param secondStart an endpoint of another line
     * @param secondEnd the other endpoint of that other line
     * @return whether the two lines cross
     */
    public static boolean linesCross(final LatLng firstStart, final LatLng firstEnd,
                                     final LatLng secondStart, final LatLng secondEnd) {
        if (LatLngUtils.same(firstStart, secondStart)
                || LatLngUtils.same(firstStart, secondEnd)
                || LatLngUtils.same(firstEnd, secondStart)
                || LatLngUtils.same(firstEnd, secondEnd)) {
            // The lines are just sharing endpoints, not crossing each other
            return false;
        }

        // A line is vertical (purely north-south) if its longitude is constant
        boolean firstVertical = LatLngUtils.same(firstStart.longitude, firstEnd.longitude);
        boolean secondVertical = LatLngUtils.same(secondStart.longitude, secondEnd.longitude);
        if (firstVertical && secondVertical) {
            // They're parallel vertical lines
            return false;
        } else if (firstVertical) {
            return lineCrossesVertical(firstStart, firstEnd, secondStart, secondEnd);
        } else if (secondVertical) {
            return lineCrossesVertical(secondStart, secondEnd, firstStart, firstEnd);
        }

        // At this point, neither line is vertical
        double firstSlope = lineSlope(firstStart, firstEnd);
        double secondSlope = lineSlope(secondStart, secondEnd);
        if (LatLngUtils.same(firstSlope, secondSlope)) {
            // They're parallel
            return false;
        }

        // At this point, the lines are non-parallel (would intersect if infinitely extended)
        double firstIntercept = firstStart.latitude - firstSlope * firstStart.longitude;
        double secondIntercept = secondStart.latitude - secondSlope * secondStart.longitude;
        double intersectionX = -(firstIntercept - secondIntercept) / (firstSlope - secondSlope);
        if (LatLngUtils.same(intersectionX, firstStart.longitude)
                || LatLngUtils.same(intersectionX, firstEnd.longitude)
                || LatLngUtils.same(intersectionX, secondStart.longitude)
                || LatLngUtils.same(intersectionX, secondEnd.longitude)) {
            // Endpoint of one line is in the middle of the other line
            return true;
        }
        boolean onFirst = intersectionX > Math.min(firstStart.longitude, firstEnd.longitude)
                && intersectionX < Math.max(firstStart.longitude, firstEnd.longitude);
        boolean onSecond = intersectionX > Math.min(secondStart.longitude, secondEnd.longitude)
                && intersectionX < Math.max(secondStart.longitude, secondEnd.longitude);
        return onFirst && onSecond;
    }

    /**
     * Determines if a non-vertical line crosses a vertical line.
     * @param verticalStart one endpoint of the vertical line
     * @param verticalEnd the other endpoint of the vertical line
     * @param lineStart one endpoint of the non-vertical line
     * @param lineEnd the other endpoint of the non-vertical line line
     * @return whether the lines cross
     */
    private static boolean lineCrossesVertical(final LatLng verticalStart, final LatLng verticalEnd,
                                               final LatLng lineStart, final LatLng lineEnd) {
        if (Math.max(lineStart.longitude, lineEnd.longitude) < verticalStart.longitude
                || Math.min(lineStart.longitude, lineEnd.longitude) > verticalStart.longitude) {
            // The non-vertical line is completely off to the side of the vertical line
            return false;
        }
        double slope = lineSlope(lineStart, lineEnd);
        double yAtVert = slope * (verticalStart.longitude - lineStart.longitude) + lineStart.latitude;
        if (LatLngUtils.same(yAtVert, verticalStart.latitude) || LatLngUtils.same(yAtVert, verticalEnd.latitude)) {
            // Ends on the middle of the non-vertical line
            return true;
        }
        // See if the intersection of the lines is between the endpoints of the vertical line segment
        return yAtVert > Math.min(verticalStart.latitude, verticalEnd.latitude)
                && yAtVert < Math.max(verticalStart.latitude, verticalEnd.latitude);
    }

    /**
     * Determines the slope of a non-vertical line.
     * @param start one endpoint of the line
     * @param end the other endpoint of the line
     * @return the slope, treating longitude as X and latitude as Y
     */
    private static double lineSlope(final LatLng start, final LatLng end) {
        return (end.latitude - start.latitude) / (end.longitude - start.longitude);
    }

}
