// The Nature of Code
// <http://www.shiffman.net/teaching/nature>
// Spring 201
// PBox2D example

// Example demonstrating distance joints 
// A bridge is formed by connected a series of particles with joints

import pbox2d.*;
import org.jbox2d.common.*;
import org.jbox2d.dynamics.joints.*;
import org.jbox2d.collision.shapes.*;
import org.jbox2d.collision.shapes.Shape;
import org.jbox2d.common.*;
import org.jbox2d.dynamics.*;
import org.jbox2d.dynamics.contacts.*;

// A reference to our box2d world
PBox2D box2d;

// A list we'll use to track fixed objects
ArrayList<Boundary> boundaries;


// A list for all of our rectangles
ArrayList<Pair> pairs;

void setup() {
  size(800,200);
  smooth();

  // Initialize box2d physics and create the world
  box2d = new PBox2D(this);
  box2d.createWorld();
  
  // Create ArrayLists	
  pairs = new ArrayList<Pair>();
  
  boundaries = new ArrayList<Boundary>();

  // Add a bunch of fixed boundaries
  boundaries.add(new Boundary(width/4,height-5,width/2-50,10));
  boundaries.add(new Boundary(3*width/4,height-50,width/2-50,10));

}

void draw() {
  background(255);

  // We must always step through time!
  box2d.step();

  // When the mouse is clicked, add a new Box object

  // Display all the boxes
  for (Pair p: pairs) {
    p.display();
  }

  // Display all the boundaries
  for (Boundary wall: boundaries) {
    wall.display();
  }
  
  fill(0);
  text("Click mouse to add connected particles.",10,20);
}

void mousePressed() {
   Pair p = new Pair(mouseX,mouseY);
   pairs.add(p);
}









