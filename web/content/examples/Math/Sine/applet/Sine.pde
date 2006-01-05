// Sine
// by REAS <http://reas.com>

// Smoothly scaling size with the sin() function.

// Updated 21 August 2002


float spin = 0.0; 
float radius = 42.0; 
float angle;

float angle_rot; 
int rad_points = 90;

void setup() 
{
  size(200, 200);
  noStroke();
  ellipseMode(CENTER_RADIUS);
  smooth();
  framerate(30);
}

void draw() 
{ 
  background(153);
  
  translate(130, 65);
  
  fill(255);
  ellipse(0, 0, 8, 8);
  
  angle_rot = 0;
  fill(51);

  for(int i=0; i<5; i++) {
    pushMatrix();
    rotate(angle_rot + -45);
    ellipse(-116, 0, radius, radius);
    popMatrix();
    angle_rot += PI*2/5;
  }

  radius = 17 * sin(angle) + 84;
  
  angle += 0.03;
  if (angle > TWO_PI) { angle = 0; }
}

