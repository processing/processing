/* -*- mode: java; c-basic-offset: 2; indent-tabs-mode: nil -*- */

/*
  Part of the Processing project - http://processing.org

  Copyright (c) 2011-12 Ben Fry and Casey Reas

  This library is free software; you can redistribute it and/or
  modify it under the terms of the GNU Lesser General Public
  License as published by the Free Software Foundation; either
  version 2.1 of the License, or (at your option) any later version.

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

import processing.core.*;

import java.io.IOException;
import java.net.URL;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.HashMap;

/**
 * This class encapsulates a GLSL shader program, including a vertex
 * and a fragment shader. Based on the GLSLShader class from GLGraphics, which
 * in turn was originally based in the code by JohnG:
 * http://processing.org/discourse/beta/num_1159494801.html
 *
 * @webref rendering:shaders
 */
public class PShader {
  // shaders constants
  static protected final int COLOR    = 0;
  static protected final int LIGHT    = 1;
  static protected final int TEXTURE  = 2;
  static protected final int TEXLIGHT = 3;
  static protected final int LINE     = 4;
  static protected final int POINT    = 5;

  protected PApplet parent;
  // The main renderer associated to the parent PApplet.
  protected PGraphicsOpenGL pgMain;
  // We need a reference to the renderer since a shader might
  // be called by different renderers within a single application
  // (the one corresponding to the main surface, or other offscreen
  // renderers).
  protected PGraphicsOpenGL pgCurrent;

  protected PGL pgl;
  protected int context;      // The context that created this shader.

  public int glProgram;
  public int glVertex;
  public int glFragment;

  protected URL vertexURL;
  protected URL fragmentURL;

  protected String vertexFilename;
  protected String fragmentFilename;

  protected String vertexShaderSource;
  protected String fragmentShaderSource;

  protected boolean bound;

  protected HashMap<Integer, UniformValue> uniformValues = null;

  protected HashMap<Integer, Texture> textures;
  protected int firstTexUnit;
  protected int lastTexUnit;

  // Direct buffers to pass shader dat to GL
  protected IntBuffer intBuffer;
  protected FloatBuffer floatBuffer;

  public PShader() {
    parent = null;
    pgMain = null;
    pgl = null;
    context = -1;

    this.vertexURL = null;
    this.fragmentURL = null;
    this.vertexFilename = null;
    this.fragmentFilename = null;

    glProgram = 0;
    glVertex = 0;
    glFragment = 0;

    firstTexUnit = 0;

    intBuffer = PGL.allocateIntBuffer(1);
    floatBuffer = PGL.allocateFloatBuffer(1);

    bound = false;
  }



  public PShader(PApplet parent) {
    this();
    this.parent = parent;
    pgMain = (PGraphicsOpenGL) parent.g;
    pgl = pgMain.pgl;
    context = pgl.createEmptyContext();
  }


  /**
   * Creates a shader program using the specified vertex and fragment
   * shaders.
   *
   * @param parent the parent program
   * @param vertFilename name of the vertex shader
   * @param fragFilename name of the fragment shader
   */
  public PShader(PApplet parent, String vertFilename, String fragFilename) {
    this.parent = parent;
    pgMain = (PGraphicsOpenGL) parent.g;
    pgl = pgMain.pgl;

    this.vertexURL = null;
    this.fragmentURL = null;
    this.vertexFilename = vertFilename;
    this.fragmentFilename = fragFilename;

    glProgram = 0;
    glVertex = 0;
    glFragment = 0;

    intBuffer = PGL.allocateIntBuffer(1);
    floatBuffer = PGL.allocateFloatBuffer(1);
  }

  /**
   * @param vertURL network location of the vertex shader
   * @param fragURL network location of the fragment shader
   */
  public PShader(PApplet parent, URL vertURL, URL fragURL) {
    this.parent = parent;
    pgMain = (PGraphicsOpenGL) parent.g;
    pgl = pgMain.pgl;

    this.vertexURL = vertURL;
    this.fragmentURL = fragURL;
    this.vertexFilename = null;
    this.fragmentFilename = null;

    glProgram = 0;
    glVertex = 0;
    glFragment = 0;

    intBuffer = PGL.allocateIntBuffer(1);
    floatBuffer = PGL.allocateFloatBuffer(1);
  }


