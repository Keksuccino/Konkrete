package de.keksuccino.konkrete.placeholder.placeholders.world;

import de.keksuccino.konkrete.mixin.mixins.common.client.AccessorMixinHud;
import de.keksuccino.konkrete.placeholder.DeserializedPlaceholderString;
import de.keksuccino.konkrete.util.rendering.text.ComponentParser;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

/** Exposes the current vanilla action-bar component as serialized JSON. */
public class ActionBarMessagePlaceholder extends AbstractWorldPlaceholder {

    /** Creates the {@code action_bar_message} placeholder. */
    public ActionBarMessagePlaceholder() {
        super("action_bar_message", "action_bar_message_fm");
    }

    @Override
    public String getReplacementFor(DeserializedPlaceholderString dps) {
        Component message = ((AccessorMixinHud) Minecraft.getInstance().gui.hud).get_overlayMessageString_Konkrete();
        return message != null ? ComponentParser.toJson(message) : "";
    }

    @Override
    protected @NotNull String getLocalizationBase() {
        return "konkrete.placeholders.world.action_bar_message";
    }

    @Override
    public @NotNull DeserializedPlaceholderString getDefaultPlaceholderString() {
        return new DeserializedPlaceholderString(this.getIdentifier(), null, "");
    }
}
