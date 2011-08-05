/**
 * Letters. 
 * 
 * Draws letters to the screen. This requires loading a font, 
 * setting the font, and then drawing the letters.
 */

PFont fontA;

void setup() 
{
  size(640, 360);
  background(0);
  smooth();
  
  // Create the font
  fontA = createFont("Courier", 42);

  // Set the font and its size (in units of pixels)
  textFont(fontA);
  textAlign(CENTER);

  // Only draw once
  noLoop();
} 

void draw() 
{
  // Set the gray value of the letters
  fill(255);

  // Set the left and top margin
  int margin = 6;
  int gap = 60;
  translate(margin*1.5, margin*2);

  // Create a matrix of letterforms
  int counter = 0;
  for(int i = 0; i < margin; i++) {
    for(int j = 0; j < margin; j++) {
      char letter;

      // Select the letter
      int count = 65+(i*margin)+j;
      if(count <= 90) {
        letter = char(65+counter);
        if(letter == 'A' || letter == 'E' || letter == 'I' || 
          letter == 'O' || letter == 'U') {
          fill(204, 204, 0);
        } 
        else {
          fill(255);
        }
      } 
      else {
        fill(153);
        letter = char(48+counter);
      }

      // Draw the letter to the screen
      text(letter, 15+j*gap*1.6, 20+i*gap);

      // Increment the counter
      counter++;
      if(counter >= 26) {
        counter = 0;
      }
    }
  }
}

