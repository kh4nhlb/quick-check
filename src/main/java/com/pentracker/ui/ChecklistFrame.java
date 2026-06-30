package com.pentracker.ui;

import burp.api.montoya.http.message.requests.HttpRequest;
import com.pentracker.data.ChecklistRepository;
import com.pentracker.data.ProgressStore;
import com.pentracker.engine.ChecklistMerger;
import com.pentracker.model.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

public class ChecklistFrame extends JFrame {

    private static final Color ACCENT = new Color(88, 153, 216);
    private static final Map<String, Color> METHOD_COLOR = Map.of(
        "GET",    new Color( 97, 175, 254),
        "POST",   new Color( 73, 204, 144),
        "PUT",    new Color(252, 161,  48),
        "PATCH",  new Color(252, 161,  48),
        "DELETE", new Color(249,  62,  62)
    );

    private final ChecklistRepository repo;
    private final ProgressStore store;
    private final ChecklistMerger merger;

    private HttpRequest currentRequest;
    private EndpointProgress currentProgress;
    private Set<String> activeItemIds = new LinkedHashSet<>();

    private final JLabel methodLabel = new JLabel("");
    private final JLabel titleLabel  = new JLabel("—");
    private final JLabel progressLbl = new JLabel("0 / 0");
    private final JProgressBar progressBar = new JProgressBar(0, 1);
    private final JPanel contentPanel = new JPanel();
    private final JButton doneBtn = new JButton("Done");
    private final List<Runnable> collapseActions = new ArrayList<>();
    private boolean pinned = true;

    public ChecklistFrame(ChecklistRepository repo, ProgressStore store, ChecklistMerger merger) {
        super("Pentest Tracker  —  by kh4nhlb");
        this.repo = repo;
        this.store = store;
        this.merger = merger;

        setSize(560, 680);
        setLocationByPlatform(true);
        setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
        setAlwaysOnTop(true);
        setLayout(new BorderLayout());

        Font base = new JLabel().getFont();
        methodLabel.setFont(base.deriveFont(Font.BOLD, 15f));
        titleLabel.setFont(base.deriveFont(Font.BOLD, 15f));
        progressLbl.setFont(base.deriveFont(13f));

        add(buildTopBar(),    BorderLayout.NORTH);
        add(buildCenter(),    BorderLayout.CENTER);
        add(buildBottomBar(), BorderLayout.SOUTH);
    }

    // ── Layout ────────────────────────────────────────────────────────────────

    private JPanel buildTopBar() {
        JPanel bar = new JPanel(new BorderLayout(0, 0));
        bar.setBorder(BorderFactory.createEmptyBorder(10, 16, 10, 16));
        bar.add(methodLabel, BorderLayout.WEST);
        bar.add(titleLabel,  BorderLayout.CENTER);

        JPanel wrap = new JPanel(new BorderLayout());
        wrap.add(new JSeparator(), BorderLayout.NORTH);
        wrap.add(bar,              BorderLayout.CENTER);
        wrap.add(new JSeparator(), BorderLayout.SOUTH);
        return wrap;
    }

    private JScrollPane buildCenter() {
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        contentPanel.setBorder(BorderFactory.createEmptyBorder(4, 0, 4, 0));

        JScrollPane sp = new JScrollPane(contentPanel,
            JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
            JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        sp.setBorder(null);
        sp.getVerticalScrollBar().setUnitIncrement(16);
        return sp;
    }

    private JPanel buildBottomBar() {
        progressBar.setPreferredSize(new Dimension(140, 8));
        progressBar.setBorderPainted(false);
        progressBar.setStringPainted(false);
        progressBar.setForeground(ACCENT);

        Font base = new JLabel().getFont();

        JButton pinBtn = new JButton(makePinIcon(true));
        pinBtn.setFocusPainted(false);
        pinBtn.setBorderPainted(false);
        pinBtn.setOpaque(false);
        pinBtn.setToolTipText("Unpin — allow window to go behind Burp");
        pinBtn.addActionListener(e -> {
            pinned = !pinned;
            setAlwaysOnTop(pinned);
            pinBtn.setIcon(makePinIcon(pinned));
            pinBtn.setToolTipText(pinned
                ? "Unpin — allow window to go behind Burp"
                : "Pin on top");
        });

        JButton collapseAllBtn = new JButton("Collapse All");
        collapseAllBtn.setFont(base.deriveFont(Font.BOLD, 14f));
        collapseAllBtn.setFocusPainted(false);
        collapseAllBtn.addActionListener(e -> {
            collapseActions.forEach(Runnable::run);
            contentPanel.revalidate();
            contentPanel.repaint();
        });

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        left.setOpaque(false);
        left.add(pinBtn);
        left.add(collapseAllBtn);

        JPanel center = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 0));
        center.setOpaque(false);
        center.add(progressBar);
        center.add(progressLbl);

