/*
 * Copyright (C) 2012 Martin Leopold <m@martinleopold.com>
 *
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation, Inc., 59 Temple
 * Place - Suite 330, Boston, MA 02111-1307, USA.
 */
package processing.mode.experimental;

import com.sun.jdi.VMDisconnectedException;
import com.sun.jdi.event.EventQueue;
import com.sun.jdi.event.EventSet;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Reader Thread for VM Events. Constantly monitors a VMs EventQueue for new
 * events and forwards them to an VMEventListener.
 *
 * @author Martin Leopold <m@martinleopold.com>
 */
public class VMEventReader extends Thread {

    EventQueue eventQueue;
    VMEventListener listener;

    /**
     * Construct a VMEventReader. Needs to be kicked off with start() once
     * constructed.
     *
     * @param eventQueue The queue to read events from. Can be obtained from a
     * VirtualMachine via eventQueue().
     * @param listener the listener to forward events to.
     */
    public VMEventReader(EventQueue eventQueue, VMEventListener listener) {
        super("VM Event Thread");
        this.eventQueue = eventQueue;
        this.listener = listener;
    }

    @Override
    public void run() {
        try {
            while (true) {
                EventSet eventSet = eventQueue.remove();
                listener.vmEvent(eventSet);
                /*
                 * for (Event e : eventSet) { System.out.println("VM Event: " +
                 * e.toString()); }
                 */
            }
        } catch (VMDisconnectedException e) {
            Logger.getLogger(VMEventReader.class.getName()).log(Level.INFO, "VMEventReader quit on VM disconnect");
        } catch (Exception e) {
            Logger.getLogger(VMEventReader.class.getName()).log(Level.SEVERE, "VMEventReader quit", e);
        }
    }
}
