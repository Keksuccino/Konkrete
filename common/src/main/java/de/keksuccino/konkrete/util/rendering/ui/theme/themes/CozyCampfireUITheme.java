package de.keksuccino.konkrete.util.rendering.ui.theme.themes;

import de.keksuccino.konkrete.util.rendering.DrawableColor;
import de.keksuccino.konkrete.util.rendering.ui.theme.UITheme;

import java.awt.Color;

/** Defines the built-in cozy campfire UI color palette. */
public class CozyCampfireUITheme extends UITheme {

    /** Creates an empty cozy campfire UI theme with default state. */
    public CozyCampfireUITheme() {

        super("cozy_campfire", "konkrete.ui.themes.cozy_campfire");

        allow_blur = true;

        ui_blur_icon_button_hover_color = DrawableColor.of(new Color(242, 229, 208, 13));
        ui_blur_icon_texture_color = DrawableColor.of(new Color(242, 229, 208));
        ui_blur_overlay_background_tint = DrawableColor.of(new Color(24, 19, 16, 185));
        ui_blur_overlay_border_color = DrawableColor.of(new Color(92, 78, 66, 160));
        ui_blur_interface_background_tint = DrawableColor.of(new Color(26, 20, 16, 215));
        ui_blur_interface_border_color = DrawableColor.of(new Color(92, 78, 66, 160));
        ui_blur_interface_title_bar_tint = DrawableColor.of(new Color(38, 30, 22, 215));
        ui_blur_interface_area_background_color_type_1 = DrawableColor.of(new Color(34, 27, 20, 155));
        ui_blur_interface_area_background_color_type_2 = DrawableColor.of(new Color(24, 20, 16, 150));
        ui_blur_interface_area_border_color = DrawableColor.of(new Color(96, 82, 70, 150));
        ui_blur_interface_area_entry_selected_color = DrawableColor.of(new Color(78, 54, 40, 150));
        ui_blur_interface_widget_background_color_normal_type_1 = DrawableColor.of(new Color(36, 28, 22, 150));
        ui_blur_interface_widget_background_color_normal_type_2 = DrawableColor.of(new Color(176, 98, 44, 150));
        ui_blur_interface_widget_background_color_hover_type_1 = DrawableColor.of(new Color(50, 38, 28, 170));
        ui_blur_interface_widget_background_color_hover_type_2 = DrawableColor.of(new Color(206, 120, 52, 170));
        ui_blur_interface_widget_border_color = DrawableColor.of(new Color(96, 82, 70, 150));
        ui_blur_interface_widget_label_color_normal = DrawableColor.of(new Color(242, 229, 208));
        ui_blur_interface_widget_label_color_inactive = DrawableColor.of(new Color(156, 146, 134));
        ui_blur_interface_input_field_background_color = DrawableColor.of(new Color(34, 27, 20, 150));
        ui_blur_interface_input_field_border_color_normal = DrawableColor.of(new Color(96, 82, 70, 150));
        ui_blur_interface_input_field_border_color_focused = DrawableColor.of(new Color(220, 126, 58, 200));
        ui_blur_interface_input_field_text_color_normal = DrawableColor.of(new Color(242, 229, 208));
        ui_blur_interface_input_field_text_color_uneditable = DrawableColor.of(new Color(156, 146, 134));
        ui_blur_interface_input_field_suggestion_text_color = DrawableColor.of(new Color(138, 128, 116));
        ui_blur_interface_generic_text_color = DrawableColor.of(new Color(242, 229, 208));
        ui_blur_tooltip_background_tint = DrawableColor.of(new Color(28, 22, 16, 220));

        ui_icon_button_hover_color = DrawableColor.of(new Color(242, 229, 208, 13));
        ui_icon_texture_color = DrawableColor.of(new Color(242, 229, 208));
        ui_overlay_background_color = DrawableColor.of(new Color(26, 20, 16));
        ui_overlay_border_color = DrawableColor.of(new Color(86, 74, 64));
        ui_interface_background_color = DrawableColor.of(new Color(24, 19, 15));
        ui_interface_border_color = DrawableColor.of(new Color(86, 74, 64));
        ui_interface_title_bar_color = DrawableColor.of(new Color(42, 32, 24));
        ui_interface_area_background_color_type_1 = DrawableColor.of(new Color(34, 27, 20));
        ui_interface_area_background_color_type_2 = DrawableColor.of(new Color(22, 18, 14));
        ui_interface_area_border_color = DrawableColor.of(new Color(86, 74, 64));
        ui_interface_area_entry_selected_color = DrawableColor.of(new Color(70, 48, 36));
        ui_interface_widget_background_color_normal_type_1 = DrawableColor.of(new Color(36, 28, 22));
        ui_interface_widget_background_color_normal_type_2 = DrawableColor.of(new Color(176, 98, 44));
        ui_interface_widget_background_color_hover_type_1 = DrawableColor.of(new Color(52, 40, 30));
        ui_interface_widget_border_color = DrawableColor.of(new Color(86, 74, 64));
        ui_interface_widget_label_color_normal = DrawableColor.of(new Color(242, 229, 208));
        ui_interface_widget_label_color_inactive = DrawableColor.of(new Color(156, 146, 134));
        ui_interface_input_field_background_color = DrawableColor.of(new Color(34, 27, 20));
        ui_interface_input_field_border_color_normal = DrawableColor.of(new Color(86, 74, 64));
        ui_interface_input_field_border_color_focused = DrawableColor.of(new Color(220, 126, 58));
        ui_interface_input_field_text_color_normal = DrawableColor.of(new Color(242, 229, 208));
        ui_interface_input_field_text_color_uneditable = DrawableColor.of(new Color(156, 146, 134));
        ui_interface_input_field_suggestion_text_color = DrawableColor.of(new Color(138, 128, 116));
        ui_interface_generic_text_color = DrawableColor.of(new Color(242, 229, 208));
        ui_tooltip_background_color = DrawableColor.of(new Color(28, 22, 16));

        success_color = DrawableColor.of(new Color(110, 196, 122));
        error_color = DrawableColor.of(new Color(220, 92, 78));
        warning_color = DrawableColor.of(new Color(232, 142, 64));

        menu_bar_close_icon_color = DrawableColor.of(new Color(232, 142, 64));
        pip_docking_overlay_color = DrawableColor.of(new Color(232, 142, 64, 80));
        pip_docking_overlay_border_color = DrawableColor.of(new Color(232, 142, 64, 200));

        scroll_grabber_color_normal = DrawableColor.of(new Color(100, 90, 80, 150));
        scroll_grabber_color_hover = DrawableColor.of(new Color(140, 124, 108, 200));


        bullet_list_dot_color_1 = DrawableColor.of(new Color(232, 142, 64));
        bullet_list_dot_color_2 = DrawableColor.of(new Color(192, 128, 90));
        bullet_list_dot_color_3 = DrawableColor.of(new Color(132, 186, 132));

        input_field_suggestions_background_color = DrawableColor.of(new Color(34, 27, 20));
        input_field_suggestions_text_color_normal = DrawableColor.of(new Color(242, 229, 208));
        input_field_suggestions_text_color_selected = DrawableColor.of(new Color(232, 142, 64));

        text_editor_line_number_text_color_normal = DrawableColor.of(new Color(130, 122, 114));
        text_editor_line_number_text_color_selected = DrawableColor.of(new Color(170, 160, 150));
        text_editor_text_color = DrawableColor.of(new Color(230, 218, 198));
        text_editor_text_formatting_nested_text_color_1 = DrawableColor.of(new Color(232, 112, 84));
        text_editor_text_formatting_nested_text_color_2 = DrawableColor.of(new Color(232, 142, 64));
        text_editor_text_formatting_nested_text_color_3 = DrawableColor.of(new Color(132, 198, 110));
        text_editor_text_formatting_nested_text_color_4 = DrawableColor.of(new Color(96, 190, 170));
        text_editor_text_formatting_nested_text_color_5 = DrawableColor.of(new Color(118, 166, 236));
        text_editor_text_formatting_nested_text_color_6 = DrawableColor.of(new Color(176, 126, 236));
        text_editor_text_formatting_nested_text_color_7 = DrawableColor.of(new Color(214, 126, 182));
        text_editor_text_formatting_nested_text_color_8 = DrawableColor.of(new Color(220, 92, 78));
        text_editor_text_formatting_nested_text_color_9 = DrawableColor.of(new Color(232, 142, 64));
        text_editor_text_formatting_nested_text_color_10 = DrawableColor.of(new Color(242, 188, 98));
        text_editor_text_formatting_nested_text_color_11 = DrawableColor.of(new Color(118, 210, 148));
        text_editor_text_formatting_nested_text_color_12 = DrawableColor.of(new Color(94, 166, 176));
        text_editor_text_formatting_brackets_color = DrawableColor.of(new Color(232, 142, 64));

    }

}
