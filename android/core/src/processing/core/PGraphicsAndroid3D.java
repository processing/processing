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
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.EmptyStackException;
import java.util.Stack;

import android.opengl.GLU;
import android.view.SurfaceHolder;

import android.opengl.GLSurfaceView;
import android.opengl.GLSurfaceView.EGLConfigChooser;
import android.opengl.GLSurfaceView.Renderer;

import javax.microedition.khronos.opengles.*;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.egl.EGL10;
import javax.microedition.khronos.egl.EGLContext;
import javax.microedition.khronos.egl.EGLDisplay;

import processing.core.PFont.Glyph;
import processing.core.PShape3D.VertexGroup;

// drawPixels is missing...calls to glDrawPixels are commented out
//   setRasterPos() is also commented out
// remove the BufferUtil class at the end (verify the endian order, rewind, etc)

/*
 * Android 3D renderer implemented with pure OpenGL ES 1.0/1.1
 * By Andres Colubri
 *
 * TODO: Comment A3D, PShape3D, PTexture, PFramebuffer, 
 * TODO: Check lighting and materials.
 * TODO: Revise triangulator (issues are particularly apparent when tesselating SVG shapes).
 */
public class PGraphicsAndroid3D extends PGraphics {
  public SurfaceHolder holder;

  public GL10 gl;
  public GL11 gl11;
  public GL11Ext gl11x;
  public GL11ExtensionPack gl11xp;
  //public GLU glu;

  // Set to 1 or 2 depending on whether to use EGL 1.x or 2.x
  static protected int EGL_CONTEXT = 1;
  // Translucency.
  static protected boolean TRANSLUCENT = true;
  // Color, depth and stencil bits.
  static protected int RED_BITS = 8;
  static protected int GREEN_BITS = 8;
  static protected int BLUE_BITS = 8;
  static protected int ALPHA_BITS = 8;
  static protected int DEPTH_BITS = 16;
  static protected int STENCIL_BITS = 0;

  // ........................................................

  /** Camera field of view. */
  public float cameraFOV;

  /** Position of the camera. */
  public float cameraX, cameraY, cameraZ;
  public float cameraNear, cameraFar;
  /** Aspect ratio of camera's view. */
  public float cameraAspect;

  /** Modelview and projection matrices **/
  protected float[] modelview;
  protected float[] modelviewInv;
  protected float[] projection;

  protected float[] camera;
  protected float[] cameraInv;

  protected boolean modelviewUpdated;
  protected boolean projectionUpdated;

  protected boolean matricesAllocated = false;

  /**
   * This is turned on at beginCamera, and off at endCamera Currently we don't
   * support nested begin/end cameras.
   */
  protected boolean manipulatingCamera;

  // ........................................................

  /**
   * Maximum lights by default is 8, the minimum defined by OpenGL.
   */
  public static final int MAX_LIGHTS = 8;

  public boolean lights;
  public int lightCount = 0;

  /** Light types */
  public int[] lightType;

  /** Light positions */
  public float[][] lightPosition;

  /** Light direction (normalized vector) */
  public float[][] lightNormal;

  /** Light falloff */
  public float[] lightFalloffConstant;
  public float[] lightFalloffLinear;
  public float[] lightFalloffQuadratic;

  /** Light spot angle */
  public float[] lightSpotAngle;

  /** Cosine of light spot angle */
  public float[] lightSpotAngleCos;

  /** Light spot concentration */
  public float[] lightSpotConcentration;

  /**
   * Diffuse colors for lights. For an ambient light, this will hold the ambient
   * color. Internally these are stored as numbers between 0 and 1.
   */
  public float[][] lightDiffuse;

  /**
   * Specular colors for lights. Internally these are stored as numbers between
   * 0 and 1.
   */
  public float[][] lightSpecular;

  /** Current specular color for lighting */
  public float[] currentLightSpecular;

  /** Current light falloff */
  public float currentLightFalloffConstant;
  public float currentLightFalloffLinear;
  public float currentLightFalloffQuadratic;

  /** Used to store empty values to be passed when a light has no ambient value **/
  public float[] zeroLight = { 0.0f, 0.0f, 0.0f, 0.0f };

  boolean lightsAllocated = false;

  // ........................................................

  // Geometry:

  // line & triangle fields (note that these overlap)
  static protected final int VERTEX1 = 0;
  static protected final int VERTEX2 = 1;
  static protected final int VERTEX3 = 2; // (triangles only)
  static protected final int POINT_FIELD_COUNT = 2;
  static protected final int LINE_FIELD_COUNT = 2;
  static protected final int TRIANGLE_FIELD_COUNT = 3;

  // Points
  static final int DEFAULT_POINTS = 512;
  protected int pointCount;
  protected int[][] points = new int[DEFAULT_POINTS][POINT_FIELD_COUNT];

  // Lines.
  static final int DEFAULT_LINES = 512;
  protected int lineCount;
  protected int[][] lines = new int[DEFAULT_LINES][LINE_FIELD_COUNT];

  // Triangles.
  static final int DEFAULT_TRIANGLES = 256;
  protected int triangleCount; // total number of triangles
  protected int[][] triangles = new int[DEFAULT_TRIANGLES][TRIANGLE_FIELD_COUNT];

  // Vertex, color, texture coordinate and normal buffers.
  static final int DEFAULT_BUFFER_SIZE = 512;
  private IntBuffer vertexBuffer;
  private IntBuffer colorBuffer;
  private IntBuffer texCoordBuffer;
  private IntBuffer normalBuffer;

  // Arrays used to put vertex data into the buffers.
  private int[] vertexArray;
  private int[] colorArray;
  private int[] texCoordArray;
  private int[] normalArray;

  protected PImage textureImagePrev;
  protected boolean buffersAllocated = false;

  /**
   * Set to true if the host system is big endian (PowerPC, MIPS, SPARC), false
   * if little endian (x86 Intel for Mac or PC).
   */
  static public boolean BIG_ENDIAN = ByteOrder.nativeOrder() == ByteOrder.BIG_ENDIAN;

  // Size of an int (in bytes).
  static protected int SIZEOF_INT = Integer.SIZE / 8;

  // ........................................................

  // pos of first vertex of current shape in vertices array
  protected int shapeFirst;

  // i think vertex_end is actually the last vertex in the current shape
  // and is separate from vertexCount for occasions where drawing happens
  // on endDraw() with all the triangles being depth sorted
  protected int shapeLast;

  // used for sorting points when triangulating a polygon
  // warning - maximum number of vertices for a polygon is DEFAULT_VERTICES
  protected int vertexOrder[] = new int[DEFAULT_VERTICES];

  // ........................................................

  // This is done to keep track of start/stop information for lines in the
  // line array, so that lines can be shown as a single path, rather than just
  // individual segments.
  protected int pathCount;
  protected int[] pathOffset = new int[64];
  protected int[] pathLength = new int[64];

  // ........................................................

  // And this is done to keep track of start/stop information for textured
  // triangles in the triangle array, so that a range of triangles with the
  // same texture applied to them are correctly textured during the
  // rendering stage.
  protected int faceCount;
  protected int[] faceOffset = new int[64];
  protected int[] faceLength = new int[64];
  protected PImage[] faceTexture = new PImage[64];

  // ........................................................

  // / Used to hold color values to be sent to OpenGL
  protected float[] colorFloats;

  // / IntBuffer to go with the pixels[] array
  protected IntBuffer pixelBuffer;

  // ........................................................

  // Extensions support.
  protected boolean npotTexSupported;
  protected boolean mipmapSupported;
  protected boolean matrixGetSupported;
  protected boolean vboSupported;
  protected boolean fboSupported;
  protected int maxTextureSize;
  protected float maxPointSize;

  // ........................................................

  // This array contains the recreateResource methods of all the GL objects
  // created in Processing. These methods are used to recreate the open GL
  // data when there is a context change or surface creation in Android.
  // TODO: Check the resource recreation method.
  protected ArrayList<GLResource> recreateResourceMethods;

  // ........................................................

  boolean recordingModel;
  ArrayList<PVector> recordedVertices;
  ArrayList<float[]> recordedColors;
  ArrayList<PVector> recordedNormals;
  ArrayList<PVector> recordedTexCoords;
  ArrayList<VertexGroup> recordedGroups;

  // .......................................................
  
  static protected Stack<PFramebuffer> fbStack;
  static protected PFramebuffer screenFramebuffer;
  static protected PFramebuffer currentFramebuffer;
  
  protected PFramebuffer drawFramebuffer;
  protected PImage[] drawImages;
  protected PTexture[] drawTextures;
  protected int drawIndex;
  protected int[] drawTexCrop;

  // ........................................................

  // Used to save a copy of the last drawn frame in order to repaint on the
  // backbuffer when using noClear mode.
  protected int[] screenTexID = {0};
  protected int screenTexWidth;
  protected int screenTexHeight;
  public int[] screenTexCrop = {0, 0, 0, 0};

  // This variable controls clearing the buffers.
  boolean clear = true;
  
  // ........................................................
  
  boolean blend;
  int blendMode;  

  // ........................................................
    
  public String OPENGL_VENDOR;
  public String OPENGL_RENDERER;
  public String OPENGL_VERSION;

  
  // ////////////////////////////////////////////////////////////

  public PGraphicsAndroid3D() {
    renderer = new A3DRenderer();
    configChooser = new A3DConfigChooser(RED_BITS, GREEN_BITS, BLUE_BITS,
        ALPHA_BITS, DEPTH_BITS, STENCIL_BITS);
    contextFactory = new A3DContextFactory();
    //glu = new GLU(); // or maybe not until used?
    recreateResourceMethods = new ArrayList<GLResource>();
  }

  // public void setParent(PApplet parent)

  // public void setPrimary(boolean primary)

  // public void setPath(String path)

  public void setSize(int iwidth, int iheight) {
    width = iwidth;
    height = iheight;
    width1 = width - 1;
    height1 = height - 1;

    allocate();
    reapplySettings();

    vertexCheck();

    // init perspective projection based on new dimensions
    cameraFOV = 60 * DEG_TO_RAD; // at least for now
    cameraX = width / 2.0f;
    cameraY = height / 2.0f;
    cameraZ = cameraY / ((float) Math.tan(cameraFOV / 2.0f));
    cameraNear = cameraZ / 10.0f;
    cameraFar = cameraZ * 10.0f;
    cameraAspect = (float) width / (float) height;
  }

  public void setSurfaceHolder(SurfaceHolder holder) {
    this.holder = holder;
  }

  protected void allocate() {
    if (!matricesAllocated) {
      projection = new float[16];
      modelview = new float[16];
      modelviewInv = new float[16];
      camera = new float[16];
      cameraInv = new float[16];
      matricesAllocated = true;
    }

    if (!lightsAllocated) {
      lightType = new int[MAX_LIGHTS];
      lightPosition = new float[MAX_LIGHTS][4];
      lightNormal = new float[MAX_LIGHTS][4];
      lightDiffuse = new float[MAX_LIGHTS][4];
      lightSpecular = new float[MAX_LIGHTS][4];
      lightFalloffConstant = new float[MAX_LIGHTS];
      lightFalloffLinear = new float[MAX_LIGHTS];
      lightFalloffQuadratic = new float[MAX_LIGHTS];
      lightSpotAngle = new float[MAX_LIGHTS];
      lightSpotAngleCos = new float[MAX_LIGHTS];
      lightSpotConcentration = new float[MAX_LIGHTS];
      currentLightSpecular = new float[4];
      lightsAllocated = true;
    }

    if (!buffersAllocated) {
      ByteBuffer vbb = ByteBuffer.allocateDirect(DEFAULT_BUFFER_SIZE * 3
          * SIZEOF_INT);
      vbb.order(ByteOrder.nativeOrder());
      vertexBuffer = vbb.asIntBuffer();

      ByteBuffer cbb = ByteBuffer.allocateDirect(DEFAULT_BUFFER_SIZE * 4
          * SIZEOF_INT);
      cbb.order(ByteOrder.nativeOrder());
      colorBuffer = cbb.asIntBuffer();

      ByteBuffer tbb = ByteBuffer.allocateDirect(DEFAULT_BUFFER_SIZE * 2
          * SIZEOF_INT);
      tbb.order(ByteOrder.nativeOrder());
      texCoordBuffer = tbb.asIntBuffer();

      ByteBuffer nbb = ByteBuffer.allocateDirect(DEFAULT_BUFFER_SIZE * 3
          * SIZEOF_INT);
      nbb.order(ByteOrder.nativeOrder());
      normalBuffer = nbb.asIntBuffer();

      vertexArray = new int[DEFAULT_BUFFER_SIZE * 3];
      colorArray = new int[DEFAULT_BUFFER_SIZE * 4];
      texCoordArray = new int[DEFAULT_BUFFER_SIZE * 2];
      normalArray = new int[DEFAULT_BUFFER_SIZE * 3];

      buffersAllocated = true;
    }
  }

  public void dispose() {
    if (screenTexID[0] == 0) {
      gl.glDeleteTextures(1, screenTexID, 0);  
    }    
  }

  public void recreateResources() {
    // Recreate the openGL resources of the registered GL objects (PTexture,
    // PShape3D)
    for (int i = 0; i < recreateResourceMethods.size(); i++) {
      GLResource resource = (GLResource) recreateResourceMethods.get(i);
      try {
        resource.method.invoke(resource.object, new Object[] { this });
      } catch (Exception e) {
        System.err.println("Error, opengl resources in " + resource.object
            + " cannot be recreated.");
        e.printStackTrace();
      }
    }
  }

  protected int addRecreateResourceMethod(Object obj, Method meth) {
    recreateResourceMethods.add(new GLResource(obj, meth));
    return recreateResourceMethods.size() - 1;
  }

  protected void removeRecreateResourceMethod(int idx) {
    if (-1 < idx && idx < recreateResourceMethods.size()) {
      recreateResourceMethods.remove(idx);
    }
  }

  // ////////////////////////////////////////////////////////////

  // SCREEN TEXTURE

