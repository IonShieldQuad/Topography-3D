package core;

import graphics.*;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableColumnModel;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.*;
import java.util.List;
import java.util.function.BiFunction;

public class MainWindow {
    private JPanel rootPanel;
    private JTextArea log;
    private JTextField lowerX;
    private JTextField upperX;
    private JButton displayButton;
    private ContourPlotDisplay3D graph;
    private JComboBox modeSel;
    private JTextField camOffsetField;
    private JCheckBox xCheckBox;
    private JCheckBox yCheckBox;
    private JCheckBox zCheckBox;
    private JTextField offXField;
    private JTextField offYField;
    private JTextField offZField;
    private JTextField rotXField;
    private JTextField rotYField;
    private JTextField rotZField;
    private JTextField scaleXField;
    private JTextField scaleYField;
    private JTextField scaleZField;
    private JComboBox angleSel;
    private JTextField imageNameField;
    private JButton selectImageButton;
    private JCheckBox showOutline;
    private JTable table1;
    private JTextField rgbTextField;
    private JButton addColorButton;
    private JButton removeColorButton;
    private JCheckBox showContoursCheckBox;
    private JTextField contoursField;
    private JTextField resolutionField;
    private JTextField heightField;
    private JComboBox colorModeSel;
    private JTextField colorWeightField;
    private JTextField gammaField;
    private JCheckBox brightContoursCheckBox;
    private JCheckBox useMipmapsCheckBox;
    private JTextField param1Field;
    private JTextField param2Field;
    private JTextField param3Field;
    private JCheckBox showBaseTextureCheckBox;
    
    
    private static final String TITLE = "Topography-3D";
    private List<Model> models = new ArrayList<>();
    Mipmapper image;
    Map<Integer, Double> tableData = new HashMap<>();
    ColorMapper colorMapper = new ColorMapper();
    ColorMapper.Mode[] modes = new ColorMapper.Mode[]{ColorMapper.Mode.RGB, ColorMapper.Mode.HSV, ColorMapper.Mode.HSL, ColorMapper.Mode.CIE76, ColorMapper.Mode.CIE94, ColorMapper.Mode.CIEDE2000};
    
    private MainWindow() {
        initComponents();
        models.add(Model.cube(100));
        models.add(Model.cube2(100));
    }
    
    private void initComponents() {
        displayButton.addActionListener(e -> display());
        selectImageButton.addActionListener(e -> selectImage());
    
        DefaultTableModel model = new DefaultTableModel(0, 3) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        table1.setAutoCreateColumnsFromModel(false);
        table1.setModel(model);
        
        TableColumn column0 = new TableColumn(0, 50, null, null);
        column0.setIdentifier(0);
        column0.setHeaderValue("Hex value");
        
        TableColumn column1 = new TableColumn(1, 20, new TableCellColorRenderer(), null);
        column1.setIdentifier(1);
        column1.setHeaderValue("Color");
        
        TableColumn column2 = new TableColumn(2, 20, null, null);
        column2.setIdentifier(2);
        column2.setHeaderValue("Height");
        
        table1.addColumn(column0);
        table1.addColumn(column1);
        table1.addColumn(column2);
        
        addColorButton.addActionListener(e -> {
            try {
                String input = rgbTextField.getText().trim();
                if (input.length() != 6) {
                    throw new NumberFormatException();
                }
                int val = Integer.parseInt(input, 16);
                double height = Double.parseDouble(heightField.getText());
                tableData.put(val, height);
                updateTable();
            } catch (NumberFormatException ex) {
                log.append("\nColor format error");
            }
        });
        removeColorButton.addActionListener(e -> {
            int row = table1.getSelectedRow();
            if (tableData.size() > 0 && row >= 0) {
                tableData.remove(Integer.parseInt(table1.getModel().getValueAt(row, 0).toString(), 16));
                updateTable();
            }
        });
    }
    
