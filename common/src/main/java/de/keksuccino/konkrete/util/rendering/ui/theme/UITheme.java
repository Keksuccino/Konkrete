package de.keksuccino.konkrete.util.rendering.ui.theme;

import de.keksuccino.konkrete.util.rendering.DrawableColor;
import de.keksuccino.konkrete.util.rendering.ui.UIBase;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import java.awt.*;

/** Defines the complete color and shape palette consumed by reusable UI components. */
public class UITheme {

    /** Stable identifier used by configuration and registry lookup. */
    protected String identifier;
    /** Translation key or literal label displayed to theme pickers. */
    protected String display_name;

    //----------------------------

    /** Whether blurred surfaces are allowed. */
    public boolean allow_blur = false;
    /** Whether UI animations are allowed. */
    public boolean allow_animations = true;
    /** Interface corner rounding radius in GUI pixels. */
    public float interface_corner_rounding_radius = 6.0f;
    /** Widget corner rounding radius in GUI pixels. */
    public float widget_corner_rounding_radius = 6.0f; // old was 4.0f

    /** Icon-button hover color on blurred surfaces. */
    public DrawableColor ui_blur_icon_button_hover_color = DrawableColor.of(new Color(255, 255, 255, 13));
    /** Icon tint on blurred surfaces. */
    public DrawableColor ui_blur_icon_texture_color = DrawableColor.of(new Color(255, 255, 255));
    /** Overlay background tint on blurred surfaces. */
    public DrawableColor ui_blur_overlay_background_tint = DrawableColor.of(new Color(38, 38, 38, 174));
    /** Overlay border color on blurred surfaces. */
    public DrawableColor ui_blur_overlay_border_color = DrawableColor.of(new Color(93, 97, 100, 100));
    /** Interface background tint on blurred surfaces. */
    public DrawableColor ui_blur_interface_background_tint = DrawableColor.of(new Color(38, 38, 38, 216));
    /** Interface border color on blurred surfaces. */
    public DrawableColor ui_blur_interface_border_color = DrawableColor.of(new Color(93, 97, 100, 100));
    /** Title-bar tint on blurred surfaces. */
    public DrawableColor ui_blur_interface_title_bar_tint = DrawableColor.of(new Color(79, 79, 79, 174));
    /** Primary area background on blurred surfaces. */
    public DrawableColor ui_blur_interface_area_background_color_type_1 = DrawableColor.of(new Color(43, 43, 43, 100));
    /** Secondary area background on blurred surfaces. */
    public DrawableColor ui_blur_interface_area_background_color_type_2 = DrawableColor.of(new Color(5, 5, 5, 73));
    /** Area border color on blurred surfaces. */
    public DrawableColor ui_blur_interface_area_border_color = DrawableColor.of(new Color(93, 97, 100, 100));
    /** Selected-entry background on blurred surfaces. */
    public DrawableColor ui_blur_interface_area_entry_selected_color = DrawableColor.of(new Color(125, 125, 131, 53));
    /** Primary idle widget background on blurred surfaces. */
    public DrawableColor ui_blur_interface_widget_background_color_normal_type_1 = DrawableColor.of(new Color(71, 71, 71, 102));
    /** Secondary idle widget background on blurred surfaces. */
    public DrawableColor ui_blur_interface_widget_background_color_normal_type_2 = DrawableColor.of(new Color(126, 126, 126, 102));
    /** Primary hovered widget background on blurred surfaces. */
    public DrawableColor ui_blur_interface_widget_background_color_hover_type_1 = DrawableColor.of(new Color(83, 156, 212, 77));
    /** Secondary hovered widget background on blurred surfaces. */
    public DrawableColor ui_blur_interface_widget_background_color_hover_type_2 = DrawableColor.of(new Color(134, 198, 248, 77));
    /** Widget border color on blurred surfaces. */
    public DrawableColor ui_blur_interface_widget_border_color = DrawableColor.of(new Color(93, 97, 100, 100));
    /** Active widget-label color on blurred surfaces. */
    public DrawableColor ui_blur_interface_widget_label_color_normal = DrawableColor.of(new Color(206, 221, 237));
    /** Inactive widget-label color on blurred surfaces. */
    public DrawableColor ui_blur_interface_widget_label_color_inactive = DrawableColor.of(new Color(113, 117, 119));
    /** Input-field background on blurred surfaces. */
    public DrawableColor ui_blur_interface_input_field_background_color = DrawableColor.of(new Color(43, 43, 43, 102));
    /** Unfocused input-field border on blurred surfaces. */
    public DrawableColor ui_blur_interface_input_field_border_color_normal = DrawableColor.of(new Color(93, 97, 100, 102));
    /** Focused input-field border on blurred surfaces. */
    public DrawableColor ui_blur_interface_input_field_border_color_focused = DrawableColor.of(new Color(93, 97, 100, 102));
    /** Editable input-field text color on blurred surfaces. */
    public DrawableColor ui_blur_interface_input_field_text_color_normal = DrawableColor.of(new Color(206, 221, 237));
    /** Read-only input-field text color on blurred surfaces. */
    public DrawableColor ui_blur_interface_input_field_text_color_uneditable = DrawableColor.of(new Color(113, 117, 119));
    /** Input-field suggestion color on blurred surfaces. */
    public DrawableColor ui_blur_interface_input_field_suggestion_text_color = DrawableColor.of(new Color(128, 128, 128));
    /** General text color on blurred surfaces. */
    public DrawableColor ui_blur_interface_generic_text_color = DrawableColor.of(new Color(255, 255, 255));
    /** Tooltip background tint on blurred surfaces. */
    public DrawableColor ui_blur_tooltip_background_tint = DrawableColor.of(new Color(19, 19, 19, 169));

