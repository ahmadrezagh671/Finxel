package com.ahmadrezagh671.finxel.models.configModel;

import java.util.ArrayList;
import java.util.List;

/**
 * Defines a UI component in a layout, such as a text field, spinner, or button,
 * with properties for id, type, hint text, default value, and child components.
 */
public class LayoutComponent {

    public String id,type,hint = "",value = "";

    // for spinner
    public List<String> items = new ArrayList<>();
    public int selected = -1;

    public List<LayoutComponent> inside = new ArrayList<>();


}
