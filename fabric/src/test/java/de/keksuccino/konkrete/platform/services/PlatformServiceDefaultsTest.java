package de.keksuccino.konkrete.platform.services;

import com.mojang.blaze3d.platform.InputConstants;
import de.keksuccino.konkrete.util.mod.UniversalModContainer;
import net.minecraft.client.KeyMapping;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlatformServiceDefaultsTest {

    @Test
    void compatibilityLayerDefaultsToImmutableEmptyBranding() {
        IPlatformCompatibilityLayer compatibilityLayer = new IPlatformCompatibilityLayer() {};

        assertTrue(compatibilityLayer.getTitleScreenBrandingLines().isEmpty());
        assertThrows(UnsupportedOperationException.class, () -> compatibilityLayer.getTitleScreenBrandingLines().add(null));
    }

    @Test
    void legacyPlatformHelperGetsImmutableMinimalModSnapshots() {
        IPlatformHelper platformHelper = new LegacyPlatformHelper();

        List<UniversalModContainer> mods = platformHelper.getLoadedMods();

        assertEquals(List.of(new UniversalModContainer("alpha", "alpha", "", "", List.of()), new UniversalModContainer("beta", "beta", "", "", List.of())), mods);
        assertEquals(new UniversalModContainer("alpha", "alpha", "", "", List.of()), platformHelper.getLoadedMod("alpha"));
        assertNull(platformHelper.getLoadedMod("missing"));
        assertThrows(UnsupportedOperationException.class, () -> mods.add(new UniversalModContainer("gamma", "gamma", "", "", List.of())));
    }

    @Test
    void legacyPlatformHelperDefaultsToImmutableEmptyResources() {
        IPlatformHelper platformHelper = new LegacyPlatformHelper();

        assertTrue(platformHelper.getLoadedClientResourceLocations().isEmpty());
        assertThrows(UnsupportedOperationException.class, () -> platformHelper.getLoadedClientResourceLocations().add(null));
    }

    private static final class LegacyPlatformHelper implements IPlatformHelper {

        @Override
        public String getPlatformName() {
            return "legacy";
        }

        @Override
        public String getPlatformDisplayName() {
            return "Legacy";
        }

        @Override
        public String getLoaderVersion() {
            return "1.0.0";
        }

        @Override
        public boolean isModLoaded(String modId) {
            return this.getLoadedModIds().contains(modId);
        }

        @Override
        public String getModVersion(String modId) {
            return "1.0.0";
        }

        @Override
        public List<String> getLoadedModIds() {
            return List.of("alpha", "beta");
        }

        @Override
        public boolean isDevelopmentEnvironment() {
            return false;
        }

        @Override
        public boolean isOnClient() {
            return false;
        }

        @Override
        public InputConstants.Key getKeyMappingKey(KeyMapping keyMapping) {
            return InputConstants.UNKNOWN;
        }

    }

}
