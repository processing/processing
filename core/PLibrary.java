/* -*- mode: jde; c-basic-offset: 2; indent-tabs-mode: nil -*- */

/*
  PLibrary - interface for classes that plug into bagel
  Part of the Processing project - http://processing.org

  Copyright (c) 2004 Ben Fry and the Processing project.

  This library is free software; you can redistribute it and/or
  modify it under the terms of the GNU Lesser General Public
  License as published by the Free Software Foundation; either
  version 2.1 of the License, or (at your option) any later version.

  This library is distributed in the hope that it will be useful,
  but WITHOUT ANY WARRANTY; without even the implied warranty of
  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
  Lesser General Public License for more details.

  You should have received a copy of the GNU Lesser General
  Public License along with this library; if not, write to the
  Free Software Foundation, Inc., 59 Temple Place, Suite 330,
  Boston, MA  02111-1307  USA
*/

package processing.core;


public interface PLibrary {

  /**
   * This sets the parent PApplet in case that's needed for anything.
   * It's called on attach().
   */
  public void setParent(PApplet parent);

  /**
   * Called before (outside of) draw() or loop().
   * Note that this also gets called before beginFrame()
   * so no drawing can occur.
   */
  public void pre();

  /** 
   * Called after (outside of) draw() or loop().
   */
  public void post(); 

  /**
   * Called when the applet or application is stopped.
   */
  public void stop();
}



/*
public void libraryEvent(PLibrary who, Object data) {
  //if (who instanceof BVideo) {
  if (who.signature() == Sonia.SIGNATURE) {
    BImage frame = (BImage)data;
    // do something with the data 
  }
}
*/
