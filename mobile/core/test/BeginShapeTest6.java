package test;
import processing.core.*;

/**
 *
 * @author  Francis Li
 */
public class BeginShapeTest6 extends PMIDlet {

    void setup() {
        beginShape(TRIANGLE_STRIP); 
        vertex(30, 75); 
        vertex(40, 20); 
        vertex(50, 75); 
        vertex(60, 20); 
        vertex(70, 75); 
        vertex(80, 20); 
        vertex(90, 75); 
        endShape(); 
    }
    
}
