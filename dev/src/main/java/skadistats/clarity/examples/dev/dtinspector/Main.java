package skadistats.clarity.examples.dev.dtinspector;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import skadistats.clarity.processor.runner.Context;
import skadistats.clarity.processor.runner.SimpleRunner;
import skadistats.clarity.processor.sendtables.DTClasses;
import skadistats.clarity.processor.sendtables.UsesDTClasses;
import skadistats.clarity.source.MappedFileSource;

import javax.swing.*;
import javax.swing.tree.DefaultTreeModel;
import java.awt.*;
import skadistats.clarity.examples.shared.ReplayChooser;


@UsesDTClasses
public class Main {

    private final Logger log = LoggerFactory.getLogger(Main.class.getPackage().getClass());

    public void run(String[] args) throws Exception {
        final Context ctx;
        String replay = ReplayChooser.choose(args);
        if (replay == null) return;
        try (MappedFileSource source = new MappedFileSource(replay)) {
            ctx = new SimpleRunner(source).runWith(this).getContext();
        }
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
