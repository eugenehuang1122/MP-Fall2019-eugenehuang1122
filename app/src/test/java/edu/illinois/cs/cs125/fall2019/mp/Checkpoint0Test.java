package edu.illinois.cs.cs125.fall2019.mp;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;

import androidx.test.core.app.ApplicationProvider;

import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.Polyline;
import com.google.firebase.auth.FirebaseAuth;
import com.google.gson.JsonObject;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.rule.PowerMockRule;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.shadow.api.Shadow;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import edu.illinois.cs.cs125.fall2019.mp.shadows.ShadowGoogleMap;
import edu.illinois.cs.cs125.fall2019.mp.shadows.ShadowLocalBroadcastManager;
import edu.illinois.cs.cs125.fall2019.mp.shadows.ShadowMarker;
import edu.illinois.cs.cs125.gradlegrader.annotations.Graded;
import edu.illinois.cs.cs125.robolectricsecurity.PowerMockSecurity;
import edu.illinois.cs.cs125.robolectricsecurity.Trusted;

/*
 * Welcome to the Machine Project test suites!
 *
 * All code in the "test" section is involved in making sure your app works as specified.
 * Testing Android apps is somewhat complicated, especially when the tests need to be able to
 * deal with an evolving project. You don't need to worry about understanding the test suite
 * or any of the supporting code. Some of it will make more sense by the end of the semester.
 * Other parts are not covered in CS 125. If you're curious, feel free to ask on the forum!
 *
 * In particular, some code here will not be used at all during the time scheduled for Checkpoint 0.
 * It's for compatibility with the rearrangements done in later checkpoints, to allow for late
 * work on Checkpoint 0 even after the original functions have been altered or removed.
 *
 * DO NOT MODIFY THE LOGIC OF THE TESTS! All changes will be overwritten during official grading.
 * You are free to add diagnostics like print statements, but changing the behavior can lead
 * to official grading problems that are difficult to diagnose.
 */

