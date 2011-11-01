// The word "avoid" stays away from the mouse because its
// position is set to the inverse of the cursor position
PFont f;

void setup() {
  size(100, 100);
  f = loadFont("Eureka-24.vlw");
  textFont(f);
  textAlign(CENTER);
  fill(0);
}

void draw() {
  background(204);
  text("avoid", width - mouseX, height - mouseY);
}
