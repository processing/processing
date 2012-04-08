/** 
 * Synthesis 1: Form and Code, 
 * Collage Engine by Casey Reas (www.processing.org)
 * p. 150
 * 
 * Step 1, working with one image. 
*/

// Load the image
PImage nyt01 = loadImage("nyt_01.jpg");

float x, y;  // Image position
float r;     // Image rotation

size(400, 300);
background(255);
tint(255, 204);

x = random(width);      // Set random x-coordinate
y = random(height);     // Set random y-coordinate
r = random(0, TWO_PI);  // Set random rotation
pushMatrix();
translate(x, y);
rotate(r);
image(nyt01, -nyt01.width/2, -nyt01.height/2);
popMatrix();
