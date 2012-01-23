/* -*- mode: java; c-basic-offset: 2; indent-tabs-mode: nil -*- */

/*
 Part of the Processing project - http://processing.org

 Copyright (c) 2010 Ben Fry and Casey Reas

 This library is free software; you can redistribute it and/or
 modify it under the terms of the GNU Lesser General Public
 License version 2.1 as published by the Free Software Foundation.

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
public class PFramebuffer implements PConstants {  
  protected PApplet parent;
  protected PGraphicsAndroid3D renderer;
  protected PGL pgl;
  
  public int glFboID;
  public int glDepthBufferID;
  public int glStencilBufferID;
  public int glDepthStencilBufferID;
  public int glColorBufferMultisampleID;
  public int width;
  public int height;

  protected int depthBits;
  protected int stencilBits;
  protected boolean combinedDepthStencil;
  
  protected boolean multisample;
  protected int nsamples;
  
  protected int numColorBuffers;
  protected PTexture[] colorBufferTex;

  protected boolean screenFb;
  protected boolean noDepth;
  protected boolean fboMode;  
   
  protected PTexture backupTexture;
  protected IntBuffer pixelBuffer;

  PFramebuffer(PApplet parent, int w, int h) {
    this(parent, w, h, 1, 1, 0, 0, false, false);
  }  
  
  PFramebuffer(PApplet parent, int w, int h, boolean screen) {    
    this(parent, w, h, 1, 1, 0, 0, false, screen);
  }

  PFramebuffer(PApplet parent, int w, int h, int samples, int colorBuffers, 
               int depthBits, int stencilBits, boolean combinedDepthStencil, 
               boolean screen) {
    this.parent = parent;
    renderer = (PGraphicsAndroid3D)parent.g;
    pgl = renderer.pgl;
    
    glFboID = 0;
    glDepthBufferID = 0;
    glStencilBufferID = 0;
    glDepthStencilBufferID = 0;    
    glColorBufferMultisampleID = 0;
        
    fboMode = PGraphicsAndroid3D.fboSupported;
    
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
    colorBufferTex = new PTexture[numColorBuffers];
    for (int i = 0; i < numColorBuffers; i++) {
      colorBufferTex[i] = null;
    }    
    
    if (depthBits < 1 && stencilBits < 1) {
      this.depthBits = 0;
      this.stencilBits = 0;
      this.combinedDepthStencil = false;
    } else {
      if (combinedDepthStencil) {
        // When combined depth/stencil format is required, the depth and stencil bits
        // are overriden and the 24/8 combination for a 32 bits surface is used. 
        this.depthBits = 24;
        this.stencilBits = 8;
        this.combinedDepthStencil = true;        
      } else {
        this.depthBits = depthBits;
        this.stencilBits = stencilBits;
        this.combinedDepthStencil = false;        
      }
    }
    
    screenFb = screen;

    allocate();
    noDepth = false;
    
    pixelBuffer = null;
    
    if (!screenFb && !fboMode) {
      // When FBOs are not available, rendering to texture is implemented by saving a portion of
      // the screen, doing the "offscreen" rendering on this portion, copying the screen color 
      // buffer to the texture bound as color buffer to this PFramebuffer object and then drawing 
      // the backup texture back on the screen.
      backupTexture = new PTexture(parent, width, height, new PTexture.Parameters(ARGB, POINT));       
    }
  }

  
  protected void finalize() throws Throwable {
    try {
      if (glFboID != 0) {
        renderer.finalizeFrameBufferObject(glFboID);
      }      
      if (glDepthBufferID != 0) {
        renderer.finalizeRenderBufferObject(glDepthBufferID);
      }      
      if (glStencilBufferID != 0) {
        renderer.finalizeRenderBufferObject(glStencilBufferID);
      }
      if (glColorBufferMultisampleID != 0) {
        renderer.finalizeRenderBufferObject(glColorBufferMultisampleID);
      }
      if (glDepthStencilBufferID != 0) {
        renderer.finalizeRenderBufferObject(glDepthStencilBufferID);
      }      
    } finally {
      super.finalize();
    }
  }  
  
  public void clear() {
    renderer.pushFramebuffer();
    renderer.setFramebuffer(this);
    pgl.setClearColor(0, 0, 0, 0);
    pgl.clearAllBuffers();
    renderer.popFramebuffer();    
  }
  
  public void copy(PFramebuffer dest) {
    pgl.bindReadFramebuffer(this.glFboID);
    pgl.bindWriteFramebuffer(dest.glFboID);    
    pgl.copyFramebuffer(this.width, this.height, dest.width, dest.height);
  }
  
  public void bind() {
    if (screenFb) {
      if (PGraphicsAndroid3D.fboSupported) {
        pgl.bindFramebuffer(0);
      }
    } else if (fboMode) {
      pgl.bindFramebuffer(glFboID);
    } else {
      backupScreen();
      
      if (0 < numColorBuffers) {
        // Drawing the current contents of the first color buffer to emulate
        // front-back buffer swap.
        renderer.drawTexture(colorBufferTex[0].glTarget, colorBufferTex[0].glID, width, height, 0, 0, width, height, 0, 0, width, height);
      }
      
      if (noDepth) {
        pgl.disableDepthTest();
      }
    }
  }
  
  public void disableDepthTest() {
    noDepth = true;  
  }
  
  public void finish() {
    if (noDepth) {
      // No need to clear depth buffer because depth testing was disabled.
      if (renderer.hintEnabled(DISABLE_DEPTH_TEST)) {
        pgl.disableDepthTest();
      } else {
        pgl.enableDepthTest();
      }        
    }
    
    if (!screenFb && !fboMode) {
      copyToColorBuffers();
      restoreBackup();
      if (!noDepth) {
        // Reading the contents of the depth buffer is not possible in OpenGL ES:
        // http://www.idevgames.com/forum/archive/index.php?t-15828.html
        // so if this framebuffer uses depth and is offscreen with no FBOs, then
        // the depth buffer is cleared to avoid artifacts when rendering more stuff
        // after this offscreen render.
        // A consequence of this behavior is that all the offscreen rendering when
        // no FBOs are available should be done before any onscreen drawing.
        pgl.setClearColor(0, 0, 0, 0);
        pgl.clearDepthBuffer();
      }
    }
  }
    
  // Saves content of the screen into the backup texture.
  public void backupScreen() {  
    if (pixelBuffer == null) createPixelBuffer();    
    pixelBuffer.rewind();
    pgl.readPixels(pixelBuffer, 0, 0, width, height);
    
    copyToTexture(pixelBuffer, backupTexture.glID, backupTexture.glTarget);
  }

  // Draws the contents of the backup texture to the screen.
  public void restoreBackup() {
    renderer.drawTexture(backupTexture, 0, 0, width, height, 0, 0, width, height);
  }
  
  // Copies current content of screen to color buffers.
  public void copyToColorBuffers() {
    if (pixelBuffer == null) createPixelBuffer();
    pixelBuffer.rewind();
    pgl.readPixels(pixelBuffer, 0, 0, width, height);
    for (int i = 0; i < numColorBuffers; i++) {
      copyToTexture(pixelBuffer, colorBufferTex[i].glID, colorBufferTex[i].glTarget);
    }
  }  
  
  public void readPixels() {
    if (pixelBuffer == null) createPixelBuffer();
    pixelBuffer.rewind();
    pgl.readPixels(pixelBuffer, 0, 0, width, height);
  }
  
  public void getPixels(int[] pixels) {
    if (pixelBuffer != null) {
      pixelBuffer.get(pixels);
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
  
  
  public void setColorBuffer(PTexture tex) {
    setColorBuffers(new PTexture[] { tex }, 1);
  }
  

  public void setColorBuffers(PTexture[] textures) {
    setColorBuffers(textures, textures.length);
  }
  
  
  public void setColorBuffers(PTexture[] textures, int n) {
    if (screenFb) return;

    if (numColorBuffers != PApplet.min(n, textures.length)) {
      throw new RuntimeException("Wrong number of textures to set the color buffers.");
    }
        
    for (int i = 0; i < numColorBuffers; i++) {
      colorBufferTex[i] = textures[i];
    }
      
    if (fboMode) {
      renderer.pushFramebuffer();
      renderer.setFramebuffer(this);

      // Making sure nothing is attached.
      for (int i = 0; i < numColorBuffers; i++) {
        pgl.cleanFramebufferTexture(i);
      }

      for (int i = 0; i < numColorBuffers; i++) {
        pgl.setFramebufferTexture(i, colorBufferTex[i].glTarget, colorBufferTex[i].glID);        
      }

      validateFbo();

      renderer.popFramebuffer();
    }
  }  
  
  
  ///////////////////////////////////////////////////////////  

  // Allocate/release framebuffer.   
  
  
  protected void allocate() {
    release(); // Just in the case this object is being re-allocated.    
    
    if (screenFb) {
      glFboID = 0;
    } else if (fboMode) {
      //glFboID = ogl.createGLResource(PGraphicsAndroid3D.GL_FRAME_BUFFER); 
      glFboID = renderer.createFrameBufferObject();
    }  else {
      glFboID = 0;
    }
    
    // create the rest of the stuff...
    if (multisample) {
      createColorBufferMultisample();
    }
   
    if (combinedDepthStencil) {
      createCombinedDepthStencilBuffer();
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
      renderer.finalizeFrameBufferObject(glFboID);
      glFboID = 0;
    }
    if (glDepthBufferID != 0) {
      renderer.finalizeRenderBufferObject(glDepthBufferID);
      glDepthBufferID = 0;
    }
    if (glStencilBufferID != 0) {
      renderer.finalizeRenderBufferObject(glStencilBufferID);
      glStencilBufferID = 0;
    }
    if (glColorBufferMultisampleID != 0) {
      renderer.finalizeRenderBufferObject(glColorBufferMultisampleID);
      glColorBufferMultisampleID = 0;
    }
    if (glDepthStencilBufferID != 0) {
      renderer.finalizeRenderBufferObject(glDepthStencilBufferID);
      glDepthStencilBufferID = 0;
    }     
  }
  
  
  protected void createColorBufferMultisample() {
    if (screenFb) return;
    
    if (fboMode) {
      renderer.pushFramebuffer();
      renderer.setFramebuffer(this);      

      glColorBufferMultisampleID = renderer.createRenderBufferObject();
      pgl.bindRenderbuffer(glColorBufferMultisampleID);
      pgl.setRenderbufferNumSamples(nsamples, PGL.RGBA8, width, height);
      pgl.setRenderbufferColorAttachment(glColorBufferMultisampleID);
      
      renderer.popFramebuffer();      
    }
  }
  
  
  protected void createCombinedDepthStencilBuffer() {
    if (screenFb) return;
    
    if (width == 0 || height == 0) {
      throw new RuntimeException("PFramebuffer: size undefined.");
    }
    
    if (fboMode) {    
      renderer.pushFramebuffer();
      renderer.setFramebuffer(this);
      
      glDepthStencilBufferID = renderer.createRenderBufferObject();
      pgl.bindRenderbuffer(glDepthStencilBufferID);      
      
      if (multisample) { 
        pgl.setRenderbufferNumSamples(nsamples, PGL.DEPTH_24BIT_STENCIL_8BIT, width, height);
      } else {
        pgl.setRenderbufferStorage(PGL.DEPTH_24BIT_STENCIL_8BIT, width, height);
      }
      
      pgl.setRenderbufferDepthAttachment(glDepthStencilBufferID);
      pgl.setRenderbufferStencilAttachment(glDepthStencilBufferID);
      
      renderer.popFramebuffer();  
    }    
  }
  
  
  protected void createDepthBuffer() {
    if (screenFb) return;
    
    if (width == 0 || height == 0) {
      throw new RuntimeException("PFramebuffer: size undefined.");
    }
    
    if (fboMode) {
      renderer.pushFramebuffer();
      renderer.setFramebuffer(this);

      glDepthBufferID = renderer.createRenderBufferObject();
      pgl.bindRenderbuffer(glDepthBufferID);

      int glConst = PGL.DEPTH_16BIT;
      if (depthBits == 16) {
        glConst = PGL.DEPTH_16BIT; 
      } else if (depthBits == 24) {
        glConst = PGL.DEPTH_24BIT;
      } else if (depthBits == 32) {
        glConst = PGL.DEPTH_32BIT;              
      }
      
      if (multisample) { 
        pgl.setRenderbufferNumSamples(nsamples, glConst, width, height);
      } else {
        pgl.setRenderbufferStorage(glConst, width, height);
      }                    

      pgl.setRenderbufferDepthAttachment(glDepthBufferID);

      renderer.popFramebuffer();
    }
  }
    
  
  protected void createStencilBuffer() {
    if (screenFb) return;
    
    if (width == 0 || height == 0) {
      throw new RuntimeException("PFramebuffer: size undefined.");
    }

    if (fboMode) {    
      renderer.pushFramebuffer();
      renderer.setFramebuffer(this);

      glStencilBufferID = renderer.createRenderBufferObject();
      pgl.bindRenderbuffer(glStencilBufferID);

      int glConst = PGL.STENCIL_1BIT;
      if (stencilBits == 1) {
        glConst = PGL.STENCIL_1BIT; 
      } else if (stencilBits == 4) {
        glConst = PGL.STENCIL_4BIT;
      } else if (stencilBits == 8) {
        glConst = PGL.STENCIL_8BIT;              
      }
      if (multisample) { 
        pgl.setRenderbufferNumSamples(nsamples, glConst, width, height);
      } else {      
        pgl.setRenderbufferStorage(glConst, width, height);
      }
      
      pgl.setRenderbufferStencilAttachment(glStencilBufferID);

      renderer.popFramebuffer();
    }
  }  
  
  
  protected void createPixelBuffer() {
    pixelBuffer = IntBuffer.allocate(width * height);
    pixelBuffer.rewind();     
  }  
  
  ///////////////////////////////////////////////////////////  

  // Utilities.  
  
  // Internal copy to texture method.
  protected void copyToTexture(IntBuffer buffer, int glid, int gltarget) {
    pgl.enableTexturing(gltarget);
    pgl.bindTexture(gltarget, glid);    
    pgl.copyTexSubImage(buffer, gltarget, 0, 0, 0, width, height);
    pgl.unbindTexture(gltarget);
    pgl.disableTexturing(gltarget);    
  }  
  
  public boolean validateFbo() {
    int status = pgl.getFramebufferStatus();
    if (status == PGL.FRAMEBUFFER_COMPLETE) {
      return true;
    } else if (status == PGL.FRAMEBUFFER_INCOMPLETE_ATTACHMENT) {
      throw new RuntimeException("PFramebuffer: GL_FRAMEBUFFER_INCOMPLETE_ATTACHMENT (" + Integer.toHexString(status) + ")");
    } else if (status == PGL.FRAMEBUFFER_INCOMPLETE_MISSING_ATTACHMENT) {
      throw new RuntimeException("PFramebuffer: GL_FRAMEBUFFER_INCOMPLETE_MISSING_ATTACHMENT (" + Integer.toHexString(status) + ")");
    } else if (status == PGL.FRAMEBUFFER_INCOMPLETE_DIMENSIONS) {
      throw new RuntimeException("PFramebuffer: GL_FRAMEBUFFER_INCOMPLETE_DIMENSIONS (" + Integer.toHexString(status) + ")");      
    } else if (status == PGL.FRAMEBUFFER_INCOMPLETE_FORMATS) {
      throw new RuntimeException("PFramebuffer: GL_FRAMEBUFFER_INCOMPLETE_FORMATS (" + Integer.toHexString(status) + ")");
    } else if (status == PGL.FRAMEBUFFER_UNSUPPORTED) {
      throw new RuntimeException("PFramebuffer: GL_FRAMEBUFFER_UNSUPPORTED" + Integer.toHexString(status));      
    } else {
      throw new RuntimeException("PFramebuffer: unknown framebuffer error (" + Integer.toHexString(status) + ")");
    }
  }
}
