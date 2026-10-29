package com.itsm.itsm.model;

public enum NoteType {
    NOTE("Investigation note"),
    QUESTION("Question for the reporter"),
    REPLY("Reply from the reporter");

    private final String label;

    NoteType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
