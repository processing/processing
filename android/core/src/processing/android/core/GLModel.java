package processing.android.core;

import java.io.IOException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.ArrayList;

import processing.android.xml.XMLElement;

import javax.microedition.khronos.opengles.*;

/**
 * This class holds a 3D model composed of vertices, normals, colors (per vertex) and 
 * texture coordinates (also per vertex). All this data is stored in Vertex Buffer Objects
 * (VBO) for fast access. 
 * This is class is still undergoing development, the API will probably change quickly
 * in the following months as features are tested and refined.
 * In particular, with the settings of the VBOs in this first implementation (GL.GL_DYNAMIC_DRAW_ARB)
 * it is assumed that the coordinates will change often during the lifetime of the model.
 * For static models a different VBO setting (GL.GL_STATIC_DRAW_ARB) should be used.
 */
public class GLModel implements GLConstants, PConstants {
  protected PApplet parent;    
  protected GL11 gl;  
  protected PGraphicsAndroid3D a3d;
  
  protected int numVertices;
  protected int numTextures;
  protected int glMode;
  protected int glUsage;
  protected boolean pointSprites;
  
  protected int[] glVertexBufferID;
  protected int[] glColorBufferID;
  protected int[] glTexCoordBufferID;
  protected int[] glNormalBufferID;

  protected FloatBuffer vertices;
  protected FloatBuffer colors;
  protected FloatBuffer normals;
  protected FloatBuffer[] texCoords;  
    
  protected float[] updateVertexArray;
  protected float[] updateColorArray;
  protected float[] updateNormalArray;
  protected float[] updateTexCoordArray;
  
  protected int updateElement;
  protected int firstUpdateIdx;
  protected int lastUpdateIdx;
  protected int selectedTexture;
    
  protected ArrayList<Integer> groupBreaks;
  protected ArrayList<VertexGroup> groups;
  
  protected static final int SIZEOF_FLOAT = 4;  

  
  protected float[] specularColor = {1.0f, 1.0f, 1.0f, 1.0f};
  protected float[] emissiveColor = {0.0f, 0.0f, 0.0f, 1.0f};
  protected float[] shininess = {0};  
  
  
  
  /**
   * Creates an instance of GLModel with the specified parameters: number of vertices,
   * mode to draw the vertices (as points, sprites, lines, etc) and usage (static if the
   * vertices will never change after the first time are initialized, dynamic if they will 
   * change frequently or stream if they will change at every frame).
   * @param parent PApplet
   * @param numVert int
   * @param mode int
   * @param usage int 
   */	

  public GLModel(PApplet parent, int numVert) {
    this(parent, numVert, 0); 
  }  
  
  
  public GLModel(PApplet parent, int numVert, int numTex) {
    this(parent, numVert, numTex, new GLModelParameters());
  }
  
  
  public GLModel(PApplet parent, int numVert, int numTex, GLModelParameters params) {
    this.parent = parent;
    a3d = (PGraphicsAndroid3D)parent.g;
    if (a3d.gl instanceof GL11) {
      gl = (GL11)a3d.gl;
    }
    else {
      throw new RuntimeException("GLModel: OpenGL ES 1.1 required");
    }
    
    numVertices = numVert;
    numTextures = numTex;
    
    readParameters(params);
    
    initBufferIDs();
        
    createVertexBuffer();
    createColorBuffer();
    createNormalBuffer();    
    createTexCoordBuffer();

    initGroups();

    updateVertexArray = null;
    updateColorArray = null;
    updateNormalArray = null;
    updateTexCoordArray = null;
    
    updateElement = -1;
    selectedTexture = 0;
    

    
    
    
    imageMode = a3d.imageMode;    
    tintR = a3d.tintR;
    
    tintR = tintG = tintB = tintA = 1.0f;
    shininess[0] = 0.0f;
        
    pointSize = 1.0f;
    lineWidth = 1.0f;
    usingPointSprites = false;
    blend = false;
    blendMode = ADD;
    
    float[] tmp = { 0.0f };
    gl.glGetFloatv(GL11.GL_POINT_SIZE_MAX, tmp, 0);
    maxPointSize = tmp[0];
  }
  

  protected void finalize() {
    deleteVertexBuffer();
    deleteColorBuffer();
    deleteTexCoordBuffer();
    deleteNormalBuffer();
  }
  
  
  public void selectTexture(int n) {
      if (updateElement != -1) {
        throw new RuntimeException("GLModel: cannot select texture between beginUpdate()/endUpdate()");
      }
      selectedTexture = n;
  }

  
  public void beginUpdate(int element) {
    if (updateElement != -1) {
      throw new RuntimeException("GLModel: only one element can be updated at the time");
    }
    
    updateElement = element;
    
    if (updateElement == VERTICES) {
      gl.glBindBuffer(GL11.GL_ARRAY_BUFFER, glVertexBufferID[0]);
      
      firstUpdateIdx = numVertices;
      lastUpdateIdx = -1;
      
      if (updateVertexArray == null) {
        updateVertexArray = new float[vertices.capacity()];
        vertices.get(updateVertexArray);
        vertices.rewind();
      }      
    } else if (updateElement == COLORS) {
      gl.glBindBuffer(GL11.GL_ARRAY_BUFFER, glColorBufferID[0]);
      
      firstUpdateIdx = numVertices;
      lastUpdateIdx = -1;
      
      if (updateColorArray == null) {
        updateColorArray = new float[colors.capacity()];
        colors.get(updateColorArray);
        colors.rewind();
      }
    } else if (updateElement == NORMALS) {
      gl.glBindBuffer(GL11.GL_ARRAY_BUFFER, glNormalBufferID[0]);
      
      firstUpdateIdx = numVertices;
      lastUpdateIdx = -1;
      
      if (updateNormalArray == null) {
        updateNormalArray = new float[normals.capacity()];
        normals.get(updateNormalArray);
        normals.rewind();      
      }
    } else if (updateElement == TEXTURES) {
      gl.glBindBuffer(GL11.GL_ARRAY_BUFFER, glTexCoordBufferID[selectedTexture]);
      
      firstUpdateIdx = numVertices;
      lastUpdateIdx = -1;
      
      if (updateTexCoordArray == null) {
        updateTexCoordArray = new float[texCoords[selectedTexture].capacity()];
        texCoords[selectedTexture].get(updateTexCoordArray);
        texCoords[selectedTexture].rewind();      
      }
    } else if (updateElement == GROUPS) {
      groupBreaks.clear();
    } else {
      throw new RuntimeException("GLModel: unknown element to update");  
    }
  }
  
  public void endUpdate() {
    if (updateElement == -1) {
      throw new RuntimeException("GLModel: call beginUpdate()");
    }
    
    if (lastUpdateIdx < firstUpdateIdx) return;  
    
    if (updateElement == VERTICES) {
      if (updateVertexArray == null) {
        throw new RuntimeException("GLModel: vertex array is null");    
      }
      
      int offset = firstUpdateIdx * 3;
      int size = (lastUpdateIdx - firstUpdateIdx + 1) * 3;
      
      vertices.put(updateVertexArray, offset, size);
      vertices.position(0);
    
      gl.glBufferSubData(GL11.GL_ARRAY_BUFFER, offset * SIZEOF_FLOAT, size * SIZEOF_FLOAT, vertices);
      gl.glBindBuffer(GL11.GL_ARRAY_BUFFER, 0);      
    } else if (updateElement == COLORS) {
      if (updateColorArray == null) {
        throw new RuntimeException("GLModel: color array is null");    
      }
      
      int offset = firstUpdateIdx * 4;
      int size = (lastUpdateIdx - firstUpdateIdx + 1) * 4;
            
      colors.put(updateColorArray, size, offset);
      colors.position(0);
    
      gl.glBufferSubData(GL11.GL_ARRAY_BUFFER, size * SIZEOF_FLOAT, offset * SIZEOF_FLOAT, colors);
      gl.glBindBuffer(GL11.GL_ARRAY_BUFFER, 0);
    } else if (updateElement == NORMALS) {
      if (updateNormalArray == null) {
        throw new RuntimeException("GLModel: normal array is null");    
      }
      
      int offset = firstUpdateIdx * 3;
      int size = (lastUpdateIdx - firstUpdateIdx + 1) * 3;
      
      normals.put(updateNormalArray, offset, size);
      normals.position(0);
    
      gl.glBufferSubData(GL11.GL_ARRAY_BUFFER, offset * SIZEOF_FLOAT, size * SIZEOF_FLOAT, normals);
      gl.glBindBuffer(GL11.GL_ARRAY_BUFFER, 0);
    } else if (updateElement == TEXTURES) {
      if (updateTexCoordArray == null) {
        throw new RuntimeException("GLModel: texture coordinates array is null");    
      }      
      
      int offset = firstUpdateIdx * 2;
      int size = (lastUpdateIdx - firstUpdateIdx + 1) * 2;
      
      texCoords[selectedTexture].put(updateNormalArray, offset, size);
      texCoords[selectedTexture].position(0);
      
      gl.glBufferSubData(GL11.GL_ARRAY_BUFFER, offset * SIZEOF_FLOAT, size * SIZEOF_FLOAT, texCoords[selectedTexture]);
      gl.glBindBuffer(GL11.GL_ARRAY_BUFFER, 0);      
    } else if (updateElement == GROUPS) {
      createGroups();      
    }
    
    updateElement = -1;
  }
  
  public PVector getVertex(int idx) {
    if (updateElement != VERTICES) {
      throw new RuntimeException("GLModel: update mode is not set to VERTICES");
    }

    float x = updateVertexArray[3 * idx + 0];
    float y = updateVertexArray[3 * idx + 1];
    float z = updateVertexArray[3 * idx + 2];
    
    PVector res = new PVector(x, y, z);
    return res;
  }

  public float[] getVertexArray() {
    if (updateElement != VERTICES) {
      throw new RuntimeException("GLModel: update mode is not set to VERTICES");
    }
    
    float[] res = new float[numVertices * 3];
    PApplet.arrayCopy(updateVertexArray, res);
    
    return res;
  }

