package test;
import processing.core.*;

/**
 *
 * @author  Francis Li
 */
public class BeginShapeTest3 extends PMIDlet {
    
    void setup() {
        beginShape(LINE_STRIP); 
        vertex(30, 20); 
        vertex(85, 20); 
        vertex(85, 75); 
        vertex(30, 75); 
        endShape(); 
    }
}
