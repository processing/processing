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
 * This class encapsulates a glsl shader. Based in the code by JohnG
 * (http://www.hardcorepawn.com/)
 */
public class PShader {
  protected PApplet parent;
  protected PGraphicsAndroid3D pg; 
  protected PGL pgl;
  
  protected int programObject;
  protected int vertexShader;
  protected int fragmentShader;  
  protected boolean initialized;
  
  /**
   * Creates an instance of GLSLShader.
   * 
   * @param parent PApplet
   */
  public PShader(PApplet parent) {
    this.parent = parent;
    pg = (PGraphicsAndroid3D) parent.g;
    pgl = pg.pgl;
    
    programObject = pg.createGLSLProgramObject();  
    
    vertexShader = 0;
    fragmentShader = 0;
    initialized = false;    
  }

  /**
   * Creates a read-to-use instance of GLSLShader with vertex and fragment shaders
   * 
   * @param parent PApplet
   * @param vertexFN String
   * @param fragmentFN String          
   */
  public PShader(PApplet parent, String vertexFN, String fragmentFN) {
    this(parent);
    loadVertexShader(vertexFN);
    loadFragmentShader(fragmentFN);
    setup();
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
   * Loads and compiles the vertex shader contained in file.
   * 
   * @param file String
   */
  public void loadVertexShader(String file) {
    String shaderSource = PApplet.join(parent.loadStrings(file), "\n");
    attachVertexShader(shaderSource, file);
  }

  public void loadVertexShaderSource(String source) {
    attachVertexShader(source, "");
  }
    
  /**
   * Loads and compiles the vertex shader contained in the URL.
   * 
   * @param file String
   */
  public void loadVertexShader(URL url) {
    String shaderSource;
    try {
      shaderSource = PApplet.join(PApplet.loadStrings(url.openStream()), "\n");
      attachVertexShader(shaderSource, url.getFile());
    } catch (IOException e) {
      PGraphics.showException("Cannot load shader " + url.getFile());
    }
  }

  /**
   * Loads and compiles the fragment shader contained in file.
   * 
   * @param file String
   */
  public void loadFragmentShader(String file) {
    String shaderSource = PApplet.join(parent.loadStrings(file), "\n");
    attachFragmentShader(shaderSource, file);
  }

  public void loadFragmentShaderSource(String source) {
    attachFragmentShader(source, "");
  }  
  
  /**
   * Loads and compiles the fragment shader contained in the URL.
   * 
   * @param url URL
   */
  public void loadFragmentShader(URL url) {
    String shaderSource;
    try {
      shaderSource = PApplet.join(PApplet.loadStrings(url.openStream()), "\n");
      attachFragmentShader(shaderSource, url.getFile());
    } catch (IOException e) {
      PGraphics.showException("Cannot load shader " + url.getFile());
    }
  }
  
  /**
   * Links the shader program and validates it.
   */
  public void setup() {
    pgl.glLinkProgram(programObject);
    pgl.glValidateProgram(programObject);
    checkLogInfo("GLSL program validation: ", programObject);
    initialized = true;
  }
  
  /**
   * Returns true or false depending on whether the shader is initialized or not.
   */  
  public boolean isInitialized() {
    return initialized;
  }
  
  /**
   * Starts the execution of the shader program.
   */
  public void start() {
    if (!initialized) {
      PGraphics.showWarning("This shader is not properly initialized. Call the setup() method first");
    }
    
    // TODO:
    // set the texture uniforms to the currently values texture units for all the
    // textures.
    // gl.glUniform1iARB(loc, unit); 
    
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
    return pgl.glGetAttribLocation(programObject, name);
  }

  /**
   * Returns the ID location of the uniform parameter given its name.
   * 
   * @param name String
   * @return int
   */
  public int getUniformLocation(String name) {
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
  
  
  /**
   * @param shaderSource a string containing the shader's code
   * @param filename the shader's filename, used to print error log information
   */
  protected void attachVertexShader(String shaderSource, String file) {
    vertexShader = pg.createGLSLVertShaderObject();
    
    pgl.glShaderSource(vertexShader, shaderSource);
    pgl.glCompileShader(vertexShader);
    
    checkLogInfo("Vertex shader " + file + " compilation: ", vertexShader);
    pgl.glAttachShader(programObject, vertexShader);    
  }  
      
  /**
   * @param shaderSource a string containing the shader's code
   * @param filename the shader's filename, used to print error log information
   */
  protected void attachFragmentShader(String shaderSource, String file) {
    fragmentShader = pg.createGLSLFragShaderObject();
    
    pgl.glShaderSource(fragmentShader, shaderSource);
    pgl.glCompileShader(fragmentShader);
    
    checkLogInfo("Fragment shader " + file + " compilation: ", fragmentShader);
    pgl.glAttachShader(programObject, fragmentShader);
  }
    
  /**
   * @invisible Check the log error for the opengl object obj. Prints error
   *            message if needed.
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

