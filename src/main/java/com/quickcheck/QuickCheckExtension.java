package com.quickcheck;

import burp.api.montoya.BurpExtension;
import burp.api.montoya.MontoyaApi;
import com.quickcheck.data.ChecklistRepository;
import com.quickcheck.data.ProgressStore;
import com.quickcheck.engine.ChecklistMerger;
import com.quickcheck.handler.QuickCheckContextMenu;
import com.quickcheck.ui.ChecklistFrame;
import com.quickcheck.ui.SettingsTab;

public class QuickCheckExtension implements BurpExtension {

    @Override
    public void initialize(MontoyaApi api) {
        api.extension().setName("QuickCheck");

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
        api.userInterface().registerSuiteTab("QuickCheck", settingsTab);
        api.userInterface().registerContextMenuItemsProvider(
            new QuickCheckContextMenu(api, checklistFrame));

        api.extension().registerUnloadingHandler(() -> {
            store.shutdown();
            checklistFrame.dispose();
        });

        api.logging().logToOutput("QuickCheck loaded — right-click any request → QuickChecklist");
    }
}