    /** Icon-button hover color on opaque surfaces. */
    public DrawableColor ui_icon_button_hover_color = DrawableColor.of(new Color(255, 255, 255, 13));
    /** Icon tint on opaque surfaces. */
    public DrawableColor ui_icon_texture_color = DrawableColor.of(new Color(255, 255, 255));
    /** Overlay background on opaque surfaces. */
    public DrawableColor ui_overlay_background_color = DrawableColor.of(new Color(40, 40, 40));
    /** Overlay border color on opaque surfaces. */
    public DrawableColor ui_overlay_border_color = DrawableColor.of(new Color(62, 64, 66));
    /** Interface background on opaque surfaces. */
    public DrawableColor ui_interface_background_color = DrawableColor.of(new Color(38, 38, 38));
    /** Interface border color on opaque surfaces. */
    public DrawableColor ui_interface_border_color = DrawableColor.of(new Color(62, 64, 66));
    /** Title-bar color on opaque surfaces. */
    public DrawableColor ui_interface_title_bar_color = DrawableColor.of(new Color(87, 87, 87));
    /** Primary area background on opaque surfaces. */
    public DrawableColor ui_interface_area_background_color_type_1 = DrawableColor.of(new Color(43, 43, 43));
    /** Secondary area background on opaque surfaces. */
    public DrawableColor ui_interface_area_background_color_type_2 = DrawableColor.of(new Color(38, 38, 38));
    /** Area border color on opaque surfaces. */
    public DrawableColor ui_interface_area_border_color = DrawableColor.of(new Color(59, 59, 59));
    /** Selected-entry background on opaque surfaces. */
    public DrawableColor ui_interface_area_entry_selected_color = DrawableColor.of(new Color(50, 50, 50));
    /** Primary idle widget background on opaque surfaces. */
    public DrawableColor ui_interface_widget_background_color_normal_type_1 = DrawableColor.of(new Color(40, 40, 40));
    /** Secondary idle widget background on opaque surfaces. */
    public DrawableColor ui_interface_widget_background_color_normal_type_2 = DrawableColor.of(new Color(52, 162, 245));
    /** Hovered widget background on opaque surfaces. */
    public DrawableColor ui_interface_widget_background_color_hover_type_1 = DrawableColor.of(new Color(20, 127, 208));
    /** Widget border color on opaque surfaces. */
    public DrawableColor ui_interface_widget_border_color = DrawableColor.of(new Color(59, 59, 59));
    /** Active widget-label color on opaque surfaces. */
    public DrawableColor ui_interface_widget_label_color_normal = DrawableColor.of(new Color(206, 221, 237));
    /** Inactive widget-label color on opaque surfaces. */
    public DrawableColor ui_interface_widget_label_color_inactive = DrawableColor.of(new Color(113, 117, 119));
    /** Input-field background on opaque surfaces. */
    public DrawableColor ui_interface_input_field_background_color = DrawableColor.of(new Color(43, 43, 43));
    /** Unfocused input-field border on opaque surfaces. */
    public DrawableColor ui_interface_input_field_border_color_normal = DrawableColor.of(new Color(59, 59, 59));
    /** Focused input-field border on opaque surfaces. */
    public DrawableColor ui_interface_input_field_border_color_focused = DrawableColor.of(new Color(59, 59, 59));
    /** Editable input-field text color on opaque surfaces. */
    public DrawableColor ui_interface_input_field_text_color_normal = DrawableColor.of(new Color(206, 221, 237));
    /** Read-only input-field text color on opaque surfaces. */
    public DrawableColor ui_interface_input_field_text_color_uneditable = DrawableColor.of(new Color(113, 117, 119));
    /** Input-field suggestion color on opaque surfaces. */
    public DrawableColor ui_interface_input_field_suggestion_text_color = DrawableColor.of(new Color(-8355712));
    /** General text color on opaque surfaces. */
    public DrawableColor ui_interface_generic_text_color = DrawableColor.of(new Color(255, 255, 255));
    /** Tooltip background on opaque surfaces. */
    public DrawableColor ui_tooltip_background_color = DrawableColor.of(new Color(43, 43, 43));

