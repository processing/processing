package processing.android.core;

import java.nio.IntBuffer;

import javax.microedition.khronos.opengles.*;

import java.nio.*;

// TODO:
// Properly implement putBuffer method to copy ALPHA, RGB and RGBA buffers to the texture. Now it has been
// quickly hacked to properly use alpha textures needed by GLFonts.

/**
 * This class adds an opengl texture to a PImage object.
 */
@SuppressWarnings("unused")
public class GLTexture extends PImage implements PConstants, GLConstants { 

  protected PGraphicsAndroid3D pgl;  
  protected GL10 gl;

  protected int[] glTexID = { 0 }; 
  protected int glTexTarget;  
  protected int glTexInternalFormat;
  protected int glMinFilter;  
  protected int glMagFilter;
  
  protected boolean usingMipmaps; 
  protected float maxTexCoordS;
  protected float maxTexCoordT;
  
  protected boolean flippedX;   
  protected boolean flippedY;
  
  ////////////////////////////////////////////////////////////
  
  // Constructors.
  
  /**
   * Creates an instance of GLTexture with size width x height. The texture is initialized (empty) to that size.
   * @param parent PApplet
   * @param width int 
   * @param height int 
   */	 
  public GLTexture(PApplet parent, int width, int height) {
    super(width, height, ARGB);  
    this.parent = parent;
       
    pgl = (PGraphicsAndroid3D)parent.g;
    gl = pgl.gl;
	  setTextureParams(new Parameters());
       
    initTexture(width, height);
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
       
    pgl = (PGraphicsAndroid3D)parent.g;
    gl = pgl.gl;
    setTextureParams(params);
       
    initTexture(width, height);
  }	
	

  /**
   * Creates an instance of GLTexture using image file filename as source.
   * @param parent PApplet
   * @param filename String
   */	
  public GLTexture(PApplet parent, String filename)  {
    super(1, 1, ARGB);  
    this.parent = parent;
	   
    pgl = (PGraphicsAndroid3D)parent.g;
    gl = pgl.gl;
        
    loadTexture(filename);
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
	   
    pgl = (PGraphicsAndroid3D)parent.g;
    gl = pgl.gl;	
        
    loadTexture(filename, params);
  }


  protected void finalize() {
    if (glTexID[0] != 0) {
      releaseTexture();
    }
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
    setTextureParams(params);
    initTexture(width, height);
  } 


  public void resize(int wide, int high) {
   // super.resize(wide, high);
  }

  
  /**
   * Returns true if the texture has been initialized.
   * @return boolean
   */  
  public boolean available()  {
    return 0 < glTexID[0];
  }

  
  ////////////////////////////////////////////////////////////
  
  // Set methods
  
  public void set(PImage img) {
    if (img.width != width || img.height != height) {
      super.init(width, height, format);
      initTexture(width, height);      
    }
    // or:
    // img -> tmp
    // resize tmp to size of this.
    // copy tmp to tex of this.
    
    img.loadPixels();
    set(img.pixels, img.format);
  }
  
  public void set(PImage img, int x, int y, int w, int h) {
  // copy (x, y, x+w, y+h) of img to tmp
    // resize tmp to size of this
    // copy tmp to tex of this
  }
  
  public void set(GLTexture tex) {
   // by interpolation, size of this doesn't change. 
  }

  public void set(int[] pixels) {
    set(pixels, ARGB); 
  }

  public void set(int[] pixels, int format) {
    
    if (pixels.length != width * height)
      throw ...
    
        if (glTexID[0] == 0)
        {
            initTexture(width, height);
        }      
  
        int[] convArray = pixels;
        int glFormat;
        if (format == ALPHA)
        {
          glFormat = GL10.GL_ALPHA;

              byte[] convArray2 = convertToAlpha(pixels);
              gl.glBindTexture(glTexTarget, glTexID[0]);
              gl.glTexSubImage2D(glTexTarget, 0, 0, 0, width, height, glFormat, GL10.GL_UNSIGNED_BYTE, ByteBuffer.wrap(convArray2));
              gl.glBindTexture(glTexTarget, 0);
              return;
        }
        else if (format == RGB)
        {
          // If in the previous case, we need to use a byte array, here with RGB we should use a byte array with 3 bytes per color, 
          // i.e.: byte[] convArray3 = byte[3 * widht * height] and then storing RGB components from intArray as follows:
          // for (int i = 0; i < width * height; i++) { 
          // convArray3[3 * i ] = red(intArray[i]);
          // convArray3[3 * i + 1] = green(intArray[i]);
          // convArray3[3 * i + 2] = blue(intArray[i]);
          //}
          // where the red, green, green and blue operators involve the correct bit shifting to extract the component from the int value.          
          glFormat = GL10.GL_RGB;
          convArray = convertToRGBA(pixels, RGB);
        }
        else
        {
          glFormat = GL10.GL_RGBA;
          convArray = convertToRGBA(pixels, ARGB);
        }
        
        int glType;
        
        // No GL_INT in ES
        //if (type == TEX_INT) glType = GL10.GL_INT;
        //else glType = GL10.GL_UNSIGNED_BYTE;
        glType = GL10.GL_UNSIGNED_BYTE;
        
        gl.glBindTexture(glTexTarget, glTexID[0]);
                
        /*
        // Apparently no 1D textures and mipmaps in OpenGL ES.
        if (texTarget == GL10.GL_TEXTURE_1D)
        {
            gl.glTexSubImage1D(texTarget, 0, 0, width, glFormat, glType, IntBuffer.wrap(convArray));
        }
        else
        {
            if (usingMipmaps)
                GLState.glu.gluBuild2DMipmaps(texTarget, texInternalFormat, width, height, glFormat, glType, IntBuffer.wrap(convArray));
            else
              gl.glTexSubImage2D(texTarget, 0, 0, 0, width, height, glFormat, glType, IntBuffer.wrap(convArray));
        }
        */
        
        gl.glTexSubImage2D(glTexTarget, 0, 0, 0, width, height, glFormat, glType, IntBuffer.wrap(convArray));

        gl.glBindTexture(glTexTarget, 0);

        
    
  }  
  
