package strategies;

import org.newsclub.net.unix.AFUNIXServerSocket;
import org.newsclub.net.unix.AFUNIXSocket;
import org.newsclub.net.unix.AFUNIXSocketAddress;
import org.newsclub.net.unix.AFUNIXSocketException;
import strategies.pojo.Mesh;

import java.io.*;
import java.net.Socket;
import java.nio.ByteBuffer;

/**
 * Created by inti on 09/05/14.
 */
public class UnixSocketForPOJO extends IPCForPOJO {
    public UnixSocketForPOJO(int message_count) {
        super(message_count);
    }
    private class SenderThread extends Thread {
        Socket[] socks;
        OutputStream[] outputStreams;
        int n;
        SenderThread(int c) {
            socks = new Socket[c];
            outputStreams = new OutputStream[c];
            n = 0;
        }

        void add(Socket s) {
            socks[n] = s;
            try {
                outputStreams[n++] = s.getOutputStream();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void run() {
            try {
                Mesh mesh = createMesh();
                ByteBuffer bb = ByteBuffer.allocate(6000);
                ByteBufferBackedOutputStream buffer = new ByteBufferBackedOutputStream(bb);
                for (int i = 0 ; i < message_count; ++i) {
                    // serialize
                    long before = System.nanoTime();
                    ObjectOutputStream objectOutputStream = new ObjectOutputStream(buffer);
                    objectOutputStream.writeObject(mesh);
                    long after = System.nanoTime();
                    serializationTime += (after - before);

                    int p = bb.position();
                    bb.position(0);
                    for (int k = 0 ; k < n ;k ++) {
                        outputStreams[k].write(bb.array(),0,p);
                        outputStreams[k].flush();
                    }
                }

                for (int k = 0 ; k < n ;k ++)
                    socks[k].close();
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
    };

    @Override
    public void producer(int nbConsumers) {
        try {
            final File socketFile = new File(new File(System
                    .getProperty("java.io.tmpdir")), "junixsocket-test.sock");

            AFUNIXServerSocket server = AFUNIXServerSocket.newInstance();
            server.bind(new AFUNIXSocketAddress(socketFile));

            SenderThread thread = new SenderThread(nbConsumers);
            int c = nbConsumers;
            while (c > 0) {
                c--;
                Socket socket = server.accept();
                thread.add(socket);
            }
            thread.start();


            server.close();
            thread.join();

        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void consumer() {
        try {
            int sum = 0;
            final File socketFile = new File(new File(System
                    .getProperty("java.io.tmpdir")), "junixsocket-test.sock");

            AFUNIXSocket sock = AFUNIXSocket.newInstance();
            try {
                sock.connect(new AFUNIXSocketAddress(socketFile));
            } catch (AFUNIXSocketException e) {
                System.out.println("Cannot connect to server. Have you started it?");
                System.out.flush();
                throw e;
            }
            InputStream inputStream = sock.getInputStream();

//            ByteBuffer bb = ByteBuffer.allocate(8000);
//            ByteBufferBackedInputStream buffer = new ByteBufferBackedInputStream(bb);

            for (int i = 0 ; i < message_count; ++i) {
                ObjectInputStream objectInputStream = new ObjectInputStream(inputStream);
                Mesh mesh = (Mesh)objectInputStream.readObject();
            }
            sock.close();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }
}
