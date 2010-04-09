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

import javax.microedition.khronos.opengles.*;
import java.nio.FloatBuffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.io.BufferedReader;
import java.lang.reflect.Method;

/**
 * This class holds a 3D model composed of vertices, normals, colors (per vertex) and 
 * texture coordinates (also per vertex). All this data is stored in Vertex Buffer Objects
 * (VBO) in GPU memory for very fast access. 
 * OBJ loading implemented using code from Saito's OBJLoader library (http://code.google.com/p/saitoobjloader/)
 * and OBJReader from Ahmet Kizilay (http://www.openprocessing.org/visuals/?visualID=191). 
 * By Andres Colubri
 * 
 */
public class PShape3D extends PShape implements PConstants {
  protected PApplet parent;    
  protected GL11 gl;  
  protected PGraphicsAndroid3D a3d;
  
  protected int numVertices;
  protected int glMode;
  protected int glUsage;
  protected boolean pointSprites;
  
  protected int[] glVertexBufferID = {0};
  protected int[] glColorBufferID = {0};
  protected int[] glTexCoordBufferID = {0};
  protected int[] glNormalBufferID = {0};

  protected FloatBuffer vertexBuffer;
  protected FloatBuffer colorBuffer;
  protected FloatBuffer normalBuffer;
  protected FloatBuffer texCoordBuffer;
  
  protected float[] vertexArray;
  protected float[] colorArray;
  protected float[] normalArray;
  protected float[] texCoordArray;
  
  protected int updateElement;
  protected int firstUpdateIdx;
  protected int lastUpdateIdx;
  protected PTexture updateTexture;
  
  protected ArrayList<VertexGroup> groups;
  protected VertexGroup[] vertGroup;
  protected boolean creatingGroup;
  protected boolean firstSetGroup;
  protected int grIdx0;
  protected int grIdx1;

  protected int recreateResourceIdx;
  
  protected float xmin, xmax;
  protected float ymin, ymax;
  protected float zmin, zmax;
  
  protected static final int SIZEOF_FLOAT = Float.SIZE / 8;
  
  
  ////////////////////////////////////////////////////////////

  // Constructors.

  
  public PShape3D(PApplet parent, int numVert) {
    this(parent, numVert, new Parameters()); 
  }  

  
  public PShape3D(PApplet parent, String filename) {
    this.parent = parent;
    a3d = (PGraphicsAndroid3D)parent.g;
    
    // Checking we have what we need:
    gl = a3d.gl11;
    if (gl == null) {
      throw new RuntimeException("PShape3D: OpenGL ES 1.1 required");
    }
    if (!a3d.vboSupported) {
       throw new RuntimeException("PShape3D: Vertex Buffer Objects are not available");
    }
  
    ArrayList<OBJFace> faces = new ArrayList<OBJFace>();
    ArrayList<OBJMaterial> materials = new ArrayList<OBJMaterial>();
    ArrayList<PVector> vertices = new ArrayList<PVector>(); 
    ArrayList<PVector> normals = new ArrayList<PVector>(); 
    ArrayList<PVector> textures = new ArrayList<PVector>();
    BufferedReader reader = getBufferedReader(filename);
    if (reader == null) {
      throw new RuntimeException("PShape3D: Cannot read source file");
    }
    parseOBJ(reader, vertices, normals, textures, faces, materials);
    recordOBJ(vertices, normals, textures, faces, materials);
  }  
  
  
  public PShape3D(PApplet parent, int numVert, Parameters params) {
    this.parent = parent;
    a3d = (PGraphicsAndroid3D)parent.g;
    
    // Checking we have what we need:
    gl = a3d.gl11;
    if (gl == null) {
      throw new RuntimeException("PShape3D: OpenGL ES 1.1 required");
    }
    if (!a3d.vboSupported) {
       throw new RuntimeException("PShape3D: Vertex Buffer Objects are not available");
    }
    
    setParameters(params);
    createModel(numVert);    
    initGroups();      
    updateElement = -1;
    
    width = height = depth = 0;
    xmin = ymin = zmin = 10000;
    xmax = ymax = zmax = -10000;

    try {
      Method meth = this.getClass().getMethod("recreateResource", new Class[] { PGraphicsAndroid3D.class });
      recreateResourceIdx =  a3d.addRecreateResourceMethod(this, meth);
    } catch (Exception e) {
      recreateResourceIdx = -1;
    }
  }
  

  protected void finalize() {
    a3d.removeRecreateResourceMethod(recreateResourceIdx);
    
    deleteVertexBuffer();
    deleteColorBuffer();
    deleteTexCoordBuffer();
    deleteNormalBuffer();
  }

  
  ////////////////////////////////////////////////////////////
  
  // Textures
  
  
  public void setTexture(PTexture tex) {
    if (updateElement == -1) { 
      for (int i = 0; i < groups.size(); i++) setGroupTexture(i, tex);
    } else if (updateElement == TEXTURES) {
      updateTexture = tex;  
    }
  }

  
  public PTexture getTexture() {
    return getGroupTexture(0);
  }
  
  
  ////////////////////////////////////////////////////////////
  
