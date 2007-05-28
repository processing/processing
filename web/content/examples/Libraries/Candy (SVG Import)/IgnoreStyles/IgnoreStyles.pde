/**
 * Ignore Styles
 * Illustration by George Brower
 * 
 * Move the mouse left and right to zoom the SVG file.
 * This shows how, unlike an imported image, the lines
 * remain smooth at any size.
 */

import processing.candy.*;
import processing.xml.*;

SVG bot;

void setup(){
  size(640, 480);
  smooth();
  // The file "bot1.svg" must be in the data folder
  // of the current sketch to load successfully
  bot = new SVG(this, "bot1.svg");
} 

void draw(){
  background(102);
  
  // Draw left bot
  bot.draw(50, 140, 160, 160);
  
  // Draw middle bot
  bot.ignoreStyles();  // Ignore the colors in the SVG
  fill(0, 102, 153);   // Set the SVG fill to blue
  stroke(255);         // Set the SVG fill to white
  bot.draw(240, 140, 160, 160);
  
  // Draw right bot
  bot.ignoreStyles(false);  // Start using the SVG's original colors again
  bot.draw(430, 140, 160, 160);
}
