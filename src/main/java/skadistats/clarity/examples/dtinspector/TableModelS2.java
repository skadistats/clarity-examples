package skadistats.clarity.examples.dtinspector;

import skadistats.clarity.io.s2.Field;
import skadistats.clarity.io.s2.S2DTClass;

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
            return "Idx";
        case 1:
            return "Name";
        case 2:
            return "Type";
        case 3:
            return "Flags";
        case 4:
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
            return rowIndex;
        case 1:
            return dtClass.getSerializer().getFieldName(rowIndex);
        case 2:
            return field.getType();
        case 3:
            return field.getDecoderProperties().getEncodeFlags();
        case 4:
            return field.getDecoderProperties().getEncoderType();
        default:
            return "";
        }
    }

}
