package de.keksuccino.konkrete.util.rendering.ui.screen.scrollnormalizer;

import de.keksuccino.konkrete.mixin.mixins.common.client.AccessorMixinScreen;
import de.keksuccino.konkrete.util.rendering.ui.widget.UniqueWidget;
import net.minecraft.client.gui.components.AbstractSelectionList;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.events.ContainerEventHandler;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.gui.screens.options.OptionsSubScreen;
import net.minecraft.client.gui.screens.packs.PackSelectionScreen;
import net.minecraft.client.gui.screens.worldselection.SelectWorldScreen;
import net.minecraft.network.chat.ComponentContents;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.contents.TranslatableContents;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Normalizes scroll input across vanilla screens through caller-supplied screen identifiers. */
public class ScrollScreenNormalizer {

    private static final Logger LOGGER = LogManager.getLogger();
    private static final List<ScrollableScreenBlacklistRule> SCROLLABLE_SCREEN_BLACKLIST = new ArrayList<>();

    static {

        addScrollableScreenBlacklistRule(screen -> (screen instanceof SelectWorldScreen));
        addScrollableScreenBlacklistRule(screen -> (screen instanceof JoinMultiplayerScreen));
        addScrollableScreenBlacklistRule(screen -> (screen instanceof PackSelectionScreen));

    }

    /** Transforms scrollable screen using the supplied settings. */
    @NotNull
    public static Screen normalizeScrollableScreen(@NotNull Screen screen) {

        if (isBlacklisted(screen)) return screen;
        if (!ScrollScreenNormalizerHandler.shouldNormalize(screen)) return screen;
        List<AbstractWidget> extracted = extractAllWidgetsFromScrollListsOfScreen(screen);
        AccessorMixinScreen accessor = ((AccessorMixinScreen)screen);

        extractAllScrollListsOfScreen(screen).forEach(scroll -> {
            scroll.updateSizeAndPosition(1000000, 1000000, 0);
            accessor.get_children_Konkrete().remove(scroll);
            accessor.get_narratables_Konkrete().remove(scroll);
            accessor.get_renderables_Konkrete().remove(scroll);
        });
        extracted.forEach(widget -> {
            accessor.get_children_Konkrete().remove(widget);
            accessor.get_narratables_Konkrete().remove(widget);
            accessor.get_renderables_Konkrete().remove(widget);
        });

        int pos = 50;
        Map<String, Integer> ids = new HashMap<>();
        for (AbstractWidget widget : extracted) {

            widget.setX(pos);
            widget.setY(pos);
            pos++;

            if (screen instanceof OptionsSubScreen) {
                if (widget instanceof UniqueWidget w) {
                    StringBuilder id = new StringBuilder("options");
                    buildId(widget.getMessage().getContents(), id);
                    String idString = id.toString();
                    if (idString.equals("options")) idString += "_generic";
                    if (ids.containsKey(idString)) {
                        int count = ids.get(idString);
                        count++;
                        ids.put(idString, count);
                        idString = idString + "_" + count;
                    } else {
                        ids.put(idString, 0);
                    }
                    w.setWidgetIdentifierKonkrete(idString);
                }
            }

            accessor.get_children_Konkrete().add(widget);
            accessor.get_renderables_Konkrete().add(widget);
            accessor.get_narratables_Konkrete().add(widget);

        }

        return screen;

    }

    @NotNull
    private static StringBuilder buildId(@NotNull ComponentContents contents, @NotNull StringBuilder builder) {
        if (contents instanceof TranslatableContents t) {
            builder.append("_");
            Object[] args = t.getArgs();
            if (args.length > 0) {
                if (args[0] instanceof MutableComponent c) {
                    if (c.getContents() instanceof TranslatableContents t2) {
                        builder.append(t2.getKey());
                        return builder;
                    }
                }
            }
            builder.append(t.getKey());
        }
        return builder;
    }

    /** Returns whether blacklisted. */
    public static boolean isBlacklisted(Screen screen) {
        if (screen == null) return false;
        for (ScrollableScreenBlacklistRule e : SCROLLABLE_SCREEN_BLACKLIST) {
            if (e.isBlacklisted(screen)) return true;
        }
        return false;
    }

    /** Extracts all scroll lists of screen from the supplied UI state. */
    @NotNull
    public static List<AbstractSelectionList<?>> extractAllScrollListsOfScreen(@NotNull Screen screen) {
        List<AbstractSelectionList<?>> list = new ArrayList<>();
        screen.children().forEach(o -> {
            if (o instanceof AbstractSelectionList<?> l) list.add(l);
        });
        return list;
    }

    /** Extracts all widgets from scroll lists of screen from the supplied UI state. */
    @NotNull
    public static List<AbstractWidget> extractAllWidgetsFromScrollListsOfScreen(@NotNull Screen screen) {
        List<AbstractWidget> list = new ArrayList<>();
        for (Object o : ((AccessorMixinScreen)screen).get_children_Konkrete()) {
            if (o instanceof AbstractSelectionList<?> sel) {
                sel.children().forEach(entry -> {
                    if (entry instanceof ContainerEventHandler h) {
                        extractWidgetsRecursively(h, list);
                    }
                });
            }
        }
        return list;
    }

    private static void extractWidgetsRecursively(@NotNull ContainerEventHandler container, @NotNull List<AbstractWidget> list) {
        container.children().forEach(child -> {
            if (child instanceof ContainerEventHandler childContainer) {
                // Recursively extract widgets from nested containers
                extractWidgetsRecursively(childContainer, list);
            } else if (child instanceof AbstractWidget widget) {
                list.add(widget);
            }
        });
    }

    /** Adds scrollable screen blacklist rule to this scroll screen normalizer. */
    public static void addScrollableScreenBlacklistRule(@NotNull ScrollScreenNormalizer.ScrollableScreenBlacklistRule rule) {
        SCROLLABLE_SCREEN_BLACKLIST.add(rule);
    }

    /** Excludes screens whose native scroll behavior must remain untouched. */
    @FunctionalInterface
    public interface ScrollableScreenBlacklistRule {
        /** Returns whether normalization must be skipped for the supplied screen. */
        boolean isBlacklisted(@NotNull Screen screen);
    }

}
