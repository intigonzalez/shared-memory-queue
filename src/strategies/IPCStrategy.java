package strategies;

/**
 * Created by inti on 08/05/14.
 */
public abstract class IPCStrategy {
    protected int message_count;

    protected IPCStrategy(int message_count) {
        this.message_count = message_count;
    }

    public abstract void producer(int nbConsumers);

    public abstract void consumer();
}
