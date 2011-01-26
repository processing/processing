// Each letter enters from the bottom in sequence and
// stops when it reaches its destination
PFont font;
String word = "rise";
char[] letters;
float[] y; // Y-coordinate for each letter
int currentLetter = 0; // Letter currently in motion

void setup() {
  size(100, 100);
  font = loadFont("EurekaSmallCaps-36.vlw");
  textFont(font);
  letters = word.toCharArray();
  y = new float[letters.length];
  for (int i = 0; i < letters.length; i++) {
    y[i] = 130; // Position off the screen
  }
  fill(0);
}

void draw() {
  background(204);
  if (y[currentLetter] > 35) {
    y[currentLetter] -= 3; // Move current letter up
  } else {
    if (currentLetter < letters.length - 1) {
      currentLetter++; // Switch to next letter
    }
  }
// Calculate x to center the word on screen
  float x = (width - textWidth(word)) / 2;
  for (int i = 0; i < letters.length; i++) {
    text(letters[i], x, y[i]);
    x += textWidth(letters[i]);
  }
}
