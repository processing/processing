// Example 06-06 from "Getting Started with Processing" 
// by Reas & Fry. O'Reilly / Make 2010

PFont font;

void setup() {
  size(480, 120);
  smooth();
  font = loadFont("AndaleMono-36.vlw");
  textFont(font);
}

void draw() {
  background(102);
  textSize(36);
  text("That’s one small step for man...", 25, 60);
  textSize(18);
  text("That’s one small step for man...", 27, 90);
}

