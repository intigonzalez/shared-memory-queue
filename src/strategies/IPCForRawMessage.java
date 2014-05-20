package strategies;

/**
 * Created by inti on 08/05/14.
 */
public abstract class IPCForRawMessage extends IPCStrategy {

    protected int message_size;

    protected IPCForRawMessage(int message_count, int message_size) {
        super(message_count);
        this.message_size = message_size;
    }

    protected void printExpectedResult() {
//        int sum = 0;
//        for (int i = 0 ; i < message_count ; ++i)
//            sum += i * message_size;
//        System.out.printf("The result must be: %d\n", sum);
    }

    protected void printObservedSum(int sum) {
//        System.out.printf("The sum is: %d\n", sum);
    }
}
