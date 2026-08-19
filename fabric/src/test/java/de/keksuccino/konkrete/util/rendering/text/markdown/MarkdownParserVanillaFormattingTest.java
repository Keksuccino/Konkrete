package de.keksuccino.konkrete.util.rendering.text.markdown;

import de.keksuccino.konkrete.util.rendering.DrawableColor;
import de.keksuccino.konkrete.util.rendering.text.color.TextColorFormatter;
import de.keksuccino.konkrete.util.rendering.text.color.TextColorFormatterRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceLock;

import java.awt.Color;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ResourceLock("TextColorFormatterRegistry global state")
class MarkdownParserVanillaFormattingTest {

    private static final TextColorFormatter CUSTOM_FORMATTER = new TextColorFormatter('q', DrawableColor.of(new Color(18, 52, 86)));
    private static final Font TEST_FONT = new Font(null) {
        @Override
        public int width(String text) {
            return text.length();
        }

        @Override
        public int width(FormattedText text) {
            return text.getString().length();
        }
    };

    @BeforeAll
    static void registerCustomFormatter() {
        TextColorFormatterRegistry.register("markdown_parser_formatting_test", CUSTOM_FORMATTER);
    }

    @Test
    void vanillaFormattingPersistsAcrossWordAndLineFragmentsAndLeavesNoControlText() {
        List<MarkdownTextFragment> fragments = parse("§nOne two\nthree §rplain", true);

        assertTrue(styleOf(fragmentContaining(fragments, "One")).isUnderlined());
        assertTrue(styleOf(fragmentContaining(fragments, "two")).isUnderlined());
        assertTrue(styleOf(fragmentContaining(fragments, "three")).isUnderlined());
        assertFalse(styleOf(fragmentContaining(fragments, "plain")).isUnderlined());
        assertFalse(joinText(fragments).contains("§"));
    }

    @Test
    void vanillaFormattingWorksWhenMarkdownParsingIsDisabled() {
        List<MarkdownTextFragment> fragments = parse("§oOne two §rplain", false);

        assertTrue(styleOf(fragmentContaining(fragments, "One")).isItalic());
        assertTrue(styleOf(fragmentContaining(fragments, "two")).isItalic());
        assertFalse(styleOf(fragmentContaining(fragments, "plain")).isItalic());
    }

    @Test
    void formattingTransitionsOwnImmediatelyFollowingWhitespace() {
        MarkdownTextFragment enabledSpace = fragmentWithExactText(parse("base§n §rnext", true), " ");
        MarkdownTextFragment resetSpace = fragmentWithExactText(parse("§nbase§r next", true), " ");

        assertTrue(styleOf(enabledSpace).isUnderlined());
        assertFalse(styleOf(resetSpace).isUnderlined());
    }

    @Test
    void resetClearsOnlyVanillaFormattingInsideMarkdownFormatting() {
        List<MarkdownTextFragment> fragments = parse("**§lA §rB**", true);
        MarkdownTextFragment beforeReset = fragmentContaining(fragments, "A");
        MarkdownTextFragment afterReset = fragmentContaining(fragments, "B");

        assertTrue(beforeReset.bold);
        assertTrue(afterReset.bold);
        assertTrue(beforeReset.minecraftFormatting.applyTo(Style.EMPTY).isBold());
        assertFalse(afterReset.minecraftFormatting.applyTo(Style.EMPTY).isBold());
        assertTrue(styleOf(afterReset).isBold());
    }

    @Test
    void leadingCodesKeepMarkdownStartOfLineRecognitionAcrossConsecutiveCodes() {
        List<MarkdownTextFragment> fragments = parse("§c§l# Heading", true);
        MarkdownTextFragment heading = fragmentContaining(fragments, "Heading");

        assertEquals(MarkdownTextFragment.HeadlineType.BIGGEST, heading.headlineType);
        assertEquals(TextColor.fromLegacyFormat(ChatFormatting.RED), styleOf(heading).getColor());
        assertTrue(styleOf(heading).isBold());
    }

    @Test
    void leadingCodesKeepQuoteAndListRecognition() {
        MarkdownTextFragment quote = fragmentContaining(parse("§c> Quote", true), "Quote");
        MarkdownTextFragment listItem = fragmentContaining(parse("§c- Item", true), "Item");

        assertNotNull(quote.quoteContext);
        assertEquals(1, listItem.bulletListLevel);
        assertTrue(listItem.bulletListItemStart);
    }

    @Test
    void interactionColorRemainsAuthoritativeOverVanillaColor() {
        MarkdownRenderer renderer = new MarkdownRenderer(TEST_FONT);
        MarkdownTextFragment hyperlink = fragmentContaining(MarkdownParser.parse(renderer, "§c[Link](https://example.invalid)", true), "Link");

        assertEquals(renderer.getHyperlinkColor().getColorInt() & 0xFFFFFF, styleOf(hyperlink).getColor().getValue());
    }

