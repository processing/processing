/**
 * SaveFile 2
 * 
 * This file a PrintWriter object to write data continuously to a file
 * while the mouse is pressed. When a key is pressed, the file closes
 * itself and the program is stopped. This example won't work in a web browser
 * because of Java security restrictions.
 */

PrintWriter output;

void setup() 
{
  size(200, 200);
  // Create a new file in the sketch directory
  output = createWriter("positions.txt");
  frameRate(12);
}

void draw() 
{
  if (mousePressed) {
    point(mouseX, mouseY);
    // Write the coordinate to a file with a
    // "\t" (TAB character) between each entry
    output.println(mouseX + "\t" + mouseY);
  }
}

void keyPressed() { // Press a key to save the data
  output.flush(); // Write the remaining data
  output.close(); // Finish the file
  exit(); // Stop the program
}
