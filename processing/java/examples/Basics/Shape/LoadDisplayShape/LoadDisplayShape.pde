/**
 * Load and Display a Shape. 
 * Illustration by George Brower. 
 * 
 * The loadShape() command is used to read simple SVG (Scalable Vector Graphics)
 * files into a Processing sketch. This library was specifically tested under
 * SVG files created from Adobe Illustrator. For now, we can't guarantee that 
 * it'll work for SVGs created with anything else. 
 */

// The next line is needed if running in JavaScript Mode with Processing.js
/* @pjs preload="bot1.svg"; */ 

PShape bot;

void setup() {
  size(640, 360);
  // The file "bot1.svg" must be in the data folder
  // of the current sketch to load successfully
  bot = loadShape("bot1.svg");
} 

void draw(){
  background(102);
  shape(bot, 110, 90, 100, 100);  // Draw at coordinate (10, 10) at size 100 x 100
  shape(bot, 280, 40);            // Draw at coordinate (70, 60) at the default size
}
