package com.ahmadrezagh671.finxel.models.configModel;

/**
 * Defines a find-and-replace rule for transforming extracted field values,
 * applying regex-based substitutions before or after field extraction.
 */
public class Replacement {
    public String find;
    public String replace;

    public Replacement(String find, String replace) {
        this.find = find;
        this.replace = replace;
    }
}