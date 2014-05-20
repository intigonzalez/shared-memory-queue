package strategies.marshalling;

import java.io.*;

/**
 * Created by inti on 10/05/14.
 */
public class BuiltinMarshalling extends ForIpcMarshalling {

    @Override
    public void marshal(OutputStream stream, Object obj) {
        try {
            ObjectOutputStream s = new ObjectOutputStream(stream);
            s.writeObject(obj);
            s.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public Object unmarshal(InputStream stream) {
        try {
            ObjectInputStream s = new ObjectInputStream(stream);
            return s.readObject();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }
}
