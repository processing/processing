/**
 * Letters. 
 * 
 * Draws letters to the screen.
 */

PFont fontA;

void setup() 
{
  size(200, 200);
  background(0);
  // Load the font. Fonts must be placed within the data 
  // directory of your sketch. A font must first be created
  // using the 'Create Font...' option in the Tools menu.
  fontA = loadFont("CourierNew36.vlw");
  textFont(fontA, 36);
  textAlign(CENTER);
  noLoop();
} 

void draw() 
{
  // Set the gray value of the letters
  fill(255);

  // Set the left and top margin
  int margin = 6;
  int gap = 30;
  translate(margin*1.5, margin*2);

  // Create a matrix of letterforms
  int counter = 0;
  for(int i=0; i<margin; i++) {
    for(int j=0; j<margin; j++) {
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
      text(letter, 15+j*gap, 20+i*gap);

      // Increment the counter
      counter++;
      if(counter >= 26) {
        counter = 0;
      }
    }
  }
}

