package edu.illinois.cs.cs125.fall2019.mp.shadows;

import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;

import java.lang.reflect.Constructor;
import java.util.Arrays;

import edu.illinois.cs.cs125.robolectricsecurity.Trusted;

@SuppressWarnings({"ConstantConditions", "unchecked"})
@Trusted
public final class MockedWrapperInstantiator {

    private MockedWrapperInstantiator() {}

    public static <T> T create(Class<T> clazz) {
        Constructor<T> oneArgCtor = (Constructor<T>) Arrays.stream(clazz.getConstructors())
            .filter(ctor -> ctor.getParameterCount() == 1).findAny().orElse(null);
        try {
            return oneArgCtor.newInstance(PowerMockito.mock(oneArgCtor.getParameterTypes()[0],
                Mockito.RETURNS_DEEP_STUBS));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
