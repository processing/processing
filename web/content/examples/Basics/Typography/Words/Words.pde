/**
 * Words. 
 * 
 * The text() function is used for writing words to the screen. 
 * 
 * Created 15 January 2003
 */


int x = 30;
PFont fontA;
  
void setup() 
{
  size(200, 200);
  background(102);

  // Load the font. Fonts must be placed within the data 
  // directory of your sketch. A font must first be created
  // using the 'Create Font...' option in the Tools menu.
  fontA = loadFont("Ziggurat-HTF-Black-32.vlw");

  // Set the font and its size (in units of pixels)
  textFont(fontA, 32);
}

void draw() {
  // Use fill() to change the value or color of the text
  fill(0);
  text("ichi", x, 60);
  fill(51);
  text("ni", x, 95);
  fill(204);
  text("san", x, 130);
  fill(255);
  text("shi", x, 165);
}
