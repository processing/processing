// The Nature of Code
// Daniel Shiffman
// http://natureofcode.com

// Simple Toxiclibs Spring

import toxi.physics2d.*;
import toxi.physics2d.behaviors.*;
import toxi.geom.*;

// Reference to physics world
VerletPhysics2D physics;

Particle p1;
Particle p2;

void setup() {
  size(640,360);

  // Initialize the physics
  physics=new VerletPhysics2D();
  physics.addBehavior(new GravityBehavior(new Vec2D(0,0.5)));

  // Set the world's bounding box
  physics.setWorldBounds(new Rect(0,0,width,height));
  
  // Make two particles
  p1 = new Particle(new Vec2D(width/2,20));
  p2 = new Particle(new Vec2D(width/2+160,20));
  // Lock one in place
  p1.lock();

  // Make a spring connecting both Particles
  VerletSpring2D spring=new VerletSpring2D(p1,p2,160,0.01);

  // Anything we make, we have to add into the physics world
  physics.addParticle(p1);
  physics.addParticle(p2);
  physics.addSpring(spring);
}

void draw() {

  // Update the physics world
  physics.update();

  background(255);

  // Draw a line between the particles
  stroke(0);
  strokeWeight(2);
  line(p1.x,p1.y,p2.x,p2.y);

  // Display the particles
  p1.display();
  p2.display();

  // Move the second one according to the mouse
  if (mousePressed) {
    p2.lock();
    p2.x = mouseX;
    p2.y = mouseY;
    p2.unlock();
  } 
}



