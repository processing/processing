/**
 * Cubic Grid 
 * by Ira Greenberg. 
 * 
 * 3D translucent colored grid uses nested pushMatrix()
 * and popMatrix() functions. 
 */

float boxSize = 40;
float margin = boxSize*2;
float depth = 400;
color boxFill;

void setup() {
  size(640, 360, P3D);
  frameRate(120);
  noStroke();
}

void draw() {
  background(255);
  
  hint(DISABLE_DEPTH_TEST);
  
  // Center and spin grid
  translate(width/2, height/2, -depth);
  rotateY(frameCount * 0.01);
  rotateX(frameCount * 0.01);

  // Build grid using multiple translations 
  for (float i =- depth/2+margin; i <= depth/2-margin; i += boxSize){
    pushMatrix();
    for (float j =- height+margin; j <= height-margin; j += boxSize){
      pushMatrix();
      for (float k =- width+margin; k <= width-margin; k += boxSize){
        // Base fill color on counter values, abs function 
        // ensures values stay within legal range
        boxFill = color(abs(i), abs(j), abs(k), 50);
        pushMatrix();
        translate(k, j, i);
        fill(boxFill);
        box(boxSize, boxSize, boxSize);
        popMatrix();
      }
      popMatrix();
    }
    popMatrix();
  }
  if (frameCount % 10 == 0) {
    println(frameRate);
  }
}

