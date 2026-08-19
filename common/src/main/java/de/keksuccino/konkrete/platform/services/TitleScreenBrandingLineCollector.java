package de.keksuccino.konkrete.platform.services;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * Adapts a reversible indexed branding source to Konkrete's top-to-bottom title-screen contract.
 *
 * <p>Loader title screens can request reversed enumeration when callback index zero is positioned at the bottom.
 * Konkrete instead positions list index zero at the top, so the source must include Minecraft's line and use its
 * natural order.</p>
 */
public final class TitleScreenBrandingLineCollector {

    private TitleScreenBrandingLineCollector() {
    }

    /**
     * Collects and maps all title-screen branding lines in natural visual order.
     *
     * @param source reversible loader branding source
     * @param mapper line mapper
     * @param <S> source line type
     * @param <T> mapped line type
     * @return immutable branding lines in top-to-bottom order
     */
    public static <S, T> List<T> collectTopToBottom(@Nonnull ReversibleLineSource<S> source, @Nonnull Function<? super S, ? extends T> mapper) {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(mapper, "mapper");
        List<T> lines = new ArrayList<>();
        source.forEachLine(true, false, (lineIndex, line) -> lines.add(mapper.apply(line)));
        return List.copyOf(lines);
    }

    /**
     * Supplies indexed branding lines with configurable Minecraft inclusion and ordering.
     *
     * @param <T> source line type
     */
    @FunctionalInterface
    public interface ReversibleLineSource<T> {

        /**
         * Enumerates branding lines.
         *
         * @param includeMinecraft whether to include Minecraft's version line
         * @param reverse whether to enumerate in reverse order
         * @param lineConsumer indexed line consumer
         */
        void forEachLine(boolean includeMinecraft, boolean reverse, @Nonnull BiConsumer<Integer, T> lineConsumer);

    }

}
