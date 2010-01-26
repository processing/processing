package processing.android.core;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;
import java.util.ArrayList;

import android.opengl.GLSurfaceView;
import android.opengl.GLU;
import android.opengl.GLSurfaceView.EGLConfigChooser;
import android.opengl.GLSurfaceView.Renderer;
import android.view.SurfaceHolder;

//import javax.microedition.khronos.egl.EGL;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.*;
import javax.microedition.khronos.egl.EGL10;
import javax.microedition.khronos.egl.EGLDisplay;

//import processing.android.opengl.Cube;

// drawPixels is missing...calls to glDrawPixels are commented out
//   setRasterPos() is also commented out
// remove the BufferUtil class at the end (verify the endian order, rewind, etc)

// other changes:
// mipmaps are disabled


public class PGraphicsAndroid3D extends PGraphics {
  public SurfaceHolder holder;
  
  public GL10 gl;
  public GLU glu;

  ////////////////////////////////////////////////////////////  
  
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

  /**
   * This is turned on at beginCamera, and off at endCamera
   * Currently we don't support nested begin/end cameras.
   */
  protected boolean manipulatingCamera;

  //////////////////////////////////////////////////////////////

  /**
   * Maximum lights by default is 8, the minimum defined by OpenGL.
   */
  public static final int MAX_LIGHTS = 8;

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

  /** Diffuse colors for lights.
   *  For an ambient light, this will hold the ambient color.
   *  Internally these are stored as numbers between 0 and 1. */
  public float[][] lightDiffuse;

  /** Specular colors for lights.
      Internally these are stored as numbers between 0 and 1. */
  public float[][] lightSpecular;

  /** Current specular color for lighting */
  public float[] currentLightSpecular;

  /** Current light falloff */
  public float currentLightFalloffConstant;
  public float currentLightFalloffLinear;
  public float currentLightFalloffQuadratic;
  
  /** Used to store empty values to be passed when a light has no ambient value **/
  public float[] zeroLight = { 0.0f, 0.0f, 0.0f, 0.0f };  
  
  //////////////////////////////////////////////////////////////  
  
  
  private IntBuffer vertexBuffer;
  private IntBuffer colorBuffer;
  private IntBuffer textureBuffer;
  private IntBuffer normalBuffer;
  
  private IntBuffer linesVertexBuffer;
  private IntBuffer linesColorBuffer;  

  /** Previous image being used as a texture */
  protected PImage textureImagePrev;  
  protected ArrayList<TexturedTriangleRange> texTriangleRanges;
  TexturedTriangleRange currentTexTriangleRange;  

  
  
    /**
   * Set to true if the host system is big endian (PowerPC, MIPS, SPARC),
   * false if little endian (x86 Intel for Mac or PC).
   */
  static public boolean BIG_ENDIAN =
    ByteOrder.nativeOrder() == ByteOrder.BIG_ENDIAN;
  
  
  // ........................................................

  // pos of first vertex of current shape in vertices array
  protected int shapeFirst;

  // i think vertex_end is actually the last vertex in the current shape
  // and is separate from vertexCount for occasions where drawing happens
  // on endDraw() with all the triangles being depth sorted
  protected int shapeLast;

  // vertices may be added during clipping against the near plane.
  protected int shapeLastPlusClipped;

  // used for sorting points when triangulating a polygon
  // warning - maximum number of vertices for a polygon is DEFAULT_VERTICES
  protected int vertexOrder[] = new int[DEFAULT_VERTICES];

  // ........................................................

  // This is done to keep track of start/stop information for lines in the
  // line array, so that lines can be shown as a single path, rather than just
  // individual segments. Currently only in use inside PGraphicsOpenGL.
  protected int pathCount;
  protected int[] pathOffset = new int[64];
  protected int[] pathLength = new int[64];

  // ........................................................

  // line & triangle fields (note that these overlap)
//  static protected final int INDEX = 0;          // shape index
  static protected final int VERTEX1 = 0;
  static protected final int VERTEX2 = 1;
  static protected final int VERTEX3 = 2;        // (triangles only)
  /** used to store the strokeColor int for efficient drawing. */
  static protected final int STROKE_COLOR = 1;   // (points only)
  static protected final int TEXTURE_INDEX = 3;  // (triangles only)
  //static protected final int STROKE_MODE = 2;    // (lines only)
  //static protected final int STROKE_WEIGHT = 3;  // (lines only)

  static protected final int POINT_FIELD_COUNT = 2;  //4
  static protected final int LINE_FIELD_COUNT = 2;  //4
  static protected final int TRIANGLE_FIELD_COUNT = 4;

  // points
  static final int DEFAULT_POINTS = 512;
  protected int[][] points = new int[DEFAULT_POINTS][POINT_FIELD_COUNT];
  protected int pointCount;

  // lines
  static final int DEFAULT_LINES = 512;
  public PLine line;  // used for drawing
  protected int[][] lines = new int[DEFAULT_LINES][LINE_FIELD_COUNT];
  protected int lineCount;

  protected int triangleCount;   // total number of triangles  
  
  /// Used to hold color values to be sent to OpenGL
  protected float[] colorFloats;
  
  /// IntBuffer to go with the pixels[] array
  protected IntBuffer pixelBuffer;  

  // The following variables to be deleted forever:
  
  // cheap picking someday
  //public int shape_index;

  // ........................................................

  //static final int DEFAULT_TEXTURES = 3;
  //protected PImage[] textures = new PImage[DEFAULT_TEXTURES];
  //int textureIndex;

/*
  static public final int TRI_DIFFUSE_R = 0;
  static public final int TRI_DIFFUSE_G = 1;
  static public final int TRI_DIFFUSE_B = 2;
  static public final int TRI_DIFFUSE_A = 3;
  static public final int TRI_SPECULAR_R = 4;
  static public final int TRI_SPECULAR_G = 5;
  static public final int TRI_SPECULAR_B = 6;
  static public final int TRI_COLOR_COUNT = 7;
  */  
  
  //  protected float[] projectionFloats;

  /// Buffer to hold light values before they're sent to OpenGL
  //protected FloatBuffer lightBuffer;
//  protected float[] lightArray = new float[] { 1, 1, 1 };
  
  //static int maxTextureSize;

//  int[] textureDeleteQueue = new int[10];
//  int textureDeleteQueueCount = 0;
  
  
  //////////////////////////////////////////////////////////////
  
  public PGraphicsAndroid3D() {
	  renderer = new A3DRenderer();
    glu = new GLU();  // or maybe not until used?
  }
    
  
  //public void setParent(PApplet parent)
  
  
  //public void setPrimary(boolean primary)
  
  
  //public void setPath(String path)
  
  
  public EGLConfigChooser getConfigChooser() {
    return configChooser;
  }
  
  
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
    cameraAspect = (float)width / (float)height;

    // Init transformation matrices.
    projection = new float[16];
    modelview = new float[16];
    modelviewInv = new float[16];
    camera = new float[16];
    cameraInv = new float[16];
    
    // Init lights.
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
    
    
    texTriangleRanges = new ArrayList<TexturedTriangleRange>();    
  }

  public void setSurfaceHolder(SurfaceHolder holder) {
    this.holder = holder;
  }
  
  
  protected void allocate() {
  }
  
  
  public void dispose() {
  }
  
  
  public void recreateResources() {
  }

  //////////////////////////////////////////////////////////////

  // FRAME


  public void requestDraw() {
	  ((GLSurfaceView) parent.surfaceView).requestRender();
  }

  
  /**
   * OpenGL cannot draw until a proper native peer is available, so this
   * returns the value of PApplet.isDisplayable() (inherited from Component).
   */
