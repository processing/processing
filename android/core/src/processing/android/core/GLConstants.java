package processing.android.core;

public interface GLConstants {
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
   * This constant identifies the texture internal format GL_RGBA: 4 color components of 8 bits each, identical as ARGB.
   */
  public static final int COLOR = 2;  
  /**
   * This constant identifies the texture internal format GL_RGBA16F_ARB: 4 float compontents of 16 bits each.
   */
  public static final int FLOAT = 1;
  /**
   * This constant identifies the texture internal format GL_RGBA32F_ARB: 4 float compontents of 32 bits each.
   */ 
  public static final int DOUBLE = 2;  

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

  public static final int GL_DEPTH_STENCIL = 0x84F9;
  public static final int GL_UNSIGNED_INT_24_8 = 0x84FA;
  public static final int GL_DEPTH24_STENCIL8 = 0x88F0;
  
  public static final int BACKGROUND_ALPHA = 16384;
}
