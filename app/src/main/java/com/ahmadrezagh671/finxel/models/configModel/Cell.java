package com.ahmadrezagh671.finxel.models.configModel;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a cell in a layout grid, holding display and sizing properties
 * along with optional items for spinner/dropdown components.
 */
public class Cell {
    public String name,value,type;
    public float size = 1f;
    public int b_color = -1,f_color = -1;
    public List<String> items = new ArrayList<>();

}