  @Override
  protected void finalize() throws Throwable {
    try {
      if (glVertex != 0) {
        pgMain.finalizeGLSLVertShaderObject(glVertex, context);
      }
      if (glFragment != 0) {
        pgMain.finalizeGLSLFragShaderObject(glFragment, context);
      }
      if (glProgram != 0) {
        pgMain.finalizeGLSLProgramObject(glProgram, context);
      }
    } finally {
      super.finalize();
    }
  }


  public void setVertexShader(String vertFilename) {
    this.vertexFilename = vertFilename;
  }


  public void setVertexShader(URL vertURL) {
    this.vertexURL = vertURL;
  }


  public void setFragmentShader(String fragFilename) {
    this.fragmentFilename = fragFilename;
  }


  public void setFragmentShader(URL fragURL) {
    this.fragmentURL = fragURL;
  }


  /**
   * Initializes (if needed) and binds the shader program.
   */
  public void bind() {
    init();
    pgl.useProgram(glProgram);
    bound = true;
    consumeUniforms();
    bindTextures();
  }


  /**
   * Unbinds the shader program.
   */
  public void unbind() {
    unbindTextures();
    pgl.useProgram(0);
    bound = false;
  }


  /**
   * Returns true if the shader is bound, false otherwise.
   */
  public boolean bound() {
    return bound;
  }

  /**
   * @webref rendering:shaders
   * @brief Sets a variable within the shader
   * @param name the name of the uniform variable to modify
   * @param x first component of the variable to modify
   */
  public void set(String name, int x) {
    setUniformImpl(name, UniformValue.INT1, new int[] { x });
  }

  /**
   * @param y second component of the variable to modify. The variable has to be declared with an array/vector type in the shader (i.e.: int[2], vec2)
   */
  public void set(String name, int x, int y) {
    setUniformImpl(name, UniformValue.INT2, new int[] { x, y });
  }

  /**
   * @param z third component of the variable to modify. The variable has to be declared with an array/vector type in the shader (i.e.: int[3], vec3)
   */
  public void set(String name, int x, int y, int z) {
    setUniformImpl(name, UniformValue.INT3, new int[] { x, y, z });
  }

  /**
   * @param w fourth component of the variable to modify. The variable has to be declared with an array/vector type in the shader (i.e.: int[4], vec4)
   */
  public void set(String name, int x, int y, int z, int w) {
    setUniformImpl(name, UniformValue.INT4, new int[] { x, y, z });
  }


  public void set(String name, float x) {
    setUniformImpl(name, UniformValue.FLOAT1, new float[] { x });
  }


  public void set(String name, float x, float y) {
    setUniformImpl(name, UniformValue.FLOAT2, new float[] { x, y });
  }


  public void set(String name, float x, float y, float z) {
    setUniformImpl(name, UniformValue.FLOAT3, new float[] { x, y, z });
  }


  public void set(String name, float x, float y, float z, float w) {
    setUniformImpl(name, UniformValue.FLOAT4, new float[] { x, y, z, w });
  }

  /**
   * @param vec modifies all the components of an array/vector uniform variable. PVector can only be used if the type of the variable is vec3.
   */
  public void set(String name, PVector vec) {
    setUniformImpl(name, UniformValue.FLOAT3,
                   new float[] { vec.x, vec.y, vec.z });
  }


  public void set(String name, int[] vec) {
    set(name, vec, 1);
  }

