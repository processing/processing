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

package processing.opengl;

import processing.core.PApplet;
import processing.core.PConstants;

import java.nio.IntBuffer;

/**
 * Encapsulates a Frame Buffer Object for offscreen rendering.
 * When created with onscreen == true, it represents the normal
 * framebuffer. Needed by the stack mechanism in OPENGL2 to return
 * to onscreen rendering after a sequence of pushFramebuffer calls.
 * It transparently handles the situations when the FBO extension is
 * not available.
 * 
 * By Andres Colubri.
 */

public class FrameBuffer implements PConstants {  
  protected PApplet parent;
  protected PGraphicsOpenGL pg;
  protected PGL pgl;
  protected PGL.Context context;      // The context that created this framebuffer.
  
  public int glFboID;
  public int glDepthBufferID;
  public int glStencilBufferID;
  public int glDepthStencilBufferID;
  public int glColorBufferMultisampleID;
  public int width;
  public int height;

  protected int depthBits;
  protected int stencilBits;
  protected boolean packedDepthStencil;
  
  protected boolean multisample;
  protected int nsamples;
  
  protected int numColorBuffers;
  protected Texture[] colorBufferTex;

  protected boolean screenFb;
  protected boolean noDepth;  
   
  protected IntBuffer pixelBuffer;

  FrameBuffer(PApplet parent, int w, int h) {
    this(parent, w, h, 1, 1, 0, 0, false, false);
  }  
  
  FrameBuffer(PApplet parent, int w, int h, boolean screen) {    
    this(parent, w, h, 1, 1, 0, 0, false, screen);
  }

  FrameBuffer(PApplet parent, int w, int h, int samples, int colorBuffers, 
               int depthBits, int stencilBits, boolean packedDepthStencil, 
               boolean screen) {
    this.parent = parent;
    pg = (PGraphicsOpenGL)parent.g;
    pgl = pg.pgl;
    context = pgl.createEmptyContext();
    
    glFboID = 0;
    glDepthBufferID = 0;
    glStencilBufferID = 0;
    glDepthStencilBufferID = 0;    
    glColorBufferMultisampleID = 0;
    
    if (screen) {
      // If this framebuffer is used to represent a on-screen buffer,
      // then it doesn't make it sense for it to have multisampling,
      // color, depth or stencil buffers.
      depthBits = stencilBits = samples = colorBuffers = 0; 
    }
    
    width = w;
    height = h;
    
    if (1 < samples) {
      multisample = true;
      nsamples = samples;      
    } else {
      multisample = false;
      nsamples = 1;      
    }
        
    numColorBuffers = colorBuffers;
    colorBufferTex = new Texture[numColorBuffers];
    for (int i = 0; i < numColorBuffers; i++) {
      colorBufferTex[i] = null;
    }    
    
    if (depthBits < 1 && stencilBits < 1) {
      this.depthBits = 0;
      this.stencilBits = 0;
      this.packedDepthStencil = false;
    } else {
      if (packedDepthStencil) {
        // When combined depth/stencil format is required, the depth and stencil bits
        // are overriden and the 24/8 combination for a 32 bits surface is used. 
        this.depthBits = 24;
        this.stencilBits = 8;
        this.packedDepthStencil = true;        
      } else {
        this.depthBits = depthBits;
        this.stencilBits = stencilBits;
        this.packedDepthStencil = false;        
      }
    }
    
    screenFb = screen;
    
    allocate();
    noDepth = false;
    
    pixelBuffer = null;
  }

  
  protected void finalize() throws Throwable {
    try {
      if (glFboID != 0) {
        pg.finalizeFrameBufferObject(glFboID, context.code());
      }      
      if (glDepthBufferID != 0) {
        pg.finalizeRenderBufferObject(glDepthBufferID, context.code());
      }      
      if (glStencilBufferID != 0) {
        pg.finalizeRenderBufferObject(glStencilBufferID, context.code());
      }
      if (glColorBufferMultisampleID != 0) {
        pg.finalizeRenderBufferObject(glColorBufferMultisampleID, context.code());
      }
      if (glDepthStencilBufferID != 0) {
        pg.finalizeRenderBufferObject(glDepthStencilBufferID, context.code());
      }      
    } finally {
      super.finalize();
    }
  }  
  
