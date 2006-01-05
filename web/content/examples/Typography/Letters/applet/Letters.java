import processing.core.*; import java.applet.*; import java.awt.*; import java.awt.image.*; import java.awt.event.*; import java.io.*; import java.net.*; import java.text.*; import java.util.*; import java.util.zip.*; public class Letters extends PApplet {public void setup() {// Letters
// by REAS <http://reas.com>

// Drawing letters to the screen in Processing
// uses a technology developed in the mid 1990s
// at the Visual Language Workshop at the MIT
// Media Laboratory. It is a closed system,
// but we have supplied a number of fonts
// located in the "font" directory in the main
// "processing" directory. We expect to change
// the Processing font technology in the future.

// Created 15 January 2003

size(200, 200);
background(0);

// Load the font. Fonts are located within the 
// main Processing directory/folder and they
// must be placed within the data directory
// of your sketch for them to load
PFont fontA = loadFont("CourierNew36.vlw");
textFont(fontA, 36);
textAlign(CENTER);

// Set the gray value of the letters
fill(255);

// Set the left and top margin
int margin = 6;
int gap = 30;
translate(margin*1.5f, margin*2);

// Create a matrix of letterforms
int counter = 0;
for(int i=0; i<margin; i++) {
  for(int j=0; j<margin; j++) {
    char letter;
    
    // Select the letter
    int count = 65+(i*margin)+j;
    if(count <= 90) {
      letter = PApplet.toChar(65+counter);
      if(letter == 'A' || letter == 'E' || letter == 'I' || 
         letter == 'O' || letter == 'U') {
           fill(204, 204, 0);
      } else {
        fill(255);
      }
    } else {
      fill(153);
      letter = PApplet.toChar(48+counter);
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

noLoop(); }}