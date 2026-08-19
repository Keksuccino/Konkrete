package de.keksuccino.konkrete.util.rendering.ui.theme.themes;

import de.keksuccino.konkrete.util.rendering.DrawableColor;
import de.keksuccino.konkrete.util.rendering.ui.theme.UITheme;

import java.awt.Color;

/** Defines the built-in spooky season UI color palette. */
public class SpookySeasonUITheme extends UITheme {

    /** Creates an empty spooky season UI theme with default state. */
    public SpookySeasonUITheme() {

        super("spooky_season", "konkrete.ui.themes.spooky_season");

        allow_blur = true;

        ui_blur_icon_button_hover_color = DrawableColor.of(new Color(242, 231, 217, 13));
        ui_blur_icon_texture_color = DrawableColor.of(new Color(242, 231, 217));
        ui_blur_overlay_background_tint = DrawableColor.of(new Color(16, 13, 20, 185));
        ui_blur_overlay_border_color = DrawableColor.of(new Color(78, 70, 86, 160));
        ui_blur_interface_background_tint = DrawableColor.of(new Color(18, 15, 23, 215));
        ui_blur_interface_border_color = DrawableColor.of(new Color(78, 70, 86, 160));
        ui_blur_interface_title_bar_tint = DrawableColor.of(new Color(30, 24, 34, 215));
        ui_blur_interface_area_background_color_type_1 = DrawableColor.of(new Color(27, 22, 31, 155));
        ui_blur_interface_area_background_color_type_2 = DrawableColor.of(new Color(19, 15, 23, 150));
        ui_blur_interface_area_border_color = DrawableColor.of(new Color(86, 77, 98, 150));
        ui_blur_interface_area_entry_selected_color = DrawableColor.of(new Color(63, 45, 80, 150));
        ui_blur_interface_widget_background_color_normal_type_1 = DrawableColor.of(new Color(31, 25, 36, 150));
        ui_blur_interface_widget_background_color_normal_type_2 = DrawableColor.of(new Color(117, 63, 25, 150));
        ui_blur_interface_widget_background_color_hover_type_1 = DrawableColor.of(new Color(50, 35, 55, 170));
        ui_blur_interface_widget_background_color_hover_type_2 = DrawableColor.of(new Color(145, 82, 34, 170));
        ui_blur_interface_widget_border_color = DrawableColor.of(new Color(86, 77, 98, 150));
        ui_blur_interface_widget_label_color_normal = DrawableColor.of(new Color(242, 231, 217));
        ui_blur_interface_widget_label_color_inactive = DrawableColor.of(new Color(150, 143, 157));
        ui_blur_interface_input_field_background_color = DrawableColor.of(new Color(27, 22, 31, 150));
        ui_blur_interface_input_field_border_color_normal = DrawableColor.of(new Color(86, 77, 98, 150));
        ui_blur_interface_input_field_border_color_focused = DrawableColor.of(new Color(212, 120, 43, 200));
        ui_blur_interface_input_field_text_color_normal = DrawableColor.of(new Color(242, 231, 217));
        ui_blur_interface_input_field_text_color_uneditable = DrawableColor.of(new Color(150, 143, 157));
        ui_blur_interface_input_field_suggestion_text_color = DrawableColor.of(new Color(133, 124, 141));
        ui_blur_interface_generic_text_color = DrawableColor.of(new Color(242, 231, 217));
        ui_blur_tooltip_background_tint = DrawableColor.of(new Color(20, 16, 25, 220));

        ui_icon_button_hover_color = DrawableColor.of(new Color(242, 231, 217, 13));
        ui_icon_texture_color = DrawableColor.of(new Color(242, 231, 217));
        ui_overlay_background_color = DrawableColor.of(new Color(23, 19, 28));
        ui_overlay_border_color = DrawableColor.of(new Color(72, 65, 82));
        ui_interface_background_color = DrawableColor.of(new Color(21, 17, 26));
        ui_interface_border_color = DrawableColor.of(new Color(72, 65, 82));
        ui_interface_title_bar_color = DrawableColor.of(new Color(34, 27, 38));
        ui_interface_area_background_color_type_1 = DrawableColor.of(new Color(28, 23, 33));
        ui_interface_area_background_color_type_2 = DrawableColor.of(new Color(20, 16, 25));
        ui_interface_area_border_color = DrawableColor.of(new Color(72, 65, 82));
        ui_interface_area_entry_selected_color = DrawableColor.of(new Color(58, 40, 74));
        ui_interface_widget_background_color_normal_type_1 = DrawableColor.of(new Color(30, 24, 35));
        ui_interface_widget_background_color_normal_type_2 = DrawableColor.of(new Color(122, 66, 27));
        ui_interface_widget_background_color_hover_type_1 = DrawableColor.of(new Color(46, 33, 52));
        ui_interface_widget_border_color = DrawableColor.of(new Color(72, 65, 82));
        ui_interface_widget_label_color_normal = DrawableColor.of(new Color(242, 231, 217));
        ui_interface_widget_label_color_inactive = DrawableColor.of(new Color(150, 143, 157));
        ui_interface_input_field_background_color = DrawableColor.of(new Color(28, 23, 33));
        ui_interface_input_field_border_color_normal = DrawableColor.of(new Color(72, 65, 82));
        ui_interface_input_field_border_color_focused = DrawableColor.of(new Color(212, 120, 43));
        ui_interface_input_field_text_color_normal = DrawableColor.of(new Color(242, 231, 217));
        ui_interface_input_field_text_color_uneditable = DrawableColor.of(new Color(150, 143, 157));
        ui_interface_input_field_suggestion_text_color = DrawableColor.of(new Color(133, 124, 141));
        ui_interface_generic_text_color = DrawableColor.of(new Color(242, 231, 217));
        ui_tooltip_background_color = DrawableColor.of(new Color(24, 19, 29));

        success_color = DrawableColor.of(new Color(105, 186, 93));
        error_color = DrawableColor.of(new Color(220, 80, 72));
        warning_color = DrawableColor.of(new Color(232, 142, 56));

        menu_bar_close_icon_color = DrawableColor.of(new Color(232, 118, 74));
        pip_docking_overlay_color = DrawableColor.of(new Color(216, 108, 43, 80));
        pip_docking_overlay_border_color = DrawableColor.of(new Color(216, 108, 43, 200));

        scroll_grabber_color_normal = DrawableColor.of(new Color(90, 83, 98, 140));
        scroll_grabber_color_hover = DrawableColor.of(new Color(120, 111, 129, 190));


        bullet_list_dot_color_1 = DrawableColor.of(new Color(232, 142, 56));
        bullet_list_dot_color_2 = DrawableColor.of(new Color(170, 108, 202));
        bullet_list_dot_color_3 = DrawableColor.of(new Color(140, 186, 120));

        input_field_suggestions_background_color = DrawableColor.of(new Color(30, 25, 36));
        input_field_suggestions_text_color_normal = DrawableColor.of(new Color(242, 231, 217));
        input_field_suggestions_text_color_selected = DrawableColor.of(new Color(232, 142, 56));

        text_editor_line_number_text_color_normal = DrawableColor.of(new Color(122, 114, 131));
        text_editor_line_number_text_color_selected = DrawableColor.of(new Color(156, 146, 165));
        text_editor_text_color = DrawableColor.of(new Color(216, 206, 193));
        text_editor_text_formatting_nested_text_color_1 = DrawableColor.of(new Color(232, 118, 74));
        text_editor_text_formatting_nested_text_color_2 = DrawableColor.of(new Color(236, 160, 88));
        text_editor_text_formatting_nested_text_color_3 = DrawableColor.of(new Color(132, 198, 96));
        text_editor_text_formatting_nested_text_color_4 = DrawableColor.of(new Color(94, 190, 168));
        text_editor_text_formatting_nested_text_color_5 = DrawableColor.of(new Color(110, 156, 236));
        text_editor_text_formatting_nested_text_color_6 = DrawableColor.of(new Color(170, 118, 236));
        text_editor_text_formatting_nested_text_color_7 = DrawableColor.of(new Color(214, 116, 176));
        text_editor_text_formatting_nested_text_color_8 = DrawableColor.of(new Color(220, 80, 72));
        text_editor_text_formatting_nested_text_color_9 = DrawableColor.of(new Color(232, 142, 56));
        text_editor_text_formatting_nested_text_color_10 = DrawableColor.of(new Color(242, 192, 88));
        text_editor_text_formatting_nested_text_color_11 = DrawableColor.of(new Color(106, 202, 132));
        text_editor_text_formatting_nested_text_color_12 = DrawableColor.of(new Color(94, 160, 170));
        text_editor_text_formatting_brackets_color = DrawableColor.of(new Color(232, 142, 56));

    }

}
