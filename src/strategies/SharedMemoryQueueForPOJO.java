package strategies;

import org.kevoree.microsandbox.javase.components.SharedMemoryChannelForConsumer2;
import org.kevoree.microsandbox.javase.components.SharedMemoryChannelForProducer2;
import strategies.pojo.Mesh;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;

/**
 * Created by inti on 10/05/14.
 */
public class SharedMemoryQueueForPOJO extends IPCForPOJO {
    public SharedMemoryQueueForPOJO(int message_count) {
        super(message_count);
    }

    @Override
    public void producer(int nbConsumers) {
        try {
            long elapsedTime = 0;
            SharedMemoryChannelForProducer2 channel =
                            new SharedMemoryChannelForProducer2("testing", nbConsumers);
            SharedMemoryChannelForConsumer2 channel2 =
                    new SharedMemoryChannelForConsumer2("testing2");
            Mesh mesh = createMesh();
            for (int i = 0 ; i < message_count ; ++i) {
                // write the message
                long before = System.nanoTime();
                OutputStream outputStream = channel.getStreamToSendMessage();
                marshal(mesh, outputStream);
                // mark message as written
                channel.notifySend(outputStream);

                for (int j = 0; j < nbConsumers; j++) {
                    int id = channel2.waitForMessage();
                    channel2.getByteBuffer(id).getInt();
                    channel2.notifyReception(id);
                }
                long after = System.nanoTime();
                elapsedTime += (after - before) / 2;
            }
            System.out.printf("Total time doing stuffs %f\n", (double)(elapsedTime)/1000000000.0);
            printSerializationTime();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void consumer() {
        try {
            SharedMemoryChannelForConsumer2 channel =
                    new SharedMemoryChannelForConsumer2("testing");
            SharedMemoryChannelForProducer2 channel2 =
                    new SharedMemoryChannelForProducer2("testing2", 1);
            for (int i = 0 ; i < message_count ; ++i) {
                // wait for message
                int id = channel.waitForMessage();
                // read the message
                Mesh mesh = (Mesh)unmarshal(channel.getMessageInputStream(id));
                // mark message as read
                channel.notifyReception(id);
                // send reply
                SharedMemoryChannelForProducer2.Pair p = channel2.getBufferToSend();
                p.bb.putInt(110);
                channel2.notifySend(p);
            }
//            printSerializationTime();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
