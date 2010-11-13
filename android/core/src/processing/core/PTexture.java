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

import java.lang.reflect.Method;
import java.nio.IntBuffer;

import javax.microedition.khronos.opengles.*;
import javax.microedition.khronos.egl.EGL10;

import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.opengl.GLUtils;

import java.nio.*;

/**
 * This class wraps an OpenGL texture.
 * By Andres Colubri
 * 
 */
@SuppressWarnings("unused")
public class PTexture implements PConstants { 
  public int width, height;
    
  protected PApplet parent;
  protected PGraphicsAndroid3D a3d;  
  protected GL10 gl;

  protected int glID; 
  protected int glTarget;
  protected int glFormat;
  protected int glMinFilter;  
  protected int glMagFilter;
  protected int glWrapS; 
  protected int glWrapT;
  
  protected int glWidth;
  protected int glHeight;
  
  protected boolean usingMipmaps; 
  protected float maxTexCoordU;
  protected float maxTexCoordV;
  
  protected boolean flippedX;   
  protected boolean flippedY;

  protected int[] tmpPixels = null;
  protected PFramebuffer tmpFbo = null;
  
  protected int recreateResourceIdx;
  
  ////////////////////////////////////////////////////////////
  
  // Constructors.

  
  /**
   * Creates an instance of PTexture with size width x height. The texture is
   * initialized (empty) to that size.
   * @param parent PApplet
   * @param width  int
   * @param height  int
   */	 
  public PTexture(PApplet parent, int width, int height) {
    this(parent, width, height, new Parameters());
  }
    
  
  /**
   * Creates an instance of PTexture with size width x height and with the specified parameters.
   *  The texture is initialized (empty) to that size.
   * @param parent PApplet
   * @param width int 
   * @param height int 
   * @param params Parameters 			
   */	 
  public PTexture(PApplet parent, int width, int height, Parameters params) { 
    this.parent = parent;
    this.width = width;
    this.height = height;
       
    a3d = (PGraphicsAndroid3D)parent.g;
    gl = a3d.gl;
    
    glID = 0;
    
    setParameters(params);
    createTexture(width, height);
    
    try {
      Method meth = this.getClass().getMethod("recreateResource", new Class[] { PGraphicsAndroid3D.class });
      recreateResourceIdx =  a3d.addRecreateResourceMethod(this, meth);
    } catch (Exception e) {
      recreateResourceIdx = -1;
    }        
  }	
	

  /**
   * Creates an instance of PTexture using image file filename as source.
   * @param parent PApplet
   * @param filename String
   */	
  public PTexture(PApplet parent, String filename)  {
    this(parent, filename,  new Parameters());
  }

  
  /**
   * Creates an instance of PTexture using image file filename as source and the specified texture parameters.
   * @param parent PApplet
   * @param filename String
   * @param params Parameters
   */	
  public PTexture(PApplet parent, String filename, Parameters params)  {
    this.parent = parent;
	   
    a3d = (PGraphicsAndroid3D)parent.g;
    gl = a3d.gl;	

    glID = 0;
    
    PImage img = parent.loadImage(filename);
    setParameters(params);
    set(img);
    
    try {
      Method meth = this.getClass().getMethod("recreateResource", new Class[] { PGraphicsAndroid3D.class });
      recreateResourceIdx =  a3d.addRecreateResourceMethod(this, meth);
    } catch (Exception e) {
      recreateResourceIdx = -1;
    }        
  }


  protected void finalize() {
    a3d.removeRecreateResourceMethod(recreateResourceIdx);    
    deleteTexture();
  }

  
  ////////////////////////////////////////////////////////////
  
  // Init, resize methods
  
  
  /**
   * Sets the size of the image and texture to width x height. If the texture is already initialized,
   * it first destroys the current OpenGL texture object and then creates a new one with the specified
   * size.
   * @param width int
   * @param height int
   */
  public void init(int width, int height) {
    init(width, height, new Parameters());
  }
  

