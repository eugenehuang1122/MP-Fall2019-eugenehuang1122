package edu.illinois.cs.cs125.fall2019.mp;

import com.google.android.gms.maps.model.LatLng;

abstract class WalkSimulator {

    private int steps = 0;

    protected abstract LatLng current();

    abstract boolean atEnd();

    protected final int getStep() {
        return steps;
    }

    final LatLng step() {
        if (atEnd()) {
            throw new IllegalStateException("Past the end of the path");
        }
        LatLng next = current();
        steps++;
        return next;
    }

}
