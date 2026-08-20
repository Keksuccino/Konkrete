package de.keksuccino.konkrete.placeholder.placeholders.gui;

import de.keksuccino.konkrete.placeholder.DeserializedPlaceholderString;
import de.keksuccino.konkrete.placeholder.Placeholder;
import de.keksuccino.konkrete.util.input.MouseInput;
import de.keksuccino.konkrete.util.LocalizationUtils;
import net.minecraft.client.resources.language.I18n;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;

/** Reads mouse pos x from the active scaled GUI for {@code mouseposx}. */
public class MousePosXPlaceholder extends Placeholder {

    /** Creates the {@code mouseposx} placeholder. */
    public MousePosXPlaceholder() {
        super("mouseposx");
    }

    /** Mouse-handler and scaled-window state are sampled only on the Minecraft client thread. */
    @Override
    public boolean canRunAsync() {
        return false;
    }

    @Override
    public String getReplacementFor(DeserializedPlaceholderString dps) {
        return "" + MouseInput.getMouseX();
    }

    @Override
    public @Nullable List<String> getValueNames() {
        return null;
    }

    @Override
    public @NotNull String getDisplayName() {
        return I18n.get("konkrete.placeholders.mouseposx");
    }

    @Override
    public List<String> getDescription() {
        return Arrays.asList(LocalizationUtils.splitLocalizedStringLines("konkrete.placeholders.mouseposx.desc"));
    }

    @Override
    public String getCategory() {
        return I18n.get("konkrete.requirements.categories.gui");
    }

    @Override
    public @NotNull DeserializedPlaceholderString getDefaultPlaceholderString() {
        DeserializedPlaceholderString dps = new DeserializedPlaceholderString();
        dps.placeholderIdentifier = this.getIdentifier();
        return dps;
    }

}