  protected void createScreenTexture() {    
    if (screenTexID[0] == 0) {
      gl.glDeleteTextures(1, screenTexID, 0);  
    }
    
    if (npotTexSupported) {
      screenTexWidth = width;
      screenTexHeight =height;
    } else {
      screenTexWidth = nextPowerOfTwo(width);
      screenTexHeight = nextPowerOfTwo(height);
    }
        
    gl.glEnable(GL10.GL_TEXTURE_2D);
    gl.glGenTextures(1, screenTexID, 0);
    gl.glBindTexture(GL10.GL_TEXTURE_2D, screenTexID[0]);    

    gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_MIN_FILTER, GL10.GL_NEAREST);
    gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_MAG_FILTER, GL10.GL_NEAREST);

    gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_WRAP_S, GL10.GL_CLAMP_TO_EDGE);
    gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_WRAP_T, GL10.GL_CLAMP_TO_EDGE);
    
    // This is the right texture environment mode to ignore the fill color when drawing the texture:
    // http://www.khronos.org/opengles/documentation/opengles1_0/html/glTexEnv.html
    gl.glTexEnvf(GL10.GL_TEXTURE_ENV, GL10.GL_TEXTURE_ENV_MODE, GL10.GL_REPLACE);
        
    int[] buf = new int [screenTexWidth * screenTexHeight];
    for (int i = 0; i < buf.length; i++) buf[i] = 0xFF000000;
    gl.glTexImage2D(GL10.GL_TEXTURE_2D, 0, GL10.GL_RGBA,  screenTexWidth, screenTexHeight, 0, GL10.GL_RGBA, GL10.GL_UNSIGNED_BYTE, IntBuffer.wrap(buf));
    gl.glDisable(GL10.GL_TEXTURE_2D);
    
    screenTexCrop[0] = 0;
    screenTexCrop[1] = 0;
    screenTexCrop[2] = width; //screenTexWidth;
    screenTexCrop[3] = height; //screenTexHeight;      
  }

  protected void drawScreenTexture() {
    gl.glEnable(GL10.GL_TEXTURE_2D);
    gl.glBindTexture(GL10.GL_TEXTURE_2D, screenTexID[0]);

    // There is no need to setup orthographic projection or any related matrix set/restore
    // operations here because glDrawTexiOES operates on window coordinates:
    // "glDrawTexiOES takes window coordinates and bypasses the transform pipeline 
    // (except for mapping Z to the depth range), so there is no need for any 
    // matrix setup/restore code."
    // (from https://www.khronos.org/message_boards/viewtopic.php?f=4&t=948&p=2553).    
    
    // Depth mask is disabled so the depth values are not modified when
    // rendering the texture quad. In this way the texture doesn't occlude
    // any geometry latter drawn by the user.
    gl.glDepthMask(false);
    gl.glDisable(GL10.GL_BLEND);
    
    gl11.glTexParameteriv(GL10.GL_TEXTURE_2D, GL11Ext.GL_TEXTURE_CROP_RECT_OES, screenTexCrop, 0);
    //gl11x.glDrawTexiOES(0, 0, 0, screenTexWidth, screenTexHeight);
    gl11x.glDrawTexiOES(0, 0, 0, width, height);
    
    gl.glDisable(GL10.GL_TEXTURE_2D);   
    gl.glDepthMask(true);
    gl.glEnable(GL10.GL_BLEND);
  }

  protected void copyFrameToScreenTexture() {
    gl.glFinish(); // Make sure that the execution off all the openGL commands
                           // is finished.
    
    gl.glEnable(GL10.GL_TEXTURE_2D);
    gl.glBindTexture(GL10.GL_TEXTURE_2D, screenTexID[0]);
    gl.glCopyTexImage2D(GL10.GL_TEXTURE_2D, 0, GL10.GL_RGB, 0, 0, screenTexWidth, screenTexHeight, 0); 
    gl.glDisable(GL10.GL_TEXTURE_2D);
  }

  // ////////////////////////////////////////////////////////////

  // FRAMEBUFFERS
  
  public void pushFramebuffer() {
    fbStack.push(currentFramebuffer);
  }

  public void setFramebuffer(PFramebuffer fbo) {
    currentFramebuffer = fbo;
    currentFramebuffer.bind();
  }

  public void popFramebuffer() {
    try {
      currentFramebuffer = fbStack.pop();
      currentFramebuffer.bind();
    } catch (EmptyStackException e) {
      PGraphics.showWarning("A3D: Empty framebuffer stack");
    }
  }
  
  public void renderDrawTexture(int idx) {
    PTexture tex = drawTextures[idx];

    gl.glEnable(tex.getGLTarget());
    gl.glBindTexture(tex.getGLTarget(), tex.getGLTextureID());
    gl.glDepthMask(false);
    gl.glDisable(GL10.GL_BLEND);
    
    gl.glTexEnvf(GL10.GL_TEXTURE_ENV, GL10.GL_TEXTURE_ENV_MODE, GL10.GL_REPLACE);
    gl11.glTexParameteriv(tex.getGLTarget(), GL11Ext.GL_TEXTURE_CROP_RECT_OES, drawTexCrop, 0);
    gl11x.glDrawTexiOES(0, 0, 0, width, height);
    
    gl.glDisable(tex.getGLTarget());   
    gl.glDepthMask(true);
    gl.glEnable(GL10.GL_BLEND);
  }
  
  public void swapDrawIndex() {
    drawIndex = (drawIndex + 1) % 2; 
  }
  
  public PImage getLastFrame() {
    return drawImages[(drawIndex + 1) % 2];
  }
  
  protected void saveGLState() {
    gl.glMatrixMode(GL10.GL_PROJECTION);
    gl.glPushMatrix();
    gl.glMatrixMode(GL10.GL_MODELVIEW);
    gl.glPushMatrix();  
  }
  
  protected void restoreGLState() {
    // Restoring blending.
    if (blend) {
      blend(blendMode);
    } else { 
      noBlend();
    }
    
    // Restoring viewport.
    gl.glViewport(0, 0, width, height);

    // Restoring hints.
    if (hints[DISABLE_DEPTH_TEST]) {
      gl.glDisable(GL10.GL_DEPTH_TEST);
      gl.glClear(GL10.GL_DEPTH_BUFFER_BIT);
    } else {
      gl.glEnable(GL10.GL_DEPTH_TEST);
    }
    
    // Restoring fill
    if (fill) {
      fillFromCalc();  
    }    
    
    // Restoring material properties.
    ambientFromCalc();
    specularFromCalc();
    shininess(shininess);
    emissiveFromCalc();
    
    // Restoring lights.
    if (lights) {
      for (int i = 0; i < lightCount; i++) {
        glLightEnable(i);
        if (lightType[i] == AMBIENT) {
          glLightAmbient(i);
          glLightPosition(i);
          glLightFalloff(i);
          glLightNoSpot(i);
        } else if (lightType[i] == DIRECTIONAL) {
          glLightNoAmbient(i);
          glLightDirection(i);
          glLightDiffuse(i);
          glLightSpecular(i);
          glLightFalloff(i);
          glLightNoSpot(i);
        } else if (lightType[i] == POINT) {
          glLightNoAmbient(i);
          glLightPosition(i);
          glLightDiffuse(i);
          glLightSpecular(i);
          glLightFalloff(i);
          glLightNoSpot(i);
        } else if (lightType[i] == SPOT) {
          glLightNoAmbient(i);
          glLightPosition(i);
          glLightDirection(i);
          glLightDiffuse(i);
          glLightSpecular(i);
          glLightFalloff(i);
          glLightSpotAngle(i);
          glLightSpotConcentration(i);
        }
      }
    } else {
      noLights();
    }
    
    // Restoring matrices.
    gl.glMatrixMode(GL10.GL_PROJECTION);
    gl.glPopMatrix();
    gl.glMatrixMode(GL10.GL_MODELVIEW);
    gl.glPopMatrix();    
  }
  
  // ////////////////////////////////////////////////////////////

  // FRAME

  public void requestDraw() {
    // This if condition is needed to avoid flickering when looping is disabled.
    if (parent.looping) {
      ((GLSurfaceView) parent.surfaceView).requestRender();
    }
  }

  /**
   * OpenGL cannot draw until a proper native peer is available, so this returns
   * the value of PApplet.isDisplayable() (inherited from Component).
   */
  // public boolean canDraw() {
  // return true;
  // //return parent.isDisplayable();
  // }

  public void beginDraw() {
    VERTEXCOUNT = 0;
    TRIANGLECOUNT = 0;
    FACECOUNT = 0;

    if (!primarySurface) {
      PGraphicsAndroid3D a3d = (PGraphicsAndroid3D)parent.g;
      a3d.saveGLState();
      
      // Disabling all lights, so the offscreen renderer can set completely
      // new light configuration (otherwise some light config from the 
      // primary renderer might stay).
      for (int i = 0; i < a3d.lightCount; i++) {
        a3d.glLightDisable(i);
      }      
    }
    
    if (!settingsInited)
      defaultSettings();

    resetMatrix(); // reset model matrix.

    report("top beginDraw()");

    vertexBuffer.rewind();
    colorBuffer.rewind();
    texCoordBuffer.rewind();
    normalBuffer.rewind();

    textureImage = null;
    textureImagePrev = null;

    // Blend is needed for alpha (i.e. fonts) to work.
    blend(BLEND);

    // this is necessary for 3D drawing
    if (hints[DISABLE_DEPTH_TEST]) {
      gl.glDisable(GL10.GL_DEPTH_TEST);
    } else {
      gl.glEnable(GL10.GL_DEPTH_TEST);
    }
    // use <= since that's what processing.core does
    gl.glDepthFunc(GL10.GL_LEQUAL);

    // because y is flipped
    gl.glFrontFace(GL10.GL_CW);

    gl.glViewport(0, 0, width, height);
    // set up the default camera
    camera();

    // defaults to perspective, if the user has setup up their
    // own projection, they'll need to fix it after resize anyway.
    // this helps the people who haven't set up their own projection.
    perspective();

    lightCount = 0;
    lightFalloff(1, 0, 0);
    lightSpecular(0, 0, 0);

    // coloured stuff
    gl.glEnable(GL10.GL_COLOR_MATERIAL);
    // TODO maybe not available in OpenGL ES?
    // gl.glColorMaterial(GL10.GL_FRONT_AND_BACK, GL10.GL_AMBIENT_AND_DIFFUSE);
    // gl.glColorMaterial(GL10.GL_FRONT_AND_BACK, GL10.GL_SPECULAR);

    // these tend to make life easier
    // (but sometimes at the expense of a little speed)
    // Not using them right now because we're doing our own lighting.
    gl.glEnable(GL10.GL_NORMALIZE);
    // gl.glEnable(GL10.GL_AUTO_NORMAL); // I think this is OpenGL 1.2 only
    gl.glEnable(GL10.GL_RESCALE_NORMAL);
    // gl.GlLightModeli(GL10.GL_LIGHT_MODEL_COLOR_CONTROL,
    // GL10.GL_SEPARATE_SPECULAR_COLOR);

    shapeFirst = 0;

    if (fbStack == null) {
      fbStack = new Stack<PFramebuffer>();

      screenFramebuffer = new PFramebuffer(parent, width, height, true);
      setFramebuffer(screenFramebuffer);
    }
    
    // TODO: rework logic depending on whether this renderer is primarySurface, etc.
    if (clear) {
      gl.glClearColor(0, 0, 0, 0);
      gl.glClear(GL10.GL_COLOR_BUFFER_BIT | GL10.GL_DEPTH_BUFFER_BIT);
    } else {
      if (fboSupported) {
        if (drawFramebuffer == null) {
          drawTexCrop = new int[4];
          drawTexCrop[0] = 0;
          drawTexCrop[1] = 0;
          drawTexCrop[2] = width;
          drawTexCrop[3] = height;      
        
          drawImages = new PImage[2];
          drawImages[0] = parent.createImage(width, height, ARGB, NEAREST);
          drawImages[1] = parent.createImage(width, height, ARGB, NEAREST);
        
          drawTextures = new PTexture[2];
          drawTextures[0] = drawImages[0].getTexture();
          drawTextures[1] = drawImages[1].getTexture();
        
          drawIndex = 0;
        
          drawFramebuffer = new PFramebuffer(parent, drawTextures[0].getGLWidth(), drawTextures[0].getGLHeight(), false);
        
          drawFramebuffer.addDepthBuffer(DEPTH_BITS);
          if (0 < STENCIL_BITS) {
            drawFramebuffer.addStencilBuffer(STENCIL_BITS); 
          }
        }
      
        pushFramebuffer();
        setFramebuffer(drawFramebuffer);
        drawFramebuffer.addColorBuffer(drawTextures[drawIndex]);

        gl.glClearColor(0, 0, 0, 0);
        gl.glClear(GL10.GL_DEPTH_BUFFER_BIT);
            
        // Render previous draw texture as background.      
        renderDrawTexture((drawIndex + 1) % 2);
      } else {    
        if (screenTexID[0] == 0) {
          createScreenTexture();
        }
        gl.glClearColor(0, 0, 0, 0);
        gl.glClear(GL10.GL_COLOR_BUFFER_BIT | GL10.GL_DEPTH_BUFFER_BIT);      
        drawScreenTexture();
      }
    }
    
    report("bot beginDraw()");
  }

  public void endDraw() {
    if (!clear) {
      if (fboSupported) {
        if (drawFramebuffer != null) {
          popFramebuffer();

          gl.glClearColor(0, 0, 0, 0);
          gl.glClear(GL10.GL_COLOR_BUFFER_BIT | GL10.GL_DEPTH_BUFFER_BIT);
      
          // Render current draw texture to screen.
          renderDrawTexture(drawIndex);
          swapDrawIndex();
        }
      } else {
        if (screenTexID[0] != 0) {
          copyFrameToScreenTexture();
        }
      }  
    }
    
    gl.glFlush();

    if (!primarySurface) {
      ((PGraphicsAndroid3D)parent.g).restoreGLState();
    }
    
    report("top endDraw()");
    /*
     * if (hints[ENABLE_DEPTH_SORT]) { flush(); }
     */
  }

  public GL10 beginGL() {
    gl.glPushMatrix();
    gl.glScalef(1, -1, 1);
    return gl;
  }

  public void endGL() {
    gl.glPopMatrix();
  }

  // //////////////////////////////////////////////////////////

  // CLEAR/NO CLEAR

  public void clear() {
    clear = true;
  }

  public void noClear() {
    clear = false;
  }

  // //////////////////////////////////////////////////////////

  // SETTINGS

  // protected void checkSettings()

  protected void defaultSettings() {
    super.defaultSettings();

    manipulatingCamera = false;
    perspective();

    // easiest for beginners
    textureMode(IMAGE);
  }

  // reapplySettings

  // //////////////////////////////////////////////////////////

  // HINTS

  public void hint(int which) {
    super.hint(which);

    if (which == DISABLE_DEPTH_TEST) {
      gl.glDisable(GL10.GL_DEPTH_TEST);
      gl.glClear(GL10.GL_DEPTH_BUFFER_BIT);

    } else if (which == ENABLE_DEPTH_TEST) {
      gl.glEnable(GL10.GL_DEPTH_TEST);

    } else if (which == DISABLE_OPENGL_2X_SMOOTH) {
      // TODO throw an error?

    } else if (which == ENABLE_OPENGL_2X_SMOOTH) {
      // TODO throw an error?

    } else if (which == ENABLE_OPENGL_4X_SMOOTH) {
      // TODO throw an error?
    }
  }

  // ////////////////////////////////////////////////////////////

  // VERTEX SHAPES

  // All picked up from either PGraphics or PGraphics3D

  // public void beginShape()

  public void beginShapeRecorder() {
    beginShapeRecorder(POLYGON);
  }

  public void beginShapeRecorder(int kind) {
    beginShapeRecorderImpl();
    beginShape(kind);
  }

  public void beginShapesRecorder() {
    if (recordingModel) {
      System.err
          .println("Already recording shapes. Recording cannot be nested");
    } else {
      beginShapeRecorderImpl();
    }
  }

  protected void beginShapeRecorderImpl() {
    recordingModel = true;
    recordedVertices = new ArrayList<PVector>(vertexBuffer.capacity() / 3);
    recordedColors = new ArrayList<float[]>(colorBuffer.capacity() / 4);
    recordedNormals = new ArrayList<PVector>(normalBuffer.capacity() / 4);
    recordedTexCoords = new ArrayList<PVector>(texCoordBuffer.capacity() / 2);
    recordedGroups = new ArrayList<VertexGroup>();
  }

  public void beginShape(int kind) {
    shape = kind;

    if (hints[ENABLE_DEPTH_SORT]) {
      // TODO:
      // Implement depth sorting with vertex arrays.

      // continue with previous vertex, line and triangle count
      // all shapes are rendered at endDraw();
      shapeFirst = vertexCount;
      shapeLast = 0;

    } else {
      // reset vertex, line and triangle information
      // every shape is rendered at endShape();
      vertexCount = 0;
      lineCount = 0;
      triangleCount = 0;
    }

    textureImage = null;
    textureImagePrev = null;

    normalMode = NORMAL_MODE_AUTO;
  }

  // public void edge(boolean e)
  // public void normal(float nx, float ny, float nz)
  // public void textureMode(int mode)

  public void texture(PImage image) {
    textureImage = image;
  }

  static public int toFixed32(float x) {
    return (int) (x * 65536.0f);
  }

  static public int toFixed16(float x) {
    return (int) (x * 4096.0f);
  }

  // public void vertex(float x, float y)
  // public void vertex(float x, float y, float z)
  // public void vertex(float x, float y, float u, float v)
  // public void vertex(float x, float y, float z, float u, float v)
  // protected void vertexTexture(float u, float v);
  // public void breakShape()

  // public void endShape()

  public void endShape(int mode) {
    shapeLast = vertexCount;

    // don't try to draw if there are no vertices
    // (fixes a bug in LINE_LOOP that re-adds a nonexistent vertex)
    if (vertexCount == 0) {
      shape = 0;
      return;
    }

    if (stroke) {
      endShapeStroke(mode);
    }

    if (fill) {
      endShapeFill();
    }

    // render shape and fill here if not saving the shapes for later
    // if true, the shapes will be rendered on endDraw
    if (!hints[ENABLE_DEPTH_SORT]) {
      if (fill) {
        renderTriangles(0, faceCount);
        if (raw != null) {
          // rawTriangles(0, triangleCount);
        }
        triangleCount = 0;
      }
      if (stroke) {
        renderLines(0, pathCount);
        if (raw != null) {
          // rawLines(0, lineCount);
        }
        lineCount = 0;
      }
      pathCount = 0;
      faceCount = 0;
    }

    shape = 0;
  }

  protected void endShapeStroke(int mode) {
    switch (shape) {
    case POINTS: {
      int stop = shapeLast;
      for (int i = shapeFirst; i < stop; i++) {
        addLineBreak(); // total overkill for points
        addLine(i, i);
      }
    }
      break;

    case LINES: {
      // store index of first vertex
      int first = lineCount;
      int stop = shapeLast - 1;
      // increment = (shape == LINES) ? 2 : 1;

      // for LINE_STRIP and LINE_LOOP, make this all one path
      if (shape != LINES)
        addLineBreak();

      for (int i = shapeFirst; i < stop; i += 2) {
        // for LINES, make a new path for each segment
        if (shape == LINES)
          addLineBreak();
        addLine(i, i + 1);
      }

      // for LINE_LOOP, close the loop with a final segment
      // if (shape == LINE_LOOP) {
      if (mode == CLOSE) {
        addLine(stop, lines[first][VERTEX1]);
      }
    }
      break;

    case TRIANGLES: {
      for (int i = shapeFirst; i < shapeLast - 2; i += 3) {
        addLineBreak();
        // counter = i - vertex_start;
        addLine(i + 0, i + 1);
        addLine(i + 1, i + 2);
        addLine(i + 2, i + 0);
      }
    }
      break;

    case TRIANGLE_STRIP: {
      // first draw all vertices as a line strip
      int stop = shapeLast - 1;

      addLineBreak();
      for (int i = shapeFirst; i < stop; i++) {
        // counter = i - vertex_start;
        addLine(i, i + 1);
      }

      // then draw from vertex (n) to (n+2)
      stop = shapeLast - 2;
      for (int i = shapeFirst; i < stop; i++) {
        addLineBreak();
        addLine(i, i + 2);
      }
    }
      break;

    case TRIANGLE_FAN: {
      // this just draws a series of line segments
      // from the center to each exterior point
      for (int i = shapeFirst + 1; i < shapeLast; i++) {
        addLineBreak();
        addLine(shapeFirst, i);
      }

      // then a single line loop around the outside.
      addLineBreak();
      for (int i = shapeFirst + 1; i < shapeLast - 1; i++) {
        addLine(i, i + 1);
      }
      // closing the loop
      addLine(shapeLast - 1, shapeFirst + 1);
    }
      break;

    case QUADS: {
      for (int i = shapeFirst; i < shapeLast; i += 4) {
        addLineBreak();
        // counter = i - vertex_start;
        addLine(i + 0, i + 1);
        addLine(i + 1, i + 2);
        addLine(i + 2, i + 3);
        addLine(i + 3, i + 0);
      }
    }
      break;

    case QUAD_STRIP: {
      for (int i = shapeFirst; i < shapeLast - 3; i += 2) {
        addLineBreak();
        addLine(i + 0, i + 2);
        addLine(i + 2, i + 3);
        addLine(i + 3, i + 1);
        addLine(i + 1, i + 0);
      }
    }
      break;

    case POLYGON: {
      // store index of first vertex
      int stop = shapeLast - 1;

      addLineBreak();
      for (int i = shapeFirst; i < stop; i++) {
        addLine(i, i + 1);
      }
      if (mode == CLOSE) {
        // draw the last line connecting back to the first point in poly
        addLine(stop, shapeFirst); // lines[first][VERTEX1]);
      }
    }
      break;
    }
  }

  protected void endShapeFill() {
    switch (shape) {
    case TRIANGLE_FAN: {
      int stop = shapeLast - 1;
      for (int i = shapeFirst + 1; i < stop; i++) {
        addTriangle(shapeFirst, i, i + 1);
      }
    }
      break;

    case TRIANGLES: {
      int stop = shapeLast - 2;
      for (int i = shapeFirst; i < stop; i += 3) {
        // have to switch between clockwise/counter-clockwise
        // otherwise the feller is backwards and renderer won't draw
        if ((i % 2) == 0) {
          addTriangle(i, i + 2, i + 1);
        } else {
          addTriangle(i, i + 1, i + 2);
        }
      }
    }
      break;

    case TRIANGLE_STRIP: {
      int stop = shapeLast - 2;
      for (int i = shapeFirst; i < stop; i++) {
        // have to switch between clockwise/counter-clockwise
        // otherwise the feller is backwards and renderer won't draw
        if ((i % 2) == 0) {
          addTriangle(i, i + 2, i + 1);
        } else {
          addTriangle(i, i + 1, i + 2);
        }
      }
    }
      break;

    case QUADS: {
      int stop = vertexCount - 3;
      for (int i = shapeFirst; i < stop; i += 4) {
        // first triangle
        addTriangle(i, i + 1, i + 2);
        // second triangle
        addTriangle(i, i + 2, i + 3);
      }
    }
      break;

    case QUAD_STRIP: {
      int stop = vertexCount - 3;
      for (int i = shapeFirst; i < stop; i += 2) {
        // first triangle
        addTriangle(i + 0, i + 2, i + 1);
        // second triangle
        addTriangle(i + 2, i + 3, i + 1);
      }
    }
      break;

    case POLYGON: {
      addPolygonTriangles();
    }
      break;
    }
  }

  public PShape3D endShapeRecorder() {
    return endShapeRecorder(OPEN);
  }

  public PShape3D endShapeRecorder(int mode) {
    endShape(mode);
    PShape3D shape = null;
    if (0 < recordedVertices.size()) {
      shape = new PShape3D(parent, recordedVertices.size());
    }
    endShapeRecorderImpl(shape);
    return shape;
  }

  public PShape3D endShapesRecorder() {
    if (recordingModel) {
      PShape3D shape = null;
      if (0 < recordedVertices.size()) {
        shape = new PShape3D(parent, recordedVertices.size());
      }
      endShapeRecorderImpl(shape);
      return shape;
    } else {
      System.err.println("Start recording with beginShapesRecorder().");
      return null;
    }
  }

  protected void endShapeRecorderImpl(PShape3D shape) {
    recordingModel = false;
    if (0 < recordedVertices.size() && shape != null) {
      shape.beginUpdate(VERTICES);
      shape.setVertex(recordedVertices);
      shape.endUpdate();

      shape.beginUpdate(COLORS);
      shape.setColor(recordedColors);
      shape.endUpdate();

      shape.beginUpdate(NORMALS);
      shape.setNormal(recordedNormals);
      shape.endUpdate();

      shape.beginUpdate(TEXTURES);
      shape.setTexCoord(recordedTexCoords);
      shape.endUpdate();

      shape.setGroups(recordedGroups);
      shape.optimizeGroups();

      // Freeing memory.
      recordedVertices.clear();
      recordedColors.clear();
      recordedNormals.clear();
      recordedTexCoords.clear();
      recordedGroups.clear();
    }
  }

  // ////////////////////////////////////////////////////////////

  // PSHAPE RENDERING IN 3D

  public void shape(PShape shape, float x, float y, float z) {
    if (shape.isVisible()) { // don't do expensive matrix ops if invisible
      pushMatrix();

      if (shapeMode == CENTER) {
        translate(x - shape.getWidth() / 2, y - shape.getHeight() / 2, z
            - shape.getDepth() / 2);

      } else if ((shapeMode == CORNER) || (shapeMode == CORNERS)) {
        translate(x, y, z);
      }
      shape.draw(this);

      popMatrix();
    }
  }

  public void shape(PShape shape, float x, float y, float z, float c, float d,
      float e) {
    if (shape.isVisible()) { // don't do expensive matrix ops if invisible
      pushMatrix();

      if (shapeMode == CENTER) {
        // x, y and z are center, c, d and e refer to a diameter
        translate(x - c / 2f, y - d / 2f, z - e / 2f);
        scale(c / shape.getWidth(), d / shape.getHeight(), e / shape.getDepth());

      } else if (shapeMode == CORNER) {
        translate(x, y, z);
        scale(c / shape.getWidth(), d / shape.getHeight(), e / shape.getDepth());

      } else if (shapeMode == CORNERS) {
        // c, d, e are x2/y2/z2, make them into width/height/depth
        c -= x;
        d -= y;
        e -= z;
        // then same as above
        translate(x, y, z);
        scale(c / shape.getWidth(), d / shape.getHeight(), e / shape.getDepth());
      }
      shape.draw(this);

      popMatrix();
    }
  }

  // ////////////////////////////////////////////////////////////

  // BEZIER CURVE VERTICES

  // TODO it seem there are no evaluators in OpenGL ES

  // protected void bezierVertexCheck();
  // public void bezierVertex(float x2, float y2,
  // float x3, float y3,
  // float x4, float y4)
  // public void bezierVertex(float x2, float y2, float z2,
  // float x3, float y3, float z3,
  // float x4, float y4, float z4)

  // ////////////////////////////////////////////////////////////

  // CATMULL-ROM CURVE VERTICES

  // TODO it seem there are no evaluators in OpenGL ES

  // protected void curveVertexCheck();
  // public void curveVertex(float x, float y)
  // public void curveVertex(float x, float y, float z)
  // protected void curveVertexSegment(float x1, float y1,
  // float x2, float y2,
  // float x3, float y3,
  // float x4, float y4)
  // protected void curveVertexSegment(float x1, float y1, float z1,
  // float x2, float y2, float z2,
  // float x3, float y3, float z3,
  // float x4, float y4, float z4)

  // ////////////////////////////////////////////////////////////

  // POINTS (override from P3D)

  // Buffers to be passed to gl*Pointer() functions
  // must be direct, i.e., they must be placed on the
  // native heap where the garbage collector cannot
  // move them.
  //
  // Buffers with multi-byte datatypes (e.g., short, int, float)
  // must have their byte order set to native order

  // ByteBuffer vbb = ByteBuffer.allocateDirect(vertices.length*4);
  // vbb.order(ByteOrder.nativeOrder());
  // mVertexBuffer = vbb.asIntBuffer();
  // mVertexBuffer.put(vertices);
  // mVertexBuffer.position(0);
  //
  // ByteBuffer cbb = ByteBuffer.allocateDirect(colors.length*4);
  // cbb.order(ByteOrder.nativeOrder());
  // mColorBuffer = cbb.asIntBuffer();
  // mColorBuffer.put(colors);
  // mColorBuffer.position(0);
  //
  // mIndexBuffer = ByteBuffer.allocateDirect(indices.length);
  // mIndexBuffer.put(indices);
  // mIndexBuffer.position(0);

  // gl.glFrontFace(gl.GL_CW);
  // gl.glVertexPointer(3, gl.GL_FIXED, 0, mVertexBuffer);
  // gl.glColorPointer(4, gl.GL_FIXED, 0, mColorBuffer);
  // gl.glDrawElements(gl.GL_TRIANGLES, 36, gl.GL_UNSIGNED_BYTE, mIndexBuffer);

  protected void renderPoints(int start, int stop) {
    gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);
    gl.glEnableClientState(GL10.GL_COLOR_ARRAY);

    // Division by three needed because each int element in the buffer is used
    // to
    // store three coordinates.
    if (vertexBuffer.capacity() / 3 < 3 * (stop - start)) {
      expandBuffers();
    }

    float sw = vertices[lines[start][VERTEX1]][SW];
    if (sw > 0) {
      gl.glPointSize(sw); // can only be set outside glBegin/glEnd

      vertexBuffer.position(0);
      colorBuffer.position(0);

      int n = 0;
      for (int i = start; i < stop; i++) {
        float[] a = vertices[points[i][VERTEX1]];
        vertexArray[3 * n + 0] = toFixed32(a[VX]);
        vertexArray[3 * n + 1] = toFixed32(a[VY]);
        vertexArray[3 * n + 2] = toFixed32(a[VZ]);
        colorArray[4 * n + 0] = toFixed32(a[SR]);
        colorArray[4 * n + 1] = toFixed32(a[SG]);
        colorArray[4 * n + 2] = toFixed32(a[SB]);
        colorArray[4 * n + 3] = toFixed32(a[SA]);
        n++;
      }

      vertexBuffer.put(vertexArray);
      colorBuffer.put(colorArray);

      vertexBuffer.position(0);
      colorBuffer.position(0);

      gl.glVertexPointer(3, GL10.GL_FIXED, 0, vertexBuffer);
      gl.glColorPointer(4, GL10.GL_FIXED, 0, colorBuffer);
      gl.glDrawArrays(GL10.GL_POINTS, start, stop - start);
    }

    gl.glDisableClientState(GL10.GL_VERTEX_ARRAY);
    gl.glDisableClientState(GL10.GL_COLOR_ARRAY);
  }

  // protected void rawPoints(int start, int stop) // PGraphics3D

  // ////////////////////////////////////////////////////////////

  // LINES (override from P3D)

  // protected final void addLineBreak() // PGraphics3D

  /**
   * Begin a new section of stroked geometry.
   */
  protected final void addLineBreak() {
    if (pathCount == pathOffset.length) {
      pathOffset = PApplet.expand(pathOffset);
      pathLength = PApplet.expand(pathLength);
    }
    pathOffset[pathCount] = lineCount;
    pathLength[pathCount] = 0;
    pathCount++;
  }

  /**
   * Add this line.
   */
  protected void addLine(int a, int b) {
    if (lineCount == lines.length) {
      int temp[][] = new int[lineCount << 1][LINE_FIELD_COUNT];
      System.arraycopy(lines, 0, temp, 0, lineCount);
      lines = temp;
    }
    lines[lineCount][VERTEX1] = a;
    lines[lineCount][VERTEX2] = b;

    // lines[lineCount][STROKE_MODE] = strokeCap | strokeJoin;
    // lines[lineCount][STROKE_WEIGHT] = (int) (strokeWeight + 0.5f); // hmm
    lineCount++;

    // mark this piece as being part of the current path
    pathLength[pathCount - 1]++;
  }

  /**
   * In the current implementation, start and stop are ignored (in OpenGL). This
   * will obviously have to be revisited if/when proper depth sorting is
   * implemented.
   */
  protected void renderLines(int start, int stop) {
    report("render_lines in");

    float sw0 = 0;

    // Last transformation: inversion of coordinate to make compatible with
    // Processing's inverted Y axis.
    gl.glPushMatrix();
    gl.glScalef(1, -1, 1);

    gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);
    gl.glEnableClientState(GL10.GL_COLOR_ARRAY);

    for (int j = start; j < stop; j++) {
      int i = pathOffset[j];
      float sw = vertices[lines[i][VERTEX1]][SW];
      // report("render_lines 1");
      // stroke weight zero will cause a gl error
      if (sw > 0) {
        gl.glLineWidth(sw);
        if (sw0 != sw && recordingModel) {
          // Add new vertex group.

          int n0 = recordedVertices.size();
          int n1 = n0 + pathLength[j] - 1;
          // Identifying where this group should end (when stroke length
          // changes).
          for (int k = j + 1; k < stop; k++) {
            int i1 = pathOffset[k];
            float sw1 = vertices[lines[i1][VERTEX1]][SW];
            if (sw0 != sw1) {
              break;
            }
            n1 = n0 + pathLength[k] - 1;
          }

          VertexGroup group = PShape3D.newVertexGroup(n0, n1, LINE_STRIP, sw,
              null);

          recordedGroups.add(group);
        }

        // Division by three needed because each int element in the buffer is
        // used to
        // store three coordinates.
        if (vertexBuffer.capacity() / 3 <= 3 * (pathLength[j] + 1)) {
          expandBuffers();
        }

        vertexBuffer.position(0);
        colorBuffer.position(0);

        int n = 0;

        // always draw a first point
        float a[] = vertices[lines[i][VERTEX1]];
        if (recordingModel) {
          recordedVertices.add(new PVector(a[X], a[Y], a[Z]));
          recordedColors.add(new float[] { a[SR], a[SG], a[SB], a[SA] });
          recordedNormals.add(new PVector(0, 0, 0));
          recordedTexCoords.add(new PVector(0, 0, 0));
        } else {
          vertexArray[3 * n + 0] = toFixed32(a[X]);
          vertexArray[3 * n + 1] = toFixed32(a[Y]);
          vertexArray[3 * n + 2] = toFixed32(a[Z]);
          colorArray[4 * n + 0] = toFixed32(a[SR]);
          colorArray[4 * n + 1] = toFixed32(a[SG]);
          colorArray[4 * n + 2] = toFixed32(a[SB]);
          colorArray[4 * n + 3] = toFixed32(a[SA]);
          n++;
        }

        // on this and subsequent lines, only draw the second point
        for (int k = 0; k < pathLength[j]; k++) {
          float b[] = vertices[lines[i][VERTEX2]];

          if (recordingModel) {
            recordedVertices.add(new PVector(b[X], b[Y], b[Z]));
            recordedColors.add(new float[] { b[SR], b[SG], b[SB], b[SA] });
            recordedNormals.add(new PVector(0, 0, 0));
            recordedTexCoords.add(new PVector(0, 0, 0));
          } else {
            vertexArray[3 * n + 0] = toFixed32(b[X]);
            vertexArray[3 * n + 1] = toFixed32(b[Y]);
            vertexArray[3 * n + 2] = toFixed32(b[Z]);
            colorArray[4 * n + 0] = toFixed32(b[SR]);
            colorArray[4 * n + 1] = toFixed32(b[SG]);
            colorArray[4 * n + 2] = toFixed32(b[SB]);
            colorArray[4 * n + 3] = toFixed32(b[SA]);
            n++;
          }

          i++;
        }

        if (!recordingModel) {
          vertexBuffer.put(vertexArray);
          colorBuffer.put(colorArray);

          vertexBuffer.position(0);
          colorBuffer.position(0);

          gl.glVertexPointer(3, GL10.GL_FIXED, 0, vertexBuffer);
          gl.glColorPointer(4, GL10.GL_FIXED, 0, colorBuffer);
          gl.glDrawArrays(GL10.GL_LINE_STRIP, 0, pathLength[j] + 1);
        }

      }
      sw0 = sw;
    }

    gl.glPopMatrix();

    report("render_lines out");
  }

  // protected void rawLines(int start, int stop)

  // ////////////////////////////////////////////////////////////

  // TRIANGLES

  /**
   * Add the triangle.
   */
  protected void addTriangle(int a, int b, int c) {
    if (triangleCount == triangles.length) {
      int temp[][] = new int[triangleCount << 1][TRIANGLE_FIELD_COUNT];
      System.arraycopy(triangles, 0, temp, 0, triangleCount);
      triangles = temp;
    }

    triangles[triangleCount][VERTEX1] = a;
    triangles[triangleCount][VERTEX2] = b;
    triangles[triangleCount][VERTEX3] = c;

    triangleCount++;
    boolean firstFace = triangleCount == 1;
    if (textureImage != textureImagePrev || firstFace) {
      // A new face starts at the first triangle or when the texture changes.
      addNewFace(firstFace);
    } else {
      // mark this triangle as being part of the current face.
      faceLength[faceCount - 1]++;
    }

    textureImagePrev = textureImage;
  }

  // New "face" starts. A face is just a range of consecutive triangles
  // with the same texture applied to them (it could be null).
  protected void addNewFace(boolean firstFace) {
    if (faceCount == faceOffset.length) {
      faceOffset = PApplet.expand(faceOffset);
      faceLength = PApplet.expand(faceLength);
      faceTexture = PApplet.expand(faceTexture);
    }
    faceOffset[faceCount] = firstFace ? 0 : triangleCount;
    faceLength[faceCount] = 1;
    faceTexture[faceCount] = textureImage;
    faceCount++;
  }

  protected void renderTriangles(int start, int stop) {
    report("render_triangles in");

    PTexture tex = null;
    boolean texturing = false;

    // Last transformation: inversion of coordinate to make compatible with
    // Processing's inverted Y axis.
    gl.glPushMatrix();
    gl.glScalef(1, -1, 1);

    gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);
    gl.glEnableClientState(GL10.GL_COLOR_ARRAY);
    gl.glEnableClientState(GL10.GL_NORMAL_ARRAY);

    for (int j = start; j < stop; j++) {
      int i = faceOffset[j];
      FACECOUNT++;

      if (faceTexture[j] != null) {
        tex = faceTexture[j].getTexture();
        if (tex != null) {
          gl.glEnable(tex.getGLTarget());
          gl.glBindTexture(tex.getGLTarget(), tex.getGLTextureID());
          gl.glEnableClientState(GL10.GL_TEXTURE_COORD_ARRAY);
          texturing = true;
        } else {
          texturing = false;
        }
      } else {
        texturing = false;
      }

      if (recordingModel) {
        int n0 = recordedVertices.size();
        int n1 = n0 + 3 * faceLength[j] - 1;
        VertexGroup group = PShape3D.newVertexGroup(n0, n1, TRIANGLES, 0,
            faceTexture[j]);
        recordedGroups.add(group);
      }

      // Division by three needed because each int element in the buffer is used
      // to
      // store three coordinates.
      if (vertexBuffer.capacity() / 3 < 3 * faceLength[j]) {
        expandBuffers();
      }

      vertexBuffer.position(0);
      colorBuffer.position(0);
      normalBuffer.position(0);
      texCoordBuffer.position(0);

      int n = 0;
      for (int k = 0; k < faceLength[j]; k++) {
        TRIANGLECOUNT++;

        float a[] = vertices[triangles[i][VERTEX1]];
        float b[] = vertices[triangles[i][VERTEX2]];
        float c[] = vertices[triangles[i][VERTEX3]];

        float uscale = 1.0f;
        float vscale = 1.0f;
        float cx = 0.0f;
        float sx = +1.0f;
        float cy = 0.0f;
        float sy = +1.0f;
        if (texturing) {
          uscale *= tex.getMaxTextureCoordS();
          vscale *= tex.getMaxTextureCoordT();

          if (tex.isFlippedX()) {
            cx = 1.0f;
            sx = -1.0f;
          }

          if (tex.isFlippedY()) {
            cy = 1.0f;
            sy = -1.0f;
          }
        }

        // Adding vertex A.
        if (recordingModel) {
          recordedVertices.add(new PVector(a[X], a[Y], a[Z]));
          recordedColors.add(new float[] { a[R], a[G], a[B], a[A] });
          recordedNormals.add(new PVector(a[NX], a[NY], a[NZ]));
          recordedTexCoords.add(new PVector((cx + sx * a[U]) * uscale, (cy + sy
              * a[V])
              * vscale, 0.0f));
        } else {
          vertexArray[3 * n + 0] = toFixed32(a[X]);
          vertexArray[3 * n + 1] = toFixed32(a[Y]);
          vertexArray[3 * n + 2] = toFixed32(a[Z]);
          colorArray[4 * n + 0] = toFixed32(a[R]);
          colorArray[4 * n + 1] = toFixed32(a[G]);
          colorArray[4 * n + 2] = toFixed32(a[B]);
          colorArray[4 * n + 3] = toFixed32(a[A]);
          normalArray[3 * n + 0] = toFixed32(a[NX]);
          normalArray[3 * n + 1] = toFixed32(a[NY]);
          normalArray[3 * n + 2] = toFixed32(a[NZ]);
          texCoordArray[2 * n + 0] = toFixed32((cx + sx * a[U]) * uscale);
          texCoordArray[2 * n + 1] = toFixed32((cy + sy * a[V]) * vscale);
          n++;

          VERTEXCOUNT++;
        }

        // Adding vertex B.
        if (recordingModel) {
          recordedVertices.add(new PVector(b[X], b[Y], b[Z]));
          recordedColors.add(new float[] { b[R], b[G], b[B], b[A] });
          recordedNormals.add(new PVector(b[NX], b[NY], b[NZ]));
          recordedTexCoords.add(new PVector((cx + sx * b[U]) * uscale, (cy + sy
              * b[V])
              * vscale, 0.0f));
        } else {
          vertexArray[3 * n + 0] = toFixed32(b[X]);
          vertexArray[3 * n + 1] = toFixed32(b[Y]);
          vertexArray[3 * n + 2] = toFixed32(b[Z]);
          colorArray[4 * n + 0] = toFixed32(b[R]);
          colorArray[4 * n + 1] = toFixed32(b[G]);
          colorArray[4 * n + 2] = toFixed32(b[B]);
          colorArray[4 * n + 3] = toFixed32(b[A]);
          normalArray[3 * n + 0] = toFixed32(b[NX]);
          normalArray[3 * n + 1] = toFixed32(b[NY]);
          normalArray[3 * n + 2] = toFixed32(b[NZ]);
          texCoordArray[2 * n + 0] = toFixed32((cx + sx * b[U]) * uscale);
          texCoordArray[2 * n + 1] = toFixed32((cy + sy * b[V]) * vscale);
          n++;

          VERTEXCOUNT++;
        }

        // Adding vertex C.
        if (recordingModel) {
          recordedVertices.add(new PVector(c[X], c[Y], c[Z]));
          recordedColors.add(new float[] { c[R], c[G], c[B], c[A] });
          recordedNormals.add(new PVector(c[NX], c[NY], c[NZ]));
          recordedTexCoords.add(new PVector((cx + sx * c[U]) * uscale, (cy + sy
              * c[V])
              * vscale, 0.0f));
        } else {
          vertexArray[3 * n + 0] = toFixed32(c[X]);
          vertexArray[3 * n + 1] = toFixed32(c[Y]);
          vertexArray[3 * n + 2] = toFixed32(c[Z]);
          colorArray[4 * n + 0] = toFixed32(c[R]);
          colorArray[4 * n + 1] = toFixed32(c[G]);
          colorArray[4 * n + 2] = toFixed32(c[B]);
          colorArray[4 * n + 3] = toFixed32(c[A]);
          normalArray[3 * n + 0] = toFixed32(c[NX]);
          normalArray[3 * n + 1] = toFixed32(c[NY]);
          normalArray[3 * n + 2] = toFixed32(c[NZ]);
          texCoordArray[2 * n + 0] = toFixed32((cx + sx * c[U]) * uscale);
          texCoordArray[2 * n + 1] = toFixed32((cy + sy * c[V]) * vscale);
          n++;

          VERTEXCOUNT++;
        }

        i++;
      }

      if (!recordingModel) {
        vertexBuffer.put(vertexArray);
        colorBuffer.put(colorArray);
        normalBuffer.put(normalArray);
        if (texturing)
          texCoordBuffer.put(texCoordArray);

        vertexBuffer.position(0);
        colorBuffer.position(0);
        normalBuffer.position(0);
        texCoordBuffer.position(0);

        gl.glVertexPointer(3, GL10.GL_FIXED, 0, vertexBuffer);
        gl.glColorPointer(4, GL10.GL_FIXED, 0, colorBuffer);
        gl.glNormalPointer(GL10.GL_FIXED, 0, normalBuffer);
        if (texturing)
          gl.glTexCoordPointer(2, GL10.GL_FIXED, 0, texCoordBuffer);
        gl.glDrawArrays(GL10.GL_TRIANGLES, 0, 3 * faceLength[j]);
      }

      if (texturing) {
        gl.glBindTexture(tex.getGLTarget(), 0);
        gl.glDisable(tex.getGLTarget());
        gl.glDisableClientState(GL10.GL_TEXTURE_COORD_ARRAY);
      }
    }

    gl.glDisableClientState(GL10.GL_NORMAL_ARRAY);
    gl.glDisableClientState(GL10.GL_COLOR_ARRAY);
    gl.glDisableClientState(GL10.GL_VERTEX_ARRAY);

    gl.glPopMatrix();

    report("render_triangles out");
  }

  // protected void rawTriangles(int start, int stop) // PGraphics3D

  /**
   * Triangulate the current polygon. <BR>
   * <BR>
   * Simple ear clipping polygon triangulation adapted from code by John W.
   * Ratcliff (jratcliff at verant.com). Presumably <A
   * HREF="http://www.flipcode.org/cgi-bin/fcarticles.cgi?show=63943">this</A>
   * bit of code from the web.
   */
  protected void addPolygonTriangles() {
    if (vertexOrder.length != vertices.length) {
      int[] temp = new int[vertices.length];
      // since vertex_start may not be zero, might need to keep old stuff around
      PApplet.arrayCopy(vertexOrder, temp, vertexCount);
      vertexOrder = temp;
    }

    // this clipping algorithm only works in 2D, so in cases where a
    // polygon is drawn perpendicular to the z-axis, the area will be zero,
    // and triangulation will fail. as such, when the area calculates to
    // zero, figure out whether x or y is empty, and calculate based on the
    // two dimensions that actually contain information.
    // http://dev.processing.org/bugs/show_bug.cgi?id=111
    int d1 = X;
    int d2 = Y;
    // this brings up the nastier point that there may be cases where
    // a polygon is irregular in space and will throw off the
    // clockwise/counterclockwise calculation. for instance, if clockwise
    // relative to x and z, but counter relative to y and z or something
    // like that.. will wait to see if this is in fact a problem before
    // hurting my head on the math.

    /*
     * // trying to track down bug #774 for (int i = vertex_start; i <
     * vertex_end; i++) { if (i > vertex_start) { if (vertices[i-1][MX] ==
     * vertices[i][MX] && vertices[i-1][MY] == vertices[i][MY]) {
     * System.out.print("**** " ); } } System.out.println(i + " " +
     * vertices[i][MX] + " " + vertices[i][MY]); } System.out.println();
     */

    // first we check if the polygon goes clockwise or counterclockwise
    float area = 0;
    for (int p = shapeLast - 1, q = shapeFirst; q < shapeLast; p = q++) {
      area += (vertices[q][d1] * vertices[p][d2] - vertices[p][d1]
          * vertices[q][d2]);
    }
    // rather than checking for the perpendicular case first, only do it
    // when the area calculates to zero. checking for perpendicular would be
    // a needless waste of time for the 99% case.
    if (area == 0) {
      // figure out which dimension is the perpendicular axis
      boolean foundValidX = false;
      boolean foundValidY = false;

      for (int i = shapeFirst; i < shapeLast; i++) {
        for (int j = i; j < shapeLast; j++) {
          if (vertices[i][X] != vertices[j][X])
            foundValidX = true;
          if (vertices[i][Y] != vertices[j][Y])
            foundValidY = true;
        }
      }

      if (foundValidX) {
        // d1 = MX; // already the case
        d2 = Z;
      } else if (foundValidY) {
        // ermm.. which is the proper order for cw/ccw here?
        d1 = Y;
        d2 = Z;
      } else {
        // screw it, this polygon is just f-ed up
        return;
      }

      // re-calculate the area, with what should be good values
      for (int p = shapeLast - 1, q = shapeFirst; q < shapeLast; p = q++) {
        area += (vertices[q][d1] * vertices[p][d2] - vertices[p][d1]
            * vertices[q][d2]);
      }
    }

    // don't allow polygons to come back and meet themselves,
    // otherwise it will anger the triangulator
    // http://dev.processing.org/bugs/show_bug.cgi?id=97
    float vfirst[] = vertices[shapeFirst];
    float vlast[] = vertices[shapeLast - 1];
    if ((abs(vfirst[X] - vlast[X]) < EPSILON)
        && (abs(vfirst[Y] - vlast[Y]) < EPSILON)
        && (abs(vfirst[Z] - vlast[Z]) < EPSILON)) {
      shapeLast--;
    }

    // then sort the vertices so they are always in a counterclockwise order
    int j = 0;
    if (area > 0) {
      for (int i = shapeFirst; i < shapeLast; i++) {
        j = i - shapeFirst;
        vertexOrder[j] = i;
      }
    } else {
      for (int i = shapeFirst; i < shapeLast; i++) {
        j = i - shapeFirst;
        vertexOrder[j] = (shapeLast - 1) - j;
      }
    }

    // remove vc-2 Vertices, creating 1 triangle every time
    int vc = shapeLast - shapeFirst;
    int count = 2 * vc; // complex polygon detection

    for (int m = 0, v = vc - 1; vc > 2;) {
      boolean snip = true;

      // if we start over again, is a complex polygon
      if (0 >= (count--)) {
        break; // triangulation failed
      }

      // get 3 consecutive vertices <u,v,w>
      int u = v;
      if (vc <= u)
        u = 0; // previous
      v = u + 1;
      if (vc <= v)
        v = 0; // current
      int w = v + 1;
      if (vc <= w)
        w = 0; // next

      // Upgrade values to doubles, and multiply by 10 so that we can have
      // some better accuracy as we tessellate. This seems to have negligible
      // speed differences on Windows and Intel Macs, but causes a 50% speed
      // drop for PPC Macs with the bug's example code that draws ~200 points
      // in a concave polygon. Apple has abandoned PPC so we may as well too.
      // http://dev.processing.org/bugs/show_bug.cgi?id=774

      // triangle A B C
      double Ax = -10 * vertices[vertexOrder[u]][d1];
      double Ay = 10 * vertices[vertexOrder[u]][d2];
      double Bx = -10 * vertices[vertexOrder[v]][d1];
      double By = 10 * vertices[vertexOrder[v]][d2];
      double Cx = -10 * vertices[vertexOrder[w]][d1];
      double Cy = 10 * vertices[vertexOrder[w]][d2];

      // first we check if <u,v,w> continues going ccw
      if (EPSILON > (((Bx - Ax) * (Cy - Ay)) - ((By - Ay) * (Cx - Ax)))) {
        continue;
      }

      for (int p = 0; p < vc; p++) {
        if ((p == u) || (p == v) || (p == w)) {
          continue;
        }

        double Px = -10 * vertices[vertexOrder[p]][d1];
        double Py = 10 * vertices[vertexOrder[p]][d2];

        double ax = Cx - Bx;
        double ay = Cy - By;
        double bx = Ax - Cx;
        double by = Ay - Cy;
        double cx = Bx - Ax;
        double cy = By - Ay;
        double apx = Px - Ax;
        double apy = Py - Ay;
        double bpx = Px - Bx;
        double bpy = Py - By;
        double cpx = Px - Cx;
        double cpy = Py - Cy;

        double aCROSSbp = ax * bpy - ay * bpx;
        double cCROSSap = cx * apy - cy * apx;
        double bCROSScp = bx * cpy - by * cpx;

        if ((aCROSSbp >= 0.0) && (bCROSScp >= 0.0) && (cCROSSap >= 0.0)) {
          snip = false;
        }
      }

      if (snip) {
        addTriangle(vertexOrder[u], vertexOrder[v], vertexOrder[w]);

        m++;

        // remove v from remaining polygon
        for (int s = v, t = v + 1; t < vc; s++, t++) {
          vertexOrder[s] = vertexOrder[t];
        }
        vc--;

        // reset error detection counter
        count = 2 * vc;
      }
    }
  }

  protected void expandBuffers() {
    int newSize = vertexBuffer.capacity() / 3 << 1;

    ByteBuffer vbb = ByteBuffer.allocateDirect(newSize * 3 * SIZEOF_INT);
    vbb.order(ByteOrder.nativeOrder());
    vertexBuffer = vbb.asIntBuffer();

    ByteBuffer cbb = ByteBuffer.allocateDirect(newSize * 4 * SIZEOF_INT);
    cbb.order(ByteOrder.nativeOrder());
    colorBuffer = cbb.asIntBuffer();

    ByteBuffer tbb = ByteBuffer.allocateDirect(newSize * 2 * SIZEOF_INT);
    tbb.order(ByteOrder.nativeOrder());
    texCoordBuffer = tbb.asIntBuffer();

    ByteBuffer nbb = ByteBuffer.allocateDirect(newSize * 3 * SIZEOF_INT);
    nbb.order(ByteOrder.nativeOrder());
    normalBuffer = nbb.asIntBuffer();

    vertexArray = new int[newSize * 3];
    colorArray = new int[newSize * 4];
    texCoordArray = new int[newSize * 2];
    normalArray = new int[newSize * 3];
  }

  // ////////////////////////////////////////////////////////////

  // RENDERING

  // public void flush()

  // protected void render()

  // protected void sort()

  // ////////////////////////////////////////////////////////////

  // POINT, LINE, TRIANGLE, QUAD

  // Because vertex(x, y) is mapped to vertex(x, y, 0), none of these commands
  // need to be overridden from their default implementation in PGraphics.

  // public void point(float x, float y)

  // public void point(float x, float y, float z)

  // public void line(float x1, float y1, float x2, float y2)

  // public void line(float x1, float y1, float z1,
  // float x2, float y2, float z2)

  // public void triangle(float x1, float y1, float x2, float y2,
  // float x3, float y3)

  // public void quad(float x1, float y1, float x2, float y2,
  // float x3, float y3, float x4, float y4)

  // ////////////////////////////////////////////////////////////

  // RECT

  // public void rectMode(int mode)

  // public void rect(float a, float b, float c, float d)

  // protected void rectImpl(float x1, float y1, float x2, float y2)

  // ////////////////////////////////////////////////////////////

  // ELLIPSE

  // public void ellipseMode(int mode)

  protected void ellipseImpl(float x, float y, float w, float h) {
    float radiusH = w / 2;
    float radiusV = h / 2;

    float centerX = x + radiusH;
    float centerY = y + radiusV;

    float sx1 = screenX(x, y);
    float sy1 = screenY(x, y);
    float sx2 = screenX(x + w, y + h);
    float sy2 = screenY(x + w, y + h);

    if (fill) {
      int accuracy = (int) (TWO_PI * PApplet.dist(sx1, sy1, sx2, sy2) / 20);
      if (accuracy < 6)
        accuracy = 6;

      float inc = (float) SINCOS_LENGTH / accuracy;
      float val = 0;

      boolean strokeSaved = stroke;
      stroke = false;
      boolean smoothSaved = smooth;
      if (smooth && stroke) {
        smooth = false;
      }

      beginShape(TRIANGLE_FAN);
      normal(0, 0, 1);
      vertex(centerX, centerY);
      for (int i = 0; i < accuracy; i++) {
        vertex(centerX + cosLUT[(int) val] * radiusH, centerY
            + sinLUT[(int) val] * radiusV);
        val = (val + inc) % SINCOS_LENGTH;
      }
      // back to the beginning
      vertex(centerX + cosLUT[0] * radiusH, centerY + sinLUT[0] * radiusV);
      endShape();

      stroke = strokeSaved;
      smooth = smoothSaved;
    }

    if (stroke) {
      int accuracy = (int) (TWO_PI * PApplet.dist(sx1, sy1, sx2, sy2) / 8);
      if (accuracy < 6)
        accuracy = 6;

      float inc = (float) SINCOS_LENGTH / accuracy;
      float val = 0;

      boolean savedFill = fill;
      fill = false;

      val = 0;
      beginShape();
      for (int i = 0; i < accuracy; i++) {
        vertex(centerX + cosLUT[(int) val] * radiusH, centerY
            + sinLUT[(int) val] * radiusV);
        val = (val + inc) % SINCOS_LENGTH;
      }
      endShape(CLOSE);

      fill = savedFill;
    }
  }

  // public void ellipse(float a, float b, float c, float d)

  // public void arc(float a, float b, float c, float d,
  // float start, float stop)

  // protected void arcImpl(float x, float y, float w, float h,
  // float start, float stop)

  // ////////////////////////////////////////////////////////////

  // BOX

  // TODO GL and GLUT in GL ES doesn't offer functions to create
  // cubes.

  // public void box(float size)

  // public void box(float w, float h, float d) // P3D

  // ////////////////////////////////////////////////////////////

  // SPHERE

  // TODO GL and GLUT in GL ES doesn't offer functions to create
  // spheres.

  // public void sphereDetail(int res)

  // public void sphereDetail(int ures, int vres)

  // public void sphere(float r)

  // ////////////////////////////////////////////////////////////

  // BEZIER

  // public float bezierPoint(float a, float b, float c, float d, float t)

  // public float bezierTangent(float a, float b, float c, float d, float t)

  // public void bezierDetail(int detail)

  // public void bezier(float x1, float y1,
  // float x2, float y2,
  // float x3, float y3,
  // float x4, float y4)

  // public void bezier(float x1, float y1, float z1,
  // float x2, float y2, float z2,
  // float x3, float y3, float z3,
  // float x4, float y4, float z4)

  // ////////////////////////////////////////////////////////////

  // CATMULL-ROM CURVES

  // public float curvePoint(float a, float b, float c, float d, float t)

  // public float curveTangent(float a, float b, float c, float d, float t)

  // public void curveDetail(int detail)

  // public void curveTightness(float tightness)

  // public void curve(float x1, float y1,
  // float x2, float y2,
  // float x3, float y3,
  // float x4, float y4)

  // public void curve(float x1, float y1, float z1,
  // float x2, float y2, float z2,
  // float x3, float y3, float z3,
  // float x4, float y4, float z4)

  // ////////////////////////////////////////////////////////////

  // SMOOTH

  public void smooth() {
    smooth = true;
    if (hints[DISABLE_OPENGL_2X_SMOOTH]) {
      // gl.glEnable(GL10.GL_MULTISAMPLE);
      gl.glEnable(GL10.GL_POINT_SMOOTH);
      gl.glEnable(GL10.GL_LINE_SMOOTH);
      // gl.glEnable(GL10.GL_POLYGON_SMOOTH); // OpenGL ES
    }
  }

  public void noSmooth() {
    smooth = false;
    if (hints[DISABLE_OPENGL_2X_SMOOTH]) {
      // gl.glDisable(GL10.GL_MULTISAMPLE);
      gl.glDisable(GL10.GL_POINT_SMOOTH);
      gl.glDisable(GL10.GL_LINE_SMOOTH);
      // gl.glDisable(GL10.GL_POLYGON_SMOOTH); // OpenGL ES
    }
  }

  // ////////////////////////////////////////////////////////////

  // IMAGES

  // public void imageMode(int mode)

  // public void image(PImage image, float x, float y)

  // public void image(PImage image, float x, float y, float c, float d)

  // public void image(PImage image,
  // float a, float b, float c, float d,
  // int u1, int v1, int u2, int v2)

  // protected void imageImpl(PImage image,
  // float x1, float y1, float x2, float y2,
  // int u1, int v1, int u2, int v2)

  // ////////////////////////////////////////////////////////////

  // SHAPE

  // public void shapeMode(int mode)

  public void shape(PShape3D shape) {
    shape.draw(this);
  }

  public void shape(PShape3D shape, float x, float y) {
  }

  public void shape(PShape3D shape, float x, float y, float z) {
    pushMatrix();
    translate(x, y, z);
    shape.draw(this);
    popMatrix();
  }

  // public void shape(PShape shape, float x, float y, float c, float d)

  // ////////////////////////////////////////////////////////////

  // TEXT SETTINGS

  // public void textAlign(int align)

  // public void textAlign(int alignX, int alignY)

  // public float textAscent()

  // public float textDescent()

  // public void textFont(PFont which)

  // public void textFont(PFont which, float size)

  // public void textLeading(float leading)

  // public void textMode(int mode)

  // protected boolean textModeCheck(int mode)

  // public void textSize(float size)

  // public float textWidth(char c)

  // public float textWidth(String str)

  // protected float textWidthImpl(char buffer[], int start, int stop)

  // ////////////////////////////////////////////////////////////

  // TEXT

  // Nothing to do here.

  // ////////////////////////////////////////////////////////////

  // TEXT IMPL

  // protected void textLineAlignImpl(char buffer[], int start, int stop,
  // float x, float y)

  /**
   * Implementation of actual drawing for a line of text.
   */
  protected void textLineImpl(char buffer[], int start, int stop, float x, float y) {
    // Init opengl state for text rendering...
    gl.glEnable(GL10.GL_TEXTURE_2D);
    
    if (!blend || blendMode != BLEND) {
      gl.glEnable(GL10.GL_BLEND);
      gl.glBlendFunc(GL10.GL_SRC_ALPHA, GL10.GL_ONE_MINUS_SRC_ALPHA);
    }
    
    if (textFont.texIDList == null) {
      textFont.initTexture(gl, maxTextureSize, maxTextureSize);
      // Add all the current glyphs to the texture.
      textFont.addAllGlyphsToTexture(gl);
    }

    textFont.currentTexID = textFont.texIDList[0];
    gl.glBindTexture(GL10.GL_TEXTURE_2D, textFont.currentTexID);

    // Setting the current fill color as the font color.
    gl.glColor4f(colorFloats[0], colorFloats[1], colorFloats[2], colorFloats[3]);

    super.textLineImpl(buffer, start, stop, x, y);

    // Restoring current blend mode.
    if (blend) {
      blend(blendMode);
    } else {
      noBlend();
    }
    
    gl.glDisable(GL10.GL_TEXTURE_2D);    
  }

  protected void textCharImpl(char ch, float x, float y) {
    PFont.Glyph glyph = textFont.getGlyph(ch);

    if (glyph != null) {
      if (glyph.texture == null) {
        // Adding new glyph to the font texture.
        glyph.addToTexture(gl);
      }
      
      if (textMode == MODEL) {
        float high = glyph.height / (float) textFont.size;
        float bwidth = glyph.width / (float) textFont.size;
        float lextent = glyph.leftExtent / (float) textFont.size;
        float textent = glyph.topExtent / (float) textFont.size;

        float x1 = x + lextent * textSize;
        float y1 = y - textent * textSize;
        float x2 = x1 + bwidth * textSize;
        float y2 = y1 + high * textSize;

        textCharModelImpl(glyph.texture, x1, y1, x2, y2);

      } else if (textMode == SCREEN) {
        int xx = (int) x + glyph.leftExtent;
        int yy = (int) y - glyph.topExtent;

        int w0 = glyph.width;
        int h0 = glyph.height;

        textCharScreenImpl(glyph.texture, xx, yy, w0, h0);
      }
    }
  }

  protected void textCharModelImpl(Glyph.TextureInfo tex, float x1, float y1,
      float x2, float y2) {
    if (textFont.currentTexID != tex.glid) {
      gl.glBindTexture(GL10.GL_TEXTURE_2D, tex.glid);
      textFont.currentTexID = tex.glid;
    }

    gl11.glTexParameteriv(GL10.GL_TEXTURE_2D, GL11Ext.GL_TEXTURE_CROP_RECT_OES,
        tex.crop, 0);
    gl11x.glDrawTexfOES(x1, height - y2, 0, x2 - x1, y2 - y1);
  }

  protected void textCharScreenImpl(Glyph.TextureInfo tex, int xx, int yy,
      int w0, int h0) {
    if (textFont.currentTexID != tex.glid) {
      gl.glBindTexture(GL10.GL_TEXTURE_2D, tex.glid);
      textFont.currentTexID = tex.glid;
    }

    gl11.glTexParameteriv(GL10.GL_TEXTURE_2D, GL11Ext.GL_TEXTURE_CROP_RECT_OES,
        tex.crop, 0);
    gl11x.glDrawTexiOES(xx, height - yy, 0, w0, h0);
  }

  // ////////////////////////////////////////////////////////////

  // MATRIX STACK

  public void pushMatrix() {
    gl.glPushMatrix();
  }

  public void popMatrix() {
    gl.glPopMatrix();
    modelviewUpdated = false;
  }

  // ////////////////////////////////////////////////////////////

  // MATRIX TRANSFORMATIONS

  public void translate(float tx, float ty) {
    translate(tx, ty, 0);
  }

  public void translate(float tx, float ty, float tz) {
    // Translation along Y is inverted to account for Processing's inverted Y
    // axis
    // with respect to OpenGL. The other place where inversion occurs is when
    // drawing the geometric primitives (vertex arrays), where a -1 scaling
    // along Y is applied.
    gl.glTranslatef(tx, -ty, tz);
    modelviewUpdated = false;
  }

  /**
   * Two dimensional rotation. Same as rotateZ (this is identical to a 3D
   * rotation along the z-axis) but included for clarity -- it'd be weird for
   * people drawing 2D graphics to be using rotateZ. And they might kick our a--
   * for the confusion.
   */
  public void rotate(float angle) {
    rotateZ(angle);
  }

  public void rotateX(float angle) {
    gl.glRotatef(angle, 1, 0, 0);
    modelviewUpdated = false;
  }

  public void rotateY(float angle) {
    gl.glRotatef(angle, 0, 1, 0);
    modelviewUpdated = false;
  }

  public void rotateZ(float angle) {
    gl.glRotatef(angle, 0, 0, 1);
    modelviewUpdated = false;
  }

  /**
   * Rotate around an arbitrary vector, similar to glRotate(), except that it
   * takes radians (instead of degrees).
   */
  public void rotate(float angle, float v0, float v1, float v2) {
    gl.glRotatef(angle, v0, v1, v2);
    modelviewUpdated = false;
  }

  /**
   * Same as scale(s, s, s).
   */
  public void scale(float s) {
    scale(s, s, s);
  }

  /**
   * Same as scale(sx, sy, 1).
   */
  public void scale(float sx, float sy) {
    scale(sx, sy, 1);
  }

  /**
   * Scale in three dimensions.
   */
  public void scale(float x, float y, float z) {
    if (manipulatingCamera) {
      throw new RuntimeException(
          "scale() cannot be called again between beginCamera()/endCamera()");
    } else {
      gl.glScalef(x, y, z);
      modelviewUpdated = false;
    }
  }

  public void skewX(float angle) {
    float t = (float) Math.tan(angle);
    applyMatrix(1, t, 0, 0, 0, 1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1);
  }

  public void skewY(float angle) {
    float t = (float) Math.tan(angle);
    applyMatrix(1, 0, 0, 0, t, 1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1);
  }

  // ////////////////////////////////////////////////////////////

  // MATRIX MORE!

  public void resetMatrix() {
    gl.glLoadIdentity();
  }

  public void applyMatrix(PMatrix2D source) {
    applyMatrix(source.m00, source.m01, source.m02, source.m10, source.m11,
        source.m12);
  }

  public void applyMatrix(float n00, float n01, float n02, float n10,
      float n11, float n12) {
    applyMatrix(n00, n01, n02, 0, n10, n11, n12, 0, 0, 0, 1, 0, 0, 0, 0, 1);
  }

  public void applyMatrix(PMatrix3D source) {
    applyMatrix(source.m00, source.m01, source.m02, source.m03, source.m10,
        source.m11, source.m12, source.m13, source.m20, source.m21, source.m22,
        source.m23, source.m30, source.m31, source.m32, source.m33);
  }

  /**
   * Apply a 4x4 transformation matrix to the modelview stack using
   * glMultMatrix(). This call will be slow because it will try to calculate the
   * inverse of the transform. So avoid it whenever possible.
   */
  public void applyMatrix(float n00, float n01, float n02, float n03,
      float n10, float n11, float n12, float n13, float n20, float n21,
      float n22, float n23, float n30, float n31, float n32, float n33) {

    float[] mat = new float[16];

    mat[0] = n00;
    mat[1] = n10;
    mat[2] = n20;
    mat[3] = n30;

    mat[4] = n01;
    mat[5] = n11;
    mat[6] = n21;
    mat[7] = n31;

    mat[8] = n02;
    mat[9] = n12;
    mat[10] = n22;
    mat[11] = n32;

    mat[12] = n03;
    mat[13] = n13;
    mat[14] = n23;
    mat[15] = n33;

    gl.glMultMatrixf(mat, 0);

    getModelviewMatrix();
    calculateModelviewInverse();
  }

  // ////////////////////////////////////////////////////////////

  // MATRIX GET/SET/PRINT

  public PMatrix getMatrix() {
    PMatrix res = new PMatrix3D();
    res.set(modelview);
    return res;
  }

  // public PMatrix2D getMatrix(PMatrix2D target)

  public PMatrix3D getMatrix(PMatrix3D target) {
    if (target == null) {
      target = new PMatrix3D();
    }
    target.set(modelview);
    return target;
  }

  // public void setMatrix(PMatrix source)

  public void setMatrix(PMatrix2D source) {
    // not efficient, but at least handles the inverse stuff.
    resetMatrix();
    applyMatrix(source);
  }

  /**
   * Set the current transformation to the contents of the specified source.
   */
  public void setMatrix(PMatrix3D source) {
    // not efficient, but at least handles the inverse stuff.
    resetMatrix();
    applyMatrix(source);
  }

  /**
   * Print the current model (or "transformation") matrix.
   */
  public void printMatrix() {
    PMatrix3D tmp = new PMatrix3D();
    tmp.set(modelview);
    tmp.print();
  }

  /*
   * This function checks if the modelview matrix is set up to likely be drawing
   * in 2D. It merely checks if the non-translational piece of the matrix is
   * unity. If this is to be used, it should be coupled with a check that the
   * raw vertex coordinates lie in the z=0 plane. Mainly useful for applying
   * sub-pixel shifts to avoid 2d artifacts in the screen plane. Added by
   * ewjordan 6/13/07
   * 
   * TODO need to invert the logic here so that we can simply return the value,
   * rather than calculating true/false and returning it.
   */
  /*
   * private boolean drawing2D() { if (modelview.m00 != 1.0f || modelview.m11 !=
   * 1.0f || modelview.m22 != 1.0f || // check scale modelview.m01 != 0.0f ||
   * modelview.m02 != 0.0f || // check rotational pieces modelview.m10 != 0.0f
   * || modelview.m12 != 0.0f || modelview.m20 != 0.0f || modelview.m21 != 0.0f
   * || !((camera.m23-modelview.m23) <= EPSILON && (camera.m23-modelview.m23) >=
   * -EPSILON)) { // check for z-translation // Something about the modelview
   * matrix indicates 3d drawing // (or rotated 2d, in which case 2d subpixel
   * fixes probably aren't needed) return false; } else { //The matrix is
   * mapping z=0 vertices to the screen plane, // which means it's likely that
   * 2D drawing is happening. return true; } }
   */

  // ////////////////////////////////////////////////////////////

  // CAMERA

  /**
   * Set matrix mode to the camera matrix (instead of the current transformation
   * matrix). This means applyMatrix, resetMatrix, etc. will affect the camera.
   * <P>
   * Note that the camera matrix is *not* the perspective matrix, it contains
   * the values of the modelview matrix immediatly after the latter was
   * initialized with ortho() or camera(), or the modelview matrix as resul of
   * the operations applied between beginCamera()/endCamera().
   * <P>
   * beginCamera() specifies that all coordinate transforms until endCamera()
   * should be pre-applied in inverse to the camera transform matrix. Note that
   * this is only challenging when a user specifies an arbitrary matrix with
   * applyMatrix(). Then that matrix will need to be inverted, which may not be
   * possible. But take heart, if a user is applying a non-invertible matrix to
   * the camera transform, then he is clearly up to no good, and we can wash our
   * hands of those bad intentions.
   * <P>
   * begin/endCamera clauses do not automatically reset the camera transform
   * matrix. That's because we set up a nice default camera transform int
   * setup(), and we expect it to hold through draw(). So we don't reset the
   * camera transform matrix at the top of draw(). That means that an
   * innocuous-looking clause like
   * 
   * <PRE>
   * beginCamera();
   * translate(0, 0, 10);
   * endCamera();
   * </PRE>
   * 
   * at the top of draw(), will result in a runaway camera that shoots
   * infinitely out of the screen over time. In order to prevent this, it is
   * necessary to call some function that does a hard reset of the camera
   * transform matrix inside of begin/endCamera. Two options are
   * 
   * <PRE>
   * camera(); // sets up the nice default camera transform
   * resetMatrix(); // sets up the identity camera transform
   * </PRE>
   * 
   * So to rotate a camera a constant amount, you might try
   * 
   * <PRE>
   * beginCamera();
   * camera();
   * rotateY(PI / 8);
   * endCamera();
   * </PRE>
   */
  public void beginCamera() {
    if (manipulatingCamera) {
      throw new RuntimeException("beginCamera() cannot be called again "
          + "before endCamera()");
    } else {
      manipulatingCamera = true;
    }
  }

  /**
   * Record the current settings into the camera matrix, and set the matrix mode
   * back to the current transformation matrix.
   * <P>
   * Note that this will destroy any settings to scale(), translate(), or
   * whatever, because the final camera matrix will be copied (not multiplied)
   * into the modelview.
   */
  public void endCamera() {
    if (!manipulatingCamera) {
      throw new RuntimeException("Cannot call endCamera() "
          + "without first calling beginCamera()");
    }

    getModelviewMatrix();

    // At this point no scaling transformations are allowed during
    // beginCamera()/endCamera() which
    // makes sense if we thing of the camera as emulating a physical camera.
    // However, for later
    // implementation scaling could be allowed, and in this case an auxiliar
    // variable should be needed
    // in order to detect if scaling was applied between beginCamera() and
    // endCamera(). Using this variable
    // the calculation of the inverse of the modelview matrix can be switched
    // between this (very fast) and a
    // more general one (slower).
    calculateModelviewInvNoScaling();

    // Copying modelview matrix after camera transformations to the camera
    // matrices.
    PApplet.arrayCopy(modelview, camera);
    PApplet.arrayCopy(modelviewInv, cameraInv);

    // all done
    manipulatingCamera = false;
  }

  protected void getProjectionMatrix() {
    if (gl11 != null && matrixGetSupported) {
      gl11.glGetFloatv(GL11.GL_PROJECTION_MATRIX, projection, 0);
      projectionUpdated = true;
    } else {
      // TODO: Mechanism to get modelview matrix when no the funtion GetFloatv
      // is available.
      // Idea: when ony GL10 is available, then PMatrix3D versions of modelview
      // and projection
      // matrices are needed, and should be updated during the call to the
      // transformation methods
      // (rotate, translate, scale, etc).
    }
  }

  protected void getModelviewMatrix() {
    if (gl11 != null && matrixGetSupported) {
      gl11.glGetFloatv(GL11.GL_MODELVIEW_MATRIX, modelview, 0);
      modelviewUpdated = true;
    } else {
      // TODO: Mechanism to get modelview matrix when no the funtion GetFloatv
      // is available.
    }
  }

  // Calculates the inverse of the modelview matrix.
  protected void calculateModelviewInverse() {
    // TODO: Please finish!
  }

  // Calculates the inverse of the modelview matrix, assuming that no scaling
  // transformation was applied,
  // only translations and rotations.
  // Here is the derivation of the formula:
  // http://www-graphics.stanford.edu/courses/cs248-98-fall/Final/q4.html
  protected void calculateModelviewInvNoScaling() {
    float ux = modelview[0];
    float uy = modelview[1];
    float uz = modelview[2];

    float vx = modelview[4];
    float vy = modelview[5];
    float vz = modelview[6];

    float wx = modelview[8];
    float wy = modelview[9];
    float wz = modelview[10];

    float tx = modelview[12];
    float ty = modelview[13];
    float tz = modelview[14];

    modelviewInv[0] = ux;
    modelviewInv[1] = vx;
    modelviewInv[2] = wx;
    modelviewInv[3] = 0.0f;

    modelviewInv[4] = uy;
    modelviewInv[5] = vy;
    modelviewInv[6] = wy;
    modelviewInv[7] = 0.0f;

    modelviewInv[8] = uz;
    modelviewInv[9] = vz;
    modelviewInv[10] = wz;
    modelviewInv[11] = 0;

    modelviewInv[12] = -(ux * tx + uy * ty + uz * tz);
    modelviewInv[13] = -(vx * tx + vy * ty + vz * tz);
    modelviewInv[14] = -(wx * tx + wy * ty + wz * tz);
    modelviewInv[15] = 1.0f;
  }

  /**
   * Set camera to the default settings.
   * <P>
   * Processing camera behavior:
   * <P>
   * Camera behavior can be split into two separate components, camera
   * transformation, and projection. The transformation corresponds to the
   * physical location, orientation, and scale of the camera. In a physical
   * camera metaphor, this is what can manipulated by handling the camera body
   * (with the exception of scale, which doesn't really have a physcial analog).
   * The projection corresponds to what can be changed by manipulating the lens.
   * <P>
   * We maintain separate matrices to represent the camera transform and
   * projection. An important distinction between the two is that the camera
   * transform should be invertible, where the projection matrix should not,
   * since it serves to map three dimensions to two. It is possible to bake the
   * two matrices into a single one just by multiplying them together, but it
   * isn't a good idea, since lighting, z-ordering, and z-buffering all demand a
   * true camera z coordinate after modelview and camera transforms have been
   * applied but before projection. If the camera transform and projection are
   * combined there is no way to recover a good camera-space z-coordinate from a
   * model coordinate.
   * <P>
   * Fortunately, there are no functions that manipulate both camera
   * transformation and projection.
   * <P>
   * camera() sets the camera position, orientation, and center of the scene. It
   * replaces the camera transform with a new one.
   * <P>
   * The transformation functions are the same ones used to manipulate the
   * modelview matrix (scale, translate, rotate, etc.). But they are bracketed
   * with beginCamera(), endCamera() to indicate that they should apply (in
   * inverse), to the camera transformation matrix.
   */
  public void camera() {
    camera(cameraX, cameraY, cameraZ, cameraX, cameraY, 0, 0, 1, 0);
  }

  /**
   * More flexible method for dealing with camera().
   * <P>
   * The actual call is like gluLookat. Here's the real skinny on what does
   * what:
   * 
   * <PRE>
   * camera(); or
   * camera(ex, ey, ez, cx, cy, cz, ux, uy, uz);
   * </PRE>
   * 
   * do not need to be called from with beginCamera();/endCamera(); That's
   * because they always apply to the camera transformation, and they always
   * totally replace it. That means that any coordinate transforms done before
   * camera(); in draw() will be wiped out. It also means that camera() always
   * operates in untransformed world coordinates. Therefore it is always
   * redundant to call resetMatrix(); before camera(); This isn't technically
   * true of gluLookat, but it's pretty much how it's used.
   * <P>
   * Now, beginCamera(); and endCamera(); are useful if you want to move the
   * camera around using transforms like translate(), etc. They will wipe out
   * any coordinate system transforms that occur before them in draw(), but they
   * will not automatically wipe out the camera transform. This means that they
   * should be at the top of draw(). It also means that the following:
   * 
   * <PRE>
   * beginCamera();
   * rotateY(PI / 8);
   * endCamera();
   * </PRE>
   * 
   * will result in a camera that spins without stopping. If you want to just
   * rotate a small constant amount, try this:
   * 
   * <PRE>
   * beginCamera();
   * camera(); // sets up the default view
   * rotateY(PI / 8);
   * endCamera();
   * </PRE>
   * 
   * That will rotate a little off of the default view. Note that this is
   * entirely equivalent to
   * 
   * <PRE>
   * camera(); // sets up the default view
   * beginCamera();
   * rotateY(PI / 8);
   * endCamera();
   * </PRE>
   * 
   * because camera() doesn't care whether or not it's inside a begin/end
   * clause. Basically it's safe to use camera() or camera(ex, ey, ez, cx, cy,
   * cz, ux, uy, uz) as naked calls because they do all the matrix resetting
   * automatically.
   */
  public void camera(float eyeX, float eyeY, float eyeZ, float centerX,
      float centerY, float centerZ, float upX, float upY, float upZ) {
    // Calculating Z vector
    float z0 = eyeX - centerX;
    float z1 = eyeY - centerY;
    float z2 = eyeZ - centerZ;
    float mag = sqrt(z0 * z0 + z1 * z1 + z2 * z2);
    if (mag != 0) {
      z0 /= mag;
      z1 /= mag;
      z2 /= mag;
    }

    // Calculating Y vector
    float y0 = upX;
    float y1 = upY;
    float y2 = upZ;

    // Computing X vector as Y cross Z
    float x0 = y1 * z2 - y2 * z1;
    float x1 = -y0 * z2 + y2 * z0;
    float x2 = y0 * z1 - y1 * z0;

    // Recompute Y = Z cross X
    y0 = z1 * x2 - z2 * x1;
    y1 = -z0 * x2 + z2 * x0;
    y2 = z0 * x1 - z1 * x0;

    // Cross product gives area of parallelogram, which is < 1.0 for
    // non-perpendicular unit-length vectors; so normalize x, y here:
    mag = sqrt(x0 * x0 + x1 * x1 + x2 * x2);
    if (mag != 0) {
      x0 /= mag;
      x1 /= mag;
      x2 /= mag;
    }

    mag = sqrt(y0 * y0 + y1 * y1 + y2 * y2);
    if (mag != 0) {
      y0 /= mag;
      y1 /= mag;
      y2 /= mag;
    }

    modelview[0] = x0;
    modelview[1] = y0;
    modelview[2] = z0;
    modelview[3] = 0.0f;

    modelview[4] = x1;
    modelview[5] = y1;
    modelview[6] = z1;
    modelview[7] = 0.0f;

    modelview[8] = x2;
    modelview[9] = y2;
    modelview[10] = z2;
    modelview[11] = 0;

    modelview[12] = -cameraX;
    modelview[13] = cameraY;
    modelview[14] = -cameraZ;
    modelview[15] = 1.0f;

    gl.glMatrixMode(GL10.GL_MODELVIEW);
    gl.glLoadMatrixf(modelview, 0);
    modelviewUpdated = true; // CPU and GPU copies of modelview matrix match
                             // each other.

    calculateModelviewInvNoScaling();
    PApplet.arrayCopy(modelview, camera);
    PApplet.arrayCopy(modelviewInv, cameraInv);
  }

  /**
   * Print the current camera matrix.
   */
  public void printCamera() {
    PMatrix3D tmp = new PMatrix3D();
    tmp.set(camera);
    tmp.print();
  }

  // ////////////////////////////////////////////////////////////

  // PROJECTION

  /**
   * Calls ortho() with the proper parameters for Processing's standard
   * orthographic projection.
   */
  public void ortho() {
    ortho(0, width, 0, height, -10, 10);
  }

  /**
   * Similar to gluOrtho(), but wipes out the current projection matrix.
   * <P>
   * Implementation partially based on Mesa's matrix.c.
   */
  public void ortho(float left, float right, float bottom, float top,
      float near, float far) {
    float x = 2.0f / (right - left);
    float y = 2.0f / (top - bottom);
    float z = -2.0f / (far - near);

    float tx = -(right + left) / (right - left);
    float ty = -(top + bottom) / (top - bottom);
    float tz = -(far + near) / (far - near);

    projection[0] = x;
    projection[1] = 0.0f;
    projection[2] = 0.0f;
    projection[3] = 0.0f;

    projection[4] = 0.0f;
    projection[5] = y;
    projection[6] = 0.0f;
    projection[7] = 0.0f;

    projection[8] = 0;
    projection[9] = 0;
    projection[10] = z;
    projection[11] = 0.0f;

    projection[12] = tx;
    projection[13] = ty;
    projection[14] = tz;
    projection[15] = 1.0f;

    gl.glLoadMatrixf(projection, 0);
    projectionUpdated = true; // CPU and GPU copies of projection matrix match
                              // each other.
  }

  /**
   * Calls perspective() with Processing's standard coordinate projection.
   * <P>
   * Projection functions:
   * <UL>
   * <LI>frustrum()
   * <LI>ortho()
   * <LI>perspective()
   * </UL>
   * Each of these three functions completely replaces the projection matrix
   * with a new one. They can be called inside setup(), and their effects will
   * be felt inside draw(). At the top of draw(), the projection matrix is not
   * reset. Therefore the last projection function to be called always
   * dominates. On resize, the default projection is always established, which
   * has perspective.
   * <P>
   * This behavior is pretty much familiar from OpenGL, except where functions
   * replace matrices, rather than multiplying against the previous.
   * <P>
   */
  public void perspective() {
    perspective(cameraFOV, cameraAspect, cameraNear, cameraFar);
  }

  /**
   * Similar to gluPerspective(). Implementation based on Mesa's glu.c
   */
  public void perspective(float fov, float aspect, float zNear, float zFar) {
    float ymax = cameraNear * (float) Math.tan(cameraFOV / 2);
    float ymin = -ymax;
    float xmin = ymin * cameraAspect;
    float xmax = ymax * cameraAspect;
    frustum(xmin, xmax, ymin, ymax, cameraNear, cameraFar);
  }

  /**
   * Same as glFrustum(), except that it wipes out (rather than multiplies
   * against) the current perspective matrix.
   * <P>
   * Implementation based on the explanation in the OpenGL blue book.
   */
  public void frustum(float left, float right, float bottom, float top,
      float znear, float zfar) {
    float temp, temp2, temp3, temp4;
    temp = 2.0f * znear;
    temp2 = right - left;
    temp3 = top - bottom;
    temp4 = zfar - znear;
    projection[0] = temp / temp2;
    projection[1] = 0.0f;
    projection[2] = 0.0f;
    projection[3] = 0.0f;
    projection[4] = 0.0f;
    projection[5] = temp / temp3;
    projection[6] = 0.0f;
    projection[7] = 0.0f;
    projection[8] = (right + left) / temp2;
    projection[9] = (top + bottom) / temp3;
    projection[10] = (-zfar - znear) / temp4;
    projection[11] = -1.0f;
    projection[12] = 0.0f;
    projection[13] = 0.0f;
    projection[14] = (-temp * zfar) / temp4;
    projection[15] = 0.0f;

    gl.glMatrixMode(GL10.GL_PROJECTION);
    gl.glLoadMatrixf(projection, 0);
    projectionUpdated = true; // CPU and GPU copies of projection matrix match
                              // each other (are in synch).

    // The matrix mode is always MODELVIEW, because the user will be doing
    // geometrical transformations,
    // al the time, projection transformations only a few times.
    gl.glMatrixMode(GL10.GL_MODELVIEW);
  }

  /**
   * Print the current projection matrix.
   */
  public void printProjection() {
    PMatrix3D tmp = new PMatrix3D();
    tmp.set(projection);
    tmp.print();
  }

  // ////////////////////////////////////////////////////////////

  // SCREEN AND MODEL COORDS

  public float screenX(float x, float y) {
    return screenX(x, y, 0);
  }

  public float screenY(float x, float y) {
    return screenY(x, y, 0);
  }

  public float screenX(float x, float y, float z) {
    y = -1 * y; // To take into account Processsing's inverted Y axis with
                // respect to OpenGL.

    if (!modelviewUpdated)
      getModelviewMatrix();
    if (!projectionUpdated)
      getProjectionMatrix();

    float ax = modelview[toArrayIndex(0, 0)] * x
        + modelview[toArrayIndex(0, 1)] * y + modelview[toArrayIndex(0, 2)] * z
        + modelview[toArrayIndex(0, 3)];
    float ay = modelview[toArrayIndex(1, 0)] * x
        + modelview[toArrayIndex(1, 1)] * y + modelview[toArrayIndex(1, 2)] * z
        + modelview[toArrayIndex(1, 3)];
    float az = modelview[toArrayIndex(2, 0)] * x
        + modelview[toArrayIndex(2, 1)] * y + modelview[toArrayIndex(2, 2)] * z
        + modelview[toArrayIndex(2, 3)];
    float aw = modelview[toArrayIndex(3, 0)] * x
        + modelview[toArrayIndex(3, 1)] * y + modelview[toArrayIndex(3, 2)] * z
        + modelview[toArrayIndex(3, 3)];

    float ox = projection[toArrayIndex(0, 0)] * ax
        + projection[toArrayIndex(0, 1)] * ay + projection[toArrayIndex(0, 2)]
        * az + projection[toArrayIndex(0, 3)] * aw;
    float ow = projection[toArrayIndex(3, 0)] * ax
        + projection[toArrayIndex(3, 1)] * ay + projection[toArrayIndex(3, 2)]
        * az + projection[toArrayIndex(3, 3)] * aw;

    if (ow != 0)
      ox /= ow;
    return width * (1 + ox) / 2.0f;
  }

  public float screenY(float x, float y, float z) {
    y = -1 * y; // To take into account Processsing's inverted Y axis with
                // respect to OpenGL.

    if (!modelviewUpdated)
      getModelviewMatrix();
    if (!projectionUpdated)
      getProjectionMatrix();

    float ax = modelview[toArrayIndex(0, 0)] * x
        + modelview[toArrayIndex(0, 1)] * y + modelview[toArrayIndex(0, 2)] * z
        + modelview[toArrayIndex(0, 3)];
    float ay = modelview[toArrayIndex(1, 0)] * x
        + modelview[toArrayIndex(1, 1)] * y + modelview[toArrayIndex(1, 2)] * z
        + modelview[toArrayIndex(1, 3)];
    float az = modelview[toArrayIndex(2, 0)] * x
        + modelview[toArrayIndex(2, 1)] * y + modelview[toArrayIndex(2, 2)] * z
        + modelview[toArrayIndex(2, 3)];
    float aw = modelview[toArrayIndex(3, 0)] * x
        + modelview[toArrayIndex(3, 1)] * y + modelview[toArrayIndex(3, 2)] * z
        + modelview[toArrayIndex(3, 3)];

    float oy = projection[toArrayIndex(1, 0)] * ax
        + projection[toArrayIndex(1, 1)] * ay + projection[toArrayIndex(1, 2)]
        * az + projection[toArrayIndex(1, 3)] * aw;
    float ow = projection[toArrayIndex(3, 0)] * ax
        + projection[toArrayIndex(3, 1)] * ay + projection[toArrayIndex(3, 2)]
        * az + projection[toArrayIndex(3, 3)] * aw;

    if (ow != 0)
      oy /= ow;
    return height * (1 + oy) / 2.0f;
  }

  public float screenZ(float x, float y, float z) {
    if (!modelviewUpdated)
      getModelviewMatrix();
    if (!projectionUpdated)
      getProjectionMatrix();

    y = -1 * y; // To take into account Processsing's inverted Y axis with
                // respect to OpenGL.

    float ax = modelview[toArrayIndex(0, 0)] * x
        + modelview[toArrayIndex(0, 1)] * y + modelview[toArrayIndex(0, 2)] * z
        + modelview[toArrayIndex(0, 3)];
    float ay = modelview[toArrayIndex(1, 0)] * x
        + modelview[toArrayIndex(1, 1)] * y + modelview[toArrayIndex(1, 2)] * z
        + modelview[toArrayIndex(1, 3)];
    float az = modelview[toArrayIndex(2, 0)] * x
        + modelview[toArrayIndex(2, 1)] * y + modelview[toArrayIndex(2, 2)] * z
        + modelview[toArrayIndex(2, 3)];
    float aw = modelview[toArrayIndex(3, 0)] * x
        + modelview[toArrayIndex(3, 1)] * y + modelview[toArrayIndex(3, 2)] * z
        + modelview[toArrayIndex(3, 3)];

    float oz = projection[toArrayIndex(2, 0)] * ax
        + projection[toArrayIndex(2, 1)] * ay + projection[toArrayIndex(2, 2)]
        * az + projection[toArrayIndex(2, 3)] * aw;
    float ow = projection[toArrayIndex(3, 0)] * ax
        + projection[toArrayIndex(3, 1)] * ay + projection[toArrayIndex(3, 2)]
        * az + projection[toArrayIndex(3, 3)] * aw;

    if (ow != 0)
      oz /= ow;
    return (oz + 1) / 2.0f;
  }

  public float modelX(float x, float y, float z) {
    if (!modelviewUpdated)
      getModelviewMatrix();

    y = -1 * y; // To take into account Processsing's inverted Y axis with
                // respect to OpenGL.

    float ax = modelview[toArrayIndex(0, 0)] * x
        + modelview[toArrayIndex(0, 1)] * y + modelview[toArrayIndex(0, 2)] * z
        + modelview[toArrayIndex(0, 3)];
    float ay = modelview[toArrayIndex(1, 0)] * x
        + modelview[toArrayIndex(1, 1)] * y + modelview[toArrayIndex(1, 2)] * z
        + modelview[toArrayIndex(1, 3)];
    float az = modelview[toArrayIndex(2, 0)] * x
        + modelview[toArrayIndex(2, 1)] * y + modelview[toArrayIndex(2, 2)] * z
        + modelview[toArrayIndex(2, 3)];
    float aw = modelview[toArrayIndex(3, 0)] * x
        + modelview[toArrayIndex(3, 1)] * y + modelview[toArrayIndex(3, 2)] * z
        + modelview[toArrayIndex(3, 3)];

    float ox = cameraInv[toArrayIndex(0, 0)] * ax
        + cameraInv[toArrayIndex(0, 1)] * ay + cameraInv[toArrayIndex(0, 2)]
        * az + cameraInv[toArrayIndex(0, 3)] * aw;
    float ow = cameraInv[toArrayIndex(3, 0)] * ax
        + cameraInv[toArrayIndex(3, 1)] * ay + cameraInv[toArrayIndex(3, 2)]
        * az + cameraInv[toArrayIndex(3, 3)] * aw;

    return (ow != 0) ? ox / ow : ox;
  }

  public float modelY(float x, float y, float z) {

    if (!modelviewUpdated)
      getModelviewMatrix();

    y = -1 * y; // To take into account Processsing's inverted Y axis with
                // respect to OpenGL.

    float ax = modelview[toArrayIndex(0, 0)] * x
        + modelview[toArrayIndex(0, 1)] * y + modelview[toArrayIndex(0, 2)] * z
        + modelview[toArrayIndex(0, 3)];
    float ay = modelview[toArrayIndex(1, 0)] * x
        + modelview[toArrayIndex(1, 1)] * y + modelview[toArrayIndex(1, 2)] * z
        + modelview[toArrayIndex(1, 3)];
    float az = modelview[toArrayIndex(2, 0)] * x
        + modelview[toArrayIndex(2, 1)] * y + modelview[toArrayIndex(2, 2)] * z
        + modelview[toArrayIndex(2, 3)];
    float aw = modelview[toArrayIndex(3, 0)] * x
        + modelview[toArrayIndex(3, 1)] * y + modelview[toArrayIndex(3, 2)] * z
        + modelview[toArrayIndex(3, 3)];

    float oy = cameraInv[toArrayIndex(1, 0)] * ax
        + cameraInv[toArrayIndex(1, 1)] * ay + cameraInv[toArrayIndex(1, 2)]
        * az + cameraInv[toArrayIndex(1, 3)] * aw;
    float ow = cameraInv[toArrayIndex(3, 0)] * ax
        + cameraInv[toArrayIndex(3, 1)] * ay + cameraInv[toArrayIndex(3, 2)]
        * az + cameraInv[toArrayIndex(3, 3)] * aw;

    return (ow != 0) ? oy / ow : oy;
  }

  public float modelZ(float x, float y, float z) {

    if (!modelviewUpdated)
      getModelviewMatrix();

    y = -1 * y; // To take into account Processsing's inverted Y axis with
                // respect to OpenGL.

    float ax = modelview[toArrayIndex(0, 0)] * x
        + modelview[toArrayIndex(0, 1)] * y + modelview[toArrayIndex(0, 2)] * z
        + modelview[toArrayIndex(0, 3)];
    float ay = modelview[toArrayIndex(1, 0)] * x
        + modelview[toArrayIndex(1, 1)] * y + modelview[toArrayIndex(1, 2)] * z
        + modelview[toArrayIndex(1, 3)];
    float az = modelview[toArrayIndex(2, 0)] * x
        + modelview[toArrayIndex(2, 1)] * y + modelview[toArrayIndex(2, 2)] * z
        + modelview[toArrayIndex(2, 3)];
    float aw = modelview[toArrayIndex(3, 0)] * x
        + modelview[toArrayIndex(3, 1)] * y + modelview[toArrayIndex(3, 2)] * z
        + modelview[toArrayIndex(3, 3)];

    float oz = cameraInv[toArrayIndex(2, 0)] * ax
        + cameraInv[toArrayIndex(2, 1)] * ay + cameraInv[toArrayIndex(2, 2)]
        * az + cameraInv[toArrayIndex(2, 3)] * aw;
    float ow = cameraInv[toArrayIndex(3, 0)] * ax
        + cameraInv[toArrayIndex(3, 1)] * ay + cameraInv[toArrayIndex(3, 2)]
        * az + cameraInv[toArrayIndex(3, 3)] * aw;

    return (ow != 0) ? oz / ow : oz;
  }

  private int toArrayIndex(int i, int j) {
    return 4 * j + i;
  }

  // STYLES

  // public void pushStyle()
  // public void popStyle()
  // public void style(PStyle)
  // public PStyle getStyle()
  // public void getStyle(PStyle)

  // ////////////////////////////////////////////////////////////

  // COLOR MODE

  // public void colorMode(int mode)
  // public void colorMode(int mode, float max)
  // public void colorMode(int mode, float mx, float my, float mz);
  // public void colorMode(int mode, float mx, float my, float mz, float ma);

  // ////////////////////////////////////////////////////////////

  // COLOR CALC

  // protected void colorCalc(int rgb)
  // protected void colorCalc(int rgb, float alpha)
  // protected void colorCalc(float gray)
  // protected void colorCalc(float gray, float alpha)
  // protected void colorCalc(float x, float y, float z)
  // protected void colorCalc(float x, float y, float z, float a)
  // protected void colorCalcARGB(int argb, float alpha)

  // ////////////////////////////////////////////////////////////

  // STROKE CAP/JOIN/WEIGHT

  public void strokeWeight(float weight) {
    this.strokeWeight = weight;
  }

  public void strokeJoin(int join) {
    if (join != DEFAULT_STROKE_JOIN) {
      showMethodWarning("strokeJoin");
    }
  }

  public void strokeCap(int cap) {
    if (cap != DEFAULT_STROKE_CAP) {
      showMethodWarning("strokeCap");
    }
  }

  // ////////////////////////////////////////////////////////////

  // STROKE, TINT, FILL

  // public void noStroke()
  // public void stroke(int rgb)
  // public void stroke(int rgb, float alpha)
  // public void stroke(float gray)
  // public void stroke(float gray, float alpha)
  // public void stroke(float x, float y, float z)
  // public void stroke(float x, float y, float z, float a)
  // protected void strokeFromCalc()

  // public void noTint()
  // public void tint(int rgb)
  // public void tint(int rgb, float alpha)
  // public void tint(float gray)
  // public void tint(float gray, float alpha)
  // public void tint(float x, float y, float z)
  // public void tint(float x, float y, float z, float a)
  // protected void tintFromCalc()

  // public void noFill()
  // public void fill(int rgb)
  // public void fill(int rgb, float alpha)
  // public void fill(float gray)
  // public void fill(float gray, float alpha)
  // public void fill(float x, float y, float z)
  // public void fill(float x, float y, float z, float a)

  protected void fillFromCalc() {
    super.fillFromCalc();
    calcColorBuffer();
    gl.glMaterialfv(GL10.GL_FRONT_AND_BACK, GL10.GL_AMBIENT_AND_DIFFUSE,
        colorFloats, 0);
  }

  // ////////////////////////////////////////////////////////////

  // MATERIAL PROPERTIES

  // public void ambient(int rgb) {
  // super.ambient(rgb);
  // calcColorBuffer();
  // gl.glMaterialfv(GL.GL_FRONT_AND_BACK, GL.GL_AMBIENT, colorBuffer, 0);
  // }

  // public void ambient(float gray) {
  // super.ambient(gray);
  // calcColorBuffer();
  // gl.glMaterialfv(GL.GL_FRONT_AND_BACK, GL.GL_AMBIENT, colorBuffer, 0);
  // }

  // public void ambient(float x, float y, float z) {
  // super.ambient(x, y, z);
  // calcColorBuffer();
  // gl.glMaterialfv(GL.GL_FRONT_AND_BACK, GL.GL_AMBIENT, colorBuffer, 0);
  // }

  protected void ambientFromCalc() {
    super.ambientFromCalc();
    calcColorBuffer();
    gl.glMaterialfv(GL10.GL_FRONT_AND_BACK, GL10.GL_AMBIENT, colorFloats, 0);
  }

  // public void specular(int rgb) {
  // super.specular(rgb);
  // calcColorBuffer();
  // gl.glMaterialfv(GL.GL_FRONT_AND_BACK, GL.GL_SPECULAR, colorBuffer, 0);
  // }

  // public void specular(float gray) {
  // super.specular(gray);
  // calcColorBuffer();
  // gl.glMaterialfv(GL.GL_FRONT_AND_BACK, GL.GL_SPECULAR, colorBuffer, 0);
  // }

  // public void specular(float x, float y, float z) {
  // super.specular(x, y, z);
  // calcColorBuffer();
  // gl.glMaterialfv(GL.GL_FRONT_AND_BACK, GL.GL_SPECULAR, colorBuffer, 0);
  // }

  protected void specularFromCalc() {
    super.specularFromCalc();
    calcColorBuffer();
    gl.glMaterialfv(GL10.GL_FRONT_AND_BACK, GL10.GL_SPECULAR, colorFloats, 0);
  }

  public void shininess(float shine) {
    super.shininess(shine);
    gl.glMaterialf(GL10.GL_FRONT_AND_BACK, GL10.GL_SHININESS, shine);
  }

  // public void emissive(int rgb) {
  // super.emissive(rgb);
  // calcColorBuffer();
  // gl.glMaterialfv(GL.GL_FRONT_AND_BACK, GL.GL_EMISSION, colorBuffer, 0);
  // }

  // public void emissive(float gray) {
  // super.emissive(gray);
  // calcColorBuffer();
  // gl.glMaterialfv(GL.GL_FRONT_AND_BACK, GL.GL_EMISSION, colorBuffer, 0);
  // }

  // public void emissive(float x, float y, float z) {
  // super.emissive(x, y, z);
  // calcColorBuffer();
  // gl.glMaterialfv(GL.GL_FRONT_AND_BACK, GL.GL_EMISSION, colorBuffer, 0);
  // }

  protected void emissiveFromCalc() {
    super.emissiveFromCalc();
    calcColorBuffer();
    gl.glMaterialfv(GL10.GL_FRONT_AND_BACK, GL10.GL_EMISSION, colorFloats, 0);
  }

  // ////////////////////////////////////////////////////////////

  // LIGHTING

  /**
   * Sets up an ambient and directional light using OpenGL. API takef from
   * PGraphics3D.
   * 
   * <PRE>
   * The Lighting Skinny:
   * The way lighting works is complicated enough that it's worth
   * producing a document to describe it. Lighting calculations proceed
   * pretty much exactly as described in the OpenGL red book.
   * Light-affecting material properties:
   *   AMBIENT COLOR
   *   - multiplies by light's ambient component
   *   - for believability this should match diffuse color
   *   DIFFUSE COLOR
   *   - multiplies by light's diffuse component
   *   SPECULAR COLOR
   *   - multiplies by light's specular component
   *   - usually less colored than diffuse/ambient
   *   SHININESS
   *   - the concentration of specular effect
   *   - this should be set pretty high (20-50) to see really
   *     noticeable specularity
   *   EMISSIVE COLOR
   *   - constant additive color effect
   * Light types:
   *   AMBIENT
   *   - one color
   *   - no specular color
   *   - no direction
   *   - may have falloff (constant, linear, and quadratic)
   *   - may have position (which matters in non-constant falloff case)
   *   - multiplies by a material's ambient reflection
   *   DIRECTIONAL
   *   - has diffuse color
   *   - has specular color
   *   - has direction
   *   - no position
   *   - no falloff
   *   - multiplies by a material's diffuse and specular reflections
   *   POINT
   *   - has diffuse color
   *   - has specular color
   *   - has position
   *   - no direction
   *   - may have falloff (constant, linear, and quadratic)
   *   - multiplies by a material's diffuse and specular reflections
   *   SPOT
   *   - has diffuse color
   *   - has specular color
   *   - has position
   *   - has direction
   *   - has cone angle (set to half the total cone angle)
   *   - has concentration value
   *   - may have falloff (constant, linear, and quadratic)
   *   - multiplies by a material's diffuse and specular reflections
   * Normal modes:
   * All of the primitives (rect, box, sphere, etc.) have their normals
   * set nicely. During beginShape/endShape normals can be set by the user.
   *   AUTO-NORMAL
   *   - if no normal is set during the shape, we are in auto-normal mode
   *   - auto-normal calculates one normal per triangle (face-normal mode)
   *   SHAPE-NORMAL
   *   - if one normal is set during the shape, it will be used for
   *     all vertices
   *   VERTEX-NORMAL
   *   - if multiple normals are set, each normal applies to
   *     subsequent vertices
   *   - (except for the first one, which applies to previous
   *     and subsequent vertices)
   * Efficiency consequences:
   *   There is a major efficiency consequence of position-dependent
   *   lighting calculations per vertex. (See below for determining
   *   whether lighting is vertex position-dependent.) If there is no
   *   position dependency then the only factors that affect the lighting
   *   contribution per vertex are its colors and its normal.
   *   There is a major efficiency win if
   *   1) lighting is not position dependent
   *   2) we are in AUTO-NORMAL or SHAPE-NORMAL mode
   *   because then we can calculate one lighting contribution per shape
   *   (SHAPE-NORMAL) or per triangle (AUTO-NORMAL) and simply multiply it
   *   into the vertex colors. The converse is our worst-case performance when
   *   1) lighting is position dependent
   *   2) we are in AUTO-NORMAL mode
   *   because then we must calculate lighting per-face * per-vertex.
   *   Each vertex has a different lighting contribution per face in
   *   which it appears. Yuck.
   * Determining vertex position dependency:
   *   If any of the following factors are TRUE then lighting is
   *   vertex position dependent:
   *   1) Any lights uses non-constant falloff
   *   2) There are any point or spot lights
   *   3) There is a light with specular color AND there is a
   *      material with specular color
   * So worth noting is that default lighting (a no-falloff ambient
   * and a directional without specularity) is not position-dependent.
   * We should capitalize.
   * Simon Greenwold, April 2005
   * </PRE>
   */
  public void lights() {
    lights = true;
    gl.glEnable(GL10.GL_LIGHTING);

    // need to make sure colorMode is RGB 255 here
    int colorModeSaved = colorMode;
    colorMode = RGB;

    lightFalloff(1, 0, 0);
    lightSpecular(0, 0, 0);

    ambientLight(colorModeX * 0.5f, colorModeY * 0.5f, colorModeZ * 0.5f);
    directionalLight(colorModeX * 0.5f, colorModeY * 0.5f, colorModeZ * 0.5f, 0, 0, -1);

    colorMode = colorModeSaved;
  }

  /**
   * Switches off all lights, but keeps lighting enabled. TODO: discuss if this
   * method is needed.
   */
  public void resetLights() {
    for (int i = 0; i < lightCount; i++) {
      glLightDisable(i);
    }
    lightCount = 0;
  }

  /**
   * Disables lighting.
   */
  public void noLights() {
    lights = false;
    gl.glDisable(GL10.GL_LIGHTING);
    lightCount = 0;
  }

  /**
   * Add an ambient light based on the current color mode.
   */
  public void ambientLight(float r, float g, float b) {
    ambientLight(r, g, b, 0, 0, 0);
  }

  /**
   * Add an ambient light based on the current color mode. This version includes
   * an (x, y, z) position for situations where the falloff distance is used.
   */
  public void ambientLight(float r, float g, float b, float x, float y, float z) {
    if (lightCount == MAX_LIGHTS) {
      throw new RuntimeException("can only create " + MAX_LIGHTS + " lights");
    }
    colorCalc(r, g, b);
    lightDiffuse[lightCount][0] = calcR;
    lightDiffuse[lightCount][1] = calcG;
    lightDiffuse[lightCount][2] = calcB;
    lightDiffuse[lightCount][3] = 1.0f;

    lightType[lightCount] = AMBIENT;
    lightFalloffConstant[lightCount] = currentLightFalloffConstant;
    lightFalloffLinear[lightCount] = currentLightFalloffLinear;
    lightFalloffQuadratic[lightCount] = currentLightFalloffQuadratic;
    lightPosition[lightCount][0] = x;
    lightPosition[lightCount][1] = y;
    lightPosition[lightCount][2] = z;
    lightPosition[lightCount][3] = 1.0f;

    glLightEnable(lightCount);
    glLightAmbient(lightCount);
    glLightPosition(lightCount);
    glLightFalloff(lightCount);
    glLightNoSpot(lightCount);

    lightCount++;
  }

  public void directionalLight(float r, float g, float b, float nx, float ny,
      float nz) {
    if (lightCount == MAX_LIGHTS) {
      throw new RuntimeException("can only create " + MAX_LIGHTS + " lights");
    }
    colorCalc(r, g, b);
    lightDiffuse[lightCount][0] = calcR;
    lightDiffuse[lightCount][1] = calcG;
    lightDiffuse[lightCount][2] = calcB;
    lightDiffuse[lightCount][3] = 1.0f;

    lightType[lightCount] = DIRECTIONAL;
    lightFalloffConstant[lightCount] = currentLightFalloffConstant;
    lightFalloffLinear[lightCount] = currentLightFalloffLinear;
    lightFalloffQuadratic[lightCount] = currentLightFalloffQuadratic;
    lightSpecular[lightCount][0] = currentLightSpecular[0];
    lightSpecular[lightCount][1] = currentLightSpecular[1];
    lightSpecular[lightCount][2] = currentLightSpecular[2];
    lightSpecular[lightCount][2] = currentLightSpecular[3];

    float invn = 1.0f / PApplet.dist(0, 0, 0, nx, ny, nz);
    lightNormal[lightCount][0] = invn * nx;
    lightNormal[lightCount][1] = invn * ny;
    lightNormal[lightCount][2] = invn * nz;
    lightNormal[lightCount][3] = 0.0f;

    glLightEnable(lightCount);
    glLightNoAmbient(lightCount);
    glLightDirection(lightCount);
    glLightDiffuse(lightCount);
    glLightSpecular(lightCount);
    glLightFalloff(lightCount);
    glLightNoSpot(lightCount);

    lightCount++;
  }

  public void pointLight(float r, float g, float b, float x, float y, float z) {
    if (lightCount == MAX_LIGHTS) {
      throw new RuntimeException("can only create " + MAX_LIGHTS + " lights");
    }
    colorCalc(r, g, b);
    lightDiffuse[lightCount][0] = calcR;
    lightDiffuse[lightCount][1] = calcG;
    lightDiffuse[lightCount][2] = calcB;
    lightDiffuse[lightCount][3] = 1.0f;

    lightType[lightCount] = POINT;
    lightFalloffConstant[lightCount] = currentLightFalloffConstant;
    lightFalloffLinear[lightCount] = currentLightFalloffLinear;
    lightFalloffQuadratic[lightCount] = currentLightFalloffQuadratic;
    lightSpecular[lightCount][0] = currentLightSpecular[0];
    lightSpecular[lightCount][1] = currentLightSpecular[1];
    lightSpecular[lightCount][2] = currentLightSpecular[2];

    lightPosition[lightCount][0] = x;
    lightPosition[lightCount][1] = y;
    lightPosition[lightCount][2] = z;
    lightPosition[lightCount][3] = 1.0f;

    glLightEnable(lightCount);
    glLightNoAmbient(lightCount);
    glLightPosition(lightCount);
    glLightDiffuse(lightCount);
    glLightSpecular(lightCount);
    glLightFalloff(lightCount);
    glLightNoSpot(lightCount);

    lightCount++;
  }

  public void spotLight(float r, float g, float b, float x, float y, float z,
      float nx, float ny, float nz, float angle, float concentration) {
    if (lightCount == MAX_LIGHTS) {
      throw new RuntimeException("can only create " + MAX_LIGHTS + " lights");
    }
    colorCalc(r, g, b);
    lightDiffuse[lightCount][0] = calcR;
    lightDiffuse[lightCount][1] = calcG;
    lightDiffuse[lightCount][2] = calcB;
    lightDiffuse[lightCount][3] = 1.0f;

    lightType[lightCount] = SPOT;
    lightFalloffConstant[lightCount] = currentLightFalloffConstant;
    lightFalloffLinear[lightCount] = currentLightFalloffLinear;
    lightFalloffQuadratic[lightCount] = currentLightFalloffQuadratic;
    lightSpecular[lightCount][0] = currentLightSpecular[0];
    lightSpecular[lightCount][1] = currentLightSpecular[1];
    lightSpecular[lightCount][2] = currentLightSpecular[2];

    lightPosition[lightCount][0] = x;
    lightPosition[lightCount][1] = y;
    lightPosition[lightCount][2] = z;
    lightPosition[lightCount][3] = 1.0f;

    float invn = 1.0f / PApplet.dist(0, 0, 0, nx, ny, nz);
    lightNormal[lightCount][0] = invn * nx;
    lightNormal[lightCount][1] = invn * ny;
    lightNormal[lightCount][2] = invn * nz;
    lightNormal[lightCount][3] = 0.0f;

    lightSpotAngle[lightCount] = PApplet.degrees(angle);
    lightSpotAngleCos[lightCount] = Math.max(0, (float) Math.cos(angle));
    lightSpotConcentration[lightCount] = concentration;

    glLightEnable(lightCount);
    glLightNoAmbient(lightCount);
    glLightPosition(lightCount);
    glLightDirection(lightCount);
    glLightDiffuse(lightCount);
    glLightSpecular(lightCount);
    glLightFalloff(lightCount);
    glLightSpotAngle(lightCount);
    glLightSpotConcentration(lightCount);

    lightCount++;
  }

  /**
   * Set the light falloff rates for the last light that was created. Default is
   * lightFalloff(1, 0, 0).
   */
  public void lightFalloff(float constant, float linear, float quadratic) {
    currentLightFalloffConstant = constant;
    currentLightFalloffLinear = linear;
    currentLightFalloffQuadratic = quadratic;
  }

  /**
   * Set the specular color of the last light created.
   */
  public void lightSpecular(float x, float y, float z) {
    colorCalc(x, y, z);
    currentLightSpecular[0] = calcR;
    currentLightSpecular[1] = calcG;
    currentLightSpecular[2] = calcB;
    currentLightSpecular[3] = 1.0f;
  }

  private void glLightAmbient(int num) {
    gl.glLightfv(GL10.GL_LIGHT0 + num, GL10.GL_AMBIENT, lightDiffuse[num], 0);
  }

  private void glLightNoAmbient(int num) {
    gl.glLightfv(GL10.GL_LIGHT0 + num, GL10.GL_AMBIENT, zeroLight, 0);
  }

  private void glLightNoSpot(int num) {
    gl.glLightf(GL10.GL_LIGHT0 + num, GL10.GL_SPOT_CUTOFF, 180);
    gl.glLightf(GL10.GL_LIGHT0 + num, GL10.GL_SPOT_EXPONENT, 0);
  }

  private void glLightDiffuse(int num) {
    gl.glLightfv(GL10.GL_LIGHT0 + num, GL10.GL_DIFFUSE, lightDiffuse[num], 0);
  }

  private void glLightDirection(int num) {
    if (lightType[num] == DIRECTIONAL) {
      // TODO this expects a fourth arg that will be set to 1
      // this is why lightBuffer is length 4,
      // and the [3] element set to 1 in the constructor.
      // however this may be a source of problems since
      // it seems a bit "hack"
      gl.glLightfv(GL10.GL_LIGHT0 + num, GL10.GL_POSITION, lightNormal[num], 0);
    } else { // spotlight
      // this one only needs the 3 arg version
      gl.glLightfv(GL10.GL_LIGHT0 + num, GL10.GL_SPOT_DIRECTION,
          lightNormal[num], 0);
    }
  }

  private void glLightEnable(int num) {
    gl.glEnable(GL10.GL_LIGHT0 + num);
  }

  private void glLightDisable(int num) {
    gl.glDisable(GL10.GL_LIGHT0 + num);
  }

  private void glLightFalloff(int num) {
    gl.glLightf(GL10.GL_LIGHT0 + num, GL10.GL_CONSTANT_ATTENUATION,
        lightFalloffConstant[num]);
    gl.glLightf(GL10.GL_LIGHT0 + num, GL10.GL_LINEAR_ATTENUATION,
        lightFalloffLinear[num]);
    gl.glLightf(GL10.GL_LIGHT0 + num, GL10.GL_QUADRATIC_ATTENUATION,
        lightFalloffQuadratic[num]);
  }

  private void glLightPosition(int num) {
    gl.glLightfv(GL10.GL_LIGHT0 + num, GL10.GL_POSITION, lightPosition[num], 0);
  }

  private void glLightSpecular(int num) {
    gl.glLightfv(GL10.GL_LIGHT0 + num, GL10.GL_SPECULAR, lightSpecular[num], 0);
  }

  private void glLightSpotAngle(int num) {
    gl.glLightf(GL10.GL_LIGHT0 + num, GL10.GL_SPOT_CUTOFF, lightSpotAngle[num]);
  }

  private void glLightSpotConcentration(int num) {
    gl.glLightf(GL10.GL_LIGHT0 + num, GL10.GL_SPOT_EXPONENT,
        lightSpotConcentration[num]);
  }

  // ////////////////////////////////////////////////////////////

  // BACKGROUND

  protected void backgroundImpl(PImage image) {
    gl.glClearColor(backgroundR, backgroundG, backgroundB, 1);
    gl.glClear(GL10.GL_COLOR_BUFFER_BIT | GL10.GL_DEPTH_BUFFER_BIT);
    set(0, 0, image);
  }

  protected void backgroundImpl() {
    gl.glClearColor(backgroundR, backgroundG, backgroundB, 1);
    gl.glClear(GL10.GL_COLOR_BUFFER_BIT | GL10.GL_DEPTH_BUFFER_BIT);
  }

  // ////////////////////////////////////////////////////////////

  // COLOR MODE

  // colorMode() is inherited from PGraphics.

  // ////////////////////////////////////////////////////////////

  // COLOR CALC

  // This is the OpenGL complement to the colorCalc() methods.

  /**
   * Load the calculated color into a pre-allocated array so that it can be
   * quickly passed over to OpenGL.
   */
  private final void calcColorBuffer() {
    if (colorFloats == null) {
      // colorBuffer = BufferUtil.newFloatBuffer(4);
      colorFloats = new float[4];
    }
    colorFloats[0] = calcR;
    colorFloats[1] = calcG;
    colorFloats[2] = calcB;
    colorFloats[3] = calcA;
    // colorBuffer.put(0, calcR);
    // colorBuffer.put(1, calcG);
    // colorBuffer.put(2, calcB);
    // colorBuffer.put(3, calcA);
    // colorBuffer.rewind();
  }

  // ////////////////////////////////////////////////////////////

  // COLOR METHODS

  // public final int color(int gray)
  // public final int color(int gray, int alpha)
  // public final int color(int rgb, float alpha)
  // public final int color(int x, int y, int z)

  // public final float alpha(int what)
  // public final float red(int what)
  // public final float green(int what)
  // public final float blue(int what)
  // public final float hue(int what)
  // public final float saturation(int what)
  // public final float brightness(int what)

  // public int lerpColor(int c1, int c2, float amt)
  // static public int lerpColor(int c1, int c2, float amt, int mode)

  // ////////////////////////////////////////////////////////////

  // BEGINRAW/ENDRAW

  // beginRaw, endRaw() both inherited.

  // ////////////////////////////////////////////////////////////

  // WARNINGS and EXCEPTIONS

  // showWarning() and showException() available from PGraphics.

  /**
   * Report on anything from glError(). Don't use this inside glBegin/glEnd
   * otherwise it'll throw an GL_INVALID_OPERATION error.
   */
  public void report(String where) {
    if (!hints[DISABLE_OPENGL_ERROR_REPORT]) {
      int err = gl.glGetError();
      if (err != GL10.GL_NO_ERROR) {
        String errString = GLU.gluErrorString(err);
        String msg = "OpenGL error " + err + " at " + where + ": " + errString;
        PGraphics.showWarning(msg);
      }
    }  
  }

  // ////////////////////////////////////////////////////////////

  // RENDERER SUPPORT QUERIES

  // public boolean displayable()

  // public boolean dimensional() // from P3D

  // ////////////////////////////////////////////////////////////

  // PIMAGE METHODS

  // getImage
  // setCache, getCache, removeCache
  // isModified, setModified

  // ////////////////////////////////////////////////////////////

  // LOAD/UPDATE PIXELS

  public void loadPixels() {
    if ((pixels == null) || (pixels.length != width * height)) {
      pixels = new int[width * height];
      pixelBuffer = BufferUtil.newIntBuffer(pixels.length);
      // pixelBuffer = IntBuffer.allocate(pixels.length);
    }

    gl.glReadPixels(0, 0, width, height, GL10.GL_RGBA, GL10.GL_UNSIGNED_BYTE,
        pixelBuffer);
    pixelBuffer.get(pixels);
    pixelBuffer.rewind();

    // flip vertically (opengl stores images upside down),
    // and swap RGBA components to ARGB (big endian)
    int index = 0;
    int yindex = (height - 1) * width;
    for (int y = 0; y < height / 2; y++) {
      if (BIG_ENDIAN) {
        for (int x = 0; x < width; x++) {
          int temp = pixels[index];
          // ignores alpha component, just sets it opaque
          pixels[index] = 0xff000000 | ((pixels[yindex] >> 8) & 0x00ffffff);
          pixels[yindex] = 0xff000000 | ((temp >> 8) & 0x00ffffff);

          index++;
          yindex++;
        }
      } else { // LITTLE_ENDIAN, convert ABGR to ARGB
        for (int x = 0; x < width; x++) {
          int temp = pixels[index];

          // identical to endPixels because only two
          // components are being swapped
          pixels[index] = 0xff000000 | ((pixels[yindex] << 16) & 0xff0000)
              | (pixels[yindex] & 0xff00) | ((pixels[yindex] >> 16) & 0xff);

          pixels[yindex] = 0xff000000 | ((temp << 16) & 0xff0000)
              | (temp & 0xff00) | ((temp >> 16) & 0xff);

          index++;
          yindex++;
        }
      }
      yindex -= width * 2;
    }

    // When height is an odd number, the middle line needs to be
    // endian swapped, but not y-swapped.
    // http://dev.processing.org/bugs/show_bug.cgi?id=944
    if ((height % 2) == 1) {
      index = (height / 2) * width;
      if (BIG_ENDIAN) {
        for (int x = 0; x < width; x++) {
          // ignores alpha component, just sets it opaque
          pixels[index] = 0xff000000 | ((pixels[index] >> 8) & 0x00ffffff);
        }
      } else {
        for (int x = 0; x < width; x++) {
          pixels[index] = 0xff000000 | ((pixels[index] << 16) & 0xff0000)
              | (pixels[index] & 0xff00) | ((pixels[index] >> 16) & 0xff);
        }
      }
    }
  }

  /**
   * Convert native OpenGL format into palatable ARGB format. This function
   * leaves alone (ignores) the alpha component. Also flips the image
   * vertically, since images are upside-down in GL.
   */
  static void nativeToJavaRGB(PImage image) {
    int index = 0;
    int yindex = (image.height - 1) * image.width;
    for (int y = 0; y < image.height / 2; y++) {
      if (BIG_ENDIAN) {
        for (int x = 0; x < image.width; x++) {
          int temp = image.pixels[index];
          // ignores alpha component, just sets it opaque
          image.pixels[index] = 0xff000000 | ((image.pixels[yindex] >> 8) & 0x00ffffff);
          image.pixels[yindex] = 0xff000000 | ((temp >> 8) & 0x00ffffff);
          index++;
          yindex++;
        }
      } else { // LITTLE_ENDIAN, convert ABGR to ARGB
        for (int x = 0; x < image.width; x++) {
          int temp = image.pixels[index];

          // identical to endPixels because only two
          // components are being swapped
          image.pixels[index] = 0xff000000
              | ((image.pixels[yindex] << 16) & 0xff0000)
              | (image.pixels[yindex] & 0xff00)
              | ((image.pixels[yindex] >> 16) & 0xff);

          image.pixels[yindex] = 0xff000000 | ((temp << 16) & 0xff0000)
              | (temp & 0xff00) | ((temp >> 16) & 0xff);

          index++;
          yindex++;
        }
      }
      yindex -= image.width * 2;
    }
  }

  /**
   * Convert native OpenGL format into palatable ARGB format. This function
   * leaves alone (ignores) the alpha component. Also flips the image
   * vertically, since images are upside-down in GL.
   */
  static void nativeToJavaARGB(PImage image) {
    int index = 0;
    int yindex = (image.height - 1) * image.width;
    for (int y = 0; y < image.height / 2; y++) {
      if (BIG_ENDIAN) {
        for (int x = 0; x < image.width; x++) {
          int temp = image.pixels[index];
          // ignores alpha component, just sets it opaque
          image.pixels[index] = (image.pixels[yindex] & 0xff000000)
              | ((image.pixels[yindex] >> 8) & 0x00ffffff);
          image.pixels[yindex] = (temp & 0xff000000)
              | ((temp >> 8) & 0x00ffffff);
          index++;
          yindex++;
        }
      } else { // LITTLE_ENDIAN, convert ABGR to ARGB
        for (int x = 0; x < image.width; x++) {
          int temp = image.pixels[index];

          // identical to endPixels because only two
          // components are being swapped
          image.pixels[index] = (image.pixels[yindex] & 0xff000000)
              | ((image.pixels[yindex] << 16) & 0xff0000)
              | (image.pixels[yindex] & 0xff00)
              | ((image.pixels[yindex] >> 16) & 0xff);

          image.pixels[yindex] = (temp & 0xff000000)
              | ((temp << 16) & 0xff0000) | (temp & 0xff00)
              | ((temp >> 16) & 0xff);

          index++;
          yindex++;
        }
      }
      yindex -= image.width * 2;
    }
  }

  /**
   * Convert ARGB (Java/Processing) data to native OpenGL format. This function
   * leaves alone (ignores) the alpha component. Also flips the image
   * vertically, since images are upside-down in GL.
   */
  static void javaToNativeRGB(PImage image) {
    int width = image.width;
    int height = image.height;
    int pixels[] = image.pixels;

    int index = 0;
    int yindex = (height - 1) * width;
    for (int y = 0; y < height / 2; y++) {
      if (BIG_ENDIAN) {
        // and convert ARGB back to opengl RGBA components (big endian)
        for (int x = 0; x < image.width; x++) {
          int temp = pixels[index];
          /*
           * pixels[index] = ((pixels[yindex] >> 24) & 0xff) | ((pixels[yindex]
           * << 8) & 0xffffff00); pixels[yindex] = ((temp >> 24) & 0xff) |
           * ((temp << 8) & 0xffffff00);
           */
          pixels[index] = ((pixels[yindex] << 8) & 0xffffff00) | 0xff;
          pixels[yindex] = ((temp << 8) & 0xffffff00) | 0xff;

          index++;
          yindex++;
        }

      } else {
        // convert ARGB back to native little endian ABGR
        for (int x = 0; x < width; x++) {
          int temp = pixels[index];

          pixels[index] = 0xff000000 | ((pixels[yindex] << 16) & 0xff0000)
              | (pixels[yindex] & 0xff00) | ((pixels[yindex] >> 16) & 0xff);

          pixels[yindex] = 0xff000000 | ((temp << 16) & 0xff0000)
              | (temp & 0xff00) | ((temp >> 16) & 0xff);

          index++;
          yindex++;
        }
      }
      yindex -= width * 2;
    }
  }

  /**
   * Convert Java ARGB to native OpenGL format. Also flips the image vertically,
   * since images are upside-down in GL.
   */
  static void javaToNativeARGB(PImage image) {
    int width = image.width;
    int height = image.height;
    int pixels[] = image.pixels;

    int index = 0;
    int yindex = (height - 1) * width;
    for (int y = 0; y < height / 2; y++) {
      if (BIG_ENDIAN) {
        // and convert ARGB back to opengl RGBA components (big endian)
        for (int x = 0; x < image.width; x++) {
          int temp = pixels[index];
          pixels[index] = ((pixels[yindex] >> 24) & 0xff)
              | ((pixels[yindex] << 8) & 0xffffff00);
          pixels[yindex] = ((temp >> 24) & 0xff) | ((temp << 8) & 0xffffff00);

          index++;
          yindex++;
        }

      } else {
        // convert ARGB back to native little endian ABGR
        for (int x = 0; x < width; x++) {
          int temp = pixels[index];

          pixels[index] = (pixels[yindex] & 0xff000000)
              | ((pixels[yindex] << 16) & 0xff0000) | (pixels[yindex] & 0xff00)
              | ((pixels[yindex] >> 16) & 0xff);

          pixels[yindex] = (pixels[yindex] & 0xff000000)
              | ((temp << 16) & 0xff0000) | (temp & 0xff00)
              | ((temp >> 16) & 0xff);

          index++;
          yindex++;
        }
      }
      yindex -= width * 2;
    }
  }

  public void updatePixels() {
    // flip vertically (opengl stores images upside down),

    int index = 0;
    int yindex = (height - 1) * width;
    for (int y = 0; y < height / 2; y++) {
      if (BIG_ENDIAN) {
        // and convert ARGB back to opengl RGBA components (big endian)
        for (int x = 0; x < width; x++) {
          int temp = pixels[index];

          pixels[index] = ((pixels[yindex] << 8) & 0xffffff00) | 0xff;
          pixels[yindex] = ((temp << 8) & 0xffffff00) | 0xff;

          index++;
          yindex++;
        }

      } else {
        // convert ARGB back to native little endian ABGR
        for (int x = 0; x < width; x++) {
          int temp = pixels[index];

          pixels[index] = 0xff000000 | ((pixels[yindex] << 16) & 0xff0000)
              | (pixels[yindex] & 0xff00) | ((pixels[yindex] >> 16) & 0xff);

          pixels[yindex] = 0xff000000 | ((temp << 16) & 0xff0000)
              | (temp & 0xff00) | ((temp >> 16) & 0xff);

          index++;
          yindex++;
        }
      }
      yindex -= width * 2;
    }

    // re-pack ARGB data into RGBA for opengl (big endian)
    // for (int i = 0; i < pixels.length; i++) {
    // pixels[i] = ((pixels[i] >> 24) & 0xff) |
    // ((pixels[i] << 8) & 0xffffff00);
    // }

    setRasterPos(0, 0); // lower-left corner

    pixelBuffer.put(pixels);
    pixelBuffer.rewind();
    // TODO fix me for android
    // gl.glDrawPixels(width, height,
    // GL10.GL_RGBA, GL10.GL_UNSIGNED_BYTE, pixelBuffer);
  }

  // ////////////////////////////////////////////////////////////

  // RESIZE

  public void resize(int wide, int high) {
    PGraphics.showMethodWarning("resize");
  }

  // ////////////////////////////////////////////////////////////

  // GET/SET

  // IntBuffer getsetBuffer = IntBuffer.allocate(1);
  IntBuffer getsetBuffer = BufferUtil.newIntBuffer(1);

  // int getset[] = new int[1];

  public int get(int x, int y) {
    gl.glReadPixels(x, y, 1, 1, GL10.GL_RGBA, GL10.GL_UNSIGNED_BYTE,
        getsetBuffer);
    int getset = getsetBuffer.get(0);

    if (BIG_ENDIAN) {
      return 0xff000000 | ((getset >> 8) & 0x00ffffff);

    } else {
      return 0xff000000 | ((getset << 16) & 0xff0000) | (getset & 0xff00)
          | ((getset >> 16) & 0xff);
    }
  }

  // public PImage get(int x, int y, int w, int h)

  protected PImage getImpl(int x, int y, int w, int h) {
    PImage newbie = new PImage(w, h); // new int[w*h], w, h, ARGB);

    // IntBuffer newbieBuffer = BufferUtil.newIntBuffer(w*h);
    IntBuffer newbieBuffer = IntBuffer.allocate(w * h);
    gl.glReadPixels(x, y, w, h, GL10.GL_RGBA, GL10.GL_UNSIGNED_BYTE,
        newbieBuffer);
    newbieBuffer.get(newbie.pixels);

    nativeToJavaARGB(newbie);
    return newbie;
  }

  public PImage get() {
    return get(0, 0, width, height);
  }

  public void set(int x, int y, int argb) {
    int getset = 0;

    if (BIG_ENDIAN) {
      // convert ARGB to RGBA
      getset = (argb << 8) | 0xff;

    } else {
      // convert ARGB to ABGR
      getset = (argb & 0xff00ff00) | ((argb << 16) & 0xff0000)
          | ((argb >> 16) & 0xff);
    }
    getsetBuffer.put(0, getset);
    getsetBuffer.rewind();
    // gl.glRasterPos2f(x + EPSILON, y + EPSILON);
    setRasterPos(x, (height - y) - 1);
    // TODO whither drawPixels?
    // gl.glDrawPixels(1, 1, GL10.GL_RGBA, GL10.GL_UNSIGNED_BYTE, getsetBuffer);
  }

  /**
   * Set an image directly to the screen.
   * <P>
   * TODO not optimized properly, creates multiple temporary buffers the size of
   * the image. Needs to instead use image cache, but that requires two types of
   * image cache. One for power of 2 textures and another for
   * glReadPixels/glDrawPixels data that's flipped vertically. Both have their
   * components all swapped to native.
   */
  public void set(int x, int y, PImage source) {
    int[] backup = new int[source.pixels.length];
    System.arraycopy(source.pixels, 0, backup, 0, source.pixels.length);
    javaToNativeARGB(source);

    // TODO is this possible without intbuffer?
    IntBuffer setBuffer = BufferUtil.newIntBuffer(source.pixels.length);
    setBuffer.put(source.pixels);
    setBuffer.rewind();

    setRasterPos(x, (height - y) - source.height); // +source.height);
    // gl.glDrawPixels(source.width, source.height,
    // GL10.GL_RGBA, GL10.GL_UNSIGNED_BYTE, setBuffer);
    source.pixels = backup;
  }

  // TODO remove the implementation above and use setImpl instead,
  // since it'll be more efficient
  // http://dev.processing.org/bugs/show_bug.cgi?id=943
  // protected void setImpl(int dx, int dy, int sx, int sy, int sw, int sh,
  // PImage src)

  /**
   * Definitive method for setting raster pos, including offscreen locations.
   * The raster position is tricky because it's affected by the modelview and
   * projection matrices. Further, offscreen coords won't properly set the
   * raster position. This code gets around both issues.
   * http://www.mesa3d.org/brianp/sig97/gotchas.htm
   * 
   * @param y
   *          the Y-coordinate, which is flipped upside down in OpenGL
   */
  protected void setRasterPos(float x, float y) {
    // float z = 0;
    // float w = 1;
    //
    // float fx, fy;
    //
    // // Push current matrix mode and viewport attributes
    // gl.glPushAttrib(GL.GL_TRANSFORM_BIT | GL.GL_VIEWPORT_BIT);
    //
    // // Setup projection parameters
    // gl.glMatrixMode(GL.GL_PROJECTION);
    // gl.glPushMatrix();
    // gl.glLoadIdentity();
    // gl.glMatrixMode(GL.GL_MODELVIEW);
    // gl.glPushMatrix();
    // gl.glLoadIdentity();
    //
    // gl.glDepthRange(z, z);
    // gl.glViewport((int) x - 1, (int) y - 1, 2, 2);
    //
    // // set the raster (window) position
    // fx = x - (int) x;
    // fy = y - (int) y;
    // gl.glRasterPos4f(fx, fy, 0, w);
    //
    // // restore matrices, viewport and matrix mode
    // gl.glPopMatrix();
    // gl.glMatrixMode(GL.GL_PROJECTION);
    // gl.glPopMatrix();
    //
    // gl.glPopAttrib();
  }

  // ////////////////////////////////////////////////////////////

  // MASK

  public void mask(int alpha[]) {
    PGraphics.showMethodWarning("mask");
  }

  public void mask(PImage alpha) {
    PGraphics.showMethodWarning("mask");
  }

  // ////////////////////////////////////////////////////////////

  // FILTER

  /**
   * This is really inefficient and not a good idea in OpenGL. Use get() and
   * set() with a smaller image area, or call the filter on an image instead,
   * and then draw that.
   */
  public void filter(int kind) {
    PImage temp = get();
    temp.filter(kind);
    set(0, 0, temp);
  }

  /**
   * This is really inefficient and not a good idea in OpenGL. Use get() and
   * set() with a smaller image area, or call the filter on an image instead,
   * and then draw that.
   */
  public void filter(int kind, float param) {
    PImage temp = get();
    temp.filter(kind, param);
    set(0, 0, temp);
  }

  // ////////////////////////////////////////////////////////////

  /**
   * Extremely slow and not optimized, should use GL methods instead. Currently
   * calls a beginPixels() on the whole canvas, then does the copy, then it
   * calls endPixels().
   */
  // public void copy(int sx1, int sy1, int sx2, int sy2,
  // int dx1, int dy1, int dx2, int dy2)

  /**
   * TODO - extremely slow and not optimized. Currently calls a beginPixels() on
   * the whole canvas, then does the copy, then it calls endPixels().
   */
  // public void copy(PImage src,
  // int sx1, int sy1, int sx2, int sy2,
  // int dx1, int dy1, int dx2, int dy2)

  // ////////////////////////////////////////////////////////////

  // BLEND

  // static public int blendColor(int c1, int c2, int mode)

  // public void blend(PImage src,
  // int sx, int sy, int dx, int dy, int mode) {
  // set(dx, dy, PImage.blendColor(src.get(sx, sy), get(dx, dy), mode));
  // }

  /**
   * Extremely slow and not optimized, should use GL methods instead. Currently
   * calls a beginPixels() on the whole canvas, then does the copy, then it
   * calls endPixels(). Please help fix: <A
   * HREF="http://dev.processing.org/bugs/show_bug.cgi?id=941">Bug 941</A>, <A
   * HREF="http://dev.processing.org/bugs/show_bug.cgi?id=942">Bug 942</A>.
   */
  // public void blend(int sx1, int sy1, int sx2, int sy2,
  // int dx1, int dy1, int dx2, int dy2, int mode) {
  // loadPixels();
  // super.blend(sx1, sy1, sx2, sy2, dx1, dy1, dx2, dy2, mode);
  // updatePixels();
  // }

  // public void blend(PImage src,
  // int sx1, int sy1, int sx2, int sy2,
  // int dx1, int dy1, int dx2, int dy2, int mode) {
  // loadPixels();
  // super.blend(src, sx1, sy1, sx2, sy2, dx1, dy1, dx2, dy2, mode);
  // updatePixels();
  // }

  /**
   * Allows to set custom blend modes for the entire scene, using openGL.
   */
  public void blend(int mode) {
    blend = true;
    blendMode = mode;
    gl.glEnable(GL10.GL_BLEND);
    if (mode == BLEND)
      gl.glBlendFunc(GL10.GL_SRC_ALPHA, GL10.GL_ONE_MINUS_SRC_ALPHA);
    else if (mode == ADD)
      gl.glBlendFunc(GL10.GL_SRC_ALPHA, GL10.GL_ONE);
    else if (mode == MULTIPLY)
      gl.glBlendFunc(GL10.GL_DST_COLOR, GL10.GL_SRC_COLOR);
    else if (mode == SUBTRACT)
      gl.glBlendFunc(GL10.GL_ONE_MINUS_DST_COLOR, GL10.GL_ZERO);
    // TODO: implement all these other blending modes:
    // else if (blendMode == LIGHTEST)
    // else if (blendMode == DIFFERENCE)
    // else if (blendMode == EXCLUSION)
    // else if (blendMode == SCREEN)
    // else if (blendMode == OVERLAY)
    // else if (blendMode == HARD_LIGHT)
    // else if (blendMode == SOFT_LIGHT)
    // else if (blendMode == DODGE)
    // else if (blendMode == BURN)
  }

  public void noBlend() {
    blend = false;
    gl.glDisable(GL10.GL_BLEND);
  }
  
  // ////////////////////////////////////////////////////////////

  // SAVE

  // public void save(String filename) // PImage calls loadPixels()

  // ////////////////////////////////////////////////////////////

  // RENDERER

  A3DRenderer renderer;

  public Renderer getRenderer() {
    return renderer;
  }

  public class A3DRenderer implements Renderer {
    public A3DRenderer() {
    }

    public void onDrawFrame(GL10 igl) {
      gl = igl;

      try {
        gl11 = (GL11) gl;
      } catch (ClassCastException cce) {
        gl11 = null;
      }

      try {
        gl11x = (GL11Ext) gl;
      } catch (ClassCastException cce) {
        gl11x = null;
      }

      try {
        gl11xp = (GL11ExtensionPack) gl;
      } catch (ClassCastException cce) {
        gl11xp = null;
      }
      
      parent.handleDraw();

      gl = null;
      gl11 = null;
      gl11x = null;
      gl11xp = null;
    }

    public void onSurfaceChanged(GL10 igl, int iwidth, int iheight) {
      gl = igl;
      // PGL2JNILib.init(iwidth, iheight);

      try {
        gl11 = (GL11) gl;
      } catch (ClassCastException cce) {
        gl11 = null;
      }

      try {
        gl11x = (GL11Ext) gl;
      } catch (ClassCastException cce) {
        gl11x = null;
      }

      try {
        gl11xp = (GL11ExtensionPack) gl;
      } catch (ClassCastException cce) {
        gl11xp = null;
      }
      
      setSize(iwidth, iheight);
      gl = null;
      gl11 = null;
      gl11x = null;
      gl11xp = null;
    }

    public void onSurfaceCreated(GL10 igl, EGLConfig config) {
      gl = igl;

      try {
        gl11 = (GL11) gl;
      } catch (ClassCastException cce) {
        gl11 = null;
      }

      try {
        gl11x = (GL11Ext) gl;
      } catch (ClassCastException cce) {
        gl11x = null;
      }

      try {
        gl11xp = (GL11ExtensionPack) gl;
      } catch (ClassCastException cce) {
        gl11xp = null;
      }

      OPENGL_VENDOR = gl.glGetString(GL10.GL_VENDOR);
      OPENGL_RENDERER = gl.glGetString(GL10.GL_RENDERER);
      OPENGL_VERSION = gl.glGetString(GL10.GL_VERSION);

      npotTexSupported = false;
      mipmapSupported = false;
      matrixGetSupported = false;
      vboSupported = false;
      fboSupported = false;
      String extensions = gl.glGetString(GL10.GL_EXTENSIONS);
      if (-1 < extensions.indexOf("texture_non_power_of_two")) {
        npotTexSupported = true;
      }
      if (-1 < extensions.indexOf("generate_mipmap")) {
        mipmapSupported = true;
      }
      if (-1 < extensions.indexOf("matrix_get")) {
        matrixGetSupported = true;
      }
      if (-1 < extensions.indexOf("vertex_buffer_object")
          || -1 < OPENGL_VERSION.indexOf("1.1") || // Just in case
                                                   // vertex_buffer_object
                                                   // doesn't appear in the list
                                                   // of extensions,
          -1 < OPENGL_VERSION.indexOf("2.")) { // if the opengl version is
                                               // greater than 1.1, VBOs should
                                               // be supported.
        vboSupported = true;
      }
      if (-1 < extensions.indexOf("GL_OES_framebuffer_object")) {
        fboSupported = true;
      }

      int maxTexSize[] = new int[1];
      gl.glGetIntegerv(GL10.GL_MAX_TEXTURE_SIZE, maxTexSize, 0);
      maxTextureSize = maxTexSize[0];

      if (gl11 != null) {
        float[] maxPtSize = { 0.0f };
        gl11.glGetFloatv(GL11.GL_POINT_SIZE_MAX, maxPtSize, 0);
        maxPointSize = maxPtSize[0];
      }

      recreateResources();
      gl = null;
      gl11 = null;
      gl11x = null;
      gl11xp = null;
    }
  }

  // ////////////////////////////////////////////////////////////

  // Config chooser

  A3DConfigChooser configChooser;

  public GLSurfaceView.EGLConfigChooser getConfigChooser() {
    return configChooser;
  }

  public static class A3DConfigChooser implements EGLConfigChooser {
    // Subclasses can adjust these values:
    protected int mRedSize;
    protected int mGreenSize;
    protected int mBlueSize;
    protected int mAlphaSize;
    protected int mDepthSize;
    protected int mStencilSize;
    private int[] mValue = new int[1];

    /*
     * This EGL config specification is used to specify 2.0 rendering. We use a
     * minimum size of 4 bits for red/green/blue, but will perform actual
     * matching in chooseConfig() below.
     */
    private static int EGL_OPENGL_ES_BIT = 0x01; // EGL 1.x attribute value for
                                                 // GL_RENDERABLE_TYPE.
    private static int EGL_OPENGL_ES2_BIT = 0x04; // EGL 2.x attribute value for
                                                  // GL_RENDERABLE_TYPE.
    private static int[] configAttribsGL2 = { EGL10.EGL_RED_SIZE, 4,
        EGL10.EGL_GREEN_SIZE, 4, EGL10.EGL_BLUE_SIZE, 4,
        EGL10.EGL_RENDERABLE_TYPE, EGL_OPENGL_ES_BIT,
        //EGL10.EGL_RENDER_BUFFER, EGL10.EGL_SINGLE_BUFFER,
        //EGL10.EGL_RENDER_BUFFER, EGL10.EGL_BACK_BUFFER,
        EGL10.EGL_NONE };

    public A3DConfigChooser(int r, int g, int b, int a, int depth, int stencil) {
      mRedSize = r;
      mGreenSize = g;
      mBlueSize = b;
      mAlphaSize = a;
      mDepthSize = depth;
      mStencilSize = stencil;
    }

    public EGLConfig chooseConfig(EGL10 egl, EGLDisplay display) {

      if (EGL_CONTEXT == 2) {
        configAttribsGL2[7] = EGL_OPENGL_ES2_BIT;
      }

      /*
       * Get the number of minimally matching EGL configurations
       */
      int[] num_config = new int[1];
      egl.eglChooseConfig(display, configAttribsGL2, null, 0, num_config);

      int numConfigs = num_config[0];

      if (numConfigs <= 0) {
        throw new IllegalArgumentException("No EGL configs match configSpec");
      }

      /*
       * Allocate then read the array of minimally matching EGL configs
       */
      EGLConfig[] configs = new EGLConfig[numConfigs];
      egl.eglChooseConfig(display, configAttribsGL2, configs, numConfigs,
          num_config);

      for (EGLConfig config : configs) {
        String configStr = "A3D - selected EGL config : "
            + printConfig(egl, display, config);
        System.out.println(configStr);
      }

      /*
       * if (DEBUG) { printConfigs(egl, display, configs); }
       */

      /*
       * Now return the "best" one
       */
      return chooseConfig(egl, display, configs);
    }

    public EGLConfig chooseConfig(EGL10 egl, EGLDisplay display,
        EGLConfig[] configs) {
      for (EGLConfig config : configs) {
        int d = findConfigAttrib(egl, display, config, EGL10.EGL_DEPTH_SIZE, 0);
        int s = findConfigAttrib(egl, display, config, EGL10.EGL_STENCIL_SIZE,
            0);

        // We need at least mDepthSize and mStencilSize bits
        if (d < mDepthSize || s < mStencilSize)
          continue;

        // We want an *exact* match for red/green/blue/alpha
        int r = findConfigAttrib(egl, display, config, EGL10.EGL_RED_SIZE, 0);
        int g = findConfigAttrib(egl, display, config, EGL10.EGL_GREEN_SIZE, 0);
        int b = findConfigAttrib(egl, display, config, EGL10.EGL_BLUE_SIZE, 0);
        int a = findConfigAttrib(egl, display, config, EGL10.EGL_ALPHA_SIZE, 0);

        if (r == mRedSize && g == mGreenSize && b == mBlueSize
            && a == mAlphaSize) {
          String configStr = "A3D - selected EGL config : "
              + printConfig(egl, display, config);
          System.out.println(configStr);
          return config;
        }
      }
      return null;
    }

    /*
     * public EGLConfig chooseConfig(EGL10 egl, EGLDisplay display) {
     * 
     * // Specify a configuration for our opengl session // and grab the first
     * configuration that matches is int[] configSpecs = { //EGL10.EGL_RED_SIZE,
     * 8, //EGL10.EGL_GREEN_SIZE, 8, //EGL10.EGL_BLUE_SIZE, 8,
     * //EGL10.EGL_ALPHA_SIZE, 0, EGL10.EGL_DEPTH_SIZE, 16,
     * //EGL10.EGL_STENCIL_SIZE, 0, EGL10.EGL_NONE};
     * 
     * 
     * int[] num_config = new int[1]; egl.eglChooseConfig(display, configSpecs,
     * null, 0, num_config);
     * 
     * int numConfigs = num_config[0];
     * 
     * if (numConfigs <= 0) { throw new
     * IllegalArgumentException("No EGL configs match configSpec"); }
     * 
     * EGLConfig[] configs = new EGLConfig[numConfigs];
     * 
     * egl.eglChooseConfig(display, configSpecs, configs, numConfigs,
     * num_config); // best choice : select first config String configStr =
     * "A3D - selected EGL config : " + printConfig(egl, display, configs[0]);
     * System.out.println(configStr);
     * 
     * return configs[0]; }
     */

    private String printConfig(EGL10 egl, EGLDisplay display, EGLConfig config) {
      int r = findConfigAttrib(egl, display, config, EGL10.EGL_RED_SIZE, 0);
      int g = findConfigAttrib(egl, display, config, EGL10.EGL_GREEN_SIZE, 0);
      int b = findConfigAttrib(egl, display, config, EGL10.EGL_BLUE_SIZE, 0);
      int a = findConfigAttrib(egl, display, config, EGL10.EGL_ALPHA_SIZE, 0);
      int d = findConfigAttrib(egl, display, config, EGL10.EGL_DEPTH_SIZE, 0);
      int s = findConfigAttrib(egl, display, config, EGL10.EGL_STENCIL_SIZE, 0);
      int type = findConfigAttrib(egl, display, config, EGL10.EGL_RENDERABLE_TYPE, 0);
      int nat = findConfigAttrib(egl, display, config, EGL10.EGL_NATIVE_RENDERABLE, 0);
      int bufSize = findConfigAttrib(egl, display, config, EGL10.EGL_BUFFER_SIZE, 0);
      int bufSurf = findConfigAttrib(egl, display, config, EGL10.EGL_RENDER_BUFFER, 0);

      return String.format("EGLConfig rgba=%d%d%d%d depth=%d stencil=%d", r,g,b,a,d,s) 
        + " type=" + type 
        + " native=" + nat 
        + " buffer size=" + bufSize 
        + " buffer surface=" + bufSurf + 
        String.format(" caveat=0x%04x", findConfigAttrib(egl, display, config, EGL10.EGL_CONFIG_CAVEAT, 0));
    }

    private int findConfigAttrib(EGL10 egl, EGLDisplay display,
        EGLConfig config, int attribute, int defaultValue) {
      // int[] mValue = new int[1];
      if (egl.eglGetConfigAttrib(display, config, attribute, mValue)) {
        return mValue[0];
      }
      return defaultValue;
    }

  }

  // ////////////////////////////////////////////////////////////

  // Context factory

  A3DContextFactory contextFactory;

  public GLSurfaceView.EGLContextFactory getContextFactory() {
    return contextFactory;
  }

  public static class A3DContextFactory implements
      GLSurfaceView.EGLContextFactory {
    private static int EGL_CONTEXT_CLIENT_VERSION = 0x3098;

    public EGLContext createContext(EGL10 egl, EGLDisplay display,
        EGLConfig eglConfig) {
      // Log.w(TAG, "creating OpenGL ES 2.0 context");
      // checkEglError("Before eglCreateContext", egl);
      int[] attrib_list = { EGL_CONTEXT_CLIENT_VERSION, EGL_CONTEXT,
          EGL10.EGL_NONE };
      EGLContext context = egl.eglCreateContext(display, eglConfig,
          EGL10.EGL_NO_CONTEXT, attrib_list);
      // checkEglError("After eglCreateContext", egl);
      return context;
    }

    public void destroyContext(EGL10 egl, EGLDisplay display, EGLContext context) {
      egl.eglDestroyContext(display, context);
    }
  }
  
  // ////////////////////////////////////////////////////////////

  // INTERNAL MATH
    
  // bit shifting this might be more efficient
  private final int nextPowerOfTwo(int val) {
    int ret = 1;
    while (ret < val) {
      ret <<= 1;
    }
    return ret;
  }      
  
  private final float sqrt(float a) {
    return (float) Math.sqrt(a);
  }

  // private final float mag(float a, float b, float c) {
  // return (float) Math.sqrt(a*a + b*b + c*c);
  // }

  // private final float clamp(float a) {
  // return (a < 1) ? a : 1;
  // }

  private final float abs(float a) {
    return (a < 0) ? -a : a;
  }

  // private float dot(float ax, float ay, float az,
  // float bx, float by, float bz) {
  // return ax*bx + ay*by + az*bz;
  // }

  // private final void cross(float a0, float a1, float a2,
  // float b0, float b1, float b2,
  // PVector out) {
  // out.x = a1*b2 - a2*b1;
  // out.y = a2*b0 - a0*b2;
  // out.z = a0*b1 - a1*b0;
  // }

  protected class GLResource {
    Object object;
    Method method;

    GLResource(Object obj, Method meth) {
      object = obj;
      method = meth;
    }

  }
}

class BufferUtil {
  static IntBuffer newIntBuffer(int big) {
    IntBuffer buffer = IntBuffer.allocate(big);
    buffer.rewind();
    return buffer;
  }

  /**
   * Return true if this renderer supports 2D drawing. Defaults to true.
   */
  public boolean is2D() {
    return true;
  }

  /**
   * Return true if this renderer supports 2D drawing. Defaults to false.
   */
  public boolean is3D() {
    return true;
  }
}
