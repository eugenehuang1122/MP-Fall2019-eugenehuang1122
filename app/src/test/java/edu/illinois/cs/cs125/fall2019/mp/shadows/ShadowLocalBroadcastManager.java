package edu.illinois.cs.cs125.fall2019.mp.shadows;

import android.content.BroadcastReceiver;
import android.content.IntentFilter;

import androidx.core.util.Pair;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;
import org.robolectric.annotation.RealObject;
import org.robolectric.shadow.api.Shadow;
import org.robolectric.util.ReflectionHelpers;

import java.util.ArrayList;
import java.util.List;

import edu.illinois.cs.cs125.robolectricsecurity.Trusted;

@Trusted
@Implements(LocalBroadcastManager.class)
public class ShadowLocalBroadcastManager {

    @RealObject private LocalBroadcastManager self;
    private static List<Pair<LocalBroadcastManager, BroadcastReceiver>> receivers = new ArrayList<>();

    @Implementation
    protected void registerReceiver(BroadcastReceiver receiver, IntentFilter filter) {
        Shadow.directlyOn(self, LocalBroadcastManager.class, "registerReceiver",
                new ReflectionHelpers.ClassParameter<>(BroadcastReceiver.class, receiver),
                new ReflectionHelpers.ClassParameter<>(IntentFilter.class, filter));
        receivers.add(Pair.create(self, receiver));
    }

    public static void reset() {
        for (Pair<LocalBroadcastManager, BroadcastReceiver> r : receivers) {
            r.first.unregisterReceiver(r.second);
        }
        receivers.clear();
    }

}