  /**
   * Sets the size of the image and texture to width x height, and the parameters of the texture to params.
   * If the texture is already  initialized, it first destroys the current OpenGL texture object and then creates 
   * a new one with the specified size.
   * @param width int
   * @param height int
   * @param params GLTextureParameters 
   */
  public void init(int width, int height, Parameters params)  {
    this.width = width;
    this.height = height;    
    setParameters(params);
    createTexture(width, height);
  } 


  public void resize(int wide, int high) {
    // Creating new texture with the appropriate size.
    PTexture tex = new PTexture(parent, wide, high, getParameters());
    
    // Copying the contents of this texture into tex.
    tex.set(this);
    
    // Now, overwriting "this" with tex.
    copyObject(tex);
    
    // Zeroing the texture id of tex, so the texture is not 
    // deleted by OpenGL when the object is finalized by the GC.
    // "This" texture now wraps the one created in tex.
    tex.glID = 0;
    
    // Nullifying some utility objects so they are recreated with the appropriate
    // size when needed.
    tmpPixels = null;
    tmpFbo = null;
  }

  
  /**
   * Returns true if the texture has been initialized.
   * @return boolean
   */  
  public boolean available()  {
    return 0 < glID;
  }

  
  ////////////////////////////////////////////////////////////
  
  // Set methods

  
  public void set(PImage img) {
    set(img.texture);
  }
  
  
  public void set(PImage img, int x, int y, int w, int h) {
    set(img.texture, x, y, w, h);
  }
  
  
  public void set(PTexture tex) {
    copyTexels(tex, 0, 0, tex.width, tex.height, true);
  }
  
  
  public void set(PTexture tex, int x, int y, int w, int h) {
    copyTexels(tex, x, y, w, h, true);
  }  

  
  public void set(int[] pixels) {
    set(pixels, 0, 0, width, height, ARGB); 
  }

  
  public void set(int[] pixels, int format) {
    set(pixels, 0, 0, width, height, format); 
  }
  
  
  public void set(int[] pixels, int x, int y, int w, int h) {
    set(pixels, x, y, w, h, ARGB); 
  }
  
  
  public void set(int[] pixels, int x, int y, int w, int h, int format) {
    // TODO: Should we throw exceptions here or just a warning?
    if (pixels == null) {
      throw new RuntimeException("PTexture: null pixels array");
    }    
    if (pixels.length != w * h) {
      throw new RuntimeException("PTexture: wrong length of pixels array");
    }
    
    if (glID == 0) {
      createTexture(width, height);
    }   
    
    gl.glEnable(glTarget);
    gl.glBindTexture(glTarget, glID);
                
    if (usingMipmaps) {
      if (a3d.gl11 != null && PGraphicsAndroid3D.mipmapSupported) {
        int[] rgbaPixels = new int[w * h];
        convertToRGBA(pixels, rgbaPixels, format);
        gl.glTexParameterf(GL11.GL_TEXTURE_2D, GL11.GL_GENERATE_MIPMAP, GL11.GL_TRUE);
        setTexels(x, y, w, h, rgbaPixels);
      } else {
        
        
        /*
        // Code by Mike Miller obtained from here:
        // http://insanitydesign.com/wp/2009/08/01/android-opengl-es-mipmaps/
        // TODO: Check if this algorithm works only for pot textures or for any resolution.
        //       and adapt for copying only one texture rectangle.
        int[] argbPixels = new int[glWidth * glHeight];
        copyARGB(pixels, argbPixels);
        int level = 0;
        int w0 = glWidth;
        int h0 = glHeight;
        
        // We create a Bitmap because then we use its built-in filtered downsampling
        // functionality.
        Bitmap bitmap = Bitmap.createBitmap(w, h, Config.ARGB_8888);
        bitmap.setPixels(argbPixels, 0, w0, 0, 0, w0, h0);
        
        while (w0 >= 1 || h0 >= 1) {
          //First of all, generate the texture from our bitmap and set it to the according level
          GLUtils.texImage2D(GL10.GL_TEXTURE_2D, level, bitmap, 0);
          
          // We are done.
          if (w0 == 1 || h0 == 1) {
            break;
          }
 
          // Increase the mipmap level
          level++;
 
          // Downsampling bitmap
          h0 /= 2;
          w0 /= 2;
          Bitmap bitmap2 = Bitmap.createScaledBitmap(bitmap, w0, h0, true);
 
          // Clean up
          bitmap.recycle();
          bitmap = bitmap2;
        }
      */
        
        
        // No mipmaps for now.
        int[] rgbaPixels = new int[w * h];
        convertToRGBA(pixels, rgbaPixels, format);        
        setTexels(x, y, w, h, rgbaPixels);
      }
    } else {
      int[] rgbaPixels = new int[w * h];
      convertToRGBA(pixels, rgbaPixels, format);
      setTexels(x, y, w, h, rgbaPixels);
    }

    gl.glDisable(glTarget);
  }  

  
  ////////////////////////////////////////////////////////////
  
