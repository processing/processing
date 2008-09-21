/**
 * Sine. 
 * 
 * Smoothly scaling size with the sin() function. 
 */
 
float spin = 0.0; 
float diameter = 84.0; 
float angle;

float angle_rot; 
int rad_points = 90;

void setup() 
{
  size(200, 200);
  noStroke();
  smooth();
}

void draw() 
{ 
  background(153);
  
  translate(130, 65);
  
  fill(255);
  ellipse(0, 0, 16, 16);
  
  angle_rot = 0;
  fill(51);

  for(int i=0; i<5; i++) {
    pushMatrix();
    rotate(angle_rot + -45);
    ellipse(-116, 0, diameter, diameter);
    popMatrix();
    angle_rot += PI*2/5;
  }

  diameter = 34 * sin(angle) + 168;
  
  angle += 0.02;
  if (angle > TWO_PI) { angle = 0; }
}

