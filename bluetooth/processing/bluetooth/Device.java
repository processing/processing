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
public class Device implements DiscoveryListener {
    public RemoteDevice     device;
    
    private Bluetooth       bt;
    private int             id;
    
    protected Device(RemoteDevice device, Bluetooth bt) {
        this.device = device;
        this.bt = bt;
        id = -1;
    }
    
    public String name() {
        try {
            return device.getFriendlyName(false);            
        } catch (IOException ioe) {
            throw new RuntimeException(ioe.getMessage());
        }
    }
    
    public String address() {
        return device.getBluetoothAddress();
    }
    
    public void discover() {
        if (id < 0) {
            try {
                id = bt.agent.searchServices(new int[] { Service.ATTR_SERVICENAME, Service.ATTR_SERVICEDESC, Service.ATTR_PROVIDERNAME }, 
                                             new UUID[] { new UUID(0x1101) }, device, this);            
            } catch (BluetoothStateException bse) {
                throw new RuntimeException(bse.getMessage());
            }
        }
    }
    
    public void cancel() {
        if (id >= 0) {
            bt.agent.cancelServiceSearch(id);
            id = -1;
        }
    }

    public void deviceDiscovered(RemoteDevice btDevice, DeviceClass cod) {
    }

    public void inquiryCompleted(int discType) {
    }

    public void serviceSearchCompleted(int transId, int respCode) {
        if (transId == id) {
            bt.midlet.enqueueLibraryEvent(bt, Bluetooth.EVENT_DISCOVER_SERVICE_COMPLETED, null);
            id = -1;
        }
    }

    public void servicesDiscovered(int transId, ServiceRecord[] servRecord) {
        if (transId == id) {
            Service[] services = new Service[servRecord.length];
            Service s;
            for (int i = 0, length = servRecord.length; i < length; i++) {
                s = new Service(servRecord[i]);
                services[i] = s;
            }
            bt.midlet.enqueueLibraryEvent(bt, Bluetooth.EVENT_DISCOVER_SERVICE, services);
        }
    }
}
