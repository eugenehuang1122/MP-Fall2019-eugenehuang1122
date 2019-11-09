
package edu.illinois.cs.cs125.fall2019.mp;

import com.google.android.gms.maps.model.LatLng;

/**
 * Holds methods for managing a path of target claims.
 * <p>
 * The set of locations is represented as a pair of identically sized arrays. The first target's latitude
 * is the first entry in the latitudes array; its longitude is the first entry in the longitudes array.
 * The path array contains indexes of the captured targets in the order they were captured. To ensure
 * that's it's possible to capture all targets, the path array has the same size as the coordinate arrays.
 * If not all targets have been captured yet, unused slots of the path array have the value -1. The path
 * array is filled starting at the beginning, so all instances of -1 are contiguous at the end of the array.
 * <p>
 * For example, if latitudes is [40.2, 40.8], longitudes is [-88.5, -88.3], and path is [1, -1],
 * there are two targets: (40.2, -88.5) and (40.8, -88.3). The target at index 1, that is, (40.8, -88.3),
 * has been captured; the other has not. If path were [-1, -1], that would indicate that the user has
 * captured no targets so far. If path were [0, 1], that would indicate that the user first captured target
 * 0, that is, (40.2, -88.5), and then captured target 1.
 * <p>
 * You need to complete the three functions here. You can then use them to implement the game in
 * the location update handler of GameActivity!
 */
public class TargetVisitChecker {

    /**
     * Gets an index of an unvisited target within the specified range of the current location.
     * <p>
     * A target is visited if its index appears in the path array.
     * <p>
     * It is assumed that all parameters are valid. The three arrays are non-null and of the same length.
     * All coordinates are valid. The range is positive.
     * <p>
     * This function should not modify the arrays it receives.
     * @param latitudes latitudes of all targets
     * @param longitudes longitudes of all targets (same size as latitudes)
     * @param path indexes of targets visited so far (same size as latitudes, -1 for empty slots)
     * @param currentLatitude the current latitude
     * @param currentLongitude the current longitude
     * @param range maximum distance to target, in meters
     * @return the index of a target within the range that is not on the path, or -1 if no such target exists
     */
    public static int getTargetWithinRange(final double[] latitudes, final double[] longitudes, final int[] path,
                                           final double currentLatitude, final double currentLongitude,
                                           final int range) {
        for (int i = 0; i < path.length; i++) {
            boolean ispresent = false;
            if (LatLngUtils.distance(currentLatitude, currentLongitude, latitudes[i], longitudes[i]) <= range) {
                for (int j = 0; j < path.length; j++) {
                    if (i == path[j]) {
                        ispresent = true;
                        break;
                    }
                }
                if (ispresent == false) {
                    return i;
                }
            }
        }
        return -1;
    }


        // HINT: To find the distance in meters between two locations, use a provided helper function:
        // LatLngUtils.distance(oneLatitude, oneLongitude, otherLatitude, otherLongitude)
    /**
     * Determines whether the specified target can be visited without violating the snake rule.
     * <p>
     * The snake rule is violated if the new line created between the last captured target and this
     * new target would cross the straight line connecting two sequentially captured targets. For example,
     * there is a line between the first-captured and second-captured targets, and between the second-captured
     * and third-captured targets, but no line directly connecting the first-captured and third-captured
     * targets. If zero or one targets have been captured so far, there are no lines and it is permissible
     * to capture any target.
     * <p>
     * It is assumed that all parameters are valid. The three arrays are non-null and of the same length.
     * All coordinates are valid. The index of the target to visit is a valid index into the coordinate
     * arrays. The index of the target to visit does not appear in the path array (i.e. the target has not
     * been visited yet). No existing lines violate the snake rule.
     * <p>
     * This function should not modify the arrays it receives.
     * @param latitudes latitudes of all targets
     * @param longitudes longitudes of all targets
     * @param path indexes of targets visited so far (-1 for empty slots)
     * @param tryVisit index of the target to try to visit
     * @return whether the target can be claimed
     */

    public static boolean checkSnakeRule(final double[] latitudes, final double[] longitudes, final int[] path,
                                         final int tryVisit) {
        // HINT: To determine whether two lines cross, use a provided helper function:
        // LineCrossDetector.linesCross(oneStartLat, oneStartLng, oneEndLat, oneEndLng,
        //                              otherStartLat, otherStartLng, otherEndLat, otherEndLng)
        if (path[0] == -1 || path[1] ==  -1) {
            return true;
        }
        int lastVisit = 0;
        for (int j = 1; j < path.length; j++) {
            if (path[j] == -1) {
                lastVisit = j - 1;
                break;
            }
        }
        for (int i = 0; i < lastVisit; i++) {
            if (LineCrossDetector.linesCross(new LatLng(latitudes[tryVisit], longitudes[tryVisit]),
                    new LatLng(latitudes[path[lastVisit]], longitudes[path[lastVisit]]),
                    new LatLng(latitudes[path[i]], longitudes[path[i]]),
                    new LatLng(latitudes[path[i + 1]], longitudes[path[i + 1]]))) {
                return false;
            }
        }
        return true;
        //activity_main.xml

    }




    /**
     * Marks a target captured by putting its index in the first available (-1) slot of the path array.
     * <p>
     * It is assumed that all parameters are valid. The path array is non-null and does not yet contain
     * the target index. The target index is non-negative.
     * @param path the path array
     * @param targetIndex the target being visited
     * @return the index in the path array that was updated, or -1 if the path array was full
     */
    public static int visitTarget(final int[] path, final int targetIndex) {
        for (int i = 0; i < path.length; i++) {
            if (path[i] == -1) {
                path[i] = targetIndex;
                return i;
            }
        }
        return -1;
    }
}
