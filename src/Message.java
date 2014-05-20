import java.io.Serializable;

/**
 * Created with IntelliJ IDEA.
 * User: inti
 * Date: 1/17/14
 * Time: 2:04 AM
 * To change this template use File | Settings | File Templates.
 */
public class Message implements Serializable {
    public Object object;

    @Override
    public String toString() {
        return "Message{" +
                "object=" + object.toString() +
                '}';
    }
}
