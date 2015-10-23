/* -*- mode: java; c-basic-offset: 2; indent-tabs-mode: nil -*- */

/*
  Part of the Processing project - http://processing.org

  Copyright (c) 2012-15 The Processing Foundation
  Copyright (c) 2004-12 Ben Fry and Casey Reas
  Copyright (c) 2001-04 Massachusetts Institute of Technology

  This library is free software; you can redistribute it and/or
  modify it under the terms of the GNU Lesser General Public
  License as published by the Free Software Foundation, version 2.1.

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
import processing.opengl.PGraphicsOpenGL.GLResourceFrameBuffer;

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
  protected PGraphicsOpenGL pg;
  protected PGL pgl;
  protected int context;   // The context that created this framebuffer.

  public int glFbo;
  public int glDepth;
  public int glStencil;
  public int glDepthStencil;
  public int glMultisample;
  public int width;
  public int height;
  private GLResourceFrameBuffer glres;

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


  FrameBuffer(PGraphicsOpenGL pg) {
    this.pg = pg;
    pgl = pg.pgl;
    context = pgl.createEmptyContext();
  }


  FrameBuffer(PGraphicsOpenGL pg, int w, int h, int samples, int colorBuffers,
              int depthBits, int stencilBits, boolean packedDepthStencil,
              boolean screen) {
    this(pg);

    glFbo = 0;
    glDepth = 0;
    glStencil = 0;
    glDepthStencil = 0;
    glMultisample = 0;

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
        // When combined depth/stencil format is required, the depth and stencil
        // bits are overriden and the 24/8 combination for a 32 bits surface is
        // used.
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


  FrameBuffer(PGraphicsOpenGL pg, int w, int h) {
    this(pg, w, h, 1, 1, 0, 0, false, false);
  }


  FrameBuffer(PGraphicsOpenGL pg, int w, int h, boolean screen) {
    this(pg, w, h, 1, 1, 0, 0, false, screen);
  }


  public void clear() {
    pg.pushFramebuffer();
    pg.setFramebuffer(this);
    pgl.clearDepth(1);
    pgl.clearStencil(0);
    pgl.clearColor(0, 0, 0, 0);
    pgl.clear(PGL.DEPTH_BUFFER_BIT |
              PGL.STENCIL_BUFFER_BIT |
              PGL.COLOR_BUFFER_BIT);
    pg.popFramebuffer();
  }

  public void copyColor(FrameBuffer dest) {
    copy(dest, PGL.COLOR_BUFFER_BIT);
  }

  public void copyDepth(FrameBuffer dest) {
    copy(dest, PGL.DEPTH_BUFFER_BIT);
  }

  public void copyStencil(FrameBuffer dest) {
    copy(dest, PGL.STENCIL_BUFFER_BIT);
  }

  public void copy(FrameBuffer dest, int mask) {
    pgl.bindFramebufferImpl(PGL.READ_FRAMEBUFFER, this.glFbo);
    pgl.bindFramebufferImpl(PGL.DRAW_FRAMEBUFFER, dest.glFbo);
    pgl.blitFramebuffer(0, 0, this.width, this.height,
                        0, 0, dest.width, dest.height, mask, PGL.NEAREST);
    pgl.bindFramebufferImpl(PGL.READ_FRAMEBUFFER, pg.getCurrentFB().glFbo);
    pgl.bindFramebufferImpl(PGL.DRAW_FRAMEBUFFER, pg.getCurrentFB().glFbo);
  }

  public void bind() {
    pgl.bindFramebufferImpl(PGL.FRAMEBUFFER, glFbo);
  }

  public void disableDepthTest() {
    noDepth = true;
  }

  public void finish() {
    if (noDepth) {
      // No need to clear depth buffer because depth testing was disabled.
      if (pg.getHint(ENABLE_DEPTH_TEST)) {
        pgl.enable(PGL.DEPTH_TEST);
      } else {
        pgl.disable(PGL.DEPTH_TEST);
      }
    }
  }

  public void readPixels() {
    if (pixelBuffer == null) createPixelBuffer();
    pixelBuffer.rewind();
    pgl.readPixels(0, 0, width, height, PGL.RGBA, PGL.UNSIGNED_BYTE,
                   pixelBuffer);
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

  public void setFBO(int id) {
    if (screenFb) {
      glFbo = id;
    }
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
      throw new RuntimeException("Wrong number of textures to set the color " +
                                 "buffers.");
    }

    for (int i = 0; i < numColorBuffers; i++) {
      colorBufferTex[i] = textures[i];
    }

    pg.pushFramebuffer();
    pg.setFramebuffer(this);

    // Making sure nothing is attached.
    for (int i = 0; i < numColorBuffers; i++) {
      pgl.framebufferTexture2D(PGL.FRAMEBUFFER, PGL.COLOR_ATTACHMENT0 + i,
                               PGL.TEXTURE_2D, 0, 0);
    }

    for (int i = 0; i < numColorBuffers; i++) {
      pgl.framebufferTexture2D(PGL.FRAMEBUFFER, PGL.COLOR_ATTACHMENT0 + i,
                               colorBufferTex[i].glTarget,
                               colorBufferTex[i].glName, 0);
    }

    pgl.validateFramebuffer();

    pg.popFramebuffer();
  }


  public void swapColorBuffers() {
    for (int i = 0; i < numColorBuffers - 1; i++) {
      int i1 = (i + 1);
      Texture tmp = colorBufferTex[i];
      colorBufferTex[i] = colorBufferTex[i1];
      colorBufferTex[i1] = tmp;
    }

    pg.pushFramebuffer();
    pg.setFramebuffer(this);
    for (int i = 0; i < numColorBuffers; i++) {
      pgl.framebufferTexture2D(PGL.FRAMEBUFFER, PGL.COLOR_ATTACHMENT0 + i,
                               colorBufferTex[i].glTarget,
                               colorBufferTex[i].glName, 0);
    }
    pgl.validateFramebuffer();

    pg.popFramebuffer();
  }


  public int getDefaultReadBuffer() {
    if (screenFb) {
      return pgl.getDefaultReadBuffer();
    } else {
      return PGL.COLOR_ATTACHMENT0;
    }
  }


  public int getDefaultDrawBuffer() {
    if (screenFb) {
      return pgl.getDefaultDrawBuffer();
    } else {
      return PGL.COLOR_ATTACHMENT0;
    }
  }


  ///////////////////////////////////////////////////////////

  // Allocate/release framebuffer.


  protected void allocate() {
    dispose(); // Just in the case this object is being re-allocated.

    context = pgl.getCurrentContext();
    glres = new GLResourceFrameBuffer(this); // create the FBO resources...

    if (screenFb) {
      glFbo = 0;
    } else {
      if (multisample) {
        initColorBufferMultisample();
      }

      if (packedDepthStencil) {
        initPackedDepthStencilBuffer();
      } else {
        if (0 < depthBits) {
          initDepthBuffer();
        }
        if (0 < stencilBits) {
          initStencilBuffer();
        }
      }
    }
  }


  protected void dispose() {
    if (screenFb) return;
    if (glres != null) {
      glres.dispose();
      glFbo = 0;
      glDepth = 0;
      glStencil = 0;
      glMultisample = 0;
      glDepthStencil = 0;
      glres = null;
    }
  }


  protected boolean contextIsOutdated() {
    if (screenFb) return false;

    boolean outdated = !pgl.contextIsCurrent(context);
    if (outdated) {
      dispose();
      for (int i = 0; i < numColorBuffers; i++) {
        colorBufferTex[i] = null;
      }
    }
    return outdated;
  }


  protected void initColorBufferMultisample() {
    if (screenFb) return;

    pg.pushFramebuffer();
    pg.setFramebuffer(this);

    pgl.bindRenderbuffer(PGL.RENDERBUFFER, glMultisample);
    pgl.renderbufferStorageMultisample(PGL.RENDERBUFFER, nsamples,
                                       PGL.RGBA8, width, height);
    pgl.framebufferRenderbuffer(PGL.FRAMEBUFFER, PGL.COLOR_ATTACHMENT0,
                                PGL.RENDERBUFFER, glMultisample);

    pg.popFramebuffer();
  }


  protected void initPackedDepthStencilBuffer() {
    if (screenFb) return;

    if (width == 0 || height == 0) {
      throw new RuntimeException("PFramebuffer: size undefined.");
    }

    pg.pushFramebuffer();
    pg.setFramebuffer(this);

    pgl.bindRenderbuffer(PGL.RENDERBUFFER, glDepthStencil);

    if (multisample) {
      pgl.renderbufferStorageMultisample(PGL.RENDERBUFFER, nsamples,
                                         PGL.DEPTH24_STENCIL8, width, height);
    } else {
      pgl.renderbufferStorage(PGL.RENDERBUFFER, PGL.DEPTH24_STENCIL8,
                              width, height);
    }

    pgl.framebufferRenderbuffer(PGL.FRAMEBUFFER, PGL.DEPTH_ATTACHMENT,
                                PGL.RENDERBUFFER, glDepthStencil);
    pgl.framebufferRenderbuffer(PGL.FRAMEBUFFER, PGL.STENCIL_ATTACHMENT,
                                PGL.RENDERBUFFER, glDepthStencil);

    pg.popFramebuffer();
  }


  protected void initDepthBuffer() {
    if (screenFb) return;

    if (width == 0 || height == 0) {
      throw new RuntimeException("PFramebuffer: size undefined.");
    }

    pg.pushFramebuffer();
    pg.setFramebuffer(this);

    pgl.bindRenderbuffer(PGL.RENDERBUFFER, glDepth);

    int glConst = PGL.DEPTH_COMPONENT16;
    if (depthBits == 16) {
      glConst = PGL.DEPTH_COMPONENT16;
    } else if (depthBits == 24) {
      glConst = PGL.DEPTH_COMPONENT24;
    } else if (depthBits == 32) {
      glConst = PGL.DEPTH_COMPONENT32;
    }

    if (multisample) {
      pgl.renderbufferStorageMultisample(PGL.RENDERBUFFER, nsamples, glConst,
                                         width, height);
    } else {
      pgl.renderbufferStorage(PGL.RENDERBUFFER, glConst, width, height);
    }

    pgl.framebufferRenderbuffer(PGL.FRAMEBUFFER, PGL.DEPTH_ATTACHMENT,
                                PGL.RENDERBUFFER, glDepth);

    pg.popFramebuffer();
  }


  protected void initStencilBuffer() {
    if (screenFb) return;

    if (width == 0 || height == 0) {
      throw new RuntimeException("PFramebuffer: size undefined.");
    }

    pg.pushFramebuffer();
    pg.setFramebuffer(this);

    pgl.bindRenderbuffer(PGL.RENDERBUFFER, glStencil);

    int glConst = PGL.STENCIL_INDEX1;
    if (stencilBits == 1) {
      glConst = PGL.STENCIL_INDEX1;
    } else if (stencilBits == 4) {
      glConst = PGL.STENCIL_INDEX4;
    } else if (stencilBits == 8) {
      glConst = PGL.STENCIL_INDEX8;
    }
    if (multisample) {
      pgl.renderbufferStorageMultisample(PGL.RENDERBUFFER, nsamples, glConst,
                                         width, height);
    } else {
      pgl.renderbufferStorage(PGL.RENDERBUFFER, glConst, width, height);
    }

    pgl.framebufferRenderbuffer(PGL.FRAMEBUFFER, PGL.STENCIL_ATTACHMENT,
                                PGL.RENDERBUFFER, glStencil);

    pg.popFramebuffer();
  }


  protected void createPixelBuffer() {
    pixelBuffer = IntBuffer.allocate(width * height);
    pixelBuffer.rewind();
  }
}
