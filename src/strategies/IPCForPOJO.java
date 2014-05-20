package strategies;

import strategies.marshalling.BuiltinMarshalling;
import strategies.marshalling.FSTMarshalling;
import strategies.marshalling.ForIpcMarshalling;
import strategies.pojo.Mesh;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;

/**
 * Created by inti on 09/05/14.
 */
public abstract class IPCForPOJO extends IPCStrategy {
    protected ForIpcMarshalling marshalling = new FSTMarshalling();
    protected IPCForPOJO(int message_count) {
        super(message_count);
    }

    protected void printSerializationTime() {
        System.out.printf("(de)Serialization time %f\n", serializationTime / 1000000000.0);
    }

    protected Object unmarshal(InputStream inputStream) {
        long before = System.nanoTime();
        Object r = marshalling.unmarshal(inputStream);
        long after = System.nanoTime();
        serializationTime += (after - before);
        return r;
    }

    protected void marshal(Mesh mesh, OutputStream buffer) {
        long before = System.nanoTime();
        marshalling.marshal(buffer, mesh);
        long after = System.nanoTime();
        serializationTime += (after - before);
    }

    public void setSerializer(ForIpcMarshalling serializer) {
        this.marshalling = serializer;
    }

    protected class ByteBufferBackedOutputStream extends OutputStream {
        ByteBuffer buf;

        public ByteBufferBackedOutputStream(ByteBuffer buf) {
            this.buf = buf;
        }

        public void write(int b) throws IOException {
            buf.put((byte) b);
        }

        public void write(byte[] bytes, int off, int len)
                throws IOException {
            buf.put(bytes, off, len);
        }
    }

    protected Mesh createMesh() {
        Mesh mesh = new Mesh();
        for (int i = 0 ; i < 50 ; ++i) {
            mesh.addVertex(i*3, i* 5, i*7);
        }
        for (int i = 0 ; i < 100 ; ++i) {
            mesh.addTriangle(i/3, i/ 5, i/ 2);
        }
        return mesh;
    }

    public long getSerializationTime() {
        return serializationTime;
    }

    protected long serializationTime = 0;
}
