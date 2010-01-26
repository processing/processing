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
  
  
  ////////////////////////////////////////////////////////////

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
  
  ////////////////////////////////////////////////////////////
  
  public void selectTexture(int n) {
      if (updateElement != -1) {
        throw new RuntimeException("GLModel: cannot select texture between beginUpdate()/endUpdate()");
      }
      selectedTexture = n;
  }

  public void setTexture(GLTexture tex) {
    VertexGroup group;
    for (int i = 0; i < groups.size(); i++) setGroupTexture(i, tex);
  }

  public GLTexture getTexture() {
    return getGroupTexture(0);
  }
  
  /**
   * Returns the number of textures.
   * @return int
   */  
  public int getNumTextures() {
    return numTextures;
  }
  
  ////////////////////////////////////////////////////////////
  
  // beginUpdate/endUpdate
  
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
  
  ////////////////////////////////////////////////////////////  
  
  // SET/GET VERTICES
  
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
    if (lastUpdateIdx < idx) lastUpdateIdx = idx;
    
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
    lastUpdateIdx = numVertices - 1;
    
    PVector vec;
    for (int i = 0; i < numVertices; i++) {
      vec = (PVector)data.get(i);
      updateVertexArray[3 * i + 0] = vec.x;
      updateVertexArray[3 * i + 1] = vec.y;
      updateVertexArray[3 * i + 2] = vec.z;
    }
  }

  ////////////////////////////////////////////////////////////
  
  // SET/GET COLORS
  
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
    
    return res;
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
    if (lastUpdateIdx < idx) lastUpdateIdx = idx;
    
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
    lastUpdateIdx = numVertices - 1;
    
    float[] rgba;
    for (int i = 0; i < numVertices; i++) {
      rgba = (float[])data.get(i);
      updateColorArray[4 * i + 0] = rgba[0];
      updateColorArray[4 * i + 1] = rgba[1];
      updateColorArray[4 * i + 2] = rgba[2];
      updateColorArray[4 * i + 3] = rgba[3];      
    }
  }

  ////////////////////////////////////////////////////////////
  
  // SET/GET NORMALS  
  
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
    if (lastUpdateIdx < idx) lastUpdateIdx = idx;
    
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
    lastUpdateIdx = numVertices - 1;
    
    PVector vec;
    for (int i = 0; i < numVertices; i++) {
      vec = (PVector)data.get(i);
      updateNormalArray[3 * i + 0] = vec.x;
      updateNormalArray[3 * i + 1] = vec.y;
      updateNormalArray[3 * i + 2] = vec.z;
    }
  }

  ////////////////////////////////////////////////////////////  

  // SET/GET TEXTURE COORDINATES
  
  
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
    if (lastUpdateIdx < idx) lastUpdateIdx = idx;
    
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
    lastUpdateIdx = numVertices - 1;
    
    PVector vec;
    for (int i = 0; i < numVertices; i++) {
      vec = (PVector)data.get(i);
      updateTexCoordArray[3 * i + 0] = vec.x;
      updateTexCoordArray[3 * i + 1] = vec.y;
    }
  }
  
  ////////////////////////////////////////////////////////////  
  
  // GROUPS   
  
  
  public void setGroup(int idx) {
    groupBreaks.add(new Integer(idx));
  }
  
  
  public int getNumGroups() {
    return groups.size();
  }
  
  
  public int getGroupFirst(int gr) {
    return ((VertexGroup)groups.get(gr)).first;
  }
  
  public int getGroupLast(int gr) {
    return ((VertexGroup)groups.get(gr)).last;
  }
  
  
  public void setGroupTexture(int gr, GLTexture tex) {
    VertexGroup group = (VertexGroup)groups.get(gr);
    group.textures[selectedTexture] = tex;
  }

  public GLTexture getGroupTexture(int gr) {
    VertexGroup group = (VertexGroup)groups.get(gr);
    return group.textures[selectedTexture];
  }
  
  
  public void setGroupColor(int gr, int c) {
    int a = (c >> 24) & 0xFF;
    int r = (c >> 16) & 0xFF;
    int g = (c >> 8) & 0xFF;
    int b = (c  >> 0) & 0xFF;
    
    setGroupColor(gr, r / 255.0f, g / 255.0f, b / 255.0f, a / 255.0f);
  }
  
  
  public void setGroupColor(int gr, float r, float g, float b) {
    setGroupColor(gr, r, g, b, 1.0f);  
  }
  
  
  public void setGroupColor(int gr, float r, float g, float b, float a) {
    VertexGroup group = (VertexGroup)groups.get(gr);
    beginUpdate(COLORS);
    firstUpdateIdx = group.first;
    lastUpdateIdx = group.last;    
    for (int i = group.first; i < group.last; i++) {
      updateColorArray[4 * i + 0] = r;
      updateColorArray[4 * i + 1] = g;
      updateColorArray[4 * i + 2] = b;
      updateColorArray[4 * i + 3] = a;      
    }
    endUpdate();
  }
  
  
  public void setGroupNormals(int i, float x, float y) {
    setGroupNormals(i, x, y, 0.0f);  
  }
  
  
  public void setGroupNormals(int gr, float x, float y, float z) {
    VertexGroup group = (VertexGroup)groups.get(gr);
    beginUpdate(NORMALS);
    firstUpdateIdx = group.first;
    lastUpdateIdx = group.last;    
    for (int i = group.first; i < group.last; i++) {
      updateNormalArray[3 * i + 0] = x;
      updateNormalArray[3 * i + 1] = y;
      updateNormalArray[3 * i + 2] = z;
    }
    endUpdate();
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
	
	public void render(int first, int last)
	{
		gl.glColor4f(tintR, tintG, tintB, tintA);
		
		gl.glLineWidth(a3d.strokeWeight);
		gl.glPointSize(PApplet.min(a3d.strokeWeight, maxPointSize));
	    
        //if (effect != null) effect.start();
	    

        
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
