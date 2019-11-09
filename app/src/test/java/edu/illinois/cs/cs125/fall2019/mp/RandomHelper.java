package edu.illinois.cs.cs125.fall2019.mp;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;

import java.util.Random;
import java.util.UUID;

public final class RandomHelper {

    private RandomHelper() {}

    private static Random random = new Random();

    private static String[] emailTlds = new String[] {"com", "net", "org"};

    private static String[] emailNames = new String[] {"nobody", "someone", "someone.else", "user", "player", "test", "fake"};

    static int randomTeam() {
        return random.nextInt(TeamID.NUM_TEAMS) + TeamID.MIN_TEAM;
    }

    static int randomRole() {
        return random.nextInt(TeamID.NUM_TEAMS + 1);
    }

    static String randomMode() {
        if (random.nextBoolean()) {
            return "area";
        } else {
            return "target";
        }
    }

    static String randomEmail(String... alreadyUsed) {
        String tld = emailTlds[random.nextInt(emailTlds.length)];
        String name = emailNames[random.nextInt(emailNames.length)];
        String email = name + "@example." + tld;
        for (String taken : alreadyUsed) {
            if (taken.equals(email)) {
                return randomEmail(alreadyUsed);
            }
        }
        return email;
    }

    public static String randomId() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    public static double randomLat() {
        return 40.109395 + randomPlusMinusRange(0.01);
    }

    public static double randomLng() {
        return -88.227212 + randomPlusMinusRange(0.01);
    }

    public static LatLngBounds randomBounds() {
        double lat1 = randomLat();
        double lat2 = randomLat();
        double lng1 = randomLng();
        double lng2 = randomLng();
        double northLat = Math.max(lat1, lat2);
        double eastLng = Math.max(lng1, lng2);
        double southLat = Math.min(lat1, lat2);
        double westLng = Math.min(lng1, lng2);
        return new LatLngBounds(new LatLng(southLat, westLng), new LatLng(northLat, eastLng));
    }

    static double randomPlusMinusRange(double magnitude) {
        return random.nextDouble() * magnitude * 2 - magnitude;
    }

    public static LatLng randomPointIn(LatLngBounds bounds) {
        double latDiff = bounds.northeast.latitude - bounds.southwest.latitude;
        double lngDiff = bounds.northeast.longitude - bounds.southwest.longitude;
        return new LatLng(bounds.southwest.latitude + random.nextDouble() * latDiff,
                bounds.southwest.longitude + random.nextDouble() * lngDiff);
    }

}
