/* -*- mode: java; c-basic-offset: 2; indent-tabs-mode: nil -*- */

/*
  Part of the Processing project - http://processing.org

  Copyright (c) 2011-12 Ben Fry and Casey Reas

  This library is free software; you can redistribute it and/or
  modify it under the terms of the GNU Lesser General Public
  License as published by the Free Software Foundation, version 2.1.

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

import processing.core.PApplet;
import processing.core.PConstants;
import processing.core.PGraphics;
import processing.core.PImage;
import processing.core.PMatrix;
import processing.core.PMatrix2D;
import processing.core.PMatrix3D;
import processing.core.PShape;
import processing.core.PVector;
import processing.opengl.PGraphicsOpenGL.LineShader;
import processing.opengl.PGraphicsOpenGL.PointShader;
import processing.opengl.PGraphicsOpenGL.BaseShader;
import processing.opengl.PGraphicsOpenGL.IndexCache;
import processing.opengl.PGraphicsOpenGL.InGeometry;
import processing.opengl.PGraphicsOpenGL.TessGeometry;
import processing.opengl.PGraphicsOpenGL.Tessellator;

import java.util.Arrays;
import java.util.HashSet;

/**
 * This class holds a 3D model composed of vertices, normals, colors
 * (per vertex) and texture coordinates (also per vertex). All this data is
 * stored in Vertex Buffer Objects (VBO) in GPU memory for very fast access.
 * OBJ loading implemented using code from Saito's OBJLoader library:
 * http://code.google.com/p/saitoobjloader/
 * and OBJReader from Ahmet Kizilay
 * http://www.openprocessing.org/visuals/?visualID=191
 * By Andres Colubri
 *
 *
 * Other formats to consider:
 * AMF: http://en.wikipedia.org/wiki/Additive_Manufacturing_File_Format
 * STL: http://en.wikipedia.org/wiki/STL_(file_format)
 * OFF: http://people.sc.fsu.edu/~jburkardt/data/off/off.html(file_format)
 * DXF: http://en.wikipedia.org/wiki/AutoCAD_DXF
 */
public class PShapeOpenGL extends PShape {
  static protected final int TRANSLATE = 0;
  static protected final int ROTATE    = 1;
  static protected final int SCALE     = 2;
  static protected final int MATRIX    = 3;

  protected PGraphicsOpenGL pg;
  protected PGL pgl;
  protected int context;      // The context that created this shape.

  protected PShapeOpenGL root;

  // ........................................................

  // Input, tessellated geometry

  protected InGeometry inGeo;
  protected TessGeometry tessGeo;
  protected Tessellator tessellator;

  // ........................................................

  // Texturing

  protected HashSet<PImage> textures;
  protected boolean strokedTexture;

  // ........................................................

  // OpenGL buffers

  public int glPolyVertex;
  public int glPolyColor;
  public int glPolyNormal;
  public int glPolyTexcoord;
  public int glPolyAmbient;
  public int glPolySpecular;
  public int glPolyEmissive;
  public int glPolyShininess;
  public int glPolyIndex;

  public int glLineVertex;
  public int glLineColor;
  public int glLineAttrib;
  public int glLineIndex;

  public int glPointVertex;
  public int glPointColor;
  public int glPointAttrib;
  public int glPointIndex;

  // ........................................................

  // Offsets for geometry aggregation and update.

  protected int polyVertCopyOffset;
  protected int polyIndCopyOffset;
  protected int lineVertCopyOffset;
  protected int lineIndCopyOffset;
  protected int pointVertCopyOffset;
  protected int pointIndCopyOffset;

  protected int polyIndexOffset;
  protected int polyVertexOffset;
  protected int polyVertexAbs;
  protected int polyVertexRel;

  protected int lineIndexOffset;
  protected int lineVertexOffset;
  protected int lineVertexAbs;
  protected int lineVertexRel;

  protected int pointIndexOffset;
  protected int pointVertexOffset;
  protected int pointVertexAbs;
  protected int pointVertexRel;

  protected int firstPolyIndexCache;
  protected int lastPolyIndexCache;
  protected int firstLineIndexCache;
  protected int lastLineIndexCache;
  protected int firstPointIndexCache;
  protected int lastPointIndexCache;

  protected int firstPolyVertex;
  protected int lastPolyVertex;
  protected int firstLineVertex;
  protected int lastLineVertex;
  protected int firstPointVertex;
  protected int lastPointVertex;

  // ........................................................

  // Geometric transformations.

  protected PMatrix transform;

  // ........................................................

  // State/rendering flags

  protected boolean tessellated;
  protected boolean needBufferInit;

  protected boolean isSolid;
  protected boolean isClosed;

  protected boolean breakShape = false;
  protected boolean shapeCreated = false;

  // These variables indicate if the shape contains
  // polygon, line and/or point geometry. In the case of
  // 3D shapes, poly geometry is coincident with the fill
  // triangles, as the lines and points are stored separately.
  // However, for 2D shapes the poly geometry contains all of
  // the three since the same rendering shader applies to
  // fill, line and point geometry.
  protected boolean hasPolys;
  protected boolean hasLines;
  protected boolean hasPoints;

  // ........................................................

  // Modes inherited from renderer

  protected int rectMode;
  protected int ellipseMode;
  protected int shapeMode;
  protected int imageMode;

  // ........................................................

  // Bezier and Catmull-Rom curves

  protected int bezierDetail = 20;
  protected int curveDetail = 20;
  protected float curveTightness = 0;

  // ........................................................

  // Normals

  protected float normalX, normalY, normalZ;

  // normal calculated per triangle
  static protected final int NORMAL_MODE_AUTO = 0;
  // one normal manually specified per shape
  static protected final int NORMAL_MODE_SHAPE = 1;
  // normals specified for each shape vertex
  static protected final int NORMAL_MODE_VERTEX = 2;

  // Current mode for normals, one of AUTO, SHAPE, or VERTEX
  protected int normalMode;

  // ........................................................

  // Modification variables (used only by the root shape)

  protected boolean modified;

  protected boolean modifiedPolyVertices;
  protected boolean modifiedPolyColors;
  protected boolean modifiedPolyNormals;
  protected boolean modifiedPolyTexCoords;
  protected boolean modifiedPolyAmbient;
  protected boolean modifiedPolySpecular;
  protected boolean modifiedPolyEmissive;
  protected boolean modifiedPolyShininess;

  protected boolean modifiedLineVertices;
  protected boolean modifiedLineColors;
  protected boolean modifiedLineAttributes;

  protected boolean modifiedPointVertices;
  protected boolean modifiedPointColors;
  protected boolean modifiedPointAttributes;

  protected int firstModifiedPolyVertex;
  protected int lastModifiedPolyVertex;
  protected int firstModifiedPolyColor;
  protected int lastModifiedPolyColor;
  protected int firstModifiedPolyNormal;
  protected int lastModifiedPolyNormal;
  protected int firstModifiedPolyTexcoord;
  protected int lastModifiedPolyTexcoord;
  protected int firstModifiedPolyAmbient;
  protected int lastModifiedPolyAmbient;
  protected int firstModifiedPolySpecular;
  protected int lastModifiedPolySpecular;
  protected int firstModifiedPolyEmissive;
  protected int lastModifiedPolyEmissive;
  protected int firstModifiedPolyShininess;
  protected int lastModifiedPolyShininess;

  protected int firstModifiedLineVertex;
  protected int lastModifiedLineVertex;
  protected int firstModifiedLineColor;
  protected int lastModifiedLineColor;
  protected int firstModifiedLineAttribute;
  protected int lastModifiedLineAttribute;

  protected int firstModifiedPointVertex;
  protected int lastModifiedPointVertex;
  protected int firstModifiedPointColor;
  protected int lastModifiedPointColor;
  protected int firstModifiedPointAttribute;
  protected int lastModifiedPointAttribute;


  PShapeOpenGL() {
  }


  public PShapeOpenGL(PApplet parent, int family) {
    pg = (PGraphicsOpenGL)parent.g;
    pgl = pg.pgl;
    context = pgl.createEmptyContext();

    glPolyVertex = 0;
    glPolyColor = 0;
    glPolyNormal = 0;
    glPolyTexcoord = 0;
    glPolyAmbient = 0;
    glPolySpecular = 0;
    glPolyEmissive = 0;
    glPolyShininess = 0;
    glPolyIndex = 0;

    glLineVertex = 0;
    glLineColor = 0;
    glLineAttrib = 0;
    glLineIndex = 0;

    glPointVertex = 0;
    glPointColor = 0;
    glPointAttrib = 0;
    glPointIndex = 0;

    this.tessellator = PGraphicsOpenGL.tessellator;
    this.family = family;
    this.root = this;
    this.parent = null;
    this.tessellated = false;

    if (family == GEOMETRY || family == PRIMITIVE || family == PATH) {
      inGeo = pg.newInGeometry(PGraphicsOpenGL.RETAINED);
    }

    // Modes are retrieved from the current values in the renderer.
    textureMode = pg.textureMode;
    rectMode = pg.rectMode;
    ellipseMode = pg.ellipseMode;
    shapeMode = pg.shapeMode;
    imageMode = pg.imageMode;

    colorMode(pg.colorMode,
              pg.colorModeX, pg.colorModeY, pg.colorModeZ, pg.colorModeA);

    // Initial values for fill, stroke and tint colors are also imported from
    // the renderer. This is particular relevant for primitive shapes, since is
    // not possible to set their color separately when creating them, and their
    // input vertices are actually generated at rendering time, by which the
    // color configuration of the renderer might have changed.
    fill = pg.fill;
    fillColor = pg.fillColor;

    stroke = pg.stroke;
    strokeColor = pg.strokeColor;
    strokeWeight = pg.strokeWeight;
    strokeCap = pg.strokeCap;
    strokeJoin = pg.strokeJoin;

    tint = pg.tint;
    tintColor = pg.tintColor;

    setAmbient = pg.setAmbient;
    ambientColor = pg.ambientColor;
    specularColor = pg.specularColor;
    emissiveColor = pg.emissiveColor;
    shininess = pg.shininess;

    normalX = normalY = 0;
    normalZ = 1;

    normalMode = NORMAL_MODE_AUTO;

    // To make sure that the first vertex is marked as a break.
    // Same behavior as in the immediate mode.
    breakShape = true;

    if (family == GROUP) {
      // GROUP shapes are always marked as ended.
      shapeCreated = true;
    }
  }


  @Override
  public void addChild(PShape child) {
    if (child instanceof PShapeOpenGL) {
      if (family == GROUP) {
        PShapeOpenGL c3d = (PShapeOpenGL)child;

        super.addChild(c3d);
        c3d.updateRoot(root);
        markForTessellation();

        if (c3d.family == GROUP) {
          if (c3d.textures != null) {
            for (PImage tex: c3d.textures) {
              addTexture(tex);
            }
          }
          if (c3d.strokedTexture) {
            strokedTexture(true);
          }
        } else {
          if (c3d.image != null) {
            addTexture(c3d.image);
            if (c3d.stroke) {
              strokedTexture(true);
            }
          }
        }

      } else {
        PGraphics.showWarning("Cannot add child shape to non-group shape.");
      }
    } else {
      PGraphics.showWarning("Shape must be 3D to be added to the group.");
    }
  }


  protected void updateRoot(PShape root) {
    this.root = (PShapeOpenGL) root;
    if (family == GROUP) {
      for (int i = 0; i < childCount; i++) {
        PShapeOpenGL child = (PShapeOpenGL)children[i];
        child.updateRoot(root);
      }
    }
  }


  @Override
  protected void finalize() throws Throwable {
    try {
      finalizePolyBuffers();
      finalizeLineBuffers();
      finalizePointBuffers();
    } finally {
      super.finalize();
    }
  }


  protected void finalizePolyBuffers() {
    if (glPolyVertex != 0) {
      pg.finalizeVertexBufferObject(glPolyVertex, context);
    }

    if (glPolyColor != 0) {
      pg.finalizeVertexBufferObject(glPolyColor, context);
    }

    if (glPolyNormal != 0) {
      pg.finalizeVertexBufferObject(glPolyNormal, context);
    }

    if (glPolyTexcoord != 0) {
      pg.finalizeVertexBufferObject(glPolyTexcoord, context);
    }

    if (glPolyAmbient != 0) {
      pg.finalizeVertexBufferObject(glPolyAmbient, context);
    }

    if (glPolySpecular != 0) {
      pg.finalizeVertexBufferObject(glPolySpecular, context);
    }

    if (glPolyEmissive != 0) {
      pg.finalizeVertexBufferObject(glPolyEmissive, context);
    }

    if (glPolyShininess != 0) {
      pg.finalizeVertexBufferObject(glPolyShininess, context);
    }

    if (glPolyIndex != 0) {
      pg.finalizeVertexBufferObject(glPolyIndex, context);
    }
  }


  protected void finalizeLineBuffers() {
    if (glLineVertex != 0) {
      pg.finalizeVertexBufferObject(glLineVertex, context);
    }

    if (glLineColor != 0) {
      pg.finalizeVertexBufferObject(glLineColor, context);
    }

    if (glLineAttrib != 0) {
      pg.finalizeVertexBufferObject(glLineAttrib, context);
    }

    if (glLineIndex != 0) {
      pg.finalizeVertexBufferObject(glLineIndex, context);
    }
  }


  protected void finalizePointBuffers() {
    if (glPointVertex != 0) {
      pg.finalizeVertexBufferObject(glPointVertex, context);
    }

    if (glPointColor != 0) {
      pg.finalizeVertexBufferObject(glPointColor, context);
    }

    if (glPointAttrib != 0) {
      pg.finalizeVertexBufferObject(glPointAttrib, context);
    }

    if (glPointIndex != 0) {
      pg.finalizeVertexBufferObject(glPointIndex, context);
    }
  }

  ///////////////////////////////////////////////////////////

  //
  // Shape creation (temporary hack)


  public static PShapeOpenGL createShape3D(PApplet parent, PShape src) {
    PShapeOpenGL dest = null;
    if (src.getFamily() == GROUP) {
      dest = PGraphics3D.createShapeImpl(parent, GROUP);
      copyGroup3D(parent, src, dest);
    } else if (src.getFamily() == PRIMITIVE) {
      dest = PGraphics3D.createShapeImpl(parent, src.getKind(),
                                         src.getParams());
      PShape.copyPrimitive(src, dest);
    } else if (src.getFamily() == GEOMETRY) {
      dest = PGraphics3D.createShapeImpl(parent, PShape.GEOMETRY);
      PShape.copyGeometry(src, dest);
    } else if (src.getFamily() == PATH) {
      dest = PGraphics3D.createShapeImpl(parent, PATH);
      PShape.copyPath(src, dest);
    }
    dest.setName(src.getName());
    dest.width = src.width;
    dest.height = src.height;
    dest.depth = src.depth;
    return dest;
  }


  static public PShapeOpenGL createShape2D(PApplet parent, PShape src) {
    PShapeOpenGL dest = null;
    if (src.getFamily() == GROUP) {
      dest = PGraphics2D.createShapeImpl(parent, GROUP);
      copyGroup2D(parent, src, dest);
    } else if (src.getFamily() == PRIMITIVE) {
      dest = PGraphics2D.createShapeImpl(parent, src.getKind(),
                                         src.getParams());
      PShape.copyPrimitive(src, dest);
    } else if (src.getFamily() == GEOMETRY) {
      dest = PGraphics2D.createShapeImpl(parent, PShape.GEOMETRY);
      PShape.copyGeometry(src, dest);
    } else if (src.getFamily() == PATH) {
      dest = PGraphics2D.createShapeImpl(parent, PATH);
      PShape.copyPath(src, dest);
    }
    dest.setName(src.getName());
    dest.width = src.width;
    dest.height = src.height;
    return dest;
  }


  static public void copyGroup3D(PApplet parent, PShape src, PShape dest) {
    copyMatrix(src, dest);
    copyStyles(src, dest);
    copyImage(src, dest);

    for (int i = 0; i < src.getChildCount(); i++) {
      PShape c = createShape3D(parent, src.getChild(i));
      dest.addChild(c);
    }
  }


  static public void copyGroup2D(PApplet parent, PShape src, PShape dest) {
    copyMatrix(src, dest);
    copyStyles(src, dest);
    copyImage(src, dest);

    for (int i = 0; i < src.getChildCount(); i++) {
      PShape c = createShape2D(parent, src.getChild(i));
      dest.addChild(c);
    }
  }


  ///////////////////////////////////////////////////////////

  //

  // Query methods


  @Override
  public float getWidth() {
    PVector min = new PVector(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY,
                              Float.POSITIVE_INFINITY);
    PVector max = new PVector(Float.NEGATIVE_INFINITY, Float.NEGATIVE_INFINITY,
                              Float.NEGATIVE_INFINITY);
    if (shapeCreated) {
      getVertexMin(min);
      getVertexMax(max);
    }
    width = max.x - min.x;
    return width;
  }


  @Override
  public float getHeight() {
    PVector min = new PVector(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY,
                              Float.POSITIVE_INFINITY);
    PVector max = new PVector(Float.NEGATIVE_INFINITY, Float.NEGATIVE_INFINITY,
                              Float.NEGATIVE_INFINITY);
    if (shapeCreated) {
      getVertexMin(min);
      getVertexMax(max);
    }
    height = max.y - min.y;
    return height;
  }


  @Override
  public float getDepth() {
    PVector min = new PVector(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY,
                              Float.POSITIVE_INFINITY);
    PVector max = new PVector(Float.NEGATIVE_INFINITY, Float.NEGATIVE_INFINITY,
                              Float.NEGATIVE_INFINITY);
    if (shapeCreated) {
      getVertexMin(min);
      getVertexMax(max);
    }
    depth = max.z - min.z;
    return depth;
  }


  protected void getVertexMin(PVector min) {
    updateTessellation();

    if (family == GROUP) {
      for (int i = 0; i < childCount; i++) {
        PShapeOpenGL child = (PShapeOpenGL) children[i];
        child.getVertexMin(min);
      }
    } else {
      if (hasPolys) {
        tessGeo.getPolyVertexMin(min, firstPolyVertex, lastPolyVertex);
      }
      if (is3D()) {
        if (hasLines) {
          tessGeo.getLineVertexMin(min, firstLineVertex, lastLineVertex);
        }
        if (hasPoints) {
          tessGeo.getPointVertexMin(min, firstPointVertex, lastPointVertex);
        }
      }
    }
  }


