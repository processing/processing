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
import javax.microedition.khronos.opengles.GL10;
import javax.microedition.khronos.opengles.GL11ExtensionPack;


/**
 * Encapsulates a Frame Buffer Object for offscreen rendering.
 * When created with onscreen == true, it represents the normal
 * framebuffer. Needed by the stack mechanism in A3D to return
 * to onscreen rendering after a sequence of pushFramebuffer calls.
 * It transparently handles the situations when the FBO extension is
 * not available.
 * 
 * By Andres Colubri.
 */
public class PFramebuffer implements PConstants {  
  protected PApplet parent;  
  protected PGraphicsAndroid3D a3d;
  
  public int glFboID;
  public int glDepthBufferID;
  public int glStencilBufferID;
  public int width;
  public int height;
  
  protected int depthBits;
  protected int stencilBits;
  
  protected int numColorBuffers;
  protected int[] colorBufferAttchPoints;
  protected PTexture[] colorBufferTex;

  protected boolean screenFb;
  protected boolean noDepth;
  protected boolean fboMode;
   
  protected PTexture backupTexture;
  protected IntBuffer pixelBuffer;

  PFramebuffer(PApplet parent) {
    this(parent, 0, 0, false);
  }  
  
  PFramebuffer(PApplet parent, int w, int h) {
    this(parent, w, h, 1, 1, 0, 0, false);
  }
  
  PFramebuffer(PApplet parent, int w, int h, boolean screen) {    
    this(parent, w, h, 1, 1, 0, 0, screen);
  }  
  
