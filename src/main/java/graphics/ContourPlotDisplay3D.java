package graphics;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.*;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;

public class ContourPlotDisplay3D extends JPanel {
    private static final Color GRID_COLOR = Color.WHITE;
    private static final Color MODEL_COLOR = new Color(0xff5599);
    
    private static final Model AXIS = Model.axis(10000);
    
    private Map<Model, Transform3D> models = new HashMap<>();
    
    private double scale = 1.0;
    private boolean parallelMode = false;
    private double angleA = 0.0;
    private double factorL = 0.5;
    private double factorD = 10;
    
    private boolean warpX = true;
    private boolean warpY = true;
    private boolean warpZ = true;
    
    //All angles are in radians!
    
    private TextureUtils.Filtering filtering = TextureUtils.Filtering.ANISOTROPIC;
    private int textureResolution = 512;
    private FunctionCache cache;
    
    private Mipmapper mipmapper;
    
    private boolean showOutline = false;
    private boolean useMipmap = true;
    private double mipmapBiasU = 0;
    private double mipmapBiasV = 0;
    
    private boolean drawContours = true;
    private int contours = 20;
    private double contourOffset = 0;
    private Color contourColor = Color.BLACK;
    
    private BufferedImage image;
    
    public ContourPlotDisplay3D() {
        super();
    }
    
    
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        
        List<Double> zBuffer = new ArrayList<>();
        for (int i = 0; i < getWidth() * getHeight(); i++) {
            zBuffer.add(Double.POSITIVE_INFINITY);
        }
        