  /**
   * @param ncoords number of coordinates per element, max 4
   */
  public void set(String name, int[] vec, int ncoords) {
    if (ncoords == 1) {
      setUniformImpl(name, UniformValue.INT1VEC, vec);
    } else if (ncoords == 2) {
      setUniformImpl(name, UniformValue.INT2VEC, vec);
    } else if (ncoords == 3) {
      setUniformImpl(name, UniformValue.INT3VEC, vec);
    } else if (ncoords == 4) {
      setUniformImpl(name, UniformValue.INT4VEC, vec);
    } else if (4 < ncoords) {
      PGraphics.showWarning("Only up to 4 coordinates per element are " +
                            "supported.");
    } else {
      PGraphics.showWarning("Wrong number of coordinates: it is negative!");
    }
  }


  public void set(String name, float[] vec) {
    set(name, vec, 1);
  }


  public void set(String name, float[] vec, int ncoords) {
    if (ncoords == 1) {
      setUniformImpl(name, UniformValue.FLOAT1VEC, vec);
    } else if (ncoords == 2) {
      setUniformImpl(name, UniformValue.FLOAT2VEC, vec);
    } else if (ncoords == 3) {
      setUniformImpl(name, UniformValue.FLOAT3VEC, vec);
    } else if (ncoords == 4) {
      setUniformImpl(name, UniformValue.FLOAT4VEC, vec);
    } else if (4 < ncoords) {
      PGraphics.showWarning("Only up to 4 coordinates per element are " +
                            "supported.");
    } else {
      PGraphics.showWarning("Wrong number of coordinates: it is negative!");
    }
  }

  /**
   * @param mat matrix of values
   */
  public void set(String name, PMatrix2D mat) {
    float[] matv = { mat.m00, mat.m01,
                     mat.m10, mat.m11 };
    setUniformImpl(name, UniformValue.MAT2, matv);
  }


  public void set(String name, PMatrix3D mat) {
    set(name, mat, false);
  }

  /**
   * @param use3x3 enforces the matrix is 3 x 3
   */
  public void set(String name, PMatrix3D mat, boolean use3x3) {
    if (use3x3) {
      float[] matv = { mat.m00, mat.m01, mat.m02,
                       mat.m10, mat.m11, mat.m12,
                       mat.m20, mat.m21, mat.m22 };
      setUniformImpl(name, UniformValue.MAT3, matv);
    } else {
      float[] matv = { mat.m00, mat.m01, mat.m02, mat.m03,
                       mat.m10, mat.m11, mat.m12, mat.m13,
                       mat.m20, mat.m21, mat.m22, mat.m23,
                       mat.m30, mat.m31, mat.m32, mat.m33 };
      setUniformImpl(name, UniformValue.MAT4, matv);
    }
  }

  /**
   * @param tex sets the sampler uniform variable to read from this image texture
   */
  public void set(String name, PImage tex) {
    setUniformImpl(name, UniformValue.SAMPLER2D, tex);
  }



  /**
   * Returns the ID location of the attribute parameter given its name.
   *
   * @param name String
   * @return int
   */
  protected int getAttributeLoc(String name) {
    init();
    return pgl.getAttribLocation(glProgram, name);
  }


  /**
   * Returns the ID location of the uniform parameter given its name.
   *
   * @param name String
   * @return int
   */
  protected int getUniformLoc(String name) {
    init();
    return pgl.getUniformLocation(glProgram, name);
  }


  protected void setAttributeVBO(int loc, int vboId, int size, int type,
                                 boolean normalized, int stride, int offset) {
    if (-1 < loc) {
      pgl.bindBuffer(PGL.ARRAY_BUFFER, vboId);
      pgl.vertexAttribPointer(loc, size, type, normalized, stride, offset);
    }
  }


  protected void setUniformValue(int loc, int x) {
    if (-1 < loc) {
      pgl.uniform1i(loc, x);
    }
  }


  protected void setUniformValue(int loc, int x, int y) {
    if (-1 < loc) {
      pgl.uniform2i(loc, x, y);
    }
  }


  protected void setUniformValue(int loc, int x, int y, int z) {
    if (-1 < loc) {
      pgl.uniform3i(loc, x, y, z);
    }
  }


