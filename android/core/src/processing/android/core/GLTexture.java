package processing.android.core;

import java.nio.IntBuffer;

import javax.microedition.khronos.opengles.*;

import java.nio.*;

// TODO:
// add texture name property.
// Fix the setBuffer methods to make them into setTexChannel or something like that.
// Overload the loadPixels and updatePixels so that texture info is copies to data[] array instead to
// pixels[] when the texture is floating point.

/**
 * This class adds an opengl texture to a PImage object. The texture is 
 * handled in a similar way to the pixels property: image data can be copied 
 * to and from the texture using loadTexture and updateTexture methods.
 * However, bringing the texture down to image or pixels data can slow down 
 * the application considerably (since involves copying texture data from GPU 
 * to CPU), especially when handling large textures. So it is recommended 
 * to do all the texture handling without calling updateTexture, and doing so 
 * only at the end if the texture is needed as a regular image.
 */
@SuppressWarnings("unused")
public class GLTexture extends PImage implements PConstants 
{
  /**
   * This constant identifies the texture target GL_TEXTURE_2D, 
   * that is, textures with normalized coordinates.
   */
  public static final int TEX_NORM = 0;
  /**
   * This constant identifies the texture target GL_TEXTURE_RECTANGLE, 
   * that is, textures with non-normalized coordinates
   */
  public static final int TEX_RECT = 1;
  /**
   * This constant identifies the texture target GL_TEXTURE_1D, that is, one-dimensional textures.
   */
  public static final int TEX_ONEDIM = 2; 

  /**
   * This constant identifies the texture internal format GL_RGBA: 4 color components of 8 bits each.
   */
  public static final int COLOR = 0;  
  /**
   * This constant identifies the texture internal format GL_RGBA16F_ARB: 4 float compontents of 16 bits each.
   */
  public static final int FLOAT = 1;
  /**
   * This constant identifies the texture internal format GL_RGBA32F_ARB: 4 float compontents of 32 bits each.
   */ 
  public static final int DOUBLE = 2;  

  /**
   * This constant identifies an image buffer that contains only RED channel info.
   */   
  public static final int TEX1 = 0;
  
  /**
   * This constant identifies an image buffer that contains only GREEN channel info.
   */   
  //public static final int GREEN = 0;

  /**
   * This constant identifies an image buffer that contains only BLUE channel info.
   */   
  //public static final int BLUE = 0;
  /**
   * This constant identifies an image buffer that contains only ALPHA channel info.
   */   
  //public static final int ALPHA = 0; Already defined in Processing with value = 4

  
  
  /**
   * This constant identifies a texture with 3 color components.
   */   
  public static final int TEX3 = 1;
  
  /**
   * This constant identifies an image buffer that contains RGB channel info.
   */   
  //public static final int RGB = 0;  Already defined in Processing with value = 1    
  
  /**
   * This constant identifies an image buffer that contains RGB channel info.
   */   
  //public static final int ARGB = 0;  Already defined in Processing with value = 2   
  
  
  /**
   * This constant identifies a texture with 4 color components.
   */   
  public static final int TEX4 = 2;

  /**
   * This constant identifies an integer texture buffer.
   */
  public static final int TEX_INT = 0;
  /**
   * This constant identifies an unsigned byte texture buffer.
   */
  public static final int TEX_BYTE = 1;

  /**
   * This constant identifies the nearest texture filter .
   */
  public static final int NEAREST = 0;
  /**
   * This constant identifies the linear texture filter .
   */
  public static final int LINEAR = 1;
  /**
   * This constant identifies the nearest/nearest function to build mipmaps .
   */
  public static final int NEAREST_MIPMAP_NEAREST = 2;
  /**
   * This constant identifies the linear/nearest function to build mipmaps .
   */
  public static final int LINEAR_MIPMAP_NEAREST = 3;
  /**
   * This constant identifies the nearest/linear function to build mipmaps .
   */
  public static final int NEAREST_MIPMAP_LINEAR = 4;
  /**
   * This constant identifies the linear/linear function to build mipmaps .
   */
  public static final int LINEAR_MIPMAP_LINEAR = 5;
  
