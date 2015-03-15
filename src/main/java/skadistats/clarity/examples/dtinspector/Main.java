package skadistats.clarity.examples.dtinspector;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import skadistats.clarity.two.processor.runner.Context;
import skadistats.clarity.two.processor.runner.Runner;
import skadistats.clarity.two.processor.sendtables.DTClasses;
import skadistats.clarity.two.processor.sendtables.UsesDTClasses;

import javax.swing.*;
import javax.swing.tree.DefaultTreeModel;
import java.awt.*;
import java.io.FileInputStream;


@UsesDTClasses
public class Main {

    private final Logger log = LoggerFactory.getLogger(Main.class.getPackage().getClass());

    public void run(String[] args) throws Exception {
        final Context ctx = new Runner().runWith(new FileInputStream(args[0]), this);
        UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        EventQueue.invokeLater(new Runnable() {
            public void run() {
                try {
                    MainWindow window = new MainWindow();
                    window.getClassTree().setModel(new DefaultTreeModel(new TreeConstructor(ctx.getProcessor(DTClasses.class)).construct()));
                    window.getFrame().setVisible(true);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

    }

    public static void main(String[] args) throws Exception {
        new Main().run(args);
    }

}
