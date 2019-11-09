package edu.illinois.cs.cs125.fall2019.mp;

import android.content.Context;

import androidx.annotation.VisibleForTesting;

import com.google.android.gms.maps.model.LatLng;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

/**
 * Loads the default targets for local games in Checkpoints 0 through 3.
 * <p>
 * STOP! Do not modify this file. Changes will be overwritten during official grading.
 */
public final class DefaultTargets {

    /** Private constructor to prevent creating instances. */
    private DefaultTargets() { }

    /** Positions of example targets. */
    private static List<LatLng> targets;

    /** Whether to allow using this class at all. */
    private static boolean allowUse = true;

    /** Whether to allow getting arrays of coordinates independently. */
    private static boolean allowIndependentArrays = true;

    /**
     * Loads the default targets list if it hasn't already been loaded.
     * @param context the Android context to load the data from
     */
    private static void loadIfNeeded(final Context context) {
        if (targets != null) {
            return;
        }
        targets = new ArrayList<>();
        Scanner scanner = new Scanner(context.getResources().openRawResource(R.raw.defaulttargets));
        while (scanner.hasNextLine()) {
            String[] parts = scanner.nextLine().split(",\\s*");
            double lat = Double.parseDouble(parts[0]);
            double lng = Double.parseDouble(parts[1]);
            targets.add(new LatLng(lat, lng));
        }
        scanner.close();
    }

    /**
     * Disables this class's functionality entirely.
     * <p>
     * Used by test suites once this class is phased out.
     */
    @VisibleForTesting(otherwise = VisibleForTesting.NONE)
    public static void disable() {
        allowIndependentArrays = false;
        allowUse = false;
    }

    /**
     * Disables the getLatitudes and getLongitudes functions.
     * <p>
     * Used by test suites once independent arrays are phased out.
     */
    @VisibleForTesting(otherwise = VisibleForTesting.NONE)
    public static void disableIndependentArrays() {
        allowIndependentArrays = false;
    }

    /**
     * Gets an array of the targets' latitudes.
     * @param context an Android context
     * @return the latitude for each target
     */
    public static double[] getLatitudes(final Context context) {
        if (!allowIndependentArrays) {
            throw new IllegalStateException("Use the LatLng class instead of independent arrays.");
        }
        loadIfNeeded(context);
        return targets.stream().mapToDouble(ll -> ll.latitude).toArray();
    }

    /**
     * Gets an array of the targets' longitudes.
     * @param context an Android context
     * @return the longitude for each target
     */
    public static double[] getLongitudes(final Context context) {
        if (!allowIndependentArrays) {
            throw new IllegalStateException("Use the LatLng class instead of independent arrays.");
        }
        loadIfNeeded(context);
        return targets.stream().mapToDouble(ll -> ll.longitude).toArray();
    }

    /**
     * Gets the positions of the default targets as LatLng instances.
     * @param context an Android context
     * @return the position of each target
     */
    public static LatLng[] getPositions(final Context context) {
        if (!allowUse) {
            throw new IllegalStateException("Use game state from the server instead of default targets.");
        }
        loadIfNeeded(context);
        return targets.toArray(new LatLng[0]);
    }

}
