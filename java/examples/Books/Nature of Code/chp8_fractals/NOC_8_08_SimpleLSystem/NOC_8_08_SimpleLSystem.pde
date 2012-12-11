// The Nature of Code
// Daniel Shiffman
// http://natureofcode.com

// L-System
// Just demonstrating working with L-System strings
// No drawing

// Start with "A"
String current = "A";
// Number of  generations
int count = 0;

void setup() {
  size(800, 200);
  println("Generation " + count + ": " + current);
}

void draw() {
  background(255);
  fill(0);
  text("Click mouse to generate", 10, height-20);
  noLoop();
}

void mousePressed() {
  // A new StringBuffer for the next generation
  StringBuffer next = new StringBuffer();
  
  // Look through the current String to replace according to L-System rules
  for (int i = 0; i < current.length(); i++) {
    char c = current.charAt(i);
    if (c == 'A') {
      // If we find A replace with AB
      next.append("AB");
    }  else if (c == 'B') {
      // If we find B replace with A
      next.append("A");
    }
  }
  // The current String is now the next one
  current = next.toString();
  count++;
  // Print to message console
  println("Generation " + count + ": " + current);
  //println(count + " " + current.length());
}

