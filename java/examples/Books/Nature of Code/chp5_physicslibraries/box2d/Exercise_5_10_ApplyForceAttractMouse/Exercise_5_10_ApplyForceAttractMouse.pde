// The Nature of Code
// Daniel Shiffman
// http://natureofcode.com

// Basic example of falling rectangles

import pbox2d.*;
import org.jbox2d.collision.shapes.*;
import org.jbox2d.common.*;
import org.jbox2d.dynamics.*;

// A reference to our box2d world
PBox2D box2d;

// A list we'll use to track fixed objects
ArrayList<Boundary> boundaries;
// A list for all of our rectangles
ArrayList<Box> boxes;

void setup() {
  size(640,360);
  // Initialize box2d physics and create the world
  box2d = new PBox2D(this);
  box2d.createWorld();
  // We are setting a custom gravity
  box2d.setGravity(0, -20);

  // Create ArrayLists	
  boxes = new ArrayList<Box>();
  boundaries = new ArrayList<Boundary>();

  // Add a bunch of fixed boundaries
  boundaries.add(new Boundary(width/4,height-5,width/2-50,10));
  boundaries.add(new Boundary(3*width/4,height-5,width/2-50,10));
  boundaries.add(new Boundary(width-5,height/2,10,height));
  boundaries.add(new Boundary(5,height/2,10,height));
}

void draw() {
  background(255);

  // We must always step through time!
  box2d.step();

  // When the mouse is clicked, add a new Box object
  if (random(1) < 0.1) {
    Box p = new Box(random(width),10);
    boxes.add(p);
  }
  
  if (mousePressed) {
    for (Box b: boxes) {
     b.attract(mouseX,mouseY);
    }
  }

  // Display all the boundaries
  for (Boundary wall: boundaries) {
    wall.display();
  }

  // Display all the boxes
  for (Box b: boxes) {
    b.display();
  }

  // Boxes that leave the screen, we delete them
  // (note they have to be deleted from both the box2d world and our list
  for (int i = boxes.size()-1; i >= 0; i--) {
    Box b = boxes.get(i);
    if (b.done()) {
      boxes.remove(i);
    }
  }
  
  fill(0);
  text("Click mouse to attract boxes",20,20);
}




