
// Based on code 43-02 (p. 409)


Ring[] rings;                  // Declare the array
int numRings = 50;
int currentRing = 0;

boolean record = false;

void setup() {
  size(513, 666);
  smooth();
  rings = new Ring[numRings];  // Construct the array
  for (int i = 0; i < numRings; i++) {
    rings[i] = new Ring();     // Construct each object
  }
}

void draw() {
  if(record) {
    beginRecord(PDF, "page_394-alt.pdf"); 
  }

  background(255);
  for (int i = 0; i < numRings; i++) {
    rings[i].grow();
    rings[i].display();
  }

  if(record) {
    endRecord();
    record = false; 
  }

}

void keyPressed() {
  record = true; 
}

// Click to create a new Ring
void mousePressed() {
  rings[currentRing].start(mouseX, mouseY);
  currentRing++;
  if (currentRing >= numRings) {
    currentRing = 0;
  }
}

class Ring {
  float x, y;          // X-coordinate, y-coordinate
  float diameter;      // Diameter of the ring
  boolean on = false;  // Turns the display on and off

  void start(float xpos, float ypos) {
    x = xpos;
    y = ypos; 
    on = true;
    diameter = 1;
  }

  void grow() {
    if (on == true) {
      diameter += 0.5;
      if (diameter > height*2) {
        on = false;
      }
    }
  }

  void display() {
    if (on == true) {
      noFill();
      strokeWeight(1);
      stroke(102, 153);
      ellipse(x, y, diameter, diameter);
    }
  }
}
