/**
 * Multiple constructors
 * 
 * A class can have multiple constructors that assign the fields in different ways. 
 * Sometimes it's beneficial to specify every aspect of an object's data by assigning 
 * parameters to the fields, but other times it might be appropriate to define only 
 * one or a few.
 */

Spot sp1, sp2;

void setup() {
  size(640, 360);
  background(204);
  noLoop();
  // Run the constructor without parameters
  sp1 = new Spot();
  // Run the constructor with three parameters
  sp2 = new Spot(width*0.5, height*0.5, 120);
}

void draw() {
  sp1.display();
  sp2.display();
}

class Spot {
  float x, y, radius;
  
  // First version of the Spot constructor;
  // the fields are assigned default values
  Spot() {
    radius = 40;
    x = width*0.25;
    y = height*0.5;
  }
  
  // Second version of the Spot constructor;
  // the fields are assigned with parameters
  Spot(float xpos, float ypos, float r) {
    x = xpos;
    y = ypos;
    radius = r;
  }
  void display() {
    ellipse(x, y, radius*2, radius*2);
  }
  
}
