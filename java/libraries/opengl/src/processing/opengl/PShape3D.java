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

package processing.opengl;

import javax.media.opengl.GL;
import javax.media.opengl.GL2;
import javax.media.opengl.GL2ES1;
import processing.core.PApplet;
import processing.core.PGraphics;
import processing.core.PImage;
import processing.core.PShape;
import processing.core.PVector;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
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
public class PShape3D extends PShape {
  protected PApplet papplet;      
  protected PGraphicsOpenGL ogl;
    
  // Element types handled by PShape3D (vertices, normals, color, texture coordinates).
  protected static final int VERTICES = 0;
  protected static final int NORMALS = 1;
  protected static final int COLORS = 2;
  protected static final int TEXCOORDS = 3; 
    
  // ROOT shape properties:
  
  // Number of texture buffers currently in use:
  protected int numTexBuffers;  
  
  // STATIC, DYNAMIC, or STREAM
  // TODO: vertex, color, normal and texcoord data can potentially have 
  // different usage modes.
  public int glUsage;
  
  // The OpenGL IDs for the different VBOs
  public int glVertexBufferID;
  public int glColorBufferID;
  public int glNormalBufferID;
  public int[] glTexCoordBufferID;
  
  // The float buffers (directly allocated) used to put data into the VBOs
  protected FloatBuffer vertexBuffer;
  protected FloatBuffer colorBuffer;
  protected FloatBuffer normalBuffer;
  protected FloatBuffer texCoordBuffer; 
  
  // Public arrays for setting/getting vertices, colors, normals, and 
  // texture coordinates when using loadVertices/updateVertices,
  // loadNormals/updateNormals, etc. This is modeled following the
  // loadPixels/updatePixels API for setting/getting pixel data in 
  // PImage.
  public float[] vertices;
  public float[] colors;
  public float[] normals;
  public float[] texcoords;
  
  // Indexed mode, testing:
  protected int glIndexBufferID = 0;
  protected IntBuffer indexBuffer = null;
  protected int indexCount = 0;
  protected int[] indices;
  protected boolean useIndices;
  
  // To put the texture coordinate values adjusted according to texture 
  // flipping mode, max UV range, etc.
  protected float[] convTexcoords;
  // The array of arrays holding the texture coordinates for all texture units.
  protected float[][] allTexcoords;
  
  // Child PShape3D associated to each vertex in the model.
  protected PShape3D[] vertexChild;

  // Some utility arrays.    
  protected boolean[] texCoordSet;      
    
  protected boolean autoBounds = true;
  
  // For OBJ loading. Only used by the root shape.
  boolean readFromOBJ = false;
  ArrayList<PVector> objVertices; 
  ArrayList<PVector> objNormal; 
  ArrayList<PVector> objTexCoords;    
  ArrayList<OBJFace> objFaces;
  ArrayList<OBJMaterial> objMaterials;  
  
  // GEOMETRY shape properties:
  
  // Draw mode, point sprites and textures. 
  // Stroke weight is inherited from PShape.
  protected int glMode;
  protected boolean pointSprites;  
  protected PImage[] textures;  
  protected float maxSpriteSize = PGraphicsOpenGL.maxPointSize;
  // Coefficients for point sprite distance attenuation function.  
  // These default values correspond to the constant sprite size.
  protected float spriteDistAtt[] = { 1.0f, 0.0f, 0.0f };
  // Used in the drawGeometry() method.
  protected PTexture[] renderTextures;
  
  // GROUP and GEOMETRY properties:
  
  // The root group shape.
  protected PShape3D root;
  
  // Element type, vertex indices and texture units being currently updated.
  protected int updateElement;
  protected int updateTexunit;
  protected int firstUpdateIdx;
  protected int lastUpdateIdx;  
  
  // first and last vertex in the shape. For the root group shape, these are 0 and
  // vertexCount - 1.
  protected int firstVertex;
  protected int lastVertex;  
    
  protected int firstIndex;
  protected int lastIndex;    
  
  // Bounding box (defined for all shapes, group and geometry):
  public float xmin, xmax;
  public float ymin, ymax;
  public float zmin, zmax;  
  
  ////////////////////////////////////////////////////////////

  // Constructors.

  public PShape3D() {
    this.papplet = null;
    ogl = null;
    
    glVertexBufferID = 0;
    glColorBufferID = 0;
    glNormalBufferID = 0;
    glTexCoordBufferID = null;
    
    updateElement = -1;
  }
  
  public PShape3D(PApplet parent) {
    this();
    this.papplet = parent;
    ogl = (PGraphicsOpenGL)parent.g;
    this.family = PShape.GROUP;
    this.name = "root";
    this.root = this;
  }
  
  public PShape3D(PApplet parent, int numVert) {
    this(parent, numVert, new Parameters()); 
  }  
  
  public PShape3D(PApplet parent, String filename, Parameters params) {
    this.papplet = parent;
    ogl = (PGraphicsOpenGL)parent.g;

    this.family = PShape.GROUP;
    this.name = "root";
    this.root = this;
    glVertexBufferID = 0;
    glColorBufferID = 0;
    glNormalBufferID = 0;
    glTexCoordBufferID = null;
    
    updateElement = -1;
    
    initShapeOBJ(filename, params);    
  }  
  
  
  public PShape3D(PApplet parent, int size, Parameters params) {
    this.papplet = parent;
    ogl = (PGraphicsOpenGL)parent.g;
    
    this.family = PShape.GROUP;
    this.name = "root";
    this.root = this;
    glVertexBufferID = 0;
    glColorBufferID = 0;
    glNormalBufferID = 0;
    glTexCoordBufferID = null;
    
    updateElement = -1; 
    
    initShape(size, params);
  }
  

  public void delete() {
    if (root != this) return; // Can be done only from the root shape.
    
    deleteVertexBuffer();
    deleteColorBuffer();
    deleteTexCoordBuffer();
    deleteNormalBuffer();    
    deleteIndexBuffer();
  }

  ////////////////////////////////////////////////////////////

