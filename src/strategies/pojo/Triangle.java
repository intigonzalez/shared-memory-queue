package strategies.pojo;

import java.io.Serializable;

/**
* Created by inti on 10/05/14.
*/
public class Triangle implements Serializable {
    public int v0,v1,v2;

    public Triangle() {

    }

    public Triangle(int v0, int v1, int v2) {
        this.v0 = v0;
        this.v1 = v1;
        this.v2 = v2;
    }
}
