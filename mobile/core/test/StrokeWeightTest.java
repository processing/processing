package test;
import processing.core.*;

/**
 *
 * @author  Francis Li
 */
public class StrokeWeightTest extends PMIDlet {
    void setup() {
        strokeWeight(1);   // Default 
        line(20, 20, 80, 20); 

        strokeWeight(4);   // Thicker 
        line(20, 40, 80, 40); 

        strokeWeight(10);  // Beastly 
        line(20, 70, 80, 70);         
    }
}
