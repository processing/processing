/**
 * Redraw. 
 * 
 * The redraw() function makes draw() execute once.  
 * In this example, draw() is executed once every time 
 * the mouse is clicked. 
 */
 
float y;
 
// The statements in the setup() function 
// execute once when the program begins
void setup() {
  size(640, 360);  // Size should be the first statement
  stroke(255);     // Set line drawing color to white
  noLoop();
  y = height * 0.5;
}

// The statements in draw() are executed until the 
// program is stopped. Each statement is executed in 
// sequence and after the last line is read, the first 
// line is executed again.
void draw() { 
  background(0);   // Set the background to black
  y = y - 4; 
  if (y < 0) { y = height; } 
  line(0, y, width, y);  
} 

void mousePressed() {
  redraw();
}


