package test;
import processing.core.*;

/**
 *
 * @author  Francis Li
 */
public class Shape_Primitives extends PMIDlet {
    void setup() {
        // Shape Primitives 
        // by REAS <http://www.groupc.net> 

        size(200, 200); 
        background(0); 
        noStroke(); 
        fill(226); 
        triangle(10, 10, 10, 200, 45, 200); 
        rect(45, 45, 35, 35); 
        quad(105, 10, 120, 10, 120, 200, 80, 200); 
        ellipse(120, 60, 40, 40); 
        triangle(160, 10, 195, 200, 160, 200);         
    }
}
