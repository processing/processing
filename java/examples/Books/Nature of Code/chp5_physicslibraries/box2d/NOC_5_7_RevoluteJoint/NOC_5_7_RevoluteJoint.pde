// The Nature of Code
// <http://www.shiffman.net/teaching/nature>
// Spring 201
// PBox2D example

// Example demonstrating revolute joint

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

// An object to describe a Windmill (two bodies and one joint)
Windmill windmill;

// An ArrayList of particles that will fall on the surface
ArrayList<Particle> particles;

void setup() {
  size(800,200);
  smooth();

  // Initialize box2d physics and create the world
  box2d = new PBox2D(this);
  box2d.createWorld();

  // Make the windmill at an x,y location
  windmill = new Windmill(width/2,175);

  // Create the empty list
  particles = new ArrayList<Particle>();

}

// Click the mouse to turn on or off the motor
void mousePressed() {
  windmill.toggleMotor();
}

void draw() {
  background(255);

  if (random(1) < 0.1) {
    float sz = random(4,8);
    particles.add(new Particle(random(width/2-100,width/2+100),-20,sz));
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

  // Draw the windmill
  windmill.display();
  
  String status = "OFF";
  if (windmill.motorOn()) status = "ON";
  
  fill(0);
  text("Click mouse to toggle motor.\nMotor: " + status,10,height-30);
  

}










