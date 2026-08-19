package de.keksuccino.konkrete.placeholder.placeholders.client;

import de.keksuccino.konkrete.placeholder.DeserializedPlaceholderString;
import de.keksuccino.konkrete.placeholder.Placeholder;
import de.keksuccino.konkrete.platform.Services;
import de.keksuccino.konkrete.util.LocalizationUtils;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.client.Minecraft;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.Arrays;
import java.util.List;

/** Reads total mods metadata from the active client for {@code totalmods}. */
public class TotalModsPlaceholder extends Placeholder {

    private static int cachedTotalMods = -10;

    /** Creates the {@code totalmods} placeholder. */
    public TotalModsPlaceholder() {
        super("totalmods");
    }

    @Override
    public boolean canRunAsync() {
        return false;
    }

    @Override
    public String getReplacementFor(DeserializedPlaceholderString dps) {
        int loaded = getLoadedMods();
        int total = getTotalMods();
        if (total < loaded) {
            total = loaded;
        }
        return "" + total;
    }

    private static int getLoadedMods() {
        return Services.PLATFORM.getLoadedModIds().size();
    }

    private static int getTotalMods() {
        if (cachedTotalMods == -10) {
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft == null) return -1;
            File modDirectory = new File(minecraft.gameDirectory, "mods");
            if (modDirectory.exists()) {
                int i = 0;
                File[] files = modDirectory.listFiles();
                if (files != null) {
                    for (File f : files) {
                        if (f.isFile() && f.getName().toLowerCase().endsWith(".jar")) {
                            i++;
                        }
                    }
                }
                cachedTotalMods = i;
            } else {
                cachedTotalMods = -1;
            }
        }
        return cachedTotalMods;
    }

    @Override
    public @Nullable List<String> getValueNames() {
        return null;
    }

    @Override
    public @NotNull String getDisplayName() {
        return I18n.get("konkrete.placeholders.totalmods");
    }

    @Override
    public List<String> getDescription() {
        return Arrays.asList(LocalizationUtils.splitLocalizedStringLines("konkrete.placeholders.totalmods.desc"));
    }

    @Override
    public String getCategory() {
        return I18n.get("konkrete.requirements.categories.client");
    }

    @Override
    public @NotNull DeserializedPlaceholderString getDefaultPlaceholderString() {
        DeserializedPlaceholderString dps = new DeserializedPlaceholderString();
        dps.placeholderIdentifier = this.getIdentifier();
        return dps;
    }

}
