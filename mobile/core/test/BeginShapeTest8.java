package test;
import processing.core.*;

/**
 *
 * @author  Francis Li
 */
public class BeginShapeTest8 extends PMIDlet {

    void setup() {
        beginShape(QUAD_STRIP); 
        vertex(30, 20); 
        vertex(30, 75); 
        vertex(50, 75); 
        vertex(50, 20); 
        vertex(65, 20); 
        vertex(65, 75); 
        vertex(85, 75); 
        vertex(85, 20); 
        endShape(); 
    }
}
