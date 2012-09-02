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


float mutationRate = 0.01;    // Mutation rate
int totalPopulation = 150;      // Total Population

DNA[] population;             // Array to hold the current population
ArrayList<DNA> matingPool;    // ArrayList which we will use for our "mating pool"
String target;                // Target phrase

PFont f;

void setup() {
  size(800, 200);
  target = "To be or not to be.";

  population = new DNA[totalPopulation];

  for (int i = 0; i < population.length; i++) {
    population[i] = new DNA(target.length());
  }
  
  f = createFont("Courier",12,true);
}

void draw() {
  for (int i = 0; i < population.length; i++) {
    population[i].calcFitness(target);
  }

  ArrayList<DNA> matingPool = new ArrayList<DNA>();  // ArrayList which we will use for our "mating pool"

  for (int i = 0; i < population.length; i++) {
    int nnnn = int(population[i].fitness * 100);  // Arbitrary multiplier, we can also use monte carlo method
    for (int j = 0; j <nnnn; j++) {              // and pick two random numbers
      matingPool.add(population[i]);
    }
  }

  for (int i = 0; i < population.length; i++) {
    int a = int(random(matingPool.size()));
    int b = int(random(matingPool.size()));
    DNA partnerA = matingPool.get(a);
    DNA partnerB = matingPool.get(b);
    DNA child = partnerA.crossover(partnerB);
    child.mutate(mutationRate);
    population[i] = child;
  }
  
  background(255);
  fill(0);
  String everything = "";
  for (int i = 0; i < population.length; i++) {
    everything += population[i].getPhrase() + "    ";
  }
  textFont(f,12);
  text(everything,10,10,width,height);

  
}

// Genetic Algorithm, Evolving Shakespeare
// Daniel Shiffman <http://www.shiffman.net>

// A class to describe a psuedo-DNA, i.e. genotype
//   Here, a virtual organism's DNA is an array of character.
//   Functionality:
//      -- convert DNA into a string
//      -- calculate DNA's "fitness"
//      -- mate DNA with another set of DNA
//      -- mutate DNA


class DNA {

  // The genetic sequence
  char[] genes;
  
  float fitness;
  
  // Constructor (makes a random DNA)
  DNA(int num) {
    genes = new char[num];
    for (int i = 0; i < genes.length; i++) {
      genes[i] = (char) random(32,128);  // Pick from range of chars
    }
  }
  
  // Converts character array to a String
  String getPhrase() {
    return new String(genes);
  }
  
  // Fitness function (returns floating point % of "correct" characters)
  void calcFitness (String target) {
     int score = 0;
     for (int i = 0; i < genes.length; i++) {
        if (genes[i] == target.charAt(i)) {
          score++;
        }
     }
     fitness = (float)score / (float)target.length();
  }
  
  // Crossover
  DNA crossover(DNA partner) {
    // A new child
    DNA child = new DNA(genes.length);
    
    int midpoint = int(random(genes.length)); // Pick a midpoint
    
    // Half from one, half from the other
    for (int i = 0; i < genes.length; i++) {
      if (i > midpoint) child.genes[i] = genes[i];
      else              child.genes[i] = partner.genes[i];
    }
    return child;
  }
  
  // Based on a mutation probability, picks a new random character
  void mutate(float mutationRate) {
    for (int i = 0; i < genes.length; i++) {
      if (random(1) < mutationRate) {
        genes[i] = (char) random(32,128);
      }
    }
  }
}

