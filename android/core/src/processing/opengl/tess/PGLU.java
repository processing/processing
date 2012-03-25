package processing.opengl.tess;

import android.opengl.GLES20;

public class PGLU {
  public static final int GLU_FALSE = 0;  
  public static final int GLU_TRUE  = 1;
  
  public static final int GLU_INVALID_ENUM      = 100900;  
  public static final int GLU_INVALID_VALUE     = 100901;  
  public static final int GLU_OUT_OF_MEMORY     = 100902;  
  public static final int GLU_INVALID_OPERATION = 100904;
  
  public static final int GLU_POINT      = 100010;  
  public static final int GLU_LINE       = 100011;  
  public static final int GLU_FILL       = 100012;  
  public static final int GLU_SILHOUETTE = 100013;
  
  public static final int GLU_SMOOTH = 100000;  
  public static final int GLU_FLAT   = 100001;
  public static final int GLU_NONE   = 100002;
  
  public static final int GLU_OUTSIDE = 100020;  
  public static final int GLU_INSIDE  = 100021;
  
  public static final int GLU_ERROR      = 100103;
  public static final int GLU_TESS_ERROR = 100103;
  
  public static final int GLU_TESS_BEGIN  = 100100;  
  public static final int GLU_BEGIN       = 100100;  
  public static final int GLU_TESS_VERTEX = 100101;  
  public static final int GLU_VERTEX      = 100101;  
  public static final int GLU_TESS_END    = 100102;  
  public static final int GLU_END         = 100102;
  
  public static final int GLU_TESS_EDGE_FLAG = 100104;  
  public static final int GLU_EDGE_FLAG = 100104;    
  public static final int GLU_TESS_COMBINE = 100105;  
  public static final int GLU_TESS_BEGIN_DATA = 100106;  
  public static final int GLU_TESS_VERTEX_DATA = 100107;  
  public static final int GLU_TESS_END_DATA = 100108;  
  public static final int GLU_TESS_ERROR_DATA = 100109;  
  public static final int GLU_TESS_EDGE_FLAG_DATA = 100110;  
  public static final int GLU_TESS_COMBINE_DATA = 100111;
  
  public static final int GLU_CW                              = 100120;  
  public static final int GLU_CCW                             = 100121;  
  public static final int GLU_INTERIOR                        = 100122;  
  public static final int GLU_EXTERIOR                        = 100123;  
  public static final int GLU_UNKNOWN                         = 100124;  
  public static final int GLU_TESS_WINDING_RULE               = 100140;
  public static final int GLU_TESS_BOUNDARY_ONLY              = 100141;  
  public static final int GLU_TESS_TOLERANCE                  = 100142;  
  public static final int GLU_TESS_AVOID_DEGENERATE_TRIANGLES = 100149;
  
  public static final int GLU_TESS_ERROR1 = 100151;  
  public static final int GLU_TESS_ERROR2 = 100152;  
  public static final int GLU_TESS_ERROR3 = 100153;  
  public static final int GLU_TESS_ERROR4 = 100154;  
  public static final int GLU_TESS_ERROR5 = 100155;  
  public static final int GLU_TESS_ERROR6 = 100156;  
  public static final int GLU_TESS_ERROR7 = 100157;  
  public static final int GLU_TESS_ERROR8 = 100158;
  
  public static final int GLU_TESS_MISSING_BEGIN_POLYGON = 100151;  
  public static final int GLU_TESS_MISSING_BEGIN_CONTOUR = 100152;  
  public static final int GLU_TESS_MISSING_END_POLYGON   = 100153;  
  public static final int GLU_TESS_MISSING_END_CONTOUR   = 100154;  
  public static final int GLU_TESS_COORD_TOO_LARGE       = 100155;  
  public static final int GLU_TESS_NEED_COMBINE_CALLBACK = 100156;
  
  public static final int GLU_TESS_WINDING_ODD         = 100130;  
  public static final int GLU_TESS_WINDING_NONZERO     = 100131;  
  public static final int GLU_TESS_WINDING_POSITIVE    = 100132;  
  public static final int GLU_TESS_WINDING_NEGATIVE    = 100133;  
  public static final int GLU_TESS_WINDING_ABS_GEQ_TWO = 100134;
  
  public static final double GLU_TESS_MAX_COORD = 1.0E150;

  private static String[] glErrorStrings = {
    "invalid enumerant",
    "invalid value",
    "invalid operation",
    "stack overflow",
    "stack underflow",
    "out of memory",
    "invalid framebuffer operation"
  };
  
  private static String[] gluErrorStrings = {
    "invalid enumerant",
    "invalid value",
    "out of memory",
    "",
    "invalid operation"
  };  
  
  private static String[] gluTessErrors = {
    " ",
    "gluTessBeginPolygon() must precede a gluTessEndPolygon",
    "gluTessBeginContour() must precede a gluTessEndContour()",
    "gluTessEndPolygon() must follow a gluTessBeginPolygon()",
    "gluTessEndContour() must follow a gluTessBeginContour()",
    "a coordinate is too large",
    "need combine callback"
  };  
  
  public static final PGLUtessellator gluNewTess() {
    return GLUtessellatorImpl.gluNewTess();
  }  
  
  public static final void gluTessCallback(PGLUtessellator tess, int which, PGLUtessellatorCallback callback) {
    ((GLUtessellatorImpl)tess).gluTessCallback(which, callback);
  }

  public static final void gluTessBeginPolygon(PGLUtessellator tess, Object data) {
    ((GLUtessellatorImpl)tess).gluTessBeginPolygon(data); 
  }
  
  public static final void gluTessEndPolygon(PGLUtessellator tess) {
    ((GLUtessellatorImpl)tess).gluTessEndPolygon();    
  }
                                                                                                                                                
  public static final void gluTessProperty(PGLUtessellator tess, int which, double value) {
    ((GLUtessellatorImpl)tess).gluTessProperty(which, value);
  }

  public static final void gluTessBeginContour(PGLUtessellator tess) {
    ((GLUtessellatorImpl)tess).gluTessBeginContour();
  }

  public static final void gluTessEndContour(PGLUtessellator tess) {
    ((GLUtessellatorImpl)tess).gluTessEndContour();
  }

  public static final void gluTessVertex(PGLUtessellator tess, double[] coords, int offset, Object vdata) {
    ((GLUtessellatorImpl)tess).gluTessVertex(coords, offset, vdata);
  }

  public static String gluErrorString( int errorCode ) {
    if( errorCode == 0 ) {
      return ("no error");
    }
    if( (errorCode >= GLES20.GL_INVALID_ENUM) && (errorCode <= GLES20.GL_INVALID_FRAMEBUFFER_OPERATION) ) {
      return (glErrorStrings[errorCode - GLES20.GL_INVALID_ENUM]);
    }
    if( errorCode == 0x8031 /* GL.GL_TABLE_TOO_LARGE */ ) {
      return ("table too large");
    }
    if( (errorCode >= GLU_INVALID_ENUM) && (errorCode <= GLU_INVALID_OPERATION) ) {
      return (gluErrorStrings[errorCode - GLU_INVALID_ENUM]);
    }
    if( (errorCode >= GLU_TESS_ERROR1) && (errorCode <= GLU_TESS_ERROR8) ) {
        return (gluTessErrors[errorCode - (GLU_TESS_ERROR1 - 1)]);
    }
    return ("error ("+errorCode+")");
  }  
}
