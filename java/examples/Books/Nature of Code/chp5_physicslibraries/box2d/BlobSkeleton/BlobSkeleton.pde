// The Nature of Code
// <http://www.shiffman.net/teaching/nature>
// Spring 2012
// PBox2D example

// A blob skeleton
// Could be used to create blobbly characters a la Nokia Friends
// http://postspectacular.com/work/nokia/friends/start

import pbox2d.*;

import org.jbox2d.collision.shapes.*;
import org.jbox2d.common.*;
import org.jbox2d.dynamics.*;
import org.jbox2d.dynamics.joints.*;

// A reference to our box2d world
PBox2D box2d;

// A list we'll use to track fixed objects
ArrayList<Boundary> boundaries;

// Our "blob" object
Skeleton blob;

// Just a single box this time
Box box;
// The Spring that will attach to the box from the mouse
Spring spring;

// Draw creature design or skeleton?
boolean skeleton;

void setup() {
  size(640, 360);
  smooth();

  // Initialize box2d physics and create the world
  box2d = new PBox2D(this);
  box2d.createWorld();

  // Add some boundaries
  boundaries = new ArrayList<Boundary>();
  boundaries.add(new Boundary(width/2, height-5, width, 10));
  boundaries.add(new Boundary(width/2, 5, width, 10));
  boundaries.add(new Boundary(width-5, height/2, 10, height));
  boundaries.add(new Boundary(5, height/2, 10, height));

  // Make a new blob
  blob = new Skeleton();

  // Make the box
  box = new Box(width/2, 100);

  // Make the spring (it doesn't really get initialized until the mouse is clicked)
  spring = new Spring();
}

// When the mouse is released we're done with the spring
void mouseReleased() {
  spring.destroy();
}

// When the mouse is pressed we. . .
void mousePressed() {
  // Check to see if the mouse was clicked on the box
  if (box.contains(mouseX, mouseY)) {
    // And if so, bind the mouse location to the box with a spring
    spring.bind(mouseX, mouseY, box);
  }
}

void draw() {
  background(255);

  // We must always step through time!

  box2d.step();


  // Show the blob!
  if (skeleton) {
    blob.displaySkeleton();
  } 
  else {
    blob.displayCreature();
  }

  // Show the boundaries!
  for (Boundary wall: boundaries) {
    wall.display();
  }

  // Always alert the spring to the new mouse location
  spring.update(mouseX, mouseY);

  // Draw the box
  box.display();
  // Draw the spring (it only appears when active)
  spring.display();

  fill(0);
  text("Space bar to toggle creature/skeleton.\nClick and drag the box.", 20, height-30);
}


void keyPressed() {
  if (key == ' ') {
    skeleton = !skeleton;
  }
}

