package processing.core;

import java.io.*;
import javax.microedition.io.*;

/** A library wrapper for an HTTP connection.
 *
 * @author Francis Li
 */
public class HTTPClient {
    private String  server;
    private int     port;
    
    public HTTPClient(String server) {
        this(server, 80);
    }
        
    public HTTPClient(String server, int port) {
        this.server = server;
        this.port = port;
    }
    
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
    
    public String GET(String file, String[] params, String[] values) {
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
    
    public String GET(String file) {
        HttpConnection con = null;
        InputStream is = null;
        try {
            //// prepend slash if necessary
            if (!file.startsWith("/")) {
                file = "/" + file;
            }
            //// open connection to server
            con = (HttpConnection) Connector.open("http://" + server + ":" + port + file);
            //// if successful, open inputstream
            is = con.openInputStream();
            //// read contents
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            int bytesRead = is.read(buffer);
            while (bytesRead >= 0) {
                baos.write(buffer, 0, bytesRead);
                bytesRead = is.read(buffer);
            }
            buffer = null;
            buffer = baos.toByteArray();
            baos.close();
            baos = null;
            //// return contents as string
            return new String(buffer);
        } catch (IOException ioe) {
            return null;
        } finally {
            close(con, is, null);
        }
    }
    
    private void close(Connection con, InputStream is, OutputStream os) {
        if (os != null) {
            try {
                os.close();
            } catch (IOException ioe) {
            }
        }
        if (is != null) {
            try {
                is.close();
            } catch (IOException ioe) {
            }
        }
        if (con != null) {
            try {
                con.close();
            } catch (IOException ioe) {
            }
        }
    }
}
