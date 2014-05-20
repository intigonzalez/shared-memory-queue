package netpipe;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

/**
 * Created by inti on 13/05/14.
 */
public class TCPNetPipe extends NetPipeStrategy {
    private SocketChannel channel;
    @Override
    public void setupReceiver() {
        try {
            channel = SocketChannel.open(new InetSocketAddress("localhost", 45456));
            channel.configureBlocking(true);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void setupTransmitter() {
        ServerSocketChannel serverSocketChannel = null;
        try {
            serverSocketChannel = ServerSocketChannel.open();
            serverSocketChannel.socket().bind(new InetSocketAddress(45456));
            channel = serverSocketChannel.accept();
            channel.configureBlocking(true);
            serverSocketChannel.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void send(ByteBuffer buffer) {
        try {
            channel.write(buffer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void receive(ByteBuffer buffer) {
        try {
            while (buffer.hasRemaining()) {
                channel.read(buffer);
            }
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(0);
        }
    }
}
