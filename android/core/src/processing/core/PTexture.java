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
import java.util.LinkedList;
import java.util.NoSuchElementException;

/**
 * This class wraps an OpenGL texture.
 * By Andres Colubri
 * 
 */
public class PTexture implements PConstants { 
  public int width, height;
      
  protected PApplet parent;           // The Processing applet
  protected PGraphicsAndroid3D renderer; // The main renderer
  protected PGL pgl;                  // The interface between Processing and OpenGL.
  protected PGL.Context context;        // The context that created this texture.
  
  // These are public but use at your own risk!
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
  
  protected Object bufferSource;
  protected LinkedList<BufferData> bufferCache = null;
  protected Method disposeBufferMethod;
  public static final int MAX_BUFFER_CACHE_SIZE = 3;  
  
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
  public PTexture(PApplet parent, int width, int height, Object params) { 
    this.parent = parent;
       
    renderer = (PGraphicsAndroid3D)parent.g;
    pgl = renderer.pgl;
    context = pgl.getContext();
    
    glID = 0;
     
    init(width, height, (Parameters)params);    
  } 

  
  protected void finalize() throws Throwable {
    try {
      if (glID != 0) {
        renderer.finalizeTextureObject(glID);
      }
    } finally {
      super.finalize();
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
    // Marking the texture object as finalized so it is deleted
    // when creating the new texture.
    release();
    
    // Creating new texture with the appropriate size.
    PTexture tex = new PTexture(parent, wide, high, getParameters());
    
    // Copying the contents of this texture into tex.
    tex.set(this);
    
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
    PTexture tex = (PTexture)img.getCache(renderer);
    set(tex);
  }
  
  
  public void set(PImage img, int x, int y, int w, int h) {
    PTexture tex = (PTexture)img.getCache(renderer);
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
    if (pixels == null) {
      pixels = null;
      PGraphics.showWarning("The pixels array is null.");
      return;
    }    
    if (pixels.length != w * h) {
      PGraphics.showWarning("The pixels array has the wrong length. It should be " + w * h);
      return;
    }
    
    pgl.enableTexturing(glTarget);
    pgl.bindTexture(glTarget, glID);
                
    if (usingMipmaps) {
      if (PGraphicsAndroid3D.mipmapGeneration) {
        // Automatic mipmap generation.
        int[] rgbaPixels = new int[w * h];
        convertToRGBA(pixels, rgbaPixels, format, w, h);        
        pgl.enableTexMipmapGen(glTarget);
        setTexels(rgbaPixels, x, y, w, h);
        rgbaPixels = null;
      } else {
        // TODO: Manual mipmap generation.
        // Open source implementation of gluBuild2DMipmaps here:
        // http://code.google.com/p/glues/source/browse/trunk/glues/source/glues_mipmap.c
      }      
    } else {
      int[] rgbaPixels = new int[w * h];
      convertToRGBA(pixels, rgbaPixels, format, w, h);
      setTexels(rgbaPixels, x, y, w, h);
      rgbaPixels = null;
    }

    pgl.bindTexture(glTarget, 0);
    pgl.disableTexturing(glTarget);        
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
      renderer.pushFramebuffer();
      renderer.setFramebuffer(tempFbo);
      tempFbo.readPixels();
      renderer.popFramebuffer();
    } else {
      // Here we don't have FBOs, so the method above is of no use. What we do instead is
      // to draw the texture to the screen framebuffer, and then grab the pixels from there.      
      renderer.pushFramebuffer();
      renderer.setFramebuffer(tempFbo);
      renderer.drawTexture(this, 0, 0, glWidth, glHeight, 0, 0, glWidth, glHeight);
      tempFbo.readPixels();
      renderer.popFramebuffer();
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
    pgl.enableTexturing(glTarget);
    pgl.bindTexture(glTarget, glID);
  }
  
  public void unbind() {
    pgl.enableTexturing(glTarget);
    pgl.unbindTexture(glTarget);    
  }  
  
  ////////////////////////////////////////////////////////////
  
  // Buffer sink interface.
  
  
  public void setBufferSource(Object source) {
    bufferSource = source;
    getSourceMethods();
  }    
  
  
  public void copyBufferFromSource(Object natRef, ByteBuffer byteBuf, int w, int h) {
    if (bufferCache == null) {
      bufferCache = new LinkedList<BufferData>();
    }
    
    if (bufferCache.size() + 1 <= MAX_BUFFER_CACHE_SIZE) {
      bufferCache.add(new BufferData(natRef, byteBuf.asIntBuffer(), w, h));
    } else {            
      // The buffer cache reached the maximum size, so we just dispose the new buffer.
      try {
        disposeBufferMethod.invoke(bufferSource, new Object[] { natRef });
      } catch (Exception e) {
        e.printStackTrace();
      }  
    }
  }
  
  
  public boolean hasBuffers() {
    return bufferSource != null && bufferCache != null && 0 < bufferCache.size();
  }  
  
  
  protected boolean bufferUpdate() {
    BufferData data = null;
    try {
      data = bufferCache.remove(0);
    } catch (NoSuchElementException ex) {
      PGraphics.showWarning("PTexture: don't have pixel data to copy to texture");
    }
    
    if (data != null) {
      if ((data.w != width) || (data.h != height)) {
        init(data.w, data.h);
      }
      bind();      
      setTexels(data.rgbBuf, 0, 0, width, height);
      unbind();
      
      data.dispose();
      
      return true;        
    } else {
      return false;
    }    
  }
 
  
  protected void getSourceMethods() {
    try {
      disposeBufferMethod = bufferSource.getClass().getMethod("disposeBuffer", new Class[] { Object.class });
    } catch (Exception e) {
      throw new RuntimeException("PTexture: provided source object doesn't have a disposeBuffer method.");
    }        
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
    maxTexCoordU = (float)width / glWidth;
    maxTexCoordV = (float)height / glHeight;    
  }
  
  
  /**
   * Allocates the opengl texture object. 
   */
  protected void allocate() {
    release(); // Just in the case this object is being re-allocated.
    
    pgl.enableTexturing(glTarget);
    
    glID = renderer.createTextureObject();    
    
    pgl.bindTexture(glTarget, glID);
    pgl.setTexMinFilter(glTarget, glMinFilter);
    pgl.setTexMagFilter(glTarget, glMagFilter);
    pgl.setTexWrapS(glTarget, glWrapS);
    pgl.setTexWrapT(glTarget, glWrapT);
            
    // First, we use glTexImage2D to set the full size of the texture (glW/glH might be diff
    // from w/h in the case that the GPU doesn't support NPOT textures)
    pgl.initTex(glTarget, glFormat, glWidth, glHeight);
    
    // Once OpenGL knows the size of the new texture, we make sure it doesn't
    // contain any garbage in the region of interest (0, 0, width, height):        
    // Doing in patches of 16x16 pixels to avoid creating a (potentially)
    // very large transient array which in certain situations (memory-
    // constrained android devices) might lead to an out-of-memory error.
    int[] texels = new int[16 * 16];
    for (int y = 0; y < height + 16; y += 16) {
      int h = PApplet.min(16, height - y);
      for (int x = 0; x < width + 16; x += 16) {
        int w = PApplet.min(16, width - x);
        setTexels(texels, x, y, w, h);        
      }
    }
    texels = null;
    
    pgl.unbindTexture(glTarget);
    pgl.disableTexturing(glTarget);
  }
  
  
  /**
   * Marks the texture object for deletion.
   */
  protected void release() {    
    if (glID != 0) {      
      renderer.finalizeTextureObject(glID);
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
    renderer.pushFramebuffer();
    renderer.setFramebuffer(tempFbo);
    if (scale) {
      // Rendering tex into "this", and scaling the source rectangle
      // to cover the entire destination region.
      renderer.drawTexture(tex, x, y, w, h, 0, 0, width, height);    
    } else {
      // Rendering tex into "this" but without scaling so the contents 
      // of the source texture fall in the corresponding texels of the
      // destination.
      renderer.drawTexture(tex, x, y, w, h, x, y, w, h);
    }
    renderer.popFramebuffer();
  }  
  
  protected void setTexels(int[] pix, int x, int y, int w, int h) {
    setTexels(pix, 0, x, y, w, h);
  }
  
  protected void setTexels(int[] pix, int level, int x, int y, int w, int h) {
    pgl.copyTexSubImage(pix, glTarget, level, x, y, w, h);
  }

  protected void setTexels(IntBuffer buffer, int x, int y, int w, int h) {
    setTexels(buffer, 0, x, y, w, h);
  }  
  
  protected void setTexels(IntBuffer buffer, int level, int x, int y, int w, int h) {
    pgl.copyTexSubImage(buffer, glTarget, level, x, y, w, h);
  }
  
  protected void copyObject(PTexture src) {
    // The OpenGL texture of this object is replaced with the one from the source object, 
    // so we delete the former to avoid resource wasting.
    release(); 
  
    width = src.width;
    height = src.height;
    
    parent = src.parent;
    renderer = src.renderer;
    
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
    
    if (glTarget == PGL.TEXTURE_2D)  {
        res.target = TEXTURE2D;
    }
    
    if (glFormat == PGL.RGB)  {
      res.format = RGB;
    } else  if (glFormat == PGL.RGBA) {
      res.format = ARGB;
    } else  if (glFormat == PGL.ALPHA) {
      res.format = ALPHA;
    }
    
    if (glMinFilter == PGL.NEAREST)  {
      res.sampling = POINT;
    } else if (glMinFilter == PGL.LINEAR)  {
      res.sampling = BILINEAR;
    } else if (glMinFilter == PGL.LINEAR_MIPMAP_LINEAR) {
      res.sampling = TRILINEAR;
    }

    if (glWrapS == PGL.CLAMP_TO_EDGE) {
      res.wrapU = CLAMP;  
    } else if (glWrapS == PGL.REPEAT) {
      res.wrapU = REPEAT;
    }

    if (glWrapT == PGL.CLAMP_TO_EDGE) {
      res.wrapV = CLAMP;  
    } else if (glWrapT == PGL.REPEAT) {
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
        glTarget = PGL.TEXTURE_2D;
    } else {
      throw new RuntimeException("OPENGL2: Unknown texture target");     
    }
    
    if (params.format == RGB)  {
      glFormat = PGL.RGB;
    } else  if (params.format == ARGB) {
      glFormat = PGL.RGBA;
    } else  if (params.format == ALPHA) {
      glFormat = PGL.ALPHA;
    } else {
      throw new RuntimeException("OPENGL2: Unknown texture format");     
    }
    
    if (params.sampling == POINT) {
      glMagFilter = PGL.NEAREST;
      glMinFilter = PGL.NEAREST;
    } else if (params.sampling == BILINEAR)  {
      glMagFilter = PGL.LINEAR;
      glMinFilter = PGL.LINEAR;
    } else if (params.sampling == TRILINEAR)  {
      glMagFilter = PGL.LINEAR;
      glMinFilter = PGL.LINEAR_MIPMAP_LINEAR;      
    } else {
      throw new RuntimeException("OPENGL2: Unknown texture filtering mode");     
    }
    
    if (params.wrapU == CLAMP) {
      glWrapS = PGL.CLAMP_TO_EDGE;  
    } else if (params.wrapU == REPEAT)  {
      glWrapS = PGL.REPEAT;
    } else {
      throw new RuntimeException("OPENGL2: Unknown wrapping mode");     
    }
    
    if (params.wrapV == CLAMP) {
      glWrapT = PGL.CLAMP_TO_EDGE;  
    } else if (params.wrapV == REPEAT)  {
      glWrapT = PGL.REPEAT;
    } else {
      throw new RuntimeException("OPENGL2: Unknown wrapping mode");     
    }
    
    usingMipmaps = glMinFilter == PGL.LINEAR_MIPMAP_LINEAR;
    
    flippedX = false;
    flippedY = false;    
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
  
  /**
   * This class stores a buffer copied from the buffer source.
   *
   */
  protected class BufferData {    
    int w, h;
    // Native buffer object.
    Object natBuf;
    // Buffer viewed as int.
    IntBuffer rgbBuf;
    
    BufferData(Object nat, IntBuffer rgb, int w, int h) {
      natBuf = nat;
      rgbBuf = rgb;
      this.w = w;
      this.h = h;
    }
    
    void dispose() {
      try {
        // Disposing the native buffer.
        disposeBufferMethod.invoke(bufferSource, new Object[] { natBuf });
        natBuf = null;       
        rgbBuf = null;
      } catch (Exception e) {
        e.printStackTrace();
      }      
    }
  }      
}