  public ArrayList<PVector> getVertexArrayList() {
    if (updateElement != VERTICES) {
      throw new RuntimeException("GLModel: update mode is not set to VERTICES");
    }

    ArrayList<PVector> res;
    res = new ArrayList<PVector>();
    
    for (int i = 0; i < numVertices; i++) res.add(getVertex(i));
    
    return res;
  }  
  
  public void setVertex(int idx, float x, float y) {
    setVertex(idx, x, y, 0);  
  }
  
  public void setVertex(int idx, float x, float y, float z) {
    if (updateElement != VERTICES) {
      throw new RuntimeException("GLModel: update mode is not set to VERTICES");
    }

    if (idx < firstUpdateIdx) firstUpdateIdx = idx;
    if (firstUpdateIdx < idx) firstUpdateIdx = idx;
    
    updateVertexArray[3 * idx + 0] = x;
    updateVertexArray[3 * idx + 1] = y;
    updateVertexArray[3 * idx + 2] = z;
  }

  public void setVertex(float[] data) {
    if (updateElement != VERTICES) {
      throw new RuntimeException("GLModel: updadate mode is not set to VERTICES");
    }
    
    firstUpdateIdx = 0;
    firstUpdateIdx = numVertices;    
    PApplet.arrayCopy(data, updateVertexArray);
  }
  
  public void setVertex(ArrayList<PVector> data) {
    if (updateElement != VERTICES) {
      throw new RuntimeException("GLModel: updadate mode is not set to VERTICES");
    }

    firstUpdateIdx = 0;
    lastUpdateIdx = numVertices;
    
    PVector vec;
    for (int i = 0; i < numVertices; i++) {
      vec = (PVector)data.get(i);
      updateVertexArray[3 * i + 0] = vec.x;
      updateVertexArray[3 * i + 1] = vec.y;
      updateVertexArray[3 * i + 2] = vec.z;
    }
  }

  
  public int color(float[] rgba) {
    int r = (int)(rgba[0] * 255);
    int g = (int)(rgba[1] * 255);
    int b = (int)(rgba[2] * 255);
    int a = (int)(rgba[3] * 255);
    
    return a << 24 | r << 16 | g << 8 | b;
  }
  
  public float[] rgba(int c) {
    int a = (c >> 24) & 0xFF;
    int r = (c >> 16) & 0xFF;
    int g = (c >> 8) & 0xFF;
    int b = (c  >> 0) & 0xFF;

    float[] res = new float[4];
    res[0] = r / 255.0f;
    res[1] = g / 255.0f;
    res[2] = b / 255.0f;
    res[3] = a / 255.0f;
  }
  
  public float[] getColor(int idx) {
    if (updateElement != COLORS) {
      throw new RuntimeException("GLModel: update mode is not set to COLORS");
    }
    
    float[] res = new float[4];
    PApplet.arrayCopy(updateColorArray, idx * 4, res, 0, 4);
    
    return res;
  }
  
  
  public int[] getColorArray() {
    if (updateElement != COLORS) {
      throw new RuntimeException("GLModel: update mode is not set to COLORS");
    }
    
    int[] res = new int[numVertices];
    PApplet.arrayCopy(updateColorArray, res);
    
    return res;
  }

  public ArrayList<float[]> getColorArrayList() {
    if (updateElement != COLORS) {
      throw new RuntimeException("GLModel: update mode is not set to COLORS");
    }

    ArrayList<float[]> res;
    res = new ArrayList<float[]>();
    
    for (int i = 0; i < numVertices; i++) res.add(getColor(i));
    
    return res;
  }
  
  public void setColor(int idx, int c)
  {
    int a = (c >> 24) & 0xFF;
    int r = (c >> 16) & 0xFF;
    int g = (c >> 8) & 0xFF;
    int b = (c  >> 0) & 0xFF;
    
    setColor(idx, r / 255.0f, g / 255.0f, b / 255.0f, a / 255.0f);
  }
  
  public void setColor(int idx, float r, float g, float b) {
    setColor(idx, r, g, b, 1.0f);  
  }
  
  public void setColor(int idx, float r, float g, float b, float a) {
    if (updateElement != COLORS) {
      throw new RuntimeException("GLModel: update mode is not set to COLORS");
    }

    if (idx < firstUpdateIdx) firstUpdateIdx = idx;
    if (firstUpdateIdx < idx) firstUpdateIdx = idx;
    
    updateColorArray[4 * idx + 0] = r;
    updateColorArray[4 * idx + 1] = g;
    updateColorArray[4 * idx + 2] = b;
    updateColorArray[4 * idx + 3] = a;    
  }

  public void setColor(float[] data) {
    if (updateElement != COLORS) {
      throw new RuntimeException("GLModel: update mode is not set to COLORS");
    }
    
    firstUpdateIdx = 0;
    firstUpdateIdx = numVertices;    
    PApplet.arrayCopy(data, updateColorArray);
  }
  
  public void setColor(ArrayList<float[]> data) {
    if (updateElement != COLORS) {
      throw new RuntimeException("GLModel: update mode is not set to COLORS");
    }

    firstUpdateIdx = 0;
    lastUpdateIdx = numVertices;
    
    float[] rgba;
    for (int i = 0; i < numVertices; i++) {
      rgba = (float[])data.get(i);
      updateColorArray[4 * i + 0] = rgba[0];
      updateColorArray[4 * i + 1] = rgba[1];
      updateColorArray[4 * i + 2] = rgba[2];
      updateColorArray[4 * i + 3] = rgba[3];      
    }
  }
  
  public PVector getNormal(int idx) {
    if (updateElement != NORMALS) {
      throw new RuntimeException("GLModel: update mode is not set to NORMALS");
    }

    float x = updateNormalArray[3 * idx + 0];
    float y = updateNormalArray[3 * idx + 1];
    float z = updateNormalArray[3 * idx + 2];
    
    PVector res = new PVector(x, y, z);
    return res;
  }

  public float[] getNormalArray() {
    if (updateElement != NORMALS) {
      throw new RuntimeException("GLModel: update mode is not set to NORMALS");
    }
    
    float[] res = new float[numVertices * 3];
    PApplet.arrayCopy(updateNormalArray, res);
    
    return res;
  }

  public ArrayList<PVector> getNormalArrayList() {
    if (updateElement != NORMALS) {
      throw new RuntimeException("GLModel: update mode is not set to NORMALS");
    }

    ArrayList<PVector> res;
    res = new ArrayList<PVector>();
    
    for (int i = 0; i < numVertices; i++) res.add(getNormal(i));
    
    return res;
  }  
  
  public void setNormal(int idx, float x, float y) {
    setNormal(idx, x, y, 0);  
  }
  
  public void setNormal(int idx, float x, float y, float z) {
    if (updateElement != NORMALS) {
      throw new RuntimeException("GLModel: update mode is not set to NORMALS");
    }

    if (idx < firstUpdateIdx) firstUpdateIdx = idx;
    if (firstUpdateIdx < idx) firstUpdateIdx = idx;
    
    updateNormalArray[3 * idx + 0] = x;
    updateNormalArray[3 * idx + 1] = y;
    updateNormalArray[3 * idx + 2] = z;
  }

  public void setNormal(float[] data) {
    if (updateElement != NORMALS) {
      throw new RuntimeException("GLModel: update mode is not set to NORMALS");
    }
    
    firstUpdateIdx = 0;
    firstUpdateIdx = numVertices;    
    PApplet.arrayCopy(data, updateNormalArray);
  }
  
  public void setNormal(ArrayList<PVector> data) {
    if (updateElement != NORMALS) {
      throw new RuntimeException("GLModel: update mode is not set to NORMALS");
    }

    firstUpdateIdx = 0;
    lastUpdateIdx = numVertices;
    
    PVector vec;
    for (int i = 0; i < numVertices; i++) {
      vec = (PVector)data.get(i);
      updateNormalArray[3 * i + 0] = vec.x;
      updateNormalArray[3 * i + 1] = vec.y;
      updateNormalArray[3 * i + 2] = vec.z;
    }
  }

  

  public PVector getTexCoord(int idx) {
    if (updateElement != TEXTURES) {
      throw new RuntimeException("GLModel: update mode is not set to TEXTURES");
    }

    float s = updateTexCoordArray[2 * idx + 0];
    float t = updateTexCoordArray[2 * idx + 1];
    
    PVector res = new PVector(s, t, 0);
    return res;
  }

  public float[] getTexCoordArray() {
    if (updateElement != TEXTURES) {
      throw new RuntimeException("GLModel: update mode is not set to TEXTURES");
    }
    
    float[] res = new float[numVertices * 2];
    PApplet.arrayCopy(updateTexCoordArray, res);
    
    return res;
  }

  public ArrayList<PVector> getTexCoordArrayList() {
    if (updateElement != TEXTURES) {
      throw new RuntimeException("GLModel: update mode is not set to TEXTURES");
    }

    ArrayList<PVector> res;
    res = new ArrayList<PVector>();
    
    for (int i = 0; i < numVertices; i++) res.add(getTexCoord(i));
    
    return res;
  }  
  
  public void setTexCoord(int idx, float u, float v) {
    if (updateElement != TEXTURES) {
      throw new RuntimeException("GLModel: update mode is not set to TEXTURES");
    }

    if (idx < firstUpdateIdx) firstUpdateIdx = idx;
    if (firstUpdateIdx < idx) firstUpdateIdx = idx;
    
    updateTexCoordArray[2 * idx + 0] = u;
    updateTexCoordArray[2 * idx + 1] = v;
  }

  public void setTexCoord(float[] data) {
    if (updateElement != TEXTURES) {
      throw new RuntimeException("GLModel: update mode is not set to TEXTURES");
    }
    
    firstUpdateIdx = 0;
    firstUpdateIdx = numVertices;    
    PApplet.arrayCopy(data, updateTexCoordArray);
  }
  
