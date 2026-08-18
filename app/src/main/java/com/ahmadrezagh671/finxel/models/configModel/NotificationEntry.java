package com.ahmadrezagh671.finxel.models.configModel;

import java.io.Serializable;

/**
 * Represents an entry from a notification, carrying a name and hint text
 * for display or further processing in the extraction pipeline.
 */
public class NotificationEntry implements Serializable {
    public String name,hint;

    public NotificationEntry(String name, String hint) {
        this.name = name;
        this.hint = hint;
    }
}
