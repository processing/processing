/**
 * Keyboard Functions. 
 * Modified from code by Martin. 
 * Original 'Color Typewriter' concept by John Maeda. 
 * 
 * Click on the window to give it focus and press the letter keys to type colors. 
 * The keyboard function keyPressed() is called whenever
 * a key is pressed. keyReleased() is another keyboard
 * function that is called when a key is released.
 */
 
int max_height = 20;
int min_height = 10;
int letter_height = max_height; // Height of the letters
int letter_width = 10;          // Width of the letter

int x = -letter_width;          // X position of the letters
int y = 0;                      // Y position of the letters

boolean newletter;              

int numChars = 26;      // There are 26 characters in the alphabet
color[] colors = new color[numChars];

void setup()
{
  size(200, 200);
  noStroke();
  colorMode(RGB, numChars);
  background(numChars/2);
  // Set a gray value for each key
  for(int i=0; i<numChars; i++) {
    colors[i] = color(i, i, i);    
  }
}

void draw()
{
  if(newletter == true) {
    // Draw the "letter"
    int y_pos;
    if (letter_height == max_height) {
      y_pos = y;
      rect( x, y_pos, letter_width, letter_height );
    } else {
      y_pos = y + min_height;
      rect( x, y_pos, letter_width, letter_height );
      fill(numChars/2);
      rect( x, y_pos-min_height, letter_width, letter_height );
    }
    newletter = false;
  }
}

void keyPressed()
{
  // if the key is between 'A'(65) and 'z'(122)
  if( key >= 'A' && key <= 'z') {
    int keyIndex;
    if(key <= 'Z') {
      keyIndex = key-'A';
      letter_height = max_height;
      fill(colors[key-'A']);
    } else {
      keyIndex = key-'a';
      letter_height = min_height;
      fill(colors[key-'a']);
    }
  } else {
    fill(0);
    letter_height = 10;
  }

  newletter = true;

  // Update the "letter" position
  x = ( x + letter_width ); 

  // Wrap horizontally
  if (x > width - letter_width) {
    x = 0;
    y+= max_height;
  }

  // Wrap vertically
  if( y > height - letter_height) {
    y = 0;      // reset y to 0
  }
}