  public void setTexCoord(ArrayList<PVector> data) {
    if (updateElement != TEXTURES) {
      throw new RuntimeException("GLModel: update mode is not set to TEXTURES");
    }

    firstUpdateIdx = 0;
    lastUpdateIdx = numVertices;
    
    PVector vec;
    for (int i = 0; i < numVertices; i++) {
      vec = (PVector)data.get(i);
      updateTexCoordArray[3 * i + 0] = vec.x;
      updateTexCoordArray[3 * i + 1] = vec.y;
    }
  }
  
  
  
  
  
  
  public void setGroup(int idx) {
    groupBreaks.add(new Integer(idx));
  }
  
  
  
  
  
  
  protected void initGroups() {
    groupBreaks = new ArrayList<Integer>();
    groups = new ArrayList<VertexGroup>();
    groups.add(new VertexGroup(0, numVertices - 1, numTextures));
  }
  
  
  protected void createGroups() {
      // Constructing the intervals given the "break-point" vertices.
      int idx0, idx1;
      idx0 = 0;
      Integer idx;
      groups.clear();
      for (int i = 0; i < groupBreaks.size(); i++) {
        // A group ends at idx1. So the interval is (idx0, idx1).
        idx = (Integer)groupBreaks.get(i);
        idx1 = idx.intValue();
      
        if (idx0 <= idx1) {
          groups.add(new VertexGroup(idx0, idx1, numTextures));
          idx0 = idx1 + 1;          
        }
      }
      
      idx1 = numVertices - 1;
      if (idx0 <= idx1) {
        groups.add(new VertexGroup(idx0, idx1, numTextures));
      }
  }
  
  
  
  
  protected void readParameters(GLModelParameters params) {
    pointSprites = false;
    if (params.drawMode == POINTS) glMode = GL11.GL_POINTS;
    else if (params.drawMode == POINT_SPRITES) {
      glMode = GL11.GL_POINTS;
      pointSprites = true;
      
      
      usingPointSprites = true;
      float[] tmp = { 0.0f };
      gl.glGetFloatv(GL11.GL_POINT_SIZE_MAX, tmp, 0);
      maxPointSize = tmp[0];
      pointSize = maxPointSize;
      spriteFadeSize = 0.6f * pointSize;
      
    }
    else if (params.drawMode == LINES) glMode = GL11.GL_LINES;
    else if (params.drawMode == LINE_STRIP) glMode = GL11.GL_LINE_STRIP;
    else if (params.drawMode == LINE_LOOP) glMode = GL11.GL_LINE_LOOP;
    else if (params.drawMode == TRIANGLES) glMode = GL11.GL_TRIANGLES; 
    else if (params.drawMode == TRIANGLE_FAN) glMode = GL11.GL_TRIANGLE_FAN;
    else if (params.drawMode == TRIANGLE_STRIP) glMode = GL11.GL_TRIANGLE_STRIP;
    else {
      throw new RuntimeException("GLModel: Unknown draw mode");
    }
    
    if (params.updateMode == STATIC) glUsage = GL11.GL_STATIC_DRAW;
    else if (params.updateMode == DYNAMIC) glUsage = GL11.GL_DYNAMIC_DRAW;
    else {
      throw new RuntimeException("GLModel: Unknown update mode");
    }
  }
  
  void initBufferIDs() {
    glVertexBufferID = new int[1];
    glColorBufferID = new int[1];
    glNormalBufferID = new int[1];
    glVertexBufferID[0] = glColorBufferID[0] = glNormalBufferID[0] = 0;
    glTexCoordBufferID = new int[numTextures];
    for (int i = 0; i < numTextures; i++) glTexCoordBufferID[i] = 0;    
  }
  
  protected void createVertexBuffer() {
    // Creating the float buffer to hold vertices as a direct byte buffer. Each vertex has 3 coordinates
    // and each coordinate takes SIZEOF_FLOAT bytes (one float).
    ByteBuffer vbb = ByteBuffer.allocateDirect(numVertices * 3 * SIZEOF_FLOAT);
    vbb.order(ByteOrder.nativeOrder());
    vertices = vbb.asFloatBuffer();    
    
    float[] values = new float[vertices.capacity()];
    for (int i = 0; i < values.length; i++) values[i] = 0.0f;
    vertices.put(values);
    vertices.position(0);
    
    gl.glGenBuffers(1, glVertexBufferID, 0);
    gl.glBindBuffer(GL11.GL_ARRAY_BUFFER, glVertexBufferID[0]);
    gl.glBufferData(GL11.GL_ARRAY_BUFFER, vertices.capacity(), vertices, glUsage);
    gl.glBindBuffer(GL11.GL_ARRAY_BUFFER, 0);        
  }
  
  protected void createColorBuffer() {
    ByteBuffer vbb = ByteBuffer.allocateDirect(numVertices * 4 * SIZEOF_FLOAT);
    vbb.order(ByteOrder.nativeOrder());                
    colors = vbb.asFloatBuffer();          

    float[] values = new float[colors.capacity()];
    for (int i = 0; i < values.length; i++) values[i] = 1.0f;
    colors.put(values);
    colors.position(0);
    
    gl.glGenBuffers(1, glColorBufferID, 0);
    gl.glBindBuffer(GL11.GL_ARRAY_BUFFER, glColorBufferID[0]);
    gl.glBufferData(GL11.GL_ARRAY_BUFFER, colors.capacity(), colors, glUsage);
    gl.glBindBuffer(GL11.GL_ARRAY_BUFFER, 0);
  }
  
  protected void createTexCoordBuffer() {
    float[] values = new float[numVertices * 2];
    for (int i = 0; i < values.length; i++) values[i] = 0.0f;
    
    for (int i =0; i < numTextures; i++) {
      ByteBuffer vbb = ByteBuffer.allocateDirect(numVertices * 2 * SIZEOF_FLOAT);
      vbb.order(ByteOrder.nativeOrder());
      texCoords[i] = vbb.asFloatBuffer();
      
      texCoords[i].put(values);
      texCoords[i].position(0);    
    }
    
    gl.glGenBuffers(numTextures, glTexCoordBufferID, numTextures);
    for (int i = 0; i < numTextures; i++)  {
      gl.glBindBuffer(GL11.GL_ARRAY_BUFFER, glTexCoordBufferID[i]);
      gl.glBufferData(GL11.GL_ARRAY_BUFFER, texCoords[i].capacity(), texCoords[i], glUsage);
    }
    gl.glBindBuffer(GL11.GL_ARRAY_BUFFER, 0);    
  }
  
  protected void createNormalBuffer() {
    ByteBuffer vbb = ByteBuffer.allocateDirect(numVertices * 3 * SIZEOF_FLOAT);
    vbb.order(ByteOrder.nativeOrder());
    normals = vbb.asFloatBuffer();        

    float[] values = new float[normals.capacity()];
    for (int i = 0; i < values.length; i++) values[i] = 0.0f;
    normals.put(values);
    normals.position(0);    
    
    gl.glGenBuffers(1, glNormalBufferID, 0);
    gl.glBindBuffer(GL11.GL_ARRAY_BUFFER, glNormalBufferID[0]);
    gl.glBufferData(GL11.GL_ARRAY_BUFFER, normals.capacity(), normals, glUsage);
    gl.glBindBuffer(GL11.GL_ARRAY_BUFFER, 0);
  }
  
  
  
  protected void deleteVertexBuffer() {
    if (glVertexBufferID[0] != 0) {    
      gl.glDeleteBuffers(1, glVertexBufferID, 0);
      glVertexBufferID[0] = 0;
    }
  }
  

  protected void deleteColorBuffer() {
    if (glColorBufferID[0] != 0) {    
      gl.glDeleteBuffers(1, glColorBufferID, 0);
      glColorBufferID[0] = 0;
    }
  }  

  
  protected void deleteTexCoordBuffer() {
    if (glTexCoordBufferID[0] != 0) {
      gl.glDeleteBuffers(numTextures, glTexCoordBufferID, 0);
      for (int i = 0; i < numTextures; i++) glTexCoordBufferID[i] = 0;
    }
  }

  
  protected void deleteNormalBuffer() {
    if (glNormalBufferID[0] != 0) {    
      gl.glDeleteBuffers(1, glNormalBufferID, 0);
      glNormalBufferID[0] = 0;
    }
  }  
  
  

  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
	public GLModel(PApplet parent, int numVert, int numTex,  int mode, int usage)	{
    initModelCommon(parent);
    size = numVert;
    	
    if (mode == POINTS) vertexMode = GL11.GL_POINTS;
    else if (mode == POINT_SPRITES) {
      vertexMode = GL11.GL_POINTS;
      usingPointSprites = true;
      float[] tmp = { 0.0f };
      gl.glGetFloatv(GL11.GL_POINT_SIZE_MAX, tmp, 0);
      maxPointSize = tmp[0];
      pointSize = maxPointSize;
      spriteFadeSize = 0.6f * pointSize;
    }
    else if (mode == LINES) vertexMode = GL11.GL_LINES;
    else if (mode == LINE_STRIP) vertexMode = GL11.GL_LINE_STRIP;
    else if (mode == LINE_LOOP) vertexMode = GL11.GL_LINE_LOOP;
    else if (mode == TRIANGLES) vertexMode = GL11.GL_TRIANGLES; 
    else if (mode == TRIANGLE_FAN) vertexMode = GL11.GL_TRIANGLE_FAN;
    else if (mode == TRIANGLE_STRIP) vertexMode = GL11.GL_TRIANGLE_STRIP;      

    if (usage == STATIC) vboUsage = GL11.GL_STATIC_DRAW;
    else if (usage == DYNAMIC) vboUsage = GL11.GL_DYNAMIC_DRAW;
    // else if (usage == STREAM) vboUsage = GL11.GL_STREAM_COPY; No stream mode.
        


    

        
    
    init textures
    init normals (and set to zero)
    init colors (and set to white)
    
    set one vertex group, with specified mode. 
    
    description = "Just another GLModel";
	}

	public GLModel(PApplet parent, float[] vertArray, int mode, int usage)	{
    this(parent, vertArray.length / 3, mode, usage);
    updateVertices(vertArray);
  }

