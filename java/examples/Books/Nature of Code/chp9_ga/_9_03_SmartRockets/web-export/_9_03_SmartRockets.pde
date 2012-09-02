// Smart Rockets w/ Genetic Algorithms
// Daniel Shiffman <http://www.shiffman.net>

// Each Rocket's DNA is an array of PVectors
// Each PVector acts as a force for each frame of animation
// Imagine an booster on the end of the rocket that can point in any direction
// and fire at any strength every frame

// The Rocket's fitness is a function of how close it gets to the target as well as how fast it gets there

// This example is inspired by Jer Thorp's Smart Rockets
// http://www.blprnt.com/smartrockets/

int lifetime;  // How long should each generation live

Population population;  // Population

int lifecycle;          // Timer for cycle of generation
int recordtime;         // Fastest time to target

Obstacle target;        // Target location

//int diam = 24;          // Size of target

ArrayList<Obstacle> obstacles;  //an array list to keep track of all the obstacles!

void setup() {
  size(800, 200);
  smooth();

  // The number of cycles we will allow a generation to live
  lifetime = 300;

  // Initialize variables
  lifecycle = 0;
  recordtime = lifetime;
  
  target = new Obstacle(width/2-12, 24, 24, 24);

  // Create a population with a mutation rate, and population max
  float mutationRate = 0.01;
  population = new Population(mutationRate, 50);

  // Create the obstacle course  
  obstacles = new ArrayList<Obstacle>();
  obstacles.add(new Obstacle(300, height/2, width-600, 10));
}

void draw() {
  background(255);

  // Draw the start and target locations
  target.display();


  // If the generation hasn't ended yet
  if (lifecycle < lifetime) {
    population.live(obstacles);
    if ((population.targetReached()) && (lifecycle < recordtime)) {
      recordtime = lifecycle;
    }
    lifecycle++;
    // Otherwise a new generation
  } 
  else {
    lifecycle = 0;
    population.fitness();
    population.selection();
    population.reproduction();
  }

  // Draw the obstacles
  for (Obstacle obs : obstacles) {
    obs.display();
  }

  // Display some info
  fill(0);
  text("Generation #: " + population.getGenerations(), 10, 18);
  text("Cycles left: " + (lifetime-lifecycle), 10, 36);
  text("Record cycles: " + recordtime, 10, 54);
  
  
}

// Move the target if the mouse is pressed
// System will adapt to new target
void mousePressed() {
  target.location.x = mouseX;
  target.location.y = mouseY;
  recordtime = lifetime;
}

// Pathfinding w/ Genetic Algorithms
// Daniel Shiffman <http://www.shiffman.net>

// DNA is an array of vectors

class DNA {

  // The genetic sequence
  PVector[] genes;

  // The maximum strength of the forces
  float maxforce = 0.1;

  // Constructor (makes a DNA of random PVectors)
  DNA() {
    genes = new PVector[lifetime];
    for (int i = 0; i < genes.length; i++) {
      float angle = random(TWO_PI);
      genes[i] = new PVector(cos(angle), sin(angle));
      genes[i].mult(random(0, maxforce));
    }

    // Let's give each Rocket an extra boost of strength for its first frame
    genes[0].normalize();
  }

  // Constructor #2, creates the instance based on an existing array
  DNA(PVector[] newgenes) {
    // We could make a copy if necessary
    // genes = (PVector []) newgenes.clone();
    genes = newgenes;
  }

  // CROSSOVER
  // Creates new DNA sequence from two (this & and a partner)
  DNA crossover(DNA partner) {
    PVector[] child = new PVector[genes.length];
    // Pick a midpoint
    int crossover = int(random(genes.length));
    // Take "half" from one and "half" from the other
    for (int i = 0; i < genes.length; i++) {
      if (i > crossover) child[i] = genes[i];
      else               child[i] = partner.genes[i];
    }    
    DNA newgenes = new DNA(child);
    return newgenes;
  }

