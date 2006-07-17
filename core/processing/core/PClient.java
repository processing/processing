package processing.core;

import java.io.*;
import javax.microedition.io.*;

/**
 * Part of the Mobile Processing project - http://mobile.processing.org
 *
 * Copyright (c) 2004-05 Francis Li
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
 * @author  Francis Li
 */
public class PClient extends InputStream implements Runnable {
    public static final int EVENT_CONNECTED     = 0;
    public static final int EVENT_ERROR         = 1;
    
    private PMIDlet         midlet;
    
    private String          server;
    private int             port;
    
    private HttpConnection  con;
    private InputStream     is;
    private String          url;
    private String          contentType;
    private byte[]          request;
    
    public PClient() {
        
    }
    
    public PClient(String server) {
        this(server, 80);
    }
    
    public PClient(String server, int port) {
        this.server = server;
        this.port = port;
    }
    
    public PClient(PMIDlet midlet, String server) {
        this(midlet, server, 80);
    }
    
    public PClient(PMIDlet midlet, String server, int port) {
        this.midlet = midlet;
        this.server = server;
        this.port = port;
    }
    
    /** Minimal URL encoding implementation */
    public static String encode(String str) {
        StringBuffer encoded = new StringBuffer();
        char c;
        for (int i = 0, length = str.length(); i < length; i++) {
            c = str.charAt(i);
            if (Character.isDigit(c) || Character.isLowerCase(c) || Character.isUpperCase(c)) {
                encoded.append(c);
            } else if (c == ' ') {
                encoded.append('+');
            } else {
                encoded.append('%');
                if (c < 16) {
                    encoded.append('0');
                }
                encoded.append(Integer.toHexString(c));
            }
        }
        return encoded.toString();
    }
    
    public boolean POST(String file, String[] params, String[] values) {
        url = "http://" + server + ((port != 80) ? ":" + port : "") + file;
        contentType = "application/x-www-form-urlencoded";
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream ps = new PrintStream(baos);
        for (int i = 0, length = params.length; i < length; i++) {
            if (i > 0) {
                ps.print("&");
            }
            ps.print(encode(params[i]));
            ps.print("=");
            ps.print(encode(values[i]));             
        }
        request = baos.toByteArray();
        return start();
    }
    
    public boolean POST(String file, String[] params, Object[] values) {
        url = "http://" + server + ((port != 80) ? ":" + port : "") + file;
        contentType = "multipart/form-data; boundary=BOUNDARY_185629";
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream ps = new PrintStream(baos);
        try {
            for (int i = 0, length = params.length; i < length; i++) {
                ps.print("--BOUNDARY_185629\r\n");
                ps.print("Content-Disposition: form-data; name=\"");
                ps.print(params[i]);
                if (values[i] instanceof String) {
                    ps.print("\"\r\n");
                    ps.print("\r\n");
                    ps.print((String) values[i]);
                    ps.print("\r\n");
                } else if (values[i] instanceof byte[]) {
                    byte[] buffer = (byte[]) values[i];
                    ps.print("\"; filename=\"");
                    ps.print(params[i]);
                    ps.print("\"\r\n");
                    ps.print("Content-Type: application/octet-stream\r\n");
                    ps.print("Content-Transfer-Encoding: binary\r\n\r\n");
                    ps.write(buffer);
                    ps.print("\r\n");
                }                
            }
            ps.print("--BOUNDARY_185629--\r\n\r\n");
            request = baos.toByteArray();
            return start();
        } catch (IOException ioe) {
            throw new RuntimeException(ioe.getMessage());
        }
    }
    
    public boolean GET(String file, String[] params, String[] values) {        
        StringBuffer query = new StringBuffer();
        query.append(file);
        query.append("?");
        for (int i = 0, length = params.length; i < length; i++) {
            query.append(params[i]);
            query.append("=");
            query.append(encode(values[i]));
            if (i < (length - 1)) {
                query.append("&");
            }
        }
        return GET(query.toString());
    }
    
    public boolean GET(String file) {
        if (file.startsWith("http")) {
            url = file;
        } else {
            url = "http://" + server + ((port != 80) ? ":" + port : "") + file;
        }
        contentType = null;
        request = null;        
        return start();
    }
    
    private boolean start() {
        Thread t = new Thread(this);
        boolean result = true;
        if (midlet == null) {
            //// block until connected
            synchronized (server) {
                t.start();
                try {
                    server.wait();
                } catch (InterruptedException ie) { }
                result = is != null;
            }
        } else {
            t.start();
        }
        return result;
    }
    
    public void run() {
        close();
        OutputStream os = null;
        try {
            //// open connection to server
            con = (HttpConnection) Connector.open(url);
            if (contentType != null) {
                con.setRequestMethod(HttpConnection.POST);
                con.setRequestProperty("Content-Type", contentType);
            } else {
                con.setRequestMethod(HttpConnection.GET);
            }
            con.setRequestProperty("Connection", "close");
            if (request != null) {
                os = con.openOutputStream();
                os.write(request);
                os.flush();
                os.close();
                os = null;
            }
            //// now, open inputstream, committing the post
            is = con.openInputStream();
            if (midlet != null) {
                //// done, notify midlet
                midlet.enqueueLibraryEvent(this, EVENT_CONNECTED, null);
            }
        } catch (IOException ioe) {
            close();
            if (midlet != null) {
                midlet.enqueueLibraryEvent(this, EVENT_ERROR, null);
            }
        } finally {
            if (os != null) {
                try {
                    os.close();
                } catch (Exception e) {                    
                }
                os = null;
            }            
        }
        if (midlet == null) {
            synchronized (server) {
                server.notify();
            }
        }
    }
        
    public int read() {
        int result = -1;
        try {
            if (is != null) {
                result = is.read();
            }
        } catch (IOException ioe) {
            close();
        }
        return result;
    }
    
    public char readChar() {
        return (char) read();        
    }
        
    public byte[] readBytes() {
        byte[] result = null;
        try {
            //// read contents
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            int bytesRead = is.read(buffer);
            while (bytesRead >= 0) {
                baos.write(buffer, 0, bytesRead);
                bytesRead = is.read(buffer);
            }
            buffer = null;
            result = baos.toByteArray();
            baos.close();
            baos = null;
        } catch (IOException ioe) {
            close();
        }
        return result;
    }
    
    public int readBytes(byte[] buffer) {
        int totalBytesRead = -1;
        if (is != null) {
            try {
                totalBytesRead = 0;
                int length = buffer.length;                
                int bytesRead = 0;
                do {
                    bytesRead = is.read(buffer, totalBytesRead, length - totalBytesRead);
                    if (bytesRead >= 0) {
                        totalBytesRead += bytesRead;
                    }
                } while ((totalBytesRead < length) && (bytesRead >= 0));
            } catch (IOException ioe) {
                close();
                totalBytesRead = -1;
            }
        }
        return totalBytesRead;
    }
    
    public String readString() {
        String result = null;
        byte[] bytes = readBytes();
        if (bytes != null) {
            result = new String(bytes);
        }
        return result;
    }
    
    public void close() {
        if (is != null) {
            try {
                is.close();
            } catch (IOException ioe) {
            }
            is = null;
        }
        if (con != null) {
            try {
                con.close();
            } catch (IOException ioe) {
            }
            con = null;
        }
    }
}
