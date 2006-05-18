package processing.bluetooth;

import java.io.*;
import javax.microedition.io.*;
import javax.bluetooth.*;

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
public class Service implements Runnable {
    public static final int ATTR_SERVICENAME    = 0x0100;
    public static final int ATTR_SERVICEDESC    = 0x0101;
    public static final int ATTR_PROVIDERNAME   = 0x0102;
    
    public ServiceRecord    record;
    public Bluetooth        bt;
    
    protected Service(ServiceRecord record, Bluetooth bt) {
        this.record = record;
        this.bt = bt;
    }
    
    public String name() {
        String name = null;
        DataElement element = record.getAttributeValue(ATTR_SERVICENAME);
        if (element != null) {
            name = (String) element.getValue();
        }
        return name;
    }
    
    public String description() {
        String desc = null;
        DataElement element = record.getAttributeValue(ATTR_SERVICEDESC);
        if (element != null) {
            desc = (String) element.getValue();
        }
        return desc;
    }
    
    public String provider() {
        String provider = null;
        DataElement element = record.getAttributeValue(ATTR_PROVIDERNAME);
        if (element != null) {
            provider = (String) element.getValue();
        }
        return provider;
    }
    
    public Client connect() {
        try {
            StreamConnection con = (StreamConnection) Connector.open(record.getConnectionURL(ServiceRecord.NOAUTHENTICATE_NOENCRYPT, false));
            Client c = new Client(con);
            return c;
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage());
        }
    }
    
    public void run() {                
        while (bt.serverThread == Thread.currentThread()) {
            try {
                StreamConnection con = bt.server.acceptAndOpen();
                Client c = new Client(con);
                bt.midlet.enqueueLibraryEvent(bt, Bluetooth.EVENT_CLIENT_CONNECTED, c);
            } catch (IOException ioe) {
                throw new RuntimeException(ioe.getMessage());
            }
        }
        try {
            bt.server.close();
        } catch (IOException ioe) {            
        }
    }
}
