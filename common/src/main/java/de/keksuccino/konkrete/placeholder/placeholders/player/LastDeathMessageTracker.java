package de.keksuccino.konkrete.placeholder.placeholders.player;

import de.keksuccino.konkrete.util.rendering.text.ComponentParser;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Stores the most recently received local-player death message. Both forms are captured at packet time so a later
 * language, registry, or connection change cannot alter the historical placeholder value.
 */
public final class LastDeathMessageTracker {

    @Nullable private static String cachedPlainText;
    @Nullable private static String cachedJson;

    private LastDeathMessageTracker() {
    }

    /** Replaces the tracked message, or clears both representations when {@code deathMessage} is {@code null}. */
    public static synchronized void record(@Nullable Component deathMessage) {
        if (deathMessage == null) {
            cachedPlainText = null;
            cachedJson = null;
            return;
        }
        cachedPlainText = deathMessage.getString();
        cachedJson = serialize(deathMessage);
    }

    /** Returns the captured plain text, or {@code null} until a death packet has been recorded. */
    @Nullable
    public static synchronized String getPlainText() {
        return cachedPlainText;
    }

    /** Returns the captured component JSON, or {@code null} until a death packet has been recorded. */
    @Nullable
    public static synchronized String getJson() {
        return cachedJson;
    }

    @NotNull
    private static String serialize(@NotNull Component component) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level != null) return ComponentParser.toJson(component, minecraft.level.registryAccess());
        if (minecraft.getConnection() != null) return ComponentParser.toJson(component, minecraft.getConnection().registryAccess());
        return ComponentParser.toJson(component);
    }

}