  /**
   * These constants identifies the texture parameter types.
   */
  public static final int TEX_FILTER_PARAM_INT = 0;
  public static final int TEX_FILTER_PARAM_FLOAT = 1;
  public static final int TEX_FILTER_PARAM_VEC2 = 2;  
  public static final int TEX_FILTER_PARAM_VEC3 = 3;
  public static final int TEX_FILTER_PARAM_VEC4 = 4;
  public static final int TEX_FILTER_PARAM_MAT2 = 5;
  public static final int TEX_FILTER_PARAM_MAT3 = 6;
  public static final int TEX_FILTER_PARAM_MAT4 = 7;

  public static final int GL_DEPTH_STENCIL = 0x84F9;
  public static final int GL_UNSIGNED_INT_24_8 = 0x84FA;
  public static final int GL_DEPTH24_STENCIL8 = 0x88F0;
  
  public static final int BACKGROUND_ALPHA = 16384;
  
  
    /**
     * Creates an instance of GLTexture with size 1x1. The texture is not initialized.
     * @param parent PApplet
     */
    public GLTexture(PApplet parent)
    {
        super(1, 1, ARGB);
        this.parent = parent;
       
        pgl = (PGraphicsAndroid3D)parent.g;
        gl = pgl.gl;
        //glstate = new GLState(gl);
	    setTextureParams(new Parameters());
    }  

    /**
     * Creates an instance of GLTexture with size width x height. The texture is initialized (empty) to that size.
     * @param parent PApplet
     * @param width int 
     * @param height int 
     */	 
    public GLTexture(PApplet parent, int width, int height)
    {
        super(width, height, ARGB);  
        this.parent = parent;
       
        pgl = (PGraphicsAndroid3D)parent.g;
        gl = pgl.gl;
        //glstate = new GLState(gl);
	    setTextureParams(new Parameters());
       
        initTexture(width, height);
    }
    
    /**
     * Creates an instance of GLTexture with size width x height and with the specified parameters.
     *  The texture is initialized (empty) to that size.
     * @param parent PApplet
     * @param width int 
     * @param height int 
     * @param params GLTextureParameters 			
     */	 
    public GLTexture(PApplet parent, int width, int height, Parameters params)
    {
        super(width, height, ARGB);  
        this.parent = parent;
       
        pgl = (PGraphicsAndroid3D)parent.g;
        gl = pgl.gl;
        //glstate = new GLState(gl);
	    setTextureParams(params);
       
        initTexture(width, height);
    }	
	
    /**
     * Creates an instance of GLTexture with size width x height and with the specified format.
     *  The texture is initialized (empty) to that size.
     * @param parent PApplet
     * @param width int 
     * @param height int 
     * @param format int  			
     */	 
    public GLTexture(PApplet parent, int width, int height, int format)
    {
        super(width, height, ARGB);  
        this.parent = parent;
       
        pgl = (PGraphicsAndroid3D)parent.g;
        gl = pgl.gl;
        //glstate = new GLState(gl);
	    setTextureParams(new Parameters(format));
       
        initTexture(width, height);
    }	

    /**
     * Creates an instance of GLTexture with size width x height and with the specified format and filtering.
     *  The texture is initialized (empty) to that size.
     * @param parent PApplet
     * @param width int 
     * @param height int 
     * @param format int 
     * @param filter int 
     */	 
    public GLTexture(PApplet parent, int width, int height, int format, int filter)
    {
        super(width, height, ARGB);  
        this.parent = parent;
       
        pgl = (PGraphicsAndroid3D)parent.g;
        gl = pgl.gl;
        //glstate = new GLState(gl);
	    setTextureParams(new Parameters(format, filter));
       
        initTexture(width, height);
    }	

    /**
     * Creates an instance of GLTexture using image file filename as source.
     * @param parent PApplet
     * @param filename String
     */	
    public GLTexture(PApplet parent, String filename)
    {
        super(1, 1, ARGB);  
        this.parent = parent;
	   
        pgl = (PGraphicsAndroid3D)parent.g;
        gl = pgl.gl;
        //glstate = new GLState(gl);		
        
        loadTexture(filename);
    }

