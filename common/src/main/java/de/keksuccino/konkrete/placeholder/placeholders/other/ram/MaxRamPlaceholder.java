package de.keksuccino.konkrete.placeholder.placeholders.other.ram;

import de.keksuccino.konkrete.placeholder.DeserializedPlaceholderString;
import de.keksuccino.konkrete.placeholder.Placeholder;
import net.minecraft.client.resources.language.I18n;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/** Exposes the JVM's configured maximum heap in megabytes. */
public class MaxRamPlaceholder extends Placeholder {

    /** Creates the {@code maxram} placeholder. */
    public MaxRamPlaceholder() {
        super("maxram");
    }

    @Override
    public String getReplacementFor(DeserializedPlaceholderString dps) {
        long i = Runtime.getRuntime().maxMemory();
        return "" + bytesToMb(i);
    }

    private static long bytesToMb(long bytes) {
        return bytes / 1024L / 1024L;
    }

    @Override
    public @Nullable List<String> getValueNames() {
        return null;
    }

    @Override
    public @NotNull String getDisplayName() {
        return I18n.get("konkrete.placeholders.maxram");
    }

    @Override
    public List<String> getDescription() {
        return null;
    }

    @Override
    public String getCategory() {
        return I18n.get("konkrete.requirements.categories.other");
    }

    @Override
    public @NotNull DeserializedPlaceholderString getDefaultPlaceholderString() {
        DeserializedPlaceholderString dps = new DeserializedPlaceholderString();
        dps.placeholderIdentifier = this.getIdentifier();
        return dps;
    }

}
