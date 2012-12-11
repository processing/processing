// The Nature of Code
// Daniel Shiffman
// http://natureofcode.com

// Pathfinding w/ Genetic Algorithms

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

