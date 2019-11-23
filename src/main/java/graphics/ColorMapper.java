package graphics;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;

public class ColorMapper {
    private Mode mode;
    private double distancePower = 2;
    private double gamma = 2.2;
    
    public ColorMapper() {}
    
    public ColorMapper(Mode mode, double distancePower) {
        this.mode = mode;
        this.distancePower = distancePower;
    }
    
    public BiFunction<Double, Double, Double> mapColors(BufferedImage image, Map<Color, Double> colorData) {
        return (inU, inV) -> {
            double u = Math.max(0, Math.min(1, inU));
            double v = Math.max(0, Math.min(1, inV));
    
            Color pointColor = TextureUtils.getColor(image, u, v);
            
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
        
        return Math.sqrt((r1 - r2) * (r1 - r2) + (g1 - g2) * (g1 - g2) + (b1 - b2) * (b1 - b2));
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
    
    public enum Mode {
        RGB, HSV, HSL, CIE76, CIE94, CIEDE2000
    }
}
