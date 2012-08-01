String s;
PFont font;
void setup() {
  font = loadFont(); // Load and set the default font for drawing text
  textFont(font);
  softkey("Input"); // Create a softkey called Input
  s = "No input"; // Initialize s with an initial message
}
void draw() {
  background(200);
  text(s, 0, height / 2); // Draw the String s in the middle of the screen
}

void softkeyPressed(String label) {
// Check the value of the softkey label to determine the action to take
  if (label.equals("Input")) {
// If the Input softkey is pressed, open a textInput window for the user
// to type text. It will be drawn on the screen by the draw() method
    s = textInput();
  }
}