  // Get methods
  
  
  /**     
   * Copy texture to pixels. Involves video memory to main memory transfer (slow).
   */   
  public void get(int[] pixels) {
    // TODO: here is ok to create a new pixels array, or an error/warning
    // should be thrown instead?
    if ((pixels == null) || (pixels.length != width * height)) {
      pixels = new int[width * height];
    }
        
    int size = glWidth * glHeight;
        
    if (tmpFbo == null) {
      tmpFbo = new PFramebuffer(parent, glWidth, glHeight);
    }
    
    if (PGraphicsAndroid3D.fboSupported) {
      // Attaching the texture to the color buffer of a FBO, binding the FBO and reading the pixels
      // from the current draw buffer (which is the color buffer of the FBO).
      tmpFbo.setColorBuffer(this);
      a3d.pushFramebuffer();
      a3d.setFramebuffer(tmpFbo);
      tmpFbo.readPixels();
      a3d.popFramebuffer();
    } else {
      // Here we don't have FBOs, so the method above is of no use. What we do instead is
      // to draw the texture to the screen framebuffer, and then grab the pixels from there.      
      a3d.pushFramebuffer();
      a3d.setFramebuffer(tmpFbo);
      a3d.drawTexture(this, 0, 0, glWidth, glHeight, 0, 0, glWidth, glHeight);
      tmpFbo.readPixels();
      a3d.popFramebuffer();
    }
    
    if (tmpPixels == null) {
      tmpPixels = new int[size];
    }
    tmpFbo.getPixels(tmpPixels);
    
    convertToARGB(tmpPixels, pixels);
    if (flippedX) flipArrayOnX(pixels, 1);
    if (flippedY) flipArrayOnY(pixels, 1);    
  }

  
  ////////////////////////////////////////////////////////////
  
  // Put methods (the source texture is not resized to cover the entire
  // destination).
  
  
  public void put(PTexture tex) {
    copyTexels(tex, 0, 0, tex.width, tex.height, false);
  }  

  
  public void put(PTexture tex, int x, int y, int w, int h) {
    copyTexels(tex, x, y, w, h, false);
  }   
    
    
  ////////////////////////////////////////////////////////////     
 
  // Get OpenGL parameters
  
  
  protected int getGLWidth() {
    return glWidth;
  }
  
  
  protected int getGLHeight() {
    return glHeight;
  }  
  
  
  /**
   * Provides the ID of the OpenGL texture object.
   * @return int
   */	
  protected int getGLID()  {
    return glID;
  }

  
  /**
   * Returns the texture target.
   * @return int
   */	
  protected int getGLTarget()  {
    return glTarget;
  }    

  
  /**
   * Returns the texture internal format.
   * @return int
   */	
  protected int getGLFormat() {
    return glFormat;
  }

  
  /**
   * Returns the texture minimization filter.
   * @return int
   */	
  protected int getGLMinFilter() {
    return glMinFilter;
  }

  
  /**
   * Returns the texture magnification filter.
   * @return int
   */	
  protected int getGLMagFilter() {
    return glMagFilter;
  }
	