  // Based on a mutation probability, picks a new random Vector
  void mutate(float m) {
    for (int i = 0; i < genes.length; i++) {
      if (random(1) < m) {
        float angle = random(TWO_PI);
        genes[i] = new PVector(cos(angle), sin(angle));
        genes[i].mult(random(0, maxforce));
        //        float angle = random(-0.1,0.1);
        //        genes[i].rotate(angle);
        //        float factor = random(0.9,1.1);
        //        genes[i].mult(factor);
        if (i ==0) genes[i].normalize();
      }
    }
  }
}

// Pathfinding w/ Genetic Algorithms
// Daniel Shiffman <http://www.shiffman.net>

// A class for an obstacle, just a simple rectangle that is drawn
// and can check if a Rocket touches it

// Also using this class for target location


class Obstacle {

  PVector location;
  float w,h;
  
  Obstacle(float x, float y, float w_, float h_) {
    location = new PVector(x,y);
    w = w_;
    h = h_;
  }

  void display() {
    stroke(0);
    fill(175);
    strokeWeight(2);
    rectMode(CORNER);
    rect(location.x,location.y,w,h);
  }

  boolean contains(PVector spot) {
    if (spot.x > location.x && spot.x < location.x + w && spot.y > location.y && spot.y < location.y + h) {
      return true;
    } else {
      return false;
    }
  }

}
// Pathfinding w/ Genetic Algorithms
// Daniel Shiffman <http://www.shiffman.net>

// A class to describe a population of "creatures"

class Population {

  float mutationRate;          // Mutation rate
  Rocket[] population;         // Array to hold the current population
  ArrayList<Rocket> matingPool;    // ArrayList which we will use for our "mating pool"
  int generations;             // Number of generations

   // Initialize the population
   Population(float m, int num) {
    mutationRate = m;
    population = new Rocket[num];
    matingPool = new ArrayList<Rocket>();
    generations = 0;
    //make a new set of creatures
    for (int i = 0; i < population.length; i++) {
      PVector location = new PVector(width/2,height+20);
      population[i] = new Rocket(location, new DNA(),population.length);
    }
  }

  void live (ArrayList<Obstacle> os) {
    // For every creature
    for (int i = 0; i < population.length; i++) {
      // If it finishes, mark it down as done!
      population[i].checkTarget();
      population[i].run(os);
    }
  }

  // Did anything finish?
  boolean targetReached() {
    for (int i = 0; i < population.length; i++) {
      if (population[i].hitTarget) return true;
    }
    return false;
  }

  // Calculate fitness for each creature
  void fitness() {
    for (int i = 0; i < population.length; i++) {
      population[i].fitness();
    }
  }

  // Generate a mating pool
  void selection() {
    // Clear the ArrayList
    matingPool.clear();

    // Calculate total fitness of whole population
    float maxFitness = getMaxFitness();

    // Calculate fitness for each member of the population (scaled to value between 0 and 1)
    // Based on fitness, each member will get added to the mating pool a certain number of times
    // A higher fitness = more entries to mating pool = more likely to be picked as a parent
    // A lower fitness = fewer entries to mating pool = less likely to be picked as a parent
    for (int i = 0; i < population.length; i++) {
      float fitnessNormal = map(population[i].getFitness(),0,maxFitness,0,1);
      int n = (int) (fitnessNormal * 100);  // Arbitrary multiplier
      for (int j = 0; j < n; j++) {
        matingPool.add(population[i]);
      }
    }
  }

  // Making the next generation
  void reproduction() {
    // Refill the population with children from the mating pool
    for (int i = 0; i < population.length; i++) {
      // Sping the wheel of fortune to pick two parents
      int m = int(random(matingPool.size()));
      int d = int(random(matingPool.size()));
      // Pick two parents
      Rocket mom = matingPool.get(m);
      Rocket dad = matingPool.get(d);
      // Get their genes
      DNA momgenes = mom.getDNA();
      DNA dadgenes = dad.getDNA();
      // Mate their genes
      DNA child = momgenes.crossover(dadgenes);
      // Mutate their genes
      child.mutate(mutationRate);
      // Fill the new population with the new child
      PVector location = new PVector(width/2,height+20);
      population[i] = new Rocket(location, child,population.length);
    }
    generations++;
  }

