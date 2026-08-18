package com.ahmadrezagh671.finxel.models.configModel;

/**
 * Defines a rule for comparing the last message against a specific field,
 * used to trigger actions or validations based on message content.
 */
public class CompareWithLastMessage {

    public String name,field,lastMessageField,errorText;

    public CompareWithLastMessage(String name, String field, String lastMessageField, String errorText) {
        this.name = name;
        this.field = field;
        this.lastMessageField = lastMessageField;
        this.errorText = errorText;
    }
}
