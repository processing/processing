/**
 * Ortho. 
 * 
 * Move the mouse across the screen to change the parameters
 * for the orthographic projection.
 * The ortho() function sets an orthographic projection and 
 * defines a parallel clipping volume. All objects with the 
 * same dimension appear the same size, regardless of whether 
 * they are near or far from the camera. The parameters to this 
 * function specify the clipping volume where left and right 
 * are the minimum and maximum x values, top and bottom are the 
 * minimum and maximum y values, and near and far are the minimum 
 * and maximum z values.
 */

void setup() 
{
  size(640, 480, P3D);
  noStroke();
  fill(102);
}

void draw() 
{
  background(255);
  lights();

  //ortho(0, width, 0, height, -10, 10);  // Default ortho settings
  //ortho(-width, mouseX, -height/4.0, mouseY/4.0, -10, 10); 
  ortho(-width/2, width/2, -height/2, height/2, -10, 10); 

  translate(100, 200, -200);
  rotateY(PI/18);
  
  for (int i = 0; i < width; i += 100) {
    box(20, 500, 100);
    translate(80, 0, -40);
  }

}

