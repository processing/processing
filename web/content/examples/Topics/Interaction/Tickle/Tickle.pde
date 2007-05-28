/**
 * Tickle. 
 *
 * The word "tickle" jitters when the cursor hovers over.
 * Sometimes, it can be tickled off the screen.
 * 
 * Created November 21 2006
 */

PFont f;
float x = 33; // X-coordinate of text
float y = 60; // Y-coordinate of text

void setup() 
{
  size(200, 200);
  f = loadFont("AmericanTypewriter-24.vlw");
  textFont(f);
  noStroke();
}

void draw() 
{
  fill(204, 120);
  rect(0, 0, width, height);
  fill(0);
  // If the cursor is over the text, change the position
  if ((mouseX >= x) && (mouseX <= x+55) &&
    (mouseY >= y-24) && (mouseY <= y)) {
    x += random(-5, 5);
    y += random(-5, 5);
  }
  text("tickle", x, y);
}
