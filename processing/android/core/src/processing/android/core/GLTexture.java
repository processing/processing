package processing.android.core;

import java.nio.IntBuffer;

import javax.microedition.khronos.opengles.*;

import javax.microedition.khronos.egl.EGL10;

import android.opengl.GLUtils;

import java.nio.*;

/**
 * This class adds an opengl texture to a PImage object.
 */
@SuppressWarnings("unused")
public class GLTexture extends PImage implements PConstants, GLConstants { 

  protected PGraphicsAndroid3D a3d;  
  protected GL10 gl;

  protected int[] glTextureID = { 0 }; 
  protected int glTarget;
  protected int glInternalFormat;
  protected int glMinFilter;  
  protected int glMagFilter;

  protected int glWidth;
  protected int glHeight;    
  
  protected boolean usingMipmaps; 
  protected float maxTexCoordS;
  protected float maxTexCoordT;
  
  protected boolean flippedX;   
  protected boolean flippedY;
  
  ////////////////////////////////////////////////////////////
  
  // Constructors.

  
  /**
   * Creates an instance of GLTexture with size width x height. The texture is
   * initialized (empty) to that size.
   * @param parent PApplet
   * @param width  int
   * @param height  int
   */	 
  public GLTexture(PApplet parent, int width, int height) {
    this(parent, width, height, new Parameters());
  }
    
  
  /**
   * Creates an instance of GLTexture with size width x height and with the specified parameters.
   *  The texture is initialized (empty) to that size.
   * @param parent PApplet
   * @param width int 
   * @param height int 
   * @param params Parameters 			
   */	 
  public GLTexture(PApplet parent, int width, int height, Parameters params) {
    super(width, height, params.format);  
    this.parent = parent;
       
    a3d = (PGraphicsAndroid3D)parent.g;
    gl = a3d.gl;
    
    setParameters(params);
    createTexture(width, height);
  }	
	

  /**
   * Creates an instance of GLTexture using image file filename as source.
   * @param parent PApplet
   * @param filename String
   */	
  public GLTexture(PApplet parent, String filename)  {
    this(parent, filename,  new Parameters());
  }

  
  /**
   * Creates an instance of GLTexture using image file filename as source and the specified texture parameters.
   * @param parent PApplet
   * @param filename String
   * @param params Parameters
   */	
  public GLTexture(PApplet parent, String filename, Parameters params)  {
    super(1, 1, params.format);  
    this.parent = parent;
	   
    a3d = (PGraphicsAndroid3D)parent.g;
    gl = a3d.gl;	
        
    PImage img = parent.loadImage(filename);
    set(img);
  }


  protected void finalize() {
    deleteTexture();
  }

  
  ////////////////////////////////////////////////////////////
  
  // Init, resize methods
  
  
  /**
   * Sets the size of the image and texture to width x height. If the texture is already initialized,
   * it first destroys the current opengl texture object and then creates a new one with the specified
   * size.
   * @param width int
   * @param height int
   */
  public void init(int width, int height) {
    init(width, height, new Parameters());
  }
  

  /**
   * Sets the size of the image and texture to width x height, and the parameters of the texture to params.
   * If the texture is already  initialized, it first destroys the current opengl texture object and then creates 
   * a new one with the specified size.
   * @param width int
   * @param height int
   * @param params GLTextureParameters 
   */
  public void init(int width, int height, Parameters params)  {
    super.init(width, height, params.format);
    setParameters(params);
    createTexture(width, height);
  } 


  // Very inefficient.
  public void resize(int wide, int high) {
    // Performance resize needs FBO to draw rescaled texture.
    
    // The following resize methods assumes the texture is already
    // stored in the pixels array.
    super.resize(wide, high);
    update();    
  }

  
  /**
   * Returns true if the texture has been initialized.
   * @return boolean
   */  
  public boolean available()  {
    return 0 < glTextureID[0];
  }

  
  ////////////////////////////////////////////////////////////
  
