package strategies;

import strategies.pojo.Mesh;
import strategies.pojo.Vertex;

import java.io.*;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

/**
 * Created by inti on 09/05/14.
 */
public class NIOForPOJO extends IPCForPOJO {

    private static final int bufferSize = 4096;//1855;

    public NIOForPOJO(int message_count) {
        super(message_count);
    }

    private class SenderThread extends Thread {
        SocketChannel[] channels;
        int n = 0;
        SenderThread(int c) {
            channels = new SocketChannel[c];
        }

        void add(SocketChannel channel) {
            channels[n++] = channel;
            try {
                channel.configureBlocking(true);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void run() {
            try {
                long elapsedTime = 0;
                Mesh mesh = createMesh();
                ByteBuffer bb = ByteBuffer.allocateDirect(bufferSize);
                ByteBufferBackedOutputStream buffer = new ByteBufferBackedOutputStream(bb);
                ByteBuffer reply = ByteBuffer.allocate(4);
                for (int i = 0 ; i < message_count; ++i) {
                    // serialize
                    long before = System.nanoTime();
                    buffer.buf.position(4);
                    marshal(mesh, buffer);

                    int total = buffer.buf.position();
//                    if (total != 1855)
//                        System.out.println("dfsdfdfs" + total);
                    buffer.buf.putInt(0, total);
                    buffer.buf.limit(total);
                    for (int k = 0 ; k < n ;k ++) {
                        bb.position(0);
                        channels[k].write(buffer.buf);
                    }
                    buffer.buf.limit(bufferSize);

                    // wait for all the replies
                    for (int j = 0; j < n; j++) {
                        reply.position(0);
                        while (reply.hasRemaining()) {
                            channels[j].read(reply);
                        }
                    }
                    long after = System.nanoTime();
                    elapsedTime += (after - before) / 2;
                }

                for (int k = 0 ; k < n ;k ++)
                    channels[k].close();

                System.out.printf("Total time doing stuffs %f\n", (double)(elapsedTime)/1000000000.0);
                printSerializationTime();
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
    }

    @Override
    public void producer(int nbConsumers) {
        try {
            ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
            serverSocketChannel.socket().bind(new InetSocketAddress(45456));

            SenderThread thread = new SenderThread(nbConsumers);
            int c = nbConsumers;
            while (c > 0) {
                c--;
                SocketChannel socket = serverSocketChannel.accept();
                thread.add(socket);
            }
            thread.start();
            serverSocketChannel.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void consumer() {
        try {
            SocketChannel socketChannel = SocketChannel.open(new InetSocketAddress("localhost", 45456));
            socketChannel.configureBlocking(true);
            SocketChannelBackedInputStream inputStream = new SocketChannelBackedInputStream(socketChannel);
            ByteBuffer forReply = ByteBuffer.allocate(4);
            for (int i = 0 ; i < message_count; ++i) {
                //System.out.println(i);
                Mesh mesh = (Mesh)unmarshal(inputStream);
                inputStream.close();
                //System.out.println(i);
                forReply.position(0);
                forReply.putInt(0,110);
                socketChannel.write(forReply);
            }
            socketChannel.close();
//            printSerializationTime();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    static class SocketChannelBackedInputStream extends InputStream {

        SocketChannel channel;
        ByteBuffer buffer;
        int r;
        boolean initialized = false;

        SocketChannelBackedInputStream(SocketChannel channel) {
            this.channel = channel;
            buffer = ByteBuffer.allocateDirect(4096/*bufferSize*/);
//            buffer.position(buffer.limit());
//            r = bufferSize;
        }

        @Override
        public int read() throws IOException {
            if (!initialized) {
                 // read the length of a packet
                buffer.position(0);
                while (buffer.position() < 4) {
                    channel.read(buffer);
                }
                int t = buffer.getInt(0); // get the length
                try {
                    buffer.limit(t);
                }
                catch (Exception e) {
                    System.out.printf("%d\n", t);
                    System.exit(12);
                }
                while (buffer.hasRemaining())
                    r = channel.read(buffer); // read packet
                buffer.position(4);
                initialized = true;
            }
            if (!buffer.hasRemaining())
                return -1;
            return buffer.get() & 0xff;
//            if (r == 0)
//                return  -1;
//            if (!buffer.hasRemaining()) {
//                buffer.position(0);
//                while (buffer.position() < buffer.limit())
//                    channel.read(buffer);
//                buffer.position(0);
//            }
//            r --;
//            return buffer.get() & 0xff;
        }

        @Override
        public int read(byte[] b, int off, int desired) throws IOException {
            if (!initialized) {
                // read the length of a packet
                buffer.position(0);
                while (buffer.position() < 4) {
                    channel.read(buffer);
                }
                int t = buffer.getInt(0); // get the length
                try {
                    buffer.limit(t);
                }
                catch (Exception e) {
                    System.out.printf("%d\n", t);
                    System.exit(12);
                }
                while (buffer.hasRemaining())
                    r = channel.read(buffer); // read packet
                buffer.position(4);
                initialized = true;
            }

//            if (!buffer.hasRemaining()) {
//                buffer.position(0);
//                while (buffer.position() < buffer.limit())
//                    channel.read(buffer);
//                buffer.position(0);
//            }
            int available = buffer.remaining();
            if (available == 0) {
                return  -1;
            }
            int toRead = Math.min(desired, available);
            buffer.get(b, off, toRead);
            r -= toRead;
            if (desired > available) {
                int tmp = read(b, off + toRead, desired - toRead);
                if (tmp == -1) {
                    tmp = 0;
                }
                toRead += tmp;
            }
            return toRead;
        }

        @Override
        public void close() throws IOException {
            buffer.position(buffer.limit());
            r = bufferSize;
            initialized = false;
        }
    }
}
