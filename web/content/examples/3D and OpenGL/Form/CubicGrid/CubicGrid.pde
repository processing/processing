 /**
 * Cubic Grid
 * By Ira Greenberg

 * 3D translucent colored grid
 * uses nested pushMatrix()
 * and popMatrix() functions
 */

float boxSize = 40;
float margin = boxSize*2;
float depth = 400;
color boxFill;

void setup(){
  size(200, 200, P3D);
  noStroke();
}

void draw(){
  background(255);
  // center and spin grid
  translate(width/2, height/2, -depth/2);
  rotateY(frameCount*PI/300);
  rotateX(frameCount*PI/300);

  // build grid using multiple translations 
  for (float i=-depth/2+margin; i<=depth/2-margin; i+=boxSize){
    pushMatrix();
    for (float j=-height+margin; j<=height-margin; j+=boxSize){
      pushMatrix();
      for (float k=-width+margin; k<=width-margin; k+=boxSize){
        // base fill color on counter values, abs function 
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
}

