// Rotate 3D
// by REAS <http://reas.com>

// Rotating simultaneously in the X and Y axis. 
// Transformation functions such as rotate() are additive.
// Successively calling rotate(1.0) and rotate(2.0)
// is equivalent to calling rotate(3.0).

// Updated 21 August 2002

float a = 0.0;
float rSize;  // rectangle size

void setup() 
{
  size(200, 200, P3D);
  rSize = width/4;  
  noStroke();
  fill(204, 204);
  framerate(30);
}

void draw() 
{
  background(0);
  
  a += 0.01;
  if(a > TWO_PI) { 
    a = 0.0; 
  }
  
  translate(width/2, height/2);
  
  rotateX(a);
  rotateY(a*2);
  rect(-rSize, -rSize, rSize*2, rSize*2);
  
  rotateX(a*1.001);
  rotateY(a*2.002);
  rect(-rSize, -rSize, rSize*2, rSize*2);

}
