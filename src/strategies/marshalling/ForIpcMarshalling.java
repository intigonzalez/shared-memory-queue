package strategies.marshalling;

import java.io.InputStream;
import java.io.OutputStream;

/**
 * Created by inti on 10/05/14.
 */
public abstract class ForIpcMarshalling {
    public abstract void marshal(OutputStream stream, Object obj);
    public abstract Object unmarshal(InputStream stream);
}
