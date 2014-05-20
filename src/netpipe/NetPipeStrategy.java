package netpipe;

import java.nio.ByteBuffer;

/**
 * Created by inti on 13/05/14.
 */
public abstract class NetPipeStrategy {
    public abstract void setupReceiver();
    public abstract void setupTransmitter();
    public abstract void send(ByteBuffer buffer);
    public abstract void receive(ByteBuffer buffer);
}