  // beginUpdate/endUpdate
  
  
  public void beginUpdate(int element) {
    beginUpdateImpl(element, 0, numVertices - 1);
  }
  
  
  protected void beginUpdateImpl(int element, int first, int last) {
    if (updateElement != -1) {
      throw new RuntimeException("PShape3D: only one element can be updated at the time");
    }
    
    updateElement = element;
    firstUpdateIdx = numVertices;
    lastUpdateIdx = -1;
    
    if (updateElement == VERTICES) {
      gl.glBindBuffer(GL11.GL_ARRAY_BUFFER, glVertexBufferID[0]);      
      
      int offset = first * 3;
      int size = (last - first + 1) * 3;
      vertexBuffer.limit(vertexBuffer.capacity());      
      vertexBuffer.rewind();
      vertexBuffer.get(vertexArray, offset, size);
      
      // For group creation inside update vertices block.
      creatingGroup = false;
      firstSetGroup = true;
    } else if (updateElement == COLORS) {
      gl.glBindBuffer(GL11.GL_ARRAY_BUFFER, glColorBufferID[0]);
            
      int offset = first * 4;
      int size = (last - first + 1) * 4;
      colorBuffer.limit(colorBuffer.capacity());
      colorBuffer.rewind();
      colorBuffer.get(colorArray, offset, size);
    } else if (updateElement == NORMALS) {
      gl.glBindBuffer(GL11.GL_ARRAY_BUFFER, glNormalBufferID[0]);
            
      int offset = first * 3;
      int size = (last - first + 1) * 3;
      normalBuffer.limit(normalBuffer.capacity());      
      normalBuffer.rewind();      
      normalBuffer.get(normalArray, offset, size);
    } else if (updateElement == TEXTURES) {      
      gl.glBindBuffer(GL11.GL_ARRAY_BUFFER, glTexCoordBufferID[0]);
          
      int offset = first * 2;
      int size = (last - first + 1) * 2;
      texCoordBuffer.limit(texCoordBuffer.capacity());      
      texCoordBuffer.rewind();
      texCoordBuffer.get(texCoordArray, offset, size);
    } else if (updateElement == GROUPS) {
      deleteGroups();
    } else {
      throw new RuntimeException("PShape3D: unknown element to update");  
    }    
  }
  
  
  public void endUpdate() {
    if (updateElement == -1) {
      throw new RuntimeException("PShape3D: call beginUpdate() first");
    }
    
    if (lastUpdateIdx < firstUpdateIdx) {
      updateElement = -1;
      return;  
    }
    
    if (updateElement == VERTICES) {      
      int offset = firstUpdateIdx * 3;
      int size = (lastUpdateIdx - firstUpdateIdx + 1) * 3;
      
      vertexBuffer.position(0);
      vertexBuffer.put(vertexArray, offset, size);
      vertexBuffer.flip();
    
      gl.glBufferSubData(GL11.GL_ARRAY_BUFFER, offset * SIZEOF_FLOAT, size * SIZEOF_FLOAT, vertexBuffer);
      gl.glBindBuffer(GL11.GL_ARRAY_BUFFER, 0);
      
      if (creatingGroup) {
        // The last group being created is added.
        addGroup(grIdx0, grIdx1, null);
      }
    } else if (updateElement == COLORS) {      
      int offset = firstUpdateIdx * 4;
      int size = (lastUpdateIdx - firstUpdateIdx + 1) * 4;
            
      colorBuffer.position(0);      
      colorBuffer.put(colorArray, offset, size);
      colorBuffer.flip();
    
      gl.glBufferSubData(GL11.GL_ARRAY_BUFFER, offset * SIZEOF_FLOAT, size * SIZEOF_FLOAT, colorBuffer);
      gl.glBindBuffer(GL11.GL_ARRAY_BUFFER, 0);
    } else if (updateElement == NORMALS) {
      int offset = firstUpdateIdx * 3;
      int size = (lastUpdateIdx - firstUpdateIdx + 1) * 3;
      
      normalBuffer.position(0);      
      normalBuffer.put(normalArray, offset, size);
      normalBuffer.flip();
    
      gl.glBufferSubData(GL11.GL_ARRAY_BUFFER, offset * SIZEOF_FLOAT, size * SIZEOF_FLOAT, normalBuffer);
      gl.glBindBuffer(GL11.GL_ARRAY_BUFFER, 0);
    } else if (updateElement == TEXTURES) {
      int offset = firstUpdateIdx * 2;
      int size = (lastUpdateIdx - firstUpdateIdx + 1) * 2;
      
      texCoordBuffer.position(0);      
      texCoordBuffer.put(texCoordArray, offset, size);
      texCoordBuffer.flip();
      
      gl.glBufferSubData(GL11.GL_ARRAY_BUFFER, offset * SIZEOF_FLOAT, size * SIZEOF_FLOAT, texCoordBuffer);
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
      throw new RuntimeException("PShape3D: update mode is not set to VERTICES");
    }

    float x = vertexArray[3 * idx + 0];
    float y = vertexArray[3 * idx + 1];
    float z = vertexArray[3 * idx + 2];
    
    PVector res = new PVector(x, y, z);
    return res;
  }
  

  public float[] getVertexArray() {
    if (updateElement != VERTICES) {
      throw new RuntimeException("PShape3D: update mode is not set to VERTICES");
    }
    
    float[] res = new float[numVertices * 3];
    getVertexArrayImpl(res, 0, numVertices, 0);
    
    return res;
  }

  
  protected float[] getVertexArrayImpl(float[] data, int firstUpd, int length, int firstData) {
    PApplet.arrayCopy(vertexArray, firstUpd * 3, data, firstData * 3,  length * 3);
    return  data;
  }
    

