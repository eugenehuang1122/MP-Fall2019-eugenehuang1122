package edu.illinois.cs.cs125.fall2019.mp.shadows;

import com.google.android.gms.dynamic.IObjectWrapper;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;

import org.junit.Assert;
import org.powermock.api.mockito.PowerMockito;
import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;
import org.robolectric.shadow.api.Shadow;

@Implements(BitmapDescriptorFactory.class)
public class ShadowBitmapDescriptorFactory {

    @Implementation
    protected static BitmapDescriptor defaultMarker(float hue) {
        Assert.assertTrue("Hues must be greater than or equal to 0", hue >= 0.0);
        Assert.assertTrue("Hues must be less than 360", hue < 360.0);
        BitmapDescriptor icon = MockedWrapperInstantiator.create(BitmapDescriptor.class);
        ShadowBitmapDescriptor descriptor = Shadow.extract(icon);
        descriptor.setHue(hue);
        return icon;
    }

    @Implementation
    protected static BitmapDescriptor defaultMarker() {
        return defaultMarker(0.0f);
    }

}
