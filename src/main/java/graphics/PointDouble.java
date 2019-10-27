package graphics;

public class PointDouble {
    private Double x;
    private Double y;
    
    public PointDouble(double x, double y) {
        this.x = x;
        this.y = y;
    }
    
    
    public Double getX() {
        return x;
    }
    
    public void setX(Double x) {
        this.x = x;
    }
    
    public Double getY() {
        return y;
    }
    
    public void setY(Double y) {
        this.y = y;
    }
    
    public PointDouble add(PointDouble b) {
        return new PointDouble(x + b.x, y + b.y);
    }
    
    public PointDouble add(double x, double y) {
        return new PointDouble(this.x + x, this.y + y);
    }
    
    public PointDouble scale(double k) {
        return new PointDouble(k * x, k * y);
    }
    
    public double lengthSquared() {
        return x * x + y * y;
    }
    
    public double length() {
        return Math.sqrt(lengthSquared());
    }
    
    @Override
    public String toString() {
        return "(" + x + "; " + y + ")";
    }
}
