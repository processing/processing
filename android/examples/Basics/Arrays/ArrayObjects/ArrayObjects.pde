/**
 * Array Objects. 
 * 
 * Demonstrates the syntax for creating an array of custom objects. 
 * 
 * Updated 26 February 2010.
 */

int unit = 40;
int count;
Module[] mods;


void setup() {
  size(320, 240);
  background(176);
  noStroke();

  int wideCount = width / unit;
  int highCount = height / unit;
  count = wideCount * highCount;
  mods = new Module[count];

  int index = 0;
  for (int y = 0; y < highCount; y++) {
    for (int x = 0; x < wideCount; x++) {
      mods[index++] = new Module(x*unit, y*unit, unit/2, unit/2, random(0.05, 0.8));
    }
  }
}


void draw() {
  for (int i = 0; i < count; i++) {
    mods[i].update();
    mods[i].draw();
  }
}

