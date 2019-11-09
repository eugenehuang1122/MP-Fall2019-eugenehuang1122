package edu.illinois.cs.cs125.fall2019.mp;

import androidx.annotation.IdRes;
import androidx.test.core.app.ApplicationProvider;

import org.junit.Assert;

final class IdLookup {

    private IdLookup() { }

    @IdRes
    static int request(String name) {
        return ApplicationProvider.getApplicationContext().getResources().getIdentifier(name,
                "id", "edu.illinois.cs.cs125.fall2019.mp");
    }

    @IdRes
    static int require(String name) {
        int id = request(name);
        Assert.assertNotEquals("There is no control with ID '" + name + "'", 0, id);
        return id;
    }

}
