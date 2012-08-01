PFont font;

void setup() {
  font = loadFont();
  textFont(font);
  softkey("Delete"); // Use softkey to to delete characters from the multitap buffer
  multitap(); // Turn on multitap key input
}

void draw() {
  background(200);
  text(multitapText, 0, height / 2); // Draw the text captured with multitap
}
void softkeyPressed(String label) {
  if (label.equals("Delete")) {
    multitapDeleteChar(); // Delete a character
  }
}
