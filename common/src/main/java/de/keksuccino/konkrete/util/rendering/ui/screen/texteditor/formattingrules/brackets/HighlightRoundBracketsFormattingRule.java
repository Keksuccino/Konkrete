package de.keksuccino.konkrete.util.rendering.ui.screen.texteditor.formattingrules.brackets;

import de.keksuccino.konkrete.util.rendering.ui.UIBase;
import net.minecraft.network.chat.Style;

/** Applies highlight round brackets syntax highlighting in the text editor. */
public class HighlightRoundBracketsFormattingRule extends HighlightBracketsFormattingRuleBase {

    /** Returns the opening delimiter recognized by this rule. */
    @Override
    protected String getOpenBracketChar() {
        return "(";
    }

    /** Returns the closing delimiter recognized by this rule. */
    @Override
    protected String getCloseBracketChar() {
        return ")";
    }

    /** Returns the style applied to a matched bracket pair. */
    @Override
    protected Style getHighlightStyle() {
        return Style.EMPTY.withColor(UIBase.getUITheme().text_editor_text_formatting_brackets_color.getColorInt());
    }

}