@RunWith(RobolectricTestRunner.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@PowerMockIgnore({"org.mockito.*", "org.powermock.*", "org.robolectric.*", "android.*", "androidx.*", "com.google.android.*", "edu.illinois.cs.cs125.fall2019.mp.shadows.*"})
@PrepareForTest({WebApi.class, FirebaseAuth.class})
@Trusted
public class Checkpoint0Test {

    // Involved in compatibility
    @Rule
    public PowerMockRule mockStaticClasses = new PowerMockRule();

    @Before
    public void setup() {
        PowerMockSecurity.secureMockMethodCache();
    }

    @After
    public void teardown() {
        WebApiMocker.reset();
        ShadowLocalBroadcastManager.reset();
    }

    @Test(timeout = 60000)
    @Graded(points = 20)
    public void testVisitTarget() throws Throwable {
        // Compatibility layer
        Method visitTarget;
        try {
            visitTarget = Class.forName("edu.illinois.cs.cs125.fall2019.mp.TargetVisitChecker")
                    .getMethod("visitTarget", int[].class, int.class);
        } catch (Exception e) {
            runS4Test();
            return;
        }
        class Invoker {
            private int invoke(int[] path, int target) throws Throwable {
                try {
                    return (int) (Integer) visitTarget.invoke(null, path, target);
                } catch (InvocationTargetException e) {
                    throw e.getCause();
                }
            }
        }
        Invoker invoker = new Invoker();

        // Test full paths
        Assert.assertEquals("visitTarget should return -1 when the path array has no available slots",
                -1, invoker.invoke(new int[] {0}, 1));
        Assert.assertEquals("visitTarget should return -1 when the path array has no available slots",
                -1, invoker.invoke(new int[] {4, 1}, 2));
        Assert.assertEquals("visitTarget should return -1 when the path array has no slots",
                -1, invoker.invoke(new int[0], 0));

        // Test empty paths
        int[] path = new int[] {-1};
        Assert.assertEquals("visitTarget should fill the one slot of a one-element array",
                0, invoker.invoke(path, 3));
        Assert.assertEquals("visitTarget didn't update the array with the new target", 3, path[0]);
        path = new int[] {-1, -1};
        Assert.assertEquals("visitTarget should first try to add to the first slot",
                0, invoker.invoke(path, 2));
        Assert.assertEquals("visitTarget didn't update the array correctly", 2, path[0]);
        Assert.assertEquals("visitTarget should only change one element", -1, path[1]);

        // Test partially filled paths
        path = new int[] {2, -1, -1};
        Assert.assertEquals("visitTarget should fill the first available slot",
                1, invoker.invoke(path, 1));
        Assert.assertEquals("visitTarget shouldn't affect previously filled slots", 2, path[0]);
        Assert.assertEquals("visitTarget shouldn't affect later unfilled slots", -1, path[2]);
        path = new int[] {6, 0, 3, -1};
        Assert.assertEquals("visitTarget should be able to fill the last slot if unfilled",
                3, invoker.invoke(path, 9));
        Assert.assertEquals("visitTarget should be able to update the last slot if unfilled", 9, path[3]);
        Assert.assertArrayEquals("visitTarget should only affect the filled slot", new int[] {6, 0, 3, 9}, path);

        // Randomized tests
        Integer[] targetIds = new Integer[405];
        for (int i = 0; i < targetIds.length; i++) {
            targetIds[i] = i;
        }
        Random random = new Random(125);
        for (int i = 0; i < 200; i++) {
            path = new int[1 + random.nextInt(i * 2 + 5)];
            Arrays.fill(path, -1);
            Collections.shuffle(Arrays.asList(targetIds), random);
            int toAdd = targetIds[404];
            int fillLength = random.nextInt(path.length);
            for (int j = 0; j < fillLength; j++) {
                path[j] = targetIds[j];
            }
            if (fillLength == path.length) {
                Assert.assertEquals("visitTarget shouldn't add to a full path array",
                        -1, invoker.invoke(path, toAdd));
            } else {
                Assert.assertEquals("visitTarget should add to the first empty slot",
                        fillLength, invoker.invoke(path, toAdd));
                Assert.assertEquals("visitTarget should update the first available slot",
                        toAdd, path[fillLength]);
                for (int j = fillLength + 1; j < path.length; j++) {
                    Assert.assertEquals("visitTarget shouldn't affect later unfilled slots",
                            -1, path[j]);
                }
            }
        }
    }

    @Test(timeout = 60000)
    @Graded(points = 20)
    public void testTargetRange() throws Throwable {
        // Compatibility layer
        class Invoker {
            private Class<?> tvcClass;
            private Method getTargetWithinRange;
            private boolean initFailed;
            private boolean s3Signature;
            private Invoker() {
                try {
                    tvcClass = Class.forName("edu.illinois.cs.cs125.fall2019.mp.TargetVisitChecker");
                } catch (ClassNotFoundException e) {
                    initFailed = true;
                    return;
                }
                try {
                    getTargetWithinRange = tvcClass.getMethod("getTargetWithinRange",
                            double[].class, double[].class, int[].class, double.class, double.class, int.class);
                } catch (Exception e1) {
                    try {
                        getTargetWithinRange = tvcClass.getMethod("getTargetWithinRange",
                                LatLng[].class, int[].class, LatLng.class, int.class);
                        s3Signature = true;
                    } catch (Exception e2) {
                        initFailed = true;
                    }
                }
            }
            private int invoke(double[] latitudes, double[] longitudes, int[] path,
                               double lat, double lng, int proximityThreshold) throws Throwable {
                Object result;
                try {
                    if (s3Signature) {
                        LatLng[] latLngs = new LatLng[latitudes.length];
                        for (int i = 0; i < latLngs.length; i++) {
                            latLngs[i] = new LatLng(latitudes[i], longitudes[i]);
                        }
                        LatLng[] originalLatLngs = Arrays.copyOf(latLngs, latLngs.length);
                        result = getTargetWithinRange.invoke(null, latLngs, path, new LatLng(lat, lng), proximityThreshold);
                        for (int i = 0; i < latLngs.length; i++) {
                            Assert.assertSame("getTargetWithinRange should not modify the coordinates array",
                                    originalLatLngs[i], latLngs[i]);
                        }
                    } else {
                        double[] originalLats = Arrays.copyOf(latitudes, latitudes.length);
                        double[] originalLngs = Arrays.copyOf(longitudes, longitudes.length);
                        int[] originalPath = Arrays.copyOf(path, path.length);
                        result = getTargetWithinRange.invoke(null, latitudes, longitudes, path, lat, lng, proximityThreshold);
                        Assert.assertArrayEquals("getTargetWithinRange should not modify the coordinate arrays",
                                originalLats, latitudes, 1e-9);
                        Assert.assertArrayEquals("getTargetWithinRange should not modify the coordinate arrays",
                                originalLngs, longitudes, 1e-9);
                        Assert.assertArrayEquals("getTargetWithinRange should not modify the path array",
                                originalPath, path);
                    }
                } catch (InvocationTargetException e) {
                    throw e.getCause();
                }
                return (int) (Integer) result;
            }
        }
        Invoker invoker = new Invoker();
        if (invoker.initFailed) {
            runS4Test();
            return;
        }

        // Test no targets within range
        Assert.assertEquals("getTargetWithinRange should return -1 when there are no targets nearby",
                -1, invoker.invoke(new double[] {40.0}, new double[] {-88.0}, new int[] {-1}, 41.0, -89.0, 50));
        Assert.assertEquals("getTargetWithinRange should return -1 when there are no targets nearby",
                -1, invoker.invoke(new double[] {40.110309}, new double[] {-88.228860},
                        new int[] {-1}, 40.106935, -88.225494, 100));
        Assert.assertEquals("getTargetWithinRange should return -1 when there are no targets within the proximity threshold",
                -1, invoker.invoke(new double[] {40.106659}, new double[] {-88.228199},
                        new int[] {-1}, 40.106630, -88.227728, 20));
        Assert.assertEquals("getTargetWithinRange should return -1 when there are no unvisited targets",
                -1, invoker.invoke(new double[] {40.105439}, new double[] {-88.227115},
                        new int[] {0}, 40.105408, -88.227074, 90));
        Assert.assertEquals("getTargetWithinRange should return -1 when there are no unvisited targets within the proximity threshold",
                -1, invoker.invoke(new double[] {40.102813, 40.104613}, new double[] {-88.227197, 88.224677},
                        new int[] {0, -1}, 40.102773, -88.227211, 45));
        Assert.assertEquals("getTargetWithinRange should return -1 when there are no targets",
                -1, invoker.invoke(new double[0], new double[0], new int[0], 41.0, -89.0, 50));

        // Test a target within range
        Assert.assertEquals("getTargetWithinRange should find the index of a target within the proximity threshold",
                0, invoker.invoke(new double[] {40.106659}, new double[] {-88.228199},
                        new int[] {-1}, 40.106630, -88.227728, 50));
        Assert.assertEquals("getTargetWithinRange should find the index of a target within the proximity threshold",
                1, invoker.invoke(new double[] {40.104613, 40.105450}, new double[] {-88.224677, -88.228802},
                        new int[] {-1, -1}, 40.105376, -88.228974, 30));
        Assert.assertEquals("getTargetWithinRange should find the index of an unvisited target within the proximity threshold",
                2, invoker.invoke(new double[] {40.096130, 40.096270, 40.096365}, new double[] {-88.218159, -88.218383, -88.217996},
                        new int[] {0, 1, -1}, 40.096284, -88.218180, 50));
        Assert.assertEquals("getTargetWithinRange should not assume that the first target in the position arrays " +
                        "is the first captured target on the path", /* same situation as last check but different order in position arrays */
                1, invoker.invoke(new double[] {40.096270, 40.096365, 40.096130}, new double[] {-88.218383, -88.217996, -88.218159},
                        new int[] {2, 0, -1}, 40.096284, -88.218180, 50));

        // Randomized tests (loaded from JSON)
        for (JsonObject test : JsonResourceLoader.loadArray("targetrange")) {
            double[] latitudes = JsonResourceLoader.getDoubleArray(test.getAsJsonArray("lats"));
            double[] longitudes = JsonResourceLoader.getDoubleArray(test.getAsJsonArray("lngs"));
            int[] path = JsonResourceLoader.getIntArray(test.getAsJsonArray("path"));
            int proximityThreshold = test.get("prox").getAsInt();
            double latitude = test.get("lat").getAsDouble();
            double longitude = test.get("lng").getAsDouble();
            Assert.assertEquals(test.get("answer").getAsInt(),
                    invoker.invoke(latitudes, longitudes, path, latitude, longitude, proximityThreshold));
        }
    }

    @Test(timeout = 60000)
    @Graded(points = 20)
    public void testSnakeRule() throws Throwable {
        // Compatibility layer
        class Invoker {
            private Class<?> tvcClass;
            private Method checkSnakeRule;
            private boolean initFailed;
            private boolean s3Signature;
            private Invoker() {
                try {
                    tvcClass = Class.forName("edu.illinois.cs.cs125.fall2019.mp.TargetVisitChecker");
                } catch (ClassNotFoundException e) {
                    initFailed = true;
                    return;
                }
                try {
                    checkSnakeRule = tvcClass.getMethod("checkSnakeRule", double[].class, double[].class, int[].class, int.class);
                } catch (Exception e1) {
                    try {
                        checkSnakeRule = tvcClass.getMethod("checkSnakeRule", LatLng[].class, int[].class, int.class);
                        s3Signature = true;
                    } catch (Exception e2) {
                        initFailed = true;
                    }
                }
            }
            private boolean invoke(double[] latitudes, double[] longitudes, int[] path, int tryVisit) throws Throwable {
                Object result;
                try {
                    if (s3Signature) {
                        LatLng[] latLngs = new LatLng[latitudes.length];
                        for (int i = 0; i < latLngs.length; i++) {
                            latLngs[i] = new LatLng(latitudes[i], longitudes[i]);
                        }
                        LatLng[] originalLatLngs = Arrays.copyOf(latLngs, latLngs.length);
                        result = checkSnakeRule.invoke(null, latLngs, path, tryVisit);
                        for (int i = 0; i < latLngs.length; i++) {
                            Assert.assertSame("checkSnakeRule should not modify the coordinates array",
                                    originalLatLngs[i], latLngs[i]);
                        }
                    } else {
                        double[] originalLats = Arrays.copyOf(latitudes, latitudes.length);
                        double[] originalLngs = Arrays.copyOf(longitudes, longitudes.length);
                        int[] originalPath = Arrays.copyOf(path, path.length);
                        result = checkSnakeRule.invoke(null, latitudes, longitudes, path, tryVisit);
                        Assert.assertArrayEquals("checkSnakeRule should not modify the coordinate arrays",
                                originalLats, latitudes, 1e-9);
                        Assert.assertArrayEquals("checkSnakeRule should not modify the coordinate arrays",
                                originalLngs, longitudes, 1e-9);
                        Assert.assertArrayEquals("checkSnakeRule should not modify the path array",
                                originalPath, path);
                    }
                } catch (InvocationTargetException e) {
                    throw e.getCause();
                }
                return (boolean) (Boolean) result;
            }
        }
        Invoker invoker = new Invoker();
        if (invoker.initFailed) {
            runS4Test();
            return;
        }

        // Test capturing the first target (i.e. an empty path)
        Assert.assertTrue("A single target should be capturable",
                invoker.invoke(new double[] {40.1}, new double[] {-88.2}, new int[] {-1}, 0));
        Assert.assertTrue("Any target should be capturable when the path is empty",
                invoker.invoke(new double[] {40.0, 40.1}, new double[] {-88.0, -88.1}, new int[] {-1, -1}, 0));
        Assert.assertTrue("Any target should be capturable when the path is empty",
                invoker.invoke(new double[] {40.0, 40.1}, new double[] {-88.0, -88.1}, new int[] {-1, -1}, 1));

        // Test extending a path
        Assert.assertTrue("Any other target should be capturable when the path has only one capture",
                invoker.invoke(new double[] {40.0, 40.1}, new double[] {-88.0, -88.1}, new int[] {1, -1}, 0));
        Assert.assertTrue("It should be possible to capture a third target",
                invoker.invoke(new double[] {40.0, 40.1, 40.1}, new double[] {-88.0, -88.1, -88.0}, new int[] {0, 1, -1}, 2));
        /*
             0       (0 is the starting point)
            /
           v
          1     3    (clear shot to 3 from the last claimed target, 2)
           \
            v
             2
         */
        Assert.assertTrue("It should be possible to capture a target when no existing line is crossed by the new line",
                invoker.invoke(new double[] {40.13, 40.12, 40.11, 40.12}, new double[] {-88.24, -88.26, -88.24, -88.22},
                        new int[] {0, 1, 2, -1}, 3));
        Assert.assertTrue("It should be possible to capture a target when no existing line is crossed by the new line " +
                        "(regardless of target IDs)", /* same picture, labels 1 and 2 switched */
                invoker.invoke(new double[] {40.13, 40.11, 40.12, 40.12}, new double[] {-88.24, -88.24, -88.26, -88.22},
                        new int[] {0, 2, 1, -1}, 3));

        // Test a line in the way
        /*
             1
            /^
           v |
          2  |  3    (can't capture 3 because the line from 2 to 3 would cross the line from 0 to 1)
             |
             |
             0       (0 is the starting point, 2 is the last target captured)
         */
        Assert.assertFalse("It should not be possible to capture across a line formed by previous captures",
                invoker.invoke(new double[] {40.11, 40.13, 40.12, 40.12}, new double[] {-88.23, -88.23, -88.25, -88.21},
                        new int[] {0, 1, 2, -1}, 3));
        Assert.assertFalse("It should not be possible to capture across a line formed by previous captures " +
                        "(regardless of target IDs)", /* same picture, labels 1 and 2 switched */
                invoker.invoke(new double[] {40.11, 40.12, 40.13, 40.12}, new double[] {-88.23, -88.25, -88.23, -88.21},
                        new int[] {0, 2, 1, -1}, 3));
        /*
                       0   (0 is the last target captured)
                       ^
          (1 is start) |
                       |
              1--->3-->2

                 4         (can't capture 4 because the line from 0 to 4 would cross 1-3 or 3-2)
         */
        Assert.assertFalse("It should not be possible to capture across a line formed by previous captures",
                invoker.invoke(new double[] {40.16, 40.12, 40.12, 40.12, 40.10}, new double[] {-88.20, -88.29, -88.20, -88.24, -88.26},
                        new int[] {1, 3, 2, 0, -1}, 4));

        // Test dodging an existing line
        /*
                    3      (can't capture 4 directly from 2, but can from 3, which is the most recent capture)
                    ^
            4   0   |
                |   |
                |   |
                v   |
                1-->2
         */
        Assert.assertTrue("Only the potential new line from the most recent capture should be checked against existing lines",
                invoker.invoke(new double[] {40.14, 40.10, 40.10, 40.16, 40.14}, new double[] {-88.25, -88.25, -88.21, -88.21, -88.29},
                        new int[] {0, 1, 2, 3, -1}, 4));
        /*
            2  6  0
                 ^|
                3 |
               ^  |
              1   v          (1 is start, can reach 2 from the most recent capture of 4)
            4<----5
         */
        Assert.assertTrue("Only the potential new line from the most recent capture should be checked against existing lines",
                invoker.invoke(new double[] {40.16, 40.12, 40.16, 40.14, 40.11, 40.11, 40.16}, new double[] {-88.21, -88.25, -88.27, -88.23, -88.27, -88.21, -88.24},
                        new int[] {1, 3, 0, 5, 4, -1, -1}, 2));

        // Random tests (from JSON)
        for (JsonObject test : JsonResourceLoader.loadArray("snakerule")) {
            double[] latitudes = JsonResourceLoader.getDoubleArray(test.getAsJsonArray("lats"));
            double[] longitudes = JsonResourceLoader.getDoubleArray(test.getAsJsonArray("lngs"));
            int[] path = JsonResourceLoader.getIntArray(test.getAsJsonArray("path"));
            int tryVisit = test.get("try").getAsInt();
            Assert.assertEquals(test.get("answer").getAsBoolean(), invoker.invoke(latitudes, longitudes, path, tryVisit));
        }
    }

    private void runS4Test() {
        try {
            testTargetModeGameplay();
        } catch (Exception e) {
            throw new RuntimeException("testTargetModeGameplay (Checkpoint 4 compatibility) failed", e);
        }
    }

    @Test(timeout = 60000)
    @Graded(points = 20)
    public void testTargetModeGameplay() throws IllegalAccessException, InvocationTargetException {
        // Set up the game (with Checkpoints 1 and 3 compatibility)
        String gameId = RandomHelper.randomId();
        Intent intent = new Intent();
        intent.putExtra("game", gameId);
        intent.putExtra("mode", "target");
        intent.putExtra("proximityThreshold", 20);

        // Start the activity and check initial UI
        FirebaseMocker.mock();
        FirebaseMocker.setEmail(SampleData.USER_EMAIL);
        WebSocketMocker webSocketControl = WebSocketMocker.expectConnection();
        GameActivityLauncher launcher = new GameActivityLauncher(intent);
        ShadowGoogleMap map = Shadow.extract(launcher.getMap());
        if (webSocketControl.isConnected()) {
            // Checkpoint 4 compatibility
            JsonObject fullUpdate = createS4Game(gameId);
            webSocketControl.sendData(fullUpdate);
            Assert.assertEquals("Incorrect marker count",
                    fullUpdate.getAsJsonArray("targets").size(), map.getMarkers().size());
        } else {
            // Working on Checkpoint 0
            Assert.assertNotEquals("Markers should be created on the map at targets' positions",
                    0, map.getMarkers().size());
            try {
                Class<?> defaultTargetsClass = Class.forName("edu.illinois.cs.cs125.fall2019.mp.DefaultTargets");
                Method getPositionsMethod = defaultTargetsClass.getMethod("getPositions", Context.class);
                LatLng[] positions = (LatLng[]) getPositionsMethod.invoke(null, ApplicationProvider.getApplicationContext());
                Assert.assertEquals("There should be exactly one marker per target",
                        positions.length, map.getMarkers().size());
                for (LatLng position : positions) {
                    Marker marker = map.getMarkerAt(position);
                    Assert.assertNotNull("Target markers not positioned correctly", marker);
                    Assert.assertNotEquals("No targets should be claimed initially",
                            BitmapDescriptorFactory.HUE_GREEN, Shadow.<ShadowMarker>extract(marker).getHue(), 1e-3);
                }
            } catch (ClassNotFoundException | NoSuchMethodException e) {
                // Actually a malfunctioning Checkpoint 4 implementation
                Assert.fail("DefaultTargets was removed but no websocket connection was attempted");
            }
        }
        int originalMarkers = map.getMarkers().size();
        Assert.assertEquals("There shouldn't be any lines on the map initially",
                0, map.getPolylines().size());

        // Walk up the middle of the quad
        for (double latitude = 40.10663; latitude < 40.10837; latitude += 0.0002) {
            launcher.sendLocationUpdate(new LatLng(latitude, -88.227176));
        }
        Assert.assertEquals("No targets should have been claimed yet (too far away)",
                0, map.getMarkersWithColor(BitmapDescriptorFactory.HUE_GREEN).size());

        // Visit the Union's south side
        LatLng position = new LatLng(40.108895, -88.227158);
        launcher.sendLocationUpdate(position);
        Marker marker = map.getMarkerAt(position);
        Assert.assertNotNull("Claiming a target shouldn't remove the marker", marker);
        Assert.assertEquals("Visiting a target should claim it (turn its marker green)",
                BitmapDescriptorFactory.HUE_GREEN, Shadow.<ShadowMarker>extract(marker).getHue(), 1e-3);
        Assert.assertEquals("Only Illini Union South should have been claimed so far",
                1, map.getMarkersWithColor(BitmapDescriptorFactory.HUE_GREEN).size());
        Assert.assertEquals("Claiming a target shouldn't create extra markers",
                originalMarkers, map.getMarkers().size());

        // Get close enough to Siebel
        launcher.sendLocationUpdate(new LatLng(40.113901, -88.224784));
        marker = map.getMarkerAt(new LatLng(40.113957, -88.224927));
        Assert.assertEquals("Getting within PROXIMITY_THRESHOLD of a target should claim it",
                BitmapDescriptorFactory.HUE_GREEN, Shadow.<ShadowMarker>extract(marker).getHue(), 1e-3);
        Assert.assertNotEquals("Claiming a second target should add a connecting line",
                0, map.getPolylines().size());
        List<Polyline> polylines = map.getPolylinesConnecting(position, marker.getPosition()); // UnionSouth-Siebel
        Assert.assertEquals("The line wasn't added in the correct position",
                map.getPolylines().size(), polylines.size());
        Assert.assertNotEquals("Connecting lines should be green",
                0, polylines.stream().filter(p -> p.getColor() == Color.GREEN).count());

        // Visit the Union's north side
        position = new LatLng(40.110029, -88.227219);
        launcher.sendLocationUpdate(position);
        marker = map.getMarkerAt(position);
        Assert.assertEquals("Claiming a third target should also turn its marker green",
                BitmapDescriptorFactory.HUE_GREEN, Shadow.<ShadowMarker>extract(marker).getHue(), 1e-3);
        polylines = map.getPolylinesConnecting(position, new LatLng(40.113957, -88.224927)); // Siebel-UnionNorth
        Assert.assertNotEquals("Claiming a third target should add another line",
                0, polylines.size());
        int lastPolylinesCount = map.getPolylines().size();

        // Try to visit Noyes (blocked by UnionSouth-Siebel line)
        position = new LatLng(40.108433, -88.226481);
        launcher.sendLocationUpdate(position);
        marker = map.getMarkerAt(position);
        Assert.assertNotEquals("It should not be possible to claim targets across a line",
                BitmapDescriptorFactory.HUE_GREEN, Shadow.<ShadowMarker>extract(marker).getHue(), 1e-3);
        Assert.assertEquals("Failed target claims shouldn't create lines",
                lastPolylinesCount, map.getPolylines().size());

        // Visit the Henry Administration Building
        position = new LatLng(40.108436, -88.227874);
        launcher.sendLocationUpdate(position);
        marker = map.getMarkerAt(position);
        Assert.assertEquals("It should be possible to claim targets not blocked by lines",
                BitmapDescriptorFactory.HUE_GREEN, Shadow.<ShadowMarker>extract(marker).getHue(), 1e-3);
        Assert.assertTrue("Claiming another target should add another line", map.getPolylines().size() > lastPolylinesCount);

        // Retry Noyes now that there's a clear shot
        launcher.sendLocationUpdate(new LatLng(40.108428, -88.226250));
        marker = map.getMarkerAt(new LatLng(40.108433, -88.226481));
        Assert.assertEquals("It should be possible to claim targets no longer blocked by lines",
                BitmapDescriptorFactory.HUE_GREEN, Shadow.<ShadowMarker>extract(marker).getHue(), 1e-3);
        polylines = map.getPolylinesConnecting(position, marker.getPosition()); // HenryAdmin-Noyes
        Assert.assertNotEquals("Successfully claiming another target should add another line",
                0, polylines.size());
        lastPolylinesCount = map.getPolylines().size();

        // Try to visit Altgeld (blocked by UnionNorth-HenryAdmin line)
        launcher.sendLocationUpdate(new LatLng(40.109026, -88.228119));
        marker = map.getMarkerAt(new LatLng(40.109035, -88.228131));
        Assert.assertNotEquals("It should not be possible to claim targets blocked by lines",
                BitmapDescriptorFactory.HUE_GREEN, Shadow.<ShadowMarker>extract(marker).getHue(), 1e-3);
        Assert.assertEquals("Failed target claims should not create lines",
                lastPolylinesCount, map.getPolylines().size());

        // Try to visit the mid-block Springfield crosswalk (blocked by UnionSouth-Siebel and Siebel-UnionNorth)
        position = new LatLng(40.112768, -88.227339);
        launcher.sendLocationUpdate(position);
        marker = map.getMarkerAt(position);
        Assert.assertNotEquals("It should not be possible to claim targets blocked by multiple lines",
                BitmapDescriptorFactory.HUE_GREEN, Shadow.<ShadowMarker>extract(marker).getHue(), 1e-3);
        Assert.assertEquals("Failed target claims should not create lines",
                lastPolylinesCount, map.getPolylines().size());
    }

    private JsonObject createS4Game(String id) {
        JsonObject fullUpdate = JsonHelper.game(id, SampleData.USER_EMAIL, GameStateID.RUNNING, "target",
                JsonHelper.player(SampleData.USER_EMAIL, TeamID.TEAM_GREEN, PlayerStateID.PLAYING));
        fullUpdate.addProperty("type", "full");
        fullUpdate.addProperty("proximityThreshold", 20);
        fullUpdate.add("targets", JsonHelper.arrayOf(
                JsonHelper.target("UnionNorth", 40.110029, -88.227219, TeamID.OBSERVER),
                JsonHelper.target("UnionSouth", 40.108895, -88.227158, TeamID.OBSERVER),
                JsonHelper.target("Siebel", 40.113957, -88.224927, TeamID.OBSERVER),
                JsonHelper.target("Noyes", 40.108433, -88.226481, TeamID.OBSERVER),
                JsonHelper.target("HenryAdmin", 40.108436, -88.227874, TeamID.OBSERVER),
                JsonHelper.target("Altgeld", 40.109035, -88.228131, TeamID.OBSERVER),
                JsonHelper.target("CSWCVMNS", 40.112768, -88.227339, TeamID.OBSERVER),
                JsonHelper.target("PlantSciences", 40.102495, -88.221941, TeamID.OBSERVER)
        ));
        return fullUpdate;
    }

}
