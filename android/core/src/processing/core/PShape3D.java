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
import java.util.Arrays;
import java.util.Hashtable;
import java.io.BufferedReader;

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
  protected PApplet papplet;    
  protected GL11 gl;  
  protected PGraphicsAndroid3D a3d;
   
  protected int numTexBuffers;
  protected int glUsage;
  
  protected int glVertexBufferID;
  protected int glColorBufferID;
  protected int glNormalBufferID;
  protected int[] glTexCoordBufferID = new int[PGraphicsAndroid3D.MAX_TEXTURES];
  
  protected FloatBuffer vertexBuffer;
  protected FloatBuffer colorBuffer;
  protected FloatBuffer normalBuffer;
  protected FloatBuffer texCoordBuffer;
  
  // Arrays for setting/getting vertices, colors, normals, and 
  // texture coordinates.
  public float[] vertices;
  public float[] colors;
  public float[] normals;
  public float[] texcoords;  
  
  // Child-only properties: first and last vertex, draw mode, point sprites 
  // and textures. Stroke weight is inherited from PShape.
  protected int firstVertex;
  protected int lastVertex; 
  protected int glMode;
  protected boolean pointSprites;  
  protected PImage[] textures;  
  
  // Bounding box: 
  protected float xmin, xmax;
  protected float ymin, ymax;
  protected float zmin, zmax;

  // PShape3D child associated to each vertex.
  protected PShape3D[] vertexChild;  
  
  protected boolean vertexColor = true;
  protected float ptDistAtt[] = { 1.0f, 0.0f, 0.01f, 1.0f };
  
  protected PTexture[] renderTextures = new PTexture[PGraphicsAndroid3D.MAX_TEXTURES];  
  protected boolean[] texCoordSet = new boolean[PGraphicsAndroid3D.MAX_TEXTURES];  

  // Elements handled by PShape3D (vertices, normals, color, texture coordinates).
  protected static final int VERTICES = 0;
  protected static final int NORMALS = 1;
  protected static final int COLORS = 2;
  protected static final int TEXCOORDS = 3; 
  
  protected int updateElement;
  protected int updateTexunit;
  protected int firstUpdateIdx;
  protected int lastUpdateIdx;

  boolean readFromOBJ = false;
  ArrayList<PVector> objVertices; 
  ArrayList<PVector> objNormal; 
  ArrayList<PVector> objTexCoords;    
  ArrayList<OBJFace> objFaces;
  ArrayList<OBJMaterial> objMaterials;
  
  ////////////////////////////////////////////////////////////

  // Constructors.

  public PShape3D() {
    style = false;
  }
  
  public PShape3D(PApplet parent) {
    this.papplet = parent;
    a3d = (PGraphicsAndroid3D)parent.g;

    glVertexBufferID = 0;
    glColorBufferID = 0;
    glNormalBufferID = 0;
    java.util.Arrays.fill(glTexCoordBufferID, 0);
    
    style = false;
  }
  
  public PShape3D(PApplet parent, int numVert) {
    this(parent, numVert, new Parameters()); 
  }  
  
  public PShape3D(PApplet parent, String filename, int mode) {
    this.papplet = parent;
    a3d = (PGraphicsAndroid3D)parent.g;

    this.family = PShape.GROUP;
    glVertexBufferID = 0;
    glColorBufferID = 0;
    glNormalBufferID = 0;
    java.util.Arrays.fill(glTexCoordBufferID, 0);
    
    style = false;
    
    // Checking we have all we need:
    gl = a3d.gl11;
    if (gl == null) {
      throw new RuntimeException("PShape3D: OpenGL ES 1.1 required");
    }
    if (!PGraphicsAndroid3D.vboSupported) {
       throw new RuntimeException("PShape3D: Vertex Buffer Objects are not available");
    }

    readFromOBJ = true;
    objVertices = new ArrayList<PVector>(); 
    objNormal = new ArrayList<PVector>(); 
    objTexCoords = new ArrayList<PVector>();    
    objFaces = new ArrayList<OBJFace>();
    objMaterials = new ArrayList<OBJMaterial>();
    BufferedReader reader = getBufferedReader(filename);
    if (reader == null) {
      throw new RuntimeException("PShape3D: Cannot read source file");
    }
    
    // Setting parameters.
    Parameters params = PShape3D.newParameters(TRIANGLES, mode); 
    setParameters(params);
    
    parseOBJ(reader, objVertices, objNormal, objTexCoords, objFaces, objMaterials);
    
    // Putting the number of vertices retrieved from the OBJ file in the vertex count
    // field, although the actual geometry hasn't been sent to the VBOs yet.
    vertexCount = objVertices.size();
  }  
  
  
  public PShape3D(PApplet parent, int size, Parameters params) {
    this.papplet = parent;
    a3d = (PGraphicsAndroid3D)parent.g;
    
    glVertexBufferID = 0;
    glColorBufferID = 0;
    glNormalBufferID = 0;
    java.util.Arrays.fill(glTexCoordBufferID, 0);    
    
    style = false;
    
    initShape(size, params);
  }
  

  public void delete() {
    if (family == PShape.GROUP) {
      deleteVertexBuffer();
      deleteColorBuffer();
      deleteTexCoordBuffer();
      deleteNormalBuffer();
    }
  }

  ////////////////////////////////////////////////////////////

  // load/update/set/get methods
 
  
  public void loadVertices() {
    loadVertices(0, vertexCount - 1);
  }

  
  public void loadVertices(int first, int last) {
    if (last < first || first < 0 || vertexCount <= last) {
      PGraphics.showWarning("PShape3D: wrong vertex index");
      updateElement = -1;
      return;  
    }
    
    if (updateElement != -1) {
      PGraphics.showWarning("PShape3D: can load only one type of data at the time");
      return;        
    }
    
    updateElement = VERTICES;
    firstUpdateIdx = first;
    lastUpdateIdx = last;
    
    gl.glBindBuffer(GL11.GL_ARRAY_BUFFER, glVertexBufferID);      
    
    int offset = first * 3;
    int size = (last - first + 1) * 3;
    vertexBuffer.limit(vertexBuffer.capacity());      
    vertexBuffer.rewind();
    vertexBuffer.get(vertices, offset, size);    
  }  
  
  
  public void updateVertices() {
    if (updateElement == VERTICES) {
      int offset = firstUpdateIdx * 3;
      int size = (lastUpdateIdx - firstUpdateIdx + 1) * 3;
    
      updateBounds();
      
      vertexBuffer.position(0);
      vertexBuffer.put(vertices, offset, size);
      vertexBuffer.flip();
      
      gl.glBufferSubData(GL11.GL_ARRAY_BUFFER, offset * PGraphicsAndroid3D.SIZEOF_FLOAT, 
                         size * PGraphicsAndroid3D.SIZEOF_FLOAT, vertexBuffer);
      gl.glBindBuffer(GL11.GL_ARRAY_BUFFER, 0);
    
      updateElement = -1;
    } else {
      PGraphics.showWarning("PShape3D: need to call loadVertices() first");
    }
  }
  
  
  public void loadColors() {
    loadColors(0, vertexCount - 1);
  }

  
  public void loadColors(int first, int last) {
    if (last < first || first < 0 || vertexCount <= last) {
      PGraphics.showWarning("PShape3D: wrong vertex index");
      updateElement = -1;
      return;  
    }
    
    if (updateElement != -1) {
      PGraphics.showWarning("PShape3D: can load only one type of data at the time");
      return;        
    }
    
    updateElement = COLORS;
    firstUpdateIdx = first;
    lastUpdateIdx = last;
    
    gl.glBindBuffer(GL11.GL_ARRAY_BUFFER, glColorBufferID);
    
    int offset = first * 4;
    int size = (last - first + 1) * 4;
    colorBuffer.limit(colorBuffer.capacity());
    colorBuffer.rewind();
    colorBuffer.get(colors, offset, size);    
  }
  
  
  public void updateColors() {
    if (updateElement == COLORS) {    
      int offset = firstUpdateIdx * 4;
      int size = (lastUpdateIdx - firstUpdateIdx + 1) * 4;
          
      colorBuffer.position(0);      
      colorBuffer.put(colors, offset, size);
      colorBuffer.flip();
  
      gl.glBufferSubData(GL11.GL_ARRAY_BUFFER, offset * PGraphicsAndroid3D.SIZEOF_FLOAT, 
                                               size * PGraphicsAndroid3D.SIZEOF_FLOAT, colorBuffer);
      gl.glBindBuffer(GL11.GL_ARRAY_BUFFER, 0);
      
      updateElement = -1;      
    } else {
      PGraphics.showWarning("PShape3D: need to call loadColors() first");
    }    
  }
  

  public void loadNormals() {
    loadNormals(0, vertexCount - 1);
  }  
  
  
  public void loadNormals(int first, int last) {
    if (last < first || first < 0 || vertexCount <= last) {
      PGraphics.showWarning("PShape3D: wrong vertex index");
      updateElement = -1;
      return;  
    }
    
    if (updateElement != -1) {
      PGraphics.showWarning("PShape3D: can load only one type of data at the time");
      return;        
    }    
    
    updateElement = NORMALS;
    firstUpdateIdx = first;
    lastUpdateIdx = last;
    
    gl.glBindBuffer(GL11.GL_ARRAY_BUFFER, glNormalBufferID);
    
    int offset = first * 3;
    int size = (last - first + 1) * 3;
    normalBuffer.limit(normalBuffer.capacity());      
    normalBuffer.rewind();      
    normalBuffer.get(normals, offset, size);    
  }
  
  
  public void updateNormals() {
    if (updateElement == NORMALS) {    
      int offset = firstUpdateIdx * 3;
      int size = (lastUpdateIdx - firstUpdateIdx + 1) * 3;
      
      normalBuffer.position(0);      
      normalBuffer.put(normals, offset, size);
      normalBuffer.flip();
    
      gl.glBufferSubData(GL11.GL_ARRAY_BUFFER, offset * PGraphicsAndroid3D.SIZEOF_FLOAT, 
                         size * PGraphicsAndroid3D.SIZEOF_FLOAT, normalBuffer);
      gl.glBindBuffer(GL11.GL_ARRAY_BUFFER, 0);
      
      updateElement = -1;      
    } else {
      PGraphics.showWarning("PShape3D: need to call loadNormals() first");
    }      
  }  
  
  
  public void loadTexcoords() {
    loadTexcoords(0);
  }
  
  
  public void loadTexcoords(int unit) {
    loadTexcoords(unit, 0, vertexCount - 1);
  }  
  
  
  protected void loadTexcoords(int unit, int first, int last) {
    if (last < first || first < 0 || vertexCount <= last) {
      PGraphics.showWarning("PShape3D: wrong vertex index");
      updateElement = -1;
      return;  
    }
    
    if (updateElement != -1) {
      PGraphics.showWarning("PShape3D: can load only one type of data at the time");
      return;        
    }    
    
    if (PGraphicsAndroid3D.maxTextureUnits <= unit) {
      PGraphics.showWarning("PShape3D: wrong texture unit");
      return;
    }
    
    updateElement = TEXCOORDS;
    firstUpdateIdx = first;
    lastUpdateIdx = last;
    
    if (numTexBuffers <= unit) {
      addTexBuffers(unit - numTexBuffers + 1);
    }
    
    gl.glBindBuffer(GL11.GL_ARRAY_BUFFER, glTexCoordBufferID[unit]);
        
    int offset = first * 2;
    int size = (last - first + 1) * 2;
    texCoordBuffer.limit(texCoordBuffer.capacity());      
    texCoordBuffer.rewind();
    texCoordBuffer.get(texcoords, offset, size);
    
    //normalizeTexcoords(unit);
    
    updateTexunit = unit;    
  }  
  
  
  public void updateTexcoords() {
    if (updateElement == TEXCOORDS) {    
      int offset = firstUpdateIdx * 2;
      int size = (lastUpdateIdx - firstUpdateIdx + 1) * 2;
      
      //unnormalizeTexcoords(updateTexunit);
      
      texCoordBuffer.position(0);      
      texCoordBuffer.put(texcoords, offset, size);
      texCoordBuffer.flip();
      
      gl.glBufferSubData(GL11.GL_ARRAY_BUFFER, offset * PGraphicsAndroid3D.SIZEOF_FLOAT, 
                         size * PGraphicsAndroid3D.SIZEOF_FLOAT, texCoordBuffer);
      gl.glBindBuffer(GL11.GL_ARRAY_BUFFER, 0);
      
      texCoordSet[updateTexunit] = true;
      
      updateElement = -1;      
    } else {
      PGraphics.showWarning("PShape3D: need to call loadTexcoords() first");
    }       
  }  
  
  
  public float[] get(int idx) {
    float[] v = null;
    if (updateElement == VERTICES) {
      v = new float[3];
      PApplet.arrayCopy(vertices, 3 * idx, v, 0, 3);
    } else if (updateElement == COLORS) {
      v = new float[4];
      PApplet.arrayCopy(colors, 4 * idx, v, 0, 4);    
    } else if (updateElement == NORMALS) {
      v = new float[3];
      PApplet.arrayCopy(normals, 3 * idx, v, 0, 3);    
    } else if (updateElement == TEXCOORDS) {
      v = new float[2];
      PApplet.arrayCopy(texcoords, 2 * idx, v, 0, 2);        
    }
    return v;
  }
  
  
  public void set(int idx, float[] v) {
    if (updateElement == VERTICES) {
      PApplet.arrayCopy(v, 0, vertices, 3 * idx, 3);
    } else if (updateElement == COLORS) {
      PApplet.arrayCopy(v, 0, colors, 4 * idx, 4);    
    } else if (updateElement == NORMALS) {
      PApplet.arrayCopy(v, 0, normals, 3 * idx, 3);    
    } else if (updateElement == TEXCOORDS) {
      PApplet.arrayCopy(v, 0, texcoords, 2 * idx, 2);        
    }    
  }

  
  public void set(int idx, int c) {
    set(idx, rgba(c)); 
  }
  
  
  public void set(int idx, float x, float y) {
    if (updateElement == VERTICES) {
      set(idx, new float[] {x, y, 0});
    } else if (updateElement == TEXCOORDS) {
      set(idx, new float[] {x, y});    
    }
  }
  
  
  public void set(int idx, float x, float y, float z) {
    if (updateElement == VERTICES) {
      set(idx, new float[] {x, y, z});
    } else if (updateElement == NORMALS) {
      set(idx, new float[] {x, y, z});    
    } else if (updateElement == COLORS) {
      set(idx, new float[] {x, y, z, 1});
    }
  }

  
  public void set(int idx, float x, float y, float z, float w) {
    if (updateElement == COLORS) {
      set(idx, new float[] {x, y, z, w});
    }
  }

  
  static public int color(float[] rgba) {
    int r = (int)(rgba[0] * 255);
    int g = (int)(rgba[1] * 255);
    int b = (int)(rgba[2] * 255);
    int a = (int)(rgba[3] * 255);
    
    return a << 24 | r << 16 | g << 8 | b;
  }
  
  
  static public float[] rgba(int c) {
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
  
  
  protected void updateBounds() {
    // TODO: extract minimum and maximum values using some sorting algorithm. 
    for (int i = firstUpdateIdx; i <= lastUpdateIdx; i++) {
      updateBounds(vertices[3 * i + 0], vertices[3 * i + 1], vertices[3 * i + 2]);      
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
  
  
  // Convert texture coordinates as read from the GPU to normalized values
  // expected by the user. 
  protected void normalizeTexcoords(int unit) {
    PTexture tex;
    float u, v;
    
    PTexture tex0 = null;
    float uscale = 1.0f;
    float vscale = 1.0f;
    float cx = 0.0f;
    float sx = +1.0f;
    float cy = 0.0f;
    float sy = +1.0f;
    for (int i = firstUpdateIdx; i <= lastUpdateIdx; i++) {
      if (vertexChild[i] != null && vertexChild[i].textures[unit] != null) {
        tex = vertexChild[i].textures[unit].getTexture();
        if (tex == null) {
          tex = vertexChild[i].textures[unit].createTexture();
        }        
        
        if (tex != null && tex != tex0) {
          uscale = 1.0f;
          vscale = 1.0f;
          cx = 0.0f;
          sx = +1.0f;
          cy = 0.0f;
          sy = +1.0f;

          if (tex != null) {
            if (tex.isFlippedX()) {
              cx = 1.0f;      
              sx = -1.0f;
            }
          
            if (tex.isFlippedY()) {
              cy = 1.0f;      
              sy = -1.0f;
            }
          
            uscale *= tex.getMaxTexCoordU();
            vscale *= tex.getMaxTexCoordV();
          }
          tex0 = tex;
        }

        u = texcoords[2 * i + 0];
        v = texcoords[2 * i + 1];
        
        // Inverting the texture coordinate transformation.
        u = (u / uscale - cx) / sx;
        v = (v / vscale - cy) / sy;          
  
        texcoords[2 * i + 0] = u;
        texcoords[2 * i + 1] = v;        
      }  
    }    
  }
  
  
  // Convert texture coordinates given by the user to values required by
  // the GPU (non-necessarily normalized because of texture sizes being smaller
  // than image sizes, for instance). 
  protected void unnormalizeTexcoords(int unit) {
    PTexture tex;
    float u, v;
    
    PTexture tex0 = null;
    float uscale = 1.0f;
    float vscale = 1.0f;
    float cx = 0.0f;
    float sx = +1.0f;
    float cy = 0.0f;
    float sy = +1.0f;
    for (int i = firstUpdateIdx; i <= lastUpdateIdx; i++) {
      if (vertexChild[i] != null && vertexChild[i].textures[unit] != null) {
        tex = vertexChild[i].textures[unit].getTexture();
        if (tex == null) {
          tex = vertexChild[i].textures[unit].createTexture();
        }        
        
        if (tex != tex0) {
          uscale = 1.0f;
          vscale = 1.0f;
          cx = 0.0f;
          sx = +1.0f;
          cy = 0.0f;
          sy = +1.0f;
          
          if (tex != null) {
            if (tex.isFlippedX()) {
              cx = 1.0f;      
              sx = -1.0f;
            }
          
            if (tex.isFlippedY()) {
              cy = 1.0f;      
              sy = -1.0f;
            }
          
            uscale *= tex.getMaxTexCoordU();
            vscale *= tex.getMaxTexCoordV();
          }
          tex0 = tex;
        }

        u = texcoords[2 * i + 0];
        v = texcoords[2 * i + 1];
        
        u = (cx +  sx * u) * uscale;
        v = (cy +  sy * v) * vscale;
        
        texcoords[2 * i + 0] = u;
        texcoords[2 * i + 1] = v;        
      }  
    }    
  }
  
  ////////////////////////////////////////////////////////////  
  
  // Children   
  
  
  static public PShape createChild(int n0, int n1) {
    return createChild(null, n0, n1, POINTS, 0, null);
  }   
  
  
  static public PShape createChild(String name, int n0, int n1) {
    return createChild(name, n0, n1, POINTS, 0, null);
  }  
  
  
  static public PShape createChild(String name, int n0, int n1, int mode) {
    return createChild(name, n0, n1, mode, 0, null);
  }
  
  
  static public PShape createChild(String name, int n0, int n1, int mode, float weight) {
    return createChild(name, n0, n1, mode, weight, null);
  }
  
  
  static public PShape createChild(String name, int n0, int n1, int mode, float weight, PImage[] tex) {
    PShape3D child = new PShape3D();
    child.family = PShape.GEOMETRY;
    child.name = name;
    child.firstVertex = n0;
    child.lastVertex = n1;
    child.setDrawModeImpl(mode);    
    child.strokeWeight = weight;
    child.textures = new PImage[PGraphicsAndroid3D.MAX_TEXTURES];
    java.util.Arrays.fill(child.textures, null);    
    if (tex != null) {
      int n = PApplet.min(tex.length, child.textures.length);
      PApplet.arrayCopy(tex, 0, child.textures, 0, n);
    }
    return child;
  }

  
  public void addChild(String name, int n0, int n1) {
    PShape child = createChild(name, n0, n1, getDrawModeImpl());
    addChild(child);
  }
  
  
  public void addChild(String name, int n0, int n1, int mode) {
    PShape child = createChild(name, n0, n1, mode);
    addChild(child);
  }
  
  
  public void addChild(String name, int n0, int n1, int mode, float weight) {
    PShape child = createChild(name, n0, n1, mode, weight);
    addChild(child);
  }
  
  
  public void addChild(String name, int n0, int n1, int mode, float weight, PImage[] tex) {
    PShape child = createChild(name, n0, n1, mode, weight, tex);
    addChild(child);
  }
      
  
  public void addChild(PShape who) {
    if (family == GROUP) {
      super.addChild(who);
      PShape3D who3d = (PShape3D)who;
      who3d.papplet = papplet;
      who3d.a3d = a3d;
      who3d.gl = gl;
      for (int n = who3d.firstVertex; n <= who3d.lastVertex; n++) {
        vertexChild[n] = who3d;
      }       
    } else {
      PGraphics.showWarning("PShape3D: Child shapes can also be added to the root shape.");
    }  
  }

  
  protected void addDefaultChild() {
    PShape child = createChild("Default", 0, vertexCount - 1, getDrawModeImpl(), 0, null);
    addChild(child);
  }
  
  
  public int getFirstVertex() {
    if (family == GROUP) {
      return getFirstVertex(0);
    } else { 
      return firstVertex;
    }
  }

  
  public int getFirstVertex(int idx) {
    if (0 <= idx && idx < childCount) {
      return ((PShape3D)children[idx]).firstVertex;
    }      
    return -1;
  }  
  
  
  public void setFirstVertex(int n0) {
    if (family == GROUP) {
      setFirstVertex(0, n0);
    } else { 
      firstVertex = n0;
    }    
  }  
  
  
  public void setFirstVertex(int idx, int n0) {
    if (0 <= idx && idx < childCount) {
      ((PShape3D)children[idx]).firstVertex = n0;
    }     
  }
  
  
  public int getLastVertex() {
    if (family == GROUP) {
      return getLastVertex(0);
    } else { 
      return lastVertex;
    }
  }
   
  
  public int getLastVertex(int idx) {
    if (0 <= idx && idx < childCount) {
      return ((PShape3D)children[idx]).lastVertex;    
    }
    return -1;    
  }
  
  
  public void setLastVertex(int n1) {
    if (family == GROUP) {
      setLastVertex(0, n1);
    } else { 
      lastVertex = n1;
    }    
  }  
  
  
  public void setLastVertex(int idx, int n1) {
    if (0 <= idx && idx < childCount) {
      ((PShape3D)children[idx]).firstVertex = n1;
    }     
  }
  
  
  public void setDrawMode(int mode) {
    if (family == GROUP) {
      setDrawModeImpl(mode);
      for (int n = 0; n < childCount; n++) {
        setDrawMode(n, mode);
      }
    } else { 
      setDrawModeImpl(mode);
    }     
  }
  
  
  public void setDrawMode(int idx, int mode) {
    if (0 <= idx && idx < childCount) {
      ((PShape3D)children[idx]).setDrawModeImpl(mode);
    }
  }
  
  
  protected void setDrawModeImpl(int mode) {
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
  
  
  public int getDrawMode() {
    if (family == GROUP) {
      return getDrawMode(0);
    } else { 
      return getDrawModeImpl();
    }     
  }
  
  
  public int getDrawMode(int idx) {
    if (0 <= idx && idx < childCount) {
      return ((PShape3D)children[idx]).getDrawModeImpl();
    }
    return -1;
  }
  
  
  protected int getDrawModeImpl() {
    if (glMode == GL11.GL_POINTS) {
      if (pointSprites) return POINT_SPRITES;
      else return POINTS;
    } else if (glMode == GL11.GL_LINES) return LINES;
    else if (glMode == GL11.GL_LINE_STRIP) return LINE_STRIP;
    else if (glMode == GL11.GL_LINE_LOOP) return LINE_LOOP;
    else if (glMode == GL11.GL_TRIANGLES) return TRIANGLES; 
    else if (glMode == GL11.GL_TRIANGLE_FAN) return TRIANGLE_FAN;
    else if (glMode == GL11.GL_TRIANGLE_STRIP) return TRIANGLE_STRIP;
    else return -1;
  }  
  
  
  public void setTexture(PImage tex) {
    if (family == GROUP) {
      if (children == null) {
        addDefaultChild();
      }
      setTexture(0, tex);
    } else {
      setTextureImpl(tex, 0);
    }      
  }
  
  
  public void setTexture(PImage tex0, PImage tex1) {
    if (family == GROUP) {
      if (children == null) {
        addDefaultChild();
      }      
      setTexture(0, tex0, tex1);
    } else {
      setTextureImpl(tex0, 0);
      setTextureImpl(tex1, 1);
    }     
  }  
  
  
  public void setTexture(PImage tex0, PImage tex1, PImage tex2) {
    if (family == GROUP) {
      if (children == null) {
        addDefaultChild();
      }      
      setTexture(0, tex0, tex1, tex2);
    } else {
      setTextureImpl(tex0, 0);
      setTextureImpl(tex1, 1);
      setTextureImpl(tex2, 2);
    }    
  }  
  
  
  public void setTexture(PImage tex0, PImage tex1, PImage tex2, PImage tex3) {
    if (family == GROUP) {
      if (children == null) {
        addDefaultChild();
      }      
      setTexture(0, tex0, tex1, tex2, tex3);
    } else {
      setTextureImpl(tex0, 0);
      setTextureImpl(tex1, 1);
      setTextureImpl(tex2, 2);
      setTextureImpl(tex3, 3);      
    }
  }  
  
  
  public void setTexture(PImage[] tex) {
    if (family == GROUP) {
      if (children == null) {
        addDefaultChild();
      }      
      setTexture(0, tex);
    } else {
      for (int i = 0; i < tex.length; i++) {
        setTextureImpl(tex[i], i);
      }
    }
  }   
  
  
  public void setTexture(int idx, PImage tex) {
    if (0 <= idx && idx < childCount) {
      PShape3D child = (PShape3D)children[idx];
      child.setTextureImpl(tex, 0);
    }    
  }
  
  
  public void setTexture(int idx, PImage tex0, PImage tex1) {
    if (0 <= idx && idx < childCount) {
      PShape3D child = (PShape3D)children[idx];
      child.setTextureImpl(tex0, 0);
      child.setTextureImpl(tex1, 1);
    }        
  }  
  
  
  public void setTexture(int idx, PImage tex0, PImage tex1, PImage tex2) {
    if (0 <= idx && idx < childCount) {
      PShape3D child = (PShape3D)children[idx];
      child.setTextureImpl(tex0, 0);
      child.setTextureImpl(tex1, 1);
      child.setTextureImpl(tex2, 2);      
    }        
  }  
  
  
  public void setTexture(int idx, PImage tex0, PImage tex1, PImage tex2, PImage tex3) {
    if (0 <= idx && idx < childCount) {
      PShape3D child = (PShape3D)children[idx];
      child.setTextureImpl(tex0, 0);
      child.setTextureImpl(tex1, 1);
      child.setTextureImpl(tex2, 2);
      child.setTextureImpl(tex3, 3);      
    }            
  }  
  
  
  public void setTexture(int idx, PImage[] tex) {
    if (0 <= idx && idx < childCount) {
      PShape3D child = (PShape3D)children[idx];    
      for (int i = 0; i < tex.length; i++) {
        child.setTextureImpl(tex[i], i);
      }    
    }
  }     
 
  
  public void setTextureImpl(PImage tex, int unit) {
    if (unit < 0 || PGraphicsAndroid3D.maxTextureUnits <= unit) {
      System.err.println("PShape3D: Wrong texture unit.");
      return;
    }
    
    PShape3D p3d = (PShape3D)parent;
    
    if (p3d.numTexBuffers <= unit) {
      p3d.addTexBuffers(unit - p3d.numTexBuffers + 1);
    }
    
    if (tex == null) {
      throw new RuntimeException("PShape3D: trying to set null texture.");
    } 
    
    if  (p3d.texCoordSet[unit]) {
      // Ok, setting a new texture, when texture coordinates have already been set. 
      // What is the problem? the new texture might have different max UV coords, 
      // flippedX/Y values, so the texture coordinates need to be updated accordingly...
      
      // The way to do it is just load the texcoords array (in the parent), which
      // will be normalized with the old texture...
      p3d.loadTexcoords(unit, firstVertex, lastVertex);
      // ... then replacing the old texture with the new and...
      textures[unit] = tex;
      // finally, updating the texture coordinats, step in which the texcoords
      // array is unnormalized, this time using the new texture.
      p3d.updateTexcoords();
    } else {
      textures[unit] = tex;  
    }    
  }
   
  
  public PImage[] getTexture() {
    if (family == GROUP) {
      return getTexture(0);
    } else {
      return textures;
    }          
  }
  
  
  public PImage[] getTexture(int idx) {
    if (0 <= idx && idx < childCount) {
      return ((PShape3D)children[idx]).textures;
    }
    return null;
  }
  
  
  public float getStrokeWeight() {
    if (family == GROUP) {
      return getStrokeWeight(0);
    } else { 
      return strokeWeight;
    }
  }
  
  
  public float getStrokeWeight(int idx) {
    if (0 <= idx && idx < childCount) {
      return ((PShape3D)children[idx]).strokeWeight;
    }
    return 0;
  }
  
  
  
  public void setStrokeWeight(float sw) {
    if (family == GROUP) {
      setStrokeWeight(0, sw);
    } else { 
      strokeWeight = sw;
    }
  }
  
  
  public void setStrokeWeight(int idx, float sw) {
    if (0 <= idx && idx < childCount) {
      ((PShape3D)children[idx]).strokeWeight = sw;
    }
  }  

  
  public void setColor(int c) {
    setColor(rgba(c));
  }    
  
  
  public void setColor(float r, float g, float b, float a) {
    setColor(new float[] {r, g, b, a});
  }  
  
  
  public void setColor(float[] c) {
    if (family == GROUP) {
      setColor(0, c);
    } else {
      setColorImpl(c);
    }
  }

  
  public void setColor(int idx, int c) {
    setColor(idx, rgba(c));
  }    
  
  
  public void setColor(int idx, float r, float g, float b, float a) {
    setColor(idx, new float[] {r, g, b, a}); 
  }  
  
  
  public void setColor(int idx, float[] c) {
    if (0 <= idx && idx < childCount) {
      ((PShape3D)children[idx]).setColorImpl(c);
    }
  } 
  
  
  protected void setColorImpl(float[] c) {
    PShape3D p = (PShape3D)parent;
    p.loadColors(firstVertex, lastVertex);
    for (int i = firstVertex; i <= lastVertex; i++) {
      p.set(i, c);
    }
    p.updateColors();
  }
  
  
  public void setNormal(float nx, float ny, float nz) {
    setNormal(new float[] {nx, ny, nz});  
  }  
  
  
  public void setNormal(float[] n) {
    if (family == GROUP) {
      setNormal(0, n);
    } else {
      setNormalImpl(n);
    }
  }

  
  public void setNormal(int idx, float nx, float ny, float nz) {
    setNormal(idx, new float[] {nx, ny, nz});  
  } 
  

  public void setNormal(int idx, float[] n) {
    if (0 <= idx && idx < childCount) {
      ((PShape3D)children[idx]).setNormalImpl(n);
    }    
  } 
    
  
  protected void setNormalImpl(float[] n) {
    PShape3D p = (PShape3D)parent;
    p.loadNormals(firstVertex, lastVertex);
    for (int i = firstVertex; i <= lastVertex; i++) {
      p.set(i, n);
    }
    p.updateNormals();
  }  
  
  
  protected void optimizeChildren() {
    if (children == null) {
      addDefaultChild();
    } else {
      PShape3D child0, child1;

      // Expanding identical, contiguous children.     
      int ndiff = 1;
      child0 = (PShape3D)children[0];
      for (int i = 1; i < childCount; i++) {
        child1 = (PShape3D)children[i];
        if (child0.equalTo(child1)) {
          child0.lastVertex = child1.lastVertex;       // Extending child0.
          child1.firstVertex = child1.lastVertex = -1; // Marking for deletion.
        } else {
          child0 = child1;
          ndiff++;
        }
      }
      
      // Deleting superfluous groups.
      PShape[] temp = new PShape[ndiff]; 
      int n = 0;
      for (int i = 0; i < childCount; i++) {
        child1 = (PShape3D)children[i];      
        if (child1.lastVertex == -1 && child1.getName() != null && nameTable != null) {
          nameTable.remove(child1.getName());
        } else {
          temp[n++] = child1;
        }
      }      
      children = temp;
      childCount = ndiff;
    }
  }

  
  protected void initChildren() {
    children = null;
    vertexChild = new PShape3D[vertexCount];
  }
  
  
  protected boolean equalTo(PShape3D child) {
    boolean res = family == child.family && glMode == child.glMode && strokeWeight == child.strokeWeight; 
    if (!res) return false;
    for (int i = 0; i < textures.length; i++) {
      if (textures[i] != child.textures[i]) {
        res = false;
      }
    }
    return res; 
  }  
  
  ////////////////////////////////////////////////////////////  
  
  // Bulk set methods.
  
  public void setVertices(ArrayList<PVector> vertexList) {
    loadVertices();
    for (int i = 0; i < vertexCount; i++) {
      PVector v = (PVector)vertexList.get(i);
      set(i, v.x, v.y, v.z);
    }
    updateVertices();
  }
  
  
  public void setColors(ArrayList<float[]> colorList) {
    loadColors();
    for (int i = 0; i < vertexCount; i++) {
      float[] c = (float[])colorList.get(i);
      set(i, c);
    }
    updateColors();    
  }  
  

  public void setNormals(ArrayList<PVector> normalList) {
    loadNormals();
    for (int i = 0; i < vertexCount; i++) {
      PVector n = (PVector)normalList.get(i);
      set(i, n.x, n.y, n.z);
    }
    updateNormals();    
  }  

  
  public void setTexcoords(ArrayList<PVector> tcoordList) {
    setTexcoords(0, tcoordList);
  }  

  
  public void setTexcoords(int unit, ArrayList<PVector> tcoordList) {
    loadTexcoords(unit);
    for (int i = 0; i < vertexCount; i++) {
      PVector tc = (PVector)tcoordList.get(i);
      set(i, tc.x, tc.y);
    }
    updateTexcoords();     
  }  
    
  
  public void setChildren(ArrayList<PShape3D> who) {
    childCount = 0;
    for (int i = 0; i < who.size(); i++) {
      PShape child = (PShape)who.get(i);
      addChild(child);   
    }
  }  
  
  
  ////////////////////////////////////////////////////////////  
  
  // Parameters  
  
  public Parameters getParameters() {
    Parameters res = new Parameters();
    
    res.drawMode = getDrawModeImpl();
    
    if (glUsage == GL11.GL_STATIC_DRAW) res.updateMode = STATIC;
    else if (glUsage == GL11.GL_DYNAMIC_DRAW) res.updateMode = DYNAMIC;
    
    return res;
  }
  
  
  protected void setParameters(Parameters params) {    
    setDrawModeImpl(params.drawMode);

    if (params.updateMode == STATIC) glUsage = GL11.GL_STATIC_DRAW;
    else if (params.updateMode == DYNAMIC) glUsage = GL11.GL_DYNAMIC_DRAW;
    else {
      throw new RuntimeException("PShape3D: Unknown update mode");
    }
  }
  
  
  ////////////////////////////////////////////////////////////  
  
  // Data allocation, deletion.

  protected void initShape(int numVert) {
    initShape(numVert, new Parameters());
  }
  
  
  protected void initShape(int numVert, Parameters params) {
    // Checking we have what we need:
    gl = a3d.gl11;
    if (gl == null) {
      throw new RuntimeException("PShape3D: OpenGL ES 1.1 required");
    }
    if (!PGraphicsAndroid3D.vboSupported) {
       throw new RuntimeException("PShape3D: Vertex Buffer Objects are not available");
    }
    
    setParameters(params);
    createShape(numVert);    
    initChildren();      
    updateElement = -1;
    
    width = height = depth = 0;
    xmin = ymin = zmin = 10000;
    xmax = ymax = zmax = -10000;
    
  }
  
  
  protected void createShape(int numVert) {
    vertexCount = numVert;
    numTexBuffers = 1;

    initVertexData();
    createVertexBuffer();   
    initColorData();
    createColorBuffer();
    initNormalData();
    createNormalBuffer();
    initTexCoordData();
    createTexCoordBuffer();
    
    initChildren();    
  }
  
  
  protected void initVertexData() {    
    ByteBuffer vbb = ByteBuffer.allocateDirect(vertexCount * 3 * PGraphicsAndroid3D.SIZEOF_FLOAT);
    vbb.order(ByteOrder.nativeOrder());
    vertexBuffer = vbb.asFloatBuffer();    
    
    vertices = new float[vertexCount * 3];
    vertexBuffer.put(vertices);
    vertexBuffer.flip();    
  }
  
  
  protected void createVertexBuffer() {    
    deleteVertexBuffer();  // Just in the case this object is being re-initialized.
    
    glVertexBufferID = a3d.createGLResource(PGraphicsAndroid3D.GL_VERTEX_BUFFER);    
    gl.glBindBuffer(GL11.GL_ARRAY_BUFFER, glVertexBufferID);    
    final int bufferSize = vertexBuffer.capacity() * PGraphicsAndroid3D.SIZEOF_FLOAT;
    gl.glBufferData(GL11.GL_ARRAY_BUFFER, bufferSize, vertexBuffer, glUsage);
    gl.glBindBuffer(GL11.GL_ARRAY_BUFFER, 0);
  }
  

  protected void initColorData() {
    ByteBuffer vbb = ByteBuffer.allocateDirect(vertexCount * 4 * PGraphicsAndroid3D.SIZEOF_FLOAT);
    vbb.order(ByteOrder.nativeOrder());                
    colorBuffer = vbb.asFloatBuffer();          

    colors = new float[vertexCount * 4];
    // Set the initial color of all vertices to white, so they are initially visible
    // even if the user doesn't set any vertex color.
    Arrays.fill(colors, 1.0f);
    
    colorBuffer.put(colors);
    colorBuffer.flip();    
  }  
  
  
  protected void createColorBuffer() {
    deleteColorBuffer();
    
    glColorBufferID = a3d.createGLResource(PGraphicsAndroid3D.GL_VERTEX_BUFFER);
    gl.glBindBuffer(GL11.GL_ARRAY_BUFFER, glColorBufferID);
    final int bufferSize = colorBuffer.capacity() * PGraphicsAndroid3D.SIZEOF_FLOAT;    
    gl.glBufferData(GL11.GL_ARRAY_BUFFER, bufferSize, colorBuffer, glUsage);
    gl.glBindBuffer(GL11.GL_ARRAY_BUFFER, 0);
  }
  

  protected void initNormalData() {
    ByteBuffer vbb = ByteBuffer.allocateDirect(vertexCount * 3 * PGraphicsAndroid3D.SIZEOF_FLOAT);
    vbb.order(ByteOrder.nativeOrder());
    normalBuffer = vbb.asFloatBuffer();    
    
    normals = new float[vertexCount * 3];
    normalBuffer.put(normals);
    normalBuffer.flip();
  }  

  
  protected void createNormalBuffer() {
    deleteNormalBuffer();
    
    glNormalBufferID = a3d.createGLResource(PGraphicsAndroid3D.GL_VERTEX_BUFFER);
    gl.glBindBuffer(GL11.GL_ARRAY_BUFFER, glNormalBufferID);
    final int bufferSize = normalBuffer.capacity() * PGraphicsAndroid3D.SIZEOF_FLOAT;    
    gl.glBufferData(GL11.GL_ARRAY_BUFFER, bufferSize, normalBuffer, glUsage);
    gl.glBindBuffer(GL11.GL_ARRAY_BUFFER, 0);
  }
  

  protected void initTexCoordData() {
    ByteBuffer vbb = ByteBuffer.allocateDirect(vertexCount * 2 * PGraphicsAndroid3D.SIZEOF_FLOAT);
    vbb.order(ByteOrder.nativeOrder());
    texCoordBuffer = vbb.asFloatBuffer();    
    
    texcoords = new float[vertexCount * 2];
    texCoordBuffer.put(texcoords);
    texCoordBuffer.flip();   
  }  
  
  
  protected void createTexCoordBuffer() {
    deleteTexCoordBuffer();
    
    glTexCoordBufferID[0] = a3d.createGLResource(PGraphicsAndroid3D.GL_VERTEX_BUFFER);
    gl.glBindBuffer(GL11.GL_ARRAY_BUFFER, glTexCoordBufferID[0]);
    final int bufferSize = texCoordBuffer.capacity() * PGraphicsAndroid3D.SIZEOF_FLOAT;
    gl.glBufferData(GL11.GL_ARRAY_BUFFER, bufferSize, texCoordBuffer, glUsage);
    gl.glBindBuffer(GL11.GL_ARRAY_BUFFER, 0);    
  }
  
  
  protected void addTexBuffers(int more) {
    for (int i = 0; i < more; i++) {
      int t = numTexBuffers + i;
      deleteTexCoordBuffer(t);
      
      glTexCoordBufferID[t] = a3d.createGLResource(PGraphicsAndroid3D.GL_VERTEX_BUFFER);      
      gl.glBindBuffer(GL11.GL_ARRAY_BUFFER, glTexCoordBufferID[t]);
      final int bufferSize = texCoordBuffer.capacity() * PGraphicsAndroid3D.SIZEOF_FLOAT;
      gl.glBufferData(GL11.GL_ARRAY_BUFFER, bufferSize, texCoordBuffer, glUsage);
      gl.glBindBuffer(GL11.GL_ARRAY_BUFFER, 0);     
    }
    
    numTexBuffers += more;
  }
  
  
  protected void deleteVertexBuffer() {
    if (glVertexBufferID != 0) {    
      a3d.deleteGLResource(glVertexBufferID, PGraphicsAndroid3D.GL_VERTEX_BUFFER);   
      glVertexBufferID = 0;
    }
  }  
  

  protected void deleteColorBuffer() {
    if (glColorBufferID != 0) {
      a3d.deleteGLResource(glColorBufferID, PGraphicsAndroid3D.GL_VERTEX_BUFFER);
      glColorBufferID = 0;
    }
  }
  
  
  protected void deleteNormalBuffer() {
    if (glNormalBufferID != 0) {
      a3d.deleteGLResource(glNormalBufferID, PGraphicsAndroid3D.GL_VERTEX_BUFFER);
      glNormalBufferID = 0;
    }
  }  

  
  protected void deleteTexCoordBuffer() {
    for (int i = 0; i < numTexBuffers; i++) { 
      deleteTexCoordBuffer(i);    
    }
  }
  
  
  protected void deleteTexCoordBuffer(int idx) {  
    if (glTexCoordBufferID[idx] != 0) {
      a3d.deleteGLResource(glTexCoordBufferID[idx], PGraphicsAndroid3D.GL_VERTEX_BUFFER);
      glTexCoordBufferID[idx] = 0;
    }
  }
  

  ///////////////////////////////////////////////////////////  

  // Re-implementing methods inherited from PShape.  
  
  
  public float[] getVertex(int index) {
    PGraphics.showMethodWarning("getVertex");
    return null;
  }
  
  
  public float getVertexX(int index) {
    PGraphics.showMethodWarning("getVertexX");
    return 0;    
  }

  
  public float getVertexY(int index) {
    PGraphics.showMethodWarning("getVertexY");
    return 0;    
  }

  
  public float getVertexZ(int index) {
    PGraphics.showMethodWarning("getVertexZ");
    return 0;      
  }


  public int[] getVertexCodes() {
    PGraphics.showMethodWarning("getVertexCodes");
    return null;
  }


  public int getVertexCodeCount() {
    PGraphics.showMethodWarning("getVertexCodeCount");
    return 0;         
  }
  

  public int getVertexCode(int index) {
    PGraphics.showMethodWarning("getVertexCode");
    return 0;    
  }
  
  
  public void translate(float tx, float ty) {
    translate(tx, ty, 0);
  }


  public void translate(float tx, float ty, float tz) {
    loadVertices();
    
    // Translating.
    for (int i = 0; i < vertexCount; i++) {
      vertices[3 * i + 0] += tx;
      vertices[3 * i + 1] += -ty;
      vertices[3 * i + 2] += tz;  
    }
    
    updateVertices();
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
    loadVertices();
    
    // Rotating around xmin, ymin, zmin)
    float c = PApplet.cos(angle);
    float s = PApplet.sin(angle);
    float t = 1.0f - c;
    float[] m = new float[9];
    m[0] = (t*v0*v0) + c;          // 0, 0
    m[1] = (t*v0*v1) - (s*v2);     // 0, 1
    m[2] = (t*v0*v2) + (s*v1);     // 0, 2 
    m[3] = (t*v0*v1) + (s*v2);     // 1, 0
    m[4] = (t*v1*v1) + c;          // 1, 1
    m[5] =  (t*v1*v2) - (s*v0);    // 1, 2
    m[6] = (t*v0*v2) - (s*v1);     // 2, 0
    m[7] = (t*v1*v2) + (s*v0);     // 2, 1 
    m[8] = (t*v2*v2) + c;          // 2, 2
    float x, y, z;
    for (int i = 0; i < vertexCount; i++) {
      x = vertices[3 * i + 0] - xmin; 
      y = vertices[3 * i + 1] - ymin;
      z = vertices[3 * i + 2] - zmin;      
      
      vertices[3 * i + 0] = m[0] * x + m[1] * y + m[2] * z + xmin; 
      vertices[3 * i + 1] = m[3] * x + m[4] * y + m[5] * z + ymin;
      vertices[3 * i + 2] = m[6] * x + m[7] * y + m[8] * z + zmin;
    }        
    
    updateVertices();       
  }

  
  public void scale(float s) {
    scale(s, s, s);
  }


  public void scale(float sx, float sy) {
    scale(sx, sy, 1);
  }


  public void scale(float x, float y, float z) {
    loadVertices();
    
    // Scaling.
    for (int i = 0; i < vertexCount; i++) {
      vertices[3 * i + 0] *= x;
      vertices[3 * i + 1] *= y;
      vertices[3 * i + 2] *= z;  
    }    
    
    updateVertices();       
  }
  
  
  public void centerAt(float cx, float cy, float cz) {
    loadVertices();
    
    float dx = cx - 0.5f * (xmin + xmax);
    float dy = cy - 0.5f * (ymin + ymax);
    float dz = cz - 0.5f * (zmin + zmax);
    
    // Centering
    for (int i = 0; i < vertexCount; i++) {
      vertices[3 * i + 0] += dx;
      vertices[3 * i + 1] += dy;
      vertices[3 * i + 2] += dz;  
    }    
    
    // Updating bounding box
    xmin += dx;
    xmax += dx;
    ymin += dy;
    ymax += dy;
    zmin += dz;
    zmax += dz;
    
    updateVertices();         
  }
  
  
  public boolean is3D() {
    return true;
  }
  
  
  ///////////////////////////////////////////////////////////  
  
  //
  
  // Drawing methods
	 

  protected void drawGroup(PGraphics g) {
    if (readFromOBJ) {
      recordOBJ(objVertices, objNormal, objTexCoords, objFaces, objMaterials);
      centerAt(0, 0, 0);
      
      readFromOBJ = false;
      objVertices = null; 
      objNormal = null; 
      objTexCoords = null;    
      objFaces = null;
      objMaterials = null;
    }
    
    if (children == null) {
      addDefaultChild();
    }
    super.drawGroup(g);
  }


  public void draw(String target) {
    PShape child = getChild(target);
    if (child != null) {
      child.draw(a3d);
    }
  }


  public void draw(int idx) {
    if (0 <= idx && idx < childCount) {
      children[idx].draw(a3d);	   
    }
  }


  protected void drawGeometry(PGraphics g) {
    PShape3D p3d = (PShape3D)parent;
    int numTextures;
    float pointSize;

    // Setting line width and point size from stroke value.
    // TODO: Here the stroke weight from the g renderer is used. Normally, no issue here, but
    // in the case the shape is being rendered from an offscreen A3D surface, then this might
    // lead to the possibility of a stroke weight different from that of the main renderer.
    // For strokeWeight it seems to make sense that the value of the offscreen renderer and not
    // of the main renderer is used. But what about other properties such as textureMode or
    // colorMode. Right now they are read from a3d, which refers to the main renderer.
    // So what should be the normal behavior.
    pointSize = PApplet.min(g.strokeWeight, PGraphicsAndroid3D.maxPointSize);
    gl.glPointSize(pointSize);

    gl.glEnableClientState(GL11.GL_NORMAL_ARRAY);
    gl.glBindBuffer(GL11.GL_ARRAY_BUFFER, p3d.glNormalBufferID);
    gl.glNormalPointer(GL11.GL_FLOAT, 0, 0);    

    if (p3d.vertexColor) {
      gl.glEnableClientState(GL11.GL_COLOR_ARRAY);
      gl.glBindBuffer(GL11.GL_ARRAY_BUFFER, p3d.glColorBufferID);
      gl.glColorPointer(4, GL11.GL_FLOAT, 0, 0);
    }        

    gl.glEnableClientState(GL11.GL_VERTEX_ARRAY);            
    gl.glBindBuffer(GL11.GL_ARRAY_BUFFER, p3d.glVertexBufferID);
    gl.glVertexPointer(3, GL11.GL_FLOAT, 0, 0);

    numTextures = 0;
    for (int t = 0; t < textures.length; t++) {
      if (textures[t] != null) {
        PTexture tex = textures[t].getTexture();
        if (tex == null) {
          tex = textures[t].createTexture();
          if (tex == null) {
            break;
          }
        }
        
        gl.glEnable(tex.getGLTarget());
        gl.glActiveTexture(GL10.GL_TEXTURE0 + t);
        gl.glBindTexture(tex.getGLTarget(), tex.getGLID());
        renderTextures[numTextures] = tex;
        numTextures++;
      } else {
        break;
      }
    }

    if (0 < numTextures)  {        
      if (pointSprites) {
        // Texturing with point sprites.

        // This is how will our point sprite's size will be modified by 
        // distance from the viewer             
        gl.glPointParameterfv(GL11.GL_POINT_DISTANCE_ATTENUATION, ptDistAtt, 0);

        // The alpha of a point is calculated to allow the fading of points 
        // instead of shrinking them past a defined threshold size. The threshold 
        // is defined by GL_POINT_FADE_THRESHOLD_SIZE and is not clamped to the 
        // minimum and maximum point sizes.
        gl.glPointParameterf(GL11.GL_POINT_FADE_THRESHOLD_SIZE, 0.6f * pointSize);
        gl.glPointParameterf(GL11.GL_POINT_SIZE_MIN, 1.0f);
        gl.glPointParameterf(GL11.GL_POINT_SIZE_MAX, PGraphicsAndroid3D.maxPointSize);

        // Specify point sprite texture coordinate replacement mode for each 
        // texture unit
        gl.glTexEnvf(GL11.GL_POINT_SPRITE_OES, GL11.GL_COORD_REPLACE_OES, GL11.GL_TRUE);

        gl.glEnable(GL11.GL_POINT_SPRITE_OES);           
      } else {
        // Regular texturing.         
        gl.glEnableClientState(GL11.GL_TEXTURE_COORD_ARRAY);
        for (int t = 0; t < numTextures; t++) {
          gl.glClientActiveTexture(GL11.GL_TEXTURE0 + t);
          gl.glBindBuffer(GL11.GL_ARRAY_BUFFER, p3d.glTexCoordBufferID[t]);
          gl.glTexCoordPointer(2, GL11.GL_FLOAT, 0, 0);
        }          
        if (1 < numTextures) {
          a3d.setMultitextureBlend(renderTextures, numTextures);
        }
      }
    }

    if (!p3d.vertexColor) {
      if (0 < numTextures) {
        if (g.tint) {
          gl.glColor4f(g.tintR, g.tintG, g.tintB, g.tintA);  
        } else {
          gl.glColor4f(1, 1, 1, 1);  
        }
      } else {
        gl.glColor4f(g.fillR, g.fillG, g.fillB, g.fillA);          
      }              
    }

    // Setting the stroke weight (line width's in OpenGL terminology) using 
    // either the group's weight or the renderer's weight. 
    if (0 < strokeWeight) {
      gl.glLineWidth(strokeWeight);
    } else {
      gl.glLineWidth(g.strokeWeight);
    }

    //if (0 < glMode && !pointSprites) {
      // Using the group's vertex mode.
    //  gl.glDrawArrays(glMode, firstVertex, lastVertex - firstVertex + 1);
    //} else {
      // Using the overall's vertex mode assigned to the entire model.
      gl.glDrawArrays(glMode, firstVertex, lastVertex - firstVertex + 1);
    //}

    if (0 < numTextures) {
      if (1 < numTextures) {
        a3d.clearMultitextureBlend(numTextures);
      }        
      if (pointSprites)   {
        gl.glDisable(GL11.GL_POINT_SPRITE_OES);
      } else {
        gl.glDisableClientState(GL11.GL_TEXTURE_COORD_ARRAY);
      }
      for (int t = 0; t < numTextures; t++) {
        PTexture tex = renderTextures[t];
        gl.glDisable(tex.getGLTarget()); 
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
    public int drawMode;    
    public int updateMode;  
    
    public Parameters() {
      drawMode= POINTS;
      updateMode = STATIC;    
    }

    public Parameters(int drawMode) {
      this.drawMode= drawMode;      
      updateMode = STATIC;    
    }

    public Parameters(int drawMode, int updateMode) {
      this.drawMode= drawMode;
      this.updateMode = updateMode;      
    }
    
    public Parameters(Parameters src) {
      drawMode= src.drawMode;      
      updateMode = src.updateMode;
    }
  }  
  
	
  ///////////////////////////////////////////////////////////////////////////   
  
  // OBJ loading
	
	
  protected BufferedReader getBufferedReader(String filename) {
    BufferedReader retval = papplet.createReader(filename);
    if (retval != null) {
      return retval;
    } else {
      PApplet.println("Could not find this file " + filename);
      return null;
    }
  }
  
  
  protected void parseOBJ(BufferedReader reader, ArrayList<PVector> vertices, ArrayList<PVector> normals, ArrayList<PVector> textures, ArrayList<OBJFace> faces, ArrayList<OBJMaterial> materials) {
    Hashtable<String, Integer> mtlTable  = new Hashtable<String, Integer>();
    int mtlIdxCur = -1;
    boolean readv, readvn, readvt;
    try {
      // Parse the line.
      
      readv = readvn = readvt = false;
      String line;
      while ((line = reader.readLine()) != null) {
        
        // The below patch/hack comes from Carlos Tomas Marti and is a
        // fix for single backslashes in Rhino obj files
        
        // BEGINNING OF RHINO OBJ FILES HACK
        // Statements can be broken in multiple lines using '\' at the
        // end of a line.
        // In regular expressions, the backslash is also an escape
        // character.
        // The regular expression \\ matches a single backslash. This
        // regular expression as a Java string, becomes "\\\\".
        // That's right: 4 backslashes to match a single one.
        while (line.contains("\\")) {
          line = line.split("\\\\")[0];
          final String s = reader.readLine();
          if (s != null)
            line += s;
        }
        // END OF RHINO OBJ FILES HACK
        
        String[] elements = line.split("\\s+");        
        // if not a blank line, process the line.
        if (elements.length > 0) {
          if (elements[0].equals("v")) {
            // vertex
            PVector tempv = new PVector(Float.valueOf(elements[1]).floatValue(), Float.valueOf(elements[2]).floatValue(), Float.valueOf(elements[3]).floatValue());
            vertices.add(tempv);
            readv = true;
          } else if (elements[0].equals("vn")) {
            // normal
            PVector tempn = new PVector(Float.valueOf(elements[1]).floatValue(), Float.valueOf(elements[2]).floatValue(), Float.valueOf(elements[3]).floatValue());
            normals.add(tempn);
            readvn = true;
          } else if (elements[0].equals("vt")) {
            // uv
            PVector tempv = new PVector(Float.valueOf(elements[1]).floatValue(), Float.valueOf(elements[2]).floatValue());
            textures.add(tempv);
            readvt = true;
          } else if (elements[0].equals("o")) {
            // Object name is ignored, for now.
          } else if (elements[0].equals("mtllib")) {
            if (elements[1] != null) {
              parseMTL(getBufferedReader(elements[1]), materials, mtlTable); 
            }
          } else if (elements[0].equals("g")) {
            // Grouping is ignored, for now. Groups are automatically generated during recording by the triangulator.
          } else if (elements[0].equals("usemtl")) {
            // Getting index of current active material (will be applied on all subsequent faces)..
            if (elements[1] != null) {
              String mtlname = elements[1];
              if (mtlTable.containsKey(mtlname)) {
                Integer tempInt = mtlTable.get(mtlname);
                mtlIdxCur = tempInt.intValue();
              } else {
                mtlIdxCur = -1;                
              }
            }
          } else if (elements[0].equals("f")) {
            // Face setting
           OBJFace face = new OBJFace();
           face.matIdx = mtlIdxCur; 
            
            for (int i = 1; i < elements.length; i++) {
              String seg = elements[i];

              if (seg.indexOf("/") > 0) {
                String[] forder = seg.split("/");

                if (forder.length > 2) {
                  // Getting vertex and texture and normal indexes.
                  if (forder[0].length() > 0 && readv) {
                    face.vertIdx.add(Integer.valueOf(forder[0]));
                  }

                  if (forder[1].length() > 0 && readvt) {
                    face.texIdx.add(Integer.valueOf(forder[1]));
                  }

                  if (forder[2].length() > 0 && readvn) {
                    face.normIdx.add(Integer.valueOf(forder[2]));
                  }
                } else if (forder.length > 1) {
                  // Getting vertex and texture/normal indexes.
                  if (forder[0].length() > 0 && readv) {
                    face.vertIdx.add(Integer.valueOf(forder[0]));
                  }
 
                  if (forder[1].length() > 0) {
                    if (readvt) {
                      face.texIdx.add(Integer.valueOf(forder[1]));  
                    } else  if (readvn) {
                      face.normIdx.add(Integer.valueOf(forder[1]));
                    }
                    
                  }
                  
                } else if (forder.length > 0) {
                  // Getting vertex index only.
                  if (forder[0].length() > 0 && readv) {
                    face.vertIdx.add(Integer.valueOf(forder[0]));
                  }
                }
              } else {
                // Getting vertex index only.
                if (seg.length() > 0 && readv) {
                  face.vertIdx.add(Integer.valueOf(seg));
                }
              }
            }
           
            faces.add(face);
            
          }
        }
      }

      if (materials.size() == 0) {
        // No materials definition so far. Adding one default material.
        OBJMaterial defMtl = new OBJMaterial(); 
        materials.add(defMtl);
      }      
      
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  
  protected void parseMTL(BufferedReader reader, ArrayList<OBJMaterial> materials, Hashtable<String, Integer> materialsHash) {
    try {
      String line;
      OBJMaterial currentMtl = null;
      while ((line = reader.readLine()) != null) {
        // Parse the line
        line = line.trim();

        String elements[] = line.split("\\s+");

        if (elements.length > 0) {
          // Extract the material data.

          if (elements[0].equals("newmtl")) {
            // Starting new material.
            String mtlname = elements[1];
            currentMtl = new OBJMaterial(mtlname);
            materialsHash.put(mtlname, new Integer(materials.size()));
            materials.add(currentMtl);
          } else if (elements[0].equals("map_Kd") && elements.length > 1) {
            // Loading texture map.
            String texname = elements[1];
            currentMtl.kdMap = papplet.loadImage(texname);
          } else if (elements[0].equals("Ka") && elements.length > 3) {
            // The ambient color of the material
            currentMtl.ka.x = Float.valueOf(elements[1]).floatValue();
            currentMtl.ka.y = Float.valueOf(elements[2]).floatValue();
            currentMtl.ka.z = Float.valueOf(elements[3]).floatValue();
          } else if (elements[0].equals("Kd") && elements.length > 3) {
            // The diffuse color of the material
            currentMtl.kd.x = Float.valueOf(elements[1]).floatValue();
            currentMtl.kd.y = Float.valueOf(elements[2]).floatValue();
            currentMtl.kd.z = Float.valueOf(elements[3]).floatValue();
          } else if (elements[0].equals("Ks") && elements.length > 3) {
            // The specular color weighted by the specular coefficient
            currentMtl.ks.x = Float.valueOf(elements[1]).floatValue();
            currentMtl.ks.y = Float.valueOf(elements[2]).floatValue();
            currentMtl.ks.z = Float.valueOf(elements[3]).floatValue();
          } else if ((elements[0].equals("d") || elements[0].equals("Tr")) && elements.length > 1) {
            // Reading the alpha transparency.
            currentMtl.d = Float.valueOf(elements[1]).floatValue();
          } else if (elements[0].equals("Ns") && elements.length > 1) {
            // The specular component of the Phong shading model
            currentMtl.ns = Float.valueOf(elements[1]).floatValue();
          } 
          
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
    }    
  }
  
  
  protected void recordOBJ(ArrayList<PVector> vertices, ArrayList<PVector> normals, ArrayList<PVector> textures, ArrayList<OBJFace> faces, ArrayList<OBJMaterial> materials) {
    int mtlIdxCur = -1;
    OBJMaterial mtl = null;
    
    // Using normal mode for texture coordinates (i.e.: normalized between 0 and 1).
    int tMode0 = a3d.textureMode;
    a3d.textureMode = NORMAL;
    
    boolean auto0 = a3d.autoNormal;
    a3d.autoNormal = true;
    
    // Using RGB mode for coloring.
    int cMode0 = a3d.colorMode; 
    a3d.colorMode = RGB;
    
    // Saving current colors.
    float specularR0, specularG0, specularB0; 
    specularR0 = a3d.specularR;
    specularG0 = a3d.specularG;
    specularB0 = a3d.specularB;
    
    float ambientR0, ambientG0, ambientB0; 
    ambientR0 = a3d.ambientR;
    ambientG0 = a3d.ambientG;
    ambientB0 = a3d.ambientB;    
    
    boolean fill0;
    float fillR0, fillG0, fillB0, fillA0;
    int fillRi0, fillGi0, fillBi0, fillAi0;
    int fillColor0;
    boolean fillAlpha0;
    fill0 = a3d.fill;
    fillR0 = a3d.fillR;
    fillG0 = a3d.fillG;
    fillB0 = a3d.fillB;
    fillA0 = a3d.fillA;
    fillRi0 = a3d.fillRi;
    fillGi0 = a3d.fillGi;
    fillBi0 = a3d.fillBi;
    fillAi0 = a3d.fillAi;
    fillColor0 = a3d.fillColor;
    fillAlpha0 = a3d.fillAlpha;
    
    boolean tint0;
    float tintR0, tintG0, tintB0, tintA0;
    int tintRi0, tintGi0, tintBi0, tintAi0;
    int tintColor0;
    boolean tintAlpha0;
    tint0 = a3d.tint;
    tintR0 = a3d.tintR;
    tintG0 = a3d.tintG;
    tintB0 = a3d.tintB;
    tintA0 = a3d.tintA;
    tintRi0 = a3d.tintRi;
    tintGi0 = a3d.tintGi;
    tintBi0 = a3d.tintBi;
    tintAi0 = a3d.tintAi;
    tintColor0 = a3d.tintColor;
    tintAlpha0 = a3d.tintAlpha;    
    
    float shininess0 = a3d.shininess;
    
    a3d.beginShapeRecorderImpl();    
    for (int i = 0; i < faces.size(); i++) {
      OBJFace face = (OBJFace) faces.get(i);
      
      // Getting current material.
      if (mtlIdxCur != face.matIdx) {
        mtlIdxCur = PApplet.max(0, face.matIdx); // To make sure that at least we get the default material.
        
        mtl = (OBJMaterial) materials.get(mtlIdxCur);

         // Setting colors.
        a3d.specular(mtl.ks.x * 255.0f, mtl.ks.y * 255.0f, mtl.ks.z * 255.0f);
        a3d.ambient(mtl.ka.x * 255.0f, mtl.ka.y * 255.0f, mtl.ka.z * 255.0f);
        if (a3d.fill) {
          a3d.fill(mtl.kd.x * 255.0f, mtl.kd.y * 255.0f, mtl.kd.z * 255.0f, mtl.d * 255.0f);  
        }        
        a3d.shininess(mtl.ns);
        
        if (a3d.tint && mtl.kdMap != null) {
          // If current material is textured, then tinting the texture using the diffuse color.
          a3d.tint(mtl.kd.x * 255.0f, mtl.kd.y * 255.0f, mtl.kd.z * 255.0f, mtl.d * 255.0f);
        }
      }

      // Recording current face.
      
      if (face.vertIdx.size() == 3) {
        a3d.beginShape(TRIANGLES); // Face is a triangle, so using appropriate shape kind.
      } else if (face.vertIdx.size() == 4) {
        a3d.beginShape(QUADS);        // Face is a quad, so using appropriate shape kind.
      } else {
        a3d.beginShape();  
      }      
      
      for (int j = 0; j < face.vertIdx.size(); j++){
        int vertIdx, normIdx;
        PVector vert, norms;

        vert = norms = null;
        
        vertIdx = face.vertIdx.get(j).intValue() - 1;
        vert = (PVector) vertices.get(vertIdx);
        
        if (j < face.normIdx.size()) {
          normIdx = face.normIdx.get(j).intValue() - 1;
          if (-1 < normIdx) {
            norms = (PVector) normals.get(normIdx);  
          }
        }
        
        if (mtl != null && mtl.kdMap != null) {
          // This face is textured.
          int texIdx;
          PVector tex = null; 
          
          if (j < face.texIdx.size()) {
            texIdx = face.texIdx.get(j).intValue() - 1;
            if (-1 < texIdx) {
              tex = (PVector) textures.get(texIdx);  
            }
          }
          
          PTexture texMtl = mtl.kdMap.getTexture();
          if (texMtl != null) {     
            // Texture orientation in Processing is inverted with respect to OpenGL.
            texMtl.setFlippedY(true);          
          }
          a3d.texture(mtl.kdMap);
          if (norms != null) {
            a3d.normal(norms.x, norms.y, norms.z);
          }
          if (tex != null) {
            a3d.vertex(vert.x, vert.y, vert.z, tex.x, tex.y);  
          } else {
            a3d.vertex(vert.x, vert.y, vert.z);
          }
        } else {
          // This face is not textured.
          if (norms != null) {
            a3d.normal(norms.x, norms.y, norms.z);
          }
          a3d.vertex(vert.x, vert.y, vert.z);          
        }
      } 
      a3d.endShape(CLOSE);
    }
    
    // Allocate space for the geometry that the triangulator has generated from the OBJ model.
    createShape(a3d.recordedVertices.size());    
    initChildren();      
    updateElement = -1;
    
    width = height = depth = 0;
    xmin = ymin = zmin = 10000;
    xmax = ymax = zmax = -10000;
    
    a3d.endShapeRecorderImpl(this);
    
    // Restore texture, color, and normal modes.
    a3d.textureMode = tMode0;
    a3d.colorMode = cMode0;
    a3d.autoNormal = auto0;
    
    // Restore colors
    a3d.calcR = specularR0;
    a3d.calcG = specularG0;
    a3d.calcB = specularB0;
    a3d.specularFromCalc();
    
    a3d.calcR = ambientR0;
    a3d.calcG = ambientG0;
    a3d.calcB = ambientB0;    
    a3d.ambientFromCalc();
    
    if (!fill0) {
      a3d.noFill();
    } else {
      a3d.calcR = fillR0;
      a3d.calcG = fillG0;
      a3d.calcB = fillB0;
      a3d.calcA = fillA0;
      a3d.calcRi = fillRi0;
      a3d.calcGi = fillGi0;
      a3d.calcBi = fillBi0;
      a3d.calcAi = fillAi0;
      a3d.calcColor = fillColor0;
      a3d.calcAlpha = fillAlpha0;
      a3d.fillFromCalc();
    }

    if (!tint0) {
      a3d.noTint();
    } else {
      a3d.calcR = tintR0;
      a3d.calcG = tintG0;
      a3d.calcB = tintB0;
      a3d.calcA = tintA0;
      a3d.calcRi = tintRi0;
      a3d.calcGi = tintGi0;
      a3d.calcBi = tintBi0;
      a3d.calcAi = tintAi0;
      a3d.calcColor = tintColor0;
      a3d.calcAlpha = tintAlpha0;
      a3d.tintFromCalc();
    }    
    
    a3d.shininess(shininess0);
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
      this("default");
    }
    
    OBJMaterial(String name) {
      this.name = name;
      ka = new PVector(0.5f, 0.5f, 0.5f);
      kd = new PVector(0.5f, 0.5f, 0.5f);
      ks = new PVector(0.5f, 0.5f, 0.5f);
      d = 1.0f;
      ns = 0.0f;
      kdMap = null;
    }    
  }
}
