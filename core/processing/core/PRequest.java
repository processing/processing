/** Part of the Mobile Processing project - http://mobile.processing.org
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
 */
package processing.core;

import java.io.*;
import javax.microedition.io.*;

/** The <b>PRequest</b> object represents an active network request. They are
 * returned by the methods in the <b>PClient</b> used to initiate network
 * requests.  A request object can be in one of the states specified by the
 * constant values below.  As the state of the request changes, library
 * events are fired to notify the sketch.
 *
 * @category Net
 * @related PClient
 *
 * @author Francis Li
 */
public class PRequest extends InputStream implements Runnable {
    /** Constant value representing that the request is being sent to the server, waiting for reply 
     * @thisref PRequest
     * @thisreftext the PRequest class
     */
    public static final int     STATE_OPENED        = 0;
    /** Constant value representing that the request has been received and a response is available.
     * @thisref PRequest
     * @thisreftext the PRequest class
     */
    public static final int     STATE_CONNECTED     = 1;
    /** Constant value representing that the response is being fetched from the server.
     * @thisref PRequest
     * @thisreftext the PRequest class
     */
    public static final int     STATE_FETCHING      = 2;
    /** Constant value representing that the entire response has been read.
     * @thisref PRequest
     * @thisreftext the PRequest class
     */
    public static final int     STATE_DONE          = 3;
    /** Constant value representing that an error occurred and the connection has been closed.
     * @thisref PRequest
     * @thisreftext the PRequest class
     */
    public static final int     STATE_ERROR         = 4;
    /** Constant value representing that connection has been closed and resources have been released.
     * @thisref PRequest
     * @thisreftext the PRequest class
     */
    public static final int     STATE_CLOSED        = 5;
    
    /** Event fired when the server has received the request and a response is available.
     *
     * @thisref PRequest
     * @thisreftext the PRequest class
     */
    public static final int     EVENT_CONNECTED     = 0;
    /** Event fired when the entire response has been read and is available.  The
     * data object will be an array of bytes (byte[]) containing the data.
     * 
     * @thisref PRequest
     * @thisreftext the PRequest class
     */
    public static final int     EVENT_DONE          = 1;
    /** Event fired when an error has occurred.  The data object will be a String
     * containing an error message.
     *
     * @thisref PRequest
     * @thisreftext the PRequest class
     */
    public static final int     EVENT_ERROR         = 2;
    
    protected PMIDlet           midlet;
    
    protected String            url;
    protected String            contentType;
    protected byte[]            bytes;

    protected HttpConnection    con;
    protected InputStream       is;
    
    protected String            authorization;
    
    /** The current state of the connection, as specified by the above constants
     * @thisref request
     * @thisreftext any variable of the type PRequest
     */
    public  int                 state;

    /** @hidden */
    public PRequest(PMIDlet midlet, String url, String contentType, byte[] bytes, String authorization) {
        this.midlet = midlet;
        this.url = url;
        this.contentType = contentType;
        this.bytes = bytes;
        this.authorization = authorization;
    }

    /** @hidden */
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
                if (authorization != null) {
                    con.setRequestProperty("Authorization", authorization);
                }
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
                synchronized (this) {
                    if (state == STATE_CONNECTED) {
                        state = STATE_FETCHING;
                    } else {
                        throw new Exception("Not connected.");
                    }
                }
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
                    if (state == STATE_FETCHING) {
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
                if ((state == STATE_CONNECTED) || (state == STATE_FETCHING)) {
                    notify = true;
                }
            }
            close();
            if (notify) {
                synchronized (this) {
                    state = STATE_ERROR;
                }
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
        
    /** Reads the next byte of data and returns it as an int.
     *
     * @thisref request
     * @thisreftext any variable of the type PRequest
     * @return int
     */
    public int read() {
        try {
            return is.read();
        } catch (IOException ioe) {
            throw new PException(ioe);
        }
    }
    
    /** Reads the next byte of data and returns it as a char.
     *
     * @thisref request
     * @thisreftext any variable of the type PRequest
     * @return char
     */
    public char readChar() {
        return (char) read();        
    }
        
    /** Reads the rest of the response from the server.  This method returns
     * immediately, and the download occurs in the background.  While it
     * is downloading, the request will be in <b>STATE_FETCHING</b>.  When it
     * is complete, the <b>EVENT_DONE</b> event will be fired back to the
     * sketch.
     *
     * @thisref request
     * @thisreftext any variable of the type PRequest
     * @return None
     */
    public void readBytes() {
        Thread t = new Thread(this);
        t.start();
    }

    /** Closes the connection and releases the resources associated with this request to the server.
     * 
     * @thisref request
     * @thisreftext any variable of the type PRequest
     * @return None
     */
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
