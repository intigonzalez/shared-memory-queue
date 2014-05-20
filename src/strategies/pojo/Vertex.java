package strategies.pojo;

import java.io.Serializable;

/**
* Created by inti on 10/05/14.
*/
public class Vertex implements Serializable {
    public double x,y,z;

    public Vertex() {

    }

    public Vertex(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }
}
