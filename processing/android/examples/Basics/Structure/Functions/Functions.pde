/**
 * Functions. 
 * 
 * The drawTarget() function makes it easy to draw many distinct targets. 
 * Each call to drawTarget() specifies the position, size, and number of 
 * rings for each target. 
 */

void setup() 
{
  size(200, 200);
  background(51);
  noStroke();
  smooth();
  noLoop();
}

void draw() 
{
  drawTarget(68, 34, 200, 10);
  drawTarget(152, 16, 100, 3);
  drawTarget(100, 144, 80, 5);
}

void drawTarget(int xloc, int yloc, int size, int num) 
{
  float grayvalues = 255/num;
  float steps = size/num;
  for(int i=0; i<num; i++) {
    fill(i*grayvalues);
    ellipse(xloc, yloc, size-i*steps, size-i*steps);
  }
}