  public void clear() {
    pg.pushFramebuffer();
    pg.setFramebuffer(this);
    pgl.glClearDepth(1);
    pgl.glClearStencil(0);
    pgl.glClearColor(0, 0, 0, 0);
    pgl.glClear(PGL.GL_DEPTH_BUFFER_BIT | PGL.GL_STENCIL_BUFFER_BIT | PGL.GL_COLOR_BUFFER_BIT);
    pg.popFramebuffer();    
  }
  
  public void copy(FrameBuffer dest) {
    pgl.glBindFramebuffer(PGL.GL_READ_FRAMEBUFFER, this.glFboID);
    pgl.glBindFramebuffer(PGL.GL_DRAW_FRAMEBUFFER, dest.glFboID);
    pgl.glBlitFramebuffer(0, 0, this.width, this.height,
                          0, 0, dest.width, dest.height, 
                          PGL.GL_COLOR_BUFFER_BIT, PGL.GL_NEAREST);
  }
  
  public void bind() {
    pgl.glBindFramebuffer(PGL.GL_FRAMEBUFFER, glFboID);
  }
  
  public void disableDepthTest() {
    noDepth = true;  
  }
  
  public void finish() {
    if (noDepth) {
      // No need to clear depth buffer because depth testing was disabled.
      if (pg.hintEnabled(ENABLE_DEPTH_TEST)) {
        pgl.glEnable(PGL.GL_DEPTH_TEST);        
      } else {
        pgl.glDisable(PGL.GL_DEPTH_TEST);
      }        
    }
  }
  
  public void readPixels() {
    if (pixelBuffer == null) createPixelBuffer();
    pixelBuffer.rewind();
    pgl.glReadPixels(0, 0, width, height, PGL.GL_RGBA, PGL.GL_UNSIGNED_BYTE, pixelBuffer);
  }
  
  public void getPixels(int[] pixels) {
    if (pixelBuffer != null) {
      pixelBuffer.get(pixels, 0, pixels.length);
      pixelBuffer.rewind();    
    }
  }
  
  public IntBuffer getPixelBuffer() {
    return pixelBuffer;
  }
  
  public boolean hasDepthBuffer() {
    return 0 < depthBits;
  }

  public boolean hasStencilBuffer() {
    return 0 < stencilBits;
  }
  
  ///////////////////////////////////////////////////////////  

  // Color buffer setters.
  
  
  public void setColorBuffer(Texture tex) {
    setColorBuffers(new Texture[] { tex }, 1);
  }
  

  public void setColorBuffers(Texture[] textures) {
    setColorBuffers(textures, textures.length);
  }
  
  
  public void setColorBuffers(Texture[] textures, int n) {
    if (screenFb) return;

    if (numColorBuffers != PApplet.min(n, textures.length)) {
      throw new RuntimeException("Wrong number of textures to set the color buffers.");
    }
        
    for (int i = 0; i < numColorBuffers; i++) {
      colorBufferTex[i] = textures[i];
    }
      
    pg.pushFramebuffer();
    pg.setFramebuffer(this);

    // Making sure nothing is attached.
    for (int i = 0; i < numColorBuffers; i++) {
      pgl.glFramebufferTexture2D(PGL.GL_FRAMEBUFFER, PGL.GL_COLOR_ATTACHMENT0 + i, PGL.GL_TEXTURE_2D, 0, 0);
    }

    for (int i = 0; i < numColorBuffers; i++) {
      pgl.glFramebufferTexture2D(PGL.GL_FRAMEBUFFER, PGL.GL_COLOR_ATTACHMENT0 + i, colorBufferTex[i].glTarget, colorBufferTex[i].glID, 0);
    }

    pgl.validateFramebuffer();

    pg.popFramebuffer();
  }  
  
  
  ///////////////////////////////////////////////////////////  

