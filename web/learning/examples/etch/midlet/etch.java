import processing.core.*; public class etch extends PMIDlet{// etch
// by Francis Li <http://www.francisli.com/>
//
// A mobile phone version of the classic Etch-A-Sketch toy.  Press
// the "Shake" softkey to erase the screen.  This simple example
// illustrates how softkey events are handled and how
// continuous key press input can be handled from within the
// draw() method
//
int x, y;

public void setup() {
  softkey("Shake");

  x = width / 2;
  y = height / 2;
  
  framerate(15);
}

public void draw() {
  point(x, y);

  if (keyPressed) {
    switch (keyCode) {
    case UP:
      y = max(0, y - 1);
      break;
    case DOWN:
      y = min(height, y + 1);
      break;
    case LEFT:
      x = max(0, x - 1);
      break;
    case RIGHT:
      x = min(width, x + 1);
      break;
    }
  }
}

public void softkeyPressed(String label) {
  if (label.equals("Shake")) {
    background(200);
  }  
}

}