  /**
   * Returns the texture wrapping mode for the S coordinate.
   * @return int
   */ 
  protected int getGLWrapS() {
    return glWrapS;
  }
  
  /**
   * Returns the texture wrapping mode for the T coordinate.
   * @return int
   */ 
  protected int getGLWrapT() {
    return glWrapT;
  }
    
  /**
   * Returns true or false whether or not the texture is using mipmaps.
   * @return boolean
   */	
  protected boolean usingMipmaps()  {
    return usingMipmaps;
  }
	
  
  /**
   * Returns the maximum possible value for the texture coordinate U (horizontal).
   * @return float
   */	
  protected float getMaxTexCoordU() {
    return maxTexCoordU;
  }
	
  
  /**
   * Returns the maximum possible value for the texture coordinate V (vertical).
   * @return float
   */	
  protected float getMaxTexCoordV() {
    return maxTexCoordV;
  }
	
  
  /**
   * Returns true if the texture is flipped along the horizontal direction.
   * @return boolean;
   */	
  protected boolean isFlippedX() {
    return flippedX;
  }

  
  /**
   * Sets the texture as flipped or not flipped on the horizontal direction.
   * @param v boolean;
   */	
  protected void setFlippedX(boolean v) {
    flippedX = v;
  }	
	
  
  /**
   * Returns true if the texture is flipped along the vertical direction.
   * @return boolean;
   */	
  protected boolean isFlippedY() {
    return flippedY;
  }

  
  /**
   * Sets the texture as flipped or not flipped on the vertical direction.
   * @param v boolean;
   */	
  protected void setFlippedY(boolean v) {
    flippedY = v;
  }
    
	
  ////////////////////////////////////////////////////////////     
 
  // Utilities 
  
  // bit shifting this might be more efficient
  private int nextPowerOfTwo(int val) {
    int ret = 1;
    while (ret < val) {
      ret <<= 1;
    }
    return ret;
  }    
      
  
  /**
   * Flips intArray along the X axis.
   * @param intArray int[]
   * @param mult int
   */
  protected void flipArrayOnX(int[] intArray, int mult)  {
    int index = 0;
    int xindex = mult * (width - 1);
    for (int x = 0; x < width / 2; x++) {
      for (int y = 0; y < height; y++)  {
        int i = index + mult * y * width;
        int j = xindex + mult * y * width;

        for (int c = 0; c < mult; c++) {
          int temp = intArray[i];
          intArray[i] = intArray[j];
          intArray[j] = temp;
                 
          i++;
          j++;
        }

      }
      index += mult;
      xindex -= mult;
    }
  }
  

