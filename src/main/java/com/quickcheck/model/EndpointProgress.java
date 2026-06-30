package com.quickcheck.model;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public class EndpointProgress {
    private String host;
    private String method;
    private String pathPattern;
    private Map<String, ItemState> items = new LinkedHashMap<>();
    private int totalItems;
    private int doneCount;
    private boolean completed;
    private String firstTested;
    private String lastUpdated;

    public String getHost() { return host; }
    public void setHost(String host) { this.host = host; }

    public String getMethod() { return method; }
    public void setMethod(String method) { this.method = method; }

    public String getPathPattern() { return pathPattern; }
    public void setPathPattern(String pathPattern) { this.pathPattern = pathPattern; }

    public Map<String, ItemState> getItems() {
        return items != null ? items : new LinkedHashMap<>();
    }
    public void setItems(Map<String, ItemState> items) { this.items = items; }

    public int getTotalItems() { return totalItems; }
    public int getDoneCount() { return doneCount; }

    public boolean isCompleted() { return completed; }
    public void setCompleted(boolean completed) { this.completed = completed; }

    public String getFirstTested() { return firstTested; }
    public void setFirstTested(String firstTested) { this.firstTested = firstTested; }

    public String getLastUpdated() { return lastUpdated; }
    public void setLastUpdated(String lastUpdated) { this.lastUpdated = lastUpdated; }

    public void recalcStats(java.util.Set<String> activeItemIds) {
        if (items == null || activeItemIds == null || activeItemIds.isEmpty()) {
            totalItems = 0; doneCount = 0; return;
        }
        totalItems = activeItemIds.size();
        doneCount = (int) activeItemIds.stream()
            .filter(id -> items.containsKey(id) && items.get(id).isDone())
            .count();
    }

    public void recalcStats() {
        if (items == null) { totalItems = 0; doneCount = 0; return; }
        totalItems = items.size();
        doneCount = (int) items.values().stream().filter(ItemState::isDone).count();
    }

    public ItemState getOrCreateItemState(String itemId) {
        if (items == null) items = new LinkedHashMap<>();
        return items.computeIfAbsent(itemId, k -> new ItemState());
    }
}
