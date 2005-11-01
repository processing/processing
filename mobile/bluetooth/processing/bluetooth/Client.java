package processing.bluetooth;

import java.io.*;
import javax.microedition.io.*;

/**
 *
 * @author Francis Li
 */
public class Client {
    private StreamConnection    con;
    private DataInputStream     is;
    private DataOutputStream    os;
    
    protected Client(StreamConnection con) throws IOException {
        this.con = con;
        is = con.openDataInputStream();
        os = con.openDataOutputStream();
    }
    
    public void stop() {
        try {
            os.close();
        } catch (IOException ioe) {
        }
        try {
            is.close();
        } catch (IOException ioe) {
        }
        try {
            con.close();
        } catch (IOException ioe) {
        }
    }
    
    public int read() {
        try {
            return is.read();
        } catch (IOException ioe) {
            throw new RuntimeException(ioe.getMessage());
        }
    }

    public boolean readBoolean() {
        try {
            return is.readBoolean();
        } catch (IOException ioe) {
            throw new RuntimeException(ioe.getMessage());
        }
    }
    
    public char readChar() {
        try {
            return is.readChar();
        } catch (IOException ioe) {
            throw new RuntimeException(ioe.getMessage());
        }
    }

    public void readBytes(byte[] b) {
        readBytes(b, 0, b.length);
    }

    public void readBytes(byte[] b, int offset, int length) {
        try {
            is.readFully(b, offset, length);
        } catch (IOException ioe) {
            throw new RuntimeException(ioe.getMessage());
        }
    }

    public int readInt() {
        try {
            return is.readInt();
        } catch (IOException ioe) {
            throw new RuntimeException(ioe.getMessage());
        }
    }

    public String readUTF() {
        try {
            return is.readUTF();
        } catch (IOException ioe) {
            throw new RuntimeException(ioe.getMessage());
        }
    }
    
    public int skipBytes(int bytes) {
        try {
            return is.skipBytes(bytes);
        } catch (IOException ioe) {
            throw new RuntimeException(ioe.getMessage());
        }
    }
    
    public void write(int data) {
        try {
            os.write(data);
        } catch (IOException ioe) {
            throw new RuntimeException(ioe.getMessage());
        }
    }
    
    public void write(byte[] data) {
        try {
            os.write(data);
        } catch (IOException ioe) {
            throw new RuntimeException(ioe.getMessage());
        }
    }

    public void writeBoolean(boolean v) {
        try {
            os.writeBoolean(v);
        } catch (IOException ioe) {
            throw new RuntimeException(ioe.getMessage());
        }
    }

    public void writeBytes(String s) {
        try {
            os.write(s.getBytes());
        } catch (IOException ioe) {
            throw new RuntimeException(ioe.getMessage());
        }
    }

    public void writeChar(int v) {
        try {
            os.writeChar(v);
        } catch (IOException ioe) {
            throw new RuntimeException(ioe.getMessage());
        }
    }
    
    public void writeInt(int v) {
        try {
            os.writeInt(v);
        } catch (IOException ioe) {
            throw new RuntimeException(ioe.getMessage());
        }
    }
    
    public void writeUTF(String s) {
        try {
            os.writeUTF(s);
        } catch (IOException ioe) {
            throw new RuntimeException(ioe.getMessage());
        }
    }
}
