package graphics;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;

public class ColorMapper {
    
    private Mode mode;
    private double distancePower = 2;
    private double gamma = 2.2;
    private boolean useMipmaps = true;
    
    private double m1 = 1;
    private double m2 = 1;
    private double m3 = 1;
    
    public ColorMapper() {}
    
    public ColorMapper(Mode mode) {
        this.mode = mode;
    }
    
    public BiFunction<Double, Double, Double> mapColors(Mipmapper image, int resolution, Map<Color, Double> colorData) {
        if (image == null) {
            return null;
        }
        return (inU, inV) -> {
            double u = Math.max(0, Math.min(1, inU));
            double v = Math.max(0, Math.min(1, inV));
    
            Color pointColor;
            if (useMipmaps && resolution > 0) {
                double mmU = Math.log(Math.max(1, image.getTexture().getWidth() / (double)resolution)) / Math.log(2);
                double mmV = Math.log(Math.max(1, image.getTexture().getHeight() / (double)resolution)) / Math.log(2);
                
                pointColor = image.getColor(u, v, mmU, mmV, TextureUtils.Filtering.ANISOTROPIC);
            }
            else {
                pointColor = image.getColor(u, v, 0, 0, TextureUtils.Filtering.BILINEAR);
            }
            
            if (colorData == null || colorData.isEmpty()) {
                return 0.299 * pointColor.getRed() / 255.0 + 0.587 * pointColor.getGreen() / 255.0 + 0.114 * pointColor.getBlue() / 255;
            }
            
            Map<Color, Double> weightMap = new HashMap<>();
            for (Color color : colorData.keySet()) {
                if (colorDistance(color, pointColor) <= 0) {
                    return colorData.get(color);
                }
                weightMap.put(color, 1 / Math.pow(colorDistance(pointColor, color), distancePower));
            }
            
            double totalValue = 0;
            double totalWeight = 0;
            for (Color color : colorData.keySet()) {
                totalValue += weightMap.get(color) * colorData.get(color);
                totalWeight += weightMap.get(color);
            }
            return totalValue / totalWeight;
        };
    }
    
    public double colorDistance(Color a, Color b) {
        double r1 = Math.pow(a.getRed() / 255.0, gamma);
        double g1 = Math.pow(a.getGreen() / 255.0, gamma);
        double b1 = Math.pow(a.getBlue() / 255.0, gamma);
    
        double r2 = Math.pow(b.getRed() / 255.0, gamma);
        double g2 = Math.pow(b.getGreen() / 255.0, gamma);
        double b2 = Math.pow(b.getBlue() / 255.0, gamma);
        
        switch (mode) {
            case HSV: {
                HSVColor hsv0 = HSVColor.fromRGB(r1, g1, b1);
                HSVColor hsv1 = HSVColor.fromRGB(r2, g2, b2);
    
                double dh = Math.min(Math.abs(hsv1.h - hsv0.h), 360 - Math.abs(hsv1.h - hsv0.h)) / 180.0;
                double ds = Math.abs(hsv1.s - hsv0.s);
                double dv = Math.abs(hsv1.v - hsv0.v);
    
                return Math.sqrt(m1 * dh * dh + m2 * ds * ds + m3 * dv * dv);
            }
                
            case HSL: {
                HSLColor hsl0 = HSLColor.fromRGB(r1, g1, b1);
                HSLColor hsl1 = HSLColor.fromRGB(r2, g2, b2);
    
                double dh = Math.min(Math.abs(hsl1.h - hsl0.h), 360 - Math.abs(hsl1.h - hsl0.h)) / 180.0;
                double ds = Math.abs(hsl1.s - hsl0.s);
                double dl = Math.abs(hsl1.l - hsl0.l);
    
                return Math.sqrt(m1 * dh * dh + m2 * ds * ds + m3 * dl * dl);
            }
            
            case CIE76: {
                LABColor lab0 = LABColor.fromXYZ(XYZColor.fromRGB(r1, g1, b1));
                LABColor lab1 = LABColor.fromXYZ(XYZColor.fromRGB(r2, g2, b2));
                return Math.sqrt(m1 * (lab0.l - lab1.l) * (lab0.l - lab1.l) + m2 * (lab0.a - lab1.a) * (lab0.a - lab1.a) + m3 * (lab0.b - lab1.b) * (lab0.b - lab1.b));
            }
            
            default:
                return Math.sqrt(m1 * (r1 - r2) * (r1 - r2) + m2 * (g1 - g2) * (g1 - g2) + m3 * (b1 - b2) * (b1 - b2));
        }
    }
    
