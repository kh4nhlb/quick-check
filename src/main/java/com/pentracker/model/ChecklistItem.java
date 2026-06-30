package com.pentracker.model;

import java.util.List;

public class ChecklistItem {
    private String id;
    private String title;
    private String severity;
    private String description;
    private String reference;
    private List<String> tags;

    public String getId() { return id; }
    public String getTitle() { return title != null ? title : ""; }
    public String getSeverity() { return severity != null ? severity.toUpperCase() : "INFO"; }
    public String getDescription() { return description != null ? description : ""; }
    public String getReference() { return reference != null ? reference : ""; }
    public List<String> getTags() { return tags != null ? tags : List.of(); }
}
