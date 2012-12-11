// The Nature of Code
// Daniel Shiffman
// http://natureofcode.com

// Forces (Gravity and Fluid Resistence) with Vectors 
 
// Demonstration of multiple force acting on bodies (Mover class)
// Bodies experience gravity continuously
// Bodies experience fluid resistance when in "water"

// Five moving bodies
Mover[] movers = new Mover[5];

// Liquid
Liquid liquid;

void setup() {
  size(450, 450);
  randomSeed(1);
  reset();
  // Create liquid object
  liquid = new Liquid(0, height/2, width, height/2, 0.1);
}

void draw() {
  background(255);
  
  // Draw water
  liquid.display();

  for (int i = 0; i < movers.length; i++) {
    
    // Is the Mover in the liquid?
    if (liquid.contains(movers[i])) {
      // Calculate drag force
      PVector dragForce = liquid.drag(movers[i]);
      // Apply drag force to Mover
      movers[i].applyForce(dragForce);
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
  //text("click mouse to reset",10,30);
  
  if (frameCount % 20 == 0) saveFrame("ch2_05_####.png");
}

void mousePressed() {
  reset();
}

// Restart all the Mover objects randomly
void reset() {
  for (int i = 0; i < movers.length; i++) {
    movers[i] = new Mover(random(0.5*2.25,3*2.25), 20*2.25+i*40*2.25, 0);
  }
}







