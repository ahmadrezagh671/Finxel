package com.ahmadrezagh671.finxel.models.configModel;

import java.util.List;

/**
 * Holds metadata about a configuration, including its name, associated SMS providers,
 * display color, and whether location data should be extracted.
 */
public class Information {
    public String name,appConfigVersion;
    public List<String> provider;
    public int color;
    public boolean location;

    public Information(String name,String appConfigVersion, List<String> provider, int color, boolean location) {
        this.name = name;
        this.appConfigVersion = appConfigVersion;
        this.provider = provider;
        this.color = color;
        this.location = location;
    }
}