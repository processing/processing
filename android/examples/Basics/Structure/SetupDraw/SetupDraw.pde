/**
 * Setup and Draw. 
 * 
 * The draw() function creates a structure in which
 * to write programs that change with time. 
 */

// The statements in the setup() function 
// execute once when the program begins
void setup() {
  size(200, 200);  // Size must be the first statement
  stroke(255);     // Set line drawing color to white
  frameRate(30);
}

float y = 100;

// The statements in draw() are executed until the 
// program is stopped. Each statement is executed in 
// sequence and after the last line is read, the first 
// line is executed again.
void draw() { 
  background(0);   // Set the background to black
  y = y - 1; 
  if (y < 0) { 
    y = height; 
  } 
  line(0, y, width, y);  
} 