  // Allocate/release framebuffer.   
  
  
  protected void allocate() {
    release(); // Just in the case this object is being re-allocated.    
    
    context = pgl.getCurrentContext();
    
    if (screenFb) {
      glFboID = 0;
    } else {      
      glFboID = pg.createFrameBufferObject(context.code());
    }
    
    // create the rest of the stuff...
    if (multisample) {
      createColorBufferMultisample();
    }
    
    if (packedDepthStencil) {
      createPackedDepthStencilBuffer();
    } else {
      if (0 < depthBits) {
        createDepthBuffer();
      }
      if (0 < stencilBits) {
        createStencilBuffer();
      }      
    }           
  }
  
  
  protected void release() {
    if (glFboID != 0) {
      pg.finalizeFrameBufferObject(glFboID, context.code());
      glFboID = 0;
    }
    if (glDepthBufferID != 0) {
      pg.finalizeRenderBufferObject(glDepthBufferID, context.code());
      glDepthBufferID = 0;
    }
    if (glStencilBufferID != 0) {
      pg.finalizeRenderBufferObject(glStencilBufferID, context.code());
      glStencilBufferID = 0;
    }
    if (glColorBufferMultisampleID != 0) {
      pg.finalizeRenderBufferObject(glColorBufferMultisampleID, context.code());
      glColorBufferMultisampleID = 0;
    }
    if (glDepthStencilBufferID != 0) {
      pg.finalizeRenderBufferObject(glDepthStencilBufferID, context.code());
      glDepthStencilBufferID = 0;
    }     
  }
  
  
  protected boolean contextIsOutdated() {
    boolean outdated = !pgl.contextIsCurrent(context);
    if (outdated) {
      pg.removeFrameBufferObject(glFboID, context.code());
      pg.removeRenderBufferObject(glDepthBufferID, context.code());
      pg.removeRenderBufferObject(glStencilBufferID, context.code());
      pg.removeRenderBufferObject(glDepthStencilBufferID, context.code());
      pg.removeRenderBufferObject(glColorBufferMultisampleID, context.code());
      
      glFboID = 0;
      glDepthBufferID = 0;
      glStencilBufferID = 0;
      glDepthStencilBufferID = 0;    
      glColorBufferMultisampleID = 0;
      
      for (int i = 0; i < numColorBuffers; i++) {
        colorBufferTex[i] = null;
      }
    }
    return outdated;
  }  
  
  
  protected void createColorBufferMultisample() {
    if (screenFb) return;
    
    pg.pushFramebuffer();
    pg.setFramebuffer(this);      

    glColorBufferMultisampleID = pg.createRenderBufferObject(context.code());
    pgl.glBindRenderbuffer(PGL.GL_RENDERBUFFER, glColorBufferMultisampleID);      
    pgl.glRenderbufferStorageMultisample(PGL.GL_RENDERBUFFER, nsamples, PGL.GL_RGBA8, width, height);
    pgl.glFramebufferRenderbuffer(PGL.GL_FRAMEBUFFER, PGL.GL_COLOR_ATTACHMENT0, PGL.GL_RENDERBUFFER, glColorBufferMultisampleID);
    
    pg.popFramebuffer();      
  }
  
  
  protected void createPackedDepthStencilBuffer() {
    if (screenFb) return;
    
    if (width == 0 || height == 0) {
      throw new RuntimeException("PFramebuffer: size undefined.");
    }
    
    pg.pushFramebuffer();
    pg.setFramebuffer(this);
    
    glDepthStencilBufferID = pg.createRenderBufferObject(context.code());
    pgl.glBindRenderbuffer(PGL.GL_RENDERBUFFER, glDepthStencilBufferID);      
    
    if (multisample) { 
      pgl.glRenderbufferStorageMultisample(PGL.GL_RENDERBUFFER, nsamples, PGL.GL_DEPTH24_STENCIL8, width, height);
    } else {
      pgl.glRenderbufferStorage(PGL.GL_RENDERBUFFER, PGL.GL_DEPTH24_STENCIL8, width, height);
    }
    
    pgl.glFramebufferRenderbuffer(PGL.GL_FRAMEBUFFER, PGL.GL_DEPTH_ATTACHMENT, PGL.GL_RENDERBUFFER, glDepthStencilBufferID);
    pgl.glFramebufferRenderbuffer(PGL.GL_FRAMEBUFFER, PGL.GL_STENCIL_ATTACHMENT, PGL.GL_RENDERBUFFER, glDepthStencilBufferID);
    
    pg.popFramebuffer();  
  }
  
  
  protected void createDepthBuffer() {
    if (screenFb) return;
    
    if (width == 0 || height == 0) {
      throw new RuntimeException("PFramebuffer: size undefined.");
    }
    
    pg.pushFramebuffer();
    pg.setFramebuffer(this);

    glDepthBufferID = pg.createRenderBufferObject(context.code());
    pgl.glBindRenderbuffer(PGL.GL_RENDERBUFFER, glDepthBufferID);

    int glConst = PGL.GL_DEPTH_COMPONENT16;
    if (depthBits == 16) {
      glConst = PGL.GL_DEPTH_COMPONENT16; 
    } else if (depthBits == 24) {
      glConst = PGL.GL_DEPTH_COMPONENT24;
    } else if (depthBits == 32) {
      glConst = PGL.GL_DEPTH_COMPONENT32;              
    }
    
    if (multisample) { 
      pgl.glRenderbufferStorageMultisample(PGL.GL_RENDERBUFFER, nsamples, glConst, width, height);
    } else {
      pgl.glRenderbufferStorage(PGL.GL_RENDERBUFFER, glConst, width, height);
    }                    

    pgl.glFramebufferRenderbuffer(PGL.GL_FRAMEBUFFER, PGL.GL_DEPTH_ATTACHMENT, PGL.GL_RENDERBUFFER, glDepthBufferID);

    pg.popFramebuffer();
  }
    
  
  protected void createStencilBuffer() {
    if (screenFb) return;
    
    if (width == 0 || height == 0) {
      throw new RuntimeException("PFramebuffer: size undefined.");
    }

    pg.pushFramebuffer();
    pg.setFramebuffer(this);

    glStencilBufferID = pg.createRenderBufferObject(context.code());
    pgl.glBindRenderbuffer(PGL.GL_RENDERBUFFER, glStencilBufferID);

    int glConst = PGL.GL_STENCIL_INDEX1;
    if (stencilBits == 1) {
      glConst = PGL.GL_STENCIL_INDEX1; 
    } else if (stencilBits == 4) {
      glConst = PGL.GL_STENCIL_INDEX4;
    } else if (stencilBits == 8) {
      glConst = PGL.GL_STENCIL_INDEX8;              
    }
    if (multisample) { 
      pgl.glRenderbufferStorageMultisample(PGL.GL_RENDERBUFFER, nsamples, glConst, width, height);
    } else {      
      pgl.glRenderbufferStorage(PGL.GL_RENDERBUFFER, glConst, width, height);
    }
    
    pgl.glFramebufferRenderbuffer(PGL.GL_FRAMEBUFFER, PGL.GL_STENCIL_ATTACHMENT, PGL.GL_RENDERBUFFER, glStencilBufferID);

    pg.popFramebuffer();
  }  
  
  
  protected void createPixelBuffer() {
    pixelBuffer = IntBuffer.allocate(width * height);
    pixelBuffer.rewind();     
  }  
}
