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

  protected URL vertexURL;
  protected URL fragmentURL;
  
  protected String vertexFilename;
  protected String fragmentFilename;
  
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
    pgl.glUniform1i(loc, x);
  }  

  
  public void set1FloatUniform(int loc, float x) {
    pgl.glUniform1f(loc, x);
  }  
  
  
  public void set2FloatUniform(int loc, float x, float y) {
    pgl.glUniform2f(loc, x, y);
  }    
  

  public void set3FloatUniform(int loc, float x, float y, float z) {
    pgl.glUniform3f(loc, x, y, z);
  }  

  
  public void set4FloatUniform(int loc, float x, float y, float z, float w) {
    pgl.glUniform4f(loc, x, y, z, w);
  }   
  
  
  public void set1FloatVecUniform(int loc, float[] vec) {
    pgl.glUniform1fv(loc, vec.length, vec, 0);
  }
  
  
  public void set2FloatVecUniform(int loc, float[] vec) {
    pgl.glUniform2fv(loc, vec.length / 2, vec, 0);
  }

  
  public void set3FloatVecUniform(int loc, float[] vec) {
    pgl.glUniform3fv(loc, vec.length / 3, vec, 0);
  }  

  
  public void set4FloatVecUniform(int loc, float[] vec) {
    pgl.glUniform4fv(loc, vec.length / 4, vec, 0);
  }    
  
  
  public void set2x2MatUniform(int loc, float[] mat) {
    pgl.glUniformMatrix2fv(loc, 1, false, mat, 0);    
  }    
  

  public void set3x3MatUniform(int loc, float[] mat) {
    pgl.glUniformMatrix3fv(loc, 1, false, mat, 0);     
  }
  
  
  public void set4x4MatUniform(int loc, float[] mat) {
    pgl.glUniformMatrix4fv(loc, 1, false, mat, 0); 
  }    

  
  public void set1FloatAttribute(int loc, float x) {
    pgl.glVertexAttrib1f(loc, x);
  }  
  

  public void set2FloatAttribute(int loc, float x, float y) {
    pgl.glVertexAttrib2f(loc, x, y);
  }    
  

  public void set3FloatAttribute(int loc, float x, float y, float z) {
    pgl.glVertexAttrib3f(loc, x, y, z);
  }    
  

  public void set4FloatAttribute(int loc, float x, float y, float z, float w) {
    pgl.glVertexAttrib4f(loc, x, y, z, w);
  }

    
  protected void init() {
    if (programObject == 0) {
      programObject = pg.createGLSLProgramObject();
      
      if (vertexFilename != null && fragmentFilename != null) {
        loadVertexShader(vertexFilename);
        loadFragmentShader(fragmentFilename);
      } else if (vertexURL != null && fragmentURL != null) {
        loadVertexShader(vertexURL);
        loadFragmentShader(fragmentURL);        
      } else {
        PGraphics.showException("Shader filenames and URLs are both null!");
      }
      
      checkLogInfo("Vertex shader " + vertexFilename + " compilation: ", vertexShader);
      checkLogInfo("Fragment shader " + fragmentFilename + " compilation: ", fragmentShader);
      
      pgl.glLinkProgram(programObject);
      pgl.glValidateProgram(programObject);
      checkLogInfo("GLSL program validation: ", programObject);
    }
  }  
  
  
  /**
   * Loads and compiles the vertex shader contained in file.
   * 
   * @param file String
   */
  protected void loadVertexShader(String filename) {
    String shaderSource = PApplet.join(parent.loadStrings(filename), "\n");
    attachVertexShader(shaderSource);
  }

  /**
   * Loads and compiles the vertex shader contained in the URL.
   * 
   * @param file String
   */
  protected void loadVertexShader(URL url) {
    try {
      String shaderSource = PApplet.join(PApplet.loadStrings(url.openStream()), "\n");
      attachVertexShader(shaderSource);
    } catch (IOException e) {
      PGraphics.showException("Cannot load shader " + url.getFile());
    }
  }  
    
  /**
   * Loads and compiles the fragment shader contained in file.
   * 
   * @param file String
   */
  protected void loadFragmentShader(String filename) {
    String shaderSource = PApplet.join(parent.loadStrings(filename), "\n");
    attachFragmentShader(shaderSource);
  }  
  
  /**
   * Loads and compiles the fragment shader contained in the URL.
   * 
   * @param url URL
   */
  protected void loadFragmentShader(URL url) {
    try {
      String shaderSource = PApplet.join(PApplet.loadStrings(url.openStream()), "\n");
      attachFragmentShader(shaderSource);
    } catch (IOException e) {
      PGraphics.showException("Cannot load shader " + url.getFile());
    }
  }  
  
  /**
   * @param shaderSource a string containing the shader's code
   */
  protected void attachVertexShader(String shaderSource) {
    vertexShader = pg.createGLSLVertShaderObject();
    
    pgl.glShaderSource(vertexShader, shaderSource);
    pgl.glCompileShader(vertexShader);
        
    pgl.glAttachShader(programObject, vertexShader);    
  }  
      
  
  /**
   * @param shaderSource a string containing the shader's code
   */
  protected void attachFragmentShader(String shaderSource) {
    fragmentShader = pg.createGLSLFragShaderObject();
    
    pgl.glShaderSource(fragmentShader, shaderSource);
    pgl.glCompileShader(fragmentShader);
        
    pgl.glAttachShader(programObject, fragmentShader);
  }
    
  
  /**
   * Check the log error for the opengl object obj. Prints error
   * message if needed.
   */
  protected void checkLogInfo(String title, int obj) {    
    String log = pgl.getShaderLog(obj);    
    if (!log.equals("")) {
      System.out.println(title);
      System.out.println(log);
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

