package graphics;

public class Point3D {
    private double x = 0.0;
    private double y = 0.0;
    private double z = 0.0;
    private double u = 0.0;
    private double v = 0.0;
    
    public Point3D(){
    
    }
    
    public Point3D(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }
    
    public Point3D(double x, double y, double z, double u, double v) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.u = u;
        this.v = v;
    }
    
    public double getX() {
        return x;
    }
    
    public void setX(double x) {
        this.x = x;
    }
    
    public double getY() {
        return y;
    }
    
    public void setY(double y) {
        this.y = y;
    }
    
    public double getZ() {
        return z;
    }
    
    public void setZ(double z) {
        this.z = z;
    }
    
    public double getU() {
        return u;
    }
    
    public void setU(double u) {
        this.u = u;
    }
    
    public double getV() {
        return v;
    }
    
    public void setV(double v) {
        this.v = v;
    }
    
    public Point3D copy() {
        return new Point3D(x, y, z, u, v);
    }
    
    public Matrix toMatrix() {
        Matrix m = Matrix.makeEmptyMatrix(1, 4);
        return m.fill(new double[][]{{x, y, z, 1}}).transpose();
    }
    
    public double dot(Point3D b) {
        return this.getX() * b.getX() + this.getY() * b.getY() + this.getZ() * b.getZ();
    }
    
    public Point3D cross(Point3D b) {
        return new Point3D(this.getY() * b.getZ() - this.getZ() * b.getY(), this.getX() * b.getZ() - this.getZ() - b.getX(), this.getX() * b.getY() - this.getY() * b.getX());
    }
    
    public Point3D plus(Point3D b) {
        return new Point3D(this.getX() + b.getX(), this.getY() + b.getY(), this.getZ() + b.getZ());
    }
    
    public Point3D minus(Point3D b) {
        return new Point3D(this.getX() - b.getX(), this.getY() - b.getY(), this.getZ() - b.getZ());
    }
    
    public Point3D mult(double f) {
        return new Point3D(this.getX() * f, this.getY() * f, this.getZ() * f);
    }
    
    public double magn() {
        return Math.sqrt(this.dot(this));
    }
    
    public Point3D normalize() {
        return this.copy().mult(1 /this.magn());
    }
    
    @Override
    public String toString() {
        return "Point3D{" +
                "x=" + x +
                ", y=" + y +
                ", z=" + z +
                ", u=" + u +
                ", v=" + v +
                '}';
    }
}
