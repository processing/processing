/* -*- mode: jde; c-basic-offset: 2; indent-tabs-mode: nil -*- */

/*
  BLibrary - interface for classes that plug into bagel
  Part of the Processing project - http://Proce55ing.net

  Copyright (c) 2001-03
  Ben Fry, Massachusetts Institute of Technology and
  Casey Reas, Interaction Design Institute Ivrea

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


public interface BLibrary {

  // called when the applet is stopped
  public void stop();
}



/*
public void libraryEvent(BLibrary who, Object data) {
  //if (who instanceof BVideo) {
  if (who.signature() == Sonia.SIGNATURE) {
    BImage frame = (BImage)data;
    // do something with the data 
  }
}
*/