    @Test
    void vanillaFormattingAppliesInsideCodeAndPlainTextWithoutRestoringMarkdownStyle() {
        List<MarkdownTextFragment> fragments = parse("`§ncode §rplain` ;;§otext §rplain;;", true);
        MarkdownTextFragment code = fragmentContaining(fragments, "code");
        MarkdownTextFragment codeAfterReset = fragmentContaining(fragments, "plain");
        MarkdownTextFragment plain = plainFragmentContaining(fragments, "text");
        MarkdownTextFragment plainAfterReset = lastPlainFragmentContaining(fragments, "plain");

        assertNotNull(code.codeBlockContext);
        assertTrue(styleOf(code).isUnderlined());
        assertFalse(styleOf(codeAfterReset).isUnderlined());
        assertTrue(plain.plainText);
        assertTrue(styleOf(plain).isItalic());
        assertTrue(plainAfterReset.plainText);
        assertFalse(styleOf(plainAfterReset).isItalic());
    }

    @Test
    void customColorPersistsAcrossFragmentsAndCombinesWithVanillaDecoration() {
        List<MarkdownTextFragment> fragments = parse("§q§lOne two", true);

        assertEquals(customColor(), colorOf(fragmentContaining(fragments, "One")));
        assertEquals(customColor(), colorOf(fragmentContaining(fragments, "two")));
        assertTrue(styleOf(fragmentContaining(fragments, "One")).isBold());
        assertTrue(styleOf(fragmentContaining(fragments, "two")).isBold());
        assertFalse(joinText(fragments).contains("§q"));
    }

    @Test
    void vanillaResetClearsDecorationButPreservesCustomColor() {
        List<MarkdownTextFragment> fragments = parse("§q§lBold §rReset", true);
        MarkdownTextFragment reset = fragmentContaining(fragments, "Reset");

        assertEquals(customColor(), colorOf(reset));
        assertFalse(styleOf(reset).isBold());
    }

    @Test
    void customColorOverridesEarlierVanillaColor() {
        List<MarkdownTextFragment> fragments = parse("§cVanilla §qCustom", true);

        assertEquals(TextColor.fromLegacyFormat(ChatFormatting.RED), styleOf(fragmentContaining(fragments, "Vanilla")).getColor());
        assertEquals(customColor(), colorOf(fragmentContaining(fragments, "Custom")));
    }

    @Test
    void resetAfterVanillaColorRevealsEarlierCustomColor() {
        List<MarkdownTextFragment> fragments = parse("§qCustom §cVanilla §rRevealed", true);

        assertEquals(customColor(), colorOf(fragmentContaining(fragments, "Custom")));
        assertEquals(TextColor.fromLegacyFormat(ChatFormatting.RED), styleOf(fragmentContaining(fragments, "Vanilla")).getColor());
        assertEquals(customColor(), colorOf(fragmentContaining(fragments, "Revealed")));
    }

    @Test
    void unregisteredSectionCodesRemainForTheDownstreamFormatter() {
        List<MarkdownTextFragment> fragments = parse("§?Unknown text", true);

        assertTrue(joinText(fragments).contains("§?Unknown"));
    }

    @Test
    void visibleLowercaseAmpersandCodesAreParsedWithoutChangingUppercaseOrCustomForms() {
        String source = "&nOne two &rplain &A &x";
        MarkdownRenderer renderer = new MarkdownRenderer(TEST_FONT);
        renderer.setText(source);
        String renderText = renderer.buildRenderText();
        List<MarkdownTextFragment> fragments = MarkdownParser.parse(renderer, renderText, true);

        assertEquals(source, renderText);
        assertTrue(styleOf(fragmentContaining(fragments, "One")).isUnderlined());
        assertTrue(styleOf(fragmentContaining(fragments, "two")).isUnderlined());
        assertFalse(styleOf(fragmentContaining(fragments, "plain")).isUnderlined());
        assertTrue(joinText(fragments).contains("&A &x"));
        assertFalse(joinText(fragments).contains("&n"));
        assertFalse(joinText(fragments).contains("&r"));
    }

    @Test
    void markdownLinkDestinationPreservesAmpersandQueryParameters() {
        String destination = "https://example.invalid/path?foo=1&lang=en&next=2";
        String source = "[Link](" + destination + ")";
        MarkdownRenderer renderer = new MarkdownRenderer(TEST_FONT);
        renderer.setText(source);
        String renderText = renderer.buildRenderText();
        MarkdownTextFragment link = fragmentContaining(MarkdownParser.parse(renderer, renderText, true), "Link");

        assertEquals(source, renderText);
        assertNotNull(link.hyperlink);
        assertEquals(destination, link.hyperlink.link);
    }

    @Test
    void preprocessingHookPreservesMarkdownDestinationAndTransformsDynamicText() {
        String placeholder = "{dynamic}";
        String destination = "https://example.invalid/path?foo=1&lang=en";
        String source = "[Link](" + destination + ") " + placeholder;
        MarkdownRenderer renderer = new MarkdownRenderer(TEST_FONT, value -> value.replace(placeholder, "Dynamic"));
        renderer.setText(source);
        String renderText = renderer.buildRenderText();
        MarkdownTextFragment link = fragmentContaining(MarkdownParser.parse(renderer, renderText, true), "Link");

        assertTrue(renderText.endsWith(" Dynamic"));
        assertTrue(renderText.contains("&lang"));
        assertNotNull(link.hyperlink);
        assertEquals(destination, link.hyperlink.link);
    }