	public GLModel(PApplet parent, ArrayList<PVector> vertArrayList, int mode, int usage)	{
    this(parent, vertArrayList.size(), mode, usage);
    
    updateVertices(vertArrayList);
  }	

	public GLModel(PApplet parent, String filename)	{
    initModelCommon(parent);

    filename = filename.replace('\\', '/');
    XMLElement xml = new XMLElement(parent, filename);
    	
    loadXML(xml);
  }	
	
  public GLModel(PApplet parent, URL url) {
    initModelCommon(parent);
    	
    try	{
      String xmlText = PApplet.join(PApplet.loadStrings(url.openStream()),"\n");
      XMLElement xml = new XMLElement(xmlText);
      loadXML(xml);
    } 
    catch (IOException e)	{
			System.err.println("Error loading effect: " + e.getMessage());
	  }
  }    	

  public String getDescription() {
    return description;
  }
    
  /**
   * Returns the OpenGL identifier of the Vertex Buffer Object holding the coordinates of 
   * this model.
   * @return int
   */	 
	public int getCoordsVBO() { 
	  return vertCoordsVBO[0]; 
  }
	
  /**
   * This method creates the normals, i.e.: it creates the internal OpenGL variables
   * to store normal data.
   */
	public void initNormals()	{
    normCoordsVBO = new int[1];
    gl.glGenBuffers(1, normCoordsVBO, 0);
    gl.glBindBuffer(GL11.GL_ARRAY_BUFFER, normCoordsVBO[0]);
    gl.glBufferData(GL11.GL_ARRAY_BUFFER, size * 4 * SIZEOF_FLOAT, null, vboUsage);
    gl.glBindBuffer(GL11.GL_ARRAY_BUFFER, 0);
        
    ByteBuffer vbb = ByteBuffer.allocateDirect(size * 3 * 4);
    vbb.order(ByteOrder.nativeOrder());
    normals = vbb.asFloatBuffer();    	
  }
		
  /**
   * This method creates the colors, i.e.: it creates the internal OpenGL variables
   * to store color data.
   */
  public void initColors()	{
    colorsVBO = new int[1];
    gl.glGenBuffers(1, colorsVBO, 0);
    gl.glBindBuffer(GL11.GL_ARRAY_BUFFER, colorsVBO[0]);
    gl.glBufferData(GL11.GL_ARRAY_BUFFER, size * 4 * SIZEOF_FLOAT, null, vboUsage);
    gl.glBindBuffer(GL11.GL_ARRAY_BUFFER, 0);

    ByteBuffer vbb = ByteBuffer.allocateDirect(size * 4 * 4);
    vbb.order(ByteOrder.nativeOrder());                
    colors = vbb.asFloatBuffer();      
  }
	
  /**
   * This method creates n textures, i.e.: it creates the internal OpenGL variables
   * to store n textures.
   * @param n int 
   */	
  public void initTexures(int n)	{
    numTextures = n;

    texCoordsVBO = new int[numTextures];
    textures = new GLTexture[numTextures];
    gl.glGenBuffers(numTextures, texCoordsVBO, 0);
    for (n = 0; n < numTextures; n++)  {
      gl.glBindBuffer(GL11.GL_ARRAY_BUFFER, texCoordsVBO[n]); // Bind the buffer.
      gl.glBufferData(GL11.GL_ARRAY_BUFFER, size * 2 * SIZEOF_FLOAT, null, vboUsage);
    }
    gl.glBindBuffer(GL11.GL_ARRAY_BUFFER, 0);
        
    ByteBuffer vbb = ByteBuffer.allocateDirect(size * 2 * 4);
    vbb.order(ByteOrder.nativeOrder());                
    texCoords = vbb.asFloatBuffer();        
  }
	
  /**
   * Sets the i-th texture.
   * @param i int 
   */		
	public void setTexture(int i, GLTexture tex)	{
    textures[i] = tex;
  }
	
  /**
   * Returns the number of textures.
   * @return int
   */		
  public int getNumTextures() {
    return numTextures;
  }

  /**
   * Returns the i-th texture.
   * @return GLTexture
   */			
  public GLTexture getTexture(int i)	{
    return textures[i];
  }	
	


  public void set(int idx, float x, float y) {
    set(idx, x, y, 0);	
  }

  public void set(int idx, PVector vec)  {
    set(idx, vec.x, vec.y, vec.z);  
  }

	public void set(int idx, float x, float y, float z)	{
    if (idx < firstVertIdx) firstVertIdx = idx;
    if (lastVertIdx < idx) lastVertIdx = idx;
    
    tmpVertArray[3 * idx + 0] = x;
    tmpVertArray[3 * idx + 1] = y;
    tmpVertArray[3 * idx + 2] = z;	
  }

  public PVector get(int idx) {
    PVector res = new PVector(tmpVertArray[3 * idx + 0], tmpVertArray[3 * idx + 1], tmpVertArray[3 * idx + 2]);
    return res;
  }
  
  public void set(float[] data) {
    if (data.length == 3 * size) {
        PApplet.arrayCopy(data, tmpVertArray);
        firstVertIdx = 0;
        lastVertIdx = size;
    }
  }

  public float[] get() {
    float[] res = new float[3 * size];
    PApplet.arrayCopy(tmpVertArray, res);
    return res;
  }
    
  public void set(ArrayList<PVector> data) {  
  }
  
  public ArrayList<PVector> getArrayList() {
    ArrayList<PVector> res = null;
    return res;
  }
  
  /*
cube.begin(VERTICES);
cube.group(10);
cube.group(50);
cube.end(); 
   */
  
  public void group(int[] start, int[] stop) {
    if (start.length == stop.length) {
      int defMode = POINTS;
      if (groups != null) defMode = groups[0].drawMode;
      groups = new VertexGroup[start.length];
      for (int i = 0; i < start.length; i++) {
        groups[i].start = start[i];
        groups[i].stop = stop[i];
        groups[i].drawMode = defMode;
      }
    }
  }
  
  public int getStart(int gr) {
    return groups[gr].start;
  }
  
  public int getStop(int gr) {
    return groups[gr].stop;
  }
  
  public int getMode(int gr) {
    return groups[gr].drawMode;
  }

  public GLTexture[] getTextures(int gr) {
    return groups[gr].textures;
  }

  public GLTexture getTextures(int gr, int i) {
    return groups[gr].textures[i];
  }

  public int getNumGroups() {
    return groups.length;
  }
  

  
  // Sets the color to all the vertices in group gr.
  //public void setColor(int gr, int color) {
    
  //}

  // Sets the normals to all the vertices in group gr..  
  public void setNormals(int gr, float x, float y, float z) {
    
  }
  
  
  // sets tex for all the texture units for this model and all the vertex groups
  public void setTexture(GLTexture tex) {
    
  }
  
  // sets tex for the texture unit i for this model and all the vertex groups
  public void setTexture(int i, GLTexture tex) {
    
  }

  // sets tex for the texture unit i for this model and all the vertex groups gr
  public void setTexture(int gr, int i, GLTexture tex) {
    
  }

  // sets mode for all the vertex groups
  public void setMode(int mode) {
    
  }
  
  // sets mode for vertex group gr.
  public void setMode(int gr, int mode) {
    
  }

  
  
  
  
  
  
	public void displaceVertex(int idx, float dx, float dy)	{	
    displaceVertex(idx, dx, dy, 0);
  }	
	
  public void displaceVertex(int idx, float dx, float dy, float dz) {	
    if (tmpVertArray == null) {
      tmpVertArray = new float[3 * size];
      vertices.get(tmpVertArray);
      vertices.rewind();
    }
	    
    tmpVertArray[3 * idx + 0] += dx;
    tmpVertArray[3 * idx + 1] += dy;
    tmpVertArray[3 * idx + 2] += dz;
  }
		
  public void updateVertices(float[] vertArray)	{
    beginUpdateVertices();
    vertices.put(vertArray);
    endUpdateVertices();
  }

  public void updateVertices(ArrayList<PVector> vertArrayList)	{
    if (vertArrayList.size() != size) {
      System.err.println("Wrong number of vertices in the array list.");
      return;
    }
		
    float p[] = new float [3 * size];
    for(int i = 0; i < vertArrayList.size(); i++)	{
      PVector point = (PVector)vertArrayList.get(i);
      p[3 * i + 0] = point.x;
      p[3 * i + 1] = point.y;
      p[3 * i + 2] = point.z;	
    }
    updateVertices(p);		
  }	

	
	
	
	
	
	public void beginUpdateColors()
	{
		gl.glBindBuffer(GL11.GL_ARRAY_BUFFER, colorsVBO[0]);
	}

	public void endUpdateColors()
	{
		if (tmpColorArray != null)
		{
			colors.put(tmpColorArray);
			tmpColorArray = null;
			colors.position(0);
		}
		gl.glBufferSubData(GL11.GL_ARRAY_BUFFER, 0, colors.capacity() * SIZEOF_FLOAT, colors);
	    gl.glBindBuffer(GL11.GL_ARRAY_BUFFER, 0);
	}

    /**
     * Paints the vertex i with the specified gray tone.
     * @param gray float
     */
    public void updateColor(int i, float gray) 
    {
        int c = parent.color(gray);
        putColorInTmpArray(i, c);
    }

    /**
     * Paints the vertex i with the specified gray tone and alpha value.
     * @param gray int
     * @param alpha int
     */
    public void updateColor(int i, int gray, int alpha) 
    {
        int c = parent.color(gray, alpha);
        putColorInTmpArray(i, c);
    }

    /**
     * Paints the vertex i with the specified rgb color and alpha value.
     * @param rgb int
     * @param alpha float
     */
    public void updateColor(int i, int rgb, float alpha) 
    {
        int c = parent.color(rgb, alpha);
        putColorInTmpArray(i, c);
    }

    /**
     * Paints the vertex i with the specified gray tone and alpha value.
     * @param gray float
     * @param alpha float
     */
    public void updateColor(int i, float gray, float alpha)
    {
        int c = parent.color(gray, alpha);
        putColorInTmpArray(i, c);
    }

