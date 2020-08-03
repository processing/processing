/* -*- mode: java; c-basic-offset: 2; indent-tabs-mode: nil -*- */

/*
  Part of the Processing project - http://processing.org

  Copyright (c) 2012-16 The Processing Foundation

  This program is free software; you can redistribute it and/or
  modify it under the terms of the GNU General Public License
  version 2, as published by the Free Software Foundation.

  This program is distributed in the hope that it will be useful,
  but WITHOUT ANY WARRANTY; without even the implied warranty of
  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  GNU General Public License for more details.

  You should have received a copy of the GNU General Public License
  along with this program; if not, write to the Free Software Foundation,
  Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
*/

package processing.mode.java.debug;

import com.sun.jdi.ReferenceType;


/**
 * Listener to be notified when a class is loaded in the debugger. Used by
 * {@link LineBreakpoint}s to activate themselves as soon as the respective
 * class is loaded.
 */
public interface ClassLoadListener {

  /**
   * Event handler called when a class is loaded.
   */
  public void classLoaded(ReferenceType theClass);
}
