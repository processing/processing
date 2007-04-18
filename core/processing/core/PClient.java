package processing.core;

import java.io.*;
import javax.microedition.io.*;

/** Network client interface for generating HTTP protocol requests.
 *
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
public class PClient {    
    private PMIDlet             midlet;
    
    private String              server;
    private int                 port;
    
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
    
    public PRequest POST(String file, String[] params, String[] values) {
        //// generate request payload
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
        return request(file, "application/x-www-form-urlencoded", baos.toByteArray());
    }
    
    public PRequest POST(String file, String[] params, Object[] values) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            PrintStream ps = new PrintStream(baos);
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
            return request(file, "multipart/form-data; boundary=BOUNDARY_185629", baos.toByteArray());
        } catch (IOException ioe) {
            throw new PException(ioe);
        }
    }
    
    public PRequest GET(String file, String[] params, String[] values) {        
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
    
    public PRequest GET(String file) {
        return request(file, null, null);
    }
    
    private PRequest request(String file, String contentType, byte[] bytes) {
        //// create url
        StringBuffer url = new StringBuffer();
        url.append("http://");
        url.append(server);
        if (port != 80) {
            url.append(":");
            url.append(port);
        }
        url.append(file);
        //// initiate request
        PRequest request = new PRequest(midlet, url.toString(), contentType, bytes);
        Thread t = new Thread(request);
        t.start();
        
        return request;
    }
}