    /**
     * Paints the vertex i with the specified color components.
     * @param x int
     * @param y int
     * @param z int
     */
    public void updateColor(int i, int x, int y, int z) 
    {
        int c = parent.color(x, y, z);
        putColorInTmpArray(i, c);
    }

    /**
     * Paints the vertex i with the specified color components.
     * @param x float
     * @param y float
     * @param z float
     */
    public void updateColor(int i, float x, float y, float z) 
    {
        int c = parent.color(x, y, z);
        putColorInTmpArray(i, c);
    }

    /**
     * Paints the vertex i with the specified color components and alpha component.
     * @param x int
     * @param y int
     * @param z int
     * @param a int
     */
    public void updateColor(int i, int x, int y, int z, int a) 
    {
        int c = parent.color(x, y, z, a);
        putColorInTmpArray(i, c);
    }

    /**
     * Paints the vertex i with the specified color components and alpha component.
     * @param x float
     * @param y float
     * @param z float
     * @param a float
     */
    public void updateColor(int i, float x, float y, float z, float a)
    {
        int c = parent.color(x, y, z, a);
        putColorInTmpArray(i, c);
    }

    /**
     * Paints all vertices with the specified gray tone.
     * @param gray float
     */
    public void setColors(float gray) 
    {
        int c = parent.color(gray);
        updateAllColors(c);
    }

    /**
     * Paints all vertices with the specified gray tone and alpha value.
     * @param gray int
     * @param alpha int
     */
    public void setColors(int gray, int alpha) 
    {
        int c = parent.color(gray, alpha);
        updateAllColors(c);
    }

    /**
     * Paints all vertices with the specified rgb color and alpha value.
     * @param rgb int
     * @param alpha float
     */
    public void setColors(int rgb, float alpha) 
    {
        int c = parent.color(rgb, alpha);
        updateAllColors(c);
    }

    /**
     * Paints all vertices with the specified gray tone and alpha value.
     * @param gray float
     * @param alpha float
     */
    public void setColors(float gray, float alpha)
    {
        int c = parent.color(gray, alpha);
        updateAllColors(c);
    }

    /**
     * Paints all vertices with the specified color components.
     * @param x int
     * @param y int
     * @param z int
     */
    public void setColors(int x, int y, int z) 
    {
        int c = parent.color(x, y, z);
        updateAllColors(c);
    }

    /**
     * Paints all vertices with the specified color components.
     * @param x float
     * @param y float
     * @param z float
     */
    public void setColors(float x, float y, float z) 
    {
        int c = parent.color(x, y, z);
        updateAllColors(c);
    }

    /**
     * Paints all vertices with the specified color components and alpha component.
     * @param x int
     * @param y int
     * @param z int
     * @param a int
     */
    public void setColors(int x, int y, int z, int a) 
    {
        int c = parent.color(x, y, z, a);
        updateAllColors(c);
    }

    /**
     * Paints all vertices with the specified color components and alpha component.
     * @param x float
     * @param y float
     * @param z float
     * @param a float
     */
    public void setColors(float x, float y, float z, float a)
    {
        int c = parent.color(x, y, z, a);
        updateAllColors(c);
    }    
    
	public void updateColors(float[] colArray)
	{
		beginUpdateColors();
		colors.put(colArray);
		endUpdateColors();
	}
	
	public void updateColors(ArrayList<float[]> colArrayList)
	{
		if (colArrayList.size() != size)
		{
            System.err.println("Wrong number of colors in the array list.");
            return;
		}

		float p[] = new float [4 * size];
		for(int i = 0; i < colArrayList.size(); i++)
		{
			float[] c = (float[])colArrayList.get(i);
					    
			if (c.length == 4)
			{
			    p[4 * i + 0] = c[0];
			    p[4 * i + 1] = c[1];
			    p[4 * i + 2] = c[2];
			    p[4 * i + 3] = c[3];
			}
		}
		updateColors(p);
	}	
	
	public void beginUpdateTexCoords(int n)
	{
		gl.glBindBuffer(GL11.GL_ARRAY_BUFFER, texCoordsVBO[n]);
	}
	
	public void updateTexCoord(int idx, float s, float t)
	{
	    if (tmpTexCoordsArray == null) {
	    	tmpTexCoordsArray = new float[2 * size];
	    	texCoords.get(tmpTexCoordsArray);
	    	texCoords.rewind();	    	
	    }
	    
	    tmpTexCoordsArray[2 * idx + 0] = s;
	    tmpTexCoordsArray[2 * idx + 1] = t;
	}
	
	public void displaceTexCoord(int idx, float ds, float dt)
	{
	    if (tmpTexCoordsArray == null)
	    {
	    	tmpTexCoordsArray = new float[2 * size];
	    	texCoords.get(tmpTexCoordsArray);
	    	texCoords.rewind();
	    }
	    
	    tmpTexCoordsArray[2 * idx + 0] += ds;
	    tmpTexCoordsArray[2 * idx + 1] += dt;
	}	
	
	public void endUpdateTexCoords()
	{
		if (tmpTexCoordsArray != null)
		{
			texCoords.put(tmpTexCoordsArray);
			tmpTexCoordsArray = null;
			texCoords.position(0);
		}
		gl.glBufferSubData(GL11.GL_ARRAY_BUFFER, 0, texCoords.capacity() * SIZEOF_FLOAT, texCoords);
	    gl.glBindBuffer(GL11.GL_ARRAY_BUFFER, 0);
	}

	public void updateTexCoords(int n, float[] texCoordsArray)
	{
		beginUpdateTexCoords(n);
		texCoords.put(texCoordsArray);
		endUpdateTexCoords();
	}

	public void updateTexCoords(int n, ArrayList<PVector> texCoordsArrayList)
	{
		if (texCoordsArrayList.size() != size)
		{
            System.err.println("Wrong number of texture coordinates in the array list.");
            return;
		}
		
		float p[] = new float [2 * size];
		for(int i = 0; i < texCoordsArrayList.size(); i++)
		{
		    PVector point = (PVector)texCoordsArrayList.get(i);
			p[2 * i + 0] = point.x;
			p[2 * i + 1] = point.y;		
		}
		updateTexCoords(n, p);		
	}	
	
	public void beginUpdateNormals()
	{
		gl.glBindBuffer(GL11.GL_ARRAY_BUFFER, normCoordsVBO[0]);
	}
	
	public void endUpdateNormals()
	{
		if (tmpNormalsArray != null)
		{
			normals.put(tmpNormalsArray);
			tmpNormalsArray = null;
			normals.position(0);
		}		
		gl.glBufferSubData(GL11.GL_ARRAY_BUFFER, 0, normals.capacity() * SIZEOF_FLOAT, normals);
	    gl.glBindBuffer(GL11.GL_ARRAY_BUFFER, 0);
	}

	public void updateNormal(int idx, float x, float y)
	{
		updateNormal(idx, x, y, 0);	
	}
	
	public void updateNormal(int idx, float x, float y, float z)
	{
	    if (tmpNormalsArray == null){
	    	tmpNormalsArray = new float[3 * size];
	    	normals.get(tmpNormalsArray);
	    	normals.rewind();
	    }
	    
	    tmpNormalsArray[3 * idx + 0] = x;
	    tmpNormalsArray[3 * idx + 1] = y;
	    tmpNormalsArray[3 * idx + 2] = z;	
	}

	public void displaceNormal(int idx, float dx, float dy)
	{	
		displaceNormal(idx, dx, dy, 0);
	}	
	
	public void displaceNormal(int idx, float dx, float dy, float dz)
	{	
	    if (tmpNormalsArray == null)
	    {
	    	tmpNormalsArray = new float[3 * size];
	    	normals.get(tmpNormalsArray);
	    	normals.rewind();
	    }
	    
	    tmpNormalsArray[3 * idx + 0] += dx;
	    tmpNormalsArray[3 * idx + 1] += dy;
	    tmpNormalsArray[3 * idx + 2] += dz;
	}
	
	public void updateNormals(float[] normArray)
	{
		beginUpdateNormals();
		normals.put(normArray);
		endUpdateNormals();
	}

	public void updateNormals(ArrayList<PVector> normArrayList)
	{
		if (normArrayList.size() != size)
		{
            System.err.println("Wrong number of normals in the array list.");
            return;
		}
		
		float p[] = new float [3 * size];
		for(int i = 0; i < normArrayList.size(); i++)
		{
		    PVector point = (PVector)normArrayList.get(i);
			p[3 * i + 0] = point.x;
			p[3 * i + 1] = point.y;
			p[3 * i + 2] = point.z;
		}
		updateNormals(p);
	}	
	
