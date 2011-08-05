/**
 * Double Random 
 * by Ira Greenberg.  
 * 
 * Using two random() calls and the point() function 
 * to create an irregular sawtooth line.
 */

int totalPts = 300;
float steps = totalPts + 1;
  
void setup() {
  size(640, 360);
  stroke(255);
  frameRate(1);
} 

void draw() {
  background(0);
  float rand = 0;
  for  (int i = 1; i < steps; i++) {
    point( (width/steps) * i, (height/2) + random(-rand, rand) );
    rand += random(-5, 5);
  }
}

