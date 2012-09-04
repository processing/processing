// Genetic Algorithm, Evolving Shakespeare
// Daniel Shiffman <http://www.shiffman.net>

// Demonstration of using a genetic algorithm to perform a search

// setup()
//  # Step 1: The Population 
//    # Create an empty population (an array or ArrayList)
//    # Fill it with DNA encoded objects (pick random values to start)

// draw()
//  # Step 1: Selection 
//    # Create an empty mating pool (an empty ArrayList)
//    # For every member of the population, evaluate its fitness based on some criteria / function, 
//      and add it to the mating pool in a manner consistant with its fitness, i.e. the more fit it 
//      is the more times it appears in the mating pool, in order to be more likely picked for reproduction.

//  # Step 2: Reproduction Create a new empty population
//    # Fill the new population by executing the following steps:
//       1. Pick two "parent" objects from the mating pool.
//       2. Crossover -- create a "child" object by mating these two parents.
//       3. Mutation -- mutate the child's DNA based on a given probability.
//       4. Add the child object to the new population.
//    # Replace the old population with the new population
//  
//   # Rinse and repeat


PFont f;
String target;
int popmax;
float mutationRate;
Population population;

void setup() {
  size(800, 200);
  f = createFont("Courier", 32, true);
  target = "To be or not to be.";
  popmax = 150;
  mutationRate = 0.01;

  // Create a populationation with a target phrase, mutation rate, and populationation max
  population = new Population(target, mutationRate, popmax);
}

void draw() {
  // Generate mating pool
  population.naturalSelection();
  //Create next generation
  population.generate();
  // Calculate fitness
  population.calcFitness();
  displayInfo();

  // If we found the target phrase, stop
  if (population.finished()) {
    println(millis()/1000.0);
    noLoop();
  }
}

void displayInfo() {
  background(255);
  // Display current status of populationation
  String answer = population.getBest();
  textFont(f);
  textAlign(LEFT);
  fill(0);
  
  
  textSize(16);
  text("Best phrase:",20,30);
  textSize(32);
  text(answer, 20, 75);

  textSize(12);
  text("total generations: " + population.getGenerations(), 20, 140);
  text("average fitness: " + nf(population.getAverageFitness(), 0, 2), 20, 155);
  text("total populationation: " + popmax, 20, 170);
  text("mutation rate: " + int(mutationRate * 100) + "%", 20, 185);
 
  textSize(10);
  text("All phrases:\n" + population.allPhrases(), 650, 10);
}



