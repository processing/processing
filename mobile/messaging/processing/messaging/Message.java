package processing.messaging;

import javax.wireless.messaging.*;
import java.util.Date;

/** A wrapper around the WMA Message class for convenience.
 *
 * @author Francis Li
 */
public class Message {
    private javax.wireless.messaging.Message msg;
    
    public final String address;
    public final Date   timestamp;
    
    protected Message(javax.wireless.messaging.Message msg) {
        this.msg = msg;
        address = msg.getAddress();
        timestamp = msg.getTimestamp();
    }
    
    public byte[] readBytes() {
        return ((BinaryMessage) msg).getPayloadData();
    }
    
    public String readString() {
        return ((TextMessage) msg).getPayloadText();
    }
}