    public double getDistancePower() {
        return distancePower;
    }
    
    public void setDistancePower(double distancePower) {
        this.distancePower = distancePower;
    }
    
    public Mode getMode() {
        return mode;
    }
    
    public void setMode(Mode mode) {
        this.mode = mode;
    }
    
    public double getGamma() {
        return gamma;
    }
    
    public void setGamma(double gamma) {
        this.gamma = gamma;
    }
    
    public double getM1() {
        return m1;
    }
    
    public void setM1(double m1) {
        this.m1 = m1;
    }
    
    public double getM2() {
        return m2;
    }
    
    public void setM2(double m2) {
        this.m2 = m2;
    }
    
    public double getM3() {
        return m3;
    }
    
    public void setM3(double m3) {
        this.m3 = m3;
    }
    
    public boolean isUseMipmaps() {
        return useMipmaps;
    }
    
    public void setUseMipmaps(boolean useMipmaps) {
        this.useMipmaps = useMipmaps;
    }
    
    public enum Mode {
        RGB, HSV, HSL, CIE76, CIE94, CIEDE2000
    }
    
    public static class HSVColor {
        public double h;
        public double s;
        public double v;
    
        public HSVColor(double h, double s, double v) {
            this.h = h;
            this.s = s;
            this.v = v;
        }
        
        public Color toRGB() {
            double h = this.h % 360;
            double c = v * s;
            double x = c * (1 - Math.abs((h / 60) % 2 - 1));
            double m = v - c;
            double r, g, b;
            
            if (h < 60) {
                r = c;
                g = x;
                b = 0;
            }
            else {
                if (h < 120) {
                    r = x;
                    g = c;
                    b = 0;
                }
                else {
                    if (h < 180) {
                        r = 0;
                        g = c;
                        b = x;
                    }
                    else {
                        if (h < 240) {
                            r = 0;
                            g = x;
                            b = c;
                        }
                        else {
                            if (h < 300) {
                                r = x;
                                g = 0;
                                b = c;
                            }
                            else {
                                r = c;
                                g = 0;
                                b = x;
                            }
                        }
                    }
                }
            }
            return new Color((int)Math.round((r + m) * 255), (int)Math.round((g + m) * 255), (int)Math.round((b + m) * 255));
        }
        
        public static HSVColor fromRGB(Color color) {
            return fromRGB(color.getRed() / 255.0 , color.getGreen() / 255.0, color.getBlue() / 255.0);
        }
    
        public static HSVColor fromRGB(double r, double g, double b) {
            double cMax = Math.max(r, Math.max(g, b));
            double cMin = Math.min(r, Math.min(g, b));
            double delta = cMax - cMin;
            
            double h;
            if (delta == 0) {
                h = 0;
            }
            else {
                if (cMax == r) {
                    h = 60 * (((g - b) / delta) % 6);
                }
                else {
                    if (cMax == g) {
                        h = 60 * (((b - r) / delta) + 2);
                    }
                    else {
                        h = 60 * (((r - g) / delta) + 4);
                    }
                }
            }
            
            double s;
            if (cMax == 0) {
                s = 0;
            }
            else {
                s = delta / cMax;
            }
            
            return new HSVColor(h, s, cMax);
        }
        
    }
    
    public static class HSLColor {
        public double h;
        public double s;
        public double l;
    
        public HSLColor(double h, double s, double l) {
            this.h = h;
            this.s = s;
            this.l = l;
        }
    
