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
import java.util.HashMap;

/**
 * This class encapsulates a GLSL shader program, including a vertex 
 * and a fragment shader. Originally based in the code by JohnG
 * (http://www.hardcorepawn.com/)
 */
public class PShader {
  // shaders constants
  static public final int FLAT     = 0;
  static public final int LIT      = 1;
  static public final int TEXTURED = 2;
  static public final int FULL     = 3;
  static public final int LINE     = 4;
  static public final int POINT    = 5;
  
  protected PApplet parent;
  // The main renderer associated to the parent PApplet.
  protected PGraphicsOpenGL pgMain;  
  // We need a reference to the renderer since a shader might
  // be called by different renderers within a single application
  // (the one corresponding to the main surface, or other offscreen
  // renderers).
  protected PGraphicsOpenGL pgCurrent;
  
  protected PGL pgl;
  protected PGL.Context context;      // The context that created this shader.

  protected URL vertexURL;
  protected URL fragmentURL;
  
  protected String vertexFilename;
  protected String fragmentFilename;

  protected String vertexShaderSource;
  protected String fragmentShaderSource;
  
  protected int programObject;
  protected int vertexShader;
  protected int fragmentShader;  

  protected boolean active;
  
  protected HashMap<Integer, UniformValue> uniformValues = null;
  
