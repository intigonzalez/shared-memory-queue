package strategies;

import org.newsclub.net.unix.AFUNIXServerSocket;
import org.newsclub.net.unix.AFUNIXSocket;
import org.newsclub.net.unix.AFUNIXSocketAddress;
import org.newsclub.net.unix.AFUNIXSocketException;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

/**
 * Created by inti on 08/05/14.
 */
public class UnixSocketsRawMessage extends IPCForRawMessage {

    public UnixSocketsRawMessage(int message_count, int message_size) {
        super(message_count, message_size);
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
                int bufferSize = message_size << 2;
                byte[] buffer = new byte[bufferSize];
                ByteBuffer bb = ByteBuffer.wrap(buffer);

                for (int i = 0 ; i < message_count; ++i) {
                    bb.position(0);
                    for (int j = 0 ; j < message_size; ++j)
                        bb.putInt(i);
                    for (int k = 0 ; k < n ;k ++) {
                        outputStreams[k].write(buffer);
                    }
                }

                printExpectedResult();
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

        } catch (IOException e) {
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

            int bufferSize = message_size << 2;
            byte[] buffer = new byte[bufferSize];
            ByteBuffer bb = ByteBuffer.wrap(buffer);

            for (int i = 0 ; i < message_count; ++i) {
                int off = 0;
                int left = bufferSize;
                while (left > 0) {
                   int tmp = inputStream.read(buffer, off, left);
                    left -= tmp;
                    off += tmp;
                }
                bb.position(0);
                for (int j = 0 ; j < message_size; ++j) {
                    sum += bb.getInt();
                }

            }
            sock.close();
            printObservedSum(sum);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