	/*
	public void beginUpdateAttributes(int n)
	{
		gl.glBindBuffer(GL11.GL_ARRAY_BUFFER, attribVBO[n]);
		// attributes = gl.glMapBuffer(GL11.GL_ARRAY_BUFFER, GL11.GL_WRITE_ONLY).asFloatBuffer(); // No Map/Unmap buffer funtions! What to do??
		curtAttrSize = attribSize[n];
	}
	
	public void updateAttribute(int idx, float x)
	{
        updateAttribute(idx, new float[]{ x });
	}
	
	public void updateAttribute(int idx, float x, float y)
	{
        updateAttribute(idx, new float[]{ x, y });
	}

	public void updateAttribute(int idx, float x, float y, float z)
	{
        updateAttribute(idx, new float[]{ x, y, z });
	}

	public void updateAttribute(int idx, float x, float y, float z, float w)
	{
        updateAttribute(idx, new float[]{ x, y, z, w });
	}	
	
	public void updateAttribute(int idx, float[] values)
	{
		if (values.length == curtAttrSize)
		{
		    if (tmpAttributesArray == null) tmpAttributesArray = new float[curtAttrSize * size];

		    for (int i = 0; i < curtAttrSize; i++) tmpAttributesArray[curtAttrSize * idx + i] = values[i];		    
		}
	}
	
	public void displaceAttribute(int idx, float dx)
	{
		displaceAttribute(idx, new float[]{ dx });
	}
	
	public void displaceAttribute(int idx, float dx, float dy)
	{
		displaceAttribute(idx, new float[]{ dx, dy });
	}

	public void displaceAttribute(int idx, float x, float y, float z)
	{
		displaceAttribute(idx, new float[]{ x, y, z });
	}

	public void displaceAttribute(int idx, float x, float y, float z, float w)
	{
		displaceAttribute(idx, new float[]{ x, y, z, w });
	}	
	
	public void displaceAttribute(int idx, float[] dvalues)
	{
		int l = attribSize[idx];
		if (dvalues.length == l)
		{
		    if (tmpAttributesArray == null) tmpAttributesArray = new float[l * size];
		
		    for (int i = 0; i < l; i++) tmpAttributesArray[l * idx + i] += dvalues[i];		    
		}
	}
	
	public void endUpdateAttributes()
	{
		if (tmpAttributesArray != null)
		{
			attributes.put(tmpAttributesArray);
			tmpAttributesArray = null;
		}
		// gl.glUnmapBuffer(GL11.GL_ARRAY_BUFFER); // No Map/Unmap buffer funtions! What to do??
	    gl.glBindBuffer(GL11.GL_ARRAY_BUFFER, 0);
	}

	public void updateAttributes(int n, float[] attributesArray)
	{
		beginUpdateAttributes(n);
		attributes.put(attributesArray);
		endUpdateAttributes();
	}
	
	public void updateAttributes(int n, ArrayList<float[]> vertAttribsArrayList)
	{
		if (vertAttribsArrayList.size() != size)
		{
            System.err.println("Wrong number of vertex attributes in the array list.");
            return;
		}
		
		int l = attribSize[n];
		float p[] = new float [l * size];
		for(int i = 0; i < vertAttribsArrayList.size(); i++)
		{
		    float[] attrib = (float[])vertAttribsArrayList.get(i);
		    
		    for (int j = 0; j < l; j++) p[l * i + j] = attrib[j]; 
		}
		updateAttributes(n, p);		
	}
	*/	
	
	public void setLineWidth(float w)
	{
    	lineWidth = w;
    }

	public void setPointSize(float s)
	{
    	pointSize = s;
    }

	public float getMaxPointSize()
	{
    	return maxPointSize;
    }

	public void setSpriteFadeSize(float s)
	{
		spriteFadeSize = s;
    }	
	
    /**
     * Disables blending.
     */    
    public void noBlend()
    {
        blend = false;
    }	
	
    /**
     * Enables blending and sets the mode.
     * @param MODE int
     */    
    public void setBlendMode(int MODE)
    {
        blend = true;
        blendMode = MODE;
    }
    
    /**
     * Set the tint color to the specified gray tone.
     * @param gray float
     */
    public void setTint(float gray) 
    {
        int c = parent.color(gray);
        setTintColor(c);
    }

    /**
     * Set the tint color to the specified gray tone and alpha value.
     * @param gray int
     * @param alpha int
     */
    public void setTint(int gray, int alpha) 
    {
        int c = parent.color(gray, alpha);
        setTintColor(c);
    }

    /**
     * Set the tint color to the specified rgb color and alpha value.
     * @param rgb int
     * @param alpha float
     */
    public void setTint(int rgb, float alpha) 
    {
        int c = parent.color(rgb, alpha);
        setTintColor(c);
    }

    /**
     * Set the tint color to the specified gray tone and alpha value.
     * @param gray float
     * @param alpha float
     */
    public void setTint(float gray, float alpha)
    {
        int c = parent.color(gray, alpha);
        setTintColor(c);
    }

    /**
     * Set the tint color to the specified color components.
     * @param x int
     * @param y int
     * @param z int
     */
    public void setTint(int x, int y, int z) 
    {
        int c = parent.color(x, y, z);
        setTintColor(c);
    }

    /**
     * Set the tint color to the specified color components.
     * @param x float
     * @param y float
     * @param z float
     */
    public void setTint(float x, float y, float z) 
    {
        int c = parent.color(x, y, z);
        setTintColor(c);
    }

    /**
     * Set the tint color to the specified color components and alpha component.
     * @param x int
     * @param y int
     * @param z int
     * @param a int
     */
    public void setTint(int x, int y, int z, int a) 
    {
        int c = parent.color(x, y, z, a);
        setTintColor(c);
    }

    /**
     * Set the tint color to the specified color components and alpha component.
     * @param x float
     * @param y float
     * @param z float
     * @param a float
     */
    public void setTint(float x, float y, float z, float a)
    {
        int c = parent.color(x, y, z, a);
        setTintColor(c);
    }    
    
	protected void setTintColor(int color)
	{
        int ir, ig, ib, ia;

        ia = (color >> 24) & 0xff;
        ir = (color >> 16) & 0xff;
        ig = (color >> 8) & 0xff;
        ib = color & 0xff; 

        tintA = ia / 255.0f;
        tintR = ir / 255.0f;
        tintG = ig / 255.0f;
        tintB = ib / 255.0f;
	}

    /**
     * Set the specular color to the specified gray tone.
     * @param gray float
     */
    public void setReflection(float gray) 
    {
        int c = parent.color(gray);
        setSpecularColor(c);
    }

    /**
     * Set the specular color to the specified gray tone and alpha value.
     * @param gray int
     * @param alpha int
     */
    public void setReflection(int gray, int alpha) 
    {
        int c = parent.color(gray, alpha);
        setSpecularColor(c);
    }

    /**
     * Set the specular color to the specified rgb color and alpha value.
     * @param rgb int
     * @param alpha float
     */
    public void setReflection(int rgb, float alpha) 
    {
        int c = parent.color(rgb, alpha);
        setSpecularColor(c);
    }

    /**
     * Set the specular color to the specified gray tone and alpha value.
     * @param gray float
     * @param alpha float
     */
    public void setReflection(float gray, float alpha)
    {
        int c = parent.color(gray, alpha);
        setSpecularColor(c);
    }

    /**
     * Set the specular color to the specified color components.
     * @param x int
     * @param y int
     * @param z int
     */
    public void setReflection(int x, int y, int z) 
    {
        int c = parent.color(x, y, z);
        setSpecularColor(c);
    }

    /**
     * Set the specular color to the specified color components.
     * @param x float
     * @param y float
     * @param z float
     */
    public void setReflection(float x, float y, float z) 
    {
        int c = parent.color(x, y, z);
        setSpecularColor(c);
    }

    /**
     * Set the specular color to the specified color components and alpha component.
     * @param x int
     * @param y int
     * @param z int
     * @param a int
     */
    public void setReflection(int x, int y, int z, int a) 
    {
        int c = parent.color(x, y, z, a);
        setSpecularColor(c);
    }

    /**
     * Set the specular color to the specified color components and alpha component.
     * @param x float
     * @param y float
     * @param z float
     * @param a float
     */
    public void setReflection(float x, float y, float z, float a)
    {
        int c = parent.color(x, y, z, a);
        setSpecularColor(c);
    }    
    
	protected void setSpecularColor(int color)
	{
        int ir, ig, ib, ia;

        ia = (color >> 24) & 0xff;
        ir = (color >> 16) & 0xff;
        ig = (color >> 8) & 0xff;
        ib = color & 0xff; 

        specularColor[0] = ir / 255.0f;
        specularColor[1] = ig / 255.0f;
        specularColor[2] = ib / 255.0f;
        specularColor[3] = ia / 255.0f;
	}
	
    /**
     * Set the emissive color to the specified gray tone.
     * @param gray float
     */
    public void setEmission(float gray) 
    {
        int c = parent.color(gray);
        setEmissiveColor(c);
    }

    /**
     * Set the emissive color to the specified gray tone and alpha value.
     * @param gray int
     * @param alpha int
     */
    public void setEmission(int gray, int alpha) 
    {
        int c = parent.color(gray, alpha);
        setEmissiveColor(c);
    }

    /**
     * Set the emissive color to the specified rgb color and alpha value.
     * @param rgb int
     * @param alpha float
     */
    public void setEmission(int rgb, float alpha) 
    {
        int c = parent.color(rgb, alpha);
        setEmissiveColor(c);
    }

    /**
     * Set the emissive color to the specified gray tone and alpha value.
     * @param gray float
     * @param alpha float
     */
    public void setEmission(float gray, float alpha)
    {
        int c = parent.color(gray, alpha);
        setEmissiveColor(c);
    }

    /**
     * Set the emissive color to the specified color components.
     * @param x int
     * @param y int
     * @param z int
     */
    public void setEmission(int x, int y, int z) 
    {
        int c = parent.color(x, y, z);
        setEmissiveColor(c);
    }

    /**
     * Set the emissive color to the specified color components.
     * @param x float
     * @param y float
     * @param z float
     */
    public void setEmission(float x, float y, float z) 
    {
        int c = parent.color(x, y, z);
        setEmissiveColor(c);
    }

    /**
     * Set the emissive color to the specified color components and alpha component.
     * @param x int
     * @param y int
     * @param z int
     * @param a int
     */
    public void setEmission(int x, int y, int z, int a) 
    {
        int c = parent.color(x, y, z, a);
        setEmissiveColor(c);
    }

    /**
     * Set the emissive color to the specified color components and alpha component.
     * @param x float
     * @param y float
     * @param z float
     * @param a float
     */
    public void setEmission(float x, float y, float z, float a)
    {
        int c = parent.color(x, y, z, a);
        setEmissiveColor(c);
    }    
    
	protected void setEmissiveColor(int color)
	{
        int ir, ig, ib, ia;

        ia = (color >> 24) & 0xff;
        ir = (color >> 16) & 0xff;
        ig = (color >> 8) & 0xff;
        ib = color & 0xff; 

        emissiveColor[0] = ir / 255.0f;
        emissiveColor[1] = ig / 255.0f;
        emissiveColor[2] = ib / 255.0f;
        emissiveColor[3] = ia / 255.0f;
	}
	
	public void render()
	{
	    render(0, size - 1);
	}

