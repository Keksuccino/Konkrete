package de.keksuccino.konkrete.util.rendering.text.markdown;

import net.minecraft.client.resources.language.I18n;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/** Builds Markdown source programmatically. */
public class MarkdownTextBuilder {

    /** Mutable buffer containing the generated Markdown source. */
    protected StringBuilder builder = new StringBuilder();

    /** Creates an empty Markdown text builder. */
    public MarkdownTextBuilder() {
    }

    /** Returns a new empty Markdown source builder. */
    @NotNull
    public static MarkdownTextBuilder create() {
        return new MarkdownTextBuilder();
    }

    /** Appends a line followed by a newline. */
    public MarkdownTextBuilder addLine(@NotNull String line) {
        this.builder.append(Objects.requireNonNull(line)).append("\n");
        return this;
    }

    /** Resolves a translation key with arguments and appends it as a line. */
    public MarkdownTextBuilder addLocalizedLine(@NotNull String key, @Nullable Object... placeholders) {
        return this.addLine(I18n.get(key, placeholders));
    }

    /** Appends a headline using the Markdown prefix for the requested visual level. */
    public MarkdownTextBuilder addHeadline(@NotNull MarkdownTextFragment.HeadlineType headlineType, @NotNull String headline) {
        Objects.requireNonNull(headlineType);
        if (headlineType == MarkdownTextFragment.HeadlineType.BIG) {
            headline = "### " + headline;
        } else if (headlineType == MarkdownTextFragment.HeadlineType.BIGGER) {
            headline = "## " + headline;
        } else if (headlineType == MarkdownTextFragment.HeadlineType.BIGGEST) {
            headline = "# " + headline;
        }
        return this.addLine(headline);
    }

    /** Resolves a translation key and appends it as the requested headline level. */
    public MarkdownTextBuilder addLocalizedHeadline(@NotNull MarkdownTextFragment.HeadlineType headlineType, @NotNull String key, @Nullable Object... placeholders) {
        return this.addHeadline(headlineType, I18n.get(key, placeholders));
    }

    /** Appends one empty line. */
    public MarkdownTextBuilder addEmptyLine() {
        return this.addLine("");
    }

    /** Appends a pipe table whose first row is the header and whose missing cells are empty. */
    public MarkdownTextBuilder addTable(@NotNull List<List<String>> rows, @Nullable List<TableCellAlignment> alignments) {
        Objects.requireNonNull(rows);
        if (rows.isEmpty()) return this;

        int columnCount = rows.get(0).size();

        // Add header row
        List<String> headerRow = rows.get(0);
        this.builder.append("|");
        for (String cell : headerRow) {
            this.builder.append(" ").append(cell).append(" |");
        }
        this.builder.append("\n");

        // Add separator row with alignments
        this.builder.append("|");
        for (int i = 0; i < columnCount; i++) {
            TableCellAlignment align = (alignments != null && i < alignments.size()) ? alignments.get(i) : TableCellAlignment.LEFT;
            switch (align) {
                case LEFT:
                    this.builder.append(":---------|");
                    break;
                case CENTER:
                    this.builder.append(":---------:|");
                    break;
                case RIGHT:
                    this.builder.append("---------:|");
                    break;
            }
        }
        this.builder.append("\n");

        // Add data rows
        for (int i = 1; i < rows.size(); i++) {
            List<String> row = rows.get(i);
            this.builder.append("|");
            for (int j = 0; j < columnCount; j++) {
                String cell = (j < row.size()) ? row.get(j) : "";
                this.builder.append(" ").append(cell).append(" |");
            }
            this.builder.append("\n");
        }

        return this;
    }

    /** Appends an array-backed pipe table using left alignment for every column. */
    public MarkdownTextBuilder addSimpleTable(@NotNull String[][] data) {
        List<List<String>> rows = new ArrayList<>();
        for (String[] row : data) {
            rows.add(Arrays.asList(row));
        }
        return addTable(rows, null);
    }

    /** Returns all generated Markdown source without clearing the builder. */
    @NotNull
    public String build() {
        return this.builder.toString();
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return this.build();
    }

    /** Controls colon placement in generated table separator cells. */
    public enum TableCellAlignment {

        /** Aligns cell content to the left. */
        LEFT,
        /** Centers cell content. */
        CENTER,
        /** Aligns cell content to the right. */
        RIGHT

    }

}
