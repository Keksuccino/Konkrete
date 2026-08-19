package de.keksuccino.konkrete.placeholder.placeholders.other.cpu;

import de.keksuccino.konkrete.placeholder.DeserializedPlaceholderString;
import de.keksuccino.konkrete.placeholder.Placeholder;
import de.keksuccino.konkrete.util.LocalizationUtils;
import de.keksuccino.konkrete.util.MathUtils;
import de.keksuccino.konkrete.util.PerformanceUtils;
import net.minecraft.client.resources.language.I18n;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.Arrays;
import java.util.List;

/** Exposes recent CPU utilization for the current JVM process as a percentage. */
public class JvmCpuUsagePlaceholder extends Placeholder {

    /** Creates the {@code jvmcpu} placeholder. */
    public JvmCpuUsagePlaceholder() {
        super("jvmcpu");
    }

    @Override
    public String getReplacementFor(DeserializedPlaceholderString dps) {
        double d = PerformanceUtils.getJvmCpuUsage();
        if (d < 0.0D) d = 0.0D;
        d = d * 100.0D;
        d = MathUtils.round(d, 1);
        return "" + d;
    }

    @Override
    public @Nullable List<String> getValueNames() {
        return null;
    }

    @Override
    public @NotNull String getDisplayName() {
        return I18n.get("konkrete.placeholders.jvm_cpu_usage");
    }

    @Override
    public List<String> getDescription() {
        return Arrays.asList(LocalizationUtils.splitLocalizedStringLines("konkrete.placeholders.jvm_cpu_usage.desc"));
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