  // Set methods

  
  public void set(PImage img) {
    if (img.width != width || img.height != height) {
      super.init(img.width, img.height, img.format);
      createTexture(width, height);      
    }
    
    img.loadPixels();
    set(img.pixels, img.format);
  }
  
  
  public void set(PImage img, int x, int y, int w, int h) {
    x = PApplet.constrain(x, 0, img.width);
    y = PApplet.constrain(y, 0, img.height);
    
    w = PApplet.constrain(w, 0, img.width - x);
    h = PApplet.constrain(h, 0, img.height - y);
    
    if ((w != width) || (h != height)) {
      super.init(w, h, img.format);
      createTexture(w, h); 
    }
    
    int p0;
    int dest[] = new int[w * h];
    for (int j = 0; j < h; j++) {
      p0 = y * img.width + x + (img.width - w) * j;
      PApplet.arrayCopy(img.pixels, p0 + w * j, dest, w * j, w);
    }
   
    set(dest, img.format);
  }
  
  
  public void set(GLTexture tex) { // Ignore
    // It doesn't work yet because efficient texture copy requires either FBO or pbuffers
    // Read this thread for more info:
    // http://www.opengl.org/discussion_boards/ubbthreads.php?ubb=showflat&Number=247142
    // The solution of writing to a framebuffer and then using 
    
    /*
    if (tex.width != width || tex.height != height) {
      super.init(tex.width, tex.height, tex.format);
      createTexture(width, height);      
    }
    
    fbo copy here.. someday....
    
    */
  }

  
  public void set(int[] pixels) {
    set(pixels, ARGB); 
  }

  
  public void set(int[] pixels, int format) {
    
    if (pixels.length != width * height) {
      throw new RuntimeException("GLTexture: wrong length of pixels array");
    }
    
    if (glTextureID[0] == 0) {
      createTexture(width, height);
    }   
    
    int[] convArray = pixels;
    int glFormat;
    if (format == ALPHA) {
      glFormat = GL10.GL_ALPHA;
    } else if (format == RGB)  {
      glFormat = GL10.GL_RGB;
    } else {
      glFormat = GL10.GL_RGBA;
    }
    convArray = convertToRGBA(pixels, format);    
    
    gl.glBindTexture(glTarget, glTextureID[0]);
                
    if (usingMipmaps) {
      if (a3d.gl11 != null && a3d.mipmapSupported) {
        gl.glTexParameterf(GL11.GL_TEXTURE_2D, GL11.GL_GENERATE_MIPMAP, GL11.GL_TRUE);
      } else {
        // TODO: alternative mipmap generation. See the following link for more info:
        // http://insanitydesign.com/wp/2009/08/01/android-opengl-es-mipmaps/
      }
    }
    gl.glTexSubImage2D(glTarget, 0, 0, 0, glWidth, glHeight, glFormat, GL10.GL_UNSIGNED_BYTE, IntBuffer.wrap(convArray));

    gl.glBindTexture(glTarget, 0);
  }  

  
  ////////////////////////////////////////////////////////////
  
  // Update methods
  
  
  /**     
   * Copy texture to pixels. Involves video memory to main memory transfer (slow).
   */   
  public void updatePixels() { // Ignore. 
    // It doesn't work yet, because there is no GetTexImage.
    // But:if the texture is in a renderable format (RGB or RGBA, not L, A, or LA) then you can bind it to an FBO and use glReadPixels.
    // From: http://www.idevgames.com/forum/showthread.php?t=17044
    // Unfortunately, FBO support seems to be available only on iPhone through an custom extension (glBindFramebufferOES, etc).
    // Read also note on opengl page on android site:
    // http://developer.android.com/guide/topics/graphics/opengl.html
    // Specifically:
    // "Finally, note that though Android does include some basic support for OpenGL ES 1.1, the support is not complete, 
    // and should not be relied upon at this time."
    // :-(  
    
    /*
    int size = glWidth * glHeight;
    IntBuffer buffer = BufferUtil.newIntBuffer(size);
    
    gl.glBindTexture(glTarget, glTextureID[0]);
    //gl.glGetTexImage(glTarget, 0, GL10.GL_RGBA, GL10.GL_UNSIGNED_BYTE, buffer);
    gl.glBindTexture(glTarget, 0);
    
    int[] tmp = new int[size];
    buffer.get(tmp);
    int[] pixelsARGB = convertToARGB(tmp);
    PApplet.arrayCopy(pixelsARGB, pixels);
    if (flippedX) flipArrayOnX(pixels, 1);
    if (flippedY) flipArrayOnY(pixels, 1);
    
    super.updatePixels();
    */
  }


  /**
   * Copy pixels to texture. Involves main memory to video memory transfer (slow).
   */     
  void update() {
    set(this.pixels);
  }
    
    
  ////////////////////////////////////////////////////////////     
 