    public GLTexture(PApplet parent, int width, int height, Parameters params, int id)
    {
        super(width, height, ARGB);  
        this.parent = parent;
       
        pgl = (PGraphicsAndroid3D)parent.g;
        gl = pgl.gl;
        //glstate = new GLState(gl);
	    setTextureParams(params);
       
        initTexture(width, height, id);
    }	
    
    /**
     * Creates an instance of GLTexture using image file filename as source and the specified texture parameters.
     * @param parent PApplet
     * @param filename String
     * @param params GLTextureParameters
     */	
    public GLTexture(PApplet parent, String filename, Parameters params)
    {
        super(1, 1, ARGB);  
        this.parent = parent;
	   
        pgl = (PGraphicsAndroid3D)parent.g;
        gl = pgl.gl;
        //glstate = new GLState(gl);		
        
        loadTexture(filename, params);
    }

    /**
     * Creates an instance of GLTexture using image file filename as source and the specified format.
     * @param parent PApplet
     * @param filename String
     * @param format int 
     */	
    public GLTexture(PApplet parent, String filename, int format)
    {
        super(1, 1, ARGB);  
        this.parent = parent;
	   
        pgl = (PGraphicsAndroid3D)parent.g;
        gl = pgl.gl;
        //glstate = new GLState(gl);		
        
        loadTexture(filename, format);
    }

    /**
     * Creates an instance of GLTexture using image file filename as source and the specified format and filtering.
     * @param parent PApplet
     * @param filename String
     * @param format int 
     * @param filter int 
     */	
    public GLTexture(PApplet parent, String filename, int format, int filter)
    {
        super(1, 1, ARGB);  
        this.parent = parent;
	   
        pgl = (PGraphicsAndroid3D)parent.g;
        gl = pgl.gl;
        //glstate = new GLState(gl);		
        
        loadTexture(filename, format, filter);
    }

    /**
     * Creates an instance of GLTexture with power-of-two width and height that such that width * height is the closest to size. 
     * The texture is initialized (empty) to that size.
     * @param parent PApplet
     * @param size int 
     */	
    public GLTexture(PApplet parent, int size)
    {
        super(1, 1, ARGB);  
        this.parent = parent;
       
        pgl = (PGraphicsAndroid3D)parent.g;
        gl = pgl.gl;
        //glstate = new GLState(gl);
        
        calculateWidthHeight(size);
        init(width, height);
    }

    /**
     * Creates an instance of GLTexture with power-of-two width and height that such that width 
     * height is the closest to size, and with the specified parameters.
     * The texture is initialized (empty) to that size.
     * @param parent PApplet
     * @param size int 
     * @param params GLTextureParameters
     */	 
    public GLTexture(PApplet parent, int size, Parameters params)
    {
        super(1, 1, ARGB);  
        this.parent = parent;
       
        pgl = (PGraphicsAndroid3D)parent.g;
        gl = pgl.gl;
        //glstate = new GLState(gl);

        calculateWidthHeight(size);
        init(width, height, params);
    }

    /**
     * Sets the size of the image and texture to width x height. If the texture is already initialized,
     * it first destroys the current opengl texture object and then creates a new one with the specified
     * size.
     * @param width int
     * @param height int
     */
    public void init(int width, int height)
    {	    init(width, height, new Parameters());
    }

    /**
     * Sets the size of the image and texture to width x height, and the parameters of the texture to params.
     * If the texture is already  initialized, it first destroys the current opengl texture object and then creates 
     * a new one with the specified size.
     * @param width int
     * @param height int
     * @param params GLTextureParameters 
     */
    public void init(int width, int height, Parameters params)
    {
        super.init(width, height, ARGB);
		setTextureParams(params);
        initTexture(width, height);
    }	
	
    /**
     * Returns true if the texture has been initialized.
     * @return boolean
     */  
    public boolean available()
    {
        return 0 < tex[0];
    }
    
    /**
     * Provides the ID of the opegl texture object.
     * @return int
     */	
    public int getTextureID()
    {
        return tex[0];
    }

