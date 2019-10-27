package graphics;

public class Polygon {
    public Point3D a;
    public Point3D b;
    public Point3D c;
    
    public Polygon(Point3D a, Point3D b, Point3D c) {
        this.a = a;
        this.b = b;
        this.c = c;
    }
    
    public Point3D barycentric(Point3D p) {
        Point3D v1 = a.minus(p);
        Point3D v2 = b.minus(p);
        Point3D v3 = c.minus(p);
        
        Point3D va0 = ((a.minus(b)).cross(a.minus(c)));
        Point3D va1 = v2.cross(v3);
        Point3D va2 = v3.cross(v1);
        Point3D va3 = v1.cross(v2);
        
        double a0 = va0.magn();
        
        double a1 = (va1.magn() / a0) * Math.signum(va0.dot(va1));
        double a2 = (va2.magn() / a0) * Math.signum(va0.dot(va2));
        double a3 = (va3.magn() / a0) * Math.signum(va0.dot(va3));
    
        /*System.out.println("p: " + p);
        System.out.println("pts: " + a + b + c);
        System.out.println("v: " + v1 + v2 + v3);
        System.out.println("va: " + va0 + va1 + va2 + va3);*/
        
        return new Point3D(a1, a2, a3);
    }
    
    public PointDouble uv(Point3D p) {
        Point3D bary = barycentric(p);
        return new PointDouble(a.getU() * bary.getX() + b.getU() * bary.getY() + c.getU() * bary.getZ(), a.getV() * bary.getX() + b.getV() * bary.getY() + c.getV() * bary.getZ());
    }
    
    public boolean contains(Point3D p) {
        Point3D b = barycentric(p);
        return b.getX() >= 0 && b.getY() >= 0 && b.getZ() >= 0;
    }
    
    public Point3D normal() {
        Point3D n = (a.minus(b)).cross(a.minus(c));
        return n.mult(1 / n.magn());
    }
    
    public Point3D intersection(Point3D linePoint, Point3D lineDirection) {
        /*Point3D diff = linePoint.minus(a);
    
        double prod1 = diff.dot(normal());
        double prod2 = lineDirection.dot(normal());
        double prod3 = prod1 / prod2;
    
        return linePoint.minus(lineDirection.mult(prod3));*/
    
        Point3D planePoint = c;
        
        double t = (normal().dot(planePoint) - normal().dot(linePoint)) / normal().dot(lineDirection.normalize());
        return linePoint.plus(lineDirection.normalize().mult(t));
    }
    
    public Polygon applyTransform(Transform3D t) {
        Polygon poly = new Polygon(this.a.copy(), this.b.copy(), this.c.copy());
        
        Matrix am = poly.a.toMatrix();
        Matrix bm = poly.b.toMatrix();
        Matrix cm = poly.c.toMatrix();
        
        
        Matrix sm = Matrix.scaleMatrix3D(t.scale.getX(), t.scale.getY(), t.scale.getZ());
        Matrix rm = Matrix.rotationMatrix3D(t.rotation.getX(), t.rotation.getY(), t.rotation.getZ());
        Matrix om = Matrix.offsetMatrix3D(t.offset.getX(), t.offset.getY(), t.offset.getZ());
        
        am = am.multiply(sm);
        bm = bm.multiply(sm);
        cm = cm.multiply(sm);
        
        am = am.multiply(rm);
        bm = bm.multiply(rm);
        cm = cm.multiply(rm);
        
        am = am.multiply(om);
        bm = bm.multiply(om);
        cm = cm.multiply(om);
        
        poly.a.setX(am.get(0, 0));
        poly.a.setY(am.get(1, 0));
        poly.a.setZ(am.get(2, 0));
        
        poly.b.setX(bm.get(0, 0));
        poly.b.setY(bm.get(1, 0));
        poly.b.setZ(bm.get(2, 0));
        
        poly.c.setX(cm.get(0, 0));
        poly.c.setY(cm.get(1, 0));
        poly.c.setZ(cm.get(2, 0));
        
        poly.a = new Point3D(am.get(0, 0), am.get(1, 0), am.get(2, 0), a.getU(), a.getV());
        poly.b = new Point3D(bm.get(0, 0), bm.get(1, 0), bm.get(2, 0), b.getU(), b.getV());
        poly.c = new Point3D(cm.get(0, 0), cm.get(1, 0), cm.get(2, 0), c.getU(), c.getV());
        
        return poly;
    }
}