//  public boolean canDraw() {
//    return true;
//    //return parent.isDisplayable();
//  }


  public void beginDraw() {
    if (!settingsInited) defaultSettings();

    resetMatrix(); // reset model matrix.
  
    report("top beginDraw()");

    
    vertexBuffer.rewind();
    colorBuffer.rewind();
    textureBuffer.rewind();
    normalBuffer.rewind();

    textureImage = null;
    textureImagePrev = null;
        
    // these are necessary for alpha (i.e. fonts) to work
    gl.glEnable(GL10.GL_BLEND);
    gl.glBlendFunc(GL10.GL_SRC_ALPHA, GL10.GL_ONE_MINUS_SRC_ALPHA);

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
//    gl.glColorMaterial(GL10.GL_FRONT_AND_BACK, GL10.GL_AMBIENT_AND_DIFFUSE);
//    gl.glColorMaterial(GL10.GL_FRONT_AND_BACK, GL10.GL_SPECULAR);
    
    // these tend to make life easier
    // (but sometimes at the expense of a little speed)
    // Not using them right now because we're doing our own lighting.
    gl.glEnable(GL10.GL_NORMALIZE);
    //gl.glEnable(GL10.GL_AUTO_NORMAL); // I think this is OpenGL 1.2 only
    gl.glEnable(GL10.GL_RESCALE_NORMAL);
    //gl.GlLightModeli(GL10.GL_LIGHT_MODEL_COLOR_CONTROL, GL10.GL_SEPARATE_SPECULAR_COLOR);
    
    report("bot beginDraw()");
  }


  public void endDraw() {
    report("top endDraw()");
/*    
    if (hints[ENABLE_DEPTH_SORT]) {
      flush();
    }
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
  
  
  // TODO: put in the right place (but this requires working out combination with PShape first).
  public void model(GLModel model, float x, float y, float z) {
	  gl.glPushMatrix();
	  gl.glTranslatef(x, y, z);
	  model.render();
	  gl.glPopMatrix();
  }
 
   
  ////////////////////////////////////////////////////////////

  // SETTINGS

  //protected void checkSettings()
 
  
  protected void defaultSettings() {
    super.defaultSettings();

    manipulatingCamera = false;
    perspective();
    
    // easiest for beginners
    textureMode(IMAGE);   
  }
  
  
  // reapplySettings


  ////////////////////////////////////////////////////////////

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
 
  
  //////////////////////////////////////////////////////////////

  // VERTEX SHAPES

  // All picked up from either PGraphics or PGraphics3D


  //public void beginShape()
  
  public void beginShape(int kind) {
    shape = kind;
    vertexCount = 0;
    shapeFirst = 0;
    shapeLast = 0;   
    triangleCount = 0;
    lineCount = 0;      
      
    textureImagePrev = null;      
    texTriangleRanges.clear();
    /*
     TODO:
    if (hints[ENABLE_DEPTH_SORT]) {
      shapeFirst = vertexCount;
      shapeLast = 0;
    }
    */
  }
  
  //public void edge(boolean e)
  //public void normal(float nx, float ny, float nz)
  
  //public void textureMode(int mode)
  
  public void texture(PImage image) {
    //textureImagePrev =textureImage; 
    textureImage = image;
  }  
  
  static public int toFixed32(float x) {
    return (int) (x * 65536.0f);
  }
  
  static public int toFixed16(float x) {
    return (int) (x * 4096.0f);
  }
  
  protected void vertexCheck() {
    super.vertexCheck();

    int vertexAlloc = vertices.length;
    
 // Taking square because the buffers will contain repeated vertices... Need better estimation though.
    int triangleAlloc = 3 * vertexAlloc;;
    int lineAlloc = 2 * vertexAlloc;
    if (vertexBuffer == null || vertexBuffer.capacity() < triangleAlloc) {
      ByteBuffer vbb = ByteBuffer.allocateDirect(triangleAlloc * 3);
      vbb.order(ByteOrder.nativeOrder());
      vertexBuffer = vbb.asIntBuffer();
//      vertexBuffer.put(vertices);
//      vertexBuffer.position(0);

      ByteBuffer cbb = ByteBuffer.allocateDirect(triangleAlloc * 4);
      cbb.order(ByteOrder.nativeOrder());
      colorBuffer = cbb.asIntBuffer();
//      mColorBuffer.put(colors);
//      mColorBuffer.position(0);

      ByteBuffer tbb = ByteBuffer.allocateDirect(triangleAlloc * 2);
      tbb.order(ByteOrder.nativeOrder());
      textureBuffer = tbb.asIntBuffer();
      
      ByteBuffer nbb = ByteBuffer.allocateDirect(triangleAlloc * 3);
      nbb.order(ByteOrder.nativeOrder());
      normalBuffer = nbb.asIntBuffer();      
    }
    
    if (linesVertexBuffer == null || linesVertexBuffer.capacity() < lineAlloc) {
        ByteBuffer vbb = ByteBuffer.allocateDirect(lineAlloc * 3);
        vbb.order(ByteOrder.nativeOrder());
        linesVertexBuffer = vbb.asIntBuffer();
//        vertexBuffer.put(vertices);
//        vertexBuffer.position(0);

        ByteBuffer cbb = ByteBuffer.allocateDirect(lineAlloc * 4);
        cbb.order(ByteOrder.nativeOrder());
        linesColorBuffer = cbb.asIntBuffer();
    }    
    
  }

  
  //public void vertex(float x, float y)
  //public void vertex(float x, float y, float z)
  //public void vertex(float x, float y, float u, float v)
  //public void vertex(float x, float y, float z, float u, float v)
  //protected void vertexTexture(float u, float v);
  //public void breakShape()
  
  //public void endShape()
  
  public void endShape(int mode) {
    shapeLast = vertexCount;
    shapeLastPlusClipped = shapeLast;

    // don't try to draw if there are no vertices
    // (fixes a bug in LINE_LOOP that re-adds a nonexistent vertex)
    if (vertexCount == 0) {
      shape = 0;
      return;
    }
    
    //shapeFirst 
    //shapeLast
    
    if (stroke) {
      // not ready yet.
      endShapeStroke(mode);
    }

    if (fill) {
      endShapeFill();
    }
      
    if (fill) renderTriangles(0, triangleCount);
    if (stroke) {
      renderLines(0, lineCount);
        pathCount = 0;
    }

    /*
     TODO:
    TO CHECK LATER: 
    // render shape and fill here if not saving the shapes for later
    // if true, the shapes will be rendered on endDraw
    if (!hints[ENABLE_DEPTH_SORT]) {
      if (fill) {
        renderTriangles(0, triangleCount);
        if (raw != null) {
          //rawTriangles(0, triangleCount);
        }
        triangleCount = 0;
      }
      if (stroke) {
        renderLines(0, lineCount);
        if (raw != null) {
          //rawLines(0, lineCount);
        }
        lineCount = 0;
      }
      pathCount = 0;
    }    
    */
    
    shape = 0;
  }
  
  
  protected void endShapeStroke(int mode) {
      switch (shape) {
        case POINTS:
        {
          int stop = shapeLast;
          for (int i = shapeFirst; i < stop; i++) {
            addLineBreak();  // total overkill for points
            addLine(i, i);
          }
        }
        break;

        case LINES:
        {
          // store index of first vertex
          int first = lineCount;
          int stop = shapeLast - 1;
          //increment = (shape == LINES) ? 2 : 1;

          // for LINE_STRIP and LINE_LOOP, make this all one path
          if (shape != LINES) addLineBreak();

          for (int i = shapeFirst; i < stop; i += 2) {
            // for LINES, make a new path for each segment
            if (shape == LINES) addLineBreak();
            addLine(i, i+1);
          }

          // for LINE_LOOP, close the loop with a final segment
          //if (shape == LINE_LOOP) {
          if (mode == CLOSE) {
            addLine(stop, lines[first][VERTEX1]);
          }
        }
        break;

        case TRIANGLES:
        {
          for (int i = shapeFirst; i < shapeLast-2; i += 3) {
            addLineBreak();
            //counter = i - vertex_start;
            addLine(i+0, i+1);
            addLine(i+1, i+2);
            addLine(i+2, i+0);
          }
        }
        break;

        case TRIANGLE_STRIP:
        {
          // first draw all vertices as a line strip
          int stop = shapeLast-1;

          addLineBreak();
          for (int i = shapeFirst; i < stop; i++) {
            //counter = i - vertex_start;
            addLine(i, i+1);
          }

          // then draw from vertex (n) to (n+2)
          stop = shapeLast-2;
          for (int i = shapeFirst; i < stop; i++) {
            addLineBreak();
            addLine(i, i+2);
          }
        }
        break;

        case TRIANGLE_FAN:
        {
          // this just draws a series of line segments
          // from the center to each exterior point
          for (int i = shapeFirst + 1; i < shapeLast; i++) {
            addLineBreak();
            addLine(shapeFirst, i);
          }

          // then a single line loop around the outside.
          addLineBreak();
          for (int i = shapeFirst + 1; i < shapeLast-1; i++) {
            addLine(i, i+1);
          }
          // closing the loop
          addLine(shapeLast-1, shapeFirst + 1);
        }
        break;

        case QUADS:
        {
          for (int i = shapeFirst; i < shapeLast; i += 4) {
            addLineBreak();
            //counter = i - vertex_start;
            addLine(i+0, i+1);
            addLine(i+1, i+2);
            addLine(i+2, i+3);
            addLine(i+3, i+0);
          }
        }
        break;

        case QUAD_STRIP:
        {
          for (int i = shapeFirst; i < shapeLast - 3; i += 2) {
            addLineBreak();
            addLine(i+0, i+2);
            addLine(i+2, i+3);
            addLine(i+3, i+1);
            addLine(i+1, i+0);
          }
        }
        break;

        case POLYGON:
        {
          // store index of first vertex
          int stop = shapeLast - 1;

          addLineBreak();
          for (int i = shapeFirst; i < stop; i++) {
            addLine(i, i+1);
          }
          if (mode == CLOSE) {
            // draw the last line connecting back to the first point in poly
            addLine(stop, shapeFirst); //lines[first][VERTEX1]);
          }
        }
        break;
      }
    }
  

  protected void endShapeFill() {
    switch (shape) {
    case TRIANGLE_FAN: 
    {
       int stop = shapeLast - 1;
       for (int i = shapeFirst + 1; i < stop; i++) {
         addTriangle(shapeFirst, i, i+1);
       }
    }
    break;

    case TRIANGLES:
    {
      int stop = shapeLast - 2;
      for (int i = shapeFirst; i < stop; i += 3) {
        // have to switch between clockwise/counter-clockwise
        // otherwise the feller is backwards and renderer won't draw
        if ((i % 2) == 0) {
          addTriangle(i, i+2, i+1);
        } else {
          addTriangle(i, i+1, i+2);
        }
      }
    }
    break;

    case TRIANGLE_STRIP:
    {
      int stop = shapeLast - 2;
      for (int i = shapeFirst; i < stop; i++) {
        // have to switch between clockwise/counter-clockwise
        // otherwise the feller is backwards and renderer won't draw
        if ((i % 2) == 0) {
          addTriangle(i, i+2, i+1);
        } else {
          addTriangle(i, i+1, i+2);
        }
      }
    }
    break;

    case QUADS:
    {
      int stop = vertexCount-3;
      for (int i = shapeFirst; i < stop; i += 4) {
        // first triangle
        addTriangle(i, i+1, i+2);
        // second triangle
        addTriangle(i, i+2, i+3);
      }
    }
    break;

    case QUAD_STRIP:
    {
      int stop = vertexCount-3;
      for (int i = shapeFirst; i < stop; i += 2) {
        // first triangle
        addTriangle(i+0, i+2, i+1);
        // second triangle
        addTriangle(i+2, i+3, i+1);
      }
    }
    break;

    case POLYGON:
    {
      addPolygonTriangles();
    }
    break;
  }
      
    if (currentTexTriangleRange != null) {
      texTriangleRanges.add(currentTexTriangleRange);
      currentTexTriangleRange = null;
    }
    if (texTriangleRanges.size() == 0) {
      texTriangleRanges.add(new TexturedTriangleRange(0, triangleCount, null));
    }
  }


  //////////////////////////////////////////////////////////////

  // BEZIER CURVE VERTICES

  // All picked up from either PGraphics or PGraphics3D, however
  // a faster version that made use of OpenGL's evaluator methods
  // would be a nice improvement.


  //protected void bezierVertexCheck();
  //public void bezierVertex(float x2, float y2,
  //                         float x3, float y3,
  //                         float x4, float y4)
  //public void bezierVertex(float x2, float y2, float z2,
  //                         float x3, float y3, float z3,
  //                         float x4, float y4, float z4)



  //////////////////////////////////////////////////////////////

  // CATMULL-ROM CURVE VERTICES

  // Like bezier, these could be implemented using an OpenGL evaluator.


  //protected void curveVertexCheck();
  //public void curveVertex(float x, float y)
  //public void curveVertex(float x, float y, float z)
  //protected void curveVertexSegment(float x1, float y1,
  //                                  float x2, float y2,
  //                                  float x3, float y3,
  //                                  float x4, float y4)
  //protected void curveVertexSegment(float x1, float y1, float z1,
  //                                  float x2, float y2, float z2,
  //                                  float x3, float y3, float z3,
  //                                  float x4, float y4, float z4)



  //////////////////////////////////////////////////////////////

  // POINTS (override from P3D)


  // Buffers to be passed to gl*Pointer() functions
  // must be direct, i.e., they must be placed on the
  // native heap where the garbage collector cannot
  // move them.
  //
  // Buffers with multi-byte datatypes (e.g., short, int, float)
  // must have their byte order set to native order

//  ByteBuffer vbb = ByteBuffer.allocateDirect(vertices.length*4);
//  vbb.order(ByteOrder.nativeOrder());
//  mVertexBuffer = vbb.asIntBuffer();
//  mVertexBuffer.put(vertices);
//  mVertexBuffer.position(0);
//
//  ByteBuffer cbb = ByteBuffer.allocateDirect(colors.length*4);
//  cbb.order(ByteOrder.nativeOrder());
//  mColorBuffer = cbb.asIntBuffer();
//  mColorBuffer.put(colors);
//  mColorBuffer.position(0);
//
//  mIndexBuffer = ByteBuffer.allocateDirect(indices.length);
//  mIndexBuffer.put(indices);
//  mIndexBuffer.position(0);
  
