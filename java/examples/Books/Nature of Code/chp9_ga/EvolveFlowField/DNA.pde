// The Nature of Code
// Daniel Shiffman
// http://natureofcode.com

// Pathfinding w/ Genetic Algorithms

// DNA is an array of vectors

class DNA {

  // The genetic sequence
  PVector[] genes;

  // Constructor (makes a DNA of random PVectors)
  DNA(int num) {
    genes = new PVector[num];
    for (int i = 0; i < genes.length; i++) {
      genes[i] = PVector.random2D();
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
        genes[i] = PVector.random2D();
      }
    }
  }
  
  void debugDraw() {
    int cols = width / gridscale;
    int rows = height / gridscale;
        for (int i = 0; i < cols; i++) {
      for (int j = 0; j < rows; j++) {
        drawVector(genes[i+j*cols],i*gridscale,j*gridscale,gridscale-2);
      }
    }
  }

  // Renders a vector object 'v' as an arrow and a location 'x,y'
  void drawVector(PVector v, float x, float y, float scayl) {
    pushMatrix();
    float arrowsize = 4;
    // Translate to location to render vector
    translate(x+gridscale/2,y);
    stroke(0,100);
    // Call vector heading function to get direction (note that pointing up is a heading of 0) and rotate
    rotate(v.heading());
    // Calculate length of vector & scale it to be bigger or smaller if necessary
    float len = v.mag()*scayl;
    // Draw three lines to make an arrow (draw pointing up since we've rotate to the proper direction)
    line(-len/2,0,len/2,0);
    //noFill();
    //ellipse(-len/2,0,2,2);
    popMatrix();
  }
  
}

