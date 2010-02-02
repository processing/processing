package processing.android.core;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.ArrayList;

import javax.microedition.khronos.opengles.*;

/**
 * This class holds a 3D model composed of vertices, normals, colors (per vertex) and 
 * texture coordinates (also per vertex). All this data is stored in Vertex Buffer Objects
 * (VBO) in GPU memory for very fast access. 
 */
@SuppressWarnings("unused")
public class GLModel implements GLConstants, PConstants {
  protected PApplet parent;    
  protected GL11 gl;  
  protected PGraphicsAndroid3D a3d;
  
  protected int numVertices;
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
  protected FloatBuffer texCoords;  

  // TODO: maybe, instead of allocating memory for this arrays, we could just use
  // updateVertexArray = vertices.array(),
  // updateColorArray = colors.array(),
  // etc.
  protected float[] updateVertexArray;
  protected float[] updateColorArray;
  protected float[] updateNormalArray;
  protected float[] updateTexCoordArray;
  
  protected int updateElement;
  protected int firstUpdateIdx;
  protected int lastUpdateIdx;
  
  //protected ArrayList<Integer> groupBreaks;
  protected ArrayList<VertexGroup> groups;
  protected VertexGroup[] vertGroup;
  protected boolean creatingGroup;
  protected boolean firstSetGroup;
  protected int grIdx0;
  protected int grIdx1;
  
  
  // TODO: this should be calculated depending on the platform.
  protected static final int SIZEOF_FLOAT = 4;
  
    
  ////////////////////////////////////////////////////////////

  // Constructors.
  
  public GLModel(PApplet parent, int numVert) {
    this(parent, numVert, new Parameters()); 
  }  

  
  public GLModel(PApplet parent, int numVert, Parameters params) {
    this.parent = parent;
    a3d = (PGraphicsAndroid3D)parent.g;
    
    // Checking we have what we need:
    gl = a3d.gl11;
    if (gl == null) {
      throw new RuntimeException("GLModel: OpenGL ES 1.1 required");
    }
    if (!a3d.vboSupported) {
       throw new RuntimeException("GLModel: Vertex Buffer Objects are not available");
    }
    
    setParameters(params);
    createModel(numVert);    
    
    updateVertexArray = null;
    updateColorArray = null;
    updateNormalArray = null;
    updateTexCoordArray = null;    
    updateElement = -1;
  }
  

  protected void finalize() {
    deleteVertexBuffer();
    deleteColorBuffer();
    deleteTexCoordBuffer();
    deleteNormalBuffer();
  }
  
  ////////////////////////////////////////////////////////////
  
  // Textures
  
  
  public void setTexture(GLTexture tex) {
    for (int i = 0; i < groups.size(); i++) setGroupTexture(i, tex);
  }

  
  public GLTexture getTexture() {
    return getGroupTexture(0);
  }
  
  
  ////////////////////////////////////////////////////////////
  
