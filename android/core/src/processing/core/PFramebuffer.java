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
import java.util.*;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL;
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
  protected GL11ExtensionPack gl;  
  protected PGraphicsAndroid3D a3d;
  protected int glFboID;
  protected int glDepthBufferID;
  protected int glStencilBufferID;
  protected int width;
  protected int height;

  
  
  protected int numDrawBuffersInUse;
  int[] colorDrawBuffers;
  int[] textureIDs;
  int[] textureTargets;
  
  PFramebuffer(PApplet parent, int w, int h, boolean onscreen) {
    
    // Checking we have what we need:
    gl = a3d.gl11xp;
    if (gl == null) {
      throw new RuntimeException("PFramebuffer: OpenGL ES 1.1 Extension Pack required");
    }
    if (!a3d.fboSupported) {
       throw new RuntimeException("PFramebuffer: Frame Buffer Objects are not available");
    }
                
    int[] tmp = new int[1];
    gl.glGenFramebuffersOES(1, tmp, 0);
    glFboID = tmp[0];
  
    width = w;
    height = h;
  }

  
  protected void finalize() {
    //a3d.removeRecreateResourceMethod(recreateResourceIdx);    
    deleteFramebuffer();
  }
  
  
  

  public void  createDepthBuffer(int bits) {
    if (width == 0 || height == 0) {
      // Throw error: size undefined.
    }
    
    a3d.pushFramebuffer();
    a3d.setFramebuffer(this);
    
        // Depth buffer config and attachment.
            int[] tmp = new int[1];
            gl.glGenRenderbuffersOES(1, tmp, 0);
            glDepthBufferID = tmp[0];
    
            gl.glBindRenderbufferOES(GL11ExtensionPack.GL_RENDERBUFFER_OES, glDepthBufferID);
            gl.glRenderbufferStorageOES(GL11ExtensionPack.GL_RENDERBUFFER_OES,
                    GL11ExtensionPack.GL_DEPTH_COMPONENT16, width, height);
            //GL11ExtensionPack.GL_DEPTH_COMPONENT24 for 24 bits depth buffer
            //GL11ExtensionPack.GL_DEPTH_COMPONENT32 for 32 bits depth buffer
            gl.glFramebufferRenderbufferOES(GL11ExtensionPack.GL_FRAMEBUFFER_OES,
                    GL11ExtensionPack.GL_DEPTH_ATTACHMENT_OES,
                    GL11ExtensionPack.GL_RENDERBUFFER_OES, glDepthBufferID);
            
    a3d.popFramebuffer();
  }
  
  
   public void  createStencilBuffer(int bits) {

    a3d.pushFramebuffer();
    a3d.setFramebuffer(this);
     
          // Stencil buffer config and attachment.
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
              
            //_OES for 1 bit stencil
            //GL11ExtensionPack.GL_STENCIL_INDEX4_OES for 4 bit stencil
            gl.glFramebufferRenderbufferOES(GL11ExtensionPack.GL_FRAMEBUFFER_OES,
                    GL11ExtensionPack.GL_STENCIL_ATTACHMENT_OES,
                    GL11ExtensionPack.GL_RENDERBUFFER_OES, glStencilBufferID);

    a3d.popFramebuffer();            
  }
    
  
  
  
  
  
  
  void setColorBuffer(PTexture tex) {
    setColorBuffers(new PTexture[] { tex }, 1);
  }

  void setDrawBuffers(PTexture[] textures) {
    setColorBuffers(textures, textures.length);
  }

  void setColorBuffers(PTexture[] textures, int n) {
    
    a3d.pushFramebuffer();
    a3d.setFramebuffer(this);

    for (int i = 0; i < numDrawBuffersInUse; i++) {
      gl.glFramebufferTexture2DOES(GL11ExtensionPack.GL_FRAMEBUFFER_OES,
                    GL11ExtensionPack.GL_COLOR_ATTACHMENT0_OES, GL10.GL_TEXTURE_2D, 0, 0);      
    }
    
    numDrawBuffersInUse = PApplet.min(n, textures.length);
    
    for (int i = 0; i < numDrawBuffersInUse; i++) {
      colorDrawBuffers[i] = GL11ExtensionPack.GL_COLOR_ATTACHMENT0_OES + i;
      textureTargets[i] = textures[i].getGLTarget();
      textureIDs[i] = textures[i].getGLTextureID();

      gl.glFramebufferTexture2DOES(GL11ExtensionPack.GL_FRAMEBUFFER_OES, colorDrawBuffers[i],
          textureTargets[i], textureIDs[i], 0);
    }
    
    
    checkFBO();
    
    /*
  
            
            // Color buffer (texture) attachment.
            int glTextureID = 0;
            
            
            gl.glBindFramebufferOES(GL11ExtensionPack.GL_FRAMEBUFFER_OES, 0);
            
            
    
    numDrawBuffersInUse = PApplet.min(n, drawTextures.length);

    colorDrawBuffers = new int[numDrawBuffersInUse];
    textureIDs = new int[numDrawBuffersInUse];
    textureTargets = new int[numDrawBuffersInUse];

    for (int i = 0; i < numDrawBuffersInUse; i++) {
      colorDrawBuffers[i] = GL11ExtensionPack.GL_COLOR_ATTACHMENT0_OES + i;
      textureTargets[i] = drawTextures[i].getTextureTarget();
      textureIDs[i] = drawTextures[i].getTextureID();

      gl.glFramebufferTexture2DEXT(GL11ExtensionPack.GL_FRAMEBUFFER_OES, colorDrawBuffers[i],
          textureTargets[i], textureIDs[i], 0);
    }

    checkFBO();
    */

    a3d.popFramebuffer();    
  }
  
  
  public void bind() {
    gl.glBindFramebufferOES(GL11ExtensionPack.GL_FRAMEBUFFER_OES, glFboID);
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
  
   
  
  
  public void checkFBO() {

            int status = gl.glCheckFramebufferStatusOES(GL11ExtensionPack.GL_FRAMEBUFFER_OES);
            if (status != GL11ExtensionPack.GL_FRAMEBUFFER_COMPLETE_OES) {
                throw new RuntimeException("Framebuffer is not complete: " +
                        Integer.toHexString(status));
            }

            
    if (status == GL11ExtensionPack.GL_FRAMEBUFFER_COMPLETE_OES)
      return;
    else if (status == GL11ExtensionPack.GL_FRAMEBUFFER_INCOMPLETE_ATTACHMENT_OES) {
      System.err
          .println("Frame buffer is incomplete (GL_FRAMEBUFFER_INCOMPLETE_ATTACHMENT_OES)");
    } else if (status == GL11ExtensionPack.GL_FRAMEBUFFER_INCOMPLETE_MISSING_ATTACHMENT_OES) {
      System.err
          .println("Frame buffer is incomplete (GL_FRAMEBUFFER_INCOMPLETE_MISSING_ATTACHMENT_OES)");
    } else if (status == GL11ExtensionPack.GL_FRAMEBUFFER_INCOMPLETE_DIMENSIONS_OES) {
      System.err
          .println("Frame buffer is incomplete (GL_FRAMEBUFFER_INCOMPLETE_DIMENSIONS_OES)");
    } else if (status == GL11ExtensionPack.GL_FRAMEBUFFER_INCOMPLETE_FORMATS_OES) {
      System.err
          .println("Frame buffer is incomplete (GL_FRAMEBUFFER_INCOMPLETE_FORMATS_EXT)");
    } else if (status == GL11ExtensionPack.GL_FRAMEBUFFER_INCOMPLETE_DRAW_BUFFER_OES) {
      System.err
          .println("Frame buffer is incomplete (GL_FRAMEBUFFER_INCOMPLETE_DRAW_BUFFER_OES)");
    } else if (status == GL11ExtensionPack.GL_FRAMEBUFFER_INCOMPLETE_READ_BUFFER_OES) {
      System.err
          .println("Frame buffer is incomplete (GL_FRAMEBUFFER_INCOMPLETE_READ_BUFFER_OES)");
    } else if (status == GL11ExtensionPack.GL_FRAMEBUFFER_UNSUPPORTED_OES) {
      System.err
          .println("Frame buffer is incomplete (GL_FRAMEBUFFER_UNSUPPORTED_OES)");
    } else {
      System.err.println("Frame buffer is incomplete (unknown error code)");
    }



  }
  
  
  
  
  /*
  private GLSurfaceView mGLSurfaceView;

    private class Renderer implements GLSurfaceView.Renderer {
        private boolean mContextSupportsFrameBufferObject;
        private int mTargetTexture;
        private int mFramebuffer;
        private int mFramebufferWidth = 256;
        private int mFramebufferHeight = 256;
        private int mSurfaceWidth;
        private int mSurfaceHeight;

        private Triangle mTriangle;
        private Cube mCube;
        private float mAngle;
        private static final boolean DEBUG_RENDER_OFFSCREEN_ONSCREEN = false;

        public void onDrawFrame(GL10 gl) {
            checkGLError(gl);
            if (mContextSupportsFrameBufferObject) {
                GL11ExtensionPack gl11ep = (GL11ExtensionPack) gl;
                if (DEBUG_RENDER_OFFSCREEN_ONSCREEN) {
                    drawOffscreenImage(gl, mSurfaceWidth, mSurfaceHeight);
                } else {
                    gl11ep.glBindFramebufferOES(GL11ExtensionPack.GL_FRAMEBUFFER_OES, mFramebuffer);
                    drawOffscreenImage(gl, mFramebufferWidth, mFramebufferHeight);
                    gl11ep.glBindFramebufferOES(GL11ExtensionPack.GL_FRAMEBUFFER_OES, 0);
                    drawOnscreen(gl, mSurfaceWidth, mSurfaceHeight);
                }
            } else {
                // Current context doesn't support frame buffer objects.
                // Indicate this by drawing a red background.
                gl.glClearColor(1,0,0,0);
                gl.glClear(GL10.GL_COLOR_BUFFER_BIT | GL10.GL_DEPTH_BUFFER_BIT);
            }
        }

        public void onSurfaceChanged(GL10 gl, int width, int height) {
            checkGLError(gl);
            mSurfaceWidth = width;
            mSurfaceHeight = height;
            gl.glViewport(0, 0, width, height);
        }

        public void onSurfaceCreated(GL10 gl, EGLConfig config) {
            mContextSupportsFrameBufferObject = checkIfContextSupportsFrameBufferObject(gl);
            if (mContextSupportsFrameBufferObject) {
                mTargetTexture = createTargetTexture(gl, mFramebufferWidth, mFramebufferHeight);
                mFramebuffer = createFrameBuffer(gl, mFramebufferWidth, mFramebufferHeight, mTargetTexture);

                mCube = new Cube();
                mTriangle = new Triangle();
            }
        }

        private void drawOnscreen(GL10 gl, int width, int height) {
            gl.glViewport(0, 0, width, height);
            float ratio = (float) width / height;
            gl.glMatrixMode(GL10.GL_PROJECTION);
            gl.glLoadIdentity();
            gl.glFrustumf(-ratio, ratio, -1, 1, 3, 7);

            gl.glClearColor(0,0,1,0);
            gl.glClear(GL10.GL_COLOR_BUFFER_BIT | GL10.GL_DEPTH_BUFFER_BIT);
            gl.glBindTexture(GL10.GL_TEXTURE_2D, mTargetTexture);

            gl.glTexEnvf(GL10.GL_TEXTURE_ENV, GL10.GL_TEXTURE_ENV_MODE,
                    GL10.GL_REPLACE);

            gl.glMatrixMode(GL10.GL_MODELVIEW);
            gl.glLoadIdentity();

            GLU.gluLookAt(gl, 0, 0, -5, 0f, 0f, 0f, 0f, 1.0f, 0.0f);

            gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);
            gl.glEnableClientState(GL10.GL_TEXTURE_COORD_ARRAY);

            gl.glActiveTexture(GL10.GL_TEXTURE0);

            long time = SystemClock.uptimeMillis() % 4000L;
            float angle = 0.090f * ((int) time);

            gl.glRotatef(angle, 0, 0, 1.0f);

            mTriangle.draw(gl);

            // Restore default state so the other renderer is not affected.

            gl.glBindTexture(GL10.GL_TEXTURE_2D, 0);
            gl.glDisableClientState(GL10.GL_VERTEX_ARRAY);
            gl.glDisableClientState(GL10.GL_TEXTURE_COORD_ARRAY);
        }

        private void drawOffscreenImage(GL10 gl, int width, int height) {
            gl.glViewport(0, 0, width, height);
            float ratio = (float) width / height;
            gl.glMatrixMode(GL10.GL_PROJECTION);
            gl.glLoadIdentity();
            gl.glFrustumf(-ratio, ratio, -1, 1, 1, 10);

            gl.glEnable(GL10.GL_CULL_FACE);
            gl.glEnable(GL10.GL_DEPTH_TEST);

            gl.glClearColor(0,0.5f,1,0);
            gl.glClear(GL10.GL_COLOR_BUFFER_BIT | GL10.GL_DEPTH_BUFFER_BIT);
            gl.glMatrixMode(GL10.GL_MODELVIEW);
            gl.glLoadIdentity();
            gl.glTranslatef(0, 0, -3.0f);
            gl.glRotatef(mAngle,        0, 1, 0);
            gl.glRotatef(mAngle*0.25f,  1, 0, 0);

            gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);
            gl.glEnableClientState(GL10.GL_COLOR_ARRAY);

            mCube.draw(gl);

            gl.glRotatef(mAngle*2.0f, 0, 1, 1);
            gl.glTranslatef(0.5f, 0.5f, 0.5f);

            mCube.draw(gl);

            mAngle += 1.2f;

            // Restore default state so the other renderer is not affected.

            gl.glDisable(GL10.GL_CULL_FACE);
            gl.glDisable(GL10.GL_DEPTH_TEST);
            gl.glDisableClientState(GL10.GL_VERTEX_ARRAY);
            gl.glDisableClientState(GL10.GL_COLOR_ARRAY);
        }

        private int createTargetTexture(GL10 gl, int width, int height) {
            int texture;
            int[] textures = new int[1];
            gl.glGenTextures(1, textures, 0);
            texture = textures[0];
            gl.glBindTexture(GL10.GL_TEXTURE_2D, texture);
            gl.glTexImage2D(GL10.GL_TEXTURE_2D, 0, GL10.GL_RGBA, width, height, 0,
                    GL10.GL_RGBA, GL10.GL_UNSIGNED_BYTE, null);
            gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_MIN_FILTER,
                    GL10.GL_NEAREST);
            gl.glTexParameterf(GL10.GL_TEXTURE_2D,
                    GL10.GL_TEXTURE_MAG_FILTER,
                    GL10.GL_LINEAR);
            gl.glTexParameterx(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_WRAP_S,
                    GL10.GL_REPEAT);
            gl.glTexParameterx(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_WRAP_T,
                    GL10.GL_REPEAT);
            return texture;
        }

        private int createFrameBuffer(GL10 gl, int width, int height, int targetTextureId) {
            GL11ExtensionPack gl11ep = (GL11ExtensionPack) gl;
            int framebuffer;
            int[] framebuffers = new int[1];
            gl11ep.glGenFramebuffersOES(1, framebuffers, 0);
            framebuffer = framebuffers[0];
            gl11ep.glBindFramebufferOES(GL11ExtensionPack.GL_FRAMEBUFFER_OES, framebuffer);

            int depthbuffer;
            int[] renderbuffers = new int[1];
            gl11ep.glGenRenderbuffersOES(1, renderbuffers, 0);
            depthbuffer = renderbuffers[0];

            gl11ep.glBindRenderbufferOES(GL11ExtensionPack.GL_RENDERBUFFER_OES, depthbuffer);
            gl11ep.glRenderbufferStorageOES(GL11ExtensionPack.GL_RENDERBUFFER_OES,
                    GL11ExtensionPack.GL_DEPTH_COMPONENT16, width, height);
            gl11ep.glFramebufferRenderbufferOES(GL11ExtensionPack.GL_FRAMEBUFFER_OES,
                    GL11ExtensionPack.GL_DEPTH_ATTACHMENT_OES,
                    GL11ExtensionPack.GL_RENDERBUFFER_OES, depthbuffer);

            gl11ep.glFramebufferTexture2DOES(GL11ExtensionPack.GL_FRAMEBUFFER_OES,
                    GL11ExtensionPack.GL_COLOR_ATTACHMENT0_OES, GL10.GL_TEXTURE_2D,
                    targetTextureId, 0);
            int status = gl11ep.glCheckFramebufferStatusOES(GL11ExtensionPack.GL_FRAMEBUFFER_OES);
            if (status != GL11ExtensionPack.GL_FRAMEBUFFER_COMPLETE_OES) {
                throw new RuntimeException("Framebuffer is not complete: " +
                        Integer.toHexString(status));
            }
            gl11ep.glBindFramebufferOES(GL11ExtensionPack.GL_FRAMEBUFFER_OES, 0);
            return framebuffer;
          
        }

        private boolean checkIfContextSupportsFrameBufferObject(GL10 gl) {
            return checkIfContextSupportsExtension(gl, "GL_OES_framebuffer_object");
        }

        
         // This is not the fastest way to check for an extension, but fine if
         // we are only checking for a few extensions each time a context is created.
        private boolean checkIfContextSupportsExtension(GL10 gl, String extension) {
            String extensions = " " + gl.glGetString(GL10.GL_EXTENSIONS) + " ";
            // The extensions string is padded with spaces between extensions, but not
            // necessarily at the beginning or end. For simplicity, add spaces at the
            // beginning and end of the extensions string and the extension string.
            // This means we can avoid special-case checks for the first or last
            // extension, as well as avoid special-case checks when an extension name
            // is the same as the first part of another extension name.
            return extensions.indexOf(" " + extension + " ") >= 0;
        }
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Create our surface view and set it as the content of our
        // Activity
        mGLSurfaceView = new GLSurfaceView(this);
        mGLSurfaceView.setRenderer(new Renderer());
        setContentView(mGLSurfaceView);
    }

    @Override
    protected void onResume() {
        // Ideally a game should implement onResume() and onPause()
        // to take appropriate action when the activity looses focus
        super.onResume();
        mGLSurfaceView.onResume();
    }

    @Override
    protected void onPause() {
        // Ideally a game should implement onResume() and onPause()
        // to take appropriate action when the activity looses focus
        super.onPause();
        mGLSurfaceView.onPause();
    }
*/    
    
}
