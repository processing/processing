/* -*- mode: java; c-basic-offset: 2; indent-tabs-mode: nil -*- */

/*
  Part of the Processing project - http://processing.org

  Copyright (c) 2011-12 Ben Fry and Casey Reas

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

package processing.glw;


import javax.media.opengl.GLCapabilities;
import javax.media.opengl.GLProfile;

import com.jogamp.newt.opengl.GLWindow;
import com.jogamp.newt.event.WindowAdapter;
import com.jogamp.newt.event.WindowEvent;

import processing.opengl.PGraphicsOpenGL;
import processing.opengl.PJOGL;

public class PNEWT extends PJOGL {

  static {
    WINDOW_TOOLKIT = NEWT;
    EVENTS_TOOLKIT = NEWT;
    USE_FBOLAYER_BY_DEFAULT = false;    
    USE_JOGL_FBOLAYER = false;
  }
  
  
  public PNEWT(PGraphicsOpenGL pg) {
    super(pg);
  }
  
  
  protected void initSurface(int antialias) {
    if (profile == null) {
      profile = GLProfile.getDefault();
    } else {
      window.removeGLEventListener(listener);
      pg.parent.remove(canvasNEWT);      
    }
    
    // Setting up the desired capabilities;
    GLCapabilities caps = new GLCapabilities(profile);
    caps.setAlphaBits(REQUESTED_ALPHA_BITS);
    caps.setDepthBits(REQUESTED_DEPTH_BITS);
    caps.setStencilBits(REQUESTED_STENCIL_BITS);
    
    if (1 < antialias) {
      caps.setSampleBuffers(true);
      caps.setNumSamples(antialias);
    } else {
      caps.setSampleBuffers(false);
    }
    fboLayerRequested = false;
    
    window = GLWindow.create(caps);
    window.setSize(pg.width, pg.height);
    window.setVisible(true);
    pg.parent.frame.setVisible(false);
    
    canvas = canvasNEWT;
    canvasAWT = null;

    window.addWindowListener(new WindowAdapter() {
      @Override
      public void windowDestroyNotify(final WindowEvent e) {
        System.exit(0);
      }
    });   
    
    registerListeners();
  }


}
