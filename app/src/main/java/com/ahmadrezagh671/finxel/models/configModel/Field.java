package com.ahmadrezagh671.finxel.models.configModel;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Represents a field definition used to extract or compute values from SMS messages.
 * Supports regex extraction, date formatting, math operations, and field combination.
 */
public class Field {
    public String name;
    public String type;

    // Optional fields (initialized with defaults)
    public String regex = "";
    public int group = 0;
    public String format = "";
    public String text = "";
    public String key = "";

    public String operator = "";
    public List<Replacement> replacementsAfter = new ArrayList<>();
    public List<Replacement> replacementsBefore = new ArrayList<>();
    public List<String> fields = new ArrayList<>();
    public List<String> flags = new ArrayList<>();

    /**
     * Converts the list of flag names into their corresponding Pattern integer bitmask.
     *
     * @return the combined flags integer for regex compilation
     */
    public int getFlagsAsInt() {
        List<String> flagsArray = flags;
        int flags = 0;
        for (int j = 0; j < flagsArray.size(); j++) {
            String flagName = flagsArray.get(j);
            switch (flagName) {
                case "CASE_INSENSITIVE":
                    flags |= Pattern.CASE_INSENSITIVE;
                    break;
                case "MULTILINE":
                    flags |= Pattern.MULTILINE;
                    break;
                case "DOTALL":
                    flags |= Pattern.DOTALL;
                    break;
                case "UNICODE_CASE":
                    flags |= Pattern.UNICODE_CASE;
                    break;
                case "COMMENTS":
                    flags |= Pattern.COMMENTS;
                    break;
                case "LITERAL":
                    flags |= Pattern.LITERAL;
                    break;
                case "UNICODE_CHARACTER_CLASS":
                    flags |= Pattern.UNICODE_CHARACTER_CLASS;
                    break;
                case "CANON_EQ":
                    flags |= Pattern.CANON_EQ;
                    break;
            }
        }
        return flags;
    }
}