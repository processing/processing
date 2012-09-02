// Pathfinding Flowfield w/ Genetic Algorithms
// Daniel Shiffman <http://www.shiffman.net>

// This example produces an obstacle course with a start and finish
// Virtual "creatures" are rewarded for making it closer to the finish

// Each creature's DNA is a "flowfield" of PVectors that
// determine steering vectors for each cell on the screen

int gridscale = 24;              // Scale of grid is 1/24 of screen size

// DNA needs one vector for every spot on the grid 
// (it's like a pixel array, but with vectors instead of colors)
int dnasize;

int lifetime;  // How long should each generation live

// Global maxforce and maxspeed (hmmm, could make this part of DNA??)
float maxspeed = 4.0;
float maxforce = 1.0;

Population population;  // Population
int lifecycle;          // Timer for cycle of generation
int recordtime;         // Fastest time to target
Obstacle target;        // Target location
Obstacle start;         // Start location
int diam = 24;          // Size of target

ArrayList<Obstacle> obstacles;  //an array list to keep track of all the obstacles!

void setup() {
  size(640,480);
  smooth();
  
  dnasize = (width / gridscale) * (height / gridscale); 
  lifetime = width/2;

  // Initialize variables
  lifecycle = 0;
  recordtime = lifetime;
  target = new Obstacle(width-diam-diam/2,height/2-diam/2,diam,diam);
  start = new Obstacle(diam/2,height/2-diam/2,diam,diam);

  // Create a population with a mutation rate, and population max
  int popmax = 1000;
  float mutationRate = 0.05;
  population = new Population(mutationRate,popmax);

  // Create the obstacle course  
  obstacles = new ArrayList<Obstacle>();
  obstacles.add(new Obstacle(width/4,40,10,height-80));
  obstacles.add(new Obstacle(width/2,0,10,height/2-10));
  obstacles.add(new Obstacle(width/2,height-height/2+10,10,height/2-10));
  obstacles.add(new Obstacle(2*width/3,height/2-height/8,10,height/4));
}

void draw() {
  background(255);

 // Draw the start and target locations
 start.display();
 target.display();
  
  // Draw the obstacles
  for (Obstacle obs : obstacles) {
    obs.display();
  }

 
  // If the generation hasn't ended yet
  if (lifecycle < lifetime) {
    population.live(obstacles);
    if ((population.targetReached()) && (lifecycle < recordtime)) {
      recordtime = lifecycle;
    }
    lifecycle++;
  // Otherwise a new generation
  } else {
    lifecycle = 0;
    population.calcFitness();
    population.naturalSelection();
    population.generate();
  }

   // Display some info
   textAlign(RIGHT);
   fill(0);
   text("Generation #:" + population.getGenerations(),width-10,18);
   text("Cycles left:" + ((lifetime-lifecycle)/10),width-10,36);
   text("Record cycles: " + recordtime,width-10,54);
   
}

// Move the target if the mouse is pressed
// System will adapt to new target
void mousePressed() {
  target = new Obstacle(mouseX,mouseY,diam,diam);
  recordtime = lifetime;
}

