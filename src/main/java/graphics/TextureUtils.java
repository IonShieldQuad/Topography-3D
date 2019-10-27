package graphics;

import java.awt.*;
import java.awt.image.BufferedImage;

public class TextureUtils {
    public static PointDouble uvToXy(BufferedImage texture, double u, double v) {
        return new PointDouble(texture.getWidth() * (u - (int) u + (u < 0 ? 1 : 0)), texture.getHeight() * (v - (int)v + (v < 0 ? 1 : 0)));
    }
    
    public static double interpolate(double a, double b, double alpha) {
        return b * alpha + a * (1 - alpha);
    }
    
    public static Color interpolate(Color c1, Color c2, double alpha) {
        double gamma = 2.2;
        int r = (int) Math.round(255 * Math.pow(Math.pow(c2.getRed() / 255.0, gamma) * alpha + Math.pow(c1.getRed() / 255.0, gamma) * (1 - alpha), 1 / gamma));
        int g = (int) Math.round(255 * Math.pow(Math.pow(c2.getGreen() / 255.0, gamma) * alpha + Math.pow(c1.getGreen() / 255.0, gamma) * (1 - alpha), 1 / gamma));
        int b = (int) Math.round(255 * Math.pow(Math.pow(c2.getBlue() / 255.0, gamma) * alpha + Math.pow(c1.getBlue() / 255.0, gamma) * (1 - alpha), 1 / gamma));
        
        return new Color(r, g, b);
    }
    
    public static Color getColor(BufferedImage texture, double u, double v, Filtering filter) {
        switch (filter) {
            case OFF:
                break;
            
            case BILINEAR:
            case TRILINEAR:
            case ANISOTROPIC:
                return getColor(texture, u, v);
        }
        PointDouble xy = uvToXy(texture, u, v);
        return new Color(getRGBSafe(texture, (int) Math.round(xy.getX()), (int) Math.round(xy.getY())));
    }
    
    public static Color getColor(BufferedImage texture, double u, double v) {
        PointDouble xy = uvToXy(texture, u, v);
        Color tl = new Color(getRGBSafe(texture, (int) Math.floor(xy.getX()), (int) Math.floor(xy.getY())));
        Color tr = new Color(getRGBSafe(texture, (int) Math.ceil(xy.getX()), (int) Math.floor(xy.getY())));
        Color bl = new Color(getRGBSafe(texture, (int) Math.floor(xy.getX()), (int) Math.ceil(xy.getY())));
        Color br = new Color(getRGBSafe(texture, (int) Math.ceil(xy.getX()), (int) Math.ceil(xy.getY())));
        double ax = 1 - Math.abs(Math.min(Math.ceil(xy.getX()), texture.getWidth() - 1) - xy.getX());
        double ay = 1 - Math.abs(Math.min(Math.ceil(xy.getY()), texture.getHeight() - 1) - xy.getY());
        
        Color t = interpolate(tl, tr, ax);
        Color b = interpolate(bl, br, ax);
        
        return interpolate(t, b, ay);
    }
    
    public static int getRGBSafe(BufferedImage tex, int x, int y) {
        return tex.getRGB(Math.min(Math.max(x, 0), tex.getWidth() - 1), Math.min(Math.max(y, 0), tex.getHeight() - 1));
    }
    
    public enum Filtering {
        OFF,
        BILINEAR,
        TRILINEAR,
        ANISOTROPIC
    }
}