    /**
     * Returns the texture target.
     * @return int
     */	
    public int getTextureTarget()
    {
        return texTarget;
    }    

    /**
     * Returns the texture internal format.
     * @return int
     */	
    public int getTextureInternalFormat()
    {
        return texInternalFormat;
    }

    /**
     * Returns the texture minimization filter.
     * @return int
     */	
    public int getTextureMinFilter()
    {
        return minFilter;
    }

    /**
     * Returns the texture magnification filter.
     * @return int
     */	
    public int getTextureMagFilter()
    {
        return magFilter;
    }
	
    /**
     * Returns true or false whether or not the texture is using mipmaps.
     * @return boolean
     */	
    public boolean usingMipmaps()
    {
        return usingMipmaps;
    }
	
    /**
     * Returns the maximum possible value for the texture coordinate S.
     * @return float
     */	
    public float getMaxTextureCoordS()
    {
        return maxTexCoordS;
    }
	
    /**
     * Returns the maximum possible value for the texture coordinate T.
     * @return float
     */	
    public float getMaxTextureCoordT()
    {
        return maxTexCoordT;
    }
	
    /**
     * Returns true if the texture is flipped along the horizontal direction.
     * @return boolean;
     */	
    public boolean isFlippedX()
    {
        return flippedX;
    }

    /**
     * Sets the texture as flipped or not flipped on the horizontal direction.
     * @param v boolean;
     */	
    public void setFlippedX(boolean v)
    {
        flippedX = v;
    }	
	
    /**
     * Returns true if the texture is flipped along the vertical direction.
     * @return boolean;
     */	
    public boolean isFlippedY()
    {
        return flippedY;
    }

    /**
     * Sets the texture as flipped or not flipped on the vertical direction.
     * @param v boolean;
     */	
    public void setFlippedY(boolean v)
    {
        flippedY = v;
    }
	
