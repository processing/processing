package test;
import processing.core.*;

/**
 *
 * @author  Francis Li
 */
public class BeginShapeTest9 extends PMIDlet {
    
    void setup() {
        beginShape(POLYGON); 
        vertex(20, 20); 
        vertex(40, 20); 
        vertex(40, 40); 
        vertex(60, 40); 
        vertex(60, 60); 
        vertex(20, 60); 
        endShape(); 
    }
}
