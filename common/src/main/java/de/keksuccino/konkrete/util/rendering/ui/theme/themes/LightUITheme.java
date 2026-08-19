package de.keksuccino.konkrete.util.rendering.ui.theme.themes;

import de.keksuccino.konkrete.util.rendering.DrawableColor;
import de.keksuccino.konkrete.util.rendering.ui.theme.UITheme;

import java.awt.Color;

/** Defines the built-in light UI color palette. */
public class LightUITheme extends UITheme {

    /** Creates an empty light UI theme with default state. */
    public LightUITheme() {

        super("light", "konkrete.ui.themes.light");

        allow_blur = true;

        ui_blur_icon_button_hover_color = DrawableColor.of(new Color(44, 49, 58, 13));
        ui_blur_icon_texture_color = DrawableColor.of(new Color(44, 49, 58));
        ui_blur_overlay_background_tint = DrawableColor.of(new Color(248, 250, 253, 180));
        ui_blur_overlay_border_color = DrawableColor.of(new Color(206, 212, 221, 150));
        ui_blur_interface_background_tint = DrawableColor.of(new Color(248, 250, 253, 210));
        ui_blur_interface_border_color = DrawableColor.of(new Color(180, 185, 190, 168));
        ui_blur_interface_title_bar_tint = DrawableColor.of(new Color(235, 239, 245, 210));
        ui_blur_interface_area_background_color_type_1 = DrawableColor.of(new Color(255, 255, 255, 150));
        ui_blur_interface_area_background_color_type_2 = DrawableColor.of(new Color(242, 246, 251, 140));
        ui_blur_interface_area_border_color = DrawableColor.of(new Color(204, 210, 220, 140));
        ui_blur_interface_area_entry_selected_color = DrawableColor.of(new Color(206, 224, 255, 130));
        ui_blur_interface_widget_background_color_normal_type_1 = DrawableColor.of(new Color(255, 255, 255, 150));
        ui_blur_interface_widget_background_color_normal_type_2 = DrawableColor.of(new Color(216, 233, 255, 150));
        ui_blur_interface_widget_background_color_hover_type_1 = DrawableColor.of(new Color(232, 242, 255, 170));
        ui_blur_interface_widget_background_color_hover_type_2 = DrawableColor.of(new Color(210, 228, 255, 170));
        ui_blur_interface_widget_border_color = DrawableColor.of(new Color(198, 206, 217, 150));
        ui_blur_interface_widget_label_color_normal = DrawableColor.of(new Color(44, 49, 58));
        ui_blur_interface_widget_label_color_inactive = DrawableColor.of(new Color(120, 126, 136));
        ui_blur_interface_input_field_background_color = DrawableColor.of(new Color(255, 255, 255, 150));
        ui_blur_interface_input_field_border_color_normal = DrawableColor.of(new Color(198, 206, 217, 150));
        ui_blur_interface_input_field_border_color_focused = DrawableColor.of(new Color(111, 169, 255, 200));
        ui_blur_interface_input_field_text_color_normal = DrawableColor.of(new Color(44, 49, 58));
        ui_blur_interface_input_field_text_color_uneditable = DrawableColor.of(new Color(120, 126, 136));
        ui_blur_interface_input_field_suggestion_text_color = DrawableColor.of(new Color(145, 151, 161));
        ui_blur_interface_generic_text_color = DrawableColor.of(new Color(44, 49, 58));
        ui_blur_tooltip_background_tint = DrawableColor.of(new Color(255, 255, 255, 220));

        ui_icon_button_hover_color = DrawableColor.of(new Color(54, 60, 69, 13));
        ui_icon_texture_color = DrawableColor.of(new Color(54, 60, 69));
        ui_overlay_background_color = DrawableColor.of(new Color(242, 244, 247));
        ui_overlay_border_color = DrawableColor.of(new Color(210, 216, 224));
        ui_interface_background_color = DrawableColor.of(new Color(247, 248, 250));
        ui_interface_border_color = DrawableColor.of(new Color(210, 216, 224));
        ui_interface_title_bar_color = DrawableColor.of(new Color(235, 238, 242));
        ui_interface_area_background_color_type_1 = DrawableColor.of(new Color(255, 255, 255));
        ui_interface_area_background_color_type_2 = DrawableColor.of(new Color(241, 244, 248));
        ui_interface_area_border_color = DrawableColor.of(new Color(214, 219, 227));
        ui_interface_area_entry_selected_color = DrawableColor.of(new Color(220, 233, 255));
        ui_interface_widget_background_color_normal_type_1 = DrawableColor.of(new Color(255, 255, 255));
        ui_interface_widget_background_color_normal_type_2 = DrawableColor.of(new Color(216, 233, 255));
        ui_interface_widget_background_color_hover_type_1 = DrawableColor.of(new Color(230, 240, 255));
        ui_interface_widget_border_color = DrawableColor.of(new Color(200, 206, 215));
        ui_interface_widget_label_color_normal = DrawableColor.of(new Color(42, 46, 54));
        ui_interface_widget_label_color_inactive = DrawableColor.of(new Color(122, 129, 138));
        ui_interface_input_field_background_color = DrawableColor.of(new Color(255, 255, 255));
        ui_interface_input_field_border_color_normal = DrawableColor.of(new Color(199, 206, 217));
        ui_interface_input_field_border_color_focused = DrawableColor.of(new Color(111, 169, 255));
        ui_interface_input_field_text_color_normal = DrawableColor.of(new Color(42, 46, 54));
        ui_interface_input_field_text_color_uneditable = DrawableColor.of(new Color(140, 146, 156));
        ui_interface_input_field_suggestion_text_color = DrawableColor.of(new Color(150, 157, 168));
        ui_interface_generic_text_color = DrawableColor.of(new Color(42, 46, 54));
        ui_tooltip_background_color = DrawableColor.of(new Color(245, 247, 251));

        success_color = DrawableColor.of(new Color(22, 163, 74));
        error_color = DrawableColor.of(new Color(220, 38, 38));
        warning_color = DrawableColor.of(new Color(217, 119, 6));

        menu_bar_close_icon_color = DrawableColor.of(new Color(239, 68, 68));
        pip_docking_overlay_color = DrawableColor.of(new Color(59, 130, 246, 80));
        pip_docking_overlay_border_color = DrawableColor.of(new Color(59, 130, 246, 200));

        scroll_grabber_color_normal = DrawableColor.of(new Color(176, 182, 190, 130));
        scroll_grabber_color_hover = DrawableColor.of(new Color(150, 156, 165, 170));


        bullet_list_dot_color_1 = DrawableColor.of(new Color(59, 130, 246));
        bullet_list_dot_color_2 = DrawableColor.of(new Color(236, 72, 153));
        bullet_list_dot_color_3 = DrawableColor.of(new Color(245, 158, 11));

        input_field_suggestions_background_color = DrawableColor.of(new Color(245, 247, 251));
        input_field_suggestions_text_color_normal = DrawableColor.of(new Color(42, 46, 54));
        input_field_suggestions_text_color_selected = DrawableColor.of(new Color(37, 99, 235));

        text_editor_line_number_text_color_normal = DrawableColor.of(new Color(145, 151, 160));
        text_editor_line_number_text_color_selected = DrawableColor.of(new Color(105, 113, 125));
        text_editor_text_color = DrawableColor.of(new Color(42, 46, 54));
        text_editor_text_formatting_nested_text_color_1 = DrawableColor.of(new Color(225, 29, 72));
        text_editor_text_formatting_nested_text_color_2 = DrawableColor.of(new Color(217, 119, 6));
        text_editor_text_formatting_nested_text_color_3 = DrawableColor.of(new Color(101, 163, 13));
        text_editor_text_formatting_nested_text_color_4 = DrawableColor.of(new Color(13, 148, 136));
        text_editor_text_formatting_nested_text_color_5 = DrawableColor.of(new Color(37, 99, 235));
        text_editor_text_formatting_nested_text_color_6 = DrawableColor.of(new Color(124, 58, 237));
        text_editor_text_formatting_nested_text_color_7 = DrawableColor.of(new Color(219, 39, 119));
        text_editor_text_formatting_nested_text_color_8 = DrawableColor.of(new Color(239, 68, 68));
        text_editor_text_formatting_nested_text_color_9 = DrawableColor.of(new Color(249, 115, 22));
        text_editor_text_formatting_nested_text_color_10 = DrawableColor.of(new Color(234, 179, 8));
        text_editor_text_formatting_nested_text_color_11 = DrawableColor.of(new Color(34, 197, 94));
        text_editor_text_formatting_nested_text_color_12 = DrawableColor.of(new Color(14, 116, 144));
        text_editor_text_formatting_brackets_color = DrawableColor.of(new Color(245, 158, 11));

    }

}
