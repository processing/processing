/**
 * Letters. 
 * 
 * Draws letters to the screen. This requires loading a font, 
 * setting the font, and then drawing the letters.
 */

PFont fontA;

void setup() {
  size(640, 360);
  background(0);
  smooth();

  // Create the font
  fontA = createFont("Mono", 18);

  // Set the font
  textFont(fontA);
  textAlign(CENTER, CENTER);
} 

void draw() {
  background(0);

  // Set the left and top margin
  int margin = 10;
  translate(margin*4, margin*4);

  int gap = 46;
  int counter = 35;
  
  for (int y = 0; y < height-gap; y += gap) {
    for (int x = 0; x < width-gap; x += gap) {

      char letter = char(counter);
      
      if (letter == 'A' || letter == 'E' || letter == 'I' || letter == 'O' || letter == 'U') {
        fill(255);
      } 
      else {
        fill(102);
      }

      // Draw the letter to the screen
      text(letter, x, y);

      // Increment the counter
      counter++;
    }
  }
}