  protected void getVertexMax(PVector max) {
    updateTessellation();

    if (family == GROUP) {
      for (int i = 0; i < childCount; i++) {
        PShapeOpenGL child = (PShapeOpenGL) children[i];
        child.getVertexMax(max);
      }
    } else {
      if (hasPolys) {
        tessGeo.getPolyVertexMax(max, firstPolyVertex, lastPolyVertex);
      }
      if (is3D()) {
        if (hasLines) {
          tessGeo.getLineVertexMax(max, firstLineVertex, lastLineVertex);
        }
        if (hasPoints) {
          tessGeo.getPointVertexMax(max, firstPointVertex, lastPointVertex);
        }
      }
    }
  }


  protected int getVertexSum(PVector sum, int count) {
    updateTessellation();

    if (family == GROUP) {
      for (int i = 0; i < childCount; i++) {
        PShapeOpenGL child = (PShapeOpenGL) children[i];
        count += child.getVertexSum(sum, count);
      }
    } else {
      if (hasPolys) {
        count += tessGeo.getPolyVertexSum(sum, firstPolyVertex, lastPolyVertex);
      }
      if (is3D()) {
        if (hasLines) {
          count += tessGeo.getLineVertexSum(sum, firstLineVertex,
                                                 lastLineVertex);
        }
        if (hasPoints) {
          count += tessGeo.getPointVertexSum(sum, firstPointVertex,
                                                  lastPointVertex);
        }
      }
    }
    return count;
  }


  ///////////////////////////////////////////////////////////

  //

  // Drawing methods


  @Override
  public void setTextureMode(int mode) {
    if (openShape) {
      PGraphics.showWarning(INSIDE_BEGIN_END_ERROR, "setTextureMode()");
      return;
    }

    if (family == GROUP) {
      for (int i = 0; i < childCount; i++) {
        PShapeOpenGL child = (PShapeOpenGL) children[i];
        child.textureMode(mode);
      }
    } else {
      textureMode = mode;
    }
  }


  @Override
  public void setTexture(PImage tex) {
    if (openShape) {
      PGraphics.showWarning(INSIDE_BEGIN_END_ERROR, "setTexture()");
      return;
    }

    if (family == GROUP) {
      for (int i = 0; i < childCount; i++) {
        PShapeOpenGL child = (PShapeOpenGL) children[i];
        child.texture(tex);
      }
    } else {
      PImage tex0 = image;
      image = tex;
      if (tex0 != tex && parent != null) {
        ((PShapeOpenGL)parent).removeTexture(tex);
      }
      if (parent != null) {
        ((PShapeOpenGL)parent).addTexture(image);
        if (is2D() && stroke) {
          ((PShapeOpenGL)parent).strokedTexture(true);
        }
      }
    }
  }


  protected void addTexture(PImage tex) {
    if (textures == null) {
      textures = new HashSet<PImage>();
    }
    textures.add(tex);
    if (parent != null) {
      ((PShapeOpenGL)parent).addTexture(tex);
    }
  }


  protected void removeTexture(PImage tex) {
    if (textures == null || !textures.contains(tex)) return; // Nothing to remove.

    // First check that none of the child shapes
    // have texture tex...
    boolean childHasTex = false;
    for (int i = 0; i < childCount; i++) {
      PShapeOpenGL child = (PShapeOpenGL) children[i];
      if (child.hasTexture(tex)) {
        childHasTex = true;
        break;
      }
    }

    if (!childHasTex) {
      // ...if not, it is safe to remove from this shape.
      textures.remove(tex);
      if (textures.size() == 0) {
        textures = null;
      }
    }

    // Since this shape and all its child shapes don't contain
    // tex anymore, we now can remove it from the parent.
    if (parent != null) {
      ((PShapeOpenGL)parent).removeTexture(tex);
    }
  }


  protected void strokedTexture(boolean newValue) {
    if (strokedTexture == newValue) return; // Nothing to change.

    if (newValue) {
      strokedTexture = true;
    } else {
      // First check that none of the child shapes
      // have have a stroked texture...
      boolean childHasStrokedTex = false;
      for (int i = 0; i < childCount; i++) {
        PShapeOpenGL child = (PShapeOpenGL) children[i];
        if (child.hasStrokedTexture()) {
          childHasStrokedTex = true;
          break;
        }
      }

      if (!childHasStrokedTex) {
        // ...if not, it is safe to mark this shape as without
        // stroked texture.
        strokedTexture = false;
      }
    }

    // Now we can update the parent shape.
    if (parent != null) {
      ((PShapeOpenGL)parent).strokedTexture(newValue);
    }
  }


  protected boolean hasTexture(PImage tex) {
    if (family == GROUP) {
      return textures != null && textures.contains(tex);
    } else {
      return image == tex;
    }
  }


  protected boolean hasStrokedTexture() {
    if (family == GROUP) {
      return strokedTexture;
    } else {
      return image != null && stroke;
    }
  }


  @Override
  public void solid(boolean solid) {
    if (family == GROUP) {
      for (int i = 0; i < childCount; i++) {
        PShapeOpenGL child = (PShapeOpenGL) children[i];
        child.solid(solid);
      }
    } else {
      isSolid = solid;
    }
  }


  @Override
  public void beginContour() {
    super.beginContour();
    breakShape = true;
  }


  @Override
  public void vertex(float x, float y) {
    vertexImpl(x, y, 0, 0, 0);
  }


  @Override
  public void vertex(float x, float y, float u, float v) {
    vertexImpl(x, y, 0, u, v);
  }


  @Override
  public void vertex(float x, float y, float z) {
    vertexImpl(x, y, z, 0, 0);
  }


  @Override
  public void vertex(float x, float y, float z, float u, float v) {
    vertexImpl(x, y, z, u, v);
  }


  protected void vertexImpl(float x, float y, float z, float u, float v) {
    if (!openShape) {
      PGraphics.showWarning(OUTSIDE_BEGIN_END_ERROR, "vertex()");
      return;
    }

    if (family == GROUP) {
      PGraphics.showWarning("Cannot add vertices to GROUP shape");
      return;
    }

    boolean textured = image != null;
    int fcolor = 0x00;
    if (fill || textured) {
      if (!textured) {
        fcolor = fillColor;
      } else {
        if (tint) {
          fcolor = tintColor;
        } else {
          fcolor = 0xffFFFFFF;
        }
      }
    }

    if (image != null && textureMode == IMAGE) {
      u = PApplet.min(1, u / image.width);
      v = PApplet.min(1, v / image.height);
    }

    int scolor = 0x00;
    float sweight = 0;
    if (stroke) {
      scolor = strokeColor;
      sweight = strokeWeight;
    }

    inGeo.addVertex(x, y, z,
                 fcolor,
                 normalX, normalY, normalZ,
                 u, v,
                 scolor, sweight,
                 ambientColor, specularColor, emissiveColor, shininess,
                 vertexCode());

    markForTessellation();
  }


  protected int vertexCode() {
    int code = VERTEX;
    if (breakShape) {
      code = BREAK;
      breakShape = false;
    }
    return code;
  }


  @Override
  public void normal(float nx, float ny, float nz) {
    if (!openShape) {
      PGraphics.showWarning(OUTSIDE_BEGIN_END_ERROR, "normal()");
      return;
    }

    if (family == GROUP) {
      PGraphics.showWarning("Cannot set normal in GROUP shape");
      return;
    }

    normalX = nx;
    normalY = ny;
    normalZ = nz;

    // if drawing a shape and the normal hasn't been set yet,
    // then we need to set the normals for each vertex so far
    if (normalMode == NORMAL_MODE_AUTO) {
      // One normal per begin/end shape
      normalMode = NORMAL_MODE_SHAPE;
    } else if (normalMode == NORMAL_MODE_SHAPE) {
      // a separate normal for each vertex
      normalMode = NORMAL_MODE_VERTEX;
    }
  }


  @Override
  public void endShape(int mode) {
    super.endShape(mode);

    // Input arrays are trimmed since they are expanded by doubling their old
    // size, which might lead to arrays larger than the vertex counts.
    inGeo.trim();

    isClosed = mode == CLOSE;
    markForTessellation();
    shapeCreated = true;
  }


  @Override
  public void setParams(float[] source) {
    if (family != PRIMITIVE) {
      PGraphics.showWarning("Parameters can only be set to PRIMITIVE shapes");
      return;
    }

    super.setParams(source);
    markForTessellation();
    shapeCreated = true;
  }


  @Override
  public void setPath(int vcount, float[][] verts, int ccount, int[] codes) {
    if (family != PATH) {
      PGraphics.showWarning("Vertex coordinates and codes can only be set to " +
                            "PATH shapes");
      return;
    }

    super.setPath(vcount, verts, ccount, codes);
    markForTessellation();
    shapeCreated = true;
  }


  ///////////////////////////////////////////////////////////

  //

  // Geometric transformations


  @Override
  public void translate(float tx, float ty) {
    transform(TRANSLATE, tx, ty);
  }


  @Override
  public void translate(float tx, float ty, float tz) {
    transform(TRANSLATE, tx, ty, tz);
  }


  @Override
  public void rotate(float angle) {
    transform(ROTATE, angle);
  }


  @Override
  public void rotateX(float angle) {
    rotate(angle, 1, 0, 0);
  }


  @Override
  public void rotateY(float angle) {
    rotate(angle, 0, 1, 0);
  }


  @Override
  public void rotateZ(float angle) {
    transform(ROTATE, angle);
  }


  @Override
  public void rotate(float angle, float v0, float v1, float v2) {
    transform(ROTATE, angle, v0, v1, v2);
  }


  @Override
  public void scale(float s) {
    transform(SCALE, s, s);
  }


  @Override
  public void scale(float x, float y) {
    transform(SCALE, x, y);
  }


  @Override
  public void scale(float x, float y, float z) {
    transform(SCALE, x, y, z);
  }


  @Override
  public void applyMatrix(PMatrix2D source) {
    transform(MATRIX, source.m00, source.m01, source.m02,
                      source.m10, source.m11, source.m12);
  }


  @Override
  public void applyMatrix(float n00, float n01, float n02,
                          float n10, float n11, float n12) {
    transform(MATRIX, n00, n01, n02,
                      n10, n11, n12);
  }


  @Override
  public void applyMatrix(float n00, float n01, float n02, float n03,
                          float n10, float n11, float n12, float n13,
                          float n20, float n21, float n22, float n23,
                          float n30, float n31, float n32, float n33) {
    transform(MATRIX, n00, n01, n02, n03,
                      n10, n11, n12, n13,
                      n20, n21, n22, n23,
                      n30, n31, n32, n33);
  }


  @Override
  public void resetMatrix() {
    if (shapeCreated && matrix != null) {
      if (family == GROUP) {
        updateTessellation();
      }
      boolean res = matrix.invert();
      if (res) {
        if (tessellated) {
          applyMatrixImpl(matrix);
        }
        matrix = null;
      } else {
        PGraphics.showWarning("The transformation matrix cannot be inverted");
      }
    }
  }


  protected void transform(int type, float... args) {
    int dimensions;
    if (type == ROTATE) {
      dimensions = args.length == 1 ? 2 : 3;
    } else if (type == MATRIX) {
      dimensions = args.length == 6 ? 2 : 3;
    } else {
      dimensions = args.length;
    }
    transformImpl(type, dimensions, args);
  }


  protected void transformImpl(int type, int ncoords, float... args) {
    checkMatrix(ncoords);
    calcTransform(type, ncoords, args);
    if (tessellated) {
      applyMatrixImpl(transform);
    }
  }


  protected void calcTransform(int type, int dimensions, float... args) {
    if (transform == null) {
      if (dimensions == 2) {
        transform = new PMatrix2D();
      } else {
        transform = new PMatrix3D();
      }
    } else {
      transform.reset();
    }

    switch (type) {
    case TRANSLATE:
      if (dimensions == 3) {
        transform.translate(args[0], args[1], args[2]);
      } else {
        transform.translate(args[0], args[1]);
      }
      break;
    case ROTATE:
      if (dimensions == 3) {
        transform.rotate(args[0], args[1], args[2], args[3]);
      } else {
        transform.rotate(args[0]);
      }
      break;
    case SCALE:
      if (dimensions == 3) {
        transform.scale(args[0], args[1], args[2]);
      } else {
        transform.scale(args[0], args[1]);
      }
      break;
    case MATRIX:
      if (dimensions == 3) {
        transform.set(args[ 0], args[ 1], args[ 2], args[ 3],
                      args[ 4], args[ 5], args[ 6], args[ 7],
                      args[ 8], args[ 9], args[10], args[11],
                      args[12], args[13], args[14], args[15]);
      } else {
        transform.set(args[0], args[1], args[2],
                      args[3], args[4], args[5]);
      }
      break;
    }
    matrix.apply(transform);
  }


  protected void applyMatrixImpl(PMatrix matrix) {
    if (hasPolys) {
      tessGeo.applyMatrixOnPolyGeometry(matrix,
                                        firstPolyVertex, lastPolyVertex);
      root.setModifiedPolyVertices(firstPolyVertex, lastPolyVertex);
      root.setModifiedPolyNormals(firstPolyVertex, lastPolyVertex);
    }

    if (is3D()) {
      if (hasLines) {
        tessGeo.applyMatrixOnLineGeometry(matrix,
                                          firstLineVertex, lastLineVertex);
        root.setModifiedLineVertices(firstLineVertex, lastLineVertex);
        root.setModifiedLineAttributes(firstLineVertex, lastLineVertex);
      }

      if (hasPoints) {
        tessGeo.applyMatrixOnPointGeometry(matrix,
                                           firstPointVertex, lastPointVertex);
        root.setModifiedPointVertices(firstPointVertex, lastPointVertex);
      }
    }
  }


  ///////////////////////////////////////////////////////////

  //

  // Bezier curves


  @Override
  public void bezierDetail(int detail) {
    bezierDetail = detail;
    pg.bezierDetail(detail);
  }


  @Override
  public void bezierVertex(float x2, float y2,
                           float x3, float y3,
                           float x4, float y4) {
    bezierVertexImpl(x2, y2, 0,
                     x3, y3, 0,
                     x4, y4, 0);
  }


  @Override
  public void bezierVertex(float x2, float y2, float z2,
                           float x3, float y3, float z3,
                           float x4, float y4, float z4) {
    bezierVertexImpl(x2, y2, z2,
                     x3, y3, z3,
                     x4, y4, z4);
  }


  protected void bezierVertexImpl(float x2, float y2, float z2,
                                  float x3, float y3, float z3,
                                  float x4, float y4, float z4) {
    inGeo.setMaterial(fillColor, strokeColor, strokeWeight,
                      ambientColor, specularColor, emissiveColor, shininess);
    inGeo.setNormal(normalX, normalY, normalZ);
    inGeo.addBezierVertex(x2, y2, z2,
                       x3, y3, z3,
                       x4, y4, z4,
                       fill, stroke, bezierDetail, vertexCode(), kind);
  }


  @Override
  public void quadraticVertex(float cx, float cy,
                              float x3, float y3) {
    quadraticVertexImpl(cx, cy, 0,
                        x3, y3, 0);
  }


  @Override
  public void quadraticVertex(float cx, float cy, float cz,
                              float x3, float y3, float z3) {
    quadraticVertexImpl(cx, cy, cz,
                        x3, y3, z3);
  }


  protected void quadraticVertexImpl(float cx, float cy, float cz,
                                     float x3, float y3, float z3) {
    inGeo.setMaterial(fillColor, strokeColor, strokeWeight,
                      ambientColor, specularColor, emissiveColor, shininess);
    inGeo.setNormal(normalX, normalY, normalZ);
    inGeo.addQuadraticVertex(cx, cy, cz,
                          x3, y3, z3,
                          fill, stroke, bezierDetail, vertexCode(), kind);
  }


  ///////////////////////////////////////////////////////////

  //

  // Catmull-Rom curves


  @Override
  public void curveDetail(int detail) {
    curveDetail = detail;
    pg.curveDetail(detail);
  }


  @Override
  public void curveTightness(float tightness) {
    curveTightness = tightness;
    pg.curveTightness(tightness);
  }


  @Override
  public void curveVertex(float x, float y) {
    curveVertexImpl(x, y, 0);
  }


  @Override
  public void curveVertex(float x, float y, float z) {
    curveVertexImpl(x, y, z);
  }


  protected void curveVertexImpl(float x, float y, float z) {
    inGeo.setMaterial(fillColor, strokeColor, strokeWeight,
                      ambientColor, specularColor, emissiveColor, shininess);
    inGeo.setNormal(normalX, normalY, normalZ);
    inGeo.addCurveVertex(x, y, z,
                      fill, stroke, curveDetail, vertexCode(), kind);
  }


  ///////////////////////////////////////////////////////////

  //

  // Setters/getters of individual vertices


  @Override
  public int getVertexCount() {
    updateTessellation();
    return family == GROUP ? 0 : inGeo.vertexCount;
  }


  @Override
  public PVector getVertex(int index, PVector vec) {
    if (vec == null) {
      vec = new PVector();
    }
    vec.x = inGeo.vertices[3 * index + 0];
    vec.y = inGeo.vertices[3 * index + 1];
    vec.z = inGeo.vertices[3 * index + 2];
    return vec;
  }


  @Override
  public float getVertexX(int index) {
    return inGeo.vertices[3 * index + 0];
  }


  @Override
  public float getVertexY(int index) {
    return inGeo.vertices[3 * index + 1];
  }


  @Override
  public float getVertexZ(int index) {
    return inGeo.vertices[3 * index + 2];
  }


  @Override
  public void setVertex(int index, float x, float y) {
    setVertex(index, x, y, 0);
  }


