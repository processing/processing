// The Nature of Code
// Daniel Shiffman
// http://natureofcode.com

// Interactive Selection
// http://www.genarts.com/karl/papers/siggraph91.html

class DNA {

  // The genetic sequence
  float[] genes;
  int len = 20;  // Arbitrary length
  
  //Constructor (makes a random DNA)
  DNA() {
    // DNA is random floating point values between 0 and 1 (!!)
    genes = new float[len];
    for (int i = 0; i < genes.length; i++) {
      genes[i] = random(0,1);
    }
  }
  
  DNA(float[] newgenes) {
    genes = newgenes;
  }
  

  // Crossover
  // Creates new DNA sequence from two (this & 
  DNA crossover(DNA partner) {
    float[] child = new float[genes.length];
    int crossover = int(random(genes.length));
    for (int i = 0; i < genes.length; i++) {
      if (i > crossover) child[i] = genes[i];
      else               child[i] = partner.genes[i];
    }
    DNA newgenes = new DNA(child);
    return newgenes;
  }
  
  // Based on a mutation probability, picks a new random character in array spots
  void mutate(float m) {
    for (int i = 0; i < genes.length; i++) {
      if (random(1) < m) {
         genes[i] = random(0,1);
      }
    }
  }
}
