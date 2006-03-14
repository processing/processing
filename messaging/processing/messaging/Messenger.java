package processing.messaging;

import processing.core.*;

import java.io.*;
import javax.microedition.io.*;
import javax.wireless.messaging.*;

/**
 *
 * @author VG3327
 */
public class Messenger implements MessageListener {    
    public static final int     PORT                = 12345;
    
    public static final int     EVENT_MSG_RECEIVED  = 1;
    
    private PMIDlet             midlet;
    private int                 port;
    private MessageConnection   con;
    
    public Messenger(PMIDlet midlet, int port) {
        this.midlet = midlet;
        this.port = port;
        try {
            con = (MessageConnection) Connector.open("sms://:" + port);
            con.setMessageListener(this);
        } catch (IOException ioe) {
            throw new RuntimeException(ioe.getMessage());
        }
    }
    
    public Messenger(PMIDlet midlet) {
        this(midlet, PORT);
    }
    
    public void send(String to, String text) {
        TextMessage msg = (TextMessage) con.newMessage(MessageConnection.TEXT_MESSAGE, "sms://" + to + ":" + port);
        msg.setPayloadText(text);
        
        MessengerThread t = new MessengerThread(msg);
        t.start();
    }
    
    public void send(String to, byte[] data) {
        BinaryMessage msg = (BinaryMessage) con.newMessage(MessageConnection.BINARY_MESSAGE, "sms://" + to + ":" + port);
        msg.setPayloadData(data);
        
        MessengerThread t = new MessengerThread(msg);
        t.start();
    }

    public void notifyIncomingMessage(MessageConnection con) {
        MessengerThread t = new MessengerThread();
        t.start();
    }
    
    private class MessengerThread extends Thread {
        private javax.wireless.messaging.Message msg;
        
        public MessengerThread(javax.wireless.messaging.Message msg) {
            this.msg = msg;
        }
        
        public MessengerThread() {
            this.msg = null;
        }
        
        
        public void run() {        
            try {
                if (msg == null) {
                    Message msg = new Message(con.receive());
                    midlet.enqueueLibraryEvent(Messenger.this, EVENT_MSG_RECEIVED, msg);
                } else {
                    con.send(msg);
                }
            } catch (IOException ioe) {
                throw new RuntimeException(ioe.getMessage());
            }        
        }
    }
}