  // load/update/set/get methods
 
  
  public void loadVertices() {
    loadVertices(firstVertex, lastVertex);
  }

  
  public void loadVertices(int first, int last) {
    if (last < first || first < firstVertex || lastVertex < last) {
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
        
    getGl().glBindBuffer(GL.GL_ARRAY_BUFFER, glVertexBufferID);
        
    vertexBuffer = getGl().glMapBuffer(GL.GL_ARRAY_BUFFER, GL.GL_WRITE_ONLY).asFloatBuffer();
    
    // * Possible optimization:
    // int offset = firstUpdateIdx * 3;
    // int size = (lastUpdateIdx - firstUpdateIdx + 1) * 3;    
    //vertexBuffer = gl.glMapBufferRange(GL.GL_ARRAY_BUFFER, offset, size, GL.GL_WRITE_ONLY).asFloatBuffer();
    // if using this, the vertexBuffer.put(vertices) should be start at 0. I think.
    // * Another optimization: use BufferData instead of Map to replace an ENTIRE buffer.
  }  
  
  
  public void updateVertices() {
    if (updateElement == VERTICES) {
      int offset = firstUpdateIdx * 3;
      int size = (lastUpdateIdx - firstUpdateIdx + 1) * 3;
    
      if (root.autoBounds) { 
        updateBounds(firstUpdateIdx, lastUpdateIdx);
      }
        
      vertexBuffer.position(0);
      vertexBuffer.put(vertices, offset, size);
      vertexBuffer.flip();      
      
      getGl().glUnmapBuffer(GL.GL_ARRAY_BUFFER);
      getGl().glBindBuffer(GL.GL_ARRAY_BUFFER, 0);

      updateElement = -1;
    } else {
      PGraphics.showWarning("PShape3D: need to call loadVertices() first");
    }
  }
  
  
  public void loadColors() {
    loadColors(firstVertex, lastVertex);
  }

  
  public void loadColors(int first, int last) {
    if (last < first || first < firstVertex || lastVertex < last) {
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
    
    getGl().glBindBuffer(GL.GL_ARRAY_BUFFER, glColorBufferID);
    colorBuffer = getGl().glMapBuffer(GL.GL_ARRAY_BUFFER, GL.GL_WRITE_ONLY).asFloatBuffer();    
  }
  
  
  public void updateColors() {
    if (updateElement == COLORS) {    
      int offset = firstUpdateIdx * 4;
      int size = (lastUpdateIdx - firstUpdateIdx + 1) * 4;
          
      colorBuffer.position(0);      
      colorBuffer.put(colors, offset, size);
      colorBuffer.flip();
  
      getGl().glUnmapBuffer(GL.GL_ARRAY_BUFFER);
      getGl().glBindBuffer(GL.GL_ARRAY_BUFFER, 0);
      
      updateElement = -1;      
    } else {
      PGraphics.showWarning("PShape3D: need to call loadColors() first");
    }    
  }
  

  public void loadNormals() {
    loadNormals(firstVertex, lastVertex);
  }  
  
  
  public void loadNormals(int first, int last) {
    if (last < first || first < firstVertex || lastVertex < last) {
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
    
    getGl().glBindBuffer(GL.GL_ARRAY_BUFFER, glNormalBufferID);
    normalBuffer = getGl().glMapBuffer(GL.GL_ARRAY_BUFFER, GL.GL_WRITE_ONLY).asFloatBuffer();      
  }
  
  
  public void updateNormals() {
    if (updateElement == NORMALS) {    
      int offset = firstUpdateIdx * 3;
      int size = (lastUpdateIdx - firstUpdateIdx + 1) * 3;
      
      normalBuffer.position(0);      
      normalBuffer.put(normals, offset, size);
      normalBuffer.flip();
    
      getGl().glUnmapBuffer(GL.GL_ARRAY_BUFFER);
      getGl().glBindBuffer(GL.GL_ARRAY_BUFFER, 0);
      
      updateElement = -1;      
    } else {
      PGraphics.showWarning("PShape3D: need to call loadNormals() first");
    }      
  }  
  
  
  public void loadTexcoords() {
    loadTexcoords(0);
  }
  
  
  public void loadTexcoords(int unit) {
    loadTexcoords(unit, firstVertex, lastVertex);
  }  
  
  
  protected void loadTexcoords(int unit, int first, int last) {
    if (last < first || first < firstVertex || lastVertex < last) {
      PGraphics.showWarning("PShape3D: wrong vertex index");
      updateElement = -1;
      return;  
    }
    
    if (updateElement != -1) {
      PGraphics.showWarning("PShape3D: can load only one type of data at the time");
      return;        
    }    
    
    if (PGraphicsOpenGL.maxTextureUnits <= unit) {
      PGraphics.showWarning("PShape3D: wrong texture unit");
      return;
    }
    
    updateElement = TEXCOORDS;
    firstUpdateIdx = first;
    lastUpdateIdx = last;
    updateTexunit = unit;
    
    if (numTexBuffers <= unit) {
      addTexBuffers(unit - numTexBuffers + 1);
    }
    
    getGl().glBindBuffer(GL.GL_ARRAY_BUFFER, glTexCoordBufferID[unit]);
    texCoordBuffer = getGl().glMapBuffer(GL.GL_ARRAY_BUFFER, GL.GL_WRITE_ONLY).asFloatBuffer(); 
    
    texcoords = allTexcoords[unit];
  }  
  
  
  public void updateTexcoords() {
    if (updateElement == TEXCOORDS) {    
      int offset = firstUpdateIdx * 2;
      int size = (lastUpdateIdx - firstUpdateIdx + 1) * 2;
      
      convertTexcoords();
      
      texCoordBuffer.position(0);      
      texCoordBuffer.put(convTexcoords, offset, size);
      texCoordBuffer.flip();
      
      getGl().glUnmapBuffer(GL.GL_ARRAY_BUFFER);
      getGl().glBindBuffer(GL.GL_ARRAY_BUFFER, 0);
      
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
  
  public void autoBounds(boolean value) {
    root.autoBounds = value;
  }
  
  public void updateBounds() {
    updateBounds(firstVertex, lastVertex);
  }
  
  protected void updateBounds(int first, int last) {
    if (first <= firstVertex && lastVertex <= last) {
      resetBounds();
    }
    
    if (family == GROUP) {      
      if (root == this && childCount == 0) {
        // This might happen: the vertices has been set
        // first but children still not initialized. So we
        // need to calculate the bounding box directly:
        for (int i = firstVertex; i <= lastVertex; i++) {
          updateBounds(vertices[3 * i + 0], vertices[3 * i + 1], vertices[3 * i + 2]);      
        }        
      } else {      
        for (int i = 0; i < childCount; i++) {
          PShape3D child = (PShape3D)children[i];
          child.updateBounds(first, last);
          xmin = PApplet.min(xmin, child.xmin);
          xmax = PApplet.max(xmax, child.xmax);
        
          ymin = PApplet.min(ymin, child.ymin);
          ymax = PApplet.max(ymax, child.ymax);

          zmin = PApplet.min(zmin, child.zmin);
          zmax = PApplet.max(zmax, child.zmax);
        
          width = xmax - xmin;
          height = ymax - ymin;
          depth = zmax - zmin;        
        }
      }
    } else {
      // TODO: extract minimum and maximum values using some sorting algorithm (maybe).
      int n0 = PApplet.max(first, firstVertex);
      int n1 = PApplet.min(last, lastVertex);
      
      for (int i = n0; i <= n1; i++) {
        updateBounds(vertices[3 * i + 0], vertices[3 * i + 1], vertices[3 * i + 2]);      
      }
    }   
  }

  protected void resetBounds() {
    if (family == GROUP) {            
      for (int i = 0; i < childCount; i++) {
        ((PShape3D)children[i]).resetBounds();
      }      
    }
    width = height = depth = 0;
    xmin = ymin = zmin = 10000;
    xmax = ymax = zmax = -10000;    
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
  
  
  // Convert texture coordinates given by the user to values required by
  // the GPU (non-necessarily normalized because of texture size being smaller
  // than image size, for instance). 
  protected void convertTexcoords() {
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
      if (vertexChild[i] != null && vertexChild[i].textures[updateTexunit] != null) {
        PImage img = vertexChild[i].textures[updateTexunit];
        tex = ogl.getTexture(img);
        
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
        
        convTexcoords[2 * i + 0] = u;
        convTexcoords[2 * i + 1] = v;        
      }  
    }    
  }
  
  ////////////////////////////////////////////////////////////  
  
  // Child shapes   
  
  
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
  
  
  static public PShape createChild(String name, int n0, int n1, int i0, int i1, int mode, float weight, PImage[] tex) {
    PShape3D child = (PShape3D)createChild(name, n0, n1, mode, weight, tex);    
    child.firstIndex = i0;
    child.lastIndex = i1;
    return child;
  }
  
  static public PShape createChild(String name, int n0, int n1, int mode, float weight, PImage[] tex) {
    PShape3D child = new PShape3D();
    child.family = PShape.GEOMETRY;    
    child.name = name;
    child.firstVertex = n0;
    child.lastVertex = n1;
    child.setDrawModeImpl(mode);    
    child.strokeWeight = weight;
    child.textures = new PImage[PGraphicsOpenGL.MAX_TEXTURES];
    child.renderTextures = new PTexture[PGraphicsOpenGL.MAX_TEXTURES];
    
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
    addChildImpl(who, true);
  }
  
  
  protected void addChildImpl(PShape who, boolean newShape) {
    if (family == GROUP) {
      super.addChild(who);
      
      if (newShape) {
        PShape3D who3d = (PShape3D)who;
        who3d.papplet = papplet;
        who3d.ogl = ogl;     
        who3d.root = root;        
        
        // So we can use the load/update methods in the child geometries.        
        who3d.numTexBuffers = root.numTexBuffers;
        
        who3d.glVertexBufferID = root.glVertexBufferID;
        who3d.glColorBufferID = root.glColorBufferID;
        who3d.glNormalBufferID = root.glNormalBufferID;
        who3d.glTexCoordBufferID = root.glTexCoordBufferID;
        who3d.glIndexBufferID = root.glIndexBufferID;
        
        who3d.vertexBuffer = root.vertexBuffer;
        who3d.colorBuffer = root.colorBuffer;
        who3d.normalBuffer = root.normalBuffer;
        who3d.texCoordBuffer = root.texCoordBuffer;
        
        who3d.vertices = root.vertices;
        who3d.colors = root.colors;
        who3d.normals = root.normals;
        who3d.texcoords = root.texcoords;
        
        who3d.convTexcoords = root.convTexcoords;
        who3d.allTexcoords = root.allTexcoords;        
        who3d.vertexChild = root.vertexChild;
        who3d.texCoordSet = root.texCoordSet;
        
        // In case the style was disabled from the root.
        who3d.style = root.style;

        // Updating vertex information.
        for (int n = who3d.firstVertex; n <= who3d.lastVertex; n++) {
          who3d.vertexChild[n] = who3d;
        }

        // If this new child shape has textures, then we should update the texture coordinates
        // for the vertices involved. All we need to do is to call loadTexcoords and updateTexcoords
        // so the current texture coordinates are converted into values that take into account
        // the texture flipping, max UV ranges, etc.
        for (int i = 0; i < who3d.textures.length; i++) {
          if (who3d.textures[i] != null && who3d.texCoordSet[i]) {
            who3d.loadTexcoords(i);  
            who3d.updateTexcoords();
          }
        }
      }      
    } else {
      PGraphics.showWarning("PShape3D: Child shapes can only be added to a group shape.");
    }  
  }

  /**
   * Add a shape to the name lookup table.
   */
  public void addName(String nom, PShape shape) {    
    if (nameTable == null) {
      nameTable = new HashMap<String,PShape>();
    }
    nameTable.put(nom, shape);
  }  

  protected void addDefaultChild() {
    PShape child = createChild("geometry", 0, vertexCount - 1, getDrawModeImpl(), 0, null);
    addChild(child);
  }
  

  public PShape groupChildren(int cidx0, int cidx1, String gname) {
    if (family != GROUP) return null; // Can be done only in a GROUP shape.
    return groupChildren(new int[] {cidx0, cidx1}, gname);
  }

  
  public PShape groupChildren(int cidx0, int cidx1, int cidx2, String gname) {
    if (family != GROUP) return null; // Can be done only in a GROUP shape.
    return groupChildren(new int[] {cidx0, cidx1, cidx2}, gname);
  }  
  
  
  public PShape groupChildren(int[] cidxs, String gname) {
    if (family != GROUP) return null; // Can be done only in a GROUP shape.
    PShape[] temp = new PShape[cidxs.length];
    
    int nsel = 0;    
    for (int i = 0; i < cidxs.length; i++) {
      PShape child = getChild(cidxs[i]);
      if (child != null) { 
        temp[nsel] = child;
        nsel++;
      }
    }
    
    PShape[] schildren = new PShape[nsel];
    PApplet.arrayCopy(temp, schildren, nsel);    
    
    return groupChildren(schildren, gname);
  }
  
  
  public PShape groupChildren(String cname0, String cname1, String gname) {
    if (family != GROUP) return null; // Can be done only in a GROUP shape.
    return groupChildren(new String[] {cname0, cname1}, gname);
  }
  
  
  public PShape groupChildren(String cname0, String cname1, String cname2, String gname) {   
    if (family != GROUP) return null; // Can be done only in a GROUP shape.
    return groupChildren(new String[] {cname0, cname1, cname2}, gname);
  }  
  
  
  public PShape groupChildren(String[] cnames, String gname) {
    if (family != GROUP) return null; // Can be done only in a GROUP shape.
    
    PShape[] temp = new PShape[cnames.length];
    
    int nsel = 0;    
    for (int i = 0; i < cnames.length; i++) {
      PShape child = getChild(cnames[i]);
      if (child != null) { 
        temp[nsel] = child;
        nsel++;
      }
    }
    
    PShape[] schildren = new PShape[nsel];
    PApplet.arrayCopy(temp, schildren, nsel);    
    
    return groupChildren(schildren, gname);
  }  
  
  
  public PShape groupChildren(PShape[] gchildren, String gname) {
    if (family != GROUP) return null; // Can be done only in a GROUP shape.
    
    // Creating the new group containing the children.
    PShape3D group = new PShape3D();
    group.family = PShape.GROUP;
    group.name = gname;
    group.papplet = papplet;
    group.ogl = ogl; 
    group.root = root;
    
    PShape child, p;
    int idx;
    
    // Inserting the new group at the position of the first
    // child shape in the group (in its original parent).
    child = gchildren[0];    
    p = child.getParent();
    if (p != null) {
      idx = p.getChildIndex(child);
      if (idx < 0) idx = 0; 
    } else {
      p = this;
      idx = 0;
    }
    p.addChild(group, idx);
    
    // Removing the children in the new group from their
    // respective parents.
    for (int i = 0; i < gchildren.length; i++) {
      child = gchildren[i];
      p = child.getParent();
      if (p != null) {
        idx = p.getChildIndex(child);
        if (-1 < idx) {
          p.removeChild(idx);
        }
      }
    }
    
    group.firstVertex = root.vertexCount;
    group.lastVertex = 0;
    for (int i = 0; i < gchildren.length; i++) {
      group.firstVertex = PApplet.min(group.firstVertex, ((PShape3D)gchildren[i]).firstVertex);
      group.lastVertex = PApplet.max(group.lastVertex, ((PShape3D)gchildren[i]).lastVertex);
    }
    
    // Adding the children shapes to the new group.
    for (int i = 0; i < gchildren.length; i++) {
      group.addChildImpl(gchildren[i], false);
    }    
    
    return group; 
  }
  
  
  public int getFirstVertex() {     
    return firstVertex;
  }

  
  public int getFirstVertex(int idx) {
    if (0 <= idx && idx < childCount) {
      return ((PShape3D)children[idx]).getFirstVertex();
    }      
    return -1;
  }  
  
  
  public void setFirstVertex(int n0) {
    firstVertex = n0;   
  }  
  
  
  public void setFirstVertex(int idx, int n0) {
    if (0 <= idx && idx < childCount) {
      ((PShape3D)children[idx]).setFirstVertex(n0);
    }     
  }
  
  
  public int getLastVertex() {
    return lastVertex;
  }
   
  
  public int getLastVertex(int idx) {
    if (0 <= idx && idx < childCount) {
      return ((PShape3D)children[idx]).getLastVertex();    
    }
    return -1;    
  }
  
  
  public void setLastVertex(int n1) {
    lastVertex = n1;
  }  
  
  
  public void setLastVertex(int idx, int n1) {
    if (0 <= idx && idx < childCount) {
      ((PShape3D)children[idx]).setLastVertex(n1);
    }     
  }
  
  
  public void setDrawMode(int mode) {
    if (family == GROUP) {
      init();
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
      ((PShape3D)children[idx]).setDrawMode(mode);
    }
  }
  
  
  protected void setDrawModeImpl(int mode) {
    pointSprites = false;
    if (mode == POINTS) glMode = GL.GL_POINTS;
    else if (mode == POINT_SPRITES) {
      glMode = GL.GL_POINTS;
      pointSprites = true;
    }
    else if (mode == LINES) glMode = GL.GL_LINES;
    else if (mode == LINE_STRIP) glMode = GL.GL_LINE_STRIP;
    else if (mode == LINE_LOOP) glMode = GL.GL_LINE_LOOP;
    else if (mode == TRIANGLES) glMode = GL.GL_TRIANGLES; 
    else if (mode == TRIANGLE_FAN) glMode = GL.GL_TRIANGLE_FAN;
    else if (mode == TRIANGLE_STRIP) glMode = GL.GL_TRIANGLE_STRIP;
    else {
      throw new RuntimeException("PShape3D: Unknown draw mode");
    }    
  }
  
  
  protected boolean isTexturable() {    
    return glMode == GL.GL_TRIANGLES || 
           glMode == GL.GL_TRIANGLE_FAN ||
           glMode == GL.GL_TRIANGLE_STRIP ||
           pointSprites;       
  }
  
  
  public int getDrawMode() {
    if (family == GROUP) {
      init();
      return getDrawMode(0);
    } else { 
      return getDrawModeImpl();
    }     
  }
  
  
  public int getDrawMode(int idx) {
    if (0 <= idx && idx < childCount) {
      return ((PShape3D)children[idx]).getDrawMode();
    }
    return -1;
  }
  
  
  protected int getDrawModeImpl() {
    if (glMode == GL.GL_POINTS) {
      if (pointSprites) return POINT_SPRITES;
      else return POINTS;
    } else if (glMode == GL.GL_LINES) return LINES;
    else if (glMode == GL.GL_LINE_STRIP) return LINE_STRIP;
    else if (glMode == GL.GL_LINE_LOOP) return LINE_LOOP;
    else if (glMode == GL.GL_TRIANGLES) return TRIANGLES; 
    else if (glMode == GL.GL_TRIANGLE_FAN) return TRIANGLE_FAN;
    else if (glMode == GL.GL_TRIANGLE_STRIP) return TRIANGLE_STRIP;
    else return -1;
  }  
  
  
  public void setTexture(PImage tex) {
    if (family == GROUP) {
      init();
      for (int i = 0; i < childCount; i++) {
        setTexture(i, tex);
      }
    } else {
      setTextureImpl(tex, 0);
    }      
  }
  
  
  public void setTexture(PImage tex0, PImage tex1) {
    if (family == GROUP) {
      init();
      for (int i = 0; i < childCount; i++) {
        setTexture(i, tex0, tex1);
      }
    } else {
      setTextureImpl(tex0, 0);
      setTextureImpl(tex1, 1);
    }     
  }  
  
  
  public void setTexture(PImage tex0, PImage tex1, PImage tex2) {
    if (family == GROUP) {
      init();
      for (int i = 0; i < childCount; i++) {
        setTexture(i, tex0, tex1, tex2);
      }
    } else {
      setTextureImpl(tex0, 0);
      setTextureImpl(tex1, 1);
      setTextureImpl(tex2, 2);
    }    
  }  
  
  
  public void setTexture(PImage tex0, PImage tex1, PImage tex2, PImage tex3) {
    if (family == GROUP) {
      init();
      for (int i = 0; i < childCount; i++) {
        setTexture(i, tex0, tex1, tex2, tex3);
      }
    } else {
      setTextureImpl(tex0, 0);
      setTextureImpl(tex1, 1);
      setTextureImpl(tex2, 2);
      setTextureImpl(tex3, 3);      
    }
  }  
  
  
  public void setTexture(PImage[] tex) {
    if (family == GROUP) {
      init();
      for (int i = 0; i < childCount; i++) {
        setTexture(i, tex);
      }
    } else {
      for (int i = 0; i < tex.length; i++) {
        setTextureImpl(tex[i], i);
      }
    }
  }   
  
  
  public void setTexture(int idx, PImage tex) {
    if (0 <= idx && idx < childCount) {
      ((PShape3D)children[idx]).setTexture(tex);
    }    
  }
  
  
  public void setTexture(int idx, PImage tex0, PImage tex1) {
    if (0 <= idx && idx < childCount) {
      ((PShape3D)children[idx]).setTexture(tex0, tex1);
    }        
  }  
  
  
  public void setTexture(int idx, PImage tex0, PImage tex1, PImage tex2) {
    if (0 <= idx && idx < childCount) {
      ((PShape3D)children[idx]).setTexture(tex0, tex1, tex2);      
    }        
  }  
  
  
  public void setTexture(int idx, PImage tex0, PImage tex1, PImage tex2, PImage tex3) {
    if (0 <= idx && idx < childCount) {
      ((PShape3D)children[idx]).setTexture(tex0, tex1, tex2, tex3);      
    }            
  }  
  
  
  public void setTexture(int idx, PImage[] tex) {
    if (0 <= idx && idx < childCount) {
      ((PShape3D)children[idx]).setTexture(tex);    
    }
  }
 
  
  protected void setTextureImpl(PImage tex, int unit) {
    if (unit < 0 || PGraphicsOpenGL.maxTextureUnits <= unit) {
      System.err.println("PShape3D: Wrong texture unit.");
      return;
    }
    
    if (numTexBuffers <= unit) {
      root.addTexBuffers(unit - numTexBuffers + 1);
    }
    
    if (tex == null) {
      throw new RuntimeException("PShape3D: trying to set null texture.");
    } 
        
    if  (texCoordSet[unit] && isTexturable()) {
      // Ok, setting a new texture, when texture coordinates have already been set. 
      // What is the problem? the new texture might have different max UV coords, 
      // flippedX/Y values, so the texture coordinates need to be updated accordingly...
      
      // The way to do it is just load the texcoords array (in the parent)...
      loadTexcoords(unit);
      // ... then replacing the old texture with the new and...
      textures[unit] = tex;
      // ...,finally, updating the texture coordinates, step in which the texcoords
      // array is converted, this time using the new texture.
      updateTexcoords();
    } else {
      textures[unit] = tex;  
    }    
  }
   
  
  public PImage[] getTexture() {
    if (family == GROUP) {
      init();
      return getTexture(0);
    } else {
      return textures;
    }          
  }
  
  
  public PImage[] getTexture(int idx) {
    if (0 <= idx && idx < childCount) {
      return ((PShape3D)children[idx]).getTexture();
    }
    return null;
  }
  
  
  public float getStrokeWeight() {
    if (family == GROUP) {
      init();
      return getStrokeWeight(0);
    } else { 
      return strokeWeight;
    }
  }
  
  
  public float getStrokeWeight(int idx) {
    if (0 <= idx && idx < childCount) {
      return ((PShape3D)children[idx]).getStrokeWeight();
    }
    return 0;
  }
  
  
  public void setStrokeWeight(float sw) {
    if (family == GROUP) {
      init();
      for (int i = 0; i < childCount; i++) {
        setStrokeWeight(i, sw);
      }            
    } else { 
      strokeWeight = sw;
    }
  }
  
  
  public void setStrokeWeight(int idx, float sw) {
    if (0 <= idx && idx < childCount) {
      ((PShape3D)children[idx]).setStrokeWeight(sw);
    }
  }  
  
  
  public float getMaxSpriteSize() {
    if (family == GROUP) {
      init();
      return getMaxSpriteSize(0);
    } else { 
      return maxSpriteSize;
    }
  }

  
  public float getMaxSpriteSize(int idx) {
    if (0 <= idx && idx < childCount) {
      return ((PShape3D)children[idx]).getMaxSpriteSize();
    }
    return 0;
  }

  
  public void setMaxSpriteSize(float s) {
    if (family == GROUP) {
      init();
      for (int i = 0; i < childCount; i++) {
        setMaxSpriteSize(i, s);
      }      
    } else {
      setMaxSpriteSizeImpl(s);
    }
  }
  

  public void setMaxSpriteSize(int idx, float s) {
    if (0 <= idx && idx < childCount) {
      ((PShape3D)children[idx]).setMaxSpriteSize(s);
    }
  }

  
  protected void setMaxSpriteSizeImpl(float s) {
    maxSpriteSize = PApplet.min(s, PGraphicsOpenGL.maxPointSize);    
  }

   
  public void setSpriteSize(float s) {
    if (family == GROUP) {
      init();
      for (int i = 0; i < childCount; i++) {
        setSpriteSize(i, s);
      }            
    } else {
      setSpriteSizeImpl(s);      
    }
  }
  
  
  public void setSpriteSize(float s, float d, int mode) {
    if (family == GROUP) {
      init();
      for (int i = 0; i < childCount; i++) {
        setSpriteSize(i, s, d, mode);
      }      
    } else {
      setSpriteSizeImpl(s, d, mode);      
    }
  }
  

  public void setSpriteSize(int idx, float s) {
    if (0 <= idx && idx < childCount) {
      ((PShape3D)children[idx]).setSpriteSize(s);
    }
  }  
  
  
  public void setSpriteSize(int idx, float s, float d, int mode) {
    if (0 <= idx && idx < childCount) {
      ((PShape3D)children[idx]).setSpriteSize(s, d, mode);
    }
  }  
  
  
  // Sets the coefficient of the distance attenuation function so that the 
  // size of the sprite is exactly s when its distance from the camera is d.
  protected void setSpriteSizeImpl(float s, float d, int mode) {
    float s0 = maxSpriteSize;
    if (mode == LINEAR) {
      spriteDistAtt[1] = (s0 - s) / (d * s);
      spriteDistAtt[2] = 0;
    } else if (mode == QUADRATIC) {
      spriteDistAtt[1] = 0; 
      spriteDistAtt[2] = (s0 - s) / (d * d * s);
    } else {
      PGraphics.showWarning("Invalid point sprite mode");
    }
  }
  

  // Sets constant sprite size equal to s.
  protected void setSpriteSizeImpl(float s) {
    setMaxSpriteSizeImpl(s);
    spriteDistAtt[1] = 0;
    spriteDistAtt[2] = 0;
  }
  
  
  public void setColor(int c) {
    setColor(rgba(c));
  }    
  
  
  public void setColor(float r, float g, float b, float a) {
    setColor(new float[] {r, g, b, a});
  }  
  
  
  public void setColor(float[] c) {
    if (family == GROUP) {
      init();
      for (int i = 0; i < childCount; i++) {
        setColor(i, c);
      }      
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
      ((PShape3D)children[idx]).setColor(c);
    }
  } 
  
  
  protected void setColorImpl(float[] c) {
    PShape3D p = (PShape3D)root;
    p.loadColors();
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
      init();      
      for (int i = 0; i < childCount; i++) {
        setNormal(i, n);
      }      
    } else {
      setNormalImpl(n);
    }
  }

  
  public void setNormal(int idx, float nx, float ny, float nz) {
    setNormal(idx, new float[] {nx, ny, nz});  
  } 
  

  public void setNormal(int idx, float[] n) {
    if (0 <= idx && idx < childCount) {
      ((PShape3D)children[idx]).setNormal(n);
    }    
  } 
    
  
  protected void setNormalImpl(float[] n) {
    PShape3D p = (PShape3D)root;
    p.loadNormals();
    for (int i = firstVertex; i <= lastVertex; i++) {
      p.set(i, n);
    }
    p.updateNormals();
  }  
  
  // Optimizes the array list containing children shapes so that shapes with identical
  // parameters are removed. Also, making sure that the names are unique.
  protected void optimizeChildren(ArrayList<PShape3D> childList) {    
    PShape3D child0, child1;

    // Expanding identical, contiguous shapes. Names are taken into account (two
    // shapes with different names are considered to be different, even though the
    // rest of their parameters are identical).
    child0 = (PShape3D)childList.get(0);
    for (int i = 1; i < childList.size(); i++) {
      child1 = (PShape3D)childList.get(i);
      if (child0.equalTo(child1, false)) {
        child0.lastVertex = child1.lastVertex;       // Extending child0.
        child0.lastIndex = child1.lastIndex;
        // Updating the vertex data:
        for (int n = child0.firstVertex; n <= child0.lastVertex; n++) {
          vertexChild[n] = child0;
        }    
        child1.firstVertex = child1.lastVertex = -1; // Marking for deletion.
      } else {
        child0 = child1;
      }
    }
      
    // Deleting superfluous shapes.
    for (int i = childList.size() - 1; i >= 0; i--) {
      if (((PShape3D)childList.get(i)).lastVertex == -1) {
        childList.remove(i);
      }
    }
    
    // Making sure the names are unique.
    for (int i = 1; i < childList.size(); i++) {
      child1 = (PShape3D)childList.get(i);
      for (int j = i - 1; j >= 0; j--) {
        child0 = (PShape3D)childList.get(j);
        if (child1.name.equals(child0.name)) {
          int pos = child0.name.indexOf(':');
          if (-1 < pos) {
            // The name of the preceding child contains the ':'
            // character so trying to use the following substring
            // as a number.
            String nstr = child0.name.substring(pos + 1);
            int n = 1;
            try {
              n = Integer.parseInt(nstr);              
            } catch (NumberFormatException e) {
              child0.name += ":1";   
            } 
            child1.name += ":" + (n + 1);
          } else {
            child0.name += ":1";
            child1.name += ":2";
          }
        }         
      }
    }
  }

 
  protected boolean equalTo(PShape3D child, boolean ignoreNames) {
    boolean res = family == child.family && 
            glMode == child.glMode && 
            strokeWeight == child.strokeWeight && 
            (ignoreNames || name.equals(child.name));
    if (!res) return false;
    for (int i = 0; i < textures.length; i++) {
      if (textures[i] != child.textures[i]) {
        res = false;
      }
    }
    return res; 
  }  
  
  ////////////////////////////////////////////////////////////


  // Some overloading of translate, rotate, scale
  
  public void resetMatrix() {
    checkMatrix(3);
    matrix.reset();
  }
  
  public void translate(float tx, float ty) {
    checkMatrix(3);
    matrix.translate(tx, ty, 0);
  }
  
  public void rotate(float angle) {
    checkMatrix(3);
    matrix.rotate(angle);
  }  
  
  public void scale(float s) {
    checkMatrix(3);
    matrix.scale(s);
  }

  public void scale(float x, float y) {
    checkMatrix(3);
    matrix.scale(x, y);
  }  
  
  public void centerAt(float cx, float cy, float cz) {
    float dx = cx - 0.5f * (xmin + xmax);
    float dy = cy - 0.5f * (ymin + ymax);
    float dz = cz - 0.5f * (zmin + zmax);
    // Centering
    loadVertices();    
    for (int i = 0; i < vertexCount; i++) {
      vertices[3 * i + 0] += dx;
      vertices[3 * i + 1] += dy;
      vertices[3 * i + 2] += dz;  
    }    
    updateVertices();
  }
  
  
  ////////////////////////////////////////////////////////////  
  
  // Bulk vertex operations.
  
  public void setVertices(ArrayList<PVector> vertexList) {
    setVertices(vertexList, 0); 
  }
  
  public void setVertices(ArrayList<PVector> vertexList, int offset) {
    loadVertices();
    for (int i = firstVertex; i <= lastVertex; i++) {
      PVector v = (PVector)vertexList.get(i - firstVertex + offset);
      set(i, v.x, v.y, v.z);
    }
    updateVertices();
  }
  
  public void setColors(ArrayList<float[]> colorList) {
    setColors(colorList, 0);
  }
  
  public void setColors(ArrayList<float[]> colorList, int offset) {
    loadColors();
    for (int i = firstVertex; i <= lastVertex; i++) {
      float[] c = (float[])colorList.get(i - firstVertex + offset);
      set(i, c);
    }
    updateColors();    
  }  
  
  
  public void setNormals(ArrayList<PVector> normalList) {
    setNormals(normalList, 0); 
  }

  public void setNormals(ArrayList<PVector> normalList, int offset) {
    loadNormals();
    for (int i = firstVertex; i <= lastVertex; i++) {
      PVector n = (PVector)normalList.get(i - firstVertex + offset);
      set(i, n.x, n.y, n.z);
    }
    updateNormals();    
  }  

  
  public void setTexcoords(ArrayList<PVector> tcoordList) {
    setTexcoords(tcoordList, 0);
  }
  
  public void setTexcoords(ArrayList<PVector> tcoordList, int offset) {
    setTexcoords(0, tcoordList, offset);
  }  
  
  public void setTexcoords(int unit, ArrayList<PVector> tcoordList) {
    setTexcoords(unit, tcoordList, 0);
  }
  
  public void setTexcoords(int unit, ArrayList<PVector> tcoordList, int offset) {
    loadTexcoords(unit);
    for (int i = firstVertex; i <= lastVertex; i++) {
      PVector tc = (PVector)tcoordList.get(i - firstVertex + offset);
      set(i, tc.x, tc.y);
    }
    updateTexcoords();     
  }  
    
  
  public void setChildren(ArrayList<PShape3D> who) {
    if (family != GROUP) return; // Can be done only from a group shape.
    
    childCount = 0;
    for (int i = 0; i < who.size(); i++) {
      PShape child = (PShape)who.get(i);
      addChild(child);   
    }
  }  
  
  public void mergeChildren() {
    if (family != GROUP) return; // Can be done only from a group shape.
    
    if (children == null) {
      if (root == this) {
        addDefaultChild();
      } else {
        return;
      }
    } else {
      PShape3D child0, child1;

      // Expanding identical, contiguous shapes (names are ignored).     
      int ndiff = 1;    
      // Looking for the first geometry child.
      int i0 = 0;
      child0 = null;
      for (int i = 0; i < childCount; i++) {
        child0 = (PShape3D)children[i];
        if (child0.family == GROUP) {
          child0.mergeChildren();
        } else {
          i0 = i + 1;
          break;
        }
      }
      if (i0 == 0) return; // No geometry child found.
      for (int i = i0; i < childCount; i++) {
        child1 = (PShape3D)children[i];
        if (child1.family == GROUP) {
          child1.mergeChildren();
          continue;
        }        
        if (child0.equalTo(child1, true)) {
          child0.lastVertex = child1.lastVertex;       // Extending child0.
          // Updating the vertex data:
          for (int n = child0.firstVertex; n <= child0.lastVertex; n++) {
            vertexChild[n] = child0;
          }    
          child1.firstVertex = child1.lastVertex = -1; // Marking for deletion.
        } else {
          child0 = child1;
          ndiff++;
        }
      }
      
      // Deleting superfluous shapes.
      PShape[] temp = new PShape[ndiff]; 
      int n = 0;
      for (int i = 0; i < childCount; i++) {
        child1 = (PShape3D)children[i];      
        if (child1.family == GEOMETRY && child1.lastVertex == -1 && 
            child1.getName() != null && nameTable != null) {
          nameTable.remove(child1.getName());
        } else {
          temp[n++] = child1;
        }
      }      
      children = temp;
      childCount = ndiff;
    }
  }  
  
  public void translateVertices(float tx, float ty) {
    translateVertices(tx, ty, 0);
  }


  public void translateVertices(float tx, float ty, float tz) {
    init();
    loadVertices();
    for (int i = firstVertex; i <= lastVertex; i++) {
      vertices[3 * i + 0] += tx;
      vertices[3 * i + 1] += -ty;
      vertices[3 * i + 2] += tz;  
    }
    updateVertices();
  }  
  
  
  public void rotateVerticesX(float angle) {
    rotateVertices(angle, 1, 0, 0);
  }


  public void rotateVerticesY(float angle) {
    rotateVertices(angle, 0, 1, 0);
  }


  public void rotateVerticesZ(float angle) {
    rotateVertices(angle, 0, 0, 1);
  }


  public void rotateVertices(float angle) {
    rotateVertices(angle, 0, 0, 1);
  }


  public void rotateVertices(float angle, float v0, float v1, float v2) {
    init();

    float norm2 = v0 * v0 + v1 * v1 + v2 * v2;
    if (Math.abs(norm2 - 1) > EPSILON) {
      // Normalizing rotation axis vector.
      float norm = PApplet.sqrt(norm2);
      v0 /= norm;
      v1 /= norm;
      v2 /= norm;
    }
    
    // Rotating around the center of the shape.
    float cx = 0.5f * (xmin + xmax);
    float cy = 0.5f * (ymin + ymax);
    float cz = 0.5f * (zmin + zmax);
    
    // Rotation matrix
    float c = PApplet.cos(angle);
    float s = PApplet.sin(angle);
    float t = 1.0f - c;
    float[] m = new float[9];
    m[0] = (t*v0*v0) + c;          // 0, 0
    m[1] = (t*v0*v1) - (s*v2);     // 0, 1
    m[2] = (t*v0*v2) + (s*v1);     // 0, 2 
    m[3] = (t*v0*v1) + (s*v2);     // 1, 0
    m[4] = (t*v1*v1) + c;          // 1, 1
    m[5] = (t*v1*v2) - (s*v0);     // 1, 2
    m[6] = (t*v0*v2) - (s*v1);     // 2, 0
    m[7] = (t*v1*v2) + (s*v0);     // 2, 1 
    m[8] = (t*v2*v2) + c;          // 2, 2

    float x, y, z;
    
    loadVertices();    
    for (int i = firstVertex; i <= lastVertex; i++) {
      x = vertices[3 * i + 0] - cx; 
      y = vertices[3 * i + 1] - cy;
      z = vertices[3 * i + 2] - cz;      
      
      vertices[3 * i + 0] = m[0] * x + m[1] * y + m[2] * z + cx; 
      vertices[3 * i + 1] = m[3] * x + m[4] * y + m[5] * z + cy;
      vertices[3 * i + 2] = m[6] * x + m[7] * y + m[8] * z + cz;
    }
    updateVertices();
    
    // Re-centering, because of loss of precision when applying the
    // rotation matrix, the center of rotation moves slightly so 
    // after many consecutive rotations the object might translate
    // significantly.
    centerAt(cx, cy, cz);
    
    // The normals also need to be rotated to reflect the new orientation 
    //of the faces.
    loadNormals();
    for (int i = firstVertex; i <= lastVertex; i++) {
      x = normals[3 * i + 0]; 
      y = normals[3 * i + 1];
      z = normals[3 * i + 2];      
      
      normals[3 * i + 0] = m[0] * x + m[1] * y + m[2] * z + cx; 
      normals[3 * i + 1] = m[3] * x + m[4] * y + m[5] * z + cy;
      normals[3 * i + 2] = m[6] * x + m[7] * y + m[8] * z + cz;
    }            
    updateNormals();    
  }

  
  public void scaleVertices(float s) {
    scaleVertices(s, s, s);
  }


  public void scaleVertices(float sx, float sy) {
    scaleVertices(sx, sy, 1);
  }


  public void scaleVertices(float x, float y, float z) {
    init();    
    loadVertices();
    for (int i = firstVertex; i <= lastVertex; i++) {
      vertices[3 * i + 0] *= x;
      vertices[3 * i + 1] *= y;
      vertices[3 * i + 2] *= z;  
    }        
    updateVertices();       
  }
  
  
  ////////////////////////////////////////////////////////////  
  
  // Parameters  
  
  public Parameters getParameters() {
    if (root != this) return null; // Can be done only from the root shape.
    
    Parameters res = new Parameters();
    
    res.drawMode = getDrawModeImpl();
    
    if (glUsage == GL.GL_STATIC_DRAW) res.updateMode = STATIC;
    else if (glUsage == GL.GL_DYNAMIC_DRAW) res.updateMode = DYNAMIC;
    else if (glUsage == GL2.GL_STREAM_COPY) res.updateMode = STREAM;
    
    return res;
  }
  
  
  protected void setParameters(Parameters params) {
    if (root != this) return; // Can be done only from the root shape.
    
    setDrawModeImpl(params.drawMode);

    if (params.updateMode == STATIC) glUsage = GL.GL_STATIC_DRAW;
    else if (params.updateMode == DYNAMIC) glUsage = GL.GL_DYNAMIC_DRAW;
    else if (params.updateMode == STREAM) glUsage = GL2.GL_STREAM_COPY;
    else {
      throw new RuntimeException("PShape3D: Unknown update mode");
    }
  }
  
  
  ////////////////////////////////////////////////////////////
  
  // INDEXED MODE: TESTING
  
  public void initIndices(int n) {
    indexCount = n;
    
    glIndexBufferID = ogl.createGLResource(PGraphicsOpenGL.GL_VERTEX_BUFFER);    
    getGl().glBindBuffer(GL.GL_ARRAY_BUFFER, glIndexBufferID);    
    final int bufferSize = indexCount * PGraphicsOpenGL.SIZEOF_INT;
    getGl().glBufferData(GL.GL_ARRAY_BUFFER, bufferSize, null, GL.GL_STATIC_DRAW);    
    getGl().glBindBuffer(GL.GL_ARRAY_BUFFER, 0); 
    
    indices = new int[indexCount];
    useIndices = true;
  }
  
  public void setIndices(ArrayList<Integer> recordedIndices) {
    getGl().glBindBuffer(GL.GL_ELEMENT_ARRAY_BUFFER, glIndexBufferID);
    indexBuffer = getGl().glMapBuffer(GL.GL_ELEMENT_ARRAY_BUFFER, GL.GL_WRITE_ONLY).asIntBuffer();
    
    for (int i = 0; i < indexCount; i++) {
      indices[i] = (Integer)recordedIndices.get(i);
    }    
    indexBuffer.put(indices);    
    
    getGl().glUnmapBuffer(GL.GL_ELEMENT_ARRAY_BUFFER);
    getGl().glBindBuffer(GL.GL_ELEMENT_ARRAY_BUFFER, 0);    
  }
  
  public void setIndices(int src[]) {
    getGl().glBindBuffer(GL.GL_ELEMENT_ARRAY_BUFFER, glIndexBufferID);
    indexBuffer = getGl().glMapBuffer(GL.GL_ELEMENT_ARRAY_BUFFER, GL.GL_WRITE_ONLY).asIntBuffer();
    
    PApplet.arrayCopy(src, indices);
    indexBuffer.put(indices);    
    
    getGl().glUnmapBuffer(GL.GL_ELEMENT_ARRAY_BUFFER);
    getGl().glBindBuffer(GL.GL_ELEMENT_ARRAY_BUFFER, 0);    
  }    
    
  public void useIndices(boolean val) {
    if (family == GROUP) {
      init();
      for (int i = 0; i < childCount; i++) {
        useIndices(i, val);
      }            
    } else { 
      useIndices = val;
    }
  }
  
  public void useIndices(int idx, boolean val) {
    if (0 <= idx && idx < childCount) {
      ((PShape3D)children[idx]).useIndices = val;
      
      // Debugging. This mess needs to be fixed soon, which means 
      // using the indexed mode everywhere and sorting out the 
      // issues with children data.      
      ((PShape3D)children[idx]).firstIndex = 0;
      ((PShape3D)children[idx]).lastIndex = indexCount - 1;
    }
  }  
  
  ////////////////////////////////////////////////////////////  
  
  // Data allocation, deletion.

  public void init() {
    if (root != this) return; // Can be done only from the root shape.
    
    if (readFromOBJ) {
      recordOBJ();
      centerAt(0, 0, 0);      
    }    
    if (children == null) {
      addDefaultChild();
    }    
  }
  
  
  protected void initShape(int numVert) {
    initShape(numVert, new Parameters());
  }
  
  
  protected void initShape(int numVert, Parameters params) {
    // Checking we have what we need:
    if (ogl.gl2f == null) {
      throw new RuntimeException("PShape3D: OpenGL ES 1.1 required");
    }
    if (!PGraphicsOpenGL.vboSupported) {
       throw new RuntimeException("PShape3D: Vertex Buffer Objects are not available");
    }
    
    setParameters(params);
    allocateShape(numVert);         
    updateElement = -1;
    
    resetBounds();
  }
  
  
  protected void initShapeOBJ(String filename, Parameters params) {
    // Checking we have all we need:
    if (ogl.gl2f == null) {
      throw new RuntimeException("PShape3D: OpenGL ES 1.1 required");
    }
    if (!PGraphicsOpenGL.vboSupported) {
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
    if (params == null) {
      params = PShape3D.newParameters(TRIANGLES, STATIC);
    } else {
      params.drawMode = TRIANGLES;
    }
    setParameters(params);
    
    parseOBJ(reader, objVertices, objNormal, objTexCoords, objFaces, objMaterials);
    
    // Putting the number of vertices retrieved from the OBJ file in the vertex count
    // field, although the actual geometry hasn't been sent to the VBOs yet.
    vertexCount = objVertices.size();
  }
    
  protected void allocateShape(int numVert) {
    vertexCount = numVert;
    numTexBuffers = 1;
    firstVertex = 0;
    lastVertex = numVert - 1;
    
    initVertexData();
    createVertexBuffer();   
    initColorData();
    createColorBuffer();
    initNormalData();
    createNormalBuffer();
    initTexCoordData();
    createTexCoordBuffer();
    
    initChildrenData(); 
  }
  
  protected void initChildrenData() {
    children = null;
    vertexChild = new PShape3D[vertexCount];    
  }
  
  protected void initVertexData() {    
    vertices = new float[vertexCount * 3];
  }
  
  
  protected void createVertexBuffer() {    
    deleteVertexBuffer();  // Just in the case this object is being re-initialized.
    
    glVertexBufferID = ogl.createGLResource(PGraphicsOpenGL.GL_VERTEX_BUFFER);    
    getGl().glBindBuffer(GL.GL_ARRAY_BUFFER, glVertexBufferID);    
    final int bufferSize = vertexCount * 3 * PGraphicsOpenGL.SIZEOF_FLOAT;
    getGl().glBufferData(GL.GL_ARRAY_BUFFER, bufferSize, null, glUsage);    
    getGl().glBindBuffer(GL.GL_ARRAY_BUFFER, 0);
  }
  

  protected void initColorData() {
    colors = new float[vertexCount * 4];
    // Set the initial color of all vertices to white, so they are initially visible
    // even if the user doesn't set any vertex color.
    Arrays.fill(colors, 1.0f);
  }  
  
  
  protected void createColorBuffer() {
    deleteColorBuffer();
    
    glColorBufferID = ogl.createGLResource(PGraphicsOpenGL.GL_VERTEX_BUFFER);
    getGl().glBindBuffer(GL.GL_ARRAY_BUFFER, glColorBufferID);
    final int bufferSize = vertexCount * 4 * PGraphicsOpenGL.SIZEOF_FLOAT;    
    getGl().glBufferData(GL.GL_ARRAY_BUFFER, bufferSize, FloatBuffer.wrap(colors), glUsage);    
    getGl().glBindBuffer(GL.GL_ARRAY_BUFFER, 0);
  }
  

  protected void initNormalData() {
    normals = new float[vertexCount * 3];
  }  

  
  protected void createNormalBuffer() {
    deleteNormalBuffer();
    
    glNormalBufferID = ogl.createGLResource(PGraphicsOpenGL.GL_VERTEX_BUFFER);
    getGl().glBindBuffer(GL.GL_ARRAY_BUFFER, glNormalBufferID);
    final int bufferSize = vertexCount * 3 * PGraphicsOpenGL.SIZEOF_FLOAT;    
    getGl().glBufferData(GL.GL_ARRAY_BUFFER, bufferSize, null, glUsage);
    getGl().glBindBuffer(GL.GL_ARRAY_BUFFER, 0);
  }
  

  protected void initTexCoordData() {
    allTexcoords = new float[1][vertexCount * 2];
    texcoords = allTexcoords[0];
    convTexcoords = new float[vertexCount * 2];    
    texCoordSet = new boolean[PGraphicsOpenGL.MAX_TEXTURES];
  }  
  
  
  protected void createTexCoordBuffer() {
    if (glTexCoordBufferID == null) {
      glTexCoordBufferID = new int[PGraphicsOpenGL.MAX_TEXTURES];
      java.util.Arrays.fill(glTexCoordBufferID, 0);      
    } else {
      deleteTexCoordBuffer();
    }
    
    glTexCoordBufferID[0] = ogl.createGLResource(PGraphicsOpenGL.GL_VERTEX_BUFFER);
    getGl().glBindBuffer(GL.GL_ARRAY_BUFFER, glTexCoordBufferID[0]);
    final int bufferSize = vertexCount * 2 * PGraphicsOpenGL.SIZEOF_FLOAT;
    getGl().glBufferData(GL.GL_ARRAY_BUFFER, bufferSize, null, glUsage);
    getGl().glBindBuffer(GL.GL_ARRAY_BUFFER, 0);    
  }
  
  
  protected void addTexBuffers(int more) {
    for (int i = 0; i < more; i++) {
      int t = numTexBuffers + i;
      deleteTexCoordBuffer(t);
      
      glTexCoordBufferID[t] = ogl.createGLResource(PGraphicsOpenGL.GL_VERTEX_BUFFER);
      
      getGl().glBindBuffer(GL.GL_ARRAY_BUFFER, glTexCoordBufferID[t]);    
      final int bufferSize = vertexCount * 2 * PGraphicsOpenGL.SIZEOF_FLOAT;
      getGl().glBufferData(GL.GL_ARRAY_BUFFER, bufferSize, null, glUsage);
      getGl().glBindBuffer(GL.GL_ARRAY_BUFFER, 0);       
    }
    
    // We need more arrays for texture coordinates, and to save the contents of the already
    // existing ones.
    float temp[][] = new float[numTexBuffers + more][vertexCount * 2];
    for (int i = 0; i < numTexBuffers; i++) {
      PApplet.arrayCopy(allTexcoords[i], temp[i]);
    }
    allTexcoords = temp;
    texcoords = allTexcoords[0];
    
    numTexBuffers += more;

    // Updating the allTexcoords and numTexBuffers in all
    // child geometries.
    updateTexBuffers();
  }
  
  protected void updateTexBuffers() {
    if (family == GROUP) {
      for (int i = 0; i < childCount; i++) {
        ((PShape3D)children[i]).updateTexBuffers();
      }
    } else {
      numTexBuffers = root.numTexBuffers;
      allTexcoords = root.allTexcoords;
      texcoords = allTexcoords[0];
    }
  }
  
  protected void deleteVertexBuffer() {
    if (glVertexBufferID != 0) {    
      ogl.deleteGLResource(glVertexBufferID, PGraphicsOpenGL.GL_VERTEX_BUFFER);   
      glVertexBufferID = 0;
    }
  }  
  

  protected void deleteColorBuffer() {
    if (glColorBufferID != 0) {
      ogl.deleteGLResource(glColorBufferID, PGraphicsOpenGL.GL_VERTEX_BUFFER);
      glColorBufferID = 0;
    }
  }
  
  
  protected void deleteNormalBuffer() {
    if (glNormalBufferID != 0) {
      ogl.deleteGLResource(glNormalBufferID, PGraphicsOpenGL.GL_VERTEX_BUFFER);
      glNormalBufferID = 0;
    }
  }  

  
  protected void deleteIndexBuffer() {
    if (glIndexBufferID != 0) {
      ogl.deleteGLResource(glIndexBufferID, PGraphicsOpenGL.GL_VERTEX_BUFFER);
      glIndexBufferID = 0;    
    }
  }
  
  protected void deleteTexCoordBuffer() {
    for (int i = 0; i < numTexBuffers; i++) { 
      deleteTexCoordBuffer(i);    
    }
  }
  
  
  protected void deleteTexCoordBuffer(int idx) {  
    if (glTexCoordBufferID[idx] != 0) {
      ogl.deleteGLResource(glTexCoordBufferID[idx], PGraphicsOpenGL.GL_VERTEX_BUFFER);
      glTexCoordBufferID[idx] = 0;
    }
  }
  

  ///////////////////////////////////////////////////////////  

  // These methods are not available in PShape3D.  
  
  
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
  
  ///////////////////////////////////////////////////////////    
  
  // Style handling
  
  public void disableStyle() {
    style = false;
    if (family == GROUP) {
      init();
      for (int i = 0; i < childCount; i++) {
        children[i].disableStyle();
      }     
    }   
  }

  
  public void enableStyle() {
    style = true;
    if (family == GROUP) {
      init();
      for (int i = 0; i < childCount; i++) {
        children[i].enableStyle();
      }     
    }   
  }  
  
  
  protected void styles(PGraphics g) {
    // Nothing to do here. The styles are set in the drawGeometry() method.  
  }
  
  
  public boolean is3D() {
    return true;
  }  
  
  ///////////////////////////////////////////////////////////  
  
  //
  
  // Drawing methods
  
  
  public void draw() {
    draw(ogl);
  }
  
  
  public void draw(PGraphics g) {
    if (visible) {
      
      if (matrix != null) {
        g.pushMatrix();
        g.applyMatrix(matrix);
      }
    
      if (family == GROUP) {
        init();
        for (int i = 0; i < childCount; i++) {
          ((PShape3D)children[i]).draw(g);
        }
      
      } else {
        drawGeometry(g);
      }
    
      if (matrix != null) {
        g.popMatrix();
      } 
    }
  }

  
  protected void pre(PGraphics g) {
    if (matrix != null) {
      g.pushMatrix();
      g.applyMatrix(matrix);
    }
    // No need to push and set styles.
  }

  
  public void post(PGraphics g) {
    if (matrix != null) {
      g.popMatrix();
    }
  }
  
  
  public void drawImpl(PGraphics g) {
    if (family == GROUP) {
      drawGroup(g);
    } else {
      drawGeometry(g);
    }
  }
  

  protected void drawGroup(PGraphics g) {
    init();
    for (int i = 0; i < childCount; i++) {
      children[i].draw(g);
    }
  }
  

  protected void drawGeometry(PGraphics g) {
    int numTextures;
    float pointSize;

    // Setting line width and point size from stroke value, using 
    // either the group's weight or the renderer's weight. 
    if (0 < strokeWeight && style) {
      getGl().glLineWidth(strokeWeight);
      pointSize = PApplet.min(strokeWeight, PGraphicsOpenGL.maxPointSize);
    } else {
      getGl().glLineWidth(g.strokeWeight);
      pointSize = PApplet.min(g.strokeWeight, PGraphicsOpenGL.maxPointSize);
    }
    if (!pointSprites) {
      // Point sprites use their own size variable (set below).
      getGl().glPointSize(pointSize); 
    }
    
    getGl().glEnableClientState(GL2.GL_NORMAL_ARRAY);
    getGl().glBindBuffer(GL.GL_ARRAY_BUFFER, glNormalBufferID);
    getGl().glNormalPointer(GL.GL_FLOAT, 0, 0);    

    if (style) {
      getGl().glEnableClientState(GL2.GL_COLOR_ARRAY);
      getGl().glBindBuffer(GL.GL_ARRAY_BUFFER, glColorBufferID);
      getGl().glColorPointer(4, GL.GL_FLOAT, 0, 0);
    }        

    getGl().glEnableClientState(GL2.GL_VERTEX_ARRAY);            
    getGl().glBindBuffer(GL.GL_ARRAY_BUFFER, glVertexBufferID);
    getGl().glVertexPointer(3, GL.GL_FLOAT, 0, 0);

    numTextures = 0;
    if (style) {
      for (int t = 0; t < textures.length; t++) {
        if (textures[t] != null) {
          PTexture tex = (PTexture)textures[t].getCache(ogl);
          tex = ogl.getTexture(textures[t]);
          if (tex == null) {
            break;
          }

          getGl().glEnable(tex.glTarget);
          getGl().glActiveTexture(GL.GL_TEXTURE0 + t);
          getGl().glBindTexture(tex.glTarget, tex.glID);
          renderTextures[numTextures] = tex;
          numTextures++;
        } else {
          break;
        }
      }
    }

    if (0 < numTextures)  {        
      if (pointSprites) {
        // Texturing with point sprites.
        
        // The alpha of a point is calculated to allow the fading of points 
        // instead of shrinking them past a defined threshold size. The threshold 
        // is defined by GL_POINT_FADE_THRESHOLD_SIZE and is not clamped to the 
        // minimum and maximum point sizes.
        getGl().glPointParameterf(GL2.GL_POINT_FADE_THRESHOLD_SIZE, 0.6f * maxSpriteSize);
        getGl().glPointParameterf(GL2.GL_POINT_SIZE_MIN, 1.0f);
        getGl().glPointParameterf(GL2.GL_POINT_SIZE_MAX, maxSpriteSize);
        getGl().glPointSize(maxSpriteSize);
        
        // This is how will our point sprite's size will be modified by 
        // distance from the viewer:
        // actualSize = pointSize / sqrt(p[0] + p[1] * d + p[2] * d * d)
        // where pointSize is the value set with glPointSize(), clamped to the extreme values
        // in glPointParameterf(GL.GL_POINT_SIZE_MIN/GL.GL_POINT_SIZE_MAX. 
        // d is the distance from the point sprite to the camera and p is the array parameter 
        // passed in the following call: 
        getGl().glPointParameterfv(GL2.GL_POINT_DISTANCE_ATTENUATION, spriteDistAtt, 0);

        // Specify point sprite texture coordinate replacement mode for each 
        // texture unit
        getGl().glTexEnvf(GL2.GL_POINT_SPRITE, GL2.GL_COORD_REPLACE, GL.GL_TRUE);

        getGl().glEnable(GL2.GL_POINT_SPRITE);           
      } else {
        // Regular texturing.         
        for (int t = 0; t < numTextures; t++) {
          getGl().glClientActiveTexture(GL.GL_TEXTURE0 + t);
          getGl().glEnableClientState(GL2.GL_TEXTURE_COORD_ARRAY);
          getGl().glBindBuffer(GL.GL_ARRAY_BUFFER, glTexCoordBufferID[t]);
          getGl().glTexCoordPointer(2, GL.GL_FLOAT, 0, 0);
        } 
        if (1 < numTextures) {
          ogl.setupTextureBlend(renderTextures, numTextures);
        }
      }
    }

    if (!style) {
      // Using fill or tint color when the style is disabled.
      if (0 < numTextures) {
        if (g.tint) {
          ogl.setTintColor();  
        } else {
          getGl().glColor4f(1, 1, 1, 1);  
        }
      } else {
        ogl.setFillColor();                  
      }              
    }
    
    if (glIndexBufferID != 0 && useIndices) {
      getGl().glBindBuffer(GL.GL_ELEMENT_ARRAY_BUFFER, glIndexBufferID);
      // Here the vertex indices are understood as the range of indices.
      int last = lastIndex;
      int first = firstIndex;
      getGl().glDrawElements(glMode, last - first + 1, GL.GL_UNSIGNED_INT, first * PGraphicsOpenGL.SIZEOF_INT);      
      getGl().glBindBuffer(GL.GL_ELEMENT_ARRAY_BUFFER, 0);
    } else {
      getGl().glDrawArrays(glMode, firstVertex, lastVertex - firstVertex + 1);  
    }

    if (0 < numTextures) {
      for (int t = 0; t < numTextures; t++) {
        PTexture tex = renderTextures[t];
        getGl().glActiveTexture(GL.GL_TEXTURE0 + t);
        getGl().glBindTexture(tex.glTarget, 0); 
      }      
      for (int t = 0; t < numTextures; t++) {
        PTexture tex = renderTextures[t];
        getGl().glDisable(tex.glTarget);
      }            
      if (pointSprites)   {
        getGl().glDisable(GL2.GL_POINT_SPRITE);
      } else {
        for (int t = 0; t < numTextures; t++) {
          getGl().glClientActiveTexture(GL.GL_TEXTURE0 + t);        
          getGl().glDisableClientState(GL2.GL_TEXTURE_COORD_ARRAY);
        }
        if (1 < numTextures) {
          ogl.cleanupTextureBlend(numTextures);
        }         
      }
    }

    getGl().glBindBuffer(GL.GL_ARRAY_BUFFER, 0);
    getGl().glDisableClientState(GL2.GL_VERTEX_ARRAY);
    getGl().glDisableClientState(GL2.GL_COLOR_ARRAY);
    getGl().glDisableClientState(GL2.GL_NORMAL_ARRAY);
  }
  
  
  /////////////////////////////////////////////////////////////////////////// 

  // Utilities 
  
  
  protected GL2ES1 getGl() {
    return ogl.gl2f;
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
      String gname = "object";
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
            gname = elements[1];
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
            face.name = gname;
            
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
  
  protected void recordOBJ() {
    recordOBJ(objVertices, objNormal, objTexCoords, objFaces, objMaterials);
    objVertices = null; 
    objNormal = null; 
    objTexCoords = null;    
    objFaces = null;
    objMaterials = null;    
    
    readFromOBJ = false;
  }
  
  protected void recordOBJ(ArrayList<PVector> vertices, ArrayList<PVector> normals, ArrayList<PVector> textures, ArrayList<OBJFace> faces, ArrayList<OBJMaterial> materials) {
    int mtlIdxCur = -1;
    OBJMaterial mtl = null;
    
    ogl.saveDrawingState();
    
    // The recorded shapes are not merged, they are grouped
    // according to the group names found in the OBJ file.    
    ogl.mergeRecShapes = false;
    
    // Using RGB mode for coloring.
    ogl.colorMode = RGB;
    
    // Strokes are not used to draw the model.
    ogl.stroke = false;    
    
    // Normals are automatically computed if not specified in the OBJ file.
    ogl.autoNormal(true);
    
    // Using normal mode for texture coordinates (i.e.: normalized between 0 and 1).
    ogl.textureMode = NORMAL;    
    
    ogl.beginShapeRecorderImpl();    
    for (int i = 0; i < faces.size(); i++) {
      OBJFace face = (OBJFace) faces.get(i);
      
      // Getting current material.
      if (mtlIdxCur != face.matIdx) {
        mtlIdxCur = PApplet.max(0, face.matIdx); // To make sure that at least we get the default material.
        
        mtl = (OBJMaterial) materials.get(mtlIdxCur);

        // Setting colors.
        ogl.specular(mtl.ks.x * 255.0f, mtl.ks.y * 255.0f, mtl.ks.z * 255.0f);
        ogl.ambient(mtl.ka.x * 255.0f, mtl.ka.y * 255.0f, mtl.ka.z * 255.0f);
        if (ogl.fill) {
          ogl.fill(mtl.kd.x * 255.0f, mtl.kd.y * 255.0f, mtl.kd.z * 255.0f, mtl.d * 255.0f);  
        }        
        ogl.shininess(mtl.ns);
        
        if (ogl.tint && mtl.kdMap != null) {
          // If current material is textured, then tinting the texture using the diffuse color.
          ogl.tint(mtl.kd.x * 255.0f, mtl.kd.y * 255.0f, mtl.kd.z * 255.0f, mtl.d * 255.0f);
        }
      }

      // Recording current face.
      if (face.vertIdx.size() == 3) {
        ogl.beginShape(TRIANGLES); // Face is a triangle, so using appropriate shape kind.
      } else if (face.vertIdx.size() == 4) {
        ogl.beginShape(QUADS);        // Face is a quad, so using appropriate shape kind.
      } else {
        ogl.beginShape();  
      }      
      
      ogl.shapeName(face.name);
      
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
          
          PTexture texMtl = (PTexture)mtl.kdMap.getCache(ogl);
          if (texMtl != null) {     
            // Texture orientation in Processing is inverted.
            texMtl.setFlippedY(true);          
          }
          ogl.texture(mtl.kdMap);
          if (norms != null) {
            ogl.normal(norms.x, norms.y, norms.z);
          }
          if (tex != null) {
            ogl.vertex(vert.x, vert.y, vert.z, tex.x, tex.y);  
          } else {
            ogl.vertex(vert.x, vert.y, vert.z);
          }
        } else {
          // This face is not textured.
          if (norms != null) {
            ogl.normal(norms.x, norms.y, norms.z);
          }
          ogl.vertex(vert.x, vert.y, vert.z);          
        }
      } 
      ogl.endShape(CLOSE);
    }
    
    // Allocate space for the geometry that the triangulator has generated from the OBJ model.
    allocateShape(ogl.recordedVertices.size());          
    updateElement = -1;
    
    width = height = depth = 0;
    xmin = ymin = zmin = 10000;
    xmax = ymax = zmax = -10000;
    
    ogl.endShapeRecorderImpl(this);
    
    ogl.restoreDrawingState();    
  }
	

  protected class OBJFace {
    ArrayList<Integer> vertIdx;
    ArrayList<Integer> texIdx;
    ArrayList<Integer> normIdx;
    int matIdx;
    String name;
    
    OBJFace() {
      vertIdx = new ArrayList<Integer>();
      texIdx = new ArrayList<Integer>();
      normIdx = new ArrayList<Integer>();
      matIdx = -1;
      name = "";
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