  @Override
  public void setVertex(int index, float x, float y, float z) {
    if (openShape) {
      PGraphics.showWarning(INSIDE_BEGIN_END_ERROR, "setVertex()");
      return;
    }

    // TODO: in certain cases (kind = TRIANGLE, etc) the correspondence between
    // input and tessellated vertices is 1-1, so in those cases re-tessellation
    // wouldnt' be neccessary.
    inGeo.vertices[3 * index + 0] = x;
    inGeo.vertices[3 * index + 1] = y;
    inGeo.vertices[3 * index + 2] = z;
    markForTessellation();
  }


  @Override
  public void setVertex(int index, PVector vec) {
    if (openShape) {
      PGraphics.showWarning(INSIDE_BEGIN_END_ERROR, "setVertex()");
      return;
    }

    inGeo.vertices[3 * index + 0] = vec.x;
    inGeo.vertices[3 * index + 1] = vec.y;
    inGeo.vertices[3 * index + 2] = vec.z;
    markForTessellation();
  }


  @Override
  public PVector getNormal(int index, PVector vec) {
    if (vec == null) {
      vec = new PVector();
    }
    vec.x = inGeo.normals[3 * index + 0];
    vec.y = inGeo.normals[3 * index + 1];
    vec.z = inGeo.normals[3 * index + 2];
    return vec;
  }


  @Override
  public float getNormalX(int index) {
    return inGeo.normals[3 * index + 0];
  }


  @Override
  public float getNormalY(int index) {
    return inGeo.normals[3 * index + 1];
  }


  @Override
  public float getNormalZ(int index) {
    return inGeo.normals[3 * index + 2];
  }


  @Override
  public void setNormal(int index, float nx, float ny, float nz) {
    if (openShape) {
      PGraphics.showWarning(INSIDE_BEGIN_END_ERROR, "setNormal()");
      return;
    }

    inGeo.normals[3 * index + 0] = nx;
    inGeo.normals[3 * index + 1] = ny;
    inGeo.normals[3 * index + 2] = nz;
    markForTessellation();
  }


  @Override
  public float getTextureU(int index) {
    return inGeo.texcoords[2 * index + 0];
  }


  @Override
  public float getTextureV(int index) {
    return inGeo.texcoords[2 * index + 1];
  }


  @Override
  public void setTextureUV(int index, float u, float v) {
    if (openShape) {
      PGraphics.showWarning(INSIDE_BEGIN_END_ERROR, "setTextureUV()");
      return;
    }

    inGeo.texcoords[2 * index + 0] = u;
    inGeo.texcoords[2 * index + 1] = v;
    markForTessellation();
  }


  @Override
  public int getFill(int index) {
    if (family != GROUP && image == null) {
      return PGL.nativeToJavaARGB(inGeo.colors[index]);
    } else {
      return 0;
    }
  }


  @Override
  public void setFill(boolean fill) {
    if (openShape) {
      PGraphics.showWarning(INSIDE_BEGIN_END_ERROR, "setFill()");
      return;
    }

    if (family == GROUP) {
      for (int i = 0; i < childCount; i++) {
        PShapeOpenGL child = (PShapeOpenGL) children[i];
        child.setFill(fill);
      }
    } else if (this.fill && !fill) {
      setFillImpl(0x0);
    }
    this.fill = fill;
  }


  @Override
  public void setFill(int fill) {
    if (openShape) {
      PGraphics.showWarning(INSIDE_BEGIN_END_ERROR, "setFill()");
      return;
    }

    if (family == GROUP) {
      for (int i = 0; i < childCount; i++) {
        PShapeOpenGL child = (PShapeOpenGL) children[i];
        child.setFill(fill);
      }
    } else {
      setFillImpl(fill);
    }
  }


  protected void setFillImpl(int fill) {
    if (fillColor == fill) return;
    fillColor = fill;

    if (image == null) {
      Arrays.fill(inGeo.colors, 0, inGeo.vertexCount,
                  PGL.javaToNativeARGB(fillColor));
      if (shapeCreated && tessellated && hasPolys) {
        if (is3D()) {
          Arrays.fill(tessGeo.polyColors, firstPolyVertex, lastPolyVertex + 1,
                      PGL.javaToNativeARGB(fillColor));
          root.setModifiedPolyColors(firstPolyVertex, lastPolyVertex);
        } else if (is2D()) {
          int last1 = lastPolyVertex + 1;
          if (-1 < firstLineVertex) last1 = firstLineVertex;
          if (-1 < firstPointVertex) last1 = firstPointVertex;
          Arrays.fill(tessGeo.polyColors, firstPolyVertex, last1,
                      PGL.javaToNativeARGB(fillColor));
          root.setModifiedPolyColors(firstPolyVertex, last1 - 1);
        }
      }
    }

    if (!setAmbient) {
      // Setting the ambient color from the current fill
      // is what the old P3D did and allows to have an
      // default ambient color when the user doesn't specify
      // it explicitly.
      setAmbientImpl(fill);
      setAmbient = false;
    }
  }


  @Override
  public void setFill(int index, int fill) {
    if (openShape) {
      PGraphics.showWarning(INSIDE_BEGIN_END_ERROR, "setFill()");
      return;
    }

    if (image == null) {
      inGeo.colors[index] = PGL.javaToNativeARGB(fill);
      markForTessellation();
    }
  }


  @Override
  public int getTint(int index) {
    if (family != GROUP && image != null) {
      return PGL.nativeToJavaARGB(inGeo.colors[index]);
    } else {
      return 0;
    }
  }


  @Override
  public void setTint(boolean tint) {
    if (openShape) {
      PGraphics.showWarning(INSIDE_BEGIN_END_ERROR, "setTint()");
      return;
    }

    if (family == GROUP) {
      for (int i = 0; i < childCount; i++) {
        PShapeOpenGL child = (PShapeOpenGL) children[i];
        child.setTint(fill);
      }
    } else if (this.tint && !tint) {
      setTintImpl(0xFFFFFFFF);
    }
    this.tint = tint;
  }


  @Override
  public void setTint(int tint) {
    if (openShape) {
      PGraphics.showWarning(INSIDE_BEGIN_END_ERROR, "setTint()");
      return;
    }

    if (family == GROUP) {
      for (int i = 0; i < childCount; i++) {
        PShapeOpenGL child = (PShapeOpenGL) children[i];
        child.setTint(tint);
      }
    } else {
      setTintImpl(tint);
    }
  }


  protected void setTintImpl(int tint) {
    if (tintColor == tint) return;
    tintColor = tint;

    if (image != null) {
      Arrays.fill(inGeo.colors, 0, inGeo.vertexCount,
                  PGL.javaToNativeARGB(tintColor));
      if (shapeCreated && tessellated && hasPolys) {
        if (is3D()) {
          Arrays.fill(tessGeo.polyColors, firstPolyVertex, lastPolyVertex + 1,
                      PGL.javaToNativeARGB(tintColor));
          root.setModifiedPolyColors(firstPolyVertex, lastPolyVertex);
        } else if (is2D()) {
          int last1 = lastPolyVertex + 1;
          if (-1 < firstLineVertex) last1 = firstLineVertex;
          if (-1 < firstPointVertex) last1 = firstPointVertex;
          Arrays.fill(tessGeo.polyColors, firstPolyVertex, last1,
                      PGL.javaToNativeARGB(tintColor));
          root.setModifiedPolyColors(firstPolyVertex, last1 - 1);
        }
      }
    }
  }


  @Override
  public void setTint(int index, int tint) {
    if (openShape) {
      PGraphics.showWarning(INSIDE_BEGIN_END_ERROR, "setTint()");
      return;
    }

    if (image != null) {
      inGeo.colors[index] = PGL.javaToNativeARGB(tint);
      markForTessellation();
    }
  }


  @Override
  public int getStroke(int index) {
    if (family != GROUP) {
      return PGL.nativeToJavaARGB(inGeo.strokeColors[index]);
    } else {
      return 0;
    }
  }


  @Override
  public void setStroke(boolean stroke) {
    if (openShape) {
      PGraphics.showWarning(INSIDE_BEGIN_END_ERROR, "setStroke()");
      return;
    }

    if (family == GROUP) {
      for (int i = 0; i < childCount; i++) {
        PShapeOpenGL child = (PShapeOpenGL) children[i];
        child.setStroke(stroke);
      }
    } else if (this.stroke != stroke) {
      if (this.stroke) {
        // Disabling stroke on a shape previously with
        // stroke needs a re-tesellation in order to remove
        // the additional geometry of lines and/or points.
        markForTessellation();
        stroke = false;
      }
      setStrokeImpl(0x0);
      if (is2D() && parent != null) {
        ((PShapeOpenGL)parent).strokedTexture(false);
      }
    }
    this.stroke = stroke;
  }


  @Override
  public void setStroke(int stroke) {
    if (openShape) {
      PGraphics.showWarning(INSIDE_BEGIN_END_ERROR, "setStroke()");
      return;
    }

    if (family == GROUP) {
      for (int i = 0; i < childCount; i++) {
        PShapeOpenGL child = (PShapeOpenGL) children[i];
        child.setStroke(stroke);
      }
    } else {
      setStrokeImpl(stroke);
    }
  }


  protected void setStrokeImpl(int stroke) {
    if (strokeColor == stroke) return;
    strokeColor = stroke;

    Arrays.fill(inGeo.strokeColors, 0, inGeo.vertexCount,
                PGL.javaToNativeARGB(strokeColor));
    if (shapeCreated && tessellated && (hasLines || hasPoints)) {
      if (hasLines) {
        if (is3D()) {
          Arrays.fill(tessGeo.lineColors, firstLineVertex, lastLineVertex + 1,
                      PGL.javaToNativeARGB(strokeColor));
          root.setModifiedLineColors(firstLineVertex, lastLineVertex);
        } else if (is2D()) {
          Arrays.fill(tessGeo.polyColors, firstLineVertex, lastLineVertex + 1,
                      PGL.javaToNativeARGB(strokeColor));
          root.setModifiedPolyColors(firstLineVertex, lastLineVertex);
        }
      }
      if (hasPoints) {
        if (is3D()) {
          Arrays.fill(tessGeo.pointColors, firstPointVertex, lastPointVertex + 1,
                      PGL.javaToNativeARGB(strokeColor));
          root.setModifiedPointColors(firstPointVertex, lastPointVertex);
        } else if (is2D()) {
          Arrays.fill(tessGeo.polyColors, firstPointVertex, lastPointVertex + 1,
                      PGL.javaToNativeARGB(strokeColor));
          root.setModifiedPolyColors(firstPointVertex, lastPointVertex);
        }
      }
    }
  }


  @Override
  public void setStroke(int index, int stroke) {
    if (openShape) {
      PGraphics.showWarning(INSIDE_BEGIN_END_ERROR, "setStroke()");
      return;
    }

    inGeo.strokeColors[index] = PGL.javaToNativeARGB(stroke);
    markForTessellation();
  }


  @Override
  public float getStrokeWeight(int index) {
    if (family != GROUP) {
      return inGeo.strokeWeights[index];
    } else {
      return 0;
    }
  }


  @Override
  public void setStrokeWeight(float weight) {
    if (openShape) {
      PGraphics.showWarning(INSIDE_BEGIN_END_ERROR, "setStrokeWeight()");
      return;
    }

    if (family == GROUP) {
      for (int i = 0; i < childCount; i++) {
        PShapeOpenGL child = (PShapeOpenGL) children[i];
        child.setStrokeWeight(weight);
      }
    } else {
      setStrokeWeightImpl(weight);
    }
  }


  protected void setStrokeWeightImpl(float weight) {
    if (PGraphicsOpenGL.same(strokeWeight, weight)) return;
    float oldWeight = strokeWeight;
    strokeWeight = weight;

    Arrays.fill(inGeo.strokeWeights, 0, inGeo.vertexCount, strokeWeight);
    if (shapeCreated && tessellated && (hasLines || hasPoints)) {
      float resizeFactor = weight / oldWeight;
      if (hasLines) {
        if (is3D()) {
          for (int i = firstLineVertex; i <= lastLineVertex; i++) {
            tessGeo.lineDirections[4 * i + 3] *= resizeFactor;
          }
          root.setModifiedLineAttributes(firstLineVertex, lastLineVertex);
        } else if (is2D()) {
          // Changing the stroke weight on a 2D shape needs a
          // re-tesellation in order to replace the old line
          // geometry.
          markForTessellation();
        }
      }
      if (hasPoints) {
        if (is3D()) {
          for (int i = firstPointVertex; i <= lastPointVertex; i++) {
            tessGeo.pointOffsets[2 * i + 0] *= resizeFactor;
            tessGeo.pointOffsets[2 * i + 1] *= resizeFactor;
          }
          root.setModifiedPointAttributes(firstPointVertex, lastPointVertex);
        } else if (is2D()) {
          // Changing the stroke weight on a 2D shape needs a
          // re-tesellation in order to replace the old point
          // geometry.
          markForTessellation();
        }
      }
    }
  }


  @Override
  public void setStrokeWeight(int index, float weight) {
    if (openShape) {
      PGraphics.showWarning(INSIDE_BEGIN_END_ERROR, "setStrokeWeight()");
      return;
    }

    inGeo.strokeWeights[index] = weight;
    markForTessellation();
  }


  @Override
  public void setStrokeJoin(int join) {
    if (openShape) {
      PGraphics.showWarning(INSIDE_BEGIN_END_ERROR, "setStrokeJoin()");
      return;
    }

    if (family == GROUP) {
      for (int i = 0; i < childCount; i++) {
        PShapeOpenGL child = (PShapeOpenGL) children[i];
        child.setStrokeJoin(join);
      }
    } else {
      if (is2D() && strokeJoin != join) {
        // Changing the stroke join on a 2D shape needs a
        // re-tesellation in order to replace the old join
        // geometry.
        markForTessellation();
      }
      strokeJoin = join;
    }
  }


  @Override
  public void setStrokeCap(int cap) {
    if (openShape) {
      PGraphics.showWarning(INSIDE_BEGIN_END_ERROR, "setStrokeCap()");
      return;
    }

    if (family == GROUP) {
      for (int i = 0; i < childCount; i++) {
        PShapeOpenGL child = (PShapeOpenGL) children[i];
        child.setStrokeCap(cap);
      }
    } else {
      if (is2D() && strokeCap != cap) {
        // Changing the stroke cap on a 2D shape needs a
        // re-tesellation in order to replace the old cap
        // geometry.
        markForTessellation();
      }
      strokeCap = cap;
    }
  }


  @Override
  public int getAmbient(int index) {
    if (family != GROUP) {
      return PGL.nativeToJavaARGB(inGeo.ambient[index]);
    } else {
      return 0;
    }
  }


  @Override
  public void setAmbient(int ambient) {
    if (openShape) {
      PGraphics.showWarning(INSIDE_BEGIN_END_ERROR, "setAmbient()");
      return;
    }

    if (family == GROUP) {
      for (int i = 0; i < childCount; i++) {
        PShapeOpenGL child = (PShapeOpenGL) children[i];
        child.setAmbient(ambient);
      }
    } else {
      setAmbientImpl(ambient);
    }
  }


  protected void setAmbientImpl(int ambient) {
    if (ambientColor == ambient) return;
    ambientColor = ambient;

    Arrays.fill(inGeo.ambient, 0, inGeo.vertexCount,
                PGL.javaToNativeARGB(ambientColor));
    if (shapeCreated && tessellated && hasPolys) {
      if (is3D()) {
        Arrays.fill(tessGeo.polyAmbient, firstPolyVertex, lastPolyVertex + 1,
                    PGL.javaToNativeARGB(ambientColor));
        root.setModifiedPolyAmbient(firstPolyVertex, lastPolyVertex);
      } else if (is2D()) {
        int last1 = lastPolyVertex + 1;
        if (-1 < firstLineVertex) last1 = firstLineVertex;
        if (-1 < firstPointVertex) last1 = firstPointVertex;
        Arrays.fill(tessGeo.polyAmbient, firstPolyVertex, last1,
                    PGL.javaToNativeARGB(ambientColor));
        root.setModifiedPolyColors(firstPolyVertex, last1 - 1);
      }
    }
    setAmbient = true;
  }


  @Override
  public void setAmbient(int index, int ambient) {
    if (openShape) {
      PGraphics.showWarning(INSIDE_BEGIN_END_ERROR, "setAmbient()");
      return;
    }

    inGeo.ambient[index] = PGL.javaToNativeARGB(ambient);
    markForTessellation();
    setAmbient = true;
  }


  @Override
  public int getSpecular(int index) {
    if (family == GROUP) {
      return PGL.nativeToJavaARGB(inGeo.specular[index]);
    } else {
      return 0;
    }
  }


  @Override
  public void setSpecular(int specular) {
    if (openShape) {
      PGraphics.showWarning(INSIDE_BEGIN_END_ERROR, "setSpecular()");
      return;
    }

    if (family == GROUP) {
      for (int i = 0; i < childCount; i++) {
        PShapeOpenGL child = (PShapeOpenGL) children[i];
        child.setSpecular(specular);
      }
    } else {
      setSpecularImpl(specular);
    }
  }


  protected void setSpecularImpl(int specular) {
    if (specularColor == specular) return;
    specularColor = specular;

    Arrays.fill(inGeo.specular, 0, inGeo.vertexCount,
                PGL.javaToNativeARGB(specularColor));
    if (shapeCreated && tessellated && hasPolys) {
      if (is3D()) {
        Arrays.fill(tessGeo.polySpecular, firstPolyVertex, lastPolyVertex + 1,
                    PGL.javaToNativeARGB(specularColor));
        root.setModifiedPolySpecular(firstPolyVertex, lastPolyVertex);
      } else if (is2D()) {
        int last1 = lastPolyVertex + 1;
        if (-1 < firstLineVertex) last1 = firstLineVertex;
        if (-1 < firstPointVertex) last1 = firstPointVertex;
        Arrays.fill(tessGeo.polySpecular, firstPolyVertex, last1,
                    PGL.javaToNativeARGB(specularColor));
        root.setModifiedPolyColors(firstPolyVertex, last1 - 1);
      }
    }
  }


