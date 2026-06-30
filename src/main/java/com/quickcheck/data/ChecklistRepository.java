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
        // Test trên hầu hết mọi API
        "auth_session", "broken_access", "injection", "sensitive_data", "error_handling", "rate_dos",
        // Rất phổ biến, hầu hết project
        "register_login", "crud_api", "business_logic", "client_side", "otp_2fa", "open_redirect",
        // Feature-specific, khá phổ biến
        "file_upload", "ssrf", "payment_logic", "email_function", "websocket", "xxe",
        // Ít phổ biến, một số loại app
        "path_traversal", "cache", "host_header", "ekyc",
        // Hiếm gặp, tech-specific
        "lfi_rfi", "deserialization", "http_smuggling", "framework_specific",
        // One-time recon
        "system_wide"
    };

    private final Gson gson = new Gson();
    private final Map<String, Checklist> map = new LinkedHashMap<>();
    private File externalDir = null;

    public ChecklistRepository() {
        loadBuiltIn();
    }

    private void loadBuiltIn() {
        for (String id : BUILTIN_ORDER) {
            try (InputStream is = getClass().getResourceAsStream("/checklists/" + id + ".json")) {
                if (is != null) {
                    Checklist cl = gson.fromJson(new String(is.readAllBytes()), Checklist.class);
                    map.put(cl.getId(), cl);
                }
            } catch (Exception ignored) {}
        }
    }

    private void loadJsonsFromDir(File dir) {
        if (dir == null || !dir.isDirectory()) return;
        File[] files = dir.listFiles((d, name) -> name.endsWith(".json"));
        if (files == null) return;

        for (File file : files) {
            try {
                String json = Files.readString(file.toPath());
                Checklist external = gson.fromJson(json, Checklist.class);
                if (external == null || external.getId() == null) continue;
                if (map.containsKey(external.getId())) {
                    map.put(external.getId(), mergeInto(map.get(external.getId()), external));
                } else {
                    map.put(external.getId(), external);
                }
            } catch (Exception ignored) {}
        }
    }

    public void setExternalDir(File dir) { this.externalDir = dir; }
    public void clearExternalDir()       { this.externalDir = null; }
    public File getExternalDir()         { return externalDir; }

    public int countExternal() {
        if (externalDir == null || !externalDir.isDirectory()) return 0;
        File[] files = externalDir.listFiles((d, n) -> n.endsWith(".json"));
        return files == null ? 0 : files.length;
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

    public void reload(File projectDir) {
        map.clear();
        loadBuiltIn();
        if (projectDir != null) loadJsonsFromDir(new File(projectDir, "custom-checklists"));
        if (externalDir != null) loadJsonsFromDir(externalDir);
    }
}