        Color doneBtnNormal = new Color(255, 102, 51);
        Color doneBtnHover  = new Color(210, 78, 35);

        doneBtn.setFont(base.deriveFont(Font.BOLD, 14f));
        doneBtn.setFocusPainted(false);
        doneBtn.setBorderPainted(false);
        doneBtn.setOpaque(true);
        doneBtn.setBackground(doneBtnNormal);
        doneBtn.setForeground(Color.WHITE);
        doneBtn.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) { doneBtn.setBackground(doneBtnHover); }
            @Override public void mouseExited(MouseEvent e)  { doneBtn.setBackground(doneBtnNormal); }
        });
        doneBtn.addActionListener(e -> onDoneClicked());

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        right.setOpaque(false);
        right.add(doneBtn);

        JPanel bar = new JPanel(new BorderLayout());
        bar.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(1, 0, 0, 0,
                UIManager.getColor("Separator.foreground")),
            BorderFactory.createEmptyBorder(8, 12, 8, 12)));
        bar.add(left,   BorderLayout.WEST);
        bar.add(center, BorderLayout.CENTER);
        bar.add(right,  BorderLayout.EAST);
        return bar;
    }

    // ── Public API ────────────────────────────────────────────────────────────

    public void loadChecklist(HttpRequest request) {
        this.currentRequest = request;
        this.currentProgress = store.getOrCreate(request);

        String method = request.method();
        String path   = normalizePath(displayPath(request));
        methodLabel.setText(method + " ");
        methodLabel.setForeground(METHOD_COLOR.getOrDefault(method, ACCENT));
        titleLabel.setText(path);
        titleLabel.setToolTipText(path);

        refreshContent();
        updateDoneBtn();
        setVisible(true);
        toFront();
    }

    private void onDoneClicked() {
        if (currentProgress == null) return;
        if (currentProgress.isCompleted()) {
            currentProgress.setCompleted(false);
            saveProgress();
            updateDoneBtn();
        } else {
            int choice = JOptionPane.showConfirmDialog(this,
                "Mark this endpoint as Done?", "Confirm",
                JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
            if (choice == JOptionPane.YES_OPTION) {
                currentProgress.setCompleted(true);
                saveProgress();
                updateDoneBtn();
            }
        }
    }

    private void updateDoneBtn() {
        if (currentProgress == null) return;
        doneBtn.setText(currentProgress.isCompleted() ? "Un-done" : "Done");
    }

    // ── Content ───────────────────────────────────────────────────────────────

    private void refreshContent() {
        contentPanel.removeAll();

        List<Map.Entry<Checklist, List<ChecklistItem>>> groups = merger.mergeGrouped(repo.getAll());

        Font base = new JLabel().getFont();

        collapseActions.clear();
        activeItemIds = new LinkedHashSet<>();
        if (groups.isEmpty()) {
            JLabel lbl = new JLabel("No checklist selected.");
            lbl.setFont(base.deriveFont(13f));
            lbl.setAlignmentX(LEFT_ALIGNMENT);
            lbl.setBorder(BorderFactory.createEmptyBorder(20, 16, 0, 0));
            contentPanel.add(lbl);
        } else {
            for (Map.Entry<Checklist, List<ChecklistItem>> g : groups) {
                contentPanel.add(buildSection(g.getKey(), g.getValue(), base));
                g.getValue().forEach(i -> activeItemIds.add(i.getId()));
            }
            contentPanel.add(Box.createVerticalGlue());
        }

        groups.forEach(g -> g.getValue().forEach(i ->
            currentProgress.getOrCreateItemState(i.getId())));
        currentProgress.recalcStats(activeItemIds);
        updateProgress();
        contentPanel.revalidate();
        contentPanel.repaint();
    }

    // ── Section ───────────────────────────────────────────────────────────────

    private JPanel buildSection(Checklist cl, List<ChecklistItem> items, Font base) {
        boolean[] open = {false};

        JPanel header   = makeSectionHeader(cl, items, open, base);
        JPanel rowBlock = new JPanel();
        rowBlock.setLayout(new BoxLayout(rowBlock, BoxLayout.Y_AXIS));
        rowBlock.setOpaque(false);
        rowBlock.setAlignmentX(LEFT_ALIGNMENT);
        rowBlock.setVisible(false);

        for (ChecklistItem item : items)
            rowBlock.add(buildItemRow(item,
                currentProgress.getOrCreateItemState(item.getId()), items, header, base));

        collapseActions.add(() -> {
            if (open[0]) {
                open[0] = false;
                rowBlock.setVisible(false);
                refreshHeader(header, items, false);
            }
        });

        MouseAdapter toggle = new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                open[0] = !open[0];
                rowBlock.setVisible(open[0]);
                refreshHeader(header, items, open[0]);
            }
        };
        header.addMouseListener(toggle);
        for (Component c : header.getComponents()) c.addMouseListener(toggle);

        JPanel sec = new JPanel();
        sec.setLayout(new BoxLayout(sec, BoxLayout.Y_AXIS));
        sec.setOpaque(false);
        sec.setAlignmentX(LEFT_ALIGNMENT);
        sec.setBorder(BorderFactory.createEmptyBorder(6, 8, 0, 8));
        sec.add(header);
        sec.add(rowBlock);
        return sec;
    }

    private JPanel makeSectionHeader(Checklist cl, List<ChecklistItem> items,
                                     boolean[] open, Font base) {
        int done = doneCount(items), total = items.size();

        JLabel icon  = new JLabel(open[0] ? "▾" : "▸");
        JLabel name  = new JLabel("  " + cl.getName());
        JLabel count = new JLabel(done + " / " + total + "  ");

        icon.setFont(base.deriveFont(Font.BOLD, 14.5f));
        icon.setForeground(ACCENT);
        name.setFont(base.deriveFont(Font.BOLD, 14.5f));
        count.setFont(base.deriveFont(12f));

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        left.setOpaque(false);
        left.add(icon);
        left.add(name);

        Color headerBg = UIManager.getColor("Table.alternateRowColor");
        if (headerBg == null) headerBg = UIManager.getColor("Panel.background");

        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(headerBg);
        header.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));
        header.setAlignmentX(LEFT_ALIGNMENT);
        header.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        header.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(1, 0, 1, 0,
                UIManager.getColor("Separator.foreground")),
            BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 3, 0, 0, ACCENT),
                BorderFactory.createEmptyBorder(4, 10, 4, 10))));
        header.add(left,  BorderLayout.WEST);
        header.add(count, BorderLayout.EAST);
        header.putClientProperty("icon",  icon);
        header.putClientProperty("count", count);
        return header;
    }

    private void refreshHeader(JPanel h, List<ChecklistItem> items, boolean open) {
        JLabel icon  = (JLabel) h.getClientProperty("icon");
        JLabel count = (JLabel) h.getClientProperty("count");
        int done = doneCount(items), total = items.size();
        if (icon  != null) icon.setText(open ? "▾" : "▸");
        if (count != null) count.setText(done + " / " + total + "  ");
    }

    // ── Item row ──────────────────────────────────────────────────────────────

    private JPanel buildItemRow(ChecklistItem item, ItemState state,
                                List<ChecklistItem> siblings, JPanel sectionHeader, Font base) {
        JCheckBox cb = new JCheckBox();
        cb.setSelected(state.isDone());
        cb.setOpaque(false);
        cb.setFocusPainted(false);

        JLabel title = new JLabel(item.getTitle());
        title.setFont(base.deriveFont(14f));
        title.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        JPanel inner = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        inner.setOpaque(false);
        inner.add(cb);
        inner.add(title);

        JPanel row = new JPanel(new BorderLayout());
        row.setOpaque(true);
        row.setAlignmentX(LEFT_ALIGNMENT);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        row.setBorder(BorderFactory.createEmptyBorder(3, 12, 0, 14));
        row.add(inner, BorderLayout.CENTER);

        JPanel detail = buildDetail(item, base);
        detail.setVisible(false);

        JPanel wrapper = new JPanel();
        wrapper.setLayout(new BoxLayout(wrapper, BoxLayout.Y_AXIS));
        wrapper.setOpaque(false);
        wrapper.setAlignmentX(LEFT_ALIGNMENT);
        wrapper.add(row);
        wrapper.add(detail);

        title.addMouseListener(new MouseAdapter() {
            boolean expanded = false;
            @Override public void mouseClicked(MouseEvent e) {
                expanded = !expanded;
                detail.setVisible(expanded);
                title.setFont(base.deriveFont(expanded ? Font.BOLD : Font.PLAIN, 14f));
                wrapper.revalidate();
            }
        });

        cb.addItemListener(e -> {
            state.setDone(cb.isSelected());
            currentProgress.recalcStats(activeItemIds);
            refreshHeader(sectionHeader, siblings, true);
            updateProgress();
            saveProgress();
        });

        return wrapper;
    }

    private JPanel buildDetail(ChecklistItem item, Font base) {
        JTextArea desc = new JTextArea(item.getDescription());
        desc.setEditable(false);
        desc.setLineWrap(true);
        desc.setWrapStyleWord(true);
        desc.setOpaque(false);
        desc.setFont(base.deriveFont(13f));

        JPanel panel = new JPanel(new BorderLayout()) {
            @Override public Dimension getMaximumSize() {
                return new Dimension(Integer.MAX_VALUE, getPreferredSize().height);
            }
        };
        panel.setOpaque(false);
        panel.setAlignmentX(LEFT_ALIGNMENT);
        panel.setBorder(BorderFactory.createEmptyBorder(2, 50, 4, 14));
        panel.add(desc, BorderLayout.CENTER);
        return panel;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private static String displayPath(burp.api.montoya.http.message.requests.HttpRequest req) {
        String url = req.url();
        int schemeEnd = url.indexOf("://");
        if (schemeEnd < 0) return req.path();
        int pathStart = url.indexOf('/', schemeEnd + 3);
        return pathStart < 0 ? "/" : url.substring(pathStart);
    }

    private static String normalizePath(String path) {
        int q = path.indexOf('?');
        if (q < 0) return path;
        String base  = path.substring(0, q);
        String query = Arrays.stream(path.substring(q + 1).split("&"))
            .map(p -> { int e = p.indexOf('='); return e > 0 ? p.substring(0, e) + "={" + p.substring(0, e) + "}" : p; })
            .collect(Collectors.joining("&"));
        return base + "?" + query;
    }

    private int doneCount(List<ChecklistItem> items) {
        return (int) items.stream().filter(i -> {
            ItemState s = currentProgress.getItems().get(i.getId());
            return s != null && s.isDone();
        }).count();
    }

    private void saveProgress() {
        if (currentRequest != null && currentProgress != null)
            store.save(currentRequest, currentProgress);
    }

    private void updateProgress() {
        int total = currentProgress.getTotalItems();
        int done  = currentProgress.getDoneCount();
        progressBar.setMaximum(Math.max(total, 1));
        progressBar.setValue(done);
        progressLbl.setText(done + " / " + total);
    }

    private static Icon makePinIcon(boolean pinned) {
        return new Icon() {
            @Override
            public void paintIcon(Component c, Graphics gr, int x, int y) {
                Graphics2D g = (Graphics2D) gr.create();
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g.translate(x, y);

                Color fg = c != null ? c.getForeground() : Color.DARK_GRAY;

                // Large round head — upper-right
                g.setColor(fg);
                g.fillOval(6, 0, 12, 12);

                // Short thin needle — lower-left
                g.setStroke(new BasicStroke(1.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g.drawLine(8, 11, 2, 18);

                if (!pinned) {
                    g.setColor(new Color(210, 50, 50));
                    g.setStroke(new BasicStroke(2.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                    g.drawLine(1, 1, 18, 18);
                }
                g.dispose();
            }
            @Override public int getIconWidth()  { return 20; }
            @Override public int getIconHeight() { return 20; }
        };
    }
}
