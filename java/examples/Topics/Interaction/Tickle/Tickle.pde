/**
 * Tickle. 
 * 
 * The word "tickle" jitters when the cursor hovers over.
 * Sometimes, it can be tickled off the screen.
 */

String message = "tickle";
float x, y; // X and Y coordinates of text
float hr, vr;  // horizontal and vertical radius of the text

void setup() {
  size(200, 200);
  PFont font = loadFont("AmericanTypewriter-24.vlw");
  textFont(font);
  textAlign(CENTER, CENTER);
  hr = textWidth(message) / 2;
  vr = (textAscent() + textDescent()) / 2;
  noStroke();
  x = width / 2;
  y = height / 2;
}

void draw() {
  // instead of clearing the background, fade it by drawing
  // a semi-transparent rectangle on top
  fill(204, 120);
  rect(0, 0, width, height);
  fill(0);
  // If the cursor is over the text, change the position
  if (abs(mouseX - x) < hr &&
      abs(mouseY - y) < vr) {
    x += random(-5, 5);
    y += random(-5, 5);
  }
  text("tickle", x, y);
}
