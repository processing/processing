package test;
import processing.core.*;

/**
 *
 * @author  Francis Li
 */
public class BezierVertexTest extends PMIDlet {
    
    void setup() {
        beginShape(LINE_STRIP); 
        bezierVertex(30, 20); 
        bezierVertex(80, 0); 
        bezierVertex(80, 75); 
        bezierVertex(30, 75); 
        endShape(); 
    }
}
