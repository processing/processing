// Ortho
// by REAS <http://reas.com>

// Move the mouse across the screen to change the parameters
// for the orthographic projection.
// The ortho() function sets an orthographic projection and 
// defines a parallel clipping volume. All objects with the 
// same dimension appear the same size, regardless of whether 
// they are near or far from the camera. The parameters to this 
// function specify the clipping volume where left and right 
// are the minimum and maximum x values, top and bottom are the 
// minimum and maximum y values, and near and far are the minimum 
// and maximum z values.

// Created 28 April 2005


void setup() 
{
  size(200, 200, P3D);
  noStroke();
}

void draw() 
{
  background(255);
  lights();
  
  //ortho(0, width, 0, height, -10, 10);  // Default ortho settings
  ortho(-width, mouseX, -height/2.0, mouseY/2.0, -10, 10); 
  
  translate(0, 0, -100);
  rotateX(PI/4);
  rotateZ(PI/3);
    
  pushMatrix();
  for(int i=0; i<width; i+=20) {
    for(int j=0; j<height; j+=20) {
      box(10, 10, (j+i) / 4.0);
      translate(20, 0, 0);
    }
    translate(-200, 20, 0);
  }
  popMatrix();

}

