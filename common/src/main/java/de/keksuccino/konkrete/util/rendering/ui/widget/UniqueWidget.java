package de.keksuccino.konkrete.util.rendering.ui.widget;

import de.keksuccino.konkrete.util.ClassExtender;
import net.minecraft.client.gui.components.AbstractWidget;
import org.jetbrains.annotations.Nullable;

/**
 * Gets applied to the {@link AbstractWidget} class, to be able to set identifiers to instances of it.
 */
@ClassExtender(AbstractWidget.class)
public interface UniqueWidget {

    /** Associates a stable identifier with this widget and returns the widget. */
    AbstractWidget setWidgetIdentifierKonkrete(@Nullable String identifier);

    /** Returns this widget's stable identifier, or {@code null} when none is assigned. */
    String getWidgetIdentifierKonkrete();

}
