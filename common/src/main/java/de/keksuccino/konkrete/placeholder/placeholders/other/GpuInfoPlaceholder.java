package de.keksuccino.konkrete.placeholder.placeholders.other;

import com.mojang.blaze3d.systems.RenderSystem;
import de.keksuccino.konkrete.placeholder.DeserializedPlaceholderString;
import de.keksuccino.konkrete.placeholder.Placeholder;
import de.keksuccino.konkrete.util.LocalizationUtils;
import net.minecraft.client.resources.language.I18n;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.Arrays;
import java.util.List;

/** Exposes the active render device name. */
public class GpuInfoPlaceholder extends Placeholder {

    /** Creates the {@code gpuinfo} placeholder. */
    public GpuInfoPlaceholder() {
        super("gpuinfo");
    }

    /** Render-device state is sampled only on the Minecraft client thread. */
    @Override
    public boolean canRunAsync() {
        return false;
    }

    @Override
    public @Nullable List<String> getAlternativeIdentifiers() {
        return List.of("drippy_gpu_info");
    }

    @Override
    public String getReplacementFor(DeserializedPlaceholderString dps) {
        return RenderSystem.getDevice().getDeviceInfo().name();
    }

    @Override
    public @Nullable List<String> getValueNames() {
        return null;
    }

    @Override
    public @NotNull String getDisplayName() {
        return I18n.get("konkrete.placeholders.gpu_info");
    }

    @Override
    public List<String> getDescription() {
        return Arrays.asList(LocalizationUtils.splitLocalizedStringLines("konkrete.placeholders.gpu_info.desc"));
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
