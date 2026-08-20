package de.keksuccino.konkrete.placeholder.placeholders.client;

import de.keksuccino.konkrete.mixin.mixins.common.client.AccessorMixinLevelLoadingScreen;
import de.keksuccino.konkrete.mixin.mixins.common.client.AccessorMixinProgressScreen;
import de.keksuccino.konkrete.placeholder.DeserializedPlaceholderString;
import de.keksuccino.konkrete.placeholder.Placeholder;
import de.keksuccino.konkrete.util.LocalizationUtils;
import de.keksuccino.konkrete.util.ScreenUtils;
import net.minecraft.client.gui.screens.LevelLoadingScreen;
import net.minecraft.client.gui.screens.ProgressScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.I18n;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/** Exposes the active vanilla world/load-task progress as an integer percentage. */
public class WorldLoadProgressPlaceholder extends Placeholder {

    /** Creates the {@code world_load_progress} placeholder. */
    public WorldLoadProgressPlaceholder() {
        super("world_load_progress");
    }

    @Override
    public boolean canRunAsync() {
        return false;
    }

    @Override
    public String getReplacementFor(DeserializedPlaceholderString dps) {
        Screen screen = ScreenUtils.getScreen();
        if (screen instanceof ProgressScreen progress) return Integer.toString(((AccessorMixinProgressScreen) progress).get_progress_Konkrete());
        if (screen instanceof LevelLoadingScreen loading) return Integer.toString((int) (((AccessorMixinLevelLoadingScreen) loading).get_loadTracker_Konkrete().serverProgress() * 100.0F));
        return "0";
    }

    @Override
    public @Nullable List<String> getValueNames() {
        return List.of();
    }

    @Override
    public @NotNull String getDisplayName() {
        return I18n.get("konkrete.placeholders.world_load_progress");
    }

    @Override
    public List<String> getDescription() {
        return List.of(LocalizationUtils.splitLocalizedStringLines("konkrete.placeholders.world_load_progress.desc"));
    }

    @Override
    public String getCategory() {
        return I18n.get("konkrete.requirements.categories.client");
    }

    @Override
    public @NotNull DeserializedPlaceholderString getDefaultPlaceholderString() {
        return new DeserializedPlaceholderString(this.getIdentifier(), null, "");
    }

}
