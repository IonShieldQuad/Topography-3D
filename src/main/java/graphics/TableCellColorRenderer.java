package graphics;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;

public class TableCellColorRenderer extends DefaultTableCellRenderer {
    
    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
        int rgb;
        try {
            rgb = (Integer)value;
        }
        catch (ClassCastException e) {
            return c;
        }
        
        Color color = new Color(rgb);
        
        c.setBackground(color);
        c.setForeground(color);
        
        return c;
    }
    
}
