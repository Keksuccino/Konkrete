package de.keksuccino.konkrete.placeholder.placeholders.gui;

import de.keksuccino.konkrete.placeholder.DeserializedPlaceholderString;
import de.keksuccino.konkrete.placeholder.Placeholder;
import de.keksuccino.konkrete.util.LocalizationUtils;
import de.keksuccino.konkrete.util.ScreenUtils;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.I18n;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/** Exposes a configurable identifier for the active screen, defaulting to its fully qualified class name. */
public class CurrentScreenIdentifierPlaceholder extends Placeholder {

    /** Creates the {@code screenid} placeholder. */
    public CurrentScreenIdentifierPlaceholder() {
        super("screenid");
    }

    @Override
    public boolean canRunAsync() {
        return false;
    }

    @Override
    public String getReplacementFor(DeserializedPlaceholderString dps) {
        Screen screen = ScreenUtils.getScreen();
        return screen != null ? ScreenIdentifierProviders.get().identify(screen) : "";
    }

    @Override
    public @Nullable List<String> getValueNames() {
        return List.of();
    }

    @Override
    public @NotNull String getDisplayName() {
        return I18n.get("konkrete.placeholders.screen_identifier");
    }

    @Override
    public List<String> getDescription() {
        return List.of(LocalizationUtils.splitLocalizedStringLines("konkrete.placeholders.screen_identifier.desc"));
    }

    @Override
    public String getCategory() {
        return I18n.get("konkrete.requirements.categories.gui");
    }

    @Override
    public @NotNull DeserializedPlaceholderString getDefaultPlaceholderString() {
        return new DeserializedPlaceholderString(this.getIdentifier(), null, "");
    }

}