  protected void setUniformValue(int loc, int x, int y, int z, int w) {
    if (-1 < loc) {
      pgl.uniform4i(loc, x, y, z, w);
    }
  }


  protected void setUniformValue(int loc, float x) {
    if (-1 < loc) {
      pgl.uniform1f(loc, x);
    }
  }

  protected void setUniformValue(int loc, float x, float y) {
    if (-1 < loc) {
      pgl.uniform2f(loc, x, y);
    }
  }


  protected void setUniformValue(int loc, float x, float y, float z) {
    if (-1 < loc) {
      pgl.uniform3f(loc, x, y, z);
    }
  }


  protected void setUniformValue(int loc, float x, float y, float z, float w) {
    if (-1 < loc) {
      pgl.uniform4f(loc, x, y, z, w);
    }
  }


  protected void setUniformVector(int loc, int[] vec, int ncoords,
                                  int length) {
    if (-1 < loc) {
      updateIntBuffer(vec);
      if (ncoords == 1) {
        pgl.uniform1iv(loc, length, intBuffer);
      } else if (ncoords == 2) {
        pgl.uniform2iv(loc, length, intBuffer);
      } else if (ncoords == 3) {
        pgl.uniform3iv(loc, length, intBuffer);
      } else if (ncoords == 4) {
        pgl.uniform3iv(loc, length, intBuffer);
      }
    }
  }


  protected void setUniformVector(int loc, float[] vec, int ncoords,
                                  int length) {
    if (-1 < loc) {
      updateFloatBuffer(vec);
      if (ncoords == 1) {
        pgl.uniform1fv(loc, length, floatBuffer);
      } else if (ncoords == 2) {
        pgl.uniform2fv(loc, length, floatBuffer);
      } else if (ncoords == 3) {
        pgl.uniform3fv(loc, length, floatBuffer);
      } else if (ncoords == 4) {
        pgl.uniform4fv(loc, length, floatBuffer);
      }
    }
  }


  protected void setUniformMatrix(int loc, float[] mat) {
    if (-1 < loc) {
      updateFloatBuffer(mat);
      if (mat.length == 4) {
        pgl.uniformMatrix2fv(loc, 1, false, floatBuffer);
      } else if (mat.length == 9) {
        pgl.uniformMatrix3fv(loc, 1, false, floatBuffer);
      } else if (mat.length == 16) {
        pgl.uniformMatrix4fv(loc, 1, false, floatBuffer);
      }
    }
  }


  protected void setUniformTex(int loc, Texture tex) {
    // get unit from last value in bindTextures ...
  //  pgl.activeTexture(PGL.TEXTURE0 + unit);
    tex.bind();
  }


  /*
  // The individual attribute setters are not really needed, read this:
  // http://stackoverflow.com/questions/7718976/what-is-glvertexattrib-versus-glvertexattribpointer-used-for
  // except for setting a constant vertex attribute value.
  public void set1FloatAttribute(int loc, float x) {
    if (-1 < loc) {
      pgl.glVertexAttrib1f(loc, x);
    }
  }


  public void set2FloatAttribute(int loc, float x, float y) {
    if (-1 < loc) {
      pgl.glVertexAttrib2f(loc, x, y);
    }
  }


  public void set3FloatAttribute(int loc, float x, float y, float z) {
    if (-1 < loc) {
      pgl.glVertexAttrib3f(loc, x, y, z);
    }
  }


  public void set4FloatAttribute(int loc, float x, float y, float z, float w) {
    if (-1 < loc) {
      pgl.glVertexAttrib4f(loc, x, y, z, w);
    }
  }
  */


  protected void setUniformImpl(String name, int type, Object value) {
    int loc = getUniformLoc(name);
    if (-1 < loc) {
      if (uniformValues == null) {
        uniformValues = new HashMap<Integer, UniformValue>();
      }
      uniformValues.put(loc, new UniformValue(type, value));
    } else {
      PGraphics.showWarning("The shader doesn't have a uniform called \"" +
                            name + "\"");
    }
  }