    /** Semantic color for informational status. */
    public DrawableColor info_color = DrawableColor.of(new Color(3, 129, 255));
    /** Semantic color for successful status. */
    public DrawableColor success_color = DrawableColor.of(new Color(49, 206, 5));
    /** Semantic color for error status. */
    public DrawableColor error_color = DrawableColor.of(new Color(237, 69, 69));
    /** Semantic color for warning status. */
    public DrawableColor warning_color = DrawableColor.of(new Color(229, 155, 18));

    /** Menu-bar close icon tint. */
    public DrawableColor menu_bar_close_icon_color = DrawableColor.of(new Color(218, 60, 30));

    /** PiP docking-target fill color. */
    public DrawableColor pip_docking_overlay_color = DrawableColor.of(new Color(64, 150, 255, 80));
    /** PiP docking-target border color. */
    public DrawableColor pip_docking_overlay_border_color = DrawableColor.of(new Color(64, 150, 255, 200));
    /** Overlay color indicating blocked PiP input. */
    public DrawableColor pip_input_blocked_overlay_color = DrawableColor.of(new Color(255, 255, 255, 60));

    /** Idle scroll-grabber color. */
    public DrawableColor scroll_grabber_color_normal = DrawableColor.of(new Color(89, 91, 93, 100));
    /** Hovered scroll-grabber color. */
    public DrawableColor scroll_grabber_color_hover = DrawableColor.of(new Color(102, 104, 104, 100));


    /** First rotating bullet-marker color. */
    public DrawableColor bullet_list_dot_color_1 = DrawableColor.of(new Color(62, 134, 160));
    /** Second rotating bullet-marker color. */
    public DrawableColor bullet_list_dot_color_2 = DrawableColor.of(new Color(173, 108, 121));
    /** Third rotating bullet-marker color. */
    public DrawableColor bullet_list_dot_color_3 = DrawableColor.of(new Color(170, 130, 63));

