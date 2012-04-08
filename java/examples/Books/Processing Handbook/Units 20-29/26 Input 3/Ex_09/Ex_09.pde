int frame = 0;

void setup() {
  size(100, 100);
  frameRate(30);
}

void draw() {
  if (frame > 60) { // If 60 frames since the mouse
    noLoop(); // was pressed, stop the program
    background(0); // and turn the background black.
  } else { // Otherwise, set the background
    background(204); // to light gray and draw lines
    line(mouseX, 0, mouseX, 100); // at the mouse position
    line(0, mouseY, 100, mouseY);
    frame++;
  }
}

void mousePressed() {
  loop();
  frame = 0;
}