  int getGenerations() {
    return generations;
  }

  // Find highest fintess for the population
  float getMaxFitness() {
    float record = 0;
    for (int i = 0; i < population.length; i++) {
       if(population[i].getFitness() > record) {
         record = population[i].getFitness();
       }
    }
    return record;
  }

}
// Pathfinding w/ Genetic Algorithms
// Daniel Shiffman <http://www.shiffman.net>

// Rocket class -- this is just like our Boid / Particle class
// the only difference is that it has DNA & fitness

class Rocket {

  // All of our physics stuff
  PVector location;
  PVector velocity;
  PVector acceleration;

  // Size
  float r;

  // How close did it get to the target
  float recordDist;

  // Fitness and DNA
  float fitness;
  DNA dna;
  // To count which force we're on in the genes
  int geneCounter = 0;

  boolean hitObstacle = false;    // Am I stuck on an obstacle?
  boolean hitTarget = false;   // Did I reach the target
  int finishTime;              // What was my finish time?

  //constructor
  Rocket(PVector l, DNA dna_, int totalRockets) {
    acceleration = new PVector();
    velocity = new PVector();
    location = l.get();
    r = 4;
    dna = dna_;
    finishTime = 0;          // We're going to count how long it takes to reach target
    recordDist = 10000;      // Some high number that will be beat instantly
  }

  // FITNESS FUNCTION 
  // distance = distance from target
  // finish = what order did i finish (first, second, etc. . .)
  // f(distance,finish) =   (1.0f / finish^1.5) * (1.0f / distance^6);
  // a lower finish is rewarded (exponentially) and/or shorter distance to target (exponetially)
  void fitness() {
    if (recordDist < 1) recordDist = 1;

    // Reward finishing faster and getting close
    fitness = (1/(finishTime*recordDist));

    // Make the function exponential
    fitness = pow(fitness, 4);

    if (hitObstacle) fitness *= 0.1; // lose 90% of fitness hitting an obstacle
    if (hitTarget) fitness *= 2; // twice the fitness for finishing!
  }

  // Run in relation to all the obstacles
  // If I'm stuck, don't bother updating or checking for intersection
  void run(ArrayList<Obstacle> os) {
    if (!hitObstacle && !hitTarget) {
      applyForce(dna.genes[geneCounter]);
      geneCounter = (geneCounter + 1) % dna.genes.length;
      update();
      // If I hit an edge or an obstacle
      obstacles(os);
    }
    // Draw me!
    if (!hitObstacle) {
      display();
    }
  }

  // Did I make it to the target?
  void checkTarget() {
    float d = dist(location.x, location.y, target.location.x, target.location.y);
    if (d < recordDist) recordDist = d;

    if (target.contains(location) && !hitTarget) {
      hitTarget = true;
    } 
    else if (!hitTarget) {
      finishTime++;
    }
  }

  // Did I hit an obstacle?
  void obstacles(ArrayList<Obstacle> os) {
    for (Obstacle obs : os) {
      if (obs.contains(location)) {
        hitObstacle = true;
      }
    }
  }

  void applyForce(PVector f) {
    acceleration.add(f);
  }


  void update() {
    velocity.add(acceleration);
    location.add(velocity);
    acceleration.mult(0);
  }

  void display() {
    //background(255,0,0);
    float theta = velocity.heading2D() + PI/2;
    fill(200, 100);
    stroke(0);
    strokeWeight(1);
    pushMatrix();
    translate(location.x, location.y);
    rotate(theta);

    // Thrusters
    rectMode(CENTER);
    fill(0);
    rect(-r/2, r*2, r/2, r);
    rect(r/2, r*2, r/2, r);

    // Rocket body
    fill(175);
    beginShape(TRIANGLES);
    vertex(0, -r*2);
    vertex(-r, r*2);
    vertex(r, r*2);
    endShape();

    popMatrix();
  }

  float getFitness() {
    return fitness;
  }

  DNA getDNA() {
    return dna;
  }

  boolean stopped() {
    return hitObstacle;
  }
}


