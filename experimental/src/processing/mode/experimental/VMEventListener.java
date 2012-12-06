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

import com.sun.jdi.event.EventSet;

/**
 * Interface for VM callbacks.
 *
 * @author Martin Leopold <m@martinleopold.com>
 */
public interface VMEventListener {

    /**
     * Receive an event from the VM. Events are sent in batches. See
     * documentation of EventSet for more information.
     *
     * @param es Set of events
     */
    void vmEvent(EventSet es);
}
