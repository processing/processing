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

import java.lang.reflect.Method;
import javax.microedition.khronos.opengles.GL10;
import javax.microedition.khronos.opengles.GL11ExtensionPack;

/**
 * Encapsulates a Frame Buffer Object for offscreen rendering.
 * When created with onscreen == true, it represents the normal
 * framebuffer. Needed by the stack mechanism in A3D to return
 * to onscreen rendering after a sequence of pushFramebuffer calls.
 * 
 * By Andres Colubri.
 */
public class PFramebuffer  {
  
  protected PApplet parent;  
  protected PGraphicsAndroid3D a3d;
  protected GL11ExtensionPack gl;  
  protected int glFboID;
  protected int glDepthBufferID;
  protected int glStencilBufferID;
  protected int width;
  protected int height;

  protected int recreateResourceIdx;  
  
  protected int numColorBuffers;
  int[] colorBufferAttchPoints;
  int[] glColorBufferTargets;
  int[] glColorBufferIDs;

  PFramebuffer(PApplet parent) {
    this(parent, 0, 0, false);
  }  
  
  PFramebuffer(PApplet parent, int w, int h) {
    this(parent, w, h, false);
  }
  
  PFramebuffer(PApplet parent, int w, int h, boolean onscreen) {
    this.parent = parent;
    a3d = (PGraphicsAndroid3D)parent.g;
    
    // Checking we have what we need:    
    gl = a3d.gl11xp;
    if (gl == null) {
      throw new RuntimeException("PFramebuffer: OpenGL ES 1.1 Extension Pack required");
    }    
    if (!a3d.fboSupported && !onscreen) {
       throw new RuntimeException("PFramebuffer: Frame Buffer Objects are not available");
    }
    
    initFramebuffer(w, h, onscreen);
    
    try {
      Method meth = this.getClass().getMethod("recreateResource", new Class[] { PGraphicsAndroid3D.class });
      recreateResourceIdx =  a3d.addRecreateResourceMethod(this, meth);
    } catch (Exception e) {
      recreateResourceIdx = -1;
    }    
  }

  protected void finalize() {
    a3d.removeRecreateResourceMethod(recreateResourceIdx);    
    deleteFramebuffer();
  }

  void addColorBuffer(PTexture tex) {
    addColorBuffers(new PTexture[] { tex }, 1);
  }

  void addColorBuffers(PTexture[] textures) {
    addColorBuffers(textures, textures.length);
  }

  void addColorBuffers(PTexture[] textures, int n) {
    a3d.pushFramebuffer();
    a3d.setFramebuffer(this);

    // Making sure nothing is attached.
    for (int i = 0; i < numColorBuffers; i++) {
      gl.glFramebufferTexture2DOES(GL11ExtensionPack.GL_FRAMEBUFFER_OES,
                                                              GL11ExtensionPack.GL_COLOR_ATTACHMENT0_OES + i, GL10.GL_TEXTURE_2D, 0, 0);      
    }
    
    numColorBuffers = PApplet.min(n, textures.length);
    colorBufferAttchPoints = new int[numColorBuffers];
    glColorBufferTargets = new int[numColorBuffers];
    glColorBufferIDs = new int[numColorBuffers];
    
    for (int i = 0; i < numColorBuffers; i++) {
      colorBufferAttchPoints[i] = GL11ExtensionPack.GL_COLOR_ATTACHMENT0_OES + i;
      glColorBufferTargets[i] = textures[i].getGLTarget();
      glColorBufferIDs[i] = textures[i].getGLTextureID();
      gl.glFramebufferTexture2DOES(GL11ExtensionPack.GL_FRAMEBUFFER_OES, colorBufferAttchPoints[i],
          glColorBufferTargets[i], glColorBufferIDs[i], 0);
    }
    
    if (validFbo() && textures != null && 0 < textures.length) {
      width = textures[0].getGLWidth();
      height = textures[0].getGLHeight();
    }

    a3d.popFramebuffer();    
  }
  