        public Color toRGB() {
            double h = this.h % 360;
            double c = (1 - Math.abs(2 * l - 1)) * s;
            double x = c * (1 - Math.abs((h / 60) % 2 - 1));
            double m = l - c / 2;
            double r, g, b;
        
            if (h < 60) {
                r = c;
                g = x;
                b = 0;
            }
            else {
                if (h < 120) {
                    r = x;
                    g = c;
                    b = 0;
                }
                else {
                    if (h < 180) {
                        r = 0;
                        g = c;
                        b = x;
                    }
                    else {
                        if (h < 240) {
                            r = 0;
                            g = x;
                            b = c;
                        }
                        else {
                            if (h < 300) {
                                r = x;
                                g = 0;
                                b = c;
                            }
                            else {
                                r = c;
                                g = 0;
                                b = x;
                            }
                        }
                    }
                }
            }
            return new Color((int)Math.round((r + m) * 255), (int)Math.round((g + m) * 255), (int)Math.round((b + m) * 255));
        }
    
        public static HSLColor fromRGB(Color color) {
            return fromRGB(color.getRed() / 255.0 , color.getGreen() / 255.0, color.getBlue() / 255.0);
        }
    
        public static HSLColor fromRGB(double r, double g, double b) {
            double cMax = Math.max(r, Math.max(g, b));
            double cMin = Math.min(r, Math.min(g, b));
            double delta = cMax - cMin;
        
            double h;
            if (delta == 0) {
                h = 0;
            }
            else {
                if (cMax == r) {
                    h = 60 * (((g - b) / delta) % 6);
                }
                else {
                    if (cMax == g) {
                        h = 60 * (((b - r) / delta) + 2);
                    }
                    else {
                        h = 60 * (((r - g) / delta) + 4);
                    }
                }
            }
        
            double l = (cMax + cMin) / 2;
            
            double s;
            if (delta == 0) {
                s = 0;
            }
            else {
                s = delta / (1 - Math.abs(2 * l - 1));
            }
        
            return new HSLColor(h, s, l);
        }
    }
    
    public static class XYZColor {
        public double x;
        public double y;
        public double z;
    
        public XYZColor(double x, double y, double z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }
        
        public Color toRGB() {
            double r = 3.2404542 * x - 1.5371385 * y - 0.4985314 * z;
            double g = -0.9692660 * x + 1.8760108 * y + 0.0415560 * z;
            double b = 0.0556434 * x - 0.2040259 * y + 1.0572252 * z;
            return new Color((int)Math.round(r * 255), (int)Math.round(g * 255), (int)Math.round(b * 255));
        }
        
        public static XYZColor fromRGB(Color c) {
            return fromRGB(c.getRed() / 255.0, c.getGreen() / 255.0, c.getBlue() / 255.0);
        }
        
        public static XYZColor fromRGB(double r, double g, double b) {
            double x = 0.4124564 * r + 0.3575761 * g + 0.1804375 * b;
            double y = 0.2126729 * r + 0.7151522 * g + 0.0721750 * b;
            double z = 0.0193339 * r + 0.1191920 * g + 0.9503041 * b;
            return new XYZColor(x, y, z);
        }
    }
    
    public static class LABColor {
        public double l;
        public double a;
        public double b;
    
        private static final double XN = 95.0489;
        private static final double YN = 100;
        private static final double ZN = 108.8840;
        
        private static final double D = 6.0/29.0;
        private static final Function<Double, Double> F_0 = t -> {
            if (t > Math.pow(D, 3)) {
                return Math.pow(t, 1/3.0);
            }
            else {
                return t / (3 * D * D) + 4.0 / 29.0;
            }
        };
        private static final Function<Double, Double> F_1 = t -> {
            if (t > D) {
                return t * t * t;
            }
            else {
                return 3 * D * D * (t - 4.0 / 29.0);
            }
        };
    
        public LABColor(double l, double a, double b) {
            this.l = l;
            this.a = a;
            this.b = b;
        }
        public XYZColor toXYZ() {
            double x = XN * F_1.apply((l + 16) / 116.0 + a / 500.0);
            double y = YN * F_1.apply((l + 16) / 116.0);
            double z = ZN * F_1.apply((l + 16) / 116.0 - b / 200.0);
            return new XYZColor(x, y, z);
        }
        
        public static LABColor fromXYZ(XYZColor xyz) {
            double l = 116 * F_0.apply(xyz.y / YN) - 16;
            double a = 500 * (F_0.apply(xyz.x / XN) - F_0.apply(xyz.y / YN));
            double b = 200 * (F_0.apply(xyz.y / YN) - F_0.apply(xyz.z / ZN));
            
            return new LABColor(l, a, b);
        }
    }
}