  /**
   * Flips intArray along the Y axis.
   * @param intArray int[]
   * @param mult int
   */
  protected void flipArrayOnY(int[] intArray, int mult) {
    int index = 0;
    int yindex = mult * (height - 1) * width;
    for (int y = 0; y < height / 2; y++) {
      for (int x = 0; x < mult * width; x++) {
        int temp = intArray[index];
        intArray[index] = intArray[yindex];
        intArray[yindex] = temp;

        index++;
        yindex++;
      }
      yindex -= mult * width * 2;
    }
	}
  
  
  /**
   * Reorders a pixel array in ARGB format into the order required by OpenGL (RGBA).
   * Both arrays are assumed to be of the same length.
   * @param intArray int[]
   * @param tIntArray int[]
   * @param arrayFormat int	 
   */
  protected void convertToRGBA(int[] intArray, int[] tIntArray, int arrayFormat)  {
    if (PGraphicsAndroid3D.BIG_ENDIAN)  {
      switch (arrayFormat) {
      case ALPHA:
                	
        for (int i = 0; i< intArray.length; i++) {
          tIntArray[i] = 0xFFFFFF00 | intArray[i];
        }
        break;

      case RGB:
                	
        for (int i = 0; i< intArray.length; i++) {
          int pixel = intArray[i];
          tIntArray[i] = (pixel << 8) | 0xff;
        }
        break;

      case ARGB:
                	
        for (int i = 0; i< intArray.length; i++) {
          int pixel = intArray[i];
          tIntArray[i] = (pixel << 8) | ((pixel >> 24) & 0xff);
        }
        break;
                    
      }       
    } else {  
      // LITTLE_ENDIAN
      // ARGB native, and RGBA opengl means ABGR on windows
      // for the most part just need to swap two components here
      // the sun.cpu.endian here might be "false", oddly enough..
      // (that's why just using an "else", rather than check for "little")
        
      switch (arrayFormat)  {    
      case ALPHA:
            	
        for (int i = 0; i< intArray.length; i++) {
          tIntArray[i] = (intArray[i] << 24) | 0x00FFFFFF;
        }
        break;

      case RGB:
                	
        for (int i = 0; i< intArray.length; i++) {
          int pixel = intArray[i];
          // needs to be ABGR, stored in memory xRGB
          // so R and B must be swapped, and the x just made FF
          tIntArray[i] = 0xff000000 |  // force opacity for good measure
                         ((pixel & 0xFF) << 16) |
                         ((pixel & 0xFF0000) >> 16) |
                         (pixel & 0x0000FF00);
        }
        break;

      case ARGB:
                    	
        for (int i = 0; i< intArray.length; i++) {
          int pixel = intArray[i];
          // needs to be ABGR stored in memory ARGB
          // so R and B must be swapped, A and G just brought back in
          tIntArray[i] = ((pixel & 0xFF) << 16) |
                         ((pixel & 0xFF0000) >> 16) |
                         (pixel & 0xFF00FF00);
        }
        break;
 
      }
    }
  }
     
  
  /**
   * Reorders a pixel array in RGBA format into ARGB. The input array must be
   * of size glWidth * glHeight, while the resulting array will be of size width * height.
   * @param intArray int[]
   * @param intArray int[]	 
   */    
  protected void convertToARGB(int[] intArray, int[] tIntArray) {
    int t = 0; 
    int p = 0;
    if (PGraphicsAndroid3D.BIG_ENDIAN) {

      for (int y = 0; y < height; y++) {
        for (int x = 0; x < width; x++) {
          int pixel = intArray[p++];
          tIntArray[t++] = (pixel >> 8) | ((pixel << 24) & 0xff);
        }
        p += glWidth - width;
      }

    } else {  
      // LITTLE_ENDIAN
      // ARGB native, and RGBA opengl means ABGR on windows
      // for the most part just need to swap two components here
      // the sun.cpu.endian here might be "false", oddly enough..
      // (that's why just using an "else", rather than check for "little")
        
      for (int y = 0; y < height; y++) {
        for (int x = 0; x < width; x++) {
          int pixel = intArray[p++];
                     
          // needs to be ARGB stored in memory ABGR (RGBA = ABGR -> ARGB) 
          // so R and B must be swapped, A and G just brought back in
          tIntArray[t++] = ((pixel & 0xFF) << 16) |
                                        ((pixel & 0xFF0000) >> 16) |
                                        (pixel & 0xFF00FF00);
                     
        }
        p += glWidth - width;
      }

    }
  }
    
  /**
   * Copies the pixel array in ARGB intArray into cIntArray. The size of the incoming 
   * array is assumed to be width * height, and the size of the returned array is 
   * glWidth * glHeight.
   * @param intArray int[]
   * @param cIntArray int[]
   */      
  protected void copyARGB(int[] intArray, int[] cIntArray) {
    int t = 0; 
    int p = 0;
    for (int y = 0; y < height; y++) {
      for (int x = 0; x < width; x++) {
        cIntArray[t++] = intArray[p++];
      }
       t += glWidth - width;
    }
  }
  
  ///////////////////////////////////////////////////////////  

