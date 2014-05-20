package netpipe;

import org.kevoree.microsandbox.javase.components.SharedMemoryChannelForConsumer2;
import org.kevoree.microsandbox.javase.components.SharedMemoryChannelForProducer2;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Created by inti on 13/05/14.
 */
public class SharedMemNetPipe extends NetPipeStrategy {

    private SharedMemoryChannelForConsumer2 rec;
    private SharedMemoryChannelForProducer2 tr;

    @Override
    public void setupReceiver() {
        try {
            rec = new SharedMemoryChannelForConsumer2("t2r");
            tr = new SharedMemoryChannelForProducer2("r2t", 1);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void setupTransmitter() {
        try {
            tr = new SharedMemoryChannelForProducer2("t2r", 1);
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
            }
            else
                bb.put(buffer);
//        System.out.println(p.bb.remaining()  + " " + buffer.remaining() + " " + buffer.limit());
            tr.notifySend(p);
        }
    }

    @Override
    public void receive(ByteBuffer buffer) {
        while (buffer.hasRemaining()) {
//            if (buffer.remaining() > 1000) {
//                System.out.println();
//            }
            int id = rec.waitForMessage();
            // read the message

            ByteBuffer bb = rec.getByteBuffer(id);
//        System.out.println(bb.remaining()  + " " + buffer.remaining() + " " + buffer.limit());
            buffer.put(bb);
//            if (buffer.hasRemaining()) {
//                System.out.println("Remaining " + buffer.remaining());
//            }
            // mark message as read
            rec.notifyReception(id);
        }
    }
}
