package skadistats.clarity.examples.shared;

import skadistats.clarity.examples.shared.ExampleLauncher.ExampleEntry;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.EnumMap;
import java.util.Map;

final class LauncherWindow extends JFrame {

    private static final Map<Category, String> GROUP_LABELS = new EnumMap<>(Category.class);
    static {
        GROUP_LABELS.put(Category.DOCS, "Docs");
        GROUP_LABELS.put(Category.REPRO, "Reproducers");
        GROUP_LABELS.put(Category.DEV, "Dev tools");
        GROUP_LABELS.put(Category.BENCH, "Benchmarks");
    }

    private final JTree tree;
    private final JLabel titleLabel;
    private final JLabel descriptionLabel;
    private final JLabel statusLabel;
    private final JButton runButton;
    private final JButton clearLogButton;
    private final JTextArea logArea;

    private ExampleEntry selected;
    private Thread currentRun;

    LauncherWindow(Map<String, ExampleEntry> registry, JTextArea logArea) {
        super("Clarity examples");
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);

        this.logArea = logArea;

        tree = buildTree(registry);
        tree.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));
        tree.addTreeSelectionListener(e -> onTreeSelectionChanged());

        titleLabel = new JLabel(" ");
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 16f));

        descriptionLabel = new JLabel(" ");
        descriptionLabel.setForeground(new Color(0x555555));

        runButton = new JButton("Run");
        runButton.setEnabled(false);
        runButton.addActionListener(e -> onRunClicked());

        clearLogButton = new JButton("Clear log");
        clearLogButton.addActionListener(e -> logArea.setText(""));

        statusLabel = new JLabel("Select an example to run.");
        statusLabel.setBorder(BorderFactory.createEmptyBorder(6, 10, 6, 10));

        JScrollPane treeScroll = new JScrollPane(tree);
        treeScroll.setBorder(BorderFactory.createLineBorder(new Color(0xCCCCCC)));
        treeScroll.setPreferredSize(new Dimension(300, 400));

        JPanel detailPane = buildDetailPane();

        JSplitPane topSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, treeScroll, detailPane);
        topSplit.setDividerLocation(320);
        topSplit.setBorder(null);
        topSplit.setContinuousLayout(true);
        topSplit.setResizeWeight(0.0);

        JPanel logPane = buildLogPane();

        JSplitPane mainSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT, topSplit, logPane);
        mainSplit.setDividerLocation(260);
        mainSplit.setBorder(null);
        mainSplit.setContinuousLayout(true);
        mainSplit.setResizeWeight(0.3);

        setLayout(new BorderLayout());
        add(mainSplit, BorderLayout.CENTER);
        add(statusLabel, BorderLayout.SOUTH);

        setSize(1000, 720);
        setLocationRelativeTo(null);
    }

    private JPanel buildDetailPane() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(12, 16, 12, 16));
        GridBagConstraints gc = new GridBagConstraints();
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.weightx = 1.0;
        gc.gridx = 0;
        gc.gridy = 0;
        gc.insets = new Insets(0, 0, 6, 0);
        panel.add(titleLabel, gc);

        gc.gridy++;
        gc.insets = new Insets(0, 0, 14, 0);
        panel.add(descriptionLabel, gc);

        gc.gridy++;
        gc.weighty = 1.0;
        gc.fill = GridBagConstraints.BOTH;
        panel.add(new JPanel() {{ setOpaque(false); }}, gc);

        gc.gridy++;
        gc.weighty = 0;
        gc.fill = GridBagConstraints.HORIZONTAL;
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        buttons.add(clearLogButton);
        buttons.add(runButton);
        panel.add(buttons, gc);
        return panel;
    }

    private JPanel buildLogPane() {
        logArea.setEditable(false);
        logArea.setLineWrap(false);
        logArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        logArea.setBackground(new Color(0x1E1E1E));
        logArea.setForeground(new Color(0xDCDCDC));
        logArea.setCaretColor(new Color(0xDCDCDC));
        logArea.setBorder(BorderFactory.createEmptyBorder(6, 8, 6, 8));
        JScrollPane scroll = new JScrollPane(logArea);
        scroll.setBorder(BorderFactory.createLineBorder(new Color(0xCCCCCC)));

        JPanel wrap = new JPanel(new BorderLayout());
        wrap.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));
        JLabel header = new JLabel("Output");
        header.setBorder(BorderFactory.createEmptyBorder(6, 0, 6, 0));
        header.setFont(header.getFont().deriveFont(Font.BOLD));
        wrap.add(header, BorderLayout.NORTH);
        wrap.add(scroll, BorderLayout.CENTER);
        return wrap;
    }

    private JTree buildTree(Map<String, ExampleEntry> registry) {
        DefaultMutableTreeNode root = new DefaultMutableTreeNode("Examples");
        EnumMap<Category, DefaultMutableTreeNode> groups = new EnumMap<>(Category.class);
        for (Category c : Category.values()) {
            DefaultMutableTreeNode node = new DefaultMutableTreeNode(GROUP_LABELS.get(c));
            groups.put(c, node);
            root.add(node);
        }
        registry.values().stream()
                .sorted((a, b) -> a.name().compareTo(b.name()))
                .forEach(e -> groups.get(e.category()).add(new DefaultMutableTreeNode(e)));

        JTree t = new JTree(new DefaultTreeModel(root));
        t.setRootVisible(false);
        t.setShowsRootHandles(true);
        t.setRowHeight(22);
        t.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
        t.setCellRenderer(new DefaultTreeCellRenderer() {
            @Override
            public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel,
                                                          boolean expanded, boolean leaf,
                                                          int row, boolean hasFocus) {
                super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);
                Object uo = ((DefaultMutableTreeNode) value).getUserObject();
                if (uo instanceof String s) {
                    setFont(getFont().deriveFont(Font.BOLD));
                    setText(s);
                } else if (uo instanceof ExampleEntry e) {
                    setFont(getFont().deriveFont(Font.PLAIN));
                    setText(e.name());
                }
                return this;
            }
        });
        for (int i = 0; i < t.getRowCount(); i++) {
            t.expandRow(i);
        }
        return t;
    }

    private void onTreeSelectionChanged() {
        TreePath path = tree.getSelectionPath();
        ExampleEntry entry = null;
        if (path != null) {
            Object last = ((DefaultMutableTreeNode) path.getLastPathComponent()).getUserObject();
            if (last instanceof ExampleEntry e) {
                entry = e;
            }
        }
        this.selected = entry;
        if (entry != null) {
            titleLabel.setText(entry.name());
            descriptionLabel.setText("<html><div style='width: 500px'>"
                    + htmlEscape(entry.description()) + "</div></html>");
        } else {
            titleLabel.setText(" ");
            descriptionLabel.setText(" ");
        }
        runButton.setEnabled(entry != null && currentRun == null);
    }

    private void onRunClicked() {
        if (selected == null || currentRun != null) return;
        ExampleEntry entry = selected;
        System.out.println();
        System.out.println("======== Running " + entry.name() + " ========");
        statusLabel.setText("Running " + entry.name() + "…");
        runButton.setEnabled(false);
        Thread worker = ExampleLauncher.runOnWorker(entry, new String[0]);
        this.currentRun = worker;
        Thread watcher = new Thread(() -> {
            try {
                worker.join();
            } catch (InterruptedException ignored) {
            }
            SwingUtilities.invokeLater(() -> {
                currentRun = null;
                System.out.println("======== " + entry.name() + " finished ========");
                statusLabel.setText(entry.name() + " finished.");
                runButton.setEnabled(selected != null);
            });
        }, "watch-" + entry.name());
        watcher.setDaemon(true);
        watcher.start();
    }

    private static String htmlEscape(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
