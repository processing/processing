// The Nature of Code
// Daniel Shiffman
// http://natureofcode.com

// Interactive Selection
// http://www.genarts.com/karl/papers/siggraph91.html

// The class for our "face", contains DNA sequence, fitness value, position on screen

// Fitness Function f(t) = t (where t is "time" mouse rolls over face)

class Face {

  DNA dna;          // Face's DNA
  float fitness;    // How good is this face?
  float x, y;       // Position on screen
  int wh = 70;      // Size of square enclosing face
  boolean rolloverOn; // Are we rolling over this face?

  Rectangle r;

  // Create a new face
  Face(DNA dna_, float x_, float y_) {
    dna = dna_;
    x = x_; 
    y = y_;
    fitness = 1;
    // Using java.awt.Rectangle (see: http://java.sun.com/j2se/1.4.2/docs/api/java/awt/Rectangle.html)
    r = new Rectangle(int(x-wh/2), int(y-wh/2), int(wh), int(wh));
  }

  // Display the face
  void display() {
    // We are using the face's DNA to pick properties for this face
    // such as: head size, color, eye position, etc.
    // Now, since every gene is a floating point between 0 and 1, we map the values
    float r          = map(dna.genes[0],0,1,0,70);
    color c          = color(dna.genes[1],dna.genes[2],dna.genes[3]);
    float eye_y      = map(dna.genes[4],0,1,0,5);
    float eye_x      = map(dna.genes[5],0,1,0,10);
    float eye_size   = map(dna.genes[5],0,1,0,10);
    color eyecolor   = color(dna.genes[4],dna.genes[5],dna.genes[6]);
    color mouthColor = color(dna.genes[7],dna.genes[8],dna.genes[9]);
    float mouth_y    = map(dna.genes[5],0,1,0,25);
    float mouth_x    = map(dna.genes[5],0,1,-25,25);
    float mouthw     = map(dna.genes[5],0,1,0,50);
    float mouthh     = map(dna.genes[5],0,1,0,10);

    // Once we calculate all the above properties, we use those variables to draw rects, ellipses, etc.
    pushMatrix();
    translate(x, y);
    noStroke();

    // Draw the head
    fill(c);
    ellipseMode(CENTER);
    ellipse(0, 0, r, r);

    // Draw the eyes
    fill(eyecolor);
    rectMode(CENTER);
    rect(-eye_x, -eye_y, eye_size, eye_size);
    rect( eye_x, -eye_y, eye_size, eye_size);

    // Draw the mouth
    fill(mouthColor);
    rectMode(CENTER);
    rect(mouth_x, mouth_y, mouthw, mouthh);

    // Draw the bounding box
    stroke(0.25);
    if (rolloverOn) fill(0, 0.25);
    else noFill();
    rectMode(CENTER);
    rect(0, 0, wh, wh);
    popMatrix();

    // Display fitness value
    textAlign(CENTER);
    if (rolloverOn) fill(0);
    else fill(0.25);
    text(int(fitness), x, y+55);
  }

  float getFitness() {
    return fitness;
  }

  DNA getDNA() {
    return dna;
  }

  // Increment fitness if mouse is rolling over face
  void rollover(int mx, int my) {
    if (r.contains(mx, my)) {
      rolloverOn = true;
      fitness += 0.25;
    } else {
      rolloverOn = false;
    }
  }
}