    @Test
    void defaultPreprocessorLeavesTextUntouchedApartFromDocumentedNewlineNormalization() {
        MarkdownRenderer renderer = new MarkdownRenderer(TEST_FONT);
        renderer.setText("before {placeholder}%n%after");

        assertEquals("before {placeholder}\nafter", renderer.buildRenderText());
    }

    @Test
    void escapedMarkdownDelimitersRemainLiteralAtDocumentBoundaries() {
        List<MarkdownTextFragment> fragments = parse("\\*\\*not bold\\*\\* and \\# not a heading", true);

        assertEquals("**not bold** and # not a heading", joinText(fragments));
        assertTrue(fragments.stream().noneMatch(fragment -> fragment.bold));
        assertTrue(fragments.stream().allMatch(fragment -> fragment.headlineType == MarkdownTextFragment.HeadlineType.NONE));
    }

    @Test
    void incompleteInlineConstructsRemainTextInsteadOfDroppingTrailingContent() {
        String source = "prefix [unfinished and `code";

        assertEquals(source, joinText(parse(source, true)));
    }

    @Test
    void tableParserKeepsEscapedPipesInsideCells() {
        List<String> cells = MarkdownParser.parseTableRow("| first \\| value | second |");
        MarkdownTextFragment table = tableFragment(parse("| first \\| value | second |\n| --- | --- |\n| data | row |", true));

        assertEquals(List.of("first \\| value", "second"), cells);
        assertEquals("first | value", joinText(table.tableContext.rows.get(0).cells.get(0).fragments));
    }

    @Test
    void leadingFormattingTokensCreateOneFormattedFirstTableCell() {
        assertLeadingFormattedTable("§c");
        assertLeadingFormattedTable("&c");
        assertLeadingFormattedTable("§c ");
        assertLeadingFormattedTable("&c ");
    }

    private static List<MarkdownTextFragment> parse(String text, boolean parseMarkdown) {
        return MarkdownParser.parse(new MarkdownRenderer(TEST_FONT), text, parseMarkdown);
    }

    private static MarkdownTextFragment fragmentContaining(List<MarkdownTextFragment> fragments, String text) {
        for (MarkdownTextFragment fragment : fragments) {
            if (fragment.text.contains(text)) return fragment;
        }
        throw new AssertionError("Missing fragment containing: " + text);
    }

    private static MarkdownTextFragment fragmentWithExactText(List<MarkdownTextFragment> fragments, String text) {
        for (MarkdownTextFragment fragment : fragments) {
            if (fragment.text.equals(text)) return fragment;
        }
        throw new AssertionError("Missing fragment with exact text: " + text);
    }

    private static MarkdownTextFragment plainFragmentContaining(List<MarkdownTextFragment> fragments, String text) {
        for (MarkdownTextFragment fragment : fragments) {
            if (fragment.plainText && fragment.text.contains(text)) return fragment;
        }
        throw new AssertionError("Missing plain-text fragment containing: " + text);
    }

    private static MarkdownTextFragment lastPlainFragmentContaining(List<MarkdownTextFragment> fragments, String text) {
        MarkdownTextFragment found = null;
        for (MarkdownTextFragment fragment : fragments) {
            if (fragment.plainText && fragment.text.contains(text)) found = fragment;
        }
        if (found != null) return found;
        throw new AssertionError("Missing plain-text fragment containing: " + text);
    }

    private static Style styleOf(MarkdownTextFragment fragment) {
        return fragment.buildRenderComponent(false).getStyle();
    }

    private static int colorOf(MarkdownTextFragment fragment) {
        return styleOf(fragment).getColor().getValue();
    }

    private static int customColor() {
        return CUSTOM_FORMATTER.getColor().getColorInt() & 0xFFFFFF;
    }

    private static void assertLeadingFormattedTable(String formatting) {
        List<MarkdownTextFragment> fragments = parse(formatting + "| Header | Other |\n| --- | --- |\n| Value | Two |", true);
        MarkdownTextFragment table = tableFragment(fragments);
        MarkdownTextFragment.TableRow header = table.tableContext.rows.get(0);
        assertEquals(2, header.cells.size());
        MarkdownTextFragment firstCell = fragmentContaining(header.cells.get(0).fragments, "Header");
        MarkdownTextFragment secondCell = fragmentContaining(header.cells.get(1).fragments, "Other");
        assertEquals(TextColor.fromLegacyFormat(ChatFormatting.RED), styleOf(firstCell).getColor());
        assertNull(styleOf(secondCell).getColor());
    }

    private static MarkdownTextFragment tableFragment(List<MarkdownTextFragment> fragments) {
        for (MarkdownTextFragment fragment : fragments) {
            if (fragment.isTable()) return fragment;
        }
        throw new AssertionError("Missing table fragment");
    }

    private static String joinText(List<MarkdownTextFragment> fragments) {
        StringBuilder joined = new StringBuilder();
        for (MarkdownTextFragment fragment : fragments) joined.append(fragment.text);
        return joined.toString();
    }

}
