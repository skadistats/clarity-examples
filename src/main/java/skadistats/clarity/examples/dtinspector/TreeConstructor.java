package skadistats.clarity.examples.dtinspector;

import skadistats.clarity.io.s1.S1DTClass;
import skadistats.clarity.model.DTClass;
import skadistats.clarity.processor.sendtables.DTClasses;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeNode;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

public class TreeConstructor {

    public static class TreePayload {
        private final DTClass dtClass;

        public TreePayload(DTClass dtClass) {
            this.dtClass = dtClass;
        }

        public DTClass getDtClass() {
            return dtClass;
        }

        public String toString() {
            return dtClass == null ? "" : dtClass.getDtName();
        }

    }

    private final Map<DTClass, Set<DTClass>> tree = new HashMap<DTClass, Set<DTClass>>();

    private static final Comparator<DTClass> COMPARATOR = new Comparator<DTClass>() {
        @Override
        public int compare(DTClass o1, DTClass o2) {
            return o1.getDtName().compareTo(o2.getDtName());
        }
    };

    public TreeConstructor(DTClasses classes) {
        Iterator<DTClass> dtClasses = classes.iterator();
        while(dtClasses.hasNext()) {
            DTClass c = dtClasses.next();
            DTClass s = null;
            if (c instanceof S1DTClass) {
                s = ((S1DTClass)c).getSuperClass();
            }
            Set<DTClass> forSuper = tree.get(s);
            if (forSuper == null) {
                forSuper = new TreeSet<>(COMPARATOR);
                tree.put(s, forSuper);
            }
            forSuper.add(c);
        }
    }

    private DefaultMutableTreeNode constructInternal(DTClass superClass) {
        DefaultMutableTreeNode node = new DefaultMutableTreeNode(new TreePayload(superClass));
        Set<DTClass> children = tree.get(superClass);
        if (children != null) {
            for (DTClass c : children) {
                node.add(constructInternal(c));
            }
        }
        return node;
    }

    public TreeNode construct() {
        return constructInternal(null);
    }
}
