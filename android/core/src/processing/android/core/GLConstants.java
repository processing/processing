package processing.android.core;

public interface GLConstants {
/**
   * This constant identifies the texture target GL_TEXTURE_2D, 
   * that is, textures with normalized coordinates.
   */
  public static final int NORMAL_TEXTURE = 0;
  
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
   * Static usage mode for GLModels (vertices won't be updated after creation).
   */
  public static final int STATIC = 0;
  /**
   * Dynamic usage mode for GLModels (vertices will be updated after creation).
   */  
  public static final int DYNAMIC = 1;    
    
  
  public static final int VERTICES = 0;
  public static final int NORMALS = 1;
  public static final int COLORS = 2;
  public static final int TEXTURES = 3;
  public static final int GROUPS = 4;
}
