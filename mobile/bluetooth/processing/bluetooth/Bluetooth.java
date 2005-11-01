package processing.bluetooth;

import processing.core.*;

import java.io.*;
import java.util.*;
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
public class Bluetooth implements DiscoveryListener, Runnable {
    public static final String UUID_DEFAULT = "102030405060708090A0B0C0D0E0F010";
    
    public static final int EVENT_DISCOVER_DEVICE               = 1;
    public static final int EVENT_DISCOVER_DEVICE_COMPLETED     = 2;
    public static final int EVENT_DISCOVER_SERVICE              = 3;
    public static final int EVENT_DISCOVER_SERVICE_COMPLETED    = 4;
    public static final int EVENT_CLIENT_CONNECTED              = 5;
    
    protected PMIDlet           midlet;
    protected LocalDevice       local;
    protected DiscoveryAgent    agent;
    
    private Vector              devices;
    private Thread              thread;
    
    private StreamConnectionNotifier    server;
    
    public Bluetooth(PMIDlet midlet) {
        this.midlet = midlet;
        try {
            local = LocalDevice.getLocalDevice();
            agent = local.getDiscoveryAgent();
        } catch (BluetoothStateException bse) {
            throw new RuntimeException(bse.getMessage());
        }
    }
        
    public void discover() {
        try {
            devices = new Vector();
            agent.startInquiry(DiscoveryAgent.GIAC, this);
        } catch (BluetoothStateException bse) {
            throw new RuntimeException(bse.getMessage());
        }
    }    
    
    public void cancel() {
        agent.cancelInquiry(this);
    }
    
    public void start(String name) {
        if (thread == null) {
            thread = new Thread(this);
            try {
                local.setDiscoverable(DiscoveryAgent.GIAC);
                UUID uuid = new UUID(UUID_DEFAULT, false);
                String url = "btspp://localhost:" + uuid.toString() + ";name=" + name;
                server = (StreamConnectionNotifier) Connector.open(url);             
                ServiceRecord record = local.getRecord(server);
                //// set availability to fully available
                record.setAttributeValue( 0x0008, new DataElement( DataElement.U_INT_1, 0xFF ) );
                //// set device class to telephony
                record.setDeviceServiceClasses( 0x400000  );                
                thread.start();
            } catch (Exception e) {
                thread = null;
                throw new RuntimeException(e.getMessage());
            }
        }
    }
    
    public void stop() {
        thread = null;
    }
    
    public void run() {                
        while (thread == Thread.currentThread()) {
            try {
                StreamConnection con = server.acceptAndOpen();
                Client c = new Client(con);
                midlet.enqueueLibraryEvent(this, EVENT_CLIENT_CONNECTED, c);
            } catch (IOException ioe) {
                throw new RuntimeException(ioe.getMessage());
            }
        }
        try {
            server.close();
        } catch (IOException ioe) {            
        }
    }

    public void deviceDiscovered(RemoteDevice btDevice, DeviceClass cod) {
        Device device = new Device(btDevice, this);
        devices.addElement(device);
        midlet.enqueueLibraryEvent(this, EVENT_DISCOVER_DEVICE, device);
    }

    public void inquiryCompleted(int discType) {
        Device[] devices = new Device[this.devices.size()];
        this.devices.copyInto(devices);
        midlet.enqueueLibraryEvent(this, EVENT_DISCOVER_DEVICE_COMPLETED, devices);
    }

    public void serviceSearchCompleted(int param, int param1) {
    }

    public void servicesDiscovered(int param, ServiceRecord[] serviceRecord) {
    }
}