  // Get opengl parameters
  
  
  protected int getGLWidth() {
    return glWidth;
  }
  
  
  protected int getGLHeight() {
    return glHeight;
  }  
  
  
  /**
   * Provides the ID of the opengll texture object.
   * @return int
   */	
  protected int getGLTextureID()  {
    return glTextureID[0];
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
  protected int getGLInternalFormat() {
    return glInternalFormat;
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
   * Returns true or false whether or not the texture is using mipmaps.
   * @return boolean
   */	
  protected boolean usingMipmaps()  {
    return usingMipmaps;
  }
	
  
  /**
   * Returns the maximum possible value for the texture coordinate S.
   * @return float
   */	
  protected float getMaxTextureCoordS() {
    return maxTexCoordS;
  }
	
  
  /**
   * Returns the maximum possible value for the texture coordinate T.
   * @return float
   */	
  protected float getMaxTextureCoordT() {
    return maxTexCoordT;
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
   * The size of the incoming array is assumed to be width*height, and the size of the
   * returned array is glWidth * glHeight.
   * @param intArray int[]
   * @param arrayFormat int	 
   */
  protected int[] convertToRGBA(int[] intArray, int arrayFormat)  {
    int t = 0; 
    int p = 0;
    int[] tIntArray = new int[glWidth * glHeight];        
    if (PGraphicsAndroid3D.BIG_ENDIAN)  {
      switch (arrayFormat) {
      case ALPHA:
                	
        for (int y = 0; y < height; y++) {
          for (int x = 0; x < width; x++) {
            tIntArray[t++] = 0xFFFFFF00 | intArray[p++];
          }
          t += glWidth - width;
        }
        break;

      case RGB:
                	
        for (int y = 0; y < height; y++)  {
          for (int x = 0; x < width; x++)  {
            int pixel = intArray[p++];
            tIntArray[t++] = (pixel << 8) | 0xff;
          }
          t += glWidth - width;
        }
        break;

      case ARGB:
                	
        for (int y = 0; y < height; y++)  {
          for (int x = 0; x < width; x++)  {
            int pixel = intArray[p++];
            tIntArray[t++] = (pixel << 8) | ((pixel >> 24) & 0xff);
          }
          t += glWidth - width;
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
            	
        for (int y = 0; y < height; y++) {
          for (int x = 0; x < width; x++) {
            tIntArray[t++] = (intArray[p++] << 24) | 0x00FFFFFF;
          }
          t += glWidth - width;
        }
        break;

      case RGB:
                	
        for (int y = 0; y < height; y++) {
          for (int x = 0; x < width; x++) {
            int pixel = intArray[p++];
              // needs to be ABGR, stored in memory xRGB
              // so R and B must be swapped, and the x just made FF
              tIntArray[t++] = 0xff000000 |  // force opacity for good measure
                                            ((pixel & 0xFF) << 16) |
                                            ((pixel & 0xFF0000) >> 16) |
                                            (pixel & 0x0000FF00);
          }
          t += glWidth - width;
        }
        break;

      case ARGB:
                    	
        for (int y = 0; y < height; y++) {
          for (int x = 0; x < width; x++) {
            int pixel = intArray[p++];
              // needs to be ABGR stored in memory ARGB
              // so R and B must be swapped, A and G just brought back in
              tIntArray[t++] =((pixel & 0xFF) << 16) |
                                           ((pixel & 0xFF0000) >> 16) |
                                           (pixel & 0xFF00FF00);
          }
          t += glWidth - width;
        }
        break;
          
      }
    }
        
    return tIntArray;    
  }
     
  
  /**
   * Reorders a pixel array in RGBA format into ARGB. The input array must be
   * of size glWidth * glHeight, while the resulting array will be of size width * height.
   * @param intArray int[]	 
   */    
  protected int[] convertToARGB(int[] intArray) {
    int t = 0; 
    int p = 0;
    int[] tIntArray = new int[width * height];
    
    if (PGraphicsAndroid3D.BIG_ENDIAN) {
      
            for (int y = 0; y < height; y++)  {
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
        for (int x = 0; x < width; x++)  {
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
        
    return tIntArray;    
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
      
    if (a3d.npotTexSupported) {
      glWidth = w;
      glHeight =h;
    } else {
      glWidth = nextPowerOfTwo(w);
      glHeight = nextPowerOfTwo(h);
    }
    
    if ((glWidth > a3d.maxTextureSize) || (glHeight > a3d.maxTextureSize)) {
      glWidth = glHeight = 0;
      throw new RuntimeException("Image width and height cannot be" +
                                 " larger than " + a3d.maxTextureSize +
                                 " with this graphics card.");
    }    
    
    usingMipmaps = ((glMinFilter == GL10.GL_NEAREST_MIPMAP_NEAREST) ||
                                     (glMinFilter == GL10.GL_LINEAR_MIPMAP_NEAREST) ||
                                     (glMinFilter == GL10.GL_NEAREST_MIPMAP_LINEAR) ||
                                     (glMinFilter == GL10.GL_LINEAR_MIPMAP_LINEAR));
        
     gl.glGenTextures(1, glTextureID, 0);
     gl.glBindTexture(glTarget, glTextureID[0]);
     gl.glTexParameterf(glTarget, GL10.GL_TEXTURE_MIN_FILTER, glMinFilter);
     gl.glTexParameterf(glTarget, GL10.GL_TEXTURE_MAG_FILTER, glMagFilter);
     gl.glTexParameterf(glTarget, GL10.GL_TEXTURE_WRAP_S, GL10.GL_CLAMP_TO_EDGE);
     gl.glTexParameterf(glTarget, GL10.GL_TEXTURE_WRAP_T, GL10.GL_CLAMP_TO_EDGE);        
     gl.glTexImage2D(glTarget, 0, glInternalFormat,  glWidth,  glHeight, 0, GL10.GL_RGBA, GL10.GL_UNSIGNED_BYTE, null);
     gl.glBindTexture(glTarget, 0);
        
     flippedX = false;
     flippedY = false;  
        
     maxTexCoordS = 1.0f;
     maxTexCoordT = 1.0f; 
  }

    
  /**
   * Deletes the opengl texture object.
   */
  protected void deleteTexture() {
    if (glTextureID[0] != 0) {
      gl.glDeleteTextures(1, glTextureID, 0);  
      glTextureID[0] = 0;
    }
  }
  
  
  ///////////////////////////////////////////////////////////  
  
  // Parameter handling
  
  
  public Parameters getParameters() {
    Parameters res = new Parameters();
    
    if ( glTarget == GL10.GL_TEXTURE_2D )  {
        res.target = NORMAL_TEXTURE;
    }
    
    if (glInternalFormat == GL10.GL_RGB)  {
      res.format = RGB;
    } else  if (glInternalFormat == GL10.GL_RGBA) {
      res.format = ARGB;
    } else  if (glInternalFormat == GL10.GL_ALPHA) {
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
    } else if (glMagFilter == GL10.GL_LINEAR)  {
      res.magFilter = LINEAR;
    }
    
    return res;
  }
  
  
  /**
   * Sets texture target and internal format according to the target and  type specified.
   * @param target int		   
   * @param params GLTextureParameters
   */		
  protected void setParameters(Parameters params) {    
	  if (params.target == NORMAL_TEXTURE)  {
        glTarget = GL10.GL_TEXTURE_2D;
    } else {
      throw new RuntimeException("GTexture: Unknown texture target");	    
	  }
	  
    if (params.format == RGB)  {
      glInternalFormat = GL10.GL_RGB;
    } else  if (params.format == ARGB) {
      glInternalFormat = GL10.GL_RGBA;
    } else  if (params.format == ALPHA) {
      glInternalFormat = GL10.GL_ALPHA;
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
    
    if (params.magFilter == NEAREST)  {
      glMagFilter = GL10.GL_NEAREST;
    } else if (params.magFilter == LINEAR)  {
      glMagFilter = GL10.GL_LINEAR;
    } else {
      throw new RuntimeException("GTexture: Unknown magnification filter");     
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
     * Creates an instance of GLTextureParameters, setting all the parameters to default values.
     */
    public Parameters() {
      target = GLTexture.NORMAL_TEXTURE;
      format = GLTexture.ARGB;
      minFilter = GLTexture.LINEAR;
      magFilter = GLTexture.LINEAR;   
    }
      
    public Parameters(int format) {
      target = GLTexture.NORMAL_TEXTURE;
      this.format = format;
      minFilter = GLTexture.LINEAR;
      magFilter = GLTexture.LINEAR;   
    }

    public Parameters(int format, int filter) {
      target = GLTexture.NORMAL_TEXTURE;
      this.format = format;
      minFilter = filter;
      magFilter = filter;   
    }
  }
}
