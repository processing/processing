/*
 * Copyright (C) 2012 Martin Leopold <m@martinleopold.com>
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package processing.mode.experimental;

import com.sun.jdi.ReferenceType;

/**
 * Listener to be notified when a class is loaded in the debugger. Used by
 * {@link LineBreakpoint}s to activate themselves as soon as the respective
 * class is loaded.
 *
 * @author Martin Leopold <m@martinleopold.com>
 */
public interface ClassLoadListener {

    /**
     * Event handler called when a class is loaded.
     *
     * @param theClass the class
     */
    public void classLoaded(ReferenceType theClass);
}