        drawModel(g, AXIS, new Transform3D(), GRID_COLOR, false, zBuffer);
        for (Model m : models.keySet()) {
            Transform3D t = models.get(m);
            drawModel(g, m, t, MODEL_COLOR, true, zBuffer);
        }
    }
    
    private void drawModel(Graphics g, Model model, Transform3D transform, Color color, boolean warp, List<Double> zBuffer) {
        BufferedImage texture;
        
        if (cache == null || !cache.isValid()) {
            texture = new BufferedImage(20, 20, BufferedImage.TYPE_INT_RGB);
            WritableRaster r = texture.getRaster();
            int[] pixelVector = new int[20 * 20];
            for (int i = 0; i < pixelVector.length; i++) {
                pixelVector[i] = interpolate(interpolate(Color.BLUE, Color.RED, (i % 20) / 20.0), interpolate(Color.CYAN, Color.ORANGE, (i % 20) / 20.0), (i - i % 20) / 400.0).getRGB();
                r.setDataElements(0, 0, 20, 20, pixelVector);
            }
        }
        else {
            if (image == null) {
                TextureGenerator texGen = new TextureGenerator(cache, textureResolution, textureResolution);
                texture = texGen.generateTexture();
            }
            else {
                texture = image;
            }
        }
        mipmapper = new Mipmapper(texture);
        
        //Draw polygons
        for (Polygon polygon : model.getPolygons()) {
            try {
                Point3D a = polygon.a.copy();
                Point3D b = polygon.b.copy();
                Point3D c = polygon.c.copy();
                
                Matrix am = a.toMatrix();
                Matrix bm = b.toMatrix();
                Matrix cm = c.toMatrix();
                
                Matrix scaleMatrix = Matrix.scaleMatrix3D(transform.scale.getX(), transform.scale.getY(), transform.scale.getZ());
                Matrix rotationMatrix = Matrix.rotationMatrix3D(transform.rotation.getX(), transform.rotation.getY(), transform.rotation.getZ());
                Matrix offsetMatrix = Matrix.offsetMatrix3D(transform.offset.getX(), transform.offset.getY(), transform.offset.getZ());
                
                am = am.multiply(scaleMatrix);
                bm = bm.multiply(scaleMatrix);
                cm = cm.multiply(scaleMatrix);
    
                am = am.multiply(rotationMatrix);
                bm = bm.multiply(rotationMatrix);
                cm = cm.multiply(rotationMatrix);
    
                am = am.multiply(offsetMatrix);
                bm = bm.multiply(offsetMatrix);
                cm = cm.multiply(offsetMatrix);
                
                a.setX(am.get(0, 0));
                a.setY(am.get(1, 0));
                a.setZ(am.get(2, 0));
    
                b.setX(bm.get(0, 0));
                b.setY(bm.get(1, 0));
                b.setZ(bm.get(2, 0));
    
                c.setX(cm.get(0, 0));
                c.setY(cm.get(1, 0));
                c.setZ(cm.get(2, 0));
                
                if (isParallelMode()) {
                    double l = getFactorL();
                    double angle = getAngleA();
                    double x;
                    double y;
                    
                    x = a.getX() + a.getZ() * (l * Math.cos(angle));
                    y = a.getY() + a.getZ() * (l * Math.sin(angle));
                    
                    a.setX(x);
                    a.setY(y);
    
                    x = b.getX() + b.getZ() * (l * Math.cos(angle));
                    y = b.getY() + b.getZ() * (l * Math.sin(angle));
    
                    b.setX(x);
                    b.setY(y);
    
                    x = c.getX() + c.getZ() * (l * Math.cos(angle));
                    y = c.getY() + c.getZ() * (l * Math.sin(angle));
    
                    c.setX(x);
                    c.setY(y);
                }
                else {
                    if (warp) {
                        double d = getFactorD();
                        double x;
                        double y;
    
                        x = a.getX() / (1 + (Math.abs(a.getX()) * (isWarpX() ? 1 : 0) / d) + (Math.abs(a.getY()) * (isWarpY() ? 1 : 0) / d) + ((a.getZ()) * (isWarpZ() ? 1 : 0) / d));
                        y = a.getY() / (1 + (Math.abs(a.getX()) * (isWarpX() ? 1 : 0) / d) + (Math.abs(a.getY()) * (isWarpY() ? 1 : 0) / d) + ((a.getZ()) * (isWarpZ() ? 1 : 0) / d));
    
                        a.setX(x);
                        a.setY(y);
    
                        x = b.getX() / (1 + (Math.abs(b.getX()) * (isWarpX() ? 1 : 0) / d) + (Math.abs(b.getY()) * (isWarpY() ? 1 : 0) / d) + ((b.getZ()) * (isWarpZ() ? 1 : 0) / d));
                        y = b.getY() / (1 + (Math.abs(b.getX()) * (isWarpX() ? 1 : 0) / d) + (Math.abs(b.getY()) * (isWarpY() ? 1 : 0) / d) + ((b.getZ()) * (isWarpZ() ? 1 : 0) / d));
    
                        b.setX(x);
                        b.setY(y);
    
                        x = c.getX() / (1 + (Math.abs(c.getX()) * (isWarpX() ? 1 : 0) / d) + (Math.abs(c.getY()) * (isWarpY() ? 1 : 0) / d) + ((c.getZ()) * (isWarpZ() ? 1 : 0) / d));
                        y = c.getY() / (1 + (Math.abs(c.getX()) * (isWarpX() ? 1 : 0) / d) + (Math.abs(c.getY()) * (isWarpY() ? 1 : 0) / d) + ((c.getZ()) * (isWarpZ() ? 1 : 0) / d));
    
                        c.setX(x);
                        c.setY(y);
                    }
                }
                
                Polygon transformedPolygon = new Polygon(a, b, c);
                
                drawPolygon(g, transformedPolygon, polygon, mipmapper, zBuffer);
                
                if (showOutline) {
                    g.setColor(color);
                    g.drawLine(normX(a.getX()), normY(a.getY()), normX(b.getX()), normY(b.getY()));
                    g.drawLine(normX(b.getX()), normY(b.getY()), normX(c.getX()), normY(c.getY()));
                    g.drawLine(normX(c.getX()), normY(c.getY()), normX(a.getX()), normY(a.getY()));
                }
                
                //g.setColor(color);
                //g.drawLine(normX(a.getX()), normY(a.getY()), normX(b.getX()), normY(b.getY()));
            }
            catch (ArrayIndexOutOfBoundsException e) {
                e.printStackTrace();
            }
        }
    
        //Draw edges
        /*for (Pair<Integer, Integer> edge : model.getEdges()) {
            try {
                Point3D a = model.getVertices().get(edge.a).copy();
                Point3D b = model.getVertices().get(edge.b).copy();
            
                Matrix am = a.toMatrix();
                Matrix bm = b.toMatrix();
            
                am = am.multiply(Matrix.scaleMatrix3D(transform.scale.getX(), transform.scale.getY(), transform.scale.getZ()));
                bm = bm.multiply(Matrix.scaleMatrix3D(transform.scale.getX(), transform.scale.getY(), transform.scale.getZ()));
            
                am = am.multiply(Matrix.rotationMatrix3D(transform.rotation.getX(), transform.rotation.getY(), transform.rotation.getZ()));
                bm = bm.multiply(Matrix.rotationMatrix3D(transform.rotation.getX(), transform.rotation.getY(), transform.rotation.getZ()));
            
                am = am.multiply(Matrix.offsetMatrix3D(transform.offset.getX(), transform.offset.getY(), transform.offset.getZ()));
                bm = bm.multiply(Matrix.offsetMatrix3D(transform.offset.getX(), transform.offset.getY(), transform.offset.getZ()));
            
                a.setX(am.get(0, 0));
                a.setY(am.get(1, 0));
                a.setZ(am.get(2, 0));
            
                b.setX(bm.get(0, 0));
                b.setY(bm.get(1, 0));
                b.setZ(bm.get(2, 0));
            
            
                if (isParallelMode()) {
                    double l = getFactorL();
                    double angle = getAngleA();
                    double x;
                    double y;
                
                    x = a.getX() + a.getZ() * (l * Math.cos(angle));
                    y = a.getY() + a.getZ() * (l * Math.sin(angle));
                
                    a.setX(x);
                    a.setY(y);
                
                    x = b.getX() + b.getZ() * (l * Math.cos(angle));
                    y = b.getY() + b.getZ() * (l * Math.sin(angle));
                
                    b.setX(x);
                    b.setY(y);
                }
                else {
                    if (warp) {
                        double d = getFactorD();
                        double x;
                        double y;
                    
                        x = a.getX() / (1 + (Math.abs(a.getX()) * (isWarpX() ? 1 : 0) / d) + (Math.abs(a.getY()) * (isWarpY() ? 1 : 0) / d) + ((a.getZ()) * (isWarpZ() ? 1 : 0) / d));
                        y = a.getY() / (1 + (Math.abs(a.getX()) * (isWarpX() ? 1 : 0) / d) + (Math.abs(a.getY()) * (isWarpY() ? 1 : 0) / d) + ((a.getZ()) * (isWarpZ() ? 1 : 0) / d));
                    
                        a.setX(x);
                        a.setY(y);
                    
                        x = b.getX() / (1 + (Math.abs(b.getX()) * (isWarpX() ? 1 : 0) / d) + (Math.abs(b.getY()) * (isWarpY() ? 1 : 0) / d) + ((b.getZ()) * (isWarpZ() ? 1 : 0) / d));
                        y = b.getY() / (1 + (Math.abs(b.getX()) * (isWarpX() ? 1 : 0) / d) + (Math.abs(b.getY()) * (isWarpY() ? 1 : 0) / d) + ((b.getZ()) * (isWarpZ() ? 1 : 0) / d));
                    
                        b.setX(x);
                        b.setY(y);
                    }
                }
            
                g.setColor(color);
                g.drawLine(normX(a.getX()), normY(a.getY()), normX(b.getX()), normY(b.getY()));
            }
            catch (ArrayIndexOutOfBoundsException e) {
                e.printStackTrace();
            }
        }*/
    }
    
    private void drawPolygon(Graphics g, Polygon p, Mipmapper mm, List<Double> zBuffer) {
        drawPolygon(g, p, p, mm, zBuffer);
    }
    
    private void drawPolygon(Graphics g, Polygon p, Polygon orig, Mipmapper mm, List<Double> zBuffer) {
        //System.out.println("Polygon: " + p.a + p.b + p.c);
        Polygon proj = new Polygon(p.a.copy(), p.b.copy(), p.c.copy());
        proj.a.setZ(0);
        proj.b.setZ(0);
        proj.c.setZ(0);
        
        int minX = (int)Math.round(Math.min(Math.min(p.a.getX(), p.b.getX()), p.c.getX()));
        int minY = (int)Math.round(Math.min(Math.min(p.a.getY(), p.b.getY()), p.c.getY()));
        int maxX = (int)Math.round(Math.max(Math.max(p.a.getX(), p.b.getX()), p.c.getX()));
        int maxY = (int)Math.round(Math.max(Math.max(p.a.getY(), p.b.getY()), p.c.getY()));
        
        List<List<Boolean>>filteredContourData = new ArrayList<>(maxX - minX + 1);
        //Gather contour data
        if (drawContours && cache != null) {
            //Initialize array
            for (int i = 0; i < maxX - minX + 1; i++) {
                filteredContourData.add(new ArrayList<>(maxY - minY + 1));
                for (int j = 0; j < maxY - minY + 1; j++) {
                    filteredContourData.get(i).add(false);
                }
            }
            //For each contour level
            for (int k = 0; k < contours; k++) {
                List<List<Boolean>> contourData = new ArrayList<>(maxX - minX + 1);
                for (int i = 0; i < maxX - minX + 1; i++) {
                    contourData.add(new ArrayList<>(maxY - minY + 1));
                    for (int j = 0; j < maxY - minY + 1; j++) {
                        contourData.get(i).add(null);
                    }
                }
                //Map of all points higher/lower then the target
                int finalK = k;
                rasterizeTriangle(p, (x, y) -> {
                    Point3D bary = p.barycentric(new Point3D(x, y, 0));
                    double val = orig.a.getY() * bary.getX() + orig.b.getY() * bary.getY() + orig.c.getY() * bary.getZ();
                    //System.out.println(cache.getContourCutoffValueNormalized(i, contours, contourOffset) + "; " + val);
                    contourData.get(x - minX).set(y - minY, val >= cache.getContourCutoffValueNormalized(finalK, contours, contourOffset));
                });
    
                //Edge detection filter
                for (int i = 0; i < maxX - minX + 1; i++) {
                    for (int j = 0; j < maxY - minY + 1; j++) {
                        
                        Boolean tc = contourData.get(Math.max(Math.min(i, maxX - minX), 0)).get(Math.max(Math.min(j + 1, maxY - minY), 0));
            
                        Boolean cl = contourData.get(Math.max(Math.min(i - 1, maxX - minX), 0)).get(Math.max(Math.min(j, maxY - minY), 0));
                        Boolean cc = contourData.get(Math.max(Math.min(i, maxX - minX), 0)).get(Math.max(Math.min(j, maxY - minY), 0));
                        Boolean cr = contourData.get(Math.max(Math.min(i + 1, maxX - minX), 0)).get(Math.max(Math.min(j, maxY - minY), 0));
                        
                        Boolean bc = contourData.get(Math.max(Math.min(i, maxX - minX), 0)).get(Math.max(Math.min(j - 1, maxY - minY), 0));
            
                        boolean res = (cc != null && cc) && ((tc != null && !tc) || (cl != null && !cl) || (cr != null && !cr) || (bc != null && !bc));
                        if (res) {
                            filteredContourData.get(i).set(j, true);
                        }
                    }
                }
            }
        }
        
        
        //System.out.println("minX: " + minX + ", minY: " + minY + ", maxX: " + maxX + ", maxY: " + maxY);
        //g.setColor(Color.CYAN);
        //rasterizeTriangle(p, (x, y) -> g.drawRect(normX(x), normY(y), 0, 0));
        //for (int i = minY; i < maxY; i++) {
            //for (int j = minX; j < maxX; j++) {
        rasterizeTriangle(p, (j, i) -> {
            
            Point3D point = new Point3D(j, i, 0);
    
            Point3D barycentric = proj.barycentric(point);
            double z = barycentric.getX() * p.a.getZ() + barycentric.getY() * p.b.getZ() + barycentric.getZ() * p.c.getZ();
            if (normX(j) >= 0 && normX(j) < getWidth() && normY(i) >= 0 && normY(i) < getHeight() && z < zBuffer.get(normY(i) * getWidth() + normX(j))) {
                zBuffer.set(normY(i) * getWidth() + normX(j), z);
            } else {
                return;
            }
    
            PointDouble uv = proj.uv(point);
            //System.out.println("UV: " + uv.getX() + ":" +uv.getY());
            //Offset UVs
            PointDouble uvl = proj.uv(new Point3D(j - 1, i, 0));
            PointDouble uvr = proj.uv(new Point3D(j + 1, i, 0));
            PointDouble uvt = proj.uv(new Point3D(j, i - 1, 0));
            PointDouble uvb = proj.uv(new Point3D(j, i + 1, 0));
    
            //UV derivative
            double dudx = ((Math.abs(uv.getX() - uvl.getX()) + Math.abs(uv.getX() - uvr.getX()) + Math.abs(uv.getX() - uvt.getX()) + Math.abs(uv.getX() - uvb.getX())) / 2) * mm.getMipmap(0, 0).getWidth();
            double dvdy = ((Math.abs(uv.getY() - uvl.getY()) + Math.abs(uv.getY() - uvr.getY()) + Math.abs(uv.getY() - uvt.getY()) + Math.abs(uv.getY() - uvb.getY())) / 2) * mm.getMipmap(0, 0).getHeight();
            double mmU = Math.max(0, (Math.log(dudx) / Math.log(2)) + mipmapBiasU);
            double mmV = Math.max(0, (Math.log(dvdy) / Math.log(2)) + mipmapBiasV);
            //System.out.println("du/dx: " + dudx + ", dv/dy: " + dvdy + ", mmU: " + mmU + ", mmV: " + mmV);
    
            //Paint pixel
            Color c;
            if (drawContours && filteredContourData.get(j - minX).get(i - minY)) {
                c = contourColor;
            }
            else {
                c = mm.getColor(uv.getX(), 1 - uv.getY(), useMipmap ? mmU : 0, useMipmap ? mmV : 0, filtering);
            }
            g.setColor(c);
            g.drawRect(normX(j), normY(i), 0, 0);
        });
            //}
        //}
        //g.setColor(Color.CYAN);
        //rasterizeTriangle(p, (x, y) -> g.drawRect(normX(x), normY(y), 0, 0));
        if (showOutline) {
            g.setColor(MODEL_COLOR);
            g.drawLine(normX(p.a.getX()), normY(p.a.getY()), normX(p.b.getX()), normY(p.b.getY()));
            g.drawLine(normX(p.b.getX()), normY(p.b.getY()), normX(p.c.getX()), normY(p.c.getY()));
            g.drawLine(normX(p.c.getX()), normY(p.c.getY()), normX(p.a.getX()), normY(p.a.getY()));
        }
        
    }
    
    private void rasterizeBottomFlat(Point3D a, Point3D b, Point3D c, BiConsumer<Integer, Integer> drawFunction) {
        List<Point3D> points = new ArrayList<>(Arrays.asList(a, b, c));
        int topI = (a.getY() >= b.getY() && a.getY() >= c.getY()) ? 0 : (b.getY() >= c.getY() ? 1 : 2);
        Point3D top = points.get(topI);
        points.remove(topI);
        
        int leftI = points.get(0).getX() <= points.get(1).getX() ? 0 : 1;
        
        Point3D left = points.get(leftI);
        points.remove(leftI);
        Point3D right = points.get(0);
        
        double invSlope1 = (Math.round(left.getX()) - Math.round(top.getX())) / (double)(Math.round(left.getY()) - Math.round(top.getY()));
        double invSlope2 = (Math.round(right.getX()) - Math.round(top.getX())) / (double)(Math.round(right.getY()) - Math.round(top.getY()));
        
        double x1 = left.getX();
        double x2 = right.getX();
        
        for (int scanY = (int)Math.round(left.getY()); scanY <= (int)Math.round(top.getY()); scanY++) {
            for (int i = (int)Math.round(x1); i <= (int)Math.round(x2); i++) {
                drawFunction.accept((int)Math.round(i), (int)Math.round(scanY));
            }
            x1 += invSlope1;
            x2 += invSlope2;
        }
    }
    
    private void rasterizeTopFlat(Point3D a, Point3D b, Point3D c, BiConsumer<Integer, Integer> drawFunction) {
        List<Point3D> points = new ArrayList<>(Arrays.asList(a, b, c));
        int botI = (a.getY() <= b.getY() && a.getY() <= c.getY()) ? 0 : (b.getY() <= c.getY() ? 1 : 2);
        Point3D bot = points.get(botI);
        points.remove(botI);
        
        int leftI = points.get(0).getX() <= points.get(1).getX() ? 0 : 1;
        
        Point3D left = points.get(leftI);
        points.remove(leftI);
        Point3D right = points.get(0);
    
        double invSlope1 = (Math.round(left.getX()) - Math.round(bot.getX())) / (double)(Math.round(left.getY()) - Math.round(bot.getY()));
        double invSlope2 = (Math.round(right.getX()) - Math.round(bot.getX())) / (double)(Math.round(right.getY()) - Math.round(bot.getY()));
        
        double x1 = left.getX();
        double x2 = right.getX();
        
        for (int scanY = (int)Math.round(left.getY()); scanY >= (int)Math.round(bot.getY()); scanY--) {
            for (int i = (int)Math.round(x1); i <= (int)Math.round(x2); i++) {
                drawFunction.accept((int)Math.round(i), (int)Math.round(scanY));
            }
            x1 -= invSlope1;
            x2 -= invSlope2;
        }
    }
    
    private void rasterizeTriangle(Polygon poly, BiConsumer<Integer, Integer> drawFunction) {
        List<Point3D> points = new ArrayList<>(Arrays.asList(poly.a, poly.b, poly.c));
        points.sort(Comparator.comparingDouble(p -> -p.getY()));
        
        if (points.get(1).getY() == points.get(2).getY()) {
            rasterizeBottomFlat(points.get(0), points.get(1), points.get(2), drawFunction);
        }
        
        if(points.get(0).getY() == points.get(1).getY()) {
            rasterizeTopFlat(points.get(2), points.get(1), points.get(0), drawFunction);
        }
        double alpha = (points.get(1).getY() - points.get(2).getY()) / (points.get(0).getY() - points.get(2).getY());
        
        double x = points.get(0).getX() * alpha + points.get(2).getX() * (1 - alpha);
        double z = points.get(0).getZ() * alpha + points.get(2).getZ() * (1 - alpha);
        double u = points.get(0).getU() * alpha + points.get(2).getU() * (1 - alpha);
        double v = points.get(0).getV() * alpha + points.get(2).getV() * (1 - alpha);
        
        Point3D d = new Point3D(x, points.get(1).getY(), z, u, v);
        rasterizeBottomFlat(points.get(0), points.get(1), d, drawFunction);
        rasterizeTopFlat(points.get(2), points.get(1), d, drawFunction);
    }
    
    private static double interpolate(double a, double b, double alpha) {
        return b * alpha + a * (1 - alpha);
    }
    
    private static Color interpolate(Color c1, Color c2, double alpha) {
        double gamma = 2.2;
        int r = (int) Math.round(255 * Math.pow(Math.pow(c2.getRed() / 255.0, gamma) * alpha + Math.pow(c1.getRed() / 255.0, gamma) * (1 - alpha), 1 / gamma));
        int g = (int) Math.round(255 * Math.pow(Math.pow(c2.getGreen() / 255.0, gamma) * alpha + Math.pow(c1.getGreen() / 255.0, gamma) * (1 - alpha), 1 / gamma));
        int b = (int) Math.round(255 * Math.pow(Math.pow(c2.getBlue() / 255.0, gamma) * alpha + Math.pow(c1.getBlue() / 255.0, gamma) * (1 - alpha), 1 / gamma));
        
        return new Color(r, g, b);
    }
    
    
    public int normX(double x) {
        return (int) Math.round((x / getScale()) + 0.5 * getWidth());
    }
    public int normY(double y) {
        return (int) Math.round(0.5 * getHeight() - (y / getScale()));
    }
    
    public double getScale() {
        return scale;
    }
    
    public void setScale(double scale) {
        this.scale = scale;
    }
    
    public Map<Model, Transform3D> getModels() {
        return models;
    }
    
    public boolean isParallelMode() {
        return parallelMode;
    }
    
    public void setParallelMode(boolean parallelMode) {
        this.parallelMode = parallelMode;
    }
    
    public double getAngleA() {
        return angleA;
    }
    
    public void setAngleA(double angleA) {
        this.angleA = angleA;
    }
    
    public double getFactorL() {
        return factorL;
    }
    
    public void setFactorL(double factorL) {
        this.factorL = factorL;
    }
    
    public double getFactorD() {
        return factorD;
    }
    
    public void setFactorD(double factorD) {
        this.factorD = factorD;
    }
    
    public boolean isWarpX() {
        return warpX;
    }
    
    public void setWarpX(boolean warpX) {
        this.warpX = warpX;
    }
    
    public boolean isWarpY() {
        return warpY;
    }
    
    public void setWarpY(boolean warpY) {
        this.warpY = warpY;
    }
    
    public boolean isWarpZ() {
        return warpZ;
    }
    
    public void setWarpZ(boolean warpZ) {
        this.warpZ = warpZ;
    }
    
    public void setShowOutline(boolean showOutline) {
        this.showOutline = showOutline;
    }
    
    public void setUseMipmap(boolean useMipmap) {
        this.useMipmap = useMipmap;
    }
    
    public int getTextureResolution() {
        return textureResolution;
    }
    
    public void setTextureResolution(int textureResolution) {
        this.textureResolution = textureResolution;
    }
    
    public FunctionCache getCache() {
        return cache;
    }
    
    public void setCache(FunctionCache cache) {
        this.cache = cache;
    }
    
    public boolean isDrawContours() {
        return drawContours;
    }
    
    public void setDrawContours(boolean drawContours) {
        this.drawContours = drawContours;
    }
    
    public int getContours() {
        return contours;
    }
    
    public void setContours(int contours) {
        this.contours = contours;
    }
    
    public double getContourOffset() {
        return contourOffset;
    }
    
    public void setContourOffset(double contourOffset) {
        this.contourOffset = contourOffset;
    }
    
    public BufferedImage getImage() {
        return image;
    }
    
    public void setImage(BufferedImage image) {
        this.image = image;
    }
    
    public static class FunctionCache {
        private BiFunction<Double, Double, Double> function;
        private ArrayList<ArrayList<Double>> data;
        private Model model;
        private int resolution;
        private double lowerX;
        private double upperX;
        private double lowerY;
        private double upperY;
        private double min;
        private double max;
        private boolean valid;
        
        public FunctionCache(BiFunction<Double, Double, Double> function, int resolution, double lowerX, double upperX, double lowerY, double upperY) {
            if (resolution < 1 || function == null) {
                throw new IllegalArgumentException();
            }
            double dx = (upperX - lowerX) / (double)resolution;
            double dy = (upperY - lowerY) / (double)resolution;
            min = Double.NaN;
            max = Double.NaN;
            
            this.function = function;
            this.resolution = resolution;
            this.lowerX = lowerX;
            this.upperX = upperX;
            this.lowerY = lowerY;
            this.upperY = upperY;
            this.valid = true;
            
            data = new ArrayList<>();
            for (int i = 0; i <= resolution; i++) {
                data.add(new ArrayList<>());
                for (int j = 0; j <= resolution; j++) {
                    data.get(i).add(Double.NaN);
                }
            }
            
            for (int i = 0; i <= resolution; i++) {
                for (int j = 0; j <= resolution; j++) {
                    double val = function.apply(lowerX + dx * j, lowerY + dy * i);
                    set(val, i, j);
                    if ((Double.isNaN(min) && !Double.isNaN(val)) || val < min) {
                        min = val;
                    }
                    if ((Double.isNaN(max) && !Double.isNaN(val)) || val > max) {
                        max = val;
                    }
                }
            }
        }
        
        public double get(double x, double y) {
            double lt;
            double rt;
            double lb;
            double rb;
            double t;
            double b;
            
            double col = resolution * (clampX(x) - lowerX) / (upperX - lowerX);
            double row = resolution * (clampY(y) - lowerY) / (upperY - lowerY);
            
            lb = get((int)Math.floor(row), (int)Math.floor(col));
            rb = get((int)Math.floor(row), (int)Math.ceil(col));
            lt = get((int)Math.ceil(row), (int)Math.floor(col));
            rt = get((int)Math.ceil(row), (int)Math.ceil(col));
            
            t = interpolate(lt, rt, col - (long)col);
            b = interpolate(lb, rb, col - (long)col);
            return interpolate(b, t, row - (long)row);
        }
        
        public Double getContourCutoffValue(int index, int totalContours, double offset) {
            double dz = (max - min) / (double)totalContours;
            return min + (index + offset + 0.5) * dz;
        }
    
        public Double getContourCutoffValueNormalized(int index, int totalContours, double offset) {
            double dz = 2 / (double)totalContours;
            return -1 + (index + offset + 0.5) * dz;
        }
        
        public Model generateModel() {
            int dataPoints = (resolution + 1) * (resolution + 1);
            int rowSize = resolution + 1;
            
            ArrayList<Point3D> vertices = new ArrayList<>(dataPoints);
            ArrayList<Pair<Integer, Integer>> edges = new ArrayList<>(2 * dataPoints);
            ArrayList<Polygon> polygons = new ArrayList<>(2 * dataPoints);
            double deltaX = (upperX - lowerX) / (double)resolution;
            double deltaY = (upperY - lowerY) / (double)resolution;
            
            //Add vertices
            for (int i = 0; i <= resolution; i++) {
                for (int j = 0; j <= resolution; j++) {
                    double x = lowerX + deltaX * j;
                    double y = lowerY + deltaY * i;
                    double z = get(x, y);
                    
                    double normX = 2.0 * j / (double)resolution - 1;
                    double normY = 2.0 * i / (double)resolution - 1;
                    double normZ = 2.0 * (z - min) / (max - min) - 1;
                    vertices.add(new Point3D(normX, normZ, normY, j / (double)resolution, i / (double)resolution));
                }
            }
    
            //Add edges
            for (int i = 0; i <= resolution; i++) {
                for (int j = 0; j <= resolution; j++) {
                    int index = i * (rowSize) + j;
                    if (j < resolution) {
                        edges.add(new Pair<>(index, index + 1));
                    }
                    if (i < resolution) {
                        edges.add(new Pair<>(index, index + rowSize));
                        if (j < resolution) {
                            edges.add(new Pair<>(index, index + rowSize + 1));
                            //Add polygons
                            polygons.add(new Polygon(vertices.get(index), vertices.get(index + rowSize), vertices.get(index + rowSize + 1)));
                            polygons.add(new Polygon(vertices.get(index + rowSize + 1), vertices.get(index + 1), vertices.get(index)));
                        }
                    }
                    
                    
                }
            }
            return new Model(vertices, edges, polygons);
        }
        
        private double clampX(double x) {
            return Math.min(Math.max(x, lowerX), upperX);
        }
        
        private double clampY(double y) {
            return Math.min(Math.max(y, lowerY), upperY);
        }
        
        private double get(int row, int col) {
            return data.get(row).get(col);
        }
        
        private void set(double value, int row, int col) {
            data.get(row).set(col, value);
        }
        
        public BiFunction<Double, Double, Double> getFunction() {
            return function;
        }
        
        public int getResolution() {
            return resolution;
        }
        
        public double getLowerX() {
            return lowerX;
        }
        
        public double getUpperX() {
            return upperX;
        }
        
        public double getLowerY() {
            return lowerY;
        }
        
        public double getUpperY() {
            return upperY;
        }
        
        public double getMin() {
            return min;
        }
        
        public double getMax() {
            return max;
        }
        
        public Model getModel() {
            if (model == null) {
                model = generateModel();
            }
            return model;
        }
        
        public boolean isValid() {
            return valid;
        }
        
        public void invalidate() {
            this.valid = false;
        }
    }
    
    public static class TextureGenerator {
        private static final Color HIGH_COLOR = /*new Color(0x7fbff3)*/ new Color(0xff6600);
        private static final Color LOW_COLOR = /*new Color(0xec80aa)*/ new Color(0x669955);
        private static final Color CONTOUR_COLOR = /*new Color(0x8869ff)*/ new Color(0x111111);
    
        private double lowerZ = Double.NEGATIVE_INFINITY;
        private double upperZ = Double.POSITIVE_INFINITY;
        
        private double contours = 20;
        private double contourWidth = 0.2;
        private double contourOffset = 0;
        
        private boolean alternateContours = false;
        
        private FunctionCache cache;
        private int width;
        private int height;
    
        public TextureGenerator(FunctionCache cache, int width, int height) {
            this.cache = cache;
            this.width = width;
            this.height = height;
        }
    
        private PointDouble valueToGraph(PointDouble point) {
            double valX = (point.getX() - cache.lowerX) / (cache.upperX - cache.lowerX);
            double valY = (point.getY() - cache.lowerY) / (cache.upperY - cache.lowerY);
            return new PointDouble((int)(width * valX), getHeight() - (int)(height * valY));
        }
    
        private PointDouble graphToValue(PointDouble point) {
            double valX = point.getX() / width;
            double valY = (point.getY() - height) / -height;
            return new PointDouble(cache.lowerX * (1 - valX) + cache.upperX * valX, cache.lowerY * (1 - valY) + cache.upperY * valY);
        }
        
        public FunctionCache getCache() {
            return cache;
        }
    
        public void setCache(FunctionCache cache) {
            this.cache = cache;
        }
    
        public int getWidth() {
            return width;
        }
    
        public void setWidth(int width) {
            this.width = width;
        }
    
        public int getHeight() {
            return height;
        }
    
        public void setHeight(int height) {
            this.height = height;
        }
    
        public BufferedImage generateTexture() {
            //g.setColor(new Color(Color.HSBtoRGB((float) Math.random(), 1.0f, 1.0f)));
            //int prev = 0;
            BufferedImage texture = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
            WritableRaster raster = texture.getRaster();
            
            int[] rasterData = new int[width * height];
            for (int i = 0; i < width * height; i++) {
                rasterData[i] = Color.BLACK.getRGB();
            }
            
            Color color;
            
            double max = Math.min(upperZ, cache.max);
            double min = Math.max(lowerZ, cache.min);
            double dz = (max - min) / (double)contours;
            boolean matched;
            for (int i = 0; i < width; i++) {
                for (int j = 0; j < height; j++) {
                    PointDouble in = graphToValue(new PointDouble(i, j));
                    double val = cache.get(in.getX(), in.getY());
                    matched = false;
                    if (alternateContours && contours > 0) {
                        for (int k = 0; k <= contours; k++) {
                            if (val >= min + (k - 0.5 * contourWidth + contourOffset + 0.5) * dz && val <= min + (k + 0.5 * contourWidth + contourOffset + 0.5) * dz) {
                                matched = true;
                                break;
                            }
                        }
                    }
                    if (matched) {
                        color = CONTOUR_COLOR;
                    }
                    else {
                        color = interpolate(LOW_COLOR, HIGH_COLOR, Math.min(Math.max((val - min) / (max - min), 0), 1));
                    }
                    
                    rasterData[j * width + i] = color.getRGB();
                /*val = new PointDouble(val.getX(), op.applyAsDouble(val.getX()));
                val = valueToGraph(val);
                if (i != 0) {
                    g.drawLine(MARGIN_X + i - 1, prev, (int) Math.round(val.getX()), (int) Math.round(val.getY()));
                }
                prev = (int) Math.round(val.getY());*/
            
                }
            }
            //Draw contours
            if (!alternateContours) {
                for (int k = 0; k < contours; k++) {
                    double target = min + (k + contourOffset + 0.5) * dz;
            
                    //Map of all points higher/lower then the target
                    ArrayList<ArrayList<Boolean>> data = new ArrayList<>(width);
                    for (int i = 0; i < width; i++) {
                        data.add(new ArrayList<>(height));
                        for (int j = 0; j < height; j++) {
                            PointDouble in = graphToValue(new PointDouble(i, j));
                            double val = cache.get(in.getX(), in.getY());
                            data.get(i).add(val > target);
                        }
                    }
            
                    //Edge detection filter
                    ArrayList<ArrayList<Boolean>> filteredData = new ArrayList<>(width);
                    for (int i = 0; i < width; i++) {
                        filteredData.add(new ArrayList<>(height));
                        for (int j = 0; j < height; j++) {
                            boolean tl = data.get(Math.max(Math.min(i - 1, width - 1), 0)).get(Math.max(Math.min(j + 1, height - 1), 0));
                            boolean tc = data.get(Math.max(Math.min(i, width - 1), 0)).get(Math.max(Math.min(j + 1, height - 1), 0));
                            boolean tr = data.get(Math.max(Math.min(i + 1, width - 1), 0)).get(Math.max(Math.min(j + 1, height - 1), 0));
                    
                            boolean cl = data.get(Math.max(Math.min(i - 1, width - 1), 0)).get(Math.max(Math.min(j, height - 1), 0));
                            boolean cc = data.get(Math.max(Math.min(i, width - 1), 0)).get(Math.max(Math.min(j, height - 1), 0));
                            boolean ct = data.get(Math.max(Math.min(i + 1, width - 1), 0)).get(Math.max(Math.min(j, height - 1), 0));
                    
                            boolean bl = data.get(Math.max(Math.min(i - 1, width - 1), 0)).get(Math.max(Math.min(j - 1, height - 1), 0));
                            boolean bc = data.get(Math.max(Math.min(i, width - 1), 0)).get(Math.max(Math.min(j - 1, height - 1), 0));
                            boolean br = data.get(Math.max(Math.min(i + 1, width - 1), 0)).get(Math.max(Math.min(j - 1, height - 1), 0));
                    
                            boolean res;
                            if (contourWidth > 0.5) {
                                res = (cc && (!tl || !tc || !tr || !cl || !ct || !bl || !bc || !br)) || (!cc && (tl || tc || tr || cl || ct || bl || bc || br));
                            }
                            else {
                                if (contourWidth > 0.25) {
                                    res = (cc && (!tc || !cl || !ct || !bc)) || (!cc && (tc || cl || ct || bc));
                                }
                                else {
                                    res = cc && (!tc || !cl || !ct || !bc);
                                }
                            }
                            filteredData.get(i).add(res);
                        }
                    }
            
                    //Draw contour
                    color = CONTOUR_COLOR;
                    for (int i = 0; i < height; i++) {
                        for (int j = 0; j < width; j++) {
                            if (filteredData.get(j).get(i)) {
                                rasterData[i * width + j] = color.getRGB();
                            }
                        }
                    }
                }
            }
            raster.setDataElements(0, 0, width, height, rasterData);
            return texture;
        }
    }
    
}

