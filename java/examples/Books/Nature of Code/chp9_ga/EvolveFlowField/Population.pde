// Pathfinding w/ Genetic Algorithms
// Daniel Shiffman <http://www.shiffman.net>

// A class to describe a population of "creatures"

class Population {

  float mutationRate;          // Mutation rate
  Rocket[] population;         // Array to hold the current population
  ArrayList<Rocket> darwin;    // ArrayList which we will use for our "mating pool"
  int generations;             // Number of generations

  int order;         // Keep track of the order of creature's finishing the maze

   // Initialize the population
   Population(float m, int num) {
    mutationRate = m;
    population = new Rocket[num];
    darwin = new ArrayList<Rocket>();
    generations = 0;
    //make a new set of creatures
    for (int i = 0; i < population.length; i++) {
      PVector location = new PVector(start.r.x+start.r.width/2,start.r.y+start.r.height/2);
      population[i] = new Rocket(location, new DNA(dnasize));
    }
    order = 1;  // The first one to finish will be #1
  }

  void live (ArrayList<Obstacle> o) {
    // For every creature
    for (int i = 0; i < population.length; i++) {
      // If it finishes, mark it down as done!
      if ((population[i].finished()) && (!population[i].stopped())) {
        population[i].setFinish(order);
        order++;
      }
      // Run it
      population[i].run(o);
    }
  }

  // Did anything finish?
  boolean targetReached() {
    for (int i = 0; i < population.length; i++) {
      if (population[i].finished()) return true;
    }
    return false;
  }

  // Calculate fitness for each creature
  void calcFitness() {
    for (int i = 0; i < population.length; i++) {
      population[i].calcFitness();
    }
    order = 1;  // Hmmm, awkward place for this, we have to reset this for the next generation
  }

  // Generate a mating pool
  void naturalSelection() {
    // Clear the ArrayList
    darwin.clear();

    // Calculate total fitness of whole population
    float totalFitness = getTotalFitness();

    // Calculate normalized fitness for each member of the population
    // Based on normalized fitness, each member will get added to the mating pool a certain number of times a la roulette wheel
    // A higher fitness = more entries to mating pool = more likely to be picked as a parent
    // A lower fitness = fewer entries to mating pool = less likely to be picked as a parent
    for (int i = 0; i < population.length; i++) {
      float fitnessNormal = population[i].getFitness() / totalFitness;
      int n = (int) (fitnessNormal * 50000);  // Arbitrary multiplier, consider mapping fix
      for (int j = 0; j < n; j++) {
        darwin.add(population[i]);
      }
    }
  }

  // Making the next generation
  void generate() {
    // Refill the population with children from the mating pool
    for (int i = 0; i < population.length; i++) {
      int m = int(random(darwin.size()));
      int d = int(random(darwin.size()));
      // Pick two parents
      Rocket mom = darwin.get(m);
      Rocket dad = darwin.get(d);
      // Get their genes
      DNA momgenes = mom.getDNA();
      DNA dadgenes = dad.getDNA();
      // Mate their genes
      DNA child = momgenes.crossover(dadgenes);
      // Mutate their genes
      child.mutate(mutationRate);
      // Fill the new population with the new child
      PVector location = new PVector(start.r.x+start.r.width/2,start.r.y+start.r.height/2);
      population[i] = new Rocket(location, child);
    }
    generations++;
  }

  int getGenerations() {
    return generations;
  }

  //compute total fitness for the population
  float getTotalFitness() {
    float total = 0;
    for (int i = 0; i < population.length; i++) {
      total += population[i].getFitness();
    }
    return total;
  }

}
