// Moving along curves
// by REAS <http://reas.com>

// Shapes may be moved along function curves by setting
// their position to be the values input and returned 
// from the equations.

// Updated 15 September 2002

// Variables for first ellipse
int e1_size = 18;
float e1_x, e1_y = 0;
float e1_speed = 1.0;
int e1_direction = 1;

// Variables for second ellipse
int e2_size = 10;
float e2_x, e2_y = 0;
float e2_speed = 0.80;
int e2_direction = 1;

// Variables for third ellipse
int e3_size = 14;
float e3_x, e3_y = 0;
float e3_speed = 0.65;
int e3_direction = 1;


void setup() 
{
  size(200, 200);
  stroke(102);
  ellipseMode(CENTER);
  smooth();
  framerate(30);
}

void draw() 
{
  background(0);
  
  stroke(102);
  
  beginShape(LINE_STRIP);
  for(int i=0; i<width; i++) {
   vertex(i, singraph((float)i/width)*height);
  }
  endShape();
  
  beginShape(LINE_STRIP);
  for(int i=0; i<width; i++) {
   vertex(i, quad((float)i/width)*height);
  }
  endShape();
  
  beginShape(LINE_STRIP);
  for(int i=0; i<width; i++) {
   vertex(i, hump((float)i/width)*height);
  }
  endShape();
  
  e1_x += e1_speed * e1_direction;
  if (e1_x > width || e1_x < 0) {
    e1_direction = e1_direction * -1;
  }
  e1_y = singraph((float)e1_x/width)*height;
  noStroke();
  fill(255);
  ellipse(e1_x, e1_y, e1_size, e1_size);
  
  e2_x += e2_speed * e2_direction;
  if (e2_x > width || e2_x < 0) {
    e2_direction = e2_direction * -1;
  }
  e2_y = quad((float)e2_x/width)*height;
  noStroke();
  fill(255);
  ellipse(e2_x, e2_y, e2_size, e2_size);
  
  e3_x += e3_speed * e3_direction;
  if (e3_x > width || e3_x < 0) {
    e3_direction = e3_direction * -1;
  }
  e3_y = hump((float)e3_x/width)*height;
  noStroke();
  fill(255);
  ellipse(e3_x, e3_y, e3_size, e3_size);
}

float singraph(float sa) {
  sa = (sa - 0.5) * 1.0; //scale from -1 to 1
  sa = sin(sa*PI)/2 + 0.5;
  return sa;
}

float quad(float sa) {
  return sa*sa*sa*sa;
}

float hump(float sa) {
  sa = (sa - 0.5) * 2; //scale from -2 to 2
  sa = sa*sa;
  if(sa > 1) { sa = 1; }
  return 1-sa;
}

