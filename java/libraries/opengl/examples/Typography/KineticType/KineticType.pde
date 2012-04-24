/**
 * Kinetic Type 
 * by Zach Lieberman. 
 * 
 * Using push() and pop() to define the curves of the lines of type. 
 */
 
Line ln;
Line lns[];

String words[] = {
  "sometimes it's like", "the lines of text", "are so happy", "that they want to dance",
  "or leave the page or jump", "can you blame them?", "living on the page like that",
  "waiting to be read..."
};

void setup() {
  size(640, 360, P3D);
  
  // Array of line objects
  lns = new Line[8];

  // Load the font from the sketch's data directory
  textFont(loadFont("Univers-66.vlw"), 1.0);

  // White type
  fill(255);

  // Creating the line objects
  for(int i = 0; i < 8; i++) {
    // For every line in the array, create a Line object to animate
    // i * 70 is the spacing
    ln = new Line(words[i], 0, i * 70);
    lns[i] = ln;
  }
  
  // To avoid letter occluding each other on the edges
  hint(DISABLE_DEPTH_TEST);  
}

void draw() {
  background(0);
  
  translate(-200, -50, -450);
  rotateY(0.3);

  // Now animate every line object & draw it...
  for(int i = 0; i < 8; i++) {
    float f1 = sin((i + 1.0) * (millis() / 10000.0) * TWO_PI);
    float f2 = sin((8.0 - i) * (millis() / 10000.0) * TWO_PI);
    Line line = lns[i];
    pushMatrix();
    translate(0.0, line.yPosition, 0.0);
    for(int j = 0; j < line.myLetters.length; j++) {
      if(j != 0) {
        translate(textWidth(line.myLetters[j - 1].myChar) * 75, 0.0, 0.0);
      }
      rotateY(f1 * 0.005 * f2);
      pushMatrix();
      scale(75.0);
      text(line.myLetters[j].myChar, 0.0, 0.0);
      popMatrix();
    }
    popMatrix();
  }
}

