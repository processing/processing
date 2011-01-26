/**
 * Get Child. 
 * 
 * SVG files can be made of many individual shapes. 
 * Each of these shapes (called a "child") has its own name 
 * that can be used to extract it from the "parent" file.
 * This example loads a map of the United States and creates
 * two new PShape objects by extracting the data from two states.
 */

PShape usa;
PShape michigan;
PShape ohio;

void setup() {
  size(640, 360);  
  usa = loadShape("usa-wikipedia.svg");
  michigan = usa.getChild("MI");
  ohio = usa.getChild("OH");
  smooth();  // Improves the drawing quality of the SVG
  noLoop();
}

void draw() {
  background(255);
  
  // Draw the full map
  shape(usa, -600, -180);
  
  // Disable the colors found in the SVG file
  michigan.disableStyle();
  // Set our own coloring
  fill(0, 51, 102);
  noStroke();
  // Draw a single state
  shape(michigan, -600, -180); // Go Blue!
  
  // Disable the colors found in the SVG file
  ohio.disableStyle();
  // Set our own coloring
  fill(153, 0, 0);
  noStroke();
  // Draw a single state
  shape(ohio, -600, -180);  // Boo Buckeyes!
}