  protected void consumeUniforms() {
    if (uniformValues != null && 0 < uniformValues.size()) {
      lastTexUnit = firstTexUnit;
      for (Integer loc: uniformValues.keySet()) {
        UniformValue val = uniformValues.get(loc);
        if (val.type == UniformValue.INT1) {
          int[] v = ((int[])val.value);
          pgl.uniform1i(loc, v[0]);
        } else if (val.type == UniformValue.INT2) {
          int[] v = ((int[])val.value);
          pgl.uniform2i(loc, v[0], v[1]);
        } else if (val.type == UniformValue.INT3) {
          int[] v = ((int[])val.value);
          pgl.uniform3i(loc, v[0], v[1], v[2]);
        } else if (val.type == UniformValue.INT4) {
          int[] v = ((int[])val.value);
          pgl.uniform4i(loc, v[0], v[1], v[2], v[4]);
        } else if (val.type == UniformValue.FLOAT1) {
          float[] v = ((float[])val.value);
          pgl.uniform1f(loc, v[0]);
        } else if (val.type == UniformValue.FLOAT2) {
          float[] v = ((float[])val.value);
          pgl.uniform2f(loc, v[0], v[1]);
        } else if (val.type == UniformValue.FLOAT3) {
          float[] v = ((float[])val.value);
          pgl.uniform3f(loc, v[0], v[1], v[2]);
        } else if (val.type == UniformValue.FLOAT4) {
          float[] v = ((float[])val.value);
          pgl.uniform4f(loc, v[0], v[1], v[2], v[3]);
        } else if (val.type == UniformValue.INT1VEC) {
          int[] v = ((int[])val.value);
          updateIntBuffer(v);
          pgl.uniform1iv(loc, v.length, intBuffer);
        } else if (val.type == UniformValue.INT2VEC) {
          int[] v = ((int[])val.value);
          updateIntBuffer(v);
          pgl.uniform2iv(loc, v.length / 2, intBuffer);
        } else if (val.type == UniformValue.INT3VEC) {
          int[] v = ((int[])val.value);
          updateIntBuffer(v);
          pgl.uniform3iv(loc, v.length / 3, intBuffer);
        } else if (val.type == UniformValue.INT4VEC) {
          int[] v = ((int[])val.value);
          updateIntBuffer(v);
          pgl.uniform4iv(loc, v.length / 4, intBuffer);
        } else if (val.type == UniformValue.FLOAT1VEC) {
          float[] v = ((float[])val.value);
          updateFloatBuffer(v);
          pgl.uniform1fv(loc, v.length, floatBuffer);
        } else if (val.type == UniformValue.FLOAT2VEC) {
          float[] v = ((float[])val.value);
          updateFloatBuffer(v);
          pgl.uniform2fv(loc, v.length / 2, floatBuffer);
        } else if (val.type == UniformValue.FLOAT3VEC) {
          float[] v = ((float[])val.value);
          updateFloatBuffer(v);
          pgl.uniform3fv(loc, v.length / 3, floatBuffer);
        } else if (val.type == UniformValue.FLOAT4VEC) {
          float[] v = ((float[])val.value);
          updateFloatBuffer(v);
          pgl.uniform4fv(loc, v.length / 4, floatBuffer);
        } else if (val.type == UniformValue.MAT2) {
          float[] v = ((float[])val.value);
          updateFloatBuffer(v);
          pgl.uniformMatrix2fv(loc, 1, false, floatBuffer);
        } else if (val.type == UniformValue.MAT3) {
          float[] v = ((float[])val.value);
          updateFloatBuffer(v);
          pgl.uniformMatrix3fv(loc, 1, false, floatBuffer);
        } else if (val.type == UniformValue.MAT4) {
          float[] v = ((float[])val.value);
          updateFloatBuffer(v);
          pgl.uniformMatrix4fv(loc, 1, false, floatBuffer);
        } else if (val.type == UniformValue.SAMPLER2D) {
          PImage img = (PImage)val.value;
          Texture tex = pgMain.getTexture(img);
          pgl.uniform1i(loc, lastTexUnit);
          if (textures == null) {
            textures = new HashMap<Integer, Texture>();
          }
          textures.put(lastTexUnit, tex);
          lastTexUnit++;
        }
      }
      uniformValues.clear();
    }
  }


