package netpipe;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

/**
 * Created by inti on 13/05/14.
 */
public class TCPNetPipeMulticast extends NetPipeStrategy {
    private boolean transmitter = false;
    private SocketChannel [] channels;
    private int consumers = 1;

    public TCPNetPipeMulticast(int n) {
        if (n < 1)
            throw new RuntimeException("You have to connect to someone, otherwise what is the point");
        consumers = n;
        channels = new SocketChannel[consumers];
    }

    public TCPNetPipeMulticast() {
        consumers = 1;
        channels = new SocketChannel[consumers];
    }

    @Override
    public void setupReceiver() {
        consumers = 1;
        channels = new SocketChannel[consumers];
        try {
            channels[0] = SocketChannel.open(new InetSocketAddress("localhost", 45456));
            channels[0].configureBlocking(true);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void setupTransmitter() {
        transmitter = true;

        ServerSocketChannel serverSocketChannel = null;
        try {
            serverSocketChannel = ServerSocketChannel.open();
            serverSocketChannel.socket().bind(new InetSocketAddress(45456));
            int c = 0;
            while (c < consumers) {
                channels[c] = serverSocketChannel.accept();
                channels[c].configureBlocking(true);
                c++;
            }
            serverSocketChannel.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void send(ByteBuffer buffer) {
        try {
            int tmp = buffer.position();
            for (int i = 0 ; i < channels.length ; ++i) {
                buffer.position(tmp);
                SocketChannel channel = channels[i];
                channel.write(buffer);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void receive(ByteBuffer buffer) {
        try {
            int tmp = buffer.position();
            for (int i = 0 ; i < channels.length ; ++i) {
                buffer.position(tmp);
                SocketChannel channel = channels[i];
                while (buffer.hasRemaining()) {
                    channel.read(buffer);
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
            System.exit(0);
        }
    }
}
