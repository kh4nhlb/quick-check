package com.pentracker.data;

import burp.api.montoya.MontoyaApi;
import burp.api.montoya.http.message.requests.HttpRequest;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.pentracker.engine.KeyNormalizer;
import com.pentracker.model.EndpointProgress;
import com.pentracker.model.ProjectProgress;

import java.io.*;
import java.nio.file.*;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;

public class ProgressStore {

    private static final String PREF_PROJECT_DIR   = "pentracker.projectDir";
    private static final String PREF_CHECKLIST_DIR = "pentracker.checklistDir";
    private static final String PROGRESS_FILENAME  = "pentracker-progress.json";

    private final MontoyaApi api;
    private final KeyNormalizer normalizer = new KeyNormalizer();
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "PenTracker-Save");
        t.setDaemon(true);
        return t;
    });

    private ScheduledFuture<?> pendingSave;
    private File projectDir;
    private File progressFile;
    private File checklistDir;
    private ProjectProgress progress = new ProjectProgress();
    private final List<Runnable> changeListeners = new ArrayList<>();

    public ProgressStore(MontoyaApi api) {
        this.api = api;
        String savedDir = api.persistence().extensionData().getString(PREF_PROJECT_DIR);
        if (savedDir != null && !savedDir.isBlank()) {
            setProjectDir(new File(savedDir));
        }
        String savedChecklistDir = api.persistence().extensionData().getString(PREF_CHECKLIST_DIR);
        if (savedChecklistDir != null && !savedChecklistDir.isBlank()) {
            this.checklistDir = new File(savedChecklistDir);
        }
    }

    public void setProjectDir(File dir) {
        this.projectDir = dir;
        this.progressFile = new File(dir, PROGRESS_FILENAME);
        this.progress = load();
        api.persistence().extensionData().setString(PREF_PROJECT_DIR, dir.getAbsolutePath());
        notifyListeners();
    }

    public File getProjectDir() { return projectDir; }
    public boolean hasProjectDir() { return projectDir != null; }

    public File getChecklistDir() { return checklistDir; }
    public boolean hasChecklistDir() { return checklistDir != null; }

    public void setChecklistDir(File dir) {
        this.checklistDir = dir;
        api.persistence().extensionData().setString(PREF_CHECKLIST_DIR, dir.getAbsolutePath());
    }

    public void clearChecklistDir() {
        this.checklistDir = null;
        api.persistence().extensionData().setString(PREF_CHECKLIST_DIR, "");
    }

    private ProjectProgress load() {
        if (progressFile == null || !progressFile.exists()) {
            ProjectProgress fresh = new ProjectProgress();
            fresh.setCreated(Instant.now().toString());
            return fresh;
        }
        try {
            String json = Files.readString(progressFile.toPath());
            ProjectProgress loaded = gson.fromJson(json, ProjectProgress.class);
            return loaded != null ? loaded : new ProjectProgress();
        } catch (Exception e) {
            api.logging().logToError("PenTracker: failed to load progress — " + e.getMessage());
            return new ProjectProgress();
        }
    }

    public EndpointProgress getOrCreate(HttpRequest request) {
        String key = normalizer.normalize(request);
        return progress.getEndpoints().computeIfAbsent(key, k -> {
            EndpointProgress ep = new EndpointProgress();
            ep.setHost(request.httpService().host());
            ep.setMethod(request.method());
            ep.setPathPattern(normalizer.normalizeForDisplay(request));
            ep.setFirstTested(Instant.now().toString());
            return ep;
        });
    }

    public void save(HttpRequest request, EndpointProgress ep) {
        if (progressFile == null) return;
        String key = normalizer.normalize(request);
        ep.setLastUpdated(Instant.now().toString());
        progress.getEndpoints().put(key, ep);
        scheduleSave();
        notifyListeners();
    }

    public String normalize(HttpRequest request) {
        return normalizer.normalize(request);
    }

    public Map<String, EndpointProgress> getAll() {
        return Collections.unmodifiableMap(progress.getEndpoints());
    }

    public int getEndpointCount() {
        return progress.getEndpoints().size();
    }

    public void addChangeListener(Runnable listener) {
        changeListeners.add(listener);
    }

    private void notifyListeners() {
        changeListeners.forEach(r -> {
            try { r.run(); } catch (Exception ignored) {}
        });
    }

    private void scheduleSave() {
        if (pendingSave != null && !pendingSave.isDone()) pendingSave.cancel(false);
        pendingSave = scheduler.schedule(this::saveNow, 500, TimeUnit.MILLISECONDS);
    }

    private void saveNow() {
        if (progressFile == null) return;
        try {
            progress.setUpdated(Instant.now().toString());
            Files.writeString(progressFile.toPath(), gson.toJson(progress));
        } catch (IOException e) {
            api.logging().logToError("PenTracker: save failed — " + e.getMessage());
        }
    }

    public void shutdown() {
        scheduler.shutdownNow();
    }
}
