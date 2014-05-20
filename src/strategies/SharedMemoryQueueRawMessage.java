package strategies;

import org.kevoree.microsandbox.javase.components.SharedMemoryChannelForConsumer2;
import org.kevoree.microsandbox.javase.components.SharedMemoryChannelForProducer2;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Created by inti on 08/05/14.
 */
public class SharedMemoryQueueRawMessage extends IPCForRawMessage {
    public SharedMemoryQueueRawMessage(int message_count, int message_size) {
        super(message_count, message_size);
    }

    @Override
    public void producer(int nbConsumers) {
        SharedMemoryChannelForProducer2 channel =
                null;
        try {
            channel = new SharedMemoryChannelForProducer2("testing", nbConsumers);
            for (int i = 0 ; i < message_count ; ++i) {
                // write the message
                SharedMemoryChannelForProducer2.Pair pair = channel.getBufferToSend();
                ByteBuffer bb = pair.bb;
                for (int j = 0 ; j < message_size; ++j)
                    bb.putInt(i);
                // mark message as written
                channel.notifySend(pair);
            }

            printExpectedResult();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void consumer() {
        // the consumer
        try {
            SharedMemoryChannelForConsumer2 channel =
                    new SharedMemoryChannelForConsumer2("testing");
            int sum = 0;
            for (int i = 0 ; i < message_count ; ++i) {
                // wait for message
                int id = channel.waitForMessage();
                // read the message
                ByteBuffer bb = channel.getByteBuffer(id);
                for (int j = 0 ; j < message_size; ++j)
                    sum += bb.getInt();
                // mark message as read
                channel.notifyReception(id);
            }
            printObservedSum(sum);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

}