  public ArrayList<PVector> getVertexArrayList() {
    if (updateElement != VERTICES) {
      throw new RuntimeException("PShape3D: update mode is not set to VERTICES");
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
      throw new RuntimeException("PShape3D: update mode is not set to VERTICES");
    }

    if (idx < firstUpdateIdx) firstUpdateIdx = idx;
    if (lastUpdateIdx < idx) lastUpdateIdx = idx;
    
    if (creatingGroup) {
      if (grIdx0 == -1) {
        grIdx0 = idx;
      }
      grIdx1 = idx;  
    }
    
    updateBounds(x, y, z);
    vertexArray[3 * idx + 0] = x;
    vertexArray[3 * idx + 1] = y;
    vertexArray[3 * idx + 2] = z;    
  }

  
  public void setVertex(float[] data) {
    if (updateElement != VERTICES) {
      throw new RuntimeException("PShape3D: updadate mode is not set to VERTICES");
    }
    
    firstUpdateIdx = 0;
    lastUpdateIdx = numVertices - 1;    
    
    if (creatingGroup) {
      grIdx0 = 0;
      grIdx1 = numVertices - 1;
    }
    
    for (int i = 0; i < numVertices; i++) {
      updateBounds(data[3 * i + 0], data[3 * i + 1], data[3 * i + 2]);      
    }
    
    PApplet.arrayCopy(data, vertexArray);
  }
  
  
  public void setVertex(ArrayList<PVector> data) {
    if (updateElement != VERTICES) {
      throw new RuntimeException("PShape3D: updadate mode is not set to VERTICES");
    }

    firstUpdateIdx = 0;
    lastUpdateIdx = numVertices - 1;

    if (creatingGroup) {
      grIdx0 = 0;
      grIdx1 = numVertices - 1;
    }
        
    PVector vec;
    for (int i = 0; i < numVertices; i++) {
      vec = (PVector)data.get(i);
      vertexArray[3 * i + 0] = vec.x;
      vertexArray[3 * i + 1] = vec.y;
      vertexArray[3 * i + 2] = vec.z;
      updateBounds(vec.x, vec.y, vec.z);
    }
  }
  
  
  protected void updateBounds(float x, float y, float z) {
    xmin = PApplet.min(xmin, x);
    xmax = PApplet.max(xmax, x);
    
    ymin = PApplet.min(ymin, y);
    ymax = PApplet.max(ymax, y);

    zmin = PApplet.min(zmin, z);
    zmax = PApplet.max(zmax, z);

    width = xmax - xmin;
    height = ymax - ymin;
    depth = zmax - zmin;
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
      throw new RuntimeException("PShape3D: update mode is not set to COLORS");
    }
    
    float[] res = new float[4];
    PApplet.arrayCopy(colorArray, idx * 4, res, 0, 4);
    