  public void addDepthBuffer(int bits) {
    if (width == 0 || height == 0) {
      throw new RuntimeException("PFramebuffer: size undefined.");
    }
    
    a3d.pushFramebuffer();
    a3d.setFramebuffer(this);

    int[] tmp = new int[1];
    gl.glGenRenderbuffersOES(1, tmp, 0);
    glDepthBufferID = tmp[0];
    
    gl.glBindRenderbufferOES(GL11ExtensionPack.GL_RENDERBUFFER_OES, glDepthBufferID);
    
    int glConst = GL11ExtensionPack.GL_DEPTH_COMPONENT16;
    if (bits == 16) {
      glConst = GL11ExtensionPack.GL_DEPTH_COMPONENT16; 
    } else if (bits == 24) {
      glConst = GL11ExtensionPack.GL_DEPTH_COMPONENT24;
    } else if (bits == 32) {
      glConst = GL11ExtensionPack.GL_DEPTH_COMPONENT32;              
    }
    gl.glRenderbufferStorageOES(GL11ExtensionPack.GL_RENDERBUFFER_OES, glConst, width, height);              
    
    gl.glFramebufferRenderbufferOES(GL11ExtensionPack.GL_FRAMEBUFFER_OES,            
                                                                  GL11ExtensionPack.GL_DEPTH_ATTACHMENT_OES,
                                                                  GL11ExtensionPack.GL_RENDERBUFFER_OES, glDepthBufferID);
            
    a3d.popFramebuffer();
  }
    
  public void addStencilBuffer(int bits) {
    if (width == 0 || height == 0) {
      throw new RuntimeException("PFramebuffer: size undefined.");
    }

    a3d.pushFramebuffer();
    a3d.setFramebuffer(this);

    int[] tmp = new int[1];
    gl.glGenRenderbuffersOES(1, tmp, 0);
    glStencilBufferID = tmp[0];
    
    gl.glBindRenderbufferOES(GL11ExtensionPack.GL_RENDERBUFFER_OES, glStencilBufferID);
            
    int glConst = GL11ExtensionPack.GL_STENCIL_INDEX1_OES;
    if (bits == 1) {
      glConst = GL11ExtensionPack.GL_STENCIL_INDEX1_OES; 
    } else if (bits == 4) {
      glConst = GL11ExtensionPack.GL_STENCIL_INDEX4_OES;
    } else if (bits == 8) {
      glConst = GL11ExtensionPack.GL_STENCIL_INDEX8_OES;              
    }
    gl.glRenderbufferStorageOES(GL11ExtensionPack.GL_RENDERBUFFER_OES, glConst, width, height);              
              
    gl.glFramebufferRenderbufferOES(GL11ExtensionPack.GL_FRAMEBUFFER_OES,
                                                                  GL11ExtensionPack.GL_STENCIL_ATTACHMENT_OES,
                                                                  GL11ExtensionPack.GL_RENDERBUFFER_OES, glStencilBufferID);
    
    a3d.popFramebuffer();            
  }
  
  public void bind() {
    if (a3d.fboSupported) {
      gl.glBindFramebufferOES(GL11ExtensionPack.GL_FRAMEBUFFER_OES, glFboID);  
    }
  }
    
  protected void initFramebuffer(int w, int h, boolean onscreen) {
    width = w;
    height = h;
        
    if (onscreen) {
      // On-screen buffer has no associated FBO.
      glFboID = 0;
    } else {  
      int[] tmp = new int[1];
      gl.glGenFramebuffersOES(1, tmp, 0);
      glFboID = tmp[0];
    }    
  }

  protected void deleteFramebuffer() {
    if (glFboID != 0) {
      int[] tmp = { glFboID };
      gl.glDeleteFramebuffersOES(1, tmp, 0);
      glFboID = 0;
    }
    
    if (glDepthBufferID !=  0) {
      int[] tmp = { glDepthBufferID };
      gl.glDeleteRenderbuffersOES(1, tmp, 0);
      glDepthBufferID = 0;
    }    
    
    if (glStencilBufferID !=  0) {
      int[] tmp = { glStencilBufferID };
      gl.glDeleteRenderbuffersOES(1, tmp, 0);
      glStencilBufferID = 0;
    }
    
    width = height = 0;
  }
  
  protected void recreateResource(PGraphicsAndroid3D renderer) {
    // Recreate GL resources (buffers, etc).
  }
  
  public boolean validFbo() {
    int status = gl.glCheckFramebufferStatusOES(GL11ExtensionPack.GL_FRAMEBUFFER_OES);        
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
}
