package graphics;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Model {
    private List<Point3D> vertices = new ArrayList<>();
    private List<Pair<Integer, Integer>> edges = new ArrayList<>();
    private List<Polygon> polygons = new ArrayList<>();
    
    public Model(List<Point3D> vertices, List<Pair<Integer, Integer>> edges) {
        this.vertices.addAll(vertices);
        this.edges.addAll(edges);
    }
    
    public Model(List<Point3D> vertices, List<Pair<Integer, Integer>> edges, List<Polygon> polygons) {
        this.vertices.addAll(vertices);
        this.edges.addAll(edges);
        this.polygons.addAll(polygons);
    }
    
    public List<Point3D> getVertices() {
        return vertices;
    }
    
    
    public List<Pair<Integer, Integer>> getEdges() {
        return edges;
    }
    
    public List<Polygon> getPolygons() {
        return polygons;
    }
    
    public static Model axis(double length) {
        return new Model(
                Arrays.asList(new Point3D(length, 0, 0), new Point3D(-length, 0, 0), new Point3D(0, length, 0), new Point3D(0, -length, 0), new Point3D(0, 0, length), new Point3D(0, 0, -length)),
                Arrays.asList(new Pair<>(0, 1), new Pair<>(2, 3), new Pair<>(4, 5))
        );
    }
    
    public static Model cube(double length) {
        double hl = length / 2.0;
        return new Model(
                Arrays.asList(
                        new Point3D(hl, hl, hl),
                        new Point3D(hl, hl, -hl),
                        new Point3D(hl, -hl, hl),
                        new Point3D(hl, -hl, -hl),
                        new Point3D(-hl, hl, hl),
                        new Point3D(-hl, hl, -hl),
                        new Point3D(-hl, -hl, hl),
                        new Point3D(-hl, -hl, -hl)
                ),
                Arrays.asList(
                        new Pair<>(0, 1),
                        new Pair<>(0, 2),
                        new Pair<>(0, 4),
                        new Pair<>(7, 6),
                        new Pair<>(7, 5),
                        new Pair<>(7, 3),
                        new Pair<>(1, 5),
                        new Pair<>(1, 3),
                        new Pair<>(2, 3),
                        new Pair<>(4, 5),
                        new Pair<>(6, 2),
                        new Pair<>(6, 4)
                        
                )
        );
    }
    
    public static Model cube2(double length) {
        double hl = length / 2.0;
        return new Model(
                Arrays.asList(
                        new Point3D(hl, hl, hl),
                        new Point3D(hl, hl, -hl),
                        new Point3D(hl, -hl, hl),
                        new Point3D(hl, -hl, -hl),
                        new Point3D(-hl, hl, hl),
                        new Point3D(-hl, hl, -hl),
                        new Point3D(-hl, -hl, hl),
                        new Point3D(-hl, -hl, -hl),
                        
                        new Point3D(0, hl, hl),
                        new Point3D(0, hl, -hl),
                        new Point3D(0, -hl, hl),
                        new Point3D(0, -hl, -hl),
                        new Point3D(hl, 0, hl),
                        new Point3D(hl, 0, -hl),
                        new Point3D(-hl, 0, hl),
                        new Point3D(-hl, 0, -hl),
                        new Point3D(hl, hl, 0),
                        new Point3D(hl, -hl, 0),
                        new Point3D(-hl, hl, 0),
                        new Point3D(-hl, -hl, 0)
                ),
                Arrays.asList(
                        new Pair<>(0, 16),
                        new Pair<>(0, 12),
                        new Pair<>(0, 8),
                        new Pair<>(7, 19),
                        new Pair<>(7, 15),
                        new Pair<>(7, 11),
                        new Pair<>(1, 9),
                        new Pair<>(1, 13),
                        new Pair<>(2, 17),
                        new Pair<>(4, 18),
                        new Pair<>(6, 10),
                        new Pair<>(6, 14),
        
                        new Pair<>(16, 1),
                        new Pair<>(12, 2),
                        new Pair<>(8, 4),
                        new Pair<>(19, 6),
                        new Pair<>(15, 5),
                        new Pair<>(11, 3),
                        new Pair<>(9, 5),
                        new Pair<>(13, 3),
                        new Pair<>(17, 3),
                        new Pair<>(18, 5),
                        new Pair<>(10, 2),
                        new Pair<>(14, 4)
                )
        );
    }
}