  // Create/delete texture.    

    
  /**
   * Creates the opengl texture object.
   * @param w int
   * @param h int	 
   */
  protected void createTexture(int w, int h) {
    deleteTexture();
      
    if (PGraphicsAndroid3D.npotTexSupported) {
      glWidth = w;
      glHeight = h;
    } else {
      glWidth = nextPowerOfTwo(w);
      glHeight = nextPowerOfTwo(h);
    }
    
    if ((glWidth > PGraphicsAndroid3D.maxTextureSize) || (glHeight > PGraphicsAndroid3D.maxTextureSize)) {
      glWidth = glHeight = 0;
      throw new RuntimeException("Image width and height cannot be" +
                                 " larger than " + PGraphicsAndroid3D.maxTextureSize +
                                 " with this graphics card.");
    }    
    
    usingMipmaps = ((glMinFilter == GL10.GL_NEAREST_MIPMAP_NEAREST) ||
                                     (glMinFilter == GL10.GL_LINEAR_MIPMAP_NEAREST) ||
                                     (glMinFilter == GL10.GL_NEAREST_MIPMAP_LINEAR) ||
                                     (glMinFilter == GL10.GL_LINEAR_MIPMAP_LINEAR));
     
     gl.glEnable(glTarget);
     int[] tmp = new int[1];
     gl.glGenTextures(1, tmp, 0);
     glID = tmp[0];
     gl.glBindTexture(glTarget, glID);
     gl.glTexParameterf(glTarget, GL10.GL_TEXTURE_MIN_FILTER, glMinFilter);
     gl.glTexParameterf(glTarget, GL10.GL_TEXTURE_MAG_FILTER, glMagFilter);
     gl.glTexParameterf(glTarget, GL10.GL_TEXTURE_WRAP_S, glWrapS);
     gl.glTexParameterf(glTarget, GL10.GL_TEXTURE_WRAP_T, glWrapT);
     
     gl.glTexImage2D(glTarget, 0, glFormat,  glWidth,  glHeight, 0, GL10.GL_RGBA, GL10.GL_UNSIGNED_BYTE, null);
     gl.glDisable(glTarget);
        
     flippedX = false;
     flippedY = false;
 
     // If non-power-of-two textures are not supported, and the specified width or height
     // is non-power-of-two, then glWidth (glHeight) will be greater than w (h) because it
     // is chosen to be the next power of two, and this quotient will give the appropriate
     // maximum texture coordinate value given this situation.
     maxTexCoordU = (float)w / glWidth;
     maxTexCoordV = (float)h / glHeight; 
  }

    
  /**
   * Deletes the opengl texture object.
   */
  protected void deleteTexture() {
    if (glID != 0) {
      int[] tmp = { glID };
      gl.glDeleteTextures(1, tmp, 0);  
      glID = 0;
    }
  }
  

  protected void recreateResource(PGraphicsAndroid3D renderer) {
    createTexture(width, height);
  }

  
  // Copies source texture to this.
  protected void copyTexels(PTexture tex, int x, int y, int w, int h, boolean scale) {
    if (tex == null) {
      throw new RuntimeException("PTexture: source texture is null");
    }        
    
    if (tmpFbo == null) {
      tmpFbo = new PFramebuffer(parent, glWidth, glHeight);
    }
    // This texture is the color (destination) buffer of the FBO. 
    tmpFbo.setColorBuffer(this);
    tmpFbo.disableDepthTest();
    
    // FBO copy:
    a3d.pushFramebuffer();
    a3d.setFramebuffer(tmpFbo);
    if (scale) {
      // Rendering tex into "this", and scaling the source rectangle
      // to cover the entire destination region.
      a3d.drawTexture(tex, x, y, w, h, 0, 0, width, height);    
    } else {
      // Rendering tex into "this" but without scaling so the contents 
      // of the source texture fall in the corresponding texels of the
      // destination.
      a3d.drawTexture(tex, x, y, w, h, x, y, w, h);
    }
    a3d.popFramebuffer();
  }  
  
