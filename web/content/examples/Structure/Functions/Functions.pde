// Functions
// by REAS <http://reas.com>

// The draw_target() function makes it easy to draw many distinct targets.
// Each call to draw_target() specifies the position, size, and number of 
// rings for each target.

// Updated 21 August 2002

void setup() 
{
  size(200, 200);
  background(51);
  noStroke();
  ellipseMode(CENTER_RADIUS);
  noLoop();
}

void draw() 
{
  draw_target(68, 34, 100, 10);
  draw_target(152, 16, 50, 3);
  draw_target(100, 144, 40, 5);
}

void draw_target(int xloc, int yloc, int size, int num) 
{
  float grayvalues = 255/num;
  float steps = size/num;
  for(int i=0; i<num; i++) {
    fill(i*grayvalues);
    ellipse(xloc, yloc, size-i*steps, size-i*steps);
  }
}
