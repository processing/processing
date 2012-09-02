// The Nature of Code
// <http://www.shiffman.net/teaching/nature>
// Spring 2011
// PBox2D example

// Basic example of controlling an object with the mouse (by attaching a spring)

import pbox2d.*;
import org.jbox2d.common.*;
import org.jbox2d.dynamics.joints.*;
import org.jbox2d.collision.shapes.*;
import org.jbox2d.collision.shapes.Shape;
import org.jbox2d.common.*;
import org.jbox2d.dynamics.*;

// A reference to our box2d world
PBox2D box2d;

// A list we'll use to track fixed objects
ArrayList<Boundary> boundaries;

// Just a single box this time
Box box;

void setup() {
  size(640,360);
  smooth();

  // Initialize box2d physics and create the world
  box2d = new PBox2D(this);
  box2d.createWorld();

  // Make the box
  box = new Box(width/2,height/2);

  // Add a bunch of fixed boundaries
  boundaries = new ArrayList<Boundary>();
  boundaries.add(new Boundary(width/2,height-5,width,10,0));
  boundaries.add(new Boundary(width/2,5,width,10,0));
  boundaries.add(new Boundary(width-5,height/2,10,height,0));
  boundaries.add(new Boundary(5,height/2,10,height,0));
}


void draw() {
  background(255);

  // We must always step through time!
    
  //if (box.dragged) {
    box.setLocation(mouseX,mouseY);
  //}
  
  box2d.step();

  // Draw the boundaries
  for (Boundary wall : boundaries) {
    wall.display();
  }

  // Draw the box
  box.display();

  
}

void mousePressed() {
  if (box.contains(mouseX,mouseY)) {
    box.dragged = true;
  } 
}

void mouseReleased() {
  box.dragged = false; 
}