  protected void setTexels(int x, int y, int w, int h, int[] pix) {
    gl.glTexSubImage2D(glTarget, 0, x, y, w, h, GL10.GL_RGBA, GL10.GL_UNSIGNED_BYTE, IntBuffer.wrap(pix));
  }
  
  protected void bind() {
    gl.glBindTexture(glTarget, glID);
  }
  
  protected void copyObject(PTexture src) {
    a3d.removeRecreateResourceMethod(recreateResourceIdx);    
    deleteTexture();
  
    width = src.width;
    height = src.height;
    
    parent = src.parent;
    a3d = src.a3d;
    gl = src.gl;
    
    glID = src.glID;
    glTarget = src.glTarget;
    glFormat = src.glFormat;
    glMinFilter = src.glMinFilter;  
    glMagFilter = src.glMagFilter;

    glWidth= src.glWidth;
    glHeight = src.glHeight;
  
    usingMipmaps = src.usingMipmaps; 
    maxTexCoordU = src.maxTexCoordU;
    maxTexCoordV = src.maxTexCoordV;
  
    flippedX = src.flippedX;   
    flippedY = src.flippedY;

    recreateResourceIdx = src.recreateResourceIdx;
  }
    
  ///////////////////////////////////////////////////////////  
  
  // Parameter handling
  
  
  public Parameters getParameters() {
    Parameters res = new Parameters();
    
    if ( glTarget == GL10.GL_TEXTURE_2D )  {
        res.target = TEXTURE2D;
    }
    
    if (glFormat == GL10.GL_RGB)  {
      res.format = RGB;
    } else  if (glFormat == GL10.GL_RGBA) {
      res.format = ARGB;
    } else  if (glFormat == GL10.GL_ALPHA) {
      res.format = ALPHA;
    }
    
    if (glMinFilter == GL10.GL_NEAREST)  {
      res.minFilter = NEAREST;
    } else if (glMinFilter == GL10.GL_LINEAR)  {
      res.minFilter = LINEAR;
    } else if (glMinFilter == GL10.GL_NEAREST_MIPMAP_NEAREST) {
      res.minFilter = NEAREST_MIPMAP_NEAREST;
    } else if (glMinFilter == GL10.GL_LINEAR_MIPMAP_NEAREST) {
      res.minFilter = LINEAR_MIPMAP_NEAREST;
    } else if (glMinFilter == GL10.GL_NEAREST_MIPMAP_LINEAR) {
      res.minFilter = NEAREST_MIPMAP_LINEAR;
    } else if (glMinFilter == GL10.GL_LINEAR_MIPMAP_LINEAR) {
      res.minFilter = LINEAR_MIPMAP_LINEAR;
    }
    
    if (glMagFilter == GL10.GL_NEAREST)  {
      res.magFilter = NEAREST;
    } else if (glMagFilter == GL10.GL_LINEAR) {
      res.magFilter = LINEAR;
    }

    if (glWrapS == GL10.GL_CLAMP_TO_EDGE) {
      res.wrapU = CLAMP;  
    } else if (glWrapS == GL10.GL_REPEAT) {
      res.wrapU = REPEAT;
    }

    if (glWrapT == GL10.GL_CLAMP_TO_EDGE) {
      res.wrapV = CLAMP;  
    } else if (glWrapT == GL10.GL_REPEAT) {
      res.wrapV = REPEAT;
    }
    
    return res;
  }
  
  
  /**
   * Sets texture target and internal format according to the target and  type specified.
   * @param target int		   
   * @param params GLTextureParameters
   */		
  protected void setParameters(Parameters params) {    
	  if (params.target == TEXTURE2D)  {
        glTarget = GL10.GL_TEXTURE_2D;
    } else {
      throw new RuntimeException("GTexture: Unknown texture target");	    
	  }
	  
    if (params.format == RGB)  {
      glFormat = GL10.GL_RGB;
    } else  if (params.format == ARGB) {
      glFormat = GL10.GL_RGBA;
    } else  if (params.format == ALPHA) {
      glFormat = GL10.GL_ALPHA;
    } else {
      throw new RuntimeException("GTexture: Unknown texture format");     
    }
    
    if (params.minFilter == NEAREST)  {
      glMinFilter = GL10.GL_NEAREST;
    } else if (params.minFilter == LINEAR)  {
      glMinFilter = GL10.GL_LINEAR;
	  } else if (params.minFilter == NEAREST_MIPMAP_NEAREST) {
      glMinFilter = GL10.GL_NEAREST_MIPMAP_NEAREST;
	  } else if (params.minFilter == LINEAR_MIPMAP_NEAREST) {
      glMinFilter = GL10.GL_LINEAR_MIPMAP_NEAREST;
	  } else if (params.minFilter == NEAREST_MIPMAP_LINEAR) {
      glMinFilter = GL10.GL_NEAREST_MIPMAP_LINEAR;
	  } else if (params.minFilter == LINEAR_MIPMAP_LINEAR) {
      glMinFilter = GL10.GL_LINEAR_MIPMAP_LINEAR;
    } else {
      throw new RuntimeException("GTexture: Unknown minimization filter");     
    }
    
    if (params.magFilter == NEAREST) {
      glMagFilter = GL10.GL_NEAREST;
    } else if (params.magFilter == LINEAR)  {
      glMagFilter = GL10.GL_LINEAR;
    } else {
      throw new RuntimeException("GTexture: Unknown magnification filter");     
    }
    
    if (params.wrapU == CLAMP) {
      glWrapS = GL10.GL_CLAMP_TO_EDGE;  
    } else if (params.wrapU == REPEAT)  {
      glWrapS = GL10.GL_REPEAT;
    } else {
      throw new RuntimeException("GTexture: Unknown wrapping mode");     
    }
    
    if (params.wrapV == CLAMP) {
      glWrapT = GL10.GL_CLAMP_TO_EDGE;  
    } else if (params.wrapV == REPEAT)  {
      glWrapT = GL10.GL_REPEAT;
    } else {
      throw new RuntimeException("GTexture: Unknown wrapping mode");     
    }
  }	