  // beginUpdate/endUpdate
  
  
  public void beginUpdate(int element) {
    beginUpdateImpl(element, 0, numVertices - 1);
  }
  
  
  protected void beginUpdateImpl(int element, int first, int last) {
    if (updateElement != -1) {
      throw new RuntimeException("GLModel: only one element can be updated at the time");
    }
    
    updateElement = element;
    firstUpdateIdx = numVertices;
    lastUpdateIdx = -1;
    
    if (updateElement == VERTICES) {
      gl.glBindBuffer(GL11.GL_ARRAY_BUFFER, glVertexBufferID[0]);      
      
      if (updateVertexArray == null) {
        updateVertexArray = new float[vertices.capacity()];
      }      
      int offset = first * 3;
      int size = (last - first + 1) * 3;
      vertices.get(updateVertexArray, offset, size);
      vertices.rewind();
      
      // For group creation inside update vertices block.
      creatingGroup = false;
      firstSetGroup = true;
    } else if (updateElement == COLORS) {
      gl.glBindBuffer(GL11.GL_ARRAY_BUFFER, glColorBufferID[0]);
            
      if (updateColorArray == null) {
        updateColorArray = new float[colors.capacity()];
      }
      int offset = first * 4;
      int size = (last - first + 1) * 4;
      colors.get(updateColorArray, offset, size);
      colors.rewind();
    } else if (updateElement == NORMALS) {
      gl.glBindBuffer(GL11.GL_ARRAY_BUFFER, glNormalBufferID[0]);
            
      if (updateNormalArray == null) {
        updateNormalArray = new float[normals.capacity()];    
      }
      int offset = first * 3;
      int size = (last - first + 1) * 3;
      normals.get(updateNormalArray, offset, size);
      normals.rewind();  
        
    } else if (updateElement == TEXTURES) {      
      gl.glBindBuffer(GL11.GL_ARRAY_BUFFER, glTexCoordBufferID[0]);
          
      if (updateTexCoordArray == null) {
        updateTexCoordArray = new float[texCoords.capacity()];
      }
      int offset = first * 2;
      int size = (last - first + 1) * 2;      
      texCoords.get(updateTexCoordArray, offset, size);
      texCoords.rewind();          
    } else if (updateElement == GROUPS) {
      groups.clear();
    } else {
      throw new RuntimeException("GLModel: unknown element to update");  
    }    
  }
  
  
  public void endUpdate() {
    if (updateElement == -1) {
      throw new RuntimeException("GLModel: call beginUpdate() first");
    }
    
    if (lastUpdateIdx < firstUpdateIdx) {
      updateElement = -1;
      return;  
    }
    
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
      
      if (creatingGroup) {
        // The last group being created is added.
        addGroup(grIdx0, grIdx1, null);
      }
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
      
      texCoords.put(updateTexCoordArray, offset, size);
      texCoords.position(0);
      
      gl.glBufferSubData(GL11.GL_ARRAY_BUFFER, offset * SIZEOF_FLOAT, size * SIZEOF_FLOAT, texCoords);
      gl.glBindBuffer(GL11.GL_ARRAY_BUFFER, 0);      
    } else if (updateElement == GROUPS) {
      // TODO: check consistency of newly created groups (make sure that there are not overlapping groups)      
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
    
    if (creatingGroup) {
      if (grIdx0 == -1) {
        grIdx0 = idx;
      }
      grIdx1 = idx;  
    }
    
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
  
  
  public void setColor(int idx, int c) {
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

    float u = updateTexCoordArray[2 * idx + 0];
    float v = updateTexCoordArray[2 * idx + 1];
    
    if (a3d.imageMode == IMAGE) {
      if (vertGroup[idx].texture == null) {
        throw new RuntimeException("GLModel: when setting texture coordinates in IMAGE mode, the textures need to be assigned first");
      }      
      u *= vertGroup[idx].texture.width;
      v *= vertGroup[idx].texture.height;
    }
    
    PVector res = new PVector(u, v, 0);
    return res;
  }

  
  public float[] getTexCoordArray() {
    if (updateElement != TEXTURES) {
      throw new RuntimeException("GLModel: update mode is not set to TEXTURES");
    }
    
    float[] res = new float[numVertices * 2];
    PApplet.arrayCopy(updateTexCoordArray, res);
    
    if (a3d.imageMode == IMAGE) {
      float u, v;
      for (int i = 0; i < numVertices; i++) {
        if (vertGroup[i].texture == null) {
          throw new RuntimeException("GLModel: when setting texture coordinates in IMAGE mode, the textures need to be assigned first");
        }        
        
        u = res[2 * i + 0];
        v = res[2 * i + 1];        
        
        u *= vertGroup[i].texture.width;
        v *= vertGroup[i].texture.height;

        res[2 * i + 0] = u;
        res[2 * i + 1] = v;
      }  
    }
    
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

    if (a3d.imageMode == IMAGE) {
      if (vertGroup[idx].texture == null) {
        throw new RuntimeException("GLModel: when setting texture coordinates in IMAGE mode, the textures need to be assigned first");
      }
      u /= vertGroup[idx].texture.width;
      v /= vertGroup[idx].texture.height; 
    }
    
    updateTexCoordArray[2 * idx + 0] = u;
    updateTexCoordArray[2 * idx + 1] = v;
  }

  
  public void setTexCoord(float[] data) {
    if (updateElement != TEXTURES) {
      throw new RuntimeException("GLModel: update mode is not set to TEXTURES");
    }
    
    firstUpdateIdx = 0;
    firstUpdateIdx = numVertices;
    
    if (a3d.imageMode == IMAGE) {
      float u, v;
      for (int i = 0; i < numVertices; i++) {
        if (vertGroup[i].texture == null) {
          throw new RuntimeException("GLModel: when setting texture coordinates in IMAGE mode, the textures need to be assigned first");
        }      
        
        u = data[2 * i + 0];
        v = data[2 * i + 1];        
        
        u /= vertGroup[i].texture.width;
        v /= vertGroup[i].texture.height;

        updateTexCoordArray[2 * i + 0] = u;
        updateTexCoordArray[2 * i + 1] = v;
      }  
    } else {
      PApplet.arrayCopy(data, updateTexCoordArray);
    }
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
      
      if (a3d.imageMode == IMAGE) {
        if (vertGroup[i].texture == null) {
          throw new RuntimeException("GLModel: when setting texture coordinates in IMAGE mode, the textures need to be assigned first");
        }      
        updateTexCoordArray[2 * i + 0] = vec.x / vertGroup[i].texture.width;
        updateTexCoordArray[2 * i + 1] = vec.y / vertGroup[i].texture.height;
      } else {
        updateTexCoordArray[2 * i + 0] = vec.x;
        updateTexCoordArray[2 * i + 1] = vec.y;
      }
    }
  }

  
  ////////////////////////////////////////////////////////////  
  