    /**
     * Puts img into texture, pixels and image.
     * @param img PImage
     */
    public void putImage(PImage img)
    {
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
	    putBuffer(img.pixels);
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
	    putBuffer(dest);
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
        gl.glBindTexture(texTarget, tex[0]);
        //gl.glGetTexImage(texTarget, 0, GL10.GL_RGBA, GL10.GL_UNSIGNED_BYTE, buffer);
        gl.glBindTexture(texTarget, 0);       
       
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
        putBuffer(pixels);
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
     * Copy texture to pixels (doesn't call updatePixels).
     */		
    public void updateTexture()
    {
        int size = width * height;
        IntBuffer buffer = BufferUtil.newIntBuffer(size);
		
        gl.glBindTexture(texTarget, tex[0]);
        //gl.glGetTexImage(texTarget, 0, GL10.GL.GL_RGBA, GL10.GL_UNSIGNED_BYTE, buffer);
        gl.glBindTexture(texTarget, 0);
        
        buffer.get(pixels);
        int[] pixelsARGB = convertToARGB(pixels);
        PApplet.arrayCopy(pixelsARGB, pixels);        
        if (flippedX) flipArrayOnX(pixels, 1);
        if (flippedY) flipArrayOnY(pixels, 1);
    }
    
    /**
     * Draws the texture using the opengl commands, inside a rectangle located at the origin with the original
     * size of the texture.
     */		
    public void render()
    {
        render(0, 0, width, height);
    }

    /**
     * Draws the texture using the opengl commands, inside a rectangle located at (x,y) with the original
     * size of the texture.
     * @param x float
     * @param y float
     */	
    public void render(float x, float y)
    {
        render(x, y, width, height);
    }
    
    /**
     * Draws the texture using the opengl commands, inside a rectangle of width w and height h
     * located at (x,y).
     * @param x float
     * @param y float
     * @param w float
     * @param h float	 
     */	
    public void render(float x, float y, float w, float h)
    {
    	render(pgl, x, y, w, h);    		
    }
    
   /**
    * Draws the texture using the opengl commands, inside a rectangle of width w and height h
    * located at (x,y) and using the specified renderer.
    * @param renderer PGraphicsAndroid3D
    * @param x float
    * @param y float
    * @param w float
    * @param h float	 
    */    
    public void render(PGraphicsAndroid3D renderer, float x, float y, float w, float h)
    {
    	render(renderer, x, y, w, h, 0, 0, 1, 1);
    }
    
    public void render(PGraphicsAndroid3D renderer, float x, float y, float w, float h, float sx, float ty, float sw, float th)
    {
    	float fw, fh;
    	if (renderer.textureMode == IMAGE)
    	{
    		fw = width;
    	    fh = height;
    	}
    	else
    	{
    		fw = 1.0f;
    	    fh = 1.0f;
    	}
    		
    	renderer.beginShape(QUADS);
    	    renderer.texture(this);
    	    renderer.vertex(x, y, fw * sx, fh * ty);
    	    renderer.vertex(x + w, y, fw * sw, fh * ty);
    	    renderer.vertex(x + w, y + h, fw * sw, fh * th);
    	    renderer.vertex(x, y + h, fw * sx, fh * th);
    	renderer.endShape();	
    }        
    
    /**
     * Copies intArray into the texture, assuming that the array contains 4 color components and pixels are unsigned bytes.
     * @param intArray int[]
     */	
    public void putBuffer(int[] intArray)
    {
        putBuffer(intArray, TEX4, TEX_BYTE);
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
        if (tex[0] == 0)
        {
            initTexture(width, height);
        }      
  
        int[] convArray = intArray;
        int glFormat;
        if (format == TEX1)
        {
        	glFormat = GL10.GL_LUMINANCE;
        	if (type == TEX_BYTE)
        	{
        		convArray = convertToRGBA(intArray, ALPHA);
        		/*
        		 
        		PApplet.println("Hey");
        		int[] convArray2 = new int[width * height]; 
        		for (int i = 0; i < width * height; i++)
        		{
        			convArray2[i] = 0x88000000;
        		}
        		gl.glBindTexture(texTarget, tex[0]);
        		gl.glTexSubImage2D(texTarget, 0, 0, 0, width, height, GL.GL_RGBA, GL.GL_UNSIGNED_BYTE, IntBuffer.wrap(convArray2));
        		gl.glBindTexture(texTarget, 0);
        		PApplet.println("CHAU");
        		return;
        		*/
        	}
        }
        else if (format == TEX3)
        {
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
        
        gl.glBindTexture(texTarget, tex[0]);
                
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
        
        gl.glTexSubImage2D(texTarget, 0, 0, 0, width, height, glFormat, glType, IntBuffer.wrap(convArray));

        gl.glBindTexture(texTarget, 0);
    }
    
    /**
     * Copies floatArray into the texture, assuming that the array has 4 components.
     * @param floatArray float[]
     * @param format int
     */
    public void putBuffer(float[] floatArray)
    {
        putBuffer(floatArray, TEX4);
    }

    /**
     * Copies floatArray into the texture, using the specified format.
     * @param floatArray float[]
     * @param format int
     */
    public void putBuffer(float[] floatArray, int format)
    {
        if (tex[0] == 0)
        {
            initTexture(width, height);
        }

        int glFormat;
        if (format == TEX1) glFormat = GL10.GL_LUMINANCE;
        else if (format == TEX3) glFormat = GL10.GL_RGB;
        else glFormat = GL10.GL_RGBA;

        gl.glBindTexture(texTarget, tex[0]);

        /*
        if (texTarget == GL.GL_TEXTURE_1D)
            gl.glTexSubImage1D(texTarget, 0, 0, width, glFormat, GL10.GL_FLOAT, FloatBuffer.wrap(floatArray));
        else
            gl.glTexSubImage2D(texTarget, 0, 0, 0, width, height, glFormat, GL10.GL_FLOAT, FloatBuffer.wrap(floatArray));
        */
        
        gl.glTexSubImage2D(texTarget, 0, 0, 0, width, height, glFormat, GL10.GL_FLOAT, FloatBuffer.wrap(floatArray));
        
        gl.glBindTexture(texTarget, 0);
    }

    /**
     * Copies the texture into intArray, assuming that the array has 4 components and the pixels are unsigned bytes.
     * @param intArray int[]
     * @param format int
     */
    public void getBuffer(int[] intArray)
    {
        getBuffer(intArray, TEX4, TEX_BYTE);
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
        if (format == TEX1) 
        { 
            mult = 1;
            glFormat = GL10.GL_LUMINANCE;
        }
        else if (format == TEX3) 
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
		
        gl.glBindTexture(texTarget, tex[0]);
        //gl.glGetTexImage(texTarget, 0, glFormat, glType, buffer);
        gl.glBindTexture(texTarget, 0);
		
        buffer.get(intArray);
        
        if (flippedX) flipArrayOnX(intArray, mult);
        if (flippedY) flipArrayOnY(intArray, mult);
    }

    /**
     * Copies the texture into floatArray.
     * @param floatArray float[]
     * @param format int
     */
    public void getBuffer(float[] floatArray, int format)
    {
        int mult;
//        int glFormat;
        if (format == TEX1) 
        { 
            mult = 1;
//            glFormat = GL10.GL_LUMINANCE;
        }
        else if (format == TEX3) 
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
		
        gl.glBindTexture(texTarget, tex[0]);
        //gl.glGetTexImage(texTarget, 0, glFormat, GL10.GL_FLOAT, buffer);
        gl.glBindTexture(texTarget, 0);
		
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
     * Creates the opengl texture object.
     * @param w int
     * @param h int	 
     */
    protected void initTexture(int w, int h)
    {
        if (tex[0] != 0)
        {
            releaseTexture();
        }
        
        gl.glGenTextures(1, tex, 0);
        gl.glBindTexture(texTarget, tex[0]);
        gl.glTexParameterf(texTarget, GL10.GL_TEXTURE_MIN_FILTER, minFilter);
        gl.glTexParameterf(texTarget, GL10.GL_TEXTURE_MAG_FILTER, magFilter);
        gl.glTexParameterf(texTarget, GL10.GL_TEXTURE_WRAP_S, GL10.GL_CLAMP_TO_EDGE);
        gl.glTexParameterf(texTarget, GL10.GL_TEXTURE_WRAP_T, GL10.GL_CLAMP_TO_EDGE);
        
        
        
        //if (texTarget == GL.GL_TEXTURE_1D) gl.glTexImage1D(texTarget, 0, texInternalFormat, w, 0, GL.GL_RGBA, GL.GL_UNSIGNED_BYTE, null);
        //else gl.glTexImage2D(texTarget, 0, texInternalFormat, w, h, 0, GL.GL_RGBA, GL.GL_UNSIGNED_BYTE, null);
        
        gl.glTexImage2D(texTarget, 0, texInternalFormat, w, h, 0, GL10.GL_RGBA, GL10.GL_UNSIGNED_BYTE, null);
        gl.glBindTexture(texTarget, 0);
		
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
     * Initializes the texture with a pre-existing OpenGL texture ID.
     * @param w int
     * @param h int
     * @param id int
     */    
    protected void initTexture(int w, int h, int id)
    {
        if (tex[0] != 0)
        {
            releaseTexture();
        }
        
      
        
        tex[0] = id;
        gl.glBindTexture(texTarget, tex[0]);
        gl.glTexParameterf(texTarget, GL10.GL_TEXTURE_MIN_FILTER, minFilter);
        gl.glTexParameterf(texTarget, GL10.GL_TEXTURE_MAG_FILTER, magFilter);
        gl.glTexParameterf(texTarget, GL10.GL_TEXTURE_WRAP_S, GL10.GL_CLAMP_TO_EDGE);
        gl.glTexParameterf(texTarget, GL10.GL_TEXTURE_WRAP_T, GL10.GL_CLAMP_TO_EDGE);
        
        //if (texTarget == GL.GL_TEXTURE_1D) gl.glTexImage1D(texTarget, 0, texInternalFormat, w, 0, GL.GL_RGBA, GL.GL_UNSIGNED_BYTE, null);
        //else gl.glTexImage2D(texTarget, 0, texInternalFormat, w, h, 0, GL.GL_RGBA, GL.GL_UNSIGNED_BYTE, null);
        
        gl.glTexImage2D(texTarget, 0, texInternalFormat, w, h, 0, GL10.GL_RGBA, GL10.GL_UNSIGNED_BYTE, null);
        gl.glBindTexture(texTarget, 0);
		
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
        gl.glDeleteTextures(1, tex, 0);  
        tex[0] = 0;
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
            texTarget = GL10.GL_TEXTURE_2D;
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
	    
	    if (params.format == COLOR)
	    {
            texInternalFormat = GL10.GL_RGBA;
        }
	    /*
	     No Float formats
        else if (params.format == FLOAT)
        {
            texInternalFormat = GL10.GL_RGBA16F;            
	    }
	    else if (params.format == DOUBLE)
        {
            texInternalFormat = GL10.GL_RGBA32F_ARB;
	    }		
	    */
	    
	    else if (params.format == 3)
	    {
	    	PApplet.println("Setting alpha tex");
            texInternalFormat = GL10.GL_ALPHA;
        }	    
	    

	    if (params.minFilter == NEAREST)
	    {
            minFilter = GL10.GL_NEAREST;
        }
	    else if (params.minFilter == LINEAR)
        {
            minFilter = GL10.GL_LINEAR;
	    }
	    else if (params.minFilter == NEAREST_MIPMAP_NEAREST)
        {
            minFilter = GL10.GL_NEAREST_MIPMAP_NEAREST;
	    }
	    else if (params.minFilter == LINEAR_MIPMAP_NEAREST)
        {
            minFilter = GL10.GL_LINEAR_MIPMAP_NEAREST;
	    }		
	    else if (params.minFilter == NEAREST_MIPMAP_LINEAR)
        {
            minFilter = GL10.GL_NEAREST_MIPMAP_LINEAR;
	    }
	    else if (params.minFilter == LINEAR_MIPMAP_LINEAR)
        {
            minFilter = GL10.GL_LINEAR_MIPMAP_LINEAR;
	    }		

	    if (params.magFilter == NEAREST)
	    {
            magFilter = GL10.GL_NEAREST;
        }
	        else if (params.magFilter == LINEAR)
        {
            magFilter = GL10.GL_LINEAR;
	    }

        usingMipmaps = (minFilter == GL10.GL_NEAREST_MIPMAP_NEAREST) ||
                       (minFilter == GL10.GL_LINEAR_MIPMAP_NEAREST) ||
                       (minFilter == GL10.GL_NEAREST_MIPMAP_LINEAR) ||
                       (minFilter == GL10.GL_LINEAR_MIPMAP_LINEAR);
					   
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
     
    /**
     * @invisible
     */	
    protected GL10 gl;
	
    /**
     * @invisible
     */			
    protected PGraphicsAndroid3D pgl;
	
    /**
     * @invisible
     */	
    protected int[] tex = { 0 }; 
	
    /**
     * @invisible
     */			
    protected int texTarget;	
	
    /**
     * @invisible
     */			
    protected int texInternalFormat;

    /**
     * @invisible
     */			
    protected int minFilter;	

    /**
     * @invisible
     */			
    protected int magFilter;
	
    /**
     * @invisible
     */		
    protected boolean usingMipmaps;	
	
    /**
     * @invisible
     */		
    protected float maxTexCoordS;
	
    /**
     * @invisible
     */		
    protected float maxTexCoordT;
	
    /**
     * @invisible
     */		
    protected boolean flippedX;	
	
    /**
     * @invisible
     */		
    protected boolean flippedY;
    
    
    /////////////////////////////////////////////////////////////////////////// 

    
    /**
     * This class stores the parameters for a texture: target, internal format, minimization filter
     * and magnification filter. 
     */
    public class Parameters //implements PConstants 
    {
        /**
         * Creates an instance of GLTextureParameters, setting all the parameters to default values.
         */
        public Parameters()
        {
            target = GLTexture.TEX_NORM;
            format = GLTexture.COLOR;
            minFilter = GLTexture.LINEAR;
            magFilter = GLTexture.LINEAR;   
        }
      
        public Parameters(int format)
        {
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