    /** Suggestion-popup background color. */
    public DrawableColor input_field_suggestions_background_color = DrawableColor.of(new Color(71, 71, 71));
    /** Unselected suggestion text color. */
    public DrawableColor input_field_suggestions_text_color_normal = DrawableColor.of(new Color(206, 221, 237));
    /** Selected suggestion text color. */
    public DrawableColor input_field_suggestions_text_color_selected = DrawableColor.of(new Color(100, 165, 236));

    /** Unfocused line-number text color. */
    public DrawableColor text_editor_line_number_text_color_normal = DrawableColor.of(new Color(91, 92, 94));
    /** Focused line-number text color. */
    public DrawableColor text_editor_line_number_text_color_selected = DrawableColor.of(new Color(137, 147, 150));
    /** Base text-editor text color. */
    public DrawableColor text_editor_text_color = DrawableColor.of(new Color(158, 170, 184));
    /** Nested-text formatting color at depth 1. */
    public DrawableColor text_editor_text_formatting_nested_text_color_1 = DrawableColor.of(new Color(235, 127, 127));
    /** Nested-text formatting color at depth 2. */
    public DrawableColor text_editor_text_formatting_nested_text_color_2 = DrawableColor.of(new Color(235, 201, 127));
    /** Nested-text formatting color at depth 3. */
    public DrawableColor text_editor_text_formatting_nested_text_color_3 = DrawableColor.of(new Color(190, 235, 127));
    /** Nested-text formatting color at depth 4. */
    public DrawableColor text_editor_text_formatting_nested_text_color_4 = DrawableColor.of(new Color(127, 235, 230));
    /** Nested-text formatting color at depth 5. */
    public DrawableColor text_editor_text_formatting_nested_text_color_5 = DrawableColor.of(new Color(127, 158, 235));
    /** Nested-text formatting color at depth 6. */
    public DrawableColor text_editor_text_formatting_nested_text_color_6 = DrawableColor.of(new Color(150, 127, 235));
    /** Nested-text formatting color at depth 7. */
    public DrawableColor text_editor_text_formatting_nested_text_color_7 = DrawableColor.of(new Color(212, 127, 235));
    /** Nested-text formatting color at depth 8. */
    public DrawableColor text_editor_text_formatting_nested_text_color_8 = DrawableColor.of(new Color(245, 54, 54));
    /** Nested-text formatting color at depth 9. */
    public DrawableColor text_editor_text_formatting_nested_text_color_9 = DrawableColor.of(new Color(245, 146, 54));
    /** Nested-text formatting color at depth 10. */
    public DrawableColor text_editor_text_formatting_nested_text_color_10 = DrawableColor.of(new Color(245, 229, 54));
    /** Nested-text formatting color at depth 11. */
    public DrawableColor text_editor_text_formatting_nested_text_color_11 = DrawableColor.of(new Color(105, 245, 54));
    /** Nested-text formatting color at depth 12. */
    public DrawableColor text_editor_text_formatting_nested_text_color_12 = DrawableColor.of(new Color(54, 137, 245));
    /** Bracket-highlight color in the text editor. */
    public DrawableColor text_editor_text_formatting_brackets_color = DrawableColor.of(new Color(252, 223, 3));

    /** Creates an empty UI theme with default state. */
    protected UITheme() {
    }

    /** Defines a theme with a stable identifier and localization key. */
    public UITheme(@NotNull String identifier, @NotNull String display_name) {
        this.identifier = identifier;
        this.display_name = display_name;
    }

    /** Sets UI texture shader color for this UI theme. */
    public void setUITextureShaderColor(GuiGraphicsExtractor graphics, float alpha) {
        boolean blur = UIBase.shouldBlur();
        UIBase.setShaderColor(graphics, blur ? ui_blur_icon_texture_color : ui_icon_texture_color, alpha);
    }

    /** Returns the stable identifier used for registry or entry lookup. */
    @NotNull
    public String getIdentifier() {
        return this.identifier;
    }

    /** Returns display name. */
    @NotNull
    public Component getDisplayName() {
        if (this.display_name.startsWith("konkrete.ui.themes.")) return Component.translatable(this.display_name);
        return Component.literal(this.display_name);
    }

}
