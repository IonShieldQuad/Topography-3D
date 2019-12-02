package graphics;

import org.apache.commons.math3.complex.Quaternion;

public class Rotation3D {
    private double angle;
    private Point3D vector;
    
    public Rotation3D() {
        this.vector = new Point3D();
    };
    
    public Rotation3D(double angle, Point3D vector) {
        this.angle = angle;
        this.vector = vector;
        if (vector == null) {
            this.vector = new Point3D();
        }
    }
    
    public Rotation3D(double angle, double x, double y, double z) {
        this.angle = angle;
        this.vector = new Point3D(x, y, z);
    }
    
    public Point3D applyTo(Point3D in) {
        Quaternion v = new Quaternion(0, in.getX(), in.getY(), in.getZ());
        double m = vector.magn();
        double sin = Math.sin(angle / 2);
        Quaternion q = new Quaternion(Math.cos(angle / 2), sin * vector.getX() / m, sin * vector.getY() / m, sin * vector.getZ() / m);
        Quaternion res = (q.multiply(v)).multiply(q.getConjugate());
        
        return new Point3D(res.getQ1(), res.getQ2(), res.getQ3(), in.getU(), in.getV());
    }
    
    public double getAngle() {
        return angle;
    }
    
    public void setAngle(double angle) {
        this.angle = angle;
    }
    
    public Point3D getVector() {
        return vector;
    }
    
    public void setVector(Point3D vector) {
        this.vector = vector;
        if (vector == null) {
            this.vector = new Point3D();
        }
    }
}
