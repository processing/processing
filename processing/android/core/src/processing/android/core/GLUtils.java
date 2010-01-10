package processing.android.core; 
 
import javax.microedition.khronos.opengles.*;

/**
 * @invisible
 * This class provides some utilities functions.
 */
public class GLUtils
{
    static public int parsePrimitiveType(String typeStr)
    {
        if (typeStr.equals("points")) return GL11.GL_POINTS;
        else if (typeStr.equals("line_strip")) return GL11.GL_LINE_STRIP;
        else if (typeStr.equals("line_loop")) return GL11.GL_LINE_LOOP;
        else if (typeStr.equals("lines")) return GL11.GL_LINES;
        else if (typeStr.equals("triangle_strip")) return GL11.GL_TRIANGLE_STRIP;
        else if (typeStr.equals("triangle_fan")) return GL11.GL_TRIANGLE_FAN;
        else if (typeStr.equals("triangles")) return GL11.GL_TRIANGLES;
        //else if (typeStr.equals("quad_strip")) return GL11.GL_QUAD_STRIP;
        //else if (typeStr.equals("quads")) return GL11.GL_QUADS;
        //else if (typeStr.equals("polygon")) return GL11.GL_POLYGON;
        else
        {
            System.err.println("Unrecognized geometry mode. Using points.");
            return GL11.GL_POINTS;
        }
    }
    
    static public int parsePrimitiveTypeUpperCase(String typeStr)
    {
        if (typeStr.equals("POINTS")) return GL11.GL_POINTS;
        else if (typeStr.equals("POINT_SPRITES")) return GL11.GL_POINTS;
        else if (typeStr.equals("LINE_STRIP")) return GL11.GL_LINE_STRIP;
        else if (typeStr.equals("LINE_LOOP")) return GL11.GL_LINE_LOOP;
        else if (typeStr.equals("LINES")) return GL11.GL_LINES;
        else if (typeStr.equals("TRIANGLE_STRIP")) return GL11.GL_TRIANGLE_STRIP;
        else if (typeStr.equals("TRIANGLE_FAN")) return GL11.GL_TRIANGLE_FAN;
        else if (typeStr.equals("TRIANGLES")) return GL11.GL_TRIANGLES;
        //else if (typeStr.equals("QUAD_STRIP")) return GL.GL_QUAD_STRIP;
        //else if (typeStr.equals("QUADS")) return GL.GL_QUADS;
        //else if (typeStr.equals("POLYGON")) return GL.GL_POLYGON;
        else
        {
            System.err.println("Unrecognized geometry mode. Using points.");
            return GL11.GL_POINTS;
        }        
    }
    
    static public int parseVBOMode(String modeStr)
    {
        if (modeStr.equals("STATIC")) return GL11.GL_STATIC_DRAW;
        else if (modeStr.equals("DYNAMIC")) return GL11.GL_DYNAMIC_DRAW;
        //else if (modeStr.equals("STREAM")) return GL.GL_STREAM_COPY;
        else
        {
            System.err.println("Unrecognized VBO mode mode. Using static.");
            return GL11.GL_STATIC_DRAW;
        }         	
    }
    
    static void printFramebufferError(int status)
    {
        /*    	
        if (status == GL11.GL_FRAMEBUFFER_COMPLETE_EXT) return;
        else if (status == GL11.GL_FRAMEBUFFER_INCOMPLETE_ATTACHMENT_EXT)
        {
        	System.err.println("Frame buffer is incomplete (GL_FRAMEBUFFER_INCOMPLETE_ATTACHMENT_EXT)");
        }
        else if (status == GL.GL_FRAMEBUFFER_INCOMPLETE_MISSING_ATTACHMENT_EXT)
        {
        	System.err.println("Frame buffer is incomplete (GL_FRAMEBUFFER_INCOMPLETE_MISSING_ATTACHMENT_EXT)");
        }
        else if (status == GL.GL_FRAMEBUFFER_INCOMPLETE_DIMENSIONS_EXT)
        {
        	System.err.println("Frame buffer is incomplete (GL_FRAMEBUFFER_INCOMPLETE_DIMENSIONS_EXT)");
        }
        else if (status == GL.GL_FRAMEBUFFER_INCOMPLETE_FORMATS_EXT)
        {
        	System.err.println("Frame buffer is incomplete (GL_FRAMEBUFFER_INCOMPLETE_FORMATS_EXT)");
        }
        else if (status == GL.GL_FRAMEBUFFER_INCOMPLETE_DRAW_BUFFER_EXT)
        {
        	System.err.println("Frame buffer is incomplete (GL_FRAMEBUFFER_INCOMPLETE_DRAW_BUFFER_EXT)");
        }
        else if (status == GL.GL_FRAMEBUFFER_INCOMPLETE_READ_BUFFER_EXT)
        {
        	System.err.println("Frame buffer is incomplete (GL_FRAMEBUFFER_INCOMPLETE_READ_BUFFER_EXT)");
        }
        else if (status == GL.GL_FRAMEBUFFER_UNSUPPORTED_EXT)
        {
        	System.err.println("Frame buffer is incomplete (GL_FRAMEBUFFER_UNSUPPORTED_EXT)");
        }
        else
        {
        	System.err.println("Frame buffer is incomplete (unknown error code)");
        }    
        */
    }
}