  @Override
  public void setSpecular(int index, int specular) {
    if (openShape) {
      PGraphics.showWarning(INSIDE_BEGIN_END_ERROR, "setSpecular()");
      return;
    }

    inGeo.specular[index] = PGL.javaToNativeARGB(specular);
    markForTessellation();
  }


  @Override
  public int getEmissive(int index) {
    if (family == GROUP) {
      return PGL.nativeToJavaARGB(inGeo.emissive[index]);
    } else {
      return 0;
    }
  }


  @Override
  public void setEmissive(int emissive) {
    if (openShape) {
      PGraphics.showWarning(INSIDE_BEGIN_END_ERROR, "setEmissive()");
      return;
    }

    if (family == GROUP) {
      for (int i = 0; i < childCount; i++) {
        PShapeOpenGL child = (PShapeOpenGL) children[i];
        child.setEmissive(emissive);
      }
    } else {
      setEmissiveImpl(emissive);
    }
  }


  protected void setEmissiveImpl(int emissive) {
    if (emissiveColor == emissive) return;
    emissiveColor = emissive;

    Arrays.fill(inGeo.emissive, 0, inGeo.vertexCount,
                PGL.javaToNativeARGB(emissiveColor));
    if (shapeCreated && tessellated && 0 < tessGeo.polyVertexCount) {
      if (is3D()) {
        Arrays.fill(tessGeo.polyEmissive, firstPolyVertex, lastPolyVertex + 1,
                    PGL.javaToNativeARGB(emissiveColor));
        root.setModifiedPolyEmissive(firstPolyVertex, lastPolyVertex);
      } else if (is2D()) {
        int last1 = lastPolyVertex + 1;
        if (-1 < firstLineVertex) last1 = firstLineVertex;
        if (-1 < firstPointVertex) last1 = firstPointVertex;
        Arrays.fill(tessGeo.polyEmissive, firstPolyVertex, last1,
                    PGL.javaToNativeARGB(emissiveColor));
        root.setModifiedPolyColors(firstPolyVertex, last1 - 1);
      }
    }
  }


  @Override
  public void setEmissive(int index, int emissive) {
    if (openShape) {
      PGraphics.showWarning(INSIDE_BEGIN_END_ERROR, "setEmissive()");
      return;
    }

    inGeo.emissive[index] = PGL.javaToNativeARGB(emissive);
    markForTessellation();
  }


  @Override
  public float getShininess(int index) {
    if (family == GROUP) {
      return inGeo.shininess[index];
    } else {
      return 0;
    }
  }


  @Override
  public void setShininess(float shininess) {
    if (openShape) {
      PGraphics.showWarning(INSIDE_BEGIN_END_ERROR, "setShininess()");
      return;
    }

    if (family == GROUP) {
      for (int i = 0; i < childCount; i++) {
        PShapeOpenGL child = (PShapeOpenGL) children[i];
        child.setShininess(shininess);
      }
    } else {
      setShininessImpl(shininess);
    }
  }


  protected void setShininessImpl(float shininess) {
    if (PGraphicsOpenGL.same(this.shininess, shininess)) return;
    this.shininess = shininess;

    Arrays.fill(inGeo.shininess, 0, inGeo.vertexCount, shininess);
    if (shapeCreated && tessellated && hasPolys) {
      if (is3D()) {
        Arrays.fill(tessGeo.polyShininess, firstPolyVertex, lastPolyVertex + 1,
                    shininess);
        root.setModifiedPolyShininess(firstPolyVertex, lastPolyVertex);
      } else if (is2D()) {
        int last1 = lastPolyVertex + 1;
        if (-1 < firstLineVertex) last1 = firstLineVertex;
        if (-1 < firstPointVertex) last1 = firstPointVertex;
        Arrays.fill(tessGeo.polyShininess, firstPolyVertex, last1, shininess);
        root.setModifiedPolyColors(firstPolyVertex, last1 - 1);
      }
    }
  }


  @Override
  public void setShininess(int index, float shine) {
    if (openShape) {
      PGraphics.showWarning(INSIDE_BEGIN_END_ERROR, "setShininess()");
      return;
    }

    inGeo.shininess[index] = shine;
    markForTessellation();
  }


  ///////////////////////////////////////////////////////////

  //

  // Tessellated geometry getter.

  @Override
  public PShape getTessellation() {
    updateTessellation();

    float[] vertices = tessGeo.polyVertices;
    float[] normals = tessGeo.polyNormals;
    int[] color = tessGeo.polyColors;
    float[] uv = tessGeo.polyTexCoords;
    short[] indices = tessGeo.polyIndices;

    PShape tess;
    if (is3D()) {
      tess = PGraphics3D.createShapeImpl(pg.parent, PShape.GEOMETRY);
    } else if (is2D()) {
      tess = PGraphics2D.createShapeImpl(pg.parent, PShape.GEOMETRY);
    } else {
      PGraphics.showWarning("This shape is not either 2D or 3D!");
      return null;
    }
    tess.beginShape(TRIANGLES);
    tess.noStroke();

    IndexCache cache = tessGeo.polyIndexCache;
    for (int n = firstPolyIndexCache; n <= lastPolyIndexCache; n++) {
      int ioffset = cache.indexOffset[n];
      int icount = cache.indexCount[n];
      int voffset = cache.vertexOffset[n];

      for (int tr = ioffset / 3; tr < (ioffset + icount) / 3; tr++) {
        int i0 = voffset + indices[3 * tr + 0];
        int i1 = voffset + indices[3 * tr + 1];
        int i2 = voffset + indices[3 * tr + 2];

        if (is3D()) {
          float x0 = vertices[4 * i0 + 0];
          float y0 = vertices[4 * i0 + 1];
          float z0 = vertices[4 * i0 + 2];
          float x1 = vertices[4 * i1 + 0];
          float y1 = vertices[4 * i1 + 1];
          float z1 = vertices[4 * i1 + 2];
          float x2 = vertices[4 * i2 + 0];
          float y2 = vertices[4 * i2 + 1];
          float z2 = vertices[4 * i2 + 2];

          float nx0 = normals[3 * i0 + 0];
          float ny0 = normals[3 * i0 + 1];
          float nz0 = normals[3 * i0 + 2];
          float nx1 = normals[3 * i1 + 0];
          float ny1 = normals[3 * i1 + 1];
          float nz1 = normals[3 * i1 + 2];
          float nx2 = normals[3 * i2 + 0];
          float ny2 = normals[3 * i2 + 1];
          float nz2 = normals[3 * i2 + 2];

          int argb0 = PGL.nativeToJavaARGB(color[i0]);
          int argb1 = PGL.nativeToJavaARGB(color[i1]);
          int argb2 = PGL.nativeToJavaARGB(color[i2]);

          tess.fill(argb0);
          tess.normal(nx0, ny0, nz0);
          tess.vertex(x0, y0, z0, uv[2 * i0 + 0], uv[2 * i0 + 1]);

          tess.fill(argb1);
          tess.normal(nx1, ny1, nz1);
          tess.vertex(x1, y1, z1, uv[2 * i1 + 0], uv[2 * i1 + 1]);

          tess.fill(argb2);
          tess.normal(nx2, ny2, nz2);
          tess.vertex(x2, y2, z2, uv[2 * i2 + 0], uv[2 * i2 + 1]);
        } else if (is2D()) {
          float x0 = vertices[4 * i0 + 0], y0 = vertices[4 * i0 + 1];
          float x1 = vertices[4 * i1 + 0], y1 = vertices[4 * i1 + 1];
          float x2 = vertices[4 * i2 + 0], y2 = vertices[4 * i2 + 1];

          int argb0 = PGL.nativeToJavaARGB(color[i0]);
          int argb1 = PGL.nativeToJavaARGB(color[i1]);
          int argb2 = PGL.nativeToJavaARGB(color[i2]);

          tess.fill(argb0);
          tess.vertex(x0, y0, uv[2 * i0 + 0], uv[2 * i0 + 1]);

          tess.fill(argb1);
          tess.vertex(x1, y1, uv[2 * i1 + 0], uv[2 * i1 + 1]);

          tess.fill(argb2);
          tess.vertex(x2, y2, uv[2 * i2 + 0], uv[2 * i2 + 1]);
        }
      }
    }
    tess.endShape();

    return tess;
  }


  ///////////////////////////////////////////////////////////

  //

  // Tessellation


  protected void updateTessellation() {
    if (!root.tessellated || root.contextIsOutdated()) {
      root.tessellate();
      root.aggregate();
    }
  }


  protected void markForTessellation() {
    root.tessellated = false;
    tessellated = false;
  }

  protected void tessellate() {
    if (root == this && parent == null) {
      if (tessGeo == null) {
        tessGeo = pg.newTessGeometry(PGraphicsOpenGL.RETAINED);
      }
      tessGeo.clear();

      tessellateImpl();

      // Tessellated arrays are trimmed since they are expanded
      // by doubling their old size, which might lead to arrays
      // larger than the vertex counts.
      tessGeo.trim();

      modified = false;
      needBufferInit = true;

      modifiedPolyVertices = false;
      modifiedPolyColors = false;
      modifiedPolyNormals = false;
      modifiedPolyTexCoords = false;
      modifiedPolyAmbient = false;
      modifiedPolySpecular = false;
      modifiedPolyEmissive = false;
      modifiedPolyShininess = false;

      modifiedLineVertices = false;
      modifiedLineColors = false;
      modifiedLineAttributes = false;

      modifiedPointVertices = false;
      modifiedPointColors = false;
      modifiedPointAttributes = false;

      firstModifiedPolyVertex = PConstants.MAX_INT;
      lastModifiedPolyVertex = PConstants.MIN_INT;
      firstModifiedPolyColor = PConstants.MAX_INT;
      lastModifiedPolyColor = PConstants.MIN_INT;
      firstModifiedPolyNormal = PConstants.MAX_INT;
      lastModifiedPolyNormal = PConstants.MIN_INT;
      firstModifiedPolyTexcoord = PConstants.MAX_INT;
      lastModifiedPolyTexcoord = PConstants.MIN_INT;
      firstModifiedPolyAmbient = PConstants.MAX_INT;
      lastModifiedPolyAmbient = PConstants.MIN_INT;
      firstModifiedPolySpecular = PConstants.MAX_INT;
      lastModifiedPolySpecular = PConstants.MIN_INT;
      firstModifiedPolyEmissive = PConstants.MAX_INT;
      lastModifiedPolyEmissive = PConstants.MIN_INT;
      firstModifiedPolyShininess = PConstants.MAX_INT;
      lastModifiedPolyShininess = PConstants.MIN_INT;

      firstModifiedLineVertex = PConstants.MAX_INT;
      lastModifiedLineVertex = PConstants.MIN_INT;
      firstModifiedLineColor = PConstants.MAX_INT;
      lastModifiedLineColor = PConstants.MIN_INT;
      firstModifiedLineAttribute = PConstants.MAX_INT;
      lastModifiedLineAttribute = PConstants.MIN_INT;

      firstModifiedPointVertex = PConstants.MAX_INT;
      lastModifiedPointVertex = PConstants.MIN_INT;
      firstModifiedPointColor = PConstants.MAX_INT;
      lastModifiedPointColor = PConstants.MIN_INT;
      firstModifiedPointAttribute = PConstants.MAX_INT;
      lastModifiedPointAttribute = PConstants.MIN_INT;
    }
  }


  protected void tessellateImpl() {
    tessGeo = root.tessGeo;

    firstPolyIndexCache = -1;
    lastPolyIndexCache = -1;
    firstLineIndexCache = -1;
    lastLineIndexCache = -1;
    firstPointIndexCache = -1;
    lastPointIndexCache = -1;

    if (family == GROUP) {
      for (int i = 0; i < childCount; i++) {
        PShapeOpenGL child = (PShapeOpenGL) children[i];
        child.tessellateImpl();
      }
    } else {
      if (shapeCreated) {
        // If the geometry was tessellated previously, then
        // the edges information will still be stored in the
        // input object, so it needs to be removed to avoid
        // duplication.
        inGeo.clearEdges();

        tessellator.setInGeometry(inGeo);
        tessellator.setTessGeometry(tessGeo);
        tessellator.setFill(fill || image != null);
        tessellator.setStroke(stroke);
        tessellator.setStrokeColor(strokeColor);
        tessellator.setStrokeWeight(strokeWeight);
        tessellator.setStrokeCap(strokeCap);
        tessellator.setStrokeJoin(strokeJoin);
        tessellator.setTexCache(null, null, null);
        tessellator.setTransform(matrix);
        tessellator.set3D(is3D());

        if (family == GEOMETRY) {
          if (kind == POINTS) {
            tessellator.tessellatePoints();
          } else if (kind == LINES) {
            tessellator.tessellateLines();
          } else if (kind == LINE_STRIP) {
            tessellator.tessellateLineStrip();
          } else if (kind == LINE_LOOP) {
            tessellator.tessellateLineLoop();
          } else if (kind == TRIANGLE || kind == TRIANGLES) {
            if (stroke) inGeo.addTrianglesEdges();
            if (normalMode == NORMAL_MODE_AUTO) inGeo.calcTrianglesNormals();
            tessellator.tessellateTriangles();
          } else if (kind == TRIANGLE_FAN) {
            if (stroke) inGeo.addTriangleFanEdges();
            if (normalMode == NORMAL_MODE_AUTO) inGeo.calcTriangleFanNormals();
            tessellator.tessellateTriangleFan();
          } else if (kind == TRIANGLE_STRIP) {
            if (stroke) inGeo.addTriangleStripEdges();
            if (normalMode == NORMAL_MODE_AUTO) inGeo.calcTriangleStripNormals();
            tessellator.tessellateTriangleStrip();
          } else if (kind == QUAD || kind == QUADS) {
            if (stroke) inGeo.addQuadsEdges();
            if (normalMode == NORMAL_MODE_AUTO) inGeo.calcQuadsNormals();
            tessellator.tessellateQuads();
          } else if (kind == QUAD_STRIP) {
            if (stroke) inGeo.addQuadStripEdges();
            if (normalMode == NORMAL_MODE_AUTO) inGeo.calcQuadStripNormals();
            tessellator.tessellateQuadStrip();
          } else if (kind == POLYGON) {
            if (stroke) inGeo.addPolygonEdges(isClosed);
            tessellator.tessellatePolygon(isSolid, isClosed,
                                          normalMode == NORMAL_MODE_AUTO);
          }
        } else if (family == PRIMITIVE) {
          // The input geometry needs to be cleared because the geometry
          // generation methods in InGeometry add the vertices of the
          // new primitive to what is already stored.
          inGeo.clear();

          if (kind == POINT) {
            tessellatePoint();
          } else if (kind == LINE) {
            tessellateLine();
          } else if (kind == TRIANGLE) {
            tessellateTriangle();
          } else if (kind == QUAD) {
            tessellateQuad();
          } else if (kind == RECT) {
            tessellateRect();
          } else if (kind == ELLIPSE) {
            tessellateEllipse();
          } else if (kind == ARC) {
            tessellateArc();
          } else if (kind == BOX) {
            tessellateBox();
          } else if (kind == SPHERE) {
            tessellateSphere();
          }
        } else if (family == PATH) {
          inGeo.clear();
          tessellatePath();
        }

        if (image != null && parent != null) {
          ((PShapeOpenGL)parent).addTexture(image);
        }

        firstPolyIndexCache = tessellator.firstPolyIndexCache;
        lastPolyIndexCache = tessellator.lastPolyIndexCache;
        firstLineIndexCache = tessellator.firstLineIndexCache;
        lastLineIndexCache = tessellator.lastLineIndexCache;
        firstPointIndexCache = tessellator.firstPointIndexCache;
        lastPointIndexCache = tessellator.lastPointIndexCache;
      }
    }

    firstPolyVertex = lastPolyVertex = -1;
    firstLineVertex = lastLineVertex = -1;
    firstPointVertex = lastPointVertex = -1;

    tessellated = true;
  }


  protected void tessellatePoint() {
    float x = 0, y = 0, z = 0;
    if (params.length == 2) {
      x = params[0];
      y = params[1];
      z = 0;
    } else if (params.length == 3) {
      x = params[0];
      y = params[1];
      z = params[2];
    }

    inGeo.setMaterial(fillColor, strokeColor, strokeWeight,
                      ambientColor, specularColor, emissiveColor, shininess);
    inGeo.setNormal(normalX, normalY, normalZ);
    inGeo.addPoint(x, y, z, fill, stroke);
    tessellator.tessellatePoints();
  }


  protected void tessellateLine() {
    float x1 = 0, y1 = 0, z1 = 0;
    float x2 = 0, y2 = 0, z2 = 0;
    if (params.length == 4) {
      x1 = params[0];
      y1 = params[1];
      x2 = params[2];
      y2 = params[3];
    } else if (params.length == 6) {
      x1 = params[0];
      y1 = params[1];
      z1 = params[2];
      x2 = params[3];
      y2 = params[4];
      z2 = params[5];
    }

    inGeo.setMaterial(fillColor, strokeColor, strokeWeight,
                      ambientColor, specularColor, emissiveColor, shininess);
    inGeo.setNormal(normalX, normalY, normalZ);
    inGeo.addLine(x1, y1, z1,
                  x2, y2, z2,
                  fill, stroke);
    tessellator.tessellateLines();
  }


  protected void tessellateTriangle() {
    float x1 = 0, y1 = 0;
    float x2 = 0, y2 = 0;
    float x3 = 0, y3 = 0;
    if (params.length == 6) {
      x1 = params[0];
      y1 = params[1];
      x2 = params[2];
      y2 = params[3];
      x3 = params[4];
      y3 = params[5];
    }

    inGeo.setMaterial(fillColor, strokeColor, strokeWeight,
                      ambientColor, specularColor, emissiveColor, shininess);
    inGeo.setNormal(normalX, normalY, normalZ);
    inGeo.addTriangle(x1, y1, 0,
                      x2, y2, 0,
                      x3, y3, 0,
                      fill, stroke);
    tessellator.tessellateTriangles();
  }


