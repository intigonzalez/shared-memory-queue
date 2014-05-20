package netpipe;

import org.kevoree.microsandbox.javase.components.SharedMemoryChannelForConsumer2;
import org.kevoree.microsandbox.javase.components.SharedMemoryChannelForProducer2;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

/**
 * Created by inti on 13/05/14.
 */
public class SharedMemNetPipeMulticast extends NetPipeStrategy {

    private int consumers;
    private SharedMemoryChannelForConsumer2 rec;
    private SharedMemoryChannelForProducer2 tr;
    private boolean transmitter = false;

    public SharedMemNetPipeMulticast(int n) {
        if (n < 1)
            throw new RuntimeException("You have to connect to someone, otherwise what is the point");
        consumers = n;
    }

    @Override
    public void setupReceiver() {
        consumers = 1;
        try {
            rec = new SharedMemoryChannelForConsumer2("t2r");
            tr = new SharedMemoryChannelForProducer2("r2t", consumers);
        } catch (IOException e) {
            e.printStackTrace();
        }
        throw  new RuntimeException("I don't want to use this strategy in receivers");
    }

    @Override
    public void setupTransmitter() {
        transmitter = true;
        try {
            tr = new SharedMemoryChannelForProducer2("t2r", consumers);
            rec = new SharedMemoryChannelForConsumer2("r2t");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void send(ByteBuffer buffer) {
        while (buffer.hasRemaining()) {
//            if (buffer.remaining() > 1000) {
//                System.out.println();
//            }
            SharedMemoryChannelForProducer2.Pair p = tr.getBufferToSend();
            ByteBuffer bb = p.bb;
            //        System.out.println(p.bb.remaining()  + " " + buffer.remaining() + " " + buffer.limit());
            int tmp = bb.remaining();
            if (tmp < buffer.remaining()) {
//                System.out.printf("\nCrazy: %d %d\n", tmp, buffer.remaining());
                ByteBuffer from = buffer.slice();
                from.limit(tmp);
                bb.put(from);
                buffer.position(buffer.position() + tmp);
//                System.out.printf("\nCrazy: %d %d\n", bb.remaining(), buffer.remaining());
            } else
                bb.put(buffer);
//        System.out.println(p.bb.remaining()  + " " + buffer.remaining() + " " + buffer.limit());
            tr.notifySend(p);
        }
    }

    @Override
    public void receive(ByteBuffer buffer) {
            int total = buffer.remaining()*consumers;
            while (total > 0) {
                int id = rec.waitForMessage();
                // read the message
                ByteBuffer bb = rec.getByteBuffer(id);
                int step = bb.remaining();
                buffer.put(bb);
                // mark message as read
                rec.notifyReception(id);

                total -= (step);
                buffer.position(buffer.position() - step);
            }
    }
}