    return res;
  }
  
  
  public float[] getColorArray() {
    if (updateElement != COLORS) {
      throw new RuntimeException("PShape3D: update mode is not set to COLORS");
    }
    
    float[] res = new float[numVertices];
    getColorArrayImpl(res, 0, numVertices, 0);
    
    return res;
  }


  protected float[] getColorArrayImpl(float[] data, int firstUpd, int length, int firstData) {
    PApplet.arrayCopy(colorArray, firstUpd * 4, data, firstData * 4,  length * 4);
    return  data;
  }
    
  
  public ArrayList<float[]> getColorArrayList() {
    if (updateElement != COLORS) {
      throw new RuntimeException("PShape3D: update mode is not set to COLORS");
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
      throw new RuntimeException("PShape3D: update mode is not set to COLORS");
    }

    if (idx < firstUpdateIdx) firstUpdateIdx = idx;
    if (lastUpdateIdx < idx) lastUpdateIdx = idx;
    
    colorArray[4 * idx + 0] = r;
    colorArray[4 * idx + 1] = g;
    colorArray[4 * idx + 2] = b;
    colorArray[4 * idx + 3] = a;    
  }

  
  public void setColor(float[] data) {
    if (updateElement != COLORS) {
      throw new RuntimeException("PShape3D: update mode is not set to COLORS");
    }
    
    firstUpdateIdx = 0;
    firstUpdateIdx = numVertices - 1;   
    PApplet.arrayCopy(data, colorArray);
  }
  
  
  public void setColor(ArrayList<float[]> data) {
    if (updateElement != COLORS) {
      throw new RuntimeException("PShape3D: update mode is not set to COLORS");
    }

    firstUpdateIdx = 0;
    lastUpdateIdx = numVertices - 1;
    
    float[] rgba;
    for (int i = 0; i < numVertices; i++) {
      rgba = (float[])data.get(i);
      colorArray[4 * i + 0] = rgba[0];
      colorArray[4 * i + 1] = rgba[1];
      colorArray[4 * i + 2] = rgba[2];
      colorArray[4 * i + 3] = rgba[3];      
    }
  }
  

  ////////////////////////////////////////////////////////////
  
  // SET/GET NORMALS
  
  
  public PVector getNormal(int idx) {
    if (updateElement != NORMALS) {
      throw new RuntimeException("PShape3D: update mode is not set to NORMALS");
    }

    float x = normalArray[3 * idx + 0];
    float y = normalArray[3 * idx + 1];
    float z = normalArray[3 * idx + 2];
    
    PVector res = new PVector(x, y, z);
    return res;
  }
  

  public float[] getNormalArray() {
    if (updateElement != NORMALS) {
      throw new RuntimeException("PShape3D: update mode is not set to NORMALS");
    }
    
    float[] res = new float[numVertices * 3];
    getNormalArrayImpl(res, 0, numVertices, 0);
    
    return res;
  }


  protected float[] getNormalArrayImpl(float[] data, int firstUpd, int length, int firstData) {
    PApplet.arrayCopy(normalArray, firstUpd * 3, data, firstData * 3, length * 3);
    return  data;
  }
  
  
  public ArrayList<PVector> getNormalArrayList() {
    if (updateElement != NORMALS) {
      throw new RuntimeException("PShape3D: update mode is not set to NORMALS");
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
      throw new RuntimeException("PShape3D: update mode is not set to NORMALS");
    }

    if (idx < firstUpdateIdx) firstUpdateIdx = idx;
    if (lastUpdateIdx < idx) lastUpdateIdx = idx;
    
    normalArray[3 * idx + 0] = x;
    normalArray[3 * idx + 1] = y;
    normalArray[3 * idx + 2] = z;
  }

  
  public void setNormal(float[] data) {
    if (updateElement != NORMALS) {
      throw new RuntimeException("PShape3D: update mode is not set to NORMALS");
    }
    
    firstUpdateIdx = 0;
    firstUpdateIdx = numVertices - 1;    
    PApplet.arrayCopy(data, normalArray);
  }
  
  
  public void setNormal(ArrayList<PVector> data) {
    if (updateElement != NORMALS) {
      throw new RuntimeException("PShape3D: update mode is not set to NORMALS");
    }

    firstUpdateIdx = 0;
    lastUpdateIdx = numVertices - 1;
    
    PVector vec;
    for (int i = 0; i < numVertices; i++) {
      vec = (PVector)data.get(i);
      normalArray[3 * i + 0] = vec.x;
      normalArray[3 * i + 1] = vec.y;
      normalArray[3 * i + 2] = vec.z;
    }
  }

  
  ////////////////////////////////////////////////////////////  

  // SET/GET TEXTURE COORDINATES
  
  
  public PVector getTexCoord(int idx) {
    if (updateElement != TEXTURES) {
      throw new RuntimeException("PShape3D: update mode is not set to TEXTURES");
    }

    float u = texCoordArray[2 * idx + 0];
    float v = texCoordArray[2 * idx + 1];
    
    if (a3d.imageMode == IMAGE) {
      if (vertGroup[idx] != null && vertGroup[idx].texture == null) {
        throw new RuntimeException("PShape3D: when setting texture coordinates in IMAGE mode, the textures need to be assigned first");
      }      
      u *= vertGroup[idx].texture.width;
      v *= vertGroup[idx].texture.height;
    }
    
    PVector res = new PVector(u, v, 0);
    return res;
  }
  
  public float[] getTexCoordArray() {
    if (updateElement != TEXTURES) {
      throw new RuntimeException("PShape3D: update mode is not set to TEXTURES");
    }
    
    float[] res = new float[numVertices * 2];
    getTexCoordArrayImpl(res, 0, numVertices, 0);
    
    PApplet.arrayCopy(texCoordArray, res);
    
    if (a3d.imageMode == IMAGE) {
      float u, v;
      for (int i = 0; i < numVertices; i++) {
        if (vertGroup[i] != null && vertGroup[i].texture == null) {
          throw new RuntimeException("PShape3D: when setting texture coordinates in IMAGE mode, the textures need to be assigned first");
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


  protected float[] getTexCoordArrayImpl(float[] data, int firstUpd, int length, int firstData) {
    PApplet.arrayCopy(texCoordArray, firstUpd * 2, data, firstData * 2,  length * 2);
    return  data;   
  }

  
  public ArrayList<PVector> getTexCoordArrayList() {
    if (updateElement != TEXTURES) {
      throw new RuntimeException("PShape3D: update mode is not set to TEXTURES");
    }

    ArrayList<PVector> res;
    res = new ArrayList<PVector>();
    
    for (int i = 0; i < numVertices; i++) res.add(getTexCoord(i));
    
    return res;
  }  
  
  
  public void setTexCoord(int idx, float u, float v) {
    if (updateElement != TEXTURES) {
      throw new RuntimeException("PShape3D: update mode is not set to TEXTURES");
    }

    if (idx < firstUpdateIdx) firstUpdateIdx = idx;
    if (lastUpdateIdx < idx) lastUpdateIdx = idx;
    
    if (updateTexture != null && vertGroup[idx] != null) {
      vertGroup[idx].texture = updateTexture;
    }

    if (a3d.imageMode == IMAGE) {
      if (vertGroup[idx] != null && vertGroup[idx].texture == null) {
        throw new RuntimeException("PShape3D: when setting texture coordinates in IMAGE mode, the textures need to be assigned first");
      }
      u /= vertGroup[idx].texture.width;
      v /= vertGroup[idx].texture.height; 
    }
    
    texCoordArray[2 * idx + 0] = u;
    texCoordArray[2 * idx + 1] = v;
  }

  
  public void setTexCoord(float[] data) {
    if (updateElement != TEXTURES) {
      throw new RuntimeException("PShape3D: update mode is not set to TEXTURES");
    }
    
    firstUpdateIdx = 0;
    firstUpdateIdx = numVertices - 1;
        
    if (updateTexture != null) {
      for (int i = 0; i < numVertices; i++) 
        if (vertGroup[i] != null) {
          vertGroup[i].texture = updateTexture;
        }
    }
    
    if (a3d.imageMode == IMAGE) {
      float u, v;
      for (int i = 0; i < numVertices; i++) {
        if (vertGroup[i] != null && vertGroup[i].texture == null) {
          throw new RuntimeException("PShape3D: when setting texture coordinates in IMAGE mode, the textures need to be assigned first");
        }      
        
        u = data[2 * i + 0];
        v = data[2 * i + 1];        
        
        u /= vertGroup[i].texture.width;
        v /= vertGroup[i].texture.height;

        texCoordArray[2 * i + 0] = u;
        texCoordArray[2 * i + 1] = v;
      }  
    } else {
      PApplet.arrayCopy(data, texCoordArray);
    }
  }
  
  
  public void setTexCoord(ArrayList<PVector> data) {
    if (updateElement != TEXTURES) {
      throw new RuntimeException("PShape3D: update mode is not set to TEXTURES");
    }

    firstUpdateIdx = 0;
    lastUpdateIdx = numVertices - 1;

    if (updateTexture != null) {
      for (int i = 0; i < numVertices; i++) 
        if (vertGroup[i] != null) {
          vertGroup[i].texture = updateTexture;
        }
    }    
    
    PVector vec;
    for (int i = 0; i < numVertices; i++) {
      vec = (PVector)data.get(i);
      
      if (a3d.imageMode == IMAGE) {
        if (vertGroup[i] != null && vertGroup[i].texture == null) {
          throw new RuntimeException("PShape3D: when setting texture coordinates in IMAGE mode, the textures need to be assigned first");
        }      
        texCoordArray[2 * i + 0] = vec.x / vertGroup[i].texture.width;
        texCoordArray[2 * i + 1] = vec.y / vertGroup[i].texture.height;
      } else {
        texCoordArray[2 * i + 0] = vec.x;
        texCoordArray[2 * i + 1] = vec.y;
      }
    }
  }

  
  ////////////////////////////////////////////////////////////  
  
  // GROUPS   
  
  
  public void setGroup(int gr) {
    if (updateElement != VERTICES) {
      throw new RuntimeException("PShape3D: setGroup() with group number as only argument must be used while updating vertices");
    }
    
    if (firstSetGroup) {
      // The first time the method setGroup is called inside the update vertices block, all the current groups
      // are erased.
      firstSetGroup = false;
      deleteGroups();
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
      throw new RuntimeException("PShape3D: update mode is not set to GROUPS");
    }

    if (groups.size() != gr) {
      throw new RuntimeException("PShape3D: wrong group index");
    }

    if (idx0 < firstUpdateIdx) firstUpdateIdx = idx0;
    if (lastUpdateIdx < idx1) lastUpdateIdx = idx1;
        
    addGroup(idx0, idx1, null);
  }  
  
  
  public void setGroup(int gr, int idx0, int idx1, PTexture tex) {
    if (updateElement != GROUPS) {
      throw new RuntimeException("PShape3D: update mode is not set to GROUPS");
    }

    if (groups.size() != gr) {
      throw new RuntimeException("PShape3D: wrong group index");
    }
        
    if (idx0 < firstUpdateIdx) firstUpdateIdx = idx0;
    if (lastUpdateIdx < idx1) lastUpdateIdx = idx1;
    
    addGroup(idx0, idx1, tex);
  }  
  
  
  public void setGroups(ArrayList<VertexGroup> list) {
    groups = new ArrayList<VertexGroup>(list);
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

  
  public void setGroupTexture(int gr, PTexture tex) {
    VertexGroup group = (VertexGroup)groups.get(gr);
    group.texture = tex;
  }

  
  public PTexture getGroupTexture(int gr) {
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
      colorArray[4 * i + 0] = r;
      colorArray[4 * i + 1] = g;
      colorArray[4 * i + 2] = b;
      colorArray[4 * i + 3] = a;      
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
      normalArray[3 * i + 0] = x;
      normalArray[3 * i + 1] = y;
      normalArray[3 * i + 2] = z;
    }
    endUpdate();
  }  
  
  
  public void setGroupDrawMode(int gr, int mode) {
    VertexGroup group = (VertexGroup)groups.get(gr);    
    if (mode == POINTS) group.glMode = GL11.GL_POINTS;
    else if (mode == POINT_SPRITES)  throw new RuntimeException("PShape3D: point sprites can only be set for entire model");
    else if (mode == LINES) group.glMode = GL11.GL_LINES;
    else if (mode == LINE_STRIP) group.glMode = GL11.GL_LINE_STRIP;
    else if (mode == LINE_LOOP) group.glMode = GL11.GL_LINE_LOOP;
    else if (mode == TRIANGLES) group.glMode = GL11.GL_TRIANGLES; 
    else if (mode == TRIANGLE_FAN) group.glMode = GL11.GL_TRIANGLE_FAN;
    else if (mode == TRIANGLE_STRIP) group.glMode = GL11.GL_TRIANGLE_STRIP;
    else {
      throw new RuntimeException("PShape3D: Unknown draw mode");
    }
  }
  
  
  public void useModelDrawMode() {
    VertexGroup group;
    for (int i = 0; i < groups.size(); i++) {
      group = (VertexGroup)groups.get(i);
      group.glMode = 0;
    }
  }
  
  
  public void setGroupStrokeWeight(int gr, float sw) {
    VertexGroup group = (VertexGroup)groups.get(gr);
    group.sw = sw;
  }
  
  
  protected void initGroups() {
    groups = new ArrayList<VertexGroup>();
    vertGroup = new VertexGroup[numVertices];
    addGroup(0, numVertices - 1, null);
  }
  
  
  protected void addGroup(int idx0, int idx1, PTexture tex) {
    if (0 <= idx0 && idx0 <=  idx1) {
      VertexGroup group = new VertexGroup(idx0, idx1, tex);
      groups.add(group);
      for (int n = idx0; n <= idx1; n++) {
        vertGroup[n] = group;
      }
    }
  }
  
  
  protected void deleteGroups() {
    groups.clear();
    for (int n = 0; n < numVertices; n++) {
       vertGroup[n] = null;
    }
  }
  
  
  ////////////////////////////////////////////////////////////  
  
  // Resize   

  /*
   * This method resizes the model to numVert vertices, and keeps 
   * the data already stored in it by copying to the beginning of the 
   * new buffers. 
   * 
   * TODO: Test!.
   *  
   */
  public void resize(int numVert) {
    // Getting the current data stored in the model.
    float[] tmpVertexArray = new float[numVert * 3];
    float[] tmpColorArray = new float[numVert * 4];
    float[] tmpNormalArray = new float[numVert * 3];
    float[] tmpTexCoordArray = new float[numVert * 2];    
    
    // Getting current data.
    beginUpdate(VERTICES);
    getVertexArrayImpl(tmpVertexArray, 0, numVertices, 0);
    endUpdate();
    
    beginUpdate(COLORS);
    getColorArrayImpl(tmpColorArray, 0, numVertices, 0);
    endUpdate();

    beginUpdate(NORMALS);
    getNormalArrayImpl(tmpNormalArray, 0, numVertices, 0);
    endUpdate();
    
    beginUpdate(TEXTURES);
    getTexCoordArrayImpl(tmpTexCoordArray, 0, numVertices, 0);
    endUpdate();

    int numVert0 = numVertices;
    
    // Recreating the model with the new number of vertices.
    createModel(numVert);
    updateElement = -1;

    // Setting the old data.
    int n = PApplet.min(numVert0, numVert);
    
    beginUpdateImpl(VERTICES, 0, n);
    setVertex(tmpVertexArray);
    endUpdate();

    beginUpdateImpl(COLORS, 0, n);
    setColor(tmpColorArray);
    endUpdate();

    beginUpdateImpl(NORMALS, 0,n);
    setNormal(tmpNormalArray);
    endUpdate();
    
    beginUpdateImpl(TEXTURES, 0, n);
    setTexCoord(tmpTexCoordArray);
    endUpdate();
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
      throw new RuntimeException("PShape3D: Unknown draw mode");
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
      throw new RuntimeException("PShape3D: Unknown draw mode");
    }
    
    if (params.updateMode == STATIC) glUsage = GL11.GL_STATIC_DRAW;
    else if (params.updateMode == DYNAMIC) glUsage = GL11.GL_DYNAMIC_DRAW;
    else {
      throw new RuntimeException("PShape3D: Unknown update mode");
    }
  }
  
  
  ////////////////////////////////////////////////////////////  
  
  // Data allocation, deletion.

  
  void createModel(int numVert) {
    numVertices = numVert;

    initVertexData();
    createVertexBuffer();   
    initColorData();
    createColorBuffer();
    initNormalData();
    createNormalBuffer();
    initTexCoordData();
    createTexCoordBuffer();
    
    initGroups();    
  }
  
  
  protected void initVertexData() {    
    ByteBuffer vbb = ByteBuffer.allocateDirect(numVertices * 3 * SIZEOF_FLOAT);
    vbb.order(ByteOrder.nativeOrder());
    vertexBuffer = vbb.asFloatBuffer();    
    
    vertexArray = new float[numVertices * 3];
    vertexBuffer.put(vertexArray);
    vertexBuffer.flip();    
  }
  
  
  protected void createVertexBuffer() {    
    deleteVertexBuffer();  // Just in case.
    
    gl.glGenBuffers(1, glVertexBufferID, 0);
    gl.glBindBuffer(GL11.GL_ARRAY_BUFFER, glVertexBufferID[0]);
    final int bufferSize = vertexBuffer.capacity() * SIZEOF_FLOAT;
    gl.glBufferData(GL11.GL_ARRAY_BUFFER, bufferSize, vertexBuffer, glUsage);
    gl.glBindBuffer(GL11.GL_ARRAY_BUFFER, 0);
  }
  

  protected void initColorData() {
    ByteBuffer vbb = ByteBuffer.allocateDirect(numVertices * 4 * SIZEOF_FLOAT);
    vbb.order(ByteOrder.nativeOrder());                
    colorBuffer = vbb.asFloatBuffer();          

    colorArray = new float[numVertices * 4];
    colorBuffer.put(colorArray);
    colorBuffer.flip();    
  }  
  
  
  protected void createColorBuffer() {
    deleteColorBuffer();  // Just in case.
    
    gl.glGenBuffers(1, glColorBufferID, 0);
    gl.glBindBuffer(GL11.GL_ARRAY_BUFFER, glColorBufferID[0]);
    final int bufferSize = colorBuffer.capacity() * SIZEOF_FLOAT;    
    gl.glBufferData(GL11.GL_ARRAY_BUFFER, bufferSize, colorBuffer, glUsage);
    gl.glBindBuffer(GL11.GL_ARRAY_BUFFER, 0);
  }
  

  protected void initNormalData() {
    ByteBuffer vbb = ByteBuffer.allocateDirect(numVertices * 3 * SIZEOF_FLOAT);
    vbb.order(ByteOrder.nativeOrder());
    normalBuffer = vbb.asFloatBuffer();    
    
    normalArray = new float[numVertices * 3];
    normalBuffer.put(normalArray);
    normalBuffer.flip();
  }  

  
  protected void createNormalBuffer() {
    deleteNormalBuffer();  // Just in case.
    
    gl.glGenBuffers(1, glNormalBufferID, 0);
    gl.glBindBuffer(GL11.GL_ARRAY_BUFFER, glNormalBufferID[0]);
    final int bufferSize = normalBuffer.capacity() * SIZEOF_FLOAT;    
    gl.glBufferData(GL11.GL_ARRAY_BUFFER, bufferSize, normalBuffer, glUsage);
    gl.glBindBuffer(GL11.GL_ARRAY_BUFFER, 0);
  }
  

  protected void initTexCoordData() {
    ByteBuffer vbb = ByteBuffer.allocateDirect(numVertices * 2 * SIZEOF_FLOAT);
    vbb.order(ByteOrder.nativeOrder());
    texCoordBuffer = vbb.asFloatBuffer();    
    
    texCoordArray = new float[numVertices * 2];
    texCoordBuffer.put(texCoordArray);
    texCoordBuffer.flip();   
  }  
  
  
  protected void createTexCoordBuffer() {
    deleteTexCoordBuffer(); // Just in case.
    
    gl.glGenBuffers(1, glTexCoordBufferID, 0);
    gl.glBindBuffer(GL11.GL_ARRAY_BUFFER, glTexCoordBufferID[0]);
    final int bufferSize = texCoordBuffer.capacity() * SIZEOF_FLOAT;
    gl.glBufferData(GL11.GL_ARRAY_BUFFER, bufferSize, texCoordBuffer, glUsage);
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
  
  
  protected void recreateResource(PGraphicsAndroid3D renderer) {
    createVertexBuffer();   
    createColorBuffer();
    createNormalBuffer();
    createTexCoordBuffer();
  }
  
  
  ///////////////////////////////////////////////////////////  

  // Reimplementing methods inherited from PShape.
  
  
  public void translate(float tx, float ty) {
    translate(tx, ty, 0);
  }


  public void translate(float tx, float ty, float tz) {
    float[] tmpVertexArray = new float[numVertices * 3];
    
    // Getting vertex data.
    beginUpdate(VERTICES);
    getVertexArrayImpl(tmpVertexArray, 0, numVertices, 0);
    endUpdate();
    
    // Translating.
    for (int i = 0; i < numVertices; i++) {
      tmpVertexArray[3 * i + 0] += tx;
      tmpVertexArray[3 * i + 1] += -ty;
      tmpVertexArray[3 * i + 2] += tz;  
    }
    
    // Pushing back to GPU.
    beginUpdateImpl(VERTICES, 0, numVertices - 1);
    setVertex(tmpVertexArray);
    endUpdate();    
  }  
  
  
  public void rotateX(float angle) {
    rotate(angle, 1, 0, 0);
  }


  public void rotateY(float angle) {
    rotate(angle, 0, 1, 0);
  }


  public void rotateZ(float angle) {
    rotate(angle, 0, 0, 1);
  }


  public void rotate(float angle) {
    rotate(angle, 0, 0, 1);
  }


  public void rotate(float angle, float v0, float v1, float v2) {
    // TODO should make sure this vector is normalized, and test that this method works ok.
    
    float[] tmpVertexArray = new float[numVertices * 3];
    
    // Getting vertex data.
    beginUpdate(VERTICES);
    getVertexArrayImpl(tmpVertexArray, 0, numVertices, 0);
    endUpdate();
    
    // Rotating (around xmin, ymin, zmin)
    float c = PApplet.cos(angle);
    float s = PApplet.sin(angle);
    float t = 1.0f - c;
    float[] m = new float[9];
    m[0] = (t*v0*v0) + c;          // 0, 0
    m[1] = (t*v0*v1) - (s*v2);   // 0, 1
    m[2] = (t*v0*v2) + (s*v1); // 0, 2 
    m[3] = (t*v0*v1) + (s*v2); // 1, 0
    m[4] = (t*v1*v1) + c;          // 1, 1
    m[5] =  (t*v1*v2) - (s*v0);  // 1, 2
    m[6] = (t*v0*v2) - (s*v1);   // 2, 0
    m[7] = (t*v1*v2) + (s*v0); // 2, 1 
    m[8] = (t*v2*v2) + c;          // 2, 2
    float x, y, z;
    for (int i = 0; i < numVertices; i++) {
      x = tmpVertexArray[3 * i + 0] - xmin; 
      y = tmpVertexArray[3 * i + 1] - ymin;
      z = tmpVertexArray[3 * i + 2] - zmin;      
      
      tmpVertexArray[3 * i + 0] = m[0] * x + m[1] * y + m[2] * z + xmin; 
      tmpVertexArray[3 * i + 1] = m[3] * x + m[4] * y + m[5] * z + ymin;
      tmpVertexArray[3 * i + 2] = m[6] * x + m[7] * y + m[8] * z + zmin;
    }        
    
    // Pushing back to GPU.
    beginUpdateImpl(VERTICES, 0, numVertices - 1);
    setVertex(tmpVertexArray);
    endUpdate();        
  }

  
  public void scale(float s) {
    scale(s, s, s);
  }


  public void scale(float sx, float sy) {
    scale(sx, sy, 1);
  }


  public void scale(float x, float y, float z) {
    float[] tmpVertexArray = new float[numVertices * 3];
    
    // Getting vertex data.
    beginUpdate(VERTICES);
    getVertexArrayImpl(tmpVertexArray, 0, numVertices, 0);
    endUpdate();
    
    // Scaling.
    for (int i = 0; i < numVertices; i++) {
      tmpVertexArray[3 * i + 0] *= x;
      tmpVertexArray[3 * i + 1] *= y;
      tmpVertexArray[3 * i + 2] *= z;  
    }    
    
    // Pushing back to GPU.
    beginUpdateImpl(VERTICES, 0, numVertices - 1);
    setVertex(tmpVertexArray);
    endUpdate();         
  }
  
  
  public boolean is3D() {
    return true;
  }
  
  
  ///////////////////////////////////////////////////////////  

  // Drawing methods   
   
   public void draw(PGraphics g) {
     draw(g, 0, groups.size() - 1);
   }
   
  
	 public void draw(PGraphics g, int gr) {
	   draw(g, gr, gr);
	 }
  
  
  public void draw(PGraphics g, int gr0, int gr1) {
	  int texTarget = GL11.GL_TEXTURE_2D;
	  PTexture tex;
	  float pointSize;
	  
	  // Setting line width and point size from stroke value.
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
      
      // Setting the stroke wight (line width's in OpenGL terminology) using either the group's weight 
      // or the renderer's weight. 
      if (0 < group.sw) {
        gl.glLineWidth(group.sw);
      } else {
        gl.glLineWidth(a3d.strokeWeight);
      }
      
      if (0 < group.glMode && !pointSprites) {
        // Using the group's vertex mode.
        gl.glDrawArrays(group.glMode, group.first, group.last - group.first + 1);
      } else {
        // Using the overall's vertex mode assigned to the entire model.
        gl.glDrawArrays(glMode, group.first, group.last - group.first + 1);
      }
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
    
    gl.glBindBuffer(GL11.GL_ARRAY_BUFFER, 0);
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
  
  // Vertex groups
  
  static public VertexGroup newVertexGroup(int n0, int n1) {
    return new VertexGroup(n0, n1);  
  }
  

  static public VertexGroup newVertexGroup(int n0, int n1, PTexture tex) {
    return new VertexGroup(n0, n1, tex);  
  }    

  
  static public VertexGroup newVertexGroup(int n0, int n1, int mode, PTexture tex) {
    return new VertexGroup(n0, n1, mode, tex);  
  }        
        

  static public VertexGroup newVertexGroup(int n0, int n1, int mode, float weight, PTexture tex) {
    return new VertexGroup(n0, n1, mode, weight, tex);  
  }        
  
  
	static public class VertexGroup {
    VertexGroup(int n0, int n1) {
      first = n0;
      last = n1;
      glMode = 0;
      sw = 0;
      texture = null;
    }

    VertexGroup(int n0, int n1, PTexture tex) {
      first = n0;
      last = n1; 
      glMode = 0;
      sw = 0;
      texture = tex;
    }

    VertexGroup(int n0, int n1, int mode, PTexture tex) {
      this(n0, n1, mode, 0, tex);
    }    
    
    VertexGroup(int n0, int n1, int mode, float weight, PTexture tex) {
      first = n0;
      last = n1; 
      if (mode == POINTS) glMode = GL11.GL_POINTS;
      else if (mode == POINT_SPRITES)  throw new RuntimeException("PShape3D: point sprites can only be set for entire model");
      else if (mode == LINES) glMode = GL11.GL_LINES;
      else if (mode == LINE_STRIP) glMode = GL11.GL_LINE_STRIP;
      else if (mode == LINE_LOOP) glMode = GL11.GL_LINE_LOOP;
      else if (mode == TRIANGLES) glMode = GL11.GL_TRIANGLES; 
      else if (mode == TRIANGLE_FAN) glMode = GL11.GL_TRIANGLE_FAN;
      else if (mode == TRIANGLE_STRIP) glMode = GL11.GL_TRIANGLE_STRIP;
      else {
        throw new RuntimeException("PShape3D: Unknown draw mode");
      }      
      sw = weight;
      texture = tex;
    }    
    
	  int first;
	  int last;
	  int glMode;
	  float sw;
    PTexture texture;      
	}
	
  ///////////////////////////////////////////////////////////////////////////   
  
  // OBJ loading
	
  protected BufferedReader getBufferedReader(String filename) {
    BufferedReader retval = parent.createReader(filename);
    if (retval != null) {
      return retval;
    } else {
      PApplet.println("Could not find this file " + filename);
      return null;
    }
  }
	
  
  protected void parseOBJ(BufferedReader reader, ArrayList<PVector> vertices, ArrayList<PVector> normals, ArrayList<PVector> textures, ArrayList<OBJFace> faces, ArrayList<OBJMaterial> materials) {
    
  }
  
  
  protected void recordOBJ(ArrayList<PVector> vertices, ArrayList<PVector> normals, ArrayList<PVector> textures, ArrayList<OBJFace> faces, ArrayList<OBJMaterial> materials) {
    int mtlIdxCur = -1;
    a3d.beginShapeRecorderImpl();    
    for (int i = 0; i < faces.size(); i++) {
      OBJFace face = (OBJFace) faces.get(i);
      OBJMaterial mtl = null;
      
      // Getting current material.
      if (mtlIdxCur != face.matIdx) {
        mtlIdxCur = PApplet.max(0, face.matIdx); // To make sure that at least we get the default material.
        
        mtl = (OBJMaterial) materials.get(mtlIdxCur);

         // Setting colors.
        a3d.specular(mtl.ks.x * 255.0f, mtl.ks.y * 255.0f, mtl.ks.z * 255.0f);
        a3d.ambient(mtl.ka.x * 255.0f, mtl.ka.y * 255.0f, mtl.ka.z * 255.0f);
        a3d.fill(mtl.kd.x * 255.0f, mtl.kd.y * 255.0f, mtl.kd.z * 255.0f, mtl.d * 255.0f);
        a3d.shininess(mtl.ns);
      }

      // Recording current face.
      a3d.beginShape();
      for (int j = 0; j < face.vertIdx.size(); j++){
        int vertIdx = face.vertIdx.get(j).intValue() - 1;
        int normIdx = face.normIdx.get(j).intValue() - 1;
        PVector vert = (PVector) vertices.get(vertIdx);
        PVector norms = (PVector) normals.get(normIdx);
        
        if (mtl != null && mtl.kdMap != null) {
          // This face is textured.
          int texIdx = face.texIdx.get(j).intValue() - 1;
          PVector tex = (PVector) textures.get(texIdx);
          
          a3d.texture(mtl.kdMap);
          a3d.normal(norms.x, norms.y, norms.z);
          a3d.vertex(vert.x, vert.y, vert.z, tex.x, tex.y);
        } else {
          // This face is not textured.
          a3d.normal(norms.x, norms.y, norms.z);
          a3d.vertex(vert.x, vert.y, vert.z);          
        }
      } 
      a3d.endShape(CLOSE);
    }
    a3d.endShapeRecorderImpl(this);
  }
	
  
  protected class OBJFace {
    ArrayList<Integer> vertIdx;
    ArrayList<Integer> texIdx;
    ArrayList<Integer> normIdx;
    int matIdx;
    
    OBJFace() {
      vertIdx = new ArrayList<Integer>();
      texIdx = new ArrayList<Integer>();
      normIdx = new ArrayList<Integer>();
      matIdx = -1;
    }
  }

  protected class OBJMaterial {
    String name;
    PVector ka;
    PVector kd;
    PVector ks;
    float d;
    float ns;
    PImage kdMap;
    
    OBJMaterial() {
      name = "default";
      ka = new PVector(0.5f, 0.5f, 0.5f);
      kd = new PVector(0.5f, 0.5f, 0.5f);
      ks = new PVector(0.5f, 0.5f, 0.5f);
      d = 1.0f;
      ns = 0.0f;
      kdMap = null;
    }
  }
}
