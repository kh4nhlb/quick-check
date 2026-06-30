package com.pentracker.model;

import java.util.List;

public class Checklist {
    private String id;
    private String name;
    private String group;
    private List<ChecklistItem> items;

    public String getId() { return id; }
    public String getName() { return name != null ? name : id; }
    public String getGroup() { return group != null ? group : "vuln"; }
    public List<ChecklistItem> getItems() { return items != null ? items : List.of(); }
}
