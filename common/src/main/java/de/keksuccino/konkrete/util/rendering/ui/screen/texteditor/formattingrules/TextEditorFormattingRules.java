package de.keksuccino.konkrete.util.rendering.ui.screen.texteditor.formattingrules;

import de.keksuccino.konkrete.util.rendering.ui.screen.texteditor.TextEditorFormattingRule;
import de.keksuccino.konkrete.util.rendering.ui.screen.texteditor.formattingrules.brackets.HighlightAngleBracketsFormattingRule;
import de.keksuccino.konkrete.util.rendering.ui.screen.texteditor.formattingrules.brackets.HighlightCurlyBracketsFormattingRule;
import de.keksuccino.konkrete.util.rendering.ui.screen.texteditor.formattingrules.brackets.HighlightRoundBracketsFormattingRule;
import de.keksuccino.konkrete.util.rendering.ui.screen.texteditor.formattingrules.brackets.HighlightSquareBracketsFormattingRule;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;

/** Registers and applies the text editor's syntax-highlighting rules. */
public class TextEditorFormattingRules {

    private static final Logger LOGGER = LogManager.getLogger();
    private static final List<Class<? extends TextEditorFormattingRule>> RULE_CLASSES = new ArrayList<>();

    static {

        addRuleAtTop(HighlightPlaceholdersFormattingRule.class);

        addRuleAtBottom(HighlightAngleBracketsFormattingRule.class);
        addRuleAtBottom(HighlightCurlyBracketsFormattingRule.class);
        addRuleAtBottom(HighlightRoundBracketsFormattingRule.class);
        addRuleAtBottom(HighlightSquareBracketsFormattingRule.class);

    }

    /** Gives a rule class highest formatting priority unless it is already registered. */
    public static void addRuleAtTop(Class<? extends TextEditorFormattingRule> rule) {
        if (!RULE_CLASSES.contains(rule)) {
            RULE_CLASSES.add(0, rule);
        }
    }

    /** Gives a rule class lowest formatting priority unless it is already registered. */
    public static void addRuleAtBottom(Class<? extends TextEditorFormattingRule> rule) {
        if (!RULE_CLASSES.contains(rule)) {
            RULE_CLASSES.add(rule);
        }
    }

    /** Instantiates a fresh ordered rule list, skipping classes that cannot be constructed. */
    public static List<TextEditorFormattingRule> getRules() {
        List<TextEditorFormattingRule> r = new ArrayList<>();
        for (Class<? extends TextEditorFormattingRule> rule : RULE_CLASSES) {
            try {
                r.add(rule.getDeclaredConstructor().newInstance());
            } catch (Exception e) {
                LOGGER.error("[KONKRETE] Unable to construct formatting rule '{}'; rules require a public no-argument constructor.", (rule != null) ? rule.getName() : "NULL", e);
            }
        }
        return r;
    }

}