  protected void updateIntBuffer(int[] vec) {
    intBuffer = PGL.updateIntBuffer(intBuffer, vec, false);
  }


  protected void updateFloatBuffer(float[] vec) {
    floatBuffer = PGL.updateFloatBuffer(floatBuffer, vec, false);
  }


  protected void bindTextures() {
    if (textures != null) {
      for (int unit: textures.keySet()) {
        Texture tex = textures.get(unit);
        pgl.activeTexture(PGL.TEXTURE0 + unit);
        tex.bind();
      }
    }
  }


  protected void unbindTextures() {
    if (textures != null) {
      for (int unit: textures.keySet()) {
        Texture tex = textures.get(unit);
        pgl.activeTexture(PGL.TEXTURE0 + unit);
        tex.unbind();
      }
      pgl.activeTexture(PGL.TEXTURE0);
    }
  }


  protected void init() {
    if (glProgram == 0 || contextIsOutdated()) {
      context = pgl.getCurrentContext();
      glProgram = pgMain.createGLSLProgramObject(context);

      boolean hasVert = false;
      if (vertexFilename != null) {
        hasVert = loadVertexShader(vertexFilename);
      } else if (vertexURL != null) {
        hasVert = loadVertexShader(vertexURL);
      } else {
        PGraphics.showException("Vertex shader filenames and URLs are " +
                                "both null!");
      }

      boolean hasFrag = false;
      if (fragmentFilename != null) {
        hasFrag = loadFragmentShader(fragmentFilename);
      } else if (fragmentURL != null) {
        hasFrag = loadFragmentShader(fragmentURL);
      } else {
        PGraphics.showException("Fragment shader filenames and URLs are " +
                                "both null!");
      }

      boolean vertRes = true;
      if (hasVert) {
        vertRes = compileVertexShader();
      }

      boolean fragRes = true;
      if (hasFrag) {
        fragRes = compileFragmentShader();
      }

      if (vertRes && fragRes) {
        if (hasVert) {
          pgl.attachShader(glProgram, glVertex);
        }
        if (hasFrag) {
          pgl.attachShader(glProgram, glFragment);
        }
        pgl.linkProgram(glProgram);

        pgl.getProgramiv(glProgram, PGL.LINK_STATUS, intBuffer);
        boolean linked = intBuffer.get(0) == 0 ? false : true;
        if (!linked) {
          PGraphics.showException("Cannot link shader program:\n" +
                                  pgl.getProgramInfoLog(glProgram));
        }

        pgl.validateProgram(glProgram);
        pgl.getProgramiv(glProgram, PGL.VALIDATE_STATUS, intBuffer);
        boolean validated = intBuffer.get(0) == 0 ? false : true;
        if (!validated) {
          PGraphics.showException("Cannot validate shader program:\n" +
                                  pgl.getProgramInfoLog(glProgram));
        }
      }
    }
  }


  protected boolean contextIsOutdated() {
    boolean outdated = !pgl.contextIsCurrent(context);
    if (outdated) {
      pgMain.removeGLSLProgramObject(glProgram, context);
      pgMain.removeGLSLVertShaderObject(glVertex, context);
      pgMain.removeGLSLFragShaderObject(glFragment, context);

      glProgram = 0;
      glVertex = 0;
      glFragment = 0;
    }
    return outdated;
  }


  /**
   * Loads and compiles the vertex shader contained in file.
   *
   * @param file String
   */
  protected boolean loadVertexShader(String filename) {
    vertexShaderSource = PApplet.join(parent.loadStrings(filename), "\n");
    return vertexShaderSource != null;
  }


