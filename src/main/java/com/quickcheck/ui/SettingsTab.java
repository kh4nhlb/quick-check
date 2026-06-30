package com.quickcheck.ui;

import com.quickcheck.data.ChecklistRepository;
import com.quickcheck.data.ProgressStore;
import com.quickcheck.model.EndpointProgress;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.File;
import java.util.Map;

public class SettingsTab extends JPanel {

    private final ChecklistRepository repo;
    private final ProgressStore store;
    private final JLabel dirStatusLabel;
    private final DefaultTableModel coverageModel;

    public SettingsTab(ChecklistRepository repo, ProgressStore store) {
        this.repo = repo;
        this.store = store;

        setLayout(new BorderLayout(0, 0));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // ── Settings section ─────────────────────────────────────────────────
        JPanel settingsPanel = new JPanel(new GridBagLayout());
        settingsPanel.setBorder(BorderFactory.createTitledBorder("Project Settings"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 8, 5, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JTextField dirField = new JTextField(
            store.hasProjectDir() ? store.getProjectDir().getAbsolutePath() : "", 32);
        JButton browseBtn = new JButton("Browse…");
        browseBtn.addActionListener(e -> {
            JFileChooser fc = new JFileChooser(
                store.hasProjectDir() ? store.getProjectDir() : new File(System.getProperty("user.home")));
            fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                dirField.setText(fc.getSelectedFile().getAbsolutePath());
            }
        });

        JButton saveBtn = new JButton("Save & Reload");
        saveBtn.addActionListener(e -> {
            String path = dirField.getText().trim();
            if (path.isBlank()) return;
            File dir = new File(path);
            if (!dir.exists() && !dir.mkdirs()) {
                JOptionPane.showMessageDialog(this, "Cannot create: " + path, "Error",
                    JOptionPane.ERROR_MESSAGE);
                return;
            }
            store.setProjectDir(dir);
            repo.reload(dir);
            updateStatus();
            refreshCoverage();
        });

        dirStatusLabel = new JLabel(buildStatus());
        dirStatusLabel.setFont(dirStatusLabel.getFont().deriveFont(Font.ITALIC, 11f));
        dirStatusLabel.setForeground(Color.GRAY);

        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0;
        settingsPanel.add(new JLabel("Project directory:"), gbc);
        gbc.gridx = 1; gbc.weightx = 1.0;
        settingsPanel.add(dirField, gbc);
        gbc.gridx = 2; gbc.weightx = 0;
        settingsPanel.add(browseBtn, gbc);
        gbc.gridx = 3;
        settingsPanel.add(saveBtn, gbc);
        gbc.gridx = 1; gbc.gridy = 1; gbc.gridwidth = 4;
        settingsPanel.add(dirStatusLabel, gbc);

        // ── External checklist directory ─────────────────────────────────────
        JTextField clDirField = new JTextField(
            store.hasChecklistDir() ? store.getChecklistDir().getAbsolutePath() : "", 32);

        JButton clBrowseBtn = new JButton("Browse…");
        clBrowseBtn.addActionListener(e -> {
            JFileChooser fc = new JFileChooser(
                store.hasChecklistDir() ? store.getChecklistDir()
                    : store.hasProjectDir() ? store.getProjectDir()
                    : new File(System.getProperty("user.home")));
            fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION)
                clDirField.setText(fc.getSelectedFile().getAbsolutePath());
        });

        JLabel clStatusLabel = new JLabel(buildChecklistStatus());
        clStatusLabel.setFont(clStatusLabel.getFont().deriveFont(Font.ITALIC, 11f));
        clStatusLabel.setForeground(Color.GRAY);

        JButton clLoadBtn = new JButton("Load");
        clLoadBtn.addActionListener(e -> {
            String path = clDirField.getText().trim();
            if (path.isBlank()) return;
            File dir = new File(path);
            if (!dir.isDirectory()) {
                JOptionPane.showMessageDialog(this, "Directory not found:\n" + path,
                    "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            store.setChecklistDir(dir);
            repo.setExternalDir(dir);
            repo.reload(store.getProjectDir());
            clStatusLabel.setText(buildChecklistStatus());
        });

        JButton clUnloadBtn = new JButton("Unload");
        clUnloadBtn.addActionListener(e -> {
            store.clearChecklistDir();
            repo.clearExternalDir();
            repo.reload(store.getProjectDir());
            clDirField.setText("");
            clStatusLabel.setText(buildChecklistStatus());
        });

        gbc.gridwidth = 1;
        gbc.gridx = 0; gbc.gridy = 2; gbc.weightx = 0;
        settingsPanel.add(new JLabel("Checklist directory:"), gbc);
        gbc.gridx = 1; gbc.weightx = 1.0;
        settingsPanel.add(clDirField, gbc);
        gbc.gridx = 2; gbc.weightx = 0;
        settingsPanel.add(clBrowseBtn, gbc);
        gbc.gridx = 3;
        settingsPanel.add(clLoadBtn, gbc);
        gbc.gridx = 4;
        settingsPanel.add(clUnloadBtn, gbc);
        gbc.gridx = 1; gbc.gridy = 3; gbc.gridwidth = 4;
        settingsPanel.add(clStatusLabel, gbc);

        // ── Coverage table ───────────────────────────────────────────────────
        String[] cols = {"Method", "Host", "Endpoint", "Status", "Done", "Total", "Last Updated"};
        coverageModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable coverageTable = new JTable(coverageModel);
        coverageTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        coverageTable.setRowSorter(new javax.swing.table.TableRowSorter<>(coverageModel));
        coverageTable.setDefaultRenderer(Object.class, new javax.swing.table.DefaultTableCellRenderer() {
            @Override public java.awt.Component getTableCellRendererComponent(
                    JTable t, Object v, boolean sel, boolean foc, int r, int c) {
                super.getTableCellRendererComponent(t, v, sel, foc, r, c);
                setBorder(BorderFactory.createEmptyBorder(0, 6, 0, 0));
                return this;
            }
        });
        coverageTable.getTableHeader().setDefaultRenderer(new javax.swing.table.DefaultTableCellRenderer() {
            { setHorizontalAlignment(LEFT); }
            @Override public java.awt.Component getTableCellRendererComponent(
                    JTable t, Object v, boolean sel, boolean foc, int r, int c) {
                super.getTableCellRendererComponent(t, v, sel, foc, r, c);
                setBorder(BorderFactory.createEmptyBorder(0, 6, 0, 0));
                return this;
            }
        });
        coverageTable.getColumnModel().getColumn(0).setPreferredWidth(60);
        coverageTable.getColumnModel().getColumn(1).setPreferredWidth(200);
        coverageTable.getColumnModel().getColumn(2).setPreferredWidth(280);
        coverageTable.getColumnModel().getColumn(3).setPreferredWidth(55);
        coverageTable.getColumnModel().getColumn(4).setPreferredWidth(45);
        coverageTable.getColumnModel().getColumn(5).setPreferredWidth(45);
        coverageTable.getColumnModel().getColumn(6).setPreferredWidth(150);

        JScrollPane tableScroll = new JScrollPane(coverageTable);
        tableScroll.setBorder(BorderFactory.createTitledBorder("Coverage — Endpoints tracked"));

        JButton refreshBtn = new JButton("Refresh");
        refreshBtn.addActionListener(e -> refreshCoverage());

        JPanel coverageHeader = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        coverageHeader.add(refreshBtn);

        JPanel coveragePanel = new JPanel(new BorderLayout());
        coveragePanel.add(coverageHeader, BorderLayout.NORTH);
        coveragePanel.add(tableScroll, BorderLayout.CENTER);

        store.addChangeListener(() -> SwingUtilities.invokeLater(this::refreshCoverage));

        // ── Layout ───────────────────────────────────────────────────────────
        add(settingsPanel, BorderLayout.NORTH);
        add(coveragePanel, BorderLayout.CENTER);

        refreshCoverage();
    }

    private void updateStatus() {
        dirStatusLabel.setText(buildStatus());
    }

    private String buildStatus() {
        if (!store.hasProjectDir()) return "No project directory set.";
        return "progress file: " + store.getProjectDir().getAbsolutePath()
            + "\\quickcheck-progress.json  |  " + store.getEndpointCount() + " endpoint(s) tracked";
    }

    private String buildChecklistStatus() {
        if (!store.hasChecklistDir()) return "Using built-in checklists only.";
        int count = repo.countExternal();
        return "Loaded " + count + " external checklist(s) from: " + store.getChecklistDir().getAbsolutePath();
    }

    private void refreshCoverage() {
        coverageModel.setRowCount(0);
        Map<String, EndpointProgress> all = store.getAll();
        for (Map.Entry<String, EndpointProgress> entry : all.entrySet()) {
            EndpointProgress ep = entry.getValue();
            String method = ep.getMethod() != null ? ep.getMethod() : "";
            String path   = ep.getPathPattern() != null ? ep.getPathPattern() : "";
            String ts = ep.getLastUpdated() != null ? ep.getLastUpdated()
                      : ep.getFirstTested() != null ? ep.getFirstTested() : "";
            String lastUpdated = ts.isEmpty() ? "" :
                ts.replace("T", " ").substring(0, Math.min(19, ts.length()));
            String host   = ep.getHost() != null ? ep.getHost() : "";
            String status = ep.isCompleted() ? "✓" : "";
            coverageModel.addRow(new Object[]{method, host, path, status, ep.getDoneCount(), ep.getTotalItems(), lastUpdated});
        }
    }
}