  PFramebuffer(PApplet parent, int w, int h, int samples, int colorBuffers, 
               int depthBits, int stencilBits, boolean screen) {    
    this.parent = parent;
    a3d = (PGraphicsAndroid3D)parent.g;

    // We need gl11 extension pack at least to bind a zero (screen) buffer.    
    if (a3d.gl11xp == null) {
      throw new RuntimeException("PFramebuffer: OpenGL ES 1.1 Extension Pack required");
    }    
    
    a3d.registerPGLObject(this);
    
    glFboID = 0;
    glDepthBufferID = 0;
    glStencilBufferID = 0;
    
    fboMode = PGraphicsAndroid3D.fboSupported;
    
    if (screen) {
      // If this framebuffer is used to represent a on-screen buffer,
      // then it doesn't make it sense for it to have multisampling,
      // color, depth or stencil buffers.
      depthBits = stencilBits = samples = colorBuffers = 0; 
    }
    
    width = w;
    height = h;    
    
    numColorBuffers = colorBuffers;
    colorBufferAttchPoints = new int[numColorBuffers];
    colorBufferTex = new PTexture[numColorBuffers];
    for (int i = 0; i < numColorBuffers; i++) {
      colorBufferAttchPoints[i] = GL11ExtensionPack.GL_COLOR_ATTACHMENT0_OES + i; 
      colorBufferTex[i] = null;
    }    
    
    if (depthBits < 1 && stencilBits < 1) {
      this.depthBits = 0;
      this.stencilBits = 0;
    } else {
      this.depthBits = depthBits;
      this.stencilBits = stencilBits;        
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

  public void delete() {
    release();
    for (int i = 0; i < numColorBuffers; i++) {
      colorBufferTex[i] = null;  
    }
    a3d.unregisterPGLObject(this);
  }

  public void backup() {
  }
  
  public void restore() {    
    setColorBuffers(colorBufferTex.clone(), colorBufferTex.length);
  }    
  
  public void clear() {
    a3d.pushFramebuffer();
    a3d.setFramebuffer(this);
    getGl().glClearColor(0f, 0f, 0f, 0.0f);
    getGl().glClear(GL10.GL_COLOR_BUFFER_BIT | GL10.GL_DEPTH_BUFFER_BIT | GL10.GL_STENCIL_BUFFER_BIT);    
    a3d.popFramebuffer();    
  }  
  
  public void bind() {
    if (screenFb) {
      if (PGraphicsAndroid3D.fboSupported) {
        getGlx().glBindFramebufferOES(GL11ExtensionPack.GL_FRAMEBUFFER_OES, 0);
      }
    } else if (fboMode) {
      getGlx().glBindFramebufferOES(GL11ExtensionPack.GL_FRAMEBUFFER_OES, glFboID);  
    } else {
      backupScreen();
      if (noDepth) {
        getGl().glDisable(GL10.GL_DEPTH_TEST); 
      }
    }
  }
  
  public void disableDepthTest() {
    noDepth = true;  
  }
  
  public void finish() {
    if (noDepth) {
      // No need to clear depth buffer because depth testing was disabled.
      if (a3d.hints[DISABLE_DEPTH_TEST]) {
        getGl().glDisable(GL10.GL_DEPTH_TEST);
      } else {
        getGl().glEnable(GL10.GL_DEPTH_TEST);
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
        getGl().glClear(GL10.GL_DEPTH_BUFFER_BIT);
      }
    }
  }
    
  // Saves content of the screen into the backup texture.
  public void backupScreen() {  
    if (pixelBuffer == null) createPixelBuffer();
    getGl().glReadPixels(0, 0, width, height, GL10.GL_RGBA, GL10.GL_UNSIGNED_BYTE, pixelBuffer);    
    copyToTexture(pixelBuffer, backupTexture.glID, backupTexture.glTarget);
  }

  // Draws the contents of the backup texture to the screen.
  public void restoreBackup() {
    a3d.drawTexture(backupTexture, 0, 0, width, height, 0, 0, width, height);
  }
  
  // Copies current content of screen to color buffers.
  public void copyToColorBuffers() {
    if (pixelBuffer == null) createPixelBuffer();
    getGl().glReadPixels(0, 0, width, height, GL10.GL_RGBA, GL10.GL_UNSIGNED_BYTE, pixelBuffer);
    for (int i = 0; i < numColorBuffers; i++) {
      copyToTexture(pixelBuffer, colorBufferTex[i].glID, colorBufferTex[i].glTarget);
    }
  }  
  
  public void readPixels() {
    if (pixelBuffer == null) createPixelBuffer();
    getGl().glReadPixels(0, 0, width, height, GL10.GL_RGBA, GL10.GL_UNSIGNED_BYTE, pixelBuffer);
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
      a3d.pushFramebuffer();
      a3d.setFramebuffer(this);

      // Making sure nothing is attached.
      for (int i = 0; i < numColorBuffers; i++) {
        getGlx().glFramebufferTexture2DOES(GL11ExtensionPack.GL_FRAMEBUFFER_OES, 
                                           GL11ExtensionPack.GL_COLOR_ATTACHMENT0_OES + i, 
                                           GL10.GL_TEXTURE_2D, 0, 0);      
      }

      for (int i = 0; i < numColorBuffers; i++) {
        getGlx().glFramebufferTexture2DOES(GL11ExtensionPack.GL_FRAMEBUFFER_OES, colorBufferAttchPoints[i],
                                           colorBufferTex[i].glTarget, colorBufferTex[i].glID, 0);
      }

      validateFbo();

      a3d.popFramebuffer();
    }
  }
  
  
  ///////////////////////////////////////////////////////////  

  // Allocate/release framebuffer.  
  
  
  protected void allocate() {
    release(); // Just in the case this object is being re-initialized.
        
    if (screenFb) {
      glFboID = 0;
    } else if (fboMode) {
      glFboID = a3d.createGLResource(PGraphicsAndroid3D.GL_FRAME_BUFFER); 
    }  else {
      glFboID = 0;
    }
    
    // create the rest of the stuff...   
    if (0 < depthBits) {
      createDepthBuffer();
    }
    if (0 < stencilBits) {
      createStencilBuffer();
    }           
  }
  
  protected void release() {
    deleteFbo();
    deleteDepthBuffer();    
    deleteStencilBuffer();
  }

  
  protected void deleteFbo() {
    if (glFboID != 0) {
      a3d.deleteGLResource(glFboID, PGraphicsAndroid3D.GL_FRAME_BUFFER);
      glFboID = 0;
    }    
  }
  
  
  protected void deleteDepthBuffer() {
    if (glDepthBufferID != 0) {
      a3d.deleteGLResource(glDepthBufferID, PGraphicsAndroid3D.GL_RENDER_BUFFER);
      glDepthBufferID = 0;
    }    
  }
  
  
  protected void deleteStencilBuffer() {
    if (glStencilBufferID != 0) {
      a3d.deleteGLResource(glStencilBufferID, PGraphicsAndroid3D.GL_RENDER_BUFFER);
      glStencilBufferID = 0;
    }     
  }
  
  
  protected void createDepthBuffer() {
    if (screenFb) return;
    
    if (width == 0 || height == 0) {
      throw new RuntimeException("PFramebuffer: size undefined.");
    }
    
    if (fboMode) {
      a3d.pushFramebuffer();
      a3d.setFramebuffer(this);

      glDepthBufferID = a3d.createGLResource(PGraphicsAndroid3D.GL_RENDER_BUFFER);
      getGlx().glBindRenderbufferOES(GL11ExtensionPack.GL_RENDERBUFFER_OES, glDepthBufferID);

      int glConst = GL11ExtensionPack.GL_DEPTH_COMPONENT16;
      if (depthBits == 16) {
        glConst = GL11ExtensionPack.GL_DEPTH_COMPONENT16; 
      } else if (depthBits == 24) {
        glConst = GL11ExtensionPack.GL_DEPTH_COMPONENT24;
      } else if (depthBits == 32) {
        glConst = GL11ExtensionPack.GL_DEPTH_COMPONENT32;              
      }
      getGlx().glRenderbufferStorageOES(GL11ExtensionPack.GL_RENDERBUFFER_OES, glConst, width, height);              

      getGlx().glFramebufferRenderbufferOES(GL11ExtensionPack.GL_FRAMEBUFFER_OES,            
                                            GL11ExtensionPack.GL_DEPTH_ATTACHMENT_OES,
                                            GL11ExtensionPack.GL_RENDERBUFFER_OES, glDepthBufferID);

      a3d.popFramebuffer();
    }
  }
    
  protected void createStencilBuffer() {
    if (screenFb) return;
    
    if (width == 0 || height == 0) {
      throw new RuntimeException("PFramebuffer: size undefined.");
    }

    if (fboMode) {    
      a3d.pushFramebuffer();
      a3d.setFramebuffer(this);

      glStencilBufferID = a3d.createGLResource(PGraphicsAndroid3D.GL_RENDER_BUFFER);
      getGlx().glBindRenderbufferOES(GL11ExtensionPack.GL_RENDERBUFFER_OES, glStencilBufferID);

      int glConst = GL11ExtensionPack.GL_STENCIL_INDEX1_OES;
      if (stencilBits == 1) {
        glConst = GL11ExtensionPack.GL_STENCIL_INDEX1_OES; 
      } else if (stencilBits == 4) {
        glConst = GL11ExtensionPack.GL_STENCIL_INDEX4_OES;
      } else if (stencilBits == 8) {
        glConst = GL11ExtensionPack.GL_STENCIL_INDEX8_OES;              
      }
      getGlx().glRenderbufferStorageOES(GL11ExtensionPack.GL_RENDERBUFFER_OES, glConst, width, height);              

      getGlx().glFramebufferRenderbufferOES(GL11ExtensionPack.GL_FRAMEBUFFER_OES,
                                            GL11ExtensionPack.GL_STENCIL_ATTACHMENT_OES,
                                            GL11ExtensionPack.GL_RENDERBUFFER_OES, glStencilBufferID);

      a3d.popFramebuffer();
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
    getGl().glEnable(gltarget);
    getGl().glBindTexture(gltarget, glid);    
    getGl().glTexSubImage2D(gltarget, 0, 0, 0, width, height, GL10.GL_RGBA, GL10.GL_UNSIGNED_BYTE, buffer);
    getGl().glBindTexture(gltarget, 0);
    getGl().glDisable(gltarget);
  }
    
  public boolean validateFbo() {
    int status = getGlx().glCheckFramebufferStatusOES(GL11ExtensionPack.GL_FRAMEBUFFER_OES);        
    if (status == GL11ExtensionPack.GL_FRAMEBUFFER_COMPLETE_OES) {
      return true;
    } else if (status == GL11ExtensionPack.GL_FRAMEBUFFER_INCOMPLETE_ATTACHMENT_OES) {
      throw new RuntimeException("PFramebuffer: GL_FRAMEBUFFER_INCOMPLETE_ATTACHMENT_OES (" + Integer.toHexString(status) + ")");
    } else if (status == GL11ExtensionPack.GL_FRAMEBUFFER_INCOMPLETE_MISSING_ATTACHMENT_OES) {
      throw new RuntimeException("PFramebuffer: GL_FRAMEBUFFER_INCOMPLETE_MISSING_ATTACHMENT_OES (" + Integer.toHexString(status) + ")");
    } else if (status == GL11ExtensionPack.GL_FRAMEBUFFER_INCOMPLETE_DIMENSIONS_OES) {
      throw new RuntimeException("PFramebuffer: GL_FRAMEBUFFER_INCOMPLETE_DIMENSIONS_OES (" + Integer.toHexString(status) + ")");      
    } else if (status == GL11ExtensionPack.GL_FRAMEBUFFER_INCOMPLETE_FORMATS_OES) {
      throw new RuntimeException("PFramebuffer: GL_FRAMEBUFFER_INCOMPLETE_FORMATS_OES (" + Integer.toHexString(status) + ")");      
    } else if (status == GL11ExtensionPack.GL_FRAMEBUFFER_INCOMPLETE_DRAW_BUFFER_OES) {
      throw new RuntimeException("PFramebuffer: GL_FRAMEBUFFER_INCOMPLETE_DRAW_BUFFER_OES (" + Integer.toHexString(status) + ")");      
    } else if (status == GL11ExtensionPack.GL_FRAMEBUFFER_INCOMPLETE_READ_BUFFER_OES) {
      throw new RuntimeException("PFramebuffer: GL_FRAMEBUFFER_INCOMPLETE_READ_BUFFER_OES (" + Integer.toHexString(status) + ")");      
    } else if (status == GL11ExtensionPack.GL_FRAMEBUFFER_UNSUPPORTED_OES) {
      throw new RuntimeException("PFramebuffer: GL_FRAMEBUFFER_UNSUPPORTED_OES" + Integer.toHexString(status));      
    } else {
      throw new RuntimeException("PFramebuffer: unknown framebuffer error (" + Integer.toHexString(status) + ")");
    }
  }
  
  protected GL10 getGl() {
    return a3d.gl;
  }
  
  protected GL11ExtensionPack getGlx() {
    return a3d.gl11xp;    
  }   
}
