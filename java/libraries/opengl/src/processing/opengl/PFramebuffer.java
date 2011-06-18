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

package processing.opengl;

import java.nio.IntBuffer;
import javax.media.opengl.GL;
import javax.media.opengl.GL2;
import javax.media.opengl.GL2GL3;
import processing.core.PApplet;
import processing.core.PConstants;

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
  protected PGraphicsOpenGL2 ogl;
  
  public int glFboID;
  public int glDepthBufferID;
  public int glStencilBufferID;
  public int glDepthStencilBufferID;
  public int glColorBufferMultisampleID;
  public int width;
  public int height;
  
  protected int numColorBuffers;
  protected int[] colorBufferAttchPoints;
  protected int[] glColorBufferTargets;
  protected int[] glColorBufferIDs;

  protected boolean screenFb;
  protected boolean noDepth;
  protected boolean fboMode;
  
  protected boolean multisample;
  protected int nsamples;
   
  protected PTexture backupTexture;
  protected IntBuffer pixelBuffer;

  PFramebuffer(PApplet parent) {
    this(parent, 0, 0, false);
  }  
  
  PFramebuffer(PApplet parent, int w, int h) {
    this(parent, w, h, false);
  }
  
  PFramebuffer(PApplet parent, int w, int h, boolean screen) {
    this.parent = parent;
    ogl = (PGraphicsOpenGL2)parent.g;
    
    glFboID = 0;
    glDepthBufferID = 0;
    glStencilBufferID = 0;
    glDepthStencilBufferID = 0;
    glColorBufferMultisampleID = 0;
    
    screenFb = screen;
    noDepth = false;
    fboMode = PGraphicsOpenGL2.fboSupported;
    numColorBuffers = 0;
        
    multisample = false;
    nsamples = 0;
    
    createFramebuffer(w, h);
    
    pixelBuffer = null;
    
    if (!screenFb && !fboMode) {
      // When FBOs are not available, rendering to texture is implemented by saving a portion of
      // the screen, doing the "offscreen" rendering on this portion, copying the screen color 
      // buffer to the texture bound as color buffer to this PFramebuffer object and then drawing 
      // the backup texture back on the screen.
      backupTexture = new PTexture(parent, width, height, new PTexture.Parameters(ARGB, POINT));       
    }  
  }

  public void delete() {
    deleteFramebuffer();
  }

  public void clear() {
    ogl.pushFramebuffer();
    ogl.setFramebuffer(this);
    getGl().glClearColor(0f, 0f, 0f, 0.0f);
    getGl().glClear(GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT | GL.GL_STENCIL_BUFFER_BIT);    
    ogl.popFramebuffer();    
  }
  
  public void copy(PFramebuffer dest) {
    getGl().glBindFramebuffer(GL2.GL_READ_FRAMEBUFFER, this.glFboID);
    getGl().glBindFramebuffer(GL2.GL_DRAW_FRAMEBUFFER, dest.glFboID);
    getGl2().glBlitFramebuffer(0, 0, this.width, this.height, 0, 0, dest.width, dest.height, 
                               GL.GL_COLOR_BUFFER_BIT, GL.GL_NEAREST);
  }
  
  public void setColorBuffer(PTexture tex) {
    setColorBuffers(new PTexture[] { tex }, 1);
  }

  public void setColorBuffers(PTexture[] textures) {
    setColorBuffers(textures, textures.length);
  }
  
  public void setColorBuffers(PTexture[] textures, int n) {
    if (screenFb) return;
    
    if (fboMode) {
      ogl.pushFramebuffer();
      ogl.setFramebuffer(this);

      // Making sure nothing is attached.
      for (int i = 0; i < numColorBuffers; i++) {
        getGl().glFramebufferTexture2D(GL.GL_FRAMEBUFFER, GL.GL_COLOR_ATTACHMENT0 + i, 
                                       GL.GL_TEXTURE_2D, 0, 0);
      }

      numColorBuffers = PApplet.min(n, textures.length);
      colorBufferAttchPoints = new int[numColorBuffers];
      glColorBufferTargets = new int[numColorBuffers];
      glColorBufferIDs = new int[numColorBuffers];

      for (int i = 0; i < numColorBuffers; i++) {
        colorBufferAttchPoints[i] = GL.GL_COLOR_ATTACHMENT0 + i;
        glColorBufferTargets[i] = textures[i].glTarget;
        glColorBufferIDs[i] = textures[i].glID;
        getGl().glFramebufferTexture2D(GL.GL_FRAMEBUFFER, colorBufferAttchPoints[i],
                                       glColorBufferTargets[i], glColorBufferIDs[i], 0);
      }

      if (validFbo() && textures != null && 0 < textures.length) {
        width = textures[0].glWidth;
        height = textures[0].glHeight;
      }

      ogl.popFramebuffer();
    } else {
      numColorBuffers = PApplet.min(n, textures.length);
      glColorBufferTargets = new int[numColorBuffers];
      glColorBufferIDs = new int[numColorBuffers];      
      for (int i = 0; i < numColorBuffers; i++) {
        glColorBufferTargets[i] = textures[i].glTarget;
        glColorBufferIDs[i] = textures[i].glID;
      }
    }
  }
  
  public void addColorBufferMultisample(int samples) {
    if (screenFb) return;
    
    if (fboMode) {
      ogl.pushFramebuffer();
      ogl.setFramebuffer(this);
      
      multisample = true;
      nsamples = samples;
      
      numColorBuffers = 1;
      colorBufferAttchPoints = new int[numColorBuffers];
      colorBufferAttchPoints[0] = GL.GL_COLOR_ATTACHMENT0;
      
      glColorBufferMultisampleID = ogl.createGLResource(PGraphicsOpenGL2.GL_RENDER_BUFFER);
      getGl().glBindRenderbuffer(GL.GL_RENDERBUFFER, glColorBufferMultisampleID);
      getGl2().glRenderbufferStorageMultisample(GL.GL_RENDERBUFFER, nsamples, 
                                                GL.GL_RGBA8, width, height);            
      getGl().glFramebufferRenderbuffer(GL.GL_FRAMEBUFFER, colorBufferAttchPoints[0], 
                                        GL.GL_RENDERBUFFER, glColorBufferMultisampleID);
      
      ogl.popFramebuffer();      
    }
  }
  
  public void addDepthStencilBuffer() {
    if (screenFb) return;
    
    if (width == 0 || height == 0) {
      throw new RuntimeException("PFramebuffer: size undefined.");
    }
    
    if (fboMode) {    
      ogl.pushFramebuffer();
      ogl.setFramebuffer(this);
      
      glDepthStencilBufferID = ogl.createGLResource(PGraphicsOpenGL2.GL_RENDER_BUFFER);
      getGl().glBindRenderbuffer(GL.GL_RENDERBUFFER, glDepthStencilBufferID);
      
      if (multisample) { 
        getGl2().glRenderbufferStorageMultisample(GL.GL_RENDERBUFFER, nsamples, GL.GL_DEPTH24_STENCIL8, width, height);
      } else {
        getGl().glRenderbufferStorage(GL.GL_RENDERBUFFER, GL.GL_DEPTH24_STENCIL8, width, height);
      }
      
      getGl().glFramebufferRenderbuffer(GL.GL_FRAMEBUFFER, GL.GL_DEPTH_ATTACHMENT, 
                                        GL.GL_RENDERBUFFER, glDepthStencilBufferID);
      getGl().glFramebufferRenderbuffer(GL.GL_FRAMEBUFFER, GL.GL_STENCIL_ATTACHMENT, 
                                        GL.GL_RENDERBUFFER, glDepthStencilBufferID);
      
      ogl.popFramebuffer();  
    }    
  }
  
  public void addDepthBuffer(int bits) {
    if (screenFb) return;
    
    if (width == 0 || height == 0) {
      throw new RuntimeException("PFramebuffer: size undefined.");
    }
    
    if (fboMode) {
      ogl.pushFramebuffer();
      ogl.setFramebuffer(this);

      glDepthBufferID = ogl.createGLResource(PGraphicsOpenGL2.GL_RENDER_BUFFER);
      getGl().glBindRenderbuffer(GL.GL_RENDERBUFFER, glDepthBufferID);

      int glConst = GL.GL_DEPTH_COMPONENT16;
      if (bits == 16) {
        glConst = GL.GL_DEPTH_COMPONENT16; 
      } else if (bits == 24) {
        glConst = GL.GL_DEPTH_COMPONENT24;
      } else if (bits == 32) {
        glConst = GL.GL_DEPTH_COMPONENT32;              
      }
      
      if (multisample) { 
        getGl2().glRenderbufferStorageMultisample(GL.GL_RENDERBUFFER, nsamples, glConst, width, height);
      } else {
        getGl().glRenderbufferStorage(GL.GL_RENDERBUFFER, glConst, width, height);  
      }                    

      getGl().glFramebufferRenderbuffer(GL.GL_FRAMEBUFFER, GL.GL_DEPTH_ATTACHMENT,
                                   GL.GL_RENDERBUFFER, glDepthBufferID);

      ogl.popFramebuffer();
    }
  }
    
  public void addStencilBuffer(int bits) {
    if (screenFb) return;
    
    if (width == 0 || height == 0) {
      throw new RuntimeException("PFramebuffer: size undefined.");
    }

    if (fboMode) {    
      ogl.pushFramebuffer();
      ogl.setFramebuffer(this);

      glStencilBufferID = ogl.createGLResource(PGraphicsOpenGL2.GL_RENDER_BUFFER);
      getGl().glBindRenderbuffer(GL.GL_RENDERBUFFER, glStencilBufferID);

      int glConst = GL.GL_STENCIL_INDEX1;
      if (bits == 1) {
        glConst = GL.GL_STENCIL_INDEX1; 
      } else if (bits == 4) {
        glConst = GL.GL_STENCIL_INDEX4;
      } else if (bits == 8) {
        glConst = GL.GL_STENCIL_INDEX8;              
      }
      if (multisample) { 
        getGl2().glRenderbufferStorageMultisample(GL.GL_RENDERBUFFER, nsamples, glConst, width, height);
      } else {      
        getGl().glRenderbufferStorage(GL.GL_RENDERBUFFER, glConst, width, height);              
      }
      getGl().glFramebufferRenderbuffer(GL.GL_FRAMEBUFFER, GL.GL_STENCIL_ATTACHMENT,
                                        GL.GL_RENDERBUFFER, glStencilBufferID);

      ogl.popFramebuffer();
    }
  }
  
  public void bind() {
    if (screenFb) {
      if (PGraphicsOpenGL2.fboSupported) {
        getGl().glBindFramebuffer(GL.GL_FRAMEBUFFER, 0);
      }
    } else if (fboMode) {
      getGl().glBindFramebuffer(GL.GL_FRAMEBUFFER, glFboID);  
    } else {
      backupScreen();
      
      if (0 < numColorBuffers) {
        // Drawing the current contents of the first color buffer to emulate
        // front-back buffer swap.
        ogl.drawTexture(glColorBufferTargets[0], glColorBufferIDs[0], width, height, 0, 0, width, height, 0, 0, width, height);
      }
      
      if (noDepth) {
        getGl().glDisable(GL.GL_DEPTH_TEST); 
      }
    }
  }
  
  public void disableDepthTest() {
    noDepth = true;  
  }
  
  public void finish() {
    if (noDepth) {
      // No need to clear depth buffer because depth testing was disabled.
      if (ogl.hintEnabled(DISABLE_DEPTH_TEST)) {
        getGl().glDisable(GL.GL_DEPTH_TEST);
      } else {
        getGl().glEnable(GL.GL_DEPTH_TEST);
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
        getGl().glClearColor(0, 0, 0, 0);
        getGl().glClear(GL.GL_DEPTH_BUFFER_BIT);
      }
    }
  }
    
  // Saves content of the screen into the backup texture.
  public void backupScreen() {  
    if (pixelBuffer == null) allocatePixelBuffer();
    getGl().glReadPixels(0, 0, width, height, GL.GL_RGBA, GL.GL_UNSIGNED_BYTE, pixelBuffer);    
    copyToTexture(pixelBuffer, backupTexture.glID, backupTexture.glTarget);
  }

  // Draws the contents of the backup texture to the screen.
  public void restoreBackup() {
    ogl.drawTexture(backupTexture, 0, 0, width, height, 0, 0, width, height);
  }
  
  // Copies current content of screen to color buffers.
  public void copyToColorBuffers() {
    if (pixelBuffer == null) allocatePixelBuffer();
    getGl().glReadPixels(0, 0, width, height, GL.GL_RGBA, GL.GL_UNSIGNED_BYTE, pixelBuffer);
    for (int i = 0; i < numColorBuffers; i++) {
      copyToTexture(pixelBuffer, glColorBufferIDs[i], glColorBufferTargets[i]);
    }
  }  
  
  public void readPixels() {
    if (pixelBuffer == null) allocatePixelBuffer();
    getGl().glReadPixels(0, 0, width, height, GL.GL_RGBA, GL.GL_UNSIGNED_BYTE, pixelBuffer);
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
  
  // Internal copy to texture method.
  protected void copyToTexture(IntBuffer buffer, int glid, int gltarget) {
    getGl().glEnable(gltarget);
    getGl().glBindTexture(gltarget, glid);    
    getGl().glTexSubImage2D(gltarget, 0, 0, 0, width, height, GL.GL_RGBA, GL.GL_UNSIGNED_BYTE, buffer);
    getGl().glBindTexture(gltarget, 0);
    getGl().glDisable(gltarget);
  }
  
  protected void allocatePixelBuffer() {
    pixelBuffer = IntBuffer.allocate(width * height);
    pixelBuffer.rewind();     
  }
  
  protected void createFramebuffer(int w, int h) {
    deleteFramebuffer(); // Just in the case this object is being re-initialized.
    
    width = w;
    height = h;
        
    if (screenFb) {
      glFboID = 0;
    } else if (fboMode) {
      glFboID = ogl.createGLResource(PGraphicsOpenGL2.GL_FRAME_BUFFER); 
    }  else {
      glFboID = 0;
    }
  }
  
  protected void deleteFramebuffer() {
    if (glFboID != 0) {
      ogl.deleteGLResource(glFboID, PGraphicsOpenGL2.GL_FRAME_BUFFER);
      glFboID = 0;
    }
    
    if (glDepthBufferID != 0) {
      ogl.deleteGLResource(glDepthBufferID, PGraphicsOpenGL2.GL_RENDER_BUFFER);
      glDepthBufferID = 0;
    }
    
    if (glStencilBufferID != 0) {
      ogl.deleteGLResource(glStencilBufferID, PGraphicsOpenGL2.GL_RENDER_BUFFER);
      glStencilBufferID = 0;
    }
    
    if (glColorBufferMultisampleID != 0) {
      ogl.deleteGLResource(glColorBufferMultisampleID, PGraphicsOpenGL2.GL_RENDER_BUFFER);
      glColorBufferMultisampleID = 0;
    }    

    if (glDepthStencilBufferID != 0) {
      ogl.deleteGLResource(glDepthStencilBufferID, PGraphicsOpenGL2.GL_RENDER_BUFFER);
      glDepthStencilBufferID = 0;
    }    
    
    width = height = 0;    
  }
  
  public boolean validFbo() {
    int status = getGl().glCheckFramebufferStatus(GL.GL_FRAMEBUFFER);        
    if (status == GL.GL_FRAMEBUFFER_COMPLETE) {
      return true;
    } else if (status == GL.GL_FRAMEBUFFER_INCOMPLETE_ATTACHMENT) {
      throw new RuntimeException("PFramebuffer: GL_FRAMEBUFFER_INCOMPLETE_ATTACHMENT (" + Integer.toHexString(status) + ")");
    } else if (status == GL.GL_FRAMEBUFFER_INCOMPLETE_MISSING_ATTACHMENT) {
      throw new RuntimeException("PFramebuffer: GL_FRAMEBUFFER_INCOMPLETE_MISSING_ATTACHMENT (" + Integer.toHexString(status) + ")");
    } else if (status == GL.GL_FRAMEBUFFER_INCOMPLETE_DIMENSIONS) {
      throw new RuntimeException("PFramebuffer: GL_FRAMEBUFFER_INCOMPLETE_DIMENSIONS (" + Integer.toHexString(status) + ")");      
    } else if (status == GL.GL_FRAMEBUFFER_INCOMPLETE_FORMATS) {
      throw new RuntimeException("PFramebuffer: GL_FRAMEBUFFER_INCOMPLETE_FORMATS (" + Integer.toHexString(status) + ")");
    } else if (status == GL.GL_FRAMEBUFFER_UNSUPPORTED) {
      throw new RuntimeException("PFramebuffer: GL_FRAMEBUFFER_UNSUPPORTED" + Integer.toHexString(status));      
    } else {
      throw new RuntimeException("PFramebuffer: unknown framebuffer error (" + Integer.toHexString(status) + ")");
    }
  }

  protected GL getGl() {
    return ogl.gl;
  }
  
  protected GL2GL3 getGl2() {
    return ogl.gl2x;
  }  
}
