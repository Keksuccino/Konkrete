package de.keksuccino.konkrete.util.rendering.text.markdown;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MarkdownRendererConfigurationTest {

    @Test
    void changingPreprocessorOutputRebuildsFragmentsOnTick() {
        AtomicReference<String> dynamicValue = new AtomicReference<>("FB");
        MarkdownRenderer renderer = new MarkdownRenderer(new FixedTextRenderer(), text -> text.replace("{value}", dynamicValue.get()));
        renderer.setText("Value: {value}");

        renderer.tick();
        assertEquals("Value: FB", joinedFragmentText(renderer));

        dynamicValue.set("Ea");
        assertEquals("FB".hashCode(), "Ea".hashCode());
        renderer.tick();
        assertEquals("Value: Ea", joinedFragmentText(renderer));
    }

    @Test
    void replacingTextRendererRecalculatesFragmentMetrics() {
        MarkdownRenderer renderer = new MarkdownRenderer(new FixedTextRenderer(1.0F, 9.0F));
        renderer.setText("width");
        renderer.tick();
        float originalWidth = renderer.getRealWidth();

        MarkdownRenderer.TextRenderer replacement = new FixedTextRenderer(2.0F, 12.0F);
        renderer.setTextRenderer(replacement);
        renderer.tick();

        assertSame(replacement, renderer.getTextRenderer());
        assertEquals(originalWidth + 5.0F, renderer.getRealWidth(), 0.001F);
        assertEquals(12.0F + renderer.getLineSpacing() + (renderer.getBorder() * 2.0F), renderer.getRealHeight(), 0.001F);
    }

    @Test
    void autoWrappingPreservesTextAndFormattingAcrossGeneratedLines() {
        MarkdownRenderer renderer = new MarkdownRenderer(new FixedTextRenderer());
        renderer.setOptimalWidth(8.0F);
        renderer.setText("§nOne two");

        renderer.tick();

        assertEquals(2, renderer.lines.size());
        assertEquals("One two", joinedFragmentText(renderer));
        assertTrue(renderer.lines.get(0).fragments.get(0).buildRenderComponent(false).getStyle().isUnderlined());
        assertTrue(renderer.lines.get(1).fragments.get(0).buildRenderComponent(false).getStyle().isUnderlined());
    }

    private static String joinedFragmentText(MarkdownRenderer renderer) {
        StringBuilder result = new StringBuilder();
        for (MarkdownTextFragment fragment : renderer.fragments) result.append(fragment.text);
        return result.toString();
    }

    private static final class FixedTextRenderer implements MarkdownRenderer.TextRenderer {

        private final float widthPerCharacter;
        private final float height;

        private FixedTextRenderer() {
            this(1.0F, 9.0F);
        }

        private FixedTextRenderer(float widthPerCharacter, float height) {
            this.widthPerCharacter = widthPerCharacter;
            this.height = height;
        }

        @Override
        public float getWidth(@NotNull Component text) {
            return text.getString().length() * this.widthPerCharacter;
        }

        @Override
        public float getWidth(@NotNull String text) {
            return text.length() * this.widthPerCharacter;
        }

        @Override
        public float getHeight() {
            return this.height;
        }

        @Override
        public void render(@NotNull GuiGraphicsExtractor graphics, @NotNull Component text, float x, float y, int color, boolean shadow) {
        }

    }

}
