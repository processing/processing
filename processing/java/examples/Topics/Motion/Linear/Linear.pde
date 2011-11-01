/**
 * Linear Motion. 
 * 
 * Changing a variable to create a moving line.  
 * When the line moves off the edge of the window, 
 * the variable is set to 0, which places the line
 * back at the bottom of the screen. 
 */
 
float a;

void setup() {
  size(640, 360);
  stroke(255);
  a = height/2;
}

void draw() {
  background(51);
  line(0, a, width, a);  
  a = a - 0.5;
  if (a < 0) { 
    a = height; 
  }
}