  // GROUPS   
  
  
  public void setGroup(int gr) {
    if (updateElement != VERTICES) {
      throw new RuntimeException("GLModel: setGroup() with group number as only argument must be used while updating vertices");
    }
    
    if (firstSetGroup) {
      // The first time the method setGroup is called inside the update vertices block, all the current groups
      // are erased.
      firstSetGroup = false; 
      groups.clear();
    }
    
    if (creatingGroup) {
      // One group was being specified. Now it is finished, before we start a new one.
      addGroup(grIdx0, grIdx1, null);
    }
    
    creatingGroup = true;
    grIdx0 =  grIdx1 = -1;
  }
  
  
  public void setGroup(int gr, int idx0, int idx1) {
    if (updateElement != GROUPS) {
      throw new RuntimeException("GLModel: update mode is not set to GROUPS");
    }

    if (groups.size() != gr) {
      throw new RuntimeException("GLModel: wrong group index");
    }

    if (idx0 < firstUpdateIdx) firstUpdateIdx = idx0;
    if (lastUpdateIdx < idx1) lastUpdateIdx = idx1;
        
    addGroup(idx0, idx1, null);
  }  
  
  
public void setGroup(int gr, int idx0, int idx1, GLTexture tex) {
    if (updateElement != GROUPS) {
      throw new RuntimeException("GLModel: update mode is not set to GROUPS");
    }

    if (groups.size() != gr) {
      throw new RuntimeException("GLModel: wrong group index");
    }
        
    if (idx0 < firstUpdateIdx) firstUpdateIdx = idx0;
    if (lastUpdateIdx < idx1) lastUpdateIdx = idx1;
    
    addGroup(idx0, idx1, tex);
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
    group.texture = tex;
  }

  
  public GLTexture getGroupTexture(int gr) {
    VertexGroup group = (VertexGroup)groups.get(gr);
    return group.texture;
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
    beginUpdateImpl(COLORS, group.first, group.last);
    firstUpdateIdx = group.first;
    lastUpdateIdx = group.last;    
    for (int i = group.first; i <= group.last; i++) {
      updateColorArray[4 * i + 0] = r;
      updateColorArray[4 * i + 1] = g;
      updateColorArray[4 * i + 2] = b;
      updateColorArray[4 * i + 3] = a;      
    }
    endUpdate();
  }
  
  
  public void setGroupNormal(int i, float x, float y) {
    setGroupNormal(i, x, y, 0.0f);  
  }
  
  
  public void setGroupNormal(int gr, float x, float y, float z) {
    VertexGroup group = (VertexGroup)groups.get(gr);
    beginUpdateImpl(NORMALS, group.first, group.last);
    firstUpdateIdx = group.first;
    lastUpdateIdx = group.last;    
    for (int i = group.first; i <= group.last; i++) {
      updateNormalArray[3 * i + 0] = x;
      updateNormalArray[3 * i + 1] = y;
      updateNormalArray[3 * i + 2] = z;
    }
    endUpdate();
  }  
  
  
  protected void initGroups() {
    groups = new ArrayList<VertexGroup>();
    vertGroup = new VertexGroup[numVertices];
    addGroup(0, numVertices - 1, null);
  }
  
  
  protected void addGroup(int idx0, int idx1, GLTexture tex) {
    if (0 <= idx0 && idx0 <=  idx1) {
      VertexGroup group = new VertexGroup(idx0, idx1, tex);
      groups.add(group);
      for (int n = idx0; n <= idx1; n++) {
        vertGroup[n] = group;
      }
    }
  }
  
  
  ////////////////////////////////////////////////////////////  
  
