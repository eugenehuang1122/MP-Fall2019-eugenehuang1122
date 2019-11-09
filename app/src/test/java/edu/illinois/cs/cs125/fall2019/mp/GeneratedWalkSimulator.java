package edu.illinois.cs.cs125.fall2019.mp;

import com.google.android.gms.maps.model.LatLng;

import java.util.Random;

abstract class GeneratedWalkSimulator extends WalkSimulator {

    double lastLat;
    double lastLng;
    Random random;

    GeneratedWalkSimulator(double startLat, double startLng) {
        lastLat = startLat;
        lastLng = startLng;
        random = new Random();
    }

    GeneratedWalkSimulator() {
        this(RandomHelper.randomLat(), RandomHelper.randomLng());
    }

    abstract void updatePosition(Random random);

    @Override
    boolean atEnd() {
        return false;
    }

    @Override
    protected LatLng current() {
        updatePosition(random);
        return new LatLng(lastLat, lastLng);
    }

}
