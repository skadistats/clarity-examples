package skadistats.clarity.examples.dtinspector;

import skadistats.clarity.examples.dtinspector.TreeConstructor.TreePayload;
import skadistats.clarity.io.s1.S1DTClass;
import skadistats.clarity.io.s2.S2DTClass;
import skadistats.clarity.model.DTClass;

import javax.swing.*;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import java.awt.*;

public class MainWindow {

    private JFrame frmNetpropertyViewer;
    private JTree classTree;
    private JScrollPane scrollPaneLeft;
    private JScrollPane scrollPaneRight;
    private JTable table;

    public MainWindow() {
        initialize();
    }

    private void initialize() {
        frmNetpropertyViewer = new JFrame();
        frmNetpropertyViewer.setMinimumSize(new Dimension(700, 300));
        frmNetpropertyViewer.setIconImage(Toolkit.getDefaultToolkit().getImage(MainWindow.class.getResource("/images/dota_2_icon.png")));
        frmNetpropertyViewer.setTitle("DT inspector");
        frmNetpropertyViewer.setBounds(100, 100, 800, 450);
        frmNetpropertyViewer.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        JSplitPane splitPane = new JSplitPane();
        frmNetpropertyViewer.getContentPane().add(splitPane, BorderLayout.CENTER);

        scrollPaneLeft = new JScrollPane();
        splitPane.setLeftComponent(scrollPaneLeft);

        classTree = new JTree();
        classTree.setRootVisible(false);
        classTree.addTreeSelectionListener(new TreeSelectionListener() {
            public void valueChanged(TreeSelectionEvent e) {
                DefaultMutableTreeNode node = (DefaultMutableTreeNode) e.getPath().getLastPathComponent();
                TreePayload p = (TreePayload) node.getUserObject();
                DTClass cls = p.getDtClass();
                if (cls instanceof S1DTClass) {
                    table.setModel(new TableModelS1((S1DTClass) cls));
                } else {
                    table.setModel(new TableModelS2((S2DTClass) cls));
                }
            }
        });
        scrollPaneLeft.setViewportView(classTree);

        scrollPaneRight = new JScrollPane();
        splitPane.setRightComponent(scrollPaneRight);

        table = new JTable();
        table.setColumnSelectionAllowed(true);
        table.setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION);
        table.setAutoCreateRowSorter(true);
        scrollPaneRight.setViewportView(table);
        splitPane.setDividerLocation(200);
    }

    protected JTree getClassTree() {
        return classTree;
    }

    public JFrame getFrame() {
        return frmNetpropertyViewer;
    }
}
