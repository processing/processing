/* -*- mode: java; c-basic-offset: 2; indent-tabs-mode: nil -*- */

/*
  Part of the Processing project - http://processing.org

  Copyright (c) 2012 Ben Fry and Casey Reas

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

import java.io.BufferedReader;
import java.util.ArrayList;
import java.util.Hashtable;

import processing.core.PApplet;
import processing.core.PGraphics;
import processing.core.PImage;
import processing.core.PShape;
import processing.core.PVector;

public class PGraphics3D extends PGraphicsOpenGL {

  public PGraphics3D() {
    super();
    hints[ENABLE_STROKE_PERSPECTIVE] = true;
  }
  
  //////////////////////////////////////////////////////////////

  // RENDERER SUPPORT QUERIES
  
  
  public boolean is2D() {
    return false;
  }

  
  public boolean is3D() {
    return true;
  }    
  
  
  //////////////////////////////////////////////////////////////

  // PROJECTION
  
  
  protected void defaultPerspective() {    
    perspective();
  }
  
  
  //////////////////////////////////////////////////////////////

  // CAMERA
  
  
  protected void defaultCamera() {    
    camera();
  }  

  
  //////////////////////////////////////////////////////////////

  // MATRIX MORE!
  
  
  protected void begin2D() {
    pushProjection();
    ortho(-width/2, +width/2, -height/2, +height/2, -1, +1);
    pushMatrix();
    camera(width/2, height/2);    
  }
  

  protected void end2D() {
    popMatrix();
    popProjection();    
  }  
  
  
  //////////////////////////////////////////////////////////////

  // SHAPE I/O
  

  static protected boolean isSupportedExtension(String extension) {
    return extension.equals("obj");
  }


  static protected PShape loadShapeImpl(PGraphics pg, String filename, 
                                        String ext) {
    ArrayList<PVector> vertices = new ArrayList<PVector>(); 
    ArrayList<PVector> normals = new ArrayList<PVector>();
    ArrayList<PVector> textures = new ArrayList<PVector>();
    ArrayList<OBJFace> faces = new ArrayList<OBJFace>();
    ArrayList<OBJMaterial> materials = new ArrayList<OBJMaterial>();    
    
    BufferedReader reader = pg.parent.createReader(filename);
    parseOBJ(pg.parent, reader, vertices, normals, textures, faces, materials);

    int prevColorMode = pg.colorMode;
    float prevColorModeX = pg.colorModeX; 
    float prevColorModeY = pg.colorModeY; 
    float prevColorModeZ = pg.colorModeZ;
    float prevColorModeA = pg.colorModeA;
    boolean prevStroke = pg.stroke;
    int prevTextureMode = pg.textureMode;
    pg.colorMode(RGB, 1);
    pg.stroke = false;        
    pg.textureMode = NORMAL;    
    
    // The OBJ geometry is stored in a group shape, 
    // with each face in a separate child geometry
    // shape.
    PShape root = createShapeImpl(pg.parent, GROUP);
    
    int mtlIdxCur = -1;
    OBJMaterial mtl = null;    
    for (int i = 0; i < faces.size(); i++) {
      OBJFace face = faces.get(i);
      
      // Getting current material.
      if (mtlIdxCur != face.matIdx) {
        mtlIdxCur = PApplet.max(0, face.matIdx); // To make sure that at least we get the default material.        
        mtl = materials.get(mtlIdxCur);
      }

      // Creating child shape for current face.
      PShape child;
      if (face.vertIdx.size() == 3) {
        child = createShapeImpl(pg.parent, TRIANGLES);   // Face is a triangle, so using appropriate shape kind.
      } else if (face.vertIdx.size() == 4) {
        child = createShapeImpl(pg.parent, QUADS);       // Face is a quad, so using appropriate shape kind.
      } else {
        child = createShapeImpl(pg.parent, POLYGON);      // Face is a general polygon
      }      
      
      // Setting material properties for the new face
      child.fill(mtl.kd.x, mtl.kd.y, mtl.kd.z);
      child.ambient(mtl.ka.x, mtl.ka.y, mtl.ka.z);
      child.specular(mtl.ks.x, mtl.ks.y, mtl.ks.z);
      child.shininess(mtl.ns);      
      if (mtl.kdMap != null) {
        // If current material is textured, then tinting the texture using the diffuse color.
        child.tint(mtl.kd.x, mtl.kd.y, mtl.kd.z, mtl.d);
      }
      
      for (int j = 0; j < face.vertIdx.size(); j++){
        int vertIdx, normIdx;
        PVector vert, norms;

        vert = norms = null;
        
        vertIdx = face.vertIdx.get(j).intValue() - 1;
        vert = vertices.get(vertIdx);
        
        if (j < face.normIdx.size()) {
          normIdx = face.normIdx.get(j).intValue() - 1;
          if (-1 < normIdx) {
            norms = normals.get(normIdx);  
          }
        }
        
        if (mtl != null && mtl.kdMap != null) {
          // This face is textured.
          int texIdx;
          PVector tex = null; 
          
          if (j < face.texIdx.size()) {
            texIdx = face.texIdx.get(j).intValue() - 1;
            if (-1 < texIdx) {
              tex = textures.get(texIdx);  
            }
          }
          
          child.texture(mtl.kdMap);
          if (norms != null) {
            child.normal(norms.x, norms.y, norms.z);
          }
          if (tex != null) {
            child.vertex(vert.x, vert.y, vert.z, tex.x, tex.y);  
          } else {
            child.vertex(vert.x, vert.y, vert.z);
          }
        } else {
          // This face is not textured.
          if (norms != null) {
            child.normal(norms.x, norms.y, norms.z);
          }
          child.vertex(vert.x, vert.y, vert.z);          
        }
      } 
      
      child.end(CLOSE);
      root.addChild(child);      
    }
    
    pg.colorMode(prevColorMode, prevColorModeX, prevColorModeY, prevColorModeZ, 
                 prevColorModeA);    
    pg.stroke = prevStroke;
    pg.textureMode = prevTextureMode; 
    
    return root;
  }  
  
  
  //////////////////////////////////////////////////////////////

  // SHAPE CREATION

  
  public PShape createShape(PShape source) {
    return PShape3D.createShape(parent, source);    
  }  
  

  public PShape createShape() {
    return createShape(POLYGON);
  }


  public PShape createShape(int type) {
    return createShapeImpl(parent, type);
  }

  
  public PShape createShape(int kind, float... p) {
    return createShapeImpl(parent, kind, p);
  }  
  

  static protected PShape3D createShapeImpl(PApplet parent, int type) {
    PShape3D shape = null;
    if (type == PShape.GROUP) {
      shape = new PShape3D(parent, PShape.GROUP);
    } else if (type == PShape.PATH) {
      shape = new PShape3D(parent, PShape.PATH);
    } else if (type == POINTS) {
      shape = new PShape3D(parent, PShape.GEOMETRY);
      shape.setKind(POINTS);
    } else if (type == LINES) {
      shape = new PShape3D(parent, PShape.GEOMETRY);
      shape.setKind(LINES);
    } else if (type == TRIANGLE || type == TRIANGLES) {
      shape = new PShape3D(parent, PShape.GEOMETRY);
      shape.setKind(TRIANGLES);
    } else if (type == TRIANGLE_FAN) {
      shape = new PShape3D(parent, PShape.GEOMETRY);
      shape.setKind(TRIANGLE_FAN);
    } else if (type == TRIANGLE_STRIP) {
      shape = new PShape3D(parent, PShape.GEOMETRY);
      shape.setKind(TRIANGLE_STRIP);
    } else if (type == QUAD || type == QUADS) {
      shape = new PShape3D(parent, PShape.GEOMETRY);
      shape.setKind(QUADS);
    } else if (type == QUAD_STRIP) {
      shape = new PShape3D(parent, PShape.GEOMETRY);
      shape.setKind(QUAD_STRIP);
    } else if (type == POLYGON) {
      shape = new PShape3D(parent, PShape.GEOMETRY);
      shape.setKind(POLYGON);
    }
    return shape;
  }
  
  
  static protected PShape3D createShapeImpl(PApplet parent, int kind, float... p) {
    PShape3D shape = null;
    int len = p.length;

    if (kind == POINT) {
      if (len != 2 && len != 3) {
        showWarning("Wrong number of parameters");
        return null;
      }
      shape = new PShape3D(parent, PShape.PRIMITIVE);
      shape.setKind(POINT);
    } else if (kind == LINE) {
      if (len != 4 && len != 6) {
        showWarning("Wrong number of parameters");
        return null;
      }
      shape = new PShape3D(parent, PShape.PRIMITIVE);
      shape.setKind(LINE);
    } else if (kind == TRIANGLE) {
      if (len != 6) {
        showWarning("Wrong number of parameters");
        return null;
      }
      shape = new PShape3D(parent, PShape.PRIMITIVE);
      shape.setKind(TRIANGLE);
    } else if (kind == QUAD) {
      if (len != 8) {
        showWarning("Wrong number of parameters");
        return null;
      }
      shape = new PShape3D(parent, PShape.PRIMITIVE);
      shape.setKind(QUAD);
    } else if (kind == RECT) {
      if (len != 4 && len != 5 && len != 8) {
        showWarning("Wrong number of parameters");
        return null;
      }
      shape = new PShape3D(parent, PShape.PRIMITIVE);
      shape.setKind(RECT);
    } else if (kind == ELLIPSE) {
      if (len != 4) {
        showWarning("Wrong number of parameters");
        return null;
      }
      shape = new PShape3D(parent, PShape.PRIMITIVE);
      shape.setKind(ELLIPSE);
    } else if (kind == ARC) {
      if (len != 6) {
        showWarning("Wrong number of parameters");
        return null;
      }
      shape = new PShape3D(parent, PShape.PRIMITIVE);
      shape.setKind(ARC);
    } else if (kind == BOX) {
      if (len != 1 && len != 3) {
        showWarning("Wrong number of parameters");
        return null;
      }
      shape = new PShape3D(parent, PShape.PRIMITIVE);
      shape.setKind(BOX);
    } else if (kind == SPHERE) {
      if (len != 1) {
        showWarning("Wrong number of parameters");
        return null;
      }
      shape = new PShape3D(parent, PShape.PRIMITIVE);
      shape.setKind(SPHERE);
    } else {
      showWarning("Unrecognized primitive type");
    }

    if (shape != null) {
      shape.setParams(p);
    }

    return shape;
  }
  
  
  //////////////////////////////////////////////////////////////

  // OBJ LOADING  

  
  static protected void parseOBJ(PApplet parent,
                                 BufferedReader reader, 
                                 ArrayList<PVector> vertices, 
                                 ArrayList<PVector> normals, 
                                 ArrayList<PVector> textures, 
                                 ArrayList<OBJFace> faces, 
                                 ArrayList<OBJMaterial> materials) {
    Hashtable<String, Integer> mtlTable  = new Hashtable<String, Integer>();
    int mtlIdxCur = -1;
    boolean readv, readvn, readvt;
    try {
      
      readv = readvn = readvt = false;
      String line;
      String gname = "object";
      while ((line = reader.readLine()) != null) {
       // Parse the line.
        
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
            PVector tempv = new PVector(Float.valueOf(elements[1]).floatValue(), 
                                        Float.valueOf(elements[2]).floatValue(), 
                                        Float.valueOf(elements[3]).floatValue());
            vertices.add(tempv);
            readv = true;
          } else if (elements[0].equals("vn")) {
            // normal
            PVector tempn = new PVector(Float.valueOf(elements[1]).floatValue(), 
                                        Float.valueOf(elements[2]).floatValue(), 
                                        Float.valueOf(elements[3]).floatValue());
            normals.add(tempn);
            readvn = true;
          } else if (elements[0].equals("vt")) {
            // uv, inverting v to take into account Processing's invertex Y axis 
            // with respect to OpenGL.
            PVector tempv = new PVector(Float.valueOf(elements[1]).floatValue(), 
                                        1 - Float.valueOf(elements[2]).
                                        floatValue());
            textures.add(tempv);
            readvt = true;
          } else if (elements[0].equals("o")) {
            // Object name is ignored, for now.
          } else if (elements[0].equals("mtllib")) {
            if (elements[1] != null) {
              BufferedReader mreader = parent.createReader(elements[1]);
              if (mreader != null) {
                parseMTL(parent, mreader, materials, mtlTable);
              }
            }
          } else if (elements[0].equals("g")) {            
            gname = 1 < elements.length ? elements[1] : "";
          } else if (elements[0].equals("usemtl")) {
            // Getting index of current active material (will be applied on all subsequent faces).
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
  
  
  static protected void parseMTL(PApplet parent,
                                 BufferedReader reader, 
                                 ArrayList<OBJMaterial> materials, 
                                 Hashtable<String, Integer> materialsHash) {
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
            currentMtl.kdMap = parent.loadImage(texname);
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
          } else if ((elements[0].equals("d") || 
                      elements[0].equals("Tr")) && elements.length > 1) {
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
  
  
  // Stores a face from an OBJ file
  static protected class OBJFace {
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
  
  
  // Stores a material defined in an MTL file.
  static protected class OBJMaterial {
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