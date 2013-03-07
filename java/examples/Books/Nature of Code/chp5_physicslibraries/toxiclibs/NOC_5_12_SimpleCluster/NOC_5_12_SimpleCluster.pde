// The Nature of Code
// Daniel Shiffman
// http://natureofcode.com

// Force directed graph,
// heavily based on: http://code.google.com/p/fidgen/

import toxi.geom.*;
import toxi.physics2d.*;

// Reference to physics world
VerletPhysics2D physics;

// A list of cluster objects
Cluster cluster;

// Boolean that indicates whether we draw connections or not
boolean showPhysics = true;
boolean showParticles = true;

// Font
PFont f;

void setup() {
  size(640, 360);
  f = createFont("Georgia", 12, true);

  // Initialize the physics
  physics=new VerletPhysics2D();
  physics.setWorldBounds(new Rect(10, 10, width-20, height-20));

  // Spawn a new random graph
  cluster = new Cluster(8, 100, new Vec2D(width/2, height/2));
}

void draw() {

  // Update the physics world
  physics.update();

  background(255);

  // Display all points
  if (showParticles) {
    cluster.display();
  }

  // If we want to see the physics
  if (showPhysics) {
    cluster.showConnections();
  }

  // Instructions
  fill(0);
  textFont(f);
  text("'p' to display or hide particles\n'c' to display or hide connections\n'n' for new graph",10,20);
}

// Key press commands
void keyPressed() {
  if (key == 'c') {
    showPhysics = !showPhysics;
    if (!showPhysics) showParticles = true;
  } 
  else if (key == 'p') {
    showParticles = !showParticles;
    if (!showParticles) showPhysics = true;
  } 
  else if (key == 'n') {
    physics.clear();
    cluster = new Cluster(int(random(2, 20)), random(10, width/2), new Vec2D(width/2, height/2));
  }
}

