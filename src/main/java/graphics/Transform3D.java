package graphics;

public class Transform3D {
    public Point3D offset = new Point3D();
    public Point3D rotation = new Point3D();
    public Point3D scale = new Point3D(1.0, 1.0, 1.0);
    
    public Transform3D(){
    
    }
    
    public Transform3D(Point3D offset, Point3D rotation, Point3D scale) {
    
        this.offset = offset;
        this.rotation = rotation;
        this.scale = scale;
    }
}
