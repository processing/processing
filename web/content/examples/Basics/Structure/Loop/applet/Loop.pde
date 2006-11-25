// Loop
// by REAS <http://reas.com>

// The loop() function causes draw() to execute
// continuously. If noLoop is called in setup()
// the draw() is only executed once. In this example
// click the mouse to execute loop(), which will
// cause the draw() the execute continuously

// Created 09 December 2002

// The statements in the setup() function 
// execute once when the program begins
void setup() 
{
  size(200, 200);  // Size should be the first statement
  stroke(255);     // Set line drawing color to white
  noLoop();
}

float y = 100;

// The statements in draw() are executed until the 
// program is stopped. Each statement is executed in 
// sequence and after the last line is read, the first 
// line is executed again.
void draw() 
{ 
  background(0);   // Set the background to black
  y = y - 1; 
  if (y < 0) { y = height; } 
  line(0, y, width, y);  
} 

void mousePressed() 
{
  loop();
}
