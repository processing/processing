/**
 * Logical Operators. 
 * 
 * The logical operators for AND (&&) and OR (||) are used to 
 * combine simple relational statements into more complex expressions.
 * The NOT (!) operator is used to negate a boolean statement. 
 */
 
size(640, 360);
background(126);

boolean test = false;

for (int i = 5; i <= height; i += 5) {
  // Logical AND
  stroke(0);
  if((i > 35) && (i < 100)) {
    line(width/4, i, width/2, i);
    test = false;
  }
  
  // Logical OR
  stroke(76);
  if ((i <= 35) || (i >= 100)) {
    line(width/2, i, width, i);
    test = true;
  }
  
  // Testing if a boolean value is "true"
  // The expression "if(test)" is equivalent to "if(test == true)"
  if (test) {
    stroke(0);
    point(width/3, i);
  }
    
  // Testing if a boolean value is "false"
  // The expression "if(!test)" is equivalent to "if(test == false)"
  if (!test) {
    stroke(255);
    point(width/4, i);
  }
}



