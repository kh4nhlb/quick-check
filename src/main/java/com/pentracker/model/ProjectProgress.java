package com.pentracker.model;

import java.util.*;

public class ProjectProgress {
    private String version = "1";
    private String projectName;
    private String projectDir;
    private String created;
    private String updated;
    private Map<String, EndpointProgress> endpoints = new LinkedHashMap<>();

    public String getVersion() { return version; }

    public String getProjectName() { return projectName != null ? projectName : ""; }
    public void setProjectName(String projectName) { this.projectName = projectName; }

    public String getProjectDir() { return projectDir; }
    public void setProjectDir(String projectDir) { this.projectDir = projectDir; }

    public String getCreated() { return created; }
    public void setCreated(String created) { this.created = created; }

    public String getUpdated() { return updated; }
    public void setUpdated(String updated) { this.updated = updated; }

    public Map<String, EndpointProgress> getEndpoints() {
        if (endpoints == null) endpoints = new LinkedHashMap<>();
        return endpoints;
    }
    public void setEndpoints(Map<String, EndpointProgress> endpoints) { this.endpoints = endpoints; }
}