  // Parameters   
  
  
  public void setDrawMode(int mode) {
    pointSprites = false;
    if (mode == POINTS) glMode = GL11.GL_POINTS;
    else if (mode == POINT_SPRITES) {
      glMode = GL11.GL_POINTS;
      pointSprites = true;
    }
    else if (mode == LINES) glMode = GL11.GL_LINES;
    else if (mode == LINE_STRIP) glMode = GL11.GL_LINE_STRIP;
    else if (mode == LINE_LOOP) glMode = GL11.GL_LINE_LOOP;
    else if (mode == TRIANGLES) glMode = GL11.GL_TRIANGLES; 
    else if (mode == TRIANGLE_FAN) glMode = GL11.GL_TRIANGLE_FAN;
    else if (mode == TRIANGLE_STRIP) glMode = GL11.GL_TRIANGLE_STRIP;
    else {
      throw new RuntimeException("GLModel: Unknown draw mode");
    }
  }
  
  
  public Parameters getParameters() {
    Parameters res = new Parameters();
    
    if (glMode == GL11.GL_POINTS) {
      if (pointSprites) res.drawMode = POINT_SPRITES;
      else res.drawMode = POINTS;
    }
    else if (glMode == GL11.GL_LINES) res.drawMode = LINES;
    else if (glMode == GL11.GL_LINE_STRIP) res.drawMode = LINE_STRIP;
    else if (glMode == GL11.GL_LINE_LOOP) res.drawMode = LINE_LOOP;
    else if (glMode == GL11.GL_TRIANGLES) res.drawMode = TRIANGLES; 
    else if (glMode == GL11.GL_TRIANGLE_FAN) res.drawMode = TRIANGLE_FAN;
    else if (glMode == GL11.GL_TRIANGLE_STRIP) res.drawMode = TRIANGLE_STRIP;
    
    if (glUsage == GL11.GL_STATIC_DRAW) res.updateMode = STATIC;
    else if (glUsage == GL11.GL_DYNAMIC_DRAW) res.updateMode = DYNAMIC;
    
    return res;
  }
  
  
  protected void setParameters(Parameters params) {
    pointSprites = false;
    if (params.drawMode == POINTS) glMode = GL11.GL_POINTS;
    else if (params.drawMode == POINT_SPRITES) {
      glMode = GL11.GL_POINTS;
      pointSprites = true;
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
  
  
  ////////////////////////////////////////////////////////////  
  
  // Data allocation, deletion.

  
  void createModel(int numVert) {
    numVertices = numVert;
    initBufferIDs();
 
    createVertexBuffer();
    createColorBuffer();
    createNormalBuffer();    
    createTexCoordBuffer();

    initGroups();    
  }
  
  
  void initBufferIDs() {
    glVertexBufferID = new int[1];
    glColorBufferID = new int[1];
    glNormalBufferID = new int[1];
    glTexCoordBufferID = new int[1];
    glVertexBufferID[0] = glColorBufferID[0] = glNormalBufferID[0] = glTexCoordBufferID[0] = 0;
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
    
    deleteVertexBuffer();  // Just in case.
    
    gl.glGenBuffers(1, glVertexBufferID, 0);
    gl.glBindBuffer(GL11.GL_ARRAY_BUFFER, glVertexBufferID[0]);
    gl.glBufferData(GL11.GL_ARRAY_BUFFER, vertices.capacity() * SIZEOF_FLOAT, vertices, glUsage);
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
    
    deleteColorBuffer();  // Just in case.
    
    gl.glGenBuffers(1, glColorBufferID, 0);
    gl.glBindBuffer(GL11.GL_ARRAY_BUFFER, glColorBufferID[0]);
    gl.glBufferData(GL11.GL_ARRAY_BUFFER, colors.capacity() * SIZEOF_FLOAT, colors, glUsage);
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
    
    deleteNormalBuffer();  // Just in case.
    
    gl.glGenBuffers(1, glNormalBufferID, 0);
    gl.glBindBuffer(GL11.GL_ARRAY_BUFFER, glNormalBufferID[0]);
    gl.glBufferData(GL11.GL_ARRAY_BUFFER, normals.capacity() * SIZEOF_FLOAT, normals, glUsage);
    gl.glBindBuffer(GL11.GL_ARRAY_BUFFER, 0);
  }
  
  
  protected void createTexCoordBuffer() {
    
    ByteBuffer vbb = ByteBuffer.allocateDirect(numVertices * 2 * SIZEOF_FLOAT);
    vbb.order(ByteOrder.nativeOrder());
    texCoords = vbb.asFloatBuffer();

    float[] values = new float[numVertices * 2];
    for (int i = 0; i < values.length; i++) values[i] = 0.0f;    
    texCoords.put(values);
    texCoords.position(0);    
    
    deleteTexCoordBuffer(); // Just in case.
    
    gl.glGenBuffers(1, glTexCoordBufferID, 0);
    gl.glBindBuffer(GL11.GL_ARRAY_BUFFER, glTexCoordBufferID[0]);
    gl.glBufferData(GL11.GL_ARRAY_BUFFER, texCoords.capacity() * SIZEOF_FLOAT, texCoords, glUsage);
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
  
  
  protected void deleteNormalBuffer() {
    if (glNormalBufferID[0] != 0) {    
      gl.glDeleteBuffers(1, glNormalBufferID, 0);
      glNormalBufferID[0] = 0;
    }
  }  
  
  
  protected void deleteTexCoordBuffer() {
    if (glTexCoordBufferID[0] != 0) {
      gl.glDeleteBuffers(1, glTexCoordBufferID, 0);
      glTexCoordBufferID[0] = 0;
    }
  }  
  
  
  ///////////////////////////////////////////////////////////  

  // Rendering methods
  

   public void render() {
     render(0, groups.size() - 1);
   }
   
  
	 public void render(int gr) {
	   render(gr, gr);
	 }
  
  
  public void render(int gr0, int gr1) {
	  int texTarget = GL11.GL_TEXTURE_2D;
	  GLTexture tex;
	  float pointSize;
	  
	  // Setting line width and point size from stroke value.
		gl.glLineWidth(a3d.strokeWeight);
		pointSize = PApplet.min(a3d.strokeWeight, a3d.maxPointSize);
    gl.glPointSize(pointSize);
	    
    gl.glEnableClientState(GL11.GL_NORMAL_ARRAY);
    gl.glBindBuffer(GL11.GL_ARRAY_BUFFER, glNormalBufferID[0]);
    gl.glNormalPointer(GL11.GL_FLOAT, 0, 0);

    gl.glEnableClientState(GL11.GL_COLOR_ARRAY);
    gl.glBindBuffer(GL11.GL_ARRAY_BUFFER, glColorBufferID[0]);
    gl.glColorPointer(4, GL11.GL_FLOAT, 0, 0);
     
    gl.glEnableClientState(GL11.GL_VERTEX_ARRAY);            
    gl.glBindBuffer(GL11.GL_ARRAY_BUFFER, glVertexBufferID[0]);
    gl.glVertexPointer(3, GL11.GL_FLOAT, 0, 0);
      
    VertexGroup group;
    for (int i = gr0; i <= gr1; i++) {
      group = (VertexGroup)groups.get(i);

      tex = group.texture; 
      if (tex != null)  {
         
        texTarget = group.texture.getGLTarget();
        gl.glEnable(texTarget);
        // Binding texture units.
         
        gl.glActiveTexture(GL11.GL_TEXTURE0);
        gl.glBindTexture(GL11.GL_TEXTURE_2D, group.texture.getGLTextureID()); 
         
        if (pointSprites) {
          // Texturing with point sprites.
              
          // This is how will our point sprite's size will be modified by 
          // distance from the viewer             
          float quadratic[] = {1.0f, 0.0f, 0.01f, 1};
          ByteBuffer temp = ByteBuffer.allocateDirect(16);
          temp.order(ByteOrder.nativeOrder());              
          gl.glPointParameterfv(GL11.GL_POINT_DISTANCE_ATTENUATION, (FloatBuffer)temp.asFloatBuffer().put(quadratic).flip());
           
          // The alpha of a point is calculated to allow the fading of points 
          // instead of shrinking them past a defined threshold size. The threshold 
          // is defined by GL_POINT_FADE_THRESHOLD_SIZE_ARB and is not clamped to 
          // the minimum and maximum point sizes.
          gl.glPointParameterf(GL11.GL_POINT_FADE_THRESHOLD_SIZE, 0.6f * pointSize);
          gl.glPointParameterf(GL11.GL_POINT_SIZE_MIN, 1.0f);
          gl.glPointParameterf(GL11.GL_POINT_SIZE_MAX, a3d.maxPointSize);

          // Specify point sprite texture coordinate replacement mode for each 
          // texture unit
          gl.glTexEnvf(GL11.GL_POINT_SPRITE_OES, GL11.GL_COORD_REPLACE_OES, GL11.GL_TRUE);

          gl.glEnable(GL11.GL_POINT_SPRITE_OES);           
        } else {
          // Regular texturing.
          gl.glEnableClientState(GL11.GL_TEXTURE_COORD_ARRAY);
          gl.glClientActiveTexture(GL11.GL_TEXTURE0);
          gl.glBindBuffer(GL11.GL_ARRAY_BUFFER, glTexCoordBufferID[0]);
          gl.glTexCoordPointer(2, GL11.GL_FLOAT, 0, 0);
        }
      }
       
      // Last transformation: inversion of coordinate to make compatible with Processing's inverted Y axis.
      gl.glPushMatrix();
      gl.glScalef(1, -1, 1);     
      gl.glDrawArrays(glMode, group.first, group.last - group.first + 1);
      gl.glPopMatrix();
      
      if (tex != null)  {
        if (pointSprites)   {
          gl.glDisable(GL11.GL_POINT_SPRITE_OES);
        } else  {
          gl.glDisableClientState(GL11.GL_TEXTURE_COORD_ARRAY);
        }
        gl.glDisable(texTarget);
      }       
    }

    gl.glDisableClientState(GL11.GL_VERTEX_ARRAY);    
	  gl.glDisableClientState(GL11.GL_COLOR_ARRAY);
    gl.glDisableClientState(GL11.GL_NORMAL_ARRAY);
	}	
  
	
  ///////////////////////////////////////////////////////////////////////////   
  
  // Parameters
  
  static public Parameters newParameters() {
    return new Parameters();
  }

  
  static public Parameters newParameters(int drawMode) {
    return new Parameters(drawMode);
  }  

  
  static public Parameters newParameters(int drawMode, int updateMode) {
    return new Parameters(drawMode, updateMode);
  }  
  
  
  static public class Parameters {
    public int updateMode;  
    public int drawMode;
    
    public Parameters() {
      updateMode = STATIC;    
      drawMode= POINTS;
    }

    public Parameters(int drawMode) {
      updateMode = STATIC;    
      this.drawMode= drawMode;
    }

    public Parameters(int drawMode, int updateMode) {
      this.updateMode = updateMode;    
      this.drawMode= drawMode;
    }
    
    public Parameters(Parameters src) {
      updateMode = src.updateMode;    
      drawMode= src.drawMode;
    }
  }  
  
	protected class VertexGroup {
    VertexGroup(int n0, int n1) {
      first = n0;
      last = n1; 
      texture = null;
    }

    VertexGroup(int n0, int n1, GLTexture tex) {
      first = n0;
      last = n1; 
      texture = tex;
    }
    
	  int first;
	  int last;	  
    GLTexture texture;      
	}
}
