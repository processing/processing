package processing.android.opengl;

import javax.microedition.khronos.egl.EGL10;
import javax.microedition.khronos.egl.EGL11;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.egl.EGLContext;
import javax.microedition.khronos.egl.EGLDisplay;
import javax.microedition.khronos.egl.EGLSurface;
import javax.microedition.khronos.opengles.GL;

import android.view.SurfaceHolder;


public class EglHelper {
  EGL10 egl;
  EGLDisplay eglDisplay;
  EGLSurface eglSurface;
  EGLConfig eglConfig;
  EGLContext eglContext;


  public EglHelper() { }


  // Needs to be separate from the constructor, since it may be restarted.
  public void start() {
    // don't recreate if already started [fry]
    if (egl == null) {

      // Specify a configuration for our opengl session
      // and grab the first configuration that matches is
      int[] configSpec = {
        EGL10.EGL_DEPTH_SIZE, 16,
        EGL10.EGL_NONE
      };

      // Get an EGL instance
      egl = (EGL10) EGLContext.getEGL();

      // Get to the default display.
      eglDisplay = egl.eglGetDisplay(EGL10.EGL_DEFAULT_DISPLAY);

      // We can now initialize EGL for that display
      int[] version = new int[2];
      egl.eglInitialize(eglDisplay, version);

      EGLConfig[] configs = new EGLConfig[1];
      int[] num_config = new int[1];
      egl.eglChooseConfig(eglDisplay, configSpec, configs, 1, num_config);
      eglConfig = configs[0];

      // Create an OpenGL ES context. This must be done only once, an
      // OpenGL context is a somewhat heavy object.
      eglContext = egl.eglCreateContext(eglDisplay, eglConfig,
                                        EGL10.EGL_NO_CONTEXT, null);

      eglSurface = null;
    }
  }


  // Create and return an OpenGL surface
  public GL createSurface(SurfaceHolder holder) {
    // The window size has changed, so we need to create a new surface.
    if (eglSurface != null) {

      // Unbind and destroy the old EGL surface, if there is one.
      egl.eglMakeCurrent(eglDisplay, EGL10.EGL_NO_SURFACE,
                         EGL10.EGL_NO_SURFACE, EGL10.EGL_NO_CONTEXT);
      egl.eglDestroySurface(eglDisplay, eglSurface);
    }

    // Create an EGL surface we can render into.
    eglSurface = egl.eglCreateWindowSurface(eglDisplay,
                                            eglConfig, holder, null);

    // Before we can issue GL commands, we need to make sure
    // the context is current and bound to a surface.
    egl.eglMakeCurrent(eglDisplay, eglSurface, eglSurface, eglContext);

    return eglContext.getGL();
  }


  public boolean swap() {
    egl.eglSwapBuffers(eglDisplay, eglSurface);

    // Always check for EGL_CONTEXT_LOST, which means the context and all 
    // associated data were lost (For instance because the device went to 
    // sleep). We need to sleep until we get a new surface.
    return egl.eglGetError() != EGL11.EGL_CONTEXT_LOST;
  }


  public void finish() {
    if (eglSurface != null) {
      egl.eglMakeCurrent(eglDisplay, EGL10.EGL_NO_SURFACE,
                          EGL10.EGL_NO_SURFACE,
                          EGL10.EGL_NO_CONTEXT);
      egl.eglDestroySurface(eglDisplay, eglSurface);
      eglSurface = null;
    }
    if (eglContext != null) {
      egl.eglDestroyContext(eglDisplay, eglContext);
      eglContext = null;
    }
    if (eglDisplay != null) {
      egl.eglTerminate(eglDisplay);
      eglDisplay = null;
    }
    // Clear this out so it's clear the restart is needed
    egl = null;
  }
}