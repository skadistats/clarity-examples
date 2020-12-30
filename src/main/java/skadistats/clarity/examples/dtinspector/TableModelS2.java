package skadistats.clarity.examples.dtinspector;

import skadistats.clarity.decoder.s2.Field;
import skadistats.clarity.decoder.s2.S2DTClass;

import javax.swing.table.AbstractTableModel;

public class TableModelS2 extends AbstractTableModel {
    private static final long serialVersionUID = 2946867068203801119L;

    private final S2DTClass dtClass;

    public TableModelS2(S2DTClass dtClass) {
        this.dtClass = dtClass;
    }

    @Override
    public int getRowCount() {
        return dtClass.getSerializer().getFieldCount();
    }

    @Override
    public int getColumnCount() {
        return 4;
    }

    @Override
    public String getColumnName(int column) {
        switch (column) {
        case 0:
            return "Name";
        case 1:
            return "Type";
        case 2:
            return "Flags";
        case 3:
            return "Encoder";
        default:
            return "";
        }
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        Field field = dtClass.getSerializer().getField(rowIndex);
        switch (columnIndex) {
        case 0:
            return field.getFieldProperties().getName();
        case 1:
            return field.getFieldProperties().getType();
        case 2:
            return field.getUnpackerProperties().getEncodeFlags();
        case 3:
            return field.getUnpackerProperties().getEncoderType();
        default:
            return "";
        }
    }

}
