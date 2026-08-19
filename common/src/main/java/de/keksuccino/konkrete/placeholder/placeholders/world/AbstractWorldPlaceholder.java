package de.keksuccino.konkrete.placeholder.placeholders.world;

import de.keksuccino.konkrete.placeholder.Placeholder;
import de.keksuccino.konkrete.util.LocalizationUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.language.I18n;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.List;

/** Supplies client-thread-only player/level access and localization metadata for vanilla-state placeholders. */
public abstract class AbstractWorldPlaceholder extends Placeholder {

    private final List<String> alternativeIdentifiers;

    /** Creates a vanilla-state placeholder with the supplied namespace-local identifier. */
    public AbstractWorldPlaceholder(@NotNull String identifier) {
        this(identifier, new String[0]);
    }

    /** Creates a vanilla-state placeholder with immutable backward-compatible serialized aliases. */
    public AbstractWorldPlaceholder(@NotNull String identifier, @NotNull String... alternativeIdentifiers) {
        super(identifier);
        this.alternativeIdentifiers = List.of(alternativeIdentifiers);
    }

    @Override
    public boolean canRunAsync() {
        return false;
    }

    /** Returns the current local player, or {@code null} before a world is active. */
    @Nullable
    protected LocalPlayer getPlayer() {
        return Minecraft.getInstance().player;
    }

    /** Returns the current client level, or {@code null} before a world is active. */
    @Nullable
    protected ClientLevel getLevel() {
        return Minecraft.getInstance().level;
    }

    @Override
    public @Nullable List<String> getValueNames() {
        return List.of();
    }

    @Override
    public @Nullable List<String> getAlternativeIdentifiers() {
        return this.alternativeIdentifiers.isEmpty() ? null : this.alternativeIdentifiers;
    }

    /** Returns the base localization key used for display name and description metadata. */
    @NotNull
    protected abstract String getLocalizationBase();

    @Override
    public @NotNull String getDisplayName() {
        return I18n.get(this.getLocalizationBase());
    }

    @Override
    public List<String> getDescription() {
        return List.of(LocalizationUtils.splitLocalizedStringLines(this.getLocalizationBase() + ".desc"));
    }

    @Override
    public String getCategory() {
        return I18n.get("konkrete.placeholders.categories.world");
    }

}
