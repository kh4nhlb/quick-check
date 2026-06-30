package com.quickcheck.data;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.quickcheck.model.Checklist;
import com.quickcheck.model.ChecklistItem;

import java.io.*;
import java.nio.file.*;
import java.util.*;

public class ChecklistRepository {

    private static final String[] BUILTIN_ORDER = {
        "broken_access", "auth_session", "injection",
        "security_misconfig", "file_resource", "business_logic", "rate_dos"
    };

    private final Gson gson = new Gson();
    private final Map<String, Checklist> map = new LinkedHashMap<>();

    public ChecklistRepository() {
        loadBuiltIn();
    }

    private void loadBuiltIn() {
        for (String id : BUILTIN_ORDER) {
            String resourcePath = "/checklists/" + id + ".json";
            try (InputStream is = getClass().getResourceAsStream(resourcePath)) {
                if (is != null) {
                    Checklist cl = gson.fromJson(new String(is.readAllBytes()), Checklist.class);
                    map.put(cl.getId(), cl);
                }
            } catch (Exception ignored) {}
        }
    }

    public void loadExternal(File projectDir) {
        File customDir = new File(projectDir, "custom-checklists");
        if (!customDir.isDirectory()) return;

        File[] files = customDir.listFiles((d, name) -> name.endsWith(".json"));
        if (files == null) return;

        for (File file : files) {
            try {
                String json = Files.readString(file.toPath());
                Checklist external = gson.fromJson(json, Checklist.class);
                String id = external.getId();

                if (map.containsKey(id)) {
                    map.put(id, mergeInto(map.get(id), external));
                } else {
                    map.put(id, external);
                }
            } catch (Exception ignored) {}
        }
    }

    private Checklist mergeInto(Checklist base, Checklist override) {
        Map<String, ChecklistItem> merged = new LinkedHashMap<>();
        base.getItems().forEach(item -> merged.put(item.getId(), item));
        override.getItems().forEach(item -> merged.put(item.getId(), item)); // override wins

        JsonObject obj = gson.toJsonTree(base).getAsJsonObject();
        JsonArray items = gson.toJsonTree(new ArrayList<>(merged.values())).getAsJsonArray();
        obj.add("items", items);
        return gson.fromJson(obj, Checklist.class);
    }

    public List<Checklist> getAll() {
        return Collections.unmodifiableList(new ArrayList<>(map.values()));
    }

    public Optional<Checklist> getById(String id) {
        return Optional.ofNullable(map.get(id));
    }

    /** Reload built-in checklists and re-apply external overrides. */
    public void reload(File projectDir) {
        map.clear();
        loadBuiltIn();
        if (projectDir != null) loadExternal(projectDir);
    }
}
