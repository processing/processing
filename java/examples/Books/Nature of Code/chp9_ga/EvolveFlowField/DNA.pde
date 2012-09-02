// Pathfinding w/ Genetic Algorithms
// Daniel Shiffman <http://www.shiffman.net>

// DNA is an array of vectors

class DNA {

  // The genetic sequence
  PVector[] genes;
  
  // Constructor (makes a DNA of random PVectors)
  DNA(int num) {
    genes = new PVector[num];
    for (int i = 0; i < genes.length; i++) {
      float angle = random(TWO_PI);
      genes[i] = new PVector(cos(angle),sin(angle));
    }
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
        genes[i] = new PVector(cos(angle),sin(angle));
      }
    }
  }
}
