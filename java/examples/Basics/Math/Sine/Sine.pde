/**
 * Sine. 
 * 
 * Smoothly scaling size with the sin() function. 
 */
 
float diameter = 84.0; 
float angle = 0;

void setup() 
{
  size(640, 360);
  noStroke();
  smooth();
}

void draw() 
{ 
  background(153);
  
  translate(width * 0.3, height * 0.5);
  
  fill(255);
  ellipse(0, 0, 16, 16);
  
  float angleOffset = 0;
  fill(51);

  for (int i = 0; i < 5; i++) {
    pushMatrix();
    rotate(angleOffset + -45);
    ellipse(-116, 0, diameter, diameter);
    popMatrix();
    angleOffset += TWO_PI/5;
  }

  diameter = 34 * sin(angle) + 168;
  
  angle += 0.02;
}

