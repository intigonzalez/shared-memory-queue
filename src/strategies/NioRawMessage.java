package strategies;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

/**
 * Created by inti on 08/05/14.
 */
public class NioRawMessage extends IPCForRawMessage {

    public NioRawMessage(int message_count, int message_size) {
        super(message_count, message_size);
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
                int bufferSize = message_size << 2;
                ByteBuffer bb = ByteBuffer.allocateDirect(bufferSize);
                for (int i = 0 ; i < message_count; ++i) {
                    bb.position(0);
                    for (int j = 0 ; j < message_size; ++j)
                        bb.putInt(i);
                    for (int k = 0 ; k < n ;k ++) {
                        bb.position(0);
                        channels[k].write(bb);
                    }
                }

                printExpectedResult();
                for (int k = 0 ; k < n ;k ++)
                    channels[k].close();
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
    };

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
            int sum = 0;
            SocketChannel socketChannel = SocketChannel.open(new InetSocketAddress("localhost", 45456));
            socketChannel.configureBlocking(true);

            int bufferSize = message_size << 2;
            ByteBuffer bb = ByteBuffer.allocateDirect(bufferSize);

            for (int i = 0 ; i < message_count; ++i) {
                bb.position(0);
                while (bb.position() < bufferSize)
                    socketChannel.read(bb);
                bb.position(0);
                for (int j = 0 ; j < message_size; ++j) {
                    sum += bb.getInt();
                }

            }
            socketChannel.close();
            printObservedSum(sum);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