  /////////////////////////////////////////////////////////////////////////// 

  // Parameters object  
  
  
  static public Parameters newParameters() {
    return new Parameters();  
  }
  

  static public Parameters newParameters(int format) {
    return new Parameters(format);  
  }    

  
  static public Parameters newParameters(int format, int filter) {
    return new Parameters(format, filter);  
  }        
      
  
  /**
   * This class stores the parameters for a texture: target, internal format, minimization filter
   * and magnification filter. 
   */
  static public class Parameters {
    /**
     * Texture target.
     */
    public int target;
      
    /**
     * Texture internal format.
     */
    public int format;
      
    /**
     * Texture minimization filter.
     */
    public int minFilter;
      
    /**
     * Texture magnification filter.
     */
    public int magFilter; 
    
    /**
     * Wrapping mode along U.
     */    
    public int wrapU;
    
    /**
     * Wrapping mode along V.
     */    
    public int wrapV;
    
    /**
     * Creates an instance of GLTextureParameters, setting all the parameters to default values.
     */
    public Parameters() {
      target = TEXTURE2D;
      format = ARGB;
      minFilter = LINEAR;
      magFilter = LINEAR;
      wrapU = CLAMP;
      wrapV = CLAMP;
    }
      
    public Parameters(int format) {
      target = TEXTURE2D;
      this.format = format;
      minFilter = LINEAR;
      magFilter = LINEAR;   
      wrapU = CLAMP;
      wrapV = CLAMP;
    }

    public Parameters(int format, int filter) {
      target = PTexture.TEXTURE2D;
      this.format = format;
      minFilter = filter;
      magFilter = filter;
      wrapU = CLAMP;
      wrapV = CLAMP;      
    }
  }
}
