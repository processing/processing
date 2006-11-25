/**
 * Lights. 
 * 
 * Uses the default lights to show a simple box. 
 */
 
float spin = 0.0;

void setup() 
{
  size(200, 200, P3D);
  noStroke();
}

void draw() 
{
  background(51);
  lights();
  spin += 0.01;
  pushMatrix();
  translate(width/2, height/2, 0);
  rotateX(PI/9);
  rotateY(PI/5 + spin);
  box(90);
  popMatrix();
}