  protected void tessellateQuad() {
    float x1 = 0, y1 = 0;
    float x2 = 0, y2 = 0;
    float x3 = 0, y3 = 0;
    float x4 = 0, y4 = 0;
    if (params.length == 8) {
      x1 = params[0];
      y1 = params[1];
      x2 = params[2];
      y2 = params[3];
      x3 = params[4];
      y3 = params[5];
      x4 = params[6];
      y4 = params[7];
    }

    inGeo.setMaterial(fillColor, strokeColor, strokeWeight,
                      ambientColor, specularColor, emissiveColor, shininess);
    inGeo.setNormal(normalX, normalY, normalZ);
    inGeo.addQuad(x1, y1, 0,
               x2, y2, 0,
               x3, y3, 0,
               x4, y4, 0,
               fill, stroke);
    tessellator.tessellateQuads();
  }


  protected void tessellateRect() {
    float a = 0, b = 0, c = 0, d = 0;
    float tl = 0, tr = 0, br = 0, bl = 0;
    boolean rounded = false;
    if (params.length == 4) {
      rounded = false;
      a = params[0];
      b = params[1];
      c = params[2];
      d = params[3];
    } else if (params.length == 5) {
      a = params[0];
      b = params[1];
      c = params[2];
      d = params[3];
      tl = tr = br = bl = params[4];
      rounded = true;
    } else if (params.length == 8) {
      a = params[0];
      b = params[1];
      c = params[2];
      d = params[3];
      tl = params[4];
      tr = params[5];
      br = params[6];
      bl = params[7];
      rounded = true;
    }

    rectMode = CORNER;
    inGeo.setMaterial(fillColor, strokeColor, strokeWeight,
                      ambientColor, specularColor, emissiveColor, shininess);
    inGeo.setNormal(normalX, normalY, normalZ);
    if (rounded) {
      inGeo.addRect(a, b, c, d,
                    tl, tr, br, bl,
                    fill, stroke, bezierDetail, rectMode);
      tessellator.tessellatePolygon(false, true, true);
    } else {
      inGeo.addRect(a, b, c, d,
                   fill, stroke, rectMode);
      tessellator.tessellateQuads();
    }
  }


  protected void tessellateEllipse() {
    float a = 0, b = 0, c = 0, d = 0;
    if (params.length == 4) {
      a = params[0];
      b = params[1];
      c = params[2];
      d = params[3];
    }

//    ellipseMode = CORNER;
    inGeo.setMaterial(fillColor, strokeColor, strokeWeight,
                      ambientColor, specularColor, emissiveColor, shininess);
    inGeo.setNormal(normalX, normalY, normalZ);
    inGeo.addEllipse(a, b, c, d, fill, stroke, ellipseMode);
    tessellator.tessellateTriangleFan();
  }


  protected void tessellateArc() {
    float a = 0, b = 0, c = 0, d = 0;
    float start = 0, stop = 0;
    int mode = 0;
    if (params.length == 6 || params.length == 7) {
      a = params[0];
      b = params[1];
      c = params[2];
      d = params[3];
      start = params[4];
      stop = params[5];
      if (params.length == 7) {
        mode = (int)(params[6]);
      }
    }

//    ellipseMode = CORNER;
    inGeo.setMaterial(fillColor, strokeColor, strokeWeight,
                      ambientColor, specularColor, emissiveColor, shininess);
    inGeo.setNormal(normalX, normalY, normalZ);
    inGeo.addArc(a, b, c, d, start, stop, fill, stroke, mode);
    tessellator.tessellateTriangleFan();
  }


  protected void tessellateBox() {
    float w = 0, h = 0, d = 0;
    if (params.length == 1) {
      w = h = d = params[0];
    } else if (params.length == 3) {
      w = params[0];
      h = params[1];
      d = params[2];
    }

    inGeo.setMaterial(fillColor, strokeColor, strokeWeight,
                      ambientColor, specularColor, emissiveColor, shininess);
    inGeo.addBox(w, h, d, fill, stroke);
    tessellator.tessellateQuads();
  }


  protected void tessellateSphere() {
    // Getting sphere detail from renderer. Is this correct?
    int nu = pg.sphereDetailU;
    int nv = pg.sphereDetailV;
    float r = 0;
    if (params.length == 1) {
      r = params[0];
    }

    inGeo.setMaterial(fillColor, strokeColor, strokeWeight,
                      ambientColor, specularColor, emissiveColor, shininess);
    int[] indices = inGeo.addSphere(r, nu, nv, fill, stroke);
    tessellator.tessellateTriangles(indices);
  }


  protected void tessellatePath() {
    if (vertices == null) return;

    inGeo.setMaterial(fillColor, strokeColor, strokeWeight,
                      ambientColor, specularColor, emissiveColor, shininess);

    if (vertexCodeCount == 0) {  // each point is a simple vertex
      if (vertices[0].length == 2) {  // tesellating 2D vertices
        for (int i = 0; i < vertexCount; i++) {
          inGeo.addVertex(vertices[i][X], vertices[i][Y], VERTEX);
        }
      } else {  // drawing 3D vertices
        for (int i = 0; i < vertexCount; i++) {
          inGeo.addVertex(vertices[i][X], vertices[i][Y], vertices[i][Z], VERTEX);
        }
      }
    } else {  // coded set of vertices
      int idx = 0;
      int code = BREAK;

      if (vertices[0].length == 2) {  // tessellating a 2D path

        for (int j = 0; j < vertexCodeCount; j++) {
          switch (vertexCodes[j]) {

          case VERTEX:
            inGeo.addVertex(vertices[idx][X], vertices[idx][Y], code);
            code = VERTEX;
            idx++;
            break;

          case QUAD_BEZIER_VERTEX:
            inGeo.addQuadraticVertex(vertices[idx+0][X], vertices[idx+0][Y], 0,
                                     vertices[idx+1][X], vertices[idx+1][Y], 0,
                                     fill, stroke, bezierDetail, code);
            code = VERTEX;
            idx += 2;
            break;

          case BEZIER_VERTEX:
            inGeo.addBezierVertex(vertices[idx+0][X], vertices[idx+0][Y], 0,
                                  vertices[idx+1][X], vertices[idx+1][Y], 0,
                                  vertices[idx+2][X], vertices[idx+2][Y], 0,
                                  fill, stroke, bezierDetail, code);
            code = VERTEX;
            idx += 3;
            break;

          case CURVE_VERTEX:
            inGeo.addCurveVertex(vertices[idx][X], vertices[idx][Y], 0,
                                 fill, stroke, curveDetail, code);
            code = VERTEX;
            idx++;
            break;

          case BREAK:
            code = BREAK;
          }
        }
      } else {  // tessellating a 3D path
        for (int j = 0; j < vertexCodeCount; j++) {
          switch (vertexCodes[j]) {

          case VERTEX:
            inGeo.addVertex(vertices[idx][X], vertices[idx][Y],
                            vertices[idx][Z], code);
            code = VERTEX;
            idx++;
            break;

          case QUAD_BEZIER_VERTEX:
            inGeo.addQuadraticVertex(vertices[idx+0][X],
                                     vertices[idx+0][Y],
                                     vertices[idx+0][Z],
                                     vertices[idx+1][X],
                                     vertices[idx+1][Y],
                                     vertices[idx+0][Z],
                                     fill, stroke, bezierDetail, code);
            code = VERTEX;
            idx += 2;
            break;


          case BEZIER_VERTEX:
            inGeo.addBezierVertex(vertices[idx+0][X],
                                  vertices[idx+0][Y],
                                  vertices[idx+0][Z],
                                  vertices[idx+1][X],
                                  vertices[idx+1][Y],
                                  vertices[idx+1][Z],
                                  vertices[idx+2][X],
                                  vertices[idx+2][Y],
                                  vertices[idx+2][Z],
                                  fill, stroke, bezierDetail, code);
            code = VERTEX;
            idx += 3;
            break;

          case CURVE_VERTEX:
            inGeo.addCurveVertex(vertices[idx][X],
                                 vertices[idx][Y],
                                 vertices[idx][Z],
                                 fill, stroke, curveDetail, code);
            code = VERTEX;
            idx++;
            break;

          case BREAK:
            code = BREAK;
          }
        }
      }
    }

    if (stroke) inGeo.addPolygonEdges(isClosed);
    tessellator.tessellatePolygon(false, isClosed, true);
  }


  ///////////////////////////////////////////////////////////

  //

  // Aggregation


  protected void aggregate() {
    if (root == this && parent == null) {
      // Initializing auxiliary variables in root node
      // needed for aggregation.
      polyIndexOffset = 0;
      polyVertexOffset = 0;
      polyVertexAbs = 0;
      polyVertexRel = 0;

      lineIndexOffset = 0;
      lineVertexOffset = 0;
      lineVertexAbs = 0;
      lineVertexRel = 0;

      pointIndexOffset = 0;
      pointVertexOffset = 0;
      pointVertexAbs = 0;
      pointVertexRel = 0;

      // Recursive aggregation.
      aggregateImpl();
    }
  }


  // This method is very important, as it is responsible of generating the
  // correct vertex and index offsets for each level of the shape hierarchy.
  // This is the core of the recursive algorithm that calculates the indices
  // for the vertices accumulated in a single VBO.
  // Basically, the algorithm traverses all the shapes in the hierarchy and
  // updates the index cache for each child shape holding geometry (those being
  // the leaf nodes in the hierarchy tree), and creates index caches for the
  // group shapes so that the draw() method can be called from any shape in the
  // hierarchy and the correct piece of geometry will be rendered.
  //
  // For example, in the following hierarchy:
  //
  //                     ROOT GROUP
  //                         |
  //       /-----------------0-----------------\
  //       |                                   |
  //  CHILD GROUP 0                       CHILD GROUP 1
  //       |                                   |
  //       |                   /---------------0-----------------\
  //       |                   |               |                 |
  //   GEO SHAPE 0         GEO SHAPE 0     GEO SHAPE 1       GEO SHAPE 2
  //   4 vertices          5 vertices      6 vertices        3 vertices
  //
  // calling draw() from the root group should result in all the
  // vertices (4 + 5 + 6 + 3 = 18) being rendered, while calling
  // draw() from either child groups 0 or 1 should result in the first
  // 4 vertices or the last 14 vertices being rendered, respectively.
  protected void aggregateImpl() {
    if (family == GROUP) {
      // Recursively aggregating the child shapes.
      hasPolys = false;
      hasLines = false;
      hasPoints = false;
      for (int i = 0; i < childCount; i++) {
        PShapeOpenGL child = (PShapeOpenGL) children[i];
        child.aggregateImpl();
        hasPolys |= child.hasPolys;
        hasLines |= child.hasLines;
        hasPoints |= child.hasPoints;
      }
    } else { // LEAF SHAPE (family either GEOMETRY, PATH or PRIMITIVE)
      hasPolys = -1 < firstPolyIndexCache && -1 < lastPolyIndexCache;
      hasLines = -1 < firstLineIndexCache && -1 < lastLineIndexCache;
      hasPoints = -1 < firstPointIndexCache && -1 < lastPointIndexCache;
    }

    if (hasPolys) {
      updatePolyIndexCache();
    }
    if (is3D()) {
      if (hasLines) updateLineIndexCache();
      if (hasPoints) updatePointIndexCache();
    }

    if (matrix != null) {
      // Some geometric transformations were applied on
      // this shape before tessellation, so they are applied now.
      //applyMatrixImpl(matrix);
      if (hasPolys) {
        tessGeo.applyMatrixOnPolyGeometry(matrix,
                                          firstPolyVertex, lastPolyVertex);
      }
      if (is3D()) {
        if (hasLines) {
          tessGeo.applyMatrixOnLineGeometry(matrix,
                                            firstLineVertex, lastLineVertex);
        }
        if (hasPoints) {
          tessGeo.applyMatrixOnPointGeometry(matrix,
                                             firstPointVertex, lastPointVertex);
        }
      }
    }
  }


  // Updates the index cache for the range that corresponds to this shape.
  protected void updatePolyIndexCache() {
    IndexCache cache = tessGeo.polyIndexCache;
    if (family == GROUP) {
      // Updates the index cache to include the elements corresponding to
      // a group shape, using the cache entries of the child shapes. The
      // index cache has a pyramidal structure where the base is formed
      // by the entries corresponding to the leaf (geometry) shapes, and
      // each subsequent level is determined by the higher-level group shapes
      // The index pyramid is flattened into arrays in order to use simple
      // data structures, so each shape needs to store the positions in the
      // cache that corresponds to itself.

      // The index ranges of the child shapes that share the vertex offset
      // are unified into a single range in the parent level.

      firstPolyIndexCache = lastPolyIndexCache = -1;
      int gindex = -1;

      for (int i = 0; i < childCount; i++) {
        PShapeOpenGL child = (PShapeOpenGL) children[i];

        int first = child.firstPolyIndexCache;
        int count = -1 < first ? child.lastPolyIndexCache - first + 1 : -1;
        for (int n = first; n < first + count; n++) {
          if (gindex == -1) {
            gindex = cache.addNew(n);
            firstPolyIndexCache = gindex;
          } else {
            if (cache.vertexOffset[gindex] == cache.vertexOffset[n]) {
              // When the vertex offsets are the same, this means that the
              // current index range in the group shape can be extended to
              // include the index range in the current child shape.
              // This is a result of how the indices are updated for the
              // leaf shapes.
              cache.incCounts(gindex,
                              cache.indexCount[n], cache.vertexCount[n]);
            } else {
              gindex = cache.addNew(n);
            }
          }
        }

        // Updating the first and last poly vertices for this group shape.
        if (-1 < child.firstPolyVertex) {
          if (firstPolyVertex == -1) {
            firstPolyVertex = Integer.MAX_VALUE;
          }
          firstPolyVertex = PApplet.min(firstPolyVertex, child.firstPolyVertex);
        }
        if (-1 < child.lastPolyVertex) {
          lastPolyVertex = PApplet.max(lastPolyVertex, child.lastPolyVertex);
        }
      }
      lastPolyIndexCache = gindex;
    } else {
      // The index cache is updated in order to reflect the fact that all
      // the vertices will be stored in a single VBO in the root shape.
      // This update works as follows (the methodology is the same for
      // poly, line and point): the VertexAbs variable in the root shape
      // stores the index of the last vertex up to this shape (plus one)
      // without taking into consideration the MAX_VERTEX_INDEX limit, so
      // it effectively runs over the entire range.
      // VertexRel, on the other hand, is reset every time the limit is
      // exceeded, therefore creating the start of a new index group in the
      // root shape. When this happens, the indices in the child shape need
      // to be restarted as well to reflect the new index offset.

      firstPolyVertex = lastPolyVertex =
                        cache.vertexOffset[firstPolyIndexCache];
      for (int n = firstPolyIndexCache; n <= lastPolyIndexCache; n++) {
        int ioffset = cache.indexOffset[n];
        int icount = cache.indexCount[n];
        int vcount = cache.vertexCount[n];

        if (PGL.MAX_VERTEX_INDEX1 <= root.polyVertexRel + vcount || // Too many vertices already signal the start of a new cache...
            (is2D() && startStrokedTex(n))) {                      // ... or, in 2D, the beginning of line or points.
          root.polyVertexRel = 0;
          root.polyVertexOffset = root.polyVertexAbs;
          cache.indexOffset[n] = root.polyIndexOffset;
        } else {
          tessGeo.incPolyIndices(ioffset, ioffset + icount - 1,
                                          root.polyVertexRel);
        }
        cache.vertexOffset[n] = root.polyVertexOffset;
        if (is2D()) {
          setFirstStrokeVertex(n, lastPolyVertex);
        }

        root.polyIndexOffset += icount;
        root.polyVertexAbs += vcount;
        root.polyVertexRel += vcount;
        lastPolyVertex += vcount;
      }
      lastPolyVertex--;
      if (is2D()) {
        setLastStrokeVertex(lastPolyVertex);
      }
    }
  }


  protected boolean startStrokedTex(int n) {
    return image != null && (n == firstLineIndexCache ||
                               n == firstPointIndexCache);
  }


  protected void setFirstStrokeVertex(int n, int vert) {
    if (n == firstLineIndexCache && firstLineVertex == -1) {
      firstLineVertex = lastLineVertex = vert;
    }
    if (n == firstPointIndexCache && firstPointVertex == -1) {
      firstPointVertex = lastPointVertex = vert;
    }
  }

  protected void setLastStrokeVertex(int vert) {
    if (-1 < lastLineVertex) {
      lastLineVertex = vert;
    }
    if (-1 < lastPointVertex) {
      lastPointVertex += vert;
    }
  }

  protected void updateLineIndexCache() {
    IndexCache cache = tessGeo.lineIndexCache;
    if (family == GROUP) {
      firstLineIndexCache = lastLineIndexCache = -1;
      int gindex = -1;

      for (int i = 0; i < childCount; i++) {
        PShapeOpenGL child = (PShapeOpenGL) children[i];

        int first = child.firstLineIndexCache;
        int count = -1 < first ? child.lastLineIndexCache - first + 1 : -1;
        for (int n = first; n < first + count; n++) {
          if (gindex == -1) {
            gindex = cache.addNew(n);
            firstLineIndexCache = gindex;
          } else {
            if (cache.vertexOffset[gindex] == cache.vertexOffset[n]) {
              cache.incCounts(gindex, cache.indexCount[n],
                                      cache.vertexCount[n]);
            } else {
              gindex = cache.addNew(n);
            }
          }
        }

        // Updating the first and last line vertices for this group shape.
        if (-1 < child.firstLineVertex) {
          if (firstLineVertex == -1) firstLineVertex = Integer.MAX_VALUE;
          firstLineVertex = PApplet.min(firstLineVertex, child.firstLineVertex);
        }
        if (-1 < child.lastLineVertex) {
          lastLineVertex = PApplet.max(lastLineVertex, child.lastLineVertex);
        }
      }
      lastLineIndexCache = gindex;
    } else {
      firstLineVertex = lastLineVertex =
                        cache.vertexOffset[firstLineIndexCache];
      for (int n = firstLineIndexCache; n <= lastLineIndexCache; n++) {
        int ioffset = cache.indexOffset[n];
        int icount = cache.indexCount[n];
        int vcount = cache.vertexCount[n];

        if (PGL.MAX_VERTEX_INDEX1 <= root.lineVertexRel + vcount) {
          root.lineVertexRel = 0;
          root.lineVertexOffset = root.lineVertexAbs;
          cache.indexOffset[n] = root.lineIndexOffset;
        } else {
          tessGeo.incLineIndices(ioffset, ioffset + icount - 1,
                                          root.lineVertexRel);
        }
        cache.vertexOffset[n] = root.lineVertexOffset;

        root.lineIndexOffset += icount;
        root.lineVertexAbs += vcount;
        root.lineVertexRel += vcount;
        lastLineVertex += vcount;
      }
      lastLineVertex--;
    }
  }


