package de.keksuccino.konkrete.util;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class LocalizationUtilsTest {

    @AfterEach
    void resetSupplier() {
        LocalizationUtils.resetLocalizationKeySupplier();
    }

    @Test
    void returnsDefensiveSnapshotsFromConfiguredSupplier() {
        List<String> publishedKeys = new ArrayList<>(List.of("example.first"));
        LocalizationUtils.setLocalizationKeySupplier(() -> publishedKeys);

        List<String> snapshot = LocalizationUtils.getLocalizationKeys();
        publishedKeys.add("example.second");

        assertEquals(List.of("example.first"), snapshot);
        assertEquals(List.of("example.first", "example.second"), LocalizationUtils.getLocalizationKeys());
    }

    @Test
    void resetRestoresEmptyDefaultAndNullSupplierIsRejected() {
        LocalizationUtils.setLocalizationKeySupplier(() -> List.of("example.key"));
        LocalizationUtils.resetLocalizationKeySupplier();

        assertEquals(List.of(), LocalizationUtils.getLocalizationKeys());
        assertThrows(NullPointerException.class, () -> LocalizationUtils.setLocalizationKeySupplier(null));
    }

    @Test
    void nullValuesFromIntegrationAreTreatedAsEmptySnapshots() {
        LocalizationUtils.setLocalizationKeySupplier(() -> null);

        assertEquals(List.of(), LocalizationUtils.getLocalizationKeys());
    }

}