//  gl.glFrontFace(gl.GL_CW);
//  gl.glVertexPointer(3, gl.GL_FIXED, 0, mVertexBuffer);
//  gl.glColorPointer(4, gl.GL_FIXED, 0, mColorBuffer);
//  gl.glDrawElements(gl.GL_TRIANGLES, 36, gl.GL_UNSIGNED_BYTE, mIndexBuffer);

  
  
  protected void renderPoints(int start, int stop) {
    gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);
    gl.glEnableClientState(GL10.GL_COLOR_ARRAY);

    vertexBuffer.rewind();
    colorBuffer.rewind();
    
    float sw = vertices[lines[start][VERTEX1]][SW];
    if (sw > 0) {
      gl.glPointSize(sw);  // can only be set outside glBegin/glEnd
//      gl.glBegin(GL10.GL_POINTS);
      for (int i = start; i < stop; i++) {
//        gl.glColor4f(a[SR], a[SG], a[SB], a[SA]);
        float[] a = vertices[points[i][VERTEX1]];
        colorBuffer.put(toFixed16(a[SR]));
        colorBuffer.put(toFixed16(a[SG]));
        colorBuffer.put(toFixed16(a[SB]));
        colorBuffer.put(toFixed16(a[SA]));
//        gl.glVertex3f(a[VX], a[VY], a[VZ]);
        vertexBuffer.put(toFixed32(a[VX]));
        vertexBuffer.put(toFixed32(a[VY]));
        vertexBuffer.put(toFixed32(a[VZ]));
      }
//      gl.glEnd();
      gl.glVertexPointer(3, GL10.GL_FIXED, 0, vertexBuffer);
      gl.glColorPointer(4, GL10.GL_FIXED, 0, colorBuffer);
      gl.glDrawArrays(GL10.GL_POINTS, start, stop - start);
    }

    gl.glDisableClientState(GL10.GL_VERTEX_ARRAY);
    gl.glDisableClientState(GL10.GL_COLOR_ARRAY);
  }


  //protected void rawPoints(int start, int stop)  // PGraphics3D



  //////////////////////////////////////////////////////////////

  // LINES (override from P3D)


  //protected final void addLineBreak()  // PGraphics3D

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
      int temp[][] = new int[lineCount<<1][LINE_FIELD_COUNT];
      System.arraycopy(lines, 0, temp, 0, lineCount);
      lines = temp;
    }
    lines[lineCount][VERTEX1] = a;
    lines[lineCount][VERTEX2] = b;

    //lines[lineCount][STROKE_MODE] = strokeCap | strokeJoin;
    //lines[lineCount][STROKE_WEIGHT] = (int) (strokeWeight + 0.5f); // hmm
    lineCount++;

    // mark this piece as being part of the current path
    pathLength[pathCount-1]++;
  }


  /**
   * In the current implementation, start and stop are ignored (in OpenGL).
   * This will obviously have to be revisited if/when proper depth sorting
   * is implemented.
   */
  protected void renderLines(int start, int stop) {
    report("render_lines in");

    // Last transformation: inversion of coordinate to make comaptible with Processing's inverted Y axis.
    gl.glPushMatrix();
    gl.glScalef(1, -1, 1);
    
    
    gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);
    gl.glEnableClientState(GL10.GL_COLOR_ARRAY);

    //int i = 0;
    for (int j = 0; j < pathCount; j++) {
      int i = pathOffset[j];
      float sw = vertices[lines[i][VERTEX1]][SW];
      //report("render_lines 1");
      // stroke weight zero will cause a gl error
      if (sw > 0) {
        // glLineWidth has to occur outside glBegin/glEnd
        gl.glLineWidth(sw);
//        gl.glBegin(GL10.GL_LINE_STRIP);
        linesVertexBuffer.rewind();
        linesColorBuffer.rewind();

        // always draw a first point
        float a[] = vertices[lines[i][VERTEX1]];
//        gl.glColor4f(a[SR], a[SG], a[SB], a[SA]);
        linesColorBuffer.put(toFixed32(a[SR]));
        linesColorBuffer.put(toFixed32(a[SG]));
        linesColorBuffer.put(toFixed32(a[SB]));
        linesColorBuffer.put(toFixed32(a[SA]));
//        gl.glVertex3f(a[VX], a[VY], a[VZ]);
        linesVertexBuffer.put(toFixed32(a[X]));
        linesVertexBuffer.put(toFixed32(a[Y]));
        linesVertexBuffer.put(toFixed32(a[Z]));

        // on this and subsequent lines, only draw the second point
        //System.out.println(pathLength[j]);
        for (int k = 0; k < pathLength[j]; k++) {
          float b[] = vertices[lines[i][VERTEX2]];
//          gl.glColor4f(b[SR], b[SG], b[SB], b[SA]);
          linesColorBuffer.put(toFixed32(b[SR]));
          linesColorBuffer.put(toFixed32(b[SG]));
          linesColorBuffer.put(toFixed32(b[SB]));
          linesColorBuffer.put(toFixed32(b[SA]));
          
          //gl.glEdgeFlag(a[EDGE] == 1);
          
//          gl.glVertex3f(b[VX], b[VY], b[VZ]);
          linesVertexBuffer.put(toFixed32(b[X]));
          linesVertexBuffer.put(toFixed32(b[Y]));
          linesVertexBuffer.put(toFixed32(b[Z]));
          i++;
        }
//        gl.glEnd();
        
        linesVertexBuffer.position(0);
        linesColorBuffer.position(0);
        
        gl.glVertexPointer(3, GL10.GL_FIXED, 0, linesVertexBuffer);
        gl.glColorPointer(4, GL10.GL_FIXED, 0, linesColorBuffer);
        gl.glDrawArrays(GL10.GL_LINE_STRIP, 0, pathLength[j] + 1);        
      }
    }
    
    gl.glPopMatrix();
    
    report("render_lines out");
  }


  //protected void rawLines(int start, int stop)



  //////////////////////////////////////////////////////////////

  // TRIANGLES
  

  /**
   * Add the triangle.
   */
  protected void addTriangle(int a, int b, int c) {
    float[] vertexa = vertices[a];
    float[] vertexb = vertices[b];
    float[] vertexc = vertices[c];
    
    // Need to think about this more:
    float uscale = 1.0f;
    float vscale = 1.0f;
    float cx = 0.0f;
    float sx = +1.0f;
    float cy = 0.0f;
    float sy = +1.0f;    
    if (textureImage != null)
      if (textureImage instanceof GLTexture) {
        GLTexture tex = (GLTexture)textureImage;
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
      else { 
        throw new RuntimeException("A3D only accepts GLTextures for texturing!");    
      }
    
    // Vertex A.
    vertexBuffer.put(toFixed32(vertexa[X]));
    vertexBuffer.put(toFixed32(vertexa[Y]));
    vertexBuffer.put(toFixed32(vertexa[Z]));
    colorBuffer.put(toFixed32(vertexa[R]));
    colorBuffer.put(toFixed32(vertexa[G]));
    colorBuffer.put(toFixed32(vertexa[B]));
    colorBuffer.put(toFixed32(vertexa[A]));
    normalBuffer.put(toFixed32(vertexa[NX]));
    normalBuffer.put(toFixed32(vertexa[NY]));
    normalBuffer.put(toFixed32(vertexa[NZ]));    
    textureBuffer.put(toFixed32((cx +  sx * vertexa[U]) * uscale));
    textureBuffer.put(toFixed32((cy +  sy * vertexa[V]) * vscale));
    
    // Vertex B.    
    vertexBuffer.put(toFixed32(vertexb[X]));
    vertexBuffer.put(toFixed32(vertexb[Y]));
    vertexBuffer.put(toFixed32(vertexb[Z]));
    colorBuffer.put(toFixed32(vertexb[R]));
    colorBuffer.put(toFixed32(vertexb[G]));
    colorBuffer.put(toFixed32(vertexb[B]));
    colorBuffer.put(toFixed32(vertexb[A]));
    normalBuffer.put(toFixed32(vertexb[NX]));
    normalBuffer.put(toFixed32(vertexb[NY]));
    normalBuffer.put(toFixed32(vertexb[NZ]));    
    textureBuffer.put(toFixed32((cx +  sx * vertexb[U]) * uscale));
    textureBuffer.put(toFixed32((cy +  sy * vertexb[V]) * vscale));    
    
    // Vertex C.    
    vertexBuffer.put(toFixed32(vertexc[X]));
    vertexBuffer.put(toFixed32(vertexc[Y]));
    vertexBuffer.put(toFixed32(vertexc[Z]));
    colorBuffer.put(toFixed32(vertexc[R]));
    colorBuffer.put(toFixed32(vertexc[G]));
    colorBuffer.put(toFixed32(vertexc[B]));
    colorBuffer.put(toFixed32(vertexc[A]));
    normalBuffer.put(toFixed32(vertexc[NX]));
    normalBuffer.put(toFixed32(vertexc[NY]));
    normalBuffer.put(toFixed32(vertexc[NZ]));
    textureBuffer.put(toFixed32((cx +  sx * vertexc[U]) * uscale));
    textureBuffer.put(toFixed32((cy +  sy * vertexc[V]) * vscale));    

    triangleCount++;
    
    // Updating the texture assigned to this triangle.
    if (textureImage != textureImagePrev)
    {
        // Add current textured triangle range, if is not null.	
    	if (currentTexTriangleRange != null) {
            texTriangleRanges.add(currentTexTriangleRange);
    	}
    	currentTexTriangleRange = new TexturedTriangleRange(triangleCount - 1,triangleCount, textureImage);
    }
    else if (textureImage != null) {
    	currentTexTriangleRange.lastTriangle = triangleCount;    	
    }
    
    textureImagePrev = textureImage;
  }

  protected void renderTriangles(int start, int stop) {
    report("render_triangles in");

    GLTexture tex = null;
    boolean texturing = false;
    
    vertexBuffer.position(0);
    colorBuffer.position(0);
    normalBuffer.position(0);
    textureBuffer.position(0);

    // Last transformation: inversion of coordinate to make comaptible with Processing's inverted Y axis.
    gl.glPushMatrix();
    gl.glScalef(1, -1, 1);
    
    gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);
    gl.glEnableClientState(GL10.GL_COLOR_ARRAY);
    gl.glEnableClientState(GL10.GL_NORMAL_ARRAY);
        
    TexturedTriangleRange texRange;
    for (int i = 0; i < texTriangleRanges.size(); i++) {
      texRange = (TexturedTriangleRange)texTriangleRanges.get(i);
      
      if (texRange.textureImage != null && textureImage instanceof GLTexture) {
        tex = (GLTexture)textureImage;
        
        gl.glEnable(tex.getTextureTarget());
        gl.glBindTexture(tex.getTextureTarget(), tex.getTextureID());        
    	  
        gl.glEnableClientState(GL10.GL_TEXTURE_COORD_ARRAY);
        texturing = true;
      }
      else texturing = false;
    	
      gl.glVertexPointer(3, GL10.GL_FIXED, 0, vertexBuffer);
      gl.glColorPointer(4, GL10.GL_FIXED, 0, colorBuffer);
      gl.glNormalPointer(GL10.GL_FIXED, 0, normalBuffer);
      if (texturing) gl.glTexCoordPointer(2, GL10.GL_FIXED, 0, textureBuffer);
      gl.glDrawArrays(GL10.GL_TRIANGLES, 3 * texRange.firstTriangle, 3 * (texRange.lastTriangle - texRange.firstTriangle));
      
      if (texturing) {
    	  gl.glDisable(tex.getTextureTarget());;
          gl.glDisableClientState(GL10.GL_TEXTURE_COORD_ARRAY);  	
      }      
    }
    //gl.glDrawArrays(GL10.GL_TRIANGLES, 3 * start, 3 * (stop - start));
        
    gl.glDisableClientState(GL10.GL_NORMAL_ARRAY);
    gl.glDisableClientState(GL10.GL_COLOR_ARRAY);    
    gl.glDisableClientState(GL10.GL_VERTEX_ARRAY);
   
    gl.glPopMatrix();
    
    report("render_triangles out");	    
  }
  

  //protected void rawTriangles(int start, int stop)  // PGraphics3D


  protected class TexturedTriangleRange {
    int firstTriangle; 
    int lastTriangle;
    PImage textureImage;
    
    TexturedTriangleRange(int first, int last, PImage img) {
      firstTriangle = first; 
      lastTriangle = last;
      textureImage = img;
    }
  }
	

  /**
   * Triangulate the current polygon.
   * <BR> <BR>
   * Simple ear clipping polygon triangulation adapted from code by
   * John W. Ratcliff (jratcliff at verant.com). Presumably
   * <A HREF="http://www.flipcode.org/cgi-bin/fcarticles.cgi?show=63943">this</A>
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
    // trying to track down bug #774
    for (int i = vertex_start; i < vertex_end; i++) {
      if (i > vertex_start) {
        if (vertices[i-1][MX] == vertices[i][MX] &&
            vertices[i-1][MY] == vertices[i][MY]) {
          System.out.print("**** " );
        }
      }
      System.out.println(i + " " + vertices[i][MX] + " " + vertices[i][MY]);
    }
    System.out.println();
    */

    // first we check if the polygon goes clockwise or counterclockwise
    float area = 0;
    for (int p = shapeLast - 1, q = shapeFirst; q < shapeLast; p = q++) {
      area += (vertices[q][d1] * vertices[p][d2] -
               vertices[p][d1] * vertices[q][d2]);
    }
    // rather than checking for the perpendicular case first, only do it
    // when the area calculates to zero. checking for perpendicular would be
    // a needless waste of time for the 99% case.
    if (area == 0) {
      // figure out which dimension is the perpendicular axis
      boolean foundValidX = false;
      boolean foundValidY = false;

      for (int i = shapeFirst; i < shapeLast; i++) {
        for (int j = i; j < shapeLast; j++){
          if ( vertices[i][X] != vertices[j][X] ) foundValidX = true;
          if ( vertices[i][Y] != vertices[j][Y] ) foundValidY = true;
        }
      }

      if (foundValidX) {
        //d1 = MX;  // already the case
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
        area += (vertices[q][d1] * vertices[p][d2] -
                 vertices[p][d1] * vertices[q][d2]);
      }
    }

    // don't allow polygons to come back and meet themselves,
    // otherwise it will anger the triangulator
    // http://dev.processing.org/bugs/show_bug.cgi?id=97
    float vfirst[] = vertices[shapeFirst];
    float vlast[] = vertices[shapeLast-1];
    if ((abs(vfirst[X] - vlast[X]) < EPSILON) &&
        (abs(vfirst[Y] - vlast[Y]) < EPSILON) &&
        (abs(vfirst[Z] - vlast[Z]) < EPSILON)) {
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
    int count = 2*vc;  // complex polygon detection

    for (int m = 0, v = vc - 1; vc > 2; ) {
      boolean snip = true;

      // if we start over again, is a complex polygon
      if (0 >= (count--)) {
        break; // triangulation failed
      }

      // get 3 consecutive vertices <u,v,w>
      int u = v ; if (vc <= u) u = 0;    // previous
      v = u + 1; if (vc <= v) v = 0;     // current
      int w = v + 1; if (vc <= w) w = 0; // next

      // Upgrade values to doubles, and multiply by 10 so that we can have
      // some better accuracy as we tessellate. This seems to have negligible
      // speed differences on Windows and Intel Macs, but causes a 50% speed
      // drop for PPC Macs with the bug's example code that draws ~200 points
      // in a concave polygon. Apple has abandoned PPC so we may as well too.
      // http://dev.processing.org/bugs/show_bug.cgi?id=774

      // triangle A B C
      double Ax = -10 * vertices[vertexOrder[u]][d1];
      double Ay =  10 * vertices[vertexOrder[u]][d2];
      double Bx = -10 * vertices[vertexOrder[v]][d1];
      double By =  10 * vertices[vertexOrder[v]][d2];
      double Cx = -10 * vertices[vertexOrder[w]][d1];
      double Cy =  10 * vertices[vertexOrder[w]][d2];

      // first we check if <u,v,w> continues going ccw
      if (EPSILON > (((Bx-Ax) * (Cy-Ay)) - ((By-Ay) * (Cx-Ax)))) {
        continue;
      }

      for (int p = 0; p < vc; p++) {
        if ((p == u) || (p == v) || (p == w)) {
          continue;
        }

        double Px = -10 * vertices[vertexOrder[p]][d1];
        double Py =  10 * vertices[vertexOrder[p]][d2];

        double ax  = Cx - Bx;  double ay  = Cy - By;
        double bx  = Ax - Cx;  double by  = Ay - Cy;
        double cx  = Bx - Ax;  double cy  = By - Ay;
        double apx = Px - Ax;  double apy = Py - Ay;
        double bpx = Px - Bx;  double bpy = Py - By;
        double cpx = Px - Cx;  double cpy = Py - Cy;

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
  
  
  //////////////////////////////////////////////////////////////

  // RENDERING


  //public void flush()


  //protected void render()


  //protected void sort()



  //////////////////////////////////////////////////////////////

  // POINT, LINE, TRIANGLE, QUAD

  // Because vertex(x, y) is mapped to vertex(x, y, 0), none of these commands
  // need to be overridden from their default implementation in PGraphics.


  //public void point(float x, float y)


  //public void point(float x, float y, float z)


  //public void line(float x1, float y1, float x2, float y2)


  //public void line(float x1, float y1, float z1,
  //                 float x2, float y2, float z2)


  //public void triangle(float x1, float y1, float x2, float y2,
  //                     float x3, float y3)


  //public void quad(float x1, float y1, float x2, float y2,
  //                 float x3, float y3, float x4, float y4)



  //////////////////////////////////////////////////////////////

  // RECT


  //public void rectMode(int mode)


  //public void rect(float a, float b, float c, float d)


  //protected void rectImpl(float x1, float y1, float x2, float y2)



  //////////////////////////////////////////////////////////////

  // ELLIPSE


  //public void ellipseMode(int mode)

  protected void ellipseImpl(float x, float y, float w, float h) {
	    float radiusH = w / 2;
	    float radiusV = h / 2;

	    float centerX = x + radiusH;
	    float centerY = y + radiusV;

	    float sx1 = screenX(x, y);
	    float sy1 = screenY(x, y);
	    float sx2 = screenX(x+w, y+h);
	    float sy2 = screenY(x+w, y+h);

	    if (fill) {
	      int accuracy = (int) (TWO_PI * PApplet.dist(sx1, sy1, sx2, sy2) / 20);
	      if (accuracy < 6) accuracy = 6;

	      float inc = (float)SINCOS_LENGTH / accuracy;
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
	        vertex(centerX + cosLUT[(int) val] * radiusH,
	               centerY + sinLUT[(int) val] * radiusV);
	        val = (val + inc) % SINCOS_LENGTH;
	      }
	      // back to the beginning
	      vertex(centerX + cosLUT[0] * radiusH,
	             centerY + sinLUT[0] * radiusV);
	      endShape();

	      stroke = strokeSaved;
	      smooth = smoothSaved;
	    }

	    if (stroke) {
	      int accuracy = (int) (TWO_PI * PApplet.dist(sx1, sy1, sx2, sy2) / 8);
	      if (accuracy < 6) accuracy = 6;

	      float inc = (float)SINCOS_LENGTH / accuracy;
	      float val = 0;

	      boolean savedFill = fill;
	      fill = false;

	      val = 0;
	      beginShape();
	      for (int i = 0; i < accuracy; i++) {
	        vertex(centerX + cosLUT[(int) val] * radiusH,
	               centerY + sinLUT[(int) val] * radiusV);
	        val = (val + inc) % SINCOS_LENGTH;
	      }
	      endShape(CLOSE);

	      fill = savedFill;
	    }
	  }
  
  
  //public void ellipse(float a, float b, float c, float d)


  //public void arc(float a, float b, float c, float d,
  //                float start, float stop)


  //protected void arcImpl(float x, float y, float w, float h,
  //                       float start, float stop)


  //////////////////////////////////////////////////////////////

  // BOX

  // TODO P3D overrides box to turn on triangle culling, but that's a waste
  // for OpenGL10. Also could just use the cube method from GL or GLUT.


  //public void box(float size)


  //public void box(float w, float h, float d)  // P3D



  //////////////////////////////////////////////////////////////

  // SPHERE

  // TODO P3D overrides sphere to turn on triangle culling, but that's a waste
  // for OpenGL10. Also could just use the cube method from GL or GLUT.


  //public void sphereDetail(int res)


  //public void sphereDetail(int ures, int vres)


  //public void sphere(float r)



  //////////////////////////////////////////////////////////////

  // BEZIER


  //public float bezierPoint(float a, float b, float c, float d, float t)


  //public float bezierTangent(float a, float b, float c, float d, float t)


  //public void bezierDetail(int detail)


  //public void bezier(float x1, float y1,
  //                   float x2, float y2,
  //                   float x3, float y3,
  //                   float x4, float y4)


  //public void bezier(float x1, float y1, float z1,
  //                   float x2, float y2, float z2,
  //                   float x3, float y3, float z3,
  //                   float x4, float y4, float z4)



  //////////////////////////////////////////////////////////////

  // CATMULL-ROM CURVES


  //public float curvePoint(float a, float b, float c, float d, float t)


  //public float curveTangent(float a, float b, float c, float d, float t)


  //public void curveDetail(int detail)


  //public void curveTightness(float tightness)


  //public void curve(float x1, float y1,
  //                  float x2, float y2,
  //                  float x3, float y3,
  //                  float x4, float y4)


  //public void curve(float x1, float y1, float z1,
  //                  float x2, float y2, float z2,
  //                  float x3, float y3, float z3,
  //                  float x4, float y4, float z4)



  //////////////////////////////////////////////////////////////

  // SMOOTH


  public void smooth() {
    smooth = true;
    if (hints[DISABLE_OPENGL_2X_SMOOTH]) {
      //gl.glEnable(GL10.GL_MULTISAMPLE);
      gl.glEnable(GL10.GL_POINT_SMOOTH);
      gl.glEnable(GL10.GL_LINE_SMOOTH);
//      gl.glEnable(GL10.GL_POLYGON_SMOOTH);  // OpenGL ES
    }
  }


  public void noSmooth() {
    smooth = false;
    if (hints[DISABLE_OPENGL_2X_SMOOTH]) {
      //gl.glDisable(GL10.GL_MULTISAMPLE);
      gl.glDisable(GL10.GL_POINT_SMOOTH);
      gl.glDisable(GL10.GL_LINE_SMOOTH);
//      gl.glDisable(GL10.GL_POLYGON_SMOOTH);  // OpenGL ES
    }
  }



  //////////////////////////////////////////////////////////////

  // IMAGES


  //public void imageMode(int mode)


  //public void image(PImage image, float x, float y)


  //public void image(PImage image, float x, float y, float c, float d)


  //public void image(PImage image,
  //                  float a, float b, float c, float d,
  //                  int u1, int v1, int u2, int v2)


  //protected void imageImpl(PImage image,
  //                         float x1, float y1, float x2, float y2,
  //                         int u1, int v1, int u2, int v2)



  //////////////////////////////////////////////////////////////

  // SHAPE


  //public void shapeMode(int mode)


  //public void shape(PShape shape)


  //public void shape(PShape shape, float x, float y)


  //public void shape(PShape shape, float x, float y, float c, float d)



  //////////////////////////////////////////////////////////////

  // TEXT SETTINGS


  //public void textAlign(int align)


  //public void textAlign(int alignX, int alignY)


  //public float textAscent()


  //public float textDescent()


  //public void textFont(PFont which)


  //public void textFont(PFont which, float size)


  //public void textLeading(float leading)


  //public void textMode(int mode)


  //protected boolean textModeCheck(int mode)


  //public void textSize(float size)


  //public float textWidth(char c)


  //public float textWidth(String str)


  //protected float textWidthImpl(char buffer[], int start, int stop)



  //////////////////////////////////////////////////////////////

  // TEXT

  // None of the variations of text() are overridden from PGraphics.



  //////////////////////////////////////////////////////////////

  // TEXT IMPL


  //protected void textLineAlignImpl(char buffer[], int start, int stop,
  //                                 float x, float y)


  //protected void textLineImpl(char buffer[], int start, int stop,
  //                            float x, float y)


  //protected void textCharImpl(char ch, float x, float y)


//  public class TessCallback extends GLUtessellatorCallbackAdapter {
//    public void begin(int type) {
//      switch (type) {
//      case GL10.GL_TRIANGLE_FAN: beginShape(TRIANGLE_FAN); break;
//      case GL10.GL_TRIANGLE_STRIP: beginShape(TRIANGLE_STRIP); break;
//      case GL10.GL_TRIANGLES: beginShape(TRIANGLES); break;
//      }
//    }
//
//    public void end() {
//      //gl.glEnd();
//      endShape();
//    }
//
//    public void edge(boolean e) {
//      PGraphicsOpenGL.this.edge(e);
//    }
//
//    public void vertex(Object data) {
//      if (data instanceof double[]) {
//        double[] d = (double[]) data;
//        if (d.length != 3) {
//          throw new RuntimeException("TessCallback vertex() data " +
//                                     "isn't length 3");
//        }
//        //System.out.println("tess callback vertex " +
//        //                 d[0] + " " + d[1] + " " + d[2]);
//        //vertexRedirect((float) d[0], (float) d[1], (float) d[2]);
//        PGraphicsOpenGL.this.vertex((float) d[0], (float) d[1], (float) d[2]);
//        /*
//        if (d.length == 6) {
//          double[] d2 = {d[0], d[1], d[2]};
//          gl.glVertex3dv(d2);
//          d2 = new double[]{d[3], d[4], d[5]};
//          gl.glColor3dv(d2);
//        } else if (d.length == 3) {
//          gl.glVertex3dv(d);
//        }
//        */
//      } else {
//        throw new RuntimeException("TessCallback vertex() data not understood");
//      }
//    }
//
//    public void error(int errnum) {
//      String estring = glu.gluErrorString(errnum);
//      PGraphics.showWarning("Tessellation Error: " + estring);
//    }
//
//    /**
//     * Implementation of the GLU_TESS_COMBINE callback.
//     * @param coords is the 3-vector of the new vertex
//     * @param data is the vertex data to be combined, up to four elements.
//     * This is useful when mixing colors together or any other
//     * user data that was passed in to gluTessVertex.
//     * @param weight is an array of weights, one for each element of "data"
//     * that should be linearly combined for new values.
//     * @param outData is the set of new values of "data" after being
//     * put back together based on the weights. it's passed back as a
//     * single element Object[] array because that's the closest
//     * that Java gets to a pointer.
//     */
//    public void combine(double[] coords, Object[] data,
//                        float[] weight, Object[] outData) {
//      //System.out.println("coords.length = " + coords.length);
//      //System.out.println("data.length = " + data.length);
//      //System.out.println("weight.length = " + weight.length);
//      //for (int i = 0; i < data.length; i++) {
//      //System.out.println(i + " " + data[i].getClass().getName() + " " + weight[i]);
//      //}
//
//      double[] vertex = new double[coords.length];
//      vertex[0] = coords[0];
//      vertex[1] = coords[1];
//      vertex[2] = coords[2];
//      //System.out.println("combine " +
//      //                 vertex[0] + " " + vertex[1] + " " + vertex[2]);
//
//      // this is just 3, so nothing interesting to bother combining
//      //System.out.println("data length " + ((double[]) data[0]).length);
//
//      // not gonna bother doing any combining,
//      // since no user data is being passed in.
//      /*
//      for (int i = 3; i < 6; i++) {
//        vertex[i] =
//          weight[0] * ((double[]) data[0])[i] +
//          weight[1] * ((double[]) data[1])[i] +
//          weight[2] * ((double[]) data[2])[i] +
//          weight[3] * ((double[]) data[3])[i];
//      }
//      */
//      outData[0] = vertex;
//    }
//  }


  //////////////////////////////////////////////////////////////

  // MATRIX STACK
  

  public void pushMatrix() {
    gl.glPushMatrix();
  }

  
  public void popMatrix() {
    gl.glPopMatrix();
    modelviewUpdated = false;     
  }
  

  //////////////////////////////////////////////////////////////

  // MATRIX TRANSFORMATIONS


  public void translate(float tx, float ty) {
    translate(tx, ty, 0);
  }


  public void translate(float tx, float ty, float tz) {
    // Translation along Y is inverted to account for Processing's inverted Y axis
    // with respect to OpenGL. The other place where inversion occurs is when
    // drawing the geometric primitives (vertex arrays), where a -1 scaling
    // along Y is applied.
    gl.glTranslatef(tx, -ty, tz);
    modelviewUpdated = false;
  }


  /**
   * Two dimensional rotation. Same as rotateZ (this is identical
   * to a 3D rotation along the z-axis) but included for clarity --
   * it'd be weird for people drawing 2D graphics to be using rotateZ.
   * And they might kick our a-- for the confusion.
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
   * Rotate around an arbitrary vector, similar to glRotate(),
   * except that it takes radians (instead of degrees).
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
      throw new RuntimeException("scale() cannot be called again between beginCamera()/endCamera()");
    } else {
      gl.glScalef(x, y, z);
      modelviewUpdated = false;
    }
  }  

    //////////////////////////////////////////////////////////////

  // MATRIX MORE!


  public void resetMatrix() {
    gl.glLoadIdentity();
  }
  
  
  public void applyMatrix(PMatrix2D source) {
    applyMatrix(source.m00, source.m01, source.m02,
                source.m10, source.m11, source.m12);
  }


  public void applyMatrix(float n00, float n01, float n02,
                          float n10, float n11, float n12) {
    applyMatrix(n00, n01, n02, 0,
                n10, n11, n12, 0,
                0,   0,   1,   0,
                0,   0,   0,   1);
  }


  public void applyMatrix(PMatrix3D source) {
    applyMatrix(source.m00, source.m01, source.m02, source.m03,
                source.m10, source.m11, source.m12, source.m13,
                source.m20, source.m21, source.m22, source.m23,
                source.m30, source.m31, source.m32, source.m33);
  }


  /**
   * Apply a 4x4 transformation matrix to the modelview stack using glMultMatrix().
   * This call will be slow because it will try to calculate the
   * inverse of the transform. So avoid it whenever possible.
   */
  public void applyMatrix(float n00, float n01, float n02, float n03,
                          float n10, float n11, float n12, float n13,
                          float n20, float n21, float n22, float n23,
                          float n30, float n31, float n32, float n33) {

    float[]  mat = new float[16];
    
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
  
  //////////////////////////////////////////////////////////////

  // MATRIX GET/SET/PRINT  
  
  
  public PMatrix getMatrix() {
    PMatrix res = new PMatrix3D();
    res.set(modelview);
    return res;
  }


  //public PMatrix2D getMatrix(PMatrix2D target)


  public PMatrix3D getMatrix(PMatrix3D target) {
    if (target == null) {
      target = new PMatrix3D();
    }
    target.set(modelview);
    return target;
  }


  //public void setMatrix(PMatrix source)


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
   * This function checks if the modelview matrix is set up to likely be
   * drawing in 2D. It merely checks if the non-translational piece of the
   * matrix is unity. If this is to be used, it should be coupled with a
   * check that the raw vertex coordinates lie in the z=0 plane.
   * Mainly useful for applying sub-pixel shifts to avoid 2d artifacts
   * in the screen plane.
   * Added by ewjordan 6/13/07
   *
   * TODO need to invert the logic here so that we can simply return
   * the value, rather than calculating true/false and returning it.
   */
  /*
  private boolean drawing2D() {
    if (modelview.m00 != 1.0f ||
        modelview.m11 != 1.0f ||
        modelview.m22 != 1.0f || // check scale
        modelview.m01 != 0.0f ||
        modelview.m02 != 0.0f || // check rotational pieces
        modelview.m10 != 0.0f ||
        modelview.m12 != 0.0f ||
        modelview.m20 != 0.0f ||
        modelview.m21 != 0.0f ||
        !((camera.m23-modelview.m23) <= EPSILON &&
          (camera.m23-modelview.m23) >= -EPSILON)) { // check for z-translation
      // Something about the modelview matrix indicates 3d drawing
      // (or rotated 2d, in which case 2d subpixel fixes probably aren't needed)
      return false;
    } else {
      //The matrix is mapping z=0 vertices to the screen plane,
      // which means it's likely that 2D drawing is happening.
      return true;
    }
  }
  */  
  
  
  //////////////////////////////////////////////////////////////

  // CAMERA

  
  /**
   * Set matrix mode to the camera matrix (instead of the current
   * transformation matrix). This means applyMatrix, resetMatrix, etc.
   * will affect the camera.
   * <P>
   * Note that the camera matrix is *not* the perspective matrix,
   * it contains the values of the modelview matrix immediatly after the latter
   * was initialized with ortho() or camera(), or the modelview matrix as resul
   * of the operations applied between beginCamera()/endCamera().
   * <P>
   * beginCamera() specifies that all coordinate transforms until endCamera()
   * should be pre-applied in inverse to the camera transform matrix.
   * Note that this is only challenging when a user specifies an arbitrary
   * matrix with applyMatrix(). Then that matrix will need to be inverted,
   * which may not be possible. But take heart, if a user is applying a
   * non-invertible matrix to the camera transform, then he is clearly
   * up to no good, and we can wash our hands of those bad intentions.
   * <P>
   * begin/endCamera clauses do not automatically reset the camera transform
   * matrix. That's because we set up a nice default camera transform int
   * setup(), and we expect it to hold through draw(). So we don't reset
   * the camera transform matrix at the top of draw(). That means that an
   * innocuous-looking clause like
   * <PRE>
   * beginCamera();
   * translate(0, 0, 10);
   * endCamera();
   * </PRE>
   * at the top of draw(), will result in a runaway camera that shoots
   * infinitely out of the screen over time. In order to prevent this,
   * it is necessary to call some function that does a hard reset of the
   * camera transform matrix inside of begin/endCamera. Two options are
   * <PRE>
   * camera(); // sets up the nice default camera transform
   * resetMatrix(); // sets up the identity camera transform
   * </PRE>
   * So to rotate a camera a constant amount, you might try
   * <PRE>
   * beginCamera();
   * camera();
   * rotateY(PI/8);
   * endCamera();
   * </PRE>
   */
  public void beginCamera() {
    if (manipulatingCamera) {
      throw new RuntimeException("beginCamera() cannot be called again " +
                                 "before endCamera()");
    } else {
      manipulatingCamera = true;      
    }
  }

  
  /**
   * Record the current settings into the camera matrix, and set
   * the matrix mode back to the current transformation matrix.
   * <P>
   * Note that this will destroy any settings to scale(), translate(),
   * or whatever, because the final camera matrix will be copied
   * (not multiplied) into the modelview.
   */
  public void endCamera() {
    if (!manipulatingCamera) {
      throw new RuntimeException("Cannot call endCamera() " +
                                 "without first calling beginCamera()");
    }
    
    getModelviewMatrix();
    
    // At this point no scaling transformations are allowed during beginCamera()/endCamera() which
    // makes sense if we thing of the camera as emulating a physical camera. However, for later
    // implementation scaling could be allowed, and in this case an auxiliar variable should be needed
    // in order to detect if scaling was applied between beginCamera() and endCamera(). Using this variable
    // the calculation of the inverse of the modelview matrix can be switched between this (very fast) and a
    // more general one (slower).
    calculateModelviewInvNoScaling();
    
    // Copying modelview matrix after camera transformations to the camera matrices.
    PApplet.arrayCopy(modelview, camera);
    PApplet.arrayCopy(modelviewInv, cameraInv);
    
    // all done
    manipulatingCamera = false;
  }

  
  protected void getProjectionMatrix() {
    if (gl instanceof GL11) {
      GL11 gl11 = (GL11) gl;
      gl11.glGetFloatv(GL11.GL_PROJECTION_MATRIX, projection, 0);
      projectionUpdated = true;
    } 
    else {
      // TODO: Mechanism to get modelview matrix when no the funtion GetFloatv is available.
      // Idea: when ony GL10 is available, then PMatrix3D versions of modelview and projection
      // matrices are needed, and should be updated during the call to the transformation methods
      // (rotate, translate, scale, etc).
    }
  }
  
  
  protected void getModelviewMatrix() {
    if (gl instanceof GL11) {
      GL11 gl11 = (GL11) gl;
      gl11.glGetFloatv(GL11.GL_MODELVIEW_MATRIX, modelview, 0);
      modelviewUpdated = true;
    } 
    else {
      // TODO: Mechanism to get modelview matrix when no the funtion GetFloatv is available. 
    }
  }

  // Calculates the inverse of the modelview matrix.
  protected void calculateModelviewInverse() {
    // TODO: Please finish!
  }
    
  // Calculates the inverse of the modelview matrix, assuming that no scaling transformation was applied,
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

    modelviewInv[4] =uy;
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
   * camera metaphor, this is what can manipulated by handling the camera
   * body (with the exception of scale, which doesn't really have a physcial
   * analog). The projection corresponds to what can be changed by
   * manipulating the lens.
   * <P>
   * We maintain separate matrices to represent the camera transform and
   * projection. An important distinction between the two is that the camera
   * transform should be invertible, where the projection matrix should not,
   * since it serves to map three dimensions to two. It is possible to bake
   * the two matrices into a single one just by multiplying them together,
   * but it isn't a good idea, since lighting, z-ordering, and z-buffering
   * all demand a true camera z coordinate after modelview and camera
   * transforms have been applied but before projection. If the camera
   * transform and projection are combined there is no way to recover a
   * good camera-space z-coordinate from a model coordinate.
   * <P>
   * Fortunately, there are no functions that manipulate both camera
   * transformation and projection.
   * <P>
   * camera() sets the camera position, orientation, and center of the scene.
   * It replaces the camera transform with a new one.
   * <P>
   * The transformation functions are the same ones used to manipulate the
   * modelview matrix (scale, translate, rotate, etc.). But they are bracketed
   * with beginCamera(), endCamera() to indicate that they should apply
   * (in inverse), to the camera transformation matrix.
   */
  public void camera() {
    camera(cameraX, cameraY, cameraZ,
                   cameraX, cameraY, 0,
                   0, 1, 0);
  }

  
  /**
   * More flexible method for dealing with camera().
   * <P>
   * The actual call is like gluLookat. Here's the real skinny on
   * what does what:
   * <PRE>
   * camera(); or
   * camera(ex, ey, ez, cx, cy, cz, ux, uy, uz);
   * </PRE>
   * do not need to be called from with beginCamera();/endCamera();
   * That's because they always apply to the camera transformation,
   * and they always totally replace it. That means that any coordinate
   * transforms done before camera(); in draw() will be wiped out.
   * It also means that camera() always operates in untransformed world
   * coordinates. Therefore it is always redundant to call resetMatrix();
   * before camera(); This isn't technically true of gluLookat, but it's
   * pretty much how it's used.
   * <P>
   * Now, beginCamera(); and endCamera(); are useful if you want to move
   * the camera around using transforms like translate(), etc. They will
   * wipe out any coordinate system transforms that occur before them in
   * draw(), but they will not automatically wipe out the camera transform.
   * This means that they should be at the top of draw(). It also means
   * that the following:
   * <PRE>
   * beginCamera();
   * rotateY(PI/8);
   * endCamera();
   * </PRE>
   * will result in a camera that spins without stopping. If you want to
   * just rotate a small constant amount, try this:
   * <PRE>
   * beginCamera();
   * camera(); // sets up the default view
   * rotateY(PI/8);
   * endCamera();
   * </PRE>
   * That will rotate a little off of the default view. Note that this
   * is entirely equivalent to
   * <PRE>
   * camera(); // sets up the default view
   * beginCamera();
   * rotateY(PI/8);
   * endCamera();
   * </PRE>
   * because camera() doesn't care whether or not it's inside a
   * begin/end clause. Basically it's safe to use camera() or
   * camera(ex, ey, ez, cx, cy, cz, ux, uy, uz) as naked calls because
   * they do all the matrix resetting automatically.
   */
  public void camera(float eyeX, float eyeY, float eyeZ,
                     float centerX, float centerY, float centerZ,
                     float upX, float upY, float upZ) {
    // Calculating Z vector
    float z0 = eyeX - centerX;
    float z1 = eyeY - centerY;
    float z2 = eyeZ - centerZ;
    float mag = sqrt(z0*z0 + z1*z1 + z2*z2);
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
    float x0 =  y1*z2 - y2*z1;
    float x1 = -y0*z2 + y2*z0;
    float x2 =  y0*z1 - y1*z0;

    // Recompute Y = Z cross X
    y0 =  z1*x2 - z2*x1;
    y1 = -z0*x2 + z2*x0;
    y2 =  z0*x1 - z1*x0;

    // Cross product gives area of parallelogram, which is < 1.0 for
    // non-perpendicular unit-length vectors; so normalize x, y here:    
    mag = sqrt(x0*x0 + x1*x1 + x2*x2);
    if (mag != 0) {
      x0 /= mag;
      x1 /= mag;
      x2 /= mag;
    }

    mag = sqrt(y0*y0 + y1*y1 + y2*y2);
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
    modelviewUpdated = true; // CPU and GPU copies of modelview matrix match each other.
    
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

  //////////////////////////////////////////////////////////////

  // PROJECTION


  /**
   * Calls ortho() with the proper parameters for Processing's
   * standard orthographic projection.
   */
  public void ortho() {
    ortho(0, width, 0, height, -10, 10);
  }

  
  /**
   * Similar to gluOrtho(), but wipes out the current projection matrix.
   * <P>
   * Implementation partially based on Mesa's matrix.c.
   */
  public void ortho(float left, float right,
                    float bottom, float top,
                    float near, float far) {
    float x =  2.0f / (right - left);
    float y =  2.0f / (top - bottom);
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
    projectionUpdated = true; // CPU and GPU copies of projection matrix match each other.
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
   * Each of these three functions completely replaces the projection
   * matrix with a new one. They can be called inside setup(), and their
   * effects will be felt inside draw(). At the top of draw(), the projection
   * matrix is not reset. Therefore the last projection function to be
   * called always dominates. On resize, the default projection is always
   * established, which has perspective.
   * <P>
   * This behavior is pretty much familiar from OpenGL, except where
   * functions replace matrices, rather than multiplying against the
   * previous.
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
   * Same as glFrustum(), except that it wipes out (rather than
   * multiplies against) the current perspective matrix.
   * <P>
   * Implementation based on the explanation in the OpenGL blue book.
   */
  public void frustum(float left, float right, float bottom,
                      float top, float znear, float zfar) {
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
    projectionUpdated = true; // CPU and GPU copies of projection matrix match each other (are in synch).
    
    // The matrix mode is always MODELVIEW, because the user will be doing geometrical transformations,
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

  
  //////////////////////////////////////////////////////////////

  // SCREEN AND MODEL COORDS

  
  public float screenX(float x, float y) {
    return screenX(x, y, 0);
  }
  
  
  public float screenY(float x, float y) {
    return screenY(x, y, 0);
  }

  
  public float screenX(float x, float y, float z) {
    y = -1*y; // To take into account Processsing's inverted Y axis with respect to OpenGL.
    
    if  (!modelviewUpdated) getModelviewMatrix();
    if (!projectionUpdated) getProjectionMatrix();
  
    float ax =
      modelview[toArrayIndex(0,0)] * x + modelview[toArrayIndex(0, 1)]*y + modelview[toArrayIndex(0,2)]*z + modelview[toArrayIndex(0, 3)];
    float ay =
      modelview[toArrayIndex(1, 0)]*x + modelview[toArrayIndex(1, 1)]*y + modelview[toArrayIndex(1, 2)]*z + modelview[toArrayIndex(1, 3)];
    float az =
      modelview[toArrayIndex(2, 0)]*x + modelview[toArrayIndex(2, 1)]*y + modelview[toArrayIndex(2, 2)]*z + modelview[toArrayIndex(2, 3)];
    float aw =
      modelview[toArrayIndex(3, 0)]*x + modelview[toArrayIndex(3, 1)]*y + modelview[toArrayIndex(3, 2)]*z + modelview[toArrayIndex(3, 3)];

    float ox =
      projection[toArrayIndex(0 ,0)]*ax + projection[toArrayIndex(0, 1)]*ay +
      projection[toArrayIndex(0, 2)]*az + projection[toArrayIndex(0, 3)]*aw;
    float ow =
      projection[toArrayIndex(3, 0)]*ax + projection[toArrayIndex(3, 1)]*ay +
      projection[toArrayIndex(3, 2)]*az + projection[toArrayIndex(3, 3)]*aw;

    if (ow != 0) ox /= ow;
    return width * (1 + ox) / 2.0f;
  }


  public float screenY(float x, float y, float z) {
    y = -1*y; // To take into account Processsing's inverted Y axis with respect to OpenGL.   

    if  (!modelviewUpdated) getModelviewMatrix();
    if (!projectionUpdated) getProjectionMatrix();
    
    float ax =
      modelview[toArrayIndex(0,0)] * x + modelview[toArrayIndex(0, 1)]*y + modelview[toArrayIndex(0,2)]*z + modelview[toArrayIndex(0, 3)];
    float ay =
      modelview[toArrayIndex(1, 0)]*x + modelview[toArrayIndex(1, 1)]*y + modelview[toArrayIndex(1, 2)]*z + modelview[toArrayIndex(1, 3)];
    float az =
      modelview[toArrayIndex(2, 0)]*x + modelview[toArrayIndex(2, 1)]*y + modelview[toArrayIndex(2, 2)]*z + modelview[toArrayIndex(2, 3)];
    float aw =
      modelview[toArrayIndex(3, 0)]*x + modelview[toArrayIndex(3, 1)]*y + modelview[toArrayIndex(3, 2)]*z + modelview[toArrayIndex(3, 3)];

    float oy =
      projection[toArrayIndex(1, 0)]*ax + projection[toArrayIndex(1, 1)]*ay +
      projection[toArrayIndex(1, 2)]*az + projection[toArrayIndex(1, 3)]*aw;
    float ow =
      projection[toArrayIndex(3, 0)]*ax + projection[toArrayIndex(3, 1)]*ay +
      projection[toArrayIndex(3, 2)]*az + projection[toArrayIndex(3, 3)]*aw;

    if (ow != 0) oy /= ow;
    return height * (1 + oy) / 2.0f; 
  }


  public float screenZ(float x, float y, float z) {
    if  (!modelviewUpdated) getModelviewMatrix();
    if (!projectionUpdated) getProjectionMatrix();

    y = -1*y; // To take into account Processsing's inverted Y axis with respect to OpenGL.
        
    float ax =
      modelview[toArrayIndex(0,0)] * x + modelview[toArrayIndex(0, 1)]*y + modelview[toArrayIndex(0,2)]*z + modelview[toArrayIndex(0, 3)];
    float ay =
      modelview[toArrayIndex(1, 0)]*x + modelview[toArrayIndex(1, 1)]*y + modelview[toArrayIndex(1, 2)]*z + modelview[toArrayIndex(1, 3)];
    float az =
      modelview[toArrayIndex(2, 0)]*x + modelview[toArrayIndex(2, 1)]*y + modelview[toArrayIndex(2, 2)]*z + modelview[toArrayIndex(2, 3)];
    float aw =
      modelview[toArrayIndex(3, 0)]*x + modelview[toArrayIndex(3, 1)]*y + modelview[toArrayIndex(3, 2)]*z + modelview[toArrayIndex(3, 3)];

    float oz =
      projection[toArrayIndex(2, 0)]*ax + projection[toArrayIndex(2, 1)]*ay +
      projection[toArrayIndex(2, 2)]*az + projection[toArrayIndex(2, 3)]*aw;
    float ow =
      projection[toArrayIndex(3, 0)]*ax + projection[toArrayIndex(3, 1)]*ay +
      projection[toArrayIndex(3, 2)]*az + projection[toArrayIndex(3, 3)]*aw;

    if (ow != 0) oz /= ow;
    return (oz + 1) / 2.0f;
  }
  
  public float modelX(float x, float y, float z) {
    if  (!modelviewUpdated) getModelviewMatrix();

    y = -1*y; // To take into account Processsing's inverted Y axis with respect to OpenGL.    
    
    float ax =
      modelview[toArrayIndex(0,0)] * x + modelview[toArrayIndex(0, 1)]*y + modelview[toArrayIndex(0,2)]*z + modelview[toArrayIndex(0, 3)];
    float ay =
      modelview[toArrayIndex(1, 0)]*x + modelview[toArrayIndex(1, 1)]*y + modelview[toArrayIndex(1, 2)]*z + modelview[toArrayIndex(1, 3)];
    float az =
      modelview[toArrayIndex(2, 0)]*x + modelview[toArrayIndex(2, 1)]*y + modelview[toArrayIndex(2, 2)]*z + modelview[toArrayIndex(2, 3)];
    float aw =
      modelview[toArrayIndex(3, 0)]*x + modelview[toArrayIndex(3, 1)]*y + modelview[toArrayIndex(3, 2)]*z + modelview[toArrayIndex(3, 3)];

    float ox =
      cameraInv[toArrayIndex(0, 0)]*ax + cameraInv[toArrayIndex(0, 1)]*ay +
      cameraInv[toArrayIndex(0, 2)]*az + cameraInv[toArrayIndex(0, 3)]*aw;
    float ow =
      cameraInv[toArrayIndex(3, 0)]*ax + cameraInv[toArrayIndex(3, 1)]*ay +
      cameraInv[toArrayIndex(3, 2)]*az + cameraInv[toArrayIndex(3, 3)]*aw;

    return (ow != 0) ? ox / ow : ox;
  }

  public float modelY(float x, float y, float z) {

    if  (!modelviewUpdated) getModelviewMatrix();     
      
    y = -1*y; // To take into account Processsing's inverted Y axis with respect to OpenGL.    
    
    float ax =
      modelview[toArrayIndex(0,0)] * x + modelview[toArrayIndex(0, 1)]*y + modelview[toArrayIndex(0,2)]*z + modelview[toArrayIndex(0, 3)];
    float ay =
      modelview[toArrayIndex(1, 0)]*x + modelview[toArrayIndex(1, 1)]*y + modelview[toArrayIndex(1, 2)]*z + modelview[toArrayIndex(1, 3)];
    float az =
      modelview[toArrayIndex(2, 0)]*x + modelview[toArrayIndex(2, 1)]*y + modelview[toArrayIndex(2, 2)]*z + modelview[toArrayIndex(2, 3)];
    float aw =
      modelview[toArrayIndex(3, 0)]*x + modelview[toArrayIndex(3, 1)]*y + modelview[toArrayIndex(3, 2)]*z + modelview[toArrayIndex(3, 3)];

    float oy =
      cameraInv[toArrayIndex(1, 0)]*ax + cameraInv[toArrayIndex(1, 1)]*ay +
      cameraInv[toArrayIndex(1, 2)]*az + cameraInv[toArrayIndex(1, 3)]*aw;
    float ow =
      cameraInv[toArrayIndex(3, 0)]*ax + cameraInv[toArrayIndex(3, 1)]*ay +
      cameraInv[toArrayIndex(3, 2)]*az + cameraInv[toArrayIndex(3, 3)]*aw;

    return (ow != 0) ? oy / ow : oy;
  }

  public float modelZ(float x, float y, float z) {
    
    if  (!modelviewUpdated) getModelviewMatrix();     
    
    y = -1*y; // To take into account Processsing's inverted Y axis with respect to OpenGL.
    
    float ax =
    modelview[toArrayIndex(0,0)] * x + modelview[toArrayIndex(0, 1)]*y + modelview[toArrayIndex(0,2)]*z + modelview[toArrayIndex(0, 3)];
    float ay =
      modelview[toArrayIndex(1, 0)]*x + modelview[toArrayIndex(1, 1)]*y + modelview[toArrayIndex(1, 2)]*z + modelview[toArrayIndex(1, 3)];
    float az =
      modelview[toArrayIndex(2, 0)]*x + modelview[toArrayIndex(2, 1)]*y + modelview[toArrayIndex(2, 2)]*z + modelview[toArrayIndex(2, 3)];
    float aw =
      modelview[toArrayIndex(3, 0)]*x + modelview[toArrayIndex(3, 1)]*y + modelview[toArrayIndex(3, 2)]*z + modelview[toArrayIndex(3, 3)];

    float oz =
      cameraInv[toArrayIndex(2, 0)]*ax + cameraInv[toArrayIndex(2, 1)]*ay +
      cameraInv[toArrayIndex(2, 2)]*az + cameraInv[toArrayIndex(2, 3)]*aw;
    float ow =
      cameraInv[toArrayIndex(3, 0)]*ax + cameraInv[toArrayIndex(3, 1)]*ay +
      cameraInv[toArrayIndex(3, 2)]*az + cameraInv[toArrayIndex(3, 3)]*aw;

    return (ow != 0) ? oz / ow : oz;
  }
  

  private int toArrayIndex(int i, int j) { 
    return 4 * j + i; 
    }  

  
  // STYLES


  //public void pushStyle()
  //public void popStyle()
  //public void style(PStyle)
  //public PStyle getStyle()
  //public void getStyle(PStyle)



  //////////////////////////////////////////////////////////////

  // COLOR MODE


  //public void colorMode(int mode)
  //public void colorMode(int mode, float max)
  //public void colorMode(int mode, float mx, float my, float mz);
  //public void colorMode(int mode, float mx, float my, float mz, float ma);



  //////////////////////////////////////////////////////////////

  // COLOR CALC


  //protected void colorCalc(int rgb)
  //protected void colorCalc(int rgb, float alpha)
  //protected void colorCalc(float gray)
  //protected void colorCalc(float gray, float alpha)
  //protected void colorCalc(float x, float y, float z)
  //protected void colorCalc(float x, float y, float z, float a)
  //protected void colorCalcARGB(int argb, float alpha)



  //////////////////////////////////////////////////////////////

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



  //////////////////////////////////////////////////////////////

  // STROKE, TINT, FILL


  //public void noStroke()
  //public void stroke(int rgb)
  //public void stroke(int rgb, float alpha)
  //public void stroke(float gray)
  //public void stroke(float gray, float alpha)
  //public void stroke(float x, float y, float z)
  //public void stroke(float x, float y, float z, float a)
  //protected void strokeFromCalc()

  //public void noTint()
  //public void tint(int rgb)
  //public void tint(int rgb, float alpha)
  //public void tint(float gray)
  //public void tint(float gray, float alpha)
  //public void tint(float x, float y, float z)
  //public void tint(float x, float y, float z, float a)
  //protected void tintFromCalc()

  //public void noFill()
  //public void fill(int rgb)
  //public void fill(int rgb, float alpha)
  //public void fill(float gray)
  //public void fill(float gray, float alpha)
  //public void fill(float x, float y, float z)
  //public void fill(float x, float y, float z, float a)


  protected void fillFromCalc() {
    super.fillFromCalc();
    calcColorBuffer();
    gl.glMaterialfv(GL10.GL_FRONT_AND_BACK, GL10.GL_AMBIENT_AND_DIFFUSE,
                    colorFloats, 0);
  }



  //////////////////////////////////////////////////////////////

  // MATERIAL PROPERTIES


//  public void ambient(int rgb) {
//    super.ambient(rgb);
//    calcColorBuffer();
//    gl.glMaterialfv(GL.GL_FRONT_AND_BACK, GL.GL_AMBIENT, colorBuffer, 0);
//  }


//  public void ambient(float gray) {
//    super.ambient(gray);
//    calcColorBuffer();
//    gl.glMaterialfv(GL.GL_FRONT_AND_BACK, GL.GL_AMBIENT, colorBuffer, 0);
//  }


//  public void ambient(float x, float y, float z) {
//    super.ambient(x, y, z);
//    calcColorBuffer();
//    gl.glMaterialfv(GL.GL_FRONT_AND_BACK, GL.GL_AMBIENT, colorBuffer, 0);
//  }


  protected void ambientFromCalc() {
    super.ambientFromCalc();
    calcColorBuffer();
    gl.glMaterialfv(GL10.GL_FRONT_AND_BACK, GL10.GL_AMBIENT, colorFloats, 0);
  }


//  public void specular(int rgb) {
//    super.specular(rgb);
//    calcColorBuffer();
//    gl.glMaterialfv(GL.GL_FRONT_AND_BACK, GL.GL_SPECULAR, colorBuffer, 0);
//  }


//  public void specular(float gray) {
//    super.specular(gray);
//    calcColorBuffer();
//    gl.glMaterialfv(GL.GL_FRONT_AND_BACK, GL.GL_SPECULAR, colorBuffer, 0);
//  }


//  public void specular(float x, float y, float z) {
//    super.specular(x, y, z);
//    calcColorBuffer();
//    gl.glMaterialfv(GL.GL_FRONT_AND_BACK, GL.GL_SPECULAR, colorBuffer, 0);
//  }


  protected void specularFromCalc() {
    super.specularFromCalc();
    calcColorBuffer();
    gl.glMaterialfv(GL10.GL_FRONT_AND_BACK, GL10.GL_SPECULAR, colorFloats, 0);
  }


  public void shininess(float shine) {
    super.shininess(shine);
    gl.glMaterialf(GL10.GL_FRONT_AND_BACK, GL10.GL_SHININESS, shine);
  }


//  public void emissive(int rgb) {
//    super.emissive(rgb);
//    calcColorBuffer();
//    gl.glMaterialfv(GL.GL_FRONT_AND_BACK, GL.GL_EMISSION, colorBuffer, 0);
//  }


//  public void emissive(float gray) {
//    super.emissive(gray);
//    calcColorBuffer();
//    gl.glMaterialfv(GL.GL_FRONT_AND_BACK, GL.GL_EMISSION, colorBuffer, 0);
//  }


//  public void emissive(float x, float y, float z) {
//    super.emissive(x, y, z);
//    calcColorBuffer();
//    gl.glMaterialfv(GL.GL_FRONT_AND_BACK, GL.GL_EMISSION, colorBuffer, 0);
//  }


  protected void emissiveFromCalc() {
    super.emissiveFromCalc();
    calcColorBuffer();
    gl.glMaterialfv(GL10.GL_FRONT_AND_BACK, GL10.GL_EMISSION, colorFloats, 0);
  }



  //////////////////////////////////////////////////////////////

  // LIGHTING

/**
   * Sets up an ambient and directional light using OpenGL. API takef from PGraphics3D.
   * <PRE>
   * The Lighting Skinny:
   *
   * The way lighting works is complicated enough that it's worth
   * producing a document to describe it. Lighting calculations proceed
   * pretty much exactly as described in the OpenGL red book.
   *
   * Light-affecting material properties:
   *
   *   AMBIENT COLOR
   *   - multiplies by light's ambient component
   *   - for believability this should match diffuse color
   *
   *   DIFFUSE COLOR
   *   - multiplies by light's diffuse component
   *
   *   SPECULAR COLOR
   *   - multiplies by light's specular component
   *   - usually less colored than diffuse/ambient
   *
   *   SHININESS
   *   - the concentration of specular effect
   *   - this should be set pretty high (20-50) to see really
   *     noticeable specularity
   *
   *   EMISSIVE COLOR
   *   - constant additive color effect
   *
   * Light types:
   *
   *   AMBIENT
   *   - one color
   *   - no specular color
   *   - no direction
   *   - may have falloff (constant, linear, and quadratic)
   *   - may have position (which matters in non-constant falloff case)
   *   - multiplies by a material's ambient reflection
   *
   *   DIRECTIONAL
   *   - has diffuse color
   *   - has specular color
   *   - has direction
   *   - no position
   *   - no falloff
   *   - multiplies by a material's diffuse and specular reflections
   *
   *   POINT
   *   - has diffuse color
   *   - has specular color
   *   - has position
   *   - no direction
   *   - may have falloff (constant, linear, and quadratic)
   *   - multiplies by a material's diffuse and specular reflections
   *
   *   SPOT
   *   - has diffuse color
   *   - has specular color
   *   - has position
   *   - has direction
   *   - has cone angle (set to half the total cone angle)
   *   - has concentration value
   *   - may have falloff (constant, linear, and quadratic)
   *   - multiplies by a material's diffuse and specular reflections
   *
   * Normal modes:
   *
   * All of the primitives (rect, box, sphere, etc.) have their normals
   * set nicely. During beginShape/endShape normals can be set by the user.
   *
   *   AUTO-NORMAL
   *   - if no normal is set during the shape, we are in auto-normal mode
   *   - auto-normal calculates one normal per triangle (face-normal mode)
   *
   *   SHAPE-NORMAL
   *   - if one normal is set during the shape, it will be used for
   *     all vertices
   *
   *   VERTEX-NORMAL
   *   - if multiple normals are set, each normal applies to
   *     subsequent vertices
   *   - (except for the first one, which applies to previous
   *     and subsequent vertices)
   *
   * Efficiency consequences:
   *
   *   There is a major efficiency consequence of position-dependent
   *   lighting calculations per vertex. (See below for determining
   *   whether lighting is vertex position-dependent.) If there is no
   *   position dependency then the only factors that affect the lighting
   *   contribution per vertex are its colors and its normal.
   *   There is a major efficiency win if
   *
   *   1) lighting is not position dependent
   *   2) we are in AUTO-NORMAL or SHAPE-NORMAL mode
   *
   *   because then we can calculate one lighting contribution per shape
   *   (SHAPE-NORMAL) or per triangle (AUTO-NORMAL) and simply multiply it
   *   into the vertex colors. The converse is our worst-case performance when
   *
   *   1) lighting is position dependent
   *   2) we are in AUTO-NORMAL mode
   *
   *   because then we must calculate lighting per-face * per-vertex.
   *   Each vertex has a different lighting contribution per face in
   *   which it appears. Yuck.
   *
   * Determining vertex position dependency:
   *
   *   If any of the following factors are TRUE then lighting is
   *   vertex position dependent:
   *
   *   1) Any lights uses non-constant falloff
   *   2) There are any point or spot lights
   *   3) There is a light with specular color AND there is a
   *      material with specular color
   *
   * So worth noting is that default lighting (a no-falloff ambient
   * and a directional without specularity) is not position-dependent.
   * We should capitalize.
   *
   * Simon Greenwold, April 2005
   * </PRE>
   */
  public void lights() 
  {
    gl.glEnable(GL10.GL_LIGHTING);
  		
    // need to make sure colorMode is RGB 255 here
    int colorModeSaved = colorMode;
    colorMode = RGB;

    lightFalloff(1, 0, 0);
    lightSpecular(0, 0, 0);

    ambientLight(colorModeX * 0.5f,
  	                         colorModeY * 0.5f,
                             colorModeZ * 0.5f);
    directionalLight(colorModeX * 0.5f,
                                  colorModeY * 0.5f,
                                  colorModeZ * 0.5f,
                                  0, 0, -1);

    colorMode = colorModeSaved;    	
  }
  

  /**
   * Switches off all lights, but keeps lighting enabled..
   */    
  public void resetLights() { 
    for (int i = 0; i < lightCount; i++) glLightDisable(i);
    lightCount = 0;
  }

  
  /**
   * Disables lighting.
   */  
  public void noLights() {
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
   * Add an ambient light based on the current color mode.
   * This version includes an (x, y, z) position for situations
   * where the falloff distance is used.
   */
  public void ambientLight(float r, float g, float b,
                                                   float x, float y, float z) {
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
    lightPosition[lightCount][3] = 0.0f;
    
    glLightEnable(lightCount);
    glLightAmbient(lightCount);
    glLightPosition(lightCount);
    glLightFalloff(lightCount);
    
    lightCount++;
  }


  public void directionalLight(float r, float g, float b,
                               float nx, float ny, float nz) {
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
    
    lightCount++;
  }


  public void pointLight(float r, float g, float b,
                         float x, float y, float z) {
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
    lightPosition[lightCount][3] = 0.0f;
    
    glLightEnable(lightCount);
    glLightNoAmbient(lightCount);
    glLightPosition(lightCount);
    glLightDiffuse(lightCount);
    glLightSpecular(lightCount);
    glLightFalloff(lightCount);      
    
    lightCount++;
  }

  
  public void spotLight(float r, float g, float b,
                        float x, float y, float z,
                        float nx, float ny, float nz,
                        float angle, float concentration) {
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
    lightPosition[lightCount][3] = 0.0f;
    
    float invn = 1.0f / PApplet.dist(0, 0, 0, nx, ny, nz);
    lightNormal[lightCount][0] = invn * nx;
    lightNormal[lightCount][1] = invn * ny;
    lightNormal[lightCount][2] = invn * nz;
    lightNormal[lightCount][3] = 0.0f;  
    
    lightSpotAngle[lightCount] = angle;
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
   * Set the light falloff rates for the last light that was created.
   * Default is lightFalloff(1, 0, 0).
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
    gl.glLightfv(GL10.GL_LIGHT0 + num,
                 GL10.GL_AMBIENT, lightDiffuse[num], 0);
  }

  
  private void glLightNoAmbient(int num) {
    gl.glLightfv(GL10.GL_LIGHT0 + num,
                 GL10.GL_AMBIENT, zeroLight, 0);
  }

  
  private void glLightDiffuse(int num) {
    gl.glLightfv(GL10.GL_LIGHT0 + num,
                 GL10.GL_DIFFUSE, lightDiffuse[num], 0);
  }

  
  private void glLightDirection(int num) {
    if (lightType[num] == DIRECTIONAL) {
      // TODO this expects a fourth arg that will be set to 1
      //      this is why lightBuffer is length 4,
      //      and the [3] element set to 1 in the constructor.
      //      however this may be a source of problems since
      //      it seems a bit "hack"
      gl.glLightfv(GL10.GL_LIGHT0 + num, GL10.GL_POSITION,
                   lightNormal[num], 0);
    } else {  // spotlight
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
    gl.glLightf(GL10.GL_LIGHT0 + num,
                GL10.GL_CONSTANT_ATTENUATION, lightFalloffConstant[num]);
    gl.glLightf(GL10.GL_LIGHT0 + num,
                GL10.GL_LINEAR_ATTENUATION, lightFalloffLinear[num]);
    gl.glLightf(GL10.GL_LIGHT0 + num,
                GL10.GL_QUADRATIC_ATTENUATION, lightFalloffQuadratic[num]);
  }


  private void glLightPosition(int num) {
    gl.glLightfv(GL10.GL_LIGHT0 + num, GL10.GL_POSITION, lightPosition[num], 0);
  }


  private void glLightSpecular(int num) {
    gl.glLightfv(GL10.GL_LIGHT0 + num, GL10.GL_SPECULAR, lightSpecular[num], 0);
  }


  private void glLightSpotAngle(int num) {
    gl.glLightf(GL10.GL_LIGHT0 + num,
                GL10.GL_SPOT_CUTOFF, lightSpotAngle[num]);
  }


  private void glLightSpotConcentration(int num) {
    gl.glLightf(GL10.GL_LIGHT0 + num,
                GL10.GL_SPOT_EXPONENT, lightSpotConcentration[num]);
  }
  
  
  //////////////////////////////////////////////////////////////

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



  //////////////////////////////////////////////////////////////

  // COLOR MODE

  // colorMode() is inherited from PGraphics.



  //////////////////////////////////////////////////////////////

  // COLOR CALC

  // This is the OpenGL complement to the colorCalc() methods.


  /**
   * Load the calculated color into a pre-allocated array so that
   * it can be quickly passed over to OpenGL.
   */
  private final void calcColorBuffer() {
    if (colorFloats == null) {
//      colorBuffer = BufferUtil.newFloatBuffer(4);
      colorFloats = new float[4];
    }
    colorFloats[0] = calcR;
    colorFloats[1] = calcG;
    colorFloats[2] = calcB;
    colorFloats[3] = calcA;
//    colorBuffer.put(0, calcR);
//    colorBuffer.put(1, calcG);
//    colorBuffer.put(2, calcB);
//    colorBuffer.put(3, calcA);
//    colorBuffer.rewind();
  }



  //////////////////////////////////////////////////////////////

  // COLOR METHODS

  //public final int color(int gray)
  //public final int color(int gray, int alpha)
  //public final int color(int rgb, float alpha)
  //public final int color(int x, int y, int z)

  //public final float alpha(int what)
  //public final float red(int what)
  //public final float green(int what)
  //public final float blue(int what)
  //public final float hue(int what)
  //public final float saturation(int what)
  //public final float brightness(int what)

  //public int lerpColor(int c1, int c2, float amt)
  //static public int lerpColor(int c1, int c2, float amt, int mode)



  //////////////////////////////////////////////////////////////

  // BEGINRAW/ENDRAW

  // beginRaw, endRaw() both inherited.



  //////////////////////////////////////////////////////////////

  // WARNINGS and EXCEPTIONS

  // showWarning() and showException() available from PGraphics.


  /**
   * Report on anything from glError().
   * Don't use this inside glBegin/glEnd otherwise it'll
   * throw an GL_INVALID_OPERATION error.
   */
  public void report(String where) {
    if (!hints[DISABLE_OPENGL_ERROR_REPORT]) {
      int err = gl.glGetError();
      if (err != 0) {
        String errString = GLU.gluErrorString(err);
        String msg = "OpenGL error " + err + " at " + where + ": " + errString;
        PGraphics.showWarning(msg);
      }
    }
  }



  //////////////////////////////////////////////////////////////

  // RENDERER SUPPORT QUERIES


  //public boolean displayable()


  //public boolean dimensional()  // from P3D



  //////////////////////////////////////////////////////////////

  // PIMAGE METHODS

  // getImage
  // setCache, getCache, removeCache
  // isModified, setModified



  //////////////////////////////////////////////////////////////

  // LOAD/UPDATE PIXELS


  public void loadPixels() {
    if ((pixels == null) || (pixels.length != width*height)) {
      pixels = new int[width * height];
      pixelBuffer = BufferUtil.newIntBuffer(pixels.length);
//      pixelBuffer = IntBuffer.allocate(pixels.length);
    }

    gl.glReadPixels(0, 0, width, height,
                    GL10.GL_RGBA, GL10.GL_UNSIGNED_BYTE, pixelBuffer);
    pixelBuffer.get(pixels);
    pixelBuffer.rewind();

    // flip vertically (opengl stores images upside down),
    // and swap RGBA components to ARGB (big endian)
    int index = 0;
    int yindex = (height - 1) * width;
    for (int y = 0; y < height/2; y++) {
      if (BIG_ENDIAN) {
        for (int x = 0; x < width; x++) {
          int temp = pixels[index];
          // ignores alpha component, just sets it opaque
          pixels[index] = 0xff000000 | ((pixels[yindex] >> 8)  & 0x00ffffff);
          pixels[yindex] = 0xff000000 | ((temp >> 8)  & 0x00ffffff);

          index++;
          yindex++;
        }
      } else {  // LITTLE_ENDIAN, convert ABGR to ARGB
        for (int x = 0; x < width; x++) {
          int temp = pixels[index];

          // identical to endPixels because only two
          // components are being swapped
          pixels[index] = 0xff000000 |
            ((pixels[yindex] << 16) & 0xff0000) |
            (pixels[yindex] & 0xff00) |
            ((pixels[yindex] >> 16) & 0xff);

          pixels[yindex] = 0xff000000 |
            ((temp << 16) & 0xff0000) |
            (temp & 0xff00) |
            ((temp >> 16) & 0xff);

          index++;
          yindex++;
        }
      }
      yindex -= width*2;
    }

    // When height is an odd number, the middle line needs to be
    // endian swapped, but not y-swapped.
    // http://dev.processing.org/bugs/show_bug.cgi?id=944
    if ((height % 2) == 1) {
      index = (height / 2) * width;
      if (BIG_ENDIAN) {
        for (int x = 0; x < width; x++) {
          // ignores alpha component, just sets it opaque
          pixels[index] = 0xff000000 | ((pixels[index] >> 8)  & 0x00ffffff);
        }
      } else {
        for (int x = 0; x < width; x++) {
          pixels[index] = 0xff000000 |
            ((pixels[index] << 16) & 0xff0000) |
             (pixels[index] & 0xff00) |
            ((pixels[index] >> 16) & 0xff);
        }
      }
    }
  }


  /**
   * Convert native OpenGL format into palatable ARGB format.
   * This function leaves alone (ignores) the alpha component.
   * Also flips the image vertically, since images are upside-down in GL.
   */
  static void nativeToJavaRGB(PImage image) {
    int index = 0;
    int yindex = (image.height - 1) * image.width;
    for (int y = 0; y < image.height/2; y++) {
      if (BIG_ENDIAN) {
        for (int x = 0; x < image.width; x++) {
          int temp = image.pixels[index];
          // ignores alpha component, just sets it opaque
          image.pixels[index] =
            0xff000000 | ((image.pixels[yindex] >> 8)  & 0x00ffffff);
          image.pixels[yindex] =
            0xff000000 | ((temp >> 8)  & 0x00ffffff);
          index++;
          yindex++;
        }
      } else {  // LITTLE_ENDIAN, convert ABGR to ARGB
        for (int x = 0; x < image.width; x++) {
          int temp = image.pixels[index];

          // identical to endPixels because only two
          // components are being swapped
          image.pixels[index] = 0xff000000 |
            ((image.pixels[yindex] << 16) & 0xff0000) |
            (image.pixels[yindex] & 0xff00) |
            ((image.pixels[yindex] >> 16) & 0xff);

          image.pixels[yindex] = 0xff000000 |
            ((temp << 16) & 0xff0000) |
            (temp & 0xff00) |
            ((temp >> 16) & 0xff);

          index++;
          yindex++;
        }
      }
      yindex -= image.width*2;
    }
  }


  /**
   * Convert native OpenGL format into palatable ARGB format.
   * This function leaves alone (ignores) the alpha component.
   * Also flips the image vertically, since images are upside-down in GL.
   */
  static void nativeToJavaARGB(PImage image) {
    int index = 0;
    int yindex = (image.height - 1) * image.width;
    for (int y = 0; y < image.height/2; y++) {
      if (BIG_ENDIAN) {
        for (int x = 0; x < image.width; x++) {
          int temp = image.pixels[index];
          // ignores alpha component, just sets it opaque
          image.pixels[index] =
            (image.pixels[yindex] & 0xff000000) |
            ((image.pixels[yindex] >> 8)  & 0x00ffffff);
          image.pixels[yindex] =
            (temp & 0xff000000) |
            ((temp >> 8)  & 0x00ffffff);
          index++;
          yindex++;
        }
      } else {  // LITTLE_ENDIAN, convert ABGR to ARGB
        for (int x = 0; x < image.width; x++) {
          int temp = image.pixels[index];

          // identical to endPixels because only two
          // components are being swapped
          image.pixels[index] =
            (image.pixels[yindex] & 0xff000000) |
            ((image.pixels[yindex] << 16) & 0xff0000) |
            (image.pixels[yindex] & 0xff00) |
            ((image.pixels[yindex] >> 16) & 0xff);

          image.pixels[yindex] =
            (temp & 0xff000000) |
            ((temp << 16) & 0xff0000) |
            (temp & 0xff00) |
            ((temp >> 16) & 0xff);

          index++;
          yindex++;
        }
      }
      yindex -= image.width*2;
    }
  }


  /**
   * Convert ARGB (Java/Processing) data to native OpenGL format.
   * This function leaves alone (ignores) the alpha component.
   * Also flips the image vertically, since images are upside-down in GL.
   */
  static void javaToNativeRGB(PImage image) {
    int width = image.width;
    int height = image.height;
    int pixels[] = image.pixels;

    int index = 0;
    int yindex = (height - 1) * width;
    for (int y = 0; y < height/2; y++) {
      if (BIG_ENDIAN) {
        // and convert ARGB back to opengl RGBA components (big endian)
        for (int x = 0; x < image.width; x++) {
          int temp = pixels[index];
          /*
            pixels[index] =
            ((pixels[yindex] >> 24) & 0xff) |
            ((pixels[yindex] << 8) & 0xffffff00);
          pixels[yindex] =
            ((temp >> 24) & 0xff) |
            ((temp << 8) & 0xffffff00);
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

          pixels[index] = 0xff000000 |
            ((pixels[yindex] << 16) & 0xff0000) |
            (pixels[yindex] & 0xff00) |
            ((pixels[yindex] >> 16) & 0xff);

          pixels[yindex] = 0xff000000 |
            ((temp << 16) & 0xff0000) |
            (temp & 0xff00) |
            ((temp >> 16) & 0xff);

          index++;
          yindex++;
        }
      }
      yindex -= width*2;
    }
  }


  /**
   * Convert Java ARGB to native OpenGL format.
   * Also flips the image vertically, since images are upside-down in GL.
   */
  static void javaToNativeARGB(PImage image) {
    int width = image.width;
    int height = image.height;
    int pixels[] = image.pixels;

    int index = 0;
    int yindex = (height - 1) * width;
    for (int y = 0; y < height/2; y++) {
      if (BIG_ENDIAN) {
        // and convert ARGB back to opengl RGBA components (big endian)
        for (int x = 0; x < image.width; x++) {
          int temp = pixels[index];
          pixels[index] =
            ((pixels[yindex] >> 24) & 0xff) |
            ((pixels[yindex] << 8) & 0xffffff00);
          pixels[yindex] =
            ((temp >> 24) & 0xff) |
            ((temp << 8) & 0xffffff00);

          index++;
          yindex++;
        }

      } else {
        // convert ARGB back to native little endian ABGR
        for (int x = 0; x < width; x++) {
          int temp = pixels[index];

          pixels[index] =
            (pixels[yindex] & 0xff000000) |
            ((pixels[yindex] << 16) & 0xff0000) |
            (pixels[yindex] & 0xff00) |
            ((pixels[yindex] >> 16) & 0xff);

          pixels[yindex] =
            (pixels[yindex] & 0xff000000) |
            ((temp << 16) & 0xff0000) |
            (temp & 0xff00) |
            ((temp >> 16) & 0xff);

          index++;
          yindex++;
        }
      }
      yindex -= width*2;
    }
  }


  public void updatePixels() {
    // flip vertically (opengl stores images upside down),

    int index = 0;
    int yindex = (height - 1) * width;
    for (int y = 0; y < height/2; y++) {
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

          pixels[index] = 0xff000000 |
            ((pixels[yindex] << 16) & 0xff0000) |
            (pixels[yindex] & 0xff00) |
            ((pixels[yindex] >> 16) & 0xff);

          pixels[yindex] = 0xff000000 |
            ((temp << 16) & 0xff0000) |
            (temp & 0xff00) |
            ((temp >> 16) & 0xff);

          index++;
          yindex++;
        }
      }
      yindex -= width*2;
    }

    // re-pack ARGB data into RGBA for opengl (big endian)
    //for (int i = 0; i < pixels.length; i++) {
      //pixels[i] = ((pixels[i] >> 24) & 0xff) |
        //((pixels[i] << 8) & 0xffffff00);
    //}

    setRasterPos(0, 0);  // lower-left corner

    pixelBuffer.put(pixels);
    pixelBuffer.rewind();
    // TODO fix me for android
//    gl.glDrawPixels(width, height,
//                    GL10.GL_RGBA, GL10.GL_UNSIGNED_BYTE, pixelBuffer);
  }



  //////////////////////////////////////////////////////////////

  // RESIZE


  public void resize(int wide, int high) {
    PGraphics.showMethodWarning("resize");
  }



  //////////////////////////////////////////////////////////////

  // GET/SET


//  IntBuffer getsetBuffer = IntBuffer.allocate(1);
  IntBuffer getsetBuffer = BufferUtil.newIntBuffer(1);
//  int getset[] = new int[1];

  public int get(int x, int y) {
    gl.glReadPixels(x, y, 1, 1, GL10.GL_RGBA, GL10.GL_UNSIGNED_BYTE, getsetBuffer);
    int getset = getsetBuffer.get(0);

    if (BIG_ENDIAN) {
      return 0xff000000 | ((getset >> 8)  & 0x00ffffff);

    } else {
      return 0xff000000 |
            ((getset << 16) & 0xff0000) |
            (getset & 0xff00) |
            ((getset >> 16) & 0xff);
    }
  }


  //public PImage get(int x, int y, int w, int h)


  protected PImage getImpl(int x, int y, int w, int h) {
    PImage newbie = new PImage(w, h); //new int[w*h], w, h, ARGB);

    //IntBuffer newbieBuffer = BufferUtil.newIntBuffer(w*h);
    IntBuffer newbieBuffer = IntBuffer.allocate(w * h);
    gl.glReadPixels(x, y, w, h, GL10.GL_RGBA, GL10.GL_UNSIGNED_BYTE, newbieBuffer);
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
      getset =
        (argb & 0xff00ff00) |
        ((argb << 16) & 0xff0000) |
        ((argb >> 16) & 0xff);
    }
    getsetBuffer.put(0, getset);
    getsetBuffer.rewind();
    //gl.glRasterPos2f(x + EPSILON, y + EPSILON);
    setRasterPos(x, (height-y) - 1);
    // TODO whither drawPixels? 
//    gl.glDrawPixels(1, 1, GL10.GL_RGBA, GL10.GL_UNSIGNED_BYTE, getsetBuffer);
  }


  /**
   * Set an image directly to the screen.
   * <P>
   * TODO not optimized properly, creates multiple temporary buffers
   * the size of the image. Needs to instead use image cache, but that
   * requires two types of image cache. One for power of 2 textures
   * and another for glReadPixels/glDrawPixels data that's flipped
   * vertically. Both have their components all swapped to native.
   */
  public void set(int x, int y, PImage source) {
    int[] backup = new int[source.pixels.length];
    System.arraycopy(source.pixels, 0, backup, 0, source.pixels.length);
    javaToNativeARGB(source);

    // TODO is this possible without intbuffer?
    IntBuffer setBuffer = BufferUtil.newIntBuffer(source.pixels.length);
    setBuffer.put(source.pixels);
    setBuffer.rewind();

    setRasterPos(x, (height-y) - source.height); //+source.height);
//    gl.glDrawPixels(source.width, source.height,
//                    GL10.GL_RGBA, GL10.GL_UNSIGNED_BYTE, setBuffer);
    source.pixels = backup;
  }


  // TODO remove the implementation above and use setImpl instead,
  // since it'll be more efficient
  // http://dev.processing.org/bugs/show_bug.cgi?id=943
  //protected void setImpl(int dx, int dy, int sx, int sy, int sw, int sh,
  //                       PImage src)


  /**
   * Definitive method for setting raster pos, including offscreen locations.
   * The raster position is tricky because it's affected by the modelview and
   * projection matrices. Further, offscreen coords won't properly set the
   * raster position. This code gets around both issues.
   * http://www.mesa3d.org/brianp/sig97/gotchas.htm
   * @param y the Y-coordinate, which is flipped upside down in OpenGL
   */
  protected void setRasterPos(float x, float y) {
//    float z = 0;
//    float w = 1;
//
//    float fx, fy;
//
//    // Push current matrix mode and viewport attributes
//    gl.glPushAttrib(GL.GL_TRANSFORM_BIT | GL.GL_VIEWPORT_BIT);
//
//    // Setup projection parameters
//    gl.glMatrixMode(GL.GL_PROJECTION);
//    gl.glPushMatrix();
//    gl.glLoadIdentity();
//    gl.glMatrixMode(GL.GL_MODELVIEW);
//    gl.glPushMatrix();
//    gl.glLoadIdentity();
//
//    gl.glDepthRange(z, z);
//    gl.glViewport((int) x - 1, (int) y - 1, 2, 2);
//
//    // set the raster (window) position
//    fx = x - (int) x;
//    fy = y - (int) y;
//    gl.glRasterPos4f(fx, fy, 0, w);
//
//    // restore matrices, viewport and matrix mode
//    gl.glPopMatrix();
//    gl.glMatrixMode(GL.GL_PROJECTION);
//    gl.glPopMatrix();
//
//    gl.glPopAttrib();
  }



  //////////////////////////////////////////////////////////////

  // MASK


  public void mask(int alpha[]) {
    PGraphics.showMethodWarning("mask");
  }


  public void mask(PImage alpha) {
    PGraphics.showMethodWarning("mask");
  }



  //////////////////////////////////////////////////////////////

  // FILTER


  /**
   * This is really inefficient and not a good idea in OpenGL.
   * Use get() and set() with a smaller image area, or call the
   * filter on an image instead, and then draw that.
   */
  public void filter(int kind) {
    PImage temp = get();
    temp.filter(kind);
    set(0, 0, temp);
  }


  /**
   * This is really inefficient and not a good idea in OpenGL.
   * Use get() and set() with a smaller image area, or call the
   * filter on an image instead, and then draw that.
   */
  public void filter(int kind, float param) {
    PImage temp = get();
    temp.filter(kind, param);
    set(0, 0, temp);
  }


  //////////////////////////////////////////////////////////////


  /**
   * Extremely slow and not optimized, should use GL methods instead.
   * Currently calls a beginPixels() on the whole canvas, then does the copy,
   * then it calls endPixels().
   */
  //public void copy(int sx1, int sy1, int sx2, int sy2,
  //                 int dx1, int dy1, int dx2, int dy2)


  /**
   * TODO - extremely slow and not optimized.
   * Currently calls a beginPixels() on the whole canvas,
   * then does the copy, then it calls endPixels().
   */
  //public void copy(PImage src,
  //                 int sx1, int sy1, int sx2, int sy2,
  //                 int dx1, int dy1, int dx2, int dy2)



  //////////////////////////////////////////////////////////////

  // BLEND


  //static public int blendColor(int c1, int c2, int mode)


//  public void blend(PImage src,
//                    int sx, int sy, int dx, int dy, int mode) {
//    set(dx, dy, PImage.blendColor(src.get(sx, sy), get(dx, dy), mode));
//  }


  /**
   * Extremely slow and not optimized, should use GL methods instead.
   * Currently calls a beginPixels() on the whole canvas, then does the copy,
   * then it calls endPixels(). Please help fix:
   * <A HREF="http://dev.processing.org/bugs/show_bug.cgi?id=941">Bug 941</A>,
   * <A HREF="http://dev.processing.org/bugs/show_bug.cgi?id=942">Bug 942</A>.
   */
//  public void blend(int sx1, int sy1, int sx2, int sy2,
//                    int dx1, int dy1, int dx2, int dy2, int mode) {
//    loadPixels();
//    super.blend(sx1, sy1, sx2, sy2, dx1, dy1, dx2, dy2, mode);
//    updatePixels();
//  }


//  public void blend(PImage src,
//                    int sx1, int sy1, int sx2, int sy2,
//                    int dx1, int dy1, int dx2, int dy2, int mode) {
//    loadPixels();
//    super.blend(src, sx1, sy1, sx2, sy2, dx1, dy1, dx2, dy2, mode);
//    updatePixels();
//  }

  /**
   * Allows to set custom blend modes for the entire scene, using openGL.
   */  
  public void setBlend(int mode) {
    if (mode == BLEND) gl.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
    else if (mode == ADD) gl.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
    else if (mode == MULTIPLY) gl.glBlendFunc(GL11.GL_DST_COLOR, GL11.GL_SRC_COLOR);
    else if (mode == SUBTRACT) gl.glBlendFunc(GL11.GL_ONE_MINUS_DST_COLOR, GL11.GL_ZERO);
//            how to implement all these other blending modes:
//            else if (blendMode == LIGHTEST)
//            else if (blendMode == DIFFERENCE)
//            else if (blendMode == EXCLUSION)
//            else if (blendMode == SCREEN)
//            else if (blendMode == OVERLAY)
//            else if (blendMode == HARD_LIGHT)
//            else if (blendMode == SOFT_LIGHT)
//            else if (blendMode == DODGE)
//            else if (blendMode == BURN)
  }    

  /**
   * Sets Processing's default blending mode.
   */    
  public void defaultBlend() {
    gl.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);    
  }
  
  //////////////////////////////////////////////////////////////

  // SAVE


  //public void save(String filename)  // PImage calls loadPixels()


  //////////////////////////////////////////////////////////////
  
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
      parent.handleDraw();
      gl = null;        
    }

    public void onSurfaceChanged(GL10 igl, int iwidth, int iheight) {
      gl = igl;
      setSize(iwidth, iheight);
      gl = null;    	
    }

    public void onSurfaceCreated(GL10 igl, EGLConfig config) {
      gl = igl;
      recreateResources();
      gl = null;
    }    
  }
  
  //////////////////////////////////////////////////////////////
  
  // Config chooser  
  
  AndroidConfigChooser configChooser;
  
  public class AndroidConfigChooser implements EGLConfigChooser {
    
    public EGLConfig chooseConfig(EGL10 egl, EGLDisplay display) {
      
      // Specify a configuration for our opengl session
      // and grab the first configuration that matches is
      int[] configSpec = {
        EGL10.EGL_DEPTH_SIZE, 16,
        EGL10.EGL_NONE
      };      
      
      EGLConfig[] configs = new EGLConfig[1];
      int[] num_config = new int[1];      
      egl.eglChooseConfig(display, configSpec, configs, 1, num_config);
      return configs[0];
    }
  }
  
  //////////////////////////////////////////////////////////////

  // INTERNAL MATH

  private final float sqrt(float a) {
    return (float) Math.sqrt(a);
  }


//  private final float mag(float a, float b, float c) {
//    return (float) Math.sqrt(a*a + b*b + c*c);
//  }


  private final float clamp(float a) {
    return (a < 1) ? a : 1;
  }


  private final float abs(float a) {
    return (a < 0) ? -a : a;
  }


//  private float dot(float ax, float ay, float az,
//                    float bx, float by, float bz) {
//    return ax*bx + ay*by + az*bz;
//  }


//  private final void cross(float a0, float a1, float a2,
//                           float b0, float b1, float b2,
//                           PVector out) {
//    out.x = a1*b2 - a2*b1;
//    out.y = a2*b0 - a0*b2;
//    out.z = a0*b1 - a1*b0;
//  }  
}


class BufferUtil {
  static IntBuffer newIntBuffer(int big) {
    IntBuffer buffer = IntBuffer.allocate(big);
    buffer.rewind();
    return buffer;
  }  
}