  protected void updatePointIndexCache() {
    IndexCache cache = tessGeo.pointIndexCache;
    if (family == GROUP) {
      firstPointIndexCache = lastPointIndexCache = -1;
      int gindex = -1;

      for (int i = 0; i < childCount; i++) {
        PShapeOpenGL child = (PShapeOpenGL) children[i];

        int first = child.firstPointIndexCache;
        int count = -1 < first ? child.lastPointIndexCache - first + 1 : -1;
        for (int n = first; n < first + count; n++) {
          if (gindex == -1) {
            gindex = cache.addNew(n);
            firstPointIndexCache = gindex;
          } else {
            if (cache.vertexOffset[gindex] == cache.vertexOffset[n]) {
              // When the vertex offsets are the same, this means that the
              // current index range in the group shape can be extended to
              // include either the index range in the current child shape.
              // This is a result of how the indices are updated for the
              // leaf shapes in aggregateImpl().
              cache.incCounts(gindex, cache.indexCount[n],
                                      cache.vertexCount[n]);
            } else {
              gindex = cache.addNew(n);
            }
          }
        }

        // Updating the first and last point vertices for this group shape.
        if (-1 < child.firstPointVertex) {
          if (firstPointVertex == -1) firstPointVertex = Integer.MAX_VALUE;
          firstPointVertex = PApplet.min(firstPointVertex,
                                         child.firstPointVertex);
        }
        if (-1 < child.lastPointVertex) {
          lastPointVertex = PApplet.max(lastPointVertex, child.lastPointVertex);
        }
      }
      lastPointIndexCache = gindex;
    } else {
      firstPointVertex = lastPointVertex =
                         cache.vertexOffset[firstPointIndexCache];
      for (int n = firstPointIndexCache; n <= lastPointIndexCache; n++) {
        int ioffset = cache.indexOffset[n];
        int icount = cache.indexCount[n];
        int vcount = cache.vertexCount[n];

        if (PGL.MAX_VERTEX_INDEX1 <= root.pointVertexRel + vcount) {
          root.pointVertexRel = 0;
          root.pointVertexOffset = root.pointVertexAbs;
          cache.indexOffset[n] = root.pointIndexOffset;
        } else {
          tessGeo.incPointIndices(ioffset, ioffset + icount - 1,
                                           root.pointVertexRel);
        }
        cache.vertexOffset[n] = root.pointVertexOffset;

        root.pointIndexOffset += icount;
        root.pointVertexAbs += vcount;
        root.pointVertexRel += vcount;
        lastPointVertex += vcount;
      }
      lastPointVertex--;
    }
  }


  ///////////////////////////////////////////////////////////

  //

  //  Buffer initialization


  protected void initBuffers() {
    if (needBufferInit) {
      context = pgl.getCurrentContext();

      if (0 < tessGeo.polyVertexCount && 0 < tessGeo.polyIndexCount) {
        initPolyBuffers();
      }

      if (0 < tessGeo.lineVertexCount && 0 < tessGeo.lineIndexCount) {
        initLineBuffers();
      }

      if (0 < tessGeo.pointVertexCount && 0 < tessGeo.pointIndexCount) {
        initPointBuffers();
      }

      needBufferInit = false;
    }
  }


  protected void initPolyBuffers() {
    int size = tessGeo.polyVertexCount;
    int sizef = size * PGL.SIZEOF_FLOAT;
    int sizei = size * PGL.SIZEOF_INT;

    tessGeo.updatePolyVerticesBuffer();
    glPolyVertex = pg.createVertexBufferObject(context);
    pgl.bindBuffer(PGL.ARRAY_BUFFER, glPolyVertex);
    pgl.bufferData(PGL.ARRAY_BUFFER, 4 * sizef,
                   tessGeo.polyVerticesBuffer,
                   PGL.STATIC_DRAW);

    tessGeo.updatePolyColorsBuffer();
    glPolyColor = pg.createVertexBufferObject(context);
    pgl.bindBuffer(PGL.ARRAY_BUFFER, glPolyColor);
    pgl.bufferData(PGL.ARRAY_BUFFER, sizei,
                   tessGeo.polyColorsBuffer, PGL.STATIC_DRAW);

    tessGeo.updatePolyNormalsBuffer();
    glPolyNormal = pg.createVertexBufferObject(context);
    pgl.bindBuffer(PGL.ARRAY_BUFFER, glPolyNormal);
    pgl.bufferData(PGL.ARRAY_BUFFER, 3 * sizef,
                   tessGeo.polyNormalsBuffer, PGL.STATIC_DRAW);

    tessGeo.updatePolyTexCoordsBuffer();
    glPolyTexcoord = pg.createVertexBufferObject(context);
    pgl.bindBuffer(PGL.ARRAY_BUFFER, glPolyTexcoord);
    pgl.bufferData(PGL.ARRAY_BUFFER, 2 * sizef,
                   tessGeo.polyTexCoordsBuffer, PGL.STATIC_DRAW);

    tessGeo.updatePolyAmbientBuffer();
    glPolyAmbient = pg.createVertexBufferObject(context);
    pgl.bindBuffer(PGL.ARRAY_BUFFER, glPolyAmbient);
    pgl.bufferData(PGL.ARRAY_BUFFER, sizei,
                   tessGeo.polyAmbientBuffer, PGL.STATIC_DRAW);

    tessGeo.updatePolySpecularBuffer();
    glPolySpecular = pg.createVertexBufferObject(context);
    pgl.bindBuffer(PGL.ARRAY_BUFFER, glPolySpecular);
    pgl.bufferData(PGL.ARRAY_BUFFER, sizei,
                   tessGeo.polySpecularBuffer, PGL.STATIC_DRAW);

    tessGeo.updatePolyEmissiveBuffer();
    glPolyEmissive = pg.createVertexBufferObject(context);
    pgl.bindBuffer(PGL.ARRAY_BUFFER, glPolyEmissive);
    pgl.bufferData(PGL.ARRAY_BUFFER, sizei,
                   tessGeo.polyEmissiveBuffer, PGL.STATIC_DRAW);

    tessGeo.updatePolyShininessBuffer();
    glPolyShininess = pg.createVertexBufferObject(context);
    pgl.bindBuffer(PGL.ARRAY_BUFFER, glPolyShininess);
    pgl.bufferData(PGL.ARRAY_BUFFER, sizef,
                   tessGeo.polyShininessBuffer, PGL.STATIC_DRAW);

    pgl.bindBuffer(PGL.ARRAY_BUFFER, 0);

    tessGeo.updatePolyIndicesBuffer();
    glPolyIndex = pg.createVertexBufferObject(context);
    pgl.bindBuffer(PGL.ELEMENT_ARRAY_BUFFER, glPolyIndex);
    pgl.bufferData(PGL.ELEMENT_ARRAY_BUFFER,
                   tessGeo.polyIndexCount * PGL.SIZEOF_INDEX,
                   tessGeo.polyIndicesBuffer, PGL.STATIC_DRAW);

    pgl.bindBuffer(PGL.ELEMENT_ARRAY_BUFFER, 0);
  }


  protected void initLineBuffers() {
    int size = tessGeo.lineVertexCount;
    int sizef = size * PGL.SIZEOF_FLOAT;
    int sizei = size * PGL.SIZEOF_INT;

    tessGeo.updateLineVerticesBuffer();
    glLineVertex = pg.createVertexBufferObject(context);
    pgl.bindBuffer(PGL.ARRAY_BUFFER, glLineVertex);
    pgl.bufferData(PGL.ARRAY_BUFFER, 4 * sizef,
                   tessGeo.lineVerticesBuffer, PGL.STATIC_DRAW);

    tessGeo.updateLineColorsBuffer();
    glLineColor = pg.createVertexBufferObject(context);
    pgl.bindBuffer(PGL.ARRAY_BUFFER, glLineColor);
    pgl.bufferData(PGL.ARRAY_BUFFER, sizei,
                   tessGeo.lineColorsBuffer, PGL.STATIC_DRAW);

    tessGeo.updateLineDirectionsBuffer();
    glLineAttrib = pg.createVertexBufferObject(context);
    pgl.bindBuffer(PGL.ARRAY_BUFFER, glLineAttrib);
    pgl.bufferData(PGL.ARRAY_BUFFER, 4 * sizef,
                   tessGeo.lineDirectionsBuffer, PGL.STATIC_DRAW);

    pgl.bindBuffer(PGL.ARRAY_BUFFER, 0);

    tessGeo.updateLineIndicesBuffer();
    glLineIndex = pg.createVertexBufferObject(context);
    pgl.bindBuffer(PGL.ELEMENT_ARRAY_BUFFER, glLineIndex);
    pgl.bufferData(PGL.ELEMENT_ARRAY_BUFFER,
                   tessGeo.lineIndexCount * PGL.SIZEOF_INDEX,
                   tessGeo.lineIndicesBuffer, PGL.STATIC_DRAW);

    pgl.bindBuffer(PGL.ELEMENT_ARRAY_BUFFER, 0);
  }


  protected void initPointBuffers() {
    int size = tessGeo.pointVertexCount;
    int sizef = size * PGL.SIZEOF_FLOAT;
    int sizei = size * PGL.SIZEOF_INT;

    tessGeo.updatePointVerticesBuffer();
    glPointVertex = pg.createVertexBufferObject(context);
    pgl.bindBuffer(PGL.ARRAY_BUFFER, glPointVertex);
    pgl.bufferData(PGL.ARRAY_BUFFER, 4 * sizef,
                   tessGeo.pointVerticesBuffer, PGL.STATIC_DRAW);

    tessGeo.updatePointColorsBuffer();
    glPointColor = pg.createVertexBufferObject(context);
    pgl.bindBuffer(PGL.ARRAY_BUFFER, glPointColor);
    pgl.bufferData(PGL.ARRAY_BUFFER, sizei,
                   tessGeo.pointColorsBuffer, PGL.STATIC_DRAW);

    tessGeo.updatePointOffsetsBuffer();
    glPointAttrib = pg.createVertexBufferObject(context);
    pgl.bindBuffer(PGL.ARRAY_BUFFER, glPointAttrib);
    pgl.bufferData(PGL.ARRAY_BUFFER, 2 * sizef,
                   tessGeo.pointOffsetsBuffer, PGL.STATIC_DRAW);

    pgl.bindBuffer(PGL.ARRAY_BUFFER, 0);

    tessGeo.updatePointIndicesBuffer();
    glPointIndex = pg.createVertexBufferObject(context);
    pgl.bindBuffer(PGL.ELEMENT_ARRAY_BUFFER, glPointIndex);
    pgl.bufferData(PGL.ELEMENT_ARRAY_BUFFER,
                   tessGeo.pointIndexCount * PGL.SIZEOF_INDEX,
                   tessGeo.pointIndicesBuffer, PGL.STATIC_DRAW);

    pgl.bindBuffer(PGL.ELEMENT_ARRAY_BUFFER, 0);
  }


  protected boolean contextIsOutdated() {
    boolean outdated = !pgl.contextIsCurrent(context);
    if (outdated) {
      // Removing the VBOs from the renderer's list so they
      // doesn't get deleted by OpenGL. The VBOs were already
      // automatically disposed when the old context was
      // destroyed.
      pg.removeVertexBufferObject(glPolyVertex, context);
      pg.removeVertexBufferObject(glPolyColor, context);
      pg.removeVertexBufferObject(glPolyNormal, context);
      pg.removeVertexBufferObject(glPolyTexcoord, context);
      pg.removeVertexBufferObject(glPolyAmbient, context);
      pg.removeVertexBufferObject(glPolySpecular, context);
      pg.removeVertexBufferObject(glPolyEmissive, context);
      pg.removeVertexBufferObject(glPolyShininess, context);
      pg.removeVertexBufferObject(glPolyIndex, context);

      pg.removeVertexBufferObject(glLineVertex, context);
      pg.removeVertexBufferObject(glLineColor, context);
      pg.removeVertexBufferObject(glLineAttrib, context);
      pg.removeVertexBufferObject(glLineIndex, context);

      pg.removeVertexBufferObject(glPointVertex, context);
      pg.removeVertexBufferObject(glPointColor, context);
      pg.removeVertexBufferObject(glPointAttrib, context);
      pg.removeVertexBufferObject(glPointIndex, context);

      // The OpenGL resources have been already deleted
      // when the context changed. We only need to zero
      // them to avoid deleting them again when the GC
      // runs the finalizers of the disposed object.
      glPolyVertex = 0;
      glPolyColor = 0;
      glPolyNormal = 0;
      glPolyTexcoord = 0;
      glPolyAmbient = 0;
      glPolySpecular = 0;
      glPolyEmissive = 0;
      glPolyShininess = 0;
      glPolyIndex = 0;

      glLineVertex = 0;
      glLineColor = 0;
      glLineAttrib = 0;
      glLineIndex = 0;

      glPointVertex = 0;
      glPointColor = 0;
      glPointAttrib = 0;
      glPointIndex = 0;
    }
    return outdated;
  }


  ///////////////////////////////////////////////////////////

  //

  // Deletion methods


  protected void release() {
    deletePolyBuffers();
    deleteLineBuffers();
    deletePointBuffers();
  }


  protected void deletePolyBuffers() {
    if (glPolyVertex != 0) {
      pg.deleteVertexBufferObject(glPolyVertex, context);
      glPolyVertex = 0;
    }

    if (glPolyColor != 0) {
      pg.deleteVertexBufferObject(glPolyColor, context);
      glPolyColor = 0;
    }

    if (glPolyNormal != 0) {
      pg.deleteVertexBufferObject(glPolyNormal, context);
      glPolyNormal = 0;
    }

    if (glPolyTexcoord != 0) {
      pg.deleteVertexBufferObject(glPolyTexcoord, context);
      glPolyTexcoord = 0;
    }

    if (glPolyAmbient != 0) {
      pg.deleteVertexBufferObject(glPolyAmbient, context);
      glPolyAmbient = 0;
    }

    if (glPolySpecular != 0) {
      pg.deleteVertexBufferObject(glPolySpecular, context);
      glPolySpecular = 0;
    }

    if (glPolyEmissive != 0) {
      pg.deleteVertexBufferObject(glPolyEmissive, context);
      glPolyEmissive = 0;
    }

    if (glPolyShininess != 0) {
      pg.deleteVertexBufferObject(glPolyShininess, context);
      glPolyShininess = 0;
    }

    if (glPolyIndex != 0) {
      pg.deleteVertexBufferObject(glPolyIndex, context);
      glPolyIndex = 0;
    }
  }


  protected void deleteLineBuffers() {
    if (glLineVertex != 0) {
      pg.deleteVertexBufferObject(glLineVertex, context);
      glLineVertex = 0;
    }

    if (glLineColor != 0) {
      pg.deleteVertexBufferObject(glLineColor, context);
      glLineColor = 0;
    }

    if (glLineAttrib != 0) {
      pg.deleteVertexBufferObject(glLineAttrib, context);
      glLineAttrib = 0;
    }

    if (glLineIndex != 0) {
      pg.deleteVertexBufferObject(glLineIndex, context);
      glLineIndex = 0;
    }
  }


  protected void deletePointBuffers() {
    if (glPointVertex != 0) {
      pg.deleteVertexBufferObject(glPointVertex, context);
      glPointVertex = 0;
    }

    if (glPointColor != 0) {
      pg.deleteVertexBufferObject(glPointColor, context);
      glPointColor = 0;
    }

    if (glPointAttrib != 0) {
      pg.deleteVertexBufferObject(glPointAttrib, context);
      glPointAttrib = 0;
    }

    if (glPointIndex != 0) {
      pg.deleteVertexBufferObject(glPointIndex, context);
      glPointIndex = 0;
    }
  }


  ///////////////////////////////////////////////////////////

  //

  //  Geometry update


  protected void updateGeometry() {
    root.initBuffers();
    if (root.modified) {
      root.updateGeometryImpl();
    }
  }


