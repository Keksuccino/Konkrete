package de.keksuccino.konkrete.placeholder.placeholders.other.cpu;

import com.mojang.blaze3d.platform.GLX;
import de.keksuccino.konkrete.placeholder.DeserializedPlaceholderString;
import de.keksuccino.konkrete.placeholder.Placeholder;
import de.keksuccino.konkrete.util.LocalizationUtils;
import net.minecraft.client.resources.language.I18n;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.Arrays;
import java.util.List;

/** Exposes the processor model reported by the host operating system. */
public class CpuInfoPlaceholder extends Placeholder {

    /** Creates the {@code cpuinfo} placeholder. */
    public CpuInfoPlaceholder() {
        super("cpuinfo");
    }

    @Override
    public @Nullable List<String> getAlternativeIdentifiers() {
        return List.of("drippy_cpu_info");
    }

    @Override
    public String getReplacementFor(DeserializedPlaceholderString dps) {
        return GLX._getCpuInfo();
    }

    @Override
    public @Nullable List<String> getValueNames() {
        return null;
    }

    @Override
    public @NotNull String getDisplayName() {
        return I18n.get("konkrete.placeholders.cpu_info");
    }

    @Override
    public List<String> getDescription() {
        return Arrays.asList(LocalizationUtils.splitLocalizedStringLines("konkrete.placeholders.cpu_info.desc"));
    }

    @Override
    public String getCategory() {
        return I18n.get("konkrete.requirements.categories.other");
    }

    @Override
    public @NotNull DeserializedPlaceholderString getDefaultPlaceholderString() {
        return new DeserializedPlaceholderString(this.getIdentifier(), null, "");
    }

}