	/*
	public void render(GLModelEffect effect)
	{
	    render(0, size - 1, effect);
	}	
	
	public void render(int first, int last)
	{
	    render(0, size - 1, null);		
	}
	*/
	
	public void render(int first, int last /*, GLModelEffect effect*/)
	{
		if (colorsVBO == null) gl.glColor4f(tintR, tintG, tintB, tintA);
		 
		// Setting specular and emissive colors.
	    gl.glMaterialfv(GL11.GL_FRONT_AND_BACK, GL11.GL_SPECULAR, specularColor, 0);
	    gl.glMaterialfv(GL11.GL_FRONT_AND_BACK, GL11.GL_SHININESS, shininess, 0);
	    gl.glMaterialfv(GL11.GL_FRONT_AND_BACK, GL11.GL_EMISSION, emissiveColor, 0);
	    	    
	    gl.glLineWidth(lineWidth);
	    if (usingPointSprites) gl.glPointSize(PApplet.min(pointSize, maxPointSize));
	    else gl.glPointSize(pointSize);
	    
        //if (effect != null) effect.start();
	    
	    // Setting-up blending.
	    blend0 = gl.glIsEnabled(GL11.GL_BLEND);
        if (blend) 
        {
            gl.glEnable(GL11.GL_BLEND);
            if (blendMode == BLEND) gl.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
            else if (blendMode == ADD) gl.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
            else if (blendMode == MULTIPLY) gl.glBlendFunc(GL11.GL_DST_COLOR, GL11.GL_SRC_COLOR);
            else if (blendMode == SUBTRACT) gl.glBlendFunc(GL11.GL_ONE_MINUS_DST_COLOR, GL11.GL_ZERO);
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
        
	    if (normCoordsVBO != null)
	    {
	    	gl.glEnableClientState(GL11.GL_NORMAL_ARRAY);
            gl.glBindBuffer(GL11.GL_ARRAY_BUFFER, normCoordsVBO[0]);
            gl.glNormalPointer(GL11.GL_FLOAT, 0, 0);
	    }
	    	    
	    if (colorsVBO != null)
	    {
	    	gl.glEnableClientState(GL11.GL_COLOR_ARRAY);
            gl.glBindBuffer(GL11.GL_ARRAY_BUFFER, colorsVBO[0]);
	        gl.glColorPointer(4, GL11.GL_FLOAT, 0, 0);
	    }

	    if (texCoordsVBO != null)
	    {
	    	gl.glEnable(textures[0].getTextureTarget());

            // Binding texture units.
            for (int n = 0; n < numTextures; n++)
            {
            	gl.glActiveTexture(GL11.GL_TEXTURE0 + n);
                gl.glBindTexture(GL11.GL_TEXTURE_2D, textures[n].getTextureID()); 
            }	    	
	    	
            if (usingPointSprites)
            {
            	// Texturing with point sprites.
            	
                // This is how will our point sprite's size will be modified by 
                // distance from the viewer            	
            	float quadratic[] = {1.0f, 0.0f, 0.01f, 1};
                ByteBuffer temp = ByteBuffer.allocateDirect(16);
                temp.order(ByteOrder.nativeOrder());            	
                gl.glPointParameterfv(GL11.GL_POINT_DISTANCE_ATTENUATION, (FloatBuffer) temp.asFloatBuffer().put(quadratic).flip());
                                
                // The alpha of a point is calculated to allow the fading of points 
                // instead of shrinking them past a defined threshold size. The threshold 
                // is defined by GL_POINT_FADE_THRESHOLD_SIZE_ARB and is not clamped to 
                // the minimum and maximum point sizes.
                gl.glPointParameterf(GL11.GL_POINT_FADE_THRESHOLD_SIZE, spriteFadeSize);
                gl.glPointParameterf(GL11.GL_POINT_SIZE_MIN, 1.0f);
                gl.glPointParameterf(GL11.GL_POINT_SIZE_MAX, maxPointSize);

                // Specify point sprite texture coordinate replacement mode for each 
                // texture unit
                gl.glTexEnvf(GL11.GL_POINT_SPRITE_OES, GL11.GL_COORD_REPLACE_OES, GL11.GL_TRUE);

                gl.glEnable(GL11.GL_POINT_SPRITE_OES);
            }
            else
            {
            	// Regular texturing.
                gl.glEnableClientState(GL11.GL_TEXTURE_COORD_ARRAY);
                for (int n = 0; n < numTextures; n++)
                {
                    gl.glClientActiveTexture(GL11.GL_TEXTURE0 + n);
                    gl.glBindBuffer(GL11.GL_ARRAY_BUFFER, texCoordsVBO[n]);
                    gl.glTexCoordPointer(2, GL11.GL_FLOAT, 0, 0);
                }
            }
            
            //if (effect != null) effect.setTextures(textures);         
	    }	    
	    
	    // Drawing the vertices:
	    gl.glEnableClientState(GL11.GL_VERTEX_ARRAY);
	    	    
	    gl.glBindBuffer(GL11.GL_ARRAY_BUFFER, vertCoordsVBO[0]);
	    
	    gl.glVertexPointer(3, GL11.GL_FLOAT, 0, 0);
	    	   
	    // Last transformation: inversion of coordinate to make comaptible with Processing's inverted Y axis.
		gl.glPushMatrix();
		gl.glScalef(1, -1, 1);	   
	    gl.glDrawArrays(vertexMode, first, last - first + 1);
	    gl.glPopMatrix();	    
	    
	    //if (effect != null) effect.disableVertexAttribs();
	    
	    gl.glBindBuffer(GL11.GL_ARRAY_BUFFER, 0);
	    gl.glDisableClientState(GL11.GL_VERTEX_ARRAY);
	    	    
	    if (texCoordsVBO != null) 
	    {	
	    	if (usingPointSprites)
	    	{
	    		gl.glDisable(GL11.GL_POINT_SPRITE_OES);
	    	}
	    	else
	    	{
	    	    gl.glDisableClientState(GL11.GL_TEXTURE_COORD_ARRAY);
	    	}
	    	gl.glDisable(textures[0].getTextureTarget());
	    }
	    if (colorsVBO != null) gl.glDisableClientState(GL11.GL_COLOR_ARRAY);
	    if (normCoordsVBO != null) gl.glDisableClientState(GL11.GL_NORMAL_ARRAY);
	    
        // If there was noblending originally
        if (!blend0 && blend) gl.glDisable(GL11.GL_BLEND);
        // Default blending mode in PGraphicsAndroid3D.
        gl.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);        
        
        //if (effect != null) effect.stop();
	}	

    /**
     * Sets the positions corresponding to vertex i in the tmpColorArray to the specified color.
     * @param i int
     * @param color int
     */
    protected void putColorInTmpArray(int i, int color)
    {
        int ir, ig, ib, ia;
        
	    if (tmpColorArray == null) {
	    	tmpColorArray = new float[4 * size];
	    	colors.get(tmpColorArray);
	    	colors.rewind();
	    }

        ia = (color >> 24) & 0xff;
        ir = (color >> 16) & 0xff;
        ig = (color >> 8) & 0xff;
        ib = color & 0xff; 

        tmpColorArray[4 * i + 0] = ir / 255.0f;
        tmpColorArray[4 * i + 1] = ig / 255.0f;
        tmpColorArray[4 * i + 2] = ib / 255.0f;
        tmpColorArray[4 * i + 3] = ia / 255.0f; 
    }

    /**
     * Sets all the vertices with the specified color.
     * @param color int
     */    
    protected void updateAllColors(int color)
    {
        int ir, ig, ib, ia;
        float fr, fg, fb, fa;
        
        beginUpdateColors();        
	    if (tmpColorArray == null) {
	    	tmpColorArray = new float[4 * size];
	    	colors.get(tmpColorArray);
	    	colors.rewind();	    	
	    }

        ia = (color >> 24) & 0xff;
        ir = (color >> 16) & 0xff;
        ig = (color >> 8) & 0xff;
        ib = color & 0xff; 

        fa = ia / 255.0f;
        fr = ir / 255.0f;
        fg = ig / 255.0f;
        fb = ib / 255.0f;
        
        for (int i = 0; i < size; i++)
        {
            tmpColorArray[4 * i + 0] = fr;
            tmpColorArray[4 * i + 1] = fg;
            tmpColorArray[4 * i + 2] = fb;
            tmpColorArray[4 * i + 3] = fa;
        }
        endUpdateColors();
    }
    
  protected void initModelCommon(PApplet parent) {
	  this.parent = parent;		
    a3d = (PGraphicsAndroid3D)parent.g; 
    imageMode = a3d.imageMode;        
    if (a3d.gl instanceof GL11) {
      gl = (GL11)a3d.gl;
    }
    else {
      throw new RuntimeException("GLModel requires OpenGL ES 1.1");
    }
        
    tintR = tintG = tintB = tintA = 1.0f;
    shininess[0] = 0.0f;
        
    pointSize = 1.0f;
    lineWidth = 1.0f;
   	usingPointSprites = false;        
   	blend = false;
   	blendMode = ADD;
    	
   	tmpVertArray = null;
   	tmpColorArray = null;
   	tmpNormalsArray = null;
   	tmpTexCoordsArray = null;
   	//tmpAttributesArray = null;
   	
	
  }

