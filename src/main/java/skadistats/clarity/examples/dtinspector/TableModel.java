package skadistats.clarity.examples.dtinspector;

import skadistats.clarity.model.DTClass;
import skadistats.clarity.model.PropFlag;
import skadistats.clarity.model.ReceiveProp;

import javax.swing.table.AbstractTableModel;

public class TableModel extends AbstractTableModel {
    private static final long serialVersionUID = 2946867068203801119L;

    private final DTClass dtClass;

    public TableModel(DTClass dtClass) {
        this.dtClass = dtClass;
    }

    @Override
    public int getRowCount() {
        return dtClass.getReceiveProps().length;
    }

    @Override
    public int getColumnCount() {
        return 5;
    }

    @Override
    public String getColumnName(int column) {
        switch (column) {
        case 0:
            return "Name";
        case 1:
            return "Type";
        case 2:
            return "Source";
        case 3:
            return "Priority";
        case 4:
            return "Flags";
        default:
            return "";
        }
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        ReceiveProp p = dtClass.getReceiveProps()[rowIndex];
        switch (columnIndex) {
        case 0:
            return p.getVarName();
        case 1:
            return p.getSendProp().getType();
        case 2:
            return p.getSrc();
        case 3:
            return p.getSendProp().getPriority();
        case 4:
            return PropFlag.descriptionForFlags(p.getSendProp().getFlags());
        default:
            return "";
        }
    }
}