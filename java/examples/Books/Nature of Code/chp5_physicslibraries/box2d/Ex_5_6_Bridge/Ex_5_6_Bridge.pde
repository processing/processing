// The Nature of Code
// <http://www.shiffman.net/teaching/nature>
// Spring 2011
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

// An object to describe a Bridget (a list of particles with joint connections)
Bridge bridge;

// A list for all of our rectangles
ArrayList<Box> boxes;

void setup() {
  size(800, 200);
  smooth();

  // Initialize box2d physics and create the world
  box2d = new PBox2D(this);
  box2d.createWorld();


  // Make the bridge
  bridge = new Bridge(width, width/10);

  // Create ArrayLists	
  boxes = new ArrayList<Box>();
}

void draw() {
  background(255);

  // We must always step through time!
  box2d.step();


  // When the mouse is clicked, add a new Box object
  if (mousePressed) {
    Box p = new Box(mouseX, mouseY);
    boxes.add(p);
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

  // Draw the windmill
  bridge.display();


  fill(0);
  //text("Click mouse to add boxes.", 10, height-10);
}









