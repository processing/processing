// Ortho
// by REAS <http://reas.com>

// Click to see the difference between orthographic projection
// and perspective projection as applied to a simple box.

// Created 28 April 2005


void setup() 
{
  size(200, 200, P3D);
  noStroke();
  fill(204);
}

void draw() 
{
  background(0);
  lights();
 
  if(mousePressed) {
    float fov = 60.0; 
    float cameraZ = (height/2.0) / tan(PI * fov / 360.0); 
    perspective(fov, float(width)/float(height), 
                cameraZ/10.0, cameraZ*10.0); 
  } else {
    ortho(-width/2, width/2, -height/2, height/2, -10, 10); 
  }
  
  translate(100, 100);
  rotateX(-PI/6); 
  rotateY(PI/3); 
  box(85); 
}

