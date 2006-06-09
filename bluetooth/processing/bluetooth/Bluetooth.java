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
    /** Default fake UUID that will probably never be used. Also used as
     * a synchronization object- wierd behavior was occuring when synchronizing
     * on (this)...
     */
    public static final String UUID_DEFAULT = "102030405060708090A0B0C0D0E0F010";
    
    public static final int EVENT_DISCOVER_DEVICE               = 1;
    public static final int EVENT_DISCOVER_DEVICE_COMPLETED     = 2;
    public static final int EVENT_DISCOVER_SERVICE              = 3;
    public static final int EVENT_DISCOVER_SERVICE_COMPLETED    = 4;
    public static final int EVENT_CLIENT_CONNECTED              = 5;
    
    protected PMIDlet                   midlet;
    protected LocalDevice               local;
    protected DiscoveryAgent            agent;
    protected UUID                      uuid;
    
    protected Vector                    devices;
    protected Thread                    serverThread;
    protected Thread                    discoverThread;
    
    /** Transaction id for the service search process, must be positive if searching */
    protected int                       transId;
    /** Whether or not to find exactly this service */
    protected boolean                   find;
    /** List of matching services found */
    protected Vector                    services;
    /** Actual server notifier object that will be used to get client connections */
    protected StreamConnectionNotifier  server;
    
    public Bluetooth(PMIDlet midlet) {
        this(midlet, UUID_DEFAULT);
    }
    
    public Bluetooth(PMIDlet midlet, String id) {
        this.midlet = midlet;
        devices = new Vector();
        services = new Vector();
        try {
            local = LocalDevice.getLocalDevice();
            agent = local.getDiscoveryAgent();
            uuid = new UUID(id, false);
        } catch (BluetoothStateException bse) {
            throw new RuntimeException(bse.getMessage());
        }
    }
        
    public void discover() {
        boolean start = false;
        synchronized (UUID_DEFAULT) {
            if (discoverThread == null) {
                discoverThread = new Thread(this);
                start = true;
            }
        }
        if (start) {
            find = false;
            discoverThread.start();
        }
    }    
    
    public void find() {
        boolean start = false;
        synchronized (UUID_DEFAULT) {
            if (discoverThread == null) {
                discoverThread = new Thread(this);
                start = true;
            }
        }
        if (start) {
            find = true;
            discoverThread.start();
        }
    }
    
    public void cancel() {
        boolean cancelled = false;
        synchronized (UUID_DEFAULT) {
            if (discoverThread != null) {
                discoverThread = null;
                cancelled = true;
            }
        }
        if (cancelled) {
            agent.cancelInquiry(this);
            cancelled = false;
            synchronized (services) {
                if (transId > 0) {
                    agent.cancelServiceSearch(transId);
                    cancelled = true;
                }
            }
        }
    }
    
    public void run() {
        try {
            devices.removeAllElements();
            services.removeAllElements();
            synchronized (devices) {
                //// start inquiry
                agent.startInquiry(DiscoveryAgent.GIAC, this);
                try {
                    //// block and wait until complete
                    devices.wait();                    
                } catch (InterruptedException ie) { }
            }
            //// check if cancelled first
            boolean cancelled = false;
            synchronized (UUID_DEFAULT) {
                if (discoverThread != Thread.currentThread()) {
                    cancelled = true;
                }
            }
            if (!cancelled) {
                //// if not, get name for each device
                Enumeration e = devices.elements();
                Device d;
                String name;
                while (e.hasMoreElements()) {
                    d = (Device) e.nextElement();
                    try {
                        name = d.device.getFriendlyName(false);
                        if (name != null) {
                            d.name = name;
                        }
                    } catch (IOException ioe) { }
                }
            }
            //// copy into array
            Device[] devices = new Device[this.devices.size()];
            this.devices.copyInto(devices);            
            //// fire event back into midlet
            midlet.enqueueLibraryEvent(this, EVENT_DISCOVER_DEVICE_COMPLETED, devices);
            
            //// if find is true, look for this specific service
            if (find) {
                for (int i = 0, length = devices.length; i < length; i++) {
                    synchronized (UUID_DEFAULT) {
                        if (discoverThread != Thread.currentThread()) {
                            cancelled = true;
                        }
                    }
                    if (cancelled) {
                        break;
                    }
                    synchronized (services) {
                        transId = agent.searchServices(new int[] { Service.ATTR_SERVICENAME, Service.ATTR_SERVICEDESC, Service.ATTR_PROVIDERNAME }, 
                                                       new UUID[] { uuid }, devices[i].device, this);      
                        try {
                            services.wait();
                        } catch (InterruptedException ie) { }
                        transId = 0;
                    }                    
                }
                //// fire event
                Service[] data = new Service[services.size()];
                services.copyInto(data);
                midlet.enqueueLibraryEvent(this, EVENT_DISCOVER_SERVICE_COMPLETED, data);
            }
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage());
        } finally {
            //// clear all results so we don't hold any lingering references
            devices.removeAllElements();
            services.removeAllElements();
        }
        synchronized (UUID_DEFAULT) {
            discoverThread = null;
        }
    }
    
    public void start(String name) {
        if (serverThread == null) {
            try {
                local.setDiscoverable(DiscoveryAgent.GIAC);
                String url = "btspp://localhost:" + uuid.toString() + ";name=" + name;
                server = (StreamConnectionNotifier) Connector.open(url);             
                ServiceRecord record = local.getRecord(server);
                //// set availability to fully available
                record.setAttributeValue(0x0008, new DataElement(DataElement.U_INT_1, 0xFF));
                //// set device class to telephony
                record.setDeviceServiceClasses(0x400000);
                //// set up a service for this record and set it up as the thread
                Service s = new Service(null, record, this);
                serverThread = new Thread(s);
                serverThread.start();
            } catch (Exception e) {
                serverThread = null;
                throw new RuntimeException(e.getMessage());
            }
        }
    }
    
    public void stop() {
        serverThread = null;
    }

    public void deviceDiscovered(RemoteDevice btDevice, DeviceClass cod) {
        Device device = new Device(btDevice, this);
        devices.addElement(device);
        midlet.enqueueLibraryEvent(this, EVENT_DISCOVER_DEVICE, device);
    }

    public void inquiryCompleted(int discType) {
        synchronized (devices) {
            //// wake up discover thread
            devices.notifyAll();
        }
    }

    public void serviceSearchCompleted(int transId, int respCode) {
        synchronized (services) {
            if (this.transId == transId) {
                services.notifyAll();
            }
        }
    }

    public void servicesDiscovered(int transId, ServiceRecord[] servRecord) {
        boolean valid = false;
        synchronized (services) {
            if (this.transId == transId) {
                valid = true;
            }
        }
        if (valid && (servRecord.length > 0)) {
            //// find matching device object (should change vector to hashtable to optimize this)
            Enumeration e = devices.elements();
            RemoteDevice host = servRecord[0].getHostDevice();
            Device d = null;
            boolean found = false;
            while (e.hasMoreElements()) {
                d = (Device) e.nextElement();
                if (d.device.equals(host)) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                //// not sure this should _ever_ happen
                d = new Device(host, this);
            }
            //// build service list for event callback
            DataElement element;
            Service[] data = new Service[servRecord.length];
            for (int i = 0, length = servRecord.length; i < length; i++) {
                data[i] = new Service(d, servRecord[i], this);
                //// add to found services list
                services.addElement(data[i]);
            }
            //// fire event 
            midlet.enqueueLibraryEvent(this, Bluetooth.EVENT_DISCOVER_SERVICE, data);
        }
    }
}