  protected void updateGeometryImpl() {
    if (modifiedPolyVertices) {
      int offset = firstModifiedPolyVertex;
      int size = lastModifiedPolyVertex - offset + 1;
      copyPolyVertices(offset, size);
      modifiedPolyVertices = false;
      firstModifiedPolyVertex = PConstants.MAX_INT;
      lastModifiedPolyVertex = PConstants.MIN_INT;
    }
    if (modifiedPolyColors) {
      int offset = firstModifiedPolyColor;
      int size = lastModifiedPolyColor - offset + 1;
      copyPolyColors(offset, size);
      modifiedPolyColors = false;
      firstModifiedPolyColor = PConstants.MAX_INT;
      lastModifiedPolyColor = PConstants.MIN_INT;
    }
    if (modifiedPolyNormals) {
      int offset = firstModifiedPolyNormal;
      int size = lastModifiedPolyNormal - offset + 1;
      copyPolyNormals(offset, size);
      modifiedPolyNormals = false;
      firstModifiedPolyNormal = PConstants.MAX_INT;
      lastModifiedPolyNormal = PConstants.MIN_INT;
    }
    if (modifiedPolyTexCoords) {
      int offset = firstModifiedPolyTexcoord;
      int size = lastModifiedPolyTexcoord - offset + 1;
      copyPolyTexCoords(offset, size);
      modifiedPolyTexCoords = false;
      firstModifiedPolyTexcoord = PConstants.MAX_INT;
      lastModifiedPolyTexcoord = PConstants.MIN_INT;
    }
    if (modifiedPolyAmbient) {
      int offset = firstModifiedPolyAmbient;
      int size = lastModifiedPolyAmbient - offset + 1;
      copyPolyAmbient(offset, size);
      modifiedPolyAmbient = false;
      firstModifiedPolyAmbient = PConstants.MAX_INT;
      lastModifiedPolyAmbient = PConstants.MIN_INT;
    }
    if (modifiedPolySpecular) {
      int offset = firstModifiedPolySpecular;
      int size = lastModifiedPolySpecular - offset + 1;
      copyPolySpecular(offset, size);
      modifiedPolySpecular = false;
      firstModifiedPolySpecular = PConstants.MAX_INT;
      lastModifiedPolySpecular = PConstants.MIN_INT;
    }
    if (modifiedPolyEmissive) {
      int offset = firstModifiedPolyEmissive;
      int size = lastModifiedPolyEmissive - offset + 1;
      copyPolyEmissive(offset, size);
      modifiedPolyEmissive = false;
      firstModifiedPolyEmissive = PConstants.MAX_INT;
      lastModifiedPolyEmissive = PConstants.MIN_INT;
    }
    if (modifiedPolyShininess) {
      int offset = firstModifiedPolyShininess;
      int size = lastModifiedPolyShininess - offset + 1;
      copyPolyShininess(offset, size);
      modifiedPolyShininess = false;
      firstModifiedPolyShininess = PConstants.MAX_INT;
      lastModifiedPolyShininess = PConstants.MIN_INT;
    }

    if (modifiedLineVertices) {
      int offset = firstModifiedLineVertex;
      int size = lastModifiedLineVertex - offset + 1;
      copyLineVertices(offset, size);
      modifiedLineVertices = false;
      firstModifiedLineVertex = PConstants.MAX_INT;
      lastModifiedLineVertex = PConstants.MIN_INT;
    }
    if (modifiedLineColors) {
      int offset = firstModifiedLineColor;
      int size = lastModifiedLineColor - offset + 1;
      copyLineColors(offset, size);
      modifiedLineColors = false;
      firstModifiedLineColor = PConstants.MAX_INT;
      lastModifiedLineColor = PConstants.MIN_INT;
    }
    if (modifiedLineAttributes) {
      int offset = firstModifiedLineAttribute;
      int size = lastModifiedLineAttribute - offset + 1;
      copyLineAttributes(offset, size);
      modifiedLineAttributes = false;
      firstModifiedLineAttribute = PConstants.MAX_INT;
      lastModifiedLineAttribute = PConstants.MIN_INT;
    }

    if (modifiedPointVertices) {
      int offset = firstModifiedPointVertex;
      int size = lastModifiedPointVertex - offset + 1;
      copyPointVertices(offset, size);
      modifiedPointVertices = false;
      firstModifiedPointVertex = PConstants.MAX_INT;
      lastModifiedPointVertex = PConstants.MIN_INT;
    }
    if (modifiedPointColors) {
      int offset = firstModifiedPointColor;
      int size = lastModifiedPointColor - offset + 1;
      copyPointColors(offset, size);
      modifiedPointColors = false;
      firstModifiedPointColor = PConstants.MAX_INT;
      lastModifiedPointColor = PConstants.MIN_INT;
    }
    if (modifiedPointAttributes) {
      int offset = firstModifiedPointAttribute;
      int size = lastModifiedPointAttribute - offset + 1;
      copyPointAttributes(offset, size);
      modifiedPointAttributes = false;
      firstModifiedPointAttribute = PConstants.MAX_INT;
      lastModifiedPointAttribute = PConstants.MIN_INT;
    }

    modified = false;
  }


  protected void copyPolyVertices(int offset, int size) {
    tessGeo.updatePolyVerticesBuffer(offset, size);
    pgl.bindBuffer(PGL.ARRAY_BUFFER, glPolyVertex);
    tessGeo.polyVerticesBuffer.position(4 * offset);
    pgl.bufferSubData(PGL.ARRAY_BUFFER, 4 * offset * PGL.SIZEOF_FLOAT,
                      4 * size * PGL.SIZEOF_FLOAT, tessGeo.polyVerticesBuffer);
    tessGeo.polyVerticesBuffer.rewind();
    pgl.bindBuffer(PGL.ARRAY_BUFFER, 0);
  }


  protected void copyPolyColors(int offset, int size) {
    tessGeo.updatePolyColorsBuffer(offset, size);
    pgl.bindBuffer(PGL.ARRAY_BUFFER, glPolyColor);
    tessGeo.polyColorsBuffer.position(offset);
    pgl.bufferSubData(PGL.ARRAY_BUFFER, offset * PGL.SIZEOF_INT,
                      size * PGL.SIZEOF_INT, tessGeo.polyColorsBuffer);
    tessGeo.polyColorsBuffer.rewind();
    pgl.bindBuffer(PGL.ARRAY_BUFFER, 0);
  }


  protected void copyPolyNormals(int offset, int size) {
    tessGeo.updatePolyNormalsBuffer(offset, size);
    pgl.bindBuffer(PGL.ARRAY_BUFFER, glPolyNormal);
    tessGeo.polyNormalsBuffer.position(3 * offset);
    pgl.bufferSubData(PGL.ARRAY_BUFFER, 3 * offset * PGL.SIZEOF_FLOAT,
                      3 * size * PGL.SIZEOF_FLOAT, tessGeo.polyNormalsBuffer);
    tessGeo.polyNormalsBuffer.rewind();
    pgl.bindBuffer(PGL.ARRAY_BUFFER, 0);
  }


  protected void copyPolyTexCoords(int offset, int size) {
    tessGeo.updatePolyTexCoordsBuffer(offset, size);
    pgl.bindBuffer(PGL.ARRAY_BUFFER, glPolyTexcoord);
    tessGeo.polyTexCoordsBuffer.position(2 * offset);
    pgl.bufferSubData(PGL.ARRAY_BUFFER, 2 * offset * PGL.SIZEOF_FLOAT,
                      2 * size * PGL.SIZEOF_FLOAT, tessGeo.polyTexCoordsBuffer);
    tessGeo.polyTexCoordsBuffer.rewind();
    pgl.bindBuffer(PGL.ARRAY_BUFFER, 0);
  }


  protected void copyPolyAmbient(int offset, int size) {
    tessGeo.updatePolyAmbientBuffer(offset, size);
    pgl.bindBuffer(PGL.ARRAY_BUFFER, glPolyAmbient);
    tessGeo.polyAmbientBuffer.position(offset);
    pgl.bufferSubData(PGL.ARRAY_BUFFER, offset * PGL.SIZEOF_INT,
                      size * PGL.SIZEOF_INT, tessGeo.polyAmbientBuffer);
    tessGeo.polyAmbientBuffer.rewind();
    pgl.bindBuffer(PGL.ARRAY_BUFFER, 0);
  }


  protected void copyPolySpecular(int offset, int size) {
    tessGeo.updatePolySpecularBuffer(offset, size);
    pgl.bindBuffer(PGL.ARRAY_BUFFER, glPolySpecular);
    tessGeo.polySpecularBuffer.position(offset);
    pgl.bufferSubData(PGL.ARRAY_BUFFER, offset * PGL.SIZEOF_INT,
                      size * PGL.SIZEOF_INT, tessGeo.polySpecularBuffer);
    tessGeo.polySpecularBuffer.rewind();
    pgl.bindBuffer(PGL.ARRAY_BUFFER, 0);
  }


  protected void copyPolyEmissive(int offset, int size) {
    tessGeo.updatePolyEmissiveBuffer(offset, size);
    pgl.bindBuffer(PGL.ARRAY_BUFFER, glPolyEmissive);
    tessGeo.polyEmissiveBuffer.position(offset);
    pgl.bufferSubData(PGL.ARRAY_BUFFER, offset * PGL.SIZEOF_INT,
                      size * PGL.SIZEOF_INT, tessGeo.polyEmissiveBuffer);
    tessGeo.polyEmissiveBuffer.rewind();
    pgl.bindBuffer(PGL.ARRAY_BUFFER, 0);
  }


  protected void copyPolyShininess(int offset, int size) {
    tessGeo.updatePolyShininessBuffer(offset, size);
    pgl.bindBuffer(PGL.ARRAY_BUFFER, glPolyShininess);
    tessGeo.polyShininessBuffer.position(offset);
    pgl.bufferSubData(PGL.ARRAY_BUFFER, offset * PGL.SIZEOF_FLOAT,
                      size * PGL.SIZEOF_FLOAT, tessGeo.polyShininessBuffer);
    tessGeo.polyShininessBuffer.rewind();
    pgl.bindBuffer(PGL.ARRAY_BUFFER, 0);
  }


  protected void copyLineVertices(int offset, int size) {
    tessGeo.updateLineVerticesBuffer(offset, size);
    pgl.bindBuffer(PGL.ARRAY_BUFFER, glLineVertex);
    tessGeo.lineVerticesBuffer.position(4 * offset);
    pgl.bufferSubData(PGL.ARRAY_BUFFER, 4 * offset * PGL.SIZEOF_FLOAT,
                      4 * size * PGL.SIZEOF_FLOAT, tessGeo.lineVerticesBuffer);
    tessGeo.lineVerticesBuffer.rewind();
    pgl.bindBuffer(PGL.ARRAY_BUFFER, 0);
  }


  protected void copyLineColors(int offset, int size) {
    tessGeo.updateLineColorsBuffer(offset, size);
    pgl.bindBuffer(PGL.ARRAY_BUFFER, glLineColor);
    tessGeo.lineColorsBuffer.position(offset);
    pgl.bufferSubData(PGL.ARRAY_BUFFER, offset * PGL.SIZEOF_INT,
                      size * PGL.SIZEOF_INT, tessGeo.lineColorsBuffer);
    tessGeo.lineColorsBuffer.rewind();
    pgl.bindBuffer(PGL.ARRAY_BUFFER, 0);
  }


  protected void copyLineAttributes(int offset, int size) {
    tessGeo.updateLineDirectionsBuffer(offset, size);
    pgl.bindBuffer(PGL.ARRAY_BUFFER, glLineAttrib);
    tessGeo.lineDirectionsBuffer.position(4 * offset);
    pgl.bufferSubData(PGL.ARRAY_BUFFER, 4 * offset * PGL.SIZEOF_FLOAT,
                      4 * size * PGL.SIZEOF_FLOAT, tessGeo.lineDirectionsBuffer);
    tessGeo.lineDirectionsBuffer.rewind();
    pgl.bindBuffer(PGL.ARRAY_BUFFER, 0);
  }


  protected void copyPointVertices(int offset, int size) {
    tessGeo.updatePointVerticesBuffer(offset, size);
    pgl.bindBuffer(PGL.ARRAY_BUFFER, glPointVertex);
    tessGeo.pointVerticesBuffer.position(4 * offset);
    pgl.bufferSubData(PGL.ARRAY_BUFFER, 4 * offset * PGL.SIZEOF_FLOAT,
                      4 * size * PGL.SIZEOF_FLOAT, tessGeo.pointVerticesBuffer);
    tessGeo.pointVerticesBuffer.rewind();
    pgl.bindBuffer(PGL.ARRAY_BUFFER, 0);
  }


  protected void copyPointColors(int offset, int size) {
    tessGeo.updatePointColorsBuffer(offset, size);
    pgl.bindBuffer(PGL.ARRAY_BUFFER, glPointColor);
    tessGeo.pointColorsBuffer.position(offset);
    pgl.bufferSubData(PGL.ARRAY_BUFFER, offset * PGL.SIZEOF_INT,
                      size * PGL.SIZEOF_INT,tessGeo.pointColorsBuffer);
    tessGeo.pointColorsBuffer.rewind();
    pgl.bindBuffer(PGL.ARRAY_BUFFER, 0);
  }


  protected void copyPointAttributes(int offset, int size) {
    tessGeo.updatePointOffsetsBuffer(offset, size);
    pgl.bindBuffer(PGL.ARRAY_BUFFER, glPointAttrib);
    tessGeo.pointOffsetsBuffer.position(2 * offset);
    pgl.bufferSubData(PGL.ARRAY_BUFFER, 2 * offset * PGL.SIZEOF_FLOAT,
                      2 * size * PGL.SIZEOF_FLOAT, tessGeo.pointOffsetsBuffer);
    tessGeo.pointOffsetsBuffer.rewind();
    pgl.bindBuffer(PGL.ARRAY_BUFFER, 0);
  }


  protected void setModifiedPolyVertices(int first, int last) {
    if (first < firstModifiedPolyVertex) firstModifiedPolyVertex = first;
    if (last > lastModifiedPolyVertex) lastModifiedPolyVertex = last;
    modifiedPolyVertices = true;
    modified = true;
  }


  protected void setModifiedPolyColors(int first, int last) {
    if (first < firstModifiedPolyColor) firstModifiedPolyColor = first;
    if (last > lastModifiedPolyColor) lastModifiedPolyColor = last;
    modifiedPolyColors = true;
    modified = true;
  }


  protected void setModifiedPolyNormals(int first, int last) {
    if (first < firstModifiedPolyNormal) firstModifiedPolyNormal = first;
    if (last > lastModifiedPolyNormal) lastModifiedPolyNormal = last;
    modifiedPolyNormals = true;
    modified = true;
  }


  protected void setModifiedPolyTexCoords(int first, int last) {
    if (first < firstModifiedPolyTexcoord) firstModifiedPolyTexcoord = first;
    if (last > lastModifiedPolyTexcoord) lastModifiedPolyTexcoord = last;
    modifiedPolyTexCoords = true;
    modified = true;
  }


  protected void setModifiedPolyAmbient(int first, int last) {
    if (first < firstModifiedPolyAmbient) firstModifiedPolyAmbient = first;
    if (last > lastModifiedPolyAmbient) lastModifiedPolyAmbient = last;
    modifiedPolyAmbient = true;
    modified = true;
  }


  protected void setModifiedPolySpecular(int first, int last) {
    if (first < firstModifiedPolySpecular) firstModifiedPolySpecular = first;
    if (last > lastModifiedPolySpecular) lastModifiedPolySpecular = last;
    modifiedPolySpecular = true;
    modified = true;
  }


  protected void setModifiedPolyEmissive(int first, int last) {
    if (first < firstModifiedPolyEmissive) firstModifiedPolyEmissive = first;
    if (last > lastModifiedPolyEmissive) lastModifiedPolyEmissive = last;
    modifiedPolyEmissive = true;
    modified = true;
  }


  protected void setModifiedPolyShininess(int first, int last) {
    if (first < firstModifiedPolyShininess) firstModifiedPolyShininess = first;
    if (last > lastModifiedPolyShininess) lastModifiedPolyShininess = last;
    modifiedPolyShininess = true;
    modified = true;
  }


  protected void setModifiedLineVertices(int first, int last) {
    if (first < firstModifiedLineVertex) firstModifiedLineVertex = first;
    if (last > lastModifiedLineVertex) lastModifiedLineVertex = last;
    modifiedLineVertices = true;
    modified = true;
  }


  protected void setModifiedLineColors(int first, int last) {
    if (first < firstModifiedLineColor) firstModifiedLineColor = first;
    if (last > lastModifiedLineColor) lastModifiedLineColor = last;
    modifiedLineColors = true;
    modified = true;
  }


  protected void setModifiedLineAttributes(int first, int last) {
    if (first < firstModifiedLineAttribute) firstModifiedLineAttribute = first;
    if (last > lastModifiedLineAttribute) lastModifiedLineAttribute = last;
    modifiedLineAttributes = true;
    modified = true;
  }


  protected void setModifiedPointVertices(int first, int last) {
    if (first < firstModifiedPointVertex) firstModifiedPointVertex = first;
    if (last > lastModifiedPointVertex) lastModifiedPointVertex = last;
    modifiedPointVertices = true;
    modified = true;
  }


  protected void setModifiedPointColors(int first, int last) {
    if (first < firstModifiedPointColor) firstModifiedPointColor = first;
    if (last > lastModifiedPointColor) lastModifiedPointColor = last;
    modifiedPointColors = true;
    modified = true;
  }


  protected void setModifiedPointAttributes(int first, int last) {
    if (first < firstModifiedPointAttribute) firstModifiedPointAttribute = first;
    if (last > lastModifiedPointAttribute) lastModifiedPointAttribute = last;
    modifiedPointAttributes = true;
    modified = true;
  }


  ///////////////////////////////////////////////////////////

  //

  // Style handling


  // Applies the styles of g.
  @Override
  protected void styles(PGraphics g) {
    if (g instanceof PGraphicsOpenGL) {
      if (stroke) {
        stroke(g.strokeColor);
        strokeWeight(g.strokeWeight);

        // These two don't to nothing probably:
        strokeCap(g.strokeCap);
        strokeJoin(g.strokeJoin);
      } else {
        noStroke();
      }

      if (fill) {
        fill(g.fillColor);
      } else {
        noFill();
      }

      ambient(g.ambientColor);
      specular(g.specularColor);
      emissive(g.emissiveColor);
      shininess(g.shininess);

      // What about other style parameters, such as rectMode, etc?
      // These should force a tessellation update, same as stroke
      // cap and weight... right?
    } else {
      super.styles(g);
    }
  }


  ///////////////////////////////////////////////////////////

  //

  // Rendering methods


  public void draw() {
    draw(pg);
  }


  @Override
  public void draw(PGraphics g) {
    if (g instanceof PGraphicsOpenGL) {
      PGraphicsOpenGL gl = (PGraphicsOpenGL)g;
      if (visible) {
        pre(gl);

        updateTessellation();
        updateGeometry();

        if (family == GROUP) {
          if (fragmentedGroup(gl)) {
            for (int i = 0; i < childCount; i++) {
              ((PShapeOpenGL) children[i]).draw(gl);
            }
          } else {
            PImage tex = null;
            if (textures != null && textures.size() == 1) {
              tex = (PImage)textures.toArray()[0];
            }
            render(gl, tex);
          }

        } else {
          render(gl, image);
        }

        post(gl);
      }
    } else {
      // The renderer is not PGraphicsOpenGL, which probably
      // means that the draw() method is being called by the
      // recorder. We just use the default drawing from the
      // parent class.
      super.draw(g);
    }
  }


