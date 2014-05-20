package strategies.marshalling;

import de.ruedigermoeller.serialization.FSTConfiguration;
import de.ruedigermoeller.serialization.FSTObjectInput;
import de.ruedigermoeller.serialization.FSTObjectOutput;
import strategies.pojo.Mesh;
import strategies.pojo.Triangle;
import strategies.pojo.Vertex;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Created by inti on 10/05/14.
 */
public class FSTMarshalling extends ForIpcMarshalling {
    static FSTConfiguration conf = FSTConfiguration.createDefaultConfiguration();

    {
//        conf.setPreferSpeed(true);
//        conf.registerClass(Vertex.class, Triangle.class, Mesh.class);
//        conf.setShareReferences(false);
    }

    @Override
    public void marshal(OutputStream stream, Object obj) {
        FSTObjectOutput out = conf.getObjectOutput(stream);
        try {
            out.writeObject( obj, Mesh.class );
            out.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public Object unmarshal(InputStream stream) {
        FSTObjectInput in = conf.getObjectInput(stream);
        try {
            Object obj = in.readObject(Mesh.class);
//            stream.close();
            return obj;
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
