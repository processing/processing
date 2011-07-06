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
import java.nio.ByteBuffer;
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
    
  protected PApplet parent;         // The Processing applet
  protected PGraphicsAndroid3D a3d; // The main renderer
  protected PImage img;             // The parent image

  public int glID; 
  public int glTarget;
  public int glFormat;
  public int glMinFilter;  
  public int glMagFilter;
  public int glWrapS; 
  public int glWrapT;  
  public int glWidth;
  public int glHeight;
  
  protected boolean usingMipmaps; 
  protected float maxTexCoordU;
  protected float maxTexCoordV;
  
  protected boolean flippedX;   
  protected boolean flippedY;

  protected int[] tempPixels = null;
  protected PFramebuffer tempFbo = null;
  
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
    a3d.registerPGLObject(this);
    
    glID = 0;
    
    init(width, height, (Parameters)params);       
  } 


  public void delete() {    
    release();
    img = null;
    a3d.unregisterPGLObject(this);
  }
  
  
  public void backup() {
    if (img != null) {
      img.loadPixels();
      if (img.pixels != null && (img instanceof PGraphicsAndroid3D)) {
        // When img is an offscreen renderer, the loadPixels() call above 
        // already takes care of copying the contents of the color buffer 
        // to  the pixels array.
        get(img.pixels);
      }
    }        
  }

  
  public void restore() {    
    if (img != null && img.pixels != null) {
      set(img.pixels);
    }    
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
    Parameters params;
    if (0 < glID) {
      // Re-initializing a pre-existing texture.
      // We use the current parameters as default:
      params = getParameters();      
    } else {
      // Just built-in default parameters otherwise:
      params = new Parameters();
    }    
    init(width, height, params);
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
    setParameters(params);
    setSize(width, height);
    allocate();
  } 


  public void resize(int wide, int high) {
    // Creating new texture with the appropriate size.
    PTexture tex = new PTexture(parent, wide, high, getParameters());
    
    // Copying the contents of this texture into tex.
    tex.set(this);
    
    // Releasing the opengl resources associated to "this".
    this.delete();
    
    // Now, overwriting "this" with tex.
    copyObject(tex);
    
    // Nullifying some utility objects so they are recreated with the appropriate
    // size when needed.
    tempPixels = null;
    tempFbo = null;
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
    PTexture tex = (PTexture)img.getCache(a3d);
    set(tex);
  }
  
  
  public void set(PImage img, int x, int y, int w, int h) {
    PTexture tex = (PTexture)img.getCache(a3d);
    set(tex, x, y, w, h);
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
    
    getGl().glEnable(glTarget);
    getGl().glBindTexture(glTarget, glID);
                
    if (usingMipmaps) {
      if (a3d.gl11 != null && PGraphicsAndroid3D.mipmapGeneration) {
        // Automatic mipmap generation.
        int[] rgbaPixels = new int[w * h];
        convertToRGBA(pixels, rgbaPixels, format, w, h);
        getGl().glTexParameterf(GL11.GL_TEXTURE_2D, GL11.GL_GENERATE_MIPMAP, GL11.GL_TRUE);
        setTexels(x, y, w, h, rgbaPixels);
        rgbaPixels = null;
      } else {
        // Manual mipmap generation.
        if (w != width || h != height) {
          System.err.println("Sorry but I don't know how to generate mipmaps for a subregion.");
          return;
        }
        
        // Code by Mike Miller obtained from here:
        // http://insanitydesign.com/wp/2009/08/01/android-opengl-es-mipmaps/
        int w0 = glWidth;
        int h0 = glHeight;        
        int[] argbPixels = new int[w0 * h0];
        convertToARGB(pixels, argbPixels, format);
        int level = 0;
        int denom = 1;
        
        // We create a Bitmap because then we use its built-in filtered downsampling
        // functionality.
        Bitmap bitmap = Bitmap.createBitmap(w0, h0, Config.ARGB_8888);
        bitmap.setPixels(argbPixels, 0, w0, 0, 0, w0, h0);
              
        while (w0 >= 1 || h0 >= 1) {
          //First of all, generate the texture from our bitmap and set it to the according level
          GLUtils.texImage2D(glTarget, level, bitmap, 0);
          
          // We are done.
          if (w0 == 1 && h0 == 1) {
            break;
          }
 
          // Increase the mipmap level
          level++;
          denom *= 2;
 
          // Downsampling bitmap. We must eventually arrive to the 1x1 level,
          // and if the width and height are different, there will be a few 1D
          // texture levels just before. 
          // This update formula also allows for NPOT resolutions.
          w0 = PApplet.max(1, PApplet.floor((float)glWidth / denom));
          h0 = PApplet.max(1, PApplet.floor((float)glHeight / denom));
          Bitmap bitmap2 = Bitmap.createScaledBitmap(bitmap, w0, h0, true);
 
          // Clean up
          bitmap.recycle();
          bitmap = bitmap2;
          bitmap2 = null;          
        }
        argbPixels = null;
        bitmap = null;
      }
    } else {
      int[] rgbaPixels = new int[w * h];
      convertToRGBA(pixels, rgbaPixels, format, w, h);
      setTexels(x, y, w, h, rgbaPixels);
      rgbaPixels = null;
    }

    getGl().glBindTexture(glTarget, 0);
    getGl().glDisable(glTarget);
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
        
    if (tempFbo == null) {
      tempFbo = new PFramebuffer(parent, glWidth, glHeight);
    }
    
    if (PGraphicsAndroid3D.fboSupported) {
      // Attaching the texture to the color buffer of a FBO, binding the FBO and reading the pixels
      // from the current draw buffer (which is the color buffer of the FBO).
      tempFbo.setColorBuffer(this);
      a3d.pushFramebuffer();
      a3d.setFramebuffer(tempFbo);
      tempFbo.readPixels();
      a3d.popFramebuffer();
    } else {
      // Here we don't have FBOs, so the method above is of no use. What we do instead is
      // to draw the texture to the screen framebuffer, and then grab the pixels from there.      
      a3d.pushFramebuffer();
      a3d.setFramebuffer(tempFbo);
      a3d.drawTexture(this, 0, 0, glWidth, glHeight, 0, 0, glWidth, glHeight);
      tempFbo.readPixels();
      a3d.popFramebuffer();
    }
    
    if (tempPixels == null) {
      tempPixels = new int[size];
    }
    tempFbo.getPixels(tempPixels);
    
    convertToARGB(tempPixels, pixels);
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
  
    
  /**
   * Returns true or false whether or not the texture is using mipmaps.
   * @return boolean
   */ 
  public boolean usingMipmaps()  {
    return usingMipmaps;
  }
  
  
  /**
   * Returns the maximum possible value for the texture coordinate U (horizontal).
   * @return float
   */ 
  public float getMaxTexCoordU() {
    return maxTexCoordU;
  }
  
  
  /**
   * Returns the maximum possible value for the texture coordinate V (vertical).
   * @return float
   */ 
  public float getMaxTexCoordV() {
    return maxTexCoordV;
  }
  
  
  /**
   * Returns true if the texture is flipped along the horizontal direction.
   * @return boolean;
   */ 
  public boolean isFlippedX() {
    return flippedX;
  }

  
  /**
   * Sets the texture as flipped or not flipped on the horizontal direction.
   * @param v boolean;
   */ 
  public void setFlippedX(boolean v) {
    flippedX = v;
  } 
  
  
  /**
   * Returns true if the texture is flipped along the vertical direction.
   * @return boolean;
   */ 
  public boolean isFlippedY() {
    return flippedY;
  }

  
  /**
   * Sets the texture as flipped or not flipped on the vertical direction.
   * @param v boolean;
   */ 
  public void setFlippedY(boolean v) {
    flippedY = v;
  }
    
  ////////////////////////////////////////////////////////////     
  
  // Bind/unbind  
  
  public void bind() {
    getGl().glEnable(glTarget);
    getGl().glBindTexture(glTarget, glID);
  }
  
  public void unbind() {
    getGl().glEnable(glTarget);
    getGl().glBindTexture(glTarget, 0);    
  }    
    
  ////////////////////////////////////////////////////////////     
 
  // Utilities 
  
  // bit shifting this might be more efficient
  protected int nextPowerOfTwo(int val) {
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
   * Reorders a pixel array in the given format into the order required by OpenGL (RGBA).
   * Both arrays are assumed to be of the same length. The width and height parameters
   * are used in the YUV420 to RBGBA conversion.
   * @param intArray int[]
   * @param tIntArray int[]
   * @param arrayFormat int  
   * @param w int
   * @param h int
   */
  protected void convertToRGBA(int[] intArray, int[] tIntArray, int arrayFormat, int w, int h)  {
    if (PGraphicsAndroid3D.BIG_ENDIAN)  {
      switch (arrayFormat) {
      case ALPHA:
                  
        // Converting from xxxA into RGBA. RGB is set to white 
        // (0xFFFFFF, i.e.: (255, 255, 255))
        for (int i = 0; i< intArray.length; i++) {
          tIntArray[i] = 0xFFFFFF00 | intArray[i];
        }
        break;

      case RGB:
                  
        // Converting xRGB into RGBA. A is set to 0xFF (255, full opacity).
        for (int i = 0; i< intArray.length; i++) {
          int pixel = intArray[i];
          tIntArray[i] = (pixel << 8) | 0xFF;
        }
        break;

      case ARGB:
               
        // Converting ARGB into RGBA. Shifting RGB to 8 bits to the left,
        // and bringing A to the first byte.
        for (int i = 0; i< intArray.length; i++) {
          int pixel = intArray[i];
          tIntArray[i] = (pixel << 8) | ((pixel >> 24) & 0xFF);
        }
        break;
                 
      case YUV420:
        
        // YUV420 to RGBA conversion.
        int frameSize = w * h;
        for (int j = 0, yp = 0; j < h; j++) {       
          int uvp = frameSize + (j >> 1) * w, u = 0, v = 0;
          for (int i = 0; i < w; i++, yp++) {
            int y = (0xFF & ((int) intArray[yp])) - 16;
            if (y < 0) y = 0;
            if ((i & 1) == 0) {
              v = (0xFF & intArray[uvp++]) - 128;
              u = (0xFF & intArray[uvp++]) - 128;
            }

            int y1192 = 1192 * y;
            int r = (y1192 + 1634 * v);
            int g = (y1192 - 833 * v - 400 * u);
            int b = (y1192 + 2066 * u);

            if (r < 0) r = 0; else if (r > 262143) r = 262143;
            if (g < 0) g = 0; else if (g > 262143) g = 262143;
            if (b < 0) b = 0; else if (b > 262143) b = 262143;

            // Output is RGBA:
            tIntArray[yp] = ((r << 6) & 0xFF000000) | ((g >> 2) & 0xFF0000) | ((b >> 10) & 0xFF00) | 0xFF;
          }
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
              
        // Converting xxxA into ARGB, with RGB set to white.
        for (int i = 0; i< intArray.length; i++) {
          tIntArray[i] = (intArray[i] << 24) | 0x00FFFFFF;
        }
        break;

      case RGB:
              
        // We need to convert xRGB into ABGR,
        // so R and B must be swapped, and the x just made 0xFF.
        for (int i = 0; i< intArray.length; i++) {
          int pixel = intArray[i];  
          tIntArray[i] = 0xFF000000 |
                         ((pixel & 0xFF) << 16) |
                         ((pixel & 0xFF0000) >> 16) |
                         (pixel & 0x0000FF00);
        }
        break;

      case ARGB:
                      
        // We need to convert ARGB into ABGR,
        // so R and B must be swapped, A and G just brought back in.        
        for (int i = 0; i < intArray.length; i++) {
          int pixel = intArray[i];
          tIntArray[i] = ((pixel & 0xFF) << 16) |
                         ((pixel & 0xFF0000) >> 16) |
                         (pixel & 0xFF00FF00);
        }
        break;
        
      case YUV420:
        
        // YUV420 to ABGR conversion.
        int frameSize = w * h;
        for (int j = 0, yp = 0; j < h; j++) {       
          int uvp = frameSize + (j >> 1) * w, u = 0, v = 0;
          for (int i = 0; i < w; i++, yp++) {
            int y = (0xFF & ((int) intArray[yp])) - 16;
            if (y < 0) y = 0;
            if ((i & 1) == 0) {
              v = (0xFF & intArray[uvp++]) - 128;
              u = (0xFF & intArray[uvp++]) - 128;
            }

            int y1192 = 1192 * y;
            int r = (y1192 + 1634 * v);
            int g = (y1192 - 833 * v - 400 * u);
            int b = (y1192 + 2066 * u);

            if (r < 0) r = 0; else if (r > 262143) r = 262143;
            if (g < 0) g = 0; else if (g > 262143) g = 262143;
            if (b < 0) b = 0; else if (b > 262143) b = 262143;

            // Output is ABGR:
            tIntArray[yp] = 0xFF000000 | ((b << 6) & 0xFF0000) | ((g >> 2) & 0xFF00) | ((r >> 10) & 0xFF);
          }
        }        
        
        break;
      }
        
    }
  }
     
  
  /**
   * Reorders a pixel array in a given format into ARGB. The input array must be
   * of size width * height, while the output array must be of glWidth * glHeight.
   * @param intArray int[]
   * @param intArray int[]   
   * @param arrayFormat int
   */    
  protected void convertToARGB(int[] intArray, int[] tIntArray, int arrayFormat) {
    int t = 0; 
    int p = 0;
    int pixel;
    
    switch (arrayFormat) {
    case ALPHA:
                
      // xxxA to ARGB, setting RGB to black.
      for (int y = 0; y < height; y++) {
        for (int x = 0; x < width; x++) {
          pixel = intArray[p++];
          tIntArray[t++] = (pixel << 24) & 0xFF000000;
        }
        t += glWidth - width;
      }
      
      break;

    case RGB:
             
      // xRGB to ARGB, setting A to be 0xFF.
      for (int y = 0; y < height; y++) {
        for (int x = 0; x < width; x++) {
          pixel = intArray[p++];
          tIntArray[t++] = pixel | 0xFF000000;
        }
        t += glWidth - width;
      }      
      
      break;

    case ARGB:
              
      // ARGB to ARGB, where the source is smaller than the destination.
      for (int y = 0; y < height; y++) {
        PApplet.arrayCopy(intArray, width * y, tIntArray, glWidth * y, width);
      }        
      
      break;
               
    case YUV420:
      
      // YUV420 to ARGB conversion.
      int frameSize = width * height;
      for (int j = 0, yp = 0, yt = 0; j < height; j++) {       
        int uvp = frameSize + (j >> 1) * width, u = 0, v = 0;
        for (int i = 0; i < width; i++, yp++) {
          int y = (0xFF & ((int) intArray[yp])) - 16;
          if (y < 0) y = 0;
          if ((i & 1) == 0) {
            v = (0xFF & intArray[uvp++]) - 128;
            u = (0xFF & intArray[uvp++]) - 128;
          }

          int y1192 = 1192 * y;
          int r = (y1192 + 1634 * v);
          int g = (y1192 - 833 * v - 400 * u);
          int b = (y1192 + 2066 * u);

          if (r < 0) r = 0; else if (r > 262143) r = 262143;
          if (g < 0) g = 0; else if (g > 262143) g = 262143;
          if (b < 0) b = 0; else if (b > 262143) b = 262143;

          // Output is ARGB:
          tIntArray[yt++] = 0xFF000000 | ((r << 6) & 0xFF0000) | ((g >> 2) & 0xFF00) | ((b >> 10) & 0xFF);
        }
        yt += glWidth - width;
      }        
      
      break;        
    }

  }

  
  /**
   * Reorders an OpenGL pixel array (RGBA) into ARGB. The input array must be
   * of size glWidth * glHeight, while the resulting array of size width * height.
   * @param intArray int[]
   * @param intArray int[]       
   */    
  protected void convertToARGB(int[] intArray, int[] tIntArray) {
    int t = 0; 
    int p = 0;
    if (PGraphicsAndroid3D.BIG_ENDIAN) {

      // RGBA to ARGB conversion: shifting RGB 8 bits to the right,
      // and placing A 24 bits to the left.
      for (int y = 0; y < height; y++) {
        for (int x = 0; x < width; x++) {
          int pixel = intArray[p++];
          tIntArray[t++] = (pixel >> 8) | ((pixel << 24) & 0xFF000000);
        }
        p += glWidth - width;
      }

    } else {  

      // We have to convert ABGR into ARGB, so R and B must be swapped, 
      // A and G just brought back in.      
      for (int y = 0; y < height; y++) {
        for (int x = 0; x < width; x++) {
          int pixel = intArray[p++];
          tIntArray[t++] = ((pixel & 0xFF) << 16) |
                           ((pixel & 0xFF0000) >> 16) |
                           (pixel & 0xFF00FF00);
                     
        }
        p += glWidth - width;
      }

    }
  }  
  
  
  ///////////////////////////////////////////////////////////  

  // Allocate/release texture.    
 

  protected void setSize(int w, int h) {
    width = w;
    height = h;
    
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
    
    // If non-power-of-two textures are not supported, and the specified width or height
    // is non-power-of-two, then glWidth (glHeight) will be greater than w (h) because it
    // is chosen to be the next power of two, and this quotient will give the appropriate
    // maximum texture coordinate value given this situation.
    maxTexCoordU = (float)w / glWidth;
    maxTexCoordV = (float)h / glHeight;  
  }
  
    
  /**
   * Creates the opengl texture object.
   */
  protected void allocate() {
    release(); // Just in the case this object is being re-initialized.
     
    getGl().glEnable(glTarget);
    glID = a3d.createGLResource(PGraphicsAndroid3D.GL_TEXTURE_OBJECT);     
    getGl().glBindTexture(glTarget, glID);
    getGl().glTexParameterf(glTarget, GL10.GL_TEXTURE_MIN_FILTER, glMinFilter);
    getGl().glTexParameterf(glTarget, GL10.GL_TEXTURE_MAG_FILTER, glMagFilter);
    getGl().glTexParameterf(glTarget, GL10.GL_TEXTURE_WRAP_S, glWrapS);
    getGl().glTexParameterf(glTarget, GL10.GL_TEXTURE_WRAP_T, glWrapT);
     
 // First, we use glTexImage2D to set the full size of the texture (glW/glH might be diff
    // from w/h in the case that the GPU doesn't support NPOT textures)
    getGl().glTexImage2D(glTarget, 0, glFormat,  glWidth,  glHeight, 0, GL10.GL_RGBA, 
                         GL10.GL_UNSIGNED_BYTE, null);

    // Once OpenGL knows the size of the new texture, we make sure it doesn't
    // contain any garbage in the region of interest (0, 0, w, h):
    int[] texels = new int[width * height];
    java.util.Arrays.fill(texels, 0, width * height, 0x00000000); 
    setTexels(0, 0, width, height, texels); 
    texels = null;
    
    getGl().glBindTexture(glTarget, 0);
    getGl().glDisable(glTarget);
  }

    
  /**
   * Deletes the opengl texture object.
   */
  protected void release() {
    if (glID != 0) {
      a3d.deleteGLResource(glID, PGraphicsAndroid3D.GL_TEXTURE_OBJECT);
      glID = 0;
    }
  }

  
  ///////////////////////////////////////////////////////////  

  // Utilities.    
  
  
  // Copies source texture tex into this.
  protected void copyTexels(PTexture tex, int x, int y, int w, int h, boolean scale) {
    if (tex == null) {
      throw new RuntimeException("PTexture: source texture is null");
    }        
    
    if (tempFbo == null) {
      tempFbo = new PFramebuffer(parent, glWidth, glHeight);
    }
    
    // This texture is the color (destination) buffer of the FBO. 
    tempFbo.setColorBuffer(this);
    tempFbo.disableDepthTest();
    
    // FBO copy:
    a3d.pushFramebuffer();
    a3d.setFramebuffer(tempFbo);
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
    setTexels(0, x, y, w, h, pix);
  }
  
  protected void setTexels(int level, int x, int y, int w, int h, int[] pix) {
    getGl().glTexSubImage2D(glTarget, 0, x, y, w, h, GL10.GL_RGBA, GL10.GL_UNSIGNED_BYTE, IntBuffer.wrap(pix));
  }
  
  protected void copyObject(PTexture src) {
    // The OpenGL texture of this object is replaced with the one from the source object, 
    // so we delete the former to avoid resource wasting.
    release(); 
  
    width = src.width;
    height = src.height;
    
    parent = src.parent;
    a3d = src.a3d;
    
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
      res.sampling = POINT;
    } else if (glMinFilter == GL10.GL_LINEAR)  {
      res.sampling = BILINEAR;
    } else if (glMinFilter == GL10.GL_LINEAR_MIPMAP_LINEAR) {
      res.sampling = TRILINEAR;
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
      throw new RuntimeException("A3D: Unknown texture target");     
    }
    
    if (params.format == RGB)  {
      glFormat = GL10.GL_RGB;
    } else  if (params.format == ARGB) {
      glFormat = GL10.GL_RGBA;
    } else  if (params.format == ALPHA) {
      glFormat = GL10.GL_ALPHA;
    } else {
      throw new RuntimeException("A3D: Unknown texture format");     
    }
    
    if (params.sampling == POINT) {
      glMagFilter = GL10.GL_NEAREST;
      glMinFilter = GL10.GL_NEAREST;
    } else if (params.sampling == BILINEAR)  {
      glMagFilter = GL10.GL_LINEAR;
      glMinFilter = GL10.GL_LINEAR;
    } else if (params.sampling == TRILINEAR)  {
      glMagFilter = GL10.GL_LINEAR;
      glMinFilter = GL10.GL_LINEAR_MIPMAP_LINEAR;      
    } else {
      throw new RuntimeException("A3D: Unknown texture filtering mode");     
    }
    
    if (params.wrapU == CLAMP) {
      glWrapS = GL10.GL_CLAMP_TO_EDGE;  
    } else if (params.wrapU == REPEAT)  {
      glWrapS = GL10.GL_REPEAT;
    } else {
      throw new RuntimeException("A3D: Unknown wrapping mode");     
    }
    
    if (params.wrapV == CLAMP) {
      glWrapT = GL10.GL_CLAMP_TO_EDGE;  
    } else if (params.wrapV == REPEAT)  {
      glWrapT = GL10.GL_REPEAT;
    } else {
      throw new RuntimeException("A3D: Unknown wrapping mode");     
    }
    
    usingMipmaps = glMinFilter == GL10.GL_LINEAR_MIPMAP_LINEAR;
    
    flippedX = false;
    flippedY = false;    
  } 

  /////////////////////////////////////////////////////////////////////////// 

  // Utilities 
  
  
  protected GL10 getGl() {
    return a3d.gl;
  }  
  
  
  /////////////////////////////////////////////////////////////////////////// 

  // Parameters object  
  
  
  static public Parameters newParameters() {
    return new Parameters();  
  }
  

  static public Parameters newParameters(int format) {
    return new Parameters(format);  
  }    

  
  static public Parameters newParameters(int format, int sampling) {
    return new Parameters(format, sampling);  
  }        
      

  static public Parameters newParameters(Parameters params) {
    return new Parameters(params);  
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
     * Texture filtering (POINT, BILINEAR or TRILINEAR).
     */
    public int sampling;
    
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
      this.target = TEXTURE2D;
      this.format = ARGB;
      this.sampling = BILINEAR;
      this.wrapU = CLAMP;
      this.wrapV = CLAMP;
    }
      
    public Parameters(int format) {
      this.target = TEXTURE2D;
      this.format = format;
      this.sampling = BILINEAR;   
      this.wrapU = CLAMP;
      this.wrapV = CLAMP;
    }

    public Parameters(int format, int sampling) {
      this.target = TEXTURE2D;
      this.format = format;
      this.sampling = sampling;
      this.wrapU = CLAMP;
      this.wrapV = CLAMP;      
    }
    
    public Parameters(Parameters src) {
      this.target = src.target;
      this.format = src.format;
      this.sampling = src.sampling;
      this.wrapU = src.wrapU;
      this.wrapV = src.wrapV;
    }
    
    public void set(int format) {
      this.format = format;
    }

    public void set(int format, int sampling) {
      this.format = format;
      this.sampling = sampling;
    }
    
    public void set(Parameters src) {
      this.target = src.target;
      this.format = src.format;
      this.sampling = src.sampling;
      this.wrapU = src.wrapU;
      this.wrapV = src.wrapV;      
    }    
  }
}
