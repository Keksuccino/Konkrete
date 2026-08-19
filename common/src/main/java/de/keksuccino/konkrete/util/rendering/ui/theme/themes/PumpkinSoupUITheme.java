package de.keksuccino.konkrete.util.rendering.ui.theme.themes;

import de.keksuccino.konkrete.util.rendering.DrawableColor;
import de.keksuccino.konkrete.util.rendering.ui.theme.UITheme;

import java.awt.Color;

/** Defines the built-in pumpkin soup UI color palette. */
public class PumpkinSoupUITheme extends UITheme {

    /** Creates an empty pumpkin soup UI theme with default state. */
    public PumpkinSoupUITheme() {

        super("pumpkin_soup", "konkrete.ui.themes.pumpkin_soup");

        allow_blur = true;

        ui_blur_icon_button_hover_color = DrawableColor.of(new Color(60, 45, 36, 13));
        ui_blur_icon_texture_color = DrawableColor.of(new Color(60, 45, 36));
        ui_blur_overlay_background_tint = DrawableColor.of(new Color(250, 244, 236, 185));
        ui_blur_overlay_border_color = DrawableColor.of(new Color(210, 192, 175, 160));
        ui_blur_interface_background_tint = DrawableColor.of(new Color(249, 242, 234, 215));
        ui_blur_interface_border_color = DrawableColor.of(new Color(210, 192, 175, 160));
        ui_blur_interface_title_bar_tint = DrawableColor.of(new Color(238, 227, 214, 215));
        ui_blur_interface_area_background_color_type_1 = DrawableColor.of(new Color(255, 250, 245, 155));
        ui_blur_interface_area_background_color_type_2 = DrawableColor.of(new Color(244, 232, 220, 150));
        ui_blur_interface_area_border_color = DrawableColor.of(new Color(204, 186, 170, 150));
        ui_blur_interface_area_entry_selected_color = DrawableColor.of(new Color(255, 213, 176, 150));
        ui_blur_interface_widget_background_color_normal_type_1 = DrawableColor.of(new Color(255, 250, 245, 155));
        ui_blur_interface_widget_background_color_normal_type_2 = DrawableColor.of(new Color(255, 204, 158, 155));
        ui_blur_interface_widget_background_color_hover_type_1 = DrawableColor.of(new Color(255, 226, 198, 175));
        ui_blur_interface_widget_background_color_hover_type_2 = DrawableColor.of(new Color(255, 190, 128, 175));
        ui_blur_interface_widget_border_color = DrawableColor.of(new Color(200, 184, 170, 150));
        ui_blur_interface_widget_label_color_normal = DrawableColor.of(new Color(60, 45, 36));
        ui_blur_interface_widget_label_color_inactive = DrawableColor.of(new Color(132, 120, 110));
        ui_blur_interface_input_field_background_color = DrawableColor.of(new Color(255, 250, 245, 150));
        ui_blur_interface_input_field_border_color_normal = DrawableColor.of(new Color(200, 184, 170, 150));
        ui_blur_interface_input_field_border_color_focused = DrawableColor.of(new Color(230, 126, 60, 210));
        ui_blur_interface_input_field_text_color_normal = DrawableColor.of(new Color(60, 45, 36));
        ui_blur_interface_input_field_text_color_uneditable = DrawableColor.of(new Color(132, 120, 110));
        ui_blur_interface_input_field_suggestion_text_color = DrawableColor.of(new Color(160, 148, 136));
        ui_blur_interface_generic_text_color = DrawableColor.of(new Color(60, 45, 36));
        ui_blur_tooltip_background_tint = DrawableColor.of(new Color(255, 249, 242, 220));

        ui_icon_button_hover_color = DrawableColor.of(new Color(60, 45, 36, 13));
        ui_icon_texture_color = DrawableColor.of(new Color(60, 45, 36));
        ui_overlay_background_color = DrawableColor.of(new Color(245, 238, 229));
        ui_overlay_border_color = DrawableColor.of(new Color(208, 190, 174));
        ui_interface_background_color = DrawableColor.of(new Color(251, 245, 237));
        ui_interface_border_color = DrawableColor.of(new Color(208, 190, 174));
        ui_interface_title_bar_color = DrawableColor.of(new Color(236, 225, 212));
        ui_interface_area_background_color_type_1 = DrawableColor.of(new Color(255, 251, 246));
        ui_interface_area_background_color_type_2 = DrawableColor.of(new Color(244, 233, 221));
        ui_interface_area_border_color = DrawableColor.of(new Color(208, 190, 174));
        ui_interface_area_entry_selected_color = DrawableColor.of(new Color(255, 213, 176));
        ui_interface_widget_background_color_normal_type_1 = DrawableColor.of(new Color(255, 250, 245));
        ui_interface_widget_background_color_normal_type_2 = DrawableColor.of(new Color(255, 204, 158));
        ui_interface_widget_background_color_hover_type_1 = DrawableColor.of(new Color(255, 226, 198));
        ui_interface_widget_border_color = DrawableColor.of(new Color(198, 182, 168));
        ui_interface_widget_label_color_normal = DrawableColor.of(new Color(56, 42, 34));
        ui_interface_widget_label_color_inactive = DrawableColor.of(new Color(136, 124, 112));
        ui_interface_input_field_background_color = DrawableColor.of(new Color(255, 250, 245));
        ui_interface_input_field_border_color_normal = DrawableColor.of(new Color(200, 184, 170));
        ui_interface_input_field_border_color_focused = DrawableColor.of(new Color(230, 126, 60));
        ui_interface_input_field_text_color_normal = DrawableColor.of(new Color(56, 42, 34));
        ui_interface_input_field_text_color_uneditable = DrawableColor.of(new Color(150, 138, 126));
        ui_interface_input_field_suggestion_text_color = DrawableColor.of(new Color(162, 150, 138));
        ui_interface_generic_text_color = DrawableColor.of(new Color(56, 42, 34));
        ui_tooltip_background_color = DrawableColor.of(new Color(248, 241, 233));

        success_color = DrawableColor.of(new Color(32, 153, 106));
        error_color = DrawableColor.of(new Color(218, 78, 58));
        warning_color = DrawableColor.of(new Color(230, 126, 60));

        menu_bar_close_icon_color = DrawableColor.of(new Color(230, 126, 60));
        pip_docking_overlay_color = DrawableColor.of(new Color(230, 126, 60, 80));
        pip_docking_overlay_border_color = DrawableColor.of(new Color(230, 126, 60, 200));

        scroll_grabber_color_normal = DrawableColor.of(new Color(186, 170, 156, 140));
        scroll_grabber_color_hover = DrawableColor.of(new Color(160, 146, 134, 180));


        bullet_list_dot_color_1 = DrawableColor.of(new Color(230, 126, 60));
        bullet_list_dot_color_2 = DrawableColor.of(new Color(192, 118, 82));
        bullet_list_dot_color_3 = DrawableColor.of(new Color(150, 170, 114));

        input_field_suggestions_background_color = DrawableColor.of(new Color(250, 244, 236));
        input_field_suggestions_text_color_normal = DrawableColor.of(new Color(56, 42, 34));
        input_field_suggestions_text_color_selected = DrawableColor.of(new Color(208, 104, 46));

        text_editor_line_number_text_color_normal = DrawableColor.of(new Color(156, 145, 134));
        text_editor_line_number_text_color_selected = DrawableColor.of(new Color(120, 110, 100));
        text_editor_text_color = DrawableColor.of(new Color(56, 42, 34));
        text_editor_text_formatting_nested_text_color_1 = DrawableColor.of(new Color(218, 78, 58));
        text_editor_text_formatting_nested_text_color_2 = DrawableColor.of(new Color(230, 126, 60));
        text_editor_text_formatting_nested_text_color_3 = DrawableColor.of(new Color(118, 166, 92));
        text_editor_text_formatting_nested_text_color_4 = DrawableColor.of(new Color(54, 156, 134));
        text_editor_text_formatting_nested_text_color_5 = DrawableColor.of(new Color(74, 132, 210));
        text_editor_text_formatting_nested_text_color_6 = DrawableColor.of(new Color(160, 102, 210));
        text_editor_text_formatting_nested_text_color_7 = DrawableColor.of(new Color(202, 92, 150));
        text_editor_text_formatting_nested_text_color_8 = DrawableColor.of(new Color(218, 78, 58));
        text_editor_text_formatting_nested_text_color_9 = DrawableColor.of(new Color(230, 126, 60));
        text_editor_text_formatting_nested_text_color_10 = DrawableColor.of(new Color(214, 172, 86));
        text_editor_text_formatting_nested_text_color_11 = DrawableColor.of(new Color(88, 170, 118));
        text_editor_text_formatting_nested_text_color_12 = DrawableColor.of(new Color(62, 130, 140));
        text_editor_text_formatting_brackets_color = DrawableColor.of(new Color(230, 126, 60));

    }

}
