package com.pentracker.engine;

import com.pentracker.model.Checklist;
import com.pentracker.model.ChecklistItem;

import java.util.*;
import java.util.stream.Collectors;

public class ChecklistMerger {

    private static final Map<String, Integer> SEVERITY_ORDER = Map.of(
        "CRITICAL", 0, "HIGH", 1, "MEDIUM", 2, "LOW", 3, "INFO", 4
    );

    public List<Map.Entry<Checklist, List<ChecklistItem>>> mergeGrouped(List<Checklist> checklists) {
        Set<String> seen = new LinkedHashSet<>();
        List<Map.Entry<Checklist, List<ChecklistItem>>> result = new ArrayList<>();

        for (Checklist cl : checklists) {
            List<ChecklistItem> uniqueItems = cl.getItems().stream()
                .filter(item -> seen.add(item.getId()))
                .sorted(Comparator.comparingInt(item ->
                    SEVERITY_ORDER.getOrDefault(item.getSeverity(), 99)))
                .collect(Collectors.toList());

            if (!uniqueItems.isEmpty()) {
                result.add(Map.entry(cl, uniqueItems));
            }
        }
        return result;
    }
}
