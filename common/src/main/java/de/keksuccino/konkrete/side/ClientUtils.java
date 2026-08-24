package de.keksuccino.konkrete.side;

import de.keksuccino.konkrete.platform.Services;

public class ClientUtils {

    public static void assertIsOnClient() {
        if (Services.PLATFORM.isOnClient()) throw new RuntimeException("Wrong side! Should be client-side, but was server instead!");
    }

}
