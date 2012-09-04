// The Nature of Code
// <http://www.shiffman.net/teaching/nature>
// Spring 2011
// PBox2D example

// Showing how to use applyForce() with box2d

import pbox2d.*;
import org.jbox2d.collision.shapes.*;
import org.jbox2d.common.*;
import org.jbox2d.dynamics.*;

// A reference to our box2d world
PBox2D box2d;

// Movers, jsut like before!
Mover[] movers = new Mover[25];

// Attractor, just like before!
Attractor a;

void setup() {
  size(800,200);
  smooth();

  box2d = new PBox2D(this);
  box2d.createWorld();
  // No global gravity force
  box2d.setGravity(0,0);

  for (int i = 0; i < movers.length; i++) {
    movers[i] = new Mover(random(8,16),random(width),random(height));
  }
  a = new Attractor(32,width/2,height/2);
}

void draw() {
  background(255);

  // We must always step through time!
  box2d.step();

  a.display();

  for (int i = 0; i < movers.length; i++) {
    // Look, this is just like what we had before!
    Vec2 force = a.attract(movers[i]);
    movers[i].applyForce(force);
    movers[i].display();
  }
}