    /**
     * Load texture, pixels and image from file.
     * @param filename String
     */
    public void loadTexture(String filename) {
    {
        PImage img = parent.loadImage(filename);
        set(img);
    }  
  
  /**     
   * Copy texture to pixels.
   */   
  public void updatePixels() { // doesn't work yet...
    int size = width * height;
    IntBuffer buffer = BufferUtil.newIntBuffer(size);
    
    gl.glBindTexture(glTexTarget, glTexID[0]);
    //gl.glGetTexImage(texTarget, 0, GL10.GL.GL_RGBA, GL10.GL_UNSIGNED_BYTE, buffer);
    gl.glBindTexture(glTexTarget, 0);
        
    buffer.get(pixels);
    int[] pixelsARGB = convertToARGB(pixels);
    PApplet.arrayCopy(pixelsARGB, pixels);        
    if (flippedX) flipArrayOnX(pixels, 1);
    if (flippedY) flipArrayOnY(pixels, 1);
        
    super.updatePixels();
  }

  
  
  
  
  
  
  
  
  
  /**
   * Puts img into texture, pixels and image.
   * @param img PImage
   */
  public void putImage(PImage img)  {
     putImage(img, new Parameters());
  }

    /**
     * Puts img into texture, pixels and image.
     * @param img PImage
     * @param format int
     */
    public void putImage(PImage img, int format)
    {
        putImage(img, new Parameters(format));
    }

    /**
     * Puts img into texture, pixels and image.
     * @param img PImage
     * @param format int
     * @param filter int
     */
    public void putImage(PImage img, int format, int filter)
    {
        putImage(img, new Parameters(format, filter));
    }

    /**
     * Puts img into texture, pixels and image.
     * @param img PImage
     * @param params GLTextureParameters
     */
    public void putImage(PImage img, Parameters params)
    {
        img.loadPixels();
        
        if ((img.width != width) || (img.height != height))
        {
            init(img.width, img.height, params);
        }
        
        for (int i = 0; i < width * height; i++)
          pixels[i] = img.pixels[i];
        
        // ...into texture...
        loadTexture();
        
        // ...and into image.
        updatePixels();        
    }

    /**
     * Puts pixels of img into texture only.
     * @param img PImage
     */
    public void putPixelsIntoTexture(PImage img)
    { 
        if ((img.width != width) || (img.height != height))
        {
            init(img.width, img.height, new Parameters());
        }
   
        // Putting into texture.
        if (glTexInternalFormat == GL10.GL_RGB) 
        {
            putBuffer(img.pixels, RGB);
        } 
        if (glTexInternalFormat == GL10.GL_RGBA) 
        {
            putBuffer(img.pixels, ARGB);
        } 
        if (glTexInternalFormat == GL10.GL_ALPHA) 
        {
            putBuffer(img.pixels, ALPHA);
        }     
      }
  
    /**
     * Puts the pixels of img inside the rectangle (x, y, x+w, y+h) into texture only.
     * @param img PImage
     * @param x int
     * @param y int
     * @param w int
     * @param h int
     */ 
    public void putPixelsIntoTexture(PImage img, int x, int y, int w, int h)
    { 
        x = PApplet.constrain(x, 0, img.width);
        y = PApplet.constrain(y, 0, img.height);
    
        w = PApplet.constrain(w, 0, img.width - x);
        h = PApplet.constrain(h, 0, img.height - y);
    
        if ((w != width) || (h != height))
        {
            init(w, h, new Parameters());
        }
    
        int p0;
        int dest[] = new int[w * h];
        for (int j = 0; j < h; j++)
      {
          p0 = y * img.width + x + (img.width - w) * j;
          PApplet.arrayCopy(img.pixels, p0 + w * j, dest, w * j, w);
      }
   
        // Putting into texture.
        if (glTexInternalFormat == GL10.GL_RGB) 
        {
            putBuffer(dest, RGB);
        } 
        if (glTexInternalFormat == GL10.GL_RGBA) 
        {
            putBuffer(dest, ARGB);
        } 
        if (glTexInternalFormat == GL10.GL_ALPHA) 
        {
            putBuffer(dest, ALPHA);
        }     
    }
  
    /**
     * Copies texture to img.
     * @param img PImage
     */ 
    public void getImage(PImage img)
    {
        int w = width;
        int h = height;
        
        if ((img.width != w) || (img.height != h))
        {
            img.init(w, h, ARGB);
        }
     
        int size = w * h;
        IntBuffer buffer = BufferUtil.newIntBuffer(size);
        gl.glBindTexture(glTexTarget, glTexID[0]);
        //gl.glGetTexImage(texTarget, 0, GL10.GL_RGBA, GL10.GL_UNSIGNED_BYTE, buffer);
        gl.glBindTexture(glTexTarget, 0);       
       
        buffer.get(img.pixels);
        
        int[] pixelsARGB = convertToARGB(img.pixels);
        PApplet.arrayCopy(pixelsARGB, img.pixels);        
        if (flippedX) flipArrayOnX(img.pixels, 1);
        if (flippedY) flipArrayOnY(img.pixels, 1);
        img.updatePixels();       
    }

    /**
     * Load texture, pixels and image from file.
     * @param filename String
     */
    public void loadTexture(String filename)
    {
        PImage img = parent.loadImage(filename);
        putImage(img);
    }

    /**
     * Load texture, pixels and image from file using the specified texture parameters.
     * @param filename String
     * @param params GLTextureParameters 
     */
    public void loadTexture(String filename, Parameters params)
    {
        PImage img = parent.loadImage(filename);
        putImage(img, params);
    }

    /**
     * Load texture, pixels and image from file using the specified texture format.
     * @param filename String
     * @param format int 
     */
    public void loadTexture(String filename, int format)
    {
        PImage img = parent.loadImage(filename);
        putImage(img, format);
    }

    /**
     * Load texture, pixels and image from file using the specified texture format and filtering.
     * @param filename String
     * @param format int 
     * @param  filter int
     */
    public void loadTexture(String filename, int format, int filter)
    {
        PImage img = parent.loadImage(filename);
        putImage(img, format, filter);
    }

    /**
     * Copy pixels to texture (loadPixels should have been called beforehand).
     */ 
    public void loadTexture()
    {
        // Putting into texture.
        if (glTexInternalFormat == GL10.GL_RGB) 
        {
            putBuffer(pixels, RGB);
        } 
        if (glTexInternalFormat == GL10.GL_RGBA) 
        {
            putBuffer(pixels, ARGB);
        } 
        if (glTexInternalFormat == GL10.GL_ALPHA) 
        {
            putBuffer(pixels, ALPHA);
        }     
    }
    
    /**
     * Copy src texture into this.
     * @param src GLTexture 
     */ 
    public void copy(GLTexture src)
    {
        //glstate.copyTex(src, this); 
    }
    
  
  
  
  
  
  
    
    
    
    
    
    
    
    
    /**
     * Provides the ID of the opegl texture object.
     * @return int
     */	
    protected int getGLTexID()
    {
        return glTexID[0];
    }

    /**
     * Returns the texture target.
     * @return int
     */	
    protected int getTextureTarget()
    {
        return glTexTarget;
    }    

    /**
     * Returns the texture internal format.
     * @return int
     */	
    protected int getTextureInternalFormat()
    {
        return glTexInternalFormat;
    }

    /**
     * Returns the texture minimization filter.
     * @return int
     */	
    protected int getTextureMinFilter()
    {
        return glMinFilter;
    }

    /**
     * Returns the texture magnification filter.
     * @return int
     */	
    protected int getTextureMagFilter()
    {
        return glMagFilter;
    }
	
    /**
     * Returns true or false whether or not the texture is using mipmaps.
     * @return boolean
     */	
    protected boolean usingMipmaps()
    {
        return usingMipmaps;
    }
	
    /**
     * Returns the maximum possible value for the texture coordinate S.
     * @return float
     */	
    protected float getMaxTextureCoordS()
    {
        return maxTexCoordS;
    }
	
    /**
     * Returns the maximum possible value for the texture coordinate T.
     * @return float
     */	
    protected float getMaxTextureCoordT()
    {
        return maxTexCoordT;
    }
	
    /**
     * Returns true if the texture is flipped along the horizontal direction.
     * @return boolean;
     */	
    protected boolean isFlippedX()
    {
        return flippedX;
    }

    /**
     * Sets the texture as flipped or not flipped on the horizontal direction.
     * @param v boolean;
     */	
    protected void setFlippedX(boolean v)
    {
        flippedX = v;
    }	
	
    /**
     * Returns true if the texture is flipped along the vertical direction.
     * @return boolean;
     */	
    protected boolean isFlippedY()
    {
        return flippedY;
    }

    /**
     * Sets the texture as flipped or not flipped on the vertical direction.
     * @param v boolean;
     */	
    protected void setFlippedY(boolean v)
    {
        flippedY = v;
    }
	
    
    

    /**
     * Copies intArray into the texture, assuming that the array contains 4 color components and pixels are unsigned bytes.
     * @param intArray int[]
     */	
    public void putBuffer(int[] intArray)
    {
        putBuffer(intArray, ARGB, TEX_BYTE);
    }

    /**
     * Copies intArray into the texture, using the specified format and assuming that the pixels are unsigned bytes.
     * @param intArray int[]
     * @param format int
     */	
    public void putBuffer(int[] intArray, int format)
    {
        putBuffer(intArray, format, TEX_BYTE);
    }

    /**
     * Copies intArray into the texture, using the specified format and assuming that the pixels are unsigned bytes.
     * @param intArray int[]
     * @param format int
     */
    public void putByteBuffer(int[] intArray, int format)
    {
        putBuffer(intArray, format, TEX_BYTE);
    }

    /**
     * Copies intArray into the texture, using the specified format and assuming that the pixels are integers.
     * @param intArray int[]
     * @param format int
     */
    public void putIntBuffer(int[] intArray, int format)
    {
        putBuffer(intArray, format, TEX_INT);
    }

    /**
     * Copies intArray into the texture, using the format and type specified.
     * @param intArray int[]
     * @param format int
     * @param type int
     */	
    public void putBuffer(int[] intArray, int format, int type)
    {
        if (glTexID[0] == 0)
        {
            initTexture(width, height);
        }      
  
        int[] convArray = intArray;
        int glFormat;
        if (format == ALPHA)
        {
          glFormat = GL10.GL_ALPHA;
          if (type == TEX_BYTE)
          {
              byte[] convArray2 = convertToAlpha(intArray);
              gl.glBindTexture(glTexTarget, glTexID[0]);
              gl.glTexSubImage2D(glTexTarget, 0, 0, 0, width, height, glFormat, GL10.GL_UNSIGNED_BYTE, ByteBuffer.wrap(convArray2));
              gl.glBindTexture(glTexTarget, 0);
              return;
          }
        }
        else if (format == RGB)
        {
          // If in the previous case, we need to use a byte array, here with RGB we should use a byte array with 3 bytes per color, 
          // i.e.: byte[] convArray3 = byte[3 * widht * height] and then storing RGB components from intArray as follows:
          // for (int i = 0; i < width * height; i++) { 
          // convArray3[3 * i ] = red(intArray[i]);
          // convArray3[3 * i + 1] = green(intArray[i]);
          // convArray3[3 * i + 2] = blue(intArray[i]);
          //}
          // where the red, green, green and blue operators involve the correct bit shifting to extract the component from the int value.          
        	glFormat = GL10.GL_RGB;
        	if (type == TEX_BYTE) convArray = convertToRGBA(intArray, RGB);
        }
        else
        {
        	glFormat = GL10.GL_RGBA;
        	if (type == TEX_BYTE) convArray = convertToRGBA(intArray, ARGB);
        }
        
        int glType;
        
        // No GL_INT in ES
        //if (type == TEX_INT) glType = GL10.GL_INT;
        //else glType = GL10.GL_UNSIGNED_BYTE;
        glType = GL10.GL_UNSIGNED_BYTE;
        
        gl.glBindTexture(glTexTarget, glTexID[0]);
                
        /*
        // Apparently no 1D textures and mipmaps in OpenGL ES.
        if (texTarget == GL10.GL_TEXTURE_1D)
        {
            gl.glTexSubImage1D(texTarget, 0, 0, width, glFormat, glType, IntBuffer.wrap(convArray));
        }
        else
        {
            if (usingMipmaps)
                GLState.glu.gluBuild2DMipmaps(texTarget, texInternalFormat, width, height, glFormat, glType, IntBuffer.wrap(convArray));
            else
            	gl.glTexSubImage2D(texTarget, 0, 0, 0, width, height, glFormat, glType, IntBuffer.wrap(convArray));
        }
        */
        
        gl.glTexSubImage2D(glTexTarget, 0, 0, 0, width, height, glFormat, glType, IntBuffer.wrap(convArray));

        gl.glBindTexture(glTexTarget, 0);
    }
    
    /**
     * Copies floatArray into the texture, assuming that the array has 4 components.
     * @param floatArray float[]
     * @param format int
     */
    public void putBuffer(float[] floatArray)
    {
        putBuffer(floatArray, 4);
    }

    /**
     * Copies floatArray into the texture, using the specified number of channels.
     * @param floatArray float[]
     * @param nchan int
     */
    public void putBuffer(float[] floatArray, int nchan)
    {
        if (glTexID[0] == 0)
        {
            initTexture(width, height);
        }

        int glFormat;
        if (nchan == 1) glFormat = GL10.GL_LUMINANCE;
        else if (nchan == 3) glFormat = GL10.GL_RGB;
        else glFormat = GL10.GL_RGBA;

        gl.glBindTexture(glTexTarget, glTexID[0]);

        /*
        if (texTarget == GL.GL_TEXTURE_1D)
            gl.glTexSubImage1D(texTarget, 0, 0, width, glFormat, GL10.GL_FLOAT, FloatBuffer.wrap(floatArray));
        else
            gl.glTexSubImage2D(texTarget, 0, 0, 0, width, height, glFormat, GL10.GL_FLOAT, FloatBuffer.wrap(floatArray));
        */
        
        gl.glTexSubImage2D(glTexTarget, 0, 0, 0, width, height, glFormat, GL10.GL_FLOAT, FloatBuffer.wrap(floatArray));
        
        gl.glBindTexture(glTexTarget, 0);
    }

    /**
     * Copies the texture into intArray, assuming that the array has 4 components and the pixels are unsigned bytes.
     * @param intArray int[]
     * @param format int
     */
    public void getBuffer(int[] intArray)
    {
        getBuffer(intArray, ARGB, TEX_BYTE);
    }

    /**
     * Copies the texture into intArray, using the specified format and assuming that the pixels are unsigned bytes.
     * @param intArray int[]
     * @param format int
     */
    public void getBuffer(int[] intArray, int format)
    {
        getBuffer(intArray, format, TEX_BYTE);
    }

    /**
     * Copies the texture into intArray, using the specified format and assuming that the pixels are unsigned bytes.
     * @param intArray int[]
     * @param format int
     */
    public void getByteBuffer(int[] intArray, int format)
    {
        getBuffer(intArray, format, TEX_BYTE);
    }

    /**
     * Copies the texture into intArray, using the specified format and assuming that the pixels are integers.
     * @param intArray int[]
     * @param format int
     */
    public void getIntBuffer(int[] intArray, int format)
    {
        getBuffer(intArray, format, TEX_INT);
    }

    /**
     * Copies the texture into intArray, using the specified format and type. The resulting array
     * is not reordered into ARGB, it follows OpenGL color component ordering (for instance, for
     * 4 components, little-endian this would be ABGR). Perhaps ordering should be done automatically?
     * @param intArray int[]
     * @param format int
     * @param type int
     */
    public void getBuffer(int[] intArray, int format, int type)
    {
        int mult;
        int glFormat;
        if (format == ALPHA) 
        { 
            mult = 1;
            glFormat = GL10.GL_LUMINANCE;
        }
        else if (format == RGB) 
        { 
            mult = 3;
            glFormat = GL10.GL_RGB;
        }
        else
        { 
            mult = 4;
            glFormat = GL10.GL_RGBA;
        }

        int size;
        int glType;
        if (type == TEX_INT)
        {
//            glType = GL10.GL_INT;
            glType = GL10.GL_UNSIGNED_BYTE;
        }
        else
        {
            mult = 1;
            glType = GL10.GL_UNSIGNED_BYTE;
        }
        size = mult * width * height;

        if (intArray.length != size) 
        {
            System.err.println("Wrong size of buffer!");
            return;
        }

        IntBuffer buffer = BufferUtil.newIntBuffer(size);
		
        gl.glBindTexture(glTexTarget, glTexID[0]);
        
        // Not Available:
        //gl.glGetTexImage(texTarget, 0, glFormat, glType, buffer); 
        
        // TODO:
        // There is no GetTexImage, but if the texture is in a renderable format (RGB or RGBA, not L, A, or LA) 
        // then you can bind it to an FBO and use glReadPixels.
        // From: http://www.idevgames.com/forum/showthread.php?t=17044
        // Unfortunately, FBO support seems to be available only on iPhone through an custom extension (glBindFramebufferOES, etc).
        // Read also note on opengl page on android site:
        // http://developer.android.com/guide/topics/graphics/opengl.html
        // Specifically:
        // "Finally, note that though Android does include some basic support for OpenGL ES 1.1, the support is not complete, 
        // and should not be relied upon at this time."
        // :-(
        
        gl.glBindTexture(glTexTarget, 0);
		
        buffer.get(intArray);
        
        if (flippedX) flipArrayOnX(intArray, mult);
        if (flippedY) flipArrayOnY(intArray, mult);
    }

    /**
     * Copies the texture into floatArray.
     * @param floatArray float[]
     * @param format int
     */
    public void getBuffer(float[] floatArray, int nchan)
    {
        int mult;
//        int glFormat;
        if (format == 1) 
        { 
            mult = 1;
//            glFormat = GL10.GL_LUMINANCE;
        }
        else if (format == 3) 
        { 
            mult = 3;
//            glFormat = GL10.GL_RGB;
        }
        else
        { 
            mult = 4;
//            glFormat = GL10.GL_RGBA;
        }

        int size = mult * width * height;
        if (floatArray.length != size) 
        {
            System.err.println("Wrong size of buffer!");
            return;
        }

        //FloatBuffer buffer = BufferUtil.newFloatBuffer(size);
        FloatBuffer buffer = FloatBuffer.allocate(size);
		
        gl.glBindTexture(glTexTarget, glTexID[0]);
        //gl.glGetTexImage(texTarget, 0, glFormat, GL10.GL_FLOAT, buffer);
        gl.glBindTexture(glTexTarget, 0);
		
        buffer.get(floatArray);

        if (flippedX) flipArrayOnX(floatArray, mult);
        if (flippedY) flipArrayOnY(floatArray, mult);
    }

    /**
     * Sets the texture to have random values in the ranges specified for each component.
     * @param r0 float
     * @param r1 float
     * @param g0 float
     * @param g1 float
     * @param b0 float
     * @param b1 float
     * @param a0 float
     * @param a1 float
     */
    public void setRandom(float r0, float r1, float g0, float g1, float b0, float b1, float a0, float a1)
    {
        float randBuffer[] = new float[4 * width * height];
        for (int j = 0; j < height; j++)
            for (int i = 0; i < width; i++)
            {
                randBuffer[i * 4 + j * width * 4] = parent.random(r0, r1);
                randBuffer[i * 4 + j * width * 4 + 1] = parent.random(g0, g1);
                randBuffer[i * 4 + j * width * 4 + 2] = parent.random(b0, b1);
                randBuffer[i * 4 + j * width * 4 + 3] = parent.random(a0, a1);
            }
        putBuffer(randBuffer);
    }

    /**
     * Sets the texture to have random values in the first two coordinates chosen on the circular region defined by the parameters.
     * @param r0 float
     * @param r1 float
     * @param phi0 float
     * @param phi1 float
     */
    public void setRandomDir2D(float r0, float r1, float phi0, float phi1)
    {
        float r, phi;
        float randBuffer[] = new float[4 * width * height];
        for (int j = 0; j < height; j++)
            for (int i = 0; i < width; i++)
            {
                r = parent.random(r0, r1);
                phi = parent.random(phi0, phi1);
                randBuffer[i * 4 + j * width * 4] = r * PApplet.cos(phi);
                randBuffer[i * 4 + j * width * 4 + 1] = r * PApplet.sin(phi);
                randBuffer[i * 4 + j * width * 4 + 2] = 0.0f;
                randBuffer[i * 4 + j * width * 4 + 3] = 0.0f;
            }
        putBuffer(randBuffer);
    }

    /**
     * Sets the texture to have random values in the first three coordinates chosen on the spherical region defined by the parameters.
     * @param r0 float
     * @param r1 float
     * @param phi0 float
     * @param phi1 float
     * @param theta0 float
     * @param theta1 float
     */
    public void setRandomDir3D(float r0, float r1, float phi0, float phi1, float theta0, float theta1)
    {
        float r, phi, theta;
        float randBuffer[] = new float[4 * width * height];
        for (int j = 0; j < height; j++)
            for (int i = 0; i < width; i++)
            {
                r = parent.random(r0, r1);
                phi = parent.random(phi0, phi1);
                theta = parent.random(theta0, theta1);

                randBuffer[i * 4 + j * width * 4] = r * PApplet.cos(phi) * PApplet.sin(theta);
                randBuffer[i * 4 + j * width * 4 + 1] = r * PApplet.sin(phi) * PApplet.sin(theta);
                randBuffer[i * 4 + j * width * 4 + 2] = r * PApplet.cos(theta);
                randBuffer[i * 4 + j * width * 4 + 3] = 0.0f;
            }
        putBuffer(randBuffer);
    }

    /**
     * Sets the texture to have the same given float value in each component.
     * @param r float
     * @param g float
     * @param b float
     * @param a float
     */
    public void setValue(float r, float g, float b, float a)
    {
        float valBuffer[] = new float[4 * width * height];
        for (int j = 0; j < height; j++)
            for (int i = 0; i < width; i++)
            {
                valBuffer[i * 4 + j * width * 4] = r;
                valBuffer[i * 4 + j * width * 4 + 1] = g;
                valBuffer[i * 4 + j * width * 4 + 2] = b;
                valBuffer[i * 4 + j * width * 4 + 3] = a;
            }
        putBuffer(valBuffer);
    }

    /**
     * Sets to zero all the pixels of the texture.
     */
    public void setZero()
    {
        setValue(0.0f, 0.0f, 0.0f, 0.0f);
    }

    /**
     * Fills the texture with the specified gray tone.
     * @param gray int
     */
    public void clear(int gray)
    {
        int c = parent.color(gray);
        //glstate.clearTex(tex[0], texTarget, c);
    }

    /**
     * Fills the texture with the specified gray tone.
     * @param gray float
     */
    public void clear(float gray) 
    {
        int c = parent.color(gray);
        //glstate.clearTex(tex[0], texTarget, c);
    }

    /**
     * Fills the texture with the specified gray tone and alpha value.
     * @param gray int
     * @param alpha int
     */
    public void clear(int gray, int alpha) 
    {
        int c = parent.color(gray, alpha);
        //glstate.clearTex(tex[0], texTarget, c);
    }

    /**
     * Fills the texture with the specified rgb color and alpha value.
     * @param rgb int
     * @param alpha float
     */
    public void clear(int rgb, float alpha) 
    {
        int c = parent.color(rgb, alpha);
        //glstate.clearTex(tex[0], texTarget, c);
    }

    /**
     * Fills the texture with the specified gray tone and alpha value.
     * @param gray float
     * @param alpha float
     */
    public void clear(float gray, float alpha)
    {
        int c = parent.color(gray, alpha);
        //glstate.clearTex(tex[0], texTarget, c);
    }

    /**
     * Fills the texture with the specified color components.
     * @param x int
     * @param y int
     * @param z int
     */
    public void clear(int x, int y, int z) 
    {
        int c = parent.color(x, y, z);
        //glstate.clearTex(tex[0], texTarget, c);
    }

    /**
     * Fills the texture with the specified color components.
     * @param x float
     * @param y float
     * @param z float
     */
    public void clear(float x, float y, float z) 
    {
        int c = parent.color(x, y, z);
        //glstate.clearTex(tex[0], texTarget, c);
    }

    /**
     * Fills the texture with the specified color components and alpha component.
     * @param x int
     * @param y int
     * @param z int
     * @param a int
     */
    public void clear(int x, int y, int z, int a) 
    {
        int c = parent.color(x, y, z, a);
        //glstate.clearTex(tex[0], texTarget, c);
    }

    /**
     * Fills the texture with the specified color components and alpha component.
     * @param x float
     * @param y float
     * @param z float
     * @param a float
     */
    public void clear(float x, float y, float z, float a)
    {
        int c = parent.color(x, y, z, a);
        //glstate.clearTex(tex[0], texTarget, c);
    }

    /**
     * Paints the texture with the specified gray tone.
     * @param gray int
     */
    public void paint(int gray)
    {
        int c = parent.color(gray);
        //glstate.paintTex(tex[0], texTarget, width, height, c);
    }

    /**
     * Paints the texture with the specified gray tone.
     * @param gray float
     */
    public void paint(float gray) 
    {
        int c = parent.color(gray);
        //glstate.paintTex(tex[0], texTarget, width, height, c);
    }

    /**
     * Paints the texture with the specified gray tone and alpha value.
     * @param gray int
     * @param alpha int
     */
    public void paint(int gray, int alpha) 
    {
        int c = parent.color(gray, alpha);
        //glstate.paintTex(tex[0], texTarget, width, height, c);
    }

    /**
     * Paints the texture with the specified rgb color and alpha value.
     * @param rgb int
     * @param alpha float
     */
    public void paint(int rgb, float alpha) 
    {
        int c = parent.color(rgb, alpha);
        //glstate.paintTex(tex[0], texTarget, width, height, c);
    }

    /**
     * Paints the texture with the specified gray tone and alpha value.
     * @param gray float
     * @param alpha float
     */
    public void paint(float gray, float alpha)
    {
        int c = parent.color(gray, alpha);
        //glstate.paintTex(tex[0], texTarget, width, height, c);
    }

    /**
     * Paints the texture with the specified color components.
     * @param x int
     * @param y int
     * @param z int
     */
    public void paint(int x, int y, int z) 
    {
        int c = parent.color(x, y, z);
        //glstate.paintTex(tex[0], texTarget, width, height, c);
    }

    /**
     * Paints the texture with the specified color components.
     * @param x float
     * @param y float
     * @param z float
     */
    public void paint(float x, float y, float z) 
    {
        int c = parent.color(x, y, z);
        //glstate.paintTex(tex[0], texTarget, width, height, c);
    }

    /**
     * Paints the texture with the specified color components and alpha component.
     * @param x int
     * @param y int
     * @param z int
     * @param a int
     */
    public void paint(int x, int y, int z, int a) 
    {
        int c = parent.color(x, y, z, a);
        //glstate.paintTex(tex[0], texTarget, width, height, c);
    }

    /**
     * Paints the texture with the specified color components and alpha component.
     * @param x float
     * @param y float
     * @param z float
     * @param a float
     */
    public void paint(float x, float y, float z, float a)
    {
        int c = parent.color(x, y, z, a);
        //glstate.paintTex(tex[0], texTarget, width, height, c);
    }
    
    /**
     * Flips intArray along the X axis.
     * @param intArray int[]
     * @param mult int
     */
    protected void flipArrayOnX(int[] intArray, int mult)
    {
        int index = 0;
        int xindex = mult * (width - 1);
        for (int x = 0; x < width / 2; x++) 
        {
             for (int y = 0; y < height; y++)
             {
                 int i = index + mult * y * width;
                 int j = xindex + mult * y * width;

                 for (int c = 0; c < mult; c++) 
                 {
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
    protected void flipArrayOnY(int[] intArray, int mult)
    {
        int index = 0;
        int yindex = mult * (height - 1) * width;
        for (int y = 0; y < height / 2; y++) 
        {
             for (int x = 0; x < mult * width; x++) 
             {
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
     * Flips floatArray along the X axis.
     * @param intArray int[]
     * @param mult int
     */
    protected void flipArrayOnX(float[] floatArray, int mult)
    {
        int index = 0;
        int xindex = mult * (width - 1);
        for (int x = 0; x < width / 2; x++) 
        {
             for (int y = 0; y < height; y++)
             {
                 int i = index + mult * y * width;
                 int j = xindex + mult * y * width;

                 for (int c = 0; c < mult; c++) 
                 {
                     float temp = floatArray[i];
                     floatArray[i] = floatArray[j];
                     floatArray[j] = temp;
                 
                     i++;
                     j++;
                 }

            }
            index += mult;
            xindex -= mult;
        }
    }

    /**
     * Flips floatArray along the Y axis.
     * @param intArray int[]
     * @param mult int
     */
    protected void flipArrayOnY(float[] floatArray, int mult)
    {
        int index = 0;
        int yindex = mult * (height - 1) * width;
        for (int y = 0; y < height / 2; y++) 
        {
             for (int x = 0; x < mult * width; x++) 
             {
                  float temp = floatArray[index];
                  floatArray[index] = floatArray[yindex];
                  floatArray[yindex] = temp;

                  index++;
                  yindex++;
            }
            yindex -= mult * width * 2;
        }
	}

    /**
     * @invisible
     * Reorders a pixel array in ARGB format into the order required by OpenGL (RGBA).
     * @param intArray int[]
     * @param arrayFormat int	 
     */
    protected int[] convertToRGBA(int[] intArray, int arrayFormat)
    {
        int twidth = width;
        int t = 0; 
        int p = 0;
        int[] tIntArray = new int[width * height];        
        if (PGraphicsAndroid3D.BIG_ENDIAN) 
        {
            switch (arrayFormat) 
            {
                case ALPHA:
                	
                    for (int y = 0; y < height; y++) 
                    {
                        for (int x = 0; x < width; x++) 
                        {
                            tIntArray[t++] = 0xFFFFFF00 | intArray[p++];
                        }
                        t += twidth - width;
                    }
                    break;

                case RGB:
                	
                    for (int y = 0; y < height; y++) 
                    {
                        for (int x = 0; x < width; x++) 
                        {
                            int pixel = intArray[p++];
                            tIntArray[t++] = (pixel << 8) | 0xff;
                        }
                        t += twidth - width;
                    }
                    break;

                case ARGB:
                	
                    for (int y = 0; y < height; y++) 
                    {
                        for (int x = 0; x < width; x++) 
                        {
                            int pixel = intArray[p++];
                            tIntArray[t++] = (pixel << 8) | ((pixel >> 24) & 0xff);
                        }
                        t += twidth - width;
                    }
                    break;
                    
            }       
        } 
        else 
        {  
            // LITTLE_ENDIAN
            // ARGB native, and RGBA opengl means ABGR on windows
            // for the most part just need to swap two components here
            // the sun.cpu.endian here might be "false", oddly enough..
            // (that's why just using an "else", rather than check for "little")
        
            switch (arrayFormat) 
            {    
                case ALPHA:
            	
                    for (int y = 0; y < height; y++) 
                    {
                        for (int x = 0; x < width; x++) 
                        {
                            tIntArray[t++] = (intArray[p++] << 24) | 0x00FFFFFF;
                        }
                        t += twidth - width;
                    }
                    break;

                case RGB:
                	
                    for (int y = 0; y < height; y++) 
                    {
                        for (int x = 0; x < width; x++) 
                        {
                            int pixel = intArray[p++];
                            // needs to be ABGR, stored in memory xRGB
                            // so R and B must be swapped, and the x just made FF
                            tIntArray[t++] =
                                0xff000000 |  // force opacity for good measure
                                ((pixel & 0xFF) << 16) |
                                ((pixel & 0xFF0000) >> 16) |
                                (pixel & 0x0000FF00);
                        }
                        t += twidth - width;
                    }
                    break;

                case ARGB:
                    	
                    for (int y = 0; y < height; y++) 
                    {
                        for (int x = 0; x < width; x++) 
                         {
                             int pixel = intArray[p++];
                             // needs to be ABGR stored in memory ARGB
                             // so R and B must be swapped, A and G just brought back in
                             tIntArray[t++] =
                                 ((pixel & 0xFF) << 16) |
                                 ((pixel & 0xFF0000) >> 16) |
                                 (pixel & 0xFF00FF00);
                         }
                         t += twidth - width;
                    }
                    break;
          
            }
        
        }
        
        return tIntArray;    
    }
        
    /**
     * @invisible
     * Reorders a pixel array in RGBA format into ARGB.
     * @param intArray int[]	 
     */    
    protected int[] convertToARGB(int[] intArray)
    {
        int twidth = width;
        int t = 0; 
        int p = 0;
        int[] tIntArray = new int[width * height];        
        if (PGraphicsAndroid3D.BIG_ENDIAN) 
        {
            for (int y = 0; y < height; y++) 
            {
                for (int x = 0; x < width; x++) 
                {
                    int pixel = intArray[p++];
                    tIntArray[t++] = (pixel >> 8) | ((pixel << 24) & 0xff);
                }
                t += twidth - width;
            }
        } 
        else 
        {  
            // LITTLE_ENDIAN
            // ARGB native, and RGBA opengl means ABGR on windows
            // for the most part just need to swap two components here
            // the sun.cpu.endian here might be "false", oddly enough..
            // (that's why just using an "else", rather than check for "little")
        
            for (int y = 0; y < height; y++) 
            {
                for (int x = 0; x < width; x++) 
                 {
                     int pixel = intArray[p++];
                     
                     // needs to be ARGB stored in memory ABGR (RGBA = ABGR -> ARGB) 
                     // so R and B must be swapped, A and G just brought back in
                     tIntArray[t++] =
                         ((pixel & 0xFF) << 16) |
                         ((pixel & 0xFF0000) >> 16) |
                         (pixel & 0xFF00FF00);
                     
                 }
                 t += twidth - width;
            }

        }
        
        return tIntArray;    
    }
    
    /**
     * @invisible
     * Creates a byte version of intArray..
     * @param intArray int[]   
     */    
    protected byte[] convertToAlpha(int[] intArray)
    {
        byte[] tByteArray = new byte[width * height];        
        for (int i = 0; i < width * height; i++)
        {
           tByteArray[i] = (byte)(intArray[i]);
        }      
        return tByteArray;      
    }    
    
    /**
     * @invisible
     * Creates the opengl texture object.
     * @param w int
     * @param h int	 
     */
    protected void initTexture(int w, int h)
    {
        if (glTexID[0] != 0)
        {
            releaseTexture();
        }
        
        gl.glGenTextures(1, glTexID, 0);
        gl.glBindTexture(glTexTarget, glTexID[0]);
        gl.glTexParameterf(glTexTarget, GL10.GL_TEXTURE_MIN_FILTER, glMinFilter);
        gl.glTexParameterf(glTexTarget, GL10.GL_TEXTURE_MAG_FILTER, glMagFilter);
        gl.glTexParameterf(glTexTarget, GL10.GL_TEXTURE_WRAP_S, GL10.GL_CLAMP_TO_EDGE);
        gl.glTexParameterf(glTexTarget, GL10.GL_TEXTURE_WRAP_T, GL10.GL_CLAMP_TO_EDGE);
        
        
        
        //if (texTarget == GL.GL_TEXTURE_1D) gl.glTexImage1D(texTarget, 0, texInternalFormat, w, 0, GL.GL_RGBA, GL.GL_UNSIGNED_BYTE, null);
        //else gl.glTexImage2D(texTarget, 0, texInternalFormat, w, h, 0, GL.GL_RGBA, GL.GL_UNSIGNED_BYTE, null);
        
        gl.glTexImage2D(glTexTarget, 0, GL10.GL_RGBA/*texInternalFormat*/, w, h, 0, GL10.GL_RGBA, GL10.GL_UNSIGNED_BYTE, null);
        gl.glBindTexture(glTexTarget, 0);
		
        /*
        No GL10.GL_TEXTURE_RECTANGLE
        if (texTarget == GL10.GL_TEXTURE_RECTANGLE)
        {
            maxTexCoordS = w;
            maxTexCoordT = h;
        }
        else
        {
            maxTexCoordS = 1.0f;
            maxTexCoordT = 1.0f; 
        }
        */
        maxTexCoordS = 1.0f;
        maxTexCoordT = 1.0f; 
        
    }

    
    /**
     * @invisible
     * Deletes the opengl texture object.
     */
    protected void releaseTexture()
    {
        gl.glDeleteTextures(1, glTexID, 0);  
        glTexID[0] = 0;
    }
    
    /**
     * @invisible
     * Sets texture target and internal format according to the target and  type specified.
     * @param target int		   
     * @param params GLTextureParameters
     */		
    protected void setTextureParams(Parameters params)
    {
	    if (params.target == TEX_NORM)
      {            
        glTexTarget = GL10.GL_TEXTURE_2D;
      }
	    /*
	    else if (params.target == TEX_RECT)
        {
            texTarget = GL10.GL_TEXTURE_RECTANGLE;		
        }
	    else if (params.target == TEX_ONEDIM)
        {
            texTarget = GL10.GL_TEXTURE_1D;
        }
        */
	    
        if (params.format == RGB) 
        {
            glTexInternalFormat = GL10.GL_RGB;
        } 
        if (params.format == ARGB) 
        {
            glTexInternalFormat = GL10.GL_RGBA;
        } 
        if (params.format == ALPHA) 
        {
            glTexInternalFormat = GL10.GL_ALPHA;
        }         
	    

	    if (params.minFilter == NEAREST)
	    {
            glMinFilter = GL10.GL_NEAREST;
        }
	    else if (params.minFilter == LINEAR)
        {
            glMinFilter = GL10.GL_LINEAR;
	    }
	    else if (params.minFilter == NEAREST_MIPMAP_NEAREST)
        {
            glMinFilter = GL10.GL_NEAREST_MIPMAP_NEAREST;
	    }
	    else if (params.minFilter == LINEAR_MIPMAP_NEAREST)
        {
            glMinFilter = GL10.GL_LINEAR_MIPMAP_NEAREST;
	    }		
	    else if (params.minFilter == NEAREST_MIPMAP_LINEAR)
        {
            glMinFilter = GL10.GL_NEAREST_MIPMAP_LINEAR;
	    }
	    else if (params.minFilter == LINEAR_MIPMAP_LINEAR)
        {
            glMinFilter = GL10.GL_LINEAR_MIPMAP_LINEAR;
	    }		

	    if (params.magFilter == NEAREST)
	    {
            glMagFilter = GL10.GL_NEAREST;
        }
	        else if (params.magFilter == LINEAR)
        {
            glMagFilter = GL10.GL_LINEAR;
	    }

        usingMipmaps = (glMinFilter == GL10.GL_NEAREST_MIPMAP_NEAREST) ||
                       (glMinFilter == GL10.GL_LINEAR_MIPMAP_NEAREST) ||
                       (glMinFilter == GL10.GL_NEAREST_MIPMAP_LINEAR) ||
                       (glMinFilter == GL10.GL_LINEAR_MIPMAP_LINEAR);
					   
        flippedX = false;
        flippedY = false;		
    }	
	
    /**
     * @invisible
     * Generates a power-of-two box width and height so that width * height is closest to size.
     * @param size int
     */	
    protected void calculateWidthHeight(int size)
    {
        int w, h;
        float l = PApplet.sqrt(size);
        for (w = 2; w < l; w *= 2);
        int n0 = w * w;
        int n1 = w * w / 2;
        if (PApplet.abs(n0 - size) < PApplet.abs(n1 - size)) h = w;
        else h = w / 2;
        
        width = w;
        height = h;
    }
     

    
    
  /////////////////////////////////////////////////////////////////////////// 

    
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
     * Creates an instance of GLTextureParameters, setting all the parameters to default values.
     */
    public Parameters() {
      target = GLTexture.TEX_NORM;
      format = GLTexture.COLOR;
      minFilter = GLTexture.LINEAR;
      magFilter = GLTexture.LINEAR;   
    }
      
    public Parameters(int format) {
      target = GLTexture.TEX_NORM;
      this.format = format;
      minFilter = GLTexture.LINEAR;
      magFilter = GLTexture.LINEAR;   
    }

    public Parameters(int format, int filter) {
      target = GLTexture.TEX_NORM;
      this.format = format;
      minFilter = filter;
      magFilter = filter;   
    }

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
  }
}
