package processing.core;

import java.io.*;
import javax.microedition.io.*;

/** Represents an active network request.
 *
 * Part of the Mobile Processing project - http://mobile.processing.org
 *
 * Copyright (c) 2004-07 Francis Li
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General
 * Public License along with this library; if not, write to the
 * Free Software Foundation, Inc., 59 Temple Place, Suite 330,
 * Boston, MA  02111-1307  USA
 *
 * @author Francis Li
 */
public class PRequest extends InputStream implements Runnable {        
    public static final int STATE_OPENED        = 0;
    public static final int STATE_CONNECTED     = 1;
    public static final int STATE_DONE          = 2;
    public static final int STATE_ERROR         = 3;
    public static final int STATE_CLOSED        = 4;
    
    public static final int EVENT_CONNECTED     = 0;
    public static final int EVENT_DONE          = 1;
    public static final int EVENT_ERROR         = 2;
    
    private PMIDlet         midlet;
    
    private String          url;
    private String          contentType;
    private byte[]          bytes;

    private HttpConnection  con;
    private InputStream     is;
    
    public  int             state;

    public PRequest(PMIDlet midlet, String url, String contentType, byte[] bytes) {
        this.midlet = midlet;
        this.url = url;
        this.contentType = contentType;
        this.bytes = bytes;
    }               

    public void run() {
        OutputStream os = null;
        try {
            if (is == null) {
                //// open connection to server
                con = (HttpConnection) Connector.open(url);
                if (contentType != null) {
                    con.setRequestMethod(HttpConnection.POST);
                    con.setRequestProperty("Content-Type", contentType);
                } else {
                    con.setRequestMethod(HttpConnection.GET);
                }
                con.setRequestProperty("Connection", "close");
                if (bytes != null) {
                    os = con.openOutputStream();
                    os.write(bytes);
                    //// we can release the request bytes and reuse the reference
                    bytes = null;
                }
                //// now, open inputstream, committing the post
                is = con.openInputStream();
                //// done, notify midlet
                boolean notify = false;
                synchronized (this) {
                    if (state == STATE_OPENED) {
                        state = STATE_CONNECTED;
                        notify = true;
                    }
                }
                if (notify) {
                    midlet.enqueueLibraryEvent(this, EVENT_CONNECTED, null);
                }
            } else {
                //// read the response
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                byte[] buffer = new byte[1024];
                int bytesRead = is.read(buffer);
                while (bytesRead >= 0) {
                    baos.write(buffer, 0, bytesRead);
                    bytesRead = is.read(buffer);
                }
                buffer = null;
                buffer = baos.toByteArray();
                //// done, notify midlet
                boolean notify = false;
                synchronized (this) {
                    if (state == STATE_CONNECTED) {
                        state = STATE_DONE;
                        notify = true;
                    }
                }
                if (notify) {
                    midlet.enqueueLibraryEvent(this, EVENT_DONE, buffer);
                }
            }
        } catch (Exception e) {
            boolean notify = false;
            synchronized (this) {
                if (state == STATE_CONNECTED) {
                    state = STATE_ERROR;
                    notify = true;
                }
            }
            if (notify) {
                midlet.enqueueLibraryEvent(this, EVENT_ERROR, e.getMessage());
            }
        } finally {
            if (os != null) {
                try {
                    os.close();
                } catch (Exception e) { }
                os = null;
            }            
        }
    }
        
    public int read() {
        try {
            return is.read();
        } catch (IOException ioe) {
            throw new PException(ioe);
        }
    }
    
    public char readChar() {
        return (char) read();        
    }
        
    public void readBytes() {
        Thread t = new Thread(this);
        t.start();
    }

    public void close() {
        synchronized (this) {
            state = STATE_CLOSED;
        }
        if (is != null) {
            try {
                is.close();
            } catch (IOException ioe) { }
            is = null;
        }
        if (con != null) {
            try {
                con.close();
            } catch (IOException ioe) { }
            con = null;
        }
    }
}
