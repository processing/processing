package test;
import processing.core.*;

/**
 *
 * @author  Francis Li
 */
public class BeginShapeTest7 extends PMIDlet {
    
    void setup() {
        beginShape(QUADS); 
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
