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


import javax.media.opengl.GL;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLCapabilities;
import javax.media.opengl.GLContext;
import javax.media.opengl.GLDrawableFactory;
import javax.media.opengl.GLEventListener;
import javax.media.opengl.GLException;
import javax.media.opengl.GLFBODrawable;
import javax.media.opengl.GLProfile;
import javax.media.opengl.awt.GLCanvas;

import com.jogamp.newt.opengl.GLWindow;
import com.jogamp.newt.event.WindowAdapter;
import com.jogamp.newt.event.WindowEvent;
import com.jogamp.opengl.FBObject;

import processing.core.PApplet;
import processing.core.PGraphics;
import processing.event.KeyEvent;
import processing.event.MouseEvent;
import processing.opengl.PGL;
import processing.opengl.PGraphicsOpenGL;
import processing.opengl.PJOGL;
import processing.opengl.Texture;


public class PNEWT extends PJOGL {

  static {
    WINDOW_TOOLKIT = NEWT;
    EVENTS_TOOLKIT = NEWT;
    USE_FBOLAYER_BY_DEFAULT = false;    
    USE_JOGL_FBOLAYER = false;
  }
  
  protected static GLCapabilities sharedCaps;
  protected static GLAutoDrawable sharedDrawable;    
  
  
  public PNEWT(PGraphicsOpenGL pg) {
    super(pg);
  }

  
  protected void initSurface(int antialias) {
    if (profile == null) {
      if (PROFILE == 2) {
        try {
          profile = GLProfile.getGL2ES1();
        } catch (GLException ex) {
          profile = GLProfile.getMaxFixedFunc(true);
        }
      } else if (PROFILE == 3) {
        try {
          profile = GLProfile.getGL2GL3();
        } catch (GLException ex) {
          profile = GLProfile.getMaxProgrammable(true);
        }
        if (!profile.isGL3()) {
          PGraphics.showWarning("Requested profile GL3 but is not available, got: " + profile);
        }
      } else if (PROFILE == 4) {
        try {
          profile = GLProfile.getGL4ES3();
        } catch (GLException ex) {
          profile = GLProfile.getMaxProgrammable(true);
        }
        if (!profile.isGL4()) {
          PGraphics.showWarning("Requested profile GL4 but is not available, got: " + profile);
        }
      } else throw new RuntimeException(UNSUPPORTED_GLPROF_ERROR);

      if (2 < PROFILE) {
        texVertShaderSource = convertVertexSource(texVertShaderSource, 120, 150);
        tex2DFragShaderSource = convertFragmentSource(tex2DFragShaderSource, 120, 150);
        texRectFragShaderSource = convertFragmentSource(texRectFragShaderSource, 120, 150);
      }
    }
    
    // Setting up the desired capabilities;
    sharedCaps = new GLCapabilities(profile);
    sharedCaps.setAlphaBits(REQUESTED_ALPHA_BITS);
    sharedCaps.setDepthBits(REQUESTED_DEPTH_BITS);
    sharedCaps.setStencilBits(REQUESTED_STENCIL_BITS);

    sharedCaps.setPBuffer(false);
    sharedCaps.setFBO(false);    
    sharedCaps.setSampleBuffers(false);
    
    fboLayerRequested = false;    
    sharedDrawable = GLDrawableFactory.getFactory(profile).createDummyAutoDrawable(null, true, sharedCaps, null);
    sharedDrawable.display(); // triggers GLContext object creation and native realization.    
    DummyListener listener = new DummyListener();
    sharedDrawable.addGLEventListener(listener);

    
    /*
    window = GLWindow.create(caps);
    window.setSize(pg.width, pg.height);
    window.setVisible(true);
    window.setTitle(pg.parent.frame.getTitle());
    //window.setUndecorated(true);
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
    */
    
    pg.parent.frame.setVisible(false);
  }
  
  
  protected boolean displayable() {
    return false;
  }
  
  protected void requestDraw() {
    // Creating windows
    for (PGraphics pg: GLW.windows.keySet()) {
      GLWindow win = GLW.windows.get(pg);
      if (win == null) {
        System.out.println("creating window");
        //win = GLWindow.create(sharedCaps);
        win = GLWindow.create(sharedCaps);
        win.setSharedAutoDrawable(sharedDrawable);
        win.setSize(pg.width, pg.height);
        win.setTitle("TEST");
        win.setVisible(true);
        GLW.windows.put(pg, win);
        
        
        NEWTListener listener = new NEWTListener(pg);
        win.addGLEventListener(listener);     
        
        NEWTMouseListener mouseListener = new NEWTMouseListener();
        win.addMouseListener(mouseListener);
        NEWTKeyListener keyListener = new NEWTKeyListener();
        win.addKeyListener(keyListener);
        NEWTWindowListener winListener = new NEWTWindowListener();
        win.addWindowListener(winListener);
 
        
        win.addWindowListener(new WindowAdapter() {
          @Override
          public void windowDestroyNotify(final WindowEvent e) {
            System.out.println("window destroyed");
//            System.exit(0);
          }
        });        
      }
    }
    
    sharedDrawable.display();
    
    // Display windows
    int totalCount = 0;
    int realizedCount = 0;
    for (GLWindow win: GLW.windows.values()) {
      if (win != null) {
        totalCount++;
        if (win.isRealized()) realizedCount++;
        win.display();
      }
    }
    
    if (0 < totalCount && realizedCount == 0) {
      System.out.println("SHOULD QUIT NOW");
      sharedDrawable.destroy();
      System.exit(0);
    }   
  }
  
  protected class DummyListener implements GLEventListener {
    public DummyListener() { 
    }

    @Override
    public void display(GLAutoDrawable glDrawable) {
      getGL(glDrawable);
      pg.parent.handleDraw();
    }

    @Override
    public void dispose(GLAutoDrawable adrawable) {
    }

    @Override
    public void init(GLAutoDrawable glDrawable) {
      getGL(glDrawable);

      capabilities = glDrawable.getChosenGLCapabilities();
      if (!hasFBOs()) {
        throw new RuntimeException(MISSING_FBO_ERROR);
      }
      if (!hasShaders()) {
        throw new RuntimeException(MISSING_GLSL_ERROR);
      }
      if (USE_JOGL_FBOLAYER && capabilities.isFBO()) {
        int maxs = maxSamples();
        numSamples = PApplet.min(capabilities.getNumSamples(), maxs);
      }
    }

    @Override
    public void reshape(GLAutoDrawable glDrawable, int x, int y, int w, int h) {
    }
  }    
  
  
  protected class NEWTListener implements GLEventListener {
    PGraphicsOpenGL pg;
    PNEWT pgl;
    
    public NEWTListener(PGraphics pg) { 
      this.pg = (PGraphicsOpenGL)pg;
      pgl = (PNEWT)this.pg.pgl;
    }

    @Override
    public void display(GLAutoDrawable glDrawable) {
      pgl.getGL(glDrawable);
      Texture tex = pg.texture;
      if (tex != null) {
        pgl.disable(PGL.BLEND);
        pgl.drawTexture(tex.glTarget, tex.glName,
                        tex.glWidth, tex.glHeight,
                        0, 0, pg.width, pg.height);
        pgl.enable(PGL.BLEND);
      }
    }

    @Override
    public void dispose(GLAutoDrawable adrawable) {
    }

    @Override
    public void init(GLAutoDrawable glDrawable) {
    }

    @Override
    public void reshape(GLAutoDrawable glDrawable, int x, int y, int w, int h) {       
    }
  }  
}