  /**
   * Loads and compiles the vertex shader contained in the URL.
   *
   * @param file String
   */
  protected boolean loadVertexShader(URL url) {
    try {
      vertexShaderSource = PApplet.join(PApplet.loadStrings(url.openStream()),
                                        "\n");
      return vertexShaderSource != null;
    } catch (IOException e) {
      PGraphics.showException("Cannot load vertex shader " + url.getFile());
      return false;
    }
  }


  /**
   * Loads and compiles the fragment shader contained in file.
   *
   * @param file String
   */
  protected boolean loadFragmentShader(String filename) {
    fragmentShaderSource = PApplet.join(parent.loadStrings(filename), "\n");
    return fragmentShaderSource != null;
  }


  /**
   * Loads and compiles the fragment shader contained in the URL.
   *
   * @param url URL
   */
  protected boolean loadFragmentShader(URL url) {
    try {
      fragmentShaderSource = PApplet.join(PApplet.loadStrings(url.openStream()),
                                          "\n");
      return fragmentShaderSource != null;
    } catch (IOException e) {
      PGraphics.showException("Cannot load fragment shader " + url.getFile());
      return false;
    }
  }


  /**
   * @param shaderSource a string containing the shader's code
   */
  protected boolean compileVertexShader() {
    glVertex = pgMain.createGLSLVertShaderObject(context);

    pgl.shaderSource(glVertex, vertexShaderSource);
    pgl.compileShader(glVertex);

    pgl.getShaderiv(glVertex, PGL.COMPILE_STATUS, intBuffer);
    boolean compiled = intBuffer.get(0) == 0 ? false : true;
    if (!compiled) {
      PGraphics.showException("Cannot compile vertex shader:\n" +
                              pgl.getShaderInfoLog(glVertex));
      return false;
    } else {
      return true;
    }
  }


  /**
   * @param shaderSource a string containing the shader's code
   */
  protected boolean compileFragmentShader() {
    glFragment = pgMain.createGLSLFragShaderObject(context);

    pgl.shaderSource(glFragment, fragmentShaderSource);
    pgl.compileShader(glFragment);

    pgl.getShaderiv(glFragment, PGL.COMPILE_STATUS, intBuffer);
    boolean compiled = intBuffer.get(0) == 0 ? false : true;
    if (!compiled) {
      PGraphics.showException("Cannot compile fragment shader:\n" +
                              pgl.getShaderInfoLog(glFragment));
      return false;
    } else {
      return true;
    }
  }


  protected void setRenderer(PGraphicsOpenGL pg) {
    pgCurrent = pg;
  }

  protected void loadAttributes() { }


  protected void loadUniforms() { }


  protected void release() {
    if (glVertex != 0) {
      pgMain.deleteGLSLVertShaderObject(glVertex, context);
      glVertex = 0;
    }
    if (glFragment != 0) {
      pgMain.deleteGLSLFragShaderObject(glFragment, context);
      glFragment = 0;
    }
    if (glProgram != 0) {
      pgMain.deleteGLSLProgramObject(glProgram, context);
      glProgram = 0;
    }
  }

  // Class to store a user-specified value for a uniform parameter
  // in the shader
  protected class UniformValue {
    static final int INT1      = 0;
    static final int INT2      = 1;
    static final int INT3      = 2;
    static final int INT4      = 3;
    static final int FLOAT1    = 4;
    static final int FLOAT2    = 5;
    static final int FLOAT3    = 6;
    static final int FLOAT4    = 7;
    static final int INT1VEC   = 8;
    static final int INT2VEC   = 9;
    static final int INT3VEC   = 10;
    static final int INT4VEC   = 11;
    static final int FLOAT1VEC = 12;
    static final int FLOAT2VEC = 13;
    static final int FLOAT3VEC = 14;
    static final int FLOAT4VEC = 15;
    static final int MAT2      = 16;
    static final int MAT3      = 17;
    static final int MAT4      = 18;
    static final int SAMPLER2D = 19;

    int type;
    Object value;

    UniformValue(int type, Object value) {
      this.type = type;
      this.value = value;
    }
  }
}
