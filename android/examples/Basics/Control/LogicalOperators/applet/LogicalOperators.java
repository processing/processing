import processing.core.*; 

import java.applet.*; 
import java.awt.*; 
import java.awt.image.*; 
import java.awt.event.*; 
import java.io.*; 
import java.net.*; 
import java.text.*; 
import java.util.*; 
import java.util.zip.*; 
import java.util.regex.*; 

public class LogicalOperators extends PApplet {
  public void setup() {/**
 * Logical Operators. 
 * 
 * The logical operators for AND (&&) and OR (||) are used to 
 * combine simple relational statements into more complex expressions.
 * The NOT (!) operator is used to negate a boolean statement. 
 */
 
size(200, 200);
background(126);

boolean op = false;

for(int i=5; i<=195; i+=5) {
  // Logical AND
  stroke(0);
  if((i > 35) && (i < 100)) {
    line(5, i, 95, i);
    op = false;
  }
  
  // Logical OR
  stroke(76);
  if((i <= 35) || (i >= 100)) {
    line(105, i, 195, i);
    op = true;
  }
  
  // Testing if a boolean value is "true"
  // The expression "if(op)" is equivalent to "if(op == true)"
  if(op) {
    stroke(0);
    point(width/2, i);
  }
    
  // Testing if a boolean value is "false"
  // The expression "if(!op)" is equivalent to "if(op == false)"
  if(!op) {
    stroke(255);
    point(width/4, i);
  }
}




  noLoop();
} 
  static public void main(String args[]) {
    PApplet.main(new String[] { "LogicalOperators" });
  }
}
