package test;
import processing.core.*;

/**
 *
 * @author  Francis Li
 */
public class BeginShapeTest4 extends PMIDlet {
    
    void setup() {
        beginShape(LINE_LOOP); 
        vertex(30, 20); 
        vertex(85, 20); 
        vertex(85, 75); 
        vertex(30, 75); 
        endShape(); 
    }
}
