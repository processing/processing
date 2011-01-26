PFont font;
String letters = "";
PrintWriter output;

void setup() {
  size(100, 100);
  fill(0);
  font = loadFont("Eureka-24.vlw");
  textFont(font);
  // Create a new file in the sketch directory
  output = createWriter("words.txt");
}

void draw() {
  background(204);
  text(letters, 5, 50);
}

void keyPressed() {
  if (key == ' ') { // Spacebar pressed
    output.println(letters); // Write data to words.txt
    letters = ""; // Clear the letter String
  } else {
    letters = letters + key;
  }
  if (key == ENTER) {
    output.flush(); // Write the remaining data
    output.close(); // Finish the file
    exit(); // Stop the program
  }
}
