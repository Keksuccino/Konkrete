package de.keksuccino.konkrete.util.rendering.ui.theme.themes;

import de.keksuccino.konkrete.util.rendering.DrawableColor;
import de.keksuccino.konkrete.util.rendering.ui.theme.UITheme;

import java.awt.Color;

/** Defines the built-in light high contrast UI color palette. */
public class LightHighContrastUITheme extends UITheme {

    /** Creates an empty light high contrast UI theme with default state. */
    public LightHighContrastUITheme() {

        super("light_high_contrast", "konkrete.ui.themes.light_high_contrast");

        allow_blur = false;
        allow_animations = false;
        interface_corner_rounding_radius = 0.0f;
        widget_corner_rounding_radius = 0.0f;

        ui_blur_icon_button_hover_color = DrawableColor.of(new Color(0, 0, 0, 13));
        ui_blur_icon_texture_color = DrawableColor.of(new Color(0, 0, 0));
        ui_blur_overlay_background_tint = DrawableColor.of(new Color(255, 255, 255, 235));
        ui_blur_overlay_border_color = DrawableColor.of(new Color(0, 0, 0, 235));
        ui_blur_interface_background_tint = DrawableColor.of(new Color(255, 255, 255, 235));
        ui_blur_interface_border_color = DrawableColor.of(new Color(0, 0, 0, 235));
        ui_blur_interface_title_bar_tint = DrawableColor.of(new Color(245, 245, 245, 235));
        ui_blur_interface_area_background_color_type_1 = DrawableColor.of(new Color(255, 255, 255, 235));
        ui_blur_interface_area_background_color_type_2 = DrawableColor.of(new Color(245, 245, 245, 235));
        ui_blur_interface_area_border_color = DrawableColor.of(new Color(0, 0, 0, 235));
        ui_blur_interface_area_entry_selected_color = DrawableColor.of(new Color(0, 120, 255, 140));
        ui_blur_interface_widget_background_color_normal_type_1 = DrawableColor.of(new Color(255, 255, 255, 235));
        ui_blur_interface_widget_background_color_normal_type_2 = DrawableColor.of(new Color(0, 120, 255, 210));
        ui_blur_interface_widget_background_color_hover_type_1 = DrawableColor.of(new Color(235, 235, 235, 235));
        ui_blur_interface_widget_background_color_hover_type_2 = DrawableColor.of(new Color(0, 160, 255, 235));
        ui_blur_interface_widget_border_color = DrawableColor.of(new Color(0, 0, 0, 235));
        ui_blur_interface_widget_label_color_normal = DrawableColor.of(new Color(0, 0, 0));
        ui_blur_interface_widget_label_color_inactive = DrawableColor.of(new Color(80, 80, 80));
        ui_blur_interface_input_field_background_color = DrawableColor.of(new Color(255, 255, 255, 235));
        ui_blur_interface_input_field_border_color_normal = DrawableColor.of(new Color(0, 0, 0, 235));
        ui_blur_interface_input_field_border_color_focused = DrawableColor.of(new Color(0, 120, 255, 235));
        ui_blur_interface_input_field_text_color_normal = DrawableColor.of(new Color(0, 0, 0));
        ui_blur_interface_input_field_text_color_uneditable = DrawableColor.of(new Color(80, 80, 80));
        ui_blur_interface_input_field_suggestion_text_color = DrawableColor.of(new Color(90, 90, 90));
        ui_blur_interface_generic_text_color = DrawableColor.of(new Color(0, 0, 0));
        ui_blur_tooltip_background_tint = DrawableColor.of(new Color(255, 255, 255, 245));

        ui_icon_button_hover_color = DrawableColor.of(new Color(0, 0, 0, 13));
        ui_icon_texture_color = DrawableColor.of(new Color(0, 0, 0));
        ui_overlay_background_color = DrawableColor.of(new Color(255, 255, 255));
        ui_overlay_border_color = DrawableColor.of(new Color(0, 0, 0));
        ui_interface_background_color = DrawableColor.of(new Color(255, 255, 255));
        ui_interface_border_color = DrawableColor.of(new Color(0, 0, 0));
        ui_interface_title_bar_color = DrawableColor.of(new Color(245, 245, 245));
        ui_interface_area_background_color_type_1 = DrawableColor.of(new Color(255, 255, 255));
        ui_interface_area_background_color_type_2 = DrawableColor.of(new Color(245, 245, 245));
        ui_interface_area_border_color = DrawableColor.of(new Color(0, 0, 0));
        ui_interface_area_entry_selected_color = DrawableColor.of(new Color(0, 120, 255));
        ui_interface_widget_background_color_normal_type_1 = DrawableColor.of(new Color(255, 255, 255));
        ui_interface_widget_background_color_normal_type_2 = DrawableColor.of(new Color(0, 120, 255));
        ui_interface_widget_background_color_hover_type_1 = DrawableColor.of(new Color(235, 235, 235));
        ui_interface_widget_border_color = DrawableColor.of(new Color(0, 0, 0));
        ui_interface_widget_label_color_normal = DrawableColor.of(new Color(0, 0, 0));
        ui_interface_widget_label_color_inactive = DrawableColor.of(new Color(80, 80, 80));
        ui_interface_input_field_background_color = DrawableColor.of(new Color(255, 255, 255));
        ui_interface_input_field_border_color_normal = DrawableColor.of(new Color(0, 0, 0));
        ui_interface_input_field_border_color_focused = DrawableColor.of(new Color(0, 120, 255));
        ui_interface_input_field_text_color_normal = DrawableColor.of(new Color(0, 0, 0));
        ui_interface_input_field_text_color_uneditable = DrawableColor.of(new Color(80, 80, 80));
        ui_interface_input_field_suggestion_text_color = DrawableColor.of(new Color(90, 90, 90));
        ui_interface_generic_text_color = DrawableColor.of(new Color(0, 0, 0));
        ui_tooltip_background_color = DrawableColor.of(new Color(255, 255, 255));

        success_color = DrawableColor.of(new Color(0, 150, 0));
        error_color = DrawableColor.of(new Color(200, 0, 0));
        warning_color = DrawableColor.of(new Color(180, 110, 0));

        menu_bar_close_icon_color = DrawableColor.of(new Color(200, 0, 0));
        pip_docking_overlay_color = DrawableColor.of(new Color(0, 120, 255, 120));
        pip_docking_overlay_border_color = DrawableColor.of(new Color(0, 120, 255, 220));

        scroll_grabber_color_normal = DrawableColor.of(new Color(0, 0, 0, 140));
        scroll_grabber_color_hover = DrawableColor.of(new Color(0, 120, 255, 200));


        bullet_list_dot_color_1 = DrawableColor.of(new Color(0, 120, 255));
        bullet_list_dot_color_2 = DrawableColor.of(new Color(140, 0, 255));
        bullet_list_dot_color_3 = DrawableColor.of(new Color(0, 150, 0));

        input_field_suggestions_background_color = DrawableColor.of(new Color(255, 255, 255));
        input_field_suggestions_text_color_normal = DrawableColor.of(new Color(0, 0, 0));
        input_field_suggestions_text_color_selected = DrawableColor.of(new Color(0, 120, 255));

        text_editor_line_number_text_color_normal = DrawableColor.of(new Color(60, 60, 60));
        text_editor_line_number_text_color_selected = DrawableColor.of(new Color(0, 0, 0));
        text_editor_text_color = DrawableColor.of(new Color(0, 0, 0));
        text_editor_text_formatting_nested_text_color_1 = DrawableColor.of(new Color(200, 0, 0));
        text_editor_text_formatting_nested_text_color_2 = DrawableColor.of(new Color(180, 110, 0));
        text_editor_text_formatting_nested_text_color_3 = DrawableColor.of(new Color(0, 150, 0));
        text_editor_text_formatting_nested_text_color_4 = DrawableColor.of(new Color(0, 140, 160));
        text_editor_text_formatting_nested_text_color_5 = DrawableColor.of(new Color(0, 120, 255));
        text_editor_text_formatting_nested_text_color_6 = DrawableColor.of(new Color(140, 0, 255));
        text_editor_text_formatting_nested_text_color_7 = DrawableColor.of(new Color(200, 0, 120));
        text_editor_text_formatting_nested_text_color_8 = DrawableColor.of(new Color(200, 0, 0));
        text_editor_text_formatting_nested_text_color_9 = DrawableColor.of(new Color(180, 110, 0));
        text_editor_text_formatting_nested_text_color_10 = DrawableColor.of(new Color(150, 120, 0));
        text_editor_text_formatting_nested_text_color_11 = DrawableColor.of(new Color(0, 150, 0));
        text_editor_text_formatting_nested_text_color_12 = DrawableColor.of(new Color(0, 110, 120));
        text_editor_text_formatting_brackets_color = DrawableColor.of(new Color(0, 120, 255));

    }

}
