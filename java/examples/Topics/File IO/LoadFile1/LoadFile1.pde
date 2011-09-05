/**
 * LoadFile 1
 * 
 * Loads a text file that contains two numbers separated by a tab ('\t').
 * A new pair of numbers is loaded each frame and used to draw a point on the screen.
 */

String[] lines;
int index = 0;

void setup() {
  size(200, 200);
  background(0);
  stroke(255);
  frameRate(12);
  lines = loadStrings("positions.txt");
}

void draw() {
  if (index < lines.length) {
    String[] pieces = split(lines[index], '\t');
    if (pieces.length == 2) {
      int x = int(pieces[0]) * 2;
      int y = int(pieces[1]) * 2;
      point(x, y);
    }
    // Go to the next line for the next run through draw()
    index = index + 1;
  }
}
