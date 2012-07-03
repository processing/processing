/**
 * Forces (Gravity and Fluid Resistence) with Vectors 
 * by Daniel Shiffman.  
 * 
 * Demonstration of multiple force acting on bodies (Mover class)
 * Bodies experience gravity continuously
 * Bodies experience fluid resistance when in "water"
 */

// Five moving bodies
Mover[] movers = new Mover[5];

// Liquid
Liquid liquid;

void setup() {
  size(360, 640);
  smooth();
  reset();
  // Create liquid object
  liquid = new Liquid(0, height/2, width, height/2, 0.1);
}

void draw() {
  background(0);
  
  // Draw water
  liquid.display();

  for (int i = 0; i < movers.length; i++) {
    
    // Is the Mover in the liquid?
    if (liquid.contains(movers[i])) {
      // Calculate drag force
      PVector drag = liquid.drag(movers[i]);
      // Apply drag force to Mover
      movers[i].applyForce(drag);
    }

    // Gravity is scaled by mass here!
    PVector gravity = new PVector(0, 0.1*movers[i].mass);
    // Apply gravity
    movers[i].applyForce(gravity);
   
    // Update and display
    movers[i].update();
    movers[i].display();
    movers[i].checkEdges();
  }
  
  fill(255);
  text("click mouse to reset",10,30);
  
}

void mousePressed() {
  reset();
}

// Restart all the Mover objects randomly
void reset() {
  for (int i = 0; i < movers.length; i++) {
    movers[i] = new Mover(random(1, 5), 40+i*70, 0);
  }
}