  public PShader() {
    parent = null;
    pgMain = null;
    pgl = null;
    context = null;
    
    this.vertexURL = null;
    this.fragmentURL = null;
    this.vertexFilename = null;
    this.fragmentFilename = null;    
    
    programObject = 0;
    vertexShader = 0;
    fragmentShader = 0;
    
    active = false;
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
   * @param parent PApplet
   * @param vertexFN String
   * @param fragmentFN String          
   */
  public PShader(PApplet parent, String vertFilename, String fragFilename) {
    this.parent = parent;
    pgMain = (PGraphicsOpenGL) parent.g;
    pgl = pgMain.pgl;    
    
    this.vertexURL = null;
    this.fragmentURL = null;
    this.vertexFilename = vertFilename;
    this.fragmentFilename = fragFilename;    
    
    programObject = 0;
    vertexShader = 0;
    fragmentShader = 0;      
  }
  
  
  public PShader(PApplet parent, URL vertURL, URL fragURL) {
    this.parent = parent;
    pgMain = (PGraphicsOpenGL) parent.g;
    pgl = pgMain.pgl;    

    this.vertexURL = vertURL;
    this.fragmentURL = fragURL;    
    this.vertexFilename = null;
    this.fragmentFilename = null;
    
    programObject = 0;
    vertexShader = 0;
    fragmentShader = 0;      
  }  
  
  
  protected void finalize() throws Throwable {
    try {
      if (vertexShader != 0) {
        pgMain.finalizeGLSLVertShaderObject(vertexShader, context.code());
      }
      if (fragmentShader != 0) {
        pgMain.finalizeGLSLFragShaderObject(fragmentShader, context.code());
      }
      if (programObject != 0) {
        pgMain.finalizeGLSLProgramObject(programObject, context.code());
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

  
  public void setUniform(String name, int x) {    
    setUniformImpl(name, UniformValue.INT1, new int[] { x });
  }
  
  
  public void setUniform(String name, int x, int y) {
    setUniformImpl(name, UniformValue.INT2, new int[] { x, y });
  }
  
  
  public void setUniform(String name, int x, int y, int z) {
    setUniformImpl(name, UniformValue.INT3, new int[] { x, y, z });
  }
  
  
  public void setUniform(String name, int x, int y, int z, int w) {
    setUniformImpl(name, UniformValue.INT4, new int[] { x, y, z });
  }
  
  
  public void setUniform(String name, float x) {
    setUniformImpl(name, UniformValue.FLOAT1, new float[] { x });
  }
  
  
  public void setUniform(String name, float x, float y) {
    setUniformImpl(name, UniformValue.FLOAT2, new float[] { x, y });
  }
  
  
  public void setUniform(String name, float x, float y, float z) {
    setUniformImpl(name, UniformValue.FLOAT3, new float[] { x, y, z });
  }
  
  
  public void setUniform(String name, float x, float y, float z, float w) {
    setUniformImpl(name, UniformValue.FLOAT4, new float[] { x, y, z, w });
  }
  
  
  public void setUniform(String name, PVector vec) {
    setUniformImpl(name, UniformValue.FLOAT3, new float[] { vec.x, vec.y, vec.z });
  }
  
  
  public void setUniform(String name, int[] vec) {
    setUniform(name, vec, 1);
  }   

  
  public void setUniform(String name, int[] vec, int ncoords) {
    if (ncoords == 1) {
      setUniformImpl(name, UniformValue.INT1VEC, vec);
    } else if (ncoords == 2) {
      setUniformImpl(name, UniformValue.INT2VEC, vec);
    } else if (ncoords == 3) {
      setUniformImpl(name, UniformValue.INT3VEC, vec);
    } else if (ncoords == 4) {
      setUniformImpl(name, UniformValue.INT4VEC, vec);
    } else if (4 < ncoords) {
      PGraphics.showWarning("Only up to 4 coordinates per element are supported.");
    } else {
      PGraphics.showWarning("Wrong number of coordinates: it is negative!");
    }
  }     
  
  
  public void setUniform(String name, float[] vec) {
    setUniform(name, vec, 1);
  }  
  
  
  public void setUniform(String name, float[] vec, int ncoords) {    
    if (ncoords == 1) {
      setUniformImpl(name, UniformValue.FLOAT1VEC, vec);
    } else if (ncoords == 2) {
      setUniformImpl(name, UniformValue.FLOAT2VEC, vec);
    } else if (ncoords == 3) {
      setUniformImpl(name, UniformValue.FLOAT3VEC, vec);
    } else if (ncoords == 4) {
      setUniformImpl(name, UniformValue.FLOAT4VEC, vec);
    } else if (4 < ncoords) {
      PGraphics.showWarning("Only up to 4 coordinates per element are supported.");
    } else {
      PGraphics.showWarning("Wrong number of coordinates: it is negative!");
    }
  }
  
  
  public void setUniform(String name, PMatrix2D mat) {
    float[] matv = { mat.m00, mat.m01, 
                     mat.m10, mat.m11 };
    setUniformImpl(name, UniformValue.MAT2, matv);
  }
  
  
  public void setUniform(String name, PMatrix3D mat) {
    setUniform(name, mat, false);
  }
  
  
  public void setUniform(String name, PMatrix3D mat, boolean use3x3) {
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
   * Starts the execution of the shader program.
   */
  protected void start() {    
    init();  
    pgl.glUseProgram(programObject);
    active = true;
    consumeUniforms();
  }

  
  /**
   * Stops the execution of the shader program.
   */
  protected void stop() {
    pgl.glUseProgram(0);
    active = false;
  }    
  
  
  /**
   * Returns true if the shader is running, false otherwise.
   */
  protected boolean active() {
    return active;
  }
  
  
  /**
   * Returns the ID location of the attribute parameter given its name.
   * 
   * @param name String
   * @return int
   */
  protected int getAttribLocation(String name) {
    init();    
    return pgl.glGetAttribLocation(programObject, name);
  }

  
  /**
   * Returns the ID location of the uniform parameter given its name.
   * 
   * @param name String
   * @return int
   */
  protected int getUniformLocation(String name) {
    init();
    return pgl.glGetUniformLocation(programObject, name);
  }

  
  protected void setAttribute(int loc, int vboId, int size, int type, boolean normalized, int stride, int offset) {
    if (-1 < loc) {
      pgl.glBindBuffer(PGL.GL_ARRAY_BUFFER, vboId);
      pgl.glVertexAttribPointer(loc, size, type, normalized, stride, offset);
    }
  }  
  
  
  protected void setIntUniform(int loc, int x) {
    if (-1 < loc) {
      pgl.glUniform1i(loc, x);
    }
  }  

  
  protected void setIntUniform(int loc, int x, int y) {
    if (-1 < loc) {
      pgl.glUniform2i(loc, x, y);
    }
  }    

  
  protected void setIntUniform(int loc, int x, int y, int z) {
    if (-1 < loc) {
      pgl.glUniform3i(loc, x, y, z);
    }
  }      

  
  protected void setIntUniform(int loc, int x, int y, int z, int w) {
    if (-1 < loc) {
      pgl.glUniform4i(loc, x, y, z, w);
    }
  }      
  
  
  protected void setFloatUniform(int loc, float x) {
    if (-1 < loc) {
      pgl.glUniform1f(loc, x);
    }
  }  
  
  protected void setFloatUniform(int loc, float x, float y) {
    if (-1 < loc) {
      pgl.glUniform2f(loc, x, y);  
    }
  }    
  

  protected void setFloatUniform(int loc, float x, float y, float z) {
    if (-1 < loc) {
      pgl.glUniform3f(loc, x, y, z);  
    }    
  }  

  
  protected void setFloatUniform(int loc, float x, float y, float z, float w) {
    if (-1 < loc) {
      pgl.glUniform4f(loc, x, y, z, w);
    }
  }   
  
  
  protected void setIntVecUniform(int loc, int[] vec, int ncoords) {
    if (-1 < loc) {
      if (ncoords == 1) {
        pgl.glUniform1iv(loc, vec.length, vec, 0);  
      } else if (ncoords == 2) {
        pgl.glUniform2iv(loc, vec.length / 2, vec, 0);
      } else if (ncoords == 3) {
        pgl.glUniform3iv(loc, vec.length / 3, vec, 0);
      } else if (ncoords == 4) {
        pgl.glUniform3iv(loc, vec.length / 4, vec, 0);
      }      
    }
  }  
  
  
  protected void setFloatVecUniform(int loc, float[] vec, int ncoords) {
    if (-1 < loc) {      
      if (ncoords == 1) {
        pgl.glUniform1fv(loc, vec.length, vec, 0);  
      } else if (ncoords == 2) {
        pgl.glUniform2fv(loc, vec.length / 2, vec, 0);
      } else if (ncoords == 3) {
        pgl.glUniform3fv(loc, vec.length / 3, vec, 0);
      } else if (ncoords == 4) {
        pgl.glUniform4fv(loc, vec.length / 4, vec, 0);
      }         
    }
  }
  
  
  protected void setMat2Uniform(int loc, float[] mat) {
    if (-1 < loc) {
      pgl.glUniformMatrix2fv(loc, 1, false, mat, 0);
    }
  }    
  

  protected void setMat3Uniform(int loc, float[] mat) {
    if (-1 < loc) {
      pgl.glUniformMatrix3fv(loc, 1, false, mat, 0);
    }
  }
  
  
  protected void setMat4Uniform(int loc, float[] mat) {
    if (-1 < loc) {
      pgl.glUniformMatrix4fv(loc, 1, false, mat, 0);
    }
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
    int loc = getUniformLocation(name);
    if (-1 < loc) {
      if (uniformValues == null) {
        uniformValues = new HashMap<Integer, UniformValue>();        
      }
      uniformValues.put(loc, new UniformValue(type, value));      
    } else {
      PGraphics.showWarning("The shader doesn't have the uniform " + name);
    }    
  }  
  
  
  protected void consumeUniforms() {
    if (uniformValues != null && 0 < uniformValues.size()) {
      for (Integer loc: uniformValues.keySet()) {        
        UniformValue val = uniformValues.get(loc);        
        if (val.type == UniformValue.INT1) {
          int[] v = ((int[])val.value);
          pgl.glUniform1i(loc, v[0]); 
        } else if (val.type == UniformValue.INT2) {
          int[] v = ((int[])val.value);
          pgl.glUniform2i(loc, v[0], v[1]); 
        } else if (val.type == UniformValue.INT3) {
          int[] v = ((int[])val.value);
          pgl.glUniform3i(loc, v[0], v[1], v[2]); 
        } else if (val.type == UniformValue.INT4) {
          int[] v = ((int[])val.value);
          pgl.glUniform4i(loc, v[0], v[1], v[2], v[4]); 
        } else if (val.type == UniformValue.FLOAT1) {
          float[] v = ((float[])val.value);
          pgl.glUniform1f(loc, v[0]);          
        } else if (val.type == UniformValue.FLOAT2) {
          float[] v = ((float[])val.value);
          pgl.glUniform2f(loc, v[0], v[1]);          
        } else if (val.type == UniformValue.FLOAT3) {
          float[] v = ((float[])val.value);
          pgl.glUniform3f(loc, v[0], v[1], v[2]);           
        } else if (val.type == UniformValue.FLOAT4) {
          float[] v = ((float[])val.value);
          pgl.glUniform4f(loc, v[0], v[1], v[2], v[3]);
        } else if (val.type == UniformValue.INT1VEC) {
          int[] v = ((int[])val.value);
          pgl.glUniform1iv(loc, v.length, v, 0);          
        } else if (val.type == UniformValue.INT2VEC) {
          int[] v = ((int[])val.value);
          pgl.glUniform2iv(loc, v.length / 2, v, 0);
        } else if (val.type == UniformValue.INT3VEC) {
          int[] v = ((int[])val.value);
          pgl.glUniform3iv(loc, v.length / 3, v, 0);          
        } else if (val.type == UniformValue.INT4VEC) {
          int[] v = ((int[])val.value);
          pgl.glUniform4iv(loc, v.length / 4, v, 0);          
        } else if (val.type == UniformValue.FLOAT1VEC) {
          float[] v = ((float[])val.value);
          pgl.glUniform1fv(loc, v.length, v, 0);  
        } else if (val.type == UniformValue.FLOAT2VEC) {
          float[] v = ((float[])val.value);
          pgl.glUniform2fv(loc, v.length / 2, v, 0);
        } else if (val.type == UniformValue.FLOAT3VEC) {
          float[] v = ((float[])val.value);
          pgl.glUniform3fv(loc, v.length / 3, v, 0);          
        } else if (val.type == UniformValue.FLOAT4VEC) {
          float[] v = ((float[])val.value);
          pgl.glUniform4fv(loc, v.length / 4, v, 0);          
        } else if (val.type == UniformValue.MAT2) {
          float[] v = ((float[])val.value);
          pgl.glUniformMatrix2fv(loc, 1, false, v, 0);          
        } else if (val.type == UniformValue.MAT3) {
          float[] v = ((float[])val.value);
          pgl.glUniformMatrix3fv(loc, 1, false, v, 0);          
        } else if (val.type == UniformValue.MAT4) {
          float[] v = ((float[])val.value);
          pgl.glUniformMatrix4fv(loc, 1, false, v, 0);          
        }
      }
      uniformValues.clear();
    }
  }
  
    
  protected void init() {
    if (programObject == 0 || contextIsOutdated()) {
      context = pgl.getCurrentContext();
      programObject = pgMain.createGLSLProgramObject(context.code());
      
      boolean hasVert = false;            
      if (vertexFilename != null) {
        hasVert = loadVertexShader(vertexFilename); 
      } else if (vertexURL != null) {
        hasVert = loadVertexShader(vertexURL);
      } else {
        PGraphics.showException("Vertex shader filenames and URLs are both null!");
      }
      
      boolean hasFrag = false;      
      if (fragmentFilename != null) {
        hasFrag = loadFragmentShader(fragmentFilename);
      } else if (fragmentURL != null) {      
        hasFrag = loadFragmentShader(fragmentURL);
      } else {
        PGraphics.showException("Fragment shader filenames and URLs are both null!");
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
          pgl.glAttachShader(programObject, vertexShader);
        }
        if (hasFrag) {
          pgl.glAttachShader(programObject, fragmentShader);
        }
        pgl.glLinkProgram(programObject);
        
        int[] linked = new int[1];
        pgl.glGetProgramiv(programObject, PGL.GL_LINK_STATUS, linked, 0);        
        if (linked[0] == PGL.GL_FALSE) {
          PGraphics.showException("Cannot link shader program:\n" + pgl.glGetProgramInfoLog(programObject));
        }
        
        pgl.glValidateProgram(programObject);
        
        int[] validated = new int[1];
        pgl.glGetProgramiv(programObject, PGL.GL_VALIDATE_STATUS, validated, 0);        
        if (validated[0] == PGL.GL_FALSE) {
          PGraphics.showException("Cannot validate shader program:\n" + pgl.glGetProgramInfoLog(programObject));
        }        
      }
    }
  }  
  
  
  protected boolean contextIsOutdated() {
    boolean outdated = !pgl.contextIsCurrent(context);
    if (outdated) {
      pgMain.removeGLSLProgramObject(programObject, context.code());
      pgMain.removeGLSLVertShaderObject(vertexShader, context.code());
      pgMain.removeGLSLFragShaderObject(fragmentShader, context.code());
      
      programObject = 0;
      vertexShader = 0;
      fragmentShader = 0;
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
      vertexShaderSource = PApplet.join(PApplet.loadStrings(url.openStream()), "\n");
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
      fragmentShaderSource = PApplet.join(PApplet.loadStrings(url.openStream()), "\n");
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
    vertexShader = pgMain.createGLSLVertShaderObject(context.code());
    
    pgl.glShaderSource(vertexShader, vertexShaderSource);
    pgl.glCompileShader(vertexShader);
    
    int[] compiled = new int[1];
    pgl.glGetShaderiv(vertexShader, PGL.GL_COMPILE_STATUS, compiled, 0);        
    if (compiled[0] == PGL.GL_FALSE) {
      PGraphics.showException("Cannot compile vertex shader:\n" + pgl.glGetShaderInfoLog(vertexShader));
      return false;
    } else {
      return true;
    }        
  }  
      
  
  /**
   * @param shaderSource a string containing the shader's code
   */
  protected boolean compileFragmentShader() {
    fragmentShader = pgMain.createGLSLFragShaderObject(context.code());
    
    pgl.glShaderSource(fragmentShader, fragmentShaderSource);
    pgl.glCompileShader(fragmentShader);
    
    int[] compiled = new int[1];
    pgl.glGetShaderiv(fragmentShader, PGL.GL_COMPILE_STATUS, compiled, 0);
    if (compiled[0] == PGL.GL_FALSE) {
      PGraphics.showException("Cannot compile fragment shader:\n" + pgl.glGetShaderInfoLog(fragmentShader));      
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
    if (vertexShader != 0) {
      pgMain.deleteGLSLVertShaderObject(vertexShader, context.code());
      vertexShader = 0;
    }
    if (fragmentShader != 0) {
      pgMain.deleteGLSLFragShaderObject(fragmentShader, context.code());
      fragmentShader = 0;
    }
    if (programObject != 0) {
      pgMain.deleteGLSLProgramObject(programObject, context.code());
      programObject = 0;
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
    
    int type;
    Object value;

    UniformValue(int type, Object value) {
      this.type = type;
      this.value = value;
    }    
  }  
}
