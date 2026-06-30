package com.pentracker;

import burp.api.montoya.BurpExtension;
import burp.api.montoya.MontoyaApi;
import com.pentracker.data.ChecklistRepository;
import com.pentracker.data.ProgressStore;
import com.pentracker.engine.ChecklistMerger;
import com.pentracker.handler.PenTrackerContextMenu;
import com.pentracker.ui.ChecklistFrame;
import com.pentracker.ui.SettingsTab;

public class PenTrackerExtension implements BurpExtension {

    @Override
    public void initialize(MontoyaApi api) {
        api.extension().setName("PenTracker");

        ChecklistRepository repo = new ChecklistRepository();
        ProgressStore store = new ProgressStore(api);
        ChecklistMerger merger = new ChecklistMerger();

        if (store.hasChecklistDir()) {
            repo.setExternalDir(store.getChecklistDir());
        }
        if (store.hasProjectDir()) {
            repo.reload(store.getProjectDir());
        }

        ChecklistFrame checklistFrame = new ChecklistFrame(repo, store, merger);
        java.util.List<java.awt.Image> burpIcons =
            api.userInterface().swingUtils().suiteFrame().getIconImages();
        if (!burpIcons.isEmpty()) checklistFrame.setIconImages(burpIcons);

        SettingsTab settingsTab = new SettingsTab(repo, store);
        api.userInterface().registerSuiteTab("PenTracker", settingsTab);
        api.userInterface().registerContextMenuItemsProvider(
            new PenTrackerContextMenu(api, checklistFrame));

        api.extension().registerUnloadingHandler(() -> {
            store.shutdown();
            checklistFrame.dispose();
        });

        api.logging().logToOutput("PenTracker loaded — right-click any request → PenTracker Checklist");
    }
}