	protected void loadXML(XMLElement xml)
	{
        int n = xml.getChildCount();
        String name, content;
        XMLElement child;
        
        GLTexture[] texturesList;
        ArrayList<PVector> verticesList;
        ArrayList<PVector>[] texCoordsList;
        //ArrayList<float[]>[] vertexAttribsList;
        ArrayList<PVector> normalsList;
        ArrayList<float[]> colorsList;
        String[] texNames;
        //String[] attrNames;
        //int[] attrSizes;
        
        texturesList = null; 
        verticesList = new ArrayList<PVector>();
        texCoordsList = null;
        //vertexAttribsList = null;        
        normalsList = new ArrayList<PVector>();
        colorsList = new ArrayList<float[]>(); 
        texNames = null;
        //attrNames = null;
        //attrSizes = null;
        
        for (int i = 0; i < n; i++)
        {
            child = xml.getChild(i);
            name = child.getName();
            if (name.equals("description"))
            {
                description = child.getContent();
            }
            else if (name.equals("size"))
            {
            	size = PApplet.parseInt(child.getContent());
            }
            else if (name.equals("geometry"))
            {
            	content = child.getContent();
            	vertexMode = GLUtils.parsePrimitiveTypeUpperCase(content);
            	if (vertexMode == GL11.GL_POINTS && content.equals("POINT_SPRITES"))
            	{
                    vertexMode = GL11.GL_POINTS;
                    usingPointSprites = true;
                    float[] tmp = { 0.0f };
                    gl.glGetFloatv(GL11.GL_POINT_SIZE_MAX, tmp, 0);
                    maxPointSize = tmp[0];
                    pointSize = maxPointSize;
                    spriteFadeSize = 0.6f * pointSize;
            	}
            }
            else if (name.equals("mode"))
            {
            	vboUsage = GLUtils.parseVBOMode(child.getContent());
            }
            else if (name.equals("textures"))
            {
            	int ntex = child.getChildCount();
                texturesList = new GLTexture[ntex];
                texNames = new String[ntex];
                texCoordsList = new ArrayList[ntex];
            	
                loadTextures(child, texturesList, texCoordsList, texNames);
            }
            /*
            else if (name.equals("vertexattribs"))
            {
                int nattr = child.getChildCount();
        		
                vertexAttribsList = new ArrayList[nattr];
                attrNames = new String[nattr];
                attrSizes = new int[nattr];
                
            	loadVertexAttribs(child, vertexAttribsList, attrNames, attrSizes);
            }
            */
            else if (name.equals("vertices"))
            {
            	loadVertices(child, verticesList);
            }
            else if (name.equals("texcoords"))
            {
            	if (texCoordsList != null)
            	{
                    int unit = child.getIntAttribute("unit");
                    if (texCoordsList[unit] != null)
                    {
            	        loadTexCoords(child, texCoordsList[unit]);
                    }            	    
            	}
            }              
            else if (name.equals("colors"))
            {
            	loadColors(child, colorsList);
            } 
            else if (name.equals("normals"))
            {
            	loadNormals(child, normalsList);
            }
            /*
            else if (name.equals("attribs"))
            {
                if (vertexAttribsList != null && attrSizes != null)
                {
                    int num = child.getIntAttribute("number");
                    if (vertexAttribsList[num] != null)
            	        loadVertexAttrib(child, vertexAttribsList[num], attrSizes[num]);
                }
            }
            */            
        }

        gl.glGenBuffers(1, vertCoordsVBO, 0);
        gl.glBindBuffer(GL11.GL_ARRAY_BUFFER, vertCoordsVBO[0]);
        gl.glBufferData(GL11.GL_ARRAY_BUFFER, size * 4 * SIZEOF_FLOAT, null, vboUsage);
        gl.glBindBuffer(GL11.GL_ARRAY_BUFFER, 0);        
        
        updateVertices(verticesList);
        
        int ntex = texturesList.length;
        if (0 < ntex)
        {
            initTexures(ntex);
        	GLTexture tex;
        	ArrayList<PVector> tcoords;
            
        	for (int j = 0; j < ntex; j++)
        	{
                tex = texturesList[j];
                setTexture(j, tex);
                // It should be something like:
                // setTexture(j, tex, texNames[j]);
                // but texture names are still not used.
                
                tcoords = (ArrayList<PVector>)texCoordsList[j];
                if (tcoords.size() == size) updateTexCoords(j, tcoords);                
            }
        }        
        
        if (normalsList.size() == size)
        {
            initNormals();
            updateNormals(normalsList);
        }

        if (colorsList.size() == size)
        {            
            initColors();
            updateColors(colorsList);
        }

        /*
        int nattr = vertexAttribsList.length;
        if (0 < nattr)
        {
        	initAttributes(nattr);
        	ArrayList<float[]> attribs;
        	
        	for (int j = 0; j < nattr; j++)
        	{
        		setAttribute(j, attrNames[j], attrSizes[j]);
        		
        		attribs = vertexAttribsList[j];
        		updateAttributes(j, attribs);
            }
        }
        */
	}
	
	protected void loadTextures(XMLElement xml, GLTexture[] texturesList, ArrayList<PVector>[] texCoordsList, String[] texNames)
	{
        int n = xml.getChildCount();
        XMLElement child;
        String name;
        
        String unitStr, fn;
        int unit;
        for (int i = 0; i < n; i++)
        {
            child = xml.getChild(i);
            name = child.getName();            
        	if (name.equals("texture")) 
        	{		
        		unitStr = child.getContent();
        		unit = PApplet.parseInt(unitStr);
        		
        		texCoordsList[unit] = new ArrayList<PVector>();
        		        		
        		texNames[unit] = child.getStringAttribute("name");
                fn = child.getStringAttribute("file");
                texturesList[unit] = new GLTexture(parent, fn);
        	}
        }
	}
	
	protected void loadVertexAttribs(XMLElement xml, ArrayList<float[]>[] vertexAttribsList, String[] attrNames, int[] attrSizes)
	{
        int n = xml.getChildCount();
        XMLElement child;
        String name;	
        
        String numStr;
        int num;
        for (int i = 0; i < n; i++)
        {
            child = xml.getChild(i);
            name = child.getName();
        	if (name.equals("vertexattrib"))
        	{
        		numStr = child.getContent();
        		num = PApplet.parseInt(numStr);
        		
        		vertexAttribsList[num] = new ArrayList<float[]>();   		
        		
        		attrNames[num] = child.getStringAttribute("name");
        		attrSizes[num] = child.getIntAttribute("size");        		
        	}
        }
	}
	
    protected void loadVertices(XMLElement xml, ArrayList<PVector> verticesList)
    {
        int n = xml.getChildCount();
        XMLElement child;
        String name;
        
        String coordStr;
        float[] coord;
        for (int i = 0; i < n; i++)
        {
            child = xml.getChild(i);
            name = child.getName();
        	if (name.equals("vertex"))
        	{
        		coordStr = child.getContent();
        		coord = PApplet.parseFloat(PApplet.split(coordStr, ' '));
        		
        		if (coord.length == 3) verticesList.add(new PVector(coord[0], coord[1], coord[2]));
        	}        	
        }
    }
    
    protected void loadTexCoords(XMLElement xml, ArrayList<PVector> texCoordsList)
    {
        int n = xml.getChildCount();
        XMLElement child;
        String name;
        
        String coordStr;
        float[] coord;
        for (int i = 0; i < n; i++)
        {
            child = xml.getChild(i);
            name = child.getName();
        	if (name.equals("texcoord"))
        	{
        		coordStr = child.getContent();
        		coord = PApplet.parseFloat(PApplet.split(coordStr, ' '));
        		
        		if (coord.length == 2) texCoordsList.add(new PVector(coord[0], coord[1]));
        	}        	
        }
    }
     
    protected void loadColors(XMLElement xml, ArrayList<float[]> colorsList)
    {
        int n = xml.getChildCount();
        XMLElement child;
        String name;
        
        String coordStr;
        float[] coord;
        for (int i = 0; i < n; i++)
        {
            child = xml.getChild(i);
            name = child.getName();
        	if (name.equals("color"))
        	{
        		coordStr = child.getContent();
        		coord = PApplet.parseFloat(PApplet.split(coordStr, ' '));
        		
        		if (coord.length == 4) colorsList.add(coord);
        	}        	
        }    	
    } 

    protected void loadNormals(XMLElement xml, ArrayList<PVector> normalsList)
    {
        int n = xml.getChildCount();
        XMLElement child;
        String name;
        
        String coordStr;
        float[] coord;
        for (int i = 0; i < n; i++)
        {
            child = xml.getChild(i);
            name = child.getName();
        	if (name.equals("normal"))
        	{
        		coordStr = child.getContent();
        		coord = PApplet.parseFloat(PApplet.split(coordStr, ' '));
        		
        		if (coord.length == 3) normalsList.add(new PVector(coord[0], coord[1], coord[2]));
        	}        	
        }    	
    }
    
	protected void loadVertexAttrib(XMLElement xml, ArrayList<float[]> vertexAttribsList, int attrSize)
	{
        int n = xml.getChildCount();
        XMLElement child;
        String name;
        
        String coordStr;
        float[] coord;
        for (int i = 0; i < n; i++)
        {
            child = xml.getChild(i);
            name = child.getName();
        	if (name.equals("attrib"))
        	{
        		coordStr = child.getContent();
        		coord = PApplet.parseFloat(PApplet.split(coordStr, ' '));
        		
        		if (coord.length == attrSize) vertexAttribsList.add(coord);
        	}        	
        }   	
	}

	
	protected class VertexGroup {
    VertexGroup(int n0, int n1, int numTex) {
      first = n0;
      last = n1;
      textures = new GLTexture[numTex];
    }
	  
	  int first;
	  int last;
    GLTexture[] textures;      
	}
	
	
  
  
	/*
	protected int vertexMode;
	protected int vboUsage;
	protected int[] vertCoordsVBO = { 0 };
	protected String description;
*/
	
	
	
	protected int[] colorsVBO = null;	
	protected int[] normCoordsVBO = null;	
	protected int[] texCoordsVBO = null;

	protected float tintR, tintG, tintB, tintA;

	
	protected float pointSize;
	protected float maxPointSize;
	protected float lineWidth;
	protected boolean usingPointSprites;
	protected boolean blend;
	protected boolean blend0;
	protected int blendMode;
	protected float spriteFadeSize;
	
	/*
	protected int numAttributes;
	protected int[] attribVBO;
	protected String[] attribName;
	protected int[] attribSize;
	protected int curtAttrSize;
	*/
	
  protected int imageMode;
	
  protected int firstVertIdx, lastVertIdx;
  
		
	public GLTexture[] textures;
	


	

    //public static final int STREAM = 2;
    

   

    //protected VertexGroup[] groups;

    
    

}
