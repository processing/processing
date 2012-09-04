// The Nature of Code
// <http://www.shiffman.net/teaching/nature>
// Spring 2011
// PBox2D example

// Basic example of controlling an object with our own motion (by attaching a MouseJoint)
// Also demonstrates how to know which object was hit

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

// An ArrayList of particles that will fall on the surface
ArrayList<Particle> particles;

Boundary wall;

void setup() {
  size(400, 300);
  smooth();

  // Initialize box2d physics and create the world
  box2d = new PBox2D(this);
  box2d.createWorld();

  // Turn on collision listening!
  box2d.listenForCollisions();

  // Create the empty list
  particles = new ArrayList<Particle>();

  wall = new Boundary(width/2, height-5, width, 10);
}

void draw() {
  background(255);

  if (random(1) < 0.1) {
    float sz = random(4, 8);
    particles.add(new Particle(random(width), 20, sz));
  }


  // We must always step through time!
  box2d.step();

  // Look at all particles
  for (int i = particles.size()-1; i >= 0; i--) {
    Particle p = particles.get(i);
    p.display();
    // Particles that leave the screen, we delete them
    // (note they have to be deleted from both the box2d world and our list
    if (p.done()) {
      particles.remove(i);
    }
  }

  wall.display();
}


// Collision event functions!
void beginContact(Contact cp) {
  // Get both shapes
  Fixture f1 = cp.getFixtureA();
  Fixture f2 = cp.getFixtureB();
  // Get both bodies
  Body b1 = f1.getBody();
  Body b2 = f2.getBody();

  // Get our objects that reference these bodies
  Object o1 = b1.getUserData();
  Object o2 = b2.getUserData();

  if (o1.getClass() == Particle.class && o2.getClass() == Particle.class) {
    Particle p1 = (Particle) o1;
    p1.delete();
    Particle p2 = (Particle) o2;
    p2.delete();
  }

  if (o1.getClass() == Boundary.class) {
    Particle p = (Particle) o2;
    p.change();
  }
  if (o2.getClass() == Boundary.class) {
    Particle p = (Particle) o1;
    p.change();
  }


}

// Objects stop touching each other
void endContact(Contact cp) {
}

















