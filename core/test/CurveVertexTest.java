package test;
import processing.core.*;

/**
 *
 * @author  Francis Li
 */
public class CurveVertexTest extends PMIDlet {
    
    void setup() {
        beginShape(LINE_STRIP); 
        curveVertex(84,  91); 
        curveVertex(84,  91); 
        curveVertex(68,  19); 
        curveVertex(21,  17); 
        curveVertex(32, 100); 
        curveVertex(32, 100); 
        endShape(); 
    }
}