    private void display() {
        try {
            graph.getModels().clear();
            
            double offX = Double.parseDouble(offXField.getText());
            double offY = Double.parseDouble(offYField.getText());
            double offZ = Double.parseDouble(offZField.getText());
            
            double rotX = Double.parseDouble(rotXField.getText());
            double rotY = Double.parseDouble(rotYField.getText());
            double rotZ = Double.parseDouble(rotZField.getText());
            
            double scaleX = Double.parseDouble(scaleXField.getText());
            double scaleY = Double.parseDouble(scaleYField.getText());
            double scaleZ = Double.parseDouble(scaleZField.getText());
            Transform3D transform = new Transform3D(new Point3D(offX, offY, offZ), new Point3D(Math.toRadians(rotX), Math.toRadians(rotY), Math.toRadians(rotZ)), new Point3D(scaleX, scaleY, scaleZ));
            
            int resolution = Integer.parseInt(resolutionField.getText());
            int contours = Integer.parseInt(contoursField.getText());
            double distancePower = Double.parseDouble(colorWeightField.getText());
            double gamma = Double.parseDouble(gammaField.getText());
            
            double m1 = Double.parseDouble(param1Field.getText());
            double m2 = Double.parseDouble(param2Field.getText());
            double m3 = Double.parseDouble(param3Field.getText());
            
            if (image == null) {
                log.append("\nNo image selected");
                return;
            }
            graph.setImage(image);
            //graph.getModels().put(models.get(modelSel.getSelectedIndex()), transform);
            //BiFunction<Double, Double, Double> f = (x, z) -> -((1/5.0) * Math.sin(x) * Math.cos(z) - (3/2.0) * Math.cos(7 * (Math.pow(x - Math.PI, 2) + Math.pow(z - Math.PI, 2))/4) * Math.exp(-(Math.pow(x - Math.PI, 2) + Math.pow(z - Math.PI, 2))));
            colorMapper.setMode(modes[colorModeSel.getSelectedIndex()]);
            colorMapper.setDistancePower(distancePower);
            colorMapper.setGamma(gamma);
            colorMapper.setM1(m1);
            colorMapper.setM2(m2);
            colorMapper.setM3(m3);
            colorMapper.setUseMipmaps(useMipmapsCheckBox.isSelected());
    
            Map<Color, Double> colorData = new HashMap<>();
            for (int i : tableData.keySet()) {
                colorData.put(new Color(i), tableData.get(i));
            }
            
            BiFunction<Double, Double, Double> f = colorMapper.mapColors(image, resolution, colorData);
            
            ContourPlotDisplay3D.FunctionCache cache = new ContourPlotDisplay3D.FunctionCache((x, y) -> f.apply(x, 1 - y), resolution, 0, 1, 0, 1);
            
            graph.getModels().put(cache.getModel(), transform);
            graph.setCache(cache);
            graph.setDrawContours(showContoursCheckBox.isSelected());
            graph.setContours(contours);
            graph.setContourColor(brightContoursCheckBox.isSelected() ? Color.WHITE : (showBaseTextureCheckBox.isSelected() ? Color.BLACK : Color.CYAN));
            graph.setUseMipmap(useMipmapsCheckBox.isSelected());
            graph.setDrawFaces(showBaseTextureCheckBox.isSelected());
            
            graph.setParallelMode(modeSel.getSelectedIndex() == 0);
            graph.setShowOutline(showOutline.isSelected());
            
            if (modeSel.getSelectedIndex() != 0) {
                graph.setAngleA(0);
                graph.setFactorD(Double.parseDouble(camOffsetField.getText()));
                graph.setWarpX(xCheckBox.isSelected());
                graph.setWarpY(yCheckBox.isSelected());
                graph.setWarpZ(zCheckBox.isSelected());
            }
            else {
                switch (angleSel.getSelectedIndex()) {
                    case 0:
                        graph.setAngleA(Math.toRadians(30));
                        graph.setFactorL(0.5);
                        break;
                    case 1:
                        graph.setAngleA(Math.toRadians(45));
                        graph.setFactorL(1);
                        break;
                    default:
                        graph.setAngleA(Math.toRadians(0));
                        graph.setFactorL(0);
                }
            }
            
            updateGraph();
        }
        catch (NumberFormatException e) {
            log.append("Invalid format!\n");
        }
    }
    
    
    
    private void updateGraph() {
        graph.repaint();
    }
    
    private void selectImage() {
        JFileChooser chooser = new JFileChooser();
        FileNameExtensionFilter filter = new FileNameExtensionFilter(
                "JPG & GIF & PNG Images", "jpg", "gif", "png");
        chooser.setFileFilter(filter);
        int returnVal = chooser.showOpenDialog(rootPanel);
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            try {
                BufferedImage img = ImageIO.read(chooser.getSelectedFile());
                imageNameField.setText(chooser.getSelectedFile().getName());
                image = new Mipmapper(img);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    
    public void addRow() {
        DefaultTableModel model = (DefaultTableModel)table1.getModel();
        model.addRow(new Object[]{"", 0});
        updateTable();
    }
    
    public void updateTable() {
        DefaultTableModel model = (DefaultTableModel)table1.getModel();
        for (int i = model.getRowCount() - 1; i >= 0; i--) {
            model.removeRow(i);
        }
        Set<Integer> keys = tableData.keySet();
        for (Integer key : keys) {
            StringBuilder str = new StringBuilder(Integer.toString(key, 16));
            while (str.length() < 6) {
                str.insert(0, "0");
            }
            model.addRow(new Object[]{str.toString(), key, tableData.get(key)});
        }
    }
    
    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException e) {
            e.printStackTrace();
        }
        JFrame frame = new JFrame(TITLE);
        MainWindow gui = new MainWindow();
        frame.setContentPane(gui.rootPanel);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.pack();
        frame.setVisible(true);
    }
    
}