  // Returns true if some child shapes below this one either
  // use different texture maps or have stroked textures,
  // so they cannot rendered in a single call.
  // Or accurate 2D mode is enabled, which forces each
  // shape to be rendered separately.
  protected boolean fragmentedGroup(PGraphicsOpenGL g) {
    return g.getHint(DISABLE_OPTIMIZED_STROKE) ||
           (textures != null && 1 < textures.size()) ||
           strokedTexture;
  }


  @Override
  protected void pre(PGraphics g) {
    if (g instanceof PGraphicsOpenGL) {
      if (!style) {
        styles(g);
      }
    } else {
      super.pre(g);
    }
  }


  @Override
  protected void post(PGraphics g) {
    if (g instanceof PGraphicsOpenGL) {
    } else {
      super.post(g);
    }
  }


  @Override
  protected void drawGeometry(PGraphics g) {
    vertexCount = inGeo.vertexCount;
    vertices = inGeo.getVertexData();

    super.drawGeometry(g);

    vertexCount = 0;
    vertices = null;
  }


  // Render the geometry stored in the root shape as VBOs, for the vertices
  // corresponding to this shape. Sometimes we can have root == this.
  protected void render(PGraphicsOpenGL g, PImage texture) {
    if (root == null) {
      // Some error. Root should never be null. At least it should be 'this'.
      throw new RuntimeException("Error rendering PShapeOpenGL, root shape is " +
                                 "null");
    }

    if (hasPolys) {
      renderPolys(g, texture);
      if (g.haveRaw()) {
        rawPolys(g, texture);
      }
    }

    if (is3D()) {
      // In 3D mode, the lines and points need to be rendered separately
      // as they require their own shaders.
      if (hasLines) {
        renderLines(g);
        if (g.haveRaw()) {
          rawLines(g);
        }
      }

      if (hasPoints) {
        renderPoints(g);
        if (g.haveRaw()) {
          rawPoints(g);
        }
      }
    }
  }


  protected void renderPolys(PGraphicsOpenGL g, PImage textureImage) {
    Texture tex = null;
    if (textureImage != null) {
      tex = g.getTexture(textureImage);
      if (tex != null) {
        tex.bind();
      }
    }

    boolean renderingFill = false, renderingStroke = false;
    BaseShader shader = null;
    IndexCache cache = tessGeo.polyIndexCache;
    for (int n = firstPolyIndexCache; n <= lastPolyIndexCache; n++) {
      if (is3D() || (tex != null && (firstLineIndexCache == -1 ||
                                     n < firstLineIndexCache) &&
                                    (firstPointIndexCache == -1 ||
                                     n < firstPointIndexCache))) {
        // Rendering fill triangles, which can be lit and textured.
        if (!renderingFill) {
          shader = g.getPolyShader(g.lights, tex != null);
          shader.bind();
          renderingFill = true;
        }
      } else {
        // Rendering line or point triangles, which are never lit nor textured.
        if (!renderingStroke) {
          if (tex != null) {
            tex.unbind();
            tex = null;
          }

          if (shader != null && shader.bound()) {
            shader.unbind();
          }

          // If the renderer is 2D, then g.lights should always be false,
          // so no need to worry about that.
          shader = g.getPolyShader(g.lights, false);
          shader.bind();

          renderingFill = false;
          renderingStroke = true;
        }
      }

      int ioffset = cache.indexOffset[n];
      int icount = cache.indexCount[n];
      int voffset = cache.vertexOffset[n];

      shader.setVertexAttribute(root.glPolyVertex, 4, PGL.FLOAT,
                                0, 4 * voffset * PGL.SIZEOF_FLOAT);
      shader.setColorAttribute(root.glPolyColor, 4, PGL.UNSIGNED_BYTE,
                               0, 4 * voffset * PGL.SIZEOF_BYTE);

      if (g.lights) {
        shader.setNormalAttribute(root.glPolyNormal, 3, PGL.FLOAT,
                                  0, 3 * voffset * PGL.SIZEOF_FLOAT);
        shader.setAmbientAttribute(root.glPolyAmbient, 4, PGL.UNSIGNED_BYTE,
                                   0, 4 * voffset * PGL.SIZEOF_BYTE);
        shader.setSpecularAttribute(root.glPolySpecular, 4, PGL.UNSIGNED_BYTE,
                                    0, 4 * voffset * PGL.SIZEOF_BYTE);
        shader.setEmissiveAttribute(root.glPolyEmissive, 4, PGL.UNSIGNED_BYTE,
                                    0, 4 * voffset * PGL.SIZEOF_BYTE);
        shader.setShininessAttribute(root.glPolyShininess, 1, PGL.FLOAT,
                                     0, voffset * PGL.SIZEOF_FLOAT);
      }

      if (tex != null) {
        shader.setTexcoordAttribute(root.glPolyTexcoord, 2, PGL.FLOAT,
                                    0, 2 * voffset * PGL.SIZEOF_FLOAT);
        shader.setTexture(tex);
      }

      pgl.bindBuffer(PGL.ELEMENT_ARRAY_BUFFER, root.glPolyIndex);
      pgl.drawElements(PGL.TRIANGLES, icount, PGL.INDEX_TYPE,
                       ioffset * PGL.SIZEOF_INDEX);
      pgl.bindBuffer(PGL.ELEMENT_ARRAY_BUFFER, 0);
    }

    if (shader != null && shader.bound()) {
      shader.unbind();
    }

    if (tex != null) {
      tex.unbind();
    }
  }


  protected void rawPolys(PGraphicsOpenGL g, PImage textureImage) {
    PGraphics raw = g.getRaw();

    raw.colorMode(RGB);
    raw.noStroke();
    raw.beginShape(TRIANGLES);

    float[] vertices = tessGeo.polyVertices;
    int[] color = tessGeo.polyColors;
    float[] uv = tessGeo.polyTexCoords;
    short[] indices = tessGeo.polyIndices;

    IndexCache cache = tessGeo.polyIndexCache;
    for (int n = firstPolyIndexCache; n <= lastPolyIndexCache; n++) {
      int ioffset = cache.indexOffset[n];
      int icount = cache.indexCount[n];
      int voffset = cache.vertexOffset[n];

      for (int tr = ioffset / 3; tr < (ioffset + icount) / 3; tr++) {
        int i0 = voffset + indices[3 * tr + 0];
        int i1 = voffset + indices[3 * tr + 1];
        int i2 = voffset + indices[3 * tr + 2];

        float[] src0 = {0, 0, 0, 0};
        float[] src1 = {0, 0, 0, 0};
        float[] src2 = {0, 0, 0, 0};
        float[] pt0 = {0, 0, 0, 0};
        float[] pt1 = {0, 0, 0, 0};
        float[] pt2 = {0, 0, 0, 0};
        int argb0 = PGL.nativeToJavaARGB(color[i0]);
        int argb1 = PGL.nativeToJavaARGB(color[i1]);
        int argb2 = PGL.nativeToJavaARGB(color[i2]);

        PApplet.arrayCopy(vertices, 4 * i0, src0, 0, 4);
        PApplet.arrayCopy(vertices, 4 * i1, src1, 0, 4);
        PApplet.arrayCopy(vertices, 4 * i2, src2, 0, 4);
        // Applying any transformation is currently stored in the
        // modelview matrix of the renderer.
        g.modelview.mult(src0, pt0);
        g.modelview.mult(src1, pt1);
        g.modelview.mult(src2, pt2);

        if (textureImage != null) {
          raw.texture(textureImage);
          if (raw.is3D()) {
            raw.fill(argb0);
            raw.vertex(pt0[X], pt0[Y], pt0[Z], uv[2 * i0 + 0], uv[2 * i0 + 1]);
            raw.fill(argb1);
            raw.vertex(pt1[X], pt1[Y], pt1[Z], uv[2 * i1 + 0], uv[2 * i1 + 1]);
            raw.fill(argb2);
            raw.vertex(pt2[X], pt2[Y], pt2[Z], uv[2 * i2 + 0], uv[2 * i2 + 1]);
          } else if (raw.is2D()) {
            float sx0 = g.screenXImpl(pt0[0], pt0[1], pt0[2], pt0[3]);
            float sy0 = g.screenYImpl(pt0[0], pt0[1], pt0[2], pt0[3]);
            float sx1 = g.screenXImpl(pt1[0], pt1[1], pt1[2], pt1[3]);
            float sy1 = g.screenYImpl(pt1[0], pt1[1], pt1[2], pt1[3]);
            float sx2 = g.screenXImpl(pt2[0], pt2[1], pt2[2], pt2[3]);
            float sy2 = g.screenYImpl(pt2[0], pt2[1], pt2[2], pt2[3]);
            raw.fill(argb0);
            raw.vertex(sx0, sy0, uv[2 * i0 + 0], uv[2 * i0 + 1]);
            raw.fill(argb1);
            raw.vertex(sx1, sy1, uv[2 * i1 + 0], uv[2 * i1 + 1]);
            raw.fill(argb1);
            raw.vertex(sx2, sy2, uv[2 * i2 + 0], uv[2 * i2 + 1]);
          }
        } else {
          if (raw.is3D()) {
            raw.fill(argb0);
            raw.vertex(pt0[X], pt0[Y], pt0[Z]);
            raw.fill(argb1);
            raw.vertex(pt1[X], pt1[Y], pt1[Z]);
            raw.fill(argb2);
            raw.vertex(pt2[X], pt2[Y], pt2[Z]);
          } else if (raw.is2D()) {
            float sx0 = g.screenXImpl(pt0[0], pt0[1], pt0[2], pt0[3]);
            float sy0 = g.screenYImpl(pt0[0], pt0[1], pt0[2], pt0[3]);
            float sx1 = g.screenXImpl(pt1[0], pt1[1], pt1[2], pt1[3]);
            float sy1 = g.screenYImpl(pt1[0], pt1[1], pt1[2], pt1[3]);
            float sx2 = g.screenXImpl(pt2[0], pt2[1], pt2[2], pt2[3]);
            float sy2 = g.screenYImpl(pt2[0], pt2[1], pt2[2], pt2[3]);
            raw.fill(argb0);
            raw.vertex(sx0, sy0);
            raw.fill(argb1);
            raw.vertex(sx1, sy1);
            raw.fill(argb2);
            raw.vertex(sx2, sy2);
          }
        }
      }
    }

    raw.endShape();
  }


  protected void renderLines(PGraphicsOpenGL g) {
    LineShader shader = g.getLineShader();
    shader.bind();

    IndexCache cache = tessGeo.lineIndexCache;
    for (int n = firstLineIndexCache; n <= lastLineIndexCache; n++) {
      int ioffset = cache.indexOffset[n];
      int icount = cache.indexCount[n];
      int voffset = cache.vertexOffset[n];

      shader.setVertexAttribute(root.glLineVertex, 4, PGL.FLOAT,
                                0, 4 * voffset * PGL.SIZEOF_FLOAT);
      shader.setColorAttribute(root.glLineColor, 4, PGL.UNSIGNED_BYTE,
                               0, 4 * voffset * PGL.SIZEOF_BYTE);
      shader.setLineAttribute(root.glLineAttrib, 4, PGL.FLOAT,
                              0, 4 * voffset * PGL.SIZEOF_FLOAT);

      pgl.bindBuffer(PGL.ELEMENT_ARRAY_BUFFER, root.glLineIndex);
      pgl.drawElements(PGL.TRIANGLES, icount, PGL.INDEX_TYPE,
                       ioffset * PGL.SIZEOF_INDEX);
      pgl.bindBuffer(PGL.ELEMENT_ARRAY_BUFFER, 0);
    }

    shader.unbind();
  }


  protected void rawLines(PGraphicsOpenGL g) {
    PGraphics raw = g.getRaw();

    raw.colorMode(RGB);
    raw.noFill();
    raw.strokeCap(strokeCap);
    raw.strokeJoin(strokeJoin);
    raw.beginShape(LINES);

    float[] vertices = tessGeo.lineVertices;
    int[] color = tessGeo.lineColors;
    float[] attribs = tessGeo.lineDirections;
    short[] indices = tessGeo.lineIndices;

    IndexCache cache = tessGeo.lineIndexCache;
    for (int n = firstLineIndexCache; n <= lastLineIndexCache; n++) {
      int ioffset = cache.indexOffset[n];
      int icount = cache.indexCount[n];
      int voffset = cache.vertexOffset[n];

      for (int ln = ioffset / 6; ln < (ioffset + icount) / 6; ln++) {
        // Each line segment is defined by six indices since its
        // formed by two triangles. We only need the first and last
        // vertices.
        // This bunch of vertices could also be the bevel triangles,
        // with we detect this situation by looking at the line weight.
        int i0 = voffset + indices[6 * ln + 0];
        int i1 = voffset + indices[6 * ln + 5];
        float sw0 = 2 * attribs[4 * i0 + 3];
        float sw1 = 2 * attribs[4 * i1 + 3];

        if (PGraphicsOpenGL.zero(sw0)) continue; // Bevel triangles, skip.

        float[] src0 = {0, 0, 0, 0};
        float[] src1 = {0, 0, 0, 0};
        float[] pt0 = {0, 0, 0, 0};
        float[] pt1 = {0, 0, 0, 0};
        int argb0 = PGL.nativeToJavaARGB(color[i0]);
        int argb1 = PGL.nativeToJavaARGB(color[i1]);

        PApplet.arrayCopy(vertices, 4 * i0, src0, 0, 4);
        PApplet.arrayCopy(vertices, 4 * i1, src1, 0, 4);
        // Applying any transformation is currently stored in the
        // modelview matrix of the renderer.
        g.modelview.mult(src0, pt0);
        g.modelview.mult(src1, pt1);

        if (raw.is3D()) {
          raw.strokeWeight(sw0);
          raw.stroke(argb0);
          raw.vertex(pt0[X], pt0[Y], pt0[Z]);
          raw.strokeWeight(sw1);
          raw.stroke(argb1);
          raw.vertex(pt1[X], pt1[Y], pt1[Z]);
        } else if (raw.is2D()) {
          float sx0 = g.screenXImpl(pt0[0], pt0[1], pt0[2], pt0[3]);
          float sy0 = g.screenYImpl(pt0[0], pt0[1], pt0[2], pt0[3]);
          float sx1 = g.screenXImpl(pt1[0], pt1[1], pt1[2], pt1[3]);
          float sy1 = g.screenYImpl(pt1[0], pt1[1], pt1[2], pt1[3]);
          raw.strokeWeight(sw0);
          raw.stroke(argb0);
          raw.vertex(sx0, sy0);
          raw.strokeWeight(sw1);
          raw.stroke(argb1);
          raw.vertex(sx1, sy1);
        }
      }
    }

    raw.endShape();
  }


  protected void renderPoints(PGraphicsOpenGL g) {
    PointShader shader = g.getPointShader();
    shader.bind();

    IndexCache cache = tessGeo.pointIndexCache;
    for (int n = firstPointIndexCache; n <= lastPointIndexCache; n++) {
      int ioffset = cache.indexOffset[n];
      int icount = cache.indexCount[n];
      int voffset = cache.vertexOffset[n];

      shader.setVertexAttribute(root.glPointVertex, 4, PGL.FLOAT,
                                0, 4 * voffset * PGL.SIZEOF_FLOAT);
      shader.setColorAttribute(root.glPointColor, 4, PGL.UNSIGNED_BYTE,
                               0, 4 * voffset * PGL.SIZEOF_BYTE);
      shader.setPointAttribute(root.glPointAttrib, 2, PGL.FLOAT,
                               0, 2 * voffset * PGL.SIZEOF_FLOAT);

      pgl.bindBuffer(PGL.ELEMENT_ARRAY_BUFFER, root.glPointIndex);
      pgl.drawElements(PGL.TRIANGLES, icount, PGL.INDEX_TYPE,
                       ioffset * PGL.SIZEOF_INDEX);
      pgl.bindBuffer(PGL.ELEMENT_ARRAY_BUFFER, 0);
    }

    shader.unbind();
  }


  protected void rawPoints(PGraphicsOpenGL g) {
    PGraphics raw = g.getRaw();

    raw.colorMode(RGB);
    raw.noFill();
    raw.strokeCap(strokeCap);
    raw.beginShape(POINTS);

    float[] vertices = tessGeo.pointVertices;
    int[] color = tessGeo.pointColors;
    float[] attribs = tessGeo.pointOffsets;
    short[] indices = tessGeo.pointIndices;

    IndexCache cache = tessGeo.pointIndexCache;
    for (int n = 0; n < cache.size; n++) {
      int ioffset = cache.indexOffset[n];
      int icount = cache.indexCount[n];
      int voffset = cache.vertexOffset[n];

      int pt = ioffset;
      while (pt < (ioffset + icount) / 3) {
        float size = attribs[2 * pt + 2];
        float weight;
        int perim;
        if (0 < size) { // round point
          weight = +size / 0.5f;
          perim = PApplet.max(PGraphicsOpenGL.MIN_POINT_ACCURACY,
                              (int) (TWO_PI * weight /
                              PGraphicsOpenGL.POINT_ACCURACY_FACTOR)) + 1;
        } else {        // Square point
          weight = -size / 0.5f;
          perim = 5;
        }

        int i0 = voffset + indices[3 * pt];
        int argb0 = PGL.nativeToJavaARGB(color[i0]);
        float[] pt0 = {0, 0, 0, 0};

        float[] src0 = {0, 0, 0, 0};
        PApplet.arrayCopy(vertices, 4 * i0, src0, 0, 4);
        g.modelview.mult(src0, pt0);

        if (raw.is3D()) {
          raw.strokeWeight(weight);
          raw.stroke(argb0);
          raw.vertex(pt0[X], pt0[Y], pt0[Z]);
        } else if (raw.is2D()) {
          float sx0 = g.screenXImpl(pt0[0], pt0[1], pt0[2], pt0[3]);
          float sy0 = g.screenYImpl(pt0[0], pt0[1], pt0[2], pt0[3]);
          raw.strokeWeight(weight);
          raw.stroke(argb0);
          raw.vertex(sx0, sy0);
        }

        pt += perim;
      }
    }

    raw.endShape();
  }
}
