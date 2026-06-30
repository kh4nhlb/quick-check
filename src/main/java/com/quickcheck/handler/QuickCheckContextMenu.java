package com.quickcheck.handler;

import burp.api.montoya.MontoyaApi;
import burp.api.montoya.http.message.HttpRequestResponse;
import burp.api.montoya.ui.contextmenu.ContextMenuEvent;
import burp.api.montoya.ui.contextmenu.ContextMenuItemsProvider;
import com.quickcheck.ui.ChecklistFrame;

import javax.swing.*;
import java.awt.*;
import java.util.List;

public class QuickCheckContextMenu implements ContextMenuItemsProvider {

    private final MontoyaApi api;
    private final ChecklistFrame checklistFrame;

    public QuickCheckContextMenu(MontoyaApi api, ChecklistFrame checklistFrame) {
        this.api = api;
        this.checklistFrame = checklistFrame;
    }

    @Override
    public List<Component> provideMenuItems(ContextMenuEvent event) {
        List<HttpRequestResponse> selected = event.selectedRequestResponses();
        boolean hasEditor = event.messageEditorRequestResponse().isPresent();

        if (selected.isEmpty() && !hasEditor) return List.of();

        JMenuItem item = new JMenuItem("QuickChecklist");
        item.addActionListener(e -> {
            HttpRequestResponse rr = selected.isEmpty()
                ? event.messageEditorRequestResponse().get().requestResponse()
                : selected.get(0);

            if (rr.request() == null) return;

            SwingUtilities.invokeLater(() ->
                checklistFrame.loadChecklist(rr.request()));
        });

        return List.of(item);
    }
}
