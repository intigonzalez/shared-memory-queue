import jfs.net.FastSocketImplFactory;
import org.kevoree.microsandbox.javase.components.SharedMemoryChannelForConsumer2;
import org.kevoree.microsandbox.javase.components.SharedMemoryChannelForProducer2;

import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

/**
 * Created with IntelliJ IDEA.
 * User: inti
 * Date: 1/15/14
 * Time: 1:36 PM
 *
 */
public class Main extends BaseMain {

    public static void main(String[] arg) {
        execute(new Main(), arg);
    }

    static class SocketBackedOutputStream extends OutputStream {

        ByteBuffer current;
        SocketChannel channel;

        SocketBackedOutputStream(SocketChannel channel) {
            this.channel = channel;
            current = ByteBuffer.allocate(4096);
        }

        @Override
        public void write(int b) throws IOException {
            if (!current.hasRemaining()) {
                current.position(0);
                channel.write(current);
                current.position(0);
            }
            current.put((byte)b);
        }


        public void writeLast() throws IOException {
            if (current.position() > 0) {
                int l = current.position();
                current.limit(l);
                current.position(0);
                channel.write(current);
            }
        }

        @Override
        public void write(byte[] b) throws IOException {
            int n = b.length;
            int off = 0;
            if (n >= current.remaining())
            {
                int tmp = current.remaining();
                n = n - tmp;
                current.put(b, 0, tmp);
                off = tmp;
                current.position(0);
                channel.write(current);
                current.position(0);
            }
            current.put(b, off, n);
        }
    }

    static class SocketChannelBackedInputStream extends InputStream {

        SocketChannel channel;
        ByteBuffer buffer;
        int r = 0;

        SocketChannelBackedInputStream(SocketChannel channel) {
            this.channel = channel;
            buffer = ByteBuffer.allocate(4096);
        }

        @Override
        public int read() throws IOException {
            if (r == 0) {
                buffer.position(0);
                r += channel.read(buffer);
                buffer.position(0);
            }
            int x = buffer.get();
            r--;
            if (x < 0)
                x+=256;
            return x;
        }
    }
}
