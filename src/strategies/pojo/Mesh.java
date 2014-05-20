package strategies.pojo;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;

/**
* Created by inti on 10/05/14.
*/
public class Mesh implements Serializable {
    List<Vertex> vertexes = new LinkedList<Vertex>();
    List<Triangle> triangles = new LinkedList<Triangle>();

    public void addVertex(double x, double y, double z) {
        vertexes.add(new Vertex(x,y,z));
    }

    public void addTriangle(int v0, int v1, int v2) {
        triangles.add(new Triangle(v0,v1,v2));
    }
}
