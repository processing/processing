/* -*- mode: java; c-basic-offset: 2; indent-tabs-mode: nil -*- */

/*
  Part of the Processing project - http://processing.org

  Copyright (c) 2011 Andres Colubri
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

import java.io.IOException;
import java.net.URL;


/**
 * This class encapsulates a GLSL shader program, including a vertex 
 * and a fragment shader. Originally based in the code by JohnG
 * (http://www.hardcorepawn.com/)
 */
public class PShader {
  protected PApplet parent;
  protected PGraphicsAndroid3D pg; 
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

  public PShader() {
    parent = null;
    pg = null;
    pgl = null;    
    
    this.vertexURL = null;
    this.fragmentURL = null;
    this.vertexFilename = null;
    this.fragmentFilename = null;    
    
    programObject = 0;
    vertexShader = 0;
    fragmentShader = 0;      
  }
    
  public PShader(PApplet parent) {
    this();
    this.parent = parent;
    pg = (PGraphicsAndroid3D) parent.g;
    pgl = pg.pgl;     
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
    pg = (PGraphicsAndroid3D) parent.g;
    pgl = pg.pgl;    
    
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
    pg = (PGraphicsAndroid3D) parent.g;
    pgl = pg.pgl;    

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
        pg.finalizeGLSLVertShaderObject(vertexShader);
      }
      if (fragmentShader != 0) {
        pg.finalizeGLSLFragShaderObject(fragmentShader);
      }
      if (programObject != 0) {
        pg.finalizeGLSLProgramObject(programObject);
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
   * Starts the execution of the shader program.
   */
  public void start() {    
    init();  
    pgl.glUseProgram(programObject);
  }

  
  /**
   * Stops the execution of the shader program.
   */
  public void stop() {
    pgl.glUseProgram(0);
  }    
  
  
  /**
   * Returns the ID location of the attribute parameter given its name.
   * 
   * @param name String
   * @return int
   */
  public int getAttribLocation(String name) {
    init();    
    return pgl.glGetAttribLocation(programObject, name);
  }

  
  /**
   * Returns the ID location of the uniform parameter given its name.
   * 
   * @param name String
   * @return int
   */
  public int getUniformLocation(String name) {
    init();
    return pgl.glGetUniformLocation(programObject, name);
  }

  
  public void setIntUniform(int loc, int x) {
    if (-1 < loc) {
      pgl.glUniform1i(loc, x);
    }
  }  

  
  public void set1FloatUniform(int loc, float x) {
    if (-1 < loc) {
      pgl.glUniform1f(loc, x);
    }
  }  
  
  
  public void set2FloatUniform(int loc, float x, float y) {
    if (-1 < loc) {
      pgl.glUniform2f(loc, x, y);  
    }
  }    
  

  public void set3FloatUniform(int loc, float x, float y, float z) {
    if (-1 < loc) {
      pgl.glUniform3f(loc, x, y, z);  
    }    
  }  

  
  public void set4FloatUniform(int loc, float x, float y, float z, float w) {
    if (-1 < loc) {
      pgl.glUniform4f(loc, x, y, z, w);
    }
  }   
  
  
  public void set1FloatVecUniform(int loc, float[] vec) {
    if (-1 < loc) {
      pgl.glUniform1fv(loc, vec.length, vec, 0);
    }
  }
  
  
  public void set2FloatVecUniform(int loc, float[] vec) {
    if (-1 < loc) {
      pgl.glUniform2fv(loc, vec.length / 2, vec, 0);
    }
  }

  
  public void set3FloatVecUniform(int loc, float[] vec) {
    if (-1 < loc) {
      pgl.glUniform3fv(loc, vec.length / 3, vec, 0);
    }
  }  

  
  public void set4FloatVecUniform(int loc, float[] vec) {
    if (-1 < loc) {
      pgl.glUniform4fv(loc, vec.length / 4, vec, 0);
    }
  }    
  
  
  public void set2x2MatUniform(int loc, float[] mat) {
    if (-1 < loc) {
      pgl.glUniformMatrix2fv(loc, 1, false, mat, 0);
    }
  }    
  

  public void set3x3MatUniform(int loc, float[] mat) {
    if (-1 < loc) {
      pgl.glUniformMatrix3fv(loc, 1, false, mat, 0);
    }
  }
  
  
  public void set4x4MatUniform(int loc, float[] mat) {
    if (-1 < loc) {
      pgl.glUniformMatrix4fv(loc, 1, false, mat, 0);
    }
  }    

  
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
    
  protected void init() {
    if (programObject == 0 || contextIsOutdated()) {      
      context = pgl.getContext();
      programObject = pg.createGLSLProgramObject();
      
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
    vertexShader = pg.createGLSLVertShaderObject();
    
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
    fragmentShader = pg.createGLSLFragShaderObject();
    
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

  
  protected void release() {
    if (vertexShader != 0) {
      pg.deleteGLSLVertShaderObject(vertexShader);
      vertexShader = 0;
    }
    if (fragmentShader != 0) {
      pg.deleteGLSLFragShaderObject(fragmentShader);
      fragmentShader = 0;
    }
    if (programObject != 0) {
      pg.deleteGLSLProgramObject(programObject);
      programObject = 0;
    }
  }  